package br.com.alfaschool.backend.application.aluno.dto;

import br.com.alfaschool.backend.domain.aluno.Aluno;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AlunoResponse(
    UUID id, UUID tenantId, UUID unitId, String nome, String cpf, String rg,
    String email, String telefone, LocalDate dataNascimento, String sexo,
    String endereco, String cidade, String estado, String cep,
    String nomeResponsavel, String telefoneResponsavel, String emailResponsavel,
    String foto, String observacoesMedicas, boolean ativo, Instant createdAt, Instant updatedAt
) {
    public static AlunoResponse from(Aluno a) {
        return new AlunoResponse(
            a.getId(), a.getTenantId(), a.getUnitId(), a.getNome(), a.getCpf(), a.getRg(),
            a.getEmail(), a.getTelefone(), a.getDataNascimento(), a.getSexo(),
            a.getEndereco(), a.getCidade(), a.getEstado(), a.getCep(),
            a.getNomeResponsavel(), a.getTelefoneResponsavel(), a.getEmailResponsavel(),
            a.getFoto(), a.getObservacoesMedicas(), a.isAtivo(), a.getCreatedAt(), a.getUpdatedAt()
        );
    }
}
