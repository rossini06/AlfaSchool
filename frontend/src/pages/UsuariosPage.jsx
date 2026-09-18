import { useState, useEffect, useCallback } from "react";
import { api } from "../services/api";
import { Modal } from "../components/Modal";
import { PermissoesUsuarioModal } from "../components/PermissoesUsuarioModal";
import { Pagination } from "../components/Pagination";
import { Icon } from "../components/Icon";

const PAGE_SIZE = 20;

const ROLES = ["USER", "GESTOR", "SUPER_ADMIN"];

const EMPTY_FORM = {
  nome: "", email: "", roles: ["USER"], ativo: true, senha: "",
};

export function UsuariosPage() {
  const [items, setItems] = useState([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [search, setSearch] = useState("");
  const [modalOpen, setModalOpen] = useState(false);
  const [permissoesDe, setPermissoesDe] = useState(null);
  const [editItem, setEditItem] = useState(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [saving, setSaving] = useState(false);
  const [deleteId, setDeleteId] = useState(null);

  const load = useCallback(async (p = 0) => {
    setLoading(true);
    setError("");
    try {
      const params = new URLSearchParams({ page: p, size: PAGE_SIZE });
      if (search) params.set("search", search);
      const data = await api.get(`/usuarios?${params}`);
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
      nome: item.nome || "",
      email: item.email || "",
      roles: item.roles || ["USER"],
      ativo: item.ativo !== false,
      senha: "",
    });
    setModalOpen(true);
  };

  const save = async () => {
    if (!form.nome.trim()) { alert("Nome é obrigatório"); return; }
    if (!form.email.trim()) { alert("E-mail é obrigatório"); return; }
    if (!editItem && !form.senha.trim()) { alert("Senha é obrigatória para novos usuários"); return; }
    setSaving(true);
    try {
      const body = { ...form };
      if (!body.senha) delete body.senha;
      if (editItem) {
        await api.put(`/usuarios/${editItem.id}`, body);
      } else {
        await api.post("/usuarios", body);
      }
      setModalOpen(false);
      load(page);
    } catch (err) {
      alert(err.message);
    } finally {
      setSaving(false);
    }
  };

  const confirmDelete = async () => {
    if (!deleteId) return;
    try {
      await api.delete(`/usuarios/${deleteId}`);
      setDeleteId(null);
      load(page);
    } catch (err) {
      alert(err.message);
    }
  };

  const toggleRole = (role) => {
    setForm((prev) => ({
      ...prev,
      roles: prev.roles.includes(role)
        ? prev.roles.filter((r) => r !== role)
        : [...prev.roles, role],
    }));
  };

  const getRoleLabel = (roles) => {
    if (!roles || !roles.length) return "—";
    if (roles.includes("SUPER_ADMIN")) return "Super Admin";
    if (roles.includes("GESTOR")) return "Gestor";
    return "Usuário";
  };

  const getRoleBadge = (roles) => {
    if (!roles || !roles.length) return <span className="badge badge-secondary">—</span>;
    if (roles.includes("SUPER_ADMIN")) return <span className="badge badge-danger">Super Admin</span>;
    if (roles.includes("GESTOR")) return <span className="badge badge-brand">Gestor</span>;
    return <span className="badge badge-info">Usuário</span>;
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

      <div className="table-wrapper">
        <table className="data-table">
          <thead>
            <tr>
              <th>Nome</th>
              <th>E-mail</th>
              <th>Perfil</th>
              <th>Status</th>
              <th>Criado em</th>
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
                        {item.nome?.[0]?.toUpperCase() || "U"}
                      </div>
                      <strong>{item.nome}</strong>
                    </div>
                  </td>
                  <td className="td-muted">{item.email}</td>
                  <td>{getRoleBadge(item.roles)}</td>
                  <td>
                    <span className={`badge ${item.ativo !== false ? "badge-success" : "badge-danger"}`}>
                      {item.ativo !== false ? "Ativo" : "Inativo"}
                    </span>
                  </td>
                  <td className="td-muted">
                    {item.createdAt ? new Date(item.createdAt).toLocaleDateString("pt-BR") : "—"}
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
                      <button className="btn btn-ghost btn-sm text-danger" onClick={() => setDeleteId(item.id)}>
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
              {ROLES.map((role) => (
                <label key={role} className="form-checkbox">
                  <input type="checkbox"
                    checked={form.roles.includes(role)}
                    onChange={() => toggleRole(role)} />
                  <span>{role === "SUPER_ADMIN" ? "Super Admin" : role === "GESTOR" ? "Gestor" : "Usuário"}</span>
                </label>
              ))}
            </div>
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
