package br.com.alfaschool.backend.application.access.calendario;

import br.com.alfaschool.backend.application.access.calendario.dto.CalendarioRequest;
import br.com.alfaschool.backend.application.access.calendario.dto.CalendarioResponse;
import br.com.alfaschool.backend.domain.access.calendario.AccCalendario;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccCalendarioDiaRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccCalendarioRepository;
import br.com.alfaschool.backend.security.filter.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class CalendarioService {

    private final AccCalendarioRepository calendarioRepository;
    private final AccCalendarioDiaRepository diaRepository;
    private final CalendarioConsultaService consultaService;

    public CalendarioService(AccCalendarioRepository calendarioRepository,
                             AccCalendarioDiaRepository diaRepository,
                             CalendarioConsultaService consultaService) {
        this.calendarioRepository = calendarioRepository;
        this.diaRepository = diaRepository;
        this.consultaService = consultaService;
    }

    public Page<CalendarioResponse> list(Pageable pageable) {
        UUID tenantId = tenantObrigatorio();
        return calendarioRepository.findByTenantIdAndDeletedFalse(tenantId, pageable).map(CalendarioResponse::from);
    }

    public List<CalendarioResponse> listByAno(Integer ano) {
        UUID tenantId = tenantObrigatorio();
        return calendarioRepository.findByTenantIdAndAnoLetivoAndDeletedFalse(tenantId, ano)
                .stream().map(CalendarioResponse::from).toList();
    }

    public CalendarioResponse findById(UUID id) {
        return CalendarioResponse.from(buscar(id));
    }

    AccCalendario buscar(UUID id) {
        UUID tenantId = tenantObrigatorio();
        return calendarioRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Calendario nao encontrado"));
    }

    @Transactional
    public CalendarioResponse create(CalendarioRequest request) {
        UUID tenantId = tenantObrigatorio();
        boolean ativo = request.ativo() == null || request.ativo();
        if (ativo) {
            garantirUnicoAtivo(tenantId, request.unitId(), request.anoLetivo(), null);
        }
        AccCalendario calendario = new AccCalendario();
        calendario.setTenantId(tenantId);
        calendario.setUnitId(request.unitId());
        calendario.setAnoLetivo(request.anoLetivo());
        calendario.setNome(request.nome().trim());
        calendario.setAtivo(ativo);
        AccCalendario salvo = calendarioRepository.save(calendario);
        consultaService.invalidar(tenantId, salvo.getUnitId(), salvo.getAnoLetivo());
        return CalendarioResponse.from(salvo);
    }

    @Transactional
    public CalendarioResponse update(UUID id, CalendarioRequest request) {
        UUID tenantId = tenantObrigatorio();
        AccCalendario calendario = buscar(id);
        Integer anoAnterior = calendario.getAnoLetivo();
        UUID unidadeAnterior = calendario.getUnitId();

        boolean ativo = request.ativo() == null || request.ativo();
        if (ativo) {
            garantirUnicoAtivo(tenantId, request.unitId(), request.anoLetivo(), calendario.getId());
        }
        calendario.setUnitId(request.unitId());
        calendario.setAnoLetivo(request.anoLetivo());
        calendario.setNome(request.nome().trim());
        calendario.setAtivo(ativo);
        AccCalendario salvo = calendarioRepository.save(calendario);

        // Trocar de unidade ou de ano muda dois recortes do cache, nao um.
        consultaService.invalidar(tenantId, unidadeAnterior, anoAnterior);
        consultaService.invalidar(tenantId, salvo.getUnitId(), salvo.getAnoLetivo());
        return CalendarioResponse.from(salvo);
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = tenantObrigatorio();
        AccCalendario calendario = buscar(id);
        long dias = diaRepository.countByTenantIdAndCalendarioIdAndDeletedFalse(tenantId, calendario.getId());
        if (dias > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Nao e possivel excluir: o calendario tem " + dias
                            + " dia(s) lancado(s). Desative-o em vez de excluir.");
        }
        calendario.setDeleted(true);
        calendarioRepository.save(calendario);
        consultaService.invalidar(tenantId, calendario.getUnitId(), calendario.getAnoLetivo());
    }

    /**
     * Dois calendarios ativos para o mesmo escopo e ano tornariam a resposta de
     * ehDiaLetivo dependente da ordem de leitura. Barramos no cadastro.
     */
    private void garantirUnicoAtivo(UUID tenantId, UUID unitId, Integer ano, UUID idAtual) {
        List<AccCalendario> ativos = unitId == null
                ? calendarioRepository.ativosGlobais(tenantId, ano)
                : calendarioRepository.ativosDaUnidade(tenantId, unitId, ano);
        boolean conflito = ativos.stream().anyMatch(c -> !Objects.equals(c.getId(), idAtual));
        if (conflito) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ja existe um calendario ativo para " + (unitId == null ? "toda a escola" : "esta unidade")
                            + " no ano letivo " + ano + ". Desative o outro antes.");
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
