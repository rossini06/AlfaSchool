package br.com.alfaschool.backend.application.responsavel.dto;

import java.util.UUID;

public record AlunoResponsavelResponse(
        UUID id,
        UUID alunoId,
        UUID responsavelId,
        ResponsavelResponse responsavel,
        String parentesco,
        String parentescoDescricao,
        boolean responsavelFinanceiro,
        boolean responsavelAcademico,
        boolean autorizadoBuscar,
        boolean principal,
        String observacoes
) {}
