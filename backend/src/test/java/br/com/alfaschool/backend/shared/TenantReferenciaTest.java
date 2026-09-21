package br.com.alfaschool.backend.shared;

import br.com.alfaschool.backend.domain.aluno.Aluno;
import br.com.alfaschool.backend.infrastructure.persistence.repository.AlunoRepository;
import br.com.alfaschool.backend.security.filter.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Trava o furo confirmado na verificacao: uma referencia (alunoId) de OUTRA
 * escola era aceita na escrita, e o nome da crianca vazava depois no
 * boletim/dashboard. {@link TenantReferencia} recusa fechado.
 */
@SpringBootTest
@ActiveProfiles("test")
class TenantReferenciaTest {

    @Autowired
    private AlunoRepository alunoRepository;

    private final UUID escolaA = UUID.randomUUID();
    private final UUID escolaB = UUID.randomUUID();

    @AfterEach
    void limpar() {
        TenantContext.clear();
    }

    private UUID criarAluno(UUID tenant, String nome) {
        // Grava sem filtro (setando o tenant na mao), como o seeder faz.
        Aluno aluno = new Aluno();
        aluno.setTenantId(tenant);
        aluno.setNome(nome);
        return TenantContext.semFiltro(() -> alunoRepository.save(aluno)).getId();
    }

    @Test
    void aceitaReferenciaDoProprioTenant() {
        UUID alunoA = criarAluno(escolaA, "Ana da Escola A");
        TenantContext.setTenantId(escolaA);

        Aluno encontrado = TenantReferencia.exigir(alunoRepository, alunoA, "não encontrado");

        assertThat(encontrado.getId()).isEqualTo(alunoA);
        assertThat(encontrado.getNome()).isEqualTo("Ana da Escola A");
    }

    @Test
    void recusaReferenciaDeOutroTenant() {
        UUID alunoA = criarAluno(escolaA, "Ana da Escola A");
        // Quem chama e' a Escola B tentando referenciar o aluno da Escola A.
        TenantContext.setTenantId(escolaB);

        assertThatThrownBy(() -> TenantReferencia.exigir(alunoRepository, alunoA, "Aluno não encontrado nesta escola."))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Aluno não encontrado nesta escola.");
    }

    @Test
    void idNuloPassaPorSerOpcional() {
        TenantContext.setTenantId(escolaA);
        assertThat(TenantReferencia.exigir(alunoRepository, null, "não encontrado")).isNull();
    }
}
