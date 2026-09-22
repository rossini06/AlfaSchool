package br.com.alfaschool.backend.application.aluno.dto;

import br.com.alfaschool.backend.application.access.biometria.FotoUrlAssinada;
import br.com.alfaschool.backend.domain.aluno.Aluno;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record AlunoResponse(
    UUID id, UUID tenantId, UUID unitId, String nome, String cpf, String rg,
    String email, String telefone, LocalDate dataNascimento, String sexo,
    String endereco, String cidade, String estado, String cep,
    String nomeResponsavel, String telefoneResponsavel, String emailResponsavel,
    String fotoKey, String fotoUrl, String observacoesMedicas, boolean ativo,
    Instant createdAt, Instant updatedAt
) {
    /** Sem assinador: a foto so' carrega com a URL assinada, entao fotoUrl fica nula. */
    public static AlunoResponse from(Aluno a) {
        return from(a, null);
    }

    public static AlunoResponse from(Aluno a, FotoUrlAssinada assinador) {
        return new AlunoResponse(
            a.getId(), a.getTenantId(), a.getUnitId(), a.getNome(), a.getCpf(), a.getRg(),
            a.getEmail(), a.getTelefone(), a.getDataNascimento(), a.getSexo(),
            a.getEndereco(), a.getCidade(), a.getEstado(), a.getCep(),
            a.getNomeResponsavel(), a.getTelefoneResponsavel(), a.getEmailResponsavel(),
            a.getFotoKey(),
            assinador != null ? assinador.emitir(a.getFotoKey()) : null,
            a.getObservacoesMedicas(), a.isAtivo(), a.getCreatedAt(), a.getUpdatedAt()
        );
    }
}
