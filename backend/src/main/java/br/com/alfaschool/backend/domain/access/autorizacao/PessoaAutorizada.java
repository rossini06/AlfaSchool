package br.com.alfaschool.backend.domain.access.autorizacao;

import br.com.alfaschool.backend.domain.shared.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * Pessoa fisica que pode se apresentar na portaria. Pode ser um responsavel
 * ja cadastrado (responsavelId preenchido) ou um terceiro sem vinculo
 * academico (avo, tia, motorista) — por isso nome/cpf ficam aqui e nao sao
 * lidos de responsaveis.
 *
 * REGRA CENTRAL — NAO ACOPLE ESTES CAMPOS, NUNCA DERIVE UM DO OUTRO:
 * podeRetirar, podeAcessarPortal e recebeNotificacao sao TRES permissoes
 * INDEPENDENTES. Estar cadastrado como responsavel do aluno nao concede
 * nenhuma das tres. A avo que busca as sextas precisa de podeRetirar=true
 * e nao precisa de login no portal nem de notificacao.
 *
 * Alem disso, podeRetirar=true NAO autoriza retirar aluno nenhum: e' apenas
 * a porta de entrada. Quem autoriza um aluno especifico e' a
 * AutorizacaoRetirada, e acima dela a Restricao tem precedencia absoluta.
 */
@Entity
@Table(name = "acc_pessoas_autorizadas")
public class PessoaAutorizada extends BaseEntity {

    /** Quando preenchido, aponta para responsaveis.id. Nulo para terceiros. */
    @Column(name = "responsavel_id")
    private UUID responsavelId;

    @Column(nullable = false, length = 120)
    private String nome;

    /** Guardado so com digitos, para casar com o CPF solto de uma restricao. */
    @Column(length = 14)
    private String cpf;

    @Column(length = 20)
    private String rg;

    @Column(length = 20)
    private String telefone;

    @Column(length = 160)
    private String email;

    @Column(name = "foto_key", length = 255)
    private String fotoKey;

    /** Permissao 1 de 3: apresentar-se na portaria para retirar aluno. */
    @Column(name = "pode_retirar", nullable = false)
    private boolean podeRetirar = true;

    /** Permissao 2 de 3: login no portal do responsavel. Independente da 1. */
    @Column(name = "pode_acessar_portal", nullable = false)
    private boolean podeAcessarPortal = false;

    /** Permissao 3 de 3: receber avisos de entrada/saida. Independente das outras. */
    @Column(name = "recebe_notificacao", nullable = false)
    private boolean recebeNotificacao = false;

    @Column(nullable = false)
    private boolean ativo = true;

    public UUID getResponsavelId() { return responsavelId; }
    public void setResponsavelId(UUID responsavelId) { this.responsavelId = responsavelId; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getCpf() { return cpf; }
    public void setCpf(String cpf) { this.cpf = cpf; }
    public String getRg() { return rg; }
    public void setRg(String rg) { this.rg = rg; }
    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getFotoKey() { return fotoKey; }
    public void setFotoKey(String fotoKey) { this.fotoKey = fotoKey; }
    public boolean isPodeRetirar() { return podeRetirar; }
    public void setPodeRetirar(boolean podeRetirar) { this.podeRetirar = podeRetirar; }
    public boolean isPodeAcessarPortal() { return podeAcessarPortal; }
    public void setPodeAcessarPortal(boolean podeAcessarPortal) { this.podeAcessarPortal = podeAcessarPortal; }
    public boolean isRecebeNotificacao() { return recebeNotificacao; }
    public void setRecebeNotificacao(boolean recebeNotificacao) { this.recebeNotificacao = recebeNotificacao; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
}
