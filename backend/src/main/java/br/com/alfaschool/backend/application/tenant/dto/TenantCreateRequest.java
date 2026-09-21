package br.com.alfaschool.backend.application.tenant.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Nova rede de ensino. Nasce com os perfis padrao, um administrador (que
 * troca a senha no primeiro acesso) e os modulos marcados — uma rede sem
 * usuario e sem modulo nao tem como ser usada por ninguem.
 */
public record TenantCreateRequest(
        @NotBlank(message = "O nome da rede é obrigatório") @Size(max = 160) String name,
        @NotBlank(message = "O CNPJ é obrigatório") @Size(max = 40) String document,
        @NotBlank(message = "O nome do administrador é obrigatório") @Size(max = 120) String adminNome,
        @NotBlank(message = "O e-mail do administrador é obrigatório") @Email(message = "Informe um e-mail válido") String adminEmail,
        @NotBlank(message = "A senha inicial é obrigatória") @Size(min = 8, message = "A senha inicial precisa de 8 caracteres") String adminSenha,
        List<String> modulos
) {
}
