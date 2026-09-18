package br.com.alfaschool.backend.application.access.painel;

import br.com.alfaschool.backend.application.access.painel.dto.PainelDispositivoCriadoResponse;
import br.com.alfaschool.backend.application.access.painel.dto.PainelDispositivoRequest;
import br.com.alfaschool.backend.application.access.painel.dto.PainelDispositivoResponse;
import br.com.alfaschool.backend.application.access.painel.dto.PainelFonteRequest;
import br.com.alfaschool.backend.application.access.painel.dto.PainelFonteResponse;
import br.com.alfaschool.backend.application.access.painel.dto.PainelRequest;
import br.com.alfaschool.backend.application.access.painel.dto.PainelResponse;
import br.com.alfaschool.backend.application.access.retirada.ContextoAcesso;
import br.com.alfaschool.backend.domain.access.painel.AccPainel;
import br.com.alfaschool.backend.domain.access.painel.AccPainelDispositivo;
import br.com.alfaschool.backend.domain.access.painel.AccPainelFonte;
import br.com.alfaschool.backend.domain.access.shared.EscopoPainel;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccPainelDispositivoRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccPainelFonteRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AccPainelRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Administracao dos paineis: a tela, o recorte dela e as TVs autorizadas.
 */
@Service
public class PainelService {

    private final AccPainelRepository painelRepository;
    private final AccPainelFonteRepository fonteRepository;
    private final AccPainelDispositivoRepository dispositivoRepository;
    private final PainelTokenService tokenService;

    public PainelService(AccPainelRepository painelRepository,
                         AccPainelFonteRepository fonteRepository,
                         AccPainelDispositivoRepository dispositivoRepository,
                         PainelTokenService tokenService) {
        this.painelRepository = painelRepository;
        this.fonteRepository = fonteRepository;
        this.dispositivoRepository = dispositivoRepository;
        this.tokenService = tokenService;
    }

    // ---------------------------------------------------------------
    // Painel
    // ---------------------------------------------------------------

    /**
     * Lista os paineis ja' com as fontes. As fontes de todos vem numa
     * consulta so': com um SELECT por painel, a tela de administracao de uma
     * rede com 60 telas faria 61 consultas.
     */
    public Page<PainelResponse> listar(Pageable pageable) {
        UUID tenantId = ContextoAcesso.tenantObrigatorio();
        Page<AccPainel> pagina = painelRepository.findByTenantIdAndDeletedFalseOrderByNomeAsc(tenantId, pageable);
        List<UUID> ids = pagina.getContent().stream().map(AccPainel::getId).toList();
        if (ids.isEmpty()) {
            return pagina.map(PainelResponse::from);
        }
        Map<UUID, List<PainelFonteResponse>> porPainel = fonteRepository.findByPaineis(tenantId, ids).stream()
                .collect(Collectors.groupingBy(AccPainelFonte::getPainelId,
                        Collectors.mapping(PainelFonteResponse::from, Collectors.toList())));
        return pagina.map(p -> PainelResponse.from(p, porPainel.getOrDefault(p.getId(), List.of())));
    }

    public PainelResponse buscar(UUID id) {
        UUID tenantId = ContextoAcesso.tenantObrigatorio();
        AccPainel painel = carregar(id);
        return PainelResponse.from(painel, fontesDe(tenantId, painel.getId()));
    }

    @Transactional
    public PainelResponse criar(PainelRequest request) {
        UUID tenantId = ContextoAcesso.tenantObrigatorio();
        if (painelRepository.existsByTenantIdAndSlugAndDeletedFalse(tenantId, request.slug())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ja existe um painel com este slug");
        }
        AccPainel painel = new AccPainel();
        painel.setTenantId(tenantId);
        aplicar(painel, request);
        painel.setCreatedBy(ContextoAcesso.userIdOuNulo());
        return PainelResponse.from(painelRepository.save(painel));
    }

    @Transactional
    public PainelResponse atualizar(UUID id, PainelRequest request) {
        UUID tenantId = ContextoAcesso.tenantObrigatorio();
        AccPainel painel = carregar(id);
        if (!painel.getSlug().equals(request.slug())
                && painelRepository.existsByTenantIdAndSlugAndDeletedFalse(tenantId, request.slug())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ja existe um painel com este slug");
        }
        aplicar(painel, request);
        painel.setUpdatedBy(ContextoAcesso.userIdOuNulo());
        return PainelResponse.from(painelRepository.save(painel), fontesDe(tenantId, painel.getId()));
    }

    @Transactional
    public void remover(UUID id) {
        AccPainel painel = carregar(id);
        painel.setDeleted(true);
        painel.setAtivo(false);
        painel.setUpdatedBy(ContextoAcesso.userIdOuNulo());
        painelRepository.save(painel);
    }

    private void aplicar(AccPainel painel, PainelRequest request) {
        painel.setUnitId(request.unitId());
        painel.setNome(request.nome());
        painel.setSlug(request.slug());
        painel.setTipo(request.tipo());
        if (request.exibeFoto() != null) {
            painel.setExibeFoto(request.exibeFoto());
        }
        if (request.retencaoSeg() != null) {
            painel.setRetencaoSeg(request.retencaoSeg());
        }
        if (request.ativo() != null) {
            painel.setAtivo(request.ativo());
        }
    }

    // ---------------------------------------------------------------
    // Fontes
    // ---------------------------------------------------------------

    public List<PainelFonteResponse> fontes(UUID painelId) {
        UUID tenantId = ContextoAcesso.tenantObrigatorio();
        carregar(painelId);
        return fontesDe(tenantId, painelId);
    }

    @Transactional
    public PainelFonteResponse adicionarFonte(UUID painelId, PainelFonteRequest request) {
        UUID tenantId = ContextoAcesso.tenantObrigatorio();
        carregar(painelId);
        // Fonte sem referencia so' faz sentido para a unidade inteira. Sem
        // esta trava, uma fonte SALA com referencia nula viraria "todas as
        // salas" — o oposto do que o cliente pediu.
        if (request.escopo() != EscopoPainel.UNIDADE && request.referenciaId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Informe a referencia da fonte para o escopo " + request.escopo());
        }
        AccPainelFonte fonte = new AccPainelFonte();
        fonte.setTenantId(tenantId);
        fonte.setPainelId(painelId);
        fonte.setEscopo(request.escopo());
        fonte.setReferenciaId(request.referenciaId());
        fonte.setCreatedAt(Instant.now());
        fonte.setUpdatedAt(Instant.now());
        return PainelFonteResponse.from(fonteRepository.save(fonte));
    }

    @Transactional
    public void removerFonte(UUID painelId, UUID fonteId) {
        UUID tenantId = ContextoAcesso.tenantObrigatorio();
        carregar(painelId);
        AccPainelFonte fonte = fonteRepository.findByIdAndTenantId(fonteId, tenantId)
                .filter(f -> f.getPainelId().equals(painelId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fonte nao encontrada"));
        fonteRepository.delete(fonte);
    }

    // ---------------------------------------------------------------
    // Dispositivos (TVs)
    // ---------------------------------------------------------------

    public List<PainelDispositivoResponse> dispositivos(UUID painelId) {
        UUID tenantId = ContextoAcesso.tenantObrigatorio();
        carregar(painelId);
        return dispositivoRepository
                .findByTenantIdAndPainelIdAndDeletedFalseOrderByNomeAsc(tenantId, painelId)
                .stream().map(PainelDispositivoResponse::from).toList();
    }

    /**
     * Gera a TV e devolve o token em claro UMA VEZ. Depois disso so' existe
     * o hash.
     */
    @Transactional
    public PainelDispositivoCriadoResponse criarDispositivo(UUID painelId, PainelDispositivoRequest request) {
        UUID tenantId = ContextoAcesso.tenantObrigatorio();
        carregar(painelId);
        PainelTokenService.TokenGerado gerado = tokenService.gerar();

        AccPainelDispositivo dispositivo = new AccPainelDispositivo();
        dispositivo.setTenantId(tenantId);
        dispositivo.setPainelId(painelId);
        dispositivo.setNome(request.nome());
        dispositivo.setTokenHash(gerado.tokenHash());
        dispositivo.setTokenPrefixo(gerado.tokenPrefixo());
        dispositivo.setCreatedBy(ContextoAcesso.userIdOuNulo());
        AccPainelDispositivo salvo = dispositivoRepository.save(dispositivo);

        return PainelDispositivoCriadoResponse.de(
                PainelDispositivoResponse.from(salvo), gerado.tokenEmClaro());
    }

    /**
     * Revogacao imediata. Nao apaga o registro: a coordenacao precisa ver
     * que aquela TV existiu, quando acessou pela ultima vez e de qual IP.
     */
    @Transactional
    public PainelDispositivoResponse revogarDispositivo(UUID painelId, UUID dispositivoId) {
        UUID tenantId = ContextoAcesso.tenantObrigatorio();
        carregar(painelId);
        AccPainelDispositivo dispositivo = dispositivoRepository
                .findByIdAndTenantIdAndDeletedFalse(dispositivoId, tenantId)
                .filter(d -> d.getPainelId().equals(painelId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dispositivo nao encontrado"));
        if (dispositivo.isRevogado()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Dispositivo ja revogado");
        }
        dispositivo.setRevogado(true);
        dispositivo.setRevogadoEm(Instant.now());
        dispositivo.setRevogadoPor(ContextoAcesso.userIdObrigatorio());
        dispositivo.setUpdatedBy(dispositivo.getRevogadoPor());
        return PainelDispositivoResponse.from(dispositivoRepository.save(dispositivo));
    }

    // ---------------------------------------------------------------

    private List<PainelFonteResponse> fontesDe(UUID tenantId, UUID painelId) {
        return fonteRepository.findByTenantIdAndPainelId(tenantId, painelId)
                .stream().map(PainelFonteResponse::from).toList();
    }

    private AccPainel carregar(UUID id) {
        UUID tenantId = ContextoAcesso.tenantObrigatorio();
        return painelRepository.findByIdAndTenantIdAndDeletedFalse(id, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Painel nao encontrado"));
    }
}
