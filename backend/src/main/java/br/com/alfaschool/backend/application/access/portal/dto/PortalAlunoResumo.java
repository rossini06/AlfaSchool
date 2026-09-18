package br.com.alfaschool.backend.application.access.portal.dto;

import java.util.UUID;

/**
 * Aluno visivel para o responsavel autenticado.
 *
 * <p>Campos deliberadamente magros: o portal da familia nao e' a ficha do
 * aluno. Nada de CPF, endereco, observacao medica ou foto.
 */
public record PortalAlunoResumo(
        UUID id,
        String nome,
        UUID unitId,
        String parentesco,
        boolean autorizadoBuscar
) {
}
