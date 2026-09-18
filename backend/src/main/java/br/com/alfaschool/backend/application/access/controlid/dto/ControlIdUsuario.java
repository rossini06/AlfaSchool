package br.com.alfaschool.backend.application.access.controlid.dto;

/**
 * Linha da tabela "users" dentro do equipamento.
 *
 * O id e' o device_user_id vindo da sequencia POR TENANT, nunca o UUID da
 * pessoa: o firmware so' entende inteiro.
 *
 * registration e' string no firmware mesmo quando parece numero.
 */
public record ControlIdUsuario(long id, String registration, String nome) {

    public ControlIdUsuario {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("Nome do usuário do equipamento é obrigatório.");
        }
        // O firmware trunca em 60 caracteres sem avisar; truncar aqui deixa
        // o valor gravado igual ao valor exibido no leitor.
        if (nome.length() > 60) {
            nome = nome.substring(0, 60);
        }
        if (registration == null) {
            registration = String.valueOf(id);
        }
    }
}
