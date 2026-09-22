import { useState, useEffect, useCallback, useMemo, useRef } from "react";
import { api } from "../../services/api";
import { Modal } from "../../components/Modal";
import { Icon } from "../../components/Icon";
import { FotoPessoa } from "../../components/access/FotoPessoa";
import { ContadorCard } from "../../components/access/ContadorCard";
import { IndicadorConexao } from "../../components/access/IndicadorConexao";
import { StatusRetiradaBadge } from "../../components/access/StatusRetiradaBadge";
import { useSseAoVivo } from "../../hooks/useSseAoVivo";
import {
  coordenacaoResumo,
  coordenacaoStreamUrl,
  filaRetiradas,
  acaoRetirada,
  criarRetiradaManual,
  normalizarRetirada,
  normalizarResumo,
  pareceEndpointAusente,
} from "../../services/accessApi";
import "../../styles/access.css";

// Token do painel de coordenação, gravado por quem provisiona a estação.
// Existindo o token, a tela usa SSE; sem ele, cai para polling.
const TOKEN_COORDENACAO_KEY = "alfaschool_painel_coordenacao_token";
const POLLING_MS = 10000;
const ESPERA_DESTAQUE_MIN = 10;

const STATUS_OPCOES = [
  { valor: "SOLICITADA", label: "Aguardando" },
  { valor: "PREPARANDO", label: "Preparando" },
  { valor: "PRONTO", label: "Pronto" },
  { valor: "ENTREGUE", label: "Entregue" },
  { valor: "CANCELADA", label: "Cancelada" },
  { valor: "NEGADA", label: "Negada" },
];

const FILTROS_VAZIOS = {
  unitId: "",
  turmaId: "",
  sala: "",
  portaria: "",
  status: "",
  minutos: "",
};

const MANUAL_VAZIO = { alunoId: "", alunoNome: "", retiranteNome: "", motivo: "" };

/**
 * Central de Coordenação — a fila de retirada vista por quem entrega a criança.
 *
 * Ao vivo por SSE quando há token de painel de coordenação; senão, polling de
 * 10s. Os dois caminhos existem e o hook escolhe sozinho.
 */
export function CoordenacaoPage() {
  const [filtros, setFiltros] = useState(FILTROS_VAZIOS);
  const [filtrosAplicados, setFiltrosAplicados] = useState(FILTROS_VAZIOS);

  const [resumo, setResumo] = useState(null);
  const [fila, setFila] = useState([]);
  const [carregando, setCarregando] = useState(true);
  const [erroFila, setErroFila] = useState("");
  const [erroResumo, setErroResumo] = useState("");
  const [semBackend, setSemBackend] = useState(false);

  const [unidades, setUnidades] = useState([]);
  const [turmas, setTurmas] = useState([]);

  const [acaoEmCurso, setAcaoEmCurso] = useState(null); // `${id}:${acao}`
  const [erroAcao, setErroAcao] = useState("");
  const [aviso, setAviso] = useState("");

  const [entregaAlvo, setEntregaAlvo] = useState(null);
  const [motivoAlvo, setMotivoAlvo] = useState(null); // { retirada, acao }
  const [motivoTexto, setMotivoTexto] = useState("");
  const [manualAberto, setManualAberto] = useState(false);

  const [agora, setAgora] = useState(() => Date.now());

  const token = useMemo(() => {
    try {
      return localStorage.getItem(TOKEN_COORDENACAO_KEY) || "";
    } catch {
      return "";
    }
  }, []);

  /* ------------------------ relógio do tempo de espera ------------------ */
  useEffect(() => {
    const t = setInterval(() => setAgora(Date.now()), 10000);
    return () => clearInterval(t);
  }, []);

  /* --------------------------- dados de apoio -------------------------- */
  useEffect(() => {
    let vivo = true;
    (async () => {
      try {
        const dados = await api.get("/unidades?page=0&size=200");
        if (vivo) setUnidades(dados?.content || dados || []);
      } catch {
        /* filtro de unidade fica indisponível; a tela segue funcionando */
      }
      try {
        const dados = await api.get("/turmas?page=0&size=200");
        if (vivo) setTurmas(dados?.content || dados || []);
      } catch {
        /* idem para turmas */
      }
    })();
    return () => {
      vivo = false;
    };
  }, []);

  /* ------------------------------ carga -------------------------------- */
  const filtrosRef = useRef(filtrosAplicados);
  useEffect(() => {
    filtrosRef.current = filtrosAplicados;
  }, [filtrosAplicados]);

  const buscarEstado = useCallback(async () => {
    const f = filtrosRef.current;

    // Só unidade, turma e status vão ao servidor: são os filtros com
    // referência real hoje. Sala, portaria e tempo de espera são aplicados
    // na tela (ver `filaVisivel`).
    const consulta = {};
    if (f.unitId) consulta.unitId = f.unitId;
    if (f.turmaId) consulta.turmaId = f.turmaId;
    if (f.status) consulta.status = f.status;

    const [resResumo, resFila] = await Promise.allSettled([
      coordenacaoResumo(f.unitId),
      filaRetiradas(consulta),
    ]);

    if (resResumo.status === "fulfilled") {
      setResumo(normalizarResumo(resResumo.value));
      setErroResumo("");
    } else {
      setErroResumo(resResumo.reason?.message || "Não foi possível carregar os contadores.");
    }

    if (resFila.status === "fulfilled") {
      const bruto = resFila.value;
      const lista = Array.isArray(bruto) ? bruto : bruto?.content || bruto?.retiradas || [];
      setFila(lista.map(normalizarRetirada).filter(Boolean));
      setErroFila("");
      setSemBackend(false);
    } else {
      setErroFila(resFila.reason?.message || "Não foi possível carregar a fila.");
      setSemBackend(pareceEndpointAusente(resFila.reason));
    }

    setCarregando(false);

    // O hook precisa saber que a rodada falhou para marcar "Sem conexão".
    if (resFila.status === "rejected") throw resFila.reason;
  }, []);

  /* -------------------------- atualização ao vivo ----------------------- */
  const recargaRef = useRef(null);
  const agendarRecarga = useCallback(() => {
    clearTimeout(recargaRef.current);
    recargaRef.current = setTimeout(() => {
      buscarEstado().catch(() => {});
    }, 400);
  }, [buscarEstado]);

  useEffect(() => () => clearTimeout(recargaRef.current), []);

  const eventos = useMemo(
    () => ({
      conectado: () => {},
      "retirada.aberta": agendarRecarga,
      "retirada.preparando": agendarRecarga,
      "retirada.pronto": agendarRecarga,
      "retirada.entregue": agendarRecarga,
      "retirada.cancelada": agendarRecarga,
    }),
    [agendarRecarga]
  );

  const urlStream = useMemo(() => coordenacaoStreamUrl(token), [token]);

  const { conexao, modo, ultimaAtualizacao, reconectarAgora } = useSseAoVivo({
    url: urlStream,
    eventos,
    buscarEstado,
    pollingMs: POLLING_MS,
  });

  const recarregar = useCallback(() => {
    setCarregando(true);
    buscarEstado().catch(() => {});
  }, [buscarEstado]);

  const aplicarFiltros = () => {
    setFiltrosAplicados(filtros);
    filtrosRef.current = filtros;
    recarregar();
  };

  const limparFiltros = () => {
    setFiltros(FILTROS_VAZIOS);
    setFiltrosAplicados(FILTROS_VAZIOS);
    filtrosRef.current = FILTROS_VAZIOS;
    recarregar();
  };

  /* ---------------------------- filtros locais -------------------------- */
  const salasDisponiveis = useMemo(
    () => distintos(fila.map((r) => r.salaNome)),
    [fila]
  );
  const portariasDisponiveis = useMemo(
    () => distintos(fila.map((r) => r.portariaNome)),
    [fila]
  );

  const filaVisivel = useMemo(() => {
    const f = filtrosAplicados;
    const minutos = Number(f.minutos) || 0;
    return fila.filter((r) => {
      if (f.status && normalizarStatus(r.status) !== normalizarStatus(f.status)) return false;
      if (f.sala && r.salaNome !== f.sala) return false;
      if (f.portaria && r.portariaNome !== f.portaria) return false;
      if (minutos > 0 && minutosDeEspera(r, agora) < minutos) return false;
      return true;
    });
  }, [fila, filtrosAplicados, agora]);

  /* ------------------------------- ações -------------------------------- */
  const executar = async (retirada, acao, corpo = {}) => {
    setErroAcao("");
    setAviso("");
    setAcaoEmCurso(`${retirada.id}:${acao}`);
    try {
      await acaoRetirada(retirada.id, acao, corpo);
      setAviso(`${textoAcao(acao)} registrado para ${retirada.aluno?.nome || "o aluno"}.`);
      await buscarEstado().catch(() => {});
      return true;
    } catch (err) {
      setErroAcao(err.message || "Não foi possível concluir a ação.");
      return false;
    } finally {
      setAcaoEmCurso(null);
    }
  };

  const confirmarEntrega = async () => {
    if (!entregaAlvo) return;
    const ok = await executar(entregaAlvo, "entregar");
    if (ok) setEntregaAlvo(null);
  };

  const confirmarMotivo = async () => {
    if (!motivoAlvo) return;
    const texto = motivoTexto.trim();
    if (!texto) return;
    const ok = await executar(motivoAlvo.retirada, motivoAlvo.acao, { motivo: texto });
    if (ok) {
      setMotivoAlvo(null);
      setMotivoTexto("");
    }
  };

  const contadores = resumo || {};

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1 className="page-title">Central de Coordenação</h1>
          <p className="page-subtitle">Fila de retirada e presença ao vivo</p>
        </div>
        <div className="flex items-center gap-3">
          <IndicadorConexao
            estado={conexao}
            ultimaAtualizacao={ultimaAtualizacao}
            rotuloConectado={modo === "sse" ? "Ao vivo" : "Atualizando a cada 10s"}
          />
          <button className="btn btn-secondary" onClick={reconectarAgora} title="Atualizar agora">
            <Icon name="RefreshCw" size={14} />
            Atualizar
          </button>
          <button className="btn btn-brand" onClick={() => setManualAberto(true)}>
            <Icon name="UserPlus" size={14} />
            Retirada manual
          </button>
        </div>
      </div>

      {semBackend && (
        <div className="ac-aviso tom-alerta">
          <Icon name="AlertCircle" size={16} />
          <span>
            O módulo de controle de acesso ainda não está publicado neste servidor. A tela continua
            aberta e volta a preencher sozinha assim que os endpoints existirem.
          </span>
        </div>
      )}

      <div className="ac-contadores">
        <ContadorCard
          label="Alunos presentes"
          valor={contadores.alunosPresentes}
          icone="Users"
          loading={carregando && !resumo}
        />
        <ContadorCard
          label="Aguardando retirada"
          valor={contadores.aguardandoRetirada}
          icone="Clock"
          tom="warning"
          loading={carregando && !resumo}
        />
        <ContadorCard
          label="Horário excedido"
          valor={contadores.horarioExcedido}
          icone="AlertCircle"
          tom="danger"
          loading={carregando && !resumo}
        />
        <ContadorCard
          label="Saídas concluídas"
          valor={contadores.saidasConcluidas}
          icone="CheckCircle"
          tom="success"
          loading={carregando && !resumo}
        />
      </div>

      {erroResumo && !semBackend && (
        <div className="ac-aviso tom-erro">
          <Icon name="AlertCircle" size={16} />
          <span>{erroResumo}</span>
        </div>
      )}

      <div className="card">
        <div className="card-body">
          <div className="filter-bar">
            <div className="form-field">
              <select
                className="form-select"
                value={filtros.unitId}
                onChange={(e) => setFiltros({ ...filtros, unitId: e.target.value })}
                aria-label="Unidade"
              >
                <option value="">Todas as unidades</option>
                {unidades.map((u) => (
                  <option key={u.id} value={u.id}>{u.nome}</option>
                ))}
              </select>
            </div>
            <div className="form-field">
              <select
                className="form-select"
                value={filtros.turmaId}
                onChange={(e) => setFiltros({ ...filtros, turmaId: e.target.value })}
                aria-label="Turma"
              >
                <option value="">Todas as turmas</option>
                {turmas.map((t) => (
                  <option key={t.id} value={t.id}>{t.nome}</option>
                ))}
              </select>
            </div>
            <div className="form-field">
              <select
                className="form-select"
                value={filtros.sala}
                onChange={(e) => setFiltros({ ...filtros, sala: e.target.value })}
                aria-label="Sala"
              >
                <option value="">Todas as salas</option>
                {salasDisponiveis.map((s) => (
                  <option key={s} value={s}>{s}</option>
                ))}
              </select>
            </div>
            <div className="form-field">
              <select
                className="form-select"
                value={filtros.portaria}
                onChange={(e) => setFiltros({ ...filtros, portaria: e.target.value })}
                aria-label="Portaria"
              >
                <option value="">Todas as portarias</option>
                {portariasDisponiveis.map((p) => (
                  <option key={p} value={p}>{p}</option>
                ))}
              </select>
            </div>
            <div className="form-field">
              <select
                className="form-select"
                value={filtros.status}
                onChange={(e) => setFiltros({ ...filtros, status: e.target.value })}
                aria-label="Status"
              >
                <option value="">Todos os status</option>
                {STATUS_OPCOES.map((s) => (
                  <option key={s.valor} value={s.valor}>{s.label}</option>
                ))}
              </select>
            </div>
            <div className="form-field">
              <input
                className="form-input"
                type="number"
                min="0"
                style={{ width: 190 }}
                placeholder="Espera acima de N min"
                aria-label="Espera acima de N minutos"
                value={filtros.minutos}
                onChange={(e) => setFiltros({ ...filtros, minutos: e.target.value })}
              />
            </div>
            <button className="btn btn-brand" onClick={aplicarFiltros}>
              <Icon name="Filter" size={14} />
              Filtrar
            </button>
            <button className="btn btn-secondary" onClick={limparFiltros}>
              Limpar
            </button>
          </div>
          <div className="form-hint mt-2">
            Sala, portaria e tempo de espera são aplicados sobre a fila já carregada.
          </div>
        </div>
      </div>

      {erroAcao && (
        <div className="ac-aviso tom-erro" role="alert">
          <Icon name="AlertCircle" size={16} />
          <span>{erroAcao}</span>
        </div>
      )}
      {aviso && !erroAcao && (
        <div className="ac-aviso tom-info" role="status">
          <Icon name="CheckCircle" size={16} />
          <span>{aviso}</span>
        </div>
      )}

      <div className="card">
        <div className="card-header">
          <h3 className="card-title">Fila de retirada</h3>
          <span className="badge badge-secondary">
            {filaVisivel.length} {filaVisivel.length === 1 ? "retirada" : "retiradas"}
          </span>
        </div>
        <div className="card-body-flush">
          {carregando && fila.length === 0 ? (
            <div style={{ padding: 20 }}>
              {Array.from({ length: 4 }).map((_, i) => (
                <div key={i} className="skeleton skeleton-row" style={{ marginBottom: 8 }} />
              ))}
            </div>
          ) : erroFila ? (
            <div className="empty-state">
              <div className="empty-state-icon">
                <Icon name="AlertCircle" size={28} />
              </div>
              <h3>Não foi possível carregar a fila</h3>
              <p>{erroFila}</p>
              <button className="btn btn-secondary mt-3" onClick={recarregar}>
                <Icon name="RefreshCw" size={14} />
                Tentar de novo
              </button>
            </div>
          ) : filaVisivel.length === 0 ? (
            <div className="empty-state">
              <div className="empty-state-icon">
                <Icon name="CheckCircle" size={28} />
              </div>
              <h3>Nenhuma retirada na fila</h3>
              <p>
                {fila.length > 0
                  ? "Nenhuma retirada atende aos filtros escolhidos."
                  : "Assim que um responsável for reconhecido na portaria, ele aparece aqui."}
              </p>
            </div>
          ) : (
            <div className="ac-fila-lista">
              {filaVisivel.map((r) => (
                <LinhaRetirada
                  key={r.id}
                  retirada={r}
                  agora={agora}
                  acaoEmCurso={acaoEmCurso}
                  onPreparar={() => executar(r, "preparar")}
                  onPronto={() => executar(r, "pronto")}
                  onEntregar={() => setEntregaAlvo(r)}
                  onCancelar={() => {
                    setMotivoTexto("");
                    setMotivoAlvo({ retirada: r, acao: "cancelar" });
                  }}
                  onNegar={() => {
                    setMotivoTexto("");
                    setMotivoAlvo({ retirada: r, acao: "negar" });
                  }}
                />
              ))}
            </div>
          )}
        </div>
      </div>

      <ModalEntrega
        retirada={entregaAlvo}
        salvando={Boolean(entregaAlvo && acaoEmCurso === `${entregaAlvo.id}:entregar`)}
        onFechar={() => setEntregaAlvo(null)}
        onConfirmar={confirmarEntrega}
      />

      <ModalMotivo
        alvo={motivoAlvo}
        texto={motivoTexto}
        onTexto={setMotivoTexto}
        salvando={Boolean(
          motivoAlvo && acaoEmCurso === `${motivoAlvo.retirada.id}:${motivoAlvo.acao}`
        )}
        onFechar={() => {
          setMotivoAlvo(null);
          setMotivoTexto("");
        }}
        onConfirmar={confirmarMotivo}
      />

      <ModalRetiradaManual
        aberto={manualAberto}
        onFechar={() => setManualAberto(false)}
        onRegistrada={(mensagem) => {
          setManualAberto(false);
          setErroAcao("");
          setAviso(mensagem);
          buscarEstado().catch(() => {});
        }}
      />
    </div>
  );
}

/* ============================== linha da fila =========================== */

function LinhaRetirada({
  retirada,
  agora,
  acaoEmCurso,
  onPreparar,
  onPronto,
  onEntregar,
  onCancelar,
  onNegar,
}) {
  const status = normalizarStatus(retirada.status);
  const espera = minutosDeEspera(retirada, agora);
  const excedido = espera >= ESPERA_DESTAQUE_MIN && !["ENTREGUE", "CANCELADA", "NEGADA"].includes(status);
  const ocupada = Boolean(acaoEmCurso && acaoEmCurso.startsWith(`${retirada.id}:`));

  const responsavel = [retirada.retirante?.nome || "Responsável não identificado", retirada.retirante?.parentesco]
    .filter(Boolean)
    .join(" · ");

  return (
    <div className="ac-fila-linha">
      <div className="ac-fila-col-foto">
        <FotoPessoa foto={retirada.aluno?.foto} nome={retirada.aluno?.nome} size={44} />
      </div>

      <div className="ac-fila-col-ident">
        <div className="ac-fila-nome">
          {retirada.aluno?.nome || "Aluno não identificado"}
          {retirada.retiradaManual && (
            <span className="badge badge-secondary" style={{ marginLeft: 8 }}>manual</span>
          )}
        </div>
        <div className="ac-fila-sub">{responsavel}</div>
      </div>

      <div className="ac-fila-col-meta">
        <StatusRetiradaBadge status={retirada.status} />
      </div>

      <div className="ac-fila-meta ac-fila-col-meta">
        <Icon name="Clock" size={13} />
        {horaDe(retirada.solicitadoEm) || "—"}
      </div>

      <div className="ac-fila-meta ac-fila-col-meta">
        <Icon name="Users" size={13} />
        {retirada.turmaNome || retirada.aluno?.turmaNome || "—"}
        {retirada.salaNome ? ` · ${retirada.salaNome}` : ""}
      </div>

      <div className="ac-fila-acoes">
        <span
          className={`ac-espera ${excedido ? "is-excedido" : ""}`}
          title="Tempo desde a chegada do responsável"
        >
          {excedido && <Icon name="AlertCircle" size={13} />}
          {formatarEspera(espera)}
        </span>

        {status === "SOLICITADA" && (
          <button className="btn btn-secondary btn-sm" onClick={onPreparar} disabled={ocupada}>
            Preparar
          </button>
        )}
        {status === "PREPARANDO" && (
          <button className="btn btn-secondary btn-sm" onClick={onPronto} disabled={ocupada}>
            Marcar pronto
          </button>
        )}
        {(status === "PRONTO" || status === "PREPARANDO") && (
          <button className="btn btn-success btn-sm" onClick={onEntregar} disabled={ocupada}>
            <Icon name="Check" size={13} />
            Confirmar entrega
          </button>
        )}
        {!["ENTREGUE", "CANCELADA", "NEGADA"].includes(status) && (
          <>
            <button className="btn btn-ghost btn-sm" onClick={onNegar} disabled={ocupada}>
              Negar
            </button>
            <button className="btn btn-ghost btn-sm text-danger" onClick={onCancelar} disabled={ocupada}>
              Cancelar
            </button>
          </>
        )}
      </div>
    </div>
  );
}

/* ================================= modais =============================== */

function ModalEntrega({ retirada, salvando, onFechar, onConfirmar }) {
  return (
    <Modal
      isOpen={Boolean(retirada)}
      onClose={onFechar}
      title="Confirmar entrega do aluno"
      footer={
        <>
          <button className="btn btn-secondary" onClick={onFechar} disabled={salvando}>
            Voltar
          </button>
          <button className="btn btn-success" onClick={onConfirmar} disabled={salvando}>
            {salvando ? "Registrando…" : "Confirmar entrega"}
          </button>
        </>
      }
    >
      {retirada && (
        <div className="flex flex-col gap-4">
          <div className="ac-aviso tom-alerta">
            <Icon name="AlertCircle" size={16} />
            <span>
              Esta confirmação registra que a criança foi entregue a esta pessoa, com seu nome e o
              horário. Confira o rosto antes de confirmar.
            </span>
          </div>

          <div className="ac-confirmar-dados">
            <FotoPessoa foto={retirada.aluno?.foto} nome={retirada.aluno?.nome} size={64} />
            <div>
              <div className="text-xs text-muted font-bold">ALUNO</div>
              <div className="ac-fila-nome" style={{ fontSize: 16 }}>
                {retirada.aluno?.nome || "—"}
              </div>
              <div className="ac-fila-sub">
                {retirada.turmaNome || retirada.aluno?.turmaNome || "Turma não informada"}
              </div>
            </div>
          </div>

          <div className="ac-confirmar-dados">
            <FotoPessoa foto={retirada.retirante?.foto} nome={retirada.retirante?.nome} size={64} />
            <div>
              <div className="text-xs text-muted font-bold">QUEM ESTÁ RETIRANDO</div>
              <div className="ac-fila-nome" style={{ fontSize: 16 }}>
                {retirada.retirante?.nome || "—"}
              </div>
              <div className="ac-fila-sub">
                {retirada.retirante?.parentesco || "Vínculo não informado"}
                {horaDe(retirada.solicitadoEm) ? ` · chegou às ${horaDe(retirada.solicitadoEm)}` : ""}
              </div>
            </div>
          </div>
        </div>
      )}
    </Modal>
  );
}

function ModalMotivo({ alvo, texto, onTexto, salvando, onFechar, onConfirmar }) {
  const negar = alvo?.acao === "negar";
  const vazio = !texto.trim();

  return (
    <Modal
      isOpen={Boolean(alvo)}
      onClose={onFechar}
      title={negar ? "Negar retirada" : "Cancelar retirada"}
      size="sm"
      footer={
        <>
          <button className="btn btn-secondary" onClick={onFechar} disabled={salvando}>
            Voltar
          </button>
          <button className="btn btn-danger" onClick={onConfirmar} disabled={salvando || vazio}>
            {salvando ? "Registrando…" : negar ? "Negar retirada" : "Cancelar retirada"}
          </button>
        </>
      }
    >
      {alvo && (
        <div className="form-grid">
          <div className="ac-aviso tom-info">
            <Icon name="Info" size={16} />
            <span>
              {negar
                ? "Negar a retirada de "
                : "Cancelar a retirada de "}
              <strong>{alvo.retirada.aluno?.nome || "este aluno"}</strong>. O motivo fica no
              histórico e é auditável.
            </span>
          </div>
          <div className="form-field">
            <label className="form-label required">Motivo</label>
            <textarea
              className="form-textarea"
              value={texto}
              onChange={(e) => onTexto(e.target.value)}
              placeholder={
                negar
                  ? "Ex.: pessoa não consta na lista de autorizados."
                  : "Ex.: responsável desistiu e foi embora."
              }
              maxLength={255}
              autoFocus
            />
            {vazio && <span className="form-hint">O motivo é obrigatório.</span>}
          </div>
        </div>
      )}
    </Modal>
  );
}

function ModalRetiradaManual({ aberto, onFechar, onRegistrada }) {
  const [form, setForm] = useState(MANUAL_VAZIO);
  const [busca, setBusca] = useState("");
  const [resultados, setResultados] = useState([]);
  const [buscando, setBuscando] = useState(false);
  const [salvando, setSalvando] = useState(false);
  const [erro, setErro] = useState("");
  const timer = useRef(null);

  useEffect(() => {
    if (aberto) {
      setForm(MANUAL_VAZIO);
      setBusca("");
      setResultados([]);
      setErro("");
    }
  }, [aberto]);

  useEffect(() => () => clearTimeout(timer.current), []);

  const buscarAlunos = (texto) => {
    setBusca(texto);
    setForm((f) => ({ ...f, alunoId: "", alunoNome: "" }));
    clearTimeout(timer.current);
    if (texto.trim().length < 2) {
      setResultados([]);
      return;
    }
    timer.current = setTimeout(async () => {
      setBuscando(true);
      try {
        const dados = await api.get(
          `/alunos?page=0&size=8&q=${encodeURIComponent(texto.trim())}`
        );
        setResultados(dados?.content || dados || []);
      } catch {
        setResultados([]);
      } finally {
        setBuscando(false);
      }
    }, 350);
  };

  const invalido = !form.alunoId || !form.retiranteNome.trim() || !form.motivo.trim();

  const salvar = async () => {
    setErro("");
    setSalvando(true);
    try {
      await criarRetiradaManual({
        alunoId: form.alunoId,
        retiranteNome: form.retiranteNome.trim(),
        motivo: form.motivo.trim(),
      });
      onRegistrada(`Retirada manual registrada para ${form.alunoNome}.`);
    } catch (err) {
      setErro(err.message || "Não foi possível registrar a retirada manual.");
    } finally {
      setSalvando(false);
    }
  };

  return (
    <Modal
      isOpen={aberto}
      onClose={onFechar}
      title="Retirada manual"
      footer={
        <>
          <button className="btn btn-secondary" onClick={onFechar} disabled={salvando}>
            Cancelar
          </button>
          <button className="btn btn-brand" onClick={salvar} disabled={salvando || invalido}>
            {salvando ? "Registrando…" : "Registrar retirada"}
          </button>
        </>
      }
    >
      <div className="form-grid">
        <div className="ac-aviso tom-alerta">
          <Icon name="AlertCircle" size={16} />
          <span>
            Use apenas quando não houve leitura biométrica. O registro fica marcado como manual e
            gera uma ocorrência para conferência posterior.
          </span>
        </div>

        {erro && (
          <div className="ac-aviso tom-erro" role="alert">
            <Icon name="AlertCircle" size={16} />
            <span>{erro}</span>
          </div>
        )}

        <div className="form-field">
          <label className="form-label required">Aluno</label>
          {form.alunoId ? (
            <div className="flex items-center gap-3">
              <span className="badge badge-brand">{form.alunoNome}</span>
              <button
                className="btn btn-ghost btn-sm"
                onClick={() => {
                  setForm({ ...form, alunoId: "", alunoNome: "" });
                  setBusca("");
                }}
              >
                Trocar
              </button>
            </div>
          ) : (
            <>
              <input
                className="form-input"
                value={busca}
                onChange={(e) => buscarAlunos(e.target.value)}
                placeholder="Digite o nome do aluno…"
                autoComplete="off"
              />
              {buscando && <span className="form-hint">Buscando…</span>}
              {!buscando && busca.trim().length >= 2 && resultados.length === 0 && (
                <span className="form-hint">Nenhum aluno encontrado.</span>
              )}
              {resultados.length > 0 && (
                <div
                  style={{
                    border: "1px solid var(--color-border)",
                    borderRadius: "var(--radius-md)",
                    overflow: "hidden",
                    marginTop: 4,
                  }}
                >
                  {resultados.map((a) => (
                    <button
                      key={a.id}
                      className="dropdown-item"
                      style={{ width: "100%", textAlign: "left" }}
                      onClick={() => {
                        setForm({ ...form, alunoId: a.id, alunoNome: a.nome });
                        setResultados([]);
                      }}
                    >
                      {a.nome}
                    </button>
                  ))}
                </div>
              )}
            </>
          )}
        </div>

        <div className="form-field">
          <label className="form-label required">Quem está retirando</label>
          <input
            className="form-input"
            value={form.retiranteNome}
            onChange={(e) => setForm({ ...form, retiranteNome: e.target.value })}
            placeholder="Nome completo de quem leva o aluno"
          />
        </div>

        <div className="form-field">
          <label className="form-label required">Motivo</label>
          <textarea
            className="form-textarea"
            value={form.motivo}
            onChange={(e) => setForm({ ...form, motivo: e.target.value })}
            placeholder="Ex.: leitor da portaria fora do ar; identidade conferida na secretaria."
            maxLength={255}
          />
        </div>
      </div>
    </Modal>
  );
}

/* ================================ helpers =============================== */

function normalizarStatus(status) {
  if (!status) return "";
  return String(status)
    .normalize("NFD")
    .replace(/[̀-ͯ]/g, "")
    .toUpperCase()
    .replace(/[^A-Z]/g, "");
}

function distintos(lista) {
  return [...new Set(lista.filter(Boolean))].sort((a, b) => a.localeCompare(b));
}

function minutosDeEspera(retirada, agora) {
  if (!retirada?.solicitadoEm) return 0;
  const inicio = new Date(retirada.solicitadoEm).getTime();
  if (Number.isNaN(inicio)) return 0;
  const fim = retirada.entregueEm ? new Date(retirada.entregueEm).getTime() : agora;
  const base = Number.isNaN(fim) ? agora : fim;
  return Math.max(0, Math.floor((base - inicio) / 60000));
}

function formatarEspera(minutos) {
  if (minutos <= 0) return "agora";
  if (minutos < 60) return `${minutos} min`;
  const h = Math.floor(minutos / 60);
  const m = minutos % 60;
  return m === 0 ? `${h} h` : `${h} h ${m} min`;
}

function horaDe(iso) {
  if (!iso) return null;
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return null;
  return d.toLocaleTimeString("pt-BR", { timeZone: "America/Sao_Paulo", hour: "2-digit", minute: "2-digit" });
}

function textoAcao(acao) {
  const mapa = {
    preparar: "Preparo",
    pronto: "Aluno pronto",
    entregar: "Entrega",
    cancelar: "Cancelamento",
    negar: "Negativa",
  };
  return mapa[acao] || "Ação";
}
