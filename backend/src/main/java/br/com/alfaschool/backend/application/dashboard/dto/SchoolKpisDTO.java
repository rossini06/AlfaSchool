package br.com.alfaschool.backend.application.dashboard.dto;

public record SchoolKpisDTO(
        long totalAlunos,
        long totalTurmas,
        long totalCursos,
        long matriculasAtivas,
        long matriculasCanceladas
) {}
