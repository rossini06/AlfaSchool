package br.com.alfaschool.backend.application.access.calendario;

import br.com.alfaschool.backend.application.access.shared.CalendarioPort;
import br.com.alfaschool.backend.domain.access.calendario.AccCalendario;
import br.com.alfaschool.backend.domain.access.calendario.AccCalendarioDia;
import br.com.alfaschool.backend.domain.access.shared.TipoCalendarioDia;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccCalendarioDiaRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccCalendarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementacao de CalendarioPort.
 *
 * Esta consulta e' chamada aos milhares por mes pela apuracao de permanencia
 * (um aluno x um dia), entao o ano inteiro de um calendario e' carregado de uma
 * vez e mantido em memoria por (tenant, unidade, ano). Sem isso seria um SELECT
 * por aluno por dia.
 *
 * O cache e' local a instancia da JVM: nao ha invalidacao entre replicas. Com
 * varias instancias, uma alteracao de calendario so aparece nas outras depois
 * do proximo restart. Vale trocar por cache distribuido se o backend escalar
 * horizontalmente (ja existe Redis no projeto).
 */
@Service
public class CalendarioConsultaService implements CalendarioPort {

    /** Chave do cache. unitId nulo representa a consulta no escopo global. */
    private record ChaveAno(UUID tenantId, UUID unitId, int ano) {}

    private final AccCalendarioRepository calendarioRepository;
    private final AccCalendarioDiaRepository diaRepository;
    private final Map<ChaveAno, Map<LocalDate, TipoCalendarioDia>> cache = new ConcurrentHashMap<>();

    public CalendarioConsultaService(AccCalendarioRepository calendarioRepository,
                                     AccCalendarioDiaRepository diaRepository) {
        this.calendarioRepository = calendarioRepository;
        this.diaRepository = diaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean ehDiaLetivo(UUID tenantId, UUID unitId, LocalDate data) {
        if (tenantId == null || data == null) {
            throw new IllegalArgumentException("tenantId e data sao obrigatorios para consultar o calendario");
        }
        TipoCalendarioDia tipo = mapaDoAno(tenantId, unitId, data.getYear()).get(data);
        // Sem lancamento, cai no padrao: o calendario so guarda as excecoes.
        return tipo != null ? ehLetivo(tipo) : padraoDaSemana(data);
    }

    /** Tipo lancado para a data, se houver. Usado pela tela do mes. */
    public TipoCalendarioDia tipoDoDia(UUID tenantId, UUID unitId, LocalDate data) {
        if (tenantId == null || data == null) {
            return null;
        }
        return mapaDoAno(tenantId, unitId, data.getYear()).get(data);
    }

    /**
     * EVENTO conta como letivo: mostra pedagogica, feira de ciencias e
     * gincana tem aluno na escola e permanencia a apurar. FACULTATIVO nao,
     * porque a escola pode abrir mas a presenca nao e' cobrada.
     */
    public static boolean ehLetivo(TipoCalendarioDia tipo) {
        return switch (tipo) {
            case LETIVO, SABADO_LETIVO, EVENTO -> true;
            case FERIADO, RECESSO, FACULTATIVO -> false;
        };
    }

    /** Padrao quando a data nao foi lancada: de segunda a sexta a escola abre. */
    public static boolean padraoDaSemana(LocalDate data) {
        DayOfWeek dia = data.getDayOfWeek();
        return dia != DayOfWeek.SATURDAY && dia != DayOfWeek.SUNDAY;
    }

    // ------------------------------------------------------------------
    // Cache
    // ------------------------------------------------------------------

    private Map<LocalDate, TipoCalendarioDia> mapaDoAno(UUID tenantId, UUID unitId, int ano) {
        return cache.computeIfAbsent(new ChaveAno(tenantId, unitId, ano), this::carregar);
    }

    private Map<LocalDate, TipoCalendarioDia> carregar(ChaveAno chave) {
        LocalDate primeiro = LocalDate.of(chave.ano(), 1, 1);
        LocalDate ultimo = LocalDate.of(chave.ano(), 12, 31);

        Map<LocalDate, TipoCalendarioDia> mapa = new HashMap<>();
        // Global primeiro (feriados nacionais valem para toda a rede) e a
        // unidade por cima: o calendario da unidade SOBREPOE o global dia a dia,
        // em vez de substitui-lo inteiro. Assim a unidade so precisa cadastrar
        // a sua excecao e nao repetir os feriados da escola toda.
        aplicar(mapa, calendarioRepository.ativosGlobais(chave.tenantId(), chave.ano()), primeiro, ultimo);
        if (chave.unitId() != null) {
            aplicar(mapa, calendarioRepository.ativosDaUnidade(chave.tenantId(), chave.unitId(), chave.ano()),
                    primeiro, ultimo);
        }
        return mapa;
    }

    private void aplicar(Map<LocalDate, TipoCalendarioDia> mapa, List<AccCalendario> calendarios,
                         LocalDate primeiro, LocalDate ultimo) {
        if (calendarios.isEmpty()) {
            return;
        }
        List<UUID> ids = new ArrayList<>();
        for (AccCalendario calendario : calendarios) {
            ids.add(calendario.getId());
        }
        for (AccCalendarioDia dia : diaRepository.findByCalendarioIdInAndDataBetweenAndDeletedFalse(
                ids, primeiro, ultimo)) {
            mapa.put(dia.getData(), dia.getTipo());
        }
    }

    /**
     * Invalida o recorte afetado por uma gravacao. Mexer no calendario global
     * derruba o ano inteiro do tenant, porque toda unidade o usa como base.
     */
    public void invalidar(UUID tenantId, UUID unitId, Integer ano) {
        if (tenantId == null || ano == null) {
            cache.clear();
            return;
        }
        if (unitId == null) {
            cache.keySet().removeIf(c -> c.tenantId().equals(tenantId) && c.ano() == ano);
            return;
        }
        cache.remove(new ChaveAno(tenantId, unitId, ano));
        // O escopo global do proprio tenant tambem le esse ano.
        cache.remove(new ChaveAno(tenantId, null, ano));
    }

    public void invalidarTudo() {
        cache.clear();
    }

    /** Observabilidade do cache: quantos recortes (tenant, unidade, ano) estao carregados. */
    public int tamanhoCache() {
        return cache.size();
    }

    public boolean contemRecorte(UUID tenantId, UUID unitId, int ano) {
        return cache.containsKey(new ChaveAno(tenantId, unitId, ano));
    }
}
