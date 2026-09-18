package br.com.alfaschool.backend.domain.modulo;

import br.com.alfaschool.backend.domain.shared.BaseEntity;
import jakarta.persistence.*;
import java.time.Instant;

/** Contratacao de um modulo por uma escola. */
@Entity
@Table(name = "tenant_modulos")
public class TenantModulo extends BaseEntity {

    @Column(name = "modulo_codigo", nullable = false, length = 40)
    private String moduloCodigo;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "ativado_em")
    private Instant ativadoEm;

    /** Nulo = sem prazo. Vencida, a contratacao deixa de valer sem precisar de job. */
    @Column(name = "expira_em")
    private Instant expiraEm;

    public String getModuloCodigo() { return moduloCodigo; }
    public void setModuloCodigo(String v) { this.moduloCodigo = v; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
    public Instant getAtivadoEm() { return ativadoEm; }
    public void setAtivadoEm(Instant v) { this.ativadoEm = v; }
    public Instant getExpiraEm() { return expiraEm; }
    public void setExpiraEm(Instant v) { this.expiraEm = v; }

    public boolean vigente() {
        return ativo && !Boolean.TRUE.equals(getDeleted())
                && (expiraEm == null || expiraEm.isAfter(Instant.now()));
    }
}
