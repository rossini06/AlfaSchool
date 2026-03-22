package br.com.alfaschool.backend.application.aluno.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.util.UUID;

public record AlunoRequest(
    @NotBlank String nome,
    String cpf,
    String rg,
    String email,
    String telefone,
    LocalDate dataNascimento,
    String sexo,
    String endereco,
    String cidade,
    String estado,
    String cep,
    String nomeResponsavel,
    String telefoneResponsavel,
    String emailResponsavel,
    String foto,
    UUID unitId,
    Boolean ativo
) {}
