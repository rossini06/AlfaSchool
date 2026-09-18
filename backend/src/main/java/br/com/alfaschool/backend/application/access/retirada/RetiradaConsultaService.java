package br.com.alfaschool.backend.application.access.retirada;

import br.com.alfaschool.backend.application.access.retirada.dto.FilaFiltro;
import br.com.alfaschool.backend.application.access.retirada.dto.RetiradaDetalheResponse;
import br.com.alfaschool.backend.application.access.retirada.dto.RetiradaFilaItem;
import br.com.alfaschool.backend.application.access.retirada.dto.RetiradaHistoricoResponse;
import br.com.alfaschool.backend.domain.access.shared.StatusRetirada;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccRetiradaHistoricoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Leitura da fila. Tudo em UMA consulta agregada por chamada.
 *
 * Os nomes de aluno, turma, sala, portaria e responsavel vem resolvidos no
 * proprio SQL. A alternativa — carregar entidades e navegar relacoes — daria
 * um SELECT por linha da fila, e a fila lota justamente no horario de pico.
 *
 * SQL cru tambem evita que esta fatia dependa das entidades de matricula,
 * autorizacao e estrutura, que pertencem a outras fatias.
 */
@Service
public class RetiradaConsultaService {

    /**
     * Fotos como CHAVE de storage. Bytes de imagem nao entram em payload de
     * fila nem de SSE: trafegam pelo endpoint de midia, com autorizacao
     * propria.
     */
    private static final String SELECT_BASE = """
            SELECT r.id                 AS id,
                   r.unit_id            AS unit_id,
                   r.aluno_id           AS aluno_id,
                   al.nome              AS aluno_nome,
                   (SELECT f.foto_key FROM acc_faces f
                     WHERE f.tenant_id = r.tenant_id
                       AND f.titular_tipo = 'ALUNO'
                       AND f.titular_id = r.aluno_id
                       AND f.ativo = TRUE AND f.deleted = FALSE
                     ORDER BY f.updated_at DESC LIMIT 1) AS aluno_foto_key,
                   r.turma_id           AS turma_id,
                   t.nome               AS turma_nome,
                   r.sala_id            AS sala_id,
                   s.nome               AS sala_nome,
                   r.portaria_id        AS portaria_id,
                   pt.nome              AS portaria_nome,
                   r.pessoa_autorizada_id AS pessoa_autorizada_id,
                   pa.nome              AS pessoa_nome,
                   pa.foto_key          AS pessoa_foto_key,
                   (SELECT ar.parentesco FROM aluno_responsaveis ar
                     WHERE ar.tenant_id = r.tenant_id
                       AND ar.aluno_id = r.aluno_id
                       AND ar.responsavel_id = pa.responsavel_id
                       AND ar.deleted = FALSE
                     LIMIT 1)           AS parentesco,
                   r.status             AS status,
                   r.ordem_chegada      AS ordem_chegada,
                   r.solicitado_em      AS solicitado_em,
                   r.preparando_em      AS preparando_em,
                   r.pronto_em          AS pronto_em,
                   r.entregue_em        AS entregue_em,
                   r.saida_em           AS saida_em,
                   r.retirada_manual    AS retirada_manual,
                   r.motivo             AS motivo,
                   r.observacao         AS observacao
              FROM acc_retiradas r
              LEFT JOIN alunos                 al ON al.id = r.aluno_id
              LEFT JOIN turmas                 t  ON t.id  = r.turma_id
              LEFT JOIN acc_salas              s  ON s.id  = r.sala_id
              LEFT JOIN acc_portarias          pt ON pt.id = r.portaria_id
              LEFT JOIN acc_pessoas_autorizadas pa ON pa.id = r.pessoa_autorizada_id
            """;

    private final JdbcTemplate jdbc;
    private final AccRetiradaHistoricoRepository historicoRepository;

    public RetiradaConsultaService(JdbcTemplate jdbc, AccRetiradaHistoricoRepository historicoRepository) {
        this.jdbc = jdbc;
        this.historicoRepository = historicoRepository;
    }

    /**
     * Fila atual, ordenada por ordem de chegada. Sem filtro de status,
     * devolve so' quem ainda espera — entregue e cancelada nao ocupam lugar
     * na tela.
     */
    public List<RetiradaFilaItem> fila(UUID tenantId, FilaFiltro filtro) {
        StringBuilder sql = new StringBuilder(SELECT_BASE);
        List<Object> args = new ArrayList<>();
        sql.append(" WHERE r.tenant_id = ? AND r.deleted = FALSE ");
        args.add(tenantId.toString());

        Collection<StatusRetirada> status = (filtro.status() == null || filtro.status().isEmpty())
                ? RetiradaService.EM_ABERTO
                : filtro.status();
        sql.append(" AND r.status IN (").append(marcadores(status.size())).append(") ");
        status.forEach(s -> args.add(s.name()));

        // A fila e' do dia. Retirada de ontem que ninguem fechou e' problema
        // de relatorio, nao linha na tela da portaria de hoje.
        LocalDate hoje = LocalDate.now(RetiradaLookupJdbc.ZONA);
        sql.append(" AND r.solicitado_em >= ? AND r.solicitado_em < ? ");
        args.add(Timestamp.from(hoje.atStartOfDay(RetiradaLookupJdbc.ZONA).toInstant()));
        args.add(Timestamp.from(hoje.plusDays(1).atStartOfDay(RetiradaLookupJdbc.ZONA).toInstant()));

        if (filtro.unitId() != null) {
            sql.append(" AND r.unit_id = ? ");
            args.add(filtro.unitId().toString());
        }
        if (filtro.turmaId() != null) {
            sql.append(" AND r.turma_id = ? ");
            args.add(filtro.turmaId().toString());
        }
        if (filtro.salaId() != null) {
            sql.append(" AND r.sala_id = ? ");
            args.add(filtro.salaId().toString());
        }
        if (filtro.portariaId() != null) {
            sql.append(" AND r.portaria_id = ? ");
            args.add(filtro.portariaId().toString());
        }
        if (filtro.esperandoHaMinutos() != null && filtro.esperandoHaMinutos() > 0) {
            // Compara data com data: o corte e' calculado aqui e o banco so'
            // compara, o que deixa o indice de solicitado_em utilizavel.
            sql.append(" AND r.entregue_em IS NULL AND r.solicitado_em <= ? ");
            args.add(Timestamp.from(Instant.now().minus(Duration.ofMinutes(filtro.esperandoHaMinutos()))));
        }
        sql.append(" ORDER BY r.ordem_chegada ASC, r.solicitado_em ASC ");

        return jdbc.query(sql.toString(), mapper(Instant.now()), args.toArray());
    }

    /**
     * Fila restrita a um recorte de painel. Recebe as listas ja' resolvidas
     * pelas fontes do painel.
     *
     * Se o painel nao tiver nenhuma fonte, devolve VAZIO — nunca a escola
     * inteira. Painel mal configurado nao pode virar vazamento.
     */
    public List<RetiradaFilaItem> filaDoRecorte(UUID tenantId,
                                                UUID unitId,
                                                Collection<UUID> turmaIds,
                                                Collection<UUID> salaIds,
                                                Collection<UUID> portariaIds,
                                                boolean unidadeInteira) {
        boolean semRecorte = !unidadeInteira
                && (turmaIds == null || turmaIds.isEmpty())
                && (salaIds == null || salaIds.isEmpty())
                && (portariaIds == null || portariaIds.isEmpty());
        if (semRecorte) {
            return List.of();
        }

        StringBuilder sql = new StringBuilder(SELECT_BASE);
        List<Object> args = new ArrayList<>();
        sql.append(" WHERE r.tenant_id = ? AND r.deleted = FALSE ");
        args.add(tenantId.toString());

        sql.append(" AND r.status IN (").append(marcadores(RetiradaService.EM_ABERTO.size())).append(") ");
        RetiradaService.EM_ABERTO.forEach(s -> args.add(s.name()));

        LocalDate hoje = LocalDate.now(RetiradaLookupJdbc.ZONA);
        sql.append(" AND r.solicitado_em >= ? AND r.solicitado_em < ? ");
        args.add(Timestamp.from(hoje.atStartOfDay(RetiradaLookupJdbc.ZONA).toInstant()));
        args.add(Timestamp.from(hoje.plusDays(1).atStartOfDay(RetiradaLookupJdbc.ZONA).toInstant()));

        if (unidadeInteira) {
            sql.append(" AND r.unit_id = ? ");
            args.add(unitId == null ? null : unitId.toString());
        } else {
            List<String> ors = new ArrayList<>();
            if (salaIds != null && !salaIds.isEmpty()) {
                ors.add("r.sala_id IN (" + marcadores(salaIds.size()) + ")");
                salaIds.forEach(id -> args.add(id.toString()));
            }
            if (turmaIds != null && !turmaIds.isEmpty()) {
                ors.add("r.turma_id IN (" + marcadores(turmaIds.size()) + ")");
                turmaIds.forEach(id -> args.add(id.toString()));
            }
            if (portariaIds != null && !portariaIds.isEmpty()) {
                ors.add("r.portaria_id IN (" + marcadores(portariaIds.size()) + ")");
                portariaIds.forEach(id -> args.add(id.toString()));
            }
            sql.append(" AND (").append(String.join(" OR ", ors)).append(") ");
        }
        sql.append(" ORDER BY r.ordem_chegada ASC, r.solicitado_em ASC ");

        return jdbc.query(sql.toString(), mapper(Instant.now()), args.toArray());
    }

    /** Um cartao so', para o payload do evento ao vivo. */
    public RetiradaFilaItem porId(UUID tenantId, UUID retiradaId) {
        String sql = SELECT_BASE + " WHERE r.tenant_id = ? AND r.id = ? AND r.deleted = FALSE ";
        List<RetiradaFilaItem> achados = jdbc.query(sql, mapper(Instant.now()),
                tenantId.toString(), retiradaId.toString());
        return achados.isEmpty() ? null : achados.get(0);
    }

    public RetiradaDetalheResponse detalhe(UUID tenantId, UUID retiradaId) {
        RetiradaFilaItem item = porId(tenantId, retiradaId);
        if (item == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Retirada nao encontrada");
        }
        List<RetiradaHistoricoResponse> historico = historicoRepository
                .findByTenantIdAndRetiradaIdOrderByCreatedAtAsc(tenantId, retiradaId)
                .stream().map(RetiradaHistoricoResponse::from).toList();
        return new RetiradaDetalheResponse(item, historico);
    }

    /** Quem retirou cada aluno, no periodo. Base do relatorio de espera. */
    public List<RetiradaFilaItem> historicoDoAluno(UUID tenantId, UUID alunoId, Instant inicio, Instant fim) {
        StringBuilder sql = new StringBuilder(SELECT_BASE);
        List<Object> args = new ArrayList<>();
        sql.append(" WHERE r.tenant_id = ? AND r.deleted = FALSE ");
        args.add(tenantId.toString());
        if (alunoId != null) {
            sql.append(" AND r.aluno_id = ? ");
            args.add(alunoId.toString());
        }
        if (inicio != null) {
            sql.append(" AND r.solicitado_em >= ? ");
            args.add(Timestamp.from(inicio));
        }
        if (fim != null) {
            sql.append(" AND r.solicitado_em < ? ");
            args.add(Timestamp.from(fim));
        }
        sql.append(" ORDER BY r.solicitado_em DESC ");
        return jdbc.query(sql.toString(), mapper(Instant.now()), args.toArray());
    }

    /**
     * Marcadores de um IN. Com zero itens devolve NULL em vez de um "?"
     * solto: "IN (NULL)" nao casa com nada, enquanto um "?" sem argumento
     * correspondente estouraria em tempo de execucao.
     */
    private static String marcadores(int quantidade) {
        if (quantidade <= 0) {
            return "NULL";
        }
        return String.join(",", java.util.Collections.nCopies(quantidade, "?"));
    }

    /**
     * agora e' fixado uma vez por consulta: se cada linha lesse o relogio,
     * duas retiradas abertas no mesmo segundo poderiam mostrar minutos de
     * espera diferentes na mesma tela.
     */
    private RowMapper<RetiradaFilaItem> mapper(Instant agora) {
        return (rs, linha) -> {
            Instant solicitadoEm = instante(rs, "solicitado_em");
            Instant entregueEm = instante(rs, "entregue_em");
            String turmaNome = rs.getString("turma_nome");
            String salaNome = rs.getString("sala_nome");
            return new RetiradaFilaItem(
                    RetiradaLookupJdbc.uuid(rs.getString("id")),
                    RetiradaLookupJdbc.uuid(rs.getString("unit_id")),
                    new RetiradaFilaItem.AlunoDoCartao(
                            RetiradaLookupJdbc.uuid(rs.getString("aluno_id")),
                            rs.getString("aluno_nome"),
                            rs.getString("aluno_foto_key"),
                            turmaNome,
                            salaNome),
                    new RetiradaFilaItem.RetiranteDoCartao(
                            RetiradaLookupJdbc.uuid(rs.getString("pessoa_autorizada_id")),
                            rs.getString("pessoa_nome"),
                            rs.getString("pessoa_foto_key"),
                            rs.getString("parentesco")),
                    RetiradaLookupJdbc.uuid(rs.getString("turma_id")),
                    turmaNome,
                    RetiradaLookupJdbc.uuid(rs.getString("sala_id")),
                    salaNome,
                    RetiradaLookupJdbc.uuid(rs.getString("portaria_id")),
                    rs.getString("portaria_nome"),
                    statusDe(rs.getString("status")),
                    (Integer) rs.getObject("ordem_chegada"),
                    solicitadoEm,
                    instante(rs, "preparando_em"),
                    instante(rs, "pronto_em"),
                    entregueEm,
                    instante(rs, "saida_em"),
                    rs.getBoolean("retirada_manual"),
                    rs.getString("motivo"),
                    rs.getString("observacao"),
                    minutosDeEspera(solicitadoEm, entregueEm, agora));
        };
    }

    /**
     * Tempo de espera: da chegada do responsavel ate' a entrega. Enquanto a
     * crianca nao desceu, o relogio continua correndo.
     */
    static long minutosDeEspera(Instant solicitadoEm, Instant entregueEm, Instant agora) {
        if (solicitadoEm == null) {
            return 0L;
        }
        Instant fim = entregueEm != null ? entregueEm : agora;
        if (fim.isBefore(solicitadoEm)) {
            return 0L;
        }
        return Duration.between(solicitadoEm, fim).toMinutes();
    }

    private static StatusRetirada statusDe(String valor) {
        if (valor == null) {
            return null;
        }
        try {
            return StatusRetirada.valueOf(valor);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static Instant instante(ResultSet rs, String coluna) throws SQLException {
        Timestamp ts = rs.getTimestamp(coluna);
        return ts == null ? null : ts.toInstant();
    }
}
