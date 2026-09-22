import { useState, useEffect, useRef, useCallback } from "react";
import { api } from "../services/api";
import { Icon } from "../components/Icon";

function situacaoVariant(situacao) {
  switch (situacao) {
    case "APROVADO":       return "success";
    case "REPROVADO":
    case "REPROVADO_FREQUENCIA":
    case "REPROVADO_NOTA": return "danger";
    case "RECUPERACAO":    return "warning";
    default:               return "secondary";
  }
}

function situacaoLabel(situacao) {
  switch (situacao) {
    case "APROVADO":             return "Aprovado";
    case "REPROVADO":            return "Reprovado";
    case "REPROVADO_FREQUENCIA": return "Repr. Frequência";
    case "REPROVADO_NOTA":       return "Repr. Nota";
    case "RECUPERACAO":          return "Em Recuperação";
    case "CURSANDO":             return "Cursando";
    default:                     return situacao || "—";
  }
}

function fmt(n) {
  if (n === null || n === undefined) return "—";
  return Number(n).toFixed(2).replace(".", ",");
}

function SituacaoBadge({ situacao }) {
  return (
    <span className={`badge badge-${situacaoVariant(situacao)}`}>
      {situacaoLabel(situacao)}
    </span>
  );
}

export function BoletimPage() {
  const [matriculas, setMatriculas] = useState([]);
  const [turmas, setTurmas] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const printRef = useRef();

  const [turmaId, setTurmaId] = useState("");
  const [matriculaId, setMatriculaId] = useState("");
  const [periodo, setPeriodo] = useState("");
  const [boletim, setBoletim] = useState(null);
  const [detalheAberto, setDetalheAberto] = useState(false);

  const loadTurmas = async () => {
    try {
      const data = await api.get("/turmas?size=100");
      setTurmas(data?.content || []);
    } catch {
      setError("Erro ao carregar turmas");
    }
  };

  const loadMatriculas = useCallback(async () => {
    try {
      const data = await api.get(`/matriculas?turmaId=${turmaId}&size=100`);
      setMatriculas(data?.content || []);
    } catch {
      setError("Erro ao carregar matrículas");
    }
  }, [turmaId]);

  useEffect(() => { loadTurmas(); }, []);

  useEffect(() => {
    if (turmaId) {
      loadMatriculas();
    } else {
      setMatriculas([]);
    }
  }, [turmaId, loadMatriculas]);

  const gerarBoletim = async () => {
    if (!matriculaId) return;
    setLoading(true);
    setError("");
    setBoletim(null);
    setDetalheAberto(false);
    try {
      let url = `/boletim/${matriculaId}`;
      if (periodo) url += `?periodo=${periodo}`;
      setBoletim(await api.get(url));
    } catch (e) {
      setError(e?.message || "Erro ao gerar boletim");
    } finally {
      setLoading(false);
    }
  };

  const handlePrint = () => {
    const content = printRef.current;
    if (!content) return;
    const win = window.open("", "_blank");
    win.document.write(`
      <html>
        <head>
          <title>Boletim — ${boletim?.alunoNome}</title>
          <style>
            * { box-sizing: border-box; margin: 0; padding: 0; }
            body { font-family: Arial, sans-serif; padding: 24px; color: #111; }
            h1 { font-size: 18px; text-align: center; margin-bottom: 4px; }
            .subtitle { text-align: center; font-size: 12px; color: #555; margin-bottom: 20px; }
            .info-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 4px 32px; margin-bottom: 16px; font-size: 12px; }
            .kpi-row { display: grid; grid-template-columns: repeat(4,1fr); gap: 12px; margin-bottom: 20px; }
            .kpi { border: 1px solid #ddd; border-radius: 6px; padding: 10px; text-align: center; }
            .kpi-val { font-size: 22px; font-weight: 800; }
            .kpi-lbl { font-size: 10px; color: #888; }
            table { width: 100%; border-collapse: collapse; font-size: 11px; margin-bottom: 16px; }
            th { background: #f0f0f0; padding: 7px 10px; text-align: left; border: 1px solid #ccc; }
            td { padding: 6px 10px; border: 1px solid #ddd; }
            .center { text-align: center; }
            .green { color: green; font-weight: 700; }
            .red { color: red; font-weight: 700; }
            .orange { color: #c77700; font-weight: 700; }
            .footer { text-align: center; font-size: 10px; color: #999; margin-top: 24px; border-top: 1px solid #ddd; padding-top: 10px; }
          </style>
        </head>
        <body>${content.innerHTML}
          <script>window.onload=function(){window.print();window.close();}</script>
        </body>
      </html>
    `);
    win.document.close();
  };

  return (
    <div className="page">
      {/* Cabeçalho */}
      <div className="page-header">
        <div>
          <h1 className="page-title" style={{ display: "flex", alignItems: "center", gap: 10 }}>
            <Icon name="FileSpreadsheet" size={22} />
            Boletim Escolar
          </h1>
          <p className="page-subtitle">Consulta de médias, frequências e situação por aluno</p>
        </div>
      </div>

      {/* Painel de filtros */}
      <div className="card">
        <div className="card-header">
          <span className="card-title">Selecione o Aluno</span>
        </div>
        <div className="card-body">
          <div style={{ display: "flex", gap: 14, flexWrap: "wrap", alignItems: "flex-end" }}>
            {/* Turma */}
            <div className="form-field" style={{ flex: "1 1 180px" }}>
              <label className="form-label">Turma</label>
              <select
                className="form-select"
                value={turmaId}
                onChange={(e) => {
                  setTurmaId(e.target.value);
                  setMatriculaId("");
                  setBoletim(null);
                }}
              >
                <option value="">Selecione...</option>
                {turmas.map((t) => (
                  <option key={t.id} value={t.id}>
                    {t.nome} ({t.anoLetivo})
                  </option>
                ))}
              </select>
            </div>

            {/* Aluno */}
            <div className="form-field" style={{ flex: "2 1 220px" }}>
              <label className="form-label">Aluno (Matrícula)</label>
              <select
                className="form-select"
                value={matriculaId}
                onChange={(e) => { setMatriculaId(e.target.value); setBoletim(null); }}
                disabled={!turmaId}
              >
                <option value="">Selecione...</option>
                {matriculas.map((m) => (
                  <option key={m.id} value={m.id}>
                    {m.alunoNome || m.numeroMatricula} — {m.numeroMatricula}
                  </option>
                ))}
              </select>
            </div>

            {/* Período */}
            <div className="form-field" style={{ flex: "1 1 160px" }}>
              <label className="form-label">Período</label>
              <select
                className="form-select"
                value={periodo}
                onChange={(e) => setPeriodo(e.target.value)}
              >
                <option value="">Todos</option>
                <option value="1bim">1º Bimestre</option>
                <option value="2bim">2º Bimestre</option>
                <option value="3bim">3º Bimestre</option>
                <option value="4bim">4º Bimestre</option>
                <option value="1sem">1º Semestre</option>
                <option value="2sem">2º Semestre</option>
                <option value="anual">Anual</option>
              </select>
            </div>

            {/* Botão */}
            <button
              className="btn btn-brand"
              disabled={!matriculaId || loading}
              onClick={gerarBoletim}
              style={{ flexShrink: 0 }}
            >
              {loading ? (
                <>
                  <Icon name="Loader" size={15} />
                  Gerando...
                </>
              ) : (
                <>
                  <Icon name="FileText" size={15} />
                  Gerar Boletim
                </>
              )}
            </button>
          </div>
        </div>
      </div>

      {/* Erro */}
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

      {/* Empty state inicial */}
      {!boletim && !loading && (
        <div className="card">
          <div className="empty-state">
            <div className="empty-state-icon">
              <Icon name="FileSpreadsheet" size={28} />
            </div>
            <h3>Nenhum boletim gerado</h3>
            <p>Selecione a turma, o aluno e clique em "Gerar Boletim".</p>
          </div>
        </div>
      )}

      {/* Boletim */}
      {boletim && (
        <div className="card">
          {/* Header do card */}
          <div className="card-header">
            <div style={{ display: "flex", flexDirection: "column", gap: 2 }}>
              <span className="card-title">Boletim Escolar</span>
              <span style={{ fontSize: 12, color: "var(--color-text-2)" }}>
                {boletim.cursoNome} · {boletim.turmaNome}
                {periodo && <> · <strong>{periodo.toUpperCase()}</strong></>}
              </span>
            </div>
            <button className="btn btn-ghost btn-sm" onClick={handlePrint}>
              <Icon name="Printer" size={14} />
              Imprimir
            </button>
          </div>

          {/* Conteúdo imprimível */}
          <div className="card-body" ref={printRef}>
            {/* Título para impressão */}
            <h1
              style={{
                textAlign: "center",
                fontSize: 20,
                fontWeight: 800,
                marginBottom: 4,
                display: "none",
              }}
              className="print-title"
            >
              BOLETIM ESCOLAR
            </h1>
            <p
              className="subtitle"
              style={{ textAlign: "center", color: "var(--color-text-2)", display: "none" }}
            >
              {boletim.cursoNome} — {boletim.turmaNome}
            </p>

            {/* Dados do aluno */}
            <div
              style={{
                display: "grid",
                gridTemplateColumns: "1fr 1fr",
                gap: "6px 32px",
                padding: "14px 0",
                borderBottom: "1px solid var(--color-border)",
                marginBottom: 20,
              }}
            >
              {[
                ["Aluno", boletim.alunoNome],
                ["Ano Letivo", boletim.anoLetivo],
                ["Matrícula", boletim.numeroMatricula],
                ["Turno", boletim.turno],
                ["Turma", boletim.turmaNome],
                ["Nível", boletim.nivel || "—"],
              ].map(([k, v]) => (
                <div key={k} style={{ fontSize: 13.5 }}>
                  <span style={{ color: "var(--color-text-2)", fontWeight: 600, marginRight: 6 }}>
                    {k}:
                  </span>
                  {v}
                </div>
              ))}
            </div>

            {/* KPI cards */}
            <div className="kpi-grid" style={{ marginBottom: 24 }}>
              <div className="kpi-card">
                <div className="kpi-card-header">
                  <span className="kpi-label">Média Geral</span>
                  <div className="kpi-icon brand">
                    <Icon name="BarChart2" size={16} />
                  </div>
                </div>
                <div className="kpi-value">{fmt(boletim.mediaGeral)}</div>
              </div>

              <div className="kpi-card">
                <div className="kpi-card-header">
                  <span className="kpi-label">Frequência Geral</span>
                  <div className="kpi-icon info">
                    <Icon name="Activity" size={16} />
                  </div>
                </div>
                <div className="kpi-value">{fmt(boletim.frequenciaGeral)}%</div>
              </div>

              <div className="kpi-card">
                <div className="kpi-card-header">
                  <span className="kpi-label">Disciplinas</span>
                  <div className="kpi-icon success">
                    <Icon name="BookOpen" size={16} />
                  </div>
                </div>
                <div className="kpi-value">{boletim.totalDisciplinas}</div>
              </div>

              <div className="kpi-card">
                <div className="kpi-card-header">
                  <span className="kpi-label">Situação</span>
                  <div className={`kpi-icon ${situacaoVariant(boletim.situacaoGeral) === "success" ? "success" : situacaoVariant(boletim.situacaoGeral) === "danger" ? "danger" : "warning"}`}>
                    <Icon name={boletim.situacaoGeral === "APROVADO" ? "CheckCircle" : "AlertCircle"} size={16} />
                  </div>
                </div>
                <div style={{ marginTop: 6 }}>
                  <SituacaoBadge situacao={boletim.situacaoGeral} />
                </div>
              </div>
            </div>

            {/* Tabela de disciplinas */}
            <div className="table-wrapper" style={{ marginBottom: 20 }}>
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Disciplina</th>
                    <th style={{ width: 90, textAlign: "center" }}>Média</th>
                    <th style={{ width: 100, textAlign: "center" }}>Frequência</th>
                    <th style={{ width: 70, textAlign: "center" }}>Faltas</th>
                    <th style={{ width: 130, textAlign: "center" }}>Situação</th>
                  </tr>
                </thead>
                <tbody>
                  {boletim.disciplinas?.map((d) => (
                    <tr key={d.disciplinaId}>
                      <td>
                        <div style={{ fontWeight: 500 }}>{d.disciplinaNome}</div>
                        {d.codigo && (
                          <div className="td-muted" style={{ fontSize: 11 }}>
                            {d.codigo}
                          </div>
                        )}
                      </td>
                      <td style={{ textAlign: "center", fontWeight: 700 }}>
                        {d.conceito || fmt(d.media)}
                      </td>
                      <td style={{ textAlign: "center" }}>
                        <span
                          style={{
                            color:
                              parseFloat(d.percentualFrequencia) < 75
                                ? "var(--color-danger)"
                                : "var(--color-success)",
                            fontWeight: 600,
                          }}
                        >
                          {fmt(d.percentualFrequencia)}%
                        </span>
                      </td>
                      <td style={{ textAlign: "center", color: "var(--color-text-2)" }}>
                        {d.faltas || 0}
                      </td>
                      <td style={{ textAlign: "center" }}>
                        <SituacaoBadge situacao={d.situacao} />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {/* Detalhe das avaliações */}
            {boletim.disciplinas?.some((d) => d.avaliacoes?.length > 0) && (
              <div
                style={{
                  border: "1px solid var(--color-border)",
                  borderRadius: "var(--radius-md)",
                  overflow: "hidden",
                }}
              >
                <button
                  onClick={() => setDetalheAberto((v) => !v)}
                  style={{
                    width: "100%",
                    display: "flex",
                    alignItems: "center",
                    justifyContent: "space-between",
                    padding: "12px 16px",
                    background: "var(--color-bg-3)",
                    border: "none",
                    cursor: "pointer",
                    fontSize: 13.5,
                    fontWeight: 600,
                    color: "var(--color-text)",
                  }}
                >
                  <span style={{ display: "flex", alignItems: "center", gap: 8 }}>
                    <Icon name="ClipboardList" size={15} />
                    Detalhes das Avaliações
                  </span>
                  <Icon name={detalheAberto ? "ChevronUp" : "ChevronDown"} size={15} />
                </button>

                {detalheAberto && (
                  <div style={{ padding: 16, display: "flex", flexDirection: "column", gap: 20 }}>
                    {boletim.disciplinas.filter((d) => d.avaliacoes?.length > 0).map((d) => (
                      <div key={d.disciplinaId}>
                        <div
                          style={{
                            fontSize: 13,
                            fontWeight: 700,
                            marginBottom: 8,
                            color: "var(--color-text-2)",
                            textTransform: "uppercase",
                            letterSpacing: "0.05em",
                          }}
                        >
                          {d.disciplinaNome}
                        </div>
                        <div className="table-wrapper">
                          <table className="data-table">
                            <thead>
                              <tr>
                                <th>Avaliação</th>
                                <th style={{ width: 90, textAlign: "center" }}>Tipo</th>
                                <th style={{ width: 60, textAlign: "center" }}>Peso</th>
                                <th style={{ width: 70, textAlign: "center" }}>Nota</th>
                                <th style={{ width: 70, textAlign: "center" }}>Rec.</th>
                                <th style={{ width: 70, textAlign: "center" }}>Final</th>
                              </tr>
                            </thead>
                            <tbody>
                              {d.avaliacoes.map((av) => (
                                <tr key={av.avaliacaoId}>
                                  <td>{av.nome}</td>
                                  <td style={{ textAlign: "center" }}>
                                    <span className="badge badge-secondary">{av.tipo}</span>
                                  </td>
                                  <td style={{ textAlign: "center", color: "var(--color-text-2)" }}>
                                    {fmt(av.peso)}
                                  </td>
                                  <td style={{ textAlign: "center" }}>{fmt(av.nota)}</td>
                                  <td style={{ textAlign: "center", color: "var(--color-warning)" }}>
                                    {fmt(av.notaRecuperacao)}
                                  </td>
                                  <td style={{ textAlign: "center", fontWeight: 700 }}>
                                    {fmt(av.notaFinal)}
                                  </td>
                                </tr>
                              ))}
                            </tbody>
                          </table>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            )}

            {/* Rodapé */}
            <div
              style={{
                marginTop: 20,
                paddingTop: 14,
                borderTop: "1px solid var(--color-border)",
                textAlign: "center",
                color: "var(--color-text-2)",
                fontSize: 12,
              }}
            >
              Documento gerado em {new Date().toLocaleDateString("pt-BR", { timeZone: "America/Sao_Paulo" })} às{" "}
              {new Date().toLocaleTimeString("pt-BR", { timeZone: "America/Sao_Paulo" })}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
