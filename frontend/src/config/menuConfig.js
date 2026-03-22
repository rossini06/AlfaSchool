export const menuGroups = [
  { key: "top",        label: null },
  { key: "escola",     label: "ESCOLA" },
  { key: "academico",  label: "ACADÊMICO" },
  { key: "diario",     label: "DIÁRIO DE CLASSE" },
  { key: "financeiro", label: "FINANCEIRO" },
  { key: "sistema",    label: "SISTEMA" },
  { key: "saas",       label: "ADMINISTRAÇÃO SAAS" },
];

export const menuItems = [
  { label: "Dashboard",        path: "/",              icon: "LayoutDashboard", group: "top" },

  // ESCOLA
  { label: "Dispositivos",     path: "/dispositivos",  icon: "Cpu",             group: "escola" },

  // ACADÊMICO
  { label: "Cursos",           path: "/cursos",        icon: "BookOpen",        group: "academico" },
  { label: "Disciplinas",      path: "/disciplinas",   icon: "BookMarked",      group: "academico" },
  { label: "Turmas",           path: "/turmas",        icon: "Users",           group: "academico" },
  { label: "Professores",      path: "/professores",   icon: "UserCheck",       group: "academico" },
  { label: "Alunos",           path: "/alunos",        icon: "GraduationCap",   group: "academico" },
  { label: "Matrículas",       path: "/matriculas",    icon: "ClipboardList",   group: "academico" },

  // DIÁRIO DE CLASSE
  { label: "Frequência",       path: "/frequencia",    icon: "CalendarCheck",   group: "diario" },
  { label: "Notas",            path: "/notas",         icon: "FileText",        group: "diario" },

  // FINANCEIRO
  { label: "Financeiro",       path: "/financeiro",    icon: "DollarSign",      group: "financeiro" },

  // SISTEMA
  { label: "Usuários",         path: "/usuarios",      icon: "UserCog",         group: "sistema" },
  { label: "Auditoria",        path: "/auditoria",     icon: "Shield",          group: "sistema" },

  // SAAS (SUPER_ADMIN)
  { label: "Redes de Ensino",  path: "/redes",         icon: "Building2",       group: "saas", roles: ["SUPER_ADMIN"] },
  { label: "Escolas",          path: "/escolas",       icon: "School",          group: "saas", roles: ["SUPER_ADMIN"] },
  { label: "Painel SaaS",      path: "/saas",          icon: "BarChart3",       group: "saas", roles: ["SUPER_ADMIN"] },
];
