package br.com.alfaschool.backend.application.access.relatorio;

import br.com.alfaschool.backend.security.filter.TenantContext;
import br.com.alfaschool.backend.shared.response.ApiResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Relatorios do modulo de acesso.
 *
 * <h2>Uma rota so', com a chave no caminho</h2>
 * As sete abas da tela compartilham periodo e filtros; o que muda e' a
 * consulta. Uma rota por relatorio multiplicaria o mesmo bloco de
 * validacao sete vezes.
 */
@RestController
@RequestMapping("/api/v1/access/relatorios")
public class RelatorioAccessController {

    /** Teto do periodo. Um ano de eventos de portaria sao centenas de milhares de linhas. */
    private static final int DIAS_MAXIMOS = 186;

    private final RelatorioAccessJdbc consultas;

    public RelatorioAccessController(RelatorioAccessJdbc consultas) {
        this.consultas = consultas;
    }

    @GetMapping("/{chave}")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_ACESSO_RELATORIOS_VER')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> gerar(
            @PathVariable String chave,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required = false) UUID turmaId,
            @RequestParam(required = false) UUID alunoId,
            @RequestParam(required = false) UUID portariaId) {

        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Tenant não identificado");
        }
        if (fim.isBefore(inicio)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A data final é anterior à inicial.");
        }
        if (inicio.plusDays(DIAS_MAXIMOS).isBefore(fim)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O período não pode passar de 6 meses. Consulte por partes.");
        }

        var filtros = new RelatorioAccessJdbc.Filtros(inicio, fim, turmaId, alunoId, portariaId);
        List<Map<String, Object>> linhas = switch (chave) {
            case "movimentacoes"   -> consultas.movimentacoes(tenantId, filtros);
            case "permanencia"     -> consultas.permanencia(tenantId, filtros);
            case "excedentes"      -> consultas.excedentes(tenantId, filtros);
            case "retiradas"       -> consultas.retiradas(tenantId, filtros);
            case "tempo-espera"    -> consultas.tempoEspera(tenantId, filtros);
            case "acessos-negados" -> consultas.acessosNegados(tenantId, filtros);
            case "ocorrencias"     -> consultas.ocorrencias(tenantId, filtros);
            default -> throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Relatório desconhecido: " + chave);
        };
        return ResponseEntity.ok(ApiResponse.of(200, "Relatório gerado com sucesso", linhas));
    }
}
