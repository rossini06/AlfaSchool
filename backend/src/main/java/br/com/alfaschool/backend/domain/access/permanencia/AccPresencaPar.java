package br.com.alfaschool.backend.domain.access.permanencia;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Um intervalo dentro do dia: entrada -&gt; saida.
 *
 * NAO estende BaseEntity de proposito: acc_presenca_pares (V39) nao tem
 * created_by/updated_by/deleted. O par e' dado derivado — quem some some
 * de verdade no recalculo, e a autoria do que foi mexido a mao vive em
 * ajustado_por/motivo_ajuste.
 *
 * saida_em nulo = intervalo em aberto (aluno dentro da escola agora).
 * Par aberto NAO soma minutos: a permanencia so' encerra em saida efetiva.
 */
@Entity
@Table(name = "acc_presenca_pares")
@EntityListeners(AuditingEntityListener.class)
public class AccPresencaPar {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "presenca_id", nullable = false)
    private UUID presencaId;

    @Column(name = "entrada_em", nullable = false)
    private Instant entradaEm;

    @Column(name = "saida_em")
    private Instant saidaEm;

    /** Nulo enquanto o par estiver aberto. */
    @Column
    private Integer minutos;

    @Column(name = "entrada_evento_id")
    private UUID entradaEventoId;

    @Column(name = "saida_evento_id")
    private UUID saidaEventoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrigemPar origem = OrigemPar.EVENTO;

    @Column(name = "ajustado_por")
    private UUID ajustadoPor;

    @Column(name = "motivo_ajuste", length = 255)
    private String motivoAjuste;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID v) { this.tenantId = v; }
    public UUID getPresencaId() { return presencaId; }
    public void setPresencaId(UUID v) { this.presencaId = v; }
    public Instant getEntradaEm() { return entradaEm; }
    public void setEntradaEm(Instant v) { this.entradaEm = v; }
    public Instant getSaidaEm() { return saidaEm; }
    public void setSaidaEm(Instant v) { this.saidaEm = v; }
    public Integer getMinutos() { return minutos; }
    public void setMinutos(Integer v) { this.minutos = v; }
    public UUID getEntradaEventoId() { return entradaEventoId; }
    public void setEntradaEventoId(UUID v) { this.entradaEventoId = v; }
    public UUID getSaidaEventoId() { return saidaEventoId; }
    public void setSaidaEventoId(UUID v) { this.saidaEventoId = v; }
    public OrigemPar getOrigem() { return origem; }
    public void setOrigem(OrigemPar v) { this.origem = v; }
    public UUID getAjustadoPor() { return ajustadoPor; }
    public void setAjustadoPor(UUID v) { this.ajustadoPor = v; }
    public String getMotivoAjuste() { return motivoAjuste; }
    public void setMotivoAjuste(String v) { this.motivoAjuste = v; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    /** Fechado = tem saida efetiva. So' par fechado soma minutos. */
    public boolean fechado() {
        return saidaEm != null;
    }
}
