package br.com.alfaschool.backend.security.permissao;

import br.com.alfaschool.backend.domain.shared.BaseEntity;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Permissao concedida a UMA pessoa, alem do que o perfil dela da'.
 *
 * <p>Somente ADITIVA: nao existe registro de negacao. Regra de "negar"
 * espalhada por usuario vira labirinto — ninguem descobre por que fulano
 * nao consegue fazer algo, e a ordem de precedencia entre conceder e negar
 * vira fonte de bug. Para tirar acesso, troca-se o perfil.
 */
@Entity
@Table(name = "usuario_permissao_extra")
public class UsuarioPermissaoExtra extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 60)
    private Permissao permissao;

    @Column(length = 255)
    private String motivo;

    @Column(name = "concedido_por")
    private UUID concedidoPor;

    @Column(name = "concedido_em", nullable = false)
    private Instant concedidoEm = Instant.now();

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public Permissao getPermissao() { return permissao; }
    public void setPermissao(Permissao permissao) { this.permissao = permissao; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public UUID getConcedidoPor() { return concedidoPor; }
    public void setConcedidoPor(UUID v) { this.concedidoPor = v; }
    public Instant getConcedidoEm() { return concedidoEm; }
    public void setConcedidoEm(Instant v) { this.concedidoEm = v; }
}
