package br.com.alfaschool.backend.application.modulo;

import br.com.alfaschool.backend.domain.modulo.Modulo;
import br.com.alfaschool.backend.domain.modulo.TenantModulo;
import br.com.alfaschool.backend.infrastructure.persistence.repository.ModuloRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.TenantModuloRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
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
        Map<String, TenantModulo> existentes = tenantModuloRepository.findByTenantIdAndDeletedFalse(tenantId).stream()
                .collect(Collectors.toMap(TenantModulo::getModuloCodigo, Function.identity(), (a, b) -> a));

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
