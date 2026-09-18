package br.com.alfaschool.backend.application.access.calendario;

import br.com.alfaschool.backend.application.access.calendario.dto.CalendarioDiaRequest;
import br.com.alfaschool.backend.application.access.calendario.dto.CalendarioDiaResponse;
import br.com.alfaschool.backend.application.access.calendario.dto.CalendarioMesResponse;
import br.com.alfaschool.backend.application.access.calendario.dto.DiaDoMesResponse;
import br.com.alfaschool.backend.application.access.calendario.dto.IntervaloDiasRequest;
import br.com.alfaschool.backend.domain.access.calendario.AccCalendario;
import br.com.alfaschool.backend.domain.access.calendario.AccCalendarioDia;
import br.com.alfaschool.backend.domain.access.shared.TipoCalendarioDia;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccCalendarioDiaRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccCalendarioRepository;
import br.com.alfaschool.backend.security.filter.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CalendarioDiaService {

    private final AccCalendarioDiaRepository diaRepository;
    private final AccCalendarioRepository calendarioRepository;
    private final CalendarioService calendarioService;
    private final CalendarioConsultaService consultaService;

    public CalendarioDiaService(AccCalendarioDiaRepository diaRepository,
                                AccCalendarioRepository calendarioRepository,
                                CalendarioService calendarioService,
                                CalendarioConsultaService consultaService) {
        this.diaRepository = diaRepository;
        this.calendarioRepository = calendarioRepository;
        this.calendarioService = calendarioService;
        this.consultaService = consultaService;
    }

    public List<CalendarioDiaResponse> listDias(UUID calendarioId) {
        UUID tenantId = tenantObrigatorio();
        calendarioService.buscar(calendarioId);
        return diaRepository.findByTenantIdAndCalendarioIdAndDeletedFalse(tenantId, calendarioId)
                .stream()
                .sorted(Comparator.comparing(AccCalendarioDia::getData))
                .map(CalendarioDiaResponse::from)
                .toList();
    }

    public CalendarioDiaResponse findDia(UUID diaId) {
        UUID tenantId = tenantObrigatorio();
        return CalendarioDiaResponse.from(diaRepository.findByIdAndTenantIdAndDeletedFalse(diaId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dia de calendario nao encontrado")));
    }

    @Transactional
    public CalendarioDiaResponse salvarDia(UUID calendarioId, CalendarioDiaRequest request) {
        UUID tenantId = tenantObrigatorio();
        AccCalendario calendario = calendarioService.buscar(calendarioId);
        validarAno(calendario, request.data());
        AccCalendarioDia dia = gravar(tenantId, calendario, request.data(),
                parseTipo(request.tipo()), request.descricao());
        invalidar(tenantId, calendario);
        return CalendarioDiaResponse.from(dia);
    }

    @Transactional
    public CalendarioDiaResponse updateDia(UUID diaId, CalendarioDiaRequest request) {
        UUID tenantId = tenantObrigatorio();
        AccCalendarioDia dia = diaRepository.findByIdAndTenantIdAndDeletedFalse(diaId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dia de calendario nao encontrado"));
        AccCalendario calendario = calendarioService.buscar(dia.getCalendarioId());
        validarAno(calendario, request.data());
        if (!dia.getData().equals(request.data())) {
            // A UNIQUE (calendario_id, data) nao deixa duas linhas na mesma data;
            // mover um lancamento de dia seria colidir com o que ja existe la.
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Nao e possivel mudar a data de um lancamento; exclua e lance na data correta");
        }
        dia.setTipo(parseTipo(request.tipo()));
        dia.setDescricao(request.descricao());
        AccCalendarioDia salvo = diaRepository.save(dia);
        invalidar(tenantId, calendario);
        return CalendarioDiaResponse.from(salvo);
    }

    @Transactional
    public void deleteDia(UUID diaId) {
        UUID tenantId = tenantObrigatorio();
        AccCalendarioDia dia = diaRepository.findByIdAndTenantIdAndDeletedFalse(diaId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dia de calendario nao encontrado"));
        AccCalendario calendario = calendarioService.buscar(dia.getCalendarioId());
        dia.setDeleted(true);
        diaRepository.save(dia);
        invalidar(tenantId, calendario);
    }

    /**
     * Lancamento de intervalo. O caso real e' o recesso de 20/12 a 31/01, que
     * atravessa o ano: as datas de janeiro precisam cair no calendario do ano
     * seguinte, senao a consulta por ano nao as encontraria. O metodo faz esse
     * roteamento sozinho, dentro do mesmo escopo (mesma unidade ou global).
     */
    @Transactional
    public List<CalendarioDiaResponse> lancarIntervalo(UUID calendarioId, IntervaloDiasRequest request) {
        UUID tenantId = tenantObrigatorio();
        AccCalendario calendario = calendarioService.buscar(calendarioId);
        if (request.dataFim().isBefore(request.dataInicio())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "dataFim nao pode ser anterior a dataInicio");
        }

        TipoCalendarioDia tipo = parseTipo(request.tipo());
        boolean apenasUteis = Boolean.TRUE.equals(request.apenasDiasUteis());
        boolean sobrescrever = request.sobrescrever() == null || request.sobrescrever();

        Map<Integer, AccCalendario> calendarioPorAno = new HashMap<>();
        if (calendario.getAnoLetivo() != null) {
            calendarioPorAno.put(calendario.getAnoLetivo(), calendario);
        }

        List<CalendarioDiaResponse> gravados = new ArrayList<>();
        for (LocalDate data = request.dataInicio(); !data.isAfter(request.dataFim()); data = data.plusDays(1)) {
            if (apenasUteis && ehFimDeSemana(data)) {
                continue;
            }
            AccCalendario alvo = calendarioPorAno.computeIfAbsent(data.getYear(),
                    ano -> calendarioDoMesmoEscopo(tenantId, calendario, ano));
            if (!sobrescrever) {
                boolean jaExiste = diaRepository.findByCalendarioIdAndData(alvo.getId(), data)
                        .filter(existente -> !Boolean.TRUE.equals(existente.getDeleted()))
                        .isPresent();
                if (jaExiste) {
                    continue;
                }
            }
            gravados.add(CalendarioDiaResponse.from(gravar(tenantId, alvo, data, tipo, request.descricao())));
        }
        for (AccCalendario afetado : calendarioPorAno.values()) {
            invalidar(tenantId, afetado);
        }
        return gravados;
    }

    /** Calendario ativo do mesmo escopo (unidade ou global) para outro ano. */
    private AccCalendario calendarioDoMesmoEscopo(UUID tenantId, AccCalendario referencia, int ano) {
        List<AccCalendario> ativos = referencia.getUnitId() == null
                ? calendarioRepository.ativosGlobais(tenantId, ano)
                : calendarioRepository.ativosDaUnidade(tenantId, referencia.getUnitId(), ano);
        if (ativos.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O intervalo alcanca o ano " + ano + ", mas nao existe calendario ativo para "
                            + (referencia.getUnitId() == null ? "toda a escola" : "esta unidade")
                            + " nesse ano. Crie o calendario de " + ano + " antes de lancar o intervalo.");
        }
        return ativos.get(0);
    }

    /** Grade do mes para a tela: todo dia do mes, lancado ou nao. */
    public CalendarioMesResponse mes(UUID calendarioId, int ano, int mes) {
        UUID tenantId = tenantObrigatorio();
        if (mes < 1 || mes > 12) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "mes deve estar entre 1 e 12");
        }
        AccCalendario calendario = calendarioService.buscar(calendarioId);
        YearMonth referencia = YearMonth.of(ano, mes);
        LocalDate primeiro = referencia.atDay(1);
        LocalDate ultimo = referencia.atEndOfMonth();

        Map<LocalDate, AccCalendarioDia> lancados = new HashMap<>();
        for (AccCalendarioDia dia : diaRepository
                .findByTenantIdAndCalendarioIdAndDataBetweenAndDeletedFalse(tenantId, calendarioId, primeiro, ultimo)) {
            lancados.put(dia.getData(), dia);
        }

        List<DiaDoMesResponse> dias = new ArrayList<>();
        long letivos = 0;
        for (LocalDate data = primeiro; !data.isAfter(ultimo); data = data.plusDays(1)) {
            AccCalendarioDia lancado = lancados.get(data);
            boolean letivo = lancado != null
                    ? CalendarioConsultaService.ehLetivo(lancado.getTipo())
                    : CalendarioConsultaService.padraoDaSemana(data);
            if (letivo) {
                letivos++;
            }
            dias.add(new DiaDoMesResponse(
                    data,
                    data.getDayOfWeek().getValue(),
                    lancado != null ? lancado.getTipo().name() : null,
                    lancado != null ? lancado.getDescricao() : null,
                    letivo,
                    lancado != null,
                    lancado != null ? lancado.getId() : null));
        }
        return new CalendarioMesResponse(calendario.getId(), ano, mes, letivos, dias);
    }

    // ------------------------------------------------------------------

    /**
     * Reaproveita a linha existente (inclusive a marcada como excluida) porque a
     * UNIQUE (calendario_id, data) e' fisica e nao enxerga o soft delete.
     */
    private AccCalendarioDia gravar(UUID tenantId, AccCalendario calendario, LocalDate data,
                                    TipoCalendarioDia tipo, String descricao) {
        AccCalendarioDia dia = diaRepository.findByCalendarioIdAndData(calendario.getId(), data)
                .orElseGet(AccCalendarioDia::new);
        dia.setTenantId(tenantId);
        dia.setCalendarioId(calendario.getId());
        dia.setData(data);
        dia.setTipo(tipo);
        dia.setDescricao(descricao);
        dia.setDeleted(false);
        return diaRepository.save(dia);
    }

    private void invalidar(UUID tenantId, AccCalendario calendario) {
        consultaService.invalidar(tenantId, calendario.getUnitId(), calendario.getAnoLetivo());
    }

    private void validarAno(AccCalendario calendario, LocalDate data) {
        if (calendario.getAnoLetivo() != null && data.getYear() != calendario.getAnoLetivo()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A data " + data + " nao pertence ao ano letivo " + calendario.getAnoLetivo()
                            + " deste calendario");
        }
    }

    private static boolean ehFimDeSemana(LocalDate data) {
        DayOfWeek dia = data.getDayOfWeek();
        return dia == DayOfWeek.SATURDAY || dia == DayOfWeek.SUNDAY;
    }

    private TipoCalendarioDia parseTipo(String tipo) {
        try {
            return TipoCalendarioDia.valueOf(tipo.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Tipo de dia invalido. Valores aceitos: "
                            + Arrays.stream(TipoCalendarioDia.values()).map(Enum::name)
                            .collect(Collectors.joining(", ")));
        }
    }

    private UUID tenantObrigatorio() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tenant nao identificado na requisicao");
        }
        return tenantId;
    }
}
