import { useState, useEffect } from "react";
import { api } from "../services/api";
import { Modal } from "../components/Modal";
import { Icon } from "../components/Icon";
import { Feedback } from "../components/access/Feedback";
import { ConfirmarModal } from "../components/access/ConfirmarModal";

const TABS = ["Dashboard", "Planos", "Redes de Ensino"];

export function SaasAdminPage() {
  const [activeTab, setActiveTab] = useState(0);
  const [metricas, setMetricas] = useState(null);
  const [planos, setPlanos] = useState([]);
  const [feedback, setFeedback] = useState(null);
  const [excluindoPlano, setExcluindoPlano] = useState(null);
  const [processandoExcluir, setProcessandoExcluir] = useState(false);
  const [redes, setRedes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  // Plan modal
  const [planModal, setPlanModal] = useState(false);
  const [editingPlan, setEditingPlan] = useState(null);
  const [planForm, setPlanForm] = useState({ nome: "", descricao: "", preco: "", limiteAlunos: "", ativo: true });
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    loadAll();
  }, []);

  const loadAll = async () => {
    setLoading(true);
    setError("");
    try {
      const [m, p, r] = await Promise.allSettled([
        api.get("/saas/metricas"),
        api.get("/saas/plans"),
        api.get("/tenants"),
      ]);
      if (m.status === "fulfilled") setMetricas(m.value);
      if (p.status === "fulfilled") setPlanos(p.value?.content || p.value || []);
      if (r.status === "fulfilled") setRedes(r.value?.content || r.value || []);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const openNewPlan = () => {
    setEditingPlan(null);
    setPlanForm({ nome: "", descricao: "", preco: "", limiteAlunos: "", ativo: true });
    setPlanModal(true);
  };

  const openEditPlan = (plan) => {
    setEditingPlan(plan);
    setPlanForm({
      nome: plan.nome || "",
      descricao: plan.descricao || "",
      preco: plan.preco ?? "",
      limiteAlunos: plan.limiteAlunos ?? "",
      ativo: plan.ativo !== false,
    });
    setPlanModal(true);
  };

  const savePlan = async () => {
    setSaving(true);
    try {
      const body = { ...planForm, preco: Number(planForm.preco), limiteAlunos: Number(planForm.limiteAlunos) };
      if (editingPlan) {
        await api.put(`/saas/plans/${editingPlan.id}`, body);
      } else {
        await api.post("/saas/plans", body);
      }
      setPlanModal(false);
      loadAll();
    } catch (err) {
      setFeedback({ tipo: "erro", mensagem: err.message });
    } finally {
      setSaving(false);
    }
  };

  const confirmarExclusaoPlano = async () => {
    setProcessandoExcluir(true);
    try {
      await api.delete(`/saas/plans/${excluindoPlano.id}`);
      setExcluindoPlano(null);
      setFeedback({ tipo: "sucesso", mensagem: "Plano excluído." });
      loadAll();
    } catch (err) {
      setFeedback({ tipo: "erro", mensagem: err.message });
    } finally {
      setProcessandoExcluir(false);
    }
  };

  const KPI_META = [
    { key: "totalRedes",   label: "Total de Redes",  icon: "Building2",  color: "brand"   },
    { key: "ativas",       label: "Redes Ativas",    icon: "CheckCircle", color: "success" },
    { key: "trial",        label: "Em Trial",        icon: "Clock",      color: "warning" },
    { key: "suspensas",    label: "Suspensas",       icon: "XCircle",    color: "danger"  },
    { key: "mrr",          label: "MRR",             icon: "DollarSign", color: "info",   prefix: "R$ " },
  ];

  const statusBadge = (s) => {
    const map = {
      ATIVO: "badge-success", TRIAL: "badge-warning",
      SUSPENSO: "badge-danger", CANCELADO: "badge-danger",
      INADIMPLENTE: "badge-warning",
    };
    return <span className={`badge ${map[s] || "badge-secondary"}`}>{s || "—"}</span>;
  };

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1 className="page-title">Painel SaaS</h1>
          <p className="page-subtitle">Administração da plataforma AlfaSchool</p>
        </div>
        <button className="btn btn-secondary" onClick={loadAll}>
          <Icon name="RefreshCw" size={14} />
          Atualizar
        </button>
      </div>

      <Feedback tipo={feedback?.tipo} mensagem={feedback?.mensagem} onFechar={() => setFeedback(null)} />

      <ConfirmarModal
        aberto={!!excluindoPlano}
        titulo="Excluir este plano?"
        textoConfirmar="Excluir plano"
        processando={processandoExcluir}
        onCancelar={() => setExcluindoPlano(null)}
        onConfirmar={confirmarExclusaoPlano}
      >
        <p>
          <strong>{excluindoPlano?.nome}</strong> deixa de ser oferecido às escolas. Quem já
          está contratado neste plano não é afetado — o vínculo existente continua valendo.
        </p>
      </ConfirmarModal>

      {error && (
        <div className="login-error">
          <Icon name="AlertCircle" size={14} /> {error}
        </div>
      )}

      {/* Tabs */}
      <div className="saas-tabs">
        {TABS.map((t, i) => (
          <button
            key={t}
            className={`saas-tab-btn ${activeTab === i ? "active" : ""}`}
            onClick={() => setActiveTab(i)}
          >
            {t}
          </button>
        ))}
      </div>

      {/* Tab: Dashboard */}
      {activeTab === 0 && (
        <div>
          {loading ? (
            <div className="saas-kpi-grid">
              {Array.from({ length: 5 }).map((_, i) => (
                <div key={i} className="skeleton skeleton-kpi" />
              ))}
            </div>
          ) : (
            <div className="saas-kpi-grid">
              {KPI_META.map((m) => (
                <div className="kpi-card" key={m.key}>
                  <div className="kpi-card-header">
                    <span className="kpi-label">{m.label}</span>
                    <div className={`kpi-icon ${m.color}`}>
                      <Icon name={m.icon} size={18} />
                    </div>
                  </div>
                  <div className="kpi-value">
                    {m.prefix || ""}{metricas?.[m.key] ?? "—"}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* Tab: Planos */}
      {activeTab === 1 && (
        <div>
          <div style={{ display: "flex", justifyContent: "flex-end", marginBottom: 12 }}>
            <button className="btn btn-brand" onClick={openNewPlan}>
              <Icon name="Plus" size={14} />
              Novo Plano
            </button>
          </div>
          <div className="table-wrapper">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Nome</th>
                  <th>Descrição</th>
                  <th>Preço (R$)</th>
                  <th>Limite Alunos</th>
                  <th>Status</th>
                  <th>Ações</th>
                </tr>
              </thead>
              <tbody>
                {loading ? (
                  Array.from({ length: 4 }).map((_, i) => (
                    <tr key={i}>
                      {Array.from({ length: 6 }).map((_, j) => (
                        <td key={j}><div className="skeleton skeleton-text" /></td>
                      ))}
                    </tr>
                  ))
                ) : planos.length === 0 ? (
                  <tr><td colSpan={6}>
                    <div className="empty-state">
                      <div className="empty-state-icon"><Icon name="Tag" size={28} /></div>
                      <h3>Nenhum plano cadastrado</h3>
                      <p>Crie o primeiro plano SaaS para disponibilizar para as redes.</p>
                    </div>
                  </td></tr>
                ) : (
                  planos.map((plan) => (
                    <tr key={plan.id}>
                      <td><strong>{plan.nome}</strong></td>
                      <td className="td-muted">{plan.descricao || "—"}</td>
                      <td>R$ {Number(plan.preco || 0).toFixed(2)}</td>
                      <td>{plan.limiteAlunos ?? "Ilimitado"}</td>
                      <td>
                        <span className={`badge ${plan.ativo ? "badge-success" : "badge-danger"}`}>
                          {plan.ativo ? "Ativo" : "Inativo"}
                        </span>
                      </td>
                      <td>
                        <div className="td-actions">
                          <button className="btn btn-ghost btn-sm" onClick={() => openEditPlan(plan)} title="Editar" aria-label="Editar">
                            <Icon name="Edit" size={13} />
                          </button>
                          <button className="btn btn-ghost btn-sm text-danger" onClick={() => setExcluindoPlano(plan)} title="Excluir" aria-label="Excluir">
                            <Icon name="Trash" size={13} />
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Tab: Redes de Ensino */}
      {activeTab === 2 && (
        <div className="table-wrapper">
          <table className="data-table">
            <thead>
              <tr>
                <th>Nome da Rede</th>
                <th>CNPJ</th>
                <th>E-mail</th>
                <th>Plano</th>
                <th>Status</th>
                <th>Criado em</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                Array.from({ length: 6 }).map((_, i) => (
                  <tr key={i}>
                    {Array.from({ length: 6 }).map((_, j) => (
                      <td key={j}><div className="skeleton skeleton-text" /></td>
                    ))}
                  </tr>
                ))
              ) : redes.length === 0 ? (
                <tr><td colSpan={6}>
                  <div className="empty-state">
                    <div className="empty-state-icon"><Icon name="Building2" size={28} /></div>
                    <h3>Nenhuma rede cadastrada</h3>
                    <p>As redes de ensino cadastradas aparecerão aqui.</p>
                  </div>
                </td></tr>
              ) : (
                redes.map((rede) => (
                  <tr key={rede.id}>
                    <td><strong>{rede.nome}</strong></td>
                    <td className="td-muted">{rede.document || rede.cnpj || "—"}</td>
                    <td className="td-muted">{rede.email || "—"}</td>
                    <td>{rede.planoNome || rede.plano || "—"}</td>
                    <td>{statusBadge(rede.statusSaas || rede.status)}</td>
                    <td className="td-muted">
                      {rede.createdAt ? new Date(rede.createdAt).toLocaleDateString("pt-BR") : "—"}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      )}

      {/* Plan modal */}
      <Modal
        isOpen={planModal}
        onClose={() => setPlanModal(false)}
        title={editingPlan ? "Editar Plano" : "Novo Plano"}
        footer={
          <>
            <button className="btn btn-secondary" onClick={() => setPlanModal(false)}>Cancelar</button>
            <button className="btn btn-brand" onClick={savePlan} disabled={saving}>
              {saving ? "Salvando..." : "Salvar"}
            </button>
          </>
        }
      >
        <div className="form-grid">
          <div className="form-field">
            <label className="form-label required">Nome do Plano</label>
            <input className="form-input" value={planForm.nome}
              onChange={(e) => setPlanForm((f) => ({ ...f, nome: e.target.value }))} placeholder="Ex: Básico, Profissional..." />
          </div>
          <div className="form-field">
            <label className="form-label">Descrição</label>
            <textarea className="form-textarea" value={planForm.descricao}
              onChange={(e) => setPlanForm((f) => ({ ...f, descricao: e.target.value }))} rows={2} />
          </div>
          <div className="form-grid-2">
            <div className="form-field">
              <label className="form-label required">Preço Mensal (R$)</label>
              <input className="form-input" type="number" min="0" step="0.01" value={planForm.preco}
                onChange={(e) => setPlanForm((f) => ({ ...f, preco: e.target.value }))} placeholder="0.00" />
            </div>
            <div className="form-field">
              <label className="form-label">Limite de Alunos</label>
              <input className="form-input" type="number" min="0" value={planForm.limiteAlunos}
                onChange={(e) => setPlanForm((f) => ({ ...f, limiteAlunos: e.target.value }))} placeholder="0 = ilimitado" />
            </div>
          </div>
          <label className="form-checkbox">
            <input type="checkbox" checked={planForm.ativo}
              onChange={(e) => setPlanForm((f) => ({ ...f, ativo: e.target.checked }))} />
            <span>Plano ativo</span>
          </label>
        </div>
      </Modal>
    </div>
  );
}
