package br.com.alfaschool.backend.application.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * Cadastro de usuario.
 *
 * Os nomes acompanham o resto da API, que e' em portugues. Antes este era o
 * unico DTO em ingles (name/email/password) e a tela mandava em portugues —
 * um dos motivos de a tela de Usuarios nunca ter funcionado.
 *
 * @param perfis quem o usuario e' dentro da escola. Vazio cria alguem que
 *               entra e nao ve nada, o que e' legitimo para um cadastro
 *               feito antes de a funcao estar definida.
 */
public record CreateUserRequest(
        @NotBlank(message = "Informe o nome") @Size(max = 120) String nome,
        @NotBlank(message = "Informe o e-mail") @Email(message = "E-mail inválido") @Size(max = 160) String email,
        @NotBlank(message = "Defina uma senha") @Size(min = 8, max = 72,
                message = "A senha precisa ter ao menos 8 caracteres") String senha,
        Boolean ativo,
        List<UUID> perfis
) {
}
