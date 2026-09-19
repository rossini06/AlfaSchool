import { useState, useEffect, useCallback } from "react";
import { accessApi, carregarAuxiliar, comoLista, comoTotal, qs } from "../../services/accessCadastrosApi";
import { Modal } from "../../components/Modal";
import { Pagination } from "../../components/Pagination";
import { Icon } from "../../components/Icon";
import { LinhasEstado, BlocoEstado } from "../../components/access/EstadoLista";
import { Feedback, Aviso } from "../../components/access/Feedback";
import { ConfirmarModal } from "../../components/access/ConfirmarModal";
import { DiasSemanaChips } from "../../components/access/DiasSemanaChips";
import { nomesDosDias, diasParaCsv, diasDeCsv } from "../../utils/diasSemana";
import { StatusAutorizacaoBadge } from "../../components/access/StatusAutorizacaoBadge";
import { STATUS_AUTORIZACAO } from "../../utils/statusAutorizacao";
import { formatarData, formatarHora, formatarDataHora, hojeIso } from "../../utils/tempo";
import "../../styles/accessCadastros.css";

const PAGE_SIZE = 20;

const FORM_VAZIO = {
  alunoId: "",
  pessoaAutorizadaId: "",
  temporaria: false,
  vigenciaInicio: hojeIso(),
  vigenciaFim: "",
  diasSemana: [1, 2, 3, 4, 5],
  horaInicio: "",
  horaFim: "",
  observacoes: "",
};

export function AutorizacoesPage() {
  const [aba, setAba] = useState("todas");

  const [itens, setItens] = useState([]);
  const [total, setTotal] = useState(0);
  const [pagina, setPagina] = useState(0);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState("");

  const [fila, setFila] = useState([]);
  const [carregandoFila, setCarregandoFila] = useState(true);
  const [erroFila, setErroFila] = useState("");

  const [alunos, setAlunos] = useState([]);
  const [pessoas, setPessoas] = useState([]);

  const [filtroAluno, setFiltroAluno] = useState("");
  const [filtroStatus, setFiltroStatus] = useState("");
  const [busca, setBusca] = useState("");

  const [modalAberto, setModalAberto] = useState(false);
  const [editando, setEditando] = useState(null);
  const [form, setForm] = useState(FORM_VAZIO);
  const [erros, setErros] = useState({});
  const [salvando, setSalvando] = useState(false);
  const [erroForm, setErroForm] = useState("");

  const [acao, setAcao] = useState(null); // { tipo, item }
  const [processandoAcao, setProcessandoAcao] = useState(false);
  const [erroAcao, setErroAcao] = useState("");

  const [historico, setHistorico] = useState(null);
  const [feedback, setFeedback] = useState(null);

  const carregar = useCallback(
    async (p = 0) => {
      setCarregando(true);
      setErro("");
      const r = await accessApi.get(
        `/access/autorizacoes?${qs({
          page: p,
          size: PAGE_SIZE,
          alunoId: filtroAluno,
          status: filtroStatus,
          q: busca,
        })}`
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
    [filtroAluno, filtroStatus, busca]
  );

  const carregarFila = useCallback(async () => {
    setCarregandoFila(true);
    setErroFila("");
    const r = await accessApi.get(`/access/autorizacoes?${qs({ status: "PENDENTE", origem: "PORTAL", size: 100 })}`);
    if (r.ok) setFila(comoLista(r.data));
    else {
      setFila([]);
      setErroFila(r.erro);
    }
    setCarregandoFila(false);
  }, []);

  useEffect(() => {
    carregar(0);
    carregarFila();
    carregarAuxiliar("/alunos?size=500").then(setAlunos);
    carregarAuxiliar("/access/pessoas-autorizadas?size=500").then(setPessoas);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const nomeAluno = (id) => alunos.find((a) => a.id === id)?.nome || "—";
  const nomePessoa = (id) => pessoas.find((p) => p.id === id)?.nome || "—";

  const abrirNovo = () => {
    setEditando(null);
    setForm({ ...FORM_VAZIO, alunoId: filtroAluno || "" });
    setErros({});
    setErroForm("");
    setModalAberto(true);
  };

  const abrirEdicao = (item) => {
    setEditando(item);
    setForm({
      alunoId: item.alunoId || "",
      pessoaAutorizadaId: item.pessoaAutorizadaId || "",
      temporaria: !item.permanente,
      vigenciaInicio: item.vigenciaInicio || hojeIso(),
      vigenciaFim: item.vigenciaFim || "",
      diasSemana: diasDeCsv(item.diasSemana),
      horaInicio: (item.horaInicio || "").slice(0, 5),
      horaFim: (item.horaFim || "").slice(0, 5),
      observacoes: item.observacao || "",
    });
    setErros({});
    setErroForm("");
    setModalAberto(true);
  };

  const validar = () => {
    const e = {};
    if (!form.alunoId) e.alunoId = "Selecione o aluno.";
    if (!form.pessoaAutorizadaId) e.pessoaAutorizadaId = "Selecione quem está sendo autorizado.";
    if (!form.vigenciaInicio) e.vigenciaInicio = "Informe o início da vigência.";
    if (form.temporaria && !form.vigenciaFim)
      e.vigenciaFim = "Autorização temporária exige data de fim — é o que a torna temporária.";
    if (form.vigenciaFim && form.vigenciaInicio && form.vigenciaFim < form.vigenciaInicio)
      e.vigenciaFim = "O fim não pode ser anterior ao início.";
    if (form.horaInicio && form.horaFim && form.horaFim <= form.horaInicio)
      e.horaFim = "O fim da faixa deve ser depois do início.";
    if ((form.horaInicio && !form.horaFim) || (!form.horaInicio && form.horaFim))
      e.horaFim = "Preencha as duas pontas da faixa de horário, ou deixe as duas em branco.";
    setErros(e);
    return Object.keys(e).length === 0;
  };

  const salvar = async () => {
    if (!validar()) return;
    setSalvando(true);
    setErroForm("");
    const corpo = {
      alunoId: form.alunoId,
      pessoaAutorizadaId: form.pessoaAutorizadaId,
      // A API guarda `permanente`, que e' o INVERSO de `temporaria`.
      // Mandar o campo errado dava 400 "permanente é obrigatório" em todo
      // cadastro — nenhuma autorizacao podia ser criada pela tela.
      permanente: !form.temporaria,
      vigenciaInicio: form.vigenciaInicio || null,
      vigenciaFim: form.temporaria ? form.vigenciaFim || null : null,
      diasSemana: diasParaCsv(form.diasSemana),
      horaInicio: form.horaInicio || null,
      horaFim: form.horaFim || null,
      observacao: form.observacoes.trim() || null,
    };
    const r = editando
      ? await accessApi.put(`/access/autorizacoes/${editando.id}`, corpo)
      : await accessApi.post("/access/autorizacoes", corpo);
    setSalvando(false);
    if (!r.ok) {
      setErroForm(r.erro);
      return;
    }
    setModalAberto(false);
    setFeedback({ tipo: "sucesso", mensagem: editando ? "Autorização atualizada." : "Autorização criada." });
    carregar(pagina);
    carregarFila();
  };

  const CONFIG_ACAO = {
    aprovar: {
      titulo: "Aprovar autorização",
      texto: "A pessoa passa a poder retirar o aluno dentro da vigência e dos horários cadastrados.",
      botao: "Aprovar",
      variante: "btn-success",
      exigeMotivo: false,
      rota: "aprovar",
      sucesso: "Autorização aprovada.",
    },
    suspender: {
      titulo: "Suspender autorização",
      texto: "A retirada fica bloqueada até alguém reativar. A autorização não é apagada.",
      botao: "Suspender",
      variante: "btn-warning",
      exigeMotivo: true,
      rota: "suspender",
      sucesso: "Autorização suspensa.",
    },
    revogar: {
      titulo: "Revogar autorização",
      texto: "A revogação é definitiva: para voltar a valer, será preciso criar uma autorização nova.",
      botao: "Revogar",
      variante: "btn-danger",
      exigeMotivo: true,
      rota: "revogar",
      sucesso: "Autorização revogada.",
    },
    reativar: {
      titulo: "Reativar autorização",
      texto: "A pessoa volta a poder retirar o aluno, respeitando a vigência cadastrada.",
      botao: "Reativar",
      variante: "btn-success",
      exigeMotivo: false,
      rota: "reativar",
      sucesso: "Autorização reativada.",
    },
  };

  const executarAcao = async (motivo) => {
    const cfg = CONFIG_ACAO[acao.tipo];
    setProcessandoAcao(true);
    setErroAcao("");
    const r = await accessApi.post(`/access/autorizacoes/${acao.item.id}/${cfg.rota}`, {
      motivo: motivo || null,
    });
    setProcessandoAcao(false);
    if (!r.ok) {
      setErroAcao(r.erro);
      return;
    }
    setAcao(null);
    setFeedback({ tipo: "sucesso", mensagem: cfg.sucesso });
    carregar(pagina);
    carregarFila();
  };

  const abrirHistorico = async (item) => {
    setHistorico({ item, carregando: true, itens: [], erro: "" });
    const r = await accessApi.get(`/access/autorizacoes/${item.id}/historico`);
    setHistorico({
      item,
      carregando: false,
      itens: r.ok ? comoLista(r.data) : [],
      erro: r.ok ? "" : r.erro,
    });
  };

  const acoesDaLinha = (item) => {
    const s = item.status;
    return (
      <div className="ac-linha-acoes">
        {s === "PENDENTE" && (
          <button
            className="btn btn-ghost btn-sm text-success"
            title="Aprovar"
            onClick={() => {
              setErroAcao("");
              setAcao({ tipo: "aprovar", item });
            }}
          >
            <Icon name="CheckCircle" size={13} />
          </button>
        )}
        {s === "ATIVA" && (
          <button
            className="btn btn-ghost btn-sm text-warning"
            title="Suspender"
            onClick={() => {
              setErroAcao("");
              setAcao({ tipo: "suspender", item });
            }}
          >
            <Icon name="Lock" size={13} />
          </button>
        )}
        {s === "SUSPENSA" && (
          <button
            className="btn btn-ghost btn-sm text-success"
            title="Reativar"
            onClick={() => {
              setErroAcao("");
              setAcao({ tipo: "reativar", item });
            }}
          >
            <Icon name="Unlock" size={13} />
          </button>
        )}
        {s !== "REVOGADA" && (
          <button
            className="btn btn-ghost btn-sm text-danger"
            title="Revogar"
            onClick={() => {
              setErroAcao("");
              setAcao({ tipo: "revogar", item });
            }}
          >
            <Icon name="XCircle" size={13} />
          </button>
        )}
        <button className="btn btn-ghost btn-sm" title="Editar" onClick={() => abrirEdicao(item)}>
          <Icon name="Edit" size={13} />
        </button>
        <button className="btn btn-ghost btn-sm" title="Histórico de alterações" onClick={() => abrirHistorico(item)}>
          <Icon name="Activity" size={13} />
        </button>
      </div>
    );
  };

  const configAcao = acao ? CONFIG_ACAO[acao.tipo] : null;

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1 className="page-title">Autorizações de Retirada</h1>
          <p className="page-subtitle">Quem pode retirar cada aluno, em que dias e em que horário</p>
        </div>
        <button className="btn btn-brand" onClick={abrirNovo}>
          <Icon name="Plus" size={14} /> Nova Autorização
        </button>
      </div>

      <Feedback {...(feedback || {})} onFechar={() => setFeedback(null)} />

      <div className="tabs-header mb-4">
        <button className={`tab-btn ${aba === "todas" ? "active" : ""}`} onClick={() => setAba("todas")}>
          Autorizações
        </button>
        <button className={`tab-btn ${aba === "fila" ? "active" : ""}`} onClick={() => setAba("fila")}>
          Aguardando aprovação
          {fila.length > 0 && <span className="badge badge-warning" style={{ marginLeft: 6 }}>{fila.length}</span>}
        </button>
      </div>

      {aba === "fila" ? (
        <>
          <Aviso tipo="info" titulo="Pedidos vindos do portal da família">
            Enquanto o pedido estiver aqui, a pessoa <strong>não</strong> pode retirar o aluno. Aprovar
            cria a autorização ativa; revogar recusa o pedido e registra o motivo.
          </Aviso>

          <div className="table-wrapper">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Pessoa</th>
                  <th>Aluno</th>
                  <th>Solicitado em</th>
                  <th>Solicitante</th>
                  <th>Vigência pedida</th>
                  <th>Ações</th>
                </tr>
              </thead>
              <tbody>
                <LinhasEstado
                  colSpan={6}
                  carregando={carregandoFila}
                  erro={erroFila}
                  vazio={fila.length === 0}
                  icone="CheckCircle"
                  tituloVazio="Nenhum pedido aguardando"
                  textoVazio="Toda solicitação feita pelo portal cai aqui para a escola decidir."
                  onTentarNovamente={carregarFila}
                />
                {!carregandoFila &&
                  !erroFila &&
                  fila.map((item) => (
                    <tr key={item.id}>
                      <td>
                        <strong>{item.pessoaNome || nomePessoa(item.pessoaAutorizadaId)}</strong>
                        {item.parentesco && <div className="ac-meta">{item.parentesco}</div>}
                      </td>
                      <td className="td-muted">{item.alunoNome || nomeAluno(item.alunoId)}</td>
                      <td className="td-muted">{formatarDataHora(item.criadoEm || item.solicitadoEm)}</td>
                      <td className="td-muted">{item.solicitadoPor || "Portal da família"}</td>
                      <td className="td-muted">
                        {formatarData(item.vigenciaInicio)} →{" "}
                        {item.vigenciaFim ? formatarData(item.vigenciaFim) : "sem fim"}
                      </td>
                      <td>{acoesDaLinha(item)}</td>
                    </tr>
                  ))}
              </tbody>
            </table>
          </div>
        </>
      ) : (
        <>
          <div className="card">
            <div className="card-body">
              <div className="filter-bar">
                <div className="form-field" style={{ flex: 1 }}>
                  <input
                    className="form-input"
                    placeholder="Buscar por pessoa..."
                    value={busca}
                    onChange={(e) => setBusca(e.target.value)}
                    onKeyDown={(e) => e.key === "Enter" && carregar(0)}
                  />
                </div>
                <div className="form-field">
                  <select className="form-select" value={filtroAluno} onChange={(e) => setFiltroAluno(e.target.value)}>
                    <option value="">Todos os alunos</option>
                    {alunos.map((a) => (
                      <option key={a.id} value={a.id}>
                        {a.nome}
                      </option>
                    ))}
                  </select>
                </div>
                <div className="form-field">
                  <select
                    className="form-select"
                    value={filtroStatus}
                    onChange={(e) => setFiltroStatus(e.target.value)}
                  >
                    <option value="">Todos os status</option>
                    {Object.entries(STATUS_AUTORIZACAO).map(([valor, cfg]) => (
                      <option key={valor} value={valor}>
                        {cfg.label}
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
                    setBusca("");
                    setFiltroAluno("");
                    setFiltroStatus("");
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
                  <th>Pessoa</th>
                  <th>Aluno</th>
                  <th>Status</th>
                  <th>Vigência</th>
                  <th>Dias</th>
                  <th>Faixa de horário</th>
                  <th>Ações</th>
                </tr>
              </thead>
              <tbody>
                <LinhasEstado
                  colSpan={7}
                  carregando={carregando}
                  erro={erro}
                  vazio={itens.length === 0}
                  icone="UserCheck"
                  tituloVazio="Nenhuma autorização encontrada"
                  textoVazio="Autorize ao menos uma pessoa por aluno para permitir a retirada."
                  onTentarNovamente={() => carregar(pagina)}
                />
                {!carregando &&
                  !erro &&
                  itens.map((item) => (
                    <tr key={item.id}>
                      <td>
                        <strong>{item.pessoaNome || nomePessoa(item.pessoaAutorizadaId)}</strong>
                        {!item.permanente && (
                          <span className="badge badge-warning" style={{ marginLeft: 6 }}>
                            Temporária
                          </span>
                        )}
                      </td>
                      <td className="td-muted">{item.alunoNome || nomeAluno(item.alunoId)}</td>
                      <td>
                        <StatusAutorizacaoBadge status={item.status} />
                        {item.motivoUltimaAlteracao && <div className="ac-meta">{item.motivoUltimaAlteracao}</div>}
                      </td>
                      <td className="td-muted">
                        {formatarData(item.vigenciaInicio)} →{" "}
                        {item.vigenciaFim ? formatarData(item.vigenciaFim) : "sem fim"}
                      </td>
                      <td className="td-muted">{nomesDosDias(item.diasSemana)}</td>
                      <td className="ac-mono">
                        {item.horaInicio && item.horaFim
                          ? `${formatarHora(item.horaInicio)}–${formatarHora(item.horaFim)}`
                          : "sem restrição"}
                      </td>
                      <td>{acoesDaLinha(item)}</td>
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
        </>
      )}

      <Modal
        isOpen={modalAberto}
        onClose={() => setModalAberto(false)}
        title={editando ? "Editar autorização" : "Nova autorização"}
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
              <label className="form-label required">Aluno</label>
              <select
                className={`form-select ${erros.alunoId ? "error" : ""}`}
                value={form.alunoId}
                onChange={(e) => {
                  setForm((p) => ({ ...p, alunoId: e.target.value }));
                  if (erros.alunoId) setErros((p) => ({ ...p, alunoId: "" }));
                }}
              >
                <option value="">Selecione o aluno</option>
                {alunos.map((a) => (
                  <option key={a.id} value={a.id}>
                    {a.nome}
                  </option>
                ))}
              </select>
              {erros.alunoId && <span className="form-error">{erros.alunoId}</span>}
            </div>
            <div className="form-field">
              <label className="form-label required">Pessoa autorizada</label>
              <select
                className={`form-select ${erros.pessoaAutorizadaId ? "error" : ""}`}
                value={form.pessoaAutorizadaId}
                onChange={(e) => {
                  setForm((p) => ({ ...p, pessoaAutorizadaId: e.target.value }));
                  if (erros.pessoaAutorizadaId) setErros((p) => ({ ...p, pessoaAutorizadaId: "" }));
                }}
              >
                <option value="">Selecione a pessoa</option>
                {pessoas.map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.nome}
                    {p.parentesco ? ` — ${p.parentesco}` : ""}
                  </option>
                ))}
              </select>
              {erros.pessoaAutorizadaId && <span className="form-error">{erros.pessoaAutorizadaId}</span>}
            </div>
          </div>

          <label className="form-checkbox">
            <input
              type="checkbox"
              checked={form.temporaria}
              onChange={(e) => {
                const v = e.target.checked;
                setForm((p) => ({ ...p, temporaria: v }));
                if (!v && erros.vigenciaFim) setErros((p) => ({ ...p, vigenciaFim: "" }));
              }}
            />
            <span>Autorização temporária</span>
          </label>
          {form.temporaria && (
            <Aviso tipo="alerta">
              Marcada como temporária, a <strong>data de fim passa a ser obrigatória</strong>: ao chegar
              nela a autorização expira sozinha e a portaria volta a barrar a retirada.
            </Aviso>
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
              <label className={`form-label ${form.temporaria ? "required" : ""}`}>Vigência — fim</label>
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
                <span className="form-hint">
                  {form.temporaria ? "Obrigatória para autorizações temporárias." : "Em branco = sem prazo."}
                </span>
              )}
            </div>
          </div>

          <div className="form-field">
            <label className="form-label">Dias em que pode retirar</label>
            <DiasSemanaChips
              value={form.diasSemana}
              onChange={(v) => setForm((p) => ({ ...p, diasSemana: v }))}
            />
            <span className="form-hint">Selecionados: {nomesDosDias(form.diasSemana)}</span>
          </div>

          <div className="form-grid-2">
            <div className="form-field">
              <label className="form-label">Faixa de horário — de</label>
              <input
                className="form-input"
                type="time"
                value={form.horaInicio}
                onChange={(e) => {
                  setForm((p) => ({ ...p, horaInicio: e.target.value }));
                  if (erros.horaFim) setErros((p) => ({ ...p, horaFim: "" }));
                }}
              />
            </div>
            <div className="form-field">
              <label className="form-label">Faixa de horário — até</label>
              <input
                className={`form-input ${erros.horaFim ? "error" : ""}`}
                type="time"
                value={form.horaFim}
                onChange={(e) => {
                  setForm((p) => ({ ...p, horaFim: e.target.value }));
                  if (erros.horaFim) setErros((p) => ({ ...p, horaFim: "" }));
                }}
              />
              {erros.horaFim ? (
                <span className="form-error">{erros.horaFim}</span>
              ) : (
                <span className="form-hint">Sem horário = pode retirar a qualquer hora do dia permitido.</span>
              )}
            </div>
          </div>

          <div className="form-field">
            <label className="form-label">Observações</label>
            <textarea
              className="form-textarea"
              rows={2}
              value={form.observacoes}
              onChange={(e) => setForm((p) => ({ ...p, observacoes: e.target.value }))}
            />
          </div>
        </div>
      </Modal>

      <ConfirmarModal
        aberto={!!acao}
        titulo={configAcao?.titulo || ""}
        textoConfirmar={configAcao?.botao || "Confirmar"}
        variante={configAcao?.variante || "btn-danger"}
        exigeMotivo={!!configAcao?.exigeMotivo}
        processando={processandoAcao}
        erro={erroAcao}
        onConfirmar={executarAcao}
        onCancelar={() => setAcao(null)}
      >
        {acao && (
          <>
            <p>
              <strong>{acao.item.pessoaNome || nomePessoa(acao.item.pessoaAutorizadaId)}</strong> —{" "}
              {acao.item.alunoNome || nomeAluno(acao.item.alunoId)}
            </p>
            <p className="ac-meta mt-2">{configAcao?.texto}</p>
          </>
        )}
      </ConfirmarModal>

      <Modal
        isOpen={!!historico}
        onClose={() => setHistorico(null)}
        title="Histórico da autorização"
        footer={
          <button className="btn btn-secondary" onClick={() => setHistorico(null)}>
            Fechar
          </button>
        }
      >
        {historico && (
          <>
            <div className="ac-kv mb-4">
              <div className="ac-kv-item">
                <span className="ac-kv-rot">Pessoa</span>
                <span className="ac-kv-val">
                  {historico.item.pessoaNome || nomePessoa(historico.item.pessoaAutorizadaId)}
                </span>
              </div>
              <div className="ac-kv-item">
                <span className="ac-kv-rot">Aluno</span>
                <span className="ac-kv-val">{historico.item.alunoNome || nomeAluno(historico.item.alunoId)}</span>
              </div>
              <div className="ac-kv-item">
                <span className="ac-kv-rot">Status atual</span>
                <span className="ac-kv-val">
                  <StatusAutorizacaoBadge status={historico.item.status} />
                </span>
              </div>
            </div>

            {historico.carregando || historico.erro || historico.itens.length === 0 ? (
              <BlocoEstado
                carregando={historico.carregando}
                erro={historico.erro}
                vazio={historico.itens.length === 0}
                icone="Activity"
                tituloVazio="Sem alterações registradas"
                textoVazio="Toda aprovação, suspensão ou revogação aparece aqui."
              />
            ) : (
              <div className="ac-hist-lista">
                {historico.itens.map((h, i) => (
                  <div key={h.id || i} className="ac-hist-item">
                    <span className="ac-hist-quando">{formatarDataHora(h.dataHora || h.criadoEm)}</span>
                    <span>
                      <strong>{h.acao || h.statusNovo}</strong>
                      {h.statusAnterior && h.statusNovo && (
                        <span className="ac-meta">
                          {" "}
                          ({h.statusAnterior} → {h.statusNovo})
                        </span>
                      )}
                      <div className="ac-meta">
                        {h.usuarioNome || "—"}
                        {h.motivo ? ` · ${h.motivo}` : ""}
                      </div>
                    </span>
                  </div>
                ))}
              </div>
            )}
          </>
        )}
      </Modal>
    </div>
  );
}
