import { useState, useEffect, useCallback } from "react";
import { accessApi, comoLista, comoTotal, qs } from "../../services/accessCadastrosApi";
import { Modal } from "../../components/Modal";
import { Pagination } from "../../components/Pagination";
import { Icon } from "../../components/Icon";
import { Avatar } from "../../components/Avatar";
import { LinhasEstado } from "../../components/access/EstadoLista";
import { Feedback, Aviso } from "../../components/access/Feedback";
import { ConfirmarModal } from "../../components/access/ConfirmarModal";
import { SwitchCampo } from "../../components/access/SwitchCampo";
import "../../styles/accessCadastros.css";

const PAGE_SIZE = 20;

const PARENTESCOS = [
  "Mãe", "Pai", "Avó", "Avô", "Tia", "Tio", "Irmã", "Irmão",
  "Madrasta", "Padrasto", "Motorista", "Babá", "Outro",
];

const FORM_VAZIO = {
  nome: "",
  cpf: "",
  rg: "",
  parentesco: "",
  telefone: "",
  email: "",
  fotoKey: "",
  observacoes: "",
  podeRetirar: true,
  podeAcessarPortal: false,
  recebeNotificacao: false,
  ativo: true,
};

function somenteDigitos(v) {
  return (v || "").replace(/\D/g, "");
}

function formatarCpf(v) {
  const d = somenteDigitos(v).slice(0, 11);
  return d
    .replace(/(\d{3})(\d)/, "$1.$2")
    .replace(/(\d{3})(\d)/, "$1.$2")
    .replace(/(\d{3})(\d{1,2})$/, "$1-$2");
}

function cpfValido(cpf) {
  const d = somenteDigitos(cpf);
  if (d.length !== 11 || /^(\d)\1{10}$/.test(d)) return false;
  let soma = 0;
  for (let i = 0; i < 9; i++) soma += Number(d[i]) * (10 - i);
  let resto = (soma * 10) % 11;
  if (resto === 10) resto = 0;
  if (resto !== Number(d[9])) return false;
  soma = 0;
  for (let i = 0; i < 10; i++) soma += Number(d[i]) * (11 - i);
  resto = (soma * 10) % 11;
  if (resto === 10) resto = 0;
  return resto === Number(d[10]);
}

export function PessoasAutorizadasPage() {
  const [itens, setItens] = useState([]);
  const [total, setTotal] = useState(0);
  const [pagina, setPagina] = useState(0);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState("");

  const [busca, setBusca] = useState("");
  const [filtroPermissao, setFiltroPermissao] = useState("");

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
        `/access/pessoas-autorizadas?${qs({ page: p, size: PAGE_SIZE, q: busca, permissao: filtroPermissao })}`
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
    [busca, filtroPermissao]
  );

  useEffect(() => {
    carregar(0);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

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
      cpf: formatarCpf(item.cpf || ""),
      rg: item.rg || "",
      parentesco: item.parentesco || "",
      telefone: item.telefone || "",
      email: item.email || "",
      fotoKey: item.fotoKey || "",
      observacoes: item.observacoes || "",
      podeRetirar: !!item.podeRetirar,
      podeAcessarPortal: !!item.podeAcessarPortal,
      recebeNotificacao: !!item.recebeNotificacao,
      ativo: item.ativo !== false,
    });
    setErros({});
    setErroForm("");
    setModalAberto(true);
  };

  const validar = () => {
    const e = {};
    if (!form.nome.trim()) e.nome = "Informe o nome completo.";
    if (!form.cpf.trim()) e.cpf = "O CPF identifica a pessoa na portaria.";
    else if (!cpfValido(form.cpf)) e.cpf = "CPF inválido.";
    if (form.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) e.email = "E-mail inválido.";
    if (form.podeAcessarPortal && !form.email) e.email = "Para acessar o portal é preciso um e-mail — é o login dela.";
    if (form.recebeNotificacao && !form.email && !form.telefone)
      e.telefone = "Para receber notificações, informe telefone ou e-mail.";
    setErros(e);
    return Object.keys(e).length === 0;
  };

  const salvar = async () => {
    if (!validar()) return;
    setSalvando(true);
    setErroForm("");
    const corpo = {
      nome: form.nome.trim(),
      cpf: somenteDigitos(form.cpf),
      rg: form.rg.trim() || null,
      parentesco: form.parentesco || null,
      telefone: form.telefone.trim() || null,
      email: form.email.trim() || null,
      fotoKey: form.fotoKey.trim() || null,
      observacoes: form.observacoes.trim() || null,
      podeRetirar: form.podeRetirar,
      podeAcessarPortal: form.podeAcessarPortal,
      recebeNotificacao: form.recebeNotificacao,
      ativo: form.ativo,
    };
    const r = editando
      ? await accessApi.put(`/access/pessoas-autorizadas/${editando.id}`, corpo)
      : await accessApi.post("/access/pessoas-autorizadas", corpo);
    setSalvando(false);
    if (!r.ok) {
      setErroForm(r.erro);
      return;
    }
    setModalAberto(false);
    setFeedback({ tipo: "sucesso", mensagem: editando ? "Pessoa atualizada." : "Pessoa cadastrada." });
    carregar(pagina);
  };

  const excluir = async () => {
    setExcluindo(true);
    setErroExcluir("");
    const r = await accessApi.delete(`/access/pessoas-autorizadas/${excluirId}`);
    setExcluindo(false);
    if (!r.ok) {
      setErroExcluir(r.erro);
      return;
    }
    setExcluirId(null);
    setFeedback({ tipo: "sucesso", mensagem: "Pessoa removida do cadastro." });
    carregar(pagina);
  };

  const campo = (k) => (e) => {
    setForm((p) => ({ ...p, [k]: e.target.value }));
    if (erros[k]) setErros((p) => ({ ...p, [k]: "" }));
  };

  const pilulaPermissao = (ligada, texto, classe) => (
    <span className={`badge ${ligada ? classe : "badge-secondary"}`} style={ligada ? {} : { opacity: 0.55 }}>
      {texto}
    </span>
  );

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1 className="page-title">Pessoas Autorizadas</h1>
          <p className="page-subtitle">Cadastro de quem se relaciona com a escola na retirada e no portal</p>
        </div>
        <button className="btn btn-brand" onClick={abrirNovo}>
          <Icon name="UserPlus" size={14} /> Nova Pessoa
        </button>
      </div>

      <Aviso tipo="alerta" titulo="As três permissões são independentes">
        Retirar o aluno, acessar o portal e receber notificações são coisas distintas: a avó pode
        buscar a criança sem nunca abrir o portal, e o pai que mora em outra cidade pode acompanhar
        tudo pelo portal sem estar autorizado a retirar. Marcar uma <strong>não</strong> liga as outras.
      </Aviso>

      <Feedback {...(feedback || {})} onFechar={() => setFeedback(null)} />

      <div className="card">
        <div className="card-body">
          <div className="filter-bar">
            <div className="form-field" style={{ flex: 1 }}>
              <input
                className="form-input"
                placeholder="Buscar por nome ou CPF..."
                value={busca}
                onChange={(e) => setBusca(e.target.value)}
                onKeyDown={(e) => e.key === "Enter" && carregar(0)}
              />
            </div>
            <div className="form-field">
              <select
                className="form-select"
                value={filtroPermissao}
                onChange={(e) => setFiltroPermissao(e.target.value)}
              >
                <option value="">Todas as permissões</option>
                <option value="RETIRAR">Autorizadas a retirar</option>
                <option value="PORTAL">Com acesso ao portal</option>
                <option value="NOTIFICACOES">Recebem notificações</option>
              </select>
            </div>
            <button className="btn btn-brand" onClick={() => carregar(0)}>
              <Icon name="Filter" size={14} /> Filtrar
            </button>
            <button
              className="btn btn-secondary"
              onClick={() => {
                setBusca("");
                setFiltroPermissao("");
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
              <th>CPF</th>
              <th>Parentesco</th>
              <th>Contato</th>
              <th>Permissões</th>
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
              icone="Users2"
              tituloVazio="Nenhuma pessoa cadastrada"
              textoVazio="Cadastre as pessoas para depois autorizá-las na retirada de cada aluno."
              onTentarNovamente={() => carregar(pagina)}
            />
            {!carregando &&
              !erro &&
              itens.map((item) => (
                <tr key={item.id}>
                  <td>
                    <div className="flex items-center gap-2">
                      <Avatar foto={item.fotoKey} nome={item.nome} />
                      <strong>{item.nome}</strong>
                    </div>
                  </td>
                  <td className="td-muted ac-mono">{formatarCpf(item.cpf)}</td>
                  <td className="td-muted">{item.parentesco || "—"}</td>
                  <td className="td-muted">
                    {item.telefone || "—"}
                    {item.email && <div className="ac-meta">{item.email}</div>}
                  </td>
                  <td>
                    <div className="ac-linha-acoes">
                      {pilulaPermissao(item.podeRetirar, "Retira", "badge-success")}
                      {pilulaPermissao(item.podeAcessarPortal, "Portal", "badge-info")}
                      {pilulaPermissao(item.recebeNotificacao, "Avisos", "badge-brand")}
                    </div>
                  </td>
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
          totalPages={Math.ceil(total / PAGE_SIZE)}
          total={total}
          pageSize={PAGE_SIZE}
          onPageChange={(p) => carregar(p)}
        />
      </div>

      <Modal
        isOpen={modalAberto}
        onClose={() => setModalAberto(false)}
        title={editando ? "Editar pessoa" : "Nova pessoa autorizada"}
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
              <label className="form-label required">Nome completo</label>
              <input
                className={`form-input ${erros.nome ? "error" : ""}`}
                value={form.nome}
                onChange={campo("nome")}
              />
              {erros.nome && <span className="form-error">{erros.nome}</span>}
            </div>
            <div className="form-field">
              <label className="form-label required">CPF</label>
              <input
                className={`form-input ${erros.cpf ? "error" : ""}`}
                value={form.cpf}
                onChange={(e) => {
                  setForm((p) => ({ ...p, cpf: formatarCpf(e.target.value) }));
                  if (erros.cpf) setErros((p) => ({ ...p, cpf: "" }));
                }}
                placeholder="000.000.000-00"
                inputMode="numeric"
              />
              {erros.cpf ? (
                <span className="form-error">{erros.cpf}</span>
              ) : (
                <span className="form-hint">É o documento conferido na portaria.</span>
              )}
            </div>
          </div>

          <div className="form-grid-3">
            <div className="form-field">
              <label className="form-label">RG</label>
              <input className="form-input" value={form.rg} onChange={campo("rg")} />
            </div>
            <div className="form-field">
              <label className="form-label">Parentesco</label>
              <select className="form-select" value={form.parentesco} onChange={campo("parentesco")}>
                <option value="">Não informado</option>
                {PARENTESCOS.map((p) => (
                  <option key={p} value={p}>
                    {p}
                  </option>
                ))}
              </select>
            </div>
            <div className="form-field">
              <label className="form-label">Telefone</label>
              <input
                className={`form-input ${erros.telefone ? "error" : ""}`}
                value={form.telefone}
                onChange={campo("telefone")}
                placeholder="(27) 99999-0000"
              />
              {erros.telefone && <span className="form-error">{erros.telefone}</span>}
            </div>
          </div>

          <div className="form-grid-2">
            <div className="form-field">
              <label className="form-label">E-mail</label>
              <input
                className={`form-input ${erros.email ? "error" : ""}`}
                type="email"
                value={form.email}
                onChange={campo("email")}
              />
              {erros.email && <span className="form-error">{erros.email}</span>}
            </div>
            <div className="form-field">
              <label className="form-label">Foto (URL)</label>
              <input
                className="form-input"
                value={form.fotoKey}
                onChange={campo("fotoKey")}
                placeholder="https://..."
              />
              <span className="form-hint">A portaria compara a foto com quem está na frente dela.</span>
            </div>
          </div>

          <div className="ac-subcard">
            <div className="ac-subcard-titulo">
              <Icon name="Key" size={14} /> Permissões — cada uma vale por si
            </div>
            <div className="ac-switch-lista">
              <SwitchCampo
                rotulo="Autorizada a retirar"
                descricao="Pode buscar o aluno na portaria. Ainda depende de uma autorização ativa por aluno."
                checked={form.podeRetirar}
                onChange={(v) => setForm((p) => ({ ...p, podeRetirar: v }))}
              />
              <SwitchCampo
                rotulo="Pode acessar o portal"
                descricao="Recebe login no portal da família para acompanhar entradas, saídas e permanência."
                checked={form.podeAcessarPortal}
                onChange={(v) => setForm((p) => ({ ...p, podeAcessarPortal: v }))}
              />
              <SwitchCampo
                rotulo="Recebe notificações"
                descricao="Recebe os avisos automáticos de entrada, saída e ocorrências."
                checked={form.recebeNotificacao}
                onChange={(v) => setForm((p) => ({ ...p, recebeNotificacao: v }))}
              />
            </div>
          </div>

          <div className="form-field">
            <label className="form-label">Observações</label>
            <textarea className="form-textarea" rows={2} value={form.observacoes} onChange={campo("observacoes")} />
          </div>

          <label className="form-checkbox">
            <input
              type="checkbox"
              checked={form.ativo}
              onChange={(e) => setForm((p) => ({ ...p, ativo: e.target.checked }))}
            />
            <span>Cadastro ativo</span>
          </label>
        </div>
      </Modal>

      <ConfirmarModal
        aberto={!!excluirId}
        titulo="Excluir pessoa"
        textoConfirmar="Excluir"
        processando={excluindo}
        erro={erroExcluir}
        onConfirmar={excluir}
        onCancelar={() => setExcluirId(null)}
      >
        As autorizações desta pessoa em todos os alunos deixam de valer. Confirma a exclusão?
      </ConfirmarModal>
    </div>
  );
}
