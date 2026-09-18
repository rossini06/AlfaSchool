package br.com.alfaschool.backend.application.access.autorizacao;

import br.com.alfaschool.backend.application.access.autorizacao.dto.AlunoAutorizadoResponse;
import br.com.alfaschool.backend.application.access.autorizacao.dto.PessoaAutorizadaResponse;
import br.com.alfaschool.backend.application.access.shared.AutorizacaoPort;
import br.com.alfaschool.backend.shared.response.ApiResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Consultas de portaria. Todas respondem sobre um MOMENTO: sem momento,
 * usa-se agora. Nenhuma delas registra retirada — so informam.
 */
@RestController
@RequestMapping("/api/v1/access/verificacao-retirada")
public class AutorizacaoConsultaController {

    private final AutorizacaoConsultaService autorizacaoConsultaService;

    public AutorizacaoConsultaController(AutorizacaoConsultaService autorizacaoConsultaService) {
        this.autorizacaoConsultaService = autorizacaoConsultaService;
    }

    /** Verificacao pontual: esta pessoa pode retirar este aluno agora? */
    @GetMapping("/verificar")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS')")
    public ResponseEntity<ApiResponse<AutorizacaoPort.Veredito>> verificar(
            @RequestParam UUID alunoId,
            @RequestParam UUID pessoaAutorizadaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant momento) {
        Instant quando = momento != null ? momento : Instant.now();
        return ResponseEntity.ok(ApiResponse.of(200, "Verificação concluída",
                autorizacaoConsultaService.verificar(alunoId, pessoaAutorizadaId, quando)));
    }

    /** Tela do aluno: quem pode busca-lo neste momento. */
    @GetMapping("/aluno/{alunoId}/quem-pode-retirar")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS')")
    public ResponseEntity<ApiResponse<List<PessoaAutorizadaResponse>>> quemPodeRetirarAgora(
            @PathVariable UUID alunoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant momento) {
        Instant quando = momento != null ? momento : Instant.now();
        return ResponseEntity.ok(ApiResponse.of(200, "Pessoas liberadas para retirada listadas com sucesso",
                autorizacaoConsultaService.quemPodeRetirarAgora(alunoId, quando)));
    }

    /**
     * Leitor da portaria: identificada a pessoa (crachá, biometria, CPF),
     * quais alunos ela pode retirar neste momento.
     */
    @GetMapping("/pessoa/{pessoaAutorizadaId}/alunos")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS')")
    public ResponseEntity<ApiResponse<List<AlunoAutorizadoResponse>>> alunosQuePodeRetirar(
            @PathVariable UUID pessoaAutorizadaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant momento) {
        Instant quando = momento != null ? momento : Instant.now();
        return ResponseEntity.ok(ApiResponse.of(200, "Alunos liberados para esta pessoa listados com sucesso",
                autorizacaoConsultaService.alunosQuePodeRetirar(pessoaAutorizadaId, quando)));
    }
}
