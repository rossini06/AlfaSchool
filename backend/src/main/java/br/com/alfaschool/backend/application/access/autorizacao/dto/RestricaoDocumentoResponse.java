package br.com.alfaschool.backend.application.access.autorizacao.dto;

import java.util.UUID;

/** Unico lugar por onde a chave do documento restrito sai da API. */
public record RestricaoDocumentoResponse(
        UUID restricaoId,
        String documentoKey
) {
}
