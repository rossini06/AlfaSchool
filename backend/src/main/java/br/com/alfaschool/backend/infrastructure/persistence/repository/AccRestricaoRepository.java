package br.com.alfaschool.backend.infrastructure.persistence.repository;

import br.com.alfaschool.backend.domain.access.autorizacao.Restricao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccRestricaoRepository extends JpaRepository<Restricao, UUID> {

    Optional<Restricao> findByIdAndTenantIdAndDeletedFalse(UUID id, UUID tenantId);

    Page<Restricao> findByTenantIdAndDeletedFalse(UUID tenantId, Pageable pageable);

    List<Restricao> findByTenantIdAndAlunoIdAndDeletedFalse(UUID tenantId, UUID alunoId);

    /**
     * Base da verificacao de portaria. Traz as restricoes ATIVAS do aluno sem
     * filtrar por pessoa de proposito: o casamento e' feito em memoria por id
     * OU por CPF solto, e uma consulta que filtrasse so por
     * pessoa_autorizada_id perderia justamente a restricao contra alguem que
     * ainda nao esta cadastrado. Sao poucas linhas por aluno.
     */
    List<Restricao> findByTenantIdAndAlunoIdAndAtivoTrueAndDeletedFalse(UUID tenantId, UUID alunoId);
}
