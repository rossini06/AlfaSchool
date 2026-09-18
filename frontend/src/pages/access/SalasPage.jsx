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
const FORM_VAZIO = {
  unidadeId: "",
  zonaId: "",
  nome: "",
  codigo: "",
  bloco: "",
  andar: "",
  capacidade: "",
};

export function SalasPage() {
  const [itens, setItens] = useState([]);
  const [unidades, setUnidades] = useState([]);
  const [zonas, setZonas] = useState([]);
  const [total, setTotal] = useState(0);
  const [pagina, setPagina] = useState(0);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState("");

  const [busca, setBusca] = useState("");
  const [filtroUnidade, setFiltroUnidade] = useState("");
  const [filtroZona, setFiltroZona] = useState("");

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
      const r = await accessApi.get(
        `/access/salas?${qs({
          page: p,
          size: PAGE_SIZE,
          search: busca,
          unidadeId: filtroUnidade,
          zonaId: filtroZona,
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
    [busca, filtroUnidade, filtroZona]
  );

  useEffect(() => {
    carregar(0);
    carregarAuxiliar("/unidades?size=200").then(setUnidades);
    carregarAuxiliar("/access/zonas?size=300").then(setZonas);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const nomeUnidade = (id) => {
    const u = unidades.find((x) => x.id === id);
    return u ? u.name || u.nome : "—";
  };
  const nomeZona = (id) => zonas.find((z) => z.id === id)?.nome || "—";

  const zonasDaUnidade = form.unidadeId ? zonas.filter((z) => !z.unidadeId || z.unidadeId === form.unidadeId) : zonas;

  const abrirNovo = () => {
    setEditando(null);
    setForm({ ...FORM_VAZIO, unidadeId: unidades.length === 1 ? unidades[0].id : "" });
    setErros({});
    setErroForm("");
    setModalAberto(true);
  };

  const abrirEdicao = (item) => {
    setEditando(item);
    setForm({
      unidadeId: item.unidadeId || "",
      zonaId: item.zonaId || "",
      nome: item.nome || "",
      codigo: item.codigo || "",
      bloco: item.bloco || "",
      andar: item.andar ?? "",
      capacidade: item.capacidade ?? "",
    });
    setErros({});
    setErroForm("");
    setModalAberto(true);
  };

  const validar = () => {
    const e = {};
    if (!form.unidadeId) e.unidadeId = "Selecione a unidade.";
    if (!form.zonaId) e.zonaId = "Selecione a zona.";
    if (!form.nome.trim()) e.nome = "Informe o nome da sala.";
    if (form.capacidade !== "" && (Number.isNaN(Number(form.capacidade)) || Number(form.capacidade) < 1)) {
      e.capacidade = "Capacidade deve ser um número maior que zero.";
    }
    setErros(e);
    return Object.keys(e).length === 0;
  };

  const salvar = async () => {
    if (!validar()) return;
    setSalvando(true);
    setErroForm("");
    const corpo = {
      unidadeId: form.unidadeId,
      zonaId: form.zonaId,
      nome: form.nome.trim(),
      codigo: form.codigo.trim() || null,
      bloco: form.bloco.trim() || null,
      andar: form.andar === "" ? null : form.andar,
      capacidade: form.capacidade === "" ? null : Number(form.capacidade),
    };
    const r = editando
      ? await accessApi.put(`/access/salas/${editando.id}`, corpo)
      : await accessApi.post("/access/salas", corpo);
    setSalvando(false);
    if (!r.ok) {
      setErroForm(r.erro);
      return;
    }
    setModalAberto(false);
    setFeedback({ tipo: "sucesso", mensagem: editando ? "Sala atualizada." : "Sala cadastrada." });
    carregar(pagina);
  };

  const excluir = async () => {
    setExcluindo(true);
    setErroExcluir("");
    const r = await accessApi.delete(`/access/salas/${excluirId}`);
    setExcluindo(false);
    if (!r.ok) {
      setErroExcluir(r.erro);
      return;
    }
    setExcluirId(null);
    setFeedback({ tipo: "sucesso", mensagem: "Sala excluída." });
    carregar(pagina);
  };

  const campo = (k) => (e) => {
    setForm((p) => ({ ...p, [k]: e.target.value }));
    if (erros[k]) setErros((p) => ({ ...p, [k]: "" }));
  };

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1 className="page-title">Salas</h1>
          <p className="page-subtitle">Espaços físicos onde as turmas são alocadas</p>
        </div>
        <button className="btn btn-brand" onClick={abrirNovo}>
          <Icon name="Plus" size={14} /> Nova Sala
        </button>
      </div>

      <Feedback {...(feedback || {})} onFechar={() => setFeedback(null)} />

      <div className="card">
        <div className="card-body">
          <div className="filter-bar">
            <div className="form-field" style={{ flex: 1 }}>
              <input
                className="form-input"
                placeholder="Buscar por nome ou código..."
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
              <select className="form-select" value={filtroZona} onChange={(e) => setFiltroZona(e.target.value)}>
                <option value="">Todas as zonas</option>
                {zonas.map((z) => (
                  <option key={z.id} value={z.id}>
                    {z.nome}
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
                setFiltroZona("");
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
              <th>Sala</th>
              <th>Código</th>
              <th>Unidade</th>
              <th>Zona</th>
              <th>Bloco / Andar</th>
              <th>Capacidade</th>
              <th>Ações</th>
            </tr>
          </thead>
          <tbody>
            <LinhasEstado
              colSpan={7}
              carregando={carregando}
              erro={erro}
              vazio={itens.length === 0}
              icone="Home"
              tituloVazio="Nenhuma sala cadastrada"
              textoVazio="Cadastre as salas para depois vincular as turmas e seus horários."
              onTentarNovamente={() => carregar(pagina)}
            />
            {!carregando &&
              !erro &&
              itens.map((item) => (
                <tr key={item.id}>
                  <td>
                    <strong>{item.nome}</strong>
                  </td>
                  <td className="td-muted ac-mono">{item.codigo || "—"}</td>
                  <td className="td-muted">{item.unidadeNome || nomeUnidade(item.unidadeId)}</td>
                  <td className="td-muted">{item.zonaNome || nomeZona(item.zonaId)}</td>
                  <td className="td-muted">
                    {[item.bloco, item.andar !== null && item.andar !== undefined && item.andar !== "" ? `${item.andar}º andar` : null]
                      .filter(Boolean)
                      .join(" · ") || "—"}
                  </td>
                  <td className="td-muted">{item.capacidade ?? "—"}</td>
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
          totalPages={Math.ceil(total / PAGE_SIZE)}
          total={total}
          pageSize={PAGE_SIZE}
          onPageChange={(p) => carregar(p)}
        />
      </div>

      <Modal
        isOpen={modalAberto}
        onClose={() => setModalAberto(false)}
        title={editando ? "Editar Sala" : "Nova Sala"}
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
              <label className="form-label required">Unidade</label>
              <select
                className={`form-select ${erros.unidadeId ? "error" : ""}`}
                value={form.unidadeId}
                onChange={(e) => {
                  setForm((p) => ({ ...p, unidadeId: e.target.value, zonaId: "" }));
                  if (erros.unidadeId) setErros((p) => ({ ...p, unidadeId: "" }));
                }}
              >
                <option value="">Selecione a unidade</option>
                {unidades.map((u) => (
                  <option key={u.id} value={u.id}>
                    {u.name || u.nome}
                  </option>
                ))}
              </select>
              {erros.unidadeId && <span className="form-error">{erros.unidadeId}</span>}
            </div>
            <div className="form-field">
              <label className="form-label required">Zona</label>
              <select
                className={`form-select ${erros.zonaId ? "error" : ""}`}
                value={form.zonaId}
                onChange={campo("zonaId")}
                disabled={!form.unidadeId}
              >
                <option value="">{form.unidadeId ? "Selecione a zona" : "Escolha a unidade primeiro"}</option>
                {zonasDaUnidade.map((z) => (
                  <option key={z.id} value={z.id}>
                    {z.nome}
                  </option>
                ))}
              </select>
              {erros.zonaId && <span className="form-error">{erros.zonaId}</span>}
            </div>
          </div>

          <div className="form-grid-2">
            <div className="form-field">
              <label className="form-label required">Nome</label>
              <input
                className={`form-input ${erros.nome ? "error" : ""}`}
                value={form.nome}
                onChange={campo("nome")}
                placeholder="Ex.: Sala 12"
              />
              {erros.nome && <span className="form-error">{erros.nome}</span>}
            </div>
            <div className="form-field">
              <label className="form-label">Código</label>
              <input className="form-input" value={form.codigo} onChange={campo("codigo")} placeholder="Ex.: B-12" />
              <span className="form-hint">Usado nos painéis e nas etiquetas.</span>
            </div>
          </div>

          <div className="form-grid-3">
            <div className="form-field">
              <label className="form-label">Bloco</label>
              <input className="form-input" value={form.bloco} onChange={campo("bloco")} placeholder="B" />
            </div>
            <div className="form-field">
              <label className="form-label">Andar</label>
              <input className="form-input" value={form.andar} onChange={campo("andar")} placeholder="1" />
            </div>
            <div className="form-field">
              <label className="form-label">Capacidade</label>
              <input
                className={`form-input ${erros.capacidade ? "error" : ""}`}
                type="number"
                min="1"
                value={form.capacidade}
                onChange={campo("capacidade")}
                placeholder="30"
              />
              {erros.capacidade && <span className="form-error">{erros.capacidade}</span>}
            </div>
          </div>
        </div>
      </Modal>

      <ConfirmarModal
        aberto={!!excluirId}
        titulo="Excluir sala"
        textoConfirmar="Excluir"
        processando={excluindo}
        erro={erroExcluir}
        onConfirmar={excluir}
        onCancelar={() => setExcluirId(null)}
      >
        Os vínculos de turma com esta sala serão perdidos. Confirma a exclusão?
      </ConfirmarModal>
    </div>
  );
}
