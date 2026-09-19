package br.com.alfaschool.backend.application.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * Edicao de usuario pela tela de administracao.
 *
 * @param senha opcional: vazio mantem a senha atual. Obrigar a redigitar a
 *              senha para trocar um nome levaria a escola a usar sempre a
 *              mesma senha fraca.
 * @param perfis substitui o conjunto de perfis. Lista vazia deixa o usuario
 *               SEM perfil — e sem perfil ele entra e nao ve nada, que e' o
 *               comportamento correto para quem perdeu a funcao mas ainda
 *               nao foi desligado.
 */
public record AtualizarUsuarioRequest(
        @NotBlank(message = "Informe o nome") @Size(max = 120) String nome,
        @NotBlank(message = "Informe o e-mail") @Email(message = "E-mail inválido") @Size(max = 160) String email,
        @Size(min = 8, max = 72, message = "A senha precisa ter ao menos 8 caracteres") String senha,
        Boolean ativo,
        List<UUID> perfis
) {
}
