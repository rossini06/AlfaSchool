package br.com.alfaschool.backend.domain.responsavel;

import br.com.alfaschool.backend.domain.shared.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "responsaveis")
public class Responsavel extends BaseEntity {

    @Column(name = "aluno_id")
    private UUID alunoId;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(length = 14)
    private String cpf;

    @Column(length = 20)
    private String rg;

    @Column(name = "data_nascimento")
    private LocalDate dataNascimento;

    @Column(length = 10)
    private String sexo;

    @Column(name = "estado_civil", length = 20)
    private String estadoCivil;

    @Column(length = 100)
    private String profissao;

    @Column(length = 120)
    private String empresa;

    @Column(length = 20)
    private String telefone;

    @Column(length = 20)
    private String telefone2;

    @Column(length = 20)
    private String whatsapp;

    @Column(length = 160)
    private String email;

    @Column(name = "email_alternativo", length = 160)
    private String emailAlternativo;

    @Column(length = 255)
    private String logradouro;

    @Column(name = "numero_endereco", length = 20)
    private String numeroEndereco;

    @Column(length = 100)
    private String complemento;

    @Column(length = 100)
    private String bairro;

    @Column(length = 120)
    private String cidade;

    @Column(length = 2)
    private String estado;

    @Column(length = 9)
    private String cep;

    @Column(columnDefinition = "TEXT")
    private String foto;

    @Column(columnDefinition = "TEXT")
    private String observacoes;

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
    public String getRg() { return rg; }
    public void setRg(String v) { this.rg = v; }
    public LocalDate getDataNascimento() { return dataNascimento; }
    public void setDataNascimento(LocalDate v) { this.dataNascimento = v; }
    public String getSexo() { return sexo; }
    public void setSexo(String v) { this.sexo = v; }
    public String getEstadoCivil() { return estadoCivil; }
    public void setEstadoCivil(String v) { this.estadoCivil = v; }
    public String getProfissao() { return profissao; }
    public void setProfissao(String v) { this.profissao = v; }
    public String getEmpresa() { return empresa; }
    public void setEmpresa(String v) { this.empresa = v; }
    public String getTelefone() { return telefone; }
    public void setTelefone(String v) { this.telefone = v; }
    public String getTelefone2() { return telefone2; }
    public void setTelefone2(String v) { this.telefone2 = v; }
    public String getWhatsapp() { return whatsapp; }
    public void setWhatsapp(String v) { this.whatsapp = v; }
    public String getEmail() { return email; }
    public void setEmail(String v) { this.email = v; }
    public String getEmailAlternativo() { return emailAlternativo; }
    public void setEmailAlternativo(String v) { this.emailAlternativo = v; }
    public String getLogradouro() { return logradouro; }
    public void setLogradouro(String v) { this.logradouro = v; }
    public String getNumeroEndereco() { return numeroEndereco; }
    public void setNumeroEndereco(String v) { this.numeroEndereco = v; }
    public String getComplemento() { return complemento; }
    public void setComplemento(String v) { this.complemento = v; }
    public String getBairro() { return bairro; }
    public void setBairro(String v) { this.bairro = v; }
    public String getCidade() { return cidade; }
    public void setCidade(String v) { this.cidade = v; }
    public String getEstado() { return estado; }
    public void setEstado(String v) { this.estado = v; }
    public String getCep() { return cep; }
    public void setCep(String v) { this.cep = v; }
    public String getFoto() { return foto; }
    public void setFoto(String v) { this.foto = v; }
    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String v) { this.observacoes = v; }
    public String getTipo() { return tipo; }
    public void setTipo(String v) { this.tipo = v; }
    public boolean isPrincipal() { return principal; }
    public void setPrincipal(boolean v) { this.principal = v; }
}
