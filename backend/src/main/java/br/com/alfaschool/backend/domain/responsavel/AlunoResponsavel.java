package br.com.alfaschool.backend.domain.responsavel;

import br.com.alfaschool.backend.domain.shared.BaseEntity;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "aluno_responsaveis")
public class AlunoResponsavel extends BaseEntity {

    @Column(name = "aluno_id", nullable = false)
    private UUID alunoId;

    @Column(name = "responsavel_id", nullable = false)
    private UUID responsavelId;

    @Column(length = 30, nullable = false)
    private String parentesco = "outro";

    @Column(name = "parentesco_descricao", length = 100)
    private String parentescoDescricao;

    @Column(name = "responsavel_financeiro", nullable = false)
    private boolean responsavelFinanceiro = false;

    @Column(name = "responsavel_academico", nullable = false)
    private boolean responsavelAcademico = false;

    @Column(name = "autorizado_buscar", nullable = false)
    private boolean autorizadoBuscar = true;

    @Column(nullable = false)
    private boolean principal = false;

    @Column(columnDefinition = "TEXT")
    private String observacoes;

    public UUID getAlunoId() { return alunoId; }
    public void setAlunoId(UUID v) { this.alunoId = v; }
    public UUID getResponsavelId() { return responsavelId; }
    public void setResponsavelId(UUID v) { this.responsavelId = v; }
    public String getParentesco() { return parentesco; }
    public void setParentesco(String v) { this.parentesco = v; }
    public String getParentescoDescricao() { return parentescoDescricao; }
    public void setParentescoDescricao(String v) { this.parentescoDescricao = v; }
    public boolean isResponsavelFinanceiro() { return responsavelFinanceiro; }
    public void setResponsavelFinanceiro(boolean v) { this.responsavelFinanceiro = v; }
    public boolean isResponsavelAcademico() { return responsavelAcademico; }
    public void setResponsavelAcademico(boolean v) { this.responsavelAcademico = v; }
    public boolean isAutorizadoBuscar() { return autorizadoBuscar; }
    public void setAutorizadoBuscar(boolean v) { this.autorizadoBuscar = v; }
    public boolean isPrincipal() { return principal; }
    public void setPrincipal(boolean v) { this.principal = v; }
    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String v) { this.observacoes = v; }
}
