package br.com.alfaschool.backend.application.access.portal;

import br.com.alfaschool.backend.application.access.portal.dto.PortalEventoResumo;
import br.com.alfaschool.backend.application.access.portal.dto.PortalPermanenciaDia;
import br.com.alfaschool.backend.application.access.portal.dto.PortalResumoDia;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Leitura de permanencia para o portal, direto de {@code acc_presencas} e
 * {@code acc_eventos} (V38/V39).
 *
 * <p>Consultas NATIVAS de proposito: essas tabelas pertencem a fatia de
 * permanencia e nao devem ganhar uma segunda entidade JPA aqui. O portal so'
 * le; nada nesta classe escreve.
 *
 * <p>Regra respeitada: dia inconsistente nao vira numero para a familia. Uma
 * presenca com {@code status <> 'FECHADA'} e sem saida e' mostrada como "ainda
 * na escola", nunca como permanencia final.
 */
@Component
public class PortalPermanenciaJpa implements PortalPermanenciaPort {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");

    /** Quantas leituras da portaria o portal mostra no resumo do dia. */
    private static final int LIMITE_EVENTOS = 10;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public PortalResumoDia resumoDoDia(UUID tenantId, UUID alunoId, LocalDate data) {
        LocalDate dia = data == null ? LocalDate.now(FUSO) : data;

        List<Tuple> linhas = entityManager.createNativeQuery("""
                        SELECT p.primeira_entrada_em AS entrada,
                               p.ultima_saida_em     AS saida,
                               p.minutos_permanencia AS permanencia,
                               p.minutos_previstos   AS previstos,
                               p.status              AS status
                        FROM acc_presencas p
                        WHERE p.tenant_id = :tenantId
                          AND p.aluno_id = :alunoId
                          AND p.data = :dia
                          AND p.deleted = FALSE
                        """, Tuple.class)
                .setParameter("tenantId", tenantId.toString())
                .setParameter("alunoId", alunoId.toString())
                .setParameter("dia", dia.toString())
                .getResultList();

        Instant entrada = null;
        Instant saida = null;
        Integer permanencia = null;
        Integer previstos = null;
        if (!linhas.isEmpty()) {
            Tuple t = linhas.get(0);
            entrada = instante(t, "entrada");
            saida = instante(t, "saida");
            permanencia = inteiro(t, "permanencia");
            previstos = inteiro(t, "previstos");
        }

        boolean presenteAgora = entrada != null && saida == null;

        // Com o aluno ainda dentro, o minutos_permanencia gravado esta
        // desatualizado: a familia quer ver o relogio correndo, nao o valor
        // do ultimo recalculo.
        Integer permanenciaAtual = presenteAgora
                ? (int) java.time.Duration.between(entrada, Instant.now()).toMinutes()
                : permanencia;

        Double percentual = null;
        if (previstos != null && previstos > 0 && permanenciaAtual != null) {
            percentual = Math.round((permanenciaAtual * 1000.0) / previstos) / 10.0;
        }

        return new PortalResumoDia(alunoId, nomeDoAluno(tenantId, alunoId), dia,
                entrada, saida, presenteAgora, previstos, permanenciaAtual, percentual,
                ultimosEventos(tenantId, alunoId, dia));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PortalPermanenciaDia> historico(UUID tenantId, UUID alunoId, LocalDate inicio, LocalDate fim) {
        List<Tuple> linhas = entityManager.createNativeQuery("""
                        SELECT p.data                AS dia,
                               p.primeira_entrada_em AS entrada,
                               p.ultima_saida_em     AS saida,
                               p.minutos_permanencia AS permanencia,
                               p.minutos_previstos   AS previstos
                        FROM acc_presencas p
                        WHERE p.tenant_id = :tenantId
                          AND p.aluno_id = :alunoId
                          AND p.data BETWEEN :inicio AND :fim
                          AND p.deleted = FALSE
                        ORDER BY p.data DESC
                        """, Tuple.class)
                .setParameter("tenantId", tenantId.toString())
                .setParameter("alunoId", alunoId.toString())
                .setParameter("inicio", inicio.toString())
                .setParameter("fim", fim.toString())
                .getResultList();

        List<PortalPermanenciaDia> saida = new ArrayList<>();
        for (Tuple t : linhas) {
            Integer permanencia = inteiro(t, "permanencia");
            Integer previstos = inteiro(t, "previstos");
            Double percentual = (previstos != null && previstos > 0 && permanencia != null)
                    ? Math.round((permanencia * 1000.0) / previstos) / 10.0
                    : null;
            saida.add(new PortalPermanenciaDia(
                    data(t, "dia"), instante(t, "entrada"), instante(t, "saida"),
                    permanencia, previstos, percentual));
        }
        return saida;
    }

    private List<PortalEventoResumo> ultimosEventos(UUID tenantId, UUID alunoId, LocalDate dia) {
        List<Tuple> linhas = entityManager.createNativeQuery("""
                        SELECT e.data_hora AS data_hora,
                               e.sentido   AS sentido,
                               po.nome     AS local
                        FROM acc_eventos e
                        LEFT JOIN acc_portarias po ON po.id = e.portaria_id
                        WHERE e.tenant_id = :tenantId
                          AND e.titular_tipo = 'ALUNO'
                          AND e.titular_id = :alunoId
                          AND e.resultado = 'PERMITIDO'
                          AND e.data_hora >= :inicioDoDia
                          AND e.data_hora < :fimDoDia
                        ORDER BY e.data_hora DESC
                        """, Tuple.class)
                .setParameter("tenantId", tenantId.toString())
                .setParameter("alunoId", alunoId.toString())
                .setParameter("inicioDoDia", Timestamp.from(dia.atStartOfDay(FUSO).toInstant()))
                .setParameter("fimDoDia", Timestamp.from(dia.plusDays(1).atStartOfDay(FUSO).toInstant()))
                .setMaxResults(LIMITE_EVENTOS)
                .getResultList();

        List<PortalEventoResumo> saida = new ArrayList<>();
        for (Tuple t : linhas) {
            saida.add(new PortalEventoResumo(instante(t, "data_hora"), texto(t, "sentido"), texto(t, "local")));
        }
        return saida;
    }

    private String nomeDoAluno(UUID tenantId, UUID alunoId) {
        List<?> r = entityManager.createNativeQuery(
                        "SELECT a.nome FROM alunos a WHERE a.tenant_id = :tenantId AND a.id = :alunoId")
                .setParameter("tenantId", tenantId.toString())
                .setParameter("alunoId", alunoId.toString())
                .getResultList();
        return r.isEmpty() || r.get(0) == null ? null : r.get(0).toString();
    }

    private Instant instante(Tuple t, String coluna) {
        Object v = t.get(coluna);
        if (v == null) {
            return null;
        }
        if (v instanceof Timestamp ts) {
            return ts.toInstant();
        }
        if (v instanceof Instant i) {
            return i;
        }
        if (v instanceof java.time.LocalDateTime ldt) {
            return ldt.atZone(FUSO).toInstant();
        }
        return null;
    }

    private LocalDate data(Tuple t, String coluna) {
        Object v = t.get(coluna);
        if (v == null) {
            return null;
        }
        if (v instanceof java.sql.Date d) {
            return d.toLocalDate();
        }
        if (v instanceof LocalDate ld) {
            return ld;
        }
        return LocalDate.parse(v.toString());
    }

    private Integer inteiro(Tuple t, String coluna) {
        Object v = t.get(coluna);
        return v instanceof Number n ? n.intValue() : null;
    }

    private String texto(Tuple t, String coluna) {
        Object v = t.get(coluna);
        return v == null ? null : v.toString();
    }
}
