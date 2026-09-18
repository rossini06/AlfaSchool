package br.com.alfaschool.backend.domain.dispositivo;

import br.com.alfaschool.backend.domain.access.shared.FuncaoDispositivo;
import br.com.alfaschool.backend.domain.access.shared.ModoSync;
import br.com.alfaschool.backend.domain.access.shared.SentidoAcesso;
import br.com.alfaschool.backend.domain.shared.BaseEntity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "dispositivos")
public class Dispositivo extends BaseEntity {

    @Column(name = "unit_id")
    private UUID unitId;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false, length = 40)
    private String tipo = "catraca";

    @Column(length = 80)
    private String fabricante;

    @Column(length = 80)
    private String modelo;

    @Column(length = 45)
    private String ip;

    @Column
    private Integer porta = 80;

    @Column(length = 80)
    private String serial;

    @Column(name = "api_token", length = 255)
    private String apiToken;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(nullable = false)
    private boolean online = false;

    @Column(name = "ultimo_ping")
    private Instant ultimoPing;

    // ---------------------------------------------------------------
    // V37 — campos do modulo ACCESS (equipamentos de leitura).
    // Os getters/setters acima permanecem intactos: o cadastro generico
    // de dispositivos continua funcionando sem conhecer nada disto.
    // ---------------------------------------------------------------

    @Column(name = "portaria_id")
    private UUID portariaId;

    /** ALUNO: catraca de aluno. RESPONSAVEL: leitor dos pais, que abre a fila de retirada. */
    @Enumerated(EnumType.STRING)
    @Column(name = "funcao", nullable = false, length = 20)
    private FuncaoDispositivo funcao = FuncaoDispositivo.ALUNO;

    @Enumerated(EnumType.STRING)
    @Column(name = "sentido", nullable = false, length = 20)
    private SentidoAcesso sentido = SentidoAcesso.ENTRADA;

    @Enumerated(EnumType.STRING)
    @Column(name = "modo_sync", nullable = false, length = 20)
    private ModoSync modoSync = ModoSync.AGENTE;

    @Column(name = "login", length = 60)
    private String login;

    /**
     * Senha do equipamento cifrada em AES-GCM (SegredoCifrador). Nunca
     * texto plano: nos sistemas anteriores do grupo ela ficava legivel no
     * banco e era devolvida em claro para qualquer cliente.
     */
    @Column(name = "senha_cifrada", length = 512)
    private byte[] senhaCifrada;

    /** Grupo de acesso dentro do firmware Control iD (inteiro, nao UUID). */
    @Column(name = "grupo_acesso_id", nullable = false)
    private Integer grupoAcessoId = 1;

    /**
     * SHA-256 (hex) do token de webhook DESTE dispositivo. O token em
     * claro nunca e' persistido nem relido. No AlfaGym havia um unico
     * segredo global: quem o tivesse postava evento para qualquer tenant.
     */
    @Column(name = "webhook_token_hash", length = 64)
    private String webhookTokenHash;

    @Column(name = "ultimo_heartbeat")
    private Instant ultimoHeartbeat;

    @Column(name = "agent_version", length = 20)
    private String agentVersion;

    /** Ate onde ja lemos o historico do leitor, para o polling incremental. */
    @Column(name = "ultima_leitura_log")
    private Instant ultimaLeituraLog;

    @Column(name = "sincroniza_auto", nullable = false)
    private boolean sincronizaAuto = true;

    public UUID getUnitId() { return unitId; }
    public void setUnitId(UUID v) { this.unitId = v; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getFabricante() { return fabricante; }
    public void setFabricante(String v) { this.fabricante = v; }
    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }
    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
    public Integer getPorta() { return porta; }
    public void setPorta(Integer porta) { this.porta = porta; }
    public String getSerial() { return serial; }
    public void setSerial(String serial) { this.serial = serial; }
    public String getApiToken() { return apiToken; }
    public void setApiToken(String apiToken) { this.apiToken = apiToken; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
    public boolean isOnline() { return online; }
    public void setOnline(boolean online) { this.online = online; }
    public Instant getUltimoPing() { return ultimoPing; }
    public void setUltimoPing(Instant v) { this.ultimoPing = v; }

    public UUID getPortariaId() { return portariaId; }
    public void setPortariaId(UUID v) { this.portariaId = v; }
    public FuncaoDispositivo getFuncao() { return funcao; }
    public void setFuncao(FuncaoDispositivo v) { this.funcao = v; }
    public SentidoAcesso getSentido() { return sentido; }
    public void setSentido(SentidoAcesso v) { this.sentido = v; }
    public ModoSync getModoSync() { return modoSync; }
    public void setModoSync(ModoSync v) { this.modoSync = v; }
    public String getLogin() { return login; }
    public void setLogin(String v) { this.login = v; }
    public byte[] getSenhaCifrada() { return senhaCifrada; }
    public void setSenhaCifrada(byte[] v) { this.senhaCifrada = v; }
    public Integer getGrupoAcessoId() { return grupoAcessoId; }
    public void setGrupoAcessoId(Integer v) { this.grupoAcessoId = v; }
    public String getWebhookTokenHash() { return webhookTokenHash; }
    public void setWebhookTokenHash(String v) { this.webhookTokenHash = v; }
    public Instant getUltimoHeartbeat() { return ultimoHeartbeat; }
    public void setUltimoHeartbeat(Instant v) { this.ultimoHeartbeat = v; }
    public String getAgentVersion() { return agentVersion; }
    public void setAgentVersion(String v) { this.agentVersion = v; }
    public Instant getUltimaLeituraLog() { return ultimaLeituraLog; }
    public void setUltimaLeituraLog(Instant v) { this.ultimaLeituraLog = v; }
    public boolean isSincronizaAuto() { return sincronizaAuto; }
    public void setSincronizaAuto(boolean v) { this.sincronizaAuto = v; }

    /** URL base do firmware. Sem isto cada chamador montava http://ip:porta na mao. */
    public String baseUrl() {
        int p = porta == null ? 80 : porta;
        return "http://" + ip + ":" + p;
    }
}
