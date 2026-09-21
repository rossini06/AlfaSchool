package br.com.alfaschool.backend.application.tenant;

import br.com.alfaschool.backend.application.modulo.ModuloService;
import br.com.alfaschool.backend.application.modulo.dto.ModuloContratacaoResponse;
import br.com.alfaschool.backend.application.tenant.dto.TenantCreateRequest;
import br.com.alfaschool.backend.application.tenant.dto.TenantResponse;
import br.com.alfaschool.backend.application.tenant.dto.TenantUpdateRequest;
import br.com.alfaschool.backend.domain.role.Role;
import br.com.alfaschool.backend.domain.tenant.Tenant;
import br.com.alfaschool.backend.domain.user.UserAccount;
import br.com.alfaschool.backend.infrastructure.persistence.repository.RoleRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.TenantRepository;
import br.com.alfaschool.backend.infrastructure.persistence.repository.UserRepository;
import br.com.alfaschool.backend.security.permissao.PerfilEscolar;
import br.com.alfaschool.backend.security.filter.TenantContext;
import br.com.alfaschool.backend.security.permissao.PermissaoSeeder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Redes de ensino, vistas pela administracao da Alfa (painel SaaS).
 *
 * Tudo aqui roda SEM o filtro de tenant (TenantContext.semFiltro): quem
 * chama e' o superadministrador, que vive no tenant mestre, e com o filtro
 * a listagem so' devolvia o mestre e a criacao nao achava os perfis da
 * rede nova.
 *
 * Criar uma rede e' mais do que inserir a linha: sem os perfis padrao o
 * seeder so' os criaria no proximo boot, sem um administrador ninguem
 * consegue entrar, e sem modulo contratado toda tela do Access responde
 * 403. Os tres passos ficam aqui, na mesma transacao.
 */
@Service
public class TenantService {

    private final TenantRepository tenantRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PermissaoSeeder permissaoSeeder;
    private final ModuloService moduloService;

    public TenantService(TenantRepository tenantRepository,
                         RoleRepository roleRepository,
                         UserRepository userRepository,
                         PasswordEncoder passwordEncoder,
                         PermissaoSeeder permissaoSeeder,
                         ModuloService moduloService) {
        this.tenantRepository = tenantRepository;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.permissaoSeeder = permissaoSeeder;
        this.moduloService = moduloService;
    }

    @Transactional(readOnly = true)
    public Page<TenantResponse> list(Pageable pageable) {
        return TenantContext.semFiltro(() -> tenantRepository.findAll(pageable)
                .map(t -> TenantResponse.from(t, moduloService.codigosVigentes(t.getTenantId()))));
    }

    @Transactional(readOnly = true)
    public TenantResponse findById(UUID id) {
        return TenantContext.semFiltro(() -> {
            Tenant tenant = buscar(id);
            return TenantResponse.from(tenant, moduloService.codigosVigentes(tenant.getTenantId()));
        });
    }

    @Transactional
    public TenantResponse criar(TenantCreateRequest request) {
        return TenantContext.semFiltro(() -> criarSemFiltro(request));
    }

    private TenantResponse criarSemFiltro(TenantCreateRequest request) {
        String documento = request.document().trim();
        if (tenantRepository.existsByDocument(documento)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe uma rede com este CNPJ");
        }

        Tenant tenant = new Tenant();
        tenant.setName(request.name().trim());
        tenant.setDocument(documento);
        tenant.setActive(true);
        // Mesmo padrao do bootstrap: o tenant_id e' o identificador que
        // todas as outras tabelas carregam; o id da linha e' outro.
        tenant.setTenantId(UUID.randomUUID());
        tenant = tenantRepository.save(tenant);
        UUID tenantId = tenant.getTenantId();

        permissaoSeeder.semear(tenantId);
        criarAdministrador(tenantId, request);

        Set<String> modulos = new HashSet<>(request.modulos() == null ? List.of() : request.modulos());
        moduloService.definir(tenantId, modulos);

        return TenantResponse.from(tenant, moduloService.codigosVigentes(tenantId));
    }

    @Transactional
    public TenantResponse atualizar(UUID id, TenantUpdateRequest request) {
        return TenantContext.semFiltro(() -> atualizarSemFiltro(id, request));
    }

    private TenantResponse atualizarSemFiltro(UUID id, TenantUpdateRequest request) {
        Tenant tenant = buscarEditavel(id);
        String documento = request.document().trim();
        tenantRepository.findByDocument(documento)
                .filter(outro -> !outro.getId().equals(tenant.getId()))
                .ifPresent(outro -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe uma rede com este CNPJ");
                });
        tenant.setName(request.name().trim());
        tenant.setDocument(documento);
        Tenant salvo = tenantRepository.save(tenant);
        return TenantResponse.from(salvo, moduloService.codigosVigentes(salvo.getTenantId()));
    }

    @Transactional
    public TenantResponse changeStatus(UUID id, String status) {
        return TenantContext.semFiltro(() -> {
            Tenant tenant = buscarEditavel(id);
            boolean active = "ATIVO".equalsIgnoreCase(status) || "ACTIVE".equalsIgnoreCase(status) || "true".equalsIgnoreCase(status);
            tenant.setActive(active);
            Tenant salvo = tenantRepository.save(tenant);
            return TenantResponse.from(salvo, moduloService.codigosVigentes(salvo.getTenantId()));
        });
    }

    @Transactional(readOnly = true)
    public List<ModuloContratacaoResponse> listarModulos(UUID id) {
        return TenantContext.semFiltro(() -> moduloService.listar(buscar(id).getTenantId()));
    }

    @Transactional
    public List<ModuloContratacaoResponse> definirModulos(UUID id, List<String> codigos) {
        return TenantContext.semFiltro(() -> {
            Tenant tenant = buscarEditavel(id);
            return moduloService.definir(tenant.getTenantId(), new HashSet<>(codigos));
        });
    }

    private void criarAdministrador(UUID tenantId, TenantCreateRequest request) {
        String email = request.adminEmail().trim().toLowerCase();
        Role diretor = roleRepository.findByTenantIdAndNameIgnoreCase(tenantId, PerfilEscolar.DIRETOR.name())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Perfil de diretor não foi criado para a nova rede"));
        UserAccount admin = new UserAccount();
        admin.setTenantId(tenantId);
        admin.setName(request.adminNome().trim());
        admin.setEmail(email);
        admin.setPassword(passwordEncoder.encode(request.adminSenha()));
        // Senha inicial e' combinada por telefone ou e-mail; a pessoa troca
        // no primeiro acesso e a Alfa deixa de conhece-la.
        admin.setMustChangePassword(true);
        admin.setActive(true);
        admin.getRoles().add(diretor);
        userRepository.save(admin);
    }

    private Tenant buscar(UUID id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rede de ensino não encontrada"));
    }

    /** O tenant mestre nao e' escola: nao se edita, suspende nem tira modulo dele. */
    private Tenant buscarEditavel(UUID id) {
        Tenant tenant = buscar(id);
        if (TenantResponse.DOCUMENTO_DO_MESTRE.equals(tenant.getDocument())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O tenant mestre da Alfa não pode ser alterado");
        }
        return tenant;
    }
}
