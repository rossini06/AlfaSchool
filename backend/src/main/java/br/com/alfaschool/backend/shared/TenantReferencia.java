package br.com.alfaschool.backend.shared;

import br.com.alfaschool.backend.domain.shared.BaseEntity;
import br.com.alfaschool.backend.security.filter.TenantContext;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * Confirma que uma referencia vinda do cliente (alunoId, turmaId, ...)
 * pertence ao tenant atual ANTES de grava-la.
 *
 * <p>Por que existe: o {@code @Filter} de tenant do Hibernate cobre queries
 * JPQL, mas NAO cobre {@code findById} (load-by-key). Sem esta checagem, um
 * POST /matriculas com o id de um aluno de OUTRA escola era aceito, e o nome
 * da crianca vazava depois no boletim, no dashboard e nos relatorios. A
 * verificacao usa o {@code getTenantId()} da propria entidade — o unico
 * caminho seguro para load-by-key.
 *
 * <p>Falha fechada: id de outra escola ou inexistente vira 400 indistinguivel,
 * para nao revelar quais ids existem em outros tenants.
 */
public final class TenantReferencia {

    private TenantReferencia() {
    }

    /** Nulo passa (campo opcional). Existe e e' do tenant: devolve. Senao, 400. */
    public static <T extends BaseEntity> T exigir(JpaRepository<T, UUID> repo, UUID id, String erro) {
        if (id == null) {
            return null;
        }
        UUID tenantId = TenantContext.getTenantId();
        return repo.findById(id)
                .filter(e -> tenantId != null
                        && tenantId.equals(e.getTenantId())
                        && !Boolean.TRUE.equals(e.getDeleted()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, erro));
    }
}
