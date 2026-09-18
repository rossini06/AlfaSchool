package br.com.alfaschool.backend.application.access.permanencia;

import br.com.alfaschool.backend.domain.access.shared.SentidoAcesso;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Leitura dos eventos brutos de acesso (acc_eventos) para o motor de
 * permanencia.
 *
 * Existe como porta, e nao como repository JPA, de proposito: acc_eventos
 * e' o livro-razao de outro modulo e nao pertence a esta fatia. Aqui so'
 * se LE, e so' o recorte que interessa — aluno, PERMITIDO, um dia.
 */
public interface EventoAcessoLeitor {

    /** Recorte minimo de um evento para parear o dia. */
    record EventoAcesso(UUID id, Instant dataHora, UUID unitId, SentidoAcesso sentido) {
    }

    /**
     * Eventos PERMITIDOS do aluno no dia, ordenados por data_hora crescente.
     * O dia e' o dia civil em America/Sao_Paulo.
     */
    List<EventoAcesso> eventosDoDia(UUID tenantId, UUID alunoId, LocalDate dia);

    /** Alunos com ao menos um evento PERMITIDO no dia — base do reprocessamento. */
    List<UUID> alunosComEventoNoDia(UUID tenantId, LocalDate dia);

    /** Tenants com ao menos um evento PERMITIDO de aluno no dia. */
    List<UUID> tenantsComEventoNoDia(LocalDate dia);
}
