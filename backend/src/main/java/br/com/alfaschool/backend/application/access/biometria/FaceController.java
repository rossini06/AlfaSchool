package br.com.alfaschool.backend.application.access.biometria;

import br.com.alfaschool.backend.application.access.biometria.dto.CadastroFaceRequest;
import br.com.alfaschool.backend.application.access.biometria.dto.FaceDto;
import br.com.alfaschool.backend.application.access.biometria.dto.FaceSyncDto;
import br.com.alfaschool.backend.domain.access.biometria.AccFace;
import br.com.alfaschool.backend.domain.access.biometria.AccFaceSync;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccFaceRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccFaceSyncRepository;
import br.com.alfaschool.backend.security.filter.TenantContext;
import br.com.alfaschool.backend.shared.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import br.com.alfaschool.backend.application.access.retirada.ContextoAcesso;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/** Cadastro de biometria facial e seu veredito por equipamento. */
@RestController
@RequestMapping("/api/v1/access/faces")
public class FaceController {

    private final FaceService service;
    private final AccFaceRepository faces;
    private final AccFaceSyncRepository sincronizacoes;

    public FaceController(FaceService service,
                          AccFaceRepository faces,
                          AccFaceSyncRepository sincronizacoes) {
        this.service = service;
        this.faces = faces;
        this.sincronizacoes = sincronizacoes;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_ACESSO_BIOMETRIA_GERIR')")
    public ResponseEntity<ApiResponse<FaceDto>> cadastrar(@Valid @RequestBody CadastroFaceRequest req) {
        AccFace face = service.cadastrar(tenant(), req);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.of(201, "Face cadastrada.", FaceDto.from(face)));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_ACESSO_BIOMETRIA_GERIR')")
    public ResponseEntity<ApiResponse<List<FaceDto>>> listar(Pageable pageable) {
        List<FaceDto> lista = faces.findByTenantIdAndDeletedFalse(tenant(), pageable)
                .getContent().stream().map(FaceDto::from).toList();
        return ResponseEntity.ok(ApiResponse.of(200, "Faces listadas.", lista));
    }

    @GetMapping("/{id}/sincronizacoes")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_ACESSO_BIOMETRIA_GERIR')")
    public ResponseEntity<ApiResponse<List<FaceSyncDto>>> sincronizacoes(@PathVariable UUID id) {
        List<FaceSyncDto> lista = sincronizacoes.findByTenantIdAndFaceId(tenant(), id)
                .stream().map(FaceSyncDto::from).toList();
        return ResponseEntity.ok(ApiResponse.of(200, "Situação por equipamento.", lista));
    }

    /**
     * Exporta a face para um equipamento.
     *
     * Bloqueio por falta de base legal/consentimento responde 422, nao
     * 500: nao houve erro, houve recusa deliberada, e a tela precisa
     * mostrar o motivo para a secretaria resolver.
     */
    @PostMapping("/{id}/sincronizar/{dispositivoId}")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_ACESSO_BIOMETRIA_GERIR')")
    public ResponseEntity<ApiResponse<FaceSyncDto>> sincronizar(@PathVariable UUID id,
                                                                @PathVariable UUID dispositivoId) {
        try {
            AccFaceSync sync = service.sincronizar(tenant(), id, dispositivoId);
            return ResponseEntity.ok(ApiResponse.of(200, "Sincronização concluída.",
                    FaceSyncDto.from(sync)));
        } catch (ExportacaoBiometriaBloqueadaException e) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
        }
    }

    @DeleteMapping("/{id}/dispositivos/{dispositivoId}")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_ACESSO_BIOMETRIA_GERIR')")
    public ResponseEntity<ApiResponse<FaceSyncDto>> remover(@PathVariable UUID id,
                                                            @PathVariable UUID dispositivoId) {
        AccFaceSync sync = service.remover(tenant(), id, dispositivoId);
        return ResponseEntity.ok(ApiResponse.of(200, "Biometria removida do equipamento.",
                FaceSyncDto.from(sync)));
    }

    /**
     * A familia retira o consentimento da biometria.
     *
     * LGPD Art. 8 par. 5: a revogacao e' um direito exercivel a qualquer
     * momento, de forma gratuita e facilitada — por isso o motivo e'
     * OPCIONAL. Pedir justificativa para exercer um direito e' criar
     * atrito onde a lei manda facilitar.
     *
     * A resposta diz de quantos leitores o rosto saiu e quais NAO
     * confirmaram: a revogacao vale de qualquer jeito, mas equipamento
     * offline com o rosto dentro e' problema que alguem precisa resolver, e
     * esconder isso seria pior do que a falha.
     */
    @PostMapping("/{id}/revogar-consentimento")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_ACESSO_BIOMETRIA_GERIR')")
    public ResponseEntity<ApiResponse<FaceService.RevogacaoConsentimento>> revogarConsentimento(
            @PathVariable UUID id,
            @RequestBody(required = false) MotivoOpcional corpo) {
        String motivo = corpo == null ? null : corpo.motivo();
        FaceService.RevogacaoConsentimento r =
                service.revogarConsentimento(tenant(), id, ContextoAcesso.userIdOuNulo(), motivo);

        String mensagem = r.equipamentosComFalha().isEmpty()
                ? "Consentimento revogado. A biometria foi removida de " + r.removidaDeEquipamentos()
                  + " equipamento(s)."
                : "Consentimento revogado, mas estes equipamentos não confirmaram a remoção: "
                  + String.join(", ", r.equipamentosComFalha()) + ". Verifique-os.";
        return ResponseEntity.ok(ApiResponse.of(200, mensagem, r));
    }

    public record MotivoOpcional(String motivo) {
    }

    private UUID tenant() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tenant não identificado.");
        }
        return tenantId;
    }
}
