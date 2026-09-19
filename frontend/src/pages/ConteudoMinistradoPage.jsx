import { useState, useEffect, useCallback } from "react";
import { api } from "../services/api";
import { Icon } from "../components/Icon";
import { Modal } from "../components/Modal";

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

export function ConteudoMinistradoPage() {
  const [turmas, setTurmas] = useState([]);
  const [disciplinas, setDisciplinas] = useState([]);
  const [conteudos, setConteudos] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const [turmaId, setTurmaId] = useState("");
  const [disciplinaId, setDisciplinaId] = useState("");

  const [modalOpen, setModalOpen] = useState(false);
  const [form, setForm] = useState(initForm());
  const [editingId, setEditingId] = useState(null);
  const [saving, setSaving] = useState(false);

  const [deleteModal, setDeleteModal] = useState(false);
  const [deleteId, setDeleteId] = useState(null);

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

  useEffect(() => {
    loadTurmas();
    loadDisciplinas();
  }, []);

  useEffect(() => {
    if (turmaId && disciplinaId) {
      loadConteudos();
    } else {
      setConteudos([]);
    }
  }, [turmaId, disciplinaId, loadConteudos]);

  const openNew = () => {
    setEditingId(null);
    setForm({ ...initForm(), turmaId, disciplinaId });
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
    } catch (err) {
      setError(err?.message || "Erro ao salvar");
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

  const turmaSelecionada = turmas.find((t) => t.id === turmaId);
  const disciplinaSelecionada = disciplinas.find((d) => d.id === disciplinaId);

  return (
    <div className="page">
      {/* Cabeçalho */}
      <div className="page-header">
        <div>
          <h1 className="page-title" style={{ display: "flex", alignItems: "center", gap: 10 }}>
            <Icon name="FileEdit" size={22} />
            Conteúdo Ministrado
          </h1>
          <p className="page-subtitle">Registro de aulas e conteúdos por turma e disciplina</p>
        </div>
      </div>

      {/* Filtros */}
      <div className="card">
        <div className="card-header">
          <span className="card-title">Selecione Turma e Disciplina</span>
        </div>
        <div className="card-body">
          <div style={{ display: "flex", gap: 16, alignItems: "flex-end", flexWrap: "wrap" }}>
            <div className="form-field" style={{ flex: "1 1 220px" }}>
              <label className="form-label">Turma</label>
              <select
                className="form-select"
                value={turmaId}
                onChange={(e) => setTurmaId(e.target.value)}
              >
                <option value="">Selecione a turma...</option>
                {turmas.map((t) => (
                  <option key={t.id} value={t.id}>
                    {t.nome} ({t.anoLetivo})
                  </option>
                ))}
              </select>
            </div>

            <div className="form-field" style={{ flex: "1 1 220px" }}>
              <label className="form-label">Disciplina</label>
              <select
                className="form-select"
                value={disciplinaId}
                onChange={(e) => setDisciplinaId(e.target.value)}
                disabled={!turmaId}
              >
                <option value="">Selecione a disciplina...</option>
                {disciplinas.map((d) => (
                  <option key={d.id} value={d.id}>
                    {d.nome}
                  </option>
                ))}
              </select>
            </div>

            <button
              className="btn btn-brand"
              disabled={!turmaId || !disciplinaId}
              onClick={openNew}
              style={{ flexShrink: 0 }}
            >
              <Icon name="Plus" size={15} />
              Novo Conteúdo
            </button>
          </div>
        </div>
      </div>

      {error && (
        <div
          style={{
            background: "var(--color-danger-dim)",
            border: "1px solid var(--color-danger)",
            borderRadius: "var(--radius-sm)",
            padding: "10px 16px",
            color: "var(--color-danger)",
            fontSize: 13.5,
          }}
        >
          {error}
        </div>
      )}

      {/* Indicador de seleção ativa */}
      {turmaId && disciplinaId && (
        <div
          style={{
            display: "flex",
            alignItems: "center",
            gap: 8,
            padding: "8px 14px",
            background: "var(--color-brand-dim)",
            border: "1px solid var(--color-brand)",
            borderRadius: "var(--radius-sm)",
            fontSize: 13,
            color: "var(--color-brand)",
          }}
        >
          <Icon name="BookOpen" size={14} />
          <strong>{turmaSelecionada?.nome}</strong>
          <span style={{ opacity: 0.6 }}>·</span>
          <span>{disciplinaSelecionada?.nome}</span>
          <span style={{ marginLeft: "auto", opacity: 0.7 }}>{conteudos.length} registro(s)</span>
        </div>
      )}

      {/* Tabela */}
      <div className="table-wrapper">
        <table className="data-table">
          <thead>
            <tr>
              <th style={{ width: 110 }}>Data</th>
              <th>Descrição do Conteúdo</th>
              <th>Objetivos</th>
              <th>Recursos</th>
              <th style={{ width: 90 }}>Ações</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td colSpan={5}>
                  <div className="empty-state" style={{ padding: "40px 0" }}>
                    <Icon name="Loader" size={24} style={{ opacity: 0.4, animation: "spin 1s linear infinite" }} />
                    <p style={{ marginTop: 8 }}>Carregando...</p>
                  </div>
                </td>
              </tr>
            ) : conteudos.length === 0 ? (
              <tr>
                <td colSpan={5}>
                  <div className="empty-state">
                    <div className="empty-state-icon">
                      <Icon name={turmaId && disciplinaId ? "FileEdit" : "Filter"} size={26} />
                    </div>
                    <h3>
                      {turmaId && disciplinaId
                        ? "Nenhum conteúdo registrado"
                        : "Selecione turma e disciplina"}
                    </h3>
                    <p>
                      {turmaId && disciplinaId
                        ? "Clique em \"+ Novo Conteúdo\" para registrar a primeira aula."
                        : "Use os filtros acima para visualizar os registros."}
                    </p>
                  </div>
                </td>
              </tr>
            ) : (
              conteudos.map((c) => (
                <tr key={c.id}>
                  <td>
                    <span
                      style={{
                        display: "inline-flex",
                        alignItems: "center",
                        gap: 5,
                        fontWeight: 600,
                        fontSize: 13,
                        color: "var(--color-brand)",
                      }}
                    >
                      <Icon name="Calendar" size={13} />
                      {fmtDate(c.data)}
                    </span>
                  </td>
                  <td>
                    <div
                      style={{
                        maxWidth: 380,
                        whiteSpace: "pre-wrap",
                        lineHeight: 1.5,
                        fontSize: 13.5,
                      }}
                    >
                      {c.descricao}
                    </div>
                  </td>
                  <td>
                    <div
                      className="td-muted"
                      style={{ maxWidth: 260, whiteSpace: "pre-wrap", lineHeight: 1.4 }}
                    >
                      {c.objetivos || "—"}
                    </div>
                  </td>
                  <td>
                    {c.recursos ? (
                      <span className="badge badge-info">{c.recursos}</span>
                    ) : (
                      <span className="td-muted">—</span>
                    )}
                  </td>
                  <td>
                    <div className="td-actions">
                      <button
                        className="btn btn-ghost btn-icon btn-sm"
                        title="Editar"
                        onClick={() => openEdit(c)}
                      >
                        <Icon name="Edit2" size={14} />
                      </button>
                      <button
                        className="btn btn-ghost btn-icon btn-sm"
                        title="Excluir"
                        onClick={() => confirmDelete(c.id)}
                        style={{ color: "var(--color-danger)" }}
                      >
                        <Icon name="Trash2" size={14} />
                      </button>
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* Modal — Novo / Editar */}
      <Modal
        isOpen={modalOpen}
        onClose={() => setModalOpen(false)}
        title={editingId ? "Editar Conteúdo" : "Novo Conteúdo"}
        footer={
          <>
            <button className="btn btn-secondary" onClick={() => setModalOpen(false)}>
              Cancelar
            </button>
            <button
              type="submit"
              form="conteudo-form"
              className="btn btn-brand"
              disabled={saving}
            >
              {saving ? "Salvando..." : "Salvar"}
            </button>
          </>
        }
      >
        <form id="conteudo-form" onSubmit={save} style={{ display: "flex", flexDirection: "column", gap: 16 }}>
          {/* Data */}
          <div className="form-field">
            <label className="form-label required">Data da Aula</label>
            <input
              type="date"
              className="form-input"
              value={form.data}
              onChange={(e) => setForm({ ...form, data: e.target.value })}
              required
            />
          </div>

          {/* Descrição */}
          <div className="form-field">
            <label className="form-label required">Descrição do Conteúdo</label>
            <textarea
              className="form-textarea"
              rows={4}
              value={form.descricao}
              onChange={(e) => setForm({ ...form, descricao: e.target.value })}
              placeholder="Descreva o conteúdo ministrado em aula..."
              required
              style={{ minHeight: 100 }}
            />
          </div>

          {/* Objetivos */}
          <div className="form-field">
            <label className="form-label">Objetivos da Aula</label>
            <textarea
              className="form-textarea"
              rows={3}
              value={form.objetivos}
              onChange={(e) => setForm({ ...form, objetivos: e.target.value })}
              placeholder="Objetivos pedagógicos esperados..."
              style={{ minHeight: 72 }}
            />
          </div>

          {/* Recursos */}
          <div className="form-field">
            <label className="form-label">Recursos Utilizados</label>
            <input
              type="text"
              className="form-input"
              value={form.recursos}
              onChange={(e) => setForm({ ...form, recursos: e.target.value })}
              placeholder="Ex: Livro didático, projetor, vídeo, lousa..."
            />
            <span className="form-hint">Materiais e tecnologias usados na aula</span>
          </div>

          {error && (
            <div style={{ color: "var(--color-danger)", fontSize: 13, padding: "8px 12px", background: "var(--color-danger-dim)", borderRadius: "var(--radius-sm)" }}>
              {error}
            </div>
          )}
        </form>
      </Modal>

      {/* Modal — Confirmação de exclusão */}
      <Modal
        isOpen={deleteModal}
        onClose={() => setDeleteModal(false)}
        title="Confirmar Exclusão"
        size="sm"
        footer={
          <>
            <button className="btn btn-secondary" onClick={() => setDeleteModal(false)}>
              Cancelar
            </button>
            <button className="btn btn-danger" onClick={doDelete}>
              <Icon name="Trash2" size={14} />
              Excluir
            </button>
          </>
        }
      >
        <div style={{ display: "flex", flexDirection: "column", alignItems: "center", gap: 12, textAlign: "center", padding: "8px 0" }}>
          <div style={{ width: 52, height: 52, borderRadius: "50%", background: "var(--color-danger-dim)", display: "flex", alignItems: "center", justifyContent: "center" }}>
            <Icon name="AlertTriangle" size={24} style={{ color: "var(--color-danger)" }} />
          </div>
          <div>
            <p style={{ fontWeight: 600, marginBottom: 4 }}>Excluir este conteúdo?</p>
            <p style={{ color: "var(--color-text-2)", fontSize: 13 }}>Esta ação não pode ser desfeita.</p>
          </div>
        </div>
      </Modal>
    </div>
  );
}
