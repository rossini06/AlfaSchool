package br.com.alfaschool.backend.application.access.notificacao;

import br.com.alfaschool.backend.application.access.shared.AcessoRegistradoEvent;
import br.com.alfaschool.backend.application.access.shared.NotificacaoPort;
import br.com.alfaschool.backend.domain.access.shared.EventoNotificacao;
import br.com.alfaschool.backend.domain.access.shared.FuncaoDispositivo;
import br.com.alfaschool.backend.domain.access.shared.ResultadoAcesso;
import br.com.alfaschool.backend.domain.access.shared.SentidoAcesso;
import br.com.alfaschool.backend.domain.access.shared.TitularTipo;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccNotificacaoPreferenciaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Traduz eventos de portaria em notificacoes.
 *
 * <p>Fica numa classe separada de proposito: o motor de notificacoes nao pode
 * conhecer o dominio de acesso. Quando a fatia de financeiro quiser avisar
 * sobre cobranca, ela escreve o proprio dispatcher e o motor continua igual.
 *
 * <p>{@code AFTER_COMMIT}: so' avisamos a familia depois que o evento existe de
 * fato no banco. Avisar antes do commit e' arriscar contar sobre uma entrada
 * que um rollback apagou.
 */
@Component
public class AcessoNotificacaoDispatcher {

    private static final Logger log = LoggerFactory.getLogger(AcessoNotificacaoDispatcher.class);
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final NotificacaoPort notificacaoPort;
    private final AccNotificacaoPreferenciaRepository preferenciaRepository;

    public AcessoNotificacaoDispatcher(NotificacaoPort notificacaoPort,
                                       AccNotificacaoPreferenciaRepository preferenciaRepository) {
        this.notificacaoPort = notificacaoPort;
        this.preferenciaRepository = preferenciaRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void aoRegistrarAcesso(AcessoRegistradoEvent evento) {
        try {
            despachar(evento);
        } catch (RuntimeException e) {
            // Listener nunca propaga: uma falha aqui nao pode afetar nada.
            log.error("Falha ao despachar notificacao do acesso {}: {}",
                    evento == null ? null : evento.eventoId(), e.toString(), e);
        }
    }

    public void despachar(AcessoRegistradoEvent e) {
        if (e == null || e.eventoId() == null || e.tenantId() == null) {
            return;
        }

        // Tentativa NEGADA num leitor de responsavel: a familia precisa saber
        // agora. O evento nao traz aluno, entao avisamos a familia de cada
        // crianca ligada a pessoa que tentou.
        if (e.resultado() == ResultadoAcesso.NEGADO && e.funcaoDispositivo() == FuncaoDispositivo.RESPONSAVEL) {
            despacharTentativaNaoAutorizada(e);
            return;
        }

        // Entrada e saida confirmadas sao sobre o ALUNO. Leitura de responsavel
        // permitida nao e' entrada de aluno: reconhecer o responsavel nao
        // entrega a crianca.
        if (!e.permitido() || e.titularTipo() != TitularTipo.ALUNO || e.titularId() == null) {
            return;
        }

        if (e.sentido() == SentidoAcesso.ENTRADA) {
            notificacaoPort.enfileirar(e.tenantId(), EventoNotificacao.ENTRADA_CONFIRMADA,
                    null, null, e.titularId(), variaveis(e), "ENTRADA:" + e.eventoId());
        } else if (e.sentido() == SentidoAcesso.SAIDA) {
            notificacaoPort.enfileirar(e.tenantId(), EventoNotificacao.SAIDA_CONFIRMADA,
                    null, null, e.titularId(), variaveis(e), "SAIDA:" + e.eventoId());
        }
    }

    /**
     * A chave inclui o aluno porque um mesmo evento negado pode gerar um aviso
     * por crianca vinculada. Sem o aluno na chave, o segundo aviso colidiria
     * com o primeiro e uma das familias ficaria sem saber.
     */
    private void despacharTentativaNaoAutorizada(AcessoRegistradoEvent e) {
        List<UUID> alunos = alunosLigadosA(e);
        Map<String, String> vars = variaveis(e);

        if (alunos.isEmpty()) {
            // Pessoa desconhecida ou sem vinculo: avisa quem tiver preferencia
            // ligada para o titular, se houver. Nunca fica silencioso por falta
            // de vinculo.
            notificacaoPort.enfileirar(e.tenantId(), EventoNotificacao.TENTATIVA_NAO_AUTORIZADA,
                    e.titularTipo(), e.titularId(), null, vars, "NAO_AUTORIZADA:" + e.eventoId());
            return;
        }

        for (UUID alunoId : alunos) {
            notificacaoPort.enfileirar(e.tenantId(), EventoNotificacao.TENTATIVA_NAO_AUTORIZADA,
                    null, null, alunoId, vars, "NAO_AUTORIZADA:" + e.eventoId() + ":" + alunoId);
        }
    }

    private List<UUID> alunosLigadosA(AcessoRegistradoEvent e) {
        if (e.titularId() == null || e.titularTipo() == null) {
            return List.of();
        }
        try {
            List<String> ids = switch (e.titularTipo()) {
                case RESPONSAVEL -> preferenciaRepository.buscarAlunosDoResponsavel(
                        e.tenantId().toString(), e.titularId().toString());
                case AUTORIZADA -> preferenciaRepository.buscarAlunosDaPessoaAutorizada(
                        e.tenantId().toString(), e.titularId().toString());
                default -> List.of();
            };
            return ids.stream().map(String::trim).map(UUID::fromString).toList();
        } catch (RuntimeException ex) {
            log.warn("Nao foi possivel resolver alunos ligados ao titular {}: {}", e.titularId(), ex.toString());
            return List.of();
        }
    }

    /**
     * Apenas o minimo util: o que aconteceu e quando.
     *
     * <p>NAO entra aqui: foto do aluno, foto da catraca, template biometrico,
     * documento, endereco, observacao medica. O aviso viaja por e-mail e
     * WhatsApp, canais que a escola nao controla. O nome do aluno e'
     * preenchido pelo motor, que ja sabe de quem se trata.
     */
    private Map<String, String> variaveis(AcessoRegistradoEvent e) {
        Instant quando = e.dataHora() == null ? Instant.now() : e.dataHora();
        Map<String, String> v = new LinkedHashMap<>();
        v.put("hora", HORA.format(quando.atZone(FUSO)));
        v.put("data", DATA.format(quando.atZone(FUSO)));
        if (e.motivo() != null && !e.motivo().isBlank()) {
            v.put("motivo", e.motivo());
        }
        return v;
    }
}
