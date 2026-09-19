package br.com.alfaschool.backend.application.access.relatorio;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Os sete relatorios do modulo de acesso.
 *
 * <h2>Por que nao existiam</h2>
 * A tela de Relatorios declarava sete abas, cada uma com suas colunas e
 * seus filtros, e chamava {@code /access/relatorios/{chave}}. Nao havia
 * controller nenhum: as sete abas respondiam 404 e o botao de exportar
 * ficava permanentemente desabilitado.
 *
 * <h2>Por que SQL e nao repositorios JPA</h2>
 * Todo relatorio atravessa fatias — evento de portaria com nome de aluno,
 * de turma, de portaria e de equipamento. Montar isso por entidades
 * significaria esta fatia depender das entidades de todas as outras, e
 * carregar objetos inteiros para exibir seis colunas.
 *
 * <h2>Os apelidos das colunas SAO o contrato</h2>
 * A tela renderiza por chave (`alunoNome`, `dataHora`) e o CSV exporta as
 * mesmas colunas. Renomear um apelido aqui esvazia uma coluna na tela sem
 * quebrar nada — por isso eles acompanham exatamente o que
 * RelatoriosAccessPage declara.
 */
@Component
public class RelatorioAccessJdbc {

    private final JdbcTemplate jdbc;

    public RelatorioAccessJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Filtros comuns; os nao aplicaveis a um relatorio chegam nulos. */
    public record Filtros(LocalDate inicio, LocalDate fim, UUID turmaId, UUID alunoId, UUID portariaId) {
    }

    // =================================================================
    // 1. Movimentacoes — toda passagem lida no periodo
    // =================================================================
    public List<Map<String, Object>> movimentacoes(UUID tenantId, Filtros f) {
        StringBuilder sql = new StringBuilder("""
                SELECT e.data_hora            AS dataHora,
                       al.nome                AS alunoNome,
                       t.nome                 AS turmaNome,
                       CASE e.sentido
                            WHEN 'ENTRADA' THEN 'Entrada'
                            WHEN 'SAIDA'   THEN 'Saída'
                            ELSE 'Indefinido'
                       END                    AS tipo,
                       p.nome                 AS portariaNome,
                       d.nome                 AS equipamentoNome
                  FROM acc_eventos e
                  LEFT JOIN alunos al       ON al.id = e.titular_id AND al.deleted = FALSE
                  LEFT JOIN matriculas m    ON m.aluno_id = al.id AND m.deleted = FALSE AND m.status = 'ativa'
                  LEFT JOIN turmas t        ON t.id = m.turma_id AND t.deleted = FALSE
                  LEFT JOIN acc_portarias p ON p.id = e.portaria_id AND p.deleted = FALSE
                  LEFT JOIN dispositivos d  ON d.id = e.dispositivo_id AND d.deleted = FALSE
                 WHERE e.tenant_id = ?
                   AND e.titular_tipo = 'ALUNO'
                   AND DATE(e.data_hora) BETWEEN ? AND ?
                """);
        List<Object> args = new ArrayList<>(List.of(tenantId.toString(), f.inicio(), f.fim()));
        if (f.alunoId() != null) { sql.append(" AND e.titular_id = ? "); args.add(f.alunoId().toString()); }
        if (f.turmaId() != null) { sql.append(" AND m.turma_id = ? "); args.add(f.turmaId().toString()); }
        if (f.portariaId() != null) { sql.append(" AND e.portaria_id = ? "); args.add(f.portariaId().toString()); }
        sql.append(" ORDER BY e.data_hora DESC LIMIT 5000");
        return jdbc.queryForList(sql.toString(), args.toArray());
    }

    // =================================================================
    // 2. Permanencia por aluno — consolidado do periodo
    // =================================================================
    public List<Map<String, Object>> permanencia(UUID tenantId, Filtros f) {
        StringBuilder sql = new StringBuilder("""
                SELECT al.nome                                  AS alunoNome,
                       t.nome                                   AS turmaNome,
                       COUNT(*)                                 AS diasApurados,
                       COALESCE(SUM(pr.minutos_permanencia), 0) AS minutosRealizados,
                       COALESCE(SUM(pr.minutos_previstos), 0)   AS minutosContratados,
                       COALESCE(SUM(pr.minutos_excedente), 0)   AS excedenteMinutos,
                       SUM(pr.status = 'INCONSISTENTE')         AS diasInconsistentes
                  FROM acc_presencas pr
                  JOIN alunos al         ON al.id = pr.aluno_id AND al.deleted = FALSE
                  LEFT JOIN matriculas m ON m.aluno_id = al.id AND m.deleted = FALSE AND m.status = 'ativa'
                  LEFT JOIN turmas t     ON t.id = m.turma_id AND t.deleted = FALSE
                 WHERE pr.tenant_id = ?
                   AND pr.deleted = FALSE
                   AND pr.data BETWEEN ? AND ?
                """);
        List<Object> args = new ArrayList<>(List.of(tenantId.toString(), f.inicio(), f.fim()));
        if (f.alunoId() != null) { sql.append(" AND pr.aluno_id = ? "); args.add(f.alunoId().toString()); }
        if (f.turmaId() != null) { sql.append(" AND m.turma_id = ? "); args.add(f.turmaId().toString()); }
        sql.append(" GROUP BY al.id, al.nome, t.nome ORDER BY al.nome");
        return jdbc.queryForList(sql.toString(), args.toArray());
    }

    // =================================================================
    // 3. Excedentes — os dias que viram cobranca
    // =================================================================
    public List<Map<String, Object>> excedentes(UUID tenantId, Filtros f) {
        StringBuilder sql = new StringBuilder("""
                SELECT pr.data                AS data,
                       al.nome                AS alunoNome,
                       t.nome                 AS turmaNome,
                       j.nome                 AS jornadaNome,
                       j.regra_excedente      AS regraExcedente,
                       pr.minutos_excedente   AS excedenteMinutos
                  FROM acc_presencas pr
                  JOIN alunos al         ON al.id = pr.aluno_id AND al.deleted = FALSE
                  LEFT JOIN acc_jornadas j ON j.id = pr.jornada_id AND j.deleted = FALSE
                  LEFT JOIN matriculas m ON m.aluno_id = al.id AND m.deleted = FALSE AND m.status = 'ativa'
                  LEFT JOIN turmas t     ON t.id = m.turma_id AND t.deleted = FALSE
                 WHERE pr.tenant_id = ?
                   AND pr.deleted = FALSE
                   AND pr.data BETWEEN ? AND ?
                   AND pr.minutos_excedente > 0
                   -- Regra 6: dia inconsistente nao entra em total nenhum,
                   -- e este relatorio e' base de cobranca.
                   AND pr.status <> 'INCONSISTENTE'
                """);
        List<Object> args = new ArrayList<>(List.of(tenantId.toString(), f.inicio(), f.fim()));
        if (f.alunoId() != null) { sql.append(" AND pr.aluno_id = ? "); args.add(f.alunoId().toString()); }
        if (f.turmaId() != null) { sql.append(" AND m.turma_id = ? "); args.add(f.turmaId().toString()); }
        sql.append(" ORDER BY pr.data DESC, al.nome LIMIT 5000");
        return jdbc.queryForList(sql.toString(), args.toArray());
    }

    // =================================================================
    // 4. Retiradas — quem levou cada crianca
    // =================================================================
    public List<Map<String, Object>> retiradas(UUID tenantId, Filtros f) {
        StringBuilder sql = new StringBuilder("""
                SELECT r.entregue_em          AS dataHora,
                       al.nome                AS alunoNome,
                       COALESCE(pa.nome, r.observacao) AS pessoaNome,
                       pa.parentesco          AS parentesco,
                       p.nome                 AS portariaNome,
                       CASE
                            WHEN r.retirada_manual = TRUE THEN 'Manual (coordenação)'
                            WHEN r.autorizacao_id IS NOT NULL THEN 'Autorizada'
                            ELSE 'Sem autorização vinculada'
                       END                    AS statusAutorizacao
                  FROM acc_retiradas r
                  JOIN alunos al ON al.id = r.aluno_id AND al.deleted = FALSE
                  LEFT JOIN acc_pessoas_autorizadas pa ON pa.id = r.pessoa_autorizada_id AND pa.deleted = FALSE
                  LEFT JOIN acc_portarias p ON p.id = r.portaria_id AND p.deleted = FALSE
                 WHERE r.tenant_id = ?
                   AND r.deleted = FALSE
                   AND r.entregue_em IS NOT NULL
                   AND DATE(r.entregue_em) BETWEEN ? AND ?
                """);
        List<Object> args = new ArrayList<>(List.of(tenantId.toString(), f.inicio(), f.fim()));
        if (f.alunoId() != null) { sql.append(" AND r.aluno_id = ? "); args.add(f.alunoId().toString()); }
        if (f.turmaId() != null) { sql.append(" AND r.turma_id = ? "); args.add(f.turmaId().toString()); }
        if (f.portariaId() != null) { sql.append(" AND r.portaria_id = ? "); args.add(f.portariaId().toString()); }
        sql.append(" ORDER BY r.entregue_em DESC LIMIT 5000");
        return jdbc.queryForList(sql.toString(), args.toArray());
    }

    // =================================================================
    // 5. Tempo de espera — da chegada do responsavel ate' a entrega
    // =================================================================
    public List<Map<String, Object>> tempoEspera(UUID tenantId, Filtros f) {
        StringBuilder sql = new StringBuilder("""
                SELECT DATE(r.solicitado_em)  AS data,
                       al.nome                AS alunoNome,
                       r.solicitado_em        AS chamadoEm,
                       r.entregue_em          AS entregueEm,
                       TIMESTAMPDIFF(MINUTE, r.solicitado_em, r.entregue_em) AS esperaMinutos,
                       p.nome                 AS portariaNome
                  FROM acc_retiradas r
                  JOIN alunos al ON al.id = r.aluno_id AND al.deleted = FALSE
                  LEFT JOIN acc_portarias p ON p.id = r.portaria_id AND p.deleted = FALSE
                 WHERE r.tenant_id = ?
                   AND r.deleted = FALSE
                   AND r.entregue_em IS NOT NULL
                   AND DATE(r.solicitado_em) BETWEEN ? AND ?
                """);
        List<Object> args = new ArrayList<>(List.of(tenantId.toString(), f.inicio(), f.fim()));
        if (f.turmaId() != null) { sql.append(" AND r.turma_id = ? "); args.add(f.turmaId().toString()); }
        if (f.portariaId() != null) { sql.append(" AND r.portaria_id = ? "); args.add(f.portariaId().toString()); }
        sql.append(" ORDER BY esperaMinutos DESC, r.solicitado_em DESC LIMIT 5000");
        return jdbc.queryForList(sql.toString(), args.toArray());
    }

    // =================================================================
    // 6. Acessos negados — o que o leitor barrou
    // =================================================================
    public List<Map<String, Object>> acessosNegados(UUID tenantId, Filtros f) {
        StringBuilder sql = new StringBuilder("""
                SELECT e.data_hora            AS dataHora,
                       pa.nome                AS pessoaNome,
                       al.nome                AS alunoNome,
                       COALESCE(e.motivo, 'Não informado') AS motivo,
                       p.nome                 AS portariaNome,
                       d.nome                 AS equipamentoNome
                  FROM acc_eventos e
                  LEFT JOIN acc_pessoas_autorizadas pa
                         ON pa.id = e.titular_id AND e.titular_tipo = 'AUTORIZADA' AND pa.deleted = FALSE
                  LEFT JOIN alunos al
                         ON al.id = e.titular_id AND e.titular_tipo = 'ALUNO' AND al.deleted = FALSE
                  LEFT JOIN acc_portarias p ON p.id = e.portaria_id AND p.deleted = FALSE
                  LEFT JOIN dispositivos d  ON d.id = e.dispositivo_id AND d.deleted = FALSE
                 WHERE e.tenant_id = ?
                   AND e.resultado <> 'PERMITIDO'
                   AND DATE(e.data_hora) BETWEEN ? AND ?
                """);
        List<Object> args = new ArrayList<>(List.of(tenantId.toString(), f.inicio(), f.fim()));
        if (f.portariaId() != null) { sql.append(" AND e.portaria_id = ? "); args.add(f.portariaId().toString()); }
        sql.append(" ORDER BY e.data_hora DESC LIMIT 5000");
        return jdbc.queryForList(sql.toString(), args.toArray());
    }

    // =================================================================
    // 7. Ocorrencias — o que a operacao registrou e como tratou
    // =================================================================
    public List<Map<String, Object>> ocorrencias(UUID tenantId, Filtros f) {
        StringBuilder sql = new StringBuilder("""
                SELECT COALESCE(o.ocorrido_em, o.created_at) AS dataHora,
                       o.tipo                 AS tipo,
                       o.gravidade            AS gravidade,
                       al.nome                AS alunoNome,
                       o.status               AS status,
                       u.name                 AS tratadaPor
                  FROM acc_ocorrencias o
                  LEFT JOIN alunos al    ON al.id = o.aluno_id AND al.deleted = FALSE
                  LEFT JOIN users u      ON u.id = o.tratado_por_user_id
                  LEFT JOIN matriculas m ON m.aluno_id = al.id AND m.deleted = FALSE AND m.status = 'ativa'
                 WHERE o.tenant_id = ?
                   AND o.deleted = FALSE
                   AND DATE(COALESCE(o.ocorrido_em, o.created_at)) BETWEEN ? AND ?
                """);
        List<Object> args = new ArrayList<>(List.of(tenantId.toString(), f.inicio(), f.fim()));
        if (f.alunoId() != null) { sql.append(" AND o.aluno_id = ? "); args.add(f.alunoId().toString()); }
        if (f.turmaId() != null) { sql.append(" AND m.turma_id = ? "); args.add(f.turmaId().toString()); }
        sql.append(" ORDER BY dataHora DESC LIMIT 5000");
        return jdbc.queryForList(sql.toString(), args.toArray());
    }
}
