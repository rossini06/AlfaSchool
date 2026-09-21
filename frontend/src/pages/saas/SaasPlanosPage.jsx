import { useEffect, useState } from "react";
import { api } from "../../services/api";
import { Modal } from "../../components/Modal";
import { Icon } from "../../components/Icon";
import { Feedback } from "../../components/access/Feedback";
import { ConfirmarModal } from "../../components/access/ConfirmarModal";
import { MODULOS } from "../../utils/saas";
import { ReguaModulos } from "./components/ReguaModulos";

const FORM_VAZIO = {
  nome: "", descricao: "",
  precoMensal: "", precoAnual: "",
  maxEscolas: "", maxUsuarios: "", maxDispositivos: "",
  modulos: [],
  ativo: true,
};

/** "Rede Completa" → "rede-completa". A API exige slug; ninguém digita isso. */
function slugDe(nome) {
  return String(nome || "")
    .normalize("NFD").replace(/[̀-ͯ]/g, "")
    .toLowerCase().replace(/[^a-z0-9]+/g, "-").replace(/^-|-$/g, "");
}

/** `recursos` é uma coluna JSON: guardamos a lista de códigos de módulo. */
function modulosDe(plano) {
  try {
    const v = JSON.parse(plano?.recursos || "[]");
    return Array.isArray(v) ? v : [];
  } catch {
    return [];
  }
}

const moeda = (v) => Number(v || 0).toLocaleString("pt-BR", { minimumFractionDigits: 2, maximumFractionDigits: 2 });

/** Planos comerciais: o que a Alfa oferece às redes. */
export function SaasPlanosPage() {
  const [planos, setPlanos] = useState([]);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState("");
  const [feedback, setFeedback] = useState(null);

  const [modal, setModal] = useState(false);
  const [editando, setEditando] = useState(null);
  const [form, setForm] = useState(FORM_VAZIO);
  const [salvando, setSalvando] = useState(false);
  const [excluindo, setExcluindo] = useState(null);
  const [processandoExcluir, setProcessandoExcluir] = useState(false);

  const carregar = async () => {
    setCarregando(true);
    setErro("");
    try {
      const p = await api.get("/saas/plans");
      setPlanos(p?.content || p || []);
    } catch (e) {
      setErro(e.message);
    } finally {
      setCarregando(false);
    }
  };

  useEffect(() => { carregar(); }, []);

  const abrirNovo = () => {
    setEditando(null);
    setForm({ ...FORM_VAZIO, modulos: MODULOS.map((m) => m.codigo) });
    setModal(true);
  };
  const abrirEdicao = (plano) => {
    setEditando(plano);
    setForm({
      nome: plano.nome || "",
      descricao: plano.descricao || "",
      precoMensal: plano.precoMensal ?? "",
      precoAnual: plano.precoAnual ?? "",
      maxEscolas: plano.maxEscolas || "",
      maxUsuarios: plano.maxUsuarios || "",
      maxDispositivos: plano.maxDispositivos || "",
      modulos: modulosDe(plano),
      ativo: plano.ativo !== false,
    });
    setModal(true);
  };

  const salvar = async () => {
    setSalvando(true);
    try {
      const body = {
        nome: form.nome.trim(),
        slug: editando?.slug || slugDe(form.nome),
        descricao: form.descricao.trim(),
        precoMensal: Number(form.precoMensal || 0),
        precoAnual: form.precoAnual === "" ? null : Number(form.precoAnual),
        maxEscolas: Number(form.maxEscolas || 0),
        maxUsuarios: Number(form.maxUsuarios || 0),
        maxDispositivos: Number(form.maxDispositivos || 0),
        recursos: JSON.stringify(form.modulos),
        ativo: form.ativo,
      };
      if (editando) await api.put(`/saas/plans/${editando.id}`, body);
      else await api.post("/saas/plans", body);
      setModal(false);
      setFeedback({ tipo: "sucesso", mensagem: editando ? "Plano atualizado." : "Plano criado." });
      carregar();
    } catch (e) {
      setFeedback({ tipo: "erro", mensagem: e.message });
    } finally {
      setSalvando(false);
    }
  };

  const confirmarExclusao = async () => {
    setProcessandoExcluir(true);
    try {
      await api.delete(`/saas/plans/${excluindo.id}`);
      setExcluindo(null);
      setFeedback({ tipo: "sucesso", mensagem: "Plano excluído." });
      carregar();
    } catch (e) {
      setFeedback({ tipo: "erro", mensagem: e.message });
    } finally {
      setProcessandoExcluir(false);
    }
  };

  const campo = (k) => (e) =>
    setForm((f) => ({ ...f, [k]: e.target.type === "checkbox" ? e.target.checked : e.target.value }));
  const alternarModulo = (codigo) =>
    setForm((f) => ({
      ...f,
      modulos: f.modulos.includes(codigo) ? f.modulos.filter((c) => c !== codigo) : [...f.modulos, codigo],
    }));

  const limite = (n, singular, plural) => (Number(n) > 0 ? `${n} ${Number(n) === 1 ? singular : plural}` : `${plural} sem limite`);

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1 className="page-title">Planos</h1>
          <p className="page-subtitle">O que a Alfa oferece às redes: módulos incluídos, preço e limites.</p>
        </div>
        <button className="btn btn-brand" onClick={abrirNovo}>
          <Icon name="Plus" size={14} /> Novo plano
        </button>
      </div>

      <Feedback tipo={feedback?.tipo} mensagem={feedback?.mensagem} onFechar={() => setFeedback(null)} />
      {erro && (
        <div className="login-error"><Icon name="AlertCircle" size={14} /> {erro}</div>
      )}

      {carregando ? (
        <div className="saas-planos-grid">
          {Array.from({ length: 3 }).map((_, i) => <div key={i} className="skeleton" style={{ height: 230, borderRadius: "var(--radius-lg)" }} />)}
        </div>
      ) : planos.length === 0 ? (
        <div className="card">
          <div className="empty-state">
            <div className="empty-state-icon"><Icon name="Tag" size={28} /></div>
            <h3>Nenhum plano cadastrado</h3>
            <p>O plano é o que vai na proposta: módulos incluídos, preço mensal e limites.</p>
            <button className="btn btn-brand" onClick={abrirNovo}><Icon name="Plus" size={14} /> Criar o primeiro plano</button>
          </div>
        </div>
      ) : (
        <div className="saas-planos-grid">
          {planos.map((plano) => (
            <article key={plano.id} className={`card saas-plano ${plano.ativo ? "" : "inativo"}`}>
              <header className="saas-plano-topo">
                <h2>{plano.nome}</h2>
                <span className={`saas-status ${plano.ativo ? "ativa" : "suspensa"}`}>{plano.ativo ? "Ativo" : "Inativo"}</span>
              </header>
              <p className="saas-plano-preco">
                <small>R$</small>
                <strong>{moeda(plano.precoMensal)}</strong>
                <small>/mês</small>
              </p>
              {plano.precoAnual > 0 && (
                <p className="saas-plano-anual">ou R$ {moeda(plano.precoAnual)} por ano</p>
              )}
              <ReguaModulos contratados={modulosDe(plano)} />
              <p className="saas-plano-desc">{plano.descricao || "Sem descrição."}</p>
              <ul className="saas-plano-limites">
                <li><Icon name="School" size={13} /> {limite(plano.maxEscolas, "escola", "escolas")}</li>
                <li><Icon name="Users" size={13} /> {limite(plano.maxUsuarios, "usuário", "usuários")}</li>
                <li><Icon name="ScanFace" size={13} /> {limite(plano.maxDispositivos, "leitor", "leitores")}</li>
              </ul>
              <footer className="saas-plano-rodape">
                <div className="td-actions">
                  <button className="btn btn-ghost btn-sm" onClick={() => abrirEdicao(plano)}>
                    <Icon name="Edit" size={13} /> Editar
                  </button>
                  <button className="btn btn-ghost btn-sm text-danger" onClick={() => setExcluindo(plano)} title="Excluir" aria-label="Excluir">
                    <Icon name="Trash" size={13} />
                  </button>
                </div>
              </footer>
            </article>
          ))}
        </div>
      )}

      <ConfirmarModal
        aberto={!!excluindo}
        titulo="Excluir este plano?"
        textoConfirmar="Excluir plano"
        processando={processandoExcluir}
        onCancelar={() => setExcluindo(null)}
        onConfirmar={confirmarExclusao}
      >
        <p>
          <strong>{excluindo?.nome}</strong> deixa de ser oferecido às redes. Quem já
          está contratado neste plano não é afetado — o vínculo existente continua valendo.
        </p>
      </ConfirmarModal>

      <Modal
        isOpen={modal}
        onClose={() => setModal(false)}
        title={editando ? "Editar plano" : "Novo plano"}
        size="lg"
        footer={
          <>
            <button className="btn btn-secondary" onClick={() => setModal(false)}>Cancelar</button>
            <button className="btn btn-brand" onClick={salvar} disabled={salvando || !form.nome.trim()}>
              {salvando ? "Salvando..." : "Salvar"}
            </button>
          </>
        }
      >
        <div className="form-grid">
          <div className="form-grid-2">
            <div className="form-field">
              <label className="form-label required">Nome do plano</label>
              <input className="form-input" value={form.nome} onChange={campo("nome")} placeholder="Ex: Essencial, Rede..." />
              <span className="form-hint">Identificador: {editando?.slug || slugDe(form.nome) || "—"}</span>
            </div>
            <div className="form-field">
              <label className="form-label">Descrição</label>
              <input className="form-input" value={form.descricao} onChange={campo("descricao")} placeholder="Uma linha, como vai na proposta" />
            </div>
          </div>

          <div className="saas-form-secao">
            <h3>Preço</h3>
            <p>O mensal é obrigatório; o anual, se houver desconto.</p>
          </div>
          <div className="form-grid-2">
            <div className="form-field">
              <label className="form-label required">Mensal (R$)</label>
              <input className="form-input" type="number" min="0" step="0.01" value={form.precoMensal} onChange={campo("precoMensal")} placeholder="0,00" />
            </div>
            <div className="form-field">
              <label className="form-label">Anual (R$)</label>
              <input className="form-input" type="number" min="0" step="0.01" value={form.precoAnual} onChange={campo("precoAnual")} placeholder="opcional" />
            </div>
          </div>

          <div className="saas-form-secao">
            <h3>Limites</h3>
            <p>Zero ou vazio significa sem limite.</p>
          </div>
          <div className="form-grid-3">
            <div className="form-field">
              <label className="form-label">Escolas</label>
              <input className="form-input" type="number" min="0" value={form.maxEscolas} onChange={campo("maxEscolas")} placeholder="sem limite" />
            </div>
            <div className="form-field">
              <label className="form-label">Usuários</label>
              <input className="form-input" type="number" min="0" value={form.maxUsuarios} onChange={campo("maxUsuarios")} placeholder="sem limite" />
            </div>
            <div className="form-field">
              <label className="form-label">Leitores</label>
              <input className="form-input" type="number" min="0" value={form.maxDispositivos} onChange={campo("maxDispositivos")} placeholder="sem limite" />
            </div>
          </div>

          <div className="saas-form-secao">
            <h3>Módulos incluídos</h3>
            <p>É o que a régua do plano mostra.</p>
          </div>
          <div className="saas-modulos-grid">
            {MODULOS.map((m) => (
              <label key={m.codigo} className={`saas-modulo ${form.modulos.includes(m.codigo) ? "marcado" : ""}`}>
                <input type="checkbox" checked={form.modulos.includes(m.codigo)} onChange={() => alternarModulo(m.codigo)} />
                <span><strong>{m.rotulo}</strong></span>
              </label>
            ))}
          </div>

          <label className="form-checkbox">
            <input type="checkbox" checked={form.ativo} onChange={campo("ativo")} />
            <span>Plano ativo (aparece nas propostas)</span>
          </label>
        </div>
      </Modal>
    </div>
  );
}
