package br.com.alfaschool.backend.application.access.autorizacao;

import br.com.alfaschool.backend.application.access.autorizacao.dto.RestricaoDocumentoResponse;
import br.com.alfaschool.backend.application.access.autorizacao.dto.RestricaoRequest;
import br.com.alfaschool.backend.application.access.autorizacao.dto.RestricaoResponse;
import br.com.alfaschool.backend.shared.response.ApiResponse;
import br.com.alfaschool.backend.shared.web.Paginacao;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

@RestController
@RequestMapping("/api/v1/access/restricoes")
public class RestricaoController {

    private final RestricaoService restricaoService;

    public RestricaoController(RestricaoService restricaoService) {
        this.restricaoService = restricaoService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_ACESSO_RESTRICOES_VER')")
    public ResponseEntity<ApiResponse<Page<RestricaoResponse>>> list(
            @RequestParam(required = false) UUID alunoId,
            /** VIGENTE | ENCERRADA. Vazio traz as duas. */
            @RequestParam(required = false) String situacao,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = Paginacao.de(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.of(200, "Restrições listadas com sucesso",
                restricaoService.list(alunoId, situacao, q, pageable)));
    }

    @GetMapping("/aluno/{alunoId}")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_ACESSO_RESTRICOES_VER')")
    public ResponseEntity<ApiResponse<List<RestricaoResponse>>> listarPorAluno(@PathVariable UUID alunoId) {
        return ResponseEntity.ok(ApiResponse.of(200, "Restrições do aluno listadas com sucesso",
                restricaoService.listarPorAluno(alunoId)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_ACESSO_RESTRICOES_VER')")
    public ResponseEntity<ApiResponse<RestricaoResponse>> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(200, "Restrição encontrada", restricaoService.findById(id)));
    }

    /**
     * Documento restrito (mandado, decisao judicial). Endpoint proprio e
     * @PreAuthorize mais estrito: alem do modulo, exige perfil de gestao.
     * Quem opera a portaria precisa saber que HA restricao, nao ler o processo.
     */
    @GetMapping("/{id}/documento")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_ACESSO_RESTRICOES_GERIR')")
    public ResponseEntity<ApiResponse<RestricaoDocumentoResponse>> documento(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(200, "Documento da restrição recuperado",
                restricaoService.documento(id)));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_ACESSO_RESTRICOES_GERIR')")
    public ResponseEntity<ApiResponse<RestricaoResponse>> create(@Valid @RequestBody RestricaoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(201, "Restrição registrada com sucesso", restricaoService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_ACESSO_RESTRICOES_GERIR')")
    public ResponseEntity<ApiResponse<RestricaoResponse>> update(@PathVariable UUID id,
                                                                  @Valid @RequestBody RestricaoRequest request) {
        return ResponseEntity.ok(ApiResponse.of(200, "Restrição atualizada com sucesso",
                restricaoService.update(id, request)));
    }

    @PostMapping("/{id}/encerrar")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_ACESSO_RESTRICOES_GERIR')")
    public ResponseEntity<ApiResponse<RestricaoResponse>> encerrar(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(200, "Restrição encerrada com sucesso",
                restricaoService.encerrar(id)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_ACESSO_RESTRICOES_GERIR')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        restricaoService.delete(id);
        return ResponseEntity.ok(ApiResponse.of(200, "Restrição removida com sucesso", null));
    }
}
