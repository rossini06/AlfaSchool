import { useState, useEffect, useCallback } from "react";
import { api } from "../services/api";
import { Modal } from "../components/Modal";
import { Pagination } from "../components/Pagination";
import { Icon } from "../components/Icon";
import { Feedback } from "../components/access/Feedback";
import { ConfirmarModal } from "../components/access/ConfirmarModal";

const PAGE_SIZE = 20;

const STATUS_MAP = {
  ativa:     { label: "Ativa",     badge: "badge-success" },
  cancelada: { label: "Cancelada", badge: "badge-danger"  },
  trancada:  { label: "Trancada",  badge: "badge-warning" },
  concluida: { label: "Concluída", badge: "badge-info"    },
};

const TIPOS_MATRICULA = ["regular", "transferencia", "rematricula", "especial"];
const TIPOS_LABELS = { regular: "Regular", transferencia: "Transferência", rematricula: "Rematrícula", especial: "Especial" };

export function MatriculasPage() {
  const [items, setItems] = useState([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [filterStatus, setFilterStatus] = useState("");
  const [filterTurma, setFilterTurma] = useState("");
  const [filterAno, setFilterAno] = useState("");
  const [turmas, setTurmas] = useState([]);
  const [feedback, setFeedback] = useState(null);
  // Cancelar matricula e' destrutivo e vinha com a mesma caixa neutra do
  // sistema que "reativar" — o confirm() nativo nao distingue os dois.
  const [mudandoStatus, setMudandoStatus] = useState(null);
  const [processandoStatus, setProcessandoStatus] = useState(false);

  // New matricula modal (multi-step)
  const [modalOpen, setModalOpen] = useState(false);
  const [step, setStep] = useState(1);
  const [alunoSearch, setAlunoSearch] = useState("");
  const [alunoResults, setAlunoResults] = useState([]);
  const [alunoSearching, setAlunoSearching] = useState(false);
  const [selectedAluno, setSelectedAluno] = useState(null);
  const [turmaSearch, setTurmaSearch] = useState("");
  const [selectedTurma, setSelectedTurma] = useState(null);
  const [matriculaData, setMatriculaData] = useState(new Date().toISOString().split("T")[0]);
  const [matriculaObs, setMatriculaObs] = useState("");
  const [matriculaTipo, setMatriculaTipo] = useState("regular");
  const [matriculaDesconto, setMatriculaDesconto] = useState("");
  const [saving, setSaving] = useState(false);

  const load = useCallback(async (p = 0) => {
    setLoading(true);
    setError("");
    try {
      const params = new URLSearchParams({ page: p, size: PAGE_SIZE });
      if (filterStatus) params.set("status", filterStatus);
      if (filterTurma) params.set("turmaId", filterTurma);
      if (filterAno) params.set("anoLetivo", filterAno);
      const data = await api.get(`/matriculas?${params}`);
      setItems(data?.content || data || []);
      setTotal(data?.totalElements ?? (data?.content ?? data ?? []).length);
      setPage(p);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, [filterStatus, filterTurma, filterAno]);

  useEffect(() => {
    load(0);
    api.get("/turmas?size=200&ativo=true").then((d) => setTurmas(d?.content || d || [])).catch(() => {});
  }, []);

  const searchAlunos = async (q) => {
    if (!q.trim()) { setAlunoResults([]); return; }
    setAlunoSearching(true);
    try {
      const data = await api.get(`/alunos?q=${encodeURIComponent(q)}&size=10`);
      setAlunoResults(data?.content || data || []);
    } catch {
      setAlunoResults([]);
    } finally {
      setAlunoSearching(false);
    }
  };

  const filteredTurmas = turmas.filter((t) =>
    !turmaSearch || t.nome.toLowerCase().includes(turmaSearch.toLowerCase()) ||
    (t.cursoNome || "").toLowerCase().includes(turmaSearch.toLowerCase())
  );

  const openNew = () => {
    setStep(1);
    setAlunoSearch(""); setAlunoResults([]); setSelectedAluno(null);
    setTurmaSearch(""); setSelectedTurma(null);
    setMatriculaData(new Date().toISOString().split("T")[0]);
    setMatriculaObs("");
    setMatriculaTipo("regular");
    setMatriculaDesconto("");
    setModalOpen(true);
  };

  const saveMatricula = async () => {
    if (!selectedAluno || !selectedTurma) return;
    setSaving(true);
    try {
      await api.post("/matriculas", {
        alunoId: selectedAluno.id,
        turmaId: selectedTurma.id,
        dataMatricula: matriculaData,
        obs: matriculaObs,
        tipo: matriculaTipo,
        desconto: matriculaDesconto ? Number(matriculaDesconto) : null,
      });
      setModalOpen(false);
      load(page);
    } catch (err) {
      setFeedback({ tipo: "erro", mensagem: err.message });
    } finally {
      setSaving(false);
    }
  };

  const updateStatus = (item, novoStatus) => setMudandoStatus({ item, novoStatus });

  const aplicarStatus = async () => {
    const { item, novoStatus } = mudandoStatus;
    setProcessandoStatus(true);
    try {
      const endpointMap = { ativa: "reativar", trancada: "trancar", cancelada: "cancelar", concluida: "concluir" };
      const endpoint = endpointMap[novoStatus];
      if (endpoint) {
        await api.post(`/matriculas/${item.id}/${endpoint}`);
      } else {
        await api.patch(`/matriculas/${item.id}/status`, { status: novoStatus });
      }
      setMudandoStatus(null);
      setFeedback({ tipo: "sucesso", mensagem: `Matrícula ${STATUS_MAP[novoStatus]?.label.toLowerCase()}.` });
      load(page);
    } catch (err) {
      setFeedback({ tipo: "erro", mensagem: err.message });
    } finally {
      setProcessandoStatus(false);
    }
  };

  const totalPages = Math.ceil(total / PAGE_SIZE);

  const currentYear = new Date().getFullYear();
  const years = Array.from({ length: 5 }, (_, i) => currentYear - 1 + i);

  const statusBadge = (s) => {
    const info = STATUS_MAP[s?.toLowerCase()] || { label: s || "—", badge: "badge-secondary" };
    return <span className={`badge ${info.badge}`}>{info.label}</span>;
  };

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1 className="page-title">Matrículas</h1>
          <p className="page-subtitle">Gerenciamento de matrículas dos alunos</p>
        </div>
        <button className="btn btn-brand" onClick={openNew}>
          <Icon name="Plus" size={14} />
          Nova Matrícula
        </button>
      </div>

      {/* Filters */}
      <div className="card">
        <div className="card-body">
          <div className="filter-bar">
            <div className="form-field">
              <select className="form-select" value={filterStatus} onChange={(e) => setFilterStatus(e.target.value)}>
                <option value="">Todos os status</option>
                {Object.entries(STATUS_MAP).map(([k, v]) => <option key={k} value={k}>{v.label}</option>)}
              </select>
            </div>
            <div className="form-field">
              <select className="form-select" value={filterTurma} onChange={(e) => setFilterTurma(e.target.value)}>
                <option value="">Todas as turmas</option>
                {turmas.map((t) => <option key={t.id} value={t.id}>{t.nome}</option>)}
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
            <button className="btn btn-secondary" onClick={() => { setFilterStatus(""); setFilterTurma(""); setFilterAno(""); load(0); }}>
              Limpar
            </button>
          </div>
        </div>
      </div>

      {error && <div className="login-error"><Icon name="AlertCircle" size={14} /> {error}</div>}
      <Feedback tipo={feedback?.tipo} mensagem={feedback?.mensagem} onFechar={() => setFeedback(null)} />

      <ConfirmarModal
        aberto={!!mudandoStatus}
        titulo={
          mudandoStatus?.novoStatus === "cancelada"
            ? "Cancelar esta matrícula?"
            : `Mudar a matrícula para "${STATUS_MAP[mudandoStatus?.novoStatus]?.label}"?`
        }
        textoConfirmar={STATUS_MAP[mudandoStatus?.novoStatus]?.label || "Confirmar"}
        variante={mudandoStatus?.novoStatus === "cancelada" ? "btn-danger" : "btn-brand"}
        processando={processandoStatus}
        onCancelar={() => setMudandoStatus(null)}
        onConfirmar={aplicarStatus}
      >
        <p>
          {mudandoStatus?.novoStatus === "cancelada"
            ? "O aluno deixa de constar na turma e sai das listas de frequência e de notas. Uma matrícula cancelada pode ser reativada depois, mas o período cancelado fica registrado."
            : "A situação da matrícula muda para quem consulta a turma, a frequência e o boletim."}
        </p>
        {mudandoStatus?.item && (
          <p className="ac-meta mt-2">
            {mudandoStatus.item.alunoNome || mudandoStatus.item.numeroMatricula || "Matrícula"}
            {mudandoStatus.item.turmaNome ? ` · ${mudandoStatus.item.turmaNome}` : ""}
          </p>
        )}
      </ConfirmarModal>

      <div className="table-wrapper">
        <table className="data-table">
          <thead>
            <tr>
              <th>Nº Matrícula</th>
              <th>Aluno</th>
              <th>Turma / Curso</th>
              <th>Data Matrícula</th>
              <th>Status</th>
              <th>Ações</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              Array.from({ length: 10 }).map((_, i) => (
                <tr key={i}>{Array.from({ length: 6 }).map((_, j) => <td key={j}><div className="skeleton skeleton-text" /></td>)}</tr>
              ))
            ) : items.length === 0 ? (
              <tr><td colSpan={6}>
                <div className="empty-state">
                  <div className="empty-state-icon"><Icon name="ClipboardList" size={28} /></div>
                  <h3>Nenhuma matrícula encontrada</h3>
                  <p>Registre a primeira matrícula de um aluno em uma turma.</p>
                </div>
              </td></tr>
            ) : (
              items.map((item) => (
                <tr key={item.id}>
                  <td className="td-muted" style={{ fontFamily: "monospace", fontSize: 12 }}>
                    {item.numeroMatricula || "—"}
                  </td>
                  <td><strong>{item.alunoNome || "—"}</strong></td>
                  <td className="td-muted">
                    {item.turmaNome || "—"}
                    {item.cursoNome && <span style={{ color: "var(--color-text-2)", fontSize: 11 }}> · {item.cursoNome}</span>}
                  </td>
                  <td className="td-muted">
                    {item.dataMatricula
                      ? new Date(item.dataMatricula).toLocaleDateString("pt-BR")
                      : "—"}
                  </td>
                  <td>{statusBadge(item.status)}</td>
                  <td>
                    <div className="td-actions">
                      {item.status !== "ativa" && (
                        <button className="btn btn-ghost btn-xs" onClick={() => updateStatus(item, "ativa")} title="Reativar" aria-label="Reativar matrícula">
                          <Icon name="Check" size={12} />
                        </button>
                      )}
                      {item.status === "ativa" && (
                        <>
                          <button className="btn btn-ghost btn-xs" onClick={() => updateStatus(item, "trancada")} title="Trancar" aria-label="Trancar matrícula">
                            <Icon name="Lock" size={12} />
                          </button>
                          <button className="btn btn-ghost btn-xs" onClick={() => updateStatus(item, "concluida")} title="Concluir" aria-label="Concluir matrícula">
                            <Icon name="CheckCircle" size={12} />
                          </button>
                          <button className="btn btn-ghost btn-xs text-danger" onClick={() => updateStatus(item, "cancelada")} title="Cancelar" aria-label="Cancelar matrícula">
                            <Icon name="X" size={12} />
                          </button>
                        </>
                      )}
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
        <Pagination page={page} totalPages={totalPages} total={total} pageSize={PAGE_SIZE} onPageChange={(p) => load(p)} />
      </div>

      {/* Nova Matrícula Modal - Multi-step */}
      <Modal isOpen={modalOpen} onClose={() => setModalOpen(false)}
        title="Nova Matrícula"
        size="lg"
        footer={
          <div style={{ display: "flex", justifyContent: "space-between", width: "100%" }}>
            <div>
              {step > 1 && (
                <button className="btn btn-secondary" onClick={() => setStep((s) => s - 1)}>
                  ← Voltar
                </button>
              )}
            </div>
            <div style={{ display: "flex", gap: 8 }}>
              <button className="btn btn-secondary" onClick={() => setModalOpen(false)}>Cancelar</button>
              {step < 3 ? (
                <button
                  className="btn btn-brand"
                  onClick={() => setStep((s) => s + 1)}
                  disabled={(step === 1 && !selectedAluno) || (step === 2 && !selectedTurma)}
                >
                  Próximo →
                </button>
              ) : (
                <button className="btn btn-brand" onClick={saveMatricula} disabled={saving || !selectedAluno || !selectedTurma}>
                  {saving ? "Salvando..." : "Confirmar Matrícula"}
                </button>
              )}
            </div>
          </div>
        }
      >
        {/* Steps indicator */}
        <div className="steps" style={{ marginBottom: 24 }}>
          {[
            { n: 1, label: "Selecionar Aluno" },
            { n: 2, label: "Selecionar Turma" },
            { n: 3, label: "Confirmar" },
          ].map((s, i, arr) => (
            <div key={s.n} className={`step ${step > s.n ? "done" : step === s.n ? "active" : ""}`}>
              <div className="step-circle">{step > s.n ? "✓" : s.n}</div>
              <span className="step-label">{s.label}</span>
              {i < arr.length - 1 && <div className="step-line" />}
            </div>
          ))}
        </div>

        {/* Step 1: Selecionar Aluno */}
        {step === 1 && (
          <div>
            <div className="form-field" style={{ marginBottom: 16 }}>
              <label className="form-label">Buscar Aluno por nome ou CPF</label>
              <div style={{ display: "flex", gap: 8 }}>
                <input
                  className="form-input"
                  value={alunoSearch}
                  onChange={(e) => setAlunoSearch(e.target.value)}
                  placeholder="Nome ou CPF do aluno..."
                  onKeyDown={(e) => e.key === "Enter" && searchAlunos(alunoSearch)}
                />
                <button className="btn btn-brand" onClick={() => searchAlunos(alunoSearch)}>
                  <Icon name="Search" size={14} />
                </button>
              </div>
            </div>

            {alunoSearching && <div style={{ textAlign: "center", color: "var(--color-text-2)", padding: 20 }}>Buscando...</div>}

            {alunoResults.length > 0 && (
              <div style={{ border: "1px solid var(--color-border)", borderRadius: "var(--radius-md)", overflow: "hidden" }}>
                {alunoResults.map((a) => (
                  <div
                    key={a.id}
                    onClick={() => setSelectedAluno(a)}
                    style={{
                      padding: "12px 16px",
                      cursor: "pointer",
                      borderBottom: "1px solid var(--color-border)",
                      background: selectedAluno?.id === a.id ? "var(--color-brand-dim)" : "transparent",
                      display: "flex", alignItems: "center", gap: 12,
                      transition: "background 0.15s",
                    }}
                  >
                    <div style={{
                      width: 32, height: 32, borderRadius: "50%",
                      background: "var(--color-brand)", color: "#fff",
                      display: "flex", alignItems: "center", justifyContent: "center",
                      fontWeight: 700, fontSize: 13, flexShrink: 0,
                    }}>
                      {a.nome?.[0]?.toUpperCase()}
                    </div>
                    <div>
                      <div style={{ fontWeight: 600, color: "var(--color-text)" }}>{a.nome}</div>
                      <div style={{ fontSize: 12, color: "var(--color-text-2)" }}>
                        {a.cpf ? `CPF: ${a.cpf}` : ""}{a.email ? ` · ${a.email}` : ""}
                      </div>
                    </div>
                    {selectedAluno?.id === a.id && (
                      <div style={{ marginLeft: "auto", color: "var(--color-brand)" }}>
                        <Icon name="Check" size={16} />
                      </div>
                    )}
                  </div>
                ))}
              </div>
            )}

            {selectedAluno && (
              <div style={{
                marginTop: 16, padding: "12px 16px",
                background: "var(--color-success-dim)",
                border: "1px solid var(--color-success)",
                borderRadius: "var(--radius-md)",
                color: "var(--color-success)", display: "flex", alignItems: "center", gap: 8,
              }}>
                <Icon name="CheckCircle" size={16} />
                <strong>Selecionado:</strong> {selectedAluno.nome}
              </div>
            )}
          </div>
        )}

        {/* Step 2: Selecionar Turma */}
        {step === 2 && (
          <div>
            <div className="form-field" style={{ marginBottom: 16 }}>
              <label className="form-label">Filtrar turmas</label>
              <input
                className="form-input"
                value={turmaSearch}
                onChange={(e) => setTurmaSearch(e.target.value)}
                placeholder="Buscar por nome da turma ou curso..."
              />
            </div>

            <div style={{ border: "1px solid var(--color-border)", borderRadius: "var(--radius-md)", overflow: "hidden", maxHeight: 320, overflowY: "auto" }}>
              {filteredTurmas.length === 0 ? (
                <div style={{ padding: 20, textAlign: "center", color: "var(--color-text-2)" }}>Nenhuma turma disponível</div>
              ) : filteredTurmas.map((t) => (
                <div
                  key={t.id}
                  onClick={() => setSelectedTurma(t)}
                  style={{
                    padding: "12px 16px",
                    cursor: "pointer",
                    borderBottom: "1px solid var(--color-border)",
                    background: selectedTurma?.id === t.id ? "var(--color-brand-dim)" : "transparent",
                    display: "flex", alignItems: "center", justifyContent: "space-between",
                    transition: "background 0.15s",
                  }}
                >
                  <div>
                    <div style={{ fontWeight: 600, color: "var(--color-text)" }}>{t.nome}</div>
                    <div style={{ fontSize: 12, color: "var(--color-text-2)" }}>
                      {t.cursoNome || "—"} · {t.anoLetivo || "—"} · {t.turno || "—"}
                    </div>
                  </div>
                  <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
                    {t.capacidade && (
                      <span className="badge badge-secondary">{t.totalAlunos || 0}/{t.capacidade} alunos</span>
                    )}
                    {selectedTurma?.id === t.id && (
                      <div style={{ color: "var(--color-brand)" }}><Icon name="Check" size={16} /></div>
                    )}
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Step 3: Confirmar */}
        {step === 3 && (
          <div className="form-grid">
            {/* Summary */}
            <div style={{
              background: "var(--color-bg-3)",
              border: "1px solid var(--color-border)",
              borderRadius: "var(--radius-md)",
              padding: 16,
            }}>
              <div style={{ fontWeight: 700, marginBottom: 10, color: "var(--color-text)" }}>Resumo da Matrícula</div>
              <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
                <div style={{ display: "flex", justifyContent: "space-between" }}>
                  <span style={{ color: "var(--color-text-2)" }}>Aluno:</span>
                  <strong style={{ color: "var(--color-text)" }}>{selectedAluno?.nome}</strong>
                </div>
                <div style={{ display: "flex", justifyContent: "space-between" }}>
                  <span style={{ color: "var(--color-text-2)" }}>Turma:</span>
                  <strong style={{ color: "var(--color-text)" }}>{selectedTurma?.nome}</strong>
                </div>
                <div style={{ display: "flex", justifyContent: "space-between" }}>
                  <span style={{ color: "var(--color-text-2)" }}>Curso:</span>
                  <span style={{ color: "var(--color-text)" }}>{selectedTurma?.cursoNome || "—"}</span>
                </div>
                <div style={{ display: "flex", justifyContent: "space-between" }}>
                  <span style={{ color: "var(--color-text-2)" }}>Ano Letivo:</span>
                  <span style={{ color: "var(--color-text)" }}>{selectedTurma?.anoLetivo || "—"}</span>
                </div>
                <div style={{ display: "flex", justifyContent: "space-between" }}>
                  <span style={{ color: "var(--color-text-2)" }}>Turno:</span>
                  <span style={{ color: "var(--color-text)" }}>{selectedTurma?.turno || "—"}</span>
                </div>
              </div>
            </div>

            <div className="form-grid-2">
              <div className="form-field">
                <label className="form-label">Tipo de Matrícula</label>
                <select className="form-select" value={matriculaTipo} onChange={(e) => setMatriculaTipo(e.target.value)}>
                  {TIPOS_MATRICULA.map(t => <option key={t} value={t}>{TIPOS_LABELS[t]}</option>)}
                </select>
              </div>
              <div className="form-field">
                <label className="form-label">Data da Matrícula</label>
                <input className="form-input" type="date" value={matriculaData}
                  onChange={(e) => setMatriculaData(e.target.value)} />
              </div>
            </div>

            <div className="form-field">
              <label className="form-label">Desconto (%)</label>
              <input className="form-input" type="number" min="0" max="100" step="0.01"
                value={matriculaDesconto} onChange={(e) => setMatriculaDesconto(e.target.value)}
                placeholder="0.00" />
            </div>

            <div className="form-field">
              <label className="form-label">Observações</label>
              <textarea className="form-textarea" value={matriculaObs}
                onChange={(e) => setMatriculaObs(e.target.value)}
                placeholder="Observações sobre a matrícula (opcional)..." rows={3} />
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
}
