package br.com.alfaschool.backend.application.access.notificacao;

import br.com.alfaschool.backend.application.access.notificacao.dto.EnvioResponse;
import br.com.alfaschool.backend.application.access.notificacao.dto.TesteNotificacaoRequest;
import br.com.alfaschool.backend.domain.access.notificacao.AccNotificacaoEnvio;
import br.com.alfaschool.backend.domain.access.shared.CanalNotificacao;
import br.com.alfaschool.backend.domain.access.shared.EventoNotificacao;
import br.com.alfaschool.backend.domain.access.shared.StatusEnvio;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccNotificacaoEnvioRepository;
import br.com.alfaschool.backend.security.filter.TenantContext;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Historico de avisos, reenvio manual e disparo de teste. */
@Service
public class NotificacaoHistoricoService {

    /**
     * Evento usado nos disparos de teste.
     *
     * <p>{@code EventoNotificacao} nao tem um valor TESTE (V42). Usamos
     * EQUIPAMENTO_OFFLINE porque e' o unico evento puramente operacional do
     * enum — ele nunca vai para familia, entao um teste nesse balde nao
     * contamina o historico que a escola le. A chave de idempotencia comeca
     * com "TESTE:", o que permite filtrar. Ver relatorio: o ideal e' um valor
     * TESTE proprio no enum.
     */
    static final EventoNotificacao EVENTO_TESTE = EventoNotificacao.EQUIPAMENTO_OFFLINE;

    private final AccNotificacaoEnvioRepository envioRepository;
    private final EnvioFilaWriter filaWriter;
    private final NotificacaoWorker worker;

    public NotificacaoHistoricoService(AccNotificacaoEnvioRepository envioRepository,
                                       EnvioFilaWriter filaWriter,
                                       NotificacaoWorker worker) {
        this.envioRepository = envioRepository;
        this.filaWriter = filaWriter;
        this.worker = worker;
    }

    /**
     * Historico paginado. Usa Specification, e nao JPQL com parametro opcional,
     * porque filtro nulo sobre enum e' fonte classica de erro de inferencia de
     * tipo no Hibernate.
     */
    public Page<EnvioResponse> historico(CanalNotificacao canal, EventoNotificacao evento, StatusEnvio status,
                                         Instant inicio, Instant fim, String destino, Pageable pageable) {
        UUID tenantId = tenantObrigatorio();

        Specification<AccNotificacaoEnvio> spec = (root, query, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            // Filtro de tenant e' obrigatorio: nunca confiar em findById solto.
            ps.add(cb.equal(root.get("tenantId"), tenantId));
            if (canal != null) {
                ps.add(cb.equal(root.get("canal"), canal));
            }
            if (evento != null) {
                ps.add(cb.equal(root.get("evento"), evento));
            }
            if (status != null) {
                ps.add(cb.equal(root.get("status"), status));
            }
            if (inicio != null) {
                ps.add(cb.greaterThanOrEqualTo(root.get("createdAt"), inicio));
            }
            if (fim != null) {
                ps.add(cb.lessThanOrEqualTo(root.get("createdAt"), fim));
            }
            if (destino != null && !destino.isBlank()) {
                ps.add(cb.like(cb.lower(root.get("destino")), "%" + destino.toLowerCase() + "%"));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };

        return envioRepository.findAll(spec, pageable).map(EnvioResponse::from);
    }

    public EnvioResponse buscar(UUID id) {
        return EnvioResponse.from(carregar(id));
    }

    /**
     * Reenvio manual de um envio FALHOU. Zera tentativas e reagenda: quem
     * clica ja sabe que o problema foi resolvido (o e-mail foi corrigido, o
     * numero foi atualizado).
     */
    @Transactional
    public EnvioResponse reenviar(UUID id) {
        AccNotificacaoEnvio envio = carregar(id);
        if (envio.getStatus() == StatusEnvio.ENVIADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Este aviso já foi entregue; reenviar duplicaria a mensagem para a família");
        }
        if (envio.getStatus() == StatusEnvio.ENVIANDO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este aviso está sendo enviado agora");
        }
        filaWriter.reabrir(envio.getId());
        worker.processar(envio.getId());
        return EnvioResponse.from(carregar(id));
    }

    /**
     * Dispara um teste para um destino informado. Nao consulta preferencias:
     * e' o operador testando a infraestrutura com o proprio contato, sem dado
     * de aluno envolvido.
     */
    @Transactional
    public EnvioResponse teste(TesteNotificacaoRequest request) {
        UUID tenantId = tenantObrigatorio();

        String assunto = request.assunto() != null && !request.assunto().isBlank()
                ? request.assunto() : "Teste de notificacao AlfaSchool";
        String corpo = request.mensagem() != null && !request.mensagem().isBlank()
                ? request.mensagem()
                : "Mensagem de teste do canal " + request.canal()
                        + ". Se voce recebeu isto, o canal esta funcionando.";

        AccNotificacaoEnvio envio = new AccNotificacaoEnvio();
        envio.setTenantId(tenantId);
        envio.setCanal(request.canal());
        envio.setEvento(EVENTO_TESTE);
        envio.setDestino(request.destino());
        envio.setAssunto(assunto);
        envio.setCorpo(corpo);
        envio.setAgendadoPara(Instant.now());
        // Chave unica por disparo: teste e' para ser repetivel.
        envio.setChaveIdempotencia("TESTE:" + UUID.randomUUID());

        AccNotificacaoEnvio salvo = filaWriter.gravarSeNovo(envio)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Não foi possível enfileirar a mensagem de teste"));
        worker.processar(salvo.getId());
        return EnvioResponse.from(salvo);
    }

    private AccNotificacaoEnvio carregar(UUID id) {
        UUID tenantId = tenantObrigatorio();
        return envioRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Envio não encontrado"));
    }

    private UUID tenantObrigatorio() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tenant não identificado na requisição");
        }
        return tenantId;
    }
}
