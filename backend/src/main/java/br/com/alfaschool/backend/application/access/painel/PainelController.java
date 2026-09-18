package br.com.alfaschool.backend.application.access.painel;

import br.com.alfaschool.backend.application.access.painel.dto.PainelDispositivoCriadoResponse;
import br.com.alfaschool.backend.application.access.painel.dto.PainelDispositivoRequest;
import br.com.alfaschool.backend.application.access.painel.dto.PainelDispositivoResponse;
import br.com.alfaschool.backend.application.access.painel.dto.PainelFonteRequest;
import br.com.alfaschool.backend.application.access.painel.dto.PainelFonteResponse;
import br.com.alfaschool.backend.application.access.painel.dto.PainelRequest;
import br.com.alfaschool.backend.application.access.painel.dto.PainelResponse;
import br.com.alfaschool.backend.application.access.painel.dto.ResumoCoordenacaoResponse;
import br.com.alfaschool.backend.application.access.retirada.ContextoAcesso;
import br.com.alfaschool.backend.shared.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Administracao dos paineis. Tudo aqui exige JWT e modulo ACCESS.
 *
 * As rotas que a TV consome (stream e estado) ficam em
 * PainelStreamController, que autentica por token de dispositivo.
 */
@RestController
@RequestMapping("/api/v1/access/paineis")
public class PainelController {

    private final PainelService painelService;
    private final PainelResumoService resumoService;

    public PainelController(PainelService painelService, PainelResumoService resumoService) {
        this.painelService = painelService;
        this.resumoService = resumoService;
    }

    // ---------------------------------------------------------------
    // Painel de coordenacao (dados agregados)
    // ---------------------------------------------------------------

    @GetMapping("/coordenacao/resumo")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_ACESSO_PAINEL_VER')")
    public ResponseEntity<ApiResponse<ResumoCoordenacaoResponse>> resumoCoordenacao(
            @RequestParam(required = false) UUID unitId,
            @RequestParam(required = false) Integer limiteEsperaMinutos) {
        UUID tenantId = ContextoAcesso.tenantObrigatorio();
        return ResponseEntity.ok(ApiResponse.of(200, "Resumo da coordenacao carregado com sucesso",
                resumoService.resumo(tenantId, unitId, limiteEsperaMinutos)));
    }

    // ---------------------------------------------------------------
    // CRUD de painel
    // ---------------------------------------------------------------

    @GetMapping
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_ACESSO_PAINEL_VER')")
    public ResponseEntity<ApiResponse<Page<PainelResponse>>> listar(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.of(200, "Paineis listados com sucesso",
                painelService.listar(pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_ACESSO_PAINEL_VER')")
    public ResponseEntity<ApiResponse<PainelResponse>> buscar(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(200, "Painel encontrado", painelService.buscar(id)));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_ACESSO_ESTRUTURA_GERIR')")
    public ResponseEntity<ApiResponse<PainelResponse>> criar(@Valid @RequestBody PainelRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(201, "Painel criado com sucesso", painelService.criar(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_ACESSO_ESTRUTURA_GERIR')")
    public ResponseEntity<ApiResponse<PainelResponse>> atualizar(@PathVariable UUID id,
                                                                  @Valid @RequestBody PainelRequest request) {
        return ResponseEntity.ok(ApiResponse.of(200, "Painel atualizado com sucesso",
                painelService.atualizar(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_ACESSO_ESTRUTURA_GERIR')")
    public ResponseEntity<ApiResponse<Void>> remover(@PathVariable UUID id) {
        painelService.remover(id);
        return ResponseEntity.ok(ApiResponse.of(200, "Painel removido com sucesso", null));
    }

    // ---------------------------------------------------------------
    // Fontes
    // ---------------------------------------------------------------

    @GetMapping("/{id}/fontes")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_ACESSO_PAINEL_VER')")
    public ResponseEntity<ApiResponse<List<PainelFonteResponse>>> fontes(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(200, "Fontes listadas com sucesso", painelService.fontes(id)));
    }

    @PostMapping("/{id}/fontes")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_ACESSO_ESTRUTURA_GERIR')")
    public ResponseEntity<ApiResponse<PainelFonteResponse>> adicionarFonte(
            @PathVariable UUID id, @Valid @RequestBody PainelFonteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(201, "Fonte adicionada com sucesso",
                        painelService.adicionarFonte(id, request)));
    }

    @DeleteMapping("/{id}/fontes/{fonteId}")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_ACESSO_ESTRUTURA_GERIR')")
    public ResponseEntity<ApiResponse<Void>> removerFonte(@PathVariable UUID id, @PathVariable UUID fonteId) {
        painelService.removerFonte(id, fonteId);
        return ResponseEntity.ok(ApiResponse.of(200, "Fonte removida com sucesso", null));
    }

    // ---------------------------------------------------------------
    // Dispositivos (TVs)
    // ---------------------------------------------------------------

    @GetMapping("/{id}/dispositivos")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_ACESSO_PAINEL_VER')")
    public ResponseEntity<ApiResponse<List<PainelDispositivoResponse>>> dispositivos(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(200, "Dispositivos listados com sucesso",
                painelService.dispositivos(id)));
    }

    /** O token so' aparece nesta resposta. Nao ha' como recupera-lo depois. */
    @PostMapping("/{id}/dispositivos")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_ACESSO_ESTRUTURA_GERIR')")
    public ResponseEntity<ApiResponse<PainelDispositivoCriadoResponse>> criarDispositivo(
            @PathVariable UUID id, @Valid @RequestBody PainelDispositivoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(201, "Dispositivo criado com sucesso. Guarde o token agora.",
                        painelService.criarDispositivo(id, request)));
    }

    @PostMapping("/{id}/dispositivos/{dispositivoId}/revogar")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_ACESSO_ESTRUTURA_GERIR')")
    public ResponseEntity<ApiResponse<PainelDispositivoResponse>> revogarDispositivo(
            @PathVariable UUID id, @PathVariable UUID dispositivoId) {
        return ResponseEntity.ok(ApiResponse.of(200, "Dispositivo revogado com sucesso",
                painelService.revogarDispositivo(id, dispositivoId)));
    }
}
