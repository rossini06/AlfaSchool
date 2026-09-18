package br.com.alfaschool.backend.domain.access.notificacao;

import br.com.alfaschool.backend.domain.access.shared.CanalNotificacao;
import br.com.alfaschool.backend.domain.shared.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;


/**
 * Configuracao de um canal de saida para uma escola (V42,
 * {@code acc_notificacao_configs}).
 *
 * <p>O segredo do provedor (token da Meta, senha SMTP) fica SEMPRE em
 * {@code segredo_cifrado}, cifrado por
 * {@link br.com.alfaschool.backend.infrastructure.crypto.SegredoCifrador}.
 * Nenhuma resposta de API devolve esse campo.
 *
 * <p>{@code limiteDiario} existe por um motivo de producao: no pico de saida
 * centenas de avisos saem em poucos minutos e estouram a cota do provedor. Ao
 * bater o teto o worker reagenda para o dia seguinte em vez de queimar
 * tentativas.
 */
@Entity
@Table(name = "acc_notificacao_configs",
        uniqueConstraints = @UniqueConstraint(name = "uk_acc_notif_configs",
                columnNames = {"tenant_id", "canal"}))
public class AccNotificacaoConfig extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CanalNotificacao canal;

    /** Identificador do provedor: SMTP, LOG, FAKE, META_CLOUD. */
    @Column(nullable = false, length = 40)
    private String provider = "LOG";

    /** E-mail remetente ou, no WhatsApp, o {@code phone-number-id} da Cloud API. */
    @Column(length = 160)
    private String remetente;

    /** Ajustes livres do provedor em JSON (porta SMTP, tls, endpoint base). */
    @Column(name = "config_json", columnDefinition = "TEXT")
    private String configJson;

    /**
     * Token/senha do provedor, cifrado em AES-GCM.
     *
     * <p>{@code VARBINARY(2048)} no schema, e o
     * {@link br.com.alfaschool.backend.infrastructure.crypto.SegredoCifrador}
     * ja trabalha em {@code byte[]}: o blob cifrado nunca vira texto no
     * caminho, o que evita um passo a mais onde o segredo poderia ser logado.
     */
    @Column(name = "segredo_cifrado", length = 2048)
    private byte[] segredoCifrado;

    /** Canal comeca DESLIGADO: ninguem liga WhatsApp por acidente. */
    @Column(nullable = false)
    private boolean ativo = false;

    @Column(name = "limite_diario", nullable = false)
    private Integer limiteDiario = 1000;

    public CanalNotificacao getCanal() { return canal; }
    public void setCanal(CanalNotificacao canal) { this.canal = canal; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getRemetente() { return remetente; }
    public void setRemetente(String remetente) { this.remetente = remetente; }
    public String getConfigJson() { return configJson; }
    public void setConfigJson(String configJson) { this.configJson = configJson; }
    public byte[] getSegredoCifrado() { return segredoCifrado; }
    public void setSegredoCifrado(byte[] segredoCifrado) { this.segredoCifrado = segredoCifrado; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
    public Integer getLimiteDiario() { return limiteDiario; }
    public void setLimiteDiario(Integer limiteDiario) { this.limiteDiario = limiteDiario; }

    public boolean temSegredo() {
        return segredoCifrado != null && segredoCifrado.length > 0;
    }
}
