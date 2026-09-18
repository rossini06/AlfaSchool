package br.com.alfaschool.backend.domain.access.permanencia;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Fechamento mensal por unidade. Fechar congela todas as presencas do
 * periodo: a partir dai o numero que foi para a fatura nao muda mais,
 * mesmo que a jornada seja editada depois.
 *
 * NAO estende BaseEntity: acc_fechamentos (V39) nao tem
 * created_by/updated_by/deleted. Fechamento nao se apaga — reabre.
 */
@Entity
@Table(name = "acc_fechamentos")
@EntityListeners(AuditingEntityListener.class)
public class AccFechamento {

    public static final String ABERTO = "ABERTO";
    public static final String FECHADO = "FECHADO";
    public static final String REABERTO = "REABERTO";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "unit_id")
    private UUID unitId;

    /** "YYYY-MM". */
    @Column(nullable = false, length = 7)
    private String competencia;

    @Column(name = "data_inicio", nullable = false)
    private LocalDate dataInicio;

    @Column(name = "data_fim", nullable = false)
    private LocalDate dataFim;

    @Column(nullable = false, length = 20)
    private String status = ABERTO;

    @Column(name = "fechado_por")
    private UUID fechadoPor;

    @Column(name = "fechado_em")
    private Instant fechadoEm;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID v) { this.tenantId = v; }
    public UUID getUnitId() { return unitId; }
    public void setUnitId(UUID v) { this.unitId = v; }
    public String getCompetencia() { return competencia; }
    public void setCompetencia(String v) { this.competencia = v; }
    public LocalDate getDataInicio() { return dataInicio; }
    public void setDataInicio(LocalDate v) { this.dataInicio = v; }
    public LocalDate getDataFim() { return dataFim; }
    public void setDataFim(LocalDate v) { this.dataFim = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public UUID getFechadoPor() { return fechadoPor; }
    public void setFechadoPor(UUID v) { this.fechadoPor = v; }
    public Instant getFechadoEm() { return fechadoEm; }
    public void setFechadoEm(Instant v) { this.fechadoEm = v; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public boolean estaFechado() {
        return FECHADO.equals(status);
    }
}
