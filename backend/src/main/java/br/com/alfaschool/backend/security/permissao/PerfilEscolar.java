package br.com.alfaschool.backend.security.permissao;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Set;

import static br.com.alfaschool.backend.security.permissao.Permissao.*;

/**
 * Perfis que uma escola realmente usa, e o que cada um pode.
 *
 * <p>Sao ATALHOS: o conjunto abaixo e' o ponto de partida ao criar a escola.
 * Depois, cada instituicao ajusta pelo cadastro de perfis, sem tocar em
 * codigo — porque escola nenhuma organiza o trabalho igual a outra.
 *
 * <p>O criterio ao montar cada conjunto foi: o que a pessoa precisa para
 * fazer o trabalho dela, e nada alem. Coordenacao nao mexe em dinheiro,
 * secretaria nao opera a portaria, professor nao cadastra ninguem.
 */
public enum PerfilEscolar {

    /** Dono da operacao da escola. */
    DIRETOR("Diretor", "Enxerga e faz tudo dentro da escola.",
            EnumSet.complementOf(EnumSet.of(PORTAL_ACESSAR))),

    /**
     * Toca a operacao do dia: quem esta na escola, quem esta esperando, quem
     * pode levar. Nao mexe em financeiro nem em usuarios.
     */
    COORDENACAO("Coordenação", "Operação diária: presença, retirada e ocorrências.",
            EnumSet.of(
                    ESCOLA_VER,
                    ALUNOS_VER, RESPONSAVEIS_VER, PROFESSORES_VER,
                    TURMAS_VER, MATRICULAS_VER, DIARIO_VER, NOTAS_VER,
                    ACESSO_PAINEL_VER, ACESSO_RETIRADA_OPERAR, ACESSO_RETIRADA_ENTREGAR,
                    ACESSO_RETIRADA_MANUAL,
                    ACESSO_AUTORIZACOES_VER, ACESSO_AUTORIZACOES_APROVAR,
                    ACESSO_RESTRICOES_VER,
                    ACESSO_PERMANENCIA_VER, ACESSO_PERMANENCIA_AJUSTAR,
                    ACESSO_OCORRENCIAS_VER, ACESSO_OCORRENCIAS_TRATAR,
                    ACESSO_CALENDARIO_GERIR, ACESSO_ESTRUTURA_GERIR,
                    ACESSO_RELATORIOS_VER, NOTIFICACOES_VER)),

    /**
     * Cuida do cadastro e do papel. Aprova quem pode retirar, mas nao fica na
     * fila da portaria.
     */
    SECRETARIA("Secretaria", "Cadastros, matrículas e autorizações.",
            EnumSet.of(
                    ESCOLA_VER,
                    ALUNOS_VER, ALUNOS_GERIR,
                    RESPONSAVEIS_VER, RESPONSAVEIS_GERIR,
                    PROFESSORES_VER, PROFESSORES_GERIR,
                    CURSOS_GERIR, TURMAS_VER, TURMAS_GERIR,
                    MATRICULAS_VER, MATRICULAS_GERIR,
                    DIARIO_VER, NOTAS_VER,
                    ACESSO_AUTORIZACOES_VER, ACESSO_AUTORIZACOES_GERIR,
                    ACESSO_AUTORIZACOES_APROVAR,
                    ACESSO_JORNADAS_GERIR, ACESSO_CALENDARIO_GERIR,
                    ACESSO_BIOMETRIA_GERIR,
                    ACESSO_PERMANENCIA_VER, ACESSO_RELATORIOS_VER,
                    NOTIFICACOES_VER)),

    /**
     * Trabalha na sala. Lanca o que e' dele.
     *
     * NAO recebe ACESSO_PAINEL_VER: essa permissao abre a Central de
     * Coordenacao, que mostra a escola inteira. O professor opera pelo
     * painel da PROPRIA sala, que roda na TV com token de dispositivo e
     * nem passa por login — por isso nao precisa de item de menu.
     */
    PROFESSOR("Professor", "Diário, notas e frequência das turmas dele.",
            EnumSet.of(
                    ALUNOS_VER, TURMAS_VER,
                    DIARIO_VER, DIARIO_LANCAR,
                    NOTAS_VER, NOTAS_LANCAR,
                    ACESSO_RETIRADA_OPERAR)),

    /**
     * Fica na entrada. Ve a fila e registra ocorrencia — e nao cadastra nada.
     * Nao recebe ENTREGAR por padrao: quem confirma a entrega da crianca e'
     * decisao da escola, nao consequencia de estar na portaria.
     */
    PORTARIA("Portaria", "Fila de retirada e ocorrências na entrada.",
            EnumSet.of(
                    ACESSO_PAINEL_VER, ACESSO_RETIRADA_OPERAR,
                    ACESSO_AUTORIZACOES_VER,
                    ACESSO_OCORRENCIAS_VER, ACESSO_OCORRENCIAS_TRATAR)),

    /** Mensalidade, contrato e a hora excedente que vira cobranca. */
    FINANCEIRO("Financeiro", "Planos, contratos, cobranças e excedentes.",
            EnumSet.of(
                    ESCOLA_VER, ALUNOS_VER, RESPONSAVEIS_VER, MATRICULAS_VER,
                    FINANCEIRO_VER, FINANCEIRO_GERIR,
                    ACESSO_PERMANENCIA_VER, ACESSO_FECHAMENTO_GERIR,
                    ACESSO_RELATORIOS_VER)),

    /** Pai, mae ou quem responde pelo aluno. So' o portal. */
    RESPONSAVEL("Responsável", "Portal da família.",
            EnumSet.of(PORTAL_ACESSAR));

    private final String rotulo;
    private final String descricao;
    private final Set<Permissao> permissoes;

    PerfilEscolar(String rotulo, String descricao, Set<Permissao> permissoes) {
        this.rotulo = rotulo;
        this.descricao = descricao;
        this.permissoes = Collections.unmodifiableSet(permissoes);
    }

    public String getRotulo() { return rotulo; }
    public String getDescricao() { return descricao; }
    public Set<Permissao> getPermissoes() { return permissoes; }

    public static PerfilEscolar porNome(String nome) {
        return Arrays.stream(values())
                .filter(p -> p.name().equalsIgnoreCase(nome))
                .findFirst()
                .orElse(null);
    }

    /** Todas as permissoes dos perfis informados, somadas. */
    public static Set<Permissao> unir(Iterable<String> nomesDePerfil) {
        Set<Permissao> total = new LinkedHashSet<>();
        for (String nome : nomesDePerfil) {
            PerfilEscolar p = porNome(nome);
            if (p != null) {
                total.addAll(p.getPermissoes());
            }
        }
        return total;
    }
}
