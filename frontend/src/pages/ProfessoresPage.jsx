import { useState, useEffect, useCallback } from "react";
import { api } from "../services/api";
import { Modal } from "../components/Modal";
import { Pagination } from "../components/Pagination";
import { Icon } from "../components/Icon";

const PAGE_SIZE = 20;
const STATUS_OPTS = ["ativo", "inativo", "licença"];
const EMPTY_FORM = { nome: "", cpf: "", email: "", telefone: "", especialidade: "", status: "ativo" };

export function ProfessoresPage() {
  const [items, setItems] = useState([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [search, setSearch] = useState("");
  const [modalOpen, setModalOpen] = useState(false);
  const [editItem, setEditItem] = useState(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [saving, setSaving] = useState(false);
  const [deleteId, setDeleteId] = useState(null);

  const load = useCallback(async (p = 0) => {
    setLoading(true); setError("");
    try {
      const params = new URLSearchParams({ page: p, size: PAGE_SIZE });
      if (search) params.set("q", search);
      const data = await api.get(`/professores?${params}`);
      setItems(data?.content || data || []);
      setTotal(data?.totalElements ?? (data?.content ?? data ?? []).length);
      setPage(p);
    } catch (err) { setError(err.message); }
    finally { setLoading(false); }
  }, [search]);

  useEffect(() => { load(0); }, []);

  const openNew = () => { setEditItem(null); setForm(EMPTY_FORM); setModalOpen(true); };
  const openEdit = (item) => {
    setEditItem(item);
    setForm({ nome: item.nome || "", cpf: item.cpf || "", email: item.email || "",
      telefone: item.telefone || "", especialidade: item.especialidade || "", status: item.status || "ativo" });
    setModalOpen(true);
  };

  const save = async () => {
    if (!form.nome.trim()) { alert("Nome é obrigatório"); return; }
    setSaving(true);
    try {
      if (editItem) await api.put(`/professores/${editItem.id}`, form);
      else await api.post("/professores", form);
      setModalOpen(false); load(page);
    } catch (err) { alert(err.message); }
    finally { setSaving(false); }
  };

  const confirmDelete = async () => {
    try { await api.delete(`/professores/${deleteId}`); setDeleteId(null); load(page); }
    catch (err) { alert(err.message); }
  };

  const f = (k) => (e) => setForm(prev => ({ ...prev, [k]: e.target.value }));
  const totalPages = Math.ceil(total / PAGE_SIZE);

  const statusBadge = (s) => {
    const map = { ativo: "badge-success", inativo: "badge-danger", "licença": "badge-warning" };
    return <span className={`badge ${map[s] || "badge-secondary"}`}>{s}</span>;
  };

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1 className="page-title">Professores</h1>
          <p className="page-subtitle">Gerencie o corpo docente da escola</p>
        </div>
        <button className="btn btn-brand" onClick={openNew}><Icon name="Plus" size={14} /> Novo Professor</button>
      </div>

      <div className="card">
        <div className="card-body">
          <div className="filter-bar">
            <div className="form-field" style={{ flex: 1 }}>
              <input className="form-input" placeholder="Buscar por nome, CPF ou e-mail..."
                value={search} onChange={e => setSearch(e.target.value)} onKeyDown={e => e.key === "Enter" && load(0)} />
            </div>
            <button className="btn btn-brand" onClick={() => load(0)}><Icon name="Search" size={14} /> Filtrar</button>
            <button className="btn btn-secondary" onClick={() => { setSearch(""); load(0); }}>Limpar</button>
          </div>
        </div>
      </div>

      {error && <div className="login-error"><Icon name="AlertCircle" size={14} /> {error}</div>}

      <div className="table-wrapper">
        <table className="data-table">
          <thead>
            <tr><th>Nome</th><th>CPF</th><th>E-mail</th><th>Telefone</th><th>Especialidade</th><th>Status</th><th>Ações</th></tr>
          </thead>
          <tbody>
            {loading ? Array.from({ length: 8 }).map((_, i) => (
              <tr key={i}>{Array.from({ length: 7 }).map((_, j) => <td key={j}><div className="skeleton skeleton-text" /></td>)}</tr>
            )) : items.length === 0 ? (
              <tr><td colSpan={7}><div className="empty-state">
                <div className="empty-state-icon"><Icon name="Users" size={28} /></div>
                <h3>Nenhum professor encontrado</h3>
                <p>Cadastre o primeiro professor para começar.</p>
              </div></td></tr>
            ) : items.map(item => (
              <tr key={item.id}>
                <td><strong>{item.nome}</strong></td>
                <td className="td-muted">{item.cpf || "—"}</td>
                <td className="td-muted">{item.email || "—"}</td>
                <td className="td-muted">{item.telefone || "—"}</td>
                <td className="td-muted">{item.especialidade || "—"}</td>
                <td>{statusBadge(item.status)}</td>
                <td><div className="td-actions">
                  <button className="btn btn-ghost btn-sm" onClick={() => openEdit(item)}><Icon name="Edit" size={13} /></button>
                  <button className="btn btn-ghost btn-sm text-danger" onClick={() => setDeleteId(item.id)}><Icon name="Trash" size={13} /></button>
                </div></td>
              </tr>
            ))}
          </tbody>
        </table>
        <Pagination page={page} totalPages={totalPages} total={total} pageSize={PAGE_SIZE} onPageChange={p => load(p)} />
      </div>

      <Modal isOpen={modalOpen} onClose={() => setModalOpen(false)}
        title={editItem ? "Editar Professor" : "Novo Professor"}
        footer={<><button className="btn btn-secondary" onClick={() => setModalOpen(false)}>Cancelar</button>
          <button className="btn btn-brand" onClick={save} disabled={saving}>{saving ? "Salvando..." : "Salvar"}</button></>}>
        <div className="form-grid">
          <div className="form-field">
            <label className="form-label required">Nome Completo</label>
            <input className="form-input" value={form.nome} onChange={f("nome")} placeholder="Nome do professor" />
          </div>
          <div className="form-grid-2">
            <div className="form-field"><label className="form-label">CPF</label>
              <input className="form-input" value={form.cpf} onChange={f("cpf")} placeholder="000.000.000-00" /></div>
            <div className="form-field"><label className="form-label">Telefone</label>
              <input className="form-input" value={form.telefone} onChange={f("telefone")} placeholder="(00) 00000-0000" /></div>
          </div>
          <div className="form-field"><label className="form-label">E-mail</label>
            <input className="form-input" type="email" value={form.email} onChange={f("email")} placeholder="professor@escola.com" /></div>
          <div className="form-grid-2">
            <div className="form-field"><label className="form-label">Especialidade</label>
              <input className="form-input" value={form.especialidade} onChange={f("especialidade")} placeholder="Ex: Matemática" /></div>
            <div className="form-field"><label className="form-label">Status</label>
              <select className="form-select" value={form.status} onChange={f("status")}>
                {STATUS_OPTS.map(s => <option key={s} value={s}>{s.charAt(0).toUpperCase() + s.slice(1)}</option>)}
              </select></div>
          </div>
        </div>
      </Modal>

      <Modal isOpen={!!deleteId} onClose={() => setDeleteId(null)} title="Confirmar Exclusão" size="sm"
        footer={<><button className="btn btn-secondary" onClick={() => setDeleteId(null)}>Cancelar</button>
          <button className="btn btn-danger" onClick={confirmDelete}>Excluir</button></>}>
        <p style={{ color: "var(--color-text)" }}>Tem certeza que deseja excluir este professor?</p>
      </Modal>
    </div>
  );
}
