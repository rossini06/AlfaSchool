import { useState, useRef, useEffect } from "react";
import { useLocation } from "react-router-dom";
import { useAuth } from "../contexts/AuthContext";
import { useTheme } from "../hooks/useTheme";
import { Icon } from "./Icon";

const PAGE_TITLES = {
  "/":             "Dashboard",
  "/redes":        "Redes de Ensino",
  "/escolas":      "Escolas",
  "/saas":         "Painel SaaS",
  "/cursos":       "Cursos",
  "/disciplinas":  "Disciplinas",
  "/turmas":       "Turmas",
  "/professores":  "Professores",
  "/alunos":       "Alunos",
  "/responsaveis": "Responsáveis",
  "/matriculas":   "Matrículas",
  "/dispositivos": "Dispositivos",
  "/usuarios":     "Usuários",
  "/auditoria":    "Auditoria",
};

const getRoleLabel = (roles) => {
  if (!roles?.length) return "";
  if (roles.includes("SUPER_ADMIN")) return "Super Admin";
  if (roles.includes("GESTOR")) return "Gestor";
  return "Usuário";
};

export function Header({ onMenuToggle }) {
  const { user, logout } = useAuth();
  const { theme, toggleTheme } = useTheme();
  const location = useLocation();
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const [searchValue, setSearchValue] = useState("");
  const dropdownRef = useRef(null);

  const pageTitle = PAGE_TITLES[location.pathname] || "AlfaSchool";

  useEffect(() => {
    function handler(e) {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target)) {
        setDropdownOpen(false);
      }
    }
    document.addEventListener("mousedown", handler);
    return () => document.removeEventListener("mousedown", handler);
  }, []);

  const getInitials = (nome) => {
    if (!nome) return "U";
    return nome.split(" ").slice(0, 2).map((n) => n[0]).join("").toUpperCase();
  };

  return (
    <header className="header">
      <div className="header-left">
        <button className="hamburger-btn" onClick={onMenuToggle}>
          <Icon name="Menu" size={22} />
        </button>
        <span className="header-title">{pageTitle}</span>
      </div>

      <div className="header-center">
        <div className="header-search">
          <span className="header-search-icon">
            <Icon name="Search" size={14} />
          </span>
          <input
            type="text"
            placeholder="Pesquisar..."
            value={searchValue}
            onChange={(e) => setSearchValue(e.target.value)}
          />
        </div>
      </div>

      <div className="header-right">
        <button
          className="theme-toggle"
          onClick={toggleTheme}
          title={theme === "dark" ? "Modo claro" : "Modo escuro"}
        >
          <Icon name={theme === "dark" ? "Sun" : "Moon"} size={18} />
        </button>

        <div className="dropdown" ref={dropdownRef}>
          <div
            className="header-user"
            onClick={() => setDropdownOpen((v) => !v)}
            role="button"
            tabIndex={0}
            onKeyDown={(e) => e.key === "Enter" && setDropdownOpen((v) => !v)}
          >
            <div className="header-avatar">{getInitials(user?.nome)}</div>
            <div style={{ display: "flex", flexDirection: "column" }}>
              <span className="header-user-name">{user?.nome || "Usuário"}</span>
              <span className="header-user-role">{getRoleLabel(user?.roles)}</span>
            </div>
            <Icon name="ChevronDown" size={16} style={{ color: "var(--color-text-2)" }} />
          </div>

          {dropdownOpen && (
            <div className="dropdown-menu">
              <button className="dropdown-item" onClick={() => setDropdownOpen(false)}>
                <Icon name="UserCog" size={16} /> Meu Perfil
              </button>
              <div className="dropdown-divider" />
              <button
                className="dropdown-item danger"
                onClick={() => { setDropdownOpen(false); logout(); }}
              >
                <Icon name="LogOut" size={16} /> Sair
              </button>
            </div>
          )}
        </div>
      </div>
    </header>
  );
}
