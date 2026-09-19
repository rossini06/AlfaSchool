package br.com.alfaschool.backend.application.lgpd;

import br.com.alfaschool.backend.application.access.retirada.ContextoAcesso;
import br.com.alfaschool.backend.application.shared.AuditService;
import br.com.alfaschool.backend.security.filter.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import br.com.alfaschool.backend.shared.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * Atendimento aos pedidos do titular (LGPD Art. 18).
 *
 * <h2>Por que a direcao, e nao a coordenacao</h2>
 * Este documento reune, num lugar so', tudo o que a escola sabe sobre uma
 * crianca: endereco, observacoes medicas, cada passagem na portaria, cada
 * aviso enviado a familia, notas e frequencia. E' a informacao mais
 * concentrada que o sistema produz.
 *
 * Quem opera a retirada precisa saber quem pode buscar o aluno hoje — nao
 * precisa do dossie. Por isso a permissao e' ESCOLA_GERIR: o pedido formal
 * do titular chega a direcao, e e' ela que responde por ele.
 *
 * <h2>A emissao fica na trilha de auditoria</h2>
 * Se um dia alguem perguntar quem tirou o historico completo de uma
 * crianca, a resposta precisa existir. Emitir o relatorio e' um ato, e atos
 * sobre dado sensivel ficam registrados.
 */
@RestController
@RequestMapping("/api/v1/lgpd")
public class DadosDoTitularController {

    private final DadosDoTitularService service;
    private final AuditService auditoria;

    public DadosDoTitularController(DadosDoTitularService service, AuditService auditoria) {
        this.service = service;
        this.auditoria = auditoria;
    }

    @GetMapping("/aluno/{alunoId}")
    @PreAuthorize("isAuthenticated() and hasAuthority('PERM_ESCOLA_GERIR')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> dadosDoAluno(
            @PathVariable UUID alunoId, HttpServletRequest http) {
        Map<String, Object> relatorio = service.relatorioDoAluno(alunoId);
        auditoria.register(TenantContext.getTenantId(), ContextoAcesso.userIdOuNulo(),
                "LGPD_RELATORIO_TITULAR", "aluno", alunoId, ContextoAcesso.ipDaRequisicao(http));
        return ResponseEntity.ok(ApiResponse.of(200,
                "Relatório de dados do titular emitido.", relatorio));
    }
}
