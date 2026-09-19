import { useState, useEffect, useCallback } from "react";
import { api } from "../services/api";
import { Modal } from "../components/Modal";
import { Pagination } from "../components/Pagination";
import { Icon } from "../components/Icon";
import { Feedback } from "../components/access/Feedback";

const PAGE_SIZE = 20;

const STATUS_OPTIONS = ["ATIVO", "TRIAL", "SUSPENSO", "CANCELADO", "INADIMPLENTE"];

const statusBadge = (s) => {
  const map = { ATIVO: "badge-success", TRIAL: "badge-warning", SUSPENSO: "badge-danger", CANCELADO: "badge-danger", INADIMPLENTE: "badge-warning" };
  return <span className={`badge ${map[s] || "badge-secondary"}`}>{s || "—"}</span>;
};

const EMPTY_FORM = {
  nome: "", document: "", email: "", telefone: "", active: true,
  statusSaas: "TRIAL", responsavel: "", obs: "",
};

export function RedesEnsinoPage() {
  const [items, setItems] = useState([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [feedback, setFeedback] = useState(null);
  const [search, setSearch] = useState("");
  const [modalOpen, setModalOpen] = useState(false);
  const [editItem, setEditItem] = useState(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [saving, setSaving] = useState(false);
  const [activeTab, setActiveTab] = useState("dados");
  const [deleteId, setDeleteId] = useState(null);

  const load = useCallback(async (p = 0, q = search) => {
    setLoading(true);
    setError("");
    try {
      const params = new URLSearchParams({ page: p, size: PAGE_SIZE });
      if (q) params.set("search", q);
      const data = await api.get(`/tenants?${params}`);
      setItems(data?.content || data || []);
      setTotal(data?.totalElements ?? (data?.content ?? data ?? []).length);
      setPage(p);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, [search]);

  useEffect(() => { load(0, search); }, []);

  const openNew = () => {
    setEditItem(null);
    setForm(EMPTY_FORM);
    setActiveTab("dados");
    setModalOpen(true);
  };

  const openEdit = (item) => {
    setEditItem(item);
    setForm({
      nome: item.nome || "",
      document: item.document || item.cnpj || "",
      email: item.email || "",
      telefone: item.telefone || "",
      active: item.active !== false,
      statusSaas: item.statusSaas || item.status || "ATIVO",
      responsavel: item.responsavel || "",
      obs: item.obs || "",
    });
    setActiveTab("dados");
    setModalOpen(true);
  };

  const save = async () => {
    if (!form.nome.trim()) { setFeedback({ tipo: "alerta", mensagem: "Informe o nome." }); return; }
    setSaving(true);
    try {
      if (editItem) {
        await api.put(`/tenants/${editItem.id}`, form);
      } else {
        await api.post("/tenants", form);
      }
      setModalOpen(false);
      load(page, search);
    } catch (err) {
      setFeedback({ tipo: "erro", mensagem: err.message });
    } finally {
      setSaving(false);
    }
  };

  const confirmDelete = async () => {
    if (!deleteId) return;
    try {
      await api.delete(`/tenants/${deleteId}`);
      setDeleteId(null);
      load(page, search);
    } catch (err) {
      setFeedback({ tipo: "erro", mensagem: err.message });
    }
  };

  const totalPages = Math.ceil(total / PAGE_SIZE);

  const f = (k) => (e) => setForm((prev) => ({ ...prev, [k]: e.target.type === "checkbox" ? e.target.checked : e.target.value }));

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1 className="page-title">Redes de Ensino</h1>
          <p className="page-subtitle">Gerencie os tenants (redes) da plataforma</p>
        </div>
        <button className="btn btn-brand" onClick={openNew}>
          <Icon name="Plus" size={14} />
          Nova Rede
        </button>
      </div>

      {/* Search */}
      <div className="card">
        <div className="card-body">
          <div className="filter-bar">
            <div className="form-field" style={{ flex: 1 }}>
              <input
                className="form-input"
                placeholder="Buscar por nome, CNPJ ou e-mail..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                onKeyDown={(e) => e.key === "Enter" && load(0, search)}
              />
            </div>
            <button className="btn btn-brand" onClick={() => load(0, search)}>
              <Icon name="Search" size={14} />
              Buscar
            </button>
            <button className="btn btn-secondary" onClick={() => { setSearch(""); load(0, ""); }}>
              Limpar
            </button>
          </div>
        </div>
      </div>

      {error && <div className="login-error"><Icon name="AlertCircle" size={14} /> {error}</div>}

      <Feedback tipo={feedback?.tipo} mensagem={feedback?.mensagem} onFechar={() => setFeedback(null)} />

      <div className="table-wrapper">
        <table className="data-table">
          <thead>
            <tr>
              <th>Nome da Rede</th>
              <th>CNPJ</th>
              <th>E-mail</th>
              <th>Responsável</th>
              <th>Status SaaS</th>
              <th>Ativo</th>
              <th>Ações</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              Array.from({ length: 8 }).map((_, i) => (
                <tr key={i}>{Array.from({ length: 7 }).map((_, j) => <td key={j}><div className="skeleton skeleton-text" /></td>)}</tr>
              ))
            ) : items.length === 0 ? (
              <tr><td colSpan={7}>
                <div className="empty-state">
                  <div className="empty-state-icon"><Icon name="Building2" size={28} /></div>
                  <h3>Nenhuma rede encontrada</h3>
                  <p>Cadastre a primeira rede de ensino para começar.</p>
                </div>
              </td></tr>
            ) : (
              items.map((item) => (
                <tr key={item.id}>
                  <td><strong>{item.nome}</strong></td>
                  <td className="td-muted">{item.document || item.cnpj || "—"}</td>
                  <td className="td-muted">{item.email || "—"}</td>
                  <td className="td-muted">{item.responsavel || "—"}</td>
                  <td>{statusBadge(item.statusSaas || item.status)}</td>
                  <td>
                    <span className={`badge ${item.active !== false ? "badge-success" : "badge-danger"}`}>
                      {item.active !== false ? "Ativo" : "Inativo"}
                    </span>
                  </td>
                  <td>
                    <div className="td-actions">
                      <button className="btn btn-ghost btn-sm" onClick={() => openEdit(item)} title="Editar">
                        <Icon name="Edit" size={13} />
                      </button>
                      <button className="btn btn-ghost btn-sm text-danger" onClick={() => setDeleteId(item.id)} title="Excluir">
                        <Icon name="Trash" size={13} />
                      </button>
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
        <Pagination page={page} totalPages={totalPages} total={total} pageSize={PAGE_SIZE} onPageChange={(p) => load(p, search)} />
      </div>

      {/* Edit/New Modal */}
      <Modal
        isOpen={modalOpen}
        onClose={() => setModalOpen(false)}
        title={editItem ? "Editar Rede de Ensino" : "Nova Rede de Ensino"}
        size="lg"
        footer={
          <>
            <button className="btn btn-secondary" onClick={() => setModalOpen(false)}>Cancelar</button>
            <button className="btn btn-brand" onClick={save} disabled={saving}>
              {saving ? "Salvando..." : "Salvar"}
            </button>
          </>
        }
      >
        <div className="tabs">
          <div className="tabs-header">
            {["dados", "contato", "status"].map((t) => (
              <button key={t} className={`tab-btn ${activeTab === t ? "active" : ""}`} onClick={() => setActiveTab(t)}>
                {t === "dados" ? "Dados" : t === "contato" ? "Contato" : "Status"}
              </button>
            ))}
          </div>
          <div className="tab-content">
            {activeTab === "dados" && (
              <div className="form-grid">
                <div className="form-field">
                  <label className="form-label required">Nome da Rede</label>
                  <input className="form-input" value={form.nome} onChange={f("nome")} placeholder="Nome completo da rede de ensino" />
                </div>
                <div className="form-grid-2">
                  <div className="form-field">
                    <label className="form-label">CNPJ</label>
                    <input className="form-input" value={form.document} onChange={f("document")} placeholder="00.000.000/0000-00" />
                  </div>
                  <div className="form-field">
                    <label className="form-label">Responsável</label>
                    <input className="form-input" value={form.responsavel} onChange={f("responsavel")} placeholder="Nome do responsável" />
                  </div>
                </div>
                <div className="form-field">
                  <label className="form-label">Observações</label>
                  <textarea className="form-textarea" value={form.obs} onChange={f("obs")} rows={3} />
                </div>
              </div>
            )}
            {activeTab === "contato" && (
              <div className="form-grid">
                <div className="form-field">
                  <label className="form-label">E-mail</label>
                  <input className="form-input" type="email" value={form.email} onChange={f("email")} placeholder="contato@rede.edu.br" />
                </div>
                <div className="form-field">
                  <label className="form-label">Telefone</label>
                  <input className="form-input" value={form.telefone} onChange={f("telefone")} placeholder="(00) 00000-0000" />
                </div>
              </div>
            )}
            {activeTab === "status" && (
              <div className="form-grid">
                <div className="form-field">
                  <label className="form-label">Status SaaS</label>
                  <select className="form-select" value={form.statusSaas} onChange={f("statusSaas")}>
                    {STATUS_OPTIONS.map((s) => <option key={s} value={s}>{s}</option>)}
                  </select>
                </div>
                <label className="form-checkbox">
                  <input type="checkbox" checked={form.active} onChange={f("active")} />
                  <span>Rede ativa</span>
                </label>
              </div>
            )}
          </div>
        </div>
      </Modal>

      {/* Delete confirm */}
      <Modal
        isOpen={!!deleteId}
        onClose={() => setDeleteId(null)}
        title="Confirmar Exclusão"
        size="sm"
        footer={
          <>
            <button className="btn btn-secondary" onClick={() => setDeleteId(null)}>Cancelar</button>
            <button className="btn btn-danger" onClick={confirmDelete}>Excluir</button>
          </>
        }
      >
        <p style={{ color: "var(--color-text)" }}>
          Tem certeza que deseja excluir esta rede de ensino? Esta ação não pode ser desfeita.
        </p>
      </Modal>
    </div>
  );
}
