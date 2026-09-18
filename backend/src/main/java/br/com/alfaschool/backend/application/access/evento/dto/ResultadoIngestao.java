package br.com.alfaschool.backend.application.access.evento.dto;

import br.com.alfaschool.backend.domain.access.evento.AccEvento;

import java.util.UUID;

/**
 * Veredito da ingestao. O chamador precisa saber se o evento foi gravado
 * ou reconhecido como replay: a fila offline do agente reenvia o mesmo
 * lote varias vezes e nao pode contar cada reenvio como uma passagem.
 */
public record ResultadoIngestao(UUID eventoId, boolean replay) {

    public static ResultadoIngestao gravado(AccEvento e) {
        return new ResultadoIngestao(e.getId(), false);
    }

    public static ResultadoIngestao replay(AccEvento e) {
        return new ResultadoIngestao(e.getId(), true);
    }
}
