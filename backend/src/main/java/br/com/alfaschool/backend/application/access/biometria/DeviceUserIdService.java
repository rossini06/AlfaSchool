package br.com.alfaschool.backend.application.access.biometria;

import br.com.alfaschool.backend.domain.access.biometria.AccDeviceUserSeq;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccDeviceUserSeqRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/**
 * Aloca o proximo device_user_id de um tenant.
 *
 * Dois pontos que parecem detalhe e nao sao:
 *
 * 1. A sequencia e' POR TENANT. Usar o id da pessoa, ou um contador
 *    global, quebra no dia em que duas escolas do grupo dividem o mesmo
 *    leitor: o usuario 1041 de uma vira o usuario 1041 da outra e as
 *    faces se sobrepoem dentro do equipamento.
 *
 * 2. A alocacao e' serializada por lock de linha. Ler o valor e depois
 *    gravar valor+1 funciona ate' duas matriculas simultaneas lerem o
 *    mesmo numero — e ai duas criancas viram a mesma pessoa na catraca.
 */
@Service
public class DeviceUserIdService {

    /**
     * O firmware reserva os primeiros ids para uso proprio em alguns
     * modelos; comecar longe disso evita colidir com o usuario de
     * fabrica sem precisar descobrir o limite de cada modelo.
     */
    public static final long PRIMEIRO_ID = 1000L;

    private final AccDeviceUserSeqRepository sequencias;

    public DeviceUserIdService(AccDeviceUserSeqRepository sequencias) {
        this.sequencias = sequencias;
    }

    /**
     * Roda em REQUIRES (participa da transacao do cadastro): o id
     * alocado tem de sumir junto se o cadastro da face falhar, senao a
     * sequencia vira um buraco a cada erro de validacao.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public long proximo(UUID tenantId) {
        Optional<AccDeviceUserSeq> existente = sequencias.travarPorTenant(tenantId);
        AccDeviceUserSeq seq;
        if (existente.isPresent()) {
            seq = existente.get();
        } else {
            seq = new AccDeviceUserSeq();
            seq.setTenantId(tenantId);
            seq.setProximoId(PRIMEIRO_ID);
        }
        long alocado = seq.getProximoId() == null ? PRIMEIRO_ID : seq.getProximoId();
        if (alocado < PRIMEIRO_ID) {
            alocado = PRIMEIRO_ID;
        }
        seq.setProximoId(alocado + 1);
        sequencias.save(seq);
        return alocado;
    }
}
