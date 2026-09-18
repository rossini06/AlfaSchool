package br.com.alfaschool.backend.application.access.notificacao;

import br.com.alfaschool.backend.application.access.notificacao.dto.NotificacaoConfigRequest;
import br.com.alfaschool.backend.application.access.notificacao.dto.NotificacaoConfigResponse;
import br.com.alfaschool.backend.infrastructure.crypto.SegredoCifrador;
import br.com.alfaschool.backend.domain.access.notificacao.AccNotificacaoConfig;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccNotificacaoConfigRepository;
import br.com.alfaschool.backend.security.filter.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * CRUD de configuracao de canal.
 *
 * <p>Regra do segredo: entra em claro, e' cifrado com
 * {@link SegredoCifrador} antes de tocar o banco, e NUNCA volta em resposta
 * nenhuma. Enviar {@code segredo = null} mantem o que ja existe (a tela de
 * edicao nao precisa reenviar o token a cada salvamento); enviar string vazia
 * apaga.
 */
@Service
public class NotificacaoConfigService {

    private final AccNotificacaoConfigRepository configRepository;
    private final SegredoCifrador segredoCifrador;

    public NotificacaoConfigService(AccNotificacaoConfigRepository configRepository,
                                    SegredoCifrador segredoCifrador) {
        this.configRepository = configRepository;
        this.segredoCifrador = segredoCifrador;
    }

    public List<NotificacaoConfigResponse> listar() {
        UUID tenantId = tenantObrigatorio();
        return configRepository.findByTenantIdAndDeletedFalseOrderByCanalAsc(tenantId).stream()
                .map(NotificacaoConfigResponse::from)
                .toList();
    }

    public NotificacaoConfigResponse buscar(UUID id) {
        return NotificacaoConfigResponse.from(carregar(id));
    }

    @Transactional
    public NotificacaoConfigResponse criar(NotificacaoConfigRequest request) {
        UUID tenantId = tenantObrigatorio();
        if (configRepository.existsByTenantIdAndCanalAndDeletedFalse(tenantId, request.canal())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ja existe configuração para o canal " + request.canal());
        }
        AccNotificacaoConfig config = new AccNotificacaoConfig();
        config.setTenantId(tenantId);
        config.setCanal(request.canal());
        aplicar(config, request);
        return NotificacaoConfigResponse.from(configRepository.save(config));
    }

    @Transactional
    public NotificacaoConfigResponse atualizar(UUID id, NotificacaoConfigRequest request) {
        AccNotificacaoConfig config = carregar(id);
        aplicar(config, request);
        return NotificacaoConfigResponse.from(configRepository.save(config));
    }

    @Transactional
    public void remover(UUID id) {
        AccNotificacaoConfig config = carregar(id);
        config.setDeleted(true);
        configRepository.save(config);
    }

    private void aplicar(AccNotificacaoConfig config, NotificacaoConfigRequest request) {
        if (request.provider() != null) {
            config.setProvider(request.provider());
        }
        config.setRemetente(request.remetente());
        config.setConfigJson(request.configJson());
        if (request.limiteDiario() != null) {
            config.setLimiteDiario(request.limiteDiario());
        }
        if (request.ativo() != null) {
            config.setAtivo(request.ativo());
        }

        // null = nao mexer no segredo; vazio = apagar; valor = cifrar.
        if (request.segredo() != null) {
            config.setSegredoCifrado(request.segredo().isBlank() ? null : segredoCifrador.cifrar(request.segredo()));
        }
    }

    private AccNotificacaoConfig carregar(UUID id) {
        UUID tenantId = tenantObrigatorio();
        return configRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Configuração de notificação não encontrada"));
    }

    private UUID tenantObrigatorio() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tenant não identificado na requisição");
        }
        return tenantId;
    }
}
