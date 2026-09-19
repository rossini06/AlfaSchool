package br.com.alfaschool.backend.application.access.portal;

import br.com.alfaschool.backend.application.access.notificacao.dto.EnvioResponse;
import br.com.alfaschool.backend.domain.access.notificacao.AccNotificacaoEnvio;
import br.com.alfaschool.backend.application.access.portal.dto.PortalAlunoResumo;
import br.com.alfaschool.backend.application.access.portal.dto.PortalAutorizacaoResumo;
import br.com.alfaschool.backend.application.access.portal.dto.PortalPermanenciaDia;
import br.com.alfaschool.backend.application.access.portal.dto.PortalResumoDia;
import br.com.alfaschool.backend.application.access.portal.dto.PortalSolicitacaoAutorizacaoRequest;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccNotificacaoEnvioRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Portal da familia: somente leitura, exceto pelo PEDIDO de autorizacao.
 *
 * <p>Todo metodo comeca resolvendo a identidade do usuario autenticado e, nos
 * endpoints com {@code /aluno/{id}}, chama
 * {@link PortalIdentidadeService#exigirAcessoAoAluno}. O id do request e' um
 * FILTRO dentro do que a pessoa ja pode ver, nunca a fonte da permissao.
 */
@Service
public class PortalService {

    private final PortalIdentidadeService identidadeService;
    private final PortalPermanenciaPort permanenciaPort;
    private final PortalAutorizacaoPort autorizacaoPort;
    private final AccNotificacaoEnvioRepository envioRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public PortalService(PortalIdentidadeService identidadeService,
                         PortalPermanenciaPort permanenciaPort,
                         PortalAutorizacaoPort autorizacaoPort,
                         AccNotificacaoEnvioRepository envioRepository) {
        this.identidadeService = identidadeService;
        this.permanenciaPort = permanenciaPort;
        this.autorizacaoPort = autorizacaoPort;
        this.envioRepository = envioRepository;
    }

    @Transactional(readOnly = true)
    public List<PortalAlunoResumo> meusAlunos() {
        PortalIdentidadeService.PortalIdentidade id = identidadeService.resolver();
        if (id.alunoIds().isEmpty()) {
            return List.of();
        }

        List<Tuple> linhas = entityManager.createNativeQuery("""
                        SELECT CAST(a.id AS CHAR(36))      AS aluno_id,
                               a.nome                      AS nome,
                               CAST(a.unit_id AS CHAR(36)) AS unit_id,
                               ar.parentesco               AS parentesco,
                               ar.autorizado_buscar        AS autorizado_buscar
                        FROM alunos a
                        LEFT JOIN aluno_responsaveis ar
                               ON ar.aluno_id = a.id
                              AND ar.responsavel_id IN (:responsavelIds)
                              AND ar.deleted = FALSE
                        WHERE a.tenant_id = :tenantId
                          AND a.id IN (:alunoIds)
                          AND a.deleted = FALSE
                        ORDER BY a.nome
                        """, Tuple.class)
                .setParameter("tenantId", id.tenantId().toString())
                .setParameter("alunoIds", id.alunoIds().stream().map(UUID::toString).toList())
                .setParameter("responsavelIds", id.responsavelIds().stream().map(UUID::toString).toList())
                .getResultList();

        List<PortalAlunoResumo> saida = new ArrayList<>();
        for (Tuple t : linhas) {
            saida.add(new PortalAlunoResumo(
                    texto(t, "aluno_id") == null ? null : UUID.fromString(texto(t, "aluno_id")),
                    texto(t, "nome"),
                    texto(t, "unit_id") == null ? null : UUID.fromString(texto(t, "unit_id")),
                    texto(t, "parentesco"),
                    booleano(t, "autorizado_buscar")));
        }
        return saida;
    }

    @Transactional(readOnly = true)
    public PortalResumoDia hoje(UUID alunoId) {
        PortalIdentidadeService.PortalIdentidade id = identidadeService.resolver();
        identidadeService.exigirAcessoAoAluno(id, alunoId);
        return permanenciaPort.resumoDoDia(id.tenantId(), alunoId, LocalDate.now());
    }

    @Transactional(readOnly = true)
    public List<PortalPermanenciaDia> historico(UUID alunoId, LocalDate inicio, LocalDate fim) {
        PortalIdentidadeService.PortalIdentidade id = identidadeService.resolver();
        identidadeService.exigirAcessoAoAluno(id, alunoId);
        LocalDate ate = fim == null ? LocalDate.now() : fim;
        LocalDate de = inicio == null ? ate.minusDays(30) : inicio;
        return permanenciaPort.historico(id.tenantId(), alunoId, de, ate);
    }

    @Transactional(readOnly = true)
    public List<PortalAutorizacaoResumo> autorizacoes(UUID alunoId) {
        PortalIdentidadeService.PortalIdentidade id = identidadeService.resolver();
        identidadeService.exigirAcessoAoAluno(id, alunoId);
        return autorizacaoPort.listar(id.tenantId(), alunoId);
    }

    /**
     * Cria um PEDIDO. Origem PORTAL, status PENDENTE, nada liberado. A escola
     * aprova.
     */
    @Transactional
    public UUID solicitarAutorizacao(UUID alunoId, PortalSolicitacaoAutorizacaoRequest request) {
        PortalIdentidadeService.PortalIdentidade id = identidadeService.resolver();
        identidadeService.exigirAcessoAoAluno(id, alunoId);
        UUID solicitante = id.responsavelIds().stream().findFirst().orElse(null);
        return autorizacaoPort.solicitar(id.tenantId(), alunoId, solicitante, request);
    }

    /** Avisos que a escola mandou para este responsavel. */
    @Transactional(readOnly = true)
    public Page<EnvioResponse> minhasNotificacoes(boolean somenteNaoLidas, Pageable pageable) {
        PortalIdentidadeService.PortalIdentidade id = identidadeService.resolver();
        if (id.responsavelIds().isEmpty()) {
            return Page.empty(pageable);
        }
        Page<AccNotificacaoEnvio> pagina = somenteNaoLidas
                ? envioRepository.findByTenantIdAndTitularIdInAndLidaEmIsNullOrderByCreatedAtDesc(
                        id.tenantId(), id.responsavelIds(), pageable)
                : envioRepository.findByTenantIdAndTitularIdInOrderByCreatedAtDesc(
                        id.tenantId(), id.responsavelIds(), pageable);
        return pagina.map(EnvioResponse::from);
    }

    /**
     * Marca o aviso como lido pela familia.
     *
     * Confere o titular antes: um responsavel nao pode marcar como lido o
     * aviso de outra familia, ainda que descubra o id.
     */
    @Transactional
    public void marcarComoLida(UUID envioId) {
        PortalIdentidadeService.PortalIdentidade id = identidadeService.resolver();
        AccNotificacaoEnvio envio = envioRepository.findById(envioId)
                .filter(e -> id.tenantId().equals(e.getTenantId()))
                .filter(e -> e.getTitularId() != null && id.responsavelIds().contains(e.getTitularId()))
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Aviso não encontrado"));
        if (envio.getLidaEm() == null) {
            envio.setLidaEm(java.time.Instant.now());
            envioRepository.save(envio);
        }
    }

    private String texto(Tuple t, String coluna) {
        Object v = t.get(coluna);
        return v == null ? null : v.toString();
    }

    private boolean booleano(Tuple t, String coluna) {
        Object v = t.get(coluna);
        if (v == null) {
            return false;
        }
        if (v instanceof Boolean b) {
            return b;
        }
        if (v instanceof Number n) {
            return n.intValue() != 0;
        }
        return Boolean.parseBoolean(v.toString());
    }
}
