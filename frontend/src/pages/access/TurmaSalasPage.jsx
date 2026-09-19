import { useState, useEffect, useCallback, useMemo } from "react";
import { accessApi, carregarAuxiliar, comoLista, comoTotal, qs } from "../../services/accessCadastrosApi";
import { Modal } from "../../components/Modal";
import { Pagination } from "../../components/Pagination";
import { Icon } from "../../components/Icon";
import { LinhasEstado } from "../../components/access/EstadoLista";
import { Feedback, Aviso } from "../../components/access/Feedback";
import { ConfirmarModal } from "../../components/access/ConfirmarModal";
import { DiasSemanaChips } from "../../components/access/DiasSemanaChips";
import { nomesDosDias, diasParaCsv, diasDeCsv } from "../../utils/diasSemana";
import { horaParaMinutos, formatarData, formatarHora } from "../../utils/tempo";
import "../../styles/accessCadastros.css";

const PAGE_SIZE = 20;
const FORM_VAZIO = {
  turmaId: "",
  salaId: "",
  vigenciaInicio: "",
  vigenciaFim: "",
  horaInicio: "",
  horaFim: "",
  diasSemana: [1, 2, 3, 4, 5],
};

/** Duas faixas de horário se cruzam? */
function faixasSeCruzam(aIni, aFim, bIni, bFim) {
  const a1 = horaParaMinutos(aIni);
  const a2 = horaParaMinutos(aFim);
  const b1 = horaParaMinutos(bIni);
  const b2 = horaParaMinutos(bFim);
  if (a1 === null || a2 === null || b1 === null || b2 === null) return false;
  return a1 < b2 && b1 < a2;
}

/** Vigências se cruzam? Datas vazias = aberto. */
function vigenciasSeCruzam(aIni, aFim, bIni, bFim) {
  const ini1 = aIni || "0000-01-01";
  const fim1 = aFim || "9999-12-31";
  const ini2 = bIni || "0000-01-01";
  const fim2 = bFim || "9999-12-31";
  return ini1 <= fim2 && ini2 <= fim1;
}

export function TurmaSalasPage() {
  const [itens, setItens] = useState([]);
  const [turmas, setTurmas] = useState([]);
  const [salas, setSalas] = useState([]);
  const [total, setTotal] = useState(0);
  const [pagina, setPagina] = useState(0);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState("");

  const [filtroTurma, setFiltroTurma] = useState("");
  const [filtroSala, setFiltroSala] = useState("");

  const [modalAberto, setModalAberto] = useState(false);
  const [editando, setEditando] = useState(null);
  const [form, setForm] = useState(FORM_VAZIO);
  const [erros, setErros] = useState({});
  const [salvando, setSalvando] = useState(false);
  const [erroForm, setErroForm] = useState("");

  const [ocupacao, setOcupacao] = useState([]);
  const [carregandoOcupacao, setCarregandoOcupacao] = useState(false);

  const [salaDetalhe, setSalaDetalhe] = useState(null);
  const [excluirId, setExcluirId] = useState(null);
  const [excluindo, setExcluindo] = useState(false);
  const [erroExcluir, setErroExcluir] = useState("");
  const [feedback, setFeedback] = useState(null);

  const carregar = useCallback(
    async (p = 0) => {
      setCarregando(true);
      setErro("");
      const r = await accessApi.get(
        `/access/turma-salas?${qs({ page: p, size: PAGE_SIZE, turmaId: filtroTurma, salaId: filtroSala })}`
      );
      if (r.ok) {
        setItens(comoLista(r.data));
        setTotal(comoTotal(r.data));
        setPagina(p);
      } else {
        setItens([]);
        setTotal(0);
        setErro(r.erro);
      }
      setCarregando(false);
    },
    [filtroTurma, filtroSala]
  );

  useEffect(() => {
    carregar(0);
    carregarAuxiliar("/turmas?size=300").then(setTurmas);
    carregarAuxiliar("/access/salas?size=300").then(setSalas);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const nomeTurma = (id) => turmas.find((t) => t.id === id)?.nome || "—";
  const nomeSala = (id) => {
    const s = salas.find((x) => x.id === id);
    return s ? `${s.nome}${s.codigo ? ` (${s.codigo})` : ""}` : "—";
  };

  /** Carrega quem mais ocupa a sala escolhida. */
  const carregarOcupacao = useCallback(async (salaId) => {
    if (!salaId) {
      setOcupacao([]);
      return;
    }
    setCarregandoOcupacao(true);
    const r = await accessApi.get(`/access/turma-salas/sala/${salaId}`);
    setOcupacao(r.ok ? comoLista(r.data) : []);
    setCarregandoOcupacao(false);
  }, []);

  useEffect(() => {
    if (modalAberto) carregarOcupacao(form.salaId);
  }, [modalAberto, form.salaId, carregarOcupacao]);

  const ocupacaoVisivel = useMemo(
    () => ocupacao.filter((o) => o.id !== editando?.id),
    [ocupacao, editando]
  );

  const conflitos = useMemo(() => {
    if (!form.salaId || !form.horaInicio || !form.horaFim) return [];
    return ocupacaoVisivel.filter(
      (o) =>
        diasDeCsv(o.diasSemanaResolvidos ?? o.diasSemana).some((d) => form.diasSemana.includes(d)) &&
        faixasSeCruzam(form.horaInicio, form.horaFim, o.horaInicio, o.horaFim) &&
        vigenciasSeCruzam(form.vigenciaInicio, form.vigenciaFim, o.vigenciaInicio, o.vigenciaFim)
    );
  }, [ocupacaoVisivel, form]);

  const abrirNovo = () => {
    setEditando(null);
    setForm(FORM_VAZIO);
    setErros({});
    setErroForm("");
    setModalAberto(true);
  };

  const abrirEdicao = (item) => {
    setEditando(item);
    setForm({
      turmaId: item.turmaId || "",
      salaId: item.salaId || "",
      vigenciaInicio: item.vigenciaInicio || "",
      vigenciaFim: item.vigenciaFim || "",
      horaInicio: (item.horaInicio || "").slice(0, 5),
      horaFim: (item.horaFim || "").slice(0, 5),
      diasSemana: diasDeCsv(item.diasSemana),
    });
    setErros({});
    setErroForm("");
    setModalAberto(true);
  };

  const validar = () => {
    const e = {};
    if (!form.turmaId) e.turmaId = "Selecione a turma.";
    if (!form.salaId) e.salaId = "Selecione a sala.";
    if (!form.vigenciaInicio) e.vigenciaInicio = "Informe o início da vigência.";
    if (form.vigenciaFim && form.vigenciaInicio && form.vigenciaFim < form.vigenciaInicio) {
      e.vigenciaFim = "O fim não pode ser anterior ao início.";
    }
    if (!form.horaInicio) e.horaInicio = "Informe o horário de início.";
    if (!form.horaFim) e.horaFim = "Informe o horário de término.";
    if (form.horaInicio && form.horaFim && horaParaMinutos(form.horaFim) <= horaParaMinutos(form.horaInicio)) {
      e.horaFim = "O término deve ser depois do início.";
    }
    if (!form.diasSemana.length) e.diasSemana = "Escolha ao menos um dia da semana.";
    setErros(e);
    return Object.keys(e).length === 0;
  };

  const salvar = async () => {
    if (!validar()) return;
    setSalvando(true);
    setErroForm("");
    const corpo = {
      turmaId: form.turmaId,
      salaId: form.salaId,
      vigenciaInicio: form.vigenciaInicio,
      vigenciaFim: form.vigenciaFim || null,
      horaInicio: form.horaInicio,
      horaFim: form.horaFim,
      diasSemana: diasParaCsv(form.diasSemana),
    };
    const r = editando
      ? await accessApi.put(`/access/turma-salas/${editando.id}`, corpo)
      : await accessApi.post("/access/turma-salas", corpo);
    setSalvando(false);
    if (!r.ok) {
      setErroForm(r.erro);
      return;
    }
    setModalAberto(false);
    setFeedback({ tipo: "sucesso", mensagem: editando ? "Vínculo atualizado." : "Turma alocada na sala." });
    carregar(pagina);
  };

  const excluir = async () => {
    setExcluindo(true);
    setErroExcluir("");
    const r = await accessApi.delete(`/access/turma-salas/${excluirId}`);
    setExcluindo(false);
    if (!r.ok) {
      setErroExcluir(r.erro);
      return;
    }
    setExcluirId(null);
    setFeedback({ tipo: "sucesso", mensagem: "Vínculo removido." });
    carregar(pagina);
  };

  const abrirDetalheSala = async (salaId) => {
    setSalaDetalhe({ salaId, itens: [], carregando: true });
    const r = await accessApi.get(`/access/turma-salas/sala/${salaId}`);
    setSalaDetalhe({ salaId, itens: r.ok ? comoLista(r.data) : [], carregando: false, erro: r.ok ? "" : r.erro });
  };

  const linhaOcupacao = (o, emConflito) => (
    <div key={o.id} className={`ac-ocupacao-item ${emConflito ? "conflito" : ""}`}>
      <span>
        <strong>{o.turmaNome || nomeTurma(o.turmaId)}</strong>
        <span className="ac-meta"> · {nomesDosDias(o.diasSemana)}</span>
      </span>
      <span className="ac-mono">
        {formatarHora(o.horaInicio)}–{formatarHora(o.horaFim)}
      </span>
    </div>
  );

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1 className="page-title">Turmas nas Salas</h1>
          <p className="page-subtitle">Quem ocupa cada sala, em que horário e durante qual período</p>
        </div>
        <button className="btn btn-brand" onClick={abrirNovo}>
          <Icon name="Plus" size={14} /> Alocar Turma
        </button>
      </div>

      <Aviso tipo="info" titulo="Uma sala abriga várias turmas">
        O vínculo é por faixa de horário e dias da semana: a mesma sala pode receber o 3º Ano A pela
        manhã e o 5º Ano B à tarde. Ao escolher a sala no formulário, a tela mostra quem mais a ocupa
        e avisa se o horário colide.
      </Aviso>

      <Feedback {...(feedback || {})} onFechar={() => setFeedback(null)} />

      <div className="card">
        <div className="card-body">
          <div className="filter-bar">
            <div className="form-field" style={{ flex: 1 }}>
              <select className="form-select" value={filtroTurma} onChange={(e) => setFiltroTurma(e.target.value)}>
                <option value="">Todas as turmas</option>
                {turmas.map((t) => (
                  <option key={t.id} value={t.id}>
                    {t.nome}
                  </option>
                ))}
              </select>
            </div>
            <div className="form-field" style={{ flex: 1 }}>
              <select className="form-select" value={filtroSala} onChange={(e) => setFiltroSala(e.target.value)}>
                <option value="">Todas as salas</option>
                {salas.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.nome}
                  </option>
                ))}
              </select>
            </div>
            <button className="btn btn-brand" onClick={() => carregar(0)}>
              <Icon name="Filter" size={14} /> Filtrar
            </button>
            <button
              className="btn btn-secondary"
              onClick={() => {
                setFiltroTurma("");
                setFiltroSala("");
                setTimeout(() => carregar(0), 0);
              }}
            >
              Limpar
            </button>
          </div>
        </div>
      </div>

      <div className="table-wrapper">
        <table className="data-table">
          <thead>
            <tr>
              <th>Turma</th>
              <th>Sala</th>
              <th>Dias</th>
              <th>Horário</th>
              <th>Vigência</th>
              <th>Ações</th>
            </tr>
          </thead>
          <tbody>
            <LinhasEstado
              colSpan={6}
              carregando={carregando}
              erro={erro}
              vazio={itens.length === 0}
              icone="Map"
              tituloVazio="Nenhuma turma alocada"
              textoVazio="Vincule as turmas às salas para que o painel saiba onde cada aluno está."
              onTentarNovamente={() => carregar(pagina)}
            />
            {!carregando &&
              !erro &&
              itens.map((item) => (
                <tr key={item.id}>
                  <td>
                    <strong>{item.turmaNome || nomeTurma(item.turmaId)}</strong>
                  </td>
                  <td>
                    <button
                      className="btn btn-ghost btn-xs"
                      onClick={() => abrirDetalheSala(item.salaId)}
                      title="Ver a ocupação completa desta sala"
                    >
                      <Icon name="Map" size={12} /> {item.salaNome || nomeSala(item.salaId)}
                    </button>
                  </td>
                  <td className="td-muted">{nomesDosDias(item.diasSemana)}</td>
                  <td className="ac-mono">
                    {formatarHora(item.horaInicio)}–{formatarHora(item.horaFim)}
                  </td>
                  <td className="td-muted">
                    {formatarData(item.vigenciaInicio)} → {item.vigenciaFim ? formatarData(item.vigenciaFim) : "sem fim"}
                  </td>
                  <td>
                    <div className="ac-linha-acoes">
                      <button className="btn btn-ghost btn-sm" onClick={() => abrirEdicao(item)} title="Editar">
                        <Icon name="Edit" size={13} />
                      </button>
                      <button
                        className="btn btn-ghost btn-sm text-danger"
                        onClick={() => {
                          setErroExcluir("");
                          setExcluirId(item.id);
                        }}
                        title="Remover vínculo"
                      >
                        <Icon name="Trash" size={13} />
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
          </tbody>
        </table>
        <Pagination
          page={pagina}
          totalPages={Math.ceil(total / PAGE_SIZE)}
          total={total}
          pageSize={PAGE_SIZE}
          onPageChange={(p) => carregar(p)}
        />
      </div>

      <Modal
        isOpen={modalAberto}
        onClose={() => setModalAberto(false)}
        title={editando ? "Editar alocação" : "Alocar turma na sala"}
        size="lg"
        footer={
          <>
            <button className="btn btn-secondary" onClick={() => setModalAberto(false)}>
              Cancelar
            </button>
            <button className="btn btn-brand" onClick={salvar} disabled={salvando}>
              {salvando ? "Salvando..." : "Salvar"}
            </button>
          </>
        }
      >
        <Feedback tipo="erro" mensagem={erroForm} />
        <div className="form-grid">
          <div className="form-grid-2">
            <div className="form-field">
              <label className="form-label required">Turma</label>
              <select
                className={`form-select ${erros.turmaId ? "error" : ""}`}
                value={form.turmaId}
                onChange={(e) => {
                  setForm((p) => ({ ...p, turmaId: e.target.value }));
                  if (erros.turmaId) setErros((p) => ({ ...p, turmaId: "" }));
                }}
              >
                <option value="">Selecione a turma</option>
                {turmas.map((t) => (
                  <option key={t.id} value={t.id}>
                    {t.nome}
                  </option>
                ))}
              </select>
              {erros.turmaId && <span className="form-error">{erros.turmaId}</span>}
            </div>
            <div className="form-field">
              <label className="form-label required">Sala</label>
              <select
                className={`form-select ${erros.salaId ? "error" : ""}`}
                value={form.salaId}
                onChange={(e) => {
                  setForm((p) => ({ ...p, salaId: e.target.value }));
                  if (erros.salaId) setErros((p) => ({ ...p, salaId: "" }));
                }}
              >
                <option value="">Selecione a sala</option>
                {salas.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.nome}
                    {s.codigo ? ` (${s.codigo})` : ""}
                  </option>
                ))}
              </select>
              {erros.salaId && <span className="form-error">{erros.salaId}</span>}
            </div>
          </div>

          {form.salaId && (
            <div className="ac-subcard">
              <div className="ac-subcard-titulo">
                <Icon name="Users" size={14} />
                Quem mais ocupa {nomeSala(form.salaId)}
              </div>
              {carregandoOcupacao ? (
                <div className="skeleton skeleton-row" />
              ) : ocupacaoVisivel.length === 0 ? (
                <p className="ac-meta">Nenhuma outra turma usa esta sala hoje.</p>
              ) : (
                <div className="ac-ocupacao-lista">
                  {ocupacaoVisivel.map((o) => linhaOcupacao(o, conflitos.some((c) => c.id === o.id)))}
                </div>
              )}
              {conflitos.length > 0 && (
                <div className="mt-2">
                  <Aviso tipo="alerta" titulo="Horário sobreposto">
                    A faixa informada colide com {conflitos.length === 1 ? "a turma" : "as turmas"}{" "}
                    {conflitos.map((c) => c.turmaNome || nomeTurma(c.turmaId)).join(", ")}. Se a sala
                    realmente comporta as duas turmas ao mesmo tempo, pode salvar; caso contrário,
                    ajuste o horário.
                  </Aviso>
                </div>
              )}
            </div>
          )}

          <div className="form-grid-2">
            <div className="form-field">
              <label className="form-label required">Vigência — início</label>
              <input
                className={`form-input ${erros.vigenciaInicio ? "error" : ""}`}
                type="date"
                value={form.vigenciaInicio}
                onChange={(e) => {
                  setForm((p) => ({ ...p, vigenciaInicio: e.target.value }));
                  if (erros.vigenciaInicio) setErros((p) => ({ ...p, vigenciaInicio: "" }));
                }}
              />
              {erros.vigenciaInicio && <span className="form-error">{erros.vigenciaInicio}</span>}
            </div>
            <div className="form-field">
              <label className="form-label">Vigência — fim</label>
              <input
                className={`form-input ${erros.vigenciaFim ? "error" : ""}`}
                type="date"
                value={form.vigenciaFim}
                onChange={(e) => {
                  setForm((p) => ({ ...p, vigenciaFim: e.target.value }));
                  if (erros.vigenciaFim) setErros((p) => ({ ...p, vigenciaFim: "" }));
                }}
              />
              {erros.vigenciaFim ? (
                <span className="form-error">{erros.vigenciaFim}</span>
              ) : (
                <span className="form-hint">Em branco = vale até segunda ordem.</span>
              )}
            </div>
          </div>

          <div className="form-grid-2">
            <div className="form-field">
              <label className="form-label required">Entrada na sala</label>
              <input
                className={`form-input ${erros.horaInicio ? "error" : ""}`}
                type="time"
                value={form.horaInicio}
                onChange={(e) => {
                  setForm((p) => ({ ...p, horaInicio: e.target.value }));
                  if (erros.horaInicio) setErros((p) => ({ ...p, horaInicio: "" }));
                }}
              />
              {erros.horaInicio && <span className="form-error">{erros.horaInicio}</span>}
            </div>
            <div className="form-field">
              <label className="form-label required">Saída da sala</label>
              <input
                className={`form-input ${erros.horaFim ? "error" : ""}`}
                type="time"
                value={form.horaFim}
                onChange={(e) => {
                  setForm((p) => ({ ...p, horaFim: e.target.value }));
                  if (erros.horaFim) setErros((p) => ({ ...p, horaFim: "" }));
                }}
              />
              {erros.horaFim && <span className="form-error">{erros.horaFim}</span>}
            </div>
          </div>

          <div className="form-field">
            <label className="form-label required">Dias da semana</label>
            <DiasSemanaChips
              value={form.diasSemana}
              onChange={(v) => {
                setForm((p) => ({ ...p, diasSemana: v }));
                if (erros.diasSemana) setErros((p) => ({ ...p, diasSemana: "" }));
              }}
            />
            {erros.diasSemana ? (
              <span className="form-error">{erros.diasSemana}</span>
            ) : (
              <span className="form-hint">Selecionados: {nomesDosDias(form.diasSemana)}</span>
            )}
          </div>
        </div>
      </Modal>

      <Modal
        isOpen={!!salaDetalhe}
        onClose={() => setSalaDetalhe(null)}
        title={salaDetalhe ? `Ocupação de ${nomeSala(salaDetalhe.salaId)}` : ""}
        footer={
          <button className="btn btn-secondary" onClick={() => setSalaDetalhe(null)}>
            Fechar
          </button>
        }
      >
        {salaDetalhe?.carregando ? (
          <div className="skeleton skeleton-row" />
        ) : salaDetalhe?.erro ? (
          <Feedback tipo="erro" mensagem={salaDetalhe.erro} />
        ) : (salaDetalhe?.itens || []).length === 0 ? (
          <p className="ac-meta">Nenhuma turma alocada nesta sala.</p>
        ) : (
          <div className="ac-ocupacao-lista">
            {salaDetalhe.itens
              .slice()
              .sort((a, b) => String(a.horaInicio).localeCompare(String(b.horaInicio)))
              .map((o) => linhaOcupacao(o, false))}
          </div>
        )}
      </Modal>

      <ConfirmarModal
        aberto={!!excluirId}
        titulo="Remover vínculo"
        textoConfirmar="Remover"
        processando={excluindo}
        erro={erroExcluir}
        onConfirmar={excluir}
        onCancelar={() => setExcluirId(null)}
      >
        A turma deixa de ser associada a esta sala nesse horário. Confirma?
      </ConfirmarModal>
    </div>
  );
}
