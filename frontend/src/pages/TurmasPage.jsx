import { useState, useEffect, useCallback } from "react";
import { api } from "../services/api";
import { Modal } from "../components/Modal";
import { Pagination } from "../components/Pagination";
import { Icon } from "../components/Icon";

const PAGE_SIZE = 20;

const TURNOS = ["Manhã", "Tarde", "Noite", "Integral"];

const EMPTY_FORM = {
  nome: "", cursoId: "", anoLetivo: new Date().getFullYear().toString(),
  turno: "Manhã", professor: "", capacidade: "", ativo: true,
};

export function TurmasPage() {
  const [items, setItems] = useState([]);
  const [cursos, setCursos] = useState([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [search, setSearch] = useState("");
  const [filterCurso, setFilterCurso] = useState("");
  const [filterAno, setFilterAno] = useState("");
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
      if (filterCurso) params.set("cursoId", filterCurso);
      if (filterAno) params.set("anoLetivo", filterAno);
      const data = await api.get(`/turmas?${params}`);
      setItems(data?.content || data || []);
      setTotal(data?.totalElements ?? (data?.content ?? data ?? []).length);
      setPage(p);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, [search, filterCurso, filterAno]);

  useEffect(() => {
    load(0);
    api.get("/cursos?size=200&ativo=true").then((d) => setCursos(d?.content || d || [])).catch(() => {});
  }, []);

  const openNew = () => { setEditItem(null); setForm(EMPTY_FORM); setModalOpen(true); };

  const openEdit = (item) => {
    setEditItem(item);
    setForm({
      nome: item.nome || "",
      cursoId: item.cursoId || "",
      anoLetivo: item.anoLetivo || "",
      turno: item.turno || "Manhã",
      professor: item.professor || "",
      capacidade: item.capacidade ?? "",
      ativo: item.ativo !== false,
    });
    setModalOpen(true);
  };

  const save = async () => {
    if (!form.nome.trim()) { alert("Nome é obrigatório"); return; }
    if (!form.cursoId) { alert("Selecione o curso"); return; }
    setSaving(true);
    try {
      const body = { ...form, capacidade: form.capacidade ? Number(form.capacidade) : null };
      if (editItem) {
        await api.put(`/turmas/${editItem.id}`, body);
      } else {
        await api.post("/turmas", body);
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
      await api.delete(`/turmas/${deleteId}`);
      setDeleteId(null);
      load(page);
    } catch (err) {
      alert(err.message);
    }
  };

  const totalPages = Math.ceil(total / PAGE_SIZE);
  const f = (k) => (e) => setForm((prev) => ({ ...prev, [k]: e.target.type === "checkbox" ? e.target.checked : e.target.value }));

  const getCursoNome = (id) => cursos.find((c) => c.id === id)?.nome || "—";

  const turnoBadge = (t) => {
    const map = { "Manhã": "badge-info", "Tarde": "badge-warning", "Noite": "badge-secondary", "Integral": "badge-brand" };
    return <span className={`badge ${map[t] || "badge-secondary"}`}>{t}</span>;
  };

  // Generate year options
  const currentYear = new Date().getFullYear();
  const years = Array.from({ length: 5 }, (_, i) => currentYear - 1 + i);

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1 className="page-title">Turmas</h1>
          <p className="page-subtitle">Gerenciamento das turmas e classes</p>
        </div>
        <button className="btn btn-brand" onClick={openNew}>
          <Icon name="Plus" size={14} />
          Nova Turma
        </button>
      </div>

      <div className="card">
        <div className="card-body">
          <div className="filter-bar">
            <div className="form-field" style={{ flex: 1 }}>
              <input className="form-input" placeholder="Buscar por nome ou professor..."
                value={search} onChange={(e) => setSearch(e.target.value)}
                onKeyDown={(e) => e.key === "Enter" && load(0)} />
            </div>
            <div className="form-field">
              <select className="form-select" value={filterCurso} onChange={(e) => setFilterCurso(e.target.value)}>
                <option value="">Todos os cursos</option>
                {cursos.map((c) => <option key={c.id} value={c.id}>{c.nome}</option>)}
              </select>
            </div>
            <div className="form-field">
              <select className="form-select" value={filterAno} onChange={(e) => setFilterAno(e.target.value)}>
                <option value="">Todos os anos</option>
                {years.map((y) => <option key={y} value={y}>{y}</option>)}
              </select>
            </div>
            <button className="btn btn-brand" onClick={() => load(0)}>
              <Icon name="Filter" size={14} />
              Filtrar
            </button>
            <button className="btn btn-secondary" onClick={() => { setSearch(""); setFilterCurso(""); setFilterAno(""); load(0); }}>
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
              <th>Nome da Turma</th>
              <th>Curso</th>
              <th>Ano Letivo</th>
              <th>Turno</th>
              <th>Professor</th>
              <th>Capacidade</th>
              <th>Alunos</th>
              <th>Status</th>
              <th>Ações</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              Array.from({ length: 8 }).map((_, i) => (
                <tr key={i}>{Array.from({ length: 9 }).map((_, j) => <td key={j}><div className="skeleton skeleton-text" /></td>)}</tr>
              ))
            ) : items.length === 0 ? (
              <tr><td colSpan={9}>
                <div className="empty-state">
                  <div className="empty-state-icon"><Icon name="Users" size={28} /></div>
                  <h3>Nenhuma turma encontrada</h3>
                  <p>Crie a primeira turma vinculando-a a um curso.</p>
                </div>
              </td></tr>
            ) : (
              items.map((item) => (
                <tr key={item.id}>
                  <td><strong>{item.nome}</strong></td>
                  <td className="td-muted">{item.cursoNome || getCursoNome(item.cursoId)}</td>
                  <td className="td-muted">{item.anoLetivo || "—"}</td>
                  <td>{turnoBadge(item.turno)}</td>
                  <td className="td-muted">{item.professor || "—"}</td>
                  <td className="td-muted">{item.capacidade ?? "—"}</td>
                  <td>
                    {item.totalAlunos !== undefined ? (
                      <span className="badge badge-brand">{item.totalAlunos} alunos</span>
                    ) : "—"}
                  </td>
                  <td>
                    <span className={`badge ${item.ativo !== false ? "badge-success" : "badge-danger"}`}>
                      {item.ativo !== false ? "Ativa" : "Inativa"}
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
        title={editItem ? "Editar Turma" : "Nova Turma"}
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
            <label className="form-label required">Nome da Turma</label>
            <input className="form-input" value={form.nome} onChange={f("nome")} placeholder="Ex: 1º Ano A" />
          </div>
          <div className="form-field">
            <label className="form-label required">Curso</label>
            <select className="form-select" value={form.cursoId} onChange={f("cursoId")}>
              <option value="">Selecione o curso</option>
              {cursos.map((c) => <option key={c.id} value={c.id}>{c.nome}</option>)}
            </select>
          </div>
          <div className="form-grid-2">
            <div className="form-field">
              <label className="form-label required">Ano Letivo</label>
              <select className="form-select" value={form.anoLetivo} onChange={f("anoLetivo")}>
                {years.map((y) => <option key={y} value={y}>{y}</option>)}
              </select>
            </div>
            <div className="form-field">
              <label className="form-label">Turno</label>
              <select className="form-select" value={form.turno} onChange={f("turno")}>
                {TURNOS.map((t) => <option key={t} value={t}>{t}</option>)}
              </select>
            </div>
          </div>
          <div className="form-grid-2">
            <div className="form-field">
              <label className="form-label">Professor Responsável</label>
              <input className="form-input" value={form.professor} onChange={f("professor")} placeholder="Nome do professor" />
            </div>
            <div className="form-field">
              <label className="form-label">Capacidade (alunos)</label>
              <input className="form-input" type="number" min="0" value={form.capacidade} onChange={f("capacidade")} placeholder="40" />
            </div>
          </div>
          <label className="form-checkbox">
            <input type="checkbox" checked={form.ativo} onChange={f("ativo")} />
            <span>Turma ativa</span>
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
        <p style={{ color: "var(--color-text)" }}>Tem certeza que deseja excluir esta turma?</p>
      </Modal>
    </div>
  );
}
