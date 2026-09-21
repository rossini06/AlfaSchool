package br.com.alfaschool.backend.domain.role;

import br.com.alfaschool.backend.domain.shared.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "roles")
public class Role extends BaseEntity {

    @Column(nullable = false, length = 80)
    private String name;

    /** Nome legivel para a tela ("Coordenação"); `name` e' a chave tecnica. */
    @Column(length = 80)
    private String rotulo;

    @Column(length = 255)
    private String description;

    /**
     * Perfil que o proprio sistema mantem (DIRETOR, COORDENACAO, ...). Nao
     * pode ser excluido: apagar um deles deixaria usuarios sem perfil algum
     * e, com isso, sem acesso a nada. Perfil criado pela escola vem false.
     */
    @Column(nullable = false)
    private boolean sistema = false;

    /**
     * Perfil de sistema editado explicitamente pela tela ao menos uma vez.
     * Enquanto for false, um perfil de sistema sem permissao gravada cai no
     * conjunto padrao (fallback do PermissaoService). Depois de personalizado,
     * o que estiver gravado — inclusive vazio — e' o que vale.
     */
    @jakarta.persistence.Column(nullable = false)
    private boolean personalizado = false;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "role_permissions",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<Permission> permissions = new HashSet<>();

    public String getName() {
        return name;
    }

    public String getRotulo() { return rotulo; }

    public void setRotulo(String rotulo) { this.rotulo = rotulo; }

    public boolean isSistema() { return sistema; }

    public void setSistema(boolean sistema) { this.sistema = sistema; }
    public boolean isPersonalizado() { return personalizado; }
    public void setPersonalizado(boolean personalizado) { this.personalizado = personalizado; }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Set<Permission> getPermissions() {
        return permissions;
    }
}
