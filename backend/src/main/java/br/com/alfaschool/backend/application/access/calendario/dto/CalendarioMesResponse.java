package br.com.alfaschool.backend.application.access.calendario.dto;

import java.util.List;
import java.util.UUID;

public record CalendarioMesResponse(
        UUID calendarioId,
        int ano,
        int mes,
        long totalDiasLetivos,
        List<DiaDoMesResponse> dias
) {}
