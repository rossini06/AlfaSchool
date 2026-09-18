package br.com.alfaschool.backend.domain.access.biometria;

import br.com.alfaschool.backend.domain.access.shared.TitularTipo;
import br.com.alfaschool.backend.domain.shared.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Foto de referencia de um titular e seu user_id dentro do equipamento.
 *
 * O matching facial acontece 100% NO LEITOR: aqui so' guardamos a
 * referencia e o veredito da sincronizacao.
 *
 * Dado biometrico de crianca e' dado sensivel com regime reforcado
 * (LGPD Art. 11 e Art. 14). Por isso base legal e consentimento sao
 * campos de primeira classe e {@link #exportavel()} e' a porta que o
 * sincronizador tem de atravessar antes de mandar a foto para o leitor.
 */
@Entity
@Table(name = "acc_faces")
public class AccFace extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "titular_tipo", nullable = false, length = 20)
    private TitularTipo titularTipo;

    @Column(name = "titular_id", nullable = false)
    private UUID titularId;

    /** Chave opaca no FotoStorage (arquivo local hoje, objeto S3 amanha). */
    @Column(name = "foto_key", nullable = false, length = 255)
    private String fotoKey;

    /**
     * user_id numerico dentro do Control iD. Vem da sequencia POR TENANT
     * (acc_device_user_seq), nunca do id da pessoa: dois tenants que
     * compartilhassem um equipamento corromperiam os dados um do outro.
     */
    @Column(name = "device_user_id", nullable = false)
    private Long deviceUserId;

    @Column(name = "base_legal", length = 40)
    private String baseLegal;

    @Column(name = "consentimento_obtido", nullable = false)
    private boolean consentimentoObtido = false;

    @Column(name = "consentimento_em")
    private Instant consentimentoEm;

    @Column(name = "consentimento_versao", length = 20)
    private String consentimentoVersao;

    @Column(name = "consentimento_origem", length = 40)
    private String consentimentoOrigem;

    @Column(name = "consentimento_por", length = 160)
    private String consentimentoPor;

    @Column(nullable = false)
    private boolean ativo = true;

    /**
     * Unica porta de saida da foto para o equipamento. Sem base legal
     * declarada ou sem consentimento registrado, a face NAO sai daqui —
     * e a recusa e' logada e registrada em acc_face_sync.
     */
    public boolean exportavel() {
        return ativo
                && Boolean.FALSE.equals(getDeleted())
                && consentimentoObtido
                && baseLegal != null
                && !baseLegal.isBlank();
    }

    public TitularTipo getTitularTipo() { return titularTipo; }
    public void setTitularTipo(TitularTipo v) { this.titularTipo = v; }
    public UUID getTitularId() { return titularId; }
    public void setTitularId(UUID v) { this.titularId = v; }
    public String getFotoKey() { return fotoKey; }
    public void setFotoKey(String v) { this.fotoKey = v; }
    public Long getDeviceUserId() { return deviceUserId; }
    public void setDeviceUserId(Long v) { this.deviceUserId = v; }
    public String getBaseLegal() { return baseLegal; }
    public void setBaseLegal(String v) { this.baseLegal = v; }
    public boolean isConsentimentoObtido() { return consentimentoObtido; }
    public void setConsentimentoObtido(boolean v) { this.consentimentoObtido = v; }
    public Instant getConsentimentoEm() { return consentimentoEm; }
    public void setConsentimentoEm(Instant v) { this.consentimentoEm = v; }
    public String getConsentimentoVersao() { return consentimentoVersao; }
    public void setConsentimentoVersao(String v) { this.consentimentoVersao = v; }
    public String getConsentimentoOrigem() { return consentimentoOrigem; }
    public void setConsentimentoOrigem(String v) { this.consentimentoOrigem = v; }
    public String getConsentimentoPor() { return consentimentoPor; }
    public void setConsentimentoPor(String v) { this.consentimentoPor = v; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean v) { this.ativo = v; }
}
