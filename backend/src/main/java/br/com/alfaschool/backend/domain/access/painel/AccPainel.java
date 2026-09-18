package br.com.alfaschool.backend.domain.access.painel;

import br.com.alfaschool.backend.domain.access.shared.TipoPainel;
import br.com.alfaschool.backend.domain.shared.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * Painel e' TELA, nao espaco fisico.
 *
 * Duas TVs penduradas na mesma sala podem ser dois paineis diferentes, e um
 * painel de coordenacao nao fica em lugar nenhum: e' um recorte de eventos.
 * O que define o que aparece sao as fontes (AccPainelFonte), nao o tipo.
 *
 * O slug entra na URL permanente e identifica a tela. Ele NAO autentica:
 * quem autentica e' o token do dispositivo (AccPainelDispositivo).
 */
@Entity
@Table(name = "acc_paineis")
public class AccPainel extends BaseEntity {

    @Column(name = "unit_id", nullable = false)
    private UUID unitId;

    @Column(name = "nome", nullable = false, length = 120)
    private String nome;

    @Column(name = "slug", nullable = false, length = 60)
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private TipoPainel tipo = TipoPainel.SALA;

    /**
     * Tela de corredor costuma ficar com exibeFoto=false: foto de crianca
     * em lugar de passagem e' exposicao desnecessaria.
     */
    @Column(name = "exibe_foto", nullable = false)
    private boolean exibeFoto = true;

    /**
     * Segundos que o cartao fica na tela apos a entrega. Depois disso a
     * foto tem de sair: a crianca ja foi liberada e nao ha motivo para o
     * rosto continuar exposto.
     */
    @Column(name = "retencao_seg", nullable = false)
    private int retencaoSeg = 20;

    @Column(name = "ativo", nullable = false)
    private boolean ativo = true;

    public UUID getUnitId() { return unitId; }
    public void setUnitId(UUID v) { this.unitId = v; }
    public String getNome() { return nome; }
    public void setNome(String v) { this.nome = v; }
    public String getSlug() { return slug; }
    public void setSlug(String v) { this.slug = v; }
    public TipoPainel getTipo() { return tipo; }
    public void setTipo(TipoPainel v) { this.tipo = v; }
    public boolean isExibeFoto() { return exibeFoto; }
    public void setExibeFoto(boolean v) { this.exibeFoto = v; }
    public int getRetencaoSeg() { return retencaoSeg; }
    public void setRetencaoSeg(int v) { this.retencaoSeg = v; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean v) { this.ativo = v; }
}
