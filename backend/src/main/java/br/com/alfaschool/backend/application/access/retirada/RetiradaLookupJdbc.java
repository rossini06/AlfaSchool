package br.com.alfaschool.backend.application.access.retirada;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

/**
 * Consultas de leitura que atravessam tabelas de outras fatias
 * (autorizacoes, matriculas, salas).
 *
 * Feito em SQL e nao via repositorios JPA daquelas fatias de proposito:
 * esta fatia nao pode depender das entidades delas, e o que se precisa aqui
 * sao ids, nao objetos.
 */
@Component
public class RetiradaLookupJdbc implements AlunosAutorizadosPort, ContextoAlunoPort {

    private static final Logger log = LoggerFactory.getLogger(RetiradaLookupJdbc.class);

    /** Horario da escola, nao do servidor: o "dia" da fila e' o dia letivo local. */
    public static final ZoneId ZONA = ZoneId.of("America/Sao_Paulo");

    private final JdbcTemplate jdbc;

    public RetiradaLookupJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Pre-filtro barato: autorizacoes ATIVAS, dentro da vigencia e do dia da
     * semana. Faixa de horario e restricao judicial ficam para o
     * AutorizacaoPort, que e' quem decide.
     *
     * Falha fechada: se a consulta quebrar, devolve lista vazia. Nenhuma
     * retirada aberta e' melhor do que uma retirada aberta por engano.
     */
    @Override
    public List<UUID> alunosCandidatos(UUID tenantId, UUID pessoaAutorizadaId, Instant momento) {
        if (tenantId == null || pessoaAutorizadaId == null) {
            return List.of();
        }
        LocalDate dia = LocalDate.ofInstant(momento, ZONA);
        // 1=segunda ... 7=domingo, mesma convencao de dias_semana no schema
        int diaSemana = DayOfWeek.from(dia).getValue();
        String sql = """
                SELECT DISTINCT a.aluno_id
                  FROM acc_autorizacoes_retirada a
                 WHERE a.tenant_id = ?
                   AND a.pessoa_autorizada_id = ?
                   AND a.deleted = FALSE
                   AND a.status = 'ATIVA'
                   AND (a.vigencia_inicio IS NULL OR a.vigencia_inicio <= ?)
                   AND (a.vigencia_fim    IS NULL OR a.vigencia_fim    >= ?)
                   AND (a.dias_semana IS NULL OR a.dias_semana = ''
                        OR FIND_IN_SET(?, a.dias_semana) > 0)
                """;
        try {
            return jdbc.query(sql,
                    (rs, i) -> uuid(rs.getString(1)),
                    tenantId.toString(), pessoaAutorizadaId.toString(),
                    dia, dia, String.valueOf(diaSemana))
                    .stream().filter(java.util.Objects::nonNull).toList();
        } catch (Exception e) {
            log.error("Falha ao listar alunos candidatos da pessoa {} (tenant {})", pessoaAutorizadaId, tenantId, e);
            return List.of();
        }
    }

    /**
     * Turma vem da matricula ativa; sala vem do vinculo turma-sala vigente
     * naquele dia e naquela hora (uma turma pode trocar de sala ao longo do
     * dia). unitId vem da matricula e, faltando, do proprio aluno.
     */
    @Override
    public ContextoAluno contextoDe(UUID tenantId, UUID alunoId, Instant momento) {
        if (tenantId == null || alunoId == null) {
            return ContextoAluno.vazio();
        }
        LocalDate dia = LocalDate.ofInstant(momento, ZONA);
        LocalTime hora = LocalTime.ofInstant(momento, ZONA);
        int diaSemana = DayOfWeek.from(dia).getValue();
        String sql = """
                SELECT m.turma_id,
                       COALESCE(m.unit_id, al.unit_id) AS unit_id,
                       (SELECT ts.sala_id
                          FROM acc_turma_salas ts
                         WHERE ts.tenant_id = m.tenant_id
                           AND ts.turma_id  = m.turma_id
                           AND ts.deleted   = FALSE
                           AND ts.vigencia_inicio <= ?
                           AND (ts.vigencia_fim IS NULL OR ts.vigencia_fim >= ?)
                           AND (ts.hora_inicio IS NULL OR ts.hora_inicio <= ?)
                           AND (ts.hora_fim    IS NULL OR ts.hora_fim    >= ?)
                           AND (ts.dias_semana IS NULL OR ts.dias_semana = ''
                                OR FIND_IN_SET(?, ts.dias_semana) > 0)
                         ORDER BY ts.vigencia_inicio DESC
                         LIMIT 1) AS sala_id
                  FROM matriculas m
                  JOIN alunos al ON al.id = m.aluno_id
                 WHERE m.tenant_id = ?
                   AND m.aluno_id  = ?
                   AND m.deleted   = FALSE
                   AND m.status    = 'ativa'
                 ORDER BY m.data_matricula DESC
                 LIMIT 1
                """;
        try {
            List<ContextoAluno> achados = jdbc.query(sql,
                    (rs, i) -> new ContextoAluno(
                            uuid(rs.getString("unit_id")),
                            uuid(rs.getString("turma_id")),
                            uuid(rs.getString("sala_id"))),
                    dia, dia, hora, hora, String.valueOf(diaSemana),
                    tenantId.toString(), alunoId.toString());
            return achados.isEmpty() ? ContextoAluno.vazio() : achados.get(0);
        } catch (Exception e) {
            // Contexto e' enriquecimento: sem ele a retirada ainda vale, so'
            // nao aparece na TV da sala. Nao vale derrubar a portaria.
            log.warn("Falha ao resolver turma/sala do aluno {} (tenant {})", alunoId, tenantId, e);
            return ContextoAluno.vazio();
        }
    }

    static UUID uuid(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(valor);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
