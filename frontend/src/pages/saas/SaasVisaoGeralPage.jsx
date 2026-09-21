import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../../services/api";
import { Icon } from "../../components/Icon";

/**
 * Visão geral da plataforma: quantas redes, quantas ativas, planos.
 * Os números vêm de /saas/metricas; a lista curta de redes é a mesma
 * página de Redes, cortada nas cinco mais recentes.
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
        api.get("/tenants?page=0&size=50"),
      ]);
      if (m.status === "fulfilled") setMetricas(m.value);
      else setErro(m.reason?.message || "Não foi possível carregar as métricas.");
      if (r.status === "fulfilled") {
        const lista = r.value?.content || r.value || [];
        setRedes(
          [...lista]
            .sort((a, b) => new Date(b.createdAt || 0) - new Date(a.createdAt || 0))
            .slice(0, 5)
        );
      }
      setCarregando(false);
    })();
  }, []);

  const total = metricas?.totalTenants ?? null;
  const ativas = metricas?.tenantsAtivos ?? null;
  const KPIS = [
    { label: "Redes cadastradas", valor: total, icone: "Network", tom: "brand" },
    { label: "Redes ativas", valor: ativas, icone: "CheckCircle", tom: "success" },
    { label: "Redes inativas", valor: total != null && ativas != null ? total - ativas : null, icone: "XCircle", tom: "danger" },
    { label: "Planos", valor: metricas?.totalPlanos ?? null, icone: "Tag", tom: "info" },
  ];

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1 className="page-title">Visão geral</h1>
          <p className="page-subtitle">A plataforma AlfaSchool vista pela Alfa: redes atendidas e planos.</p>
        </div>
        <Link to="/saas/redes" className="btn btn-brand">
          <Icon name="Plus" size={14} /> Nova rede
        </Link>
      </div>

      {erro && (
        <div className="login-error">
          <Icon name="AlertCircle" size={14} /> {erro}
        </div>
      )}

      <div className="saas-kpi-grid">
        {carregando
          ? KPIS.map((k) => <div key={k.label} className="skeleton skeleton-kpi" />)
          : KPIS.map((k) => (
              <div className="kpi-card" key={k.label}>
                <div className="kpi-card-header">
                  <span className="kpi-label">{k.label}</span>
                  <div className={`kpi-icon ${k.tom}`}>
                    <Icon name={k.icone} size={18} />
                  </div>
                </div>
                <div className="kpi-value">{k.valor ?? "—"}</div>
              </div>
            ))}
      </div>

      <div className="card">
        <div className="card-header">
          <span className="card-title">Redes mais recentes</span>
          <Link to="/saas/redes" className="btn-link">Ver todas</Link>
        </div>
        <div className="table-wrapper" style={{ border: 0, borderRadius: 0 }}>
          <table className="data-table">
            <thead>
              <tr>
                <th>Rede</th>
                <th>CNPJ</th>
                <th>Módulos</th>
                <th>Situação</th>
                <th>Criada em</th>
              </tr>
            </thead>
            <tbody>
              {carregando ? (
                Array.from({ length: 3 }).map((_, i) => (
                  <tr key={i}>
                    {Array.from({ length: 5 }).map((_, j) => (
                      <td key={j}><div className="skeleton skeleton-text" /></td>
                    ))}
                  </tr>
                ))
              ) : redes.length === 0 ? (
                <tr><td colSpan={5}>
                  <div className="empty-state">
                    <div className="empty-state-icon"><Icon name="Network" size={28} /></div>
                    <h3>Nenhuma rede ainda</h3>
                    <p>Cadastre a primeira em Redes de ensino.</p>
                  </div>
                </td></tr>
              ) : (
                redes.map((r) => (
                  <tr key={r.id}>
                    <td>
                      <strong>{r.name}</strong>
                      {r.mestre && <span className="badge badge-secondary" style={{ marginLeft: 8 }}>Alfa</span>}
                    </td>
                    <td className="td-muted">{r.mestre ? "—" : r.document}</td>
                    <td className="td-muted">{(r.modulos || []).length}</td>
                    <td>
                      <span className={`badge ${r.active ? "badge-success" : "badge-danger"}`}>
                        {r.active ? "Ativa" : "Inativa"}
                      </span>
                    </td>
                    <td className="td-muted">
                      {r.createdAt ? new Date(r.createdAt).toLocaleDateString("pt-BR") : "—"}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
