package br.com.alfaschool.backend.application.access.portal;

import br.com.alfaschool.backend.domain.responsavel.Responsavel;
import br.com.alfaschool.backend.infrastructure.persistence.repository.ResponsavelRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.UserRepository;
import br.com.alfaschool.backend.security.filter.TenantContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Regras do vinculo acesso <-> responsavel. Ver PortalVinculoController. */
@Service
public class PortalVinculoService {

    private static final Logger log = LoggerFactory.getLogger(PortalVinculoService.class);

    private final ResponsavelRepository responsavelRepository;
    private final UserRepository userRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public PortalVinculoService(ResponsavelRepository responsavelRepository, UserRepository userRepository) {
        this.responsavelRepository = responsavelRepository;
        this.userRepository = userRepository;
    }

    private UUID tenantObrigatorio() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tenant não identificado na requisição");
        }
        return tenantId;
    }

    /**
     * Quem ainda nao tem acesso vinculado, com o alerta de e-mail repetido —
     * que e' justamente quem esta impedido de entrar no portal.
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> pendentes() {
        UUID tenantId = tenantObrigatorio();
        @SuppressWarnings("unchecked")
        List<Object[]> linhas = entityManager.createNativeQuery("""
                        SELECT CAST(r.id AS CHAR(36)), r.nome, r.email,
                               (SELECT COUNT(*) FROM responsaveis o
                                 WHERE o.tenant_id = r.tenant_id
                                   AND o.deleted = FALSE
                                   AND o.email IS NOT NULL
                                   AND LOWER(o.email) = LOWER(r.email)) AS repetidos
                        FROM responsaveis r
                        WHERE r.tenant_id = :tenantId
                          AND r.deleted = FALSE
                          AND r.user_id IS NULL
                        ORDER BY r.nome
                        """)
                .setParameter("tenantId", tenantId.toString())
                .getResultList();

        return linhas.stream().map(l -> {
            long repetidos = l[3] == null ? 0L : ((Number) l[3]).longValue();
            return Map.<String, Object>of(
                    "responsavelId", l[0],
                    "nome", l[1] == null ? "" : l[1],
                    "email", l[2] == null ? "" : l[2],
                    // Sem e-mail ou com e-mail repetido, a pessoa NAO consegue
                    // entrar no portal ate alguem vincular o acesso aqui.
                    "bloqueadoNoPortal", l[2] == null || repetidos > 1);
        }).toList();
    }

    @Transactional
    public void vincular(UUID responsavelId, UUID userId) {
        UUID tenantId = tenantObrigatorio();

        Responsavel responsavel = responsavelRepository.findById(responsavelId)
                .filter(r -> tenantId.equals(r.getTenantId()) && !Boolean.TRUE.equals(r.getDeleted()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Responsável não encontrado"));

        // O usuario precisa ser do MESMO tenant. Sem esta checagem, vincular
        // um usuario de outra escola daria a ele a rotina diaria destas criancas.
        userRepository.findByIdAndTenantId(userId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Usuário não encontrado neste tenant"));

        responsavelRepository.findAll().stream()
                .filter(r -> userId.equals(r.getUserId()) && !r.getId().equals(responsavelId))
                .findFirst()
                .ifPresent(r -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Este usuário já está vinculado a outro responsável");
                });

        responsavel.setUserId(userId);
        responsavelRepository.save(responsavel);
        log.info("Acesso do usuario {} vinculado ao responsavel {} no tenant {}", userId, responsavelId, tenantId);
    }

    @Transactional
    public void desvincular(UUID responsavelId) {
        UUID tenantId = tenantObrigatorio();
        Responsavel responsavel = responsavelRepository.findById(responsavelId)
                .filter(r -> tenantId.equals(r.getTenantId()) && !Boolean.TRUE.equals(r.getDeleted()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Responsável não encontrado"));
        responsavel.setUserId(null);
        responsavelRepository.save(responsavel);
        log.info("Acesso desvinculado do responsavel {} no tenant {}", responsavelId, tenantId);
    }
}
