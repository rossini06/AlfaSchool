package br.com.alfaschool.backend.application.access.shared;

import java.time.Instant;
import java.util.UUID;

/**
 * Consulta de quem pode retirar um aluno, sem acoplar a fila de retirada
 * ao cadastro de autorizacoes.
 *
 * Contrato de seguranca: FALHA FECHADA. Qualquer duvida (sem autorizacao,
 * fora de vigencia, fora do dia, restricao judicial, erro interno) devolve
 * negado com motivo. Nunca devolver permitido por omissao.
 */
public interface AutorizacaoPort {

    /**
     * @param alunoId            aluno a ser retirado
     * @param pessoaAutorizadaId quem se apresentou
     * @param momento            instante da tentativa (respeita dia e faixa de horario)
     */
    Veredito verificar(UUID alunoId, UUID pessoaAutorizadaId, Instant momento);

    /**
     * @param autorizacaoId autorizacao aplicada, quando permitido; nulo quando negado
     */
    record Veredito(boolean permitido, String motivo, UUID autorizacaoId) {

        public static Veredito permitir(UUID autorizacaoId) {
            return new Veredito(true, null, autorizacaoId);
        }

        public static Veredito negar(String motivo) {
            return new Veredito(false, motivo, null);
        }
    }
}
