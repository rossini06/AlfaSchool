package br.com.alfaschool.backend.domain.access.estrutura;

import br.com.alfaschool.backend.domain.shared.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * Espaco fisico onde a turma fica. Uma sala abriga VARIAS turmas ao longo do
 * dia, entao a sala nao aponta para turma: o vinculo vive em AccTurmaSala.
 */
@Entity
@Table(name = "acc_salas")
public class AccSala extends BaseEntity {

    @Column(name = "unit_id", nullable = false)
    private UUID unitId;

    // Opcional: nem toda sala fica dentro de uma zona de acesso controlado.
    @Column(name = "zona_id")
    private UUID zonaId;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(length = 30)
    private String codigo;

    @Column(length = 60)
    private String bloco;

    @Column(length = 30)
    private String andar;

    @Column
    private Integer capacidade;

    @Column(nullable = false)
    private boolean ativo = true;

    public UUID getUnitId() { return unitId; }
    public void setUnitId(UUID unitId) { this.unitId = unitId; }
    public UUID getZonaId() { return zonaId; }
    public void setZonaId(UUID zonaId) { this.zonaId = zonaId; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getBloco() { return bloco; }
    public void setBloco(String bloco) { this.bloco = bloco; }
    public String getAndar() { return andar; }
    public void setAndar(String andar) { this.andar = andar; }
    public Integer getCapacidade() { return capacidade; }
    public void setCapacidade(Integer capacidade) { this.capacidade = capacidade; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
}
