import { useState, useEffect, useCallback, useMemo } from "react";
import { accessApi, carregarAuxiliar, comoLista, qs } from "../../services/accessCadastrosApi";
import { Icon } from "../../components/Icon";
import { LinhasEstado } from "../../components/access/EstadoLista";
import { Feedback } from "../../components/access/Feedback";
import { exportarCsv } from "../../utils/exportCsv";
import { formatarHora, formatarDuracao, minutosDesde } from "../../utils/tempo";
import "../../styles/accessCadastros.css";

const INTERVALO_MS = 60000;

export function PresentesAgoraPage() {
  const [itens, setItens] = useState([]);
  const [turmas, setTurmas] = useState([]);
  const [salas, setSalas] = useState([]);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState("");
  const [atualizadoEm, setAtualizadoEm] = useState(null);

  const [filtroTurma, setFiltroTurma] = useState("");
  const [filtroSala, setFiltroSala] = useState("");
  const [busca, setBusca] = useState("");
  const [auto, setAuto] = useState(true);

  const carregar = useCallback(
    async (silencioso = false) => {
      if (!silencioso) setCarregando(true);
      const r = await accessApi.get(`/access/presentes-agora?${qs({ turmaId: filtroTurma, salaId: filtroSala })}`);
      if (r.ok) {
        setItens(comoLista(r.data));
        setErro("");
        setAtualizadoEm(new Date());
      } else if (!silencioso) {
        setItens([]);
        setErro(r.erro);
      }
      setCarregando(false);
    },
    [filtroTurma, filtroSala]
  );

  useEffect(() => {
    carregar();
    // recarrega ao trocar o recorte de turma ou sala
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [filtroTurma, filtroSala]);

  useEffect(() => {
    carregarAuxiliar("/turmas?size=300").then(setTurmas);
    carregarAuxiliar("/access/salas?size=300").then(setSalas);
  }, []);

  useEffect(() => {
    if (!auto) return undefined;
    const id = setInterval(() => carregar(true), INTERVALO_MS);
    return () => clearInterval(id);
  }, [auto, carregar]);

  const filtrados = useMemo(() => {
    const termo = busca.trim().toLowerCase();
    if (!termo) return itens;
    return itens.filter((i) => (i.alunoNome || "").toLowerCase().includes(termo));
  }, [itens, busca]);

  const porTurma = useMemo(() => {
    const mapa = new Map();
    filtrados.forEach((i) => {
      const chave = i.turmaNome || "Sem turma";
      mapa.set(chave, (mapa.get(chave) || 0) + 1);
    });
    return [...mapa.entries()].sort((a, b) => a[0].localeCompare(b[0]));
  }, [filtrados]);

  const exportar = () =>
    exportarCsv(
      "presentes-agora",
      [
        { key: "alunoNome", label: "Aluno" },
        { key: "turmaNome", label: "Turma" },
        { key: "salaNome", label: "Sala" },
        { key: "entrada", label: "Entrada", format: formatarHora },
      ],
      filtrados
    );

  return (
    <div className="page">
      <div className="page-header ac-no-print">
        <div>
          <h1 className="page-title">Presentes Agora</h1>
          <p className="page-subtitle">
            Quem está na unidade neste momento
            {atualizadoEm && ` · atualizado às ${atualizadoEm.toLocaleTimeString("pt-BR").slice(0, 5)}`}
          </p>
        </div>
        <div className="ac-linha-acoes">
          <button className="btn btn-secondary" onClick={() => carregar()}>
            <Icon name="RefreshCw" size={14} /> Atualizar
          </button>
          <button className="btn btn-secondary" onClick={exportar} disabled={filtrados.length === 0}>
            <Icon name="Download" size={14} /> CSV
          </button>
          <button className="btn btn-brand" onClick={() => window.print()}>
            <Icon name="Printer" size={14} /> Imprimir
          </button>
        </div>
      </div>

      <div className="ac-print-cabecalho">
        <h2>Lista de presentes — {new Date().toLocaleString("pt-BR")}</h2>
        <p>
          {filtrados.length} aluno(s)
          {filtroTurma ? ` · turma ${turmas.find((t) => t.id === filtroTurma)?.nome || ""}` : ""}
          {filtroSala ? ` · sala ${salas.find((s) => s.id === filtroSala)?.nome || ""}` : ""}
        </p>
      </div>

      {erro && <Feedback tipo="erro" mensagem={erro} />}

      <div className="kpi-grid mb-4">
        <div className="kpi-card">
          <div className="kpi-label">Alunos presentes</div>
          <div className="kpi-value">{carregando ? "—" : filtrados.length}</div>
        </div>
        {porTurma.slice(0, 3).map(([nome, qtd]) => (
          <div className="kpi-card" key={nome}>
            <div className="kpi-label">{nome}</div>
            <div className="kpi-value">{qtd}</div>
          </div>
        ))}
      </div>

      <div className="card ac-no-print">
        <div className="card-body">
          <div className="filter-bar">
            <div className="form-field" style={{ flex: 1 }}>
              <input
                className="form-input"
                placeholder="Buscar aluno na lista..."
                value={busca}
                onChange={(e) => setBusca(e.target.value)}
              />
            </div>
            <div className="form-field">
              <select className="form-select" value={filtroTurma} onChange={(e) => setFiltroTurma(e.target.value)}>
                <option value="">Todas as turmas</option>
                {turmas.map((t) => (
                  <option key={t.id} value={t.id}>
                    {t.nome}
                  </option>
                ))}
              </select>
            </div>
            <div className="form-field">
              <select className="form-select" value={filtroSala} onChange={(e) => setFiltroSala(e.target.value)}>
                <option value="">Todas as salas</option>
                {salas.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.nome}
                  </option>
                ))}
              </select>
            </div>
            <label className="form-checkbox">
              <input type="checkbox" checked={auto} onChange={(e) => setAuto(e.target.checked)} />
              <span>Atualizar sozinho</span>
            </label>
          </div>
        </div>
      </div>

      <div className="table-wrapper">
        <table className="data-table">
          <thead>
            <tr>
              <th>Aluno</th>
              <th>Turma</th>
              <th>Sala</th>
              <th>Entrada</th>
              <th>Tempo na unidade</th>
            </tr>
          </thead>
          <tbody>
            <LinhasEstado
              colSpan={5}
              carregando={carregando}
              erro={erro}
              vazio={filtrados.length === 0}
              icone="Users"
              tituloVazio="Ninguém presente no momento"
              textoVazio="Assim que houver entrada registrada, o aluno aparece aqui."
              onTentarNovamente={() => carregar()}
            />
            {!carregando &&
              !erro &&
              filtrados.map((i) => (
                <tr key={i.alunoId || `${i.alunoNome}-${i.entrada}`}>
                  <td>
                    <strong>{i.alunoNome}</strong>
                  </td>
                  <td className="td-muted">{i.turmaNome || "—"}</td>
                  <td className="td-muted">{i.salaNome || "—"}</td>
                  <td className="ac-mono">{formatarHora(i.entrada)}</td>
                  <td className="td-muted">{formatarDuracao(minutosDesde(i.entrada))}</td>
                </tr>
              ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
