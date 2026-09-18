import { useState, useEffect, useCallback, useMemo } from "react";
import { accessApi, carregarAuxiliar, comoLista, comoTotal, qs } from "../../services/accessCadastrosApi";
import { Modal } from "../../components/Modal";
import { Pagination } from "../../components/Pagination";
import { Icon } from "../../components/Icon";
import { Avatar } from "../../components/Avatar";
import { LinhasEstado } from "../../components/access/EstadoLista";
import { Feedback, Aviso } from "../../components/access/Feedback";
import { ConfirmarModal } from "../../components/access/ConfirmarModal";
import { formatarData, hojeIso } from "../../utils/tempo";
import "../../styles/accessCadastros.css";

const PAGE_SIZE = 20;
const FORM_VAZIO = { alunoId: "", jornadaId: "", vigenciaInicio: hojeIso(), vigenciaFim: "" };
const LOTE_VAZIO = { turmaId: "", jornadaId: "", vigenciaInicio: hojeIso(), vigenciaFim: "", encerrarVigentes: true };

export function AlunoJornadasPage() {
  const [itens, setItens] = useState([]);
  const [turmas, setTurmas] = useState([]);
  const [jornadas, setJornadas] = useState([]);
  const [alunos, setAlunos] = useState([]);
  const [total, setTotal] = useState(0);
  const [pagina, setPagina] = useState(0);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState("");

  const [filtroTurma, setFiltroTurma] = useState("");
  const [filtroJornada, setFiltroJornada] = useState("");
  const [busca, setBusca] = useState("");

  const [modalAberto, setModalAberto] = useState(false);
  const [editando, setEditando] = useState(null);
  const [form, setForm] = useState(FORM_VAZIO);
  const [erros, setErros] = useState({});
  const [salvando, setSalvando] = useState(false);
  const [erroForm, setErroForm] = useState("");

  const [loteAberto, setLoteAberto] = useState(false);
  const [lote, setLote] = useState(LOTE_VAZIO);
  const [errosLote, setErrosLote] = useState({});
  const [aplicando, setAplicando] = useState(false);
  const [erroLote, setErroLote] = useState("");

  const [excluirId, setExcluirId] = useState(null);
  const [excluindo, setExcluindo] = useState(false);
  const [erroExcluir, setErroExcluir] = useState("");
  const [feedback, setFeedback] = useState(null);

  const carregar = useCallback(
    async (p = 0) => {
      setCarregando(true);
      setErro("");
      const r = await accessApi.get(
        `/access/aluno-jornadas?${qs({
          page: p,
          size: PAGE_SIZE,
          turmaId: filtroTurma,
          jornadaId: filtroJornada,
          search: busca,
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
    [filtroTurma, filtroJornada, busca]
  );

  useEffect(() => {
    carregar(0);
    carregarAuxiliar("/turmas?size=300").then(setTurmas);
    carregarAuxiliar("/access/jornadas?size=200").then(setJornadas);
    carregarAuxiliar("/alunos?size=500").then(setAlunos);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const nomeAluno = (id) => alunos.find((a) => a.id === id)?.nome || "—";
  const nomeJornada = (id) => jornadas.find((j) => j.id === id)?.nome || "—";
  const nomeTurma = (id) => turmas.find((t) => t.id === id)?.nome || "—";

  /** Distribuição das jornadas dentro do recorte atual — deixa visível que a turma é heterogênea. */
  const distribuicao = useMemo(() => {
    const mapa = new Map();
    itens.forEach((i) => {
      const chave = i.jornadaNome || nomeJornada(i.jornadaId);
      mapa.set(chave, (mapa.get(chave) || 0) + 1);
    });
    return [...mapa.entries()].sort((a, b) => b[1] - a[1]);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [itens, jornadas]);

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
      alunoId: item.alunoId || "",
      jornadaId: item.jornadaId || "",
      vigenciaInicio: item.vigenciaInicio || hojeIso(),
      vigenciaFim: item.vigenciaFim || "",
    });
    setErros({});
    setErroForm("");
    setModalAberto(true);
  };

  const validar = () => {
    const e = {};
    if (!form.alunoId) e.alunoId = "Selecione o aluno.";
    if (!form.jornadaId) e.jornadaId = "Selecione a jornada.";
    if (!form.vigenciaInicio) e.vigenciaInicio = "Informe o início da vigência.";
    if (form.vigenciaFim && form.vigenciaInicio && form.vigenciaFim < form.vigenciaInicio)
      e.vigenciaFim = "O fim não pode ser anterior ao início.";
    setErros(e);
    return Object.keys(e).length === 0;
  };

  const salvar = async () => {
    if (!validar()) return;
    setSalvando(true);
    setErroForm("");
    const corpo = {
      alunoId: form.alunoId,
      jornadaId: form.jornadaId,
      vigenciaInicio: form.vigenciaInicio,
      vigenciaFim: form.vigenciaFim || null,
    };
    const r = editando
      ? await accessApi.put(`/access/aluno-jornadas/${editando.id}`, corpo)
      : await accessApi.post("/access/aluno-jornadas", corpo);
    setSalvando(false);
    if (!r.ok) {
      setErroForm(r.erro);
      return;
    }
    setModalAberto(false);
    setFeedback({ tipo: "sucesso", mensagem: editando ? "Vínculo atualizado." : "Jornada vinculada ao aluno." });
    carregar(pagina);
  };

  const validarLote = () => {
    const e = {};
    if (!lote.turmaId) e.turmaId = "Selecione a turma.";
    if (!lote.jornadaId) e.jornadaId = "Selecione a jornada.";
    if (!lote.vigenciaInicio) e.vigenciaInicio = "Informe o início da vigência.";
    if (lote.vigenciaFim && lote.vigenciaInicio && lote.vigenciaFim < lote.vigenciaInicio)
      e.vigenciaFim = "O fim não pode ser anterior ao início.";
    setErrosLote(e);
    return Object.keys(e).length === 0;
  };

  const aplicarLote = async () => {
    if (!validarLote()) return;
    setAplicando(true);
    setErroLote("");
    const r = await accessApi.post("/access/aluno-jornadas/aplicar-turma", {
      turmaId: lote.turmaId,
      jornadaId: lote.jornadaId,
      vigenciaInicio: lote.vigenciaInicio,
      vigenciaFim: lote.vigenciaFim || null,
      encerrarVigentes: lote.encerrarVigentes,
    });
    setAplicando(false);
    if (!r.ok) {
      setErroLote(r.erro);
      return;
    }
    const qtd = r.data?.total ?? r.data?.afetados ?? null;
    setLoteAberto(false);
    setLote(LOTE_VAZIO);
    setFeedback({
      tipo: "sucesso",
      mensagem: qtd
        ? `Jornada aplicada a ${qtd} aluno(s) da turma ${nomeTurma(lote.turmaId)}.`
        : `Jornada aplicada aos alunos da turma ${nomeTurma(lote.turmaId)}.`,
    });
    carregar(0);
  };

  const excluir = async () => {
    setExcluindo(true);
    setErroExcluir("");
    const r = await accessApi.delete(`/access/aluno-jornadas/${excluirId}`);
    setExcluindo(false);
    if (!r.ok) {
      setErroExcluir(r.erro);
      return;
    }
    setExcluirId(null);
    setFeedback({ tipo: "sucesso", mensagem: "Vínculo removido." });
    carregar(pagina);
  };

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1 className="page-title">Jornadas dos Alunos</h1>
          <p className="page-subtitle">Qual carga cada aluno tem contratada e desde quando</p>
        </div>
        <div className="ac-linha-acoes">
          <button className="btn btn-secondary" onClick={() => setLoteAberto(true)}>
            <Icon name="Users" size={14} /> Aplicar a uma turma
          </button>
          <button className="btn btn-brand" onClick={abrirNovo}>
            <Icon name="Plus" size={14} /> Novo Vínculo
          </button>
        </div>
      </div>

      <Aviso tipo="alerta" titulo="A jornada é do aluno, não da turma">
        Dois alunos da mesma turma podem ter jornadas diferentes — um fica em período integral e o
        outro sai ao meio-dia. Aplicar uma jornada à turma inteira é um atalho de digitação: depois
        disso, cada aluno continua podendo ser ajustado individualmente.
      </Aviso>

      <Feedback {...(feedback || {})} onFechar={() => setFeedback(null)} />

      <div className="card">
        <div className="card-body">
          <div className="filter-bar">
            <div className="form-field" style={{ flex: 1 }}>
              <input
                className="form-input"
                placeholder="Buscar aluno..."
                value={busca}
                onChange={(e) => setBusca(e.target.value)}
                onKeyDown={(e) => e.key === "Enter" && carregar(0)}
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
              <select className="form-select" value={filtroJornada} onChange={(e) => setFiltroJornada(e.target.value)}>
                <option value="">Todas as jornadas</option>
                {jornadas.map((j) => (
                  <option key={j.id} value={j.id}>
                    {j.nome}
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
                setFiltroTurma("");
                setFiltroJornada("");
                setTimeout(() => carregar(0), 0);
              }}
            >
              Limpar
            </button>
          </div>

          {!carregando && !erro && distribuicao.length > 0 && (
            <div className="ac-ocupacao-lista mt-3">
              <div className="ac-subcard-titulo">
                <Icon name="BarChart3" size={14} />
                Distribuição nesta página{filtroTurma ? ` — ${nomeTurma(filtroTurma)}` : ""}
              </div>
              {distribuicao.map(([nome, qtd]) => (
                <div key={nome} className="ac-ocupacao-item">
                  <span>{nome}</span>
                  <strong>{qtd} aluno(s)</strong>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      <div className="table-wrapper">
        <table className="data-table">
          <thead>
            <tr>
              <th>Aluno</th>
              <th>Turma</th>
              <th>Jornada</th>
              <th>Vigência</th>
              <th>Situação</th>
              <th>Ações</th>
            </tr>
          </thead>
          <tbody>
            <LinhasEstado
              colSpan={6}
              carregando={carregando}
              erro={erro}
              vazio={itens.length === 0}
              icone="Clock"
              tituloVazio="Nenhum aluno com jornada"
              textoVazio="Vincule a jornada aos alunos para que a permanência seja apurada."
              onTentarNovamente={() => carregar(pagina)}
            />
            {!carregando &&
              !erro &&
              itens.map((item) => {
                const vigente =
                  (!item.vigenciaFim || item.vigenciaFim >= hojeIso()) && item.vigenciaInicio <= hojeIso();
                return (
                  <tr key={item.id}>
                    <td>
                      <div className="flex items-center gap-2">
                        <Avatar foto={item.alunoFoto} nome={item.alunoNome || nomeAluno(item.alunoId)} />
                        <strong>{item.alunoNome || nomeAluno(item.alunoId)}</strong>
                      </div>
                    </td>
                    <td className="td-muted">{item.turmaNome || nomeTurma(item.turmaId)}</td>
                    <td>
                      <span className="badge badge-brand">{item.jornadaNome || nomeJornada(item.jornadaId)}</span>
                    </td>
                    <td className="td-muted">
                      {formatarData(item.vigenciaInicio)} →{" "}
                      {item.vigenciaFim ? formatarData(item.vigenciaFim) : "sem fim"}
                    </td>
                    <td>
                      <span className={`badge ${vigente ? "badge-success" : "badge-secondary"}`}>
                        {vigente ? "Vigente" : "Fora da vigência"}
                      </span>
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
                          title="Remover"
                        >
                          <Icon name="Trash" size={13} />
                        </button>
                      </div>
                    </td>
                  </tr>
                );
              })}
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
        title={editando ? "Editar vínculo" : "Vincular jornada ao aluno"}
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
            <label className="form-label required">Jornada</label>
            <select
              className={`form-select ${erros.jornadaId ? "error" : ""}`}
              value={form.jornadaId}
              onChange={(e) => {
                setForm((p) => ({ ...p, jornadaId: e.target.value }));
                if (erros.jornadaId) setErros((p) => ({ ...p, jornadaId: "" }));
              }}
            >
              <option value="">Selecione a jornada</option>
              {jornadas.map((j) => (
                <option key={j.id} value={j.id}>
                  {j.nome}
                </option>
              ))}
            </select>
            {erros.jornadaId && <span className="form-error">{erros.jornadaId}</span>}
          </div>

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
                <span className="form-hint">Em branco = vigente por tempo indeterminado.</span>
              )}
            </div>
          </div>
        </div>
      </Modal>

      <Modal
        isOpen={loteAberto}
        onClose={() => setLoteAberto(false)}
        title="Aplicar jornada a uma turma"
        footer={
          <>
            <button className="btn btn-secondary" onClick={() => setLoteAberto(false)}>
              Cancelar
            </button>
            <button className="btn btn-brand" onClick={aplicarLote} disabled={aplicando}>
              {aplicando ? "Aplicando..." : "Aplicar a todos"}
            </button>
          </>
        }
      >
        <Aviso tipo="info">
          A jornada será vinculada a todos os alunos matriculados na turma. Alunos que precisam de uma
          carga diferente continuam podendo ser ajustados um a um depois.
        </Aviso>
        <Feedback tipo="erro" mensagem={erroLote} />
        <div className="form-grid">
          <div className="form-grid-2">
            <div className="form-field">
              <label className="form-label required">Turma</label>
              <select
                className={`form-select ${errosLote.turmaId ? "error" : ""}`}
                value={lote.turmaId}
                onChange={(e) => {
                  setLote((p) => ({ ...p, turmaId: e.target.value }));
                  if (errosLote.turmaId) setErrosLote((p) => ({ ...p, turmaId: "" }));
                }}
              >
                <option value="">Selecione a turma</option>
                {turmas.map((t) => (
                  <option key={t.id} value={t.id}>
                    {t.nome}
                  </option>
                ))}
              </select>
              {errosLote.turmaId && <span className="form-error">{errosLote.turmaId}</span>}
            </div>
            <div className="form-field">
              <label className="form-label required">Jornada</label>
              <select
                className={`form-select ${errosLote.jornadaId ? "error" : ""}`}
                value={lote.jornadaId}
                onChange={(e) => {
                  setLote((p) => ({ ...p, jornadaId: e.target.value }));
                  if (errosLote.jornadaId) setErrosLote((p) => ({ ...p, jornadaId: "" }));
                }}
              >
                <option value="">Selecione a jornada</option>
                {jornadas.map((j) => (
                  <option key={j.id} value={j.id}>
                    {j.nome}
                  </option>
                ))}
              </select>
              {errosLote.jornadaId && <span className="form-error">{errosLote.jornadaId}</span>}
            </div>
          </div>
          <div className="form-grid-2">
            <div className="form-field">
              <label className="form-label required">Vigência — início</label>
              <input
                className={`form-input ${errosLote.vigenciaInicio ? "error" : ""}`}
                type="date"
                value={lote.vigenciaInicio}
                onChange={(e) => {
                  setLote((p) => ({ ...p, vigenciaInicio: e.target.value }));
                  if (errosLote.vigenciaInicio) setErrosLote((p) => ({ ...p, vigenciaInicio: "" }));
                }}
              />
              {errosLote.vigenciaInicio && <span className="form-error">{errosLote.vigenciaInicio}</span>}
            </div>
            <div className="form-field">
              <label className="form-label">Vigência — fim</label>
              <input
                className={`form-input ${errosLote.vigenciaFim ? "error" : ""}`}
                type="date"
                value={lote.vigenciaFim}
                onChange={(e) => {
                  setLote((p) => ({ ...p, vigenciaFim: e.target.value }));
                  if (errosLote.vigenciaFim) setErrosLote((p) => ({ ...p, vigenciaFim: "" }));
                }}
              />
              {errosLote.vigenciaFim && <span className="form-error">{errosLote.vigenciaFim}</span>}
            </div>
          </div>
          <label className="form-checkbox">
            <input
              type="checkbox"
              checked={lote.encerrarVigentes}
              onChange={(e) => setLote((p) => ({ ...p, encerrarVigentes: e.target.checked }))}
            />
            <span>Encerrar a jornada vigente dos alunos na véspera do novo início</span>
          </label>
        </div>
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
        O aluno fica sem jornada contratada e deixa de gerar apuração de excedente. Confirma?
      </ConfirmarModal>
    </div>
  );
}
