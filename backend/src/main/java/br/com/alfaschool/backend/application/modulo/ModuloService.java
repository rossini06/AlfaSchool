package br.com.alfaschool.backend.application.modulo;

import br.com.alfaschool.backend.application.modulo.dto.ModuloContratacaoResponse;
import br.com.alfaschool.backend.domain.modulo.Modulo;
import br.com.alfaschool.backend.domain.modulo.TenantModulo;
import br.com.alfaschool.backend.infrastructure.persistence.repository.ModuloRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.TenantModuloRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Consulta e provisionamento de modulos contratados por tenant.
 *
 * O {@link ModuloGuard} decide endpoint a endpoint; aqui fica o que a tela
 * precisa saber de uma vez (o que esta vigente) e o que o bootstrap
 * precisa garantir (o tenant mestre com tudo).
 */
@Service
public class ModuloService {

    private final ModuloRepository moduloRepository;
    private final TenantModuloRepository tenantModuloRepository;

    public ModuloService(ModuloRepository moduloRepository,
                         TenantModuloRepository tenantModuloRepository) {
        this.moduloRepository = moduloRepository;
        this.tenantModuloRepository = tenantModuloRepository;
    }

    /** Codigos vigentes, na ordem do catalogo. Tenant nulo = nada. */
    @Transactional(readOnly = true)
    public List<String> codigosVigentes(UUID tenantId) {
        if (tenantId == null) {
            return List.of();
        }
        List<String> vigentes = tenantModuloRepository.findByTenantIdAndDeletedFalse(tenantId).stream()
                .filter(TenantModulo::vigente)
                .map(TenantModulo::getModuloCodigo)
                .toList();
        if (vigentes.isEmpty()) {
            return List.of();
        }
        // Ordem do catalogo, para a resposta nao mudar conforme a ordem de
        // insercao no banco.
        List<String> ordenados = moduloRepository.findAllByOrderByOrdemAsc().stream()
                .map(Modulo::getCodigo)
                .filter(vigentes::contains)
                .collect(Collectors.toList());
        vigentes.stream().filter(c -> !ordenados.contains(c)).forEach(ordenados::add);
        return List.copyOf(ordenados);
    }

    /** Catalogo inteiro, com a situacao de cada modulo no tenant. */
    @Transactional(readOnly = true)
    public List<ModuloContratacaoResponse> listar(UUID tenantId) {
        Map<String, TenantModulo> existentes = contratacoesPorCodigo(tenantId);
        return moduloRepository.findAllByOrderByOrdemAsc().stream()
                .map(m -> ModuloContratacaoResponse.from(m, existentes.get(m.getCodigo())))
                .toList();
    }

    /**
     * Define o conjunto contratado: o que esta na lista fica vigente sem
     * prazo, o que nao esta e' desativado (a linha fica, com historico).
     * Codigo fora do catalogo e' erro, nao silencio — um typo aqui
     * trancaria a escola fora de um modulo pago.
     */
    @Transactional
    public List<ModuloContratacaoResponse> definir(UUID tenantId, Set<String> codigos) {
        List<Modulo> catalogo = moduloRepository.findAllByOrderByOrdemAsc();
        Set<String> conhecidos = catalogo.stream().map(Modulo::getCodigo).collect(Collectors.toSet());
        for (String codigo : codigos) {
            if (!conhecidos.contains(codigo)) {
                throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.BAD_REQUEST, "Módulo desconhecido: " + codigo);
            }
        }
        Map<String, TenantModulo> existentes = contratacoesPorCodigo(tenantId);
        for (Modulo modulo : catalogo) {
            boolean querVigente = codigos.contains(modulo.getCodigo());
            TenantModulo contratacao = existentes.get(modulo.getCodigo());
            if (querVigente) {
                if (contratacao != null && contratacao.vigente()) {
                    continue;
                }
                if (contratacao == null) {
                    contratacao = new TenantModulo();
                    contratacao.setTenantId(tenantId);
                    contratacao.setModuloCodigo(modulo.getCodigo());
                }
                contratacao.setAtivo(true);
                contratacao.setExpiraEm(null);
                contratacao.setAtivadoEm(Instant.now());
                tenantModuloRepository.save(contratacao);
            } else if (contratacao != null && contratacao.isAtivo()) {
                contratacao.setAtivo(false);
                tenantModuloRepository.save(contratacao);
            }
        }
        return listar(tenantId);
    }

    private Map<String, TenantModulo> contratacoesPorCodigo(UUID tenantId) {
        return tenantModuloRepository.findByTenantIdAndDeletedFalse(tenantId).stream()
                .collect(Collectors.toMap(TenantModulo::getModuloCodigo, Function.identity(), (a, b) -> a));
    }

    /**
     * Deixa TODOS os modulos do catalogo vigentes para o tenant, sem prazo.
     *
     * Feito para o tenant mestre: o superadministrador precisa enxergar o
     * sistema inteiro para dar suporte e demonstrar, e sem isto ele batia
     * em 403 em cada tela do Access. Idempotente: contratacao que ja existe
     * e' reativada, nao duplicada.
     */
    @Transactional
    public void garantirTodosVigentes(UUID tenantId) {
        Map<String, TenantModulo> existentes = contratacoesPorCodigo(tenantId);

        for (Modulo modulo : moduloRepository.findAllByOrderByOrdemAsc()) {
            TenantModulo contratacao = existentes.get(modulo.getCodigo());
            if (contratacao != null && contratacao.vigente()) {
                continue;
            }
            if (contratacao == null) {
                contratacao = new TenantModulo();
                contratacao.setTenantId(tenantId);
                contratacao.setModuloCodigo(modulo.getCodigo());
            }
            contratacao.setAtivo(true);
            contratacao.setExpiraEm(null);
            contratacao.setAtivadoEm(Instant.now());
            tenantModuloRepository.save(contratacao);
        }
    }
}
