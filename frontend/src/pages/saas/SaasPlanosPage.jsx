import { useEffect, useState } from "react";
import { api } from "../../services/api";
import { Modal } from "../../components/Modal";
import { Icon } from "../../components/Icon";
import { Feedback } from "../../components/access/Feedback";
import { ConfirmarModal } from "../../components/access/ConfirmarModal";

const FORM_VAZIO = { nome: "", descricao: "", preco: "", limiteAlunos: "", ativo: true };

/** Planos comerciais oferecidos às redes. */
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

  const abrirNovo = () => { setEditando(null); setForm(FORM_VAZIO); setModal(true); };
  const abrirEdicao = (plano) => {
    setEditando(plano);
    setForm({
      nome: plano.nome || "",
      descricao: plano.descricao || "",
      preco: plano.preco ?? "",
      limiteAlunos: plano.limiteAlunos ?? "",
      ativo: plano.ativo !== false,
    });
    setModal(true);
  };

  const salvar = async () => {
    setSalvando(true);
    try {
      const body = { ...form, preco: Number(form.preco), limiteAlunos: Number(form.limiteAlunos) };
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

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1 className="page-title">Planos</h1>
          <p className="page-subtitle">O que a Alfa oferece às redes: preço mensal e limite de alunos.</p>
        </div>
        <button className="btn btn-brand" onClick={abrirNovo}>
          <Icon name="Plus" size={14} /> Novo plano
        </button>
      </div>

      <Feedback tipo={feedback?.tipo} mensagem={feedback?.mensagem} onFechar={() => setFeedback(null)} />
      {erro && (
        <div className="login-error"><Icon name="AlertCircle" size={14} /> {erro}</div>
      )}

      <div className="table-wrapper">
        <table className="data-table">
          <thead>
            <tr>
              <th>Nome</th>
              <th>Descrição</th>
              <th>Preço (R$)</th>
              <th>Limite de alunos</th>
              <th>Situação</th>
              <th>Ações</th>
            </tr>
          </thead>
          <tbody>
            {carregando ? (
              Array.from({ length: 4 }).map((_, i) => (
                <tr key={i}>
                  {Array.from({ length: 6 }).map((_, j) => (
                    <td key={j}><div className="skeleton skeleton-text" /></td>
                  ))}
                </tr>
              ))
            ) : planos.length === 0 ? (
              <tr><td colSpan={6}>
                <div className="empty-state">
                  <div className="empty-state-icon"><Icon name="Tag" size={28} /></div>
                  <h3>Nenhum plano cadastrado</h3>
                  <p>Crie o primeiro plano para oferecer às redes.</p>
                </div>
              </td></tr>
            ) : (
              planos.map((plano) => (
                <tr key={plano.id}>
                  <td><strong>{plano.nome}</strong></td>
                  <td className="td-muted">{plano.descricao || "—"}</td>
                  <td>R$ {Number(plano.preco || 0).toFixed(2)}</td>
                  <td>{plano.limiteAlunos || "Ilimitado"}</td>
                  <td>
                    <span className={`badge ${plano.ativo ? "badge-success" : "badge-danger"}`}>
                      {plano.ativo ? "Ativo" : "Inativo"}
                    </span>
                  </td>
                  <td>
                    <div className="td-actions">
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
          <div className="form-field">
            <label className="form-label required">Nome do plano</label>
            <input className="form-input" value={form.nome} onChange={campo("nome")} placeholder="Ex: Básico, Profissional..." />
          </div>
          <div className="form-field">
            <label className="form-label">Descrição</label>
            <textarea className="form-textarea" value={form.descricao} onChange={campo("descricao")} rows={2} />
          </div>
          <div className="form-grid-2">
            <div className="form-field">
              <label className="form-label required">Preço mensal (R$)</label>
              <input className="form-input" type="number" min="0" step="0.01" value={form.preco} onChange={campo("preco")} placeholder="0.00" />
            </div>
            <div className="form-field">
              <label className="form-label">Limite de alunos</label>
              <input className="form-input" type="number" min="0" value={form.limiteAlunos} onChange={campo("limiteAlunos")} placeholder="0 = ilimitado" />
            </div>
          </div>
          <label className="form-checkbox">
            <input type="checkbox" checked={form.ativo} onChange={campo("ativo")} />
            <span>Plano ativo</span>
          </label>
        </div>
      </Modal>
    </div>
  );
}
