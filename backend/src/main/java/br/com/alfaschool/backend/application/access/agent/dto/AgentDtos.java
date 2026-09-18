package br.com.alfaschool.backend.application.access.agent.dto;

import br.com.alfaschool.backend.domain.access.equipamento.AccAgentTask;
import br.com.alfaschool.backend.domain.access.equipamento.StatusAgentTask;
import br.com.alfaschool.backend.domain.access.equipamento.TipoAgentTask;
import br.com.alfaschool.backend.domain.access.shared.FuncaoDispositivo;
import br.com.alfaschool.backend.domain.access.shared.ModoSync;
import br.com.alfaschool.backend.domain.access.shared.SentidoAcesso;
import br.com.alfaschool.backend.domain.access.shared.TitularTipo;
import br.com.alfaschool.backend.domain.dispositivo.Dispositivo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

/**
 * Contratos do protocolo do agente local.
 *
 * Agrupados num arquivo so' porque sao records curtos de um unico
 * protocolo: espalhar dez arquivos de tres linhas esconderia o desenho
 * da conversa entre backend e agente.
 */
public final class AgentDtos {

    private AgentDtos() {
    }

    public record LoginRequest(
            @NotNull(message = "clientId é obrigatório.") UUID clientId,
            @NotBlank(message = "username é obrigatório.") String username,
            @NotBlank(message = "password é obrigatório.") String password) {
    }

    public record LoginResponse(String token, Instant expiraEm, UUID tenantId, UUID unitId) {
    }

    /**
     * Equipamento visto pelo agente.
     *
     * RISCO DOCUMENTADO: {@code senha} so' vem preenchida quando o agente
     * pede explicitamente (incluirSenha=true), porque e' o agente que
     * abre a sessao no firmware dentro da LAN — o backend nao alcanca o
     * leitor. Quando vier preenchida, o campo carrega a senha de
     * administracao do equipamento em CLARO.
     *
     * Consequencias praticas: esta rota EXIGE HTTPS, o agente nao pode
     * gravar a resposta em disco nem em log, e o proxy reverso na frente
     * do backend nao pode ter body logging ligado nela. A alternativa
     * sem esse risco e' o agente guardar a propria copia da senha na
     * instalacao — o que troca vazamento em transito por vazamento em
     * repouso num micro de portaria.
     */
    public record DeviceDto(
            UUID id,
            String nome,
            String ip,
            Integer porta,
            String login,
            String senha,
            String modelo,
            String serial,
            FuncaoDispositivo funcao,
            SentidoAcesso sentido,
            ModoSync modoSync,
            Integer grupoAcessoId,
            boolean ativo,
            boolean sincronizaAuto,
            Instant ultimaLeituraLog) {

        public static DeviceDto from(Dispositivo d, String senhaEmClaro) {
            return new DeviceDto(d.getId(), d.getNome(), d.getIp(), d.getPorta(),
                    d.getLogin(), senhaEmClaro, d.getModelo(), d.getSerial(),
                    d.getFuncao(), d.getSentido(), d.getModoSync(), d.getGrupoAcessoId(),
                    d.isAtivo(), d.isSincronizaAuto(), d.getUltimaLeituraLog());
        }
    }

    /**
     * Pessoa + face a sincronizar. Vem apenas quem passou pela checagem
     * de base legal e consentimento — o agente nao tem como saber disso e
     * nao deve precisar saber.
     */
    public record UserDto(
            UUID faceId,
            long deviceUserId,
            TitularTipo titularTipo,
            UUID titularId,
            String nomeNoEquipamento,
            String fotoBase64,
            String fotoHash,
            boolean ativo,
            Instant atualizadoEm) {
    }

    public record UsersPage(java.util.List<UserDto> itens, Instant proximoCursor, boolean temMais) {
    }

    /** Uma leitura vinda do agente. O epoch e' o do relogio LOCAL do leitor. */
    public record EventRequest(
            @NotNull(message = "dispositivoId é obrigatório.") UUID dispositivoId,
            Long deviceLogId,
            Long deviceUserId,
            Long time,
            Integer event,
            String motivo,
            String raw) {
    }

    public record EventResponse(UUID eventoId, boolean replay) {
    }

    public record HeartbeatRequest(
            @NotNull(message = "dispositivoId é obrigatório.") UUID dispositivoId,
            String agentVersion,
            Long ultimoLogIdLido) {
    }

    public record TaskDto(
            UUID id,
            UUID dispositivoId,
            TipoAgentTask tipo,
            String parametros,
            StatusAgentTask status,
            Instant expiraEm,
            Instant criadoEm) {

        public static TaskDto from(AccAgentTask t) {
            return new TaskDto(t.getId(), t.getDispositivoId(), t.getTipo(), t.getParametros(),
                    t.getStatus(), t.getExpiraEm(), t.getCreatedAt());
        }
    }

    public record TaskResultRequest(boolean sucesso, String resultado) {
    }

    /**
     * Credencial recem-criada. A senha aparece UMA UNICA VEZ, nesta
     * resposta; o banco guarda so' o hash BCrypt.
     */
    public record CredencialCriadaDto(UUID id, UUID tenantId, UUID unitId,
                                      String username, String password, String aviso) {
    }

    public record NovaCredencialRequest(
            @NotBlank(message = "username é obrigatório.") String username,
            UUID unitId,
            String descricao) {
    }
}
