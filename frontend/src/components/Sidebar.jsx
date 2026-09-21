import { useCallback, useMemo, useState } from "react";
import { Link, NavLink, useLocation } from "react-router-dom";
import { useAuth } from "../contexts/AuthContext";
import { menuGroups, menuItems, podeVerItem } from "../config/menuConfig";
import { Icon } from "./Icon";
import icone from "/alfaschool-icon.svg";
import wordmark from "/alfaschool-logo.png";

const CHAVE_SECOES = "alfaschool_menu_secoes";

function lerSecoesSalvas() {
  try {
    const bruto = localStorage.getItem(CHAVE_SECOES);
    return bruto ? JSON.parse(bruto) : {};
  } catch {
    // localStorage bloqueado (aba anônima, política do navegador): o menu
    // continua funcionando, só não lembra o que estava aberto.
    return {};
  }
}

export function Sidebar({ collapsed, onToggle, mobileOpen, onMobileClose }) {
  const { user } = useAuth();
  const location = useLocation();
  const [secoes, setSecoes] = useState(lerSecoesSalvas);
  const [busca, setBusca] = useState("");

  const permissoes = useMemo(() => user?.permissoes ?? [], [user]);
  const roles = useMemo(() => user?.roles ?? [], [user]);
  const modulos = useMemo(() => user?.modulos ?? [], [user]);

  const alternarSecao = useCallback((chave) => {
    setSecoes((atual) => {
      const proximo = { ...atual, [chave]: !(chave in atual ? atual[chave] : true) };
      try {
        localStorage.setItem(CHAVE_SECOES, JSON.stringify(proximo));
      } catch {
        // sem persistência; não é motivo para quebrar a navegação
      }
      return proximo;
    });
  }, []);

  const termo = busca.trim().toLowerCase();

  const gruposVisiveis = useMemo(() => {
    const permitidos = menuItems.filter((item) => podeVerItem(item, { permissoes, roles, modulos }));
    const filtrados = termo
      ? permitidos.filter((i) => i.label.toLowerCase().includes(termo))
      : permitidos;

    return menuGroups
      .map((grupo) => ({
        ...grupo,
        items: filtrados.filter((i) => i.group === grupo.key),
      }))
      // Grupo sem item algum some sozinho: é o que faz a barra se adaptar
      // ao perfil sem nenhuma regra extra. O porteiro simplesmente não vê
      // "Financeiro" existir.
      .filter((grupo) => grupo.items.length > 0);
  }, [permissoes, roles, modulos, termo]);

  // Recolhida, a barra é só ícones: acordeão ali não faria sentido, então
  // tudo fica aberto. Buscando, idem — esconder resultado seria perverso.
  const secaoAberta = (grupo) =>
    collapsed || termo
      ? true
      : grupo.key in secoes
        ? secoes[grupo.key]
        : grupo.aberto !== false;

  return (
    <aside
      className={`sidebar ${collapsed ? "sidebar-collapsed" : ""} ${mobileOpen ? "sidebar-mobile-open" : ""}`}
    >
      <div className="sidebar-top">
        {/* Mesmo lockup do login (símbolo + wordmark), na proporção de
            brand/README.md: ícone = 1,8 × cap-height do wordmark, gap = 25%
            do ícone. Recolhida, sobra só o símbolo — é ele que identifica a
            marca a 32px. Os dois arquivos são os mesmos do login, então a
            marca não tem duas versões que podem divergir. */}
        <Link to="/" className="sidebar-brand-wrap" title="AlfaSchool">
          <img src={icone} alt="" aria-hidden="true" decoding="sync" className="sidebar-brand-icon" />
          {!collapsed && (
            <img
              src={wordmark}
              alt="AlfaSchool"
              decoding="sync"
              width={2053}
              height={332}
              className="sidebar-brand-logo"
            />
          )}
        </Link>
        <button
          className="sidebar-collapse-toggle"
          onClick={onToggle}
          title={collapsed ? "Expandir" : "Recolher"}
          aria-label={collapsed ? "Expandir menu" : "Recolher menu"}
        >
          <Icon name={collapsed ? "ChevronRight" : "ChevronLeft"} size={14} />
        </button>
      </div>

      {!collapsed && (
        <div className="sidebar-busca">
          <Icon name="Search" size={14} className="sidebar-busca-icone" />
          <input
            type="search"
            value={busca}
            onChange={(e) => setBusca(e.target.value)}
            placeholder="Buscar no menu"
            aria-label="Buscar no menu"
          />
        </div>
      )}

      <nav className="sidebar-nav">
        {gruposVisiveis.map((grupo) => {
          const aberta = secaoAberta(grupo);
          return (
            <div className="sidebar-group" key={grupo.key}>
              {grupo.label && !collapsed && (
                <button
                  type="button"
                  className="sidebar-group-header"
                  onClick={() => alternarSecao(grupo.key)}
                  aria-expanded={aberta}
                >
                  <span className="sidebar-group-label">{grupo.label}</span>
                  <Icon
                    name="ChevronDown"
                    size={14}
                    className={`sidebar-group-chevron ${aberta ? "aberto" : ""}`}
                  />
                </button>
              )}
              {aberta && (
                <div className="sidebar-group-items">
                  {grupo.items.map((item) => {
                    const isActive =
                      item.path === "/"
                        ? location.pathname === "/"
                        : location.pathname.startsWith(item.path);

                    return (
                      <NavLink
                        key={item.path}
                        to={item.path}
                        className={`sidebar-nav-item ${isActive ? "active" : ""}`}
                        onClick={onMobileClose}
                      >
                        <Icon name={item.icon} size={20} className="sidebar-nav-icon" />
                        <span className="sidebar-nav-label">{item.label}</span>
                        {collapsed && <span className="sidebar-tooltip">{item.label}</span>}
                      </NavLink>
                    );
                  })}
                </div>
              )}
            </div>
          );
        })}

        {gruposVisiveis.length === 0 && !collapsed && (
          <p className="sidebar-vazio">
            {termo ? "Nada encontrado." : "Seu perfil não tem telas liberadas."}
          </p>
        )}
      </nav>

      <div className="sidebar-footer">
        {/* Fica no rodapé, sempre visível, e fora dos grupos: o tutorial não
            é uma "tela do sistema", é como se aprende o sistema. */}
        <NavLink to="/ajuda" className="sidebar-ajuda" title="Como usar o sistema">
          <Icon name="Info" size={15} />
          {!collapsed && <span>Como usar o sistema</span>}
        </NavLink>
        {!collapsed && <p className="sidebar-footer-text">AlfaSchool v1.0</p>}
      </div>
    </aside>
  );
}
