package br.com.alfaschool.backend.domain.access.biometria;

import br.com.alfaschool.backend.domain.access.shared.StatusFaceSync;
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

import java.time.Instant;
import java.util.UUID;

/**
 * Veredito da foto POR equipamento.
 *
 * Sem esta tabela a tela mente: marcar "sincronizado" na face e' facil,
 * mas quem aceita ou recusa a foto e' o leitor, um por um. Um aluno pode
 * estar ACEITA na catraca da entrada e RECUSADA no leitor do patio por
 * iluminacao diferente.
 *
 * foto_hash evita reenviar a mesma imagem a cada ciclo de sincronizacao —
 * num full sync de 2000 alunos isso e' a diferenca entre minutos e horas.
 */
@Entity
@Table(name = "acc_face_sync")
public class AccFaceSync {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "face_id", nullable = false)
    private UUID faceId;

    @Column(name = "dispositivo_id", nullable = false)
    private UUID dispositivoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusFaceSync status = StatusFaceSync.PENDENTE;

    @Column(name = "codigo_erro", length = 40)
    private String codigoErro;

    @Column(length = 255)
    private String detalhe;

    @Column(nullable = false)
    private int tentativas = 0;

    @Column(name = "foto_hash", length = 64)
    private String fotoHash;

    @Column(name = "sincronizado_em")
    private Instant sincronizadoEm;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void aoCriar() {
        Instant agora = Instant.now();
        this.createdAt = agora;
        this.updatedAt = agora;
    }

    @PreUpdate
    void aoAtualizar() {
        this.updatedAt = Instant.now();
    }

    public void aceitar(String hash) {
        this.status = StatusFaceSync.ACEITA;
        this.codigoErro = null;
        this.detalhe = null;
        this.fotoHash = hash;
        this.sincronizadoEm = Instant.now();
    }

    public void recusar(String codigo, String mensagem) {
        this.status = StatusFaceSync.RECUSADA;
        this.codigoErro = codigo;
        this.detalhe = mensagem == null ? null
                : mensagem.substring(0, Math.min(mensagem.length(), 255));
        this.tentativas = this.tentativas + 1;
    }

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID v) { this.tenantId = v; }
    public UUID getFaceId() { return faceId; }
    public void setFaceId(UUID v) { this.faceId = v; }
    public UUID getDispositivoId() { return dispositivoId; }
    public void setDispositivoId(UUID v) { this.dispositivoId = v; }
    public StatusFaceSync getStatus() { return status; }
    public void setStatus(StatusFaceSync v) { this.status = v; }
    public String getCodigoErro() { return codigoErro; }
    public void setCodigoErro(String v) { this.codigoErro = v; }
    public String getDetalhe() { return detalhe; }
    public void setDetalhe(String v) { this.detalhe = v; }
    public int getTentativas() { return tentativas; }
    public void setTentativas(int v) { this.tentativas = v; }
    public String getFotoHash() { return fotoHash; }
    public void setFotoHash(String v) { this.fotoHash = v; }
    public Instant getSincronizadoEm() { return sincronizadoEm; }
    public void setSincronizadoEm(Instant v) { this.sincronizadoEm = v; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
