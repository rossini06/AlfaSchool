package br.com.alfaschool.backend.application.access.retirada.dto;

import br.com.alfaschool.backend.domain.access.shared.StatusRetirada;

import java.time.Instant;
import java.util.UUID;

/**
 * Uma linha da fila, ja' com tudo o que a tela precisa mostrar.
 *
 * Vem de UMA consulta agregada, com os nomes resolvidos no proprio SQL.
 * Se cada linha fosse buscar o nome do aluno, da turma e do responsavel por
 * conta propria, a fila de 40 criancas viraria 160 selects — e a portaria
 * as 17h e' exatamente o pior momento para isso.
 *
 * O formato aninhado (`aluno`, `pessoaAutorizada`) e' o que
 * `frontend/src/services/accessApi.js` consome.
 *
 * Fotos vao como CHAVE de storage (`fotoKey`), nunca como bytes: o payload
 * da TV e do SSE nao pode carregar imagem de crianca.
 */
public record RetiradaFilaItem(
        UUID id,
        UUID unitId,
        AlunoDoCartao aluno,
        RetiranteDoCartao pessoaAutorizada,
        UUID turmaId,
        String turmaNome,
        UUID salaId,
        String salaNome,
        UUID portariaId,
        String portariaNome,
        StatusRetirada status,
        Integer ordemChegada,
        Instant solicitadoEm,
        Instant preparandoEm,
        Instant prontoEm,
        Instant entregueEm,
        Instant saidaEm,
        boolean retiradaManual,
        String motivo,
        String observacao,
        long tempoEsperaMinutos
) {

    /**
     * A crianca.
     *
     * `fotoKey` e' chave de storage, nao imagem. `fotoUrl` e' o endereco
     * ASSINADO que a tag img usa: tem prazo de minutos e esta preso a
     * chave, entao nao serve para outra pessoa nem depois de vencer.
     * Nulo quando nao ha foto cadastrada — a tela mostra a silhueta.
     */
    public record AlunoDoCartao(UUID id, String nome, String fotoKey, String fotoUrl,
                                String turmaNome, String salaNome) {
    }

    /** Quem veio buscar, com o vinculo que a familia declarou. */
    public record RetiranteDoCartao(UUID id, String nome, String fotoKey, String fotoUrl,
                                    String parentesco) {
    }
}
