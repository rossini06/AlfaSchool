package br.com.alfaschool.backend.access.notificacao;

import br.com.alfaschool.backend.domain.access.notificacao.AccNotificacaoEnvio;
import br.com.alfaschool.backend.domain.access.notificacao.AccNotificacaoPreferencia;
import br.com.alfaschool.backend.domain.access.shared.CanalNotificacao;
import br.com.alfaschool.backend.domain.access.shared.EventoNotificacao;
import br.com.alfaschool.backend.domain.access.shared.StatusEnvio;
import br.com.alfaschool.backend.domain.access.shared.TitularTipo;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccNotificacaoPreferenciaRepository;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.UUID;

/**
 * Fabricas dos testes.
 *
 * <p>{@code id} e {@code createdAt} sao preenchidos por JPA e nao tem setter.
 * Nos testes usamos reflexao para simular uma linha que ja passou pelo banco.
 */
final class TestFixtures {

    private TestFixtures() {
    }

    static AccNotificacaoEnvio envio(UUID tenantId, CanalNotificacao canal, StatusEnvio status,
                                     int tentativas, Instant createdAt) {
        AccNotificacaoEnvio envio = new AccNotificacaoEnvio();
        envio.setTenantId(tenantId);
        envio.setCanal(canal);
        envio.setEvento(EventoNotificacao.ENTRADA_CONFIRMADA);
        envio.setDestino(canal == CanalNotificacao.EMAIL ? "mae@exemplo.com" : "27999990000");
        envio.setAssunto("Entrada registrada");
        envio.setCorpo("Maria entrou as 07:42");
        envio.setStatus(status);
        envio.setTentativas(tentativas);
        envio.setAgendadoPara(Instant.now());
        envio.setChaveIdempotencia("ENTRADA:" + UUID.randomUUID());
        ReflectionTestUtils.setField(envio, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(envio, "createdAt", createdAt);
        return envio;
    }

    static AccNotificacaoPreferencia preferencia(UUID tenantId, UUID titularId, CanalNotificacao canal,
                                                 boolean habilitado, Instant optInEm) {
        AccNotificacaoPreferencia p = new AccNotificacaoPreferencia();
        p.setTenantId(tenantId);
        p.setTitularTipo(TitularTipo.RESPONSAVEL);
        p.setTitularId(titularId);
        p.setCanal(canal);
        p.setDestino(canal == CanalNotificacao.EMAIL ? "mae@exemplo.com" : "27999990000");
        p.setHabilitado(habilitado);
        p.setOptInEm(optInEm);
        ReflectionTestUtils.setField(p, "id", UUID.randomUUID());
        return p;
    }

    /** Projection de destinatario como o banco devolveria. */
    record Destinatario(String titularId, String nome)
            implements AccNotificacaoPreferenciaRepository.DestinatarioProjection {
        @Override
        public String getTitularId() {
            return titularId;
        }

        @Override
        public String getNome() {
            return nome;
        }
    }
}
