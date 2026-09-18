package br.com.alfaschool.backend.application.access.permanencia;

import br.com.alfaschool.backend.domain.access.shared.SentidoAcesso;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * Leitura direta de acc_eventos por SQL.
 *
 * Nao ha entidade JPA aqui de proposito: acc_eventos pertence ao modulo de
 * eventos e mapea-la duas vezes criaria duas verdades sobre a mesma
 * tabela. O motor de permanencia precisa de quatro colunas e le' as
 * quatro.
 *
 * O recorte da janela usa DATETIME local (data_hora e' DATETIME(6), sem
 * fuso): o dia civil e' convertido para America/Sao_Paulo antes de virar
 * limite da consulta.
 */
@Component
public class JdbcEventoAcessoLeitor implements EventoAcessoLeitor {

    /**
     * Os limites viajam como texto e nao como Timestamp: Timestamp carrega
     * um instante e o driver o reconverteria para o fuso da sessao. Com a
     * string, o MySQL compara DATETIME com DATETIME, e o dia civil que
     * entrou na consulta e' o mesmo que sai.
     */
    private static final DateTimeFormatter SQL_DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JdbcTemplate jdbc;

    public JdbcEventoAcessoLeitor(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<EventoAcesso> eventosDoDia(UUID tenantId, UUID alunoId, LocalDate dia) {
        String inicio = limite(dia);
        String fim = limite(dia.plusDays(1));
        return jdbc.query("""
                        select id, data_hora, unit_id, sentido
                          from acc_eventos
                         where tenant_id = ?
                           and titular_tipo = 'ALUNO'
                           and titular_id = ?
                           and resultado = 'PERMITIDO'
                           and data_hora >= ?
                           and data_hora < ?
                         order by data_hora asc, id asc
                        """,
                (rs, i) -> new EventoAcesso(
                        uuid(rs.getString("id")),
                        instante(rs.getTimestamp("data_hora")),
                        uuid(rs.getString("unit_id")),
                        sentido(rs.getString("sentido"))),
                tenantId.toString(), alunoId.toString(), inicio, fim);
    }

    @Override
    public List<UUID> alunosComEventoNoDia(UUID tenantId, LocalDate dia) {
        return jdbc.query("""
                        select distinct titular_id
                          from acc_eventos
                         where tenant_id = ?
                           and titular_tipo = 'ALUNO'
                           and titular_id is not null
                           and resultado = 'PERMITIDO'
                           and data_hora >= ?
                           and data_hora < ?
                        """,
                (rs, i) -> uuid(rs.getString(1)),
                tenantId.toString(), limite(dia), limite(dia.plusDays(1)));
    }

    @Override
    public List<UUID> tenantsComEventoNoDia(LocalDate dia) {
        return jdbc.query("""
                        select distinct tenant_id
                          from acc_eventos
                         where titular_tipo = 'ALUNO'
                           and titular_id is not null
                           and resultado = 'PERMITIDO'
                           and data_hora >= ?
                           and data_hora < ?
                        """,
                (rs, i) -> uuid(rs.getString(1)),
                limite(dia), limite(dia.plusDays(1)));
    }

    private static String limite(LocalDate dia) {
        return dia.atStartOfDay().format(SQL_DATETIME);
    }

    private static UUID uuid(String valor) {
        return valor == null || valor.isBlank() ? null : UUID.fromString(valor);
    }

    private static Instant instante(Timestamp ts) {
        return ts == null ? null : ts.toInstant();
    }

    private static SentidoAcesso sentido(String valor) {
        if (valor == null) {
            return SentidoAcesso.INDEFINIDO;
        }
        try {
            return SentidoAcesso.valueOf(valor);
        } catch (IllegalArgumentException e) {
            return SentidoAcesso.INDEFINIDO;
        }
    }
}
