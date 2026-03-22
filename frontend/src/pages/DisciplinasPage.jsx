import { useState, useEffect, useCallback } from "react";
import { api } from "../services/api";
import { Modal } from "../components/Modal";
import { Pagination } from "../components/Pagination";
import { Icon } from "../components/Icon";

const PAGE_SIZE = 20;
const EMPTY_FORM = { nome: "", codigo: "", cargaHoraria: "", descricao: "", ativa: true };

export function DisciplinasPage() {
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
      const data = await api.get(`/disciplinas?${params}`);
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
    setForm({ nome: item.nome || "", codigo: item.codigo || "",
      cargaHoraria: item.cargaHoraria ?? "", descricao: item.descricao || "", ativa: item.ativa !== false });
    setModalOpen(true);
  };

  const save = async () => {
    if (!form.nome.trim()) { alert("Nome é obrigatório"); return; }
    setSaving(true);
    try {
      const body = { ...form, cargaHoraria: form.cargaHoraria ? Number(form.cargaHoraria) : null };
      if (editItem) await api.put(`/disciplinas/${editItem.id}`, body);
      else await api.post("/disciplinas", body);
      setModalOpen(false); load(page);
    } catch (err) { alert(err.message); }
    finally { setSaving(false); }
  };

  const confirmDelete = async () => {
    try { await api.delete(`/disciplinas/${deleteId}`); setDeleteId(null); load(page); }
    catch (err) { alert(err.message); }
  };

  const f = (k) => (e) => setForm(prev => ({
    ...prev, [k]: e.target.type === "checkbox" ? e.target.checked : e.target.value
  }));
  const totalPages = Math.ceil(total / PAGE_SIZE);

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1 className="page-title">Disciplinas</h1>
          <p className="page-subtitle">Gerencie as disciplinas e matérias oferecidas</p>
        </div>
        <button className="btn btn-brand" onClick={openNew}><Icon name="Plus" size={14} /> Nova Disciplina</button>
      </div>

      <div className="card">
        <div className="card-body">
          <div className="filter-bar">
            <div className="form-field" style={{ flex: 1 }}>
              <input className="form-input" placeholder="Buscar por nome..."
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
            <tr><th>Nome</th><th>Código</th><th>Carga Horária</th><th>Status</th><th>Ações</th></tr>
          </thead>
          <tbody>
            {loading ? Array.from({ length: 8 }).map((_, i) => (
              <tr key={i}>{Array.from({ length: 5 }).map((_, j) => <td key={j}><div className="skeleton skeleton-text" /></td>)}</tr>
            )) : items.length === 0 ? (
              <tr><td colSpan={5}><div className="empty-state">
                <div className="empty-state-icon"><Icon name="BookMarked" size={28} /></div>
                <h3>Nenhuma disciplina encontrada</h3>
                <p>Cadastre disciplinas para montar a grade curricular.</p>
              </div></td></tr>
            ) : items.map(item => (
              <tr key={item.id}>
                <td><strong>{item.nome}</strong></td>
                <td className="td-muted">{item.codigo || "—"}</td>
                <td className="td-muted">{item.cargaHoraria ? `${item.cargaHoraria}h` : "—"}</td>
                <td><span className={`badge ${item.ativa !== false ? "badge-success" : "badge-danger"}`}>
                  {item.ativa !== false ? "Ativa" : "Inativa"}
                </span></td>
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
        title={editItem ? "Editar Disciplina" : "Nova Disciplina"}
        footer={<><button className="btn btn-secondary" onClick={() => setModalOpen(false)}>Cancelar</button>
          <button className="btn btn-brand" onClick={save} disabled={saving}>{saving ? "Salvando..." : "Salvar"}</button></>}>
        <div className="form-grid">
          <div className="form-field">
            <label className="form-label required">Nome da Disciplina</label>
            <input className="form-input" value={form.nome} onChange={f("nome")} placeholder="Ex: Matemática" />
          </div>
          <div className="form-grid-2">
            <div className="form-field"><label className="form-label">Código</label>
              <input className="form-input" value={form.codigo} onChange={f("codigo")} placeholder="MAT-01" /></div>
            <div className="form-field"><label className="form-label">Carga Horária (h)</label>
              <input className="form-input" type="number" min="0" value={form.cargaHoraria} onChange={f("cargaHoraria")} placeholder="80" /></div>
          </div>
          <div className="form-field"><label className="form-label">Descrição</label>
            <textarea className="form-textarea" value={form.descricao} onChange={f("descricao")} rows={3} /></div>
          <label className="form-checkbox">
            <input type="checkbox" checked={form.ativa} onChange={f("ativa")} />
            <span>Disciplina ativa</span>
          </label>
        </div>
      </Modal>

      <Modal isOpen={!!deleteId} onClose={() => setDeleteId(null)} title="Confirmar Exclusão" size="sm"
        footer={<><button className="btn btn-secondary" onClick={() => setDeleteId(null)}>Cancelar</button>
          <button className="btn btn-danger" onClick={confirmDelete}>Excluir</button></>}>
        <p style={{ color: "var(--color-text)" }}>Tem certeza que deseja excluir esta disciplina?</p>
      </Modal>
    </div>
  );
}
