package br.com.alfaschool.backend.auth.dto;

public record LoginResponse(
        String token,
        String nome,
        String email,
        boolean superAdmin,
        String mensagem
) {
}
