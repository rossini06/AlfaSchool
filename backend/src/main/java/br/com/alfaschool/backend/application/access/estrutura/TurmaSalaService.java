package br.com.alfaschool.backend.application.access.estrutura;

import br.com.alfaschool.backend.application.access.estrutura.dto.SalaResponse;
import br.com.alfaschool.backend.application.access.estrutura.dto.SalaVigenteResponse;
import br.com.alfaschool.backend.application.access.estrutura.dto.TurmaSalaRequest;
import br.com.alfaschool.backend.application.access.estrutura.dto.TurmaSalaResponse;
import br.com.alfaschool.backend.domain.access.estrutura.AccSala;
import br.com.alfaschool.backend.domain.access.estrutura.AccTurmaSala;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccSalaRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccTurmaSalaRepository;
import br.com.alfaschool.backend.security.filter.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Vinculo turma<->sala. Toda a dificuldade esta em responder "qual sala esta
 * turma ocupa agora", porque a resposta depende de tres eixos ao mesmo tempo:
 * vigencia (periodo do ano), dia da semana e faixa de horario.
 */
@Service
public class TurmaSalaService {

    /**
     * Ordem de preferencia quando mais de um vinculo casa com o instante:
     *  1. o mais especifico (faixa de horario ganha de vinculo aberto);
     *  2. o de vigencia mais recente (remanejamento novo ganha do antigo);
     *  3. o cadastrado por ultimo, so para a resposta ser deterministica.
     */
    private static final Comparator<AccTurmaSala> MAIS_ESPECIFICO_PRIMEIRO =
            TurmaSalaService::compararPreferencia;

    private static int compararPreferencia(AccTurmaSala a, AccTurmaSala b) {
        int porEspecificidade = Integer.compare(b.especificidade(), a.especificidade());
        if (porEspecificidade != 0) {
            return porEspecificidade;
        }
        int porVigencia = compararDatasDesc(a.getVigenciaInicio(), b.getVigenciaInicio());
        if (porVigencia != 0) {
            return porVigencia;
        }
        Instant criadoA = a.getCreatedAt() != null ? a.getCreatedAt() : Instant.EPOCH;
        Instant criadoB = b.getCreatedAt() != null ? b.getCreatedAt() : Instant.EPOCH;
        int porCriacao = criadoB.compareTo(criadoA);
        if (porCriacao != 0) {
            return porCriacao;
        }
        String idA = a.getId() != null ? a.getId().toString() : "";
        String idB = b.getId() != null ? b.getId().toString() : "";
        return idA.compareTo(idB);
    }

    private static int compararDatasDesc(LocalDate a, LocalDate b) {
        if (a == null && b == null) {
            return 0;
        }
        if (a == null) {
            return 1;
        }
        if (b == null) {
            return -1;
        }
        return b.compareTo(a);
    }

    private final AccTurmaSalaRepository turmaSalaRepository;
    private final AccSalaRepository salaRepository;

    public TurmaSalaService(AccTurmaSalaRepository turmaSalaRepository, AccSalaRepository salaRepository) {
        this.turmaSalaRepository = turmaSalaRepository;
        this.salaRepository = salaRepository;
    }

    // ------------------------------------------------------------------
    // CRUD
    // ------------------------------------------------------------------

    public Page<TurmaSalaResponse> list(UUID turmaId, UUID salaId, Pageable pageable) {
        UUID tenantId = tenantObrigatorio();
        if (turmaId == null && salaId == null) {
            return turmaSalaRepository.findByTenantIdAndDeletedFalse(tenantId, pageable)
                    .map(TurmaSalaResponse::from);
        }
        return turmaSalaRepository.buscar(tenantId, turmaId, salaId, pageable).map(TurmaSalaResponse::from);
    }

    public List<TurmaSalaResponse> listByTurma(UUID turmaId) {
        UUID tenantId = tenantObrigatorio();
        return turmaSalaRepository.findByTenantIdAndTurmaIdAndDeletedFalse(tenantId, turmaId)
                .stream().map(TurmaSalaResponse::from).toList();
    }

    public List<TurmaSalaResponse> listBySala(UUID salaId) {
        UUID tenantId = tenantObrigatorio();
        return turmaSalaRepository.findByTenantIdAndSalaIdAndDeletedFalse(tenantId, salaId)
                .stream().map(TurmaSalaResponse::from).toList();
    }

    public TurmaSalaResponse findById(UUID id) {
        return TurmaSalaResponse.from(buscar(id));
    }

    @Transactional
    public TurmaSalaResponse create(TurmaSalaRequest request) {
        UUID tenantId = tenantObrigatorio();
        AccTurmaSala vinculo = new AccTurmaSala();
        vinculo.setTenantId(tenantId);
        vinculo.setTurmaId(request.turmaId());
        aplicar(vinculo, request, tenantId);
        validarSobreposicao(tenantId, vinculo);
        return TurmaSalaResponse.from(turmaSalaRepository.save(vinculo));
    }

    @Transactional
    public TurmaSalaResponse update(UUID id, TurmaSalaRequest request) {
        UUID tenantId = tenantObrigatorio();
        AccTurmaSala vinculo = buscar(id);
        if (!vinculo.getTurmaId().equals(request.turmaId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Nao e possivel trocar a turma do vinculo; encerre este e crie outro");
        }
        aplicar(vinculo, request, tenantId);
        validarSobreposicao(tenantId, vinculo);
        return TurmaSalaResponse.from(turmaSalaRepository.save(vinculo));
    }

    @Transactional
    public void delete(UUID id) {
        AccTurmaSala vinculo = buscar(id);
        vinculo.setDeleted(true);
        turmaSalaRepository.save(vinculo);
    }

    // ------------------------------------------------------------------
    // Resolucao temporal
    // ------------------------------------------------------------------

    /** Sala que a turma ocupa no instante informado, com o vinculo que a explica. */
    public SalaVigenteResponse salaVigenteDaTurma(UUID turmaId, LocalDateTime momento) {
        UUID tenantId = tenantObrigatorio();
        AccTurmaSala vinculo = resolverVinculoVigente(tenantId, turmaId, momento)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Nenhuma sala vigente para esta turma no momento informado"));
        AccSala sala = salaRepository.findByIdAndTenantIdAndDeletedFalse(vinculo.getSalaId(), tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Sala do vinculo vigente nao encontrada"));
        return new SalaVigenteResponse(momento, SalaResponse.from(sala), TurmaSalaResponse.from(vinculo));
    }

    /**
     * Nucleo da regra, sem HTTP e sem TenantContext, para ficar testavel direto.
     * O banco corta pela vigencia (indexada); dias da semana e faixa de horario
     * sao aplicados aqui porque CSV nao se compara com indice.
     */
    public Optional<AccTurmaSala> resolverVinculoVigente(UUID tenantId, UUID turmaId, LocalDateTime momento) {
        if (momento == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Momento e obrigatorio");
        }
        return turmaSalaRepository.candidatosPorTurma(tenantId, turmaId, momento.toLocalDate())
                .stream()
                .filter(v -> v.aplicaEm(momento))
                .min(MAIS_ESPECIFICO_PRIMEIRO);
    }

    /** Inverso: turmas que ocupam a sala naquele instante. */
    public List<TurmaSalaResponse> turmasNaSala(UUID salaId, LocalDateTime momento) {
        UUID tenantId = tenantObrigatorio();
        return resolverVinculosNaSala(tenantId, salaId, momento).stream()
                .map(TurmaSalaResponse::from)
                .toList();
    }

    public List<AccTurmaSala> resolverVinculosNaSala(UUID tenantId, UUID salaId, LocalDateTime momento) {
        if (momento == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Momento e obrigatorio");
        }
        List<AccTurmaSala> candidatos = turmaSalaRepository
                .candidatosPorSala(tenantId, salaId, momento.toLocalDate())
                .stream()
                .filter(v -> v.aplicaEm(momento))
                .sorted(MAIS_ESPECIFICO_PRIMEIRO)
                .toList();

        // Um candidato da sala pode estar perdendo o desempate para outro
        // vinculo mais especifico da MESMA turma (ex.: a turma saiu para o
        // laboratorio). Se ficasse aqui, o painel mostraria a turma em duas
        // salas ao mesmo tempo. O cache evita repetir a consulta por turma.
        Map<UUID, Optional<AccTurmaSala>> vigentePorTurma = new HashMap<>();
        List<AccTurmaSala> resultado = new ArrayList<>();
        for (AccTurmaSala candidato : candidatos) {
            Optional<AccTurmaSala> vigente = vigentePorTurma.computeIfAbsent(candidato.getTurmaId(),
                    turmaId -> resolverVinculoVigente(tenantId, turmaId, momento));
            // So entra o vinculo que a turma de fato esta cumprindo agora: uma
            // turma nunca aparece duas vezes na mesma sala.
            if (vigente.isPresent() && mesmoVinculo(vigente.get(), candidato)) {
                resultado.add(candidato);
            }
        }
        return resultado;
    }

    /** Compara por id; cai na identidade quando o vinculo ainda nao foi gravado. */
    private static boolean mesmoVinculo(AccTurmaSala a, AccTurmaSala b) {
        if (a == b) {
            return true;
        }
        return a != null && b != null && a.getId() != null && Objects.equals(a.getId(), b.getId());
    }

    // ------------------------------------------------------------------
    // Validacao
    // ------------------------------------------------------------------

    /**
     * Dois vinculos da mesma turma que se cruzem em vigencia E dias E horario
     * tornariam a sala ambigua: a apuracao de permanencia nao teria como
     * decidir onde o aluno deveria estar. Barramos no cadastro, com 409.
     */
    void validarSobreposicao(UUID tenantId, AccTurmaSala candidato) {
        List<AccTurmaSala> existentes =
                turmaSalaRepository.findByTenantIdAndTurmaIdAndDeletedFalse(tenantId, candidato.getTurmaId());
        for (AccTurmaSala existente : existentes) {
            if (candidato.getId() != null && candidato.getId().equals(existente.getId())) {
                continue;
            }
            if (candidato.sobrepoe(existente)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Conflito de vinculo turma-sala: ja existe um vinculo desta turma"
                                + " valendo no mesmo periodo, nos mesmos dias da semana e no mesmo horario"
                                + " (vinculo " + existente.getId() + ", vigencia a partir de "
                                + existente.getVigenciaInicio() + "). Ajuste a vigencia,"
                                + " os dias da semana ou a faixa de horario.");
            }
        }
    }

    private void aplicar(AccTurmaSala vinculo, TurmaSalaRequest request, UUID tenantId) {
        if (request.vigenciaFim() != null && request.vigenciaFim().isBefore(request.vigenciaInicio())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "vigenciaFim nao pode ser anterior a vigenciaInicio");
        }
        if (request.horaInicio() != null && request.horaFim() != null
                && !request.horaFim().isAfter(request.horaInicio())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "horaFim deve ser maior que horaInicio");
        }
        salaRepository.findByIdAndTenantIdAndDeletedFalse(request.salaId(), tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sala informada nao encontrada"));

        vinculo.setSalaId(request.salaId());
        vinculo.setVigenciaInicio(request.vigenciaInicio());
        vinculo.setVigenciaFim(request.vigenciaFim());
        vinculo.setHoraInicio(request.horaInicio());
        vinculo.setHoraFim(request.horaFim());
        vinculo.setDiasSemana(normalizarDias(request.diasSemana()));
    }

    /** Normaliza "1, 2 ,3" para "1,2,3" e trata vazio como nulo (sem restricao). */
    private String normalizarDias(String csv) {
        if (csv == null || csv.isBlank()) {
            return null;
        }
        return AccTurmaSala.parseDias(csv).stream()
                .sorted()
                .map(String::valueOf)
                .reduce((a, b) -> a + "," + b)
                .orElse(null);
    }

    private AccTurmaSala buscar(UUID id) {
        UUID tenantId = tenantObrigatorio();
        return turmaSalaRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Vinculo turma-sala nao encontrado"));
    }

    private UUID tenantObrigatorio() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tenant nao identificado na requisicao");
        }
        return tenantId;
    }
}
