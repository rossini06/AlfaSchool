import { useState, useEffect, useCallback } from "react";
import { accessApi, carregarAuxiliar, comoLista, comoTotal, qs } from "../../services/accessCadastrosApi";
import { Modal } from "../../components/Modal";
import { Pagination } from "../../components/Pagination";
import { Icon } from "../../components/Icon";
import { LinhasEstado } from "../../components/access/EstadoLista";
import { Feedback } from "../../components/access/Feedback";
import { ConfirmarModal } from "../../components/access/ConfirmarModal";
import "../../styles/accessCadastros.css";

const PAGE_SIZE = 20;
const TIPOS = [
  { valor: "PRINCIPAL", label: "Principal" },
  { valor: "SECUNDARIA", label: "Secundária" },
];

const FORM_VAZIO = {
  unitId: "",
  nome: "",
  tipo: "PRINCIPAL",
  descricao: "",
  ativo: true,
};

export function PortariasPage() {
  const [itens, setItens] = useState([]);
  const [unidades, setUnidades] = useState([]);
  const [total, setTotal] = useState(0);
  const [pagina, setPagina] = useState(0);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState("");

  const [busca, setBusca] = useState("");
  const [filtroUnidade, setFiltroUnidade] = useState("");
  const [filtroTipo, setFiltroTipo] = useState("");

  const [modalAberto, setModalAberto] = useState(false);
  const [editando, setEditando] = useState(null);
  const [form, setForm] = useState(FORM_VAZIO);
  const [erros, setErros] = useState({});
  const [salvando, setSalvando] = useState(false);
  const [erroForm, setErroForm] = useState("");

  const [excluirId, setExcluirId] = useState(null);
  const [excluindo, setExcluindo] = useState(false);
  const [erroExcluir, setErroExcluir] = useState("");
  const [feedback, setFeedback] = useState(null);

  const carregar = useCallback(
    async (p = 0) => {
      setCarregando(true);
      setErro("");
      const url = `/access/portarias?${qs({
        page: p,
        size: PAGE_SIZE,
        q: busca,
        unitId: filtroUnidade,
        tipo: filtroTipo,
      })}`;
      const r = await accessApi.get(url);
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
    [busca, filtroUnidade, filtroTipo]
  );

  useEffect(() => {
    carregar(0);
    carregarAuxiliar("/unidades?size=200").then(setUnidades);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const nomeUnidade = (id) => {
    const u = unidades.find((x) => x.id === id);
    return u ? u.name || u.nome : "—";
  };

  const abrirNovo = () => {
    setEditando(null);
    setForm({ ...FORM_VAZIO, unitId: unidades.length === 1 ? unidades[0].id : "" });
    setErros({});
    setErroForm("");
    setModalAberto(true);
  };

  const abrirEdicao = (item) => {
    setEditando(item);
    setForm({
      unitId: item.unitId || "",
      nome: item.nome || "",
      tipo: item.tipo || "PRINCIPAL",
      descricao: item.descricao || "",
      ativo: item.ativo !== false,
    });
    setErros({});
    setErroForm("");
    setModalAberto(true);
  };

  const validar = () => {
    const e = {};
    if (!form.unitId) e.unitId = "Selecione a unidade.";
    if (!form.nome.trim()) e.nome = "Informe o nome da portaria.";
    else if (form.nome.trim().length < 3) e.nome = "Use ao menos 3 caracteres.";
    if (!form.tipo) e.tipo = "Selecione o tipo.";
    setErros(e);
    return Object.keys(e).length === 0;
  };

  const salvar = async () => {
    if (!validar()) return;
    setSalvando(true);
    setErroForm("");
    const corpo = {
      unitId: form.unitId,
      nome: form.nome.trim(),
      tipo: form.tipo,
      descricao: form.descricao.trim() || null,
      ativo: form.ativo,
    };
    const r = editando
      ? await accessApi.put(`/access/portarias/${editando.id}`, corpo)
      : await accessApi.post("/access/portarias", corpo);
    setSalvando(false);
    if (!r.ok) {
      setErroForm(r.erro);
      return;
    }
    setModalAberto(false);
    setFeedback({ tipo: "sucesso", mensagem: editando ? "Portaria atualizada." : "Portaria cadastrada." });
    carregar(pagina);
  };

  const excluir = async () => {
    setExcluindo(true);
    setErroExcluir("");
    const r = await accessApi.delete(`/access/portarias/${excluirId}`);
    setExcluindo(false);
    if (!r.ok) {
      setErroExcluir(r.erro);
      return;
    }
    setExcluirId(null);
    setFeedback({ tipo: "sucesso", mensagem: "Portaria excluída." });
    carregar(pagina);
  };

  const campo = (k) => (e) => {
    const valor = e.target.type === "checkbox" ? e.target.checked : e.target.value;
    setForm((p) => ({ ...p, [k]: valor }));
    if (erros[k]) setErros((p) => ({ ...p, [k]: "" }));
  };

  const totalPaginas = Math.ceil(total / PAGE_SIZE);

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1 className="page-title">Portarias</h1>
          <p className="page-subtitle">Pontos de entrada e saída de cada unidade</p>
        </div>
        <button className="btn btn-brand" onClick={abrirNovo}>
          <Icon name="Plus" size={14} />
          Nova Portaria
        </button>
      </div>

      <Feedback {...(feedback || {})} onFechar={() => setFeedback(null)} />

      <div className="card">
        <div className="card-body">
          <div className="filter-bar">
            <div className="form-field" style={{ flex: 1 }}>
              <input
                className="form-input"
                placeholder="Buscar por nome ou descrição..."
                value={busca}
                onChange={(e) => setBusca(e.target.value)}
                onKeyDown={(e) => e.key === "Enter" && carregar(0)}
              />
            </div>
            <div className="form-field">
              <select className="form-select" value={filtroUnidade} onChange={(e) => setFiltroUnidade(e.target.value)}>
                <option value="">Todas as unidades</option>
                {unidades.map((u) => (
                  <option key={u.id} value={u.id}>
                    {u.name || u.nome}
                  </option>
                ))}
              </select>
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
            <button className="btn btn-brand" onClick={() => carregar(0)}>
              <Icon name="Filter" size={14} /> Filtrar
            </button>
            <button
              className="btn btn-secondary"
              onClick={() => {
                setBusca("");
                setFiltroUnidade("");
                setFiltroTipo("");
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
              <th>Portaria</th>
              <th>Unidade</th>
              <th>Tipo</th>
              <th>Descrição</th>
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
              icone="Home"
              tituloVazio="Nenhuma portaria cadastrada"
              textoVazio="Cadastre a portaria principal antes de vincular os leitores."
              onTentarNovamente={() => carregar(pagina)}
            />
            {!carregando &&
              !erro &&
              itens.map((item) => (
                <tr key={item.id}>
                  <td>
                    <strong>{item.nome}</strong>
                  </td>
                  <td className="td-muted">{item.unidadeNome || nomeUnidade(item.unitId)}</td>
                  <td>
                    <span className={`badge ${item.tipo === "PRINCIPAL" ? "badge-brand" : "badge-secondary"}`}>
                      {TIPOS.find((t) => t.valor === item.tipo)?.label || item.tipo}
                    </span>
                  </td>
                  <td className="td-muted">{item.descricao || "—"}</td>
                  <td>
                    <span className={`badge ${item.ativo !== false ? "badge-success" : "badge-danger"}`}>
                      {item.ativo !== false ? "Ativa" : "Inativa"}
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
                        title="Excluir"
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
          totalPages={totalPaginas}
          total={total}
          pageSize={PAGE_SIZE}
          onPageChange={(p) => carregar(p)}
        />
      </div>

      <Modal
        isOpen={modalAberto}
        onClose={() => setModalAberto(false)}
        title={editando ? "Editar Portaria" : "Nova Portaria"}
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

          <div className="form-grid-2">
            <div className="form-field">
              <label className="form-label required">Nome</label>
              <input
                className={`form-input ${erros.nome ? "error" : ""}`}
                value={form.nome}
                onChange={campo("nome")}
                placeholder="Ex.: Portaria da Rua Mariz e Barros"
              />
              {erros.nome && <span className="form-error">{erros.nome}</span>}
            </div>
            <div className="form-field">
              <label className="form-label required">Tipo</label>
              <select className={`form-select ${erros.tipo ? "error" : ""}`} value={form.tipo} onChange={campo("tipo")}>
                {TIPOS.map((t) => (
                  <option key={t.valor} value={t.valor}>
                    {t.label}
                  </option>
                ))}
              </select>
              {erros.tipo && <span className="form-error">{erros.tipo}</span>}
            </div>
          </div>

          <div className="form-field">
            <label className="form-label">Descrição</label>
            <textarea
              className="form-textarea"
              rows={3}
              value={form.descricao}
              onChange={campo("descricao")}
              placeholder="Referência de localização, horário de funcionamento..."
            />
          </div>

          <label className="form-checkbox">
            <input type="checkbox" checked={form.ativo} onChange={campo("ativo")} />
            <span>Portaria ativa</span>
          </label>
        </div>
      </Modal>

      <ConfirmarModal
        aberto={!!excluirId}
        titulo="Excluir portaria"
        textoConfirmar="Excluir"
        processando={excluindo}
        erro={erroExcluir}
        onConfirmar={excluir}
        onCancelar={() => setExcluirId(null)}
      >
        Os leitores vinculados a esta portaria ficam sem ponto de coleta. Confirma a exclusão?
      </ConfirmarModal>
    </div>
  );
}
