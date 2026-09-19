package br.com.alfaschool.backend.application.aluno.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.UUID;

public record AlunoRequest(
    @NotBlank(message = "Informe o nome do aluno") @Size(max = 120, message = "Nome deve ter no máximo 120 caracteres") String nome,
    @Size(max = 14, message = "CPF deve ter no máximo 14 caracteres") String cpf,
    @Size(max = 20, message = "RG deve ter no máximo 20 caracteres") String rg,
    @Size(max = 160, message = "E-mail deve ter no máximo 160 caracteres") String email,
    @Size(max = 20, message = "Telefone deve ter no máximo 20 caracteres") String telefone,
    LocalDate dataNascimento,
    @Size(max = 10, message = "Sexo deve ter no máximo 10 caracteres") String sexo,
    @Size(max = 255, message = "Endereço deve ter no máximo 255 caracteres") String endereco,
    @Size(max = 120, message = "Cidade deve ter no máximo 120 caracteres") String cidade,
    @Size(max = 2, message = "Estado deve ser a sigla com 2 letras") String estado,
    @Size(max = 9, message = "CEP deve ter no máximo 9 caracteres") String cep,
    @Size(max = 120, message = "Nome do responsável deve ter no máximo 120 caracteres") String nomeResponsavel,
    @Size(max = 20, message = "Telefone do responsável deve ter no máximo 20 caracteres") String telefoneResponsavel,
    @Size(max = 160, message = "E-mail do responsável deve ter no máximo 160 caracteres") String emailResponsavel,
    String foto,
    String observacoesMedicas,
    UUID unitId,
    Boolean ativo
) {}
