import { useState, useEffect, useCallback, useMemo } from "react";
import { accessApi, carregarAuxiliar, comoLista, qs } from "../../services/accessCadastrosApi";
import { Modal } from "../../components/Modal";
import { Icon } from "../../components/Icon";
import { LinhasEstado } from "../../components/access/EstadoLista";
import { Feedback, Aviso } from "../../components/access/Feedback";
import { exportarCsv } from "../../utils/exportCsv";
import {
  formatarData,
  formatarHora,
  formatarDuracao,
  hojeIso,
  isoComDiferencaDeDias,
  calcularCarga,
} from "../../utils/tempo";
import "../../styles/accessCadastros.css";

const AJUSTE_VAZIO = { entrada: "", saida: "", motivo: "" };

function ehInconsistente(dia) {
  return dia.status === "INCONSISTENTE" || dia.inconsistente === true;
}

/** Minutos realizados: usa o que o servidor mandou ou calcula pela marcação. */
function realizado(dia) {
  if (typeof dia.minutosRealizados === "number") return dia.minutosRealizados;
  return calcularCarga(dia.entrada, dia.saida);
}

function contratado(dia) {
  return typeof dia.minutosContratados === "number" ? dia.minutosContratados : null;
}

function excedente(dia) {
  if (typeof dia.excedenteMinutos === "number") return dia.excedenteMinutos;
  const r = realizado(dia);
  const c = contratado(dia);
  if (r === null || c === null) return null;
  return Math.max(0, r - c);
}

export function PermanenciaPage() {
  const [alunos, setAlunos] = useState([]);
  const [alunoId, setAlunoId] = useState("");
  const [inicio, setInicio] = useState(isoComDiferencaDeDias(-30));
  const [fim, setFim] = useState(hojeIso());

  const [dados, setDados] = useState(null);
  const [carregando, setCarregando] = useState(false);
  const [erro, setErro] = useState("");
  const [consultou, setConsultou] = useState(false);
  const [erroFiltro, setErroFiltro] = useState("");

  const [diaSelecionado, setDiaSelecionado] = useState(null);
  const [ajuste, setAjuste] = useState(null); // { dia }
  const [formAjuste, setFormAjuste] = useState(AJUSTE_VAZIO);
  const [errosAjuste, setErrosAjuste] = useState({});
  const [salvandoAjuste, setSalvandoAjuste] = useState(false);
  const [erroAjuste, setErroAjuste] = useState("");
  const [feedback, setFeedback] = useState(null);

  useEffect(() => {
    carregarAuxiliar("/alunos?size=500").then(setAlunos);
  }, []);

  const consultar = useCallback(async () => {
    if (!alunoId) {
      setErroFiltro("Selecione o aluno para ver o extrato.");
      return;
    }
    if (fim < inicio) {
      setErroFiltro("A data final não pode ser anterior à inicial.");
      return;
    }
    setErroFiltro("");
    setCarregando(true);
    setErro("");
    setConsultou(true);
    const r = await accessApi.get(`/access/permanencia?${qs({ alunoId, inicio, fim })}`);
    if (r.ok) {
      const dias = comoLista(r.data?.dias ?? r.data);
      setDados({ ...(r.data || {}), dias });
      const ultimoOk = [...dias].reverse().find((d) => !ehInconsistente(d));
      setDiaSelecionado(ultimoOk || dias[dias.length - 1] || null);
    } else {
      setDados(null);
      setErro(r.erro);
    }
    setCarregando(false);
  }, [alunoId, inicio, fim]);

  const dias = useMemo(() => dados?.dias || [], [dados]);

  const totais = useMemo(() => {
    const validos = dias.filter((d) => !ehInconsistente(d));
    return {
      diasApurados: validos.length,
      diasInconsistentes: dias.length - validos.length,
      realizado: validos.reduce((s, d) => s + (realizado(d) || 0), 0),
      contratado: validos.reduce((s, d) => s + (contratado(d) || 0), 0),
      excedente: validos.reduce((s, d) => s + (excedente(d) || 0), 0),
    };
  }, [dias]);

  const abrirAjuste = (dia) => {
    setAjuste({ dia });
    setFormAjuste({
      entrada: formatarHora(dia.entrada) === "—" ? "" : formatarHora(dia.entrada),
      saida: formatarHora(dia.saida) === "—" ? "" : formatarHora(dia.saida),
      motivo: "",
    });
    setErrosAjuste({});
    setErroAjuste("");
  };

  const salvarAjuste = async () => {
    const e = {};
    if (!formAjuste.entrada) e.entrada = "Informe o horário de entrada.";
    if (!formAjuste.saida) e.saida = "Informe o horário de saída.";
    if (formAjuste.motivo.trim().length < 5)
      e.motivo = "Descreva o motivo do ajuste — ele fica registrado na auditoria.";
    setErrosAjuste(e);
    if (Object.keys(e).length > 0) return;

    setSalvandoAjuste(true);
    setErroAjuste("");
    const dia = ajuste.dia;
    const corpo = {
      alunoId,
      data: dia.data,
      entrada: formAjuste.entrada,
      saida: formAjuste.saida,
      motivo: formAjuste.motivo.trim(),
    };
    const r = dia.id
      ? await accessApi.post(`/access/permanencia/${dia.id}/ajuste`, corpo)
      : await accessApi.post(`/access/permanencia/ajuste`, corpo);
    setSalvandoAjuste(false);
    if (!r.ok) {
      setErroAjuste(r.erro);
      return;
    }
    setAjuste(null);
    setFeedback({ tipo: "sucesso", mensagem: `Dia ${formatarData(dia.data)} ajustado e reapurado.` });
    consultar();
  };

  const exportar = () => {
    const aluno = dados?.alunoNome || alunos.find((a) => a.id === alunoId)?.nome || "aluno";
    exportarCsv(
      `permanencia-${aluno.replace(/\s+/g, "-").toLowerCase()}`,
      [
        { key: "data", label: "Data", format: formatarData },
        { key: "entrada", label: "Entrada", format: formatarHora },
        { key: "saida", label: "Saída", format: formatarHora },
        { key: "realizado", label: "Tempo realizado (min)" },
        { key: "contratado", label: "Jornada contratada (min)" },
        { key: "excedente", label: "Excedente (min)" },
        { key: "status", label: "Situação" },
      ],
      dias.map((d) => ({
        data: d.data,
        entrada: d.entrada,
        saida: d.saida,
        realizado: realizado(d) ?? "",
        contratado: contratado(d) ?? "",
        excedente: excedente(d) ?? "",
        status: ehInconsistente(d) ? "INCONSISTENTE" : d.status || "OK",
      }))
    );
  };

  const cartaoApuracao = (dia) => {
    const r = realizado(dia);
    const c = contratado(dia);
    const ex = excedente(dia);
    const incons = ehInconsistente(dia);
    return (
      <div className="ac-apuracao">
        <div className="ac-apuracao-head">
          <div>
            <strong>{formatarData(dia.data)}</strong>
            <div className="ac-meta">
              {dados?.alunoNome || alunos.find((a) => a.id === alunoId)?.nome}
              {dados?.turmaNome ? ` · ${dados.turmaNome}` : ""}
              {dia.jornadaNome ? ` · ${dia.jornadaNome}` : ""}
            </div>
          </div>
          <div className="ac-linha-acoes">
            {incons && <span className="badge badge-danger">Inconsistente</span>}
            <button className="btn btn-secondary btn-sm" onClick={() => abrirAjuste(dia)}>
              <Icon name="Edit3" size={13} /> Ajuste manual
            </button>
          </div>
        </div>

        <div className="ac-apuracao-grid">
          <div className="ac-apuracao-item">
            <div className="ac-apuracao-rot">Entrada registrada</div>
            <div className="ac-apuracao-val">{formatarHora(dia.entrada)}</div>
          </div>
          <div className="ac-apuracao-item">
            <div className="ac-apuracao-rot">Saída registrada</div>
            <div className="ac-apuracao-val">{formatarHora(dia.saida)}</div>
          </div>
          <div className="ac-apuracao-item">
            <div className="ac-apuracao-rot">Jornada contratada</div>
            <div className="ac-apuracao-val">{formatarDuracao(c)}</div>
          </div>
          <div className="ac-apuracao-item">
            <div className="ac-apuracao-rot">Tempo realizado</div>
            <div className="ac-apuracao-val">{formatarDuracao(r)}</div>
          </div>
        </div>

        {incons ? (
          <div className="ac-faixa-excedente ac-faixa-inconsistente">
            <span>
              <Icon name="AlertCircle" size={14} /> Dia inconsistente — {dia.motivoInconsistencia || "marcação faltando ou fora de ordem"}
            </span>
            <span>fora dos totais</span>
          </div>
        ) : ex > 0 ? (
          <div className="ac-faixa-excedente">
            <span>Excedente sobre a jornada</span>
            <span>{formatarDuracao(ex)}</span>
          </div>
        ) : (
          <div className="ac-faixa-excedente ac-faixa-ok">
            <span>Dentro da jornada contratada</span>
            <span>sem excedente</span>
          </div>
        )}
      </div>
    );
  };

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1 className="page-title">Permanência</h1>
          <p className="page-subtitle">Extrato por aluno: tempo realizado contra a jornada contratada</p>
        </div>
        {dias.length > 0 && (
          <button className="btn btn-secondary" onClick={exportar}>
            <Icon name="Download" size={14} /> Exportar CSV
          </button>
        )}
      </div>

      <Feedback {...(feedback || {})} onFechar={() => setFeedback(null)} />

      <div className="card">
        <div className="card-body">
          <div className="filter-bar">
            <div className="form-field" style={{ flex: 1 }}>
              <label className="form-label required">Aluno</label>
              <select
                className={`form-select ${erroFiltro && !alunoId ? "error" : ""}`}
                value={alunoId}
                onChange={(e) => {
                  setAlunoId(e.target.value);
                  setErroFiltro("");
                }}
              >
                <option value="">Selecione o aluno</option>
                {alunos.map((a) => (
                  <option key={a.id} value={a.id}>
                    {a.nome}
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
                <Icon name="Search" size={14} /> Consultar
              </button>
            </div>
          </div>
          {erroFiltro && <span className="form-error">{erroFiltro}</span>}
        </div>
      </div>

      {erro && <Feedback tipo="erro" mensagem={erro} />}

      {!consultou && !carregando && (
        <Aviso tipo="info">
          Escolha o aluno e o período para ver o extrato. O excedente é calculado dia a dia, segundo a
          regra da jornada vigente naquele dia.
        </Aviso>
      )}

      {carregando && (
        <div className="card">
          <div className="card-body">
            <div className="skeleton skeleton-row" />
            <div className="skeleton skeleton-row mt-2" />
            <div className="skeleton skeleton-row mt-2" />
          </div>
        </div>
      )}

      {!carregando && consultou && !erro && (
        <>
          {diaSelecionado && <div className="mb-4">{cartaoApuracao(diaSelecionado)}</div>}

          <div className="kpi-grid mb-4">
            <div className="kpi-card">
              <div className="kpi-label">Dias apurados</div>
              <div className="kpi-value">{totais.diasApurados}</div>
            </div>
            <div className="kpi-card">
              <div className="kpi-label">Tempo realizado</div>
              <div className="kpi-value">{formatarDuracao(totais.realizado)}</div>
            </div>
            <div className="kpi-card">
              <div className="kpi-label">Jornada contratada</div>
              <div className="kpi-value">{formatarDuracao(totais.contratado)}</div>
            </div>
            <div className="kpi-card">
              <div className="kpi-label">Excedente acumulado</div>
              <div className="kpi-value text-warning">{formatarDuracao(totais.excedente)}</div>
            </div>
          </div>

          {totais.diasInconsistentes > 0 && (
            <Aviso tipo="erro" titulo={`${totais.diasInconsistentes} dia(s) inconsistente(s) fora dos totais`}>
              Esses dias ficaram sem par de marcação ou com marcações fora de ordem. Eles{" "}
              <strong>não entram</strong> em nenhum total acima e continuam listados abaixo até alguém
              corrigir — use o ajuste manual informando o motivo.
            </Aviso>
          )}

          <div className="table-wrapper">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Data</th>
                  <th>Entrada</th>
                  <th>Saída</th>
                  <th>Tempo realizado</th>
                  <th>Jornada contratada</th>
                  <th>Excedente</th>
                  <th>Situação</th>
                  <th>Ações</th>
                </tr>
              </thead>
              <tbody>
                <LinhasEstado
                  colSpan={8}
                  carregando={false}
                  erro=""
                  vazio={dias.length === 0}
                  icone="Calendar"
                  tituloVazio="Nenhum dia no período"
                  textoVazio="Não há marcação registrada para este aluno no intervalo escolhido."
                />
                {dias.map((dia) => {
                  const incons = ehInconsistente(dia);
                  const ex = excedente(dia);
                  return (
                    <tr
                      key={dia.id || dia.data}
                      className={incons ? "ac-linha-inconsistente" : ""}
                      style={{ cursor: "pointer" }}
                      onClick={() => setDiaSelecionado(dia)}
                    >
                      <td>
                        <strong>{formatarData(dia.data)}</strong>
                      </td>
                      <td className="ac-mono">{formatarHora(dia.entrada)}</td>
                      <td className="ac-mono">{formatarHora(dia.saida)}</td>
                      <td className={incons ? "ac-fora-totais" : ""}>{formatarDuracao(realizado(dia))}</td>
                      <td className="td-muted">{formatarDuracao(contratado(dia))}</td>
                      <td className={incons ? "ac-fora-totais" : ex > 0 ? "text-warning font-bold" : "td-muted"}>
                        {ex > 0 ? formatarDuracao(ex) : "—"}
                      </td>
                      <td>
                        {incons ? (
                          <span className="badge badge-danger" title={dia.motivoInconsistencia || ""}>
                            Inconsistente
                          </span>
                        ) : (
                          <span className="badge badge-success">Apurado</span>
                        )}
                      </td>
                      <td onClick={(e) => e.stopPropagation()}>
                        <button
                          className={`btn btn-sm ${incons ? "btn-warning" : "btn-ghost"}`}
                          onClick={() => abrirAjuste(dia)}
                        >
                          <Icon name="Edit3" size={13} /> {incons ? "Corrigir" : "Ajustar"}
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </>
      )}

      <Modal
        isOpen={!!ajuste}
        onClose={() => setAjuste(null)}
        title={ajuste ? `Ajuste manual — ${formatarData(ajuste.dia.data)}` : ""}
        footer={
          <>
            <button className="btn btn-secondary" onClick={() => setAjuste(null)}>
              Cancelar
            </button>
            <button className="btn btn-brand" onClick={salvarAjuste} disabled={salvandoAjuste}>
              {salvandoAjuste ? "Salvando..." : "Salvar ajuste"}
            </button>
          </>
        }
      >
        <Aviso tipo="alerta">
          O ajuste substitui a marcação do equipamento e reapura o dia. Ele fica registrado com o seu
          usuário, o horário anterior e o motivo.
        </Aviso>
        <Feedback tipo="erro" mensagem={erroAjuste} />
        {ajuste && (
          <div className="form-grid">
            {ajuste.dia.motivoInconsistencia && (
              <p className="ac-meta">
                Motivo apontado pelo sistema: <strong>{ajuste.dia.motivoInconsistencia}</strong>
              </p>
            )}
            <div className="form-grid-2">
              <div className="form-field">
                <label className="form-label required">Entrada</label>
                <input
                  className={`form-input ${errosAjuste.entrada ? "error" : ""}`}
                  type="time"
                  value={formAjuste.entrada}
                  onChange={(e) => {
                    setFormAjuste((p) => ({ ...p, entrada: e.target.value }));
                    if (errosAjuste.entrada) setErrosAjuste((p) => ({ ...p, entrada: "" }));
                  }}
                />
                {errosAjuste.entrada && <span className="form-error">{errosAjuste.entrada}</span>}
              </div>
              <div className="form-field">
                <label className="form-label required">Saída</label>
                <input
                  className={`form-input ${errosAjuste.saida ? "error" : ""}`}
                  type="time"
                  value={formAjuste.saida}
                  onChange={(e) => {
                    setFormAjuste((p) => ({ ...p, saida: e.target.value }));
                    if (errosAjuste.saida) setErrosAjuste((p) => ({ ...p, saida: "" }));
                  }}
                />
                {errosAjuste.saida && <span className="form-error">{errosAjuste.saida}</span>}
              </div>
            </div>
            <div className="form-field">
              <label className="form-label">Tempo resultante</label>
              <div className="ac-apuracao-val">
                {formatarDuracao(calcularCarga(formAjuste.entrada, formAjuste.saida))}
              </div>
            </div>
            <div className="form-field">
              <label className="form-label required">Motivo do ajuste</label>
              <textarea
                className={`form-textarea ${errosAjuste.motivo ? "error" : ""}`}
                rows={3}
                value={formAjuste.motivo}
                onChange={(e) => {
                  setFormAjuste((p) => ({ ...p, motivo: e.target.value }));
                  if (errosAjuste.motivo) setErrosAjuste((p) => ({ ...p, motivo: "" }));
                }}
                placeholder="Ex.: catraca sem energia na saída; horário confirmado pela professora."
              />
              {errosAjuste.motivo && <span className="form-error">{errosAjuste.motivo}</span>}
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
}
