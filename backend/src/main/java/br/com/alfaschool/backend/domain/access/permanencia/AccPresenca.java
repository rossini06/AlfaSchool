package br.com.alfaschool.backend.domain.access.permanencia;

import br.com.alfaschool.backend.domain.access.shared.StatusPresenca;
import br.com.alfaschool.backend.domain.shared.BaseEntity;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * O DIA do aluno. Os intervalos vivem em {@link AccPresencaPar}: um dia
 * pode ter varios pares (o aluno sai para a consulta e volta), e o total
 * do dia e' a soma dos pares FECHADOS.
 *
 * Esta linha vira cobranca. Duas regras mandam mais que qualquer outra:
 *
 * 1) congelada = true NUNCA e' recalculada. Fatura emitida nao muda,
 *    mesmo que alguem edite a jornada depois.
 * 2) status INCONSISTENTE nao entra em NENHUM total agregado. A linha
 *    aparece no relatorio rotulada, para alguem corrigir, mas os
 *    derivados ficam zerados e toda consulta agregada ainda filtra o
 *    status — protecao dupla de proposito.
 */
@Entity
@Table(name = "acc_presencas")
public class AccPresenca extends BaseEntity {

    @Column(name = "unit_id")
    private UUID unitId;

    @Column(name = "aluno_id", nullable = false)
    private UUID alunoId;

    @Column(nullable = false)
    private LocalDate data;

    @Column(name = "primeira_entrada_em")
    private Instant primeiraEntradaEm;

    @Column(name = "ultima_saida_em")
    private Instant ultimaSaidaEm;

    @Column(name = "minutos_permanencia", nullable = false)
    private int minutosPermanencia = 0;

    @Column(name = "minutos_previstos", nullable = false)
    private int minutosPrevistos = 0;

    @Column(name = "minutos_excedente", nullable = false)
    private int minutosExcedente = 0;

    @Column(name = "minutos_antecipacao", nullable = false)
    private int minutosAntecipacao = 0;

    /**
     * Atraso na ENTRADA, ja descontada a tolerancia da jornada. E' a
     * contrapartida do excedente na saida: sem persistir, nao da para
     * responder se um aluno chega tarde sempre.
     */
    @Column(name = "minutos_atraso", nullable = false)
    private int minutosAtraso = 0;

    /** Snapshot de QUAL jornada foi usada — auditoria da conta cobrada. */
    @Column(name = "jornada_id")
    private UUID jornadaId;

    @Column(name = "dia_letivo", nullable = false)
    private boolean diaLetivo = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusPresenca status = StatusPresenca.ABERTA;

    @Column(nullable = false)
    private boolean congelada = false;

    @Column(name = "congelada_em")
    private Instant congeladaEm;

    @Column(length = 255)
    private String observacao;

    public UUID getUnitId() { return unitId; }
    public void setUnitId(UUID v) { this.unitId = v; }
    public UUID getAlunoId() { return alunoId; }
    public void setAlunoId(UUID v) { this.alunoId = v; }
    public LocalDate getData() { return data; }
    public void setData(LocalDate v) { this.data = v; }
    public Instant getPrimeiraEntradaEm() { return primeiraEntradaEm; }
    public void setPrimeiraEntradaEm(Instant v) { this.primeiraEntradaEm = v; }
    public Instant getUltimaSaidaEm() { return ultimaSaidaEm; }
    public void setUltimaSaidaEm(Instant v) { this.ultimaSaidaEm = v; }
    public int getMinutosPermanencia() { return minutosPermanencia; }
    public void setMinutosPermanencia(int v) { this.minutosPermanencia = v; }
    public int getMinutosPrevistos() { return minutosPrevistos; }
    public void setMinutosPrevistos(int v) { this.minutosPrevistos = v; }
    public int getMinutosExcedente() { return minutosExcedente; }
    public void setMinutosExcedente(int v) { this.minutosExcedente = v; }
    public int getMinutosAntecipacao() { return minutosAntecipacao; }
    public void setMinutosAntecipacao(int v) { this.minutosAntecipacao = v; }
    public int getMinutosAtraso() { return minutosAtraso; }
    public void setMinutosAtraso(int v) { this.minutosAtraso = v; }
    public UUID getJornadaId() { return jornadaId; }
    public void setJornadaId(UUID v) { this.jornadaId = v; }
    public boolean isDiaLetivo() { return diaLetivo; }
    public void setDiaLetivo(boolean v) { this.diaLetivo = v; }
    public StatusPresenca getStatus() { return status; }
    public void setStatus(StatusPresenca v) { this.status = v; }
    public boolean isCongelada() { return congelada; }
    public void setCongelada(boolean v) { this.congelada = v; }
    public Instant getCongeladaEm() { return congeladaEm; }
    public void setCongeladaEm(Instant v) { this.congeladaEm = v; }
    public String getObservacao() { return observacao; }
    public void setObservacao(String v) { this.observacao = v; }
}
