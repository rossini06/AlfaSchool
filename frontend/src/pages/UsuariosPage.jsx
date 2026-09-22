import { useState, useEffect, useCallback } from "react";
import { api } from "../services/api";
import { Modal } from "../components/Modal";
import { PermissoesUsuarioModal } from "../components/PermissoesUsuarioModal";
import { Pagination } from "../components/Pagination";
import { Icon } from "../components/Icon";
import { Feedback } from "../components/access/Feedback";

const PAGE_SIZE = 20;



const EMPTY_FORM = {
  nome: "", email: "", perfis: [], ativo: true, senha: "",
};

export function UsuariosPage() {
  const [items, setItems] = useState([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [feedback, setFeedback] = useState(null);
  const [search, setSearch] = useState("");
  const [modalOpen, setModalOpen] = useState(false);
  const [permissoesDe, setPermissoesDe] = useState(null);
  const [editItem, setEditItem] = useState(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [saving, setSaving] = useState(false);
  const [deleteId, setDeleteId] = useState(null);
  // Os perfis sao por escola e a escola pode criar os seus. Buscar do
  // backend e' a unica forma de a tela oferecer os que existem de verdade.
  const [perfis, setPerfis] = useState([]);

  useEffect(() => {
    api
      .get("/perfis")
      .then((r) => setPerfis(Array.isArray(r) ? r : r?.content || []))
      .catch(() => setPerfis([]));
  }, []);

  const load = useCallback(async (p = 0) => {
    setLoading(true);
    setError("");
    try {
      const params = new URLSearchParams({ page: p, size: PAGE_SIZE });
      if (search) params.set("search", search);
      const data = await api.get(`/users?${params}`);
      setItems(data?.content || data || []);
      setTotal(data?.totalElements ?? (data?.content ?? data ?? []).length);
      setPage(p);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, [search]);

  useEffect(() => { load(0); }, []);

  const openNew = () => { setEditItem(null); setForm(EMPTY_FORM); setModalOpen(true); };

  const openEdit = (item) => {
    setEditItem(item);
    setForm({
      nome: item.name || "",
      email: item.email || "",
      perfis: (item.perfis || []).map((p) => p.id),
      ativo: item.active !== false,
      senha: "",
    });
    setModalOpen(true);
  };

  const save = async () => {
    if (!form.nome.trim()) { setFeedback({ tipo: "alerta", mensagem: "Informe o nome." }); return; }
    if (!form.email.trim()) { setFeedback({ tipo: "alerta", mensagem: "Informe o e-mail." }); return; }
    if (!editItem && !form.senha.trim()) { setFeedback({ tipo: "alerta", mensagem: "Defina uma senha para o novo usuário." }); return; }
    setSaving(true);
    try {
      const body = { ...form };
      if (!body.senha) delete body.senha;
      if (editItem) {
        await api.put(`/users/${editItem.id}`, body);
      } else {
        await api.post("/users", body);
      }
      setModalOpen(false);
      load(page);
    } catch (err) {
      setFeedback({ tipo: "erro", mensagem: err.message });
    } finally {
      setSaving(false);
    }
  };

  const confirmDelete = async () => {
    if (!deleteId) return;
    try {
      await api.delete(`/users/${deleteId}`);
      setDeleteId(null);
      load(page);
    } catch (err) {
      setFeedback({ tipo: "erro", mensagem: err.message });
    }
  };

  const togglePerfil = (id) => {
    setForm((prev) => ({
      ...prev,
      perfis: prev.perfis.includes(id)
        ? prev.perfis.filter((p) => p !== id)
        : [...prev.perfis, id],
    }));
  };

  /**
   * Mostra os perfis que o usuario realmente tem, com o rotulo que a escola
   * ve. Antes havia um de-para fixo para "USER"/"GESTOR"/"SUPER_ADMIN", que
   * nao existem: quem fosse COORDENACAO aparecia como "Usuário".
   */
  const badgesDePerfil = (item) => {
    const lista = item.perfis || [];
    if (lista.length === 0) {
      return (
        <span className="badge badge-secondary" title="Este usuário entra no sistema e não vê nenhuma tela">
          Sem perfil
        </span>
      );
    }
    return (
      <div style={{ display: "flex", gap: 4, flexWrap: "wrap" }}>
        {lista.map((p) => (
          <span key={p.id} className="badge badge-brand">{p.rotulo || p.nome}</span>
        ))}
      </div>
    );
  };

  const totalPages = Math.ceil(total / PAGE_SIZE);
  const f = (k) => (e) => setForm((prev) => ({ ...prev, [k]: e.target.type === "checkbox" ? e.target.checked : e.target.value }));

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1 className="page-title">Usuários</h1>
          <p className="page-subtitle">Gerenciamento de usuários do sistema</p>
        </div>
        <button className="btn btn-brand" onClick={openNew}>
          <Icon name="Plus" size={14} />
          Novo Usuário
        </button>
      </div>

      <div className="card">
        <div className="card-body">
          <div className="filter-bar">
            <div className="form-field" style={{ flex: 1 }}>
              <input className="form-input" placeholder="Buscar por nome ou e-mail..."
                value={search} onChange={(e) => setSearch(e.target.value)}
                onKeyDown={(e) => e.key === "Enter" && load(0)} />
            </div>
            <button className="btn btn-brand" onClick={() => load(0)}>
              <Icon name="Search" size={14} />
              Buscar
            </button>
            <button className="btn btn-secondary" onClick={() => { setSearch(""); load(0); }}>
              Limpar
            </button>
          </div>
        </div>
      </div>

      {error && <div className="login-error"><Icon name="AlertCircle" size={14} /> {error}</div>}

      <Feedback tipo={feedback?.tipo} mensagem={feedback?.mensagem} onFechar={() => setFeedback(null)} />

      <div className="table-wrapper">
        <table className="data-table">
          <thead>
            <tr>
              <th>Nome</th>
              <th>E-mail</th>
              <th>Perfil</th>
              <th>Status</th>
              <th>Último acesso</th>
              <th>Ações</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              Array.from({ length: 8 }).map((_, i) => (
                <tr key={i}>{Array.from({ length: 6 }).map((_, j) => <td key={j}><div className="skeleton skeleton-text" /></td>)}</tr>
              ))
            ) : items.length === 0 ? (
              <tr><td colSpan={6}>
                <div className="empty-state">
                  <div className="empty-state-icon"><Icon name="UserCog" size={28} /></div>
                  <h3>Nenhum usuário encontrado</h3>
                  <p>Adicione usuários para controlar o acesso ao sistema.</p>
                </div>
              </td></tr>
            ) : (
              items.map((item) => (
                <tr key={item.id}>
                  <td>
                    <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
                      <div style={{
                        width: 30, height: 30, borderRadius: "50%",
                        background: "var(--color-primary)",
                        color: "#fff", display: "flex", alignItems: "center",
                        justifyContent: "center", fontWeight: 700, fontSize: 12, flexShrink: 0,
                      }}>
                        {item.name?.[0]?.toUpperCase() || "U"}
                      </div>
                      <strong>{item.name}</strong>
                    </div>
                  </td>
                  <td className="td-muted">{item.email}</td>
                  <td>{badgesDePerfil(item)}</td>
                  <td>
                    <span className={`badge ${item.active !== false ? "badge-success" : "badge-secondary"}`}>
                      {item.active !== false ? "Ativo" : "Inativo"}
                    </span>
                    {item.locked && (
                      <span className="badge badge-warning" title="Bloqueado por tentativas de login inválidas">
                        Bloqueado
                      </span>
                    )}
                  </td>
                  <td className="td-muted">
                    {item.lastLogin
                      ? new Date(item.lastLogin).toLocaleString("pt-BR", {
                          timeZone: "America/Sao_Paulo",
                          day: "2-digit", month: "2-digit", year: "numeric",
                          hour: "2-digit", minute: "2-digit",
                        })
                      : "nunca entrou"}
                  </td>
                  <td>
                    <div className="td-actions">
                      <button className="btn btn-ghost btn-sm" onClick={() => openEdit(item)} title="Editar">
                        <Icon name="Edit" size={13} />
                      </button>
                      <button
                        className="btn btn-ghost btn-sm"
                        onClick={() => setPermissoesDe(item)}
                        title="Permissões"
                      >
                        <Icon name="ShieldCheck" size={13} />
                      </button>
                      <button className="btn btn-ghost btn-sm text-danger" onClick={() => setDeleteId(item.id)} title="Excluir" aria-label="Excluir">
                        <Icon name="Trash" size={13} />
                      </button>
                    </div>
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
        <Pagination page={page} totalPages={totalPages} total={total} pageSize={PAGE_SIZE} onPageChange={(p) => load(p)} />
      </div>

      {permissoesDe && (
        <PermissoesUsuarioModal
          usuario={permissoesDe}
          onClose={() => setPermissoesDe(null)}
        />
      )}

      {/* Modal */}
      <Modal isOpen={modalOpen} onClose={() => setModalOpen(false)}
        title={editItem ? "Editar Usuário" : "Novo Usuário"}
        footer={
          <>
            <button className="btn btn-secondary" onClick={() => setModalOpen(false)}>Cancelar</button>
            <button className="btn btn-brand" onClick={save} disabled={saving}>
              {saving ? "Salvando..." : "Salvar"}
            </button>
          </>
        }
      >
        <div className="form-grid">
          <div className="form-field">
            <label className="form-label required">Nome</label>
            <input className="form-input" value={form.nome} onChange={f("nome")} placeholder="Nome completo" />
          </div>
          <div className="form-field">
            <label className="form-label required">E-mail</label>
            <input className="form-input" type="email" value={form.email} onChange={f("email")} placeholder="usuario@escola.com" />
          </div>
          <div className="form-field">
            <label className="form-label">{editItem ? "Nova Senha (deixe vazio para manter)" : "Senha"}</label>
            <input className="form-input" type="password" value={form.senha} onChange={f("senha")}
              placeholder={editItem ? "Nova senha (opcional)" : "Senha do usuário"} />
          </div>
          <div className="form-field">
            <label className="form-label">Perfis de Acesso</label>
            <div style={{ display: "flex", gap: 10, flexWrap: "wrap", marginTop: 6 }}>
              {perfis.length === 0 && (
                <span className="form-hint">Nenhum perfil cadastrado nesta escola.</span>
              )}
              {perfis.map((perfil) => (
                <label key={perfil.id} className="form-checkbox" title={perfil.descricao || ""}>
                  <input
                    type="checkbox"
                    checked={form.perfis.includes(perfil.id)}
                    onChange={() => togglePerfil(perfil.id)}
                  />
                  <span>{perfil.rotulo || perfil.nome}</span>
                </label>
              ))}
            </div>
            <span className="form-hint">
              Sem nenhum perfil marcado, a pessoa entra no sistema e não vê nenhuma tela.
            </span>
          </div>
          <label className="form-checkbox">
            <input type="checkbox" checked={form.ativo} onChange={f("ativo")} />
            <span>Usuário ativo</span>
          </label>
        </div>
      </Modal>

      {/* Delete confirm */}
      <Modal isOpen={!!deleteId} onClose={() => setDeleteId(null)} title="Confirmar Exclusão" size="sm"
        footer={
          <>
            <button className="btn btn-secondary" onClick={() => setDeleteId(null)}>Cancelar</button>
            <button className="btn btn-danger" onClick={confirmDelete}>Excluir</button>
          </>
        }
      >
        <p style={{ color: "var(--color-text)" }}>
          Tem certeza que deseja excluir este usuário? O acesso será revogado imediatamente.
        </p>
      </Modal>
    </div>
  );
}
