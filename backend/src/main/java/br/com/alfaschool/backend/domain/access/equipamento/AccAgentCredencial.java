package br.com.alfaschool.backend.domain.access.equipamento;

import br.com.alfaschool.backend.domain.shared.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Credencial do agente local (gateway instalado na LAN da escola).
 *
 * A senha NUNCA e' guardada nem devolvida em claro: so' o hash BCrypt vive
 * aqui. O valor em claro e' exibido uma unica vez, no momento da criacao.
 * Agente perdeu a senha? Gera-se outra credencial — nao existe "recuperar".
 */
@Entity
@Table(name = "acc_agent_credenciais")
public class AccAgentCredencial extends BaseEntity {

    @Column(name = "unit_id")
    private UUID unitId;

    @Column(nullable = false, length = 60)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(length = 255)
    private String descricao;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "ultimo_login")
    private Instant ultimoLogin;

    public UUID getUnitId() { return unitId; }
    public void setUnitId(UUID v) { this.unitId = v; }
    public String getUsername() { return username; }
    public void setUsername(String v) { this.username = v; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String v) { this.passwordHash = v; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String v) { this.descricao = v; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean v) { this.ativo = v; }
    public Instant getUltimoLogin() { return ultimoLogin; }
    public void setUltimoLogin(Instant v) { this.ultimoLogin = v; }
}
