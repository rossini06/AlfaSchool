package br.com.alfaschool.backend.application.responsavel.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record AlunoResponsavelRequest(
        @NotNull UUID responsavelId,
        String parentesco,
        String parentescoDescricao,
        Boolean responsavelFinanceiro,
        Boolean responsavelAcademico,
        Boolean autorizadoBuscar,
        Boolean principal,
        String observacoes
) {}
