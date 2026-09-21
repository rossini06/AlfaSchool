package br.com.alfaschool.backend.application.modulo;

import br.com.alfaschool.backend.domain.modulo.Modulo;
import br.com.alfaschool.backend.domain.modulo.TenantModulo;
import br.com.alfaschool.backend.infrastructure.persistence.repository.ModuloRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.TenantModuloRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O perfil de teste nao roda Flyway, entao o catalogo de modulos comeca
 * vazio: cada teste monta o que precisa.
 */
@SpringBootTest
@ActiveProfiles("test")
class ModuloServiceTest {

    @Autowired
    private ModuloService moduloService;

    @Autowired
    private ModuloRepository moduloRepository;

    @Autowired
    private TenantModuloRepository tenantModuloRepository;

    private UUID tenant;

    @BeforeEach
    void setUp() {
        tenantModuloRepository.deleteAll();
        moduloRepository.deleteAll();
        tenant = UUID.randomUUID();
        catalogo("PORTAL", 40);
        catalogo("ACCESS", 10);
        catalogo("PEDAGOGICO", 20);
    }

    @Test
    void semContratacaoNaoDevolveNada() {
        assertThat(moduloService.codigosVigentes(tenant)).isEmpty();
        assertThat(moduloService.codigosVigentes(null)).isEmpty();
    }

    @Test
    void devolveSoOsVigentesNaOrdemDoCatalogo() {
        contratar("PORTAL", true, null);
        contratar("ACCESS", true, null);
        // Inativo e vencido nao contam, mesmo com a linha no banco.
        contratar("PEDAGOGICO", false, null);
        TenantModulo vencido = contratar("FINANCEIRO", true, Instant.now().minus(1, ChronoUnit.DAYS));
        assertThat(vencido.vigente()).isFalse();

        assertThat(moduloService.codigosVigentes(tenant)).containsExactly("ACCESS", "PORTAL");
    }

    @Test
    void naoVazaContratacaoDeOutroTenant() {
        contratar("ACCESS", true, null);

        assertThat(moduloService.codigosVigentes(UUID.randomUUID())).isEmpty();
    }

    @Test
    void garantirTodosCriaOQueFaltaEReativaOQueVenceu() {
        contratar("ACCESS", false, null);
        contratar("PORTAL", true, Instant.now().minus(1, ChronoUnit.HOURS));

        moduloService.garantirTodosVigentes(tenant);

        assertThat(moduloService.codigosVigentes(tenant)).containsExactly("ACCESS", "PEDAGOGICO", "PORTAL");
        // Idempotente: rodar de novo nao duplica linha (a unicidade
        // tenant+modulo e' garantida no MySQL, nao no H2 — por isso o assert).
        moduloService.garantirTodosVigentes(tenant);
        List<TenantModulo> linhas = tenantModuloRepository.findByTenantIdAndDeletedFalse(tenant);
        assertThat(linhas).hasSize(3);
        assertThat(linhas).allMatch(TenantModulo::vigente);
        assertThat(linhas).allMatch(l -> l.getExpiraEm() == null);
    }

    private void catalogo(String codigo, int ordem) {
        Modulo m = new Modulo();
        m.setCodigo(codigo);
        m.setNome(codigo);
        m.setOrdem(ordem);
        moduloRepository.save(m);
    }

    private TenantModulo contratar(String codigo, boolean ativo, Instant expiraEm) {
        TenantModulo tm = new TenantModulo();
        tm.setTenantId(tenant);
        tm.setModuloCodigo(codigo);
        tm.setAtivo(ativo);
        tm.setAtivadoEm(Instant.now());
        tm.setExpiraEm(expiraEm);
        return tenantModuloRepository.save(tm);
    }
}
