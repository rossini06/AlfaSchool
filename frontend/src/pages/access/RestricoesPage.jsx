import { useState, useEffect, useCallback } from "react";
import { accessApi, carregarAuxiliar, comoLista, comoTotal, qs } from "../../services/accessCadastrosApi";
import { useAuth } from "../../contexts/AuthContext";
import { Modal } from "../../components/Modal";
import { Pagination } from "../../components/Pagination";
import { Icon } from "../../components/Icon";
import { LinhasEstado } from "../../components/access/EstadoLista";
import { Feedback, Aviso } from "../../components/access/Feedback";
import { ConfirmarModal } from "../../components/access/ConfirmarModal";
import { formatarData, hojeIso } from "../../utils/tempo";
import "../../styles/accessCadastros.css";

const PAGE_SIZE = 20;

/** Quem pode abrir o documento anexado à medida judicial. */
const PAPEIS_DOCUMENTO = ["SUPER_ADMIN", "ADMIN", "DIRETOR", "COORDENADOR", "SECRETARIA"];

const FORM_VAZIO = {
  alunoId: "",
  pessoaNome: "",
  pessoaCpf: "",
  numeroProcesso: "",
  orgaoEmissor: "",
  dataInicio: hojeIso(),
  dataFim: "",
  documentoUrl: "",
  observacoes: "",
  ativa: true,
};

export function RestricoesPage() {
  const { user } = useAuth();
  const podeVerDocumentoPorPapel = (user?.roles || []).some((r) => PAPEIS_DOCUMENTO.includes(r));

  const [itens, setItens] = useState([]);
  const [alunos, setAlunos] = useState([]);
  const [total, setTotal] = useState(0);
  const [pagina, setPagina] = useState(0);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState("");

  const [filtroAluno, setFiltroAluno] = useState("");
  const [filtroSituacao, setFiltroSituacao] = useState("");
  const [busca, setBusca] = useState("");

  const [modalAberto, setModalAberto] = useState(false);
  const [editando, setEditando] = useState(null);
  const [form, setForm] = useState(FORM_VAZIO);
  const [erros, setErros] = useState({});
  const [salvando, setSalvando] = useState(false);
  const [erroForm, setErroForm] = useState("");

  const [encerrar, setEncerrar] = useState(null);
  const [processando, setProcessando] = useState(false);
  const [erroEncerrar, setErroEncerrar] = useState("");
  const [feedback, setFeedback] = useState(null);

  const carregar = useCallback(
    async (p = 0) => {
      setCarregando(true);
      setErro("");
      const r = await accessApi.get(
        `/access/restricoes?${qs({
          page: p,
          size: PAGE_SIZE,
          alunoId: filtroAluno,
          situacao: filtroSituacao,
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
    [filtroAluno, filtroSituacao, busca]
  );

  useEffect(() => {
    carregar(0);
    carregarAuxiliar("/alunos?size=500").then(setAlunos);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const nomeAluno = (id) => alunos.find((a) => a.id === id)?.nome || "—";

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
      pessoaNome: item.pessoaNome || "",
      pessoaCpf: item.pessoaCpf || "",
      numeroProcesso: item.numeroProcesso || "",
      orgaoEmissor: item.orgaoEmissor || "",
      dataInicio: item.dataInicio || hojeIso(),
      dataFim: item.dataFim || "",
      documentoUrl: item.documentoUrl || "",
      observacoes: item.observacoes || "",
      ativa: item.ativa !== false,
    });
    setErros({});
    setErroForm("");
    setModalAberto(true);
  };

  const validar = () => {
    const e = {};
    if (!form.alunoId) e.alunoId = "Selecione o aluno protegido pela medida.";
    if (!form.pessoaNome.trim()) e.pessoaNome = "Informe o nome de quem está impedido.";
    if (!form.numeroProcesso.trim()) e.numeroProcesso = "O número do processo é o que sustenta o bloqueio.";
    if (!form.orgaoEmissor.trim()) e.orgaoEmissor = "Informe a vara ou o órgão que expediu a medida.";
    if (!form.dataInicio) e.dataInicio = "Informe a data de início da vigência.";
    if (form.dataFim && form.dataInicio && form.dataFim < form.dataInicio)
      e.dataFim = "O fim não pode ser anterior ao início.";
    setErros(e);
    return Object.keys(e).length === 0;
  };

  const salvar = async () => {
    if (!validar()) return;
    setSalvando(true);
    setErroForm("");
    const corpo = {
      alunoId: form.alunoId,
      pessoaNome: form.pessoaNome.trim(),
      pessoaCpf: form.pessoaCpf.replace(/\D/g, "") || null,
      numeroProcesso: form.numeroProcesso.trim(),
      orgaoEmissor: form.orgaoEmissor.trim(),
      dataInicio: form.dataInicio,
      dataFim: form.dataFim || null,
      documentoUrl: form.documentoUrl.trim() || null,
      observacoes: form.observacoes.trim() || null,
      ativa: form.ativa,
    };
    const r = editando
      ? await accessApi.put(`/access/restricoes/${editando.id}`, corpo)
      : await accessApi.post("/access/restricoes", corpo);
    setSalvando(false);
    if (!r.ok) {
      setErroForm(r.erro);
      return;
    }
    setModalAberto(false);
    setFeedback({ tipo: "sucesso", mensagem: editando ? "Restrição atualizada." : "Restrição registrada." });
    carregar(pagina);
  };

  const confirmarEncerramento = async (motivo) => {
    setProcessando(true);
    setErroEncerrar("");
    const r = await accessApi.post(`/access/restricoes/${encerrar.id}/encerrar`, { motivo });
    setProcessando(false);
    if (!r.ok) {
      setErroEncerrar(r.erro);
      return;
    }
    setEncerrar(null);
    setFeedback({ tipo: "sucesso", mensagem: "Restrição encerrada. O bloqueio deixa de valer." });
    carregar(pagina);
  };

  const campo = (k) => (e) => {
    setForm((p) => ({ ...p, [k]: e.target.value }));
    if (erros[k]) setErros((p) => ({ ...p, [k]: "" }));
  };

  const vigente = (item) =>
    item.ativa !== false && (!item.dataFim || item.dataFim >= hojeIso()) && item.dataInicio <= hojeIso();

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1 className="page-title">Restrições Judiciais</h1>
          <p className="page-subtitle">Medidas que impedem a aproximação ou a retirada do aluno</p>
        </div>
        <button className="btn btn-brand" onClick={abrirNovo}>
          <Icon name="Plus" size={14} /> Registrar Restrição
        </button>
      </div>

      <Aviso tipo="erro" titulo="A restrição prevalece sobre qualquer autorização" icone="Shield">
        Enquanto vigente, ela <strong>bloqueia a retirada do aluno pela pessoa indicada</strong>, mesmo
        que exista autorização ativa, mesmo que a pessoa seja o pai ou a mãe e mesmo que a família peça
        na hora. Alterar ou encerrar uma restrição exige respaldo documental.
      </Aviso>

      <Feedback {...(feedback || {})} onFechar={() => setFeedback(null)} />

      <div className="card">
        <div className="card-body">
          <div className="filter-bar">
            <div className="form-field" style={{ flex: 1 }}>
              <input
                className="form-input"
                placeholder="Buscar por nome ou processo..."
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
                value={filtroSituacao}
                onChange={(e) => setFiltroSituacao(e.target.value)}
              >
                <option value="">Todas</option>
                <option value="VIGENTE">Somente vigentes</option>
                <option value="ENCERRADA">Encerradas</option>
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
                setFiltroSituacao("");
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
              <th>Aluno</th>
              <th>Pessoa impedida</th>
              <th>Processo</th>
              <th>Órgão</th>
              <th>Vigência</th>
              <th>Situação</th>
              <th>Documento</th>
              <th>Ações</th>
            </tr>
          </thead>
          <tbody>
            <LinhasEstado
              colSpan={8}
              carregando={carregando}
              erro={erro}
              vazio={itens.length === 0}
              icone="Shield"
              tituloVazio="Nenhuma restrição registrada"
              textoVazio="Registre aqui somente medidas com respaldo judicial."
              onTentarNovamente={() => carregar(pagina)}
            />
            {!carregando &&
              !erro &&
              itens.map((item) => {
                const podeVerDocumento = item.documentoVisivel ?? podeVerDocumentoPorPapel;
                return (
                  <tr key={item.id}>
                    <td>
                      <strong>{item.alunoNome || nomeAluno(item.alunoId)}</strong>
                    </td>
                    <td>{item.pessoaNome}</td>
                    <td className="td-muted ac-mono">{item.numeroProcesso}</td>
                    <td className="td-muted">{item.orgaoEmissor}</td>
                    <td className="td-muted">
                      {formatarData(item.dataInicio)} → {item.dataFim ? formatarData(item.dataFim) : "sem prazo"}
                    </td>
                    <td>
                      <span className={`badge ${vigente(item) ? "badge-danger" : "badge-secondary"}`}>
                        {vigente(item) ? "Vigente — bloqueia" : "Encerrada"}
                      </span>
                    </td>
                    <td>
                      {!item.documentoUrl ? (
                        <span className="ac-meta">Sem anexo</span>
                      ) : podeVerDocumento ? (
                        <a
                          className="btn btn-ghost btn-xs"
                          href={item.documentoUrl}
                          target="_blank"
                          rel="noreferrer"
                        >
                          <Icon name="FileText" size={12} /> Abrir
                        </a>
                      ) : (
                        <span className="ac-meta" title="Seu perfil não tem permissão para abrir o documento">
                          <Icon name="Lock" size={12} /> Restrito
                        </span>
                      )}
                    </td>
                    <td>
                      <div className="ac-linha-acoes">
                        <button className="btn btn-ghost btn-sm" onClick={() => abrirEdicao(item)} title="Editar">
                          <Icon name="Edit" size={13} />
                        </button>
                        {vigente(item) && (
                          <button
                            className="btn btn-ghost btn-sm text-warning"
                            title="Encerrar restrição"
                            onClick={() => {
                              setErroEncerrar("");
                              setEncerrar(item);
                            }}
                          >
                            <Icon name="Unlock" size={13} />
                          </button>
                        )}
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
        title={editando ? "Editar restrição judicial" : "Registrar restrição judicial"}
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
          <div className="form-field">
            <label className="form-label required">Aluno protegido</label>
            <select
              className={`form-select ${erros.alunoId ? "error" : ""}`}
              value={form.alunoId}
              onChange={campo("alunoId")}
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

          <div className="form-grid-2">
            <div className="form-field">
              <label className="form-label required">Pessoa impedida</label>
              <input
                className={`form-input ${erros.pessoaNome ? "error" : ""}`}
                value={form.pessoaNome}
                onChange={campo("pessoaNome")}
                placeholder="Nome como consta na decisão"
              />
              {erros.pessoaNome && <span className="form-error">{erros.pessoaNome}</span>}
            </div>
            <div className="form-field">
              <label className="form-label">CPF da pessoa impedida</label>
              <input className="form-input" value={form.pessoaCpf} onChange={campo("pessoaCpf")} inputMode="numeric" />
              <span className="form-hint">Ajuda a portaria a não confundir homônimos.</span>
            </div>
          </div>

          <div className="form-grid-2">
            <div className="form-field">
              <label className="form-label required">Número do processo</label>
              <input
                className={`form-input ${erros.numeroProcesso ? "error" : ""}`}
                value={form.numeroProcesso}
                onChange={campo("numeroProcesso")}
              />
              {erros.numeroProcesso && <span className="form-error">{erros.numeroProcesso}</span>}
            </div>
            <div className="form-field">
              <label className="form-label required">Órgão emissor</label>
              <input
                className={`form-input ${erros.orgaoEmissor ? "error" : ""}`}
                value={form.orgaoEmissor}
                onChange={campo("orgaoEmissor")}
                placeholder="Ex.: 2ª Vara de Família de Vitória"
              />
              {erros.orgaoEmissor && <span className="form-error">{erros.orgaoEmissor}</span>}
            </div>
          </div>

          <div className="form-grid-2">
            <div className="form-field">
              <label className="form-label required">Início da vigência</label>
              <input
                className={`form-input ${erros.dataInicio ? "error" : ""}`}
                type="date"
                value={form.dataInicio}
                onChange={campo("dataInicio")}
              />
              {erros.dataInicio && <span className="form-error">{erros.dataInicio}</span>}
            </div>
            <div className="form-field">
              <label className="form-label">Fim da vigência</label>
              <input
                className={`form-input ${erros.dataFim ? "error" : ""}`}
                type="date"
                value={form.dataFim}
                onChange={campo("dataFim")}
              />
              {erros.dataFim ? (
                <span className="form-error">{erros.dataFim}</span>
              ) : (
                <span className="form-hint">Em branco = vale até decisão em contrário.</span>
              )}
            </div>
          </div>

          <div className="form-field">
            <label className="form-label">Documento (URL do arquivo)</label>
            <input
              className="form-input"
              value={form.documentoUrl}
              onChange={campo("documentoUrl")}
              placeholder="https://..."
            />
            <span className="form-hint">
              <Icon name="Lock" size={11} /> O anexo só é aberto por perfis autorizados — a portaria vê
              apenas o bloqueio, não o conteúdo da decisão.
            </span>
          </div>

          <div className="form-field">
            <label className="form-label">Observações internas</label>
            <textarea className="form-textarea" rows={3} value={form.observacoes} onChange={campo("observacoes")} />
          </div>

          <label className="form-checkbox">
            <input
              type="checkbox"
              checked={form.ativa}
              onChange={(e) => setForm((p) => ({ ...p, ativa: e.target.checked }))}
            />
            <span>Restrição ativa</span>
          </label>
        </div>
      </Modal>

      <ConfirmarModal
        aberto={!!encerrar}
        titulo="Encerrar restrição"
        textoConfirmar="Encerrar restrição"
        variante="btn-warning"
        exigeMotivo
        rotuloMotivo="Respaldo do encerramento"
        dicaMotivo="Informe a decisão ou o documento que autoriza liberar a pessoa."
        processando={processando}
        erro={erroEncerrar}
        onConfirmar={confirmarEncerramento}
        onCancelar={() => setEncerrar(null)}
      >
        {encerrar && (
          <p>
            <strong>{encerrar.pessoaNome}</strong> volta a poder ser autorizada a retirar{" "}
            {encerrar.alunoNome || nomeAluno(encerrar.alunoId)}. O encerramento fica registrado com o
            seu usuário.
          </p>
        )}
      </ConfirmarModal>
    </div>
  );
}
