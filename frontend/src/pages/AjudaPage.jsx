import { useEffect, useMemo, useState } from "react";
import { Link, useLocation } from "react-router-dom";
import { Icon } from "../components/Icon";
import { useAuth } from "../contexts/AuthContext";
import { api } from "../services/api";
import { menuGroups, menuItems, podeVerItem } from "../config/menuConfig";
import { ajudaPorTela, regrasDoSistema } from "../config/ajudaConfig";

/**
 * Tutorial do sistema.
 *
 * <h2>Por que ele é montado e não escrito</h2>
 * Um tutorial em texto fixo envelhece na primeira tela nova e passa a
 * mentir — e o pior tipo de documentação é a que o leitor ainda acredita.
 * Aqui a lista de telas vem de `menuConfig.js` e o filtro vem das
 * PERMISSÕES REAIS de quem está logado, as mesmas que a barra lateral usa.
 *
 * Consequências, todas de propósito:
 *  - a portaria não lê sobre o financeiro, e o professor não lê sobre
 *    permissões: cada um vê o seu sistema;
 *  - tela nova sem texto de ajuda aparece com um aviso visível, em vez de
 *    sumir e deixar a impressão de que está tudo documentado;
 *  - "o que cada perfil vê" é buscado de /perfis, com os perfis que a
 *    ESCOLA tem de verdade — inclusive os que ela criou.
 *
 * O bloco "o que você não vê" existe porque metade das dúvidas de um
 * sistema com permissão é "sumiu o menu?". Dizer o que existe, que você não
 * alcança e a quem pedir resolve isso antes de virar chamado.
 */
export function AjudaPage() {
  const { user } = useAuth();
  const location = useLocation();
  const [busca, setBusca] = useState("");
  const [abertos, setAbertos] = useState({});
  const [perfis, setPerfis] = useState([]);

  const permissoes = user?.permissoes || [];
  const roles = user?.roles || [];

  /* A matriz de perfis só faz sentido para quem administra pessoas. Quem
     não pode ler /perfis simplesmente não vê a seção — sem erro na tela. */
  useEffect(() => {
    if (!permissoes.includes("PERFIS_GERIR") && !permissoes.includes("USUARIOS_VER")) return;
    api
      .get("/perfis")
      .then((r) => setPerfis(Array.isArray(r) ? r : r?.content || []))
      .catch(() => setPerfis([]));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user?.id]);

  /* Chegou por "/ajuda#/access/coordenacao": abre direto aquele item. */
  useEffect(() => {
    const alvo = decodeURIComponent(location.hash.replace(/^#/, ""));
    if (!alvo) return;
    setAbertos((p) => ({ ...p, [alvo]: true }));
    const el = document.getElementById(`ajuda-${alvo}`);
    if (el) el.scrollIntoView({ behavior: "smooth", block: "center" });
  }, [location.hash]);

  const { visiveis, ocultas } = useMemo(() => {
    const ctx = { permissoes, roles };
    const dentro = [];
    const fora = [];
    menuItems.forEach((item) => (podeVerItem(item, ctx) ? dentro : fora).push(item));
    return { visiveis: dentro, ocultas: fora };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user?.id, permissoes.length]);

  const termo = busca.trim().toLowerCase();
  const casa = (item) => {
    if (!termo) return true;
    const a = ajudaPorTela[item.path] || {};
    const texto = [
      item.label,
      a.oQueE,
      ...(a.comoUsar || []),
      ...(a.regras || []),
    ]
      .filter(Boolean)
      .join(" ")
      .toLowerCase();
    return texto.includes(termo);
  };

  const porGrupo = useMemo(() => {
    const mapa = new Map();
    visiveis.filter(casa).forEach((item) => {
      if (!mapa.has(item.group)) mapa.set(item.group, []);
      mapa.get(item.group).push(item);
    });
    return mapa;
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [visiveis, termo]);

  const perfilDoUsuario = perfis
    .filter((p) => roles.includes(p.nome))
    .map((p) => p.rotulo || p.nome)
    .join(", ");

  return (
    <div className="page ajuda">
      <div className="page-header">
        <div>
          <h1 className="page-title">Como usar o sistema</h1>
          <p className="page-subtitle">
            {perfilDoUsuario
              ? `Você está como ${perfilDoUsuario}. Abaixo está o que o seu acesso permite fazer.`
              : "Abaixo está o que o seu acesso permite fazer."}
          </p>
        </div>
      </div>

      <div className="card">
        <div className="card-body">
          <div className="filter-bar">
            <div className="form-field" style={{ flex: 1 }}>
              <label className="form-label" htmlFor="ajuda-busca">
                Procurar no tutorial
              </label>
              <input
                id="ajuda-busca"
                className="form-input"
                placeholder="Ex.: restrição, excedente, quem pode buscar..."
                value={busca}
                onChange={(e) => setBusca(e.target.value)}
              />
            </div>
          </div>
        </div>
      </div>

      {/* ------------------------------------------------ as telas ---- */}
      {menuGroups.map((grupo) => {
        const itens = porGrupo.get(grupo.key);
        if (!itens || itens.length === 0) return null;
        return (
          <section key={grupo.key} className="ajuda-secao">
            <h2 className="ajuda-secao-titulo">{grupo.label || "Início"}</h2>
            {itens.map((item) => (
              <TelaDaAjuda
                key={item.path}
                item={item}
                aberto={!!abertos[item.path]}
                onAlternar={() =>
                  setAbertos((p) => ({ ...p, [item.path]: !p[item.path] }))
                }
              />
            ))}
          </section>
        );
      })}

      {termo && porGrupo.size === 0 && (
        <div className="empty-state">
          <div className="empty-state-icon">
            <Icon name="Search" size={28} />
          </div>
          <h3>Nada encontrado para “{busca}”</h3>
          <p>Tente outra palavra, ou limpe a busca para ver tudo.</p>
        </div>
      )}

      {/* --------------------------------------------- o que não vê ---- */}
      {ocultas.length > 0 && !termo && (
        <section className="ajuda-secao">
          <h2 className="ajuda-secao-titulo">O que existe e você não acessa</h2>
          <p className="ajuda-secao-sub">
            Estas telas fazem parte do sistema, mas o seu perfil não as abre. Não é falha:
            é a divisão de responsabilidade da escola. Se precisar de alguma, peça a quem
            administra os perfis.
          </p>
          <div className="ajuda-ocultas">
            {ocultas.map((item) => (
              <span key={item.path} className="ajuda-oculta">
                <Icon name={item.icon} size={13} />
                {item.label}
              </span>
            ))}
          </div>
        </section>
      )}

      {/* ------------------------------------------------- as regras ---- */}
      {!termo && (
        <section className="ajuda-secao">
          <h2 className="ajuda-secao-titulo">Regras que o sistema não deixa violar</h2>
          <p className="ajuda-secao-sub">
            Elas explicam a maior parte dos “por que não consigo fazer isso?”.
          </p>
          <div className="ajuda-regras">
            {regrasDoSistema.map((r) => (
              <div className="ajuda-regra" key={r.titulo}>
                <Icon name="Shield" size={16} className="ajuda-regra-icone" />
                <div>
                  <strong>{r.titulo}</strong>
                  <p>{r.texto}</p>
                </div>
              </div>
            ))}
          </div>
        </section>
      )}

      {/* --------------------------------------- o que cada perfil vê ---- */}
      {!termo && perfis.length > 0 && (
        <section className="ajuda-secao">
          <h2 className="ajuda-secao-titulo">O que cada perfil vê</h2>
          <p className="ajuda-secao-sub">
            Os perfis desta escola, com as telas que cada um abre. Vem da configuração real —
            se a escola mudar uma permissão, esta lista muda junto.
          </p>
          <div className="ajuda-perfis">
            {perfis.map((perfil) => {
              const telas = menuItems.filter((item) =>
                podeVerItem(item, { permissoes: perfil.permissoes || [], roles: [] })
              );
              return (
                <div className="ajuda-perfil" key={perfil.id}>
                  <div className="ajuda-perfil-topo">
                    <strong>{perfil.rotulo || perfil.nome}</strong>
                    <span className="badge badge-secondary">{telas.length} telas</span>
                  </div>
                  {perfil.descricao && <p className="ajuda-perfil-desc">{perfil.descricao}</p>}
                  <div className="ajuda-ocultas">
                    {telas.map((t) => (
                      <span key={t.path} className="ajuda-oculta">
                        {t.label}
                      </span>
                    ))}
                    {telas.length === 0 && (
                      <span className="ajuda-oculta">Nenhuma tela — entra e não vê nada.</span>
                    )}
                  </div>
                </div>
              );
            })}
          </div>
        </section>
      )}
    </div>
  );
}

function TelaDaAjuda({ item, aberto, onAlternar }) {
  const ajuda = ajudaPorTela[item.path];

  return (
    <article className="ajuda-tela" id={`ajuda-${item.path}`}>
      <button
        type="button"
        className="ajuda-tela-topo"
        onClick={onAlternar}
        aria-expanded={aberto}
      >
        <Icon name={item.icon} size={18} />
        <span className="ajuda-tela-nome">{item.label}</span>
        <span className="ajuda-tela-resumo">{ajuda?.oQueE || "Sem descrição cadastrada."}</span>
        <Icon name={aberto ? "ChevronDown" : "ChevronRight"} size={16} />
      </button>

      {aberto && (
        <div className="ajuda-tela-corpo">
          {!ajuda && (
            <p className="ajuda-sem-texto">
              Esta tela ainda não tem tutorial escrito. Avise quem cuida do sistema —
              o texto fica em <code>src/config/ajudaConfig.js</code>.
            </p>
          )}

          {ajuda?.comoUsar?.length > 0 && (
            <>
              <h4>Como usar</h4>
              <ul>
                {ajuda.comoUsar.map((p, i) => (
                  <li key={i}>{p}</li>
                ))}
              </ul>
            </>
          )}

          {ajuda?.regras?.length > 0 && (
            <>
              <h4>O que o sistema não deixa</h4>
              <ul className="ajuda-lista-regra">
                {ajuda.regras.map((p, i) => (
                  <li key={i}>{p}</li>
                ))}
              </ul>
            </>
          )}

          <Link className="btn btn-secondary btn-sm" to={item.path}>
            <Icon name="ArrowRight" size={13} /> Abrir {item.label}
          </Link>
        </div>
      )}
    </article>
  );
}
