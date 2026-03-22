package br.com.alfaschool.backend.domain.dispositivo;

import br.com.alfaschool.backend.domain.shared.BaseEntity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "dispositivos")
public class Dispositivo extends BaseEntity {

    @Column(name = "unit_id")
    private UUID unitId;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false, length = 40)
    private String tipo = "catraca";

    @Column(length = 80)
    private String fabricante;

    @Column(length = 80)
    private String modelo;

    @Column(length = 45)
    private String ip;

    @Column
    private Integer porta = 80;

    @Column(length = 80)
    private String serial;

    @Column(name = "api_token", length = 255)
    private String apiToken;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(nullable = false)
    private boolean online = false;

    @Column(name = "ultimo_ping")
    private Instant ultimoPing;

    public UUID getUnitId() { return unitId; }
    public void setUnitId(UUID v) { this.unitId = v; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getFabricante() { return fabricante; }
    public void setFabricante(String v) { this.fabricante = v; }
    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }
    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
    public Integer getPorta() { return porta; }
    public void setPorta(Integer porta) { this.porta = porta; }
    public String getSerial() { return serial; }
    public void setSerial(String serial) { this.serial = serial; }
    public String getApiToken() { return apiToken; }
    public void setApiToken(String apiToken) { this.apiToken = apiToken; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
    public boolean isOnline() { return online; }
    public void setOnline(boolean online) { this.online = online; }
    public Instant getUltimoPing() { return ultimoPing; }
    public void setUltimoPing(Instant v) { this.ultimoPing = v; }
}
