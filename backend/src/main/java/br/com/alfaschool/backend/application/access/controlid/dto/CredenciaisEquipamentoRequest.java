package br.com.alfaschool.backend.application.access.controlid.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Credenciais de administracao do leitor. A senha entra por aqui, e' logo
 * cifrada com AES-GCM e nunca mais sai em claro por nenhuma rota.
 */
public record CredenciaisEquipamentoRequest(
        @NotBlank(message = "Login do equipamento é obrigatório.") String login,
        @NotBlank(message = "Senha do equipamento é obrigatória.") String senha) {
}
