package br.com.alfaschool.backend.application.access.controlid;

import br.com.alfaschool.backend.application.access.controlid.dto.AcionamentoRequest;
import br.com.alfaschool.backend.application.access.controlid.dto.CredenciaisEquipamentoRequest;
import br.com.alfaschool.backend.application.access.controlid.dto.EquipamentoDto;
import br.com.alfaschool.backend.domain.dispositivo.Dispositivo;
import br.com.alfaschool.backend.infrastructure.crypto.SegredoCifrador;
import br.com.alfaschool.backend.infrastructure.persistence.repository.DispositivoRepository;
import br.com.alfaschool.backend.security.filter.TenantContext;
import br.com.alfaschool.backend.shared.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Administracao dos equipamentos de leitura: credenciais, token de
 * webhook e acionamento manual da porta/catraca.
 */
@RestController
@RequestMapping("/api/v1/access/equipamentos")
public class EquipamentoAccessController {

    private final DispositivoRepository dispositivos;
    private final WebhookTokenService webhookTokens;
    private final SegredoCifrador cifrador;
    private final ControlIdClient client;

    public EquipamentoAccessController(DispositivoRepository dispositivos,
                                       WebhookTokenService webhookTokens,
                                       SegredoCifrador cifrador,
                                       ControlIdClient client) {
        this.dispositivos = dispositivos;
        this.webhookTokens = webhookTokens;
        this.cifrador = cifrador;
        this.client = client;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_ACESSO_EQUIPAMENTOS_GERIR')")
    public ResponseEntity<ApiResponse<List<EquipamentoDto>>> listar(Pageable pageable) {
        UUID tenantId = tenant();
        Page<Dispositivo> page = dispositivos.findByTenantIdAndDeletedFalse(tenantId, pageable);
        return ResponseEntity.ok(ApiResponse.of(200, "Equipamentos listados.",
                page.getContent().stream().map(EquipamentoDto::from).toList()));
    }

    /**
     * Gera ou rotaciona o token de webhook.
     *
     * O token em claro aparece UMA UNICA VEZ, nesta resposta. Nao ha
     * endpoint de consulta: perdeu, rotaciona de novo e reconfigura o
     * leitor. Rotacionar invalida o anterior imediatamente — durante a
     * troca o equipamento vai tomar 401, e isso e' o comportamento certo.
     */
    @PostMapping("/{id}/webhook-token")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_ACESSO_EQUIPAMENTOS_GERIR')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> rotacionarToken(@PathVariable UUID id) {
        UUID tenantId = tenant();
        String token = webhookTokens.rotacionar(tenantId, id);
        return ResponseEntity.ok(ApiResponse.of(200,
                "Token gerado. Copie agora: ele não será exibido novamente.",
                Map.of(
                        "dispositivoId", id,
                        "token", token,
                        "header", "X-Webhook-Token",
                        "url", "/api/v1/access/webhook/" + id)));
    }

    /** Grava login e senha do leitor. A senha e' cifrada antes de tocar o banco. */
    @PutMapping("/{id}/credenciais")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_ACESSO_EQUIPAMENTOS_GERIR')")
    public ResponseEntity<ApiResponse<EquipamentoDto>> definirCredenciais(
            @PathVariable UUID id,
            @Valid @RequestBody CredenciaisEquipamentoRequest req) {
        Dispositivo d = carregar(id);
        d.setLogin(req.login());
        d.setSenhaCifrada(cifrador.cifrar(req.senha()));
        dispositivos.save(d);
        return ResponseEntity.ok(ApiResponse.of(200, "Credenciais atualizadas.",
                EquipamentoDto.from(d)));
    }

    /**
     * Acionamento manual (abrir porta, liberar catraca).
     *
     * O comando passa pela whitelist de AcionamentoAcesso antes de virar
     * payload. Tipo desconhecido ou parametro fora do padrao viram 400
     * AQUI, nunca chegam ao equipamento.
     */
    /**
     * Testa se o leitor responde no IP e na porta cadastrados.
     *
     * A tela tinha o botao "Testar conexao" chamando um caminho que nao
     * existia. Este endpoint fala com o equipamento de verdade — e' isso
     * que distingue "cadastrei certo" de "o leitor esta' na rede".
     */
    @PostMapping("/{id}/testar-conexao")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_ACESSO_EQUIPAMENTOS_GERIR')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testarConexao(@PathVariable UUID id) {
        Dispositivo d = carregar(id);
        try {
            // login() e nao sessao(): a sessao devolve a credencial em
            // cache e nao encostaria no equipamento — o botao diria "OK"
            // sobre um leitor desligado. O login forca a ida ate' ele.
            client.login(d);
        } catch (ControlIdException e) {
            // 502 e nao 500: o problema esta' no equipamento ou na rede da
            // escola, nao no servidor. A mensagem vai crua porque ela e'
            // exatamente o que a operacao precisa ler ("Connection refused",
            // "timeout", "senha invalida").
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, e.getMessage());
        }
        return ResponseEntity.ok(ApiResponse.of(200, "Equipamento respondeu",
                Map.of("dispositivoId", d.getId(), "ip", String.valueOf(d.getIp()),
                        "porta", d.getPorta() == null ? 0 : d.getPorta())));
    }

    @PostMapping("/{id}/acionar")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS') "
            + "and hasAuthority('PERM_ACESSO_EQUIPAMENTOS_GERIR')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> acionar(
            @PathVariable UUID id,
            @Valid @RequestBody AcionamentoRequest req) {
        Dispositivo d = carregar(id);
        AcionamentoAcesso acionamento;
        try {
            acionamento = AcionamentoAcesso.de(req.tipo(), req.parametros());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
        try {
            client.acionar(d, acionamento);
        } catch (ControlIdException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, e.getMessage());
        }
        return ResponseEntity.ok(ApiResponse.of(200, "Acionamento enviado.",
                Map.of("dispositivoId", id, "acao", acionamento.action(),
                        "parametros", acionamento.parametros())));
    }

    private Dispositivo carregar(UUID id) {
        UUID tenantId = tenant();
        return dispositivos.findById(id)
                .filter(d -> tenantId.equals(d.getTenantId()))
                .filter(d -> Boolean.FALSE.equals(d.getDeleted()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Equipamento não encontrado."));
    }

    private UUID tenant() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tenant não identificado.");
        }
        return tenantId;
    }
}
