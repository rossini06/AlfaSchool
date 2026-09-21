/**
 * Menu lateral.
 *
 * ORGANIZAÇÃO — por jornada, não por entidade. De cima para baixo: o que
 * se usa todo dia, depois o que se usa toda semana, e por último o que se
 * configura uma vez. Grupos de configuração nascem fechados (`aberto:
 * false`), porque ninguém abre "Estrutura" duas vezes por ano.
 *
 * VISIBILIDADE — cada item declara a PERMISSÃO que exige (`perm`), não o
 * cargo. É o que mantém o menu automaticamente coerente com o backend:
 * mudou a matriz de permissões, a barra lateral acompanha sozinha. Listar
 * cargos aqui obrigaria a reeditar esta lista toda vez que uma escola
 * organizasse o trabalho de um jeito diferente.
 *
  * Item sem `perm` aparece para qualquer pessoa autenticada (o Dashboard).
 * `perm` pode ser uma string ou um array — tendo QUALQUER uma, o item
 * aparece.
 *
 * MÓDULO — item de bloco contratável declara `modulo` (hoje só "ACCESS").
 * Escola que não contratou não vê o item: sem isto o menu mostrava as
 * telas do Access e cada uma respondia 403 no console. A lista de módulos
 * vigentes vem do login, como as permissões.
 *
 * O painel SaaS (administração da Alfa) NÃO está neste menu: tem layout e
 * rota próprios em /saas. O superadmin chega lá pelo menu do perfil.
 */

export const menuGroups = [
  { key: "top", label: null, aberto: true },

  // --------------------------------------------------------- todo dia
  { key: "operacao", label: "OPERAÇÃO", aberto: true },
  { key: "familias", label: "FAMÍLIAS E RETIRADA", aberto: true },

  // ------------------------------------------------------- toda semana
  { key: "pessoas", label: "PESSOAS", aberto: true },
  { key: "academico", label: "ACADÊMICO", aberto: false },
  { key: "diario", label: "DIÁRIO DE CLASSE", aberto: false },
  { key: "financeiro", label: "FINANCEIRO", aberto: false },

  // ---------------------------------------------- configura uma vez
  { key: "jornadas", label: "JORNADAS E CALENDÁRIO", aberto: false },
  { key: "estrutura", label: "ESTRUTURA FÍSICA", aberto: false },
  { key: "equipamentos", label: "EQUIPAMENTOS E PAINÉIS", aberto: false },
  { key: "sistema", label: "SISTEMA", aberto: false },
];

export const menuItems = [
  { label: "Dashboard", path: "/", icon: "LayoutDashboard", group: "top" },

  // ---------------------------------------------------------- OPERAÇÃO
  {
    label: "Coordenação",
    path: "/access/coordenacao",
    icon: "UserCheck",
    group: "operacao",
    perm: "ACESSO_PAINEL_VER",
    modulo: "ACCESS",
  },
  {
    label: "Presentes Agora",
    path: "/access/presentes-agora",
    icon: "Users",
    group: "operacao",
    perm: "ACESSO_PERMANENCIA_VER",
    modulo: "ACCESS",
  },
  {
    label: "Ocorrências",
    path: "/access/ocorrencias",
    icon: "AlertTriangle",
    group: "operacao",
    perm: "ACESSO_OCORRENCIAS_VER",
    modulo: "ACCESS",
  },
  {
    label: "Permanência",
    path: "/access/permanencia",
    icon: "Clock",
    group: "operacao",
    perm: "ACESSO_PERMANENCIA_VER",
    modulo: "ACCESS",
  },
  {
    label: "Relatórios de Acesso",
    path: "/access/relatorios",
    icon: "FileText",
    group: "operacao",
    perm: "ACESSO_RELATORIOS_VER",
    modulo: "ACCESS",
  },

  // ------------------------------------------------ FAMÍLIAS E RETIRADA
  {
    label: "Autorizações",
    path: "/access/autorizacoes",
    icon: "ShieldCheck",
    group: "familias",
    perm: "ACESSO_AUTORIZACOES_VER",
    modulo: "ACCESS",
  },
  {
    label: "Pessoas Autorizadas",
    path: "/access/pessoas-autorizadas",
    icon: "UserPlus",
    group: "familias",
    perm: "ACESSO_AUTORIZACOES_VER",
    modulo: "ACCESS",
  },
  {
    label: "Restrições Judiciais",
    path: "/access/restricoes",
    icon: "Gavel",
    group: "familias",
    perm: "ACESSO_RESTRICOES_VER",
    modulo: "ACCESS",
  },

  // ----------------------------------------------------------- PESSOAS
  { label: "Alunos", path: "/alunos", icon: "GraduationCap", group: "pessoas", perm: "ALUNOS_VER" },
  { label: "Responsáveis", path: "/responsaveis", icon: "Users", group: "pessoas", perm: "RESPONSAVEIS_VER" },
  { label: "Professores", path: "/professores", icon: "UserCog", group: "pessoas", perm: "PROFESSORES_VER" },

  // --------------------------------------------------------- ACADÊMICO
  { label: "Turmas", path: "/turmas", icon: "Users", group: "academico", perm: "TURMAS_VER" },
  { label: "Matrículas", path: "/matriculas", icon: "ClipboardList", group: "academico", perm: "MATRICULAS_VER" },
  { label: "Cursos", path: "/cursos", icon: "BookOpen", group: "academico", perm: "CURSOS_GERIR" },
  { label: "Disciplinas", path: "/disciplinas", icon: "BookMarked", group: "academico", perm: "CURSOS_GERIR" },

  // --------------------------------------------------- DIÁRIO DE CLASSE
  { label: "Frequência", path: "/frequencia", icon: "CalendarCheck", group: "diario", perm: "DIARIO_VER" },
  { label: "Conteúdo", path: "/conteudo-ministrado", icon: "NotebookPen", group: "diario", perm: "DIARIO_VER" },
  { label: "Avaliações", path: "/avaliacoes", icon: "ClipboardCheck", group: "diario", perm: "DIARIO_VER" },
  { label: "Notas", path: "/notas", icon: "FileText", group: "diario", perm: "NOTAS_VER" },
  { label: "Boletim", path: "/boletim", icon: "Award", group: "diario", perm: "NOTAS_VER" },

  // -------------------------------------------------------- FINANCEIRO
  { label: "Financeiro", path: "/financeiro", icon: "DollarSign", group: "financeiro", perm: "FINANCEIRO_VER" },

  // --------------------------------------------- JORNADAS E CALENDÁRIO
  { label: "Jornadas", path: "/access/jornadas", icon: "Timer", group: "jornadas", perm: "ACESSO_JORNADAS_GERIR", modulo: "ACCESS" },
  {
    label: "Jornadas dos Alunos",
    path: "/access/aluno-jornadas",
    icon: "UserClock",
    group: "jornadas",
    perm: "ACESSO_JORNADAS_GERIR",
    modulo: "ACCESS",
  },
  { label: "Calendário", path: "/access/calendario", icon: "Calendar", group: "jornadas", perm: "ACESSO_CALENDARIO_GERIR", modulo: "ACCESS" },

  // ---------------------------------------------------- ESTRUTURA FÍSICA
  { label: "Portarias", path: "/access/portarias", icon: "DoorOpen", group: "estrutura", perm: "ACESSO_ESTRUTURA_GERIR", modulo: "ACCESS" },
  { label: "Zonas", path: "/access/zonas", icon: "Map", group: "estrutura", perm: "ACESSO_ESTRUTURA_GERIR", modulo: "ACCESS" },
  { label: "Salas", path: "/access/salas", icon: "Home", group: "estrutura", perm: "ACESSO_ESTRUTURA_GERIR", modulo: "ACCESS" },
  {
    label: "Turmas nas Salas",
    path: "/access/turma-salas",
    icon: "LayoutGrid",
    group: "estrutura",
    perm: "ACESSO_ESTRUTURA_GERIR",
    modulo: "ACCESS",
  },

  // ----------------------------------------------- EQUIPAMENTOS E PAINÉIS
  {
    label: "Leitores de Acesso",
    path: "/access/equipamentos",
    icon: "ScanFace",
    group: "equipamentos",
    perm: "ACESSO_EQUIPAMENTOS_GERIR",
    modulo: "ACCESS",
  },
  { label: "Painéis e TVs", path: "/access/paineis", icon: "Monitor", group: "equipamentos", perm: "ACESSO_ESTRUTURA_GERIR", modulo: "ACCESS" },
  { label: "Dispositivos", path: "/dispositivos", icon: "Cpu", group: "equipamentos", perm: "ACESSO_EQUIPAMENTOS_GERIR" },

  // ----------------------------------------------------------- SISTEMA
  { label: "Usuários", path: "/usuarios", icon: "UserCog", group: "sistema", perm: "USUARIOS_VER" },
  { label: "Perfis e Permissões", path: "/perfis", icon: "ShieldCheck", group: "sistema", perm: "PERFIS_GERIR" },
  { label: "Auditoria", path: "/auditoria", icon: "History", group: "sistema", perm: "AUDITORIA_VER" },
  { label: "Escolas", path: "/escolas", icon: "School", group: "sistema", perm: "ESCOLA_VER" },
];

/**
 * Decide se a pessoa vê um item.
 *
 * Fail-closed: item que exige permissão e não encontra nenhuma some. É o
 * lado certo de errar — melhor esconder algo que a pessoa poderia usar do
 * que mostrar um botão que vai dar 403 na cara dela.
 */
export function podeVerItem(item, { permissoes = [], roles = [], modulos = [] } = {}) {
  if (item.modulo && !modulos.includes(item.modulo)) {
    return false;
  }
  if (item.role) {
    return roles.includes(item.role);
  }
  if (!item.perm) {
    return true;
  }
  const exigidas = Array.isArray(item.perm) ? item.perm : [item.perm];
  return exigidas.some((p) => permissoes.includes(p));
}
