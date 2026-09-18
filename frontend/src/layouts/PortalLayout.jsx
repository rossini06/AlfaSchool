import { Outlet, NavLink } from "react-router-dom";
import { useAuth } from "../contexts/AuthContext";
import { useTheme } from "../hooks/useTheme";
import { Icon } from "../components/Icon";
import "../styles/accessCadastros.css";

/**
 * Layout do Portal da Família.
 * Sem sidebar administrativa: quem entra aqui é pai, mãe ou responsável,
 * geralmente pelo celular e com pressa.
 */
const LINKS = [
  { to: "/portal", label: "Início", fim: true },
  { to: "/portal/historico", label: "Histórico" },
  { to: "/portal/autorizacoes", label: "Autorizações" },
  { to: "/portal/notificacoes", label: "Avisos" },
];

export function PortalLayout() {
  const { user, logout } = useAuth();
  const { theme, toggleTheme } = useTheme();

  return (
    <div className="ac-portal-shell">
      <header className="ac-portal-topo">
        <div className="ac-portal-topo-inner">
          <span className="ac-portal-marca">
            Alfa<span>School</span> · Portal da Família
          </span>
          <div className="ac-portal-user">
            <span className="truncate">{user?.nome || user?.email}</span>
            <button
              className="btn btn-ghost btn-sm"
              onClick={toggleTheme}
              title={theme === "dark" ? "Tema claro" : "Tema escuro"}
            >
              <Icon name={theme === "dark" ? "Sun" : "Moon"} size={15} />
            </button>
            <button className="btn btn-ghost btn-sm" onClick={logout} title="Sair">
              <Icon name="LogOut" size={15} />
            </button>
          </div>
        </div>
        <nav className="ac-portal-nav">
          {LINKS.map((l) => (
            <NavLink key={l.to} to={l.to} end={l.fim} className={({ isActive }) => (isActive ? "ativo" : "")}>
              {l.label}
            </NavLink>
          ))}
        </nav>
      </header>

      <main className="ac-portal-main">
        <Outlet />
      </main>
    </div>
  );
}
