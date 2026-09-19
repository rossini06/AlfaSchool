import { useState, useEffect, useCallback } from "react";
import { api } from "../services/api";
import { Icon } from "../components/Icon";
import { Feedback } from "../components/access/Feedback";

const today = new Date().toISOString().split("T")[0];

export function FrequenciaPage() {
  const [turmas, setTurmas] = useState([]);
  const [disciplinas, setDisciplinas] = useState([]);
  const [turmaId, setTurmaId] = useState("");
  const [disciplinaId, setDisciplinaId] = useState("");
  const [data, setData] = useState(today);
  const [alunos, setAlunos] = useState([]);
  const [frequencias, setFrequencias] = useState({});
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [feedback, setFeedback] = useState(null);
  const [success, setSuccess] = useState("");

  useEffect(() => {
    api.get("/turmas?size=100").then(d => setTurmas(d?.content || d || [])).catch(() => {});
    api.get("/disciplinas/ativas").then(d => setDisciplinas(Array.isArray(d) ? d : d?.content || [])).catch(() => {});
  }, []);

  const loadAlunos = useCallback(async () => {
    if (!turmaId) return;
    setLoading(true); setError("");
    try {
      const [matriculas, freqs] = await Promise.all([
        api.get(`/matriculas?turmaId=${turmaId}`),
        disciplinaId && data
          ? api.get(`/frequencias?turmaId=${turmaId}&disciplinaId=${disciplinaId}&inicio=${data}&fim=${data}`)
          : Promise.resolve([])
      ]);
      const alunoIds = (matriculas?.content || matriculas || []).map(m => m.alunoId);
      const alunoDetails = await Promise.all(
        alunoIds.map(id => api.get(`/alunos/${id}`).catch(() => null))
      );
      setAlunos(alunoDetails.filter(Boolean));
      const freqMap = {};
      (freqs || []).forEach(f => { freqMap[f.alunoId] = f; });
      const initialPresenca = {};
      alunoIds.forEach(id => { initialPresenca[id] = freqMap[id] ? freqMap[id].presente : true; });
      setFrequencias(initialPresenca);
    } catch (err) { setError(err.message); }
    finally { setLoading(false); }
  }, [turmaId, disciplinaId, data]);

  useEffect(() => { if (turmaId) loadAlunos(); }, [turmaId, disciplinaId, data]);

  const togglePresenca = (alunoId) => {
    setFrequencias(prev => ({ ...prev, [alunoId]: !prev[alunoId] }));
  };

  const salvar = async () => {
    if (!turmaId || !disciplinaId || !data) { setFeedback({ tipo: "alerta", mensagem: "Selecione a turma, a disciplina e a data." }); return; }
    setSaving(true); setSuccess(""); setError("");
    try {
      await Promise.all(alunos.map(aluno =>
        api.post("/frequencias", {
          alunoId: aluno.id, turmaId, disciplinaId, data,
          presente: frequencias[aluno.id] !== false
        }).catch(() => {})
      ));
      setSuccess("Frequência salva com sucesso!");
      setTimeout(() => setSuccess(""), 3000);
    } catch (err) { setError(err.message); }
    finally { setSaving(false); }
  };

  const presentes = Object.values(frequencias).filter(Boolean).length;
  const ausentes = alunos.length - presentes;

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1 className="page-title">Diário de Frequência</h1>
          <p className="page-subtitle">Registre a presença dos alunos por turma e disciplina</p>
        </div>
      </div>

      <div className="card">
        <div className="card-body">
          <div className="filter-bar">
            <div className="form-field" style={{ flex: 1 }}>
              <label className="form-label">Turma</label>
              <select className="form-select" value={turmaId} onChange={e => setTurmaId(e.target.value)}>
                <option value="">Selecione uma turma</option>
                {turmas.map(t => <option key={t.id} value={t.id}>{t.nome} — {t.anoLetivo}</option>)}
              </select>
            </div>
            <div className="form-field" style={{ flex: 1 }}>
              <label className="form-label">Disciplina</label>
              <select className="form-select" value={disciplinaId} onChange={e => setDisciplinaId(e.target.value)}>
                <option value="">Selecione uma disciplina</option>
                {disciplinas.map(d => <option key={d.id} value={d.id}>{d.nome}</option>)}
              </select>
            </div>
            <div className="form-field">
              <label className="form-label">Data</label>
              <input className="form-input" type="date" value={data} onChange={e => setData(e.target.value)} />
            </div>
          </div>
        </div>
      </div>

      {alunos.length > 0 && (
        <div className="card" style={{ marginTop: "0.5rem" }}>
          <div className="card-body" style={{ display: "flex", gap: "2rem" }}>
            <span style={{ color: "var(--color-text-muted)" }}>
              <strong style={{ color: "var(--color-success)" }}>{presentes}</strong> presentes
            </span>
            <span style={{ color: "var(--color-text-muted)" }}>
              <strong style={{ color: "var(--color-danger)" }}>{ausentes}</strong> ausentes
            </span>
            <span style={{ color: "var(--color-text-muted)" }}>{alunos.length} total</span>
          </div>
        </div>
      )}

      {error && <div className="login-error"><Icon name="AlertCircle" size={14} /> {error}</div>}

      <Feedback tipo={feedback?.tipo} mensagem={feedback?.mensagem} onFechar={() => setFeedback(null)} />
      {success && <div className="login-error" style={{ background: "var(--color-success-bg)", color: "var(--color-success)", border: "1px solid var(--color-success)" }}>
        <Icon name="CheckCircle" size={14} /> {success}
      </div>}

      {loading ? (
        <div className="table-wrapper">
          {Array.from({ length: 5 }).map((_, i) => <div key={i} className="skeleton skeleton-text" style={{ margin: "0.5rem 0", height: "3rem" }} />)}
        </div>
      ) : !turmaId ? (
        <div className="empty-state" style={{ padding: "3rem" }}>
          <div className="empty-state-icon"><Icon name="CalendarCheck" size={32} /></div>
          <h3>Selecione uma turma</h3>
          <p>Escolha a turma, disciplina e data para registrar a frequência.</p>
        </div>
      ) : alunos.length === 0 ? (
        <div className="empty-state" style={{ padding: "3rem" }}>
          <div className="empty-state-icon"><Icon name="Users" size={32} /></div>
          <h3>Nenhum aluno matriculado</h3>
          <p>Esta turma não possui alunos matriculados.</p>
        </div>
      ) : (
        <>
          <div className="table-wrapper">
            <table className="data-table">
              <thead>
                <tr><th>#</th><th>Aluno</th><th>CPF</th><th style={{ textAlign: "center" }}>Presente</th></tr>
              </thead>
              <tbody>
                {alunos.map((aluno, idx) => (
                  <tr key={aluno.id} style={{ cursor: "pointer" }} onClick={() => togglePresenca(aluno.id)}>
                    <td className="td-muted">{idx + 1}</td>
                    <td><strong>{aluno.nome}</strong></td>
                    <td className="td-muted">{aluno.cpf || "—"}</td>
                    <td style={{ textAlign: "center" }}>
                      <span className={`badge ${frequencias[aluno.id] !== false ? "badge-success" : "badge-danger"}`}
                        style={{ minWidth: "5rem", cursor: "pointer" }}>
                        {frequencias[aluno.id] !== false ? "✓ Presente" : "✗ Ausente"}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div style={{ display: "flex", justifyContent: "flex-end", padding: "1rem 0", gap: "1rem" }}>
            <button className="btn btn-secondary" onClick={() => {
              const all = {};
              alunos.forEach(a => { all[a.id] = true; });
              setFrequencias(all);
            }}>Todos Presentes</button>
            <button className="btn btn-brand" onClick={salvar} disabled={saving}>
              <Icon name="Save" size={14} />
              {saving ? "Salvando..." : "Salvar Frequência"}
            </button>
          </div>
        </>
      )}
    </div>
  );
}
