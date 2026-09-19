import { useState, useEffect, useCallback } from "react";
import { api } from "../services/api";
import { Modal } from "../components/Modal";
import { Pagination } from "../components/Pagination";
import { Icon } from "../components/Icon";
import { Feedback } from "../components/access/Feedback";

const PAGE_SIZE = 20;

const ESTADOS_BR = [
  "AC","AL","AP","AM","BA","CE","DF","ES","GO","MA","MT","MS","MG",
  "PA","PB","PR","PE","PI","RJ","RN","RS","RO","RR","SC","SP","SE","TO",
];

const EMPTY_FORM = {
  nome: "", endereco: "", cidade: "", estado: "", cep: "",
  email: "", telefone: "", active: true,
};

export function EscolasPage() {
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
  const [deleteId, setDeleteId] = useState(null);

  const load = useCallback(async (p = 0) => {
    setLoading(true);
    setError("");
    try {
      const params = new URLSearchParams({ page: p, size: PAGE_SIZE });
      if (search) params.set("q", search);
      const data = await api.get(`/unidades?${params}`);
      setItems(data?.content || data || []);
      setTotal(data?.totalElements ?? (data?.content ?? data ?? []).length);
      setPage(p);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, [search]);

  useEffect(() => {
    load(0);
  }, []);

  const openNew = () => {
    setEditItem(null);
    setForm(EMPTY_FORM);
    setModalOpen(true);
  };

  const openEdit = (item) => {
    setEditItem(item);
    setForm({
      nome: item.name || "",
      endereco: item.address || "",
      cidade: item.city || "",
      estado: item.state || "",
      cep: item.cep || "",
      email: item.email || "",
      telefone: item.telefone || "",
      active: item.active !== false,
    });
    setModalOpen(true);
  };

  const save = async () => {
    if (!form.nome.trim()) { setFeedback({ tipo: "alerta", mensagem: "Informe o nome." }); return; }
    setSaving(true);
    try {
      // A API de unidades usa name/address/city/state. A tela mandava
      // nome/endereco/cidade/estado, entao `name` — que e' obrigatorio —
      // nunca chegava: todo salvamento dava 400 "Informe o nome da unidade".
      const body = {
        name: form.nome.trim(),
        address: form.endereco?.trim() || null,
        city: form.cidade?.trim() || null,
        state: form.estado?.trim() || null,
        cep: form.cep?.trim() || null,
        email: form.email?.trim() || null,
        telefone: form.telefone?.trim() || null,
        active: form.active,
      };
      if (editItem) {
        await api.put(`/unidades/${editItem.id}`, body);
      } else {
        await api.post("/unidades", body);
      }
      setModalOpen(false);
      load(page);
    } catch (err) {
      setFeedback({ tipo: "erro", mensagem: err.message });
    } finally {
      setSaving(false);
    }
  };

  const confirmDelete = async () => {
    if (!deleteId) return;
    try {
      await api.delete(`/unidades/${deleteId}`);
      setDeleteId(null);
      load(page);
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
          <h1 className="page-title">Escolas</h1>
          <p className="page-subtitle">Unidades escolares cadastradas na plataforma</p>
        </div>
        <button className="btn btn-brand" onClick={openNew}>
          <Icon name="Plus" size={14} />
          Nova Escola
        </button>
      </div>

      <div className="card">
        <div className="card-body">
          <div className="filter-bar">
            <div className="form-field" style={{ flex: 1 }}>
              <input
                className="form-input"
                placeholder="Buscar por nome, cidade..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                onKeyDown={(e) => e.key === "Enter" && load(0)}
              />
            </div>
            <button className="btn btn-brand" onClick={() => load(0)}>
              <Icon name="Search" size={14} />
              Buscar
            </button>
            <button className="btn btn-secondary" onClick={() => { setSearch(""); setFilterRede(""); load(0); }}>
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
              <th>Nome da Escola</th>
              <th>Cidade / Estado</th>
              <th>E-mail</th>
              <th>Telefone</th>
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
              <tr><td colSpan={6}>
                <div className="empty-state">
                  <div className="empty-state-icon"><Icon name="School" size={28} /></div>
                  <h3>Nenhuma escola encontrada</h3>
                  <p>Cadastre a primeira escola da plataforma.</p>
                </div>
              </td></tr>
            ) : (
              items.map((item) => (
                <tr key={item.id}>
                  <td><strong>{item.name}</strong></td>
                  <td className="td-muted">
                    {[item.city, item.state].filter(Boolean).join(" / ") || "—"}
                  </td>
                  <td className="td-muted">{item.email || "—"}</td>
                  <td className="td-muted">{item.telefone || "—"}</td>
                  <td>
                    <span className={`badge ${item.active !== false ? "badge-success" : "badge-danger"}`}>
                      {item.active !== false ? "Ativo" : "Inativo"}
                    </span>
                  </td>
                  <td>
                    <div className="td-actions">
                      <button className="btn btn-ghost btn-sm" onClick={() => openEdit(item)} title="Editar" aria-label="Editar">
                        <Icon name="Edit" size={13} />
                      </button>
                      <button className="btn btn-ghost btn-sm text-danger" onClick={() => setDeleteId(item.id)} title="Excluir" aria-label="Excluir">
                        <Icon name="Trash" size={13} />
                      </button>
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
        <Pagination page={page} totalPages={totalPages} total={total} pageSize={PAGE_SIZE} onPageChange={(p) => load(p)} />
      </div>

      {/* Modal */}
      <Modal
        isOpen={modalOpen}
        onClose={() => setModalOpen(false)}
        title={editItem ? "Editar Escola" : "Nova Escola"}
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
        <div className="form-grid">
          <div className="form-field">
            <label className="form-label required">Nome da Escola</label>
            <input className="form-input" value={form.nome} onChange={f("nome")} placeholder="Nome completo da escola" />
          </div>
          <div className="form-field">
            <label className="form-label">Endereço</label>
            <input className="form-input" value={form.endereco} onChange={f("endereco")} placeholder="Rua, número, bairro" />
          </div>
          <div className="form-grid-3">
            <div className="form-field">
              <label className="form-label">Cidade</label>
              <input className="form-input" value={form.cidade} onChange={f("cidade")} placeholder="Cidade" />
            </div>
            <div className="form-field">
              <label className="form-label">Estado</label>
              <select className="form-select" value={form.estado} onChange={f("estado")}>
                <option value="">UF</option>
                {ESTADOS_BR.map((e) => <option key={e} value={e}>{e}</option>)}
              </select>
            </div>
            <div className="form-field">
              <label className="form-label">CEP</label>
              <input className="form-input" value={form.cep} onChange={f("cep")} placeholder="00000-000" />
            </div>
          </div>
          <div className="form-grid-2">
            <div className="form-field">
              <label className="form-label">E-mail</label>
              <input className="form-input" type="email" value={form.email} onChange={f("email")} />
            </div>
            <div className="form-field">
              <label className="form-label">Telefone</label>
              <input className="form-input" value={form.telefone} onChange={f("telefone")} />
            </div>
          </div>
          <label className="form-checkbox">
            <input type="checkbox" checked={form.active} onChange={f("active")} />
            <span>Escola ativa</span>
          </label>
        </div>
      </Modal>

      {/* Delete confirm */}
      <Modal isOpen={!!deleteId} onClose={() => setDeleteId(null)} title="Confirmar Exclusão" size="sm"
        footer={
          <>
            <button className="btn btn-secondary" onClick={() => setDeleteId(null)}>Cancelar</button>
            <button className="btn btn-danger" onClick={confirmDelete}>Excluir</button>
          </>
        }
      >
        <p style={{ color: "var(--color-text)" }}>Tem certeza que deseja excluir esta escola?</p>
      </Modal>
    </div>
  );
}
