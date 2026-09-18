package br.com.alfaschool.backend.domain.access.biometria;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/**
 * Sequencia de device_user_id POR TENANT.
 *
 * O Control iD identifica a pessoa por um inteiro. Usar o id global da
 * pessoa (ou um AUTO_INCREMENT compartilhado) quebra assim que duas
 * escolas dividem um equipamento: o user 1041 de uma vira o user 1041 da
 * outra e as faces se sobrepoem.
 *
 * O incremento e' feito em UPDATE ... SET proximo_id = proximo_id + 1
 * dentro da transacao, no repositorio — nunca com leitura-depois-escrita
 * no Java, que corre em duas requisicoes concorrentes.
 */
@Entity
@Table(name = "acc_device_user_seq")
public class AccDeviceUserSeq {

    @Id
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "proximo_id", nullable = false)
    private Long proximoId = 1L;

    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID v) { this.tenantId = v; }
    public Long getProximoId() { return proximoId; }
    public void setProximoId(Long v) { this.proximoId = v; }
}
