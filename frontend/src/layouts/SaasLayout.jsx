import { useEffect, useRef, useState } from "react";
import { Link, NavLink, Outlet } from "react-router-dom";
import { useAuth } from "../contexts/AuthContext";
import { useTheme } from "../hooks/useTheme";
import { Icon } from "../components/Icon";
import icone from "/alfaschool-icon.svg";
import wordmark from "/alfaschool-logo.png";

/**
 * Painel SaaS: a administração da Alfa, não da escola.
 *
 * Layout próprio, sem a sidebar do cliente. Misturar os dois no mesmo
 * menu fazia "Redes de Ensino" aparecer ao lado de "Alunos", como se a
 * plataforma fosse uma tela da escola — e é exatamente o que o AlfaControl
 * e o AlfaJornada evitam com um painel separado. O badge no topo diz onde
 * a pessoa está antes de ela ler qualquer título.
 */
const ABAS = [
  { to: "/saas", label: "Visão geral", icone: "LayoutDashboard", fim: true },
  { to: "/saas/redes", label: "Redes de ensino", icone: "Network" },
  { to: "/saas/planos", label: "Planos", icone: "Tag" },
];

export function SaasLayout() {
  const { user, logout } = useAuth();
  const { theme, toggleTheme } = useTheme();
  const [menuAberto, setMenuAberto] = useState(false);
  const menuRef = useRef(null);

  useEffect(() => {
    const fechar = (e) => {
      if (menuRef.current && !menuRef.current.contains(e.target)) setMenuAberto(false);
    };
    document.addEventListener("mousedown", fechar);
    return () => document.removeEventListener("mousedown", fechar);
  }, []);

  const iniciais = (user?.nome || "?")
    .trim()
    .split(/\s+/)
    .slice(0, 2)
    .map((p) => p[0])
    .join("")
    .toUpperCase();

  return (
    <div className="saas-shell">
      <header className="saas-header">
        <div className="saas-header-esq">
          <Link to="/saas" className="saas-brand" title="AlfaSchool">
            <img src={icone} alt="" aria-hidden="true" decoding="sync" className="saas-brand-icon" />
            <img
              src={wordmark}
              alt="AlfaSchool"
              decoding="sync"
              width={2053}
              height={332}
              className="saas-brand-logo"
            />
          </Link>
          <span className="saas-badge">PAINEL SAAS</span>
        </div>

        <nav className="saas-nav" aria-label="Seções do painel">
          {ABAS.map((a) => (
            <NavLink
              key={a.to}
              to={a.to}
              end={a.fim}
              className={({ isActive }) => `saas-nav-item ${isActive ? "ativo" : ""}`}
            >
              <Icon name={a.icone} size={16} />
              <span>{a.label}</span>
            </NavLink>
          ))}
        </nav>

        <div className="saas-header-dir">
          {/* O superadmin também usa o painel da escola (o tenant mestre tem
              todos os módulos) para demonstrar e dar suporte. */}
          <Link to="/" className="btn btn-ghost btn-sm saas-ir-escola" title="Abrir o painel da escola">
            <Icon name="School" size={15} />
            <span>Painel da escola</span>
          </Link>
          <button
            type="button"
            className="theme-toggle"
            onClick={toggleTheme}
            title={theme === "dark" ? "Modo claro" : "Modo escuro"}
          >
            <Icon name={theme === "dark" ? "Sun" : "Moon"} size={18} />
          </button>

          <div className="dropdown" ref={menuRef}>
            <button
              type="button"
              className="header-user saas-user"
              onClick={() => setMenuAberto((v) => !v)}
              aria-expanded={menuAberto}
            >
              <span className="saas-user-avatar">{iniciais}</span>
              <span className="header-user-name">{user?.nome || "Super Admin"}</span>
              <Icon name="ChevronDown" size={16} style={{ color: "var(--color-text-2)" }} />
            </button>
            {menuAberto && (
              <div className="dropdown-menu">
                <div className="saas-user-cabecalho">
                  <strong>{user?.nome}</strong>
                  <span>{user?.email}</span>
                </div>
                <div className="dropdown-divider" />
                <button className="dropdown-item danger" onClick={logout}>
                  <Icon name="LogOut" size={16} /> Sair
                </button>
              </div>
            )}
          </div>
        </div>
      </header>

      <main className="saas-conteudo">
        <Outlet />
      </main>
    </div>
  );
}
