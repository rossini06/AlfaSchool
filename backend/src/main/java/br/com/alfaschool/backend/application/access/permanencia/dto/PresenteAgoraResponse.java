package br.com.alfaschool.backend.application.access.permanencia.dto;

import br.com.alfaschool.backend.domain.access.permanencia.AccPresenca;

import java.time.Instant;
import java.util.UUID;

/**
 * Quem esta na escola AGORA, do jeito que a tela precisa ler.
 *
 * <h2>Por que nao reusar PresencaResponse</h2>
 * PresencaResponse e' o extrato do dia: carrega minutos previstos,
 * excedente, congelamento e a lista de pares. Nada disso interessa a quem
 * olha o painel para saber quem esta dentro, e faltava o essencial — o
 * NOME do aluno, da turma e da sala. A tela lia alunoNome/turmaNome/
 * salaNome, a resposta so' tinha ids, e as tres colunas ficavam vazias; a
 * busca por nome nunca achava ninguem e os totais por turma agrupavam
 * tudo em "Sem turma".
 */
public record PresenteAgoraResponse(
        UUID presencaId,
        UUID alunoId,
        String alunoNome,
        UUID turmaId,
        String turmaNome,
        UUID salaId,
        String salaNome,
        UUID unitId,
        Instant entrada,
        int minutosPermanencia,
        int minutosPrevistos
) {
    public static PresenteAgoraResponse de(AccPresenca p, String alunoNome,
                                           UUID turmaId, String turmaNome,
                                           UUID salaId, String salaNome) {
        return new PresenteAgoraResponse(
                p.getId(), p.getAlunoId(), alunoNome,
                turmaId, turmaNome, salaId, salaNome,
                p.getUnitId(), p.getPrimeiraEntradaEm(),
                p.getMinutosPermanencia(), p.getMinutosPrevistos());
    }
}
