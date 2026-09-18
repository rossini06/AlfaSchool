import { useState, useEffect, useCallback } from "react";
import { accessApi, carregarAuxiliar, comoLista, qs } from "../../services/accessCadastrosApi";
import { Icon } from "../../components/Icon";
import { LinhasEstado } from "../../components/access/EstadoLista";
import { Feedback, Aviso } from "../../components/access/Feedback";
import { exportarCsv } from "../../utils/exportCsv";
import {
  formatarData,
  formatarDataHora,
  formatarHora,
  formatarDuracao,
  hojeIso,
  isoComDiferencaDeDias,
} from "../../utils/tempo";
import "../../styles/accessCadastros.css";

const dur = (v) => (v === null || v === undefined || v === "" ? "—" : formatarDuracao(Number(v)));

/**
 * Cada relatório declara seu endpoint, as colunas da tela e os filtros extras
 * que aceita. A exportação usa as mesmas colunas — o CSV é o que está na tela.
 */
const RELATORIOS = [
  {
    chave: "movimentacoes",
    label: "Movimentações",
    icone: "Activity",
    descricao: "Toda passagem registrada pelos leitores no período.",
    filtros: ["turma", "aluno", "portaria"],
    colunas: [
      { key: "dataHora", label: "Data e hora", format: formatarDataHora },
      { key: "alunoNome", label: "Aluno" },
      { key: "turmaNome", label: "Turma" },
      { key: "tipo", label: "Tipo" },
      { key: "portariaNome", label: "Portaria" },
      { key: "equipamentoNome", label: "Equipamento" },
    ],
  },
  {
    chave: "permanencia",
    label: "Permanência por aluno",
    icone: "Clock",
    descricao: "Tempo realizado contra a jornada contratada, consolidado por aluno.",
    filtros: ["turma", "aluno"],
    colunas: [
      { key: "alunoNome", label: "Aluno" },
      { key: "turmaNome", label: "Turma" },
      { key: "diasApurados", label: "Dias apurados" },
      { key: "minutosRealizados", label: "Tempo realizado", format: dur },
      { key: "minutosContratados", label: "Jornada contratada", format: dur },
      { key: "excedenteMinutos", label: "Excedente", format: dur },
      { key: "diasInconsistentes", label: "Dias inconsistentes" },
    ],
  },
  {
    chave: "excedentes",
    label: "Excedentes",
    icone: "TrendingUp",
    descricao: "Dias em que o aluno passou da jornada contratada — base para cobrança.",
    filtros: ["turma", "aluno"],
    colunas: [
      { key: "data", label: "Data", format: formatarData },
      { key: "alunoNome", label: "Aluno" },
      { key: "turmaNome", label: "Turma" },
      { key: "jornadaNome", label: "Jornada" },
      { key: "regraExcedente", label: "Regra" },
      { key: "excedenteMinutos", label: "Excedente", format: dur },
    ],
  },
  {
    chave: "retiradas",
    label: "Retiradas",
    icone: "UserCheck",
    descricao: "Quem retirou cada aluno, quando e sob qual autorização.",
    filtros: ["turma", "aluno", "portaria"],
    colunas: [
      { key: "dataHora", label: "Data e hora", format: formatarDataHora },
      { key: "alunoNome", label: "Aluno" },
      { key: "pessoaNome", label: "Retirado por" },
      { key: "parentesco", label: "Parentesco" },
      { key: "portariaNome", label: "Portaria" },
      { key: "statusAutorizacao", label: "Autorização" },
    ],
  },
  {
    chave: "tempo-espera",
    label: "Tempo de espera",
    icone: "Clock",
    descricao: "Intervalo entre o chamado do aluno e a entrega na portaria.",
    filtros: ["turma", "portaria"],
    colunas: [
      { key: "data", label: "Data", format: formatarData },
      { key: "alunoNome", label: "Aluno" },
      { key: "chamadoEm", label: "Chamado", format: formatarHora },
      { key: "entregueEm", label: "Entregue", format: formatarHora },
      { key: "esperaMinutos", label: "Espera", format: dur },
      { key: "portariaNome", label: "Portaria" },
    ],
  },
  {
    chave: "acessos-negados",
    label: "Acessos negados",
    icone: "XCircle",
    descricao: "Tentativas barradas pelo leitor e o motivo da recusa.",
    filtros: ["portaria"],
    colunas: [
      { key: "dataHora", label: "Data e hora", format: formatarDataHora },
      { key: "pessoaNome", label: "Pessoa" },
      { key: "alunoNome", label: "Aluno" },
      { key: "motivo", label: "Motivo" },
      { key: "portariaNome", label: "Portaria" },
      { key: "equipamentoNome", label: "Equipamento" },
    ],
  },
  {
    chave: "ocorrencias",
    label: "Ocorrências",
    icone: "AlertCircle",
    descricao: "Registros abertos pela operação e como foram tratados.",
    filtros: ["turma", "aluno"],
    colunas: [
      { key: "dataHora", label: "Data e hora", format: formatarDataHora },
      { key: "tipo", label: "Tipo" },
      { key: "gravidade", label: "Gravidade" },
      { key: "alunoNome", label: "Aluno" },
      { key: "status", label: "Status" },
      { key: "tratadaPor", label: "Tratada por" },
    ],
  },
];

export function RelatoriosAccessPage() {
  const [chave, setChave] = useState(RELATORIOS[0].chave);
  const relatorio = RELATORIOS.find((r) => r.chave === chave);

  const [inicio, setInicio] = useState(isoComDiferencaDeDias(-30));
  const [fim, setFim] = useState(hojeIso());
  const [turmaId, setTurmaId] = useState("");
  const [alunoId, setAlunoId] = useState("");
  const [portariaId, setPortariaId] = useState("");

  const [turmas, setTurmas] = useState([]);
  const [alunos, setAlunos] = useState([]);
  const [portarias, setPortarias] = useState([]);

  const [linhas, setLinhas] = useState([]);
  const [carregando, setCarregando] = useState(false);
  const [erro, setErro] = useState("");
  const [consultou, setConsultou] = useState(false);
  const [erroFiltro, setErroFiltro] = useState("");

  useEffect(() => {
    carregarAuxiliar("/turmas?size=300").then(setTurmas);
    carregarAuxiliar("/alunos?size=500").then(setAlunos);
    carregarAuxiliar("/access/portarias?size=200").then(setPortarias);
  }, []);

  const consultar = useCallback(async () => {
    if (fim < inicio) {
      setErroFiltro("A data final não pode ser anterior à inicial.");
      return;
    }
    setErroFiltro("");
    setCarregando(true);
    setErro("");
    setConsultou(true);
    const params = { inicio, fim };
    if (relatorio.filtros.includes("turma") && turmaId) params.turmaId = turmaId;
    if (relatorio.filtros.includes("aluno") && alunoId) params.alunoId = alunoId;
    if (relatorio.filtros.includes("portaria") && portariaId) params.portariaId = portariaId;

    const r = await accessApi.get(`/access/relatorios/${chave}?${qs(params)}`);
    if (r.ok) setLinhas(comoLista(r.data));
    else {
      setLinhas([]);
      setErro(r.erro);
    }
    setCarregando(false);
  }, [chave, inicio, fim, turmaId, alunoId, portariaId, relatorio]);

  const trocarRelatorio = (nova) => {
    setChave(nova);
    setLinhas([]);
    setConsultou(false);
    setErro("");
  };

  const exportar = () => exportarCsv(`relatorio-${chave}`, relatorio.colunas, linhas);

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1 className="page-title">Relatórios de Acesso</h1>
          <p className="page-subtitle">Consultas do módulo com exportação em CSV</p>
        </div>
        <button className="btn btn-secondary" onClick={exportar} disabled={linhas.length === 0}>
          <Icon name="Download" size={14} /> Exportar CSV
        </button>
      </div>

      <div className="tabs-header mb-4">
        {RELATORIOS.map((r) => (
          <button
            key={r.chave}
            className={`tab-btn ${chave === r.chave ? "active" : ""}`}
            onClick={() => trocarRelatorio(r.chave)}
          >
            <Icon name={r.icone} size={13} /> {r.label}
          </button>
        ))}
      </div>

      <Aviso tipo="info">{relatorio.descricao}</Aviso>

      <div className="card">
        <div className="card-body">
          <div className="filter-bar">
            <div className="form-field">
              <label className="form-label">De</label>
              <input className="form-input" type="date" value={inicio} onChange={(e) => setInicio(e.target.value)} />
            </div>
            <div className="form-field">
              <label className="form-label">Até</label>
              <input className="form-input" type="date" value={fim} onChange={(e) => setFim(e.target.value)} />
            </div>
            {relatorio.filtros.includes("turma") && (
              <div className="form-field" style={{ flex: 1 }}>
                <label className="form-label">Turma</label>
                <select className="form-select" value={turmaId} onChange={(e) => setTurmaId(e.target.value)}>
                  <option value="">Todas</option>
                  {turmas.map((t) => (
                    <option key={t.id} value={t.id}>
                      {t.nome}
                    </option>
                  ))}
                </select>
              </div>
            )}
            {relatorio.filtros.includes("aluno") && (
              <div className="form-field" style={{ flex: 1 }}>
                <label className="form-label">Aluno</label>
                <select className="form-select" value={alunoId} onChange={(e) => setAlunoId(e.target.value)}>
                  <option value="">Todos</option>
                  {alunos.map((a) => (
                    <option key={a.id} value={a.id}>
                      {a.nome}
                    </option>
                  ))}
                </select>
              </div>
            )}
            {relatorio.filtros.includes("portaria") && (
              <div className="form-field" style={{ flex: 1 }}>
                <label className="form-label">Portaria</label>
                <select className="form-select" value={portariaId} onChange={(e) => setPortariaId(e.target.value)}>
                  <option value="">Todas</option>
                  {portarias.map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.nome}
                    </option>
                  ))}
                </select>
              </div>
            )}
            <div className="form-field">
              <label className="form-label">&nbsp;</label>
              <button className="btn btn-brand" onClick={consultar}>
                <Icon name="Search" size={14} /> Gerar
              </button>
            </div>
          </div>
          {erroFiltro && <span className="form-error">{erroFiltro}</span>}
        </div>
      </div>

      {erro && <Feedback tipo="erro" mensagem={erro} />}

      <div className="table-wrapper">
        <table className="data-table">
          <thead>
            <tr>
              {relatorio.colunas.map((c) => (
                <th key={c.key}>{c.label}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            <LinhasEstado
              colSpan={relatorio.colunas.length}
              carregando={carregando}
              erro={erro}
              vazio={linhas.length === 0}
              icone={relatorio.icone}
              tituloVazio={consultou ? "Nenhum registro no período" : "Defina o período e gere o relatório"}
              textoVazio={
                consultou
                  ? "Amplie o intervalo ou revise os filtros."
                  : "Os dados aparecem aqui e podem ser exportados em CSV."
              }
              onTentarNovamente={consultou ? consultar : undefined}
            />
            {!carregando &&
              !erro &&
              linhas.map((linha, i) => (
                <tr key={linha.id || i}>
                  {relatorio.colunas.map((c) => (
                    <td key={c.key} className={c.key.includes("Nome") || c.key === "data" ? "" : "td-muted"}>
                      {c.format ? c.format(linha[c.key], linha) : linha[c.key] ?? "—"}
                    </td>
                  ))}
                </tr>
              ))}
          </tbody>
        </table>
      </div>

      {linhas.length > 0 && (
        <p className="ac-meta mt-2">
          {linhas.length} registro(s) — o CSV exportado contém exatamente estas linhas e colunas.
        </p>
      )}
    </div>
  );
}
