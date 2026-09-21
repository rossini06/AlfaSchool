import { useEffect, useRef, useState } from "react";
import { Link, NavLink, Outlet, useNavigate } from "react-router-dom";
import { api } from "../services/api";
import { Monograma } from "../pages/saas/components/Monograma";
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
  { to: "/saas", label: "Dashboard", icone: "LayoutDashboard", fim: true },
  { to: "/saas/redes", label: "Redes de ensino", icone: "Network" },
  { to: "/saas/planos", label: "Planos", icone: "Tag" },
];

export function SaasLayout() {
  const { user, logout, selecionarRede } = useAuth();
  const { theme, toggleTheme } = useTheme();
  const navigate = useNavigate();
  const [menuAberto, setMenuAberto] = useState(false);
  const menuRef = useRef(null);

  // Seletor de rede: "Painel da escola" precisa dizer QUAL escola.
  const [seletorAberto, setSeletorAberto] = useState(false);
  const [redes, setRedes] = useState(null);
  const [buscaRede, setBuscaRede] = useState("");
  const [entrando, setEntrando] = useState(null);
  const [erroRede, setErroRede] = useState("");
  const seletorRef = useRef(null);

  useEffect(() => {
    const fechar = (e) => {
      if (menuRef.current && !menuRef.current.contains(e.target)) setMenuAberto(false);
      if (seletorRef.current && !seletorRef.current.contains(e.target)) setSeletorAberto(false);
    };
    document.addEventListener("mousedown", fechar);
    return () => document.removeEventListener("mousedown", fechar);
  }, []);

  const abrirSeletor = async () => {
    setSeletorAberto((v) => !v);
    setErroRede("");
    if (redes === null) {
      try {
        const data = await api.get("/tenants?page=0&size=200");
        setRedes((data?.content || []).filter((r) => !r.mestre));
      } catch (e) {
        setErroRede(e.message);
        setRedes([]);
      }
    }
  };

  const entrarNaRede = async (rede) => {
    setEntrando(rede.tenantId);
    setErroRede("");
    try {
      await selecionarRede(rede.tenantId);
      setSeletorAberto(false);
      navigate("/");
    } catch (e) {
      setErroRede(e.message);
    } finally {
      setEntrando(null);
    }
  };

  const termo = buscaRede.trim().toLowerCase();
  const redesVisiveis = (redes || []).filter((r) => !termo || (r.name || "").toLowerCase().includes(termo));

  const iniciais = (user?.nome || "?")
    .trim()
    .split(/\s+/)
    .slice(0, 2)
    .map((p) => p[0])
    .join("")
    .toUpperCase();

  // A ordem dos hooks precisa ser estável; `Link` continua importado para o lockup.
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

        <div className="saas-header-dir">
          {/* Abre o painel de UMA escola, escolhida aqui. O tenant mestre
              não é escola; entrar "no painel" sem dizer qual não faz sentido. */}
          <div className="dropdown" ref={seletorRef}>
            <button type="button" className="btn btn-ghost btn-sm saas-ir-escola" onClick={abrirSeletor} aria-expanded={seletorAberto} title="Entrar no painel de uma escola">
              <Icon name="School" size={15} />
              <span>Entrar numa escola</span>
              <Icon name="ChevronDown" size={14} />
            </button>
            {seletorAberto && (
              <div className="dropdown-menu saas-seletor">
                <div className="saas-seletor-busca">
                  <Icon name="Search" size={14} />
                  <input
                    autoFocus
                    type="search"
                    placeholder="Buscar rede"
                    value={buscaRede}
                    onChange={(e) => setBuscaRede(e.target.value)}
                    aria-label="Buscar rede"
                  />
                </div>
                {erroRede && <p className="saas-seletor-vazio">{erroRede}</p>}
                {redes === null ? (
                  <p className="saas-seletor-vazio">Carregando…</p>
                ) : redesVisiveis.length === 0 ? (
                  <p className="saas-seletor-vazio">{redes.length === 0 ? "Nenhuma rede cadastrada ainda." : "Nada com esse nome."}</p>
                ) : (
                  <ul className="saas-seletor-lista">
                    {redesVisiveis.map((r) => (
                      <li key={r.id}>
                        <button
                          type="button"
                          className="saas-seletor-item"
                          disabled={!r.active || entrando === r.tenantId}
                          onClick={() => entrarNaRede(r)}
                          title={r.active ? `Entrar em ${r.name}` : "Rede suspensa"}
                        >
                          <Monograma nome={r.name} tamanho={28} />
                          <span className="saas-seletor-texto">
                            <strong>{r.name}</strong>
                            <small>{r.active ? `${(r.modulos || []).length} módulos` : "suspensa"}</small>
                          </span>
                          <Icon name={entrando === r.tenantId ? "Loader" : "ArrowRight"} size={14} />
                        </button>
                      </li>
                    ))}
                  </ul>
                )}
              </div>
            )}
          </div>
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
        {/* Mesma moldura do AlfaControl: título da página, abas sublinhadas
            logo abaixo, e o conteúdo da aba. As abas são rotas, então cada
            uma tem URL própria e o F5 volta onde estava. */}
        <div className="page-header">
          <div>
            <h1 className="page-title">Painel SaaS</h1>
            <p className="page-subtitle">Gerencie redes de ensino, módulos e planos da plataforma</p>
          </div>
        </div>
        <nav className="saas-tabs" aria-label="Seções do painel">
          {ABAS.map((a) => (
            <NavLink
              key={a.to}
              to={a.to}
              end={a.fim}
              className={({ isActive }) => `saas-tab ${isActive ? "active" : ""}`}
            >
              <Icon name={a.icone} size={17} />
              {a.label}
            </NavLink>
          ))}
        </nav>
        <div className="saas-tab-content">
          <Outlet />
        </div>
      </main>
    </div>
  );
}
