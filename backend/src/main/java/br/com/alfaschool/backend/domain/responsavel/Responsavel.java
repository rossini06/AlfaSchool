package br.com.alfaschool.backend.domain.responsavel;

import br.com.alfaschool.backend.domain.shared.BaseEntity;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "responsaveis")
public class Responsavel extends BaseEntity {

    @Column(name = "aluno_id", nullable = false)
    private UUID alunoId;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(length = 14)
    private String cpf;

    @Column(length = 20)
    private String telefone;

    @Column(length = 160)
    private String email;

    @Column(length = 20, nullable = false)
    private String tipo = "financeiro";

    @Column(nullable = false)
    private boolean principal = false;

    public UUID getAlunoId() { return alunoId; }
    public void setAlunoId(UUID v) { this.alunoId = v; }
    public String getNome() { return nome; }
    public void setNome(String v) { this.nome = v; }
    public String getCpf() { return cpf; }
    public void setCpf(String v) { this.cpf = v; }
    public String getTelefone() { return telefone; }
    public void setTelefone(String v) { this.telefone = v; }
    public String getEmail() { return email; }
    public void setEmail(String v) { this.email = v; }
    public String getTipo() { return tipo; }
    public void setTipo(String v) { this.tipo = v; }
    public boolean isPrincipal() { return principal; }
    public void setPrincipal(boolean v) { this.principal = v; }
}
