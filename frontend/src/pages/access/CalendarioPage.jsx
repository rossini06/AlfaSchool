import { useState, useEffect, useCallback, useMemo } from "react";
import { accessApi, carregarAuxiliar, comoLista, qs } from "../../services/accessCadastrosApi";
import { Modal } from "../../components/Modal";
import { Icon } from "../../components/Icon";
import { BlocoEstado } from "../../components/access/EstadoLista";
import { Feedback, Aviso } from "../../components/access/Feedback";
import { ConfirmarModal } from "../../components/access/ConfirmarModal";
import { formatarData, hojeIso } from "../../utils/tempo";
import "../../styles/accessCadastros.css";

const TIPOS_DIA = [
  { valor: "LETIVO", label: "Letivo" },
  { valor: "FERIADO", label: "Feriado" },
  { valor: "RECESSO", label: "Recesso" },
  { valor: "FACULTATIVO", label: "Facultativo" },
  { valor: "SABADO_LETIVO", label: "Sábado letivo" },
  { valor: "EVENTO", label: "Evento" },
];

const DOW = ["Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb"];
const MESES = [
  "janeiro", "fevereiro", "março", "abril", "maio", "junho",
  "julho", "agosto", "setembro", "outubro", "novembro", "dezembro",
];

function iso(ano, mes, dia) {
  return `${ano}-${String(mes + 1).padStart(2, "0")}-${String(dia).padStart(2, "0")}`;
}

const EDITOR_VAZIO = {
  id: null,
  data: "",
  dataFim: "",
  intervalo: false,
  tipo: "LETIVO",
  descricao: "",
  unidadeId: "",
  sobrescrever: true,
};

export function CalendarioPage() {
  const hoje = new Date();
  const [ano, setAno] = useState(hoje.getFullYear());
  const [mes, setMes] = useState(hoje.getMonth());
  const [unidades, setUnidades] = useState([]);
  const [filtroUnidade, setFiltroUnidade] = useState("");

  const [registros, setRegistros] = useState([]);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState("");

  const [editor, setEditor] = useState(null);
  const [erros, setErros] = useState({});
  const [salvando, setSalvando] = useState(false);
  const [erroForm, setErroForm] = useState("");

  const [excluir, setExcluir] = useState(null);
  const [excluindo, setExcluindo] = useState(false);
  const [erroExcluir, setErroExcluir] = useState("");
  const [feedback, setFeedback] = useState(null);

  const primeiroDia = iso(ano, mes, 1);
  const ultimoDiaNum = new Date(ano, mes + 1, 0).getDate();
  const ultimoDia = iso(ano, mes, ultimoDiaNum);

  const carregar = useCallback(async () => {
    setCarregando(true);
    setErro("");
    const r = await accessApi.get(
      `/access/calendario?${qs({ inicio: primeiroDia, fim: ultimoDia, unidadeId: filtroUnidade })}`
    );
    if (r.ok) setRegistros(comoLista(r.data));
    else {
      setRegistros([]);
      setErro(r.erro);
    }
    setCarregando(false);
  }, [primeiroDia, ultimoDia, filtroUnidade]);

  useEffect(() => {
    carregar();
    // recarrega ao trocar de mês ou de unidade
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [primeiroDia, ultimoDia, filtroUnidade]);

  useEffect(() => {
    carregarAuxiliar("/unidades?size=200").then(setUnidades);
  }, []);

  const porData = useMemo(() => {
    const m = new Map();
    registros.forEach((r) => m.set(String(r.data).slice(0, 10), r));
    return m;
  }, [registros]);

  const celulas = useMemo(() => {
    const inicioSemana = new Date(ano, mes, 1).getDay();
    const lista = [];
    for (let i = 0; i < inicioSemana; i++) lista.push(null);
    for (let d = 1; d <= ultimoDiaNum; d++) lista.push(d);
    return lista;
  }, [ano, mes, ultimoDiaNum]);

  const navegar = (delta) => {
    const d = new Date(ano, mes + delta, 1);
    setAno(d.getFullYear());
    setMes(d.getMonth());
  };

  const abrirDia = (dia) => {
    const data = iso(ano, mes, dia);
    const existente = porData.get(data);
    setErros({});
    setErroForm("");
    setEditor({
      ...EDITOR_VAZIO,
      id: existente?.id || null,
      data,
      tipo: existente?.tipo || "LETIVO",
      descricao: existente?.descricao || "",
      unidadeId: existente?.unidadeId || filtroUnidade || "",
    });
  };

  const abrirIntervalo = () => {
    setErros({});
    setErroForm("");
    setEditor({
      ...EDITOR_VAZIO,
      intervalo: true,
      data: primeiroDia,
      dataFim: ultimoDia,
      tipo: "RECESSO",
      unidadeId: filtroUnidade || "",
    });
  };

  const validar = () => {
    const e = {};
    if (!editor.data) e.data = "Informe a data inicial.";
    if (editor.intervalo) {
      if (!editor.dataFim) e.dataFim = "Informe a data final do intervalo.";
      else if (editor.dataFim < editor.data) e.dataFim = "A data final não pode ser anterior à inicial.";
    }
    if (!editor.tipo) e.tipo = "Escolha o tipo de dia.";
    if (editor.tipo === "EVENTO" && !editor.descricao.trim())
      e.descricao = "Descreva o evento — é o texto que aparece no calendário.";
    setErros(e);
    return Object.keys(e).length === 0;
  };

  const salvar = async () => {
    if (!validar()) return;
    setSalvando(true);
    setErroForm("");
    let r;
    if (editor.intervalo) {
      r = await accessApi.post("/access/calendario/intervalo", {
        dataInicio: editor.data,
        dataFim: editor.dataFim,
        tipo: editor.tipo,
        descricao: editor.descricao.trim() || null,
        unidadeId: editor.unidadeId || null,
        sobrescrever: editor.sobrescrever,
      });
    } else {
      const corpo = {
        data: editor.data,
        tipo: editor.tipo,
        descricao: editor.descricao.trim() || null,
        unidadeId: editor.unidadeId || null,
      };
      r = editor.id
        ? await accessApi.put(`/access/calendario/${editor.id}`, corpo)
        : await accessApi.post("/access/calendario", corpo);
    }
    setSalvando(false);
    if (!r.ok) {
      setErroForm(r.erro);
      return;
    }
    setFeedback({
      tipo: "sucesso",
      mensagem: editor.intervalo
        ? `Intervalo de ${formatarData(editor.data)} a ${formatarData(editor.dataFim)} marcado como ${
            TIPOS_DIA.find((t) => t.valor === editor.tipo)?.label
          }.`
        : "Dia atualizado no calendário.",
    });
    setEditor(null);
    carregar();
  };

  const confirmarExclusao = async () => {
    setExcluindo(true);
    setErroExcluir("");
    const r = await accessApi.delete(`/access/calendario/${excluir.id}`);
    setExcluindo(false);
    if (!r.ok) {
      setErroExcluir(r.erro);
      return;
    }
    setExcluir(null);
    setEditor(null);
    setFeedback({ tipo: "sucesso", mensagem: "Marcação removida — o dia volta ao padrão." });
    carregar();
  };

  const hojeStr = hojeIso();

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1 className="page-title">Calendário Escolar</h1>
          <p className="page-subtitle">Define em quais dias há apuração de permanência</p>
        </div>
        <button className="btn btn-brand" onClick={abrirIntervalo}>
          <Icon name="Calendar" size={14} /> Marcar intervalo
        </button>
      </div>

      <Aviso tipo="info">
        Clique em um dia para editá-lo. Para períodos longos — recesso de 20/12 a 31/01, por exemplo —
        use <strong>Marcar intervalo</strong> e grave tudo de uma vez.
      </Aviso>

      <Feedback {...(feedback || {})} onFechar={() => setFeedback(null)} />

      <div className="card">
        <div className="card-body">
          <div className="ac-cal-topo">
            <div className="ac-linha-acoes">
              <button className="btn btn-secondary btn-sm" onClick={() => navegar(-1)} title="Mês anterior">
                <Icon name="ChevronLeft" size={14} />
              </button>
              <span className="ac-cal-mes">
                {MESES[mes]} de {ano}
              </span>
              <button className="btn btn-secondary btn-sm" onClick={() => navegar(1)} title="Próximo mês">
                <Icon name="ChevronRight" size={14} />
              </button>
              <button
                className="btn btn-ghost btn-sm"
                onClick={() => {
                  setAno(hoje.getFullYear());
                  setMes(hoje.getMonth());
                }}
              >
                Hoje
              </button>
            </div>
            <div className="form-field" style={{ minWidth: 220 }}>
              <select className="form-select" value={filtroUnidade} onChange={(e) => setFiltroUnidade(e.target.value)}>
                <option value="">Calendário de todas as unidades</option>
                {unidades.map((u) => (
                  <option key={u.id} value={u.id}>
                    {u.name || u.nome}
                  </option>
                ))}
              </select>
            </div>
          </div>

          {carregando || erro ? (
            <BlocoEstado carregando={carregando} erro={erro} onTentarNovamente={carregar} />
          ) : (
            <>
              <div className="ac-cal-grid">
                {DOW.map((d) => (
                  <div key={d} className="ac-cal-dow">
                    {d}
                  </div>
                ))}
                {celulas.map((dia, i) => {
                  if (dia === null) return <div key={`v-${i}`} className="ac-cal-cell vazio" />;
                  const data = iso(ano, mes, dia);
                  const reg = porData.get(data);
                  return (
                    <button
                      key={data}
                      type="button"
                      className={`ac-cal-cell ${data === hojeStr ? "hoje" : ""}`}
                      onClick={() => abrirDia(dia)}
                      title={reg ? `${reg.tipo}${reg.descricao ? ` — ${reg.descricao}` : ""}` : "Sem marcação"}
                    >
                      <span className="ac-cal-num">{dia}</span>
                      {reg && (
                        <>
                          <span className={`ac-cal-tag ac-tipo-${reg.tipo}`}>
                            {TIPOS_DIA.find((t) => t.valor === reg.tipo)?.label || reg.tipo}
                          </span>
                          {reg.descricao && <span className="ac-cal-desc">{reg.descricao}</span>}
                        </>
                      )}
                    </button>
                  );
                })}
              </div>

              <div className="ac-legenda">
                {TIPOS_DIA.map((t) => (
                  <span key={t.valor} className="ac-legenda-item">
                    <span className={`ac-legenda-cor ac-tipo-${t.valor}`} />
                    {t.label}
                  </span>
                ))}
                <span className="ac-legenda-item">Dias sem marcação seguem o padrão da unidade.</span>
              </div>
            </>
          )}
        </div>
      </div>

      <Modal
        isOpen={!!editor}
        onClose={() => setEditor(null)}
        title={editor?.intervalo ? "Marcar intervalo de datas" : `Dia ${editor ? formatarData(editor.data) : ""}`}
        footer={
          <>
            {editor?.id && !editor?.intervalo && (
              <button
                className="btn btn-danger"
                onClick={() => {
                  setErroExcluir("");
                  setExcluir(editor);
                }}
                style={{ marginRight: "auto" }}
              >
                <Icon name="Trash" size={13} /> Remover marcação
              </button>
            )}
            <button className="btn btn-secondary" onClick={() => setEditor(null)}>
              Cancelar
            </button>
            <button className="btn btn-brand" onClick={salvar} disabled={salvando}>
              {salvando ? "Salvando..." : "Salvar"}
            </button>
          </>
        }
      >
        {editor && (
          <>
            <Feedback tipo="erro" mensagem={erroForm} />
            <div className="form-grid">
              <div className="form-grid-2">
                <div className="form-field">
                  <label className="form-label required">{editor.intervalo ? "De" : "Data"}</label>
                  <input
                    className={`form-input ${erros.data ? "error" : ""}`}
                    type="date"
                    value={editor.data}
                    onChange={(e) => {
                      setEditor((p) => ({ ...p, data: e.target.value }));
                      if (erros.data) setErros((p) => ({ ...p, data: "" }));
                    }}
                  />
                  {erros.data && <span className="form-error">{erros.data}</span>}
                </div>
                {editor.intervalo && (
                  <div className="form-field">
                    <label className="form-label required">Até</label>
                    <input
                      className={`form-input ${erros.dataFim ? "error" : ""}`}
                      type="date"
                      value={editor.dataFim}
                      onChange={(e) => {
                        setEditor((p) => ({ ...p, dataFim: e.target.value }));
                        if (erros.dataFim) setErros((p) => ({ ...p, dataFim: "" }));
                      }}
                    />
                    {erros.dataFim && <span className="form-error">{erros.dataFim}</span>}
                  </div>
                )}
              </div>

              {!editor.intervalo && (
                <label className="form-checkbox">
                  <input
                    type="checkbox"
                    checked={editor.intervalo}
                    onChange={(e) =>
                      setEditor((p) => ({
                        ...p,
                        intervalo: e.target.checked,
                        dataFim: e.target.checked ? p.data : "",
                        id: e.target.checked ? null : p.id,
                      }))
                    }
                  />
                  <span>Aplicar a um intervalo de datas a partir deste dia</span>
                </label>
              )}

              <div className="form-field">
                <label className="form-label required">Tipo de dia</label>
                <select
                  className={`form-select ${erros.tipo ? "error" : ""}`}
                  value={editor.tipo}
                  onChange={(e) => {
                    setEditor((p) => ({ ...p, tipo: e.target.value }));
                    if (erros.tipo) setErros((p) => ({ ...p, tipo: "" }));
                  }}
                >
                  {TIPOS_DIA.map((t) => (
                    <option key={t.valor} value={t.valor}>
                      {t.label}
                    </option>
                  ))}
                </select>
                {erros.tipo && <span className="form-error">{erros.tipo}</span>}
              </div>

              <div className="form-field">
                <label className={`form-label ${editor.tipo === "EVENTO" ? "required" : ""}`}>Descrição</label>
                <input
                  className={`form-input ${erros.descricao ? "error" : ""}`}
                  value={editor.descricao}
                  onChange={(e) => {
                    setEditor((p) => ({ ...p, descricao: e.target.value }));
                    if (erros.descricao) setErros((p) => ({ ...p, descricao: "" }));
                  }}
                  placeholder="Ex.: Recesso de fim de ano"
                />
                {erros.descricao && <span className="form-error">{erros.descricao}</span>}
              </div>

              <div className="form-field">
                <label className="form-label">Unidade</label>
                <select
                  className="form-select"
                  value={editor.unidadeId}
                  onChange={(e) => setEditor((p) => ({ ...p, unidadeId: e.target.value }))}
                >
                  <option value="">Todas as unidades</option>
                  {unidades.map((u) => (
                    <option key={u.id} value={u.id}>
                      {u.name || u.nome}
                    </option>
                  ))}
                </select>
              </div>

              {editor.intervalo && (
                <label className="form-checkbox">
                  <input
                    type="checkbox"
                    checked={editor.sobrescrever}
                    onChange={(e) => setEditor((p) => ({ ...p, sobrescrever: e.target.checked }))}
                  />
                  <span>Substituir marcações já existentes dentro do intervalo</span>
                </label>
              )}
            </div>
          </>
        )}
      </Modal>

      <ConfirmarModal
        aberto={!!excluir}
        titulo="Remover marcação do dia"
        textoConfirmar="Remover"
        processando={excluindo}
        erro={erroExcluir}
        onConfirmar={confirmarExclusao}
        onCancelar={() => setExcluir(null)}
      >
        O dia {excluir ? formatarData(excluir.data) : ""} volta ao comportamento padrão do calendário.
        Confirma?
      </ConfirmarModal>
    </div>
  );
}
