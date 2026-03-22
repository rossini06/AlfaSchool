import { useState, useEffect, useRef, useCallback } from "react";
import { api } from "../services/api";
import { Icon } from "../components/Icon";

// ─── Helper Functions ────────────────────────────────────────────────────────

function situacaoBadgeClass(situacao) {
  switch (situacao) {
    case "APROVADO":
      return "badge bg-success";
    case "REPROVADO":
    case "REPROVADO_FREQUENCIA":
    case "REPROVADO_NOTA":
      return "badge bg-danger";
    case "RECUPERACAO":
      return "badge bg-warning text-dark";
    default:
      return "badge bg-secondary";
  }
}

function situacaoLabel(situacao) {
  switch (situacao) {
    case "APROVADO":
      return "Aprovado";
    case "REPROVADO":
      return "Reprovado";
    case "REPROVADO_FREQUENCIA":
      return "Reprovado (Frequência)";
    case "REPROVADO_NOTA":
      return "Reprovado (Nota)";
    case "RECUPERACAO":
      return "Em Recuperação";
    case "CURSANDO":
      return "Cursando";
    default:
      return situacao || "—";
  }
}

function formatNumber(n) {
  if (n === null || n === undefined) return "—";
  return Number(n).toFixed(2).replace(".", ",");
}

// ─── Page Component ──────────────────────────────────────────────────────────

export function BoletimPage() {
  const [matriculas, setMatriculas] = useState([]);
  const [turmas, setTurmas] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const printRef = useRef();

  // Filtros
  const [turmaId, setTurmaId] = useState("");
  const [matriculaId, setMatriculaId] = useState("");
  const [periodo, setPeriodo] = useState("");

  // Boletim
  const [boletim, setBoletim] = useState(null);

  // Definir callbacks ANTES dos useEffects que os usam
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

  useEffect(() => {
    loadTurmas();
  }, []);

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

    try {
      let url = `/boletim/${matriculaId}`;
      if (periodo) url += `?periodo=${periodo}`;
      const data = await api.get(url);
      setBoletim(data);
    } catch (e) {
      setError(e?.message || "Erro ao gerar boletim");
    } finally {
      setLoading(false);
    }
  };

  const handlePrint = () => {
    const content = printRef.current;
    if (!content) return;

    const printWindow = window.open("", "_blank");
    printWindow.document.write(`
      <html>
        <head>
          <title>Boletim - ${boletim?.alunoNome}</title>
          <style>
            body { font-family: Arial, sans-serif; margin: 20px; }
            .header { text-align: center; margin-bottom: 20px; }
            .header h1 { margin: 0; font-size: 18px; }
            .header p { margin: 5px 0; font-size: 12px; }
            .info { margin-bottom: 15px; }
            .info p { margin: 3px 0; font-size: 11px; }
            table { width: 100%; border-collapse: collapse; font-size: 10px; margin-top: 10px; }
            th, td { border: 1px solid #333; padding: 5px; text-align: center; }
            th { background: #f0f0f0; }
            .text-left { text-align: left; }
            .situacao-aprovado { color: green; font-weight: bold; }
            .situacao-reprovado { color: red; font-weight: bold; }
            .situacao-recuperacao { color: orange; font-weight: bold; }
            .footer { margin-top: 30px; font-size: 10px; text-align: center; }
            @media print {
              body { margin: 0; }
            }
          </style>
        </head>
        <body>
          ${content.innerHTML}
          <script>window.onload = function() { window.print(); window.close(); }</script>
        </body>
      </html>
    `);
    printWindow.document.close();
  };

  return (
    <>
      {/* Header */}
      <div className="page-header">
        <h1 className="page-title">
          <Icon name="FileSpreadsheet" size={24} />
          Boletim
        </h1>
      </div>

      {/* Filtros */}
      <div className="card mb-4">
        <div className="card-header">Selecione Aluno</div>
        <div className="card-body">
          <div className="row g-3">
            <div className="col-md-3">
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
            <div className="col-md-4">
              <label className="form-label">Aluno (Matrícula)</label>
              <select
                className="form-select"
                value={matriculaId}
                onChange={(e) => {
                  setMatriculaId(e.target.value);
                  setBoletim(null);
                }}
                disabled={!turmaId}
              >
                <option value="">Selecione...</option>
                {matriculas.map((m) => (
                  <option key={m.id} value={m.id}>
                    {m.alunoNome || m.numeroMatricula} - {m.numeroMatricula}
                  </option>
                ))}
              </select>
            </div>
            <div className="col-md-3">
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
            <div className="col-md-2 d-flex align-items-end gap-2">
              <button
                className="btn btn-primary flex-grow-1"
                disabled={!matriculaId || loading}
                onClick={gerarBoletim}
              >
                {loading ? (
                  <>
                    <span className="spinner-border spinner-border-sm me-1"></span>
                    Gerando...
                  </>
                ) : (
                  <>
                    <Icon name="FileText" size={16} /> Gerar
                  </>
                )}
              </button>
            </div>
          </div>
        </div>
      </div>

      {error && <div className="alert alert-danger">{error}</div>}

      {/* Boletim */}
      {boletim && (
        <div className="card">
          <div className="card-header d-flex justify-content-between align-items-center">
            <span>Boletim Escolar</span>
            <button
              className="btn btn-sm btn-outline-primary"
              onClick={handlePrint}
            >
              <Icon name="Printer" size={14} /> Imprimir
            </button>
          </div>
          <div className="card-body" ref={printRef}>
            <div className="header text-center mb-4">
              <h1 className="h4 mb-2">BOLETIM ESCOLAR</h1>
              <p className="text-muted mb-0">
                {boletim.cursoNome} - {boletim.turmaNome}
              </p>
            </div>

            <div className="row mb-4">
              <div className="col-md-6">
                <p className="mb-1">
                  <strong>Aluno:</strong> {boletim.alunoNome}
                </p>
                <p className="mb-1">
                  <strong>Matrícula:</strong> {boletim.numeroMatricula}
                </p>
                <p className="mb-1">
                  <strong>Turma:</strong> {boletim.turmaNome}
                </p>
              </div>
              <div className="col-md-6">
                <p className="mb-1">
                  <strong>Ano Letivo:</strong> {boletim.anoLetivo}
                </p>
                <p className="mb-1">
                  <strong>Turno:</strong> {boletim.turno}
                </p>
                <p className="mb-1">
                  <strong>Nível:</strong> {boletim.nivel || "—"}
                </p>
              </div>
            </div>

            {/* Resumo */}
            <div className="row mb-4">
              <div className="col-md-3">
                <div className="card bg-light">
                  <div className="card-body text-center py-3">
                    <div className="h4 mb-0">
                      {formatNumber(boletim.mediaGeral)}
                    </div>
                    <small className="text-muted">Média Geral</small>
                  </div>
                </div>
              </div>
              <div className="col-md-3">
                <div className="card bg-light">
                  <div className="card-body text-center py-3">
                    <div className="h4 mb-0">
                      {formatNumber(boletim.frequenciaGeral)}%
                    </div>
                    <small className="text-muted">Frequência Geral</small>
                  </div>
                </div>
              </div>
              <div className="col-md-3">
                <div className="card bg-light">
                  <div className="card-body text-center py-3">
                    <div className="h4 mb-0">{boletim.totalDisciplinas}</div>
                    <small className="text-muted">Disciplinas</small>
                  </div>
                </div>
              </div>
              <div className="col-md-3">
                <div className="card bg-light">
                  <div className="card-body text-center py-3">
                    <span className={situacaoBadgeClass(boletim.situacaoGeral)}>
                      {situacaoLabel(boletim.situacaoGeral)}
                    </span>
                    <br />
                    <small className="text-muted">Situação</small>
                  </div>
                </div>
              </div>
            </div>

            {/* Tabela de Disciplinas */}
            <div className="table-responsive">
              <table className="table table-bordered table-sm">
                <thead className="table-light">
                  <tr>
                    <th className="text-left">Disciplina</th>
                    <th style={{ width: 80 }}>Média</th>
                    <th style={{ width: 80 }}>Freq. %</th>
                    <th style={{ width: 80 }}>Faltas</th>
                    <th style={{ width: 100 }}>Situação</th>
                  </tr>
                </thead>
                <tbody>
                  {boletim.disciplinas?.map((d) => (
                    <tr key={d.disciplinaId}>
                      <td className="text-left">
                        {d.disciplinaNome}
                        {d.codigo && (
                          <small className="text-muted ms-2">
                            ({d.codigo})
                          </small>
                        )}
                      </td>
                      <td className="text-center">
                        {d.conceito || formatNumber(d.media)}
                      </td>
                      <td className="text-center">
                        {formatNumber(d.percentualFrequencia)}%
                      </td>
                      <td className="text-center">{d.faltas || 0}</td>
                      <td className="text-center">
                        <span className={situacaoBadgeClass(d.situacao)}>
                          {situacaoLabel(d.situacao)}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {/* Detalhes das Avaliações (opcional, expansível) */}
            {boletim.disciplinas?.some((d) => d.avaliacoes?.length > 0) && (
              <details className="mt-4">
                <summary className="cursor-pointer text-primary mb-2">
                  Ver detalhes das avaliações
                </summary>
                {boletim.disciplinas?.map(
                  (d) =>
                    d.avaliacoes?.length > 0 && (
                      <div key={d.disciplinaId} className="mb-3">
                        <strong>{d.disciplinaNome}</strong>
                        <table className="table table-sm table-bordered mt-1">
                          <thead className="table-light">
                            <tr>
                              <th>Avaliação</th>
                              <th style={{ width: 80 }}>Tipo</th>
                              <th style={{ width: 60 }}>Peso</th>
                              <th style={{ width: 60 }}>Nota</th>
                              <th style={{ width: 60 }}>Rec.</th>
                              <th style={{ width: 60 }}>Final</th>
                            </tr>
                          </thead>
                          <tbody>
                            {d.avaliacoes.map((av) => (
                              <tr key={av.avaliacaoId}>
                                <td>{av.nome}</td>
                                <td className="text-center">{av.tipo}</td>
                                <td className="text-center">
                                  {formatNumber(av.peso)}
                                </td>
                                <td className="text-center">
                                  {formatNumber(av.nota)}
                                </td>
                                <td className="text-center">
                                  {formatNumber(av.notaRecuperacao)}
                                </td>
                                <td className="text-center fw-bold">
                                  {formatNumber(av.notaFinal)}
                                </td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                    ),
                )}
              </details>
            )}

            <div className="footer text-center text-muted mt-4 pt-3 border-top">
              <small>
                Documento gerado em {new Date().toLocaleDateString("pt-BR")} às{" "}
                {new Date().toLocaleTimeString("pt-BR")}
              </small>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
