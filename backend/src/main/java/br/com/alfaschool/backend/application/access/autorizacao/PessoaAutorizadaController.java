package br.com.alfaschool.backend.application.access.autorizacao;

import br.com.alfaschool.backend.application.access.autorizacao.dto.PessoaAutorizadaRequest;
import br.com.alfaschool.backend.application.access.autorizacao.dto.PessoaAutorizadaResponse;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/access/pessoas-autorizadas")
public class PessoaAutorizadaController {

    private final PessoaAutorizadaService pessoaAutorizadaService;

    public PessoaAutorizadaController(PessoaAutorizadaService pessoaAutorizadaService) {
        this.pessoaAutorizadaService = pessoaAutorizadaService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_ACESSO_AUTORIZACOES_VER')")
    public ResponseEntity<ApiResponse<Page<PessoaAutorizadaResponse>>> list(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = Paginacao.de(page, size, Sort.by("nome").ascending());
        return ResponseEntity.ok(ApiResponse.of(200, "Pessoas autorizadas listadas com sucesso",
                pessoaAutorizadaService.list(q, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_ACESSO_AUTORIZACOES_VER')")
    public ResponseEntity<ApiResponse<PessoaAutorizadaResponse>> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.of(200, "Pessoa autorizada encontrada",
                pessoaAutorizadaService.findById(id)));
    }

    @PostMapping
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_ACESSO_AUTORIZACOES_GERIR')")
    public ResponseEntity<ApiResponse<PessoaAutorizadaResponse>> create(
            @Valid @RequestBody PessoaAutorizadaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(201, "Pessoa autorizada criada com sucesso",
                        pessoaAutorizadaService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_ACESSO_AUTORIZACOES_GERIR')")
    public ResponseEntity<ApiResponse<PessoaAutorizadaResponse>> update(
            @PathVariable UUID id, @Valid @RequestBody PessoaAutorizadaRequest request) {
        return ResponseEntity.ok(ApiResponse.of(200, "Pessoa autorizada atualizada com sucesso",
                pessoaAutorizadaService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_ACESSO_AUTORIZACOES_GERIR')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        pessoaAutorizadaService.delete(id);
        return ResponseEntity.ok(ApiResponse.of(200, "Pessoa autorizada removida com sucesso", null));
    }
}
