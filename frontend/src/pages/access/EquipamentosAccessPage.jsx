import { useState, useEffect, useCallback } from "react";
import { accessApi, carregarAuxiliar, comoLista, comoTotal, qs } from "../../services/accessCadastrosApi";
import { Modal } from "../../components/Modal";
import { Pagination } from "../../components/Pagination";
import { Icon } from "../../components/Icon";
import { LinhasEstado } from "../../components/access/EstadoLista";
import { Feedback, Aviso } from "../../components/access/Feedback";
import { ConfirmarModal } from "../../components/access/ConfirmarModal";
import { TokenUnico } from "../../components/access/TokenUnico";
import { formatarDataHora, minutosDesde, formatarDuracao } from "../../utils/tempo";
import "../../styles/accessCadastros.css";

const PAGE_SIZE = 20;

const FUNCOES = [
  { valor: "ALUNO", label: "Alunos", ajuda: "lê somente credenciais de aluno" },
  { valor: "RESPONSAVEL", label: "Responsáveis", ajuda: "lê somente credenciais de responsável" },
  { valor: "MISTO", label: "Misto", ajuda: "lê aluno e responsável no mesmo leitor" },
];
const SENTIDOS = [
  { valor: "ENTRADA", label: "Entrada" },
  { valor: "SAIDA", label: "Saída" },
  { valor: "AMBOS", label: "Entrada e saída" },
];
const MODOS_SINC = [
  { valor: "PUSH", label: "Push (webhook)", ajuda: "o leitor avisa o servidor a cada evento" },
  { valor: "POLLING", label: "Polling", ajuda: "o servidor consulta o leitor periodicamente" },
  { valor: "MANUAL", label: "Manual", ajuda: "a sincronização só acontece quando alguém pede" },
];

const FORM_VAZIO = {
  nome: "",
  portariaId: "",
  funcao: "MISTO",
  sentido: "ENTRADA",
  modoSincronizacao: "PUSH",
  ip: "",
  porta: "80",
  modelo: "",
  serial: "",
  ativo: true,
};

const IPV4 = /^(\d{1,3}\.){3}\d{1,3}$/;

export function EquipamentosAccessPage() {
  const [itens, setItens] = useState([]);
  const [portarias, setPortarias] = useState([]);
  const [total, setTotal] = useState(0);
  const [pagina, setPagina] = useState(0);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState("");

  const [busca, setBusca] = useState("");
  const [filtroPortaria, setFiltroPortaria] = useState("");
  const [filtroStatus, setFiltroStatus] = useState("");

  const [modalAberto, setModalAberto] = useState(false);
  const [editando, setEditando] = useState(null);
  const [form, setForm] = useState(FORM_VAZIO);
  const [erros, setErros] = useState({});
  const [salvando, setSalvando] = useState(false);
  const [erroForm, setErroForm] = useState("");

  const [ocupado, setOcupado] = useState(null); // id do equipamento em ação
  const [token, setToken] = useState(null);
  const [rotacionar, setRotacionar] = useState(null);
  const [erroRotacionar, setErroRotacionar] = useState("");
  const [rotacionando, setRotacionando] = useState(false);

  const [excluirId, setExcluirId] = useState(null);
  const [excluindo, setExcluindo] = useState(false);
  const [erroExcluir, setErroExcluir] = useState("");
  const [feedback, setFeedback] = useState(null);

  const carregar = useCallback(
    async (p = 0) => {
      setCarregando(true);
      setErro("");
      const r = await accessApi.get(
        `/access/equipamentos?${qs({
          page: p,
          size: PAGE_SIZE,
          search: busca,
          portariaId: filtroPortaria,
          status: filtroStatus,
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
    [busca, filtroPortaria, filtroStatus]
  );

  useEffect(() => {
    carregar(0);
    carregarAuxiliar("/access/portarias?size=200").then(setPortarias);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const nomePortaria = (id) => portarias.find((p) => p.id === id)?.nome || "—";

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
      nome: item.nome || "",
      portariaId: item.portariaId || "",
      funcao: item.funcao || "MISTO",
      sentido: item.sentido || "ENTRADA",
      modoSincronizacao: item.modoSincronizacao || "PUSH",
      ip: item.ip || "",
      porta: String(item.porta ?? "80"),
      modelo: item.modelo || "",
      serial: item.serial || "",
      ativo: item.ativo !== false,
    });
    setErros({});
    setErroForm("");
    setModalAberto(true);
  };

  const validar = () => {
    const e = {};
    if (!form.nome.trim()) e.nome = "Dê um nome que a operação reconheça.";
    if (!form.portariaId) e.portariaId = "Selecione a portaria onde o leitor está instalado.";
    if (!form.ip.trim()) e.ip = "Informe o IP do leitor na rede da escola.";
    else if (!IPV4.test(form.ip.trim()) || form.ip.split(".").some((o) => Number(o) > 255))
      e.ip = "IP inválido. Use o formato 192.168.0.10.";
    if (!form.porta || Number(form.porta) < 1 || Number(form.porta) > 65535) e.porta = "Porta entre 1 e 65535.";
    if (!form.serial.trim()) e.serial = "O serial identifica o equipamento no fabricante.";
    setErros(e);
    return Object.keys(e).length === 0;
  };

  const salvar = async () => {
    if (!validar()) return;
    setSalvando(true);
    setErroForm("");
    const corpo = {
      nome: form.nome.trim(),
      portariaId: form.portariaId,
      funcao: form.funcao,
      sentido: form.sentido,
      modoSincronizacao: form.modoSincronizacao,
      ip: form.ip.trim(),
      porta: Number(form.porta),
      modelo: form.modelo.trim() || null,
      serial: form.serial.trim(),
      ativo: form.ativo,
    };
    const r = editando
      ? await accessApi.put(`/access/equipamentos/${editando.id}`, corpo)
      : await accessApi.post("/access/equipamentos", corpo);
    setSalvando(false);
    if (!r.ok) {
      setErroForm(r.erro);
      return;
    }
    setModalAberto(false);
    setFeedback({ tipo: "sucesso", mensagem: editando ? "Equipamento atualizado." : "Equipamento cadastrado." });
    carregar(pagina);
  };

  const testarConexao = async (item) => {
    setOcupado(item.id);
    const r = await accessApi.post(`/access/equipamentos/${item.id}/testar-conexao`, {});
    setOcupado(null);
    if (!r.ok) {
      setFeedback({ tipo: "erro", mensagem: `${item.nome}: ${r.erro}` });
      return;
    }
    const ok = r.data?.online ?? r.data?.sucesso ?? true;
    setFeedback({
      tipo: ok ? "sucesso" : "alerta",
      mensagem: ok
        ? `${item.nome} respondeu${r.data?.latenciaMs ? ` em ${r.data.latenciaMs} ms` : ""}.`
        : `${item.nome} não respondeu. ${r.data?.mensagem || "Verifique rede e alimentação."}`,
    });
    carregar(pagina);
  };

  const sincronizar = async (item) => {
    setOcupado(item.id);
    const r = await accessApi.post(`/access/equipamentos/${item.id}/sincronizar`, {});
    setOcupado(null);
    if (!r.ok) {
      setFeedback({ tipo: "erro", mensagem: `${item.nome}: ${r.erro}` });
      return;
    }
    setFeedback({
      tipo: "sucesso",
      mensagem: r.data?.totalCredenciais
        ? `${item.nome}: ${r.data.totalCredenciais} credencial(is) enviada(s).`
        : `${item.nome}: sincronização disparada.`,
    });
    carregar(pagina);
  };

  const gerarToken = async () => {
    setRotacionando(true);
    setErroRotacionar("");
    const r = await accessApi.post(`/access/equipamentos/${rotacionar.id}/webhook-token`, {});
    setRotacionando(false);
    if (!r.ok) {
      setErroRotacionar(r.erro);
      return;
    }
    const valor = r.data?.token || r.data?.webhookToken || "";
    setRotacionar(null);
    if (valor) {
      setToken({ valor, equipamento: rotacionar.nome });
    } else {
      setFeedback({
        tipo: "alerta",
        mensagem: "O servidor não devolveu o token. Gere novamente ou consulte o log do backend.",
      });
    }
    carregar(pagina);
  };

  const excluir = async () => {
    setExcluindo(true);
    setErroExcluir("");
    const r = await accessApi.delete(`/access/equipamentos/${excluirId}`);
    setExcluindo(false);
    if (!r.ok) {
      setErroExcluir(r.erro);
      return;
    }
    setExcluirId(null);
    setFeedback({ tipo: "sucesso", mensagem: "Equipamento removido." });
    carregar(pagina);
  };

  const campo = (k) => (e) => {
    setForm((p) => ({ ...p, [k]: e.target.value }));
    if (erros[k]) setErros((p) => ({ ...p, [k]: "" }));
  };

  const statusOnline = (item) => {
    const minutos = minutosDesde(item.ultimoHeartbeat);
    const online = item.online ?? (minutos !== null && minutos <= 5);
    return (
      <div>
        <span className={`status-dot ${online ? "online" : "offline"}`} />
        <span style={{ marginLeft: 6, fontWeight: 600 }}>{online ? "Online" : "Offline"}</span>
        <div className="ac-meta">
          {item.ultimoHeartbeat
            ? `último sinal ${formatarDataHora(item.ultimoHeartbeat)}${
                minutos !== null ? ` (há ${formatarDuracao(minutos)})` : ""
              }`
            : "nunca enviou sinal"}
        </div>
      </div>
    );
  };

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1 className="page-title">Leitores de Acesso</h1>
          <p className="page-subtitle">Equipamentos Control iD instalados nas portarias</p>
        </div>
        <button className="btn btn-brand" onClick={abrirNovo}>
          <Icon name="Plus" size={14} /> Novo Equipamento
        </button>
      </div>

      <Feedback {...(feedback || {})} onFechar={() => setFeedback(null)} />

      <div className="card">
        <div className="card-body">
          <div className="filter-bar">
            <div className="form-field" style={{ flex: 1 }}>
              <input
                className="form-input"
                placeholder="Buscar por nome, serial ou IP..."
                value={busca}
                onChange={(e) => setBusca(e.target.value)}
                onKeyDown={(e) => e.key === "Enter" && carregar(0)}
              />
            </div>
            <div className="form-field">
              <select className="form-select" value={filtroPortaria} onChange={(e) => setFiltroPortaria(e.target.value)}>
                <option value="">Todas as portarias</option>
                {portarias.map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.nome}
                  </option>
                ))}
              </select>
            </div>
            <div className="form-field">
              <select className="form-select" value={filtroStatus} onChange={(e) => setFiltroStatus(e.target.value)}>
                <option value="">Todos</option>
                <option value="ONLINE">Somente online</option>
                <option value="OFFLINE">Somente offline</option>
              </select>
            </div>
            <button className="btn btn-brand" onClick={() => carregar(0)}>
              <Icon name="Filter" size={14} /> Filtrar
            </button>
            <button className="btn btn-secondary" onClick={() => carregar(pagina)} title="Recarregar">
              <Icon name="RefreshCw" size={14} />
            </button>
          </div>
        </div>
      </div>

      <div className="table-wrapper">
        <table className="data-table">
          <thead>
            <tr>
              <th>Equipamento</th>
              <th>Portaria</th>
              <th>Função / Sentido</th>
              <th>Rede</th>
              <th>Sincronização</th>
              <th>Status</th>
              <th>Ações</th>
            </tr>
          </thead>
          <tbody>
            <LinhasEstado
              colSpan={7}
              carregando={carregando}
              erro={erro}
              vazio={itens.length === 0}
              icone="Cpu"
              tituloVazio="Nenhum leitor cadastrado"
              textoVazio="Cadastre os leitores para que as passagens comecem a ser registradas."
              onTentarNovamente={() => carregar(pagina)}
            />
            {!carregando &&
              !erro &&
              itens.map((item) => (
                <tr key={item.id}>
                  <td>
                    <strong>{item.nome}</strong>
                    <div className="ac-meta">
                      {item.modelo || "modelo não informado"} · série {item.serial || "—"}
                    </div>
                  </td>
                  <td className="td-muted">{item.portariaNome || nomePortaria(item.portariaId)}</td>
                  <td className="td-muted">
                    <span className="badge badge-info">
                      {FUNCOES.find((f) => f.valor === item.funcao)?.label || item.funcao}
                    </span>
                    <div className="ac-meta">
                      {SENTIDOS.find((s) => s.valor === item.sentido)?.label || item.sentido}
                    </div>
                  </td>
                  <td className="td-muted ac-mono">
                    {item.ip || "—"}
                    {item.porta ? `:${item.porta}` : ""}
                  </td>
                  <td className="td-muted">
                    {MODOS_SINC.find((m) => m.valor === item.modoSincronizacao)?.label || item.modoSincronizacao}
                    {item.webhookTokenGeradoEm && (
                      <div className="ac-meta">token em {formatarDataHora(item.webhookTokenGeradoEm)}</div>
                    )}
                  </td>
                  <td>{statusOnline(item)}</td>
                  <td>
                    <div className="ac-linha-acoes">
                      <button
                        className="btn btn-ghost btn-sm"
                        title="Testar conexão"
                        disabled={ocupado === item.id}
                        onClick={() => testarConexao(item)}
                      >
                        <Icon name="Zap" size={13} />
                      </button>
                      <button
                        className="btn btn-ghost btn-sm"
                        title="Sincronizar credenciais"
                        disabled={ocupado === item.id}
                        onClick={() => sincronizar(item)}
                      >
                        <Icon name="RefreshCw" size={13} />
                      </button>
                      <button
                        className="btn btn-ghost btn-sm"
                        title={item.webhookTokenGeradoEm ? "Rotacionar token de webhook" : "Gerar token de webhook"}
                        onClick={() => {
                          setErroRotacionar("");
                          setRotacionar(item);
                        }}
                      >
                        <Icon name="Key" size={13} />
                      </button>
                      <button className="btn btn-ghost btn-sm" title="Editar" onClick={() => abrirEdicao(item)}>
                        <Icon name="Edit" size={13} />
                      </button>
                      <button
                        className="btn btn-ghost btn-sm text-danger"
                        title="Excluir"
                        onClick={() => {
                          setErroExcluir("");
                          setExcluirId(item.id);
                        }}
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
        title={editando ? "Editar equipamento" : "Novo equipamento"}
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
              <label className="form-label required">Nome</label>
              <input
                className={`form-input ${erros.nome ? "error" : ""}`}
                value={form.nome}
                onChange={campo("nome")}
                placeholder="Ex.: Catraca entrada — Portaria Principal"
              />
              {erros.nome && <span className="form-error">{erros.nome}</span>}
            </div>
            <div className="form-field">
              <label className="form-label required">Portaria</label>
              <select
                className={`form-select ${erros.portariaId ? "error" : ""}`}
                value={form.portariaId}
                onChange={campo("portariaId")}
              >
                <option value="">Selecione a portaria</option>
                {portarias.map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.nome}
                  </option>
                ))}
              </select>
              {erros.portariaId && <span className="form-error">{erros.portariaId}</span>}
            </div>
          </div>

          <div className="form-grid-3">
            <div className="form-field">
              <label className="form-label required">Função</label>
              <select className="form-select" value={form.funcao} onChange={campo("funcao")}>
                {FUNCOES.map((f) => (
                  <option key={f.valor} value={f.valor}>
                    {f.label}
                  </option>
                ))}
              </select>
              <span className="form-hint">{FUNCOES.find((f) => f.valor === form.funcao)?.ajuda}</span>
            </div>
            <div className="form-field">
              <label className="form-label required">Sentido</label>
              <select className="form-select" value={form.sentido} onChange={campo("sentido")}>
                {SENTIDOS.map((s) => (
                  <option key={s.valor} value={s.valor}>
                    {s.label}
                  </option>
                ))}
              </select>
            </div>
            <div className="form-field">
              <label className="form-label required">Sincronização</label>
              <select className="form-select" value={form.modoSincronizacao} onChange={campo("modoSincronizacao")}>
                {MODOS_SINC.map((m) => (
                  <option key={m.valor} value={m.valor}>
                    {m.label}
                  </option>
                ))}
              </select>
              <span className="form-hint">{MODOS_SINC.find((m) => m.valor === form.modoSincronizacao)?.ajuda}</span>
            </div>
          </div>

          <div className="form-grid-2">
            <div className="form-field">
              <label className="form-label required">IP</label>
              <input
                className={`form-input ${erros.ip ? "error" : ""}`}
                value={form.ip}
                onChange={campo("ip")}
                placeholder="192.168.0.10"
              />
              {erros.ip && <span className="form-error">{erros.ip}</span>}
            </div>
            <div className="form-field">
              <label className="form-label required">Porta</label>
              <input
                className={`form-input ${erros.porta ? "error" : ""}`}
                type="number"
                value={form.porta}
                onChange={campo("porta")}
              />
              {erros.porta && <span className="form-error">{erros.porta}</span>}
            </div>
          </div>

          <div className="form-grid-2">
            <div className="form-field">
              <label className="form-label">Modelo</label>
              <input
                className="form-input"
                value={form.modelo}
                onChange={campo("modelo")}
                placeholder="Ex.: iDFace / iDBlock"
              />
            </div>
            <div className="form-field">
              <label className="form-label required">Serial</label>
              <input
                className={`form-input ${erros.serial ? "error" : ""}`}
                value={form.serial}
                onChange={campo("serial")}
              />
              {erros.serial && <span className="form-error">{erros.serial}</span>}
            </div>
          </div>

          <label className="form-checkbox">
            <input
              type="checkbox"
              checked={form.ativo}
              onChange={(e) => setForm((p) => ({ ...p, ativo: e.target.checked }))}
            />
            <span>Equipamento ativo (participa da sincronização)</span>
          </label>
        </div>
      </Modal>

      <ConfirmarModal
        aberto={!!rotacionar}
        titulo={rotacionar?.webhookTokenGeradoEm ? "Rotacionar token de webhook" : "Gerar token de webhook"}
        textoConfirmar={rotacionar?.webhookTokenGeradoEm ? "Rotacionar" : "Gerar"}
        variante="btn-brand"
        processando={rotacionando}
        erro={erroRotacionar}
        onConfirmar={gerarToken}
        onCancelar={() => setRotacionar(null)}
      >
        {rotacionar?.webhookTokenGeradoEm ? (
          <>
            <p>
              O token atual de <strong>{rotacionar.nome}</strong> deixa de funcionar imediatamente. O
              leitor fica sem enviar eventos até ser reconfigurado com o novo valor.
            </p>
            <p className="ac-meta mt-2">O novo token aparece uma única vez, na tela seguinte.</p>
          </>
        ) : (
          <p>
            O token será exibido <strong>uma única vez</strong> para você copiar e configurar no leitor.
          </p>
        )}
      </ConfirmarModal>

      <TokenUnico
        aberto={!!token}
        token={token?.valor || ""}
        titulo={`Token de webhook — ${token?.equipamento || ""}`}
        descricao="Configure este valor no leitor Control iD, no campo de autenticação do webhook de eventos."
        onFechar={() => setToken(null)}
      />

      <ConfirmarModal
        aberto={!!excluirId}
        titulo="Excluir equipamento"
        textoConfirmar="Excluir"
        processando={excluindo}
        erro={erroExcluir}
        onConfirmar={excluir}
        onCancelar={() => setExcluirId(null)}
      >
        O histórico de passagens é mantido, mas o leitor deixa de sincronizar e de aceitar eventos.
        Confirma?
      </ConfirmarModal>
    </div>
  );
}
