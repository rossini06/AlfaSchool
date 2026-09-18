package br.com.alfaschool.backend.application.access.notificacao;

import br.com.alfaschool.backend.application.access.notificacao.dto.NotificacaoTemplateRequest;
import br.com.alfaschool.backend.application.access.notificacao.dto.NotificacaoTemplateResponse;
import br.com.alfaschool.backend.application.access.notificacao.dto.TemplatePreviewRequest;
import br.com.alfaschool.backend.application.access.notificacao.dto.TemplatePreviewResponse;
import br.com.alfaschool.backend.domain.access.notificacao.AccNotificacaoTemplate;
import br.com.alfaschool.backend.domain.access.shared.CanalNotificacao;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccNotificacaoTemplateRepository;
import br.com.alfaschool.backend.security.filter.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** CRUD de templates por evento e canal, com preview renderizado. */
@Service
public class NotificacaoTemplateService {

    private final AccNotificacaoTemplateRepository templateRepository;
    private final NotificacaoRenderer renderer;

    public NotificacaoTemplateService(AccNotificacaoTemplateRepository templateRepository,
                                      NotificacaoRenderer renderer) {
        this.templateRepository = templateRepository;
        this.renderer = renderer;
    }

    public List<NotificacaoTemplateResponse> listar() {
        UUID tenantId = tenantObrigatorio();
        return templateRepository.findByTenantIdAndDeletedFalseOrderByEventoAscCanalAsc(tenantId).stream()
                .map(NotificacaoTemplateResponse::from)
                .toList();
    }

    public NotificacaoTemplateResponse buscar(UUID id) {
        return NotificacaoTemplateResponse.from(carregar(id));
    }

    @Transactional
    public NotificacaoTemplateResponse criar(NotificacaoTemplateRequest request) {
        UUID tenantId = tenantObrigatorio();
        validarCorpo(request.corpo());
        AccNotificacaoTemplate template = new AccNotificacaoTemplate();
        template.setTenantId(tenantId);
        aplicar(template, request);
        return NotificacaoTemplateResponse.from(templateRepository.save(template));
    }

    @Transactional
    public NotificacaoTemplateResponse atualizar(UUID id, NotificacaoTemplateRequest request) {
        AccNotificacaoTemplate template = carregar(id);
        validarCorpo(request.corpo());
        aplicar(template, request);
        return NotificacaoTemplateResponse.from(templateRepository.save(template));
    }

    @Transactional
    public void remover(UUID id) {
        AccNotificacaoTemplate template = carregar(id);
        template.setDeleted(true);
        templateRepository.save(template);
    }

    /**
     * Renderiza o template com valores de exemplo. Serve tambem de barreira:
     * um template que pede {@code {foto}} e' recusado aqui, antes de virar
     * cadastro.
     */
    public TemplatePreviewResponse preview(TemplatePreviewRequest request) {
        UUID tenantId = tenantObrigatorio();
        String assunto = request.assunto();
        String corpo = request.corpo();
        boolean usouPadrao = false;

        if ((corpo == null || corpo.isBlank()) && request.evento() != null) {
            CanalNotificacao canal = request.canal() == null ? CanalNotificacao.EMAIL : request.canal();
            Optional<AccNotificacaoTemplate> gravado = templateRepository
                    .findFirstByTenantIdAndEventoAndCanalAndAtivoTrueAndDeletedFalse(tenantId, request.evento(), canal);
            if (gravado.isPresent()) {
                assunto = gravado.get().getAssunto();
                corpo = gravado.get().getCorpo();
            } else {
                NotificacaoRenderer.TemplatePadrao padrao = renderer.padrao(request.evento(), canal);
                assunto = padrao.assunto();
                corpo = padrao.corpo();
                usouPadrao = true;
            }
        }
        validarCorpo(corpo);

        Map<String, String> variaveis = request.variaveis() == null ? exemplo() : request.variaveis();
        try {
            renderer.validarVariaveis(variaveis);
        } catch (NotificacaoRenderer.VariavelProibidaException e) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
        }

        LinkedHashSet<String> marcadores = new LinkedHashSet<>(renderer.extrairMarcadores(corpo).keySet());
        marcadores.addAll(renderer.extrairMarcadores(assunto).keySet());

        return new TemplatePreviewResponse(
                renderer.renderizar(assunto, variaveis),
                renderer.renderizar(corpo, variaveis),
                marcadores,
                usouPadrao);
    }

    /**
     * Um template nao pode PEDIR dado sensivel. Bloquear no cadastro e' melhor
     * do que bloquear no envio: o operador descobre o problema na hora.
     */
    private void validarCorpo(String corpo) {
        if (corpo == null) {
            return;
        }
        Map<String, String> marcadores = renderer.extrairMarcadores(corpo);
        Map<String, String> falsos = marcadores.keySet().stream()
                .collect(java.util.stream.Collectors.toMap(k -> k, k -> ""));
        try {
            renderer.validarVariaveis(falsos);
        } catch (NotificacaoRenderer.VariavelProibidaException e) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Template rejeitado: " + e.getMessage());
        }
    }

    private Map<String, String> exemplo() {
        return Map.of(
                "aluno", "Maria Silva",
                "hora", "07:42",
                "unidade", "Unidade Centro",
                "data", "18/09/2026",
                "destinatario", "Responsável",
                "pessoa", "João Souza");
    }

    private void aplicar(AccNotificacaoTemplate template, NotificacaoTemplateRequest request) {
        template.setEvento(request.evento());
        template.setCanal(request.canal());
        template.setAssunto(request.assunto());
        template.setCorpo(request.corpo());
        template.setTemplateExterno(request.templateExterno());
        if (request.ativo() != null) {
            template.setAtivo(request.ativo());
        }
    }

    private AccNotificacaoTemplate carregar(UUID id) {
        UUID tenantId = tenantObrigatorio();
        return templateRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Template de notificação não encontrado"));
    }

    private UUID tenantObrigatorio() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tenant não identificado na requisição");
        }
        return tenantId;
    }
}
