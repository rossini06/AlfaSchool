package br.com.alfaschool.backend.domain.access.estrutura;

import br.com.alfaschool.backend.domain.shared.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Vinculo turma<->sala com vigencia. Uma sala abriga VARIAS turmas e uma turma
 * troca de sala ao longo do dia (aula de laboratorio) ou do ano (mudanca de
 * bloco). Por isso nao existe "a sala da turma": existe a sala vigente NAQUELE
 * instante, que sai daqui.
 *
 * Convencoes de nulo, que sao a parte que mais confunde na operacao:
 *   vigenciaFim  nulo = vale por tempo indeterminado;
 *   diasSemana   nulo/vazio = vale todos os dias;
 *   hora inicio/fim nulos = vale o dia inteiro.
 * Ou seja, nulo sempre significa "sem restricao", nunca "nenhum".
 */
@Entity
@Table(name = "acc_turma_salas")
public class AccTurmaSala extends BaseEntity {

    @Column(name = "turma_id", nullable = false)
    private UUID turmaId;

    @Column(name = "sala_id", nullable = false)
    private UUID salaId;

    @Column(name = "vigencia_inicio", nullable = false)
    private LocalDate vigenciaInicio;

    @Column(name = "vigencia_fim")
    private LocalDate vigenciaFim;

    @Column(name = "hora_inicio")
    private LocalTime horaInicio;

    @Column(name = "hora_fim")
    private LocalTime horaFim;

    // Mascara CSV "1,2,3,4,5" seguindo ISO-8601: 1=segunda ... 7=domingo.
    @Column(name = "dias_semana", length = 20)
    private String diasSemana;

    public UUID getTurmaId() { return turmaId; }
    public void setTurmaId(UUID turmaId) { this.turmaId = turmaId; }
    public UUID getSalaId() { return salaId; }
    public void setSalaId(UUID salaId) { this.salaId = salaId; }
    public LocalDate getVigenciaInicio() { return vigenciaInicio; }
    public void setVigenciaInicio(LocalDate vigenciaInicio) { this.vigenciaInicio = vigenciaInicio; }
    public LocalDate getVigenciaFim() { return vigenciaFim; }
    public void setVigenciaFim(LocalDate vigenciaFim) { this.vigenciaFim = vigenciaFim; }
    public LocalTime getHoraInicio() { return horaInicio; }
    public void setHoraInicio(LocalTime horaInicio) { this.horaInicio = horaInicio; }
    public LocalTime getHoraFim() { return horaFim; }
    public void setHoraFim(LocalTime horaFim) { this.horaFim = horaFim; }
    public String getDiasSemana() { return diasSemana; }
    public void setDiasSemana(String diasSemana) { this.diasSemana = diasSemana; }

    // ------------------------------------------------------------------
    // Regras de vigencia
    // ------------------------------------------------------------------

    /** Dias da semana cobertos; vazio no CSV significa a semana inteira. */
    public Set<Integer> diasSemanaResolvidos() {
        return parseDias(diasSemana);
    }

    public boolean temFaixaHoraria() {
        return horaInicio != null || horaFim != null;
    }

    public boolean temRestricaoDeDias() {
        Set<Integer> dias = diasSemanaResolvidos();
        return dias.size() < 7;
    }

    /** Verdadeiro se este vinculo esta valendo no instante informado. */
    public boolean aplicaEm(LocalDateTime momento) {
        if (momento == null) {
            return false;
        }
        LocalDate data = momento.toLocalDate();
        if (vigenciaInicio != null && data.isBefore(vigenciaInicio)) {
            return false;
        }
        if (vigenciaFim != null && data.isAfter(vigenciaFim)) {
            return false;
        }
        if (!diasSemanaResolvidos().contains(data.getDayOfWeek().getValue())) {
            return false;
        }
        return cobreHora(momento.toLocalTime());
    }

    /**
     * Faixa horaria com inicio inclusivo e fim EXCLUSIVO. Sem isso, dois turnos
     * encostados (08:00-12:00 e 12:00-18:00) casariam os dois as 12:00 e a sala
     * ficaria ambigua justamente na troca.
     */
    private boolean cobreHora(LocalTime hora) {
        if (horaInicio != null && hora.isBefore(horaInicio)) {
            return false;
        }
        return horaFim == null || hora.isBefore(horaFim);
    }

    /**
     * Quanto mais restrito o vinculo, mais especifico ele e'. Serve de criterio
     * de desempate: a regra de excecao (aula de laboratorio das 10h as 12h)
     * precisa ganhar da regra geral (sala fixa da turma o ano inteiro).
     */
    public int especificidade() {
        int peso = 0;
        if (temFaixaHoraria()) {
            peso += 2;
        }
        if (temRestricaoDeDias()) {
            peso += 1;
        }
        return peso;
    }

    /**
     * Sobreposicao = vigencia E dias E faixa horaria se cruzam. Dois vinculos
     * assim deixariam a sala da turma ambigua naquele instante, e a apuracao de
     * permanencia nao teria como escolher.
     */
    public boolean sobrepoe(AccTurmaSala outro) {
        if (outro == null) {
            return false;
        }
        return vigenciasCruzam(outro) && diasCruzam(outro) && horariosCruzam(outro);
    }

    private boolean vigenciasCruzam(AccTurmaSala outro) {
        LocalDate aIni = this.vigenciaInicio;
        LocalDate bIni = outro.vigenciaInicio;
        // fim nulo = aberto; comparamos com null-safety em vez de usar MAX/MIN.
        boolean aComecaDepoisDoFimDeB = outro.vigenciaFim != null && aIni != null && aIni.isAfter(outro.vigenciaFim);
        boolean bComecaDepoisDoFimDeA = this.vigenciaFim != null && bIni != null && bIni.isAfter(this.vigenciaFim);
        return !aComecaDepoisDoFimDeB && !bComecaDepoisDoFimDeA;
    }

    private boolean diasCruzam(AccTurmaSala outro) {
        Set<Integer> meus = this.diasSemanaResolvidos();
        for (Integer dia : outro.diasSemanaResolvidos()) {
            if (meus.contains(dia)) {
                return true;
            }
        }
        return false;
    }

    private boolean horariosCruzam(AccTurmaSala outro) {
        LocalTime aIni = this.horaInicio != null ? this.horaInicio : LocalTime.MIN;
        LocalTime bIni = outro.horaInicio != null ? outro.horaInicio : LocalTime.MIN;
        // Fim exclusivo tambem aqui: 08:00-12:00 nao colide com 12:00-18:00.
        boolean aTerminaAntesDeB = this.horaFim != null && !this.horaFim.isAfter(bIni);
        boolean bTerminaAntesDeA = outro.horaFim != null && !outro.horaFim.isAfter(aIni);
        return !aTerminaAntesDeB && !bTerminaAntesDeA;
    }

    /** CSV "1,2,3,4,5" -> {1,2,3,4,5}. Nulo/vazio devolve a semana inteira. */
    public static Set<Integer> parseDias(String csv) {
        Set<Integer> dias = new LinkedHashSet<>();
        if (csv != null && !csv.isBlank()) {
            for (String parte : csv.split(",")) {
                String limpo = parte.trim();
                if (limpo.isEmpty()) {
                    continue;
                }
                dias.add(Integer.parseInt(limpo));
            }
        }
        if (dias.isEmpty()) {
            for (int dia = 1; dia <= 7; dia++) {
                dias.add(dia);
            }
        }
        return dias;
    }
}
