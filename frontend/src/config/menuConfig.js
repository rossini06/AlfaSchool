export const menuGroups = [
  { key: "top",      label: null },
  { key: "escola",   label: "ESCOLA" },
  { key: "academico", label: "ACADÊMICO" },
  { key: "sistema",  label: "SISTEMA" },
  { key: "saas",     label: "ADMINISTRAÇÃO SAAS" },
];

export const menuItems = [
  { label: "Dashboard",       path: "/",             icon: "LayoutDashboard", group: "top" },
  { label: "Redes de Ensino", path: "/redes",        icon: "Building2",       group: "saas",     roles: ["SUPER_ADMIN"] },
  { label: "Escolas",         path: "/escolas",      icon: "School",          group: "saas",     roles: ["SUPER_ADMIN"] },
  { label: "Painel SaaS",     path: "/saas",         icon: "BarChart3",       group: "saas",     roles: ["SUPER_ADMIN"] },
  { label: "Cursos",          path: "/cursos",       icon: "BookOpen",        group: "academico" },
  { label: "Turmas",          path: "/turmas",       icon: "Users",           group: "academico" },
  { label: "Alunos",          path: "/alunos",       icon: "GraduationCap",   group: "academico" },
  { label: "Matrículas",      path: "/matriculas",   icon: "ClipboardList",   group: "academico" },
  { label: "Dispositivos",    path: "/dispositivos", icon: "Cpu",             group: "escola" },
  { label: "Usuários",        path: "/usuarios",     icon: "UserCog",         group: "sistema" },
  { label: "Auditoria",       path: "/auditoria",    icon: "Shield",          group: "sistema" },
];
