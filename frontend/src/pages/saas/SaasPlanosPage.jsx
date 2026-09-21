import { useEffect, useState } from "react";
import { api } from "../../services/api";
import { Modal } from "../../components/Modal";
import { Icon } from "../../components/Icon";
import { Feedback } from "../../components/access/Feedback";
import { ConfirmarModal } from "../../components/access/ConfirmarModal";
import { MODULOS, rotuloModulo } from "../../utils/saas";
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

  const limite = (n, singular, plural) => (Number(n) > 0 ? `${n} ${Number(n) === 1 ? singular : plural}` : `sem limite de ${plural}`);

  return (
    <div>
      <Feedback tipo={feedback?.tipo} mensagem={feedback?.mensagem} onFechar={() => setFeedback(null)} />
      {erro && (
        <div className="login-error"><Icon name="AlertCircle" size={14} /> {erro}</div>
      )}

      <div className="filter-bar saas-filter-bar">
        <p className="saas-filter-nota">O que a Alfa oferece às redes: módulos incluídos, preço e limites.</p>
        <button className="btn btn-brand" onClick={abrirNovo}>
          <Icon name="Plus" size={16} /> Novo plano
        </button>
        <button className="btn btn-secondary" onClick={carregar} title="Atualizar" aria-label="Atualizar">
          <Icon name="RefreshCw" size={15} />
        </button>
      </div>

      <div className="table-wrapper">
        <table className="data-table">
          <thead>
            <tr>
              <th>Plano</th>
              <th>Mensal</th>
              <th>Anual</th>
              <th>Módulos</th>
              <th>Limites</th>
              <th>Situação</th>
              <th style={{ textAlign: "right" }}>Ações</th>
            </tr>
          </thead>
          <tbody>
            {carregando ? (
              Array.from({ length: 3 }).map((_, i) => (
                <tr key={i}>{Array.from({ length: 7 }).map((_, j) => <td key={j}><div className="skeleton" style={{ height: 16, width: "80%" }} /></td>)}</tr>
              ))
            ) : planos.length === 0 ? (
              <tr><td colSpan={7}>
                <div className="empty-state">
                  <div className="empty-state-icon"><Icon name="Tag" size={28} /></div>
                  <h3>Nenhum plano cadastrado</h3>
                  <p>O plano é o que vai na proposta: módulos incluídos, preço mensal e limites.</p>
                </div>
              </td></tr>
            ) : (
              planos.map((plano) => (
                <tr key={plano.id} className={plano.ativo ? "" : "saas-linha-suspensa"}>
                  <td>
                    <strong>{plano.nome}</strong>
                    {plano.descricao && <div className="td-muted" style={{ fontSize: 12 }}>{plano.descricao}</div>}
                  </td>
                  <td className="saas-mono">R$ {moeda(plano.precoMensal)}</td>
                  <td className="saas-mono td-muted">{plano.precoAnual > 0 ? `R$ ${moeda(plano.precoAnual)}` : "—"}</td>
                  <td>
                    <div className="saas-celula-modulos" title={modulosDe(plano).map(rotuloModulo).join(", ") || "Nenhum"}>
                      <ReguaModulos contratados={modulosDe(plano)} compacta />
                      <span>{modulosDe(plano).length} de {MODULOS.length}</span>
                    </div>
                  </td>
                  <td className="td-muted" style={{ fontSize: 12.5 }}>
                    {!plano.maxEscolas && !plano.maxUsuarios && !plano.maxDispositivos
                      ? "Sem limites"
                      : `${limite(plano.maxEscolas, "escola", "escolas")} · ${limite(plano.maxUsuarios, "usuário", "usuários")} · ${limite(plano.maxDispositivos, "leitor", "leitores")}`}
                  </td>
                  <td>
                    <span className={`badge ${plano.ativo ? "badge-success" : "badge-danger"}`}>{plano.ativo ? "Ativo" : "Inativo"}</span>
                  </td>
                  <td>
                    <div className="td-actions" style={{ justifyContent: "flex-end" }}>
                      <button className="btn btn-ghost btn-sm" onClick={() => abrirEdicao(plano)} title="Editar" aria-label="Editar">
                        <Icon name="Edit" size={13} />
                      </button>
                      <button className="btn btn-ghost btn-sm text-danger" onClick={() => setExcluindo(plano)} title="Excluir" aria-label="Excluir">
                        <Icon name="Trash" size={13} />
                      </button>
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

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
