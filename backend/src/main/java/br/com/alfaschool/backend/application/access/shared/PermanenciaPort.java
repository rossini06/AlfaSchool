package br.com.alfaschool.backend.application.access.shared;

import java.time.Instant;
import java.util.UUID;

/**
 * Registro de permanencia do aluno a partir dos eventos de acesso.
 *
 * REGRA INVIOLAVEL: a permanencia so' encerra em saida efetiva. O
 * reconhecimento do responsavel na portaria e a confirmacao de entrega
 * NAO chamam registrarSaida.
 */
public interface PermanenciaPort {

    void registrarEntrada(UUID tenantId, UUID alunoId, Instant momento, UUID eventoId);

    void registrarSaida(UUID tenantId, UUID alunoId, Instant momento, UUID eventoId);

    /** Recalcula o dia inteiro do zero a partir dos eventos. Idempotente. */
    void recalcularDia(UUID tenantId, UUID alunoId, java.time.LocalDate dia);
}
