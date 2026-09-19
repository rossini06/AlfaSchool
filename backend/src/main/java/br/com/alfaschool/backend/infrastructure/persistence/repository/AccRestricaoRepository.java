package br.com.alfaschool.backend.infrastructure.persistence.repository;

import br.com.alfaschool.backend.domain.access.autorizacao.Restricao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
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
     * Listagem da tela, com os tres filtros que ela ja oferecia e que o
     * controller ignorava: aluno, situacao e busca por texto.
     *
     * A situacao NAO e' so' o booleano `ativo`: uma restricao com
     * vigencia_fim no passado continua com ativo=1 e ja nao bloqueia
     * ninguem. Por isso a data entra na condicao — senao a tela diria
     * "vigente" para medida vencida.
     */
    @Query("select r from Restricao r where r.tenantId = :tenantId and r.deleted = false "
         + "and (:alunoId is null or r.alunoId = :alunoId) "
         + "and (:q is null or lower(coalesce(r.pessoaNome, '')) like lower(concat('%', :q, '%')) "
         + "                or coalesce(r.pessoaCpf, '') like concat('%', :q, '%') "
         + "                or lower(coalesce(r.numeroProcesso, '')) like lower(concat('%', :q, '%')) "
         + "                or lower(coalesce(r.orgaoEmissor, '')) like lower(concat('%', :q, '%'))) "
         + "and (:vigentes is null "
         + "     or (:vigentes = true  and r.ativo = true  and r.vigenciaInicio <= :hoje "
         + "         and (r.vigenciaFim is null or r.vigenciaFim >= :hoje)) "
         + "     or (:vigentes = false and (r.ativo = false or r.vigenciaInicio > :hoje "
         + "         or (r.vigenciaFim is not null and r.vigenciaFim < :hoje))))")
    Page<Restricao> buscar(@Param("tenantId") UUID tenantId,
                           @Param("alunoId") UUID alunoId,
                           @Param("q") String q,
                           @Param("vigentes") Boolean vigentes,
                           @Param("hoje") java.time.LocalDate hoje,
                           Pageable pageable);

    /**
     * Base da verificacao de portaria. Traz as restricoes ATIVAS do aluno sem
     * filtrar por pessoa de proposito: o casamento e' feito em memoria por id
     * OU por CPF solto, e uma consulta que filtrasse so por
     * pessoa_autorizada_id perderia justamente a restricao contra alguem que
     * ainda nao esta cadastrado. Sao poucas linhas por aluno.
     */
    List<Restricao> findByTenantIdAndAlunoIdAndAtivoTrueAndDeletedFalse(UUID tenantId, UUID alunoId);
}
