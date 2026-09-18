package br.com.alfaschool.backend.application.access.painel;

import br.com.alfaschool.backend.application.access.painel.dto.PainelEstadoResponse;
import br.com.alfaschool.backend.application.access.retirada.ContextoAcesso;
import br.com.alfaschool.backend.application.access.shared.SseHub;
import br.com.alfaschool.backend.shared.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import br.com.alfaschool.backend.application.access.retirada.RetiradaService;
import br.com.alfaschool.backend.application.access.retirada.dto.RetiradaFilaItem;
import br.com.alfaschool.backend.security.filter.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

/**
 * As duas rotas que a TV consome.
 *
 * ELAS NAO PASSAM PELO JWT: quem autentica e' o token do dispositivo,
 * validado aqui dentro. Precisam ser liberadas no SecurityConfig
 * (permitAll), que NAO foi alterado por esta fatia:
 *
 *   GET /api/v1/access/paineis/{slug}/stream
 *   GET /api/v1/access/paineis/{slug}/estado
 *   GET /api/v1/access/paineis/coordenacao/stream
 *
 * Liberar no filtro de seguranca nao as torna publicas: sem
 * X-Painel-Token valido, todas devolvem 401/403 aqui.
 */
@RestController
@RequestMapping("/api/v1/access/paineis")
public class PainelStreamController {

    private static final Logger log = LoggerFactory.getLogger(PainelStreamController.class);

    private final PainelAcessoService acessoService;
    private final SseHub sseHub;
    private final RetiradaService retiradaService;

    public PainelStreamController(PainelAcessoService acessoService, SseHub sseHub,
                                  RetiradaService retiradaService) {
        this.acessoService = acessoService;
        this.sseHub = sseHub;
        this.retiradaService = retiradaService;
    }

    /**
     * Fluxo ao vivo. O EventSource do navegador nao manda header, por isso
     * o token tambem e' aceito na query string — mas isso vaza o segredo
     * para log de proxy e historico, entao fica registrado em WARN para que
     * a operacao saiba que aquela TV precisa migrar para o header.
     */
    @GetMapping(value = "/{slug}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@PathVariable String slug,
                             @RequestHeader(value = "X-Painel-Token", required = false) String tokenHeader,
                             @RequestParam(value = "token", required = false) String tokenQuery,
                             HttpServletRequest http) {
        String token = escolherToken(slug, tokenHeader, tokenQuery);
        PainelAcessoService.PainelAutenticado autenticado = acessoService.autenticar(
                slug, token, ContextoAcesso.ipDaRequisicao(http), http.getHeader("User-Agent"));

        // Canal isolado por (tenant, painel): nem outra escola nem outra
        // sala compartilham topico.
        return sseHub.inscrever(autenticado.tenantId(), autenticado.topico());
    }

    /**
     * Fluxo da tela de coordenacao, que nao passa slug na URL.
     *
     * O painel vem do proprio token — e' o unico dado que prova a que tela
     * aquele dispositivo tem direito. Por isso a validacao de slug e'
     * dispensada aqui, e nao enfraquecida: nada e' escolhido pelo cliente.
     */
    @GetMapping(value = "/coordenacao/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamDaCoordenacao(
            @RequestHeader(value = "X-Painel-Token", required = false) String tokenHeader,
            @RequestParam(value = "token", required = false) String tokenQuery,
            HttpServletRequest http) {
        String token = escolherToken("coordenacao", tokenHeader, tokenQuery);
        PainelAcessoService.PainelAutenticado autenticado = acessoService.autenticar(
                null, token, ContextoAcesso.ipDaRequisicao(http), http.getHeader("User-Agent"));
        return sseHub.inscrever(autenticado.tenantId(), autenticado.topico());
    }

    /**
     * Estado completo para a TV montar a tela ao conectar e ao reconectar.
     * Mesma autenticacao do stream.
     */
    @GetMapping("/{slug}/estado")
    public ResponseEntity<ApiResponse<PainelEstadoResponse>> estado(
            @PathVariable String slug,
            @RequestHeader(value = "X-Painel-Token", required = false) String tokenHeader,
            @RequestParam(value = "token", required = false) String tokenQuery,
            HttpServletRequest http) {
        String token = escolherToken(slug, tokenHeader, tokenQuery);
        PainelAcessoService.PainelAutenticado autenticado = acessoService.autenticar(
                slug, token, ContextoAcesso.ipDaRequisicao(http), http.getHeader("User-Agent"));
        return ResponseEntity.ok(ApiResponse.of(200, "Estado do painel carregado com sucesso",
                acessoService.estado(autenticado)));
    }

    /**
     * O botao "Preparar aluno para saida" da TV da sala.
     *
     * A Smart TV nao faz login: fica ligada o dia inteiro, nao tem teclado e
     * e' compartilhada pela turma. Por isso esta rota autentica pelo token do
     * dispositivo, e nao pelo JWT — mas so' aceita retirada que esteja DENTRO
     * DO RECORTE daquele painel. A TV da sala 101 nao prepara aluno da 102.
     *
     * Preparar nao entrega crianca nenhuma. O ato de responsabilidade e'
     * ENTREGAR, que continua exigindo colaborador autenticado em
     * POST /api/v1/access/retiradas/{id}/entregar.
     */
    @PostMapping("/{slug}/retiradas/{retiradaId}/preparar")
    public ResponseEntity<ApiResponse<Void>> prepararPeloPainel(
            @PathVariable String slug,
            @PathVariable UUID retiradaId,
            @RequestHeader(value = "X-Painel-Token", required = false) String tokenHeader,
            @RequestParam(value = "token", required = false) String tokenQuery,
            HttpServletRequest http) {
        String token = escolherToken(slug, tokenHeader, tokenQuery);
        String ip = ContextoAcesso.ipDaRequisicao(http);
        PainelAcessoService.PainelAutenticado autenticado = acessoService.autenticar(
                slug, token, ip, http.getHeader("User-Agent"));

        boolean noRecorte = acessoService.itensDoRecorte(autenticado.painel()).stream()
                .map(RetiradaFilaItem::id)
                .anyMatch(retiradaId::equals);
        if (!noRecorte) {
            // Fail-closed: fora do recorte, a TV nem confirma que a retirada
            // existe. 404 em vez de 403 para nao virar sonda de existencia.
            log.warn("Painel {} tentou preparar a retirada {}, que esta fora do seu recorte.",
                    slug, retiradaId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Retirada nao encontrada neste painel");
        }

        // As rotas do painel nao passam pelo JWT, entao o TenantContext ainda
        // nao foi populado; quem define o tenant aqui e' o proprio token.
        UUID tenantAnterior = TenantContext.getTenantId();
        try {
            TenantContext.setTenantId(autenticado.tenantId());
            String identificacao = "Painel " + autenticado.painel().getNome()
                    + " / TV " + autenticado.dispositivo().getNome();
            retiradaService.prepararPeloPainel(retiradaId, identificacao, ip);
        } finally {
            if (tenantAnterior == null) {
                TenantContext.clear();
            } else {
                TenantContext.setTenantId(tenantAnterior);
            }
        }
        return ResponseEntity.ok(ApiResponse.of(200, "Aluno em preparo", null));
    }

    private String escolherToken(String slug, String tokenHeader, String tokenQuery) {
        if (tokenHeader != null && !tokenHeader.isBlank()) {
            return tokenHeader;
        }
        if (tokenQuery != null && !tokenQuery.isBlank()) {
            log.warn("Painel {} autenticou por token na query string. O segredo fica em log de proxy "
                    + "e no historico do navegador: prefira o header X-Painel-Token.", slug);
            return tokenQuery;
        }
        return null;
    }
}
