package br.com.alfaschool.backend.application.access.estrutura.dto;

import java.time.LocalDateTime;

/** Resposta de "onde a turma esta agora": a sala e o vinculo que a justificou. */
public record SalaVigenteResponse(
        LocalDateTime momento,
        SalaResponse sala,
        TurmaSalaResponse vinculo
) {}
