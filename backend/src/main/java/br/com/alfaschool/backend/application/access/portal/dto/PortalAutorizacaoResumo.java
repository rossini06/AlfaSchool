package br.com.alfaschool.backend.application.access.portal.dto;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Quem pode retirar o aluno.
 *
 * <p>Sem documento completo e sem foto: a familia precisa reconhecer a pessoa
 * da lista, nao ter uma copia do RG dela.
 */
public record PortalAutorizacaoResumo(
        UUID id,
        String nome,
        String parentesco,
        String documentoMascarado,
        String status,
        String origem,
        LocalDate validoAte
) {
}
