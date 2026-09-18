package br.com.alfaschool.backend.domain.access.jornada;

import br.com.alfaschool.backend.domain.shared.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Um dia da semana dentro da jornada. dia_semana segue o padrao ISO do
 * java.time: 1=segunda ... 7=domingo — o mesmo que
 * {@code LocalDate.getDayOfWeek().getValue()}, para nao precisar de
 * conversao no motor de apuracao.
 */
@Entity
@Table(name = "acc_jornada_dias")
public class AccJornadaDia extends BaseEntity {

    @Column(name = "jornada_id", nullable = false)
    private UUID jornadaId;

    @Column(name = "dia_semana", nullable = false)
    private int diaSemana;

    @Column(nullable = false)
    private boolean frequenta = true;

    @Column(name = "entrada_prevista")
    private LocalTime entradaPrevista;

    @Column(name = "saida_prevista")
    private LocalTime saidaPrevista;

    /** Carga contratada do dia em minutos. E' a base do excedente por DURACAO. */
    @Column(name = "carga_minutos", nullable = false)
    private int cargaMinutos = 0;

    public UUID getJornadaId() { return jornadaId; }
    public void setJornadaId(UUID v) { this.jornadaId = v; }
    public int getDiaSemana() { return diaSemana; }
    public void setDiaSemana(int v) { this.diaSemana = v; }
    public boolean isFrequenta() { return frequenta; }
    public void setFrequenta(boolean v) { this.frequenta = v; }
    public LocalTime getEntradaPrevista() { return entradaPrevista; }
    public void setEntradaPrevista(LocalTime v) { this.entradaPrevista = v; }
    public LocalTime getSaidaPrevista() { return saidaPrevista; }
    public void setSaidaPrevista(LocalTime v) { this.saidaPrevista = v; }
    public int getCargaMinutos() { return cargaMinutos; }
    public void setCargaMinutos(int v) { this.cargaMinutos = v; }
}
