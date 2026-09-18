package br.com.alfaschool.backend.security.permissao;

/**
 * Catalogo de permissoes do AlfaSchool.
 *
 * <h2>Por que permissao e nao so' cargo</h2>
 * Cargo diz quem a pessoa e'; permissao diz o que ela pode. Amarrar tela a
 * cargo obriga a mexer em codigo toda vez que uma escola organiza o
 * trabalho de um jeito diferente — e elas organizam. Aqui o cargo e' um
 * ATALHO para um conjunto de permissoes, e a escola pode acrescentar
 * permissao a uma pessoa sem inventar cargo novo.
 *
 * <h2>Nomenclatura</h2>
 * {@code AREA_ACAO}. Viram authority {@code PERM_<nome>} no contexto de
 * seguranca e sao checadas com {@code hasAuthority('PERM_X')}.
 *
 * <h2>VER x GERIR</h2>
 * VER e' leitura. GERIR inclui criar, editar e excluir. Onde a acao tem
 * peso proprio, ela ganha permissao propria — e' o caso de
 * {@link #ACESSO_RETIRADA_ENTREGAR}.
 */
public enum Permissao {

    // ---------------------------------------------------------------- escola
    ESCOLA_VER("Ver dados da escola e unidades"),
    ESCOLA_GERIR("Editar escola, unidades e configuracoes"),
    USUARIOS_VER("Ver usuarios do sistema"),
    USUARIOS_GERIR("Criar, editar e desativar usuarios"),
    PERFIS_GERIR("Definir perfis e permissoes"),
    AUDITORIA_VER("Consultar a trilha de auditoria"),

    // --------------------------------------------------------------- pessoas
    ALUNOS_VER("Ver alunos"),
    ALUNOS_GERIR("Cadastrar e editar alunos"),
    RESPONSAVEIS_VER("Ver responsaveis"),
    RESPONSAVEIS_GERIR("Cadastrar e editar responsaveis"),
    PROFESSORES_VER("Ver professores"),
    PROFESSORES_GERIR("Cadastrar e editar professores"),

    // -------------------------------------------------------------- academico
    CURSOS_GERIR("Cadastrar cursos e disciplinas"),
    TURMAS_VER("Ver turmas"),
    TURMAS_GERIR("Cadastrar e editar turmas"),
    MATRICULAS_VER("Ver matriculas"),
    MATRICULAS_GERIR("Matricular, transferir e cancelar"),
    DIARIO_VER("Ver o diario de classe"),
    DIARIO_LANCAR("Lancar conteudo, frequencia e avaliacoes"),
    NOTAS_VER("Ver notas e boletim"),
    NOTAS_LANCAR("Lancar e alterar notas"),

    // ------------------------------------------------------------- financeiro
    FINANCEIRO_VER("Ver planos, contratos e cobrancas"),
    FINANCEIRO_GERIR("Emitir, baixar e cancelar cobrancas"),

    // ---------------------------------------------------- controle de acesso
    ACESSO_PAINEL_VER("Abrir a Central de Coordenacao e os paineis"),
    ACESSO_RETIRADA_OPERAR("Preparar o aluno e marcar como pronto"),
    /**
     * Separada de proposito. Preparar e' operacao de sala; ENTREGAR e' o ato
     * que registra responsabilidade por uma crianca, e a escola precisa poder
     * conceder isso a quem ela escolher, independente do cargo.
     */
    ACESSO_RETIRADA_ENTREGAR("Confirmar a entrega do aluno"),
    ACESSO_RETIRADA_MANUAL("Registrar retirada sem leitura biometrica"),
    ACESSO_ESTRUTURA_GERIR("Portarias, zonas, salas e paineis"),
    ACESSO_JORNADAS_GERIR("Jornadas contratadas e vinculos"),
    ACESSO_CALENDARIO_GERIR("Calendario letivo"),
    ACESSO_AUTORIZACOES_VER("Ver quem pode retirar cada aluno"),
    ACESSO_AUTORIZACOES_GERIR("Cadastrar pessoas autorizadas e autorizacoes"),
    ACESSO_AUTORIZACOES_APROVAR("Aprovar, suspender e revogar autorizacoes"),
    /** Documento judicial e' acesso restrito, nao segue a permissao de ver. */
    ACESSO_RESTRICOES_VER("Ver restricoes judiciais"),
    ACESSO_RESTRICOES_GERIR("Registrar e encerrar restricoes judiciais"),
    ACESSO_EQUIPAMENTOS_GERIR("Leitores, catracas e agente local"),
    ACESSO_BIOMETRIA_GERIR("Cadastrar e remover faces"),
    ACESSO_PERMANENCIA_VER("Ver permanencia e excedentes"),
    ACESSO_PERMANENCIA_AJUSTAR("Corrigir entrada e saida a mao"),
    ACESSO_FECHAMENTO_GERIR("Fechar e reabrir competencia"),
    ACESSO_OCORRENCIAS_VER("Ver ocorrencias"),
    ACESSO_OCORRENCIAS_TRATAR("Tratar e encerrar ocorrencias"),
    ACESSO_RELATORIOS_VER("Relatorios do controle de acesso"),

    // ----------------------------------------------------------- comunicacao
    NOTIFICACOES_VER("Ver o historico de avisos enviados"),
    NOTIFICACOES_CONFIGURAR("Configurar canais, modelos e destinatarios"),

    // ---------------------------------------------------------------- portal
    PORTAL_ACESSAR("Acessar o portal da familia");

    private final String descricao;

    Permissao(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

    /** Nome da authority no contexto de seguranca. */
    public String authority() {
        return "PERM_" + name();
    }
}
