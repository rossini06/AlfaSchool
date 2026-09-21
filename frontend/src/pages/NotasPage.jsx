import { useState, useEffect } from "react";
import { api } from "../services/api";
import { Icon } from "../components/Icon";
import { Feedback } from "../components/access/Feedback";

export function NotasPage() {
  const [turmas, setTurmas] = useState([]);
  const [disciplinas, setDisciplinas] = useState([]);
  const [turmaId, setTurmaId] = useState("");
  const [disciplinaId, setDisciplinaId] = useState("");
  const [avaliacoes, setAvaliacoes] = useState([]);
  const [avaliacaoId, setAvaliacaoId] = useState("");
  const [alunos, setAlunos] = useState([]);
  const [notas, setNotas] = useState({});
  const [obs, setObs] = useState({});
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [feedback, setFeedback] = useState(null);
  const [success, setSuccess] = useState("");
  const [newAval, setNewAval] = useState({ nome: "", tipo: "prova", peso: "1", dataAvaliacao: "" });
  const [showNewAval, setShowNewAval] = useState(false);

  useEffect(() => {
    api.get("/turmas?size=100").then(d => setTurmas(d?.content || d || [])).catch(() => {});
    api.get("/disciplinas/ativas").then(d => setDisciplinas(Array.isArray(d) ? d : d?.content || [])).catch(() => {});
  }, []);

  useEffect(() => {
    if (!turmaId || !disciplinaId) { setAvaliacoes([]); setAvaliacaoId(""); return; }
    api.get(`/avaliacoes?turmaId=${turmaId}&disciplinaId=${disciplinaId}`)
      .then(d => { const list = d?.content || d || []; setAvaliacoes(list); if (list.length > 0) setAvaliacaoId(list[0].id); })
      .catch(() => {});
  }, [turmaId, disciplinaId]);

  useEffect(() => {
    if (!turmaId || !avaliacaoId) { setAlunos([]); return; }
    setLoading(true);
    Promise.all([
      api.get(`/matriculas?turmaId=${turmaId}`),
      api.get(`/notas/avaliacao/${avaliacaoId}`)
    ]).then(async ([matriculas, notasList]) => {
      const alunoIds = (matriculas?.content || matriculas || []).map(m => m.alunoId);
      const alunoDetails = await Promise.all(alunoIds.map(id => api.get(`/alunos/${id}`).catch(() => null)));
      setAlunos(alunoDetails.filter(Boolean));
      const notasMap = {}, obsMap = {};
      (notasList || []).forEach(n => { notasMap[n.alunoId] = n.nota ?? ""; obsMap[n.alunoId] = n.obs || ""; });
      setNotas(notasMap); setObs(obsMap);
    }).catch(err => setError(err.message)).finally(() => setLoading(false));
  }, [turmaId, avaliacaoId]);

  const salvar = async () => {
    if (!avaliacaoId) { setFeedback({ tipo: "alerta", mensagem: "Selecione a avaliação antes de lançar as notas." }); return; }
    setSaving(true); setSuccess(""); setError("");
    try {
      await Promise.all(alunos.map(a =>
        api.post("/notas", { alunoId: a.id, avaliacaoId, nota: notas[a.id] !== "" ? Number(notas[a.id]) : null, obs: obs[a.id] || null })
          .catch(() => {})
      ));
      setSuccess("Notas salvas com sucesso!");
      setTimeout(() => setSuccess(""), 3000);
    } catch (err) { setError(err.message); }
    finally { setSaving(false); }
  };

  const criarAvaliacao = async () => {
    if (!newAval.nome.trim()) { setFeedback({ tipo: "alerta", mensagem: "Informe o nome." }); return; }
    try {
      await api.post("/avaliacoes", { turmaId, disciplinaId, ...newAval, peso: Number(newAval.peso) });
      setShowNewAval(false);
      setNewAval({ nome: "", tipo: "prova", peso: "1", dataAvaliacao: "" });
      const updated = await api.get(`/avaliacoes?turmaId=${turmaId}&disciplinaId=${disciplinaId}`);
      setAvaliacoes(updated?.content || updated || []);
    } catch (err) { setFeedback({ tipo: "erro", mensagem: err.message }); }
  };

  const notaColor = (v) => {
    if (v === "" || v === undefined || v === null) return "";
    const n = Number(v);
    if (n >= 7) return "var(--color-success)";
    if (n >= 5) return "var(--color-warning)";
    return "var(--color-danger)";
  };

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1 className="page-title">Lançamento de Notas</h1>
          <p className="page-subtitle">Registre as notas por avaliação e disciplina</p>
        </div>
      </div>

      <div className="card">
        <div className="card-body">
          <div className="filter-bar">
            <div className="form-field" style={{ flex: 1 }}>
              <label className="form-label">Turma</label>
              <select className="form-select" value={turmaId} onChange={e => { setTurmaId(e.target.value); setAvaliacaoId(""); }}>
                <option value="">Selecione</option>
                {turmas.map(t => <option key={t.id} value={t.id}>{t.nome}</option>)}
              </select>
            </div>
            <div className="form-field" style={{ flex: 1 }}>
              <label className="form-label">Disciplina</label>
              <select className="form-select" value={disciplinaId} onChange={e => setDisciplinaId(e.target.value)}>
                <option value="">Selecione</option>
                {disciplinas.map(d => <option key={d.id} value={d.id}>{d.nome}</option>)}
              </select>
            </div>
            <div className="form-field" style={{ flex: 1 }}>
              <label className="form-label">Avaliação</label>
              <select className="form-select" value={avaliacaoId} onChange={e => setAvaliacaoId(e.target.value)}>
                <option value="">Selecione</option>
                {avaliacoes.map(a => <option key={a.id} value={a.id}>{a.nome} (peso {a.peso})</option>)}
              </select>
            </div>
            {turmaId && disciplinaId && (
              <button className="btn btn-secondary" style={{ alignSelf: "flex-end" }} onClick={() => setShowNewAval(!showNewAval)}>
                <Icon name="Plus" size={14} /> Nova Avaliação
              </button>
            )}
          </div>

          {showNewAval && (
            <div style={{ marginTop: "1rem", padding: "1rem", background: "var(--color-bg-secondary)", borderRadius: "var(--radius-md)" }}>
              <div className="filter-bar">
                <div className="form-field" style={{ flex: 2 }}>
                  <label className="form-label required">Nome da Avaliação</label>
                  <input className="form-input" value={newAval.nome} onChange={e => setNewAval(p => ({ ...p, nome: e.target.value }))} placeholder="Ex: Prova 1" />
                </div>
                <div className="form-field">
                  <label className="form-label">Tipo</label>
                  <select className="form-select" value={newAval.tipo} onChange={e => setNewAval(p => ({ ...p, tipo: e.target.value }))}>
                    {["prova", "trabalho", "seminário", "projeto"].map(t => <option key={t} value={t}>{t}</option>)}
                  </select>
                </div>
                <div className="form-field">
                  <label className="form-label">Peso</label>
                  <input className="form-input" type="number" min="0.1" step="0.1" value={newAval.peso} onChange={e => setNewAval(p => ({ ...p, peso: e.target.value }))} />
                </div>
                <div className="form-field">
                  <label className="form-label">Data</label>
                  <input className="form-input" type="date" value={newAval.dataAvaliacao} onChange={e => setNewAval(p => ({ ...p, dataAvaliacao: e.target.value }))} />
                </div>
                <button className="btn btn-brand" style={{ alignSelf: "flex-end" }} onClick={criarAvaliacao}>Criar</button>
              </div>
            </div>
          )}
        </div>
      </div>

      {error && <div className="login-error"><Icon name="AlertCircle" size={14} /> {error}</div>}

      <Feedback tipo={feedback?.tipo} mensagem={feedback?.mensagem} onFechar={() => setFeedback(null)} />
      {success && <div className="login-error" style={{ background: "var(--color-success-bg)", color: "var(--color-success)", border: "1px solid var(--color-success)" }}>
        <Icon name="CheckCircle" size={14} /> {success}
      </div>}

      {!avaliacaoId ? (
        <div className="empty-state" style={{ padding: "3rem" }}>
          <div className="empty-state-icon"><Icon name="FileText" size={32} /></div>
          <h3>Selecione a avaliação</h3>
          <p>Escolha turma, disciplina e avaliação para lançar as notas.</p>
        </div>
      ) : loading ? (
        <div className="table-wrapper">{Array.from({ length: 5 }).map((_, i) => <div key={i} className="skeleton skeleton-text" style={{ height: "3rem", margin: "0.5rem 0" }} />)}</div>
      ) : (
        <>
          <div className="table-wrapper">
            <table className="data-table">
              <thead>
                <tr><th>#</th><th>Aluno</th><th style={{ width: "120px" }}>Nota (0-10)</th><th>Observação</th></tr>
              </thead>
              <tbody>
                {alunos.map((aluno, idx) => (
                  <tr key={aluno.id}>
                    <td className="td-muted">{idx + 1}</td>
                    <td><strong>{aluno.nome}</strong></td>
                    <td>
                      <input type="number" min="0" max="10" step="0.1"
                        className="form-input"
                        style={{ width: "100px", color: notaColor(notas[aluno.id]) }}
                        value={notas[aluno.id] ?? ""}
                        onChange={e => setNotas(p => ({ ...p, [aluno.id]: e.target.value }))} />
                    </td>
                    <td>
                      <input className="form-input" placeholder="Obs..." value={obs[aluno.id] || ""}
                        onChange={e => setObs(p => ({ ...p, [aluno.id]: e.target.value }))} />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div style={{ display: "flex", justifyContent: "flex-end", padding: "1rem 0" }}>
            <button className="btn btn-brand" onClick={salvar} disabled={saving}>
              <Icon name="Save" size={14} />
              {saving ? "Salvando..." : "Salvar Notas"}
            </button>
          </div>
        </>
      )}
    </div>
  );
}
