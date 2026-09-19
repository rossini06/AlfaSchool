package br.com.alfaschool.backend.domain.access.notificacao;

import br.com.alfaschool.backend.domain.access.shared.CanalNotificacao;
import br.com.alfaschool.backend.domain.access.shared.EventoNotificacao;
import br.com.alfaschool.backend.domain.access.shared.StatusEnvio;
import br.com.alfaschool.backend.domain.access.shared.TitularTipo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

/**
 * Uma linha da fila de envio: um destinatario, um canal, uma mensagem ja
 * renderizada (V42, {@code acc_notificacao_envios}).
 *
 * <p>A fila vive em tabela, e nao em memoria, porque o processo reinicia e o
 * aviso de "seu filho entrou na escola" nao pode morrer com ele.
 *
 * <h2>Por que esta entidade NAO estende BaseEntity</h2>
 * A tabela da V42 nao tem {@code created_by}, {@code updated_by} nem
 * {@code deleted} — e' um log de fila, nao um cadastro editavel. Estender
 * BaseEntity faria o Hibernate procurar colunas inexistentes em producao. O
 * schema e' o contrato, entao os campos comuns sao declarados a mao.
 *
 * <h2>Como o retry e' representado sem colunas extras</h2>
 * A V42 nao tem {@code erro_permanente} nem {@code max_tentativas}. A
 * convencao, usada pelo worker e pela varredura, e':
 * <ul>
 *   <li><b>Erro permanente</b> = {@code status = FALHOU} e
 *       {@code agendado_para IS NULL}. A varredura so' pega linhas com
 *       {@code agendado_para <= agora}, entao uma linha sem agendamento nunca
 *       volta — que e' exatamente o efeito desejado.</li>
 *   <li><b>Erro transitorio</b> = {@code status = FALHOU} com
 *       {@code agendado_para} no futuro, segundo o backoff.</li>
 *   <li><b>Teto de tentativas</b> vem de uma constante do worker, ja que nao
 *       ha coluna. Ver relatorio.</li>
 * </ul>
 */
@Entity
@Table(name = "acc_notificacao_envios",
        uniqueConstraints = @UniqueConstraint(name = "uk_acc_notif_envios_idem",
                columnNames = {"tenant_id", "chave_idempotencia"}))
public class AccNotificacaoEnvio {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CanalNotificacao canal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private EventoNotificacao evento;

    @Enumerated(EnumType.STRING)
    @Column(name = "titular_tipo", length = 20)
    private TitularTipo titularTipo;

    @Column(name = "titular_id")
    private UUID titularId;

    @Column(name = "aluno_id")
    private UUID alunoId;

    @Column(nullable = false, length = 160)
    private String destino;

    @Column(length = 255)
    private String assunto;

    @Column(columnDefinition = "TEXT")
    private String corpo;

    /**
     * Variaveis ja validadas do aviso, em JSON. Sao os componentes que o
     * template aprovado da Meta exige separados do texto final. Nunca contem
     * foto nem biometria: passaram pela validacao do NotificacaoRenderer.
     */
    @Column(name = "payload_json", columnDefinition = "TEXT")
    private String payloadJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusEnvio status = StatusEnvio.PENDENTE;

    @Column(nullable = false)
    private Integer tentativas = 0;

    /** Ultimo erro, no formato {@code CODIGO: mensagem}. */
    @Column(length = 500)
    private String erro;

    @Column(name = "provider_message_id", length = 120)
    private String providerMessageId;

    @Column(name = "chave_idempotencia", length = 160)
    private String chaveIdempotencia;

    /** Nulo em status FALHOU significa erro permanente: nao volta para a fila. */
    @Column(name = "agendado_para")
    private Instant agendadoPara;

    @Column(name = "enviado_em")
    private Instant enviadoEm;

    /** Confirmacao de entrega do provedor (webhook), quando houver. */
    @Column(name = "entregue_em")
    private Instant entregueEm;

    /**
     * Quando a FAMILIA abriu o aviso no portal. Diferente de entregueEm,
     * que e' a confirmacao do provedor: a mensagem pode ter sido entregue
     * no WhatsApp e ninguem ter lido.
     */
    @Column(name = "lida_em")
    private Instant lidaEm;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void aoCriar() {
        Instant agora = Instant.now();
        if (createdAt == null) {
            createdAt = agora;
        }
        updatedAt = agora;
    }

    @PreUpdate
    void aoAtualizar() {
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public CanalNotificacao getCanal() { return canal; }
    public void setCanal(CanalNotificacao canal) { this.canal = canal; }
    public EventoNotificacao getEvento() { return evento; }
    public void setEvento(EventoNotificacao evento) { this.evento = evento; }
    public TitularTipo getTitularTipo() { return titularTipo; }
    public void setTitularTipo(TitularTipo titularTipo) { this.titularTipo = titularTipo; }
    public UUID getTitularId() { return titularId; }
    public void setTitularId(UUID titularId) { this.titularId = titularId; }
    public UUID getAlunoId() { return alunoId; }
    public void setAlunoId(UUID alunoId) { this.alunoId = alunoId; }
    public String getDestino() { return destino; }
    public void setDestino(String destino) { this.destino = destino; }
    public String getAssunto() { return assunto; }
    public void setAssunto(String assunto) { this.assunto = assunto; }
    public String getCorpo() { return corpo; }
    public void setCorpo(String corpo) { this.corpo = corpo; }
    public String getPayloadJson() { return payloadJson; }
    public void setPayloadJson(String payloadJson) { this.payloadJson = payloadJson; }
    public StatusEnvio getStatus() { return status; }
    public void setStatus(StatusEnvio status) { this.status = status; }
    public Integer getTentativas() { return tentativas; }
    public void setTentativas(Integer tentativas) { this.tentativas = tentativas; }
    public String getErro() { return erro; }
    public void setErro(String erro) { this.erro = erro; }
    public String getProviderMessageId() { return providerMessageId; }
    public void setProviderMessageId(String providerMessageId) { this.providerMessageId = providerMessageId; }
    public String getChaveIdempotencia() { return chaveIdempotencia; }
    public void setChaveIdempotencia(String chaveIdempotencia) { this.chaveIdempotencia = chaveIdempotencia; }
    public Instant getAgendadoPara() { return agendadoPara; }
    public void setAgendadoPara(Instant agendadoPara) { this.agendadoPara = agendadoPara; }
    public Instant getEnviadoEm() { return enviadoEm; }
    public void setEnviadoEm(Instant enviadoEm) { this.enviadoEm = enviadoEm; }
    public Instant getLidaEm() { return lidaEm; }
    public void setLidaEm(Instant lidaEm) { this.lidaEm = lidaEm; }

    public Instant getEntregueEm() { return entregueEm; }
    public void setEntregueEm(Instant entregueEm) { this.entregueEm = entregueEm; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    /**
     * Erro permanente: falhou e nao ha nova tentativa agendada. Endereco
     * invalido, opt-out no provedor ou template nao aprovado nao melhoram com
     * insistencia — insistir so' queima cota e reputacao do remetente.
     */
    public boolean erroPermanente() {
        return status == StatusEnvio.FALHOU && agendadoPara == null;
    }
}