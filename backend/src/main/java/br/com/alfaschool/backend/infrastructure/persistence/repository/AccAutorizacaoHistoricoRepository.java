package br.com.alfaschool.backend.infrastructure.persistence.repository;

import br.com.alfaschool.backend.domain.access.autorizacao.AutorizacaoHistorico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * APPEND-ONLY. Use apenas save(novo) e as consultas abaixo.
 *
 * JpaRepository herda os metodos de remocao, mas AutorizacaoHistorico nao
 * tem setters e todas as suas colunas sao updatable=false,
 * entao nao ha caminho normal para reescrever a trilha. Nunca adicione um
 * metodo de delete ou de update aqui: essa trilha e' a prova de quem
 * liberou a retirada de uma crianca.
 */
@Repository
public interface AccAutorizacaoHistoricoRepository extends JpaRepository<AutorizacaoHistorico, UUID> {

    List<AutorizacaoHistorico> findByTenantIdAndAutorizacaoIdOrderByCreatedAtAsc(UUID tenantId, UUID autorizacaoId);
}
