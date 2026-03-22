import { useState, useEffect, useCallback } from "react";
import { api } from "../services/api";
import { Modal } from "../components/Modal";
import { Pagination } from "../components/Pagination";
import { Icon } from "../components/Icon";

const PAGE_SIZE = 20;

const MODALIDADES = ["Presencial", "EAD", "Híbrido"];
const NIVEIS = ["Fundamental", "Médio", "Técnico", "Superior", "Pós-graduação", "Extensão"];

const EMPTY_FORM = {
  nome: "", codigo: "", modalidade: "Presencial", nivel: "Médio",
  cargaHoraria: "", descricao: "", ativo: true,
};

export function CursosPage() {
  const [items, setItems] = useState([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [search, setSearch] = useState("");
  const [filterModalidade, setFilterModalidade] = useState("");
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
      if (search) params.set("search", search);
      if (filterModalidade) params.set("modalidade", filterModalidade);
      const data = await api.get(`/cursos?${params}`);
      setItems(data?.content || data || []);
      setTotal(data?.totalElements ?? (data?.content ?? data ?? []).length);
      setPage(p);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, [search, filterModalidade]);

  useEffect(() => { load(0); }, []);

  const openNew = () => { setEditItem(null); setForm(EMPTY_FORM); setModalOpen(true); };

  const openEdit = (item) => {
    setEditItem(item);
    setForm({
      nome: item.nome || "",
      codigo: item.codigo || "",
      modalidade: item.modalidade || "Presencial",
      nivel: item.nivel || "Médio",
      cargaHoraria: item.cargaHoraria ?? "",
      descricao: item.descricao || "",
      ativo: item.ativo !== false,
    });
    setModalOpen(true);
  };

  const save = async () => {
    if (!form.nome.trim()) { alert("Nome é obrigatório"); return; }
    setSaving(true);
    try {
      const body = { ...form, cargaHoraria: form.cargaHoraria ? Number(form.cargaHoraria) : null };
      if (editItem) {
        await api.put(`/cursos/${editItem.id}`, body);
      } else {
        await api.post("/cursos", body);
      }
      setModalOpen(false);
      load(page);
    } catch (err) {
      alert(err.message);
    } finally {
      setSaving(false);
    }
  };

  const confirmDelete = async () => {
    if (!deleteId) return;
    try {
      await api.delete(`/cursos/${deleteId}`);
      setDeleteId(null);
      load(page);
    } catch (err) {
      alert(err.message);
    }
  };

  const totalPages = Math.ceil(total / PAGE_SIZE);
  const f = (k) => (e) => setForm((prev) => ({ ...prev, [k]: e.target.type === "checkbox" ? e.target.checked : e.target.value }));

  const modalidadeBadge = (m) => {
    const map = { "Presencial": "badge-success", "EAD": "badge-info", "Híbrido": "badge-warning" };
    return <span className={`badge ${map[m] || "badge-secondary"}`}>{m}</span>;
  };

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1 className="page-title">Cursos</h1>
          <p className="page-subtitle">Gerencie os cursos oferecidos pelas escolas</p>
        </div>
        <button className="btn btn-brand" onClick={openNew}>
          <Icon name="Plus" size={14} />
          Novo Curso
        </button>
      </div>

      <div className="card">
        <div className="card-body">
          <div className="filter-bar">
            <div className="form-field" style={{ flex: 1 }}>
              <input className="form-input" placeholder="Buscar por nome ou código..."
                value={search} onChange={(e) => setSearch(e.target.value)}
                onKeyDown={(e) => e.key === "Enter" && load(0)} />
            </div>
            <div className="form-field">
              <select className="form-select" value={filterModalidade} onChange={(e) => { setFilterModalidade(e.target.value); }}>
                <option value="">Todas as modalidades</option>
                {MODALIDADES.map((m) => <option key={m} value={m}>{m}</option>)}
              </select>
            </div>
            <button className="btn btn-brand" onClick={() => load(0)}>
              <Icon name="Search" size={14} />
              Filtrar
            </button>
            <button className="btn btn-secondary" onClick={() => { setSearch(""); setFilterModalidade(""); load(0); }}>
              Limpar
            </button>
          </div>
        </div>
      </div>

      {error && <div className="login-error"><Icon name="AlertCircle" size={14} /> {error}</div>}

      <div className="table-wrapper">
        <table className="data-table">
          <thead>
            <tr>
              <th>Nome</th>
              <th>Código</th>
              <th>Modalidade</th>
              <th>Nível</th>
              <th>Carga Horária</th>
              <th>Status</th>
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
                  <div className="empty-state-icon"><Icon name="BookOpen" size={28} /></div>
                  <h3>Nenhum curso encontrado</h3>
                  <p>Crie o primeiro curso para começar a organizar sua grade curricular.</p>
                </div>
              </td></tr>
            ) : (
              items.map((item) => (
                <tr key={item.id}>
                  <td><strong>{item.nome}</strong></td>
                  <td className="td-muted">{item.codigo || "—"}</td>
                  <td>{modalidadeBadge(item.modalidade)}</td>
                  <td className="td-muted">{item.nivel || "—"}</td>
                  <td className="td-muted">
                    {item.cargaHoraria ? `${item.cargaHoraria}h` : "—"}
                  </td>
                  <td>
                    <span className={`badge ${item.ativo !== false ? "badge-success" : "badge-danger"}`}>
                      {item.ativo !== false ? "Ativo" : "Inativo"}
                    </span>
                  </td>
                  <td>
                    <div className="td-actions">
                      <button className="btn btn-ghost btn-sm" onClick={() => openEdit(item)}>
                        <Icon name="Edit" size={13} />
                      </button>
                      <button className="btn btn-ghost btn-sm text-danger" onClick={() => setDeleteId(item.id)}>
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
      <Modal isOpen={modalOpen} onClose={() => setModalOpen(false)}
        title={editItem ? "Editar Curso" : "Novo Curso"}
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
            <label className="form-label required">Nome do Curso</label>
            <input className="form-input" value={form.nome} onChange={f("nome")} placeholder="Ex: Ensino Médio Regular" />
          </div>
          <div className="form-grid-2">
            <div className="form-field">
              <label className="form-label">Código</label>
              <input className="form-input" value={form.codigo} onChange={f("codigo")} placeholder="Ex: EMR-01" />
            </div>
            <div className="form-field">
              <label className="form-label">Carga Horária (h)</label>
              <input className="form-input" type="number" min="0" value={form.cargaHoraria} onChange={f("cargaHoraria")} placeholder="800" />
            </div>
          </div>
          <div className="form-grid-2">
            <div className="form-field">
              <label className="form-label">Modalidade</label>
              <select className="form-select" value={form.modalidade} onChange={f("modalidade")}>
                {MODALIDADES.map((m) => <option key={m} value={m}>{m}</option>)}
              </select>
            </div>
            <div className="form-field">
              <label className="form-label">Nível</label>
              <select className="form-select" value={form.nivel} onChange={f("nivel")}>
                {NIVEIS.map((n) => <option key={n} value={n}>{n}</option>)}
              </select>
            </div>
          </div>
          <div className="form-field">
            <label className="form-label">Descrição</label>
            <textarea className="form-textarea" value={form.descricao} onChange={f("descricao")} rows={3} />
          </div>
          <label className="form-checkbox">
            <input type="checkbox" checked={form.ativo} onChange={f("ativo")} />
            <span>Curso ativo</span>
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
        <p style={{ color: "var(--color-text)" }}>Tem certeza que deseja excluir este curso?</p>
      </Modal>
    </div>
  );
}
