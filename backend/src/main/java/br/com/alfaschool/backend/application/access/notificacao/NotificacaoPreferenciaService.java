package br.com.alfaschool.backend.application.access.notificacao;

import br.com.alfaschool.backend.application.access.notificacao.dto.PreferenciaRequest;
import br.com.alfaschool.backend.application.access.notificacao.dto.PreferenciaResponse;
import br.com.alfaschool.backend.domain.access.notificacao.AccNotificacaoPreferencia;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccNotificacaoPreferenciaRepository;
import br.com.alfaschool.backend.security.filter.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * CRUD de preferencias/opt-in.
 *
 * <p>O carimbo de consentimento e' o produto principal desta classe. Ligar
 * ({@code habilitado = true}) grava {@code opt_in_em}; desligar grava
 * {@code opt_out_em} e NAO apaga o registro anterior — a escola precisa poder
 * provar quando o responsavel autorizou e quando revogou.
 */
@Service
public class NotificacaoPreferenciaService {

    private final AccNotificacaoPreferenciaRepository preferenciaRepository;

    public NotificacaoPreferenciaService(AccNotificacaoPreferenciaRepository preferenciaRepository) {
        this.preferenciaRepository = preferenciaRepository;
    }

    public List<PreferenciaResponse> listarPorTitular(UUID titularId) {
        UUID tenantId = tenantObrigatorio();
        return preferenciaRepository.findByTenantIdAndTitularIdAndDeletedFalse(tenantId, titularId).stream()
                .map(PreferenciaResponse::from)
                .toList();
    }

    @Transactional
    public PreferenciaResponse salvar(PreferenciaRequest request) {
        UUID tenantId = tenantObrigatorio();

        AccNotificacaoPreferencia preferencia = preferenciaRepository
                .findFirstByTenantIdAndTitularTipoAndTitularIdAndCanalAndEventoIsNullAndDeletedFalse(
                        tenantId, request.titularTipo(), request.titularId(), request.canal())
                .filter(p -> request.evento() == null)
                .orElseGet(() -> {
                    AccNotificacaoPreferencia nova = new AccNotificacaoPreferencia();
                    nova.setTenantId(tenantId);
                    nova.setTitularTipo(request.titularTipo());
                    nova.setTitularId(request.titularId());
                    nova.setCanal(request.canal());
                    nova.setEvento(request.evento());
                    return nova;
                });

        preferencia.setDestino(request.destino());
        aplicarConsentimento(preferencia, Boolean.TRUE.equals(request.habilitado()));

        return PreferenciaResponse.from(preferenciaRepository.save(preferencia));
    }

    @Transactional
    public PreferenciaResponse alternar(UUID id, boolean habilitado) {
        AccNotificacaoPreferencia preferencia = carregar(id);
        aplicarConsentimento(preferencia, habilitado);
        return PreferenciaResponse.from(preferenciaRepository.save(preferencia));
    }

    @Transactional
    public void remover(UUID id) {
        AccNotificacaoPreferencia preferencia = carregar(id);
        // Soft delete + opt-out: o historico de consentimento nao pode sumir.
        preferencia.setHabilitado(false);
        preferencia.setOptOutEm(Instant.now());
        preferencia.setDeleted(true);
        preferenciaRepository.save(preferencia);
    }

    private void aplicarConsentimento(AccNotificacaoPreferencia preferencia, boolean habilitado) {
        Instant agora = Instant.now();
        if (habilitado) {
            preferencia.setHabilitado(true);
            // So' carimbamos o opt-in na transicao; renovar o carimbo a cada
            // salvamento de tela falsificaria a data do consentimento.
            if (preferencia.getOptInEm() == null) {
                preferencia.setOptInEm(agora);
            }
            preferencia.setOptOutEm(null);
        } else {
            preferencia.setHabilitado(false);
            preferencia.setOptOutEm(agora);
        }
    }

    private AccNotificacaoPreferencia carregar(UUID id) {
        UUID tenantId = tenantObrigatorio();
        return preferenciaRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Preferência de notificação não encontrada"));
    }

    private UUID tenantObrigatorio() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tenant não identificado na requisição");
        }
        return tenantId;
    }
}
