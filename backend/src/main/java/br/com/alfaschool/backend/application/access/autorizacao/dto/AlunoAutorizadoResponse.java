package br.com.alfaschool.backend.application.access.autorizacao.dto;

import java.util.UUID;

/**
 * Resposta do leitor da portaria: identificada a pessoa, quais alunos ela
 * pode retirar NESTE momento. Cada item ja passou pela verificacao completa
 * (restricao, permissao, status, vigencia, dia, horario), entao a lista
 * vazia significa "nenhum" e nao "erro".
 */
public record AlunoAutorizadoResponse(
        UUID alunoId,
        String alunoNome,
        String alunoFoto,
        UUID autorizacaoId
) {
}
