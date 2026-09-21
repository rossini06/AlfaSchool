package br.com.alfaschool.backend.application.modulo.dto;

import br.com.alfaschool.backend.domain.modulo.Modulo;
import br.com.alfaschool.backend.domain.modulo.TenantModulo;

import java.time.Instant;

/** Um modulo do catalogo e a situacao dele num tenant. */
public record ModuloContratacaoResponse(
        String codigo,
        String nome,
        String descricao,
        boolean contratado,
        Instant ativadoEm,
        Instant expiraEm
) {
    public static ModuloContratacaoResponse from(Modulo modulo, TenantModulo contratacao) {
        boolean vigente = contratacao != null && contratacao.vigente();
        return new ModuloContratacaoResponse(
                modulo.getCodigo(),
                modulo.getNome(),
                modulo.getDescricao(),
                vigente,
                vigente ? contratacao.getAtivadoEm() : null,
                vigente ? contratacao.getExpiraEm() : null);
    }
}
