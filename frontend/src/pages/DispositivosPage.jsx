import { useState, useEffect, useCallback } from "react";
import { api } from "../services/api";
import { Modal } from "../components/Modal";
import { Pagination } from "../components/Pagination";
import { Icon } from "../components/Icon";

const PAGE_SIZE = 20;

const TIPOS = ["Computador", "Tablet", "Impressora", "Câmera", "Roteador", "Servidor", "Projetor", "Outro"];

const EMPTY_FORM = {
  nome: "", tipo: "Computador", fabricante: "", modelo: "",
  ip: "", porta: "", serial: "", ativo: true,
};

export function DispositivosPage() {
  const [items, setItems] = useState([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [search, setSearch] = useState("");
  const [filterTipo, setFilterTipo] = useState("");
  const [modalOpen, setModalOpen] = useState(false);
  const [editItem, setEditItem] = useState(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [saving, setSaving] = useState(false);
  const [deleteId, setDeleteId] = useState(null);
  const [pinging, setPinging] = useState({});
  const [pingResults, setPingResults] = useState({});

  const load = useCallback(async (p = 0) => {
    setLoading(true);
    setError("");
    try {
      const params = new URLSearchParams({ page: p, size: PAGE_SIZE });
      if (search) params.set("search", search);
      if (filterTipo) params.set("tipo", filterTipo);
      const data = await api.get(`/dispositivos?${params}`);
      setItems(data?.content || data || []);
      setTotal(data?.totalElements ?? (data?.content ?? data ?? []).length);
      setPage(p);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, [search, filterTipo]);

  useEffect(() => { load(0); }, []);

  const openNew = () => { setEditItem(null); setForm(EMPTY_FORM); setModalOpen(true); };

  const openEdit = (item) => {
    setEditItem(item);
    setForm({
      nome: item.nome || "", tipo: item.tipo || "Computador",
      fabricante: item.fabricante || "", modelo: item.modelo || "",
      ip: item.ip || "", porta: item.porta || "",
      serial: item.serial || "", ativo: item.ativo !== false,
    });
    setModalOpen(true);
  };

  const save = async () => {
    if (!form.nome.trim()) { alert("Nome é obrigatório"); return; }
    setSaving(true);
    try {
      const body = { ...form, porta: form.porta ? Number(form.porta) : null };
      if (editItem) {
        await api.put(`/dispositivos/${editItem.id}`, body);
      } else {
        await api.post("/dispositivos", body);
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
      await api.delete(`/dispositivos/${deleteId}`);
      setDeleteId(null);
      load(page);
    } catch (err) {
      alert(err.message);
    }
  };

  const toggleAtivo = async (item) => {
    try {
      await api.patch(`/dispositivos/${item.id}`, { ativo: !item.ativo });
      load(page);
    } catch (err) {
      alert(err.message);
    }
  };

  const pingDevice = async (item) => {
    setPinging((prev) => ({ ...prev, [item.id]: true }));
    try {
      const result = await api.post(`/dispositivos/${item.id}/ping`, {});
      setPingResults((prev) => ({ ...prev, [item.id]: result?.online ? "online" : "offline" }));
    } catch {
      setPingResults((prev) => ({ ...prev, [item.id]: "error" }));
    } finally {
      setPinging((prev) => ({ ...prev, [item.id]: false }));
    }
  };

  const totalPages = Math.ceil(total / PAGE_SIZE);
  const f = (k) => (e) => setForm((prev) => ({ ...prev, [k]: e.target.type === "checkbox" ? e.target.checked : e.target.value }));

  const statusDisplay = (item) => {
    const pr = pingResults[item.id];
    if (pr === "online") return <span className="status-dot online">Online</span>;
    if (pr === "offline") return <span className="status-dot offline">Offline</span>;
    if (item.status === "online" || item.online) return <span className="status-dot online">Online</span>;
    if (item.status === "offline") return <span className="status-dot offline">Offline</span>;
    return <span className="status-dot offline">Desconhecido</span>;
  };

  const formatUltimoPing = (dt) => {
    if (!dt) return "—";
    const d = new Date(dt);
    const diff = Date.now() - d.getTime();
    if (diff < 60000) return "Agora";
    if (diff < 3600000) return `${Math.floor(diff / 60000)}min atrás`;
    if (diff < 86400000) return `${Math.floor(diff / 3600000)}h atrás`;
    return d.toLocaleDateString("pt-BR");
  };

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1 className="page-title">Dispositivos</h1>
          <p className="page-subtitle">Equipamentos e dispositivos cadastrados</p>
        </div>
        <button className="btn btn-brand" onClick={openNew}>
          <Icon name="Plus" size={14} />
          Novo Dispositivo
        </button>
      </div>

      <div className="card">
        <div className="card-body">
          <div className="filter-bar">
            <div className="form-field" style={{ flex: 1 }}>
              <input className="form-input" placeholder="Buscar por nome, IP ou modelo..."
                value={search} onChange={(e) => setSearch(e.target.value)}
                onKeyDown={(e) => e.key === "Enter" && load(0)} />
            </div>
            <div className="form-field">
              <select className="form-select" value={filterTipo} onChange={(e) => setFilterTipo(e.target.value)}>
                <option value="">Todos os tipos</option>
                {TIPOS.map((t) => <option key={t} value={t}>{t}</option>)}
              </select>
            </div>
            <button className="btn btn-brand" onClick={() => load(0)}>
              <Icon name="Search" size={14} />
              Buscar
            </button>
            <button className="btn btn-secondary" onClick={() => { setSearch(""); setFilterTipo(""); load(0); }}>
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
              <th>Tipo</th>
              <th>Fabricante / Modelo</th>
              <th>IP</th>
              <th>Status</th>
              <th>Último Ping</th>
              <th>Ativo</th>
              <th>Ações</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              Array.from({ length: 8 }).map((_, i) => (
                <tr key={i}>{Array.from({ length: 8 }).map((_, j) => <td key={j}><div className="skeleton skeleton-text" /></td>)}</tr>
              ))
            ) : items.length === 0 ? (
              <tr><td colSpan={8}>
                <div className="empty-state">
                  <div className="empty-state-icon"><Icon name="Cpu" size={28} /></div>
                  <h3>Nenhum dispositivo encontrado</h3>
                  <p>Cadastre o primeiro dispositivo da sua escola.</p>
                </div>
              </td></tr>
            ) : (
              items.map((item) => (
                <tr key={item.id}>
                  <td><strong>{item.nome}</strong></td>
                  <td>
                    <span className="badge badge-brand">{item.tipo || "—"}</span>
                  </td>
                  <td className="td-muted">
                    {[item.fabricante, item.modelo].filter(Boolean).join(" / ") || "—"}
                  </td>
                  <td style={{ fontFamily: "monospace", fontSize: 13 }}>
                    {item.ip || "—"}
                    {item.porta ? `:${item.porta}` : ""}
                  </td>
                  <td>{statusDisplay(item)}</td>
                  <td className="td-muted">{formatUltimoPing(item.ultimoPing || item.lastSeen)}</td>
                  <td>
                    <span
                      className={`badge ${item.ativo !== false ? "badge-success" : "badge-danger"}`}
                      style={{ cursor: "pointer" }}
                      onClick={() => toggleAtivo(item)}
                      title="Clique para alternar"
                    >
                      {item.ativo !== false ? "Ativo" : "Inativo"}
                    </span>
                  </td>
                  <td>
                    <div className="td-actions">
                      <button
                        className="btn btn-ghost btn-sm"
                        onClick={() => pingDevice(item)}
                        disabled={pinging[item.id]}
                        title="Testar conectividade"
                      >
                        {pinging[item.id] ? (
                          <span style={{ fontSize: 13 }}>⟳</span>
                        ) : (
                          <Icon name="Wifi" size={13} />
                        )}
                      </button>
                      <button className="btn btn-ghost btn-sm" onClick={() => openEdit(item)}>
                        <Icon name="Edit" size={13} />
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

      {/* Modal */}
      <Modal isOpen={modalOpen} onClose={() => setModalOpen(false)}
        title={editItem ? "Editar Dispositivo" : "Novo Dispositivo"}
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
            <label className="form-label required">Nome do Dispositivo</label>
            <input className="form-input" value={form.nome} onChange={f("nome")} placeholder="Ex: Computador Sala 1" />
          </div>
          <div className="form-grid-2">
            <div className="form-field">
              <label className="form-label">Tipo</label>
              <select className="form-select" value={form.tipo} onChange={f("tipo")}>
                {TIPOS.map((t) => <option key={t} value={t}>{t}</option>)}
              </select>
            </div>
            <div className="form-field">
              <label className="form-label">Número de Série</label>
              <input className="form-input" value={form.serial} onChange={f("serial")} placeholder="SN: ..." />
            </div>
          </div>
          <div className="form-grid-2">
            <div className="form-field">
              <label className="form-label">Fabricante</label>
              <input className="form-input" value={form.fabricante} onChange={f("fabricante")} placeholder="Ex: Dell, HP, Lenovo..." />
            </div>
            <div className="form-field">
              <label className="form-label">Modelo</label>
              <input className="form-input" value={form.modelo} onChange={f("modelo")} placeholder="Ex: Inspiron 3000" />
            </div>
          </div>
          <div className="form-grid-2">
            <div className="form-field">
              <label className="form-label">Endereço IP</label>
              <input className="form-input" value={form.ip} onChange={f("ip")} placeholder="192.168.0.100" />
            </div>
            <div className="form-field">
              <label className="form-label">Porta</label>
              <input className="form-input" type="number" min="1" max="65535" value={form.porta} onChange={f("porta")} placeholder="80" />
            </div>
          </div>
          <label className="form-checkbox">
            <input type="checkbox" checked={form.ativo} onChange={f("ativo")} />
            <span>Dispositivo ativo</span>
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
        <p style={{ color: "var(--color-text)" }}>Tem certeza que deseja excluir este dispositivo?</p>
      </Modal>
    </div>
  );
}
