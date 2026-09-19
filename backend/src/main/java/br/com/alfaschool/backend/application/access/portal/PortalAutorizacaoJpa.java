package br.com.alfaschool.backend.application.access.portal;

import br.com.alfaschool.backend.application.access.portal.dto.PortalAutorizacaoResumo;
import br.com.alfaschool.backend.application.access.portal.dto.PortalSolicitacaoAutorizacaoRequest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Autorizacoes de retirada vistas pelo portal, sobre {@code acc_pessoas_autorizadas}
 * e {@code acc_autorizacoes_retirada} (V35).
 *
 * <p>Consultas NATIVAS: essas tabelas sao da fatia de autorizacoes e nao ganham
 * entidade JPA aqui.
 *
 * <h2>A regra mais importante desta classe</h2>
 * {@link #solicitar} NAO LIBERA NADA. Grava com {@code origem = 'PORTAL'} e
 * {@code status = 'PENDENTE'}, e a portaria ignora autorizacao que nao esteja
 * ATIVA. Se o portal pudesse liberar sozinho, uma conta de responsavel
 * comprometida bastaria para alguem retirar uma crianca.
 */
@Component
public class PortalAutorizacaoJpa implements PortalAutorizacaoPort {

    private static final Logger log = LoggerFactory.getLogger(PortalAutorizacaoJpa.class);

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public List<PortalAutorizacaoResumo> listar(UUID tenantId, UUID alunoId) {
        List<Tuple> linhas = entityManager.createNativeQuery("""
                        SELECT CAST(aut.id AS CHAR(36)) AS id,
                               pa.nome                  AS nome,
                               aut.motivo               AS parentesco,
                               pa.cpf                   AS cpf,
                               aut.status               AS status,
                               aut.origem               AS origem,
                               aut.vigencia_fim         AS vigencia_fim
                        FROM acc_autorizacoes_retirada aut
                        JOIN acc_pessoas_autorizadas pa ON pa.id = aut.pessoa_autorizada_id
                        WHERE aut.tenant_id = :tenantId
                          AND aut.aluno_id = :alunoId
                          AND aut.deleted = FALSE
                          AND pa.deleted = FALSE
                        ORDER BY aut.status, pa.nome
                        """, Tuple.class)
                .setParameter("tenantId", tenantId.toString())
                .setParameter("alunoId", alunoId.toString())
                .getResultList();

        List<PortalAutorizacaoResumo> saida = new ArrayList<>();
        for (Tuple t : linhas) {
            saida.add(new PortalAutorizacaoResumo(
                    texto(t, "id") == null ? null : UUID.fromString(texto(t, "id")),
                    texto(t, "nome"),
                    texto(t, "parentesco"),
                    mascararDocumento(texto(t, "cpf")),
                    texto(t, "status"),
                    texto(t, "origem"),
                    data(t, "vigencia_fim")));
        }
        return saida;
    }

    @Override
    @Transactional
    public UUID solicitar(UUID tenantId, UUID alunoId, UUID solicitanteResponsavelId,
                          PortalSolicitacaoAutorizacaoRequest request) {
        UUID pessoaId = UUID.randomUUID();
        UUID autorizacaoId = UUID.randomUUID();
        Timestamp agora = Timestamp.from(Instant.now());

        // A pessoa nasce sem NENHUMA permissao efetiva: nao retira, nao acessa
        // o portal, nao recebe aviso. Quem concede e' a escola, na aprovacao.
        entityManager.createNativeQuery("""
                        INSERT INTO acc_pessoas_autorizadas
                            (id, tenant_id, responsavel_id, nome, cpf, telefone,
                             pode_retirar, pode_acessar_portal, recebe_notificacao,
                             ativo, created_at, updated_at, created_by, deleted)
                        VALUES
                            (:id, :tenantId, :responsavelId, :nome, :cpf, :telefone,
                             FALSE, FALSE, FALSE,
                             TRUE, :agora, :agora, :responsavelId, FALSE)
                        """)
                .setParameter("id", pessoaId.toString())
                .setParameter("tenantId", tenantId.toString())
                .setParameter("responsavelId", solicitanteResponsavelId == null
                        ? null : solicitanteResponsavelId.toString())
                .setParameter("nome", request.nome())
                .setParameter("cpf", request.documento())
                .setParameter("telefone", request.telefone())
                .setParameter("agora", agora)
                .executeUpdate();

        // Regra 4: autorizacao temporaria EXIGE data de fim, para nunca virar
        // permanente por esquecimento. Sem data, o pedido e' de permanente — e
        // permanente tambem depende de aprovacao da escola.
        boolean permanente = request.validoAte() == null;

        entityManager.createNativeQuery("""
                        INSERT INTO acc_autorizacoes_retirada
                            (id, tenant_id, aluno_id, pessoa_autorizada_id, permanente,
                             vigencia_inicio, vigencia_fim, dias_semana, hora_inicio, hora_fim,
                             status, origem, motivo, observacao,
                             created_at, updated_at, created_by, deleted)
                        VALUES
                            (:id, :tenantId, :alunoId, :pessoaId, :permanente,
                             :vigenciaInicio, :vigenciaFim, :diasSemana, :horaInicio, :horaFim,
                             'PENDENTE', 'PORTAL', :motivo, :observacao,
                             :agora, :agora, :responsavelId, FALSE)
                        """)
                .setParameter("id", autorizacaoId.toString())
                .setParameter("tenantId", tenantId.toString())
                .setParameter("alunoId", alunoId.toString())
                .setParameter("pessoaId", pessoaId.toString())
                .setParameter("permanente", permanente)
                .setParameter("vigenciaInicio", permanente
                        ? null
                        : (request.validoDe() != null ? request.validoDe() : LocalDate.now()).toString())
                .setParameter("vigenciaFim", permanente ? null : request.validoAte().toString())
                .setParameter("diasSemana", request.diasSemana())
                .setParameter("horaInicio", request.horaInicio() == null ? null : request.horaInicio().toString())
                .setParameter("horaFim", request.horaFim() == null ? null : request.horaFim().toString())
                .setParameter("motivo", request.parentesco())
                .setParameter("observacao", request.justificativa())
                .setParameter("agora", agora)
                .setParameter("responsavelId", solicitanteResponsavelId == null
                        ? null : solicitanteResponsavelId.toString())
                .executeUpdate();

        log.info("Solicitacao de autorizacao pelo portal: tenant={} aluno={} pessoa={} status=PENDENTE",
                tenantId, alunoId, pessoaId);
        return autorizacaoId;
    }

    /**
     * A familia so' precisa reconhecer a pessoa da lista, nao ter uma copia do
     * documento dela. Mostramos os ultimos digitos.
     */
    private String mascararDocumento(String documento) {
        if (documento == null || documento.isBlank()) {
            return null;
        }
        String so = documento.replaceAll("\\D", "");
        if (so.length() < 4) {
            return "***";
        }
        return "***." + so.substring(so.length() - 2);
    }

    private String texto(Tuple t, String coluna) {
        Object v = t.get(coluna);
        return v == null ? null : v.toString();
    }

    private LocalDate data(Tuple t, String coluna) {
        Object v = t.get(coluna);
        if (v == null) {
            return null;
        }
        if (v instanceof java.sql.Date d) {
            return d.toLocalDate();
        }
        if (v instanceof LocalDate ld) {
            return ld;
        }
        return LocalDate.parse(v.toString());
    }
}
