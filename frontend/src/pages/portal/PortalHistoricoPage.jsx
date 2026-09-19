import { useState, useEffect, useCallback } from "react";
import { accessApi, comoLista, qs } from "../../services/accessCadastrosApi";
import { Icon } from "../../components/Icon";
import { LinhasEstado } from "../../components/access/EstadoLista";
import { Feedback } from "../../components/access/Feedback";
import {
  formatarData,
  formatarHora,
  formatarDuracao,
  hojeIso,
  isoComDiferencaDeDias,
  calcularCarga,
} from "../../utils/tempo";
import "../../styles/accessCadastros.css";

export function PortalHistoricoPage() {
  const [filhos, setFilhos] = useState([]);
  const [alunoId, setAlunoId] = useState("");
  const [inicio, setInicio] = useState(isoComDiferencaDeDias(-14));
  const [fim, setFim] = useState(hojeIso());

  const [dias, setDias] = useState([]);
  const [carregando, setCarregando] = useState(false);
  const [erro, setErro] = useState("");
  const [erroFiltro, setErroFiltro] = useState("");
  const [consultou, setConsultou] = useState(false);

  useEffect(() => {
    accessApi.get("/access/portal/meus-alunos").then((r) => {
      const lista = r.ok ? comoLista(r.data) : [];
      setFilhos(lista);
      if (lista.length > 0) setAlunoId(lista[0].alunoId || lista[0].id);
    });
  }, []);

  const consultar = useCallback(async () => {
    if (!alunoId) {
      setErroFiltro("Escolha o aluno.");
      return;
    }
    if (fim < inicio) {
      setErroFiltro("A data final não pode ser anterior à inicial.");
      return;
    }
    setErroFiltro("");
    setCarregando(true);
    setConsultou(true);
    const r = await accessApi.get(`/access/portal/aluno/${alunoId}/historico?${qs({ inicio, fim })}`);
    if (r.ok) {
      setDias(comoLista(r.data?.dias ?? r.data));
      setErro("");
    } else {
      setDias([]);
      setErro(r.erro);
    }
    setCarregando(false);
  }, [alunoId, inicio, fim]);

  useEffect(() => {
    if (alunoId) consultar();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [alunoId]);

  const realizado = (d) =>
    typeof d.permanenciaMinutos === "number" ? d.permanenciaMinutos : calcularCarga(d.entrada, d.saida);

  const totalPeriodo = dias.reduce((s, d) => s + (realizado(d) || 0), 0);

  return (
    <div>
      <h1 className="ac-portal-titulo">Histórico</h1>
      <p className="ac-portal-sub">Entradas, saídas e permanência dia a dia.</p>

      <div className="card mb-4">
        <div className="card-body">
          <div className="filter-bar">
            <div className="form-field" style={{ flex: 1 }}>
              <label className="form-label">Aluno</label>
              <select className="form-select" value={alunoId} onChange={(e) => setAlunoId(e.target.value)}>
                {filhos.length === 0 && <option value="">Nenhum aluno vinculado</option>}
                {filhos.map((f) => (
                  <option key={f.alunoId || f.id} value={f.alunoId || f.id}>
                    {f.nome}
                  </option>
                ))}
              </select>
            </div>
            <div className="form-field">
              <label className="form-label">De</label>
              <input className="form-input" type="date" value={inicio} onChange={(e) => setInicio(e.target.value)} />
            </div>
            <div className="form-field">
              <label className="form-label">Até</label>
              <input className="form-input" type="date" value={fim} onChange={(e) => setFim(e.target.value)} />
            </div>
            <div className="form-field">
              <label className="form-label">&nbsp;</label>
              <button className="btn btn-brand" onClick={consultar}>
                <Icon name="Search" size={14} /> Ver
              </button>
            </div>
          </div>
          {erroFiltro && <span className="form-error">{erroFiltro}</span>}
        </div>
      </div>

      {erro && <Feedback tipo="erro" mensagem={erro} />}

      {dias.length > 0 && (
        <p className="ac-meta mb-2">
          Permanência total no período: <strong>{formatarDuracao(totalPeriodo)}</strong> em {dias.length} dia(s).
        </p>
      )}

      <div className="table-wrapper">
        <table className="data-table">
          <thead>
            <tr>
              <th>Data</th>
              <th>Entrada</th>
              <th>Saída</th>
              <th>Permanência</th>
              <th>Jornada</th>
              <th>Excedente</th>
            </tr>
          </thead>
          <tbody>
            <LinhasEstado
              colSpan={6}
              carregando={carregando}
              erro={erro}
              vazio={dias.length === 0}
              icone="Calendar"
              tituloVazio={consultou ? "Nenhum dia no período" : "Escolha o período"}
              textoVazio={
                consultou
                  ? "Não há registro de entrada nesse intervalo."
                  : "Os dias com registro aparecem aqui."
              }
              onTentarNovamente={consultou ? consultar : undefined}
            />
            {!carregando &&
              !erro &&
              dias.map((d) => {
                const ex = d.excedenteMinutos ?? null;
                const inconsistente = d.status === "INCONSISTENTE";
                return (
                  <tr key={d.id || d.data}>
                    <td>
                      <strong>{formatarData(d.data)}</strong>
                      {inconsistente && (
                        <div className="ac-meta">registro em conferência pela escola</div>
                      )}
                    </td>
                    <td className="ac-mono">{formatarHora(d.entrada)}</td>
                    <td className="ac-mono">{formatarHora(d.saida)}</td>
                    <td>{formatarDuracao(realizado(d))}</td>
                    <td className="td-muted">{formatarDuracao(d.jornadaContratadaMinutos)}</td>
                    <td className={ex > 0 ? "text-warning font-bold" : "td-muted"}>
                      {ex > 0 ? formatarDuracao(ex) : "—"}
                    </td>
                  </tr>
                );
              })}
          </tbody>
        </table>
      </div>
    </div>
  );
}
