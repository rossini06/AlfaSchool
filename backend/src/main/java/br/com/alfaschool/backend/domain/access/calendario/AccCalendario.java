package br.com.alfaschool.backend.domain.access.calendario;

import br.com.alfaschool.backend.domain.shared.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * Calendario letivo de um ano. unitId nulo = calendario global do tenant,
 * usado como base por todas as unidades (feriados nacionais, por exemplo);
 * unitId preenchido = calendario da unidade, que sobrepoe o global.
 */
@Entity
@Table(name = "acc_calendarios")
public class AccCalendario extends BaseEntity {

    @Column(name = "unit_id")
    private UUID unitId;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(name = "ano_letivo", nullable = false)
    private Integer anoLetivo;

    @Column(nullable = false)
    private boolean ativo = true;

    public UUID getUnitId() { return unitId; }
    public void setUnitId(UUID unitId) { this.unitId = unitId; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public Integer getAnoLetivo() { return anoLetivo; }
    public void setAnoLetivo(Integer anoLetivo) { this.anoLetivo = anoLetivo; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
}
