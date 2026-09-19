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

    /**
     * Devolve a mesma linha SEM nenhuma foto.
     *
     * Existe porque "nao exibir foto" precisa acontecer no SERVIDOR. O
     * painel com {@code exibeFoto=false} recebia a URL assinada assim
     * mesmo, e apenas a tela deixava de desenhar a imagem — quem tivesse o
     * token da TV (ou abrisse a aba de rede do navegador) baixava a foto da
     * crianca. Politica de exibicao decidida no cliente nao e' politica.
     *
     * A TV e' o ponto mais exposto do sistema: fica pendurada a vista de
     * outras criancas e de quem passa no corredor.
     */
    public RetiradaFilaItem semFotos() {
        return new RetiradaFilaItem(
                id, unitId,
                aluno == null ? null
                        : new AlunoDoCartao(aluno.id(), aluno.nome(), null, null,
                                aluno.turmaNome(), aluno.salaNome()),
                pessoaAutorizada == null ? null
                        : new RetiranteDoCartao(pessoaAutorizada.id(), pessoaAutorizada.nome(),
                                null, null, pessoaAutorizada.parentesco()),
                turmaId, turmaNome, salaId, salaNome, portariaId, portariaNome,
                status, ordemChegada, solicitadoEm, preparandoEm, prontoEm, entregueEm, saidaEm,
                retiradaManual, motivo, observacao, tempoEsperaMinutos);
    }
}
