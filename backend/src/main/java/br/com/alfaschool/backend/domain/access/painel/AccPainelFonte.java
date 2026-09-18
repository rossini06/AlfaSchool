package br.com.alfaschool.backend.domain.access.painel;

import br.com.alfaschool.backend.domain.access.shared.EscopoPainel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * O recorte que alimenta um painel: esta turma, esta sala, esta portaria ou
 * a unidade inteira.
 *
 * E' o que impede a tela da sala A de mostrar o aluno da sala B. Painel sem
 * fonte nao recebe nada — falha fechada de proposito.
 *
 * Nao estende BaseEntity: a tabela acc_painel_fontes so' tem created_at e
 * updated_at, sem created_by/deleted.
 */
@Entity
@Table(name = "acc_painel_fontes")
public class AccPainelFonte {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "painel_id", nullable = false)
    private UUID painelId;

    @Enumerated(EnumType.STRING)
    @Column(name = "escopo", nullable = false, length = 20)
    private EscopoPainel escopo;

    /** Id da turma/sala/portaria/unidade. Nulo so' faz sentido em UNIDADE. */
    @Column(name = "referencia_id")
    private UUID referenciaId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public UUID getId() { return id; }
    public void setId(UUID v) { this.id = v; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID v) { this.tenantId = v; }
    public UUID getPainelId() { return painelId; }
    public void setPainelId(UUID v) { this.painelId = v; }
    public EscopoPainel getEscopo() { return escopo; }
    public void setEscopo(EscopoPainel v) { this.escopo = v; }
    public UUID getReferenciaId() { return referenciaId; }
    public void setReferenciaId(UUID v) { this.referenciaId = v; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant v) { this.createdAt = v; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant v) { this.updatedAt = v; }
}
