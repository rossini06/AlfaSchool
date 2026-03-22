package br.com.alfaschool.backend.application.responsavel.dto;

import br.com.alfaschool.backend.domain.responsavel.Responsavel;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ResponsavelResponse(
        UUID id,
        UUID tenantId,
        UUID alunoId,
        String nome,
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
        boolean principal,
        Instant createdAt,
        Instant updatedAt
) {
    public static ResponsavelResponse from(Responsavel r) {
        return new ResponsavelResponse(
                r.getId(),
                r.getTenantId(),
                r.getAlunoId(),
                r.getNome(),
                r.getCpf(),
                r.getRg(),
                r.getDataNascimento(),
                r.getSexo(),
                r.getEstadoCivil(),
                r.getProfissao(),
                r.getEmpresa(),
                r.getTelefone(),
                r.getTelefone2(),
                r.getWhatsapp(),
                r.getEmail(),
                r.getEmailAlternativo(),
                r.getLogradouro(),
                r.getNumeroEndereco(),
                r.getComplemento(),
                r.getBairro(),
                r.getCidade(),
                r.getEstado(),
                r.getCep(),
                r.getFoto(),
                r.getObservacoes(),
                r.getTipo(),
                r.isPrincipal(),
                r.getCreatedAt(),
                r.getUpdatedAt()
        );
    }
}
