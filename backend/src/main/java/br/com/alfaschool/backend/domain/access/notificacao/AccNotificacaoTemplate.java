package br.com.alfaschool.backend.domain.access.notificacao;

import br.com.alfaschool.backend.domain.access.shared.CanalNotificacao;
import br.com.alfaschool.backend.domain.access.shared.EventoNotificacao;
import br.com.alfaschool.backend.domain.shared.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Texto de um aviso, por (tenant, evento, canal) — V42,
 * {@code acc_notificacao_templates}.
 *
 * <p>O corpo usa marcadores {@code {variavel}} substituidos no momento de
 * ENFILEIRAR, e nao no de enviar: assim o historico guarda exatamente o que a
 * familia leu, mesmo que o template mude depois.
 *
 * <p>{@code templateExterno} e' o nome do template APROVADO no provedor (Meta
 * Cloud API). Fora da janela de 24h de atendimento o WhatsApp so' aceita
 * mensagem baseada em template aprovado; o corpo local vira apenas registro
 * para o historico.
 */
@Entity
@Table(name = "acc_notificacao_templates",
        uniqueConstraints = @UniqueConstraint(name = "uk_acc_notif_templates",
                columnNames = {"tenant_id", "evento", "canal"}))
public class AccNotificacaoTemplate extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private EventoNotificacao evento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CanalNotificacao canal;

    @Column(length = 255)
    private String assunto;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String corpo;

    /** Nome do template aprovado no provedor externo (Meta Business). */
    @Column(name = "template_externo", length = 120)
    private String templateExterno;

    @Column(nullable = false)
    private boolean ativo = true;

    public EventoNotificacao getEvento() { return evento; }
    public void setEvento(EventoNotificacao evento) { this.evento = evento; }
    public CanalNotificacao getCanal() { return canal; }
    public void setCanal(CanalNotificacao canal) { this.canal = canal; }
    public String getAssunto() { return assunto; }
    public void setAssunto(String assunto) { this.assunto = assunto; }
    public String getCorpo() { return corpo; }
    public void setCorpo(String corpo) { this.corpo = corpo; }
    public String getTemplateExterno() { return templateExterno; }
    public void setTemplateExterno(String templateExterno) { this.templateExterno = templateExterno; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
}
