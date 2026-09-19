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
     * Por que a retirada foi negada.
     *
     * Existe como CODIGO, e nao so' como texto, porque quem decide o que
     * fazer com a negativa precisa distinguir os casos — e comparar
     * mensagem por string e' frágil demais para uma decisao que envolve
     * entregar uma crianca. Em especial:
     *
     * RESTRICAO_JUDICIAL **nao pode ser contornada por nenhum caminho**,
     * nem pela retirada manual. Os demais motivos podem ser assumidos pela
     * coordenacao, com justificativa registrada.
     */
    enum MotivoNegativa {
        RESTRICAO_JUDICIAL,
        PESSOA_NAO_ENCONTRADA,
        PESSOA_INATIVA,
        SEM_PERMISSAO_RETIRADA,
        SEM_AUTORIZACAO,
        AUTORIZACAO_NAO_ATIVA,
        FORA_DA_JANELA,
        DADOS_INSUFICIENTES,
        ERRO_INTERNO
    }

    /**
     * @param autorizacaoId autorizacao aplicada, quando permitido; nulo quando negado
     * @param codigo        por que negou; nulo quando permitido
     */
    record Veredito(boolean permitido, String motivo, UUID autorizacaoId, MotivoNegativa codigo) {

        public static Veredito permitir(UUID autorizacaoId) {
            return new Veredito(true, null, autorizacaoId, null);
        }

        public static Veredito negar(MotivoNegativa codigo, String motivo) {
            return new Veredito(false, motivo, null, codigo);
        }

        /** Restricao judicial e' a unica negativa absoluta do sistema. */
        public boolean restricaoJudicial() {
            return codigo == MotivoNegativa.RESTRICAO_JUDICIAL;
        }
    }
}
