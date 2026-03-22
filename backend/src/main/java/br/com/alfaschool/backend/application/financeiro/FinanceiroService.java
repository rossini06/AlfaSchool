package br.com.alfaschool.backend.application.financeiro;

import br.com.alfaschool.backend.application.financeiro.dto.*;
import br.com.alfaschool.backend.domain.financeiro.*;
import br.com.alfaschool.backend.infrastructure.persistence.repository.*;
import br.com.alfaschool.backend.security.filter.TenantContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class FinanceiroService {

    private final PlanoFinanceiroRepository planoRepository;
    private final ContratoRepository contratoRepository;
    private final CobrancaRepository cobrancaRepository;

    public FinanceiroService(PlanoFinanceiroRepository planoRepository,
                              ContratoRepository contratoRepository,
                              CobrancaRepository cobrancaRepository) {
        this.planoRepository = planoRepository;
        this.contratoRepository = contratoRepository;
        this.cobrancaRepository = cobrancaRepository;
    }

    // ─── Planos ───────────────────────────────────────────────────────────────

    public Page<PlanoFinanceiroResponse> listPlanos(Pageable pageable) {
        return planoRepository.findByTenantIdAndDeletedFalse(requiredTenant(), pageable)
                .map(PlanoFinanceiroResponse::from);
    }

    public List<PlanoFinanceiroResponse> listPlanosAtivos() {
        return planoRepository.findByTenantIdAndDeletedFalseAndAtivoTrue(requiredTenant())
                .stream().map(PlanoFinanceiroResponse::from).toList();
    }

    @Transactional
    public PlanoFinanceiroResponse createPlano(PlanoFinanceiroRequest request) {
        UUID tenantId = requiredTenant();
        PlanoFinanceiro p = new PlanoFinanceiro();
        p.setTenantId(tenantId);
        p.setNome(request.nome());
        p.setValor(request.valor());
        if (request.periodicidade() != null) p.setPeriodicidade(request.periodicidade());
        p.setDescricao(request.descricao());
        if (request.ativo() != null) p.setAtivo(request.ativo());
        return PlanoFinanceiroResponse.from(planoRepository.save(p));
    }

    @Transactional
    public PlanoFinanceiroResponse updatePlano(UUID id, PlanoFinanceiroRequest request) {
        UUID tenantId = requiredTenant();
        PlanoFinanceiro p = planoRepository.findById(id)
                .filter(x -> tenantId.equals(x.getTenantId()) && !Boolean.TRUE.equals(x.getDeleted()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plano não encontrado"));
        p.setNome(request.nome());
        p.setValor(request.valor());
        if (request.periodicidade() != null) p.setPeriodicidade(request.periodicidade());
        p.setDescricao(request.descricao());
        if (request.ativo() != null) p.setAtivo(request.ativo());
        return PlanoFinanceiroResponse.from(planoRepository.save(p));
    }

    @Transactional
    public void deletePlano(UUID id) {
        UUID tenantId = requiredTenant();
        PlanoFinanceiro p = planoRepository.findById(id)
                .filter(x -> tenantId.equals(x.getTenantId()) && !Boolean.TRUE.equals(x.getDeleted()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plano não encontrado"));
        p.setDeleted(true);
        planoRepository.save(p);
    }

    // ─── Contratos ────────────────────────────────────────────────────────────

    public Page<ContratoResponse> listContratos(Pageable pageable) {
        return contratoRepository.findByTenantIdAndDeletedFalse(requiredTenant(), pageable)
                .map(ContratoResponse::from);
    }

    public List<ContratoResponse> listContratosByAluno(UUID alunoId) {
        return contratoRepository.findByTenantIdAndAlunoIdAndDeletedFalse(requiredTenant(), alunoId)
                .stream().map(ContratoResponse::from).toList();
    }

    @Transactional
    public ContratoResponse createContrato(ContratoRequest request) {
        UUID tenantId = requiredTenant();
        PlanoFinanceiro plano = planoRepository.findById(request.planoId())
                .filter(p -> tenantId.equals(p.getTenantId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plano não encontrado"));

        Contrato c = new Contrato();
        c.setTenantId(tenantId);
        c.setAlunoId(request.alunoId());
        c.setResponsavelId(request.responsavelId());
        c.setPlanoId(request.planoId());
        c.setMatriculaId(request.matriculaId());
        c.setDataInicio(request.dataInicio());
        c.setDataFim(request.dataFim());
        c.setObs(request.obs());
        c.setStatus("ativo");
        Contrato saved = contratoRepository.save(c);

        gerarCobrancasIniciais(saved, plano);
        return ContratoResponse.from(saved);
    }

    @Transactional
    public ContratoResponse encerrarContrato(UUID id) {
        UUID tenantId = requiredTenant();
        Contrato c = contratoRepository.findById(id)
                .filter(x -> tenantId.equals(x.getTenantId()) && !Boolean.TRUE.equals(x.getDeleted()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Contrato não encontrado"));
        c.setStatus("encerrado");
        c.setDataFim(LocalDate.now());
        return ContratoResponse.from(contratoRepository.save(c));
    }

    // ─── Cobranças ────────────────────────────────────────────────────────────

    public Page<CobrancaResponse> listCobrancas(Pageable pageable) {
        return cobrancaRepository.findByTenantIdAndDeletedFalse(requiredTenant(), pageable)
                .map(CobrancaResponse::from);
    }

    public List<CobrancaResponse> listCobrancasByAluno(UUID alunoId) {
        return cobrancaRepository.findByTenantIdAndAlunoIdAndDeletedFalse(requiredTenant(), alunoId)
                .stream().map(CobrancaResponse::from).toList();
    }

    @Transactional
    public CobrancaResponse registrarPagamento(UUID id, LocalDate dataPagamento) {
        UUID tenantId = requiredTenant();
        Cobranca c = cobrancaRepository.findById(id)
                .filter(x -> tenantId.equals(x.getTenantId()) && !Boolean.TRUE.equals(x.getDeleted()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cobrança não encontrada"));
        c.setStatus("pago");
        c.setDataPagamento(dataPagamento != null ? dataPagamento : LocalDate.now());
        return CobrancaResponse.from(cobrancaRepository.save(c));
    }

    // ─── Internos ─────────────────────────────────────────────────────────────

    private void gerarCobrancasIniciais(Contrato contrato, PlanoFinanceiro plano) {
        int meses = "anual".equalsIgnoreCase(plano.getPeriodicidade()) ? 12 : 1;
        List<Cobranca> cobrancas = new ArrayList<>();
        for (int i = 0; i < meses; i++) {
            LocalDate vencimento = contrato.getDataInicio().plusMonths(i).withDayOfMonth(
                    Math.min(contrato.getDataInicio().getDayOfMonth(), contrato.getDataInicio().plusMonths(i).lengthOfMonth()));
            String competencia = String.format("%02d/%04d", vencimento.getMonthValue(), vencimento.getYear());

            if (!cobrancaRepository.existsByTenantIdAndContratoIdAndCompetenciaAndDeletedFalse(
                    contrato.getTenantId(), contrato.getId(), competencia)) {
                Cobranca c = new Cobranca();
                c.setTenantId(contrato.getTenantId());
                c.setContratoId(contrato.getId());
                c.setAlunoId(contrato.getAlunoId());
                c.setValor(plano.getValor());
                c.setDescricao("Mensalidade " + competencia + " — " + plano.getNome());
                c.setVencimento(vencimento);
                c.setStatus("pendente");
                c.setCompetencia(competencia);
                cobrancas.add(c);
            }
        }
        cobrancaRepository.saveAll(cobrancas);
    }

    private UUID requiredTenant() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Tenant não identificado");
        return tenantId;
    }
}
