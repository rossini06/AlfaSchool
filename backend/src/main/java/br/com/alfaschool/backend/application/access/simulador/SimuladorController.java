package br.com.alfaschool.backend.application.access.simulador;

import br.com.alfaschool.backend.application.access.simulador.dto.SimuladorDtos;
import br.com.alfaschool.backend.security.filter.TenantContext;
import br.com.alfaschool.backend.shared.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.UUID;

/**
 * Cenarios de leitura para desenvolvimento e homologacao.
 *
 * Toda a classe fica atras de app.access.simulador-habilitado (false por
 * padrao) E de autenticacao com o modulo ACCESS: um endpoint que fabrica
 * entrada de aluno e' falsificacao de registro de frequencia se escapar
 * para producao. Sao duas travas porque uma so' ja falhou antes em
 * projetos parecidos — a flag foi ligada "so' para testar" num ambiente
 * que virou producao.
 */
@RestController
@RequestMapping("/api/v1/access/simulador")
@ConditionalOnProperty(name = "app.access.simulador-habilitado", havingValue = "true")
public class SimuladorController {

    private final SimuladorService service;
    private final FirmwareFakeState firmware;

    public SimuladorController(SimuladorService service, FirmwareFakeState firmware) {
        this.service = service;
        this.firmware = firmware;
    }

    @PostMapping("/entrada-aluno")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS')")
    public ResponseEntity<ApiResponse<SimuladorDtos.EventoGeradoDto>> entradaAluno(
            @Valid @RequestBody SimuladorDtos.EntradaAlunoRequest req) {
        return ResponseEntity.ok(ApiResponse.of(200, "Entrada simulada.",
                service.entradaAluno(tenant(), req.alunoId(), req.dispositivoId(), req.dataHora())));
    }

    @PostMapping("/chegada-responsavel")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS')")
    public ResponseEntity<ApiResponse<SimuladorDtos.EventoGeradoDto>> chegadaResponsavel(
            @Valid @RequestBody SimuladorDtos.ChegadaResponsavelRequest req) {
        return ResponseEntity.ok(ApiResponse.of(200, "Chegada de responsável simulada.",
                service.chegadaResponsavel(tenant(), req.pessoaAutorizadaId(),
                        req.dispositivoId(), req.dataHora())));
    }

    @PostMapping("/saida-aluno")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS')")
    public ResponseEntity<ApiResponse<SimuladorDtos.EventoGeradoDto>> saidaAluno(
            @Valid @RequestBody SimuladorDtos.SaidaAlunoRequest req) {
        return ResponseEntity.ok(ApiResponse.of(200, "Saída simulada.",
                service.saidaAluno(tenant(), req.alunoId(), req.dispositivoId(), req.dataHora())));
    }

    @PostMapping("/acesso-negado")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS')")
    public ResponseEntity<ApiResponse<SimuladorDtos.EventoGeradoDto>> acessoNegado(
            @Valid @RequestBody SimuladorDtos.AcessoNegadoRequest req) {
        return ResponseEntity.ok(ApiResponse.of(200, "Acesso negado simulado.",
                service.acessoNegado(tenant(), req.deviceUserId(), req.dispositivoId(), req.motivo())));
    }

    @PostMapping("/pessoa-desconhecida")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS')")
    public ResponseEntity<ApiResponse<SimuladorDtos.EventoGeradoDto>> pessoaDesconhecida(
            @Valid @RequestBody SimuladorDtos.PessoaDesconhecidaRequest req) {
        return ResponseEntity.ok(ApiResponse.of(200, "Pessoa desconhecida simulada.",
                service.pessoaDesconhecida(tenant(), req.dispositivoId())));
    }

    /** Prova da deduplicacao: reenvia o mesmo device_log_id do evento informado. */
    @PostMapping("/evento-duplicado")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS')")
    public ResponseEntity<ApiResponse<SimuladorDtos.EventoGeradoDto>> eventoDuplicado(
            @Valid @RequestBody SimuladorDtos.EventoDuplicadoRequest req) {
        SimuladorDtos.EventoGeradoDto r = service.eventoDuplicado(tenant(), req.eventoId());
        return ResponseEntity.ok(ApiResponse.of(200,
                r.replay() ? "Reenvio reconhecido como replay (deduplicação funcionou)."
                        : "ATENÇÃO: o reenvio gerou um evento novo.", r));
    }

    @PostMapping("/rotina-dia")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS')")
    public ResponseEntity<ApiResponse<SimuladorDtos.RotinaDiaDto>> rotinaDia(
            @Valid @RequestBody SimuladorDtos.RotinaDiaRequest req) {
        return ResponseEntity.ok(ApiResponse.of(200, "Rotina do dia simulada.",
                service.rotinaDia(tenant(), req.unitId(), req.maximoAlunos())));
    }

    // =================================================================
    // Controle do leitor falso
    // =================================================================

    /**
     * Ajusta o veredito do /user_set_image.fcgi do leitor falso.
     *
     * codigo null volta a aceitar. E' assim que se testa "olhos fechados"
     * sem uma crianca de olhos fechados na frente da camera.
     */
    @PostMapping("/firmware/foto-erro")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> definirErroDeFoto(
            @RequestParam(required = false) Integer codigo) {
        firmware.proximoErroDeFoto = codigo;
        return ResponseEntity.ok(ApiResponse.of(200,
                codigo == null ? "Leitor falso voltará a aceitar fotos."
                        : "Leitor falso recusará fotos com o código " + codigo + ".",
                Map.of("codigo", String.valueOf(codigo))));
    }

    /** Simula firmware antigo, que nao conhece destroy_objects.fcgi. */
    @PostMapping("/firmware/antigo")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> firmwareAntigo(
            @RequestParam(defaultValue = "true") boolean ativo) {
        firmware.firmwareAntigo = ativo;
        return ResponseEntity.ok(ApiResponse.of(200, "Modo firmware antigo ajustado.",
                Map.of("firmwareAntigo", ativo)));
    }

    /** Limpa o historico do leitor falso: reinicia o contador de log em 1. */
    @PostMapping("/firmware/limpar")
    @PreAuthorize("isAuthenticated() and @moduloGuard.has('ACCESS')")
    public ResponseEntity<ApiResponse<Void>> limparFirmware() {
        firmware.limpar();
        return ResponseEntity.ok(ApiResponse.of(200,
                "Histórico do leitor falso apagado (device_log_id reinicia em 1).", null));
    }

    private UUID tenant() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tenant não identificado.");
        }
        return tenantId;
    }
}
