package br.com.alfaschool.backend.application.access.painel;

import br.com.alfaschool.backend.application.access.painel.dto.ResumoCoordenacaoResponse;
import br.com.alfaschool.backend.application.access.retirada.RetiradaConsultaService;
import br.com.alfaschool.backend.application.access.retirada.RetiradaLookupJdbc;
import br.com.alfaschool.backend.application.access.retirada.RetiradaService;
import br.com.alfaschool.backend.application.access.retirada.dto.FilaFiltro;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccRetiradaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Contadores do painel de coordenacao.
 *
 * Cada numero e' UMA consulta agregada. A alternativa obvia — carregar a
 * lista e contar em Java — funciona na escola de teste e derruba a tela na
 * rede com 1200 alunos, justo no horario em que a coordenacao mais olha.
 */
@Service
public class PainelResumoService {

    private static final Logger log = LoggerFactory.getLogger(PainelResumoService.class);

    /** Minutos de espera a partir dos quais a coordenacao quer ser avisada. */
    public static final int LIMITE_ESPERA_PADRAO = 20;

    private final JdbcTemplate jdbc;
    private final AccRetiradaRepository retiradaRepository;
    private final RetiradaConsultaService consultaService;

    public PainelResumoService(JdbcTemplate jdbc,
                               AccRetiradaRepository retiradaRepository,
                               RetiradaConsultaService consultaService) {
        this.jdbc = jdbc;
        this.retiradaRepository = retiradaRepository;
        this.consultaService = consultaService;
    }

    public ResumoCoordenacaoResponse resumo(UUID tenantId, UUID unitId, Integer limiteEsperaMinutos) {
        int limite = (limiteEsperaMinutos == null || limiteEsperaMinutos <= 0)
                ? LIMITE_ESPERA_PADRAO : limiteEsperaMinutos;

        LocalDate hoje = LocalDate.now(RetiradaLookupJdbc.ZONA);
        Instant inicio = hoje.atStartOfDay(RetiradaLookupJdbc.ZONA).toInstant();
        Instant fim = hoje.plusDays(1).atStartOfDay(RetiradaLookupJdbc.ZONA).toInstant();
        Instant corte = Instant.now().minus(Duration.ofMinutes(limite));

        long presentes = contarPresentes(tenantId, unitId, hoje);
        long aguardando = retiradaRepository.contarPorStatusNoDia(
                tenantId, unitId, RetiradaService.EM_ABERTO, inicio, fim);
        long excedido = retiradaRepository.contarEsperandoAlemDe(
                tenantId, unitId, RetiradaService.EM_ABERTO, corte);
        long saidas = retiradaRepository.contarSaidasNoDia(tenantId, unitId, inicio, fim);

        FilaFiltro filtro = new FilaFiltro(unitId, null, null, null, null, null);
        return new ResumoCoordenacaoResponse(
                unitId,
                Instant.now(),
                presentes,
                aguardando,
                excedido,
                saidas,
                limite,
                consultaService.fila(tenantId, filtro));
    }

    /**
     * Presenca ABERTA e' aluno dentro da escola agora. INCONSISTENTE fica de
     * fora de proposito: par entrada/saida quebrado nao pode inflar o numero
     * que a coordenacao usa para saber quantas criancas ainda estao no
     * predio.
     *
     * A tabela e' da fatia de permanencia; por isso SQL, e nao a entidade
     * dela.
     */
    private long contarPresentes(UUID tenantId, UUID unitId, LocalDate dia) {
        String sql = """
                SELECT COUNT(*)
                  FROM acc_presencas p
                 WHERE p.tenant_id = ?
                   AND p.deleted = FALSE
                   AND p.data = ?
                   AND p.status = 'ABERTA'
                   AND (? IS NULL OR p.unit_id = ?)
                """;
        try {
            String unit = unitId == null ? null : unitId.toString();
            Long total = jdbc.queryForObject(sql, Long.class, tenantId.toString(), dia, unit, unit);
            return total == null ? 0L : total;
        } catch (Exception e) {
            // A tela da coordenacao nao pode cair inteira porque um contador
            // falhou: zero visivel e' melhor do que 500.
            log.warn("Falha ao contar alunos presentes (tenant {}, unidade {})", tenantId, unitId, e);
            return 0L;
        }
    }
}
