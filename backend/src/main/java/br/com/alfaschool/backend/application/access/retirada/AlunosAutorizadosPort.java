package br.com.alfaschool.backend.application.access.retirada;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Quais alunos esta pessoa tem alguma autorizacao para retirar.
 *
 * ATENCAO: isto e' um PRE-FILTRO, nao um veredito. A palavra final e'
 * sempre de AutorizacaoPort.verificar(alunoId, pessoaId, momento), que
 * falha fechada e considera vigencia, dia da semana, faixa de horario e
 * restricao judicial. Aqui so' se reduz o universo de alunos a consultar,
 * para nao percorrer a escola inteira a cada leitura na portaria.
 *
 * Existe porque AutorizacaoPort (contrato compartilhado, de outra fatia)
 * nao expoe um metodo de listagem por pessoa.
 */
public interface AlunosAutorizadosPort {

    List<UUID> alunosCandidatos(UUID tenantId, UUID pessoaAutorizadaId, Instant momento);
}
