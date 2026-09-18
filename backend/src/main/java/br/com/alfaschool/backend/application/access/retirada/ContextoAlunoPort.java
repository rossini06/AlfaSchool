package br.com.alfaschool.backend.application.access.retirada;

import java.time.Instant;
import java.util.UUID;

/**
 * Onde o aluno esta' agora: unidade, turma e sala.
 *
 * A retirada guarda esse recorte no momento em que e' aberta, e nao por
 * referencia, porque e' ele que decide em qual TV o cartao aparece. Se a
 * turma trocar de sala depois, o cartao que ja esta' na tela da sala certa
 * nao pode pular para outra.
 */
public interface ContextoAlunoPort {

    ContextoAluno contextoDe(UUID tenantId, UUID alunoId, Instant momento);

    record ContextoAluno(UUID unitId, UUID turmaId, UUID salaId) {

        public static ContextoAluno vazio() {
            return new ContextoAluno(null, null, null);
        }
    }
}
