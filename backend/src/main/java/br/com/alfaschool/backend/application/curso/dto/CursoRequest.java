package br.com.alfaschool.backend.application.curso.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record CursoRequest(
    @NotBlank String nome,
    String codigo,
    String descricao,
    Integer cargaHoraria,
    String modalidade,
    String nivel,
    UUID unitId,
    Boolean ativo
) {}
