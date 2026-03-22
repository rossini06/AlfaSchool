package br.com.alfaschool.backend.application.dispositivo.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record DispositivoRequest(
    @NotBlank String nome,
    String tipo,
    String fabricante,
    String modelo,
    String ip,
    Integer porta,
    String serial,
    String apiToken,
    UUID unitId,
    Boolean ativo
) {}
