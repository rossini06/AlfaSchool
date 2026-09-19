import { useState, useEffect, useCallback } from "react";
import { accessApi, carregarAuxiliar, comoLista, comoTotal, qs } from "../../services/accessCadastrosApi";
import { Modal } from "../../components/Modal";
import { Pagination } from "../../components/Pagination";
import { Icon } from "../../components/Icon";
import { LinhasEstado, BlocoEstado } from "../../components/access/EstadoLista";
import { Feedback, Aviso } from "../../components/access/Feedback";
import { ConfirmarModal } from "../../components/access/ConfirmarModal";
import { TokenUnico } from "../../components/access/TokenUnico";
import { formatarDataHora } from "../../utils/tempo";
import "../../styles/accessCadastros.css";

const PAGE_SIZE = 20;

const TIPOS_PAINEL = [
  { valor: "SALA", label: "Painel de sala", ajuda: "mostra a chamada da sala para a TV da porta" },
  { valor: "PORTARIA", label: "Painel de portaria", ajuda: "mostra a fila de retirada na entrada" },
];

const FORM_VAZIO = {
  nome: "",
  tipo: "SALA",
  unitId: "",
  salaId: "",
  portariaId: "",
  segundosAtualizacao: "15",
  ativo: true,
};

/**
 * A URL que vai para a TV é pelo SLUG, não pelo id: o painel público é
 * servido por /access/paineis/{slug}/estado e /{slug}/stream. O link
 * copiado apontava para o id e não abria nada.
 */
function urlDoPainel(item) {
  if (item.url) return item.url;
  const base = typeof window !== "undefined" ? window.location.origin : "";
  return `${base}/painel/${item.slug || item.id}`;
}

export function PaineisPage() {
  const [itens, setItens] = useState([]);
  const [unidades, setUnidades] = useState([]);
  const [salas, setSalas] = useState([]);
  const [portarias, setPortarias] = useState([]);
  const [total, setTotal] = useState(0);
  const [pagina, setPagina] = useState(0);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState("");
  const [busca, setBusca] = useState("");

  const [modalAberto, setModalAberto] = useState(false);
  const [editando, setEditando] = useState(null);
  const [form, setForm] = useState(FORM_VAZIO);
  const [erros, setErros] = useState({});
  const [salvando, setSalvando] = useState(false);
  const [erroForm, setErroForm] = useState("");

  const [tvs, setTvs] = useState(null); // { painel, carregando, itens, erro }
  const [novaTv, setNovaTv] = useState("");
  const [erroNovaTv, setErroNovaTv] = useState("");
  const [gerandoTv, setGerandoTv] = useState(false);
  const [token, setToken] = useState(null);
  const [revogarTv, setRevogarTv] = useState(null);
  const [revogando, setRevogando] = useState(false);
  const [erroRevogar, setErroRevogar] = useState("");

  const [excluirId, setExcluirId] = useState(null);
  const [excluindo, setExcluindo] = useState(false);
  const [erroExcluir, setErroExcluir] = useState("");
  const [feedback, setFeedback] = useState(null);
  const [copiado, setCopiado] = useState("");

  const carregar = useCallback(
    async (p = 0) => {
      setCarregando(true);
      setErro("");
      const r = await accessApi.get(`/access/paineis?${qs({ page: p, size: PAGE_SIZE, q: busca })}`);
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
    [busca]
  );

  useEffect(() => {
    carregar(0);
    carregarAuxiliar("/unidades?size=200").then(setUnidades);
    carregarAuxiliar("/access/salas?size=300").then(setSalas);
    carregarAuxiliar("/access/portarias?size=200").then(setPortarias);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const nomeSala = (id) => salas.find((s) => s.id === id)?.nome || "—";
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
      tipo: item.tipo || "SALA",
      unitId: item.unitId || "",
      salaId: item.salaId || "",
      portariaId: item.portariaId || "",
      segundosAtualizacao: String(item.segundosAtualizacao ?? 15),
      ativo: item.ativo !== false,
    });
    setErros({});
    setErroForm("");
    setModalAberto(true);
  };

  const validar = () => {
    const e = {};
    if (!form.nome.trim()) e.nome = "Dê um nome ao painel.";
    if (!form.unitId) e.unitId = "Selecione a unidade.";
    if (form.tipo === "SALA" && !form.salaId) e.salaId = "Painel de sala precisa de uma sala.";
    if (form.tipo === "PORTARIA" && !form.portariaId) e.portariaId = "Painel de portaria precisa de uma portaria.";
    const seg = Number(form.segundosAtualizacao);
    if (!seg || seg < 5 || seg > 300) e.segundosAtualizacao = "Use um intervalo entre 5 e 300 segundos.";
    setErros(e);
    return Object.keys(e).length === 0;
  };

  const salvar = async () => {
    if (!validar()) return;
    setSalvando(true);
    setErroForm("");
    const corpo = {
      nome: form.nome.trim(),
      tipo: form.tipo,
      unitId: form.unitId,
      salaId: form.tipo === "SALA" ? form.salaId : null,
      portariaId: form.tipo === "PORTARIA" ? form.portariaId : null,
      segundosAtualizacao: Number(form.segundosAtualizacao),
      ativo: form.ativo,
    };
    const r = editando
      ? await accessApi.put(`/access/paineis/${editando.id}`, corpo)
      : await accessApi.post("/access/paineis", corpo);
    setSalvando(false);
    if (!r.ok) {
      setErroForm(r.erro);
      return;
    }
    setModalAberto(false);
    setFeedback({ tipo: "sucesso", mensagem: editando ? "Painel atualizado." : "Painel cadastrado." });
    carregar(pagina);
  };

  const abrirTvs = async (painel) => {
    setNovaTv("");
    setErroNovaTv("");
    setTvs({ painel, carregando: true, itens: [], erro: "" });
    const r = await accessApi.get(`/access/paineis/${painel.id}/dispositivos`);
    setTvs({
      painel,
      carregando: false,
      itens: r.ok ? comoLista(r.data) : [],
      erro: r.ok ? "" : r.erro,
    });
  };

  const gerarTokenTv = async () => {
    if (!novaTv.trim()) {
      setErroNovaTv("Dê um nome para a TV — é como você vai reconhecê-la depois.");
      return;
    }
    setGerandoTv(true);
    setErroNovaTv("");
    const r = await accessApi.post(`/access/paineis/${tvs.painel.id}/dispositivos`, { nome: novaTv.trim() });
    setGerandoTv(false);
    if (!r.ok) {
      setErroNovaTv(r.erro);
      return;
    }
    const valor = r.data?.token || "";
    setNovaTv("");
    abrirTvs(tvs.painel);
    if (valor) setToken({ valor, painel: tvs.painel });
    else
      setFeedback({
        tipo: "alerta",
        mensagem: "A TV foi autorizada, mas o servidor não devolveu o token. Gere outro para obter o valor.",
      });
  };

  const confirmarRevogacao = async () => {
    setRevogando(true);
    setErroRevogar("");
    const r = await accessApi.post(`/access/paineis/${tvs.painel.id}/dispositivos/${revogarTv.id}/revogar`, {});
    setRevogando(false);
    if (!r.ok) {
      setErroRevogar(r.erro);
      return;
    }
    const painel = tvs.painel;
    setRevogarTv(null);
    setFeedback({ tipo: "sucesso", mensagem: "Token revogado. A TV para de receber o painel." });
    abrirTvs(painel);
  };

  const excluir = async () => {
    setExcluindo(true);
    setErroExcluir("");
    const r = await accessApi.delete(`/access/paineis/${excluirId}`);
    setExcluindo(false);
    if (!r.ok) {
      setErroExcluir(r.erro);
      return;
    }
    setExcluirId(null);
    setFeedback({ tipo: "sucesso", mensagem: "Painel excluído." });
    carregar(pagina);
  };

  const copiarUrl = async (item) => {
    const url = urlDoPainel(item);
    try {
      if (navigator.clipboard?.writeText) await navigator.clipboard.writeText(url);
      setCopiado(item.id);
      setTimeout(() => setCopiado(""), 2500);
    } catch {
      setFeedback({ tipo: "alerta", mensagem: `Copie manualmente: ${url}` });
    }
  };

  const campo = (k) => (e) => {
    setForm((p) => ({ ...p, [k]: e.target.value }));
    if (erros[k]) setErros((p) => ({ ...p, [k]: "" }));
  };

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1 className="page-title">Painéis e TVs</h1>
          <p className="page-subtitle">Telas que exibem a chamada e a fila de retirada</p>
        </div>
        <button className="btn btn-brand" onClick={abrirNovo}>
          <Icon name="Plus" size={14} /> Novo Painel
        </button>
      </div>

      <Aviso tipo="info">
        Cada TV recebe um token próprio. Assim é possível revogar o acesso de uma televisão trocada ou
        levada para manutenção sem mexer nas outras.
      </Aviso>

      <Feedback {...(feedback || {})} onFechar={() => setFeedback(null)} />

      <div className="card">
        <div className="card-body">
          <div className="filter-bar">
            <div className="form-field" style={{ flex: 1 }}>
              <input
                className="form-input"
                placeholder="Buscar painel..."
                value={busca}
                onChange={(e) => setBusca(e.target.value)}
                onKeyDown={(e) => e.key === "Enter" && carregar(0)}
              />
            </div>
            <button className="btn btn-brand" onClick={() => carregar(0)}>
              <Icon name="Filter" size={14} /> Filtrar
            </button>
            <button
              className="btn btn-secondary"
              onClick={() => {
                setBusca("");
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
              <th>Painel</th>
              <th>Tipo</th>
              <th>Local</th>
              <th>URL do painel</th>
              <th>TVs</th>
              <th>Situação</th>
              <th>Ações</th>
            </tr>
          </thead>
          <tbody>
            <LinhasEstado
              colSpan={7}
              carregando={carregando}
              erro={erro}
              vazio={itens.length === 0}
              icone="Globe"
              tituloVazio="Nenhum painel cadastrado"
              textoVazio="Crie um painel e gere o token da TV que vai exibi-lo."
              onTentarNovamente={() => carregar(pagina)}
            />
            {!carregando &&
              !erro &&
              itens.map((item) => (
                <tr key={item.id}>
                  <td>
                    <strong>{item.nome}</strong>
                    <div className="ac-meta">atualiza a cada {item.segundosAtualizacao ?? 15}s</div>
                  </td>
                  <td>
                    <span className="badge badge-info">
                      {TIPOS_PAINEL.find((t) => t.valor === item.tipo)?.label || item.tipo}
                    </span>
                  </td>
                  <td className="td-muted">
                    {item.tipo === "PORTARIA"
                      ? item.portariaNome || nomePortaria(item.portariaId)
                      : item.salaNome || nomeSala(item.salaId)}
                  </td>
                  <td>
                    <div className="ac-url-painel">
                      <span className="ac-mono truncate">{urlDoPainel(item)}</span>
                      <button className="btn btn-ghost btn-xs" onClick={() => copiarUrl(item)} title="Copiar URL">
                        <Icon name={copiado === item.id ? "Check" : "Copy"} size={12} />
                      </button>
                    </div>
                  </td>
                  <td className="td-muted">{item.totalTvs ?? "—"}</td>
                  <td>
                    <span className={`badge ${item.ativo !== false ? "badge-success" : "badge-danger"}`}>
                      {item.ativo !== false ? "Ativo" : "Inativo"}
                    </span>
                  </td>
                  <td>
                    <div className="ac-linha-acoes">
                      <button className="btn btn-ghost btn-sm" title="TVs autorizadas" onClick={() => abrirTvs(item)}>
                        <Icon name="Cpu" size={13} />
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
        title={editando ? "Editar painel" : "Novo painel"}
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
            <label className="form-label required">Nome</label>
            <input className={`form-input ${erros.nome ? "error" : ""}`} value={form.nome} onChange={campo("nome")} />
            {erros.nome && <span className="form-error">{erros.nome}</span>}
          </div>

          <div className="form-grid-2">
            <div className="form-field">
              <label className="form-label required">Tipo</label>
              <select className="form-select" value={form.tipo} onChange={campo("tipo")}>
                {TIPOS_PAINEL.map((t) => (
                  <option key={t.valor} value={t.valor}>
                    {t.label}
                  </option>
                ))}
              </select>
              <span className="form-hint">{TIPOS_PAINEL.find((t) => t.valor === form.tipo)?.ajuda}</span>
            </div>
            <div className="form-field">
              <label className="form-label required">Unidade</label>
              <select
                className={`form-select ${erros.unitId ? "error" : ""}`}
                value={form.unitId}
                onChange={campo("unitId")}
              >
                <option value="">Selecione a unidade</option>
                {unidades.map((u) => (
                  <option key={u.id} value={u.id}>
                    {u.name || u.nome}
                  </option>
                ))}
              </select>
              {erros.unitId && <span className="form-error">{erros.unitId}</span>}
            </div>
          </div>

          {form.tipo === "SALA" ? (
            <div className="form-field">
              <label className="form-label required">Sala</label>
              <select
                className={`form-select ${erros.salaId ? "error" : ""}`}
                value={form.salaId}
                onChange={campo("salaId")}
              >
                <option value="">Selecione a sala</option>
                {salas.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.nome}
                  </option>
                ))}
              </select>
              {erros.salaId && <span className="form-error">{erros.salaId}</span>}
            </div>
          ) : (
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
          )}

          <div className="form-field">
            <label className="form-label required">Intervalo de atualização (segundos)</label>
            <input
              className={`form-input ${erros.segundosAtualizacao ? "error" : ""}`}
              type="number"
              min="5"
              max="300"
              value={form.segundosAtualizacao}
              onChange={campo("segundosAtualizacao")}
            />
            {erros.segundosAtualizacao && <span className="form-error">{erros.segundosAtualizacao}</span>}
          </div>

          <label className="form-checkbox">
            <input
              type="checkbox"
              checked={form.ativo}
              onChange={(e) => setForm((p) => ({ ...p, ativo: e.target.checked }))}
            />
            <span>Painel ativo</span>
          </label>
        </div>
      </Modal>

      <Modal
        isOpen={!!tvs}
        onClose={() => setTvs(null)}
        title={tvs ? `TVs autorizadas — ${tvs.painel.nome}` : ""}
        size="lg"
        footer={
          <button className="btn btn-secondary" onClick={() => setTvs(null)}>
            Fechar
          </button>
        }
      >
        {tvs && (
          <>
            <div className="ac-url-painel mb-4">
              <Icon name="Globe" size={13} />
              <span className="ac-mono truncate">{urlDoPainel(tvs.painel)}</span>
              <button className="btn btn-ghost btn-xs" onClick={() => copiarUrl(tvs.painel)}>
                <Icon name={copiado === tvs.painel.id ? "Check" : "Copy"} size={12} /> Copiar
              </button>
            </div>

            <div className="ac-subcard">
              <div className="ac-subcard-titulo">
                <Icon name="Plus" size={14} /> Autorizar uma nova TV
              </div>
              <div className="filter-bar">
                <div className="form-field" style={{ flex: 1 }}>
                  <input
                    className={`form-input ${erroNovaTv ? "error" : ""}`}
                    placeholder="Ex.: TV da porta da Sala 12"
                    value={novaTv}
                    onChange={(e) => {
                      setNovaTv(e.target.value);
                      if (erroNovaTv) setErroNovaTv("");
                    }}
                  />
                  {erroNovaTv ? (
                    <span className="form-error">{erroNovaTv}</span>
                  ) : (
                    <span className="form-hint">O token aparece uma única vez, logo após gerar.</span>
                  )}
                </div>
                <button className="btn btn-brand" onClick={gerarTokenTv} disabled={gerandoTv}>
                  <Icon name="Key" size={13} /> {gerandoTv ? "Gerando..." : "Gerar token"}
                </button>
              </div>
            </div>

            <div className="mt-4">
              {tvs.carregando || tvs.erro || tvs.itens.length === 0 ? (
                <BlocoEstado
                  carregando={tvs.carregando}
                  erro={tvs.erro}
                  vazio={tvs.itens.length === 0}
                  icone="Cpu"
                  tituloVazio="Nenhuma TV autorizada"
                  textoVazio="Gere um token para a primeira televisão."
                  onTentarNovamente={() => abrirTvs(tvs.painel)}
                />
              ) : (
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>TV</th>
                      <th>Token gerado em</th>
                      <th>Último acesso</th>
                      <th>Ação</th>
                    </tr>
                  </thead>
                  <tbody>
                    {tvs.itens.map((tv) => (
                      <tr key={tv.id}>
                        <td>
                          <strong>{tv.nome}</strong>
                        </td>
                        <td className="td-muted">{formatarDataHora(tv.criadoEm || tv.geradoEm)}</td>
                        <td className="td-muted">
                          {tv.ultimoAcesso ? formatarDataHora(tv.ultimoAcesso) : "nunca acessou"}
                        </td>
                        <td>
                          <button
                            className="btn btn-ghost btn-sm text-danger"
                            onClick={() => {
                              setErroRevogar("");
                              setRevogarTv(tv);
                            }}
                          >
                            <Icon name="XCircle" size={13} /> Revogar
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>
          </>
        )}
      </Modal>

      <TokenUnico
        aberto={!!token}
        token={token?.valor || ""}
        titulo="Token da TV"
        descricao={
          token
            ? `Abra ${urlDoPainel(token.painel)} na TV e informe este token quando ele for pedido.`
            : ""
        }
        onFechar={() => setToken(null)}
      />

      <ConfirmarModal
        aberto={!!revogarTv}
        titulo="Revogar token da TV"
        textoConfirmar="Revogar"
        processando={revogando}
        erro={erroRevogar}
        onConfirmar={confirmarRevogacao}
        onCancelar={() => setRevogarTv(null)}
      >
        {revogarTv && (
          <p>
            <strong>{revogarTv.nome}</strong> para de exibir o painel imediatamente. Para voltar, será
            preciso gerar um token novo.
          </p>
        )}
      </ConfirmarModal>

      <ConfirmarModal
        aberto={!!excluirId}
        titulo="Excluir painel"
        textoConfirmar="Excluir"
        processando={excluindo}
        erro={erroExcluir}
        onConfirmar={excluir}
        onCancelar={() => setExcluirId(null)}
      >
        Todas as TVs autorizadas neste painel deixam de funcionar. Confirma?
      </ConfirmarModal>
    </div>
  );
}
