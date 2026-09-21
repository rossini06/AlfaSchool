import { useState, useRef, useEffect } from "react";
import { api } from "../services/api";
import { Link, useLocation } from "react-router-dom";
import { useAuth } from "../contexts/AuthContext";
import { useTheme } from "../hooks/useTheme";
import { Icon } from "./Icon";

const PAGE_TITLES = {
  "/":             "Dashboard",
  "/escolas":      "Escolas",
  "/cursos":       "Cursos",
  "/disciplinas":  "Disciplinas",
  "/turmas":       "Turmas",
  "/professores":  "Professores",
  "/alunos":       "Alunos",
  "/responsaveis": "Responsáveis",
  "/matriculas":   "Matrículas",
  "/dispositivos": "Dispositivos",
  "/access/coordenacao": "Central de Coordenação",
  "/perfis": "Perfis e Permissões",
  "/usuarios":     "Usuários",
  "/auditoria":    "Auditoria",
  // Controle de Acesso — fatia H
  "/access/portarias":           "Portarias",
  "/access/zonas":               "Zonas",
  "/access/salas":               "Salas",
  "/access/turma-salas":         "Turmas nas Salas",
  "/access/jornadas":            "Jornadas",
  "/access/aluno-jornadas":      "Jornadas dos Alunos",
  "/access/calendario":          "Calendário Escolar",
  "/access/pessoas-autorizadas": "Pessoas Autorizadas",
  "/access/autorizacoes":        "Autorizações de Retirada",
  "/access/restricoes":          "Restrições Judiciais",
  "/access/equipamentos":        "Leitores de Acesso",
  "/access/paineis":             "Painéis e TVs",
  "/access/permanencia":         "Permanência",
  "/access/presentes-agora":     "Presentes Agora",
  "/access/relatorios":          "Relatórios de Acesso",
  "/access/ocorrencias":         "Ocorrências",
};

const getRoleLabel = (roles) => {
  if (!roles?.length) return "";
  if (roles.includes("SUPER_ADMIN")) return "Super Admin";
  if (roles.includes("GESTOR")) return "Gestor";
  return "Usuário";
};

function SearchGroup({ label, items, route }) {
  if (!items?.length) return null;
  return (
    <div style={{ borderBottom: "1px solid var(--color-border)", padding: "8px 0" }}>
      <div style={{ fontWeight: 600, fontSize: 13, color: "var(--color-text-2)", padding: "0 16px 4px" }}>
        {label}
      </div>
      {items.map((item) => (
        <a
          key={item.id || item.cpf || item.email}
          href={`/${route}/${item.id || ""}`}
          style={{
            display: "block",
            padding: "8px 16px",
            color: "var(--color-text)",
            textDecoration: "none",
            fontSize: 14,
            borderRadius: "var(--radius-sm)",
            transition: "background 0.15s",
          }}
          onMouseDown={(e) => e.preventDefault()}
          onMouseEnter={(e) => (e.currentTarget.style.background = "var(--color-bg-3)")}
          onMouseLeave={(e) => (e.currentTarget.style.background = "transparent")}
        >
          {item.nome || item.nomeCompleto || item.email || "Sem nome"}
        </a>
      ))}
    </div>
  );
}

export function Header({ onMenuToggle }) {
  const { user, logout } = useAuth();
  const { theme, toggleTheme } = useTheme();
  const location = useLocation();
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const [searchValue, setSearchValue] = useState("");
  const [searchResults, setSearchResults] = useState({
    alunos: [],
    responsaveis: [],
    professores: [],
  });
  const [searchLoading, setSearchLoading] = useState(false);
  const [searchDropdown, setSearchDropdown] = useState(false);
  const searchTimeout = useRef(null);
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

  // Busca global
  useEffect(() => {
    if (!searchValue.trim()) {
      setSearchResults({ alunos: [], responsaveis: [], professores: [] });
      setSearchDropdown(false);
      return;
    }
    setSearchLoading(true);
    setSearchDropdown(true);
    if (searchTimeout.current) clearTimeout(searchTimeout.current);
    searchTimeout.current = setTimeout(async () => {
      const q = encodeURIComponent(searchValue);
      const safe = (promise) => promise.catch(() => ({ content: [] }));
      try {
        const [alunos, responsaveis, professores] = await Promise.all([
          safe(api.get(`/alunos?q=${q}&page=0&size=5`)).then((r) => r.content || []),
          safe(api.get(`/responsaveis?q=${q}&page=0&size=5`)).then((r) => r.content || []),
          safe(api.get(`/professores?q=${q}&page=0&size=5`)).then((r) => r.content || []),
        ]);
        setSearchResults({ alunos, responsaveis, professores });
      } catch {
        setSearchResults({ alunos: [], responsaveis: [], professores: [] });
      } finally {
        setSearchLoading(false);
      }
    }, 350);
    // eslint-disable-next-line
  }, [searchValue]);

  // Fecha dropdown ao clicar fora
  useEffect(() => {
    function handleClick(e) {
      if (!e.target.closest(".header-search")) setSearchDropdown(false);
    }
    if (searchDropdown) document.addEventListener("mousedown", handleClick);
    return () => document.removeEventListener("mousedown", handleClick);
  }, [searchDropdown]);

  const hasResults =
    searchResults.alunos.length > 0 ||
    searchResults.responsaveis.length > 0 ||
    searchResults.professores.length > 0;

  return (
    <header className="header">
      <div className="header-left">
        <button className="hamburger-btn" onClick={onMenuToggle} title="Abrir menu" aria-label="Abrir menu">
          <Icon name="Menu" size={22} />
        </button>
        <span className="header-title">{pageTitle}</span>
      </div>

      <div className="header-center">
        <div className="header-search" style={{ position: "relative" }}>
          <span className="header-search-icon">
            <Icon name="Search" size={14} />
          </span>
          <input
            type="text"
            placeholder="Pesquisar alunos, responsáveis, professores..."
            value={searchValue}
            onChange={(e) => setSearchValue(e.target.value)}
            onFocus={() => searchValue && setSearchDropdown(true)}
            style={{ minWidth: 220 }}
          />
          {searchDropdown && (
            <div
              className="header-search-dropdown"
              style={{
                position: "absolute",
                top: 40,
                left: 0,
                right: 0,
                background: "var(--color-bg-2)",
                border: "1px solid var(--color-border)",
                borderRadius: "var(--radius-md)",
                zIndex: 1000,
                boxShadow: "var(--shadow-md)",
                maxHeight: 380,
                overflowY: "auto",
              }}
            >
              {searchLoading && (
                <div style={{ padding: 16, textAlign: "center", color: "var(--color-text-2)" }}>
                  Buscando...
                </div>
              )}
              {!searchLoading && (
                <>
                  <SearchGroup label="Alunos" items={searchResults.alunos} route="alunos" />
                  <SearchGroup label="Responsáveis" items={searchResults.responsaveis} route="responsaveis" />
                  <SearchGroup label="Professores" items={searchResults.professores} route="professores" />
                  {!hasResults && (
                    <div style={{ padding: 16, color: "var(--color-text-2)", textAlign: "center" }}>
                      Nenhum resultado encontrado
                    </div>
                  )}
                </>
              )}
            </div>
          )}
        </div>
      </div>

      <div className="header-right">
        {/* Ajuda CONTEXTUAL: abre o tutorial já aberto na tela em que a
            pessoa está. Um tutorial que obriga a procurar o próprio assunto
            é lido uma vez e abandonado. */}
        <Link
          className="theme-toggle"
          to={`/ajuda#${encodeURIComponent(location.pathname)}`}
          title="Como usar esta tela"
          aria-label="Como usar esta tela"
        >
          <Icon name="Info" size={18} />
        </Link>

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
              {/* O painel da Alfa fica fora da sidebar da escola; este é o
                  caminho de volta para quem administra a plataforma. */}
              {user?.roles?.includes("SUPER_ADMIN") && (
                <Link className="dropdown-item" to="/saas" onClick={() => setDropdownOpen(false)}>
                  <Icon name="Settings" size={16} /> Painel SaaS
                </Link>
              )}
              {/* Antes do Sair: e' onde a pessoa procura quando esta perdida,
                  e o tutorial se recorta sozinho pelo perfil dela. */}
              <Link
                className="dropdown-item"
                to="/ajuda"
                onClick={() => setDropdownOpen(false)}
              >
                <Icon name="Info" size={16} /> Como usar o sistema
              </Link>
              <div className="dropdown-divider" />
              <button
                className="dropdown-item danger"
                onClick={() => {
                  setDropdownOpen(false);
                  logout();
                }}
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
