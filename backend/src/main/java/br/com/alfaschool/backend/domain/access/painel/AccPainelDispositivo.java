package br.com.alfaschool.backend.domain.access.painel;

import br.com.alfaschool.backend.domain.shared.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * A TV. E' o que de fato autentica o acesso ao stream do painel.
 *
 * O token em claro NUNCA e' guardado: so' o SHA-256 e um prefixo curto para
 * a coordenacao saber qual TV e' qual na tela de revogacao. Se o banco
 * vazar, ninguem assiste ao fluxo de fotos de crianca com o que vazou.
 *
 * revogado e' checado a cada conexao: TV roubada ou trocada de sala sai do
 * ar na hora, sem esperar expiracao.
 */
@Entity
@Table(name = "acc_painel_dispositivos")
public class AccPainelDispositivo extends BaseEntity {

    @Column(name = "painel_id", nullable = false)
    private UUID painelId;

    @Column(name = "nome", nullable = false, length = 120)
    private String nome;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "token_prefixo", length = 12)
    private String tokenPrefixo;

    @Column(name = "ultimo_acesso")
    private Instant ultimoAcesso;

    @Column(name = "ultimo_ip", length = 45)
    private String ultimoIp;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(name = "revogado", nullable = false)
    private boolean revogado = false;

    @Column(name = "revogado_em")
    private Instant revogadoEm;

    @Column(name = "revogado_por")
    private UUID revogadoPor;

    public UUID getPainelId() { return painelId; }
    public void setPainelId(UUID v) { this.painelId = v; }
    public String getNome() { return nome; }
    public void setNome(String v) { this.nome = v; }
    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String v) { this.tokenHash = v; }
    public String getTokenPrefixo() { return tokenPrefixo; }
    public void setTokenPrefixo(String v) { this.tokenPrefixo = v; }
    public Instant getUltimoAcesso() { return ultimoAcesso; }
    public void setUltimoAcesso(Instant v) { this.ultimoAcesso = v; }
    public String getUltimoIp() { return ultimoIp; }
    public void setUltimoIp(String v) { this.ultimoIp = v; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String v) { this.userAgent = v; }
    public boolean isRevogado() { return revogado; }
    public void setRevogado(boolean v) { this.revogado = v; }
    public Instant getRevogadoEm() { return revogadoEm; }
    public void setRevogadoEm(Instant v) { this.revogadoEm = v; }
    public UUID getRevogadoPor() { return revogadoPor; }
    public void setRevogadoPor(UUID v) { this.revogadoPor = v; }
}
