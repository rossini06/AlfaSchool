import { useState, useEffect, useCallback } from "react";
import { api } from "../services/api";
import { Icon } from "../components/Icon";
import { Modal } from "../components/Modal";

// ─── Constants ───────────────────────────────────────────────────────────────

function fmtDate(d) {
  if (!d) return "—";
  const [y, m, day] = d.split("-");
  return `${day}/${m}/${y}`;
}

function initForm() {
  return {
    turmaId: "",
    disciplinaId: "",
    data: new Date().toISOString().slice(0, 10),
    descricao: "",
    objetivos: "",
    recursos: "",
  };
}

// ─── Page Component ──────────────────────────────────────────────────────────

export function ConteudoMinistradoPage() {
  const [turmas, setTurmas] = useState([]);
  const [disciplinas, setDisciplinas] = useState([]);
  const [conteudos, setConteudos] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  // Filtros
  const [turmaId, setTurmaId] = useState("");
  const [disciplinaId, setDisciplinaId] = useState("");

  // Modal
  const [modalOpen, setModalOpen] = useState(false);
  const [form, setForm] = useState(initForm());
  const [editingId, setEditingId] = useState(null);
  const [saving, setSaving] = useState(false);

  // Delete
  const [deleteModal, setDeleteModal] = useState(false);
  const [deleteId, setDeleteId] = useState(null);

  // Carregar turmas e disciplinas
  useEffect(() => {
    loadTurmas();
    loadDisciplinas();
  }, []);

  // Carregar conteúdos quando filtros mudam
  useEffect(() => {
    if (turmaId && disciplinaId) {
      loadConteudos();
    } else {
      setConteudos([]);
    }
  }, [turmaId, disciplinaId, loadConteudos]);

  const loadTurmas = async () => {
    try {
      const data = await api.get("/turmas?size=100");
      setTurmas(data?.content || []);
    } catch {
      setError("Erro ao carregar turmas");
    }
  };

  const loadDisciplinas = async () => {
    try {
      const data = await api.get("/disciplinas?size=100");
      setDisciplinas(data?.content || []);
    } catch {
      setError("Erro ao carregar disciplinas");
    }
  };

  const loadConteudos = useCallback(async () => {
    if (!turmaId || !disciplinaId) return;
    setLoading(true);
    setError("");
    try {
      const data = await api.get(
        `/conteudos-ministrados/all?turmaId=${turmaId}&disciplinaId=${disciplinaId}`,
      );
      setConteudos(data || []);
    } catch {
      setError("Erro ao carregar conteúdos");
    } finally {
      setLoading(false);
    }
  }, [turmaId, disciplinaId]);

  const openNew = () => {
    setEditingId(null);
    setForm({
      ...initForm(),
      turmaId,
      disciplinaId,
    });
    setModalOpen(true);
  };

  const openEdit = (c) => {
    setEditingId(c.id);
    setForm({
      turmaId: c.turmaId || turmaId,
      disciplinaId: c.disciplinaId || disciplinaId,
      data: c.data || "",
      descricao: c.descricao || "",
      objetivos: c.objetivos || "",
      recursos: c.recursos || "",
    });
    setModalOpen(true);
  };

  const save = async (e) => {
    e.preventDefault();
    setSaving(true);
    setError("");

    const payload = {
      turmaId: form.turmaId || turmaId,
      disciplinaId: form.disciplinaId || disciplinaId,
      data: form.data,
      descricao: form.descricao,
      objetivos: form.objetivos || null,
      recursos: form.recursos || null,
    };

    try {
      if (editingId) {
        await api.put(`/conteudos-ministrados/${editingId}`, payload);
      } else {
        await api.post("/conteudos-ministrados", payload);
      }
      setModalOpen(false);
      loadConteudos();
    } catch (e) {
      setError(e?.message || "Erro ao salvar");
    } finally {
      setSaving(false);
    }
  };

  const confirmDelete = (id) => {
    setDeleteId(id);
    setDeleteModal(true);
  };

  const doDelete = async () => {
    try {
      await api.delete(`/conteudos-ministrados/${deleteId}`);
      setDeleteModal(false);
      loadConteudos();
    } catch {
      setError("Erro ao excluir");
    }
  };

  return (
    <>
      {/* Header */}
      <div className="page-header">
        <h1 className="page-title">
          <Icon name="FileEdit" size={24} />
          Conteúdo Ministrado
        </h1>
      </div>

      {/* Filtros */}
      <div className="card mb-4">
        <div className="card-header">Selecione Turma e Disciplina</div>
        <div className="card-body">
          <div className="row g-3">
            <div className="col-md-5">
              <label className="form-label">Turma</label>
              <select
                className="form-select"
                value={turmaId}
                onChange={(e) => setTurmaId(e.target.value)}
              >
                <option value="">Selecione...</option>
                {turmas.map((t) => (
                  <option key={t.id} value={t.id}>
                    {t.nome} ({t.anoLetivo})
                  </option>
                ))}
              </select>
            </div>
            <div className="col-md-5">
              <label className="form-label">Disciplina</label>
              <select
                className="form-select"
                value={disciplinaId}
                onChange={(e) => setDisciplinaId(e.target.value)}
              >
                <option value="">Selecione...</option>
                {disciplinas.map((d) => (
                  <option key={d.id} value={d.id}>
                    {d.nome}
                  </option>
                ))}
              </select>
            </div>
            <div className="col-md-2 d-flex align-items-end">
              <button
                className="btn btn-primary w-100"
                disabled={!turmaId || !disciplinaId}
                onClick={openNew}
              >
                <Icon name="Plus" size={16} /> Novo
              </button>
            </div>
          </div>
        </div>
      </div>

      {error && <div className="alert alert-danger">{error}</div>}

      {/* Lista */}
      <div className="card">
        <div className="table-responsive">
          <table className="table table-hover mb-0">
            <thead>
              <tr>
                <th style={{ width: 100 }}>Data</th>
                <th>Descrição</th>
                <th>Objetivos</th>
                <th style={{ width: 100 }}>Ações</th>
              </tr>
            </thead>
            <tbody>
              {loading ? (
                <tr>
                  <td colSpan={4} className="text-center text-muted py-5">
                    <div className="spinner-border spinner-border-sm me-2"></div>
                    Carregando...
                  </td>
                </tr>
              ) : conteudos.length === 0 ? (
                <tr>
                  <td colSpan={4} className="text-center text-muted py-5">
                    {turmaId && disciplinaId
                      ? "Nenhum conteúdo registrado"
                      : "Selecione turma e disciplina"}
                  </td>
                </tr>
              ) : (
                conteudos.map((c) => (
                  <tr key={c.id}>
                    <td>{fmtDate(c.data)}</td>
                    <td>
                      <div style={{ maxWidth: 400, whiteSpace: "pre-wrap" }}>
                        {c.descricao}
                      </div>
                    </td>
                    <td>
                      <div style={{ maxWidth: 300, whiteSpace: "pre-wrap" }}>
                        {c.objetivos || "—"}
                      </div>
                    </td>
                    <td>
                      <button
                        className="btn btn-sm btn-outline-primary me-1"
                        title="Editar"
                        onClick={() => openEdit(c)}
                      >
                        <Icon name="Edit" size={14} />
                      </button>
                      <button
                        className="btn btn-sm btn-outline-danger"
                        title="Excluir"
                        onClick={() => confirmDelete(c.id)}
                      >
                        <Icon name="Trash2" size={14} />
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {/* Modal de edição */}
      <Modal
        isOpen={modalOpen}
        onClose={() => setModalOpen(false)}
        title={editingId ? "Editar Conteúdo" : "Novo Conteúdo"}
        footer={
          <>
            <button
              type="button"
              className="btn btn-secondary"
              onClick={() => setModalOpen(false)}
            >
              Cancelar
            </button>
            <button
              type="submit"
              form="conteudo-form"
              className="btn btn-primary"
              disabled={saving}
            >
              {saving ? "Salvando..." : "Salvar"}
            </button>
          </>
        }
      >
        <form id="conteudo-form" onSubmit={save}>
          <div className="mb-3">
            <label className="form-label">Data *</label>
            <input
              type="date"
              className="form-control"
              value={form.data}
              onChange={(e) => setForm({ ...form, data: e.target.value })}
              required
            />
          </div>
          <div className="mb-3">
            <label className="form-label">Descrição *</label>
            <textarea
              className="form-control"
              rows={4}
              value={form.descricao}
              onChange={(e) =>
                setForm({ ...form, descricao: e.target.value })
              }
              placeholder="Descreva o conteúdo ministrado..."
              required
            />
          </div>
          <div className="mb-3">
            <label className="form-label">Objetivos</label>
            <textarea
              className="form-control"
              rows={2}
              value={form.objetivos}
              onChange={(e) =>
                setForm({ ...form, objetivos: e.target.value })
              }
              placeholder="Objetivos da aula..."
            />
          </div>
          <div className="mb-3">
            <label className="form-label">Recursos Utilizados</label>
            <input
              type="text"
              className="form-control"
              value={form.recursos}
              onChange={(e) => setForm({ ...form, recursos: e.target.value })}
              placeholder="Livro, slides, vídeo, etc."
            />
          </div>
        </form>
      </Modal>

      {/* Modal de exclusão */}
      <Modal
        isOpen={deleteModal}
        onClose={() => setDeleteModal(false)}
        title="Confirmar Exclusão"
        footer={
          <>
            <button
              className="btn btn-secondary"
              onClick={() => setDeleteModal(false)}
            >
              Cancelar
            </button>
            <button className="btn btn-danger" onClick={doDelete}>
              Excluir
            </button>
          </>
        }
      >
        <p>Deseja realmente excluir este conteúdo?</p>
      </Modal>
    </>
  );
}
