import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../../services/api";
import { Icon } from "../../components/Icon";
import { MODULOS, formatarCnpj } from "../../utils/saas";
import { Monograma } from "./components/Monograma";
import { ReguaModulos } from "./components/ReguaModulos";

function saudacao() {
  const h = new Date().getHours();
  if (h < 12) return "Bom dia";
  if (h < 18) return "Boa tarde";
  return "Boa noite";
}

/**
 * Visão geral: a carteira da Alfa num relance. Quantas redes, quantas no
 * ar, e — o que importa para o negócio — quais módulos cada uma contratou.
 * A cobertura por módulo é o mapa de onde ainda há o que vender.
 */
export function SaasVisaoGeralPage() {
  const [metricas, setMetricas] = useState(null);
  const [redes, setRedes] = useState([]);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState("");

  useEffect(() => {
    (async () => {
      setCarregando(true);
      setErro("");
      const [m, r] = await Promise.allSettled([
        api.get("/saas/metricas"),
        api.get("/tenants?page=0&size=200"),
      ]);
      if (m.status === "fulfilled") setMetricas(m.value);
      else setErro(m.reason?.message || "Não foi possível carregar as métricas.");
      if (r.status === "fulfilled") setRedes(r.value?.content || []);
      setCarregando(false);
    })();
  }, []);

  // Escolas de verdade: o tenant mestre é da Alfa e não conta como cliente.
  const escolas = useMemo(() => redes.filter((r) => !r.mestre), [redes]);
  const ativas = escolas.filter((r) => r.active).length;
  const suspensas = escolas.length - ativas;
  const semModulo = escolas.filter((r) => (r.modulos || []).length === 0).length;

  const cobertura = useMemo(
    () =>
      MODULOS.map((m) => ({
        ...m,
        quantas: escolas.filter((r) => (r.modulos || []).includes(m.codigo)).length,
      })),
    [escolas]
  );

  const recentes = useMemo(
    () =>
      [...escolas]
        .sort((a, b) => new Date(b.createdAt || 0) - new Date(a.createdAt || 0))
        .slice(0, 6),
    [escolas]
  );

  const dataBruta = new Date().toLocaleDateString("pt-BR", { weekday: "long", day: "numeric", month: "long" });
  // So' a primeira letra: "segunda-feira, 21 de setembro" e' como se escreve.
  const data = dataBruta.charAt(0).toUpperCase() + dataBruta.slice(1);

  return (
    <div className="page saas-visao">
      <div className="saas-visao-topo">
        <div>
          <p className="saas-visao-data">{data}</p>
          <h1 className="saas-visao-titulo">{saudacao()}. A plataforma está com {carregando ? "…" : ativas} {ativas === 1 ? "rede" : "redes"} no ar.</h1>
        </div>
        <Link to="/saas/redes?nova=1" className="btn btn-brand">
          <Icon name="Plus" size={14} /> Nova rede
        </Link>
      </div>

      {erro && (
        <div className="login-error">
          <Icon name="AlertCircle" size={14} /> {erro}
        </div>
      )}

      <div className="saas-visao-grid">
        {/* Carteira: três números numa linha só, separados por filete. Um
            card por número seria pompa demais para "4, 4 e 0". */}
        <section className="card saas-carteira">
          <header className="saas-card-cabecalho">
            <h2>Carteira</h2>
            <span className="saas-card-nota">{metricas?.totalPlanos ?? 0} {metricas?.totalPlanos === 1 ? "plano" : "planos"} no catálogo</span>
          </header>
          <div className="saas-carteira-numeros">
            <div>
              <strong>{carregando ? "—" : escolas.length}</strong>
              <span>redes cadastradas</span>
            </div>
            <div className="ok">
              <strong>{carregando ? "—" : ativas}</strong>
              <span>ativas</span>
            </div>
            <div className={suspensas ? "alerta" : ""}>
              <strong>{carregando ? "—" : suspensas}</strong>
              <span>suspensas</span>
            </div>
            <div className={semModulo ? "alerta" : ""}>
              <strong>{carregando ? "—" : semModulo}</strong>
              <span>sem módulo</span>
            </div>
          </div>
        </section>

        {/* Cobertura: para cada módulo, quantas redes contratam. É a leitura
            comercial — onde ainda há o que oferecer. */}
        <section className="card saas-cobertura">
          <header className="saas-card-cabecalho">
            <h2>Cobertura por módulo</h2>
            <span className="saas-card-nota">de {carregando ? "—" : escolas.length} redes</span>
          </header>
          <ul className="saas-cobertura-lista">
            {cobertura.map((m) => {
              const pct = escolas.length ? Math.round((m.quantas / escolas.length) * 100) : 0;
              return (
                <li key={m.codigo}>
                  <span className="saas-cobertura-rotulo">
                    <Icon name={m.icone} size={14} /> {m.rotulo}
                  </span>
                  <span className="saas-cobertura-barra" aria-hidden="true">
                    <span style={{ width: `${pct}%` }} />
                  </span>
                  <span className="saas-cobertura-valor">
                    <strong>{carregando ? "—" : m.quantas}</strong>
                    <small>{pct}%</small>
                  </span>
                </li>
              );
            })}
          </ul>
        </section>
      </div>

      <section className="card saas-recentes">
        <header className="saas-card-cabecalho">
          <h2>Redes mais recentes</h2>
          <Link to="/saas/redes" className="btn-link">Ver todas</Link>
        </header>
        {carregando ? (
          <div className="saas-recentes-lista">
            {Array.from({ length: 3 }).map((_, i) => <div key={i} className="skeleton" style={{ height: 56 }} />)}
          </div>
        ) : recentes.length === 0 ? (
          <div className="empty-state">
            <div className="empty-state-icon"><Icon name="Network" size={28} /></div>
            <h3>Nenhuma rede ainda</h3>
            <p>Cadastre a primeira em Redes de ensino.</p>
          </div>
        ) : (
          <ul className="saas-recentes-lista">
            {recentes.map((r) => (
              <li key={r.id} className="saas-recentes-item">
                <Monograma nome={r.name} tamanho={36} />
                <div className="saas-recentes-texto">
                  <strong>{r.name}</strong>
                  <span>{formatarCnpj(r.document)} · desde {r.createdAt ? new Date(r.createdAt).toLocaleDateString("pt-BR") : "—"}</span>
                </div>
                <ReguaModulos contratados={r.modulos} compacta />
                <span className={`saas-status ${r.active ? "ativa" : "suspensa"}`}>
                  {r.active ? "Ativa" : "Suspensa"}
                </span>
              </li>
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}
