import { useState, useEffect, useCallback } from "react";
import { api } from "../services/api";
import { Modal } from "../components/Modal";
import { Pagination } from "../components/Pagination";
import { Icon } from "../components/Icon";
import { Feedback } from "../components/access/Feedback";
import { ConfirmarModal } from "../components/access/ConfirmarModal";

const PAGE_SIZE = 20;
const TABS = ["Cobranças", "Contratos", "Planos"];
const STATUS_COBRANCA = ["pendente", "pago", "vencido", "cancelado"];
const PERIODICIDADE = ["mensal", "anual", "semestral", "trimestral", "único"];

export function FinanceiroPage() {
  const [tab, setTab] = useState(0);
  const [items, setItems] = useState([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [modalOpen, setModalOpen] = useState(false);
  const [editItem, setEditItem] = useState(null);
  const [saving, setSaving] = useState(false);
  const [planos, setPlanos] = useState([]);
  const [feedback, setFeedback] = useState(null);
  // Encerrar contrato nao tem desfazer: sai do onClick direto e passa a
  // exigir confirmacao com o nome do aluno na frente de quem clica.
  const [encerrando, setEncerrando] = useState(null);
  const [processandoEncerrar, setProcessandoEncerrar] = useState(false);
  const [alunos, setAlunos] = useState([]);

  // Forms
  const [planoForm, setPlanoForm] = useState({ nome: "", valor: "", periodicidade: "mensal", descricao: "", ativo: true });
  const [contratoForm, setContratoForm] = useState({ alunoId: "", planoId: "", dataInicio: "", obs: "" });
  const [pagamentoId, setPagamentoId] = useState(null);
  const [dataPagamento, setDataPagamento] = useState(new Date().toISOString().split("T")[0]);

  const endpoints = ["/financeiro/cobrancas", "/financeiro/contratos", "/financeiro/planos"];

  const load = useCallback(async (p = 0) => {
    setLoading(true); setError("");
    try {
      const data = await api.get(`${endpoints[tab]}?page=${p}&size=${PAGE_SIZE}`);
      setItems(data?.content || data || []);
      setTotal(data?.totalElements ?? (data?.content ?? data ?? []).length);
      setPage(p);
    } catch (err) { setError(err.message); }
    finally { setLoading(false); }
  }, [tab]);

  useEffect(() => { load(0); }, [tab]);

  useEffect(() => {
    api.get("/financeiro/planos/ativos").then(d => setPlanos(Array.isArray(d) ? d : d?.content || [])).catch(() => {});
    api.get("/alunos?size=200").then(d => setAlunos(d?.content || d || [])).catch(() => {});
  }, []);

  const openNewPlano = () => {
    setEditItem(null);
    setPlanoForm({ nome: "", valor: "", periodicidade: "mensal", descricao: "", ativo: true });
    setModalOpen(true);
  };
  const openEditPlano = (item) => {
    setEditItem(item);
    setPlanoForm({ nome: item.nome, valor: item.valor, periodicidade: item.periodicidade, descricao: item.descricao || "", ativo: item.ativo !== false });
    setModalOpen(true);
  };

  const savePlano = async () => {
    if (!planoForm.nome.trim() || !planoForm.valor) { setFeedback({ tipo: "alerta", mensagem: "Informe o nome e o valor do plano." }); return; }
    setSaving(true);
    try {
      const body = { ...planoForm, valor: Number(planoForm.valor) };
      if (editItem) await api.put(`/financeiro/planos/${editItem.id}`, body);
      else await api.post("/financeiro/planos", body);
      setModalOpen(false); load(page);
    } catch (err) { setFeedback({ tipo: "erro", mensagem: err.message }); }
    finally { setSaving(false); }
  };

  const saveContrato = async () => {
    if (!contratoForm.alunoId || !contratoForm.planoId || !contratoForm.dataInicio) {
      setFeedback({ tipo: "alerta", mensagem: "Informe o aluno, o plano e a data de início." }); return;
    }
    setSaving(true);
    try {
      await api.post("/financeiro/contratos", contratoForm);
      setModalOpen(false); load(page);
    } catch (err) { setFeedback({ tipo: "erro", mensagem: err.message }); }
    finally { setSaving(false); }
  };

  const registrarPagamento = async () => {
    try {
      await api.patch(`/financeiro/cobrancas/${pagamentoId}/pagar?dataPagamento=${dataPagamento}`);
      setPagamentoId(null); load(page);
    } catch (err) { setFeedback({ tipo: "erro", mensagem: err.message }); }
  };

  const totalPages = Math.ceil(total / PAGE_SIZE);

  const statusBadge = (s) => {
    const map = { pago: "badge-success", pendente: "badge-warning", vencido: "badge-danger", cancelado: "badge-secondary", ativo: "badge-success", encerrado: "badge-secondary" };
    return <span className={`badge ${map[s] || "badge-secondary"}`}>{s}</span>;
  };

  const formatCurrency = (v) => v != null ? `R$ ${Number(v).toFixed(2).replace(".", ",")}` : "—";

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1 className="page-title">Financeiro</h1>
          <p className="page-subtitle">Gerencie planos, contratos e cobranças dos alunos</p>
        </div>
        {tab === 2 && <button className="btn btn-brand" onClick={openNewPlano}><Icon name="Plus" size={14} /> Novo Plano</button>}
        {tab === 1 && <button className="btn btn-brand" onClick={() => { setEditItem(null); setContratoForm({ alunoId: "", planoId: "", dataInicio: "", obs: "" }); setModalOpen(true); }}>
          <Icon name="Plus" size={14} /> Novo Contrato
        </button>}
      </div>

      <div className="card">
        <div className="card-body" style={{ padding: "0" }}>
          <div style={{ display: "flex", borderBottom: "1px solid var(--color-border)" }}>
            {TABS.map((t, i) => (
              <button key={t} onClick={() => { setTab(i); setItems([]); }}
                style={{ padding: "1rem 1.5rem", background: "none", border: "none", cursor: "pointer",
                  fontWeight: tab === i ? "600" : "400",
                  color: tab === i ? "var(--color-brand)" : "var(--color-text-muted)",
                  borderBottom: tab === i ? "2px solid var(--color-brand)" : "2px solid transparent" }}>
                {t}
              </button>
            ))}
          </div>
        </div>
      </div>

      {error && <div className="login-error"><Icon name="AlertCircle" size={14} /> {error}</div>}
      <Feedback tipo={feedback?.tipo} mensagem={feedback?.mensagem} onFechar={() => setFeedback(null)} />

      <div className="table-wrapper">
        <table className="data-table">
          <thead>
            {tab === 0 && <tr><th>Aluno</th><th>Descrição</th><th>Valor</th><th>Vencimento</th><th>Competência</th><th>Status</th><th>Ações</th></tr>}
            {tab === 1 && <tr><th>Aluno</th><th>Plano</th><th>Início</th><th>Fim</th><th>Status</th><th>Ações</th></tr>}
            {tab === 2 && <tr><th>Nome</th><th>Valor</th><th>Periodicidade</th><th>Status</th><th>Ações</th></tr>}
          </thead>
          <tbody>
            {loading ? Array.from({ length: 8 }).map((_, i) => (
              <tr key={i}>{Array.from({ length: tab === 0 ? 7 : 6 }).map((_, j) => <td key={j}><div className="skeleton skeleton-text" /></td>)}</tr>
            )) : items.length === 0 ? (
              <tr><td colSpan={8}><div className="empty-state">
                <div className="empty-state-icon"><Icon name="DollarSign" size={28} /></div>
                <h3>Nenhum registro encontrado</h3>
              </div></td></tr>
            ) : tab === 0 ? items.map(item => (
              <tr key={item.id}>
                <td className="td-muted">{item.alunoId?.substring(0, 8)}...</td>
                <td>{item.descricao || "—"}</td>
                <td><strong>{formatCurrency(item.valor)}</strong></td>
                <td className="td-muted">{item.vencimento}</td>
                <td className="td-muted">{item.competencia || "—"}</td>
                <td>{statusBadge(item.status)}</td>
                <td><div className="td-actions">
                  {item.status === "pendente" && (
                    <button className="btn btn-ghost btn-sm" title="Registrar Pagamento"
                      onClick={() => { setPagamentoId(item.id); setDataPagamento(new Date().toISOString().split("T")[0]); }}>
                      <Icon name="CheckCircle" size={13} />
                    </button>
                  )}
                </div></td>
              </tr>
            )) : tab === 1 ? items.map(item => (
              <tr key={item.id}>
                <td className="td-muted">{item.alunoId?.substring(0, 8)}...</td>
                <td className="td-muted">{item.planoId?.substring(0, 8)}...</td>
                <td className="td-muted">{item.dataInicio}</td>
                <td className="td-muted">{item.dataFim || "—"}</td>
                <td>{statusBadge(item.status)}</td>
                <td><div className="td-actions">
                  {item.status === "ativo" && (
                    <button className="btn btn-ghost btn-sm text-danger" title="Encerrar contrato"
                      aria-label="Encerrar contrato"
                      onClick={() => setEncerrando(item)}>
                      <Icon name="X" size={13} />
                    </button>
                  )}
                </div></td>
              </tr>
            )) : items.map(item => (
              <tr key={item.id}>
                <td><strong>{item.nome}</strong></td>
                <td><strong>{formatCurrency(item.valor)}</strong></td>
                <td className="td-muted">{item.periodicidade}</td>
                <td><span className={`badge ${item.ativo !== false ? "badge-success" : "badge-danger"}`}>{item.ativo !== false ? "Ativo" : "Inativo"}</span></td>
                <td><div className="td-actions">
                  <button className="btn btn-ghost btn-sm" onClick={() => openEditPlano(item)} title="Editar" aria-label="Editar"><Icon name="Edit" size={13} /></button>
                </div></td>
              </tr>
            ))}
          </tbody>
        </table>
        <Pagination page={page} totalPages={totalPages} total={total} pageSize={PAGE_SIZE} onPageChange={p => load(p)} />
      </div>

      <ConfirmarModal
        aberto={!!encerrando}
        titulo="Encerrar este contrato?"
        textoConfirmar="Encerrar contrato"
        processando={processandoEncerrar}
        onCancelar={() => setEncerrando(null)}
        onConfirmar={async () => {
          setProcessandoEncerrar(true);
          try {
            await api.patch(`/financeiro/contratos/${encerrando.id}/encerrar`);
            setEncerrando(null);
            setFeedback({ tipo: "sucesso", mensagem: "Contrato encerrado." });
            load(page);
          } catch (err) {
            setFeedback({ tipo: "erro", mensagem: err.message });
          } finally {
            setProcessandoEncerrar(false);
          }
        }}
      >
        <p>
          O contrato deixa de gerar cobranças a partir de hoje. Não há como desfazer pela
          tela — seria preciso lançar um contrato novo.
        </p>
        {encerrando && (
          <p className="ac-meta mt-2">
            Início em {encerrando.dataInicio}
            {encerrando.dataFim ? ` · término previsto ${encerrando.dataFim}` : ""}
          </p>
        )}
      </ConfirmarModal>

      {/* Modal Plano */}
      <Modal isOpen={modalOpen && tab === 2} onClose={() => setModalOpen(false)}
        title={editItem ? "Editar Plano" : "Novo Plano Financeiro"}
        footer={<><button className="btn btn-secondary" onClick={() => setModalOpen(false)}>Cancelar</button>
          <button className="btn btn-brand" onClick={savePlano} disabled={saving}>{saving ? "Salvando..." : "Salvar"}</button></>}>
        <div className="form-grid">
          <div className="form-field"><label className="form-label required">Nome</label>
            <input className="form-input" value={planoForm.nome} onChange={e => setPlanoForm(p => ({ ...p, nome: e.target.value }))} placeholder="Ex: Mensalidade Regular" /></div>
          <div className="form-grid-2">
            <div className="form-field"><label className="form-label required">Valor (R$)</label>
              <input className="form-input" type="number" min="0" step="0.01" value={planoForm.valor} onChange={e => setPlanoForm(p => ({ ...p, valor: e.target.value }))} /></div>
            <div className="form-field"><label className="form-label">Periodicidade</label>
              <select className="form-select" value={planoForm.periodicidade} onChange={e => setPlanoForm(p => ({ ...p, periodicidade: e.target.value }))}>
                {PERIODICIDADE.map(x => <option key={x} value={x}>{x}</option>)}
              </select></div>
          </div>
          <div className="form-field"><label className="form-label">Descrição</label>
            <textarea className="form-textarea" value={planoForm.descricao} onChange={e => setPlanoForm(p => ({ ...p, descricao: e.target.value }))} rows={2} /></div>
          <label className="form-checkbox">
            <input type="checkbox" checked={planoForm.ativo} onChange={e => setPlanoForm(p => ({ ...p, ativo: e.target.checked }))} />
            <span>Plano ativo</span>
          </label>
        </div>
      </Modal>

      {/* Modal Contrato */}
      <Modal isOpen={modalOpen && tab === 1} onClose={() => setModalOpen(false)}
        title="Novo Contrato"
        footer={<><button className="btn btn-secondary" onClick={() => setModalOpen(false)}>Cancelar</button>
          <button className="btn btn-brand" onClick={saveContrato} disabled={saving}>{saving ? "Salvando..." : "Salvar"}</button></>}>
        <div className="form-grid">
          <div className="form-field"><label className="form-label required">Aluno</label>
            <select className="form-select" value={contratoForm.alunoId} onChange={e => setContratoForm(p => ({ ...p, alunoId: e.target.value }))}>
              <option value="">Selecione um aluno</option>
              {alunos.map(a => <option key={a.id} value={a.id}>{a.nome}</option>)}
            </select></div>
          <div className="form-field"><label className="form-label required">Plano Financeiro</label>
            <select className="form-select" value={contratoForm.planoId} onChange={e => setContratoForm(p => ({ ...p, planoId: e.target.value }))}>
              <option value="">Selecione um plano</option>
              {planos.map(p => <option key={p.id} value={p.id}>{p.nome} — R$ {Number(p.valor).toFixed(2)}</option>)}
            </select></div>
          <div className="form-field"><label className="form-label required">Data de Início</label>
            <input className="form-input" type="date" value={contratoForm.dataInicio} onChange={e => setContratoForm(p => ({ ...p, dataInicio: e.target.value }))} /></div>
          <div className="form-field"><label className="form-label">Observações</label>
            <textarea className="form-textarea" value={contratoForm.obs} onChange={e => setContratoForm(p => ({ ...p, obs: e.target.value }))} rows={2} /></div>
        </div>
      </Modal>

      {/* Modal Pagamento */}
      <Modal isOpen={!!pagamentoId} onClose={() => setPagamentoId(null)} title="Registrar Pagamento" size="sm"
        footer={<><button className="btn btn-secondary" onClick={() => setPagamentoId(null)}>Cancelar</button>
          <button className="btn btn-brand" onClick={registrarPagamento}>Confirmar Pagamento</button></>}>
        <div className="form-field">
          <label className="form-label">Data do Pagamento</label>
          <input className="form-input" type="date" value={dataPagamento} onChange={e => setDataPagamento(e.target.value)} />
        </div>
      </Modal>
    </div>
  );
}
