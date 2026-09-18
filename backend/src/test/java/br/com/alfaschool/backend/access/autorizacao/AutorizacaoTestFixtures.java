package br.com.alfaschool.backend.access.autorizacao;

import br.com.alfaschool.backend.domain.access.autorizacao.AutorizacaoRetirada;
import br.com.alfaschool.backend.domain.access.autorizacao.PessoaAutorizada;
import br.com.alfaschool.backend.domain.access.autorizacao.Restricao;
import br.com.alfaschool.backend.domain.access.shared.OrigemAutorizacao;
import br.com.alfaschool.backend.domain.access.shared.StatusAutorizacao;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

/**
 * Construtores de entidade para os testes. O id vem de BaseEntity e nao tem
 * setter (quem gera e' o JPA), entao usamos reflexao só nos testes.
 */
final class AutorizacaoTestFixtures {

    private AutorizacaoTestFixtures() {
    }

    static PessoaAutorizada pessoa(UUID id, UUID tenantId, String cpf, boolean podeRetirar, boolean ativo) {
        PessoaAutorizada pessoa = new PessoaAutorizada();
        ReflectionTestUtils.setField(pessoa, "id", id);
        pessoa.setTenantId(tenantId);
        pessoa.setNome("Maria da Silva");
        pessoa.setCpf(cpf);
        pessoa.setPodeRetirar(podeRetirar);
        pessoa.setAtivo(ativo);
        return pessoa;
    }

    static AutorizacaoRetirada autorizacao(UUID id, UUID tenantId, UUID alunoId, UUID pessoaId,
                                           StatusAutorizacao status) {
        AutorizacaoRetirada autorizacao = new AutorizacaoRetirada();
        ReflectionTestUtils.setField(autorizacao, "id", id);
        autorizacao.setTenantId(tenantId);
        autorizacao.setAlunoId(alunoId);
        autorizacao.setPessoaAutorizadaId(pessoaId);
        autorizacao.setStatus(status);
        autorizacao.setOrigem(OrigemAutorizacao.ESCOLA);
        autorizacao.setPermanente(true);
        return autorizacao;
    }

    static Restricao restricao(UUID tenantId, UUID alunoId, UUID pessoaAutorizadaId, String pessoaCpf) {
        Restricao restricao = new Restricao();
        ReflectionTestUtils.setField(restricao, "id", UUID.randomUUID());
        restricao.setTenantId(tenantId);
        restricao.setAlunoId(alunoId);
        restricao.setPessoaAutorizadaId(pessoaAutorizadaId);
        restricao.setPessoaCpf(pessoaCpf);
        restricao.setDescricao("Medida protetiva");
        restricao.setAtivo(true);
        return restricao;
    }
}
