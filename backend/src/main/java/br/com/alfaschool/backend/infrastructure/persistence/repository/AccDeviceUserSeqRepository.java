package br.com.alfaschool.backend.infrastructure.persistence.repository;

import br.com.alfaschool.backend.domain.access.biometria.AccDeviceUserSeq;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccDeviceUserSeqRepository extends JpaRepository<AccDeviceUserSeq, UUID> {

    /**
     * SELECT ... FOR UPDATE na linha do tenant.
     *
     * Ler o valor e depois gravar valor+1 sem lock parece funcionar ate'
     * duas matriculas simultaneas pegarem o mesmo numero — e dai duas
     * criancas viram o mesmo usuario dentro da catraca. O lock
     * pessimista serializa a alocacao; o escopo e' UMA linha por tenant,
     * entao a contencao e' irrelevante na pratica.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from AccDeviceUserSeq s where s.tenantId = :tenantId")
    Optional<AccDeviceUserSeq> travarPorTenant(@Param("tenantId") UUID tenantId);

    /**
     * Alternativa sem ler-antes-de-escrever, para quando o banco nao
     * puder segurar lock de linha (replica de leitura, por exemplo).
     * O SET usa a propria coluna como origem, entao o incremento e'
     * atomico dentro do proprio SGBD.
     */
    @Modifying
    @Query("update AccDeviceUserSeq s set s.proximoId = s.proximoId + 1 where s.tenantId = :tenantId")
    int incrementar(@Param("tenantId") UUID tenantId);
}
