package br.com.alfaschool.backend.application.access.notificacao;

import br.com.alfaschool.backend.application.access.shared.NotificacaoPort;
import br.com.alfaschool.backend.domain.access.notificacao.AccNotificacaoEnvio;
import br.com.alfaschool.backend.domain.access.notificacao.AccNotificacaoPreferencia;
import br.com.alfaschool.backend.domain.access.notificacao.AccNotificacaoTemplate;
import br.com.alfaschool.backend.domain.access.shared.CanalNotificacao;
import br.com.alfaschool.backend.domain.access.shared.EventoNotificacao;
import br.com.alfaschool.backend.domain.access.shared.TitularTipo;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccNotificacaoPreferenciaRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccNotificacaoTemplateRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Motor central de notificacoes.
 *
 * <h2>Por que um motor unico</h2>
 * Se cada funcionalidade disparasse a propria mensagem, teriamos N
 * implementacoes de retry, N formas de respeitar opt-in e nenhum lugar para
 * auditar o que a escola mandou para a familia. Aqui tudo passa por uma fila em
 * tabela: quem enfileira nao espera, nao escolhe canal e nao conhece provedor.
 *
 * <h2>Garantias</h2>
 * <ul>
 *   <li><b>Nunca lanca.</b> {@link #enfileirar} captura tudo. Notificacao e'
 *       efeito colateral; nao pode derrubar o registro de uma catraca.</li>
 *   <li><b>Consentimento obrigatorio.</b> So' vira envio quem tem preferencia
 *       com {@code habilitado = true} e {@code opt_in_em} preenchido. Sem
 *       opt-in nao ha envio, e o motivo vai para o log.</li>
 *   <li><b>Idempotente.</b> A chave do chamador identifica o FATO; a chave
 *       gravada deriva dela com canal e destinatario. UNIQUE
 *       (tenant, chave_idempotencia) garante uma linha so'.</li>
 *   <li><b>Sem dado sensivel.</b> As variaveis passam por
 *       {@link NotificacaoRenderer#validarVariaveis}.</li>
 * </ul>
 */
@Service
public class NotificacaoService implements NotificacaoPort {

    private static final Logger log = LoggerFactory.getLogger(NotificacaoService.class);

    private final AccNotificacaoPreferenciaRepository preferenciaRepository;
    private final AccNotificacaoTemplateRepository templateRepository;
    private final EnvioFilaWriter filaWriter;
    private final NotificacaoRenderer renderer;
    private final NotificacaoWorker worker;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public NotificacaoService(AccNotificacaoPreferenciaRepository preferenciaRepository,
                              AccNotificacaoTemplateRepository templateRepository,
                              EnvioFilaWriter filaWriter,
                              NotificacaoRenderer renderer,
                              NotificacaoWorker worker) {
        this.preferenciaRepository = preferenciaRepository;
        this.templateRepository = templateRepository;
        this.filaWriter = filaWriter;
        this.renderer = renderer;
        this.worker = worker;
    }

    @Override
    public void enfileirar(UUID tenantId, EventoNotificacao evento, TitularTipo titularTipo, UUID titularId,
                           UUID alunoId, Map<String, String> variaveis, String chaveIdempotencia) {
        try {
            enfileirarInterno(tenantId, evento, titularTipo, titularId, alunoId, variaveis, chaveIdempotencia);
        } catch (RuntimeException e) {
            // Rede de seguranca final. Qualquer coisa que escape vira log.
            log.error("Falha ao enfileirar notificacao (evento={}, chave={}): {}",
                    evento, chaveIdempotencia, e.toString(), e);
        }
    }

    /**
     * @return envios efetivamente criados. Visivel para os testes e para o
     *         endpoint de teste manual, que precisa saber o que foi gerado.
     */
    public List<AccNotificacaoEnvio> enfileirarInterno(UUID tenantId, EventoNotificacao evento,
                                                       TitularTipo titularTipo, UUID titularId, UUID alunoId,
                                                       Map<String, String> variaveis, String chaveIdempotencia) {
        List<AccNotificacaoEnvio> criados = new ArrayList<>();
        if (tenantId == null || evento == null) {
            log.warn("Requisicao de notificacao invalida: tenant ou evento nulo");
            return criados;
        }
        if (chaveIdempotencia == null || chaveIdempotencia.isBlank()) {
            log.warn("Requisicao de notificacao sem chave de idempotencia (evento={})", evento);
            return criados;
        }

        // Privacidade antes de tudo: se as variaveis carregam foto ou
        // biometria, nada e' enfileirado. Falha fechada.
        Map<String, String> vars = variaveis == null ? Map.of() : variaveis;
        try {
            renderer.validarVariaveis(vars);
        } catch (NotificacaoRenderer.VariavelProibidaException e) {
            log.error("Notificacao BLOQUEADA por variavel proibida (evento={}, chave={}): {}",
                    evento, chaveIdempotencia, e.getMessage());
            return criados;
        }

        List<Destinatario> destinatarios = resolverDestinatarios(tenantId, titularTipo, titularId, alunoId);
        if (destinatarios.isEmpty()) {
            log.info("Nenhum destinatario para evento={} aluno={} — nada enfileirado", evento, alunoId);
            return criados;
        }

        Map<String, String> completas = new LinkedHashMap<>(vars);
        completas.computeIfAbsent("aluno", k -> nomeDoAluno(tenantId, alunoId));

        for (Destinatario destinatario : destinatarios) {
            List<AccNotificacaoPreferencia> preferencias =
                    preferenciaRepository.findByTenantIdAndTitularIdAndDeletedFalse(tenantId, destinatario.id());

            List<AccNotificacaoPreferencia> aplicaveis = preferencias.stream()
                    .filter(p -> p.getEvento() == null || p.getEvento() == evento)
                    .toList();

            if (aplicaveis.isEmpty()) {
                log.info("Sem preferencia cadastrada para titular={} evento={} — sem envio "
                        + "(opt-in e' obrigatorio)", destinatario.id(), evento);
                continue;
            }

            for (AccNotificacaoPreferencia preferencia : aplicaveis) {
                if (!preferencia.temConsentimento()) {
                    log.info("Envio bloqueado por falta de opt-in: titular={} canal={} evento={} "
                                    + "(habilitado={}, optInEm={})",
                            destinatario.id(), preferencia.getCanal(), evento,
                            preferencia.isHabilitado(), preferencia.getOptInEm());
                    continue;
                }

                AccNotificacaoEnvio envio = montarEnvio(tenantId, evento, alunoId, destinatario,
                        preferencia, completas, chaveIdempotencia);
                filaWriter.gravarSeNovo(envio).ifPresent(criados::add);
            }
        }

        // Dispara o processamento fora da transacao do chamador. Se o processo
        // cair agora, a varredura de 60s pega o que ficou PENDENTE.
        for (AccNotificacaoEnvio envio : criados) {
            worker.processar(envio.getId());
        }
        return criados;
    }

    private AccNotificacaoEnvio montarEnvio(UUID tenantId, EventoNotificacao evento, UUID alunoId,
                                            Destinatario destinatario, AccNotificacaoPreferencia preferencia,
                                            Map<String, String> variaveis, String chaveBase) {
        CanalNotificacao canal = preferencia.getCanal();

        Optional<AccNotificacaoTemplate> template = templateRepository
                .findFirstByTenantIdAndEventoAndCanalAndAtivoTrueAndDeletedFalse(tenantId, evento, canal);

        String assuntoCru;
        String corpoCru;
        if (template.isPresent()) {
            assuntoCru = template.get().getAssunto();
            corpoCru = template.get().getCorpo();
        } else {
            // Sem template cadastrado usamos o texto de fabrica: o pior cenario
            // e' o evento acontecer e ninguem ser avisado por falta de cadastro.
            NotificacaoRenderer.TemplatePadrao padrao = renderer.padrao(evento, canal);
            assuntoCru = padrao.assunto();
            corpoCru = padrao.corpo();
        }

        Map<String, String> completas = new LinkedHashMap<>(variaveis);
        completas.putIfAbsent("destinatario", destinatario.nome() == null ? "" : destinatario.nome());

        AccNotificacaoEnvio envio = new AccNotificacaoEnvio();
        envio.setTenantId(tenantId);
        envio.setCanal(canal);
        envio.setEvento(evento);
        envio.setTitularTipo(destinatario.tipo());
        envio.setTitularId(destinatario.id());
        envio.setAlunoId(alunoId);
        envio.setDestino(preferencia.getDestino());
        envio.setAssunto(truncar(renderer.renderizar(assuntoCru, completas), 255));
        envio.setCorpo(renderer.renderizar(corpoCru, completas));
        envio.setPayloadJson(serializar(completas));
        envio.setAgendadoPara(Instant.now());
        envio.setChaveIdempotencia(derivarChave(chaveBase, canal, destinatario.id()));
        return envio;
    }

    /**
     * A chave que o chamador passa identifica o FATO ("ENTRADA:{eventoId}").
     * Como um fato gera uma linha por destinatario e canal, a chave gravada
     * precisa incluir os dois — senao o segundo responsavel colidiria com o
     * primeiro e ficaria sem aviso. Reprocessar o mesmo fato continua gerando
     * exatamente as mesmas chaves derivadas, entao a dedupe se mantem.
     */
    static String derivarChave(String chaveBase, CanalNotificacao canal, UUID titularId) {
        String derivada = chaveBase + "|" + canal + "|" + titularId;
        return derivada.length() <= 160 ? derivada : derivada.substring(0, 160);
    }

    /**
     * Resolve quem pode ser avisado, ANTES de qualquer checagem de
     * consentimento.
     *
     * <p>Quando ha aluno, expande para responsaveis vinculados e para pessoas
     * autorizadas com {@code recebe_notificacao}. Quando ha titular direto, ele
     * tambem entra. Um mesmo id nunca entra duas vezes.
     */
    private List<Destinatario> resolverDestinatarios(UUID tenantId, TitularTipo titularTipo, UUID titularId,
                                                     UUID alunoId) {
        Map<UUID, Destinatario> porId = new LinkedHashMap<>();

        if (titularId != null && titularTipo != null && titularTipo != TitularTipo.ALUNO) {
            String nome = null;
            if (titularTipo == TitularTipo.RESPONSAVEL) {
                try {
                    nome = preferenciaRepository.buscarResponsavel(tenantId.toString(), titularId.toString())
                            .map(AccNotificacaoPreferenciaRepository.DestinatarioProjection::getNome)
                            .orElse(null);
                } catch (RuntimeException e) {
                    log.debug("Nao foi possivel carregar nome do titular {}: {}", titularId, e.toString());
                }
            }
            porId.put(titularId, new Destinatario(titularTipo, titularId, nome));
        }

        if (alunoId != null) {
            try {
                preferenciaRepository.buscarResponsaveisDoAluno(tenantId.toString(), alunoId.toString())
                        .forEach(p -> adicionar(porId, TitularTipo.RESPONSAVEL, p));
            } catch (RuntimeException e) {
                log.warn("Falha ao resolver responsaveis do aluno {}: {}", alunoId, e.toString());
            }
            try {
                preferenciaRepository.buscarPessoasAutorizadasDoAluno(tenantId.toString(), alunoId.toString())
                        .forEach(p -> adicionar(porId, TitularTipo.AUTORIZADA, p));
            } catch (RuntimeException e) {
                // Nao avisar as pessoas autorizadas nao pode impedir o aviso aos
                // responsaveis.
                log.warn("Falha ao resolver pessoas autorizadas do aluno {}: {}", alunoId, e.toString());
            }
        }

        return new ArrayList<>(porId.values());
    }

    private void adicionar(Map<UUID, Destinatario> porId, TitularTipo tipo,
                           AccNotificacaoPreferenciaRepository.DestinatarioProjection p) {
        if (p == null || p.getTitularId() == null) {
            return;
        }
        try {
            UUID id = UUID.fromString(p.getTitularId().trim());
            porId.putIfAbsent(id, new Destinatario(tipo, id, p.getNome()));
        } catch (IllegalArgumentException e) {
            log.warn("Id de destinatario invalido vindo do banco: {}", p.getTitularId());
        }
    }

    private String nomeDoAluno(UUID tenantId, UUID alunoId) {
        if (alunoId == null) {
            return "";
        }
        try {
            return preferenciaRepository.buscarNomeDoAluno(tenantId.toString(), alunoId.toString()).orElse("");
        } catch (RuntimeException e) {
            log.debug("Nao foi possivel carregar o nome do aluno {}: {}", alunoId, e.toString());
            return "";
        }
    }

    private String serializar(Map<String, String> variaveis) {
        try {
            return objectMapper.writeValueAsString(variaveis);
        } catch (Exception e) {
            // payload_json e' conveniencia de rastreio; nunca vale uma falha.
            return null;
        }
    }

    private String truncar(String s, int max) {
        if (s == null) {
            return null;
        }
        return s.length() <= max ? s : s.substring(0, max);
    }

    /** Pessoa que pode receber, antes de qualquer checagem de consentimento. */
    record Destinatario(TitularTipo tipo, UUID id, String nome) {
    }
}
