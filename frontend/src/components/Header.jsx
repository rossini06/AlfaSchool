import { useState, useRef, useEffect } from "react";
import { useLocation } from "react-router-dom";
import { useAuth } from "../contexts/AuthContext";
import { useTheme } from "../hooks/useTheme";
import { Icon } from "./Icon";
import { menuItems } from "../config/menuConfig";

const PAGE_TITLES = {
  "/":            "Dashboard",
  "/redes":       "Redes de Ensino",
  "/escolas":     "Escolas",
  "/saas":        "Painel SaaS",
  "/cursos":      "Cursos",
  "/turmas":      "Turmas",
  "/alunos":      "Alunos",
  "/matriculas":  "Matrículas",
  "/dispositivos":"Dispositivos",
  "/usuarios":    "Usuários",
  "/auditoria":   "Auditoria",
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
      <button className="header-hamburger btn btn-ghost btn-icon" onClick={onMenuToggle}>
        <Icon name="Menu" size={18} />
      </button>

      <span className="header-title">{pageTitle}</span>

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

      <div className="header-actions">
        <button
          className="header-icon-btn"
          onClick={toggleTheme}
          title={theme === "dark" ? "Modo claro" : "Modo escuro"}
        >
          <Icon name={theme === "dark" ? "Sun" : "Moon"} size={16} />
        </button>

        <div className="dropdown" ref={dropdownRef}>
          <div
            className="header-user"
            onClick={() => setDropdownOpen((v) => !v)}
            role="button"
            tabIndex={0}
            onKeyDown={(e) => e.key === "Enter" && setDropdownOpen((v) => !v)}
          >
            <div className="header-user-avatar">{getInitials(user?.nome)}</div>
            <span className="header-user-name">{user?.nome || "Usuário"}</span>
            <Icon name="ChevronDown" size={12} />
          </div>

          {dropdownOpen && (
            <div className="header-user-dropdown">
              <div style={{ padding: "8px 10px 10px", borderBottom: "1px solid var(--color-border)", marginBottom: 4 }}>
                <div style={{ fontSize: 13, fontWeight: 600, color: "var(--color-text)" }}>
                  {user?.nome}
                </div>
                <div style={{ fontSize: 12, color: "var(--color-text-2)" }}>
                  {user?.email}
                </div>
              </div>
              <button onClick={() => { setDropdownOpen(false); }}>
                <Icon name="UserCog" size={14} />
                Meu Perfil
              </button>
              <div className="dropdown-divider" />
              <button className="logout-btn" onClick={() => { setDropdownOpen(false); logout(); }}>
                <Icon name="LogOut" size={14} />
                Sair
              </button>
            </div>
          )}
        </div>
      </div>
    </header>
  );
}
