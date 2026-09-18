package br.com.alfaschool.backend.application.access.portal;

import br.com.alfaschool.backend.domain.user.UserAccount;
import br.com.alfaschool.backend.infrastructure.persistence.repository.UserRepository;
import br.com.alfaschool.backend.security.filter.TenantContext;
import br.com.alfaschool.backend.security.jwt.AuthenticatedUser;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Descobre QUEM esta pedindo e QUAIS alunos essa pessoa pode ver.
 *
 * <h2>Isolamento (a regra mais importante do portal)</h2>
 * A lista de alunos vem SEMPRE do usuario autenticado. Nenhum id de responsavel
 * vindo do request entra nessa conta. Se a lista viesse do request, trocar um
 * UUID na URL daria a qualquer responsavel a rotina diaria de qualquer crianca
 * da escola — horario de entrada, horario de saida, quem busca. E' o pior
 * vazamento possivel neste produto.
 *
 * <h2>Como o vinculo e' resolvido</h2>
 * Por ordem, e a primeira que responder decide:
 * <ol>
 *   <li><b>{@code responsaveis.user_id}</b> (V44) — o vinculo real, por id.
 *       Quando existe, NADA mais e' consultado.</li>
 *   <li><b>E-mail</b>, apenas para a base ainda nao migrada, e somente
 *       quando o e-mail aponta para UM unico responsavel.</li>
 * </ol>
 *
 * <p>E-mail que casa com mais de um responsavel e' <b>recusado</b>, nao
 * somado. Antes da V44 esse caso — casal que divide a mesma caixa — fazia
 * cada um enxergar os filhos do outro. Somar era o comportamento errado:
 * na duvida sobre quem e' a pessoa, o portal nao mostra crianca nenhuma.
 */
@Service
public class PortalIdentidadeService {

    private static final Logger log = LoggerFactory.getLogger(PortalIdentidadeService.class);

    private final UserRepository userRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public PortalIdentidadeService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** Identidade do responsavel autenticado, ja com o escopo de alunos. */
    public record PortalIdentidade(
            UUID tenantId,
            UUID userId,
            String email,
            Set<UUID> responsavelIds,
            Set<UUID> alunoIds
    ) {
    }

    @Transactional(readOnly = true)
    public PortalIdentidade resolver() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthenticatedUser usuario)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não autenticado");
        }

        UUID tenantId = TenantContext.getTenantId() != null ? TenantContext.getTenantId() : usuario.tenantId();
        if (tenantId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tenant não identificado na requisição");
        }

        UserAccount conta = userRepository.findByIdAndTenantId(usuario.userId(), tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Usuário não pertence a este tenant"));

        String email = conta.getEmail();
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Usuário sem e-mail cadastrado não pode acessar o portal da família");
        }

        // 1. Vinculo por id. Existindo, e' a palavra final.
        Set<UUID> responsavelIds = buscarResponsavelIdsPorUsuario(tenantId, usuario.userId());

        // 2. So' entao o e-mail, para a base ainda nao migrada.
        if (responsavelIds.isEmpty()) {
            Set<UUID> porEmail = buscarResponsavelIdsPorEmail(tenantId, email);
            if (porEmail.size() > 1) {
                // Ambiguo: mais de um responsavel com este e-mail. Somar os
                // dois faria cada um enxergar os filhos do outro.
                log.warn("E-mail {} casa com {} responsaveis no tenant {}. Portal recusado ate que "
                        + "responsaveis.user_id seja preenchido para cada um.",
                        email, porEmail.size(), tenantId);
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Este e-mail está cadastrado para mais de um responsável. "
                      + "Peça à secretaria para vincular o seu acesso ao seu cadastro.");
            }
            responsavelIds = porEmail;
            if (!responsavelIds.isEmpty()) {
                log.info("Usuario {} entrou no portal pelo e-mail, sem vinculo por id. "
                        + "Preencha responsaveis.user_id para tornar o vinculo estavel.", usuario.userId());
            }
        }

        if (responsavelIds.isEmpty()) {
            // Falha fechada: sem vinculo, sem portal.
            log.info("Usuario {} sem responsavel correspondente no tenant {}", usuario.userId(), tenantId);
            return new PortalIdentidade(tenantId, usuario.userId(), email, Set.of(), Set.of());
        }

        Set<UUID> alunoIds = buscarAlunoIds(tenantId, responsavelIds);
        return new PortalIdentidade(tenantId, usuario.userId(), email, responsavelIds, alunoIds);
    }

    /**
     * Porta de entrada de todo endpoint com {@code /aluno/{id}}.
     *
     * <p>Devolve 404 (e nao 403) quando o aluno nao e' do responsavel: um 403
     * confirmaria que aquele UUID existe na escola, o que ja e' informacao
     * demais para quem esta sondando ids.
     */
    public void exigirAcessoAoAluno(PortalIdentidade identidade, UUID alunoId) {
        if (alunoId == null || !identidade.alunoIds().contains(alunoId)) {
            log.warn("Acesso negado no portal: usuario {} tentou ver aluno {}", identidade.userId(), alunoId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Aluno não encontrado para este responsável");
        }
    }

    /** O vinculo real (V44). Sem ambiguidade possivel: user_id e' unico. */
    private Set<UUID> buscarResponsavelIdsPorUsuario(UUID tenantId, UUID userId) {
        @SuppressWarnings("unchecked")
        List<String> ids = entityManager.createNativeQuery("""
                        SELECT CAST(r.id AS CHAR(36))
                        FROM responsaveis r
                        WHERE r.tenant_id = :tenantId
                          AND r.user_id = :userId
                          AND r.deleted = FALSE
                        """)
                .setParameter("tenantId", tenantId.toString())
                .setParameter("userId", userId.toString())
                .getResultList();
        return paraUuids(ids);
    }

    private Set<UUID> buscarResponsavelIdsPorEmail(UUID tenantId, String email) {
        @SuppressWarnings("unchecked")
        List<String> ids = entityManager.createNativeQuery("""
                        SELECT CAST(r.id AS CHAR(36))
                        FROM responsaveis r
                        WHERE r.tenant_id = :tenantId
                          AND LOWER(r.email) = LOWER(:email)
                          AND r.deleted = FALSE
                        """)
                .setParameter("tenantId", tenantId.toString())
                .setParameter("email", email)
                .getResultList();
        return paraUuids(ids);
    }

    /**
     * Alunos vinculados. Cobre os dois modelos que convivem no schema: a tabela
     * de juncao {@code aluno_responsaveis} (V20) e o campo legado
     * {@code responsaveis.aluno_id} (V13), que ainda tem dado antigo.
     */
    private Set<UUID> buscarAlunoIds(UUID tenantId, Set<UUID> responsavelIds) {
        List<String> textoIds = responsavelIds.stream().map(UUID::toString).toList();

        @SuppressWarnings("unchecked")
        List<String> porJuncao = entityManager.createNativeQuery("""
                        SELECT DISTINCT CAST(ar.aluno_id AS CHAR(36))
                        FROM aluno_responsaveis ar
                        WHERE ar.tenant_id = :tenantId
                          AND ar.responsavel_id IN (:responsavelIds)
                          AND ar.deleted = FALSE
                        """)
                .setParameter("tenantId", tenantId.toString())
                .setParameter("responsavelIds", textoIds)
                .getResultList();

        @SuppressWarnings("unchecked")
        List<String> porLegado = entityManager.createNativeQuery("""
                        SELECT DISTINCT CAST(r.aluno_id AS CHAR(36))
                        FROM responsaveis r
                        WHERE r.tenant_id = :tenantId
                          AND r.id IN (:responsavelIds)
                          AND r.aluno_id IS NOT NULL
                          AND r.deleted = FALSE
                        """)
                .setParameter("tenantId", tenantId.toString())
                .setParameter("responsavelIds", textoIds)
                .getResultList();

        Set<UUID> todos = new LinkedHashSet<>(paraUuids(porJuncao));
        todos.addAll(paraUuids(porLegado));
        return todos;
    }

    private Set<UUID> paraUuids(List<String> ids) {
        Set<UUID> saida = new LinkedHashSet<>();
        for (Object id : ids) {
            if (id == null) {
                continue;
            }
            try {
                saida.add(UUID.fromString(id.toString().trim()));
            } catch (IllegalArgumentException e) {
                log.warn("Id invalido vindo do banco no portal: {}", id);
            }
        }
        return saida;
    }
}
