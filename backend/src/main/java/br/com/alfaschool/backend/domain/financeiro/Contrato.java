package br.com.alfaschool.backend.domain.financeiro;

import br.com.alfaschool.backend.domain.shared.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "contratos")
public class Contrato extends BaseEntity {

    @Column(name = "aluno_id", nullable = false)
    private UUID alunoId;

    @Column(name = "responsavel_id")
    private UUID responsavelId;

    @Column(name = "plano_id", nullable = false)
    private UUID planoId;

    @Column(name = "matricula_id")
    private UUID matriculaId;

    @Column(name = "data_inicio", nullable = false)
    private LocalDate dataInicio;

    @Column(name = "data_fim")
    private LocalDate dataFim;

    @Column(length = 20, nullable = false)
    private String status = "ativo";

    @Column(columnDefinition = "TEXT")
    private String obs;

    public UUID getAlunoId() { return alunoId; }
    public void setAlunoId(UUID v) { this.alunoId = v; }
    public UUID getResponsavelId() { return responsavelId; }
    public void setResponsavelId(UUID v) { this.responsavelId = v; }
    public UUID getPlanoId() { return planoId; }
    public void setPlanoId(UUID v) { this.planoId = v; }
    public UUID getMatriculaId() { return matriculaId; }
    public void setMatriculaId(UUID v) { this.matriculaId = v; }
    public LocalDate getDataInicio() { return dataInicio; }
    public void setDataInicio(LocalDate v) { this.dataInicio = v; }
    public LocalDate getDataFim() { return dataFim; }
    public void setDataFim(LocalDate v) { this.dataFim = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
    public String getObs() { return obs; }
    public void setObs(String v) { this.obs = v; }
}
