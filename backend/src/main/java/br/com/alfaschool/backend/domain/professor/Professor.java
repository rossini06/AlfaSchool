package br.com.alfaschool.backend.domain.professor;

import br.com.alfaschool.backend.domain.shared.BaseEntity;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "professores")
public class Professor extends BaseEntity {

    @Column(name = "unit_id")
    private UUID unitId;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(length = 14)
    private String cpf;

    @Column(length = 160)
    private String email;

    @Column(length = 20)
    private String telefone;

    @Column(length = 120)
    private String especialidade;

    @Column(length = 20, nullable = false)
    private String status = "ativo";

    public UUID getUnitId() { return unitId; }
    public void setUnitId(UUID v) { this.unitId = v; }
    public String getNome() { return nome; }
    public void setNome(String v) { this.nome = v; }
    public String getCpf() { return cpf; }
    public void setCpf(String v) { this.cpf = v; }
    public String getEmail() { return email; }
    public void setEmail(String v) { this.email = v; }
    public String getTelefone() { return telefone; }
    public void setTelefone(String v) { this.telefone = v; }
    public String getEspecialidade() { return especialidade; }
    public void setEspecialidade(String v) { this.especialidade = v; }
    public String getStatus() { return status; }
    public void setStatus(String v) { this.status = v; }
}
