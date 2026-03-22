package br.com.alfaschool.backend.domain.diario;

import br.com.alfaschool.backend.domain.shared.BaseEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "historico_notas")
public class HistoricoNota extends BaseEntity {

    @Column(name = "nota_id", nullable = false)
    private UUID notaId;

    @Column(name = "nota_anterior", precision = 5, scale = 2)
    private BigDecimal notaAnterior;

    @Column(name = "nota_nova", precision = 5, scale = 2)
    private BigDecimal notaNova;

    @Column(name = "tipo_alteracao", nullable = false, length = 30)
    private String tipoAlteracao;

    @Column(length = 255)
    private String motivo;

    @Column(name = "alterado_por", nullable = false)
    private UUID alteradoPor;

    @Column(name = "alterado_em", nullable = false)
    private Instant alteradoEm;

    // Getters e Setters
    public UUID getNotaId() { return notaId; }
    public void setNotaId(UUID v) { this.notaId = v; }

    public BigDecimal getNotaAnterior() { return notaAnterior; }
    public void setNotaAnterior(BigDecimal v) { this.notaAnterior = v; }

    public BigDecimal getNotaNova() { return notaNova; }
    public void setNotaNova(BigDecimal v) { this.notaNova = v; }

    public String getTipoAlteracao() { return tipoAlteracao; }
    public void setTipoAlteracao(String v) { this.tipoAlteracao = v; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String v) { this.motivo = v; }

    public UUID getAlteradoPor() { return alteradoPor; }
    public void setAlteradoPor(UUID v) { this.alteradoPor = v; }

    public Instant getAlteradoEm() { return alteradoEm; }
    public void setAlteradoEm(Instant v) { this.alteradoEm = v; }
}
