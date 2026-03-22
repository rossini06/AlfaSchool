package br.com.alfaschool.backend.application.responsavel.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.util.UUID;

public record ResponsavelRequest(
        UUID alunoId,
        @NotBlank(message = "Nome é obrigatório") String nome,
        String cpf,
        String rg,
        LocalDate dataNascimento,
        String sexo,
        String estadoCivil,
        String profissao,
        String empresa,
        String telefone,
        String telefone2,
        String whatsapp,
        String email,
        String emailAlternativo,
        String logradouro,
        String numeroEndereco,
        String complemento,
        String bairro,
        String cidade,
        String estado,
        String cep,
        String foto,
        String observacoes,
        String tipo,
        Boolean principal
) {}
