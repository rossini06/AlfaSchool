package br.com.alfaschool.backend.application.access.permanencia;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Nome do aluno, da turma e da sala para o painel "Presentes Agora".
 *
 * <h2>Por que JDBC e por que em lote</h2>
 * O painel mostra a escola inteira e recarrega sozinho a cada minuto.
 * Resolver nome por linha seriam tres consultas por aluno, varias vezes
 * por minuto — e atravessando fatias (matriculas, turmas, salas) que esta
 * aqui nao deve depender como entidade. Uma consulta por recarga resolve.
 */
@Component
public class PresentesLookupJdbc {

    /** Nome resolvido de um aluno presente. Campos nulos quando nao ha matricula vigente. */
    public record Contexto(String alunoNome, UUID turmaId, String turmaNome, UUID salaId, String salaNome) {
        static Contexto vazio() {
            return new Contexto(null, null, null, null, null);
        }
    }

    private final JdbcTemplate jdbc;

    public PresentesLookupJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Map<UUID, Contexto> de(UUID tenantId, List<UUID> alunoIds, LocalDate dia) {
        if (tenantId == null || alunoIds == null || alunoIds.isEmpty()) {
            return Map.of();
        }
        String marcadores = String.join(",", java.util.Collections.nCopies(alunoIds.size(), "?"));
        int diaSemana = DayOfWeek.from(dia).getValue();

        // A sala sai do vinculo turma-sala vigente no dia. A escola pode ter
        // a mesma turma em salas diferentes ao longo do ano, e o painel tem
        // de mostrar onde a crianca esta HOJE.
        String sql = """
                SELECT al.id            AS aluno_id,
                       al.nome          AS aluno_nome,
                       m.turma_id       AS turma_id,
                       t.nome           AS turma_nome,
                       s.id             AS sala_id,
                       s.nome           AS sala_nome
                  FROM alunos al
                  LEFT JOIN matriculas m
                         ON m.aluno_id = al.id
                        AND m.tenant_id = al.tenant_id
                        AND m.deleted = FALSE
                        AND m.status = 'ativa'
                  LEFT JOIN turmas t
                         ON t.id = m.turma_id AND t.deleted = FALSE
                  LEFT JOIN acc_turma_salas ts
                         ON ts.turma_id = m.turma_id
                        AND ts.tenant_id = al.tenant_id
                        AND ts.deleted = FALSE
                        AND ts.vigencia_inicio <= ?
                        AND (ts.vigencia_fim IS NULL OR ts.vigencia_fim >= ?)
                        AND (ts.dias_semana IS NULL OR ts.dias_semana = ''
                             OR FIND_IN_SET(?, ts.dias_semana) > 0)
                  LEFT JOIN acc_salas s
                         ON s.id = ts.sala_id AND s.deleted = FALSE
                 WHERE al.tenant_id = ?
                   AND al.deleted = FALSE
                   AND al.id IN (%s)
                """.formatted(marcadores);

        Object[] args = new Object[4 + alunoIds.size()];
        args[0] = dia;
        args[1] = dia;
        args[2] = diaSemana;
        args[3] = tenantId.toString();
        for (int i = 0; i < alunoIds.size(); i++) {
            args[4 + i] = alunoIds.get(i).toString();
        }

        Map<UUID, Contexto> mapa = new HashMap<>();
        jdbc.query(sql, rs -> {
            UUID alunoId = UUID.fromString(rs.getString("aluno_id"));
            // Um aluno com mais de uma matricula ativa apareceria duas vezes.
            // A primeira linha basta: o painel mostra uma turma por crianca.
            mapa.putIfAbsent(alunoId, new Contexto(
                    rs.getString("aluno_nome"),
                    rs.getString("turma_id") == null ? null : UUID.fromString(rs.getString("turma_id")),
                    rs.getString("turma_nome"),
                    rs.getString("sala_id") == null ? null : UUID.fromString(rs.getString("sala_id")),
                    rs.getString("sala_nome")));
        }, args);

        for (UUID id : alunoIds) {
            mapa.putIfAbsent(id, Contexto.vazio());
        }
        return mapa;
    }
}
