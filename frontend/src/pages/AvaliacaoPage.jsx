import { useState, useEffect, useCallback, useMemo } from "react";
import { api } from "../services/api";
import { Icon } from "../components/Icon";
import { Feedback } from "../components/access/Feedback";
import { Modal } from "../components/Modal";
import { Pagination } from "../components/Pagination";
import { Avatar } from "../components/Avatar";

// ─── Constants ───────────────────────────────────────────────────────────────

const TIPOS = [
  { value: "prova-escrita",   label: "Prova Escrita" },
  { value: "prova-oral",      label: "Prova Oral" },
  { value: "trabalho-escrito", label: "Trabalho Escrito" },
  { value: "seminario",       label: "Seminário" },
  { value: "projeto",         label: "Projeto" },
  { value: "atividade",       label: "Atividade" },
  { value: "simulado",        label: "Simulado" },
  { value: "recuperacao",     label: "Recuperação" },
  { value: "auto-avaliacao",  label: "Auto-avaliação" },
  { value: "outro",           label: "Outro" },
];

const PERIODOS = [
  { value: "1bim", label: "1º Bimestre" },
  { value: "2bim", label: "2º Bimestre" },
  { value: "3bim", label: "3º Bimestre" },
  { value: "4bim", label: "4º Bimestre" },
  { value: "1sem", label: "1º Semestre" },
  { value: "2sem", label: "2º Semestre" },
  { value: "anual", label: "Anual" },
];

const STATUS_OPTS = [
  { value: "rascunho",  label: "Rascunho" },
  { value: "publicada", label: "Publicada" },
  { value: "aplicada",  label: "Aplicada" },
  { value: "corrigida", label: "Corrigida" },
];

// ─── Helpers ─────────────────────────────────────────────────────────────────

function tipoBadgeClass(tipo) {
  if (!tipo) return "badge-secondary";
  const t = tipo.toLowerCase();
  if (t === "prova-escrita" || t === "prova-oral") return "badge-danger";
  if (t === "trabalho-escrito" || t === "projeto") return "badge-info";
  if (t === "seminario") return "badge-warning";
  if (t === "recuperacao") return "badge-danger";
  return "badge-secondary";
}

function statusBadgeClass(status) {
  switch (status) {
    case "publicada": return "badge-info";
    case "aplicada":  return "badge-warning";
    case "corrigida": return "badge-success";
    default:          return "badge-secondary";
  }
}

function tipoLabel(value) {
  return TIPOS.find(t => t.value === value)?.label || value || "—";
}

function periodoLabel(value) {
  return PERIODOS.find(p => p.value === value)?.label || value || "—";
}

function statusLabel(value) {
  return STATUS_OPTS.find(s => s.value === value)?.label || value || "—";
}

function fmtDate(d) {
  if (!d) return "—";
  const [y, m, day] = d.split("-");
  return `${day}/${m}/${y}`;
}

function initForm() {
  return {
    nome: "",
    turmaId: "",
    disciplinaId: "",
    tipo: "prova-escrita",
    status: "rascunho",
    peso: "1",
    notaMaxima: "10",
    notaMinima: "5",
    periodo: "",
    dataAvaliacao: "",
    dataEntrega: "",
    descricao: "",
    criterios: "",
  };
}

// ─── KPI Card ────────────────────────────────────────────────────────────────

function KpiCard({ icon, label, value, color, subtitle }) {
  return (
    <div className="card" style={{ flex: 1, minWidth: 0 }}>
      <div className="card-body" style={{ display: "flex", alignItems: "center", gap: "1rem" }}>
        <div style={{
          width: 44, height: 44, borderRadius: "var(--radius-md)",
          background: `${color}18`, display: "flex", alignItems: "center",
          justifyContent: "center", flexShrink: 0, color
        }}>
          <Icon name={icon} size={20} />
        </div>
        <div style={{ minWidth: 0 }}>
          <div style={{ fontSize: "0.72rem", color: "var(--color-text-muted)", textTransform: "uppercase", letterSpacing: "0.05em", fontWeight: 600 }}>{label}</div>
          <div style={{ fontSize: "1.5rem", fontWeight: 700, color: "var(--color-text)", lineHeight: 1.2 }}>{value}</div>
          {subtitle && <div style={{ fontSize: "0.72rem", color: "var(--color-text-muted)" }}>{subtitle}</div>}
        </div>
      </div>
    </div>
  );
}

// ─── Lançar Notas Modal ───────────────────────────────────────────────────────

function LancarNotasModal({ avaliacao, turmaLabel, disciplinaLabel, onClose, onSaved }) {
  const [alunos, setAlunos] = useState([]);
  const [notas, setNotas]   = useState({});
  const [obsMap, setObsMap] = useState({});
  const [loading, setLoading] = useState(true);
  const [saving, setSaving]   = useState(false);
  const [error, setError]     = useState("");

  const notaMax = parseFloat(avaliacao?.notaMaxima ?? 10);
  const notaMin = parseFloat(avaliacao?.notaMinima ?? 5);

  useEffect(() => {
    if (!avaliacao) return;
    setLoading(true); setError("");
    Promise.all([
      api.get(`/matriculas?turmaId=${avaliacao.turmaId}`),
      api.get(`/notas/avaliacao/${avaliacao.id}`),
    ]).then(async ([matriculas, notasList]) => {
      const ids = (matriculas?.content || matriculas || []).map(m => m.alunoId);
      const details = await Promise.all(ids.map(id => api.get(`/alunos/${id}`).catch(() => null)));
      const alunosFetched = details.filter(Boolean);
      setAlunos(alunosFetched);

      const nm = {}, om = {};
      (notasList || []).forEach(n => {
        nm[n.alunoId] = n.nota !== null && n.nota !== undefined ? String(n.nota) : "";
        om[n.alunoId] = n.obs || "";
      });
      setNotas(nm);
      setObsMap(om);
    }).catch(e => setError(e.message))
      .finally(() => setLoading(false));
  }, [avaliacao]);

  // Live stats — recalculated on every notas change
  const stats = useMemo(() => {
    const vals = alunos
      .map(a => notas[a.id])
      .filter(v => v !== "" && v !== undefined && v !== null)
      .map(Number)
      .filter(n => !isNaN(n));

    if (vals.length === 0) return { media: null, maior: null, menor: null, aprovacao: null, semNota: alunos.length };

    const media = vals.reduce((s, v) => s + v, 0) / vals.length;
    const aprovados = vals.filter(v => v >= notaMin).length;
    return {
      media:     media.toFixed(1),
      maior:     Math.max(...vals).toFixed(1),
      menor:     Math.min(...vals).toFixed(1),
      aprovacao: vals.length > 0 ? ((aprovados / vals.length) * 100).toFixed(0) : 0,
      semNota:   alunos.length - vals.length,
    };
  }, [notas, alunos, notaMin]);

  const setNota = (alunoId, value) => setNotas(p => ({ ...p, [alunoId]: value }));
  const setObs  = (alunoId, value) => setObsMap(p => ({ ...p, [alunoId]: value }));

  const marcarAusentes = () => {
    const all = {};
    alunos.forEach(a => { all[a.id] = ""; });
    setNotas(all);
  };

  const salvar = async () => {
    setSaving(true); setError("");
    try {
      await Promise.all(alunos.map(a =>
        api.post("/notas", {
          alunoId: a.id,
          avaliacaoId: avaliacao.id,
          nota: notas[a.id] !== "" && notas[a.id] !== undefined ? Number(notas[a.id]) : null,
          obs: obsMap[a.id] || null,
        }).catch(() => {})
      ));
      onSaved?.();
      onClose();
    } catch (e) { setError(e.message); }
    finally { setSaving(false); }
  };

  const notaStyle = (alunoId) => {
    const v = notas[alunoId];
    if (v === "" || v === undefined || v === null) return {};
    const n = Number(v);
    if (isNaN(n)) return {};
    return { color: n >= notaMin ? "var(--color-success)" : "var(--color-danger)", fontWeight: 600 };
  };

  const situacaoBadge = (alunoId) => {
    const v = notas[alunoId];
    if (v === "" || v === undefined || v === null) return <span className="badge badge-secondary">—</span>;
    const n = Number(v);
    if (isNaN(n)) return null;
    return n >= notaMin
      ? <span className="badge badge-success">Aprovado</span>
      : <span className="badge badge-danger">Reprovado</span>;
  };

  return (
    <Modal
      isOpen={!!avaliacao}
      onClose={onClose}
      title={`Lançar Notas — ${avaliacao?.nome || ""}`}
      size="xl"
      footer={
        <div style={{ display: "flex", justifyContent: "space-between", width: "100%", alignItems: "center" }}>
          <button
            className="btn btn-secondary"
            onClick={marcarAusentes}
            title="Define nota em branco para todos os alunos (ausentes)"
          >
            <Icon name="XSquare" size={14} /> Marcar todos como ausentes
          </button>
          <div style={{ display: "flex", gap: "0.5rem" }}>
            <button className="btn btn-ghost" onClick={onClose}>Cancelar</button>
            <button className="btn btn-brand" onClick={salvar} disabled={saving}>
              <Icon name="Save" size={14} />
              {saving ? "Salvando..." : "Salvar Notas"}
            </button>
          </div>
        </div>
      }
    >
      {/* Header info */}
      <div style={{ display: "flex", gap: "1rem", marginBottom: "1rem", flexWrap: "wrap" }}>
        <div style={{ display: "flex", gap: "0.5rem", alignItems: "center", flexWrap: "wrap" }}>
          <span className={`badge ${tipoBadgeClass(avaliacao?.tipo)}`}>{tipoLabel(avaliacao?.tipo)}</span>
          {avaliacao?.periodo && <span className="badge badge-secondary">{periodoLabel(avaliacao.periodo)}</span>}
          {avaliacao?.dataAvaliacao && (
            <span style={{ color: "var(--color-text-muted)", fontSize: "0.85rem" }}>
              <Icon name="Calendar" size={12} style={{ marginRight: 4 }} />
              {fmtDate(avaliacao.dataAvaliacao)}
            </span>
          )}
          <span style={{ color: "var(--color-text-muted)", fontSize: "0.85rem" }}>
            <Icon name="School" size={12} style={{ marginRight: 4 }} />
            {disciplinaLabel} · {turmaLabel}
          </span>
          <span style={{ color: "var(--color-text-muted)", fontSize: "0.85rem" }}>
            Nota: 0 – {notaMax} | Mín: {notaMin}
          </span>
        </div>
      </div>

      {/* Live stats bar */}
      {!loading && alunos.length > 0 && (
        <div style={{
          display: "flex", gap: "1rem", padding: "0.75rem 1rem",
          background: "var(--color-bg-secondary)", borderRadius: "var(--radius-md)",
          marginBottom: "1rem", flexWrap: "wrap"
        }}>
          {[
            { label: "Média Atual", value: stats.media ?? "—", color: "var(--color-brand)" },
            { label: "Maior Nota",  value: stats.maior ?? "—", color: "var(--color-success)" },
            { label: "Menor Nota",  value: stats.menor ?? "—", color: "var(--color-danger)" },
            { label: "% Aprovação", value: stats.aprovacao !== null ? `${stats.aprovacao}%` : "—", color: "var(--color-success)" },
            { label: "Sem nota",    value: stats.semNota, color: "var(--color-text-muted)" },
          ].map(s => (
            <div key={s.label} style={{ textAlign: "center", flex: 1, minWidth: 60 }}>
              <div style={{ fontSize: "1.1rem", fontWeight: 700, color: s.color }}>{s.value}</div>
              <div style={{ fontSize: "0.7rem", color: "var(--color-text-muted)", textTransform: "uppercase", letterSpacing: "0.04em" }}>{s.label}</div>
            </div>
          ))}
        </div>
      )}

      {error && (
        <div className="login-error" style={{ marginBottom: "1rem" }}>
          <Icon name="AlertCircle" size={14} /> {error}
        </div>
      )}

      {loading ? (
        <div>
          {Array.from({ length: 6 }).map((_, i) => (
            <div key={i} className="skeleton skeleton-text" style={{ height: "2.5rem", margin: "0.4rem 0" }} />
          ))}
        </div>
      ) : alunos.length === 0 ? (
        <div className="empty-state" style={{ padding: "2rem" }}>
          <div className="empty-state-icon"><Icon name="Users" size={28} /></div>
          <h3>Nenhum aluno matriculado</h3>
          <p>Esta turma não possui alunos matriculados ativos.</p>
        </div>
      ) : (
        <div style={{ overflowX: "auto" }}>
          <table className="data-table">
            <thead>
              <tr>
                <th style={{ width: 40 }}>#</th>
                <th>Aluno</th>
                <th style={{ width: 140 }}>Nota (0–{notaMax})</th>
                <th>Observação</th>
                <th style={{ width: 110, textAlign: "center" }}>Situação</th>
              </tr>
            </thead>
            <tbody>
              {alunos.map((aluno, idx) => (
                <tr key={aluno.id}>
                  <td className="td-muted">{idx + 1}</td>
                  <td>
                    <div style={{ display: "flex", alignItems: "center", gap: "0.6rem" }}>
                      <Avatar nome={aluno.nome} size={30} />
                      <strong style={{ fontSize: "0.875rem" }}>{aluno.nome}</strong>
                    </div>
                  </td>
                  <td>
                    <input
                      type="number"
                      min="0"
                      max={notaMax}
                      step="0.1"
                      className="form-input"
                      style={{ width: "110px", ...notaStyle(aluno.id) }}
                      value={notas[aluno.id] ?? ""}
                      placeholder="—"
                      onChange={e => setNota(aluno.id, e.target.value)}
                    />
                  </td>
                  <td>
                    <input
                      className="form-input"
                      placeholder="Observação..."
                      value={obsMap[aluno.id] || ""}
                      onChange={e => setObs(aluno.id, e.target.value)}
                    />
                  </td>
                  <td style={{ textAlign: "center" }}>{situacaoBadge(aluno.id)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </Modal>
  );
}

// ─── Create/Edit Modal ────────────────────────────────────────────────────────

function AvaliacaoFormModal({ avaliacao, turmas, disciplinas, onClose, onSaved }) {
  const isEdit = !!avaliacao?.id;
  const [tab, setTab]     = useState("identificacao");
  const [form, setForm]   = useState(initForm);
  const [saving, setSaving] = useState(false);
  const [error, setError]   = useState("");

  useEffect(() => {
    if (avaliacao) {
      setForm({
        nome:          avaliacao.nome          || "",
        turmaId:       avaliacao.turmaId       || "",
        disciplinaId:  avaliacao.disciplinaId  || "",
        tipo:          avaliacao.tipo          || "prova-escrita",
        status:        avaliacao.status        || "rascunho",
        peso:          String(avaliacao.peso   ?? "1"),
        notaMaxima:    String(avaliacao.notaMaxima ?? "10"),
        notaMinima:    String(avaliacao.notaMinima ?? "5"),
        periodo:       avaliacao.periodo       || "",
        dataAvaliacao: avaliacao.dataAvaliacao || "",
        dataEntrega:   avaliacao.dataEntrega   || "",
        descricao:     avaliacao.descricao     || "",
        criterios:     avaliacao.criterios     || "",
      });
    } else {
      setForm(initForm());
    }
    setTab("identificacao");
    setError("");
  }, [avaliacao]);

  const set = (k, v) => setForm(p => ({ ...p, [k]: v }));

  const salvar = async () => {
    if (!form.nome.trim()) { setError("Nome é obrigatório."); setTab("identificacao"); return; }
    if (!form.turmaId)     { setError("Turma é obrigatória."); setTab("identificacao"); return; }
    if (!form.disciplinaId){ setError("Disciplina é obrigatória."); setTab("identificacao"); return; }

    setSaving(true); setError("");
    const payload = {
      nome:         form.nome.trim(),
      turmaId:      form.turmaId,
      disciplinaId: form.disciplinaId,
      tipo:         form.tipo || null,
      status:       form.status || "rascunho",
      peso:         form.peso ? Number(form.peso) : 1,
      notaMaxima:   form.notaMaxima ? Number(form.notaMaxima) : 10,
      notaMinima:   form.notaMinima ? Number(form.notaMinima) : 5,
      periodo:      form.periodo || null,
      dataAvaliacao: form.dataAvaliacao || null,
      dataEntrega:   form.dataEntrega   || null,
      descricao:     form.descricao     || null,
      criterios:     form.criterios     || null,
    };
    try {
      if (isEdit) {
        await api.put(`/avaliacoes/${avaliacao.id}`, payload);
      } else {
        await api.post("/avaliacoes", payload);
      }
      onSaved?.();
      onClose();
    } catch (e) { setError(e.message); }
    finally { setSaving(false); }
  };

  const tabs = [
    { key: "identificacao", label: "Identificação" },
    { key: "configuracao",  label: "Configuração" },
    { key: "conteudo",      label: "Conteúdo" },
  ];

  const inputRow = (children) => (
    <div style={{ display: "flex", gap: "1rem", flexWrap: "wrap", marginBottom: "1rem" }}>{children}</div>
  );
  const field = (label, required, children, flex = 1) => (
    <div className="form-field" style={{ flex }}>
      <label className={`form-label${required ? " required" : ""}`}>{label}</label>
      {children}
    </div>
  );

  return (
    <Modal
      isOpen={true}
      onClose={onClose}
      title={isEdit ? `Editar Avaliação` : "Nova Avaliação"}
      size="lg"
      footer={
        <div style={{ display: "flex", justifyContent: "flex-end", gap: "0.5rem" }}>
          <button className="btn btn-ghost" onClick={onClose}>Cancelar</button>
          <button className="btn btn-brand" onClick={salvar} disabled={saving}>
            <Icon name="Save" size={14} />
            {saving ? "Salvando..." : isEdit ? "Atualizar" : "Criar Avaliação"}
          </button>
        </div>
      }
    >
      {/* Tab nav */}
      <div style={{ display: "flex", gap: 0, borderBottom: "2px solid var(--color-border)", marginBottom: "1.25rem" }}>
        {tabs.map(t => (
          <button
            key={t.key}
            onClick={() => setTab(t.key)}
            style={{
              border: "none", background: "none", cursor: "pointer",
              padding: "0.5rem 1rem", fontSize: "0.875rem", fontWeight: 600,
              color: tab === t.key ? "var(--color-brand)" : "var(--color-text-muted)",
              borderBottom: tab === t.key ? "2px solid var(--color-brand)" : "2px solid transparent",
              marginBottom: "-2px", transition: "all 0.15s",
            }}
          >
            {t.label}
          </button>
        ))}
      </div>

      {error && (
        <div className="login-error" style={{ marginBottom: "1rem" }}>
          <Icon name="AlertCircle" size={14} /> {error}
        </div>
      )}

      {/* ── Tab: Identificação ── */}
      {tab === "identificacao" && (
        <div>
          {inputRow(
            field("Nome da Avaliação", true, (
              <input
                className="form-input"
                value={form.nome}
                onChange={e => set("nome", e.target.value)}
                placeholder="Ex: Prova Bimestral 1"
              />
            ), 2)
          )}
          {inputRow(
            <>
              {field("Turma", true, (
                <select className="form-select" value={form.turmaId} onChange={e => set("turmaId", e.target.value)}>
                  <option value="">Selecione a turma</option>
                  {turmas.map(t => <option key={t.id} value={t.id}>{t.nome}{t.anoLetivo ? ` — ${t.anoLetivo}` : ""}</option>)}
                </select>
              ))}
              {field("Disciplina", true, (
                <select className="form-select" value={form.disciplinaId} onChange={e => set("disciplinaId", e.target.value)}>
                  <option value="">Selecione a disciplina</option>
                  {disciplinas.map(d => <option key={d.id} value={d.id}>{d.nome}</option>)}
                </select>
              ))}
            </>
          )}
          {inputRow(
            <>
              {field("Tipo de Avaliação", false, (
                <select className="form-select" value={form.tipo} onChange={e => set("tipo", e.target.value)}>
                  {TIPOS.map(t => <option key={t.value} value={t.value}>{t.label}</option>)}
                </select>
              ))}
              {field("Status", false, (
                <select className="form-select" value={form.status} onChange={e => set("status", e.target.value)}>
                  {STATUS_OPTS.map(s => <option key={s.value} value={s.value}>{s.label}</option>)}
                </select>
              ))}
            </>
          )}
        </div>
      )}

      {/* ── Tab: Configuração ── */}
      {tab === "configuracao" && (
        <div>
          {inputRow(
            <>
              {field("Peso", false, (
                <input
                  className="form-input"
                  type="number" min="0.1" step="0.1"
                  value={form.peso}
                  onChange={e => set("peso", e.target.value)}
                  placeholder="1"
                />
              ), 0.7)}
              {field("Nota Máxima", false, (
                <input
                  className="form-input"
                  type="number" min="0" step="0.5"
                  value={form.notaMaxima}
                  onChange={e => set("notaMaxima", e.target.value)}
                  placeholder="10"
                />
              ), 0.7)}
              {field("Nota Mínima (Aprovação)", false, (
                <input
                  className="form-input"
                  type="number" min="0" step="0.5"
                  value={form.notaMinima}
                  onChange={e => set("notaMinima", e.target.value)}
                  placeholder="5"
                />
              ), 1)}
            </>
          )}
          {inputRow(
            field("Período / Etapa", false, (
              <select className="form-select" value={form.periodo} onChange={e => set("periodo", e.target.value)}>
                <option value="">Selecione o período</option>
                {PERIODOS.map(p => <option key={p.value} value={p.value}>{p.label}</option>)}
              </select>
            ))
          )}
          {inputRow(
            <>
              {field("Data da Avaliação", false, (
                <input
                  className="form-input"
                  type="date"
                  value={form.dataAvaliacao}
                  onChange={e => set("dataAvaliacao", e.target.value)}
                />
              ))}
              {field("Data de Entrega (trabalhos)", false, (
                <input
                  className="form-input"
                  type="date"
                  value={form.dataEntrega}
                  onChange={e => set("dataEntrega", e.target.value)}
                />
              ))}
            </>
          )}
          <div style={{
            padding: "0.75rem 1rem", background: "var(--color-bg-secondary)",
            borderRadius: "var(--radius-md)", fontSize: "0.8rem", color: "var(--color-text-muted)"
          }}>
            <Icon name="Info" size={13} style={{ marginRight: 4 }} />
            A nota mínima define o limiar de aprovação exibido na situação do aluno e nas estatísticas.
            O peso é utilizado para cálculo da média ponderada quando aplicável.
          </div>
        </div>
      )}

      {/* ── Tab: Conteúdo ── */}
      {tab === "conteudo" && (
        <div>
          <div className="form-field" style={{ marginBottom: "1rem" }}>
            <label className="form-label">Descrição / Enunciado da Avaliação</label>
            <textarea
              className="form-input"
              style={{ minHeight: "120px", resize: "vertical", fontFamily: "inherit" }}
              placeholder="Descreva o conteúdo, escopo e instruções da avaliação..."
              value={form.descricao}
              onChange={e => set("descricao", e.target.value)}
            />
          </div>
          <div className="form-field">
            <label className="form-label">Critérios de Avaliação e Rubrica</label>
            <textarea
              className="form-input"
              style={{ minHeight: "140px", resize: "vertical", fontFamily: "inherit" }}
              placeholder="Descreva os critérios, competências avaliadas e rubricas de pontuação..."
              value={form.criterios}
              onChange={e => set("criterios", e.target.value)}
            />
          </div>
          <div style={{
            padding: "0.75rem 1rem", background: "var(--color-bg-secondary)",
            borderRadius: "var(--radius-md)", fontSize: "0.8rem", color: "var(--color-text-muted)",
            marginTop: "0.5rem"
          }}>
            <Icon name="Info" size={13} style={{ marginRight: 4 }} />
            Os critérios de avaliação ajudam a garantir consistência na correção e
            transparência para os alunos e responsáveis.
          </div>
        </div>
      )}
    </Modal>
  );
}

// ─── Delete Confirm Modal ─────────────────────────────────────────────────────

function DeleteModal({ avaliacao, onClose, onDeleted }) {
  const [deleting, setDeleting] = useState(false);
  const [erro, setErro] = useState("");
  const confirm = async () => {
    setDeleting(true); setErro("");
    try {
      await api.delete(`/avaliacoes/${avaliacao.id}`);
      onDeleted?.();
      onClose();
    } catch (e) { setErro(e.message || "Não foi possível excluir a avaliação."); }
    finally { setDeleting(false); }
  };
  return (
    <Modal
      isOpen={!!avaliacao}
      onClose={onClose}
      title="Excluir Avaliação"
      footer={
        <div style={{ display: "flex", justifyContent: "flex-end", gap: "0.5rem" }}>
          <button className="btn btn-ghost" onClick={onClose}>Cancelar</button>
          <button className="btn btn-danger" onClick={confirm} disabled={deleting}>
            <Icon name="Trash" size={14} />
            {deleting ? "Excluindo..." : "Confirmar Exclusão"}
          </button>
        </div>
      }
    >
      <div style={{ padding: "0.5rem 0" }}>
        <p style={{ marginBottom: "0.75rem" }}>
          Tem certeza que deseja excluir a avaliação{" "}
          <strong>"{avaliacao?.nome}"</strong>?
        </p>
        <p style={{ color: "var(--color-danger)", fontSize: "0.85rem" }}>
          <Icon name="AlertCircle" size={13} style={{ marginRight: 4 }} />
          Esta ação é irreversível. Todas as notas lançadas para esta avaliação
          também serão excluídas logicamente.
        </p>
        {erro && (
          <p className="login-error" style={{ marginTop: "0.75rem" }}>
            <Icon name="AlertCircle" size={13} style={{ marginRight: 4 }} />{erro}
          </p>
        )}
      </div>
    </Modal>
  );
}

// ─── Main Page ────────────────────────────────────────────────────────────────

export function AvaliacaoPage() {
  // Filter state
  const [filterTurmaId,      setFilterTurmaId]      = useState("");
  const [filterDisciplinaId, setFilterDisciplinaId] = useState("");
  const [filterPeriodo,      setFilterPeriodo]       = useState("");
  const [filterTipo,         setFilterTipo]          = useState("");
  const [filterStatus,       setFilterStatus]        = useState("");

  // Data
  const [turmas,      setTurmas]      = useState([]);
  const [disciplinas, setDisciplinas] = useState([]);
  const [avaliacoes,  setAvaliacoes]  = useState([]);
  const [notasCounts, setNotasCounts] = useState({}); // { [avaliacaoId]: count }
  const [turmaAlunos, setTurmaAlunos] = useState(0);  // students in selected turma

  // Pagination
  const [page,       setPage]       = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [totalItems, setTotalItems] = useState(0);
  const PAGE_SIZE = 20;

  // UI state
  const [loading,    setLoading]    = useState(false);
  // Mensagem de resultado no topo da página (erro ao excluir etc.). Ficava
  // declarada no modal de notas e usada aqui: a página inteira quebrava
  // ao renderizar com "feedback is not defined".
  const [feedback,   setFeedback]   = useState(null);
  const [error,      setError]      = useState("");

  // Modals
  const [modalForm,    setModalForm]    = useState(null); // avaliacao obj or {} for new
  const [modalLancar,  setModalLancar]  = useState(null); // avaliacao to launch notes
  const [modalDelete,  setModalDelete]  = useState(null); // avaliacao to delete

  // ── Load reference data ────────────────────────────────────────────────────
  useEffect(() => {
    api.get("/turmas?size=100").then(d => setTurmas(d?.content || d || [])).catch(() => {});
    api.get("/disciplinas/ativas").then(d => setDisciplinas(Array.isArray(d) ? d : d?.content || [])).catch(() => {});
  }, []);

  // ── Load avaliacoes with filters ───────────────────────────────────────────
  const loadAvaliacoes = useCallback(async (currentPage = 0) => {
    setLoading(true); setError("");
    try {
      const params = new URLSearchParams({ page: String(currentPage), size: String(PAGE_SIZE) });
      if (filterTurmaId)      params.set("turmaId",      filterTurmaId);
      if (filterDisciplinaId) params.set("disciplinaId", filterDisciplinaId);
      if (filterPeriodo)      params.set("periodo",      filterPeriodo);
      if (filterStatus)       params.set("status",       filterStatus);

      const res = await api.get(`/avaliacoes?${params.toString()}`);
      const content = res?.content || res || [];
      const pageItems = Array.isArray(content) ? content : [];

      // Filter by tipo client-side (not all backends support it yet)
      const filtered = filterTipo ? pageItems.filter(a => a.tipo === filterTipo) : pageItems;

      setAvaliacoes(filtered);
      setTotalPages(res?.totalPages ?? 1);
      setTotalItems(res?.totalElements ?? filtered.length);

      // Fetch notes count per avaliação lazily
      if (filtered.length > 0) {
        const counts = {};
        await Promise.all(filtered.map(async (av) => {
          try {
            const notas = await api.get(`/notas/avaliacao/${av.id}`);
            counts[av.id] = (notas || []).filter(n => n.nota !== null && n.nota !== undefined).length;
          } catch { counts[av.id] = 0; }
        }));
        setNotasCounts(counts);
      }
    } catch (e) { setError(e.message); }
    finally { setLoading(false); }
  }, [filterTurmaId, filterDisciplinaId, filterPeriodo, filterTipo, filterStatus]);

  // Load students count for selected turma
  useEffect(() => {
    if (!filterTurmaId) { setTurmaAlunos(0); return; }
    api.get(`/matriculas?turmaId=${filterTurmaId}`)
      .then(d => {
        const list = d?.content || d || [];
        setTurmaAlunos(Array.isArray(list) ? list.length : 0);
      })
      .catch(() => setTurmaAlunos(0));
  }, [filterTurmaId]);

  useEffect(() => {
    setPage(0);
    loadAvaliacoes(0);
  }, [filterTurmaId, filterDisciplinaId, filterPeriodo, filterTipo, filterStatus]);

  // ── KPIs computed from current page list + notasCounts ────────────────────
  const kpis = useMemo(() => {
    const total = totalItems;
    const aguardando = avaliacoes.filter(a =>
      (a.status === "publicada" || a.status === "aplicada") &&
      turmaAlunos > 0 &&
      (notasCounts[a.id] ?? 0) < turmaAlunos
    ).length;

    // We'd need all notas for proper media — approximate from what we have
    // For simplicity show based on page data
    const mediaGeral = "—";
    const taxaAprov  = "—";

    return { total, aguardando, mediaGeral, taxaAprov };
  }, [avaliacoes, notasCounts, turmaAlunos, totalItems]);

  // ── Page change ───────────────────────────────────────────────────────────
  const goToPage = (p) => {
    setPage(p);
    loadAvaliacoes(p);
  };

  // ── Helpers ───────────────────────────────────────────────────────────────
  const turmaLabel = (turmaId) => {
    const t = turmas.find(t => t.id === turmaId);
    return t ? t.nome : "—";
  };
  const disciplinaLabel = (disciplinaId) => {
    const d = disciplinas.find(d => d.id === disciplinaId);
    return d ? d.nome : "—";
  };

  const lancamentoProgress = (av) => {
    if (!turmaAlunos && !filterTurmaId) return "—";
    const total = turmaAlunos || 0;
    const done  = notasCounts[av.id] ?? 0;
    if (total === 0) return "—";
    const pct = Math.round((done / total) * 100);
    if (done >= total) return <span style={{ color: "var(--color-success)", fontWeight: 600 }}>{done}/{total} ✓</span>;
    return <span style={{ color: "var(--color-text-muted)" }}>{done}/{total} <span style={{ fontSize: "0.75rem" }}>({pct}%)</span></span>;
  };

  // ─────────────────────────────────────────────────────────────────────────

  return (
    <div className="page">
      {/* ── Page Header ── */}
      <Feedback tipo={feedback?.tipo} mensagem={feedback?.mensagem} onFechar={() => setFeedback(null)} />
      <div className="page-header">
        <div>
          <h1 className="page-title">Avaliações</h1>
          <p className="page-subtitle">Planejamento e gestão de avaliações acadêmicas</p>
        </div>
        <button className="btn btn-brand" onClick={() => setModalForm({})}>
          <Icon name="Plus" size={14} /> Nova Avaliação
        </button>
      </div>

      {/* ── Filter Bar ── */}
      <div className="card">
        <div className="card-body">
          <div className="filter-bar" style={{ flexWrap: "wrap" }}>
            <div className="form-field" style={{ flex: "1 1 160px" }}>
              <label className="form-label">Turma</label>
              <select
                className="form-select"
                value={filterTurmaId}
                onChange={e => { setFilterTurmaId(e.target.value); setFilterDisciplinaId(""); }}
              >
                <option value="">Todas as turmas</option>
                {turmas.map(t => (
                  <option key={t.id} value={t.id}>{t.nome}{t.anoLetivo ? ` — ${t.anoLetivo}` : ""}</option>
                ))}
              </select>
            </div>
            <div className="form-field" style={{ flex: "1 1 160px" }}>
              <label className="form-label">Disciplina</label>
              <select
                className="form-select"
                value={filterDisciplinaId}
                onChange={e => setFilterDisciplinaId(e.target.value)}
              >
                <option value="">Todas as disciplinas</option>
                {disciplinas.map(d => <option key={d.id} value={d.id}>{d.nome}</option>)}
              </select>
            </div>
            <div className="form-field" style={{ flex: "1 1 130px" }}>
              <label className="form-label">Período</label>
              <select className="form-select" value={filterPeriodo} onChange={e => setFilterPeriodo(e.target.value)}>
                <option value="">Todos</option>
                {PERIODOS.map(p => <option key={p.value} value={p.value}>{p.label}</option>)}
              </select>
            </div>
            <div className="form-field" style={{ flex: "1 1 130px" }}>
              <label className="form-label">Tipo</label>
              <select className="form-select" value={filterTipo} onChange={e => setFilterTipo(e.target.value)}>
                <option value="">Todos</option>
                {TIPOS.map(t => <option key={t.value} value={t.value}>{t.label}</option>)}
              </select>
            </div>
            <div className="form-field" style={{ flex: "1 1 120px" }}>
              <label className="form-label">Status</label>
              <select className="form-select" value={filterStatus} onChange={e => setFilterStatus(e.target.value)}>
                <option value="">Todos</option>
                {STATUS_OPTS.map(s => <option key={s.value} value={s.value}>{s.label}</option>)}
              </select>
            </div>
            {(filterTurmaId || filterDisciplinaId || filterPeriodo || filterTipo || filterStatus) && (
              <div className="form-field" style={{ flex: "0 0 auto", alignSelf: "flex-end" }}>
                <button
                  className="btn btn-ghost"
                  onClick={() => {
                    setFilterTurmaId(""); setFilterDisciplinaId("");
                    setFilterPeriodo(""); setFilterTipo(""); setFilterStatus("");
                  }}
                >
                  <Icon name="X" size={13} /> Limpar
                </button>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* ── KPI Cards (show when turma is selected) ── */}
      {filterTurmaId && (
        <div style={{ display: "flex", gap: "1rem", marginTop: "0.5rem", flexWrap: "wrap" }}>
          <KpiCard
            icon="ClipboardCheck"
            label="Total de Avaliações"
            value={kpis.total}
            color="var(--color-brand)"
            subtitle={filterDisciplinaId ? disciplinaLabel(filterDisciplinaId) : "todas as disciplinas"}
          />
          <KpiCard
            icon="Clock"
            label="Aguardando Lançamento"
            value={kpis.aguardando}
            color="var(--color-warning)"
            subtitle="publicadas/aplicadas sem 100% notas"
          />
          <KpiCard
            icon="BarChart2"
            label="Média Geral da Turma"
            value={kpis.mediaGeral}
            color="var(--color-success)"
            subtitle="nas avaliações corrigidas"
          />
          <KpiCard
            icon="TrendingUp"
            label="Taxa de Aprovação"
            value={kpis.taxaAprov}
            color="var(--color-info, #0ea5e9)"
            subtitle="alunos acima da nota mínima"
          />
        </div>
      )}

      {/* ── Error ── */}
      {error && (
        <div className="login-error" style={{ marginTop: "0.5rem" }}>
          <Icon name="AlertCircle" size={14} /> {error}
        </div>
      )}

      {/* ── Table ── */}
      {loading ? (
        <div className="table-wrapper" style={{ marginTop: "0.5rem" }}>
          {Array.from({ length: 6 }).map((_, i) => (
            <div key={i} className="skeleton skeleton-text" style={{ height: "3rem", margin: "0.4rem 0" }} />
          ))}
        </div>
      ) : avaliacoes.length === 0 ? (
        <div className="empty-state" style={{ padding: "4rem 2rem", marginTop: "0.5rem" }}>
          <div className="empty-state-icon">
            <Icon name="ClipboardCheck" size={36} />
          </div>
          <h3>Nenhuma avaliação encontrada</h3>
          <p>
            {filterTurmaId || filterDisciplinaId || filterPeriodo || filterTipo || filterStatus
              ? "Nenhuma avaliação corresponde aos filtros aplicados."
              : "Crie a primeira avaliação clicando em \"Nova Avaliação\"."}
          </p>
          <button className="btn btn-brand" style={{ marginTop: "1rem" }} onClick={() => setModalForm({})}>
            <Icon name="Plus" size={14} /> Nova Avaliação
          </button>
        </div>
      ) : (
        <>
          <div className="table-wrapper" style={{ marginTop: "0.5rem" }}>
            <table className="data-table">
              <thead>
                <tr>
                  <th>Avaliação</th>
                  <th>Turma / Disciplina</th>
                  <th>Período</th>
                  <th>Data</th>
                  <th style={{ textAlign: "center" }}>Peso</th>
                  <th style={{ textAlign: "center" }}>Nota Máx/Mín</th>
                  <th style={{ textAlign: "center" }}>Status</th>
                  <th style={{ textAlign: "center" }}>Lançamento</th>
                  <th style={{ textAlign: "right" }}>Ações</th>
                </tr>
              </thead>
              <tbody>
                {avaliacoes.map(av => (
                  <tr key={av.id}>
                    {/* Avaliação */}
                    <td>
                      <div style={{ display: "flex", flexDirection: "column", gap: "0.25rem" }}>
                        <strong style={{ fontSize: "0.9rem" }}>{av.nome}</strong>
                        <span className={`badge ${tipoBadgeClass(av.tipo)}`} style={{ alignSelf: "flex-start", fontSize: "0.7rem" }}>
                          {tipoLabel(av.tipo)}
                        </span>
                      </div>
                    </td>
                    {/* Turma / Disciplina */}
                    <td>
                      <div style={{ display: "flex", flexDirection: "column", gap: "0.15rem" }}>
                        <span style={{ fontSize: "0.85rem", fontWeight: 600 }}>{turmaLabel(av.turmaId)}</span>
                        <span style={{ fontSize: "0.78rem", color: "var(--color-text-muted)" }}>{disciplinaLabel(av.disciplinaId)}</span>
                      </div>
                    </td>
                    {/* Período */}
                    <td className="td-muted" style={{ fontSize: "0.82rem" }}>
                      {av.periodo ? periodoLabel(av.periodo) : "—"}
                    </td>
                    {/* Data */}
                    <td className="td-muted" style={{ fontSize: "0.82rem", whiteSpace: "nowrap" }}>
                      <div>{av.dataAvaliacao ? fmtDate(av.dataAvaliacao) : "—"}</div>
                      {av.dataEntrega && (
                        <div style={{ fontSize: "0.73rem" }}>
                          <Icon name="Download" size={10} style={{ marginRight: 2 }} />
                          {fmtDate(av.dataEntrega)}
                        </div>
                      )}
                    </td>
                    {/* Peso */}
                    <td style={{ textAlign: "center" }}>
                      <span style={{ fontWeight: 600, color: "var(--color-brand)" }}>{av.peso ?? 1}</span>
                    </td>
                    {/* Nota Máx/Mín */}
                    <td style={{ textAlign: "center", fontSize: "0.85rem", whiteSpace: "nowrap" }}>
                      <span style={{ color: "var(--color-success)", fontWeight: 600 }}>{av.notaMaxima ?? 10}</span>
                      <span style={{ color: "var(--color-text-muted)", margin: "0 3px" }}>/</span>
                      <span style={{ color: "var(--color-danger)", fontWeight: 600 }}>{av.notaMinima ?? 5}</span>
                    </td>
                    {/* Status */}
                    <td style={{ textAlign: "center" }}>
                      <span className={`badge ${statusBadgeClass(av.status)}`}>
                        {statusLabel(av.status)}
                      </span>
                    </td>
                    {/* Lançamento Progress */}
                    <td style={{ textAlign: "center", fontSize: "0.82rem" }}>
                      {lancamentoProgress(av)}
                    </td>
                    {/* Ações */}
                    <td style={{ textAlign: "right", whiteSpace: "nowrap" }}>
                      <div style={{ display: "flex", gap: "0.25rem", justifyContent: "flex-end" }}>
                        <button
                          className="btn btn-secondary btn-sm"
                          title="Lançar Notas"
                          onClick={() => setModalLancar(av)}
                          style={{ fontSize: "0.75rem", padding: "0.25rem 0.6rem" }}
                        >
                          <Icon name="Edit3" size={12} /> Lançar
                        </button>
                        <button
                          className="btn btn-ghost btn-icon"
                          title="Editar avaliação"
                          onClick={() => setModalForm(av)}
                        >
                          <Icon name="Edit" size={14} />
                        </button>
                        <button
                          className="btn btn-ghost btn-icon"
                          title="Excluir avaliação"
                          style={{ color: "var(--color-danger)" }}
                          onClick={() => setModalDelete(av)}
                        >
                          <Icon name="Trash" size={14} />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          {/* Pagination */}
          {totalPages > 1 && (
            <div style={{ marginTop: "1rem" }}>
              <Pagination
                page={page}
                totalPages={totalPages}
                total={totalItems}
                pageSize={PAGE_SIZE}
                onPageChange={goToPage}
              />
            </div>
          )}

          {/* Row count summary */}
          <div style={{ textAlign: "right", color: "var(--color-text-muted)", fontSize: "0.8rem", marginTop: "0.5rem" }}>
            {totalItems} avaliação{totalItems !== 1 ? "ões" : ""}
            {(filterTurmaId || filterDisciplinaId) ? " encontrada(s) nos filtros aplicados" : " no total"}
          </div>
        </>
      )}

      {/* ── Lançar Notas Modal ── */}
      {modalLancar && (
        <LancarNotasModal
          avaliacao={modalLancar}
          turmaLabel={turmaLabel(modalLancar.turmaId)}
          disciplinaLabel={disciplinaLabel(modalLancar.disciplinaId)}
          onClose={() => setModalLancar(null)}
          onSaved={() => loadAvaliacoes(page)}
        />
      )}

      {/* ── Create / Edit Modal ── */}
      {modalForm !== null && (
        <AvaliacaoFormModal
          avaliacao={modalForm?.id ? modalForm : null}
          turmas={turmas}
          disciplinas={disciplinas}
          onClose={() => setModalForm(null)}
          onSaved={() => loadAvaliacoes(page)}
        />
      )}

      {/* ── Delete Confirm Modal ── */}
      {modalDelete && (
        <DeleteModal
          avaliacao={modalDelete}
          onClose={() => setModalDelete(null)}
          onDeleted={() => loadAvaliacoes(page)}
        />
      )}
    </div>
  );
}
