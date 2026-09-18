import { useState, useEffect, useCallback } from "react";
import { accessApi, carregarAuxiliar, comoLista, comoTotal, qs } from "../../services/accessCadastrosApi";
import { Modal } from "../../components/Modal";
import { Pagination } from "../../components/Pagination";
import { Icon } from "../../components/Icon";
import { LinhasEstado } from "../../components/access/EstadoLista";
import { Feedback } from "../../components/access/Feedback";
import { exportarCsv } from "../../utils/exportCsv";
import { formatarDataHora, hojeIso } from "../../utils/tempo";
import "../../styles/accessCadastros.css";

const PAGE_SIZE = 20;

const TIPOS = [
  { valor: "ACESSO_NEGADO", label: "Acesso negado" },
  { valor: "RETIRADA_NAO_AUTORIZADA", label: "Tentativa de retirada não autorizada" },
  { valor: "ALUNO_NAO_RETIRADO", label: "Aluno não retirado no horário" },
  { valor: "EQUIPAMENTO", label: "Falha de equipamento" },
  { valor: "MARCACAO_INCONSISTENTE", label: "Marcação inconsistente" },
  { valor: "OUTRO", label: "Outro" },
];

const GRAVIDADES = [
  { valor: "BAIXA", label: "Baixa", classe: "badge-secondary" },
  { valor: "MEDIA", label: "Média", classe: "badge-info" },
  { valor: "ALTA", label: "Alta", classe: "badge-warning" },
  { valor: "CRITICA", label: "Crítica", classe: "badge-danger" },
];

const STATUS = [
  { valor: "ABERTA", label: "Aberta", classe: "badge-danger" },
  { valor: "EM_ANALISE", label: "Em análise", classe: "badge-warning" },
  { valor: "TRATADA", label: "Tratada", classe: "badge-success" },
  { valor: "CANCELADA", label: "Cancelada", classe: "badge-secondary" },
];

const FORM_VAZIO = {
  tipo: "OUTRO",
  gravidade: "MEDIA",
  alunoId: "",
  dataHora: "",
  descricao: "",
};

export function OcorrenciasPage() {
  const [itens, setItens] = useState([]);
  const [alunos, setAlunos] = useState([]);
  const [total, setTotal] = useState(0);
  const [pagina, setPagina] = useState(0);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState("");

  const [filtroTipo, setFiltroTipo] = useState("");
  const [filtroGravidade, setFiltroGravidade] = useState("");
  const [filtroStatus, setFiltroStatus] = useState("ABERTA");
  const [busca, setBusca] = useState("");

  const [modalAberto, setModalAberto] = useState(false);
  const [form, setForm] = useState(FORM_VAZIO);
  const [erros, setErros] = useState({});
  const [salvando, setSalvando] = useState(false);
  const [erroForm, setErroForm] = useState("");

  const [tratar, setTratar] = useState(null);
  const [formTrativa, setFormTrativa] = useState({ tratativa: "", status: "TRATADA" });
  const [errosTrativa, setErrosTrativa] = useState({});
  const [tratando, setTratando] = useState(false);
  const [erroTrativa, setErroTrativa] = useState("");

  const [detalhe, setDetalhe] = useState(null);
  const [feedback, setFeedback] = useState(null);

  const carregar = useCallback(
    async (p = 0) => {
      setCarregando(true);
      setErro("");
      const r = await accessApi.get(
        `/access/ocorrencias?${qs({
          page: p,
          size: PAGE_SIZE,
          tipo: filtroTipo,
          gravidade: filtroGravidade,
          status: filtroStatus,
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
    [filtroTipo, filtroGravidade, filtroStatus, busca]
  );

  useEffect(() => {
    carregar(0);
    carregarAuxiliar("/alunos?size=500").then(setAlunos);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const rotulo = (lista, valor) => lista.find((x) => x.valor === valor)?.label || valor || "—";
  const classe = (lista, valor) => lista.find((x) => x.valor === valor)?.classe || "badge-secondary";

  const abrirNova = () => {
    setForm({ ...FORM_VAZIO, dataHora: `${hojeIso()}T${new Date().toTimeString().slice(0, 5)}` });
    setErros({});
    setErroForm("");
    setModalAberto(true);
  };

  const validar = () => {
    const e = {};
    if (!form.tipo) e.tipo = "Escolha o tipo.";
    if (!form.gravidade) e.gravidade = "Escolha a gravidade.";
    if (!form.dataHora) e.dataHora = "Informe quando aconteceu.";
    if (form.descricao.trim().length < 10) e.descricao = "Descreva o que aconteceu (mínimo 10 caracteres).";
    setErros(e);
    return Object.keys(e).length === 0;
  };

  const salvar = async () => {
    if (!validar()) return;
    setSalvando(true);
    setErroForm("");
    const r = await accessApi.post("/access/ocorrencias", {
      tipo: form.tipo,
      gravidade: form.gravidade,
      alunoId: form.alunoId || null,
      dataHora: form.dataHora,
      descricao: form.descricao.trim(),
    });
    setSalvando(false);
    if (!r.ok) {
      setErroForm(r.erro);
      return;
    }
    setModalAberto(false);
    setFeedback({ tipo: "sucesso", mensagem: "Ocorrência registrada." });
    carregar(pagina);
  };

  const abrirTratamento = (item) => {
    setTratar(item);
    setFormTrativa({ tratativa: "", status: "TRATADA" });
    setErrosTrativa({});
    setErroTrativa("");
  };

  const salvarTratamento = async () => {
    const e = {};
    if (formTrativa.tratativa.trim().length < 10)
      e.tratativa = "Descreva o que foi feito (mínimo 10 caracteres) — é o que fica no histórico.";
    setErrosTrativa(e);
    if (Object.keys(e).length > 0) return;

    setTratando(true);
    setErroTrativa("");
    const r = await accessApi.post(`/access/ocorrencias/${tratar.id}/tratar`, {
      tratativa: formTrativa.tratativa.trim(),
      status: formTrativa.status,
    });
    setTratando(false);
    if (!r.ok) {
      setErroTrativa(r.erro);
      return;
    }
    setTratar(null);
    setFeedback({ tipo: "sucesso", mensagem: "Tratativa registrada." });
    carregar(pagina);
  };

  const exportar = () =>
    exportarCsv(
      "ocorrencias",
      [
        { key: "dataHora", label: "Data e hora", format: formatarDataHora },
        { key: "tipo", label: "Tipo", format: (v) => rotulo(TIPOS, v) },
        { key: "gravidade", label: "Gravidade", format: (v) => rotulo(GRAVIDADES, v) },
        { key: "alunoNome", label: "Aluno" },
        { key: "status", label: "Status", format: (v) => rotulo(STATUS, v) },
        { key: "descricao", label: "Descrição" },
        { key: "tratadaPor", label: "Tratada por" },
        { key: "tratativa", label: "Tratativa" },
      ],
      itens
    );

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1 className="page-title">Ocorrências</h1>
          <p className="page-subtitle">Eventos do controle de acesso que exigem alguém olhando</p>
        </div>
        <div className="ac-linha-acoes">
          <button className="btn btn-secondary" onClick={exportar} disabled={itens.length === 0}>
            <Icon name="Download" size={14} /> CSV
          </button>
          <button className="btn btn-brand" onClick={abrirNova}>
            <Icon name="Plus" size={14} /> Nova Ocorrência
          </button>
        </div>
      </div>

      <Feedback {...(feedback || {})} onFechar={() => setFeedback(null)} />

      <div className="card">
        <div className="card-body">
          <div className="filter-bar">
            <div className="form-field" style={{ flex: 1 }}>
              <input
                className="form-input"
                placeholder="Buscar na descrição..."
                value={busca}
                onChange={(e) => setBusca(e.target.value)}
                onKeyDown={(e) => e.key === "Enter" && carregar(0)}
              />
            </div>
            <div className="form-field">
              <select className="form-select" value={filtroTipo} onChange={(e) => setFiltroTipo(e.target.value)}>
                <option value="">Todos os tipos</option>
                {TIPOS.map((t) => (
                  <option key={t.valor} value={t.valor}>
                    {t.label}
                  </option>
                ))}
              </select>
            </div>
            <div className="form-field">
              <select
                className="form-select"
                value={filtroGravidade}
                onChange={(e) => setFiltroGravidade(e.target.value)}
              >
                <option value="">Todas as gravidades</option>
                {GRAVIDADES.map((g) => (
                  <option key={g.valor} value={g.valor}>
                    {g.label}
                  </option>
                ))}
              </select>
            </div>
            <div className="form-field">
              <select className="form-select" value={filtroStatus} onChange={(e) => setFiltroStatus(e.target.value)}>
                <option value="">Todos os status</option>
                {STATUS.map((s) => (
                  <option key={s.valor} value={s.valor}>
                    {s.label}
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
                setFiltroTipo("");
                setFiltroGravidade("");
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
              <th>Quando</th>
              <th>Tipo</th>
              <th>Gravidade</th>
              <th>Aluno</th>
              <th>Descrição</th>
              <th>Status</th>
              <th>Tratamento</th>
              <th>Ações</th>
            </tr>
          </thead>
          <tbody>
            <LinhasEstado
              colSpan={8}
              carregando={carregando}
              erro={erro}
              vazio={itens.length === 0}
              icone="AlertCircle"
              tituloVazio="Nenhuma ocorrência"
              textoVazio="Nada pendente com os filtros atuais — o que é bom sinal."
              onTentarNovamente={() => carregar(pagina)}
            />
            {!carregando &&
              !erro &&
              itens.map((item) => (
                <tr key={item.id}>
                  <td className="td-muted">{formatarDataHora(item.dataHora)}</td>
                  <td>{rotulo(TIPOS, item.tipo)}</td>
                  <td>
                    <span className={`badge ${classe(GRAVIDADES, item.gravidade)}`}>
                      {rotulo(GRAVIDADES, item.gravidade)}
                    </span>
                  </td>
                  <td className="td-muted">{item.alunoNome || "—"}</td>
                  <td className="td-muted truncate" style={{ maxWidth: 260 }}>
                    {item.descricao}
                  </td>
                  <td>
                    <span className={`badge ${classe(STATUS, item.status)}`}>{rotulo(STATUS, item.status)}</span>
                  </td>
                  <td className="td-muted">
                    {item.tratadaPor ? (
                      <>
                        {item.tratadaPor}
                        <div className="ac-meta">{formatarDataHora(item.tratadaEm)}</div>
                      </>
                    ) : (
                      "—"
                    )}
                  </td>
                  <td>
                    <div className="ac-linha-acoes">
                      <button className="btn btn-ghost btn-sm" title="Ver detalhes" onClick={() => setDetalhe(item)}>
                        <Icon name="Eye" size={13} />
                      </button>
                      {item.status !== "TRATADA" && item.status !== "CANCELADA" && (
                        <button className="btn btn-secondary btn-sm" onClick={() => abrirTratamento(item)}>
                          <Icon name="CheckSquare" size={13} /> Tratar
                        </button>
                      )}
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
        title="Nova ocorrência"
        footer={
          <>
            <button className="btn btn-secondary" onClick={() => setModalAberto(false)}>
              Cancelar
            </button>
            <button className="btn btn-brand" onClick={salvar} disabled={salvando}>
              {salvando ? "Salvando..." : "Registrar"}
            </button>
          </>
        }
      >
        <Feedback tipo="erro" mensagem={erroForm} />
        <div className="form-grid">
          <div className="form-grid-2">
            <div className="form-field">
              <label className="form-label required">Tipo</label>
              <select
                className="form-select"
                value={form.tipo}
                onChange={(e) => setForm((p) => ({ ...p, tipo: e.target.value }))}
              >
                {TIPOS.map((t) => (
                  <option key={t.valor} value={t.valor}>
                    {t.label}
                  </option>
                ))}
              </select>
            </div>
            <div className="form-field">
              <label className="form-label required">Gravidade</label>
              <select
                className="form-select"
                value={form.gravidade}
                onChange={(e) => setForm((p) => ({ ...p, gravidade: e.target.value }))}
              >
                {GRAVIDADES.map((g) => (
                  <option key={g.valor} value={g.valor}>
                    {g.label}
                  </option>
                ))}
              </select>
            </div>
          </div>

          <div className="form-grid-2">
            <div className="form-field">
              <label className="form-label">Aluno envolvido</label>
              <select
                className="form-select"
                value={form.alunoId}
                onChange={(e) => setForm((p) => ({ ...p, alunoId: e.target.value }))}
              >
                <option value="">Nenhum / não se aplica</option>
                {alunos.map((a) => (
                  <option key={a.id} value={a.id}>
                    {a.nome}
                  </option>
                ))}
              </select>
            </div>
            <div className="form-field">
              <label className="form-label required">Quando aconteceu</label>
              <input
                className={`form-input ${erros.dataHora ? "error" : ""}`}
                type="datetime-local"
                value={form.dataHora}
                onChange={(e) => {
                  setForm((p) => ({ ...p, dataHora: e.target.value }));
                  if (erros.dataHora) setErros((p) => ({ ...p, dataHora: "" }));
                }}
              />
              {erros.dataHora && <span className="form-error">{erros.dataHora}</span>}
            </div>
          </div>

          <div className="form-field">
            <label className="form-label required">Descrição</label>
            <textarea
              className={`form-textarea ${erros.descricao ? "error" : ""}`}
              rows={4}
              value={form.descricao}
              onChange={(e) => {
                setForm((p) => ({ ...p, descricao: e.target.value }));
                if (erros.descricao) setErros((p) => ({ ...p, descricao: "" }));
              }}
            />
            {erros.descricao && <span className="form-error">{erros.descricao}</span>}
          </div>
        </div>
      </Modal>

      <Modal
        isOpen={!!tratar}
        onClose={() => setTratar(null)}
        title="Tratar ocorrência"
        footer={
          <>
            <button className="btn btn-secondary" onClick={() => setTratar(null)}>
              Cancelar
            </button>
            <button className="btn btn-brand" onClick={salvarTratamento} disabled={tratando}>
              {tratando ? "Salvando..." : "Registrar tratativa"}
            </button>
          </>
        }
      >
        {tratar && (
          <>
            <div className="ac-kv mb-4">
              <div className="ac-kv-item">
                <span className="ac-kv-rot">Quando</span>
                <span className="ac-kv-val">{formatarDataHora(tratar.dataHora)}</span>
              </div>
              <div className="ac-kv-item">
                <span className="ac-kv-rot">Tipo</span>
                <span className="ac-kv-val">{rotulo(TIPOS, tratar.tipo)}</span>
              </div>
              <div className="ac-kv-item">
                <span className="ac-kv-rot">Gravidade</span>
                <span className="ac-kv-val">{rotulo(GRAVIDADES, tratar.gravidade)}</span>
              </div>
            </div>
            <p className="ac-meta mb-4">{tratar.descricao}</p>

            <Feedback tipo="erro" mensagem={erroTrativa} />
            <div className="form-grid">
              <div className="form-field">
                <label className="form-label required">O que foi feito</label>
                <textarea
                  className={`form-textarea ${errosTrativa.tratativa ? "error" : ""}`}
                  rows={4}
                  value={formTrativa.tratativa}
                  onChange={(e) => {
                    setFormTrativa((p) => ({ ...p, tratativa: e.target.value }));
                    if (errosTrativa.tratativa) setErrosTrativa((p) => ({ ...p, tratativa: "" }));
                  }}
                  placeholder="Ex.: contato com a mãe às 17h50; aluno retirado pelo pai com autorização temporária."
                />
                {errosTrativa.tratativa ? (
                  <span className="form-error">{errosTrativa.tratativa}</span>
                ) : (
                  <span className="form-hint">Fica registrado com o seu usuário e a data/hora.</span>
                )}
              </div>
              <div className="form-field">
                <label className="form-label required">Situação após o tratamento</label>
                <select
                  className="form-select"
                  value={formTrativa.status}
                  onChange={(e) => setFormTrativa((p) => ({ ...p, status: e.target.value }))}
                >
                  <option value="EM_ANALISE">Em análise — segue pendente</option>
                  <option value="TRATADA">Tratada — resolvida</option>
                  <option value="CANCELADA">Cancelada — registro indevido</option>
                </select>
              </div>
            </div>
          </>
        )}
      </Modal>

      <Modal
        isOpen={!!detalhe}
        onClose={() => setDetalhe(null)}
        title="Detalhes da ocorrência"
        footer={
          <button className="btn btn-secondary" onClick={() => setDetalhe(null)}>
            Fechar
          </button>
        }
      >
        {detalhe && (
          <>
            <div className="ac-kv mb-4">
              <div className="ac-kv-item">
                <span className="ac-kv-rot">Quando</span>
                <span className="ac-kv-val">{formatarDataHora(detalhe.dataHora)}</span>
              </div>
              <div className="ac-kv-item">
                <span className="ac-kv-rot">Tipo</span>
                <span className="ac-kv-val">{rotulo(TIPOS, detalhe.tipo)}</span>
              </div>
              <div className="ac-kv-item">
                <span className="ac-kv-rot">Gravidade</span>
                <span className="ac-kv-val">{rotulo(GRAVIDADES, detalhe.gravidade)}</span>
              </div>
              <div className="ac-kv-item">
                <span className="ac-kv-rot">Status</span>
                <span className="ac-kv-val">{rotulo(STATUS, detalhe.status)}</span>
              </div>
              <div className="ac-kv-item">
                <span className="ac-kv-rot">Aluno</span>
                <span className="ac-kv-val">{detalhe.alunoNome || "—"}</span>
              </div>
              <div className="ac-kv-item">
                <span className="ac-kv-rot">Registrada por</span>
                <span className="ac-kv-val">{detalhe.registradaPor || "Sistema"}</span>
              </div>
            </div>

            <div className="ac-subcard">
              <div className="ac-subcard-titulo">Descrição</div>
              <p className="ac-meta">{detalhe.descricao}</p>
            </div>

            {detalhe.tratativa && (
              <div className="ac-subcard">
                <div className="ac-subcard-titulo">Tratativa</div>
                <p className="ac-meta">{detalhe.tratativa}</p>
                <p className="ac-meta mt-2">
                  Por <strong>{detalhe.tratadaPor || "—"}</strong> em {formatarDataHora(detalhe.tratadaEm)}
                </p>
              </div>
            )}
          </>
        )}
      </Modal>
    </div>
  );
}
