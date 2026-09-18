package br.com.alfaschool.backend.domain.access.notificacao;

import br.com.alfaschool.backend.domain.access.shared.CanalNotificacao;
import br.com.alfaschool.backend.domain.access.shared.EventoNotificacao;
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
 * Consentimento de uma pessoa concreta para receber um aviso por um canal
 * (V42, {@code acc_notificacao_preferencias}).
 *
 * <p>REGRA INEGOCIAVEL: so' ha envio quando {@code habilitado = true} E
 * {@code opt_in_em} esta preenchido. Nao existe "todo mundo recebe por
 * padrao" — dado de rotina de crianca circulando por WhatsApp sem
 * consentimento registrado e' exposicao legal e reputacional. Sem opt-in o
 * motor registra o motivo no log e nao envia.
 *
 * <p>Repare que a coluna {@code habilitado} tem DEFAULT TRUE no schema: isso
 * NAO significa recebimento por padrao. O portao e' o par
 * (habilitado, opt_in_em), e {@code opt_in_em} nasce nulo.
 *
 * <p>{@code evento} nulo significa "vale para todos os eventos"; uma linha com
 * evento especifico tem precedencia sobre a coringa.
 */
@Entity
@Table(name = "acc_notificacao_preferencias")
public class AccNotificacaoPreferencia extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "titular_tipo", nullable = false, length = 20)
    private TitularTipo titularTipo;

    @Column(name = "titular_id", nullable = false)
    private UUID titularId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CanalNotificacao canal;

    /** Nulo = preferencia coringa, vale para qualquer evento. */
    @Enumerated(EnumType.STRING)
    @Column(length = 40)
    private EventoNotificacao evento;

    /** E-mail ou telefone para onde a mensagem vai. */
    @Column(nullable = false, length = 160)
    private String destino;

    @Column(nullable = false)
    private boolean habilitado = true;

    /** Carimbo do consentimento. Sem ele nao ha envio, mesmo com habilitado. */
    @Column(name = "opt_in_em")
    private Instant optInEm;

    @Column(name = "opt_out_em")
    private Instant optOutEm;

    public TitularTipo getTitularTipo() { return titularTipo; }
    public void setTitularTipo(TitularTipo titularTipo) { this.titularTipo = titularTipo; }
    public UUID getTitularId() { return titularId; }
    public void setTitularId(UUID titularId) { this.titularId = titularId; }
    public CanalNotificacao getCanal() { return canal; }
    public void setCanal(CanalNotificacao canal) { this.canal = canal; }
    public EventoNotificacao getEvento() { return evento; }
    public void setEvento(EventoNotificacao evento) { this.evento = evento; }
    public String getDestino() { return destino; }
    public void setDestino(String destino) { this.destino = destino; }
    public boolean isHabilitado() { return habilitado; }
    public void setHabilitado(boolean habilitado) { this.habilitado = habilitado; }
    public Instant getOptInEm() { return optInEm; }
    public void setOptInEm(Instant optInEm) { this.optInEm = optInEm; }
    public Instant getOptOutEm() { return optOutEm; }
    public void setOptOutEm(Instant optOutEm) { this.optOutEm = optOutEm; }

    /** Consentimento valido. Unico portao de envio do motor. */
    public boolean temConsentimento() {
        return habilitado && optInEm != null && destino != null && !destino.isBlank();
    }
}
