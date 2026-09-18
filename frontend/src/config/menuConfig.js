export const menuGroups = [
  { key: "top", label: null },
  { key: "escola", label: "ESCOLA" },
  { key: "academico", label: "ACADÊMICO" },
  { key: "diario", label: "DIÁRIO DE CLASSE" },
  { key: "acesso", label: "CONTROLE DE ACESSO" },
  { key: "financeiro", label: "FINANCEIRO" },
  { key: "sistema", label: "SISTEMA" },
  { key: "saas", label: "ADMINISTRAÇÃO SAAS" },
];

export const menuItems = [
  { label: "Dashboard", path: "/", icon: "LayoutDashboard", group: "top" },

  // ESCOLA
  {
    label: "Coordenação",
    path: "/access/coordenacao",
    icon: "UserCheck",
    group: "escola",
  },
  {
    label: "Dispositivos",
    path: "/dispositivos",
    icon: "Cpu",
    group: "escola",
  },

  // ACADÊMICO
  { label: "Cursos", path: "/cursos", icon: "BookOpen", group: "academico" },
  {
    label: "Disciplinas",
    path: "/disciplinas",
    icon: "BookMarked",
    group: "academico",
  },
  { label: "Turmas", path: "/turmas", icon: "Users", group: "academico" },
  {
    label: "Professores",
    path: "/professores",
    icon: "UserCheck",
    group: "academico",
  },
  {
    label: "Alunos",
    path: "/alunos",
    icon: "GraduationCap",
    group: "academico",
  },
  {
    label: "Responsáveis",
    path: "/responsaveis",
    icon: "Users2",
    group: "academico",
  },
  {
    label: "Matrículas",
    path: "/matriculas",
    icon: "ClipboardList",
    group: "academico",
  },

  // DIÁRIO DE CLASSE
  {
    label: "Frequência",
    path: "/frequencia",
    icon: "CalendarCheck",
    group: "diario",
  },
  {
    label: "Conteúdo",
    path: "/conteudo-ministrado",
    icon: "FileEdit",
    group: "diario",
  },
  {
    label: "Avaliações",
    path: "/avaliacoes",
    icon: "ClipboardCheck",
    group: "diario",
  },
  { label: "Notas", path: "/notas", icon: "FileText", group: "diario" },
  {
    label: "Boletim",
    path: "/boletim",
    icon: "FileSpreadsheet",
    group: "diario",
  },

  // CONTROLE DE ACESSO (fatia H — cadastros, permanência e relatórios)
  {
    label: "Presentes Agora",
    path: "/access/presentes-agora",
    icon: "Users",
    group: "acesso",
  },
  {
    label: "Permanência",
    path: "/access/permanencia",
    icon: "Clock",
    group: "acesso",
  },
  {
    label: "Autorizações",
    path: "/access/autorizacoes",
    icon: "UserCheck",
    group: "acesso",
  },
  {
    label: "Pessoas Autorizadas",
    path: "/access/pessoas-autorizadas",
    icon: "Users2",
    group: "acesso",
  },
  {
    label: "Restrições Judiciais",
    path: "/access/restricoes",
    icon: "Shield",
    group: "acesso",
  },
  {
    label: "Ocorrências",
    path: "/access/ocorrencias",
    icon: "AlertCircle",
    group: "acesso",
  },
  {
    label: "Jornadas",
    path: "/access/jornadas",
    icon: "Clock",
    group: "acesso",
  },
  {
    label: "Jornadas dos Alunos",
    path: "/access/aluno-jornadas",
    icon: "GraduationCap",
    group: "acesso",
  },
  {
    label: "Calendário",
    path: "/access/calendario",
    icon: "Calendar",
    group: "acesso",
  },
  {
    label: "Portarias",
    path: "/access/portarias",
    icon: "Home",
    group: "acesso",
  },
  { label: "Zonas", path: "/access/zonas", icon: "Map", group: "acesso" },
  { label: "Salas", path: "/access/salas", icon: "Home", group: "acesso" },
  {
    label: "Turmas nas Salas",
    path: "/access/turma-salas",
    icon: "List",
    group: "acesso",
  },
  {
    label: "Leitores de Acesso",
    path: "/access/equipamentos",
    icon: "Cpu",
    group: "acesso",
  },
  {
    label: "Painéis e TVs",
    path: "/access/paineis",
    icon: "Globe",
    group: "acesso",
  },
  {
    label: "Relatórios de Acesso",
    path: "/access/relatorios",
    icon: "BarChart3",
    group: "acesso",
  },

  // FINANCEIRO
  {
    label: "Financeiro",
    path: "/financeiro",
    icon: "DollarSign",
    group: "financeiro",
  },

  // SISTEMA
  { label: "Usuários", path: "/usuarios", icon: "UserCog", group: "sistema" },
  { label: "Auditoria", path: "/auditoria", icon: "Shield", group: "sistema" },

  // SAAS (SUPER_ADMIN)
  {
    label: "Redes de Ensino",
    path: "/redes",
    icon: "Building2",
    group: "saas",
    roles: ["SUPER_ADMIN"],
  },
  {
    label: "Escolas",
    path: "/escolas",
    icon: "School",
    group: "saas",
    roles: ["SUPER_ADMIN"],
  },
  {
    label: "Painel SaaS",
    path: "/saas",
    icon: "BarChart3",
    group: "saas",
    roles: ["SUPER_ADMIN"],
  },
];
