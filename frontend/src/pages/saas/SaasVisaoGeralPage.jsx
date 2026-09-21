import { useEffect, useMemo, useState } from "react";
import { api } from "../../services/api";
import { Icon } from "../../components/Icon";
import { MODULOS } from "../../utils/saas";

/**
 * Dashboard do SaaS, no padrão do AlfaControl: grupos de KPIs com título
 * de seção, cada KPI com o ícone tingido à esquerda e o número em
 * destaque. Os números vêm de /saas/metricas e da lista de redes.
 */
function GrupoKpi({ titulo, itens, carregando }) {
  return (
    <section className="saas-kpi-secao">
      <h3 className="saas-kpi-titulo">{titulo}</h3>
      <div className="saas-kpi-grid">
        {itens.map((k) => (
          <div key={k.label} className="saas-kpi">
            <div className={`saas-kpi-icone ${k.tom}`}>
              <Icon name={k.icone} size={22} />
            </div>
            <div>
              <div className="saas-kpi-valor">{carregando ? "—" : k.valor}</div>
              <div className="saas-kpi-label">{k.label}</div>
            </div>
          </div>
        ))}
      </div>
    </section>
  );
}

export function SaasVisaoGeralPage() {
  const [metricas, setMetricas] = useState(null);
  const [redes, setRedes] = useState([]);
  const [planos, setPlanos] = useState([]);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState("");

  useEffect(() => {
    (async () => {
      setCarregando(true);
      setErro("");
      const [m, r, p] = await Promise.allSettled([
        api.get("/saas/metricas"),
        api.get("/tenants?page=0&size=200"),
        api.get("/saas/plans"),
      ]);
      if (m.status === "fulfilled") setMetricas(m.value);
      else setErro(m.reason?.message || "Não foi possível carregar as métricas.");
      if (r.status === "fulfilled") setRedes(r.value?.content || []);
      if (p.status === "fulfilled") setPlanos(p.value?.content || p.value || []);
      setCarregando(false);
    })();
  }, []);

  // Escolas de verdade: o tenant mestre é da Alfa e não conta como cliente.
  const escolas = useMemo(() => redes.filter((r) => !r.mestre), [redes]);
  const ativas = escolas.filter((r) => r.active).length;
  const suspensas = escolas.length - ativas;
  const semModulo = escolas.filter((r) => (r.modulos || []).length === 0).length;

  const kpisRedes = [
    { label: "Total de redes", valor: escolas.length, icone: "Network", tom: "brand" },
    { label: "Redes ativas", valor: ativas, icone: "CheckCircle", tom: "success" },
    { label: "Suspensas", valor: suspensas, icone: "AlertTriangle", tom: suspensas ? "danger" : "success" },
    { label: "Sem módulo", valor: semModulo, icone: "AlertCircle", tom: semModulo ? "warning" : "success" },
  ];

  const kpisModulos = MODULOS.map((m) => {
    const quantas = escolas.filter((r) => (r.modulos || []).includes(m.codigo)).length;
    return { label: `Redes com ${m.rotulo}`, valor: quantas, icone: m.icone, tom: quantas ? "brand" : "neutro" };
  });

  const planosAtivos = planos.filter((p) => p.ativo !== false).length;
  const kpisPlanos = [
    { label: "Planos no catálogo", valor: metricas?.totalPlanos ?? planos.length, icone: "Tag", tom: "info" },
    { label: "Planos ativos", valor: planosAtivos, icone: "CheckCircle", tom: "success" },
    { label: "Módulos no catálogo", valor: MODULOS.length, icone: "LayoutGrid", tom: "brand" },
  ];

  return (
    <div>
      {erro && (
        <div className="login-error" style={{ marginBottom: 16 }}>
          <Icon name="AlertCircle" size={14} /> {erro}
        </div>
      )}
      <GrupoKpi titulo="Redes de ensino" itens={kpisRedes} carregando={carregando} />
      <GrupoKpi titulo="Módulos contratados" itens={kpisModulos} carregando={carregando} />
      <GrupoKpi titulo="Planos" itens={kpisPlanos} carregando={carregando} />
    </div>
  );
}
