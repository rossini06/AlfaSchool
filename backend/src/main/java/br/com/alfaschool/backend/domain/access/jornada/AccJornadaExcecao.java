package br.com.alfaschool.backend.domain.access.jornada;

import br.com.alfaschool.backend.domain.shared.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Excecao pontual de um aluno num dia: "hoje a Ana sai as 13h porque tem
 * consulta". Sobrescreve o dia da jornada vigente, e so' aquele dia.
 *
 * carga_minutos e' anulavel de proposito: a secretaria normalmente informa
 * so' o novo horario. Quando vem nula, o motor deriva a carga de
 * entrada/saida prevista da propria excecao.
 */
@Entity
@Table(name = "acc_jornada_excecoes")
public class AccJornadaExcecao extends BaseEntity {

    @Column(name = "aluno_id", nullable = false)
    private UUID alunoId;

    @Column(nullable = false)
    private LocalDate data;

    @Column(nullable = false)
    private boolean frequenta = true;

    @Column(name = "entrada_prevista")
    private LocalTime entradaPrevista;

    @Column(name = "saida_prevista")
    private LocalTime saidaPrevista;

    @Column(name = "carga_minutos")
    private Integer cargaMinutos;

    @Column(length = 255)
    private String motivo;

    public UUID getAlunoId() { return alunoId; }
    public void setAlunoId(UUID v) { this.alunoId = v; }
    public LocalDate getData() { return data; }
    public void setData(LocalDate v) { this.data = v; }
    public boolean isFrequenta() { return frequenta; }
    public void setFrequenta(boolean v) { this.frequenta = v; }
    public LocalTime getEntradaPrevista() { return entradaPrevista; }
    public void setEntradaPrevista(LocalTime v) { this.entradaPrevista = v; }
    public LocalTime getSaidaPrevista() { return saidaPrevista; }
    public void setSaidaPrevista(LocalTime v) { this.saidaPrevista = v; }
    public Integer getCargaMinutos() { return cargaMinutos; }
    public void setCargaMinutos(Integer v) { this.cargaMinutos = v; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String v) { this.motivo = v; }
}
