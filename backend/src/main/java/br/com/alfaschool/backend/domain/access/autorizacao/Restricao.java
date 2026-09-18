package br.com.alfaschool.backend.domain.access.autorizacao;

import br.com.alfaschool.backend.domain.shared.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Impedimento judicial ou administrativo de aproximacao/retirada.
 *
 * PRECEDENCIA ABSOLUTA: uma restricao ativa e vigente vence QUALQUER
 * autorizacao, inclusive ATIVA e permanente. Na ordem de avaliacao do
 * AutorizacaoConsultaService ela e' o passo 1, antes mesmo de checar se a
 * pessoa existe — porque o pai afastado por medida protetiva costuma estar
 * cadastrado e autorizado de antes da decisao.
 *
 * A pessoa restrita pode NAO estar cadastrada: por isso existe o par
 * pessoaNome/pessoaCpf solto, alternativo a pessoaAutorizadaId. O casamento
 * por CPF (so digitos) e' o que bloqueia alguem que se cadastre depois.
 */
@Entity
@Table(name = "acc_restricoes")
public class Restricao extends BaseEntity {

    @Column(name = "aluno_id", nullable = false)
    private UUID alunoId;

    /** Nulo quando a restricao aponta apenas para nome+CPF soltos. */
    @Column(name = "pessoa_autorizada_id")
    private UUID pessoaAutorizadaId;

    @Column(name = "pessoa_nome", length = 120)
    private String pessoaNome;

    /** Guardado so com digitos para casar com PessoaAutorizada.cpf. */
    @Column(name = "pessoa_cpf", length = 14)
    private String pessoaCpf;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoRestricao tipo = TipoRestricao.JUDICIAL;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descricao;

    /**
     * Chave do documento em storage de ACESSO RESTRITO (mandado, decisao).
     * Nao deve ir para listagem comum — so pelo endpoint dedicado com
     * @PreAuthorize mais estrito.
     */
    @Column(name = "documento_key", length = 255)
    private String documentoKey;

    @Column(name = "vigencia_inicio")
    private LocalDate vigenciaInicio;

    /** Nulo = sem prazo, vale ate ser encerrada explicitamente. */
    @Column(name = "vigencia_fim")
    private LocalDate vigenciaFim;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "registrado_por_user_id")
    private UUID registradoPorUserId;

    public UUID getAlunoId() { return alunoId; }
    public void setAlunoId(UUID alunoId) { this.alunoId = alunoId; }
    public UUID getPessoaAutorizadaId() { return pessoaAutorizadaId; }
    public void setPessoaAutorizadaId(UUID pessoaAutorizadaId) { this.pessoaAutorizadaId = pessoaAutorizadaId; }
    public String getPessoaNome() { return pessoaNome; }
    public void setPessoaNome(String pessoaNome) { this.pessoaNome = pessoaNome; }
    public String getPessoaCpf() { return pessoaCpf; }
    public void setPessoaCpf(String pessoaCpf) { this.pessoaCpf = pessoaCpf; }
    public TipoRestricao getTipo() { return tipo; }
    public void setTipo(TipoRestricao tipo) { this.tipo = tipo; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public String getDocumentoKey() { return documentoKey; }
    public void setDocumentoKey(String documentoKey) { this.documentoKey = documentoKey; }
    public LocalDate getVigenciaInicio() { return vigenciaInicio; }
    public void setVigenciaInicio(LocalDate vigenciaInicio) { this.vigenciaInicio = vigenciaInicio; }
    public LocalDate getVigenciaFim() { return vigenciaFim; }
    public void setVigenciaFim(LocalDate vigenciaFim) { this.vigenciaFim = vigenciaFim; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
    public UUID getRegistradoPorUserId() { return registradoPorUserId; }
    public void setRegistradoPorUserId(UUID registradoPorUserId) { this.registradoPorUserId = registradoPorUserId; }

    /**
     * Vigente na data informada. Extremos INCLUSIVOS e limites nulos
     * significam "sem limite" — o dia em que a decisao comeca e o dia em que
     * ela termina sao dias em que ela vale.
     */
    public boolean vigenteEm(LocalDate data) {
        if (vigenciaInicio != null && data.isBefore(vigenciaInicio)) {
            return false;
        }
        return vigenciaFim == null || !data.isAfter(vigenciaFim);
    }
}
