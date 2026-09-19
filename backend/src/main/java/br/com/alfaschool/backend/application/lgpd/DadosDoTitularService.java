package br.com.alfaschool.backend.application.lgpd;

import br.com.alfaschool.backend.security.filter.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Tudo o que a escola guarda sobre um aluno, num documento so'.
 *
 * <h2>Por que existe</h2>
 * LGPD Art. 18, incisos II e V: o titular — aqui, a crianca, representada
 * por quem responde por ela — tem direito de saber quais dados a escola
 * trata e de recebe-los em formato legivel. Ate' agora nao havia como
 * atender a esse pedido sem alguem abrir o banco a mao, o que significa que
 * na pratica o direito nao existia.
 *
 * <h2>O que NAO sai daqui</h2>
 * O template biometrico. Dele sai apenas o METADADO — que existe, com qual
 * base legal, quando o consentimento foi dado e em quais leitores foi
 * gravado. Entregar o vetor da face num relatorio criaria uma copia do dado
 * mais sensivel do sistema fora do cofre, e nao e' isso que o Art. 18 pede:
 * o titular tem direito de saber que o dado existe e mandar apaga-lo.
 *
 * Tambem nao sai senha, hash de senha nem token de equipamento.
 *
 * <h2>Uma consulta por assunto, e nao um join gigante</h2>
 * O relatorio e' emitido raramente, sob pedido formal, e precisa ser LIDO
 * por uma pessoa. Blocos separados sao mais faceis de conferir e de
 * explicar do que uma tabela larga com trinta colunas.
 */
@Service
public class DadosDoTitularService {

    /** Teto por bloco: o relatorio e' para ser lido, nao para despejar o banco. */
    private static final int LIMITE_POR_BLOCO = 2000;

    private final JdbcTemplate jdbc;

    public DadosDoTitularService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Map<String, Object> relatorioDoAluno(UUID alunoId) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Tenant não identificado");
        }

        Map<String, Object> cadastro = umOuNulo("""
                SELECT a.id, a.nome, a.cpf, a.rg, a.email, a.telefone, a.data_nascimento AS dataNascimento,
                       a.sexo, a.endereco, a.cidade, a.estado, a.cep,
                       a.nome_responsavel AS nomeResponsavel, a.telefone_responsavel AS telefoneResponsavel,
                       a.email_responsavel AS emailResponsavel, a.observacoes_medicas AS observacoesMedicas,
                       a.created_at AS cadastradoEm, a.updated_at AS atualizadoEm
                  FROM alunos a WHERE a.tenant_id = ? AND a.id = ? AND a.deleted = FALSE
                """, tenantId, alunoId);

        if (cadastro == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Aluno não encontrado");
        }

        Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("emitidoEm", Instant.now());
        doc.put("aviso", "Documento emitido para atender ao Art. 18 da LGPD. "
                + "O template biométrico não é incluído: dele consta apenas o registro de que existe, "
                + "a base legal e em quais equipamentos foi gravado.");
        doc.put("cadastro", cadastro);

        doc.put("responsaveis", muitos("""
                SELECT r.nome, r.cpf, r.email, r.telefone, ar.parentesco,
                       ar.responsavel_financeiro AS responsavelFinanceiro,
                       ar.responsavel_academico  AS responsavelAcademico,
                       ar.autorizado_buscar      AS autorizadoBuscar,
                       ar.principal
                  FROM aluno_responsaveis ar
                  JOIN responsaveis r ON r.id = ar.responsavel_id AND r.deleted = FALSE
                 WHERE ar.tenant_id = ? AND ar.aluno_id = ? AND ar.deleted = FALSE
                """, tenantId, alunoId));

        doc.put("matriculas", muitos("""
                SELECT m.numero_matricula AS numeroMatricula, m.data_matricula AS dataMatricula,
                       m.status, t.nome AS turma, c.nome AS curso
                  FROM matriculas m
                  LEFT JOIN turmas t ON t.id = m.turma_id
                  LEFT JOIN cursos c ON c.id = t.curso_id
                 WHERE m.tenant_id = ? AND m.aluno_id = ? AND m.deleted = FALSE
                """, tenantId, alunoId));

        doc.put("quemPodeRetirar", muitos("""
                SELECT pa.nome, pa.parentesco, pa.cpf, au.permanente, au.vigencia_inicio AS vigenciaInicio,
                       au.vigencia_fim AS vigenciaFim, au.dias_semana AS diasSemana,
                       au.hora_inicio AS horaInicio, au.hora_fim AS horaFim, au.status, au.origem
                  FROM acc_autorizacoes_retirada au
                  JOIN acc_pessoas_autorizadas pa ON pa.id = au.pessoa_autorizada_id
                 WHERE au.tenant_id = ? AND au.aluno_id = ? AND au.deleted = FALSE
                """, tenantId, alunoId));

        doc.put("restricoes", muitos("""
                SELECT r.tipo, r.numero_processo AS numeroProcesso, r.orgao_emissor AS orgaoEmissor,
                       r.descricao, r.pessoa_nome AS pessoaNome,
                       r.vigencia_inicio AS vigenciaInicio, r.vigencia_fim AS vigenciaFim, r.ativo
                  FROM acc_restricoes r
                 WHERE r.tenant_id = ? AND r.aluno_id = ? AND r.deleted = FALSE
                """, tenantId, alunoId));

        // Metadado, nunca o template.
        doc.put("biometria", muitos("""
                SELECT f.base_legal AS baseLegal, f.consentimento_obtido AS consentimentoObtido,
                       f.consentimento_em AS consentimentoEm, f.consentimento_versao AS consentimentoVersao,
                       f.consentimento_origem AS consentimentoOrigem,
                       f.consentimento_revogado_em AS consentimentoRevogadoEm,
                       f.consentimento_revogado_motivo AS consentimentoRevogadoMotivo,
                       f.ativo, f.created_at AS cadastradaEm,
                       (SELECT GROUP_CONCAT(d.nome SEPARATOR ', ')
                          FROM acc_face_sync fs JOIN dispositivos d ON d.id = fs.dispositivo_id
                         WHERE fs.face_id = f.id AND fs.status IN ('ENVIADA','ACEITA')) AS gravadaNosEquipamentos
                  FROM acc_faces f
                 WHERE f.tenant_id = ? AND f.titular_id = ? AND f.titular_tipo = 'ALUNO' AND f.deleted = FALSE
                """, tenantId, alunoId));

        doc.put("permanencia", muitos("""
                SELECT p.data, p.primeira_entrada_em AS primeiraEntrada, p.ultima_saida_em AS ultimaSaida,
                       p.minutos_permanencia AS minutosPermanencia, p.minutos_previstos AS minutosPrevistos,
                       p.minutos_excedente AS minutosExcedente, p.status
                  FROM acc_presencas p
                 WHERE p.tenant_id = ? AND p.aluno_id = ? AND p.deleted = FALSE
                 ORDER BY p.data DESC
                """, tenantId, alunoId));

        doc.put("passagensNaPortaria", muitos("""
                SELECT e.data_hora AS dataHora, e.sentido, e.resultado, d.nome AS equipamento
                  FROM acc_eventos e
                  LEFT JOIN dispositivos d ON d.id = e.dispositivo_id
                 WHERE e.tenant_id = ? AND e.titular_id = ? AND e.titular_tipo = 'ALUNO'
                 ORDER BY e.data_hora DESC
                """, tenantId, alunoId));

        doc.put("retiradas", muitos("""
                SELECT r.solicitado_em AS solicitadoEm, r.entregue_em AS entregueEm, r.saida_em AS saidaEm,
                       r.status, r.retirada_manual AS retiradaManual, r.motivo,
                       pa.nome AS retiradoPor
                  FROM acc_retiradas r
                  LEFT JOIN acc_pessoas_autorizadas pa ON pa.id = r.pessoa_autorizada_id
                 WHERE r.tenant_id = ? AND r.aluno_id = ? AND r.deleted = FALSE
                 ORDER BY r.solicitado_em DESC
                """, tenantId, alunoId));

        doc.put("ocorrencias", muitos("""
                SELECT COALESCE(o.ocorrido_em, o.created_at) AS dataHora, o.tipo, o.gravidade,
                       o.descricao, o.status, o.tratativa
                  FROM acc_ocorrencias o
                 WHERE o.tenant_id = ? AND o.aluno_id = ? AND o.deleted = FALSE
                 ORDER BY dataHora DESC
                """, tenantId, alunoId));

        doc.put("avisosEnviados", muitos("""
                SELECT n.canal, n.evento, n.destino, n.status, n.enviado_em AS enviadoEm,
                       n.entregue_em AS entregueEm, n.lida_em AS lidaEm
                  FROM acc_notificacao_envios n
                 WHERE n.tenant_id = ? AND n.aluno_id = ?
                 ORDER BY n.created_at DESC
                """, tenantId, alunoId));

        doc.put("notas", muitos("""
                SELECT d.nome AS disciplina, av.nome AS avaliacao, av.data_avaliacao AS dataAvaliacao,
                       n.nota, n.nota_recuperacao AS notaRecuperacao, n.nota_final AS notaFinal, n.obs
                  FROM notas n
                  JOIN avaliacoes av ON av.id = n.avaliacao_id
                  LEFT JOIN disciplinas d ON d.id = av.disciplina_id
                 WHERE n.tenant_id = ? AND n.aluno_id = ? AND n.deleted = FALSE
                 ORDER BY av.data_avaliacao DESC
                """, tenantId, alunoId));

        doc.put("frequencia", muitos("""
                SELECT f.data, f.presente, f.status, d.nome AS disciplina, f.obs
                  FROM frequencias f
                  LEFT JOIN disciplinas d ON d.id = f.disciplina_id
                 WHERE f.tenant_id = ? AND f.aluno_id = ? AND f.deleted = FALSE
                 ORDER BY f.data DESC
                """, tenantId, alunoId));

        return doc;
    }

    private Map<String, Object> umOuNulo(String sql, UUID tenantId, UUID alunoId) {
        List<Map<String, Object>> linhas = jdbc.queryForList(sql, tenantId.toString(), alunoId.toString());
        return linhas.isEmpty() ? null : linhas.get(0);
    }

    /**
     * Bloco com teto. Um aluno de seis anos de casa tem milhares de
     * passagens de portaria; o relatorio avisa quando corta em vez de
     * entregar um recorte silencioso, que seria pior do que o limite.
     */
    private Object muitos(String sql, UUID tenantId, UUID alunoId) {
        List<Map<String, Object>> linhas = jdbc.queryForList(
                sql + " LIMIT " + (LIMITE_POR_BLOCO + 1), tenantId.toString(), alunoId.toString());
        if (linhas.size() <= LIMITE_POR_BLOCO) {
            return linhas;
        }
        Map<String, Object> cortado = new LinkedHashMap<>();
        cortado.put("aviso", "Mais de " + LIMITE_POR_BLOCO + " registros. "
                + "Este bloco foi truncado; peça o período específico para receber o restante.");
        cortado.put("registros", linhas.subList(0, LIMITE_POR_BLOCO));
        return cortado;
    }
}
