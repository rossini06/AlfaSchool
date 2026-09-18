package br.com.alfaschool.backend.domain.access.equipamento;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Zonas que um equipamento controla. Um leitor pode liberar mais de uma
 * zona (hall e patio, por exemplo), por isso a relacao e' N:N e nao uma
 * coluna zona_id dentro de dispositivos.
 *
 * Nao estende BaseEntity: acc_dispositivo_zonas nao tem created_by,
 * updated_by nem deleted. Vinculo e' criado e removido, nao versionado.
 */
@Entity
@Table(name = "acc_dispositivo_zonas")
public class AccDispositivoZona {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "dispositivo_id", nullable = false)
    private UUID dispositivoId;

    @Column(name = "zona_id", nullable = false)
    private UUID zonaId;

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

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID v) { this.tenantId = v; }
    public UUID getDispositivoId() { return dispositivoId; }
    public void setDispositivoId(UUID v) { this.dispositivoId = v; }
    public UUID getZonaId() { return zonaId; }
    public void setZonaId(UUID v) { this.zonaId = v; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
