package br.com.alfaschool.backend.application.role;

import br.com.alfaschool.backend.application.role.dto.CreateRoleRequest;
import br.com.alfaschool.backend.application.role.dto.RoleResponse;
import br.com.alfaschool.backend.domain.role.Role;
import br.com.alfaschool.backend.infrastructure.persistence.repository.RoleRepository;
import br.com.alfaschool.backend.security.filter.TenantContext;
import br.com.alfaschool.backend.security.permissao.NomesDePerfilReservados;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class RoleApplicationService {

    private final RoleRepository roleRepository;
    private final br.com.alfaschool.backend.application.shared.AuditService auditService;

    public RoleApplicationService(RoleRepository roleRepository, br.com.alfaschool.backend.application.shared.AuditService auditService) {
        this.roleRepository = roleRepository;
        this.auditService = auditService;
    }

    @Transactional
    public RoleResponse create(CreateRoleRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Tenant não identificado");
        }

        // Nome reservado nao e' conflito de cadastro, e' tentativa de
        // assumir um perfil que o sistema reconhece. Ver NomesDePerfilReservados.
        if (NomesDePerfilReservados.reservado(request.name())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "'" + request.name().trim() + "' é um nome de perfil reservado do sistema. "
                  + "Escolha outro nome. Para ajustar o que um perfil do sistema pode fazer, "
                  + "use a tela de Perfis e Permissões.");
        }

        roleRepository.findByTenantIdAndNameIgnoreCase(tenantId, request.name())
                .ifPresent(role -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Role já existe para este tenant");
                });

        Role role = new Role();
        role.setTenantId(tenantId);
        role.setName(request.name().toUpperCase());
        role.setDescription(request.description());
        Role saved = roleRepository.save(role);
        auditService.registrarAcao("PERFIL_CRIADO", "ROLE", saved.getId());

        return new RoleResponse(saved.getId(), saved.getTenantId(), saved.getName(), saved.getDescription());
    }
}
