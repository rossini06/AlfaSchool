import { useState, useEffect, useCallback } from "react";
import { api } from "../services/api";
import { Modal } from "../components/Modal";
import { Pagination } from "../components/Pagination";
import { Icon } from "../components/Icon";

const ACTION_COLORS = {
  CREATE: "badge-success",
  UPDATE: "badge-info",
  DELETE: "badge-danger",
  LOGIN:  "badge-brand",
  LOGOUT: "badge-secondary",
  VIEW:   "badge-secondary",
};

const MODULE_LABELS = {
  ALUNOS:      "Alunos",
  TURMAS:      "Turmas",
  CURSOS:      "Cursos",
  MATRICULAS:  "Matrículas",
  USUARIOS:    "Usuários",
  DISPOSITIVOS:"Dispositivos",
  AUTH:        "Autenticação",
  REDES:       "Redes",
  ESCOLAS:     "Escolas",
  SISTEMA:     "Sistema",
};

const PAGE_SIZE = 20;

export function AuditoriaPage() {
  const [logs, setLogs] = useState([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [selected, setSelected] = useState(null);

  const [filters, setFilters] = useState({
    modulo: "",
    acao: "",
    dataInicio: "",
    dataFim: "",
  });

  const load = useCallback(async (p = 0, f = filters) => {
    setLoading(true);
    setError("");
    try {
      const params = new URLSearchParams({ page: p, size: PAGE_SIZE });
      if (f.modulo)     params.set("modulo", f.modulo);
      if (f.acao)       params.set("acao", f.acao);
      if (f.dataInicio) params.set("dataInicio", f.dataInicio);
      if (f.dataFim)    params.set("dataFim", f.dataFim);

      const data = await api.get(`/auditoria?${params}`);
      setLogs(data?.content || data || []);
      setTotal(data?.totalElements ?? (data?.content ?? data ?? []).length);
      setPage(p);
    } catch (err) {
      setError(err.message);
      setLogs([]);
      setTotal(0);
    } finally {
      setLoading(false);
    }
  }, [filters]);

  useEffect(() => { load(0, filters); }, []);

  const applyFilters = () => { load(0, filters); };
  const clearFilters = () => {
    const f = { modulo: "", acao: "", dataInicio: "", dataFim: "" };
    setFilters(f);
    load(0, f);
  };

  const formatDateTime = (dt) => {
    if (!dt) return "—";
    try { return new Date(dt).toLocaleString("pt-BR"); } catch { return dt; }
  };

  const totalPages = Math.ceil(total / PAGE_SIZE);

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1 className="page-title">Auditoria</h1>
          <p className="page-subtitle">Registro de todas as ações realizadas no sistema</p>
        </div>
        <button className="btn btn-secondary" onClick={() => load(page, filters)}>
          <Icon name="RefreshCw" size={14} />
          Atualizar
        </button>
      </div>

      {/* Filters */}
      <div className="card">
        <div className="card-body">
          <div className="filter-bar">
            <div className="form-field">
              <label className="form-label">Módulo</label>
              <select
                className="form-select"
                value={filters.modulo}
                onChange={(e) => setFilters((f) => ({ ...f, modulo: e.target.value }))}
              >
                <option value="">Todos</option>
                {Object.entries(MODULE_LABELS).map(([k, v]) => (
                  <option key={k} value={k}>{v}</option>
                ))}
              </select>
            </div>
            <div className="form-field">
              <label className="form-label">Ação</label>
              <select
                className="form-select"
                value={filters.acao}
                onChange={(e) => setFilters((f) => ({ ...f, acao: e.target.value }))}
              >
                <option value="">Todas</option>
                <option value="CREATE">Criação</option>
                <option value="UPDATE">Atualização</option>
                <option value="DELETE">Exclusão</option>
                <option value="LOGIN">Login</option>
                <option value="LOGOUT">Logout</option>
                <option value="VIEW">Visualização</option>
              </select>
            </div>
            <div className="form-field">
              <label className="form-label">Data início</label>
              <input
                type="date"
                className="form-input"
                value={filters.dataInicio}
                onChange={(e) => setFilters((f) => ({ ...f, dataInicio: e.target.value }))}
              />
            </div>
            <div className="form-field">
              <label className="form-label">Data fim</label>
              <input
                type="date"
                className="form-input"
                value={filters.dataFim}
                onChange={(e) => setFilters((f) => ({ ...f, dataFim: e.target.value }))}
              />
            </div>
            <div className="form-field" style={{ justifyContent: "flex-end" }}>
              <label className="form-label" style={{ opacity: 0 }}>-</label>
              <div style={{ display: "flex", gap: 8 }}>
                <button className="btn btn-brand" onClick={applyFilters}>
                  <Icon name="Filter" size={14} />
                  Filtrar
                </button>
                <button className="btn btn-secondary" onClick={clearFilters}>
                  Limpar
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>

      {error && (
        <div className="login-error">
          <Icon name="AlertCircle" size={14} /> {error}
        </div>
      )}

      {/* Table */}
      <div className="table-wrapper">
        <table className="data-table">
          <thead>
            <tr>
              <th>Data / Hora</th>
              <th>Usuário</th>
              <th>Módulo</th>
              <th>Ação</th>
              <th>Descrição</th>
              <th>IP</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              Array.from({ length: 10 }).map((_, i) => (
                <tr key={i}>
                  {Array.from({ length: 6 }).map((_, j) => (
                    <td key={j}>
                      <div className="skeleton skeleton-text" style={{ width: j === 4 ? "80%" : "60%" }} />
                    </td>
                  ))}
                </tr>
              ))
            ) : logs.length === 0 ? (
              <tr>
                <td colSpan={6}>
                  <div className="empty-state">
                    <div className="empty-state-icon"><Icon name="Shield" size={28} /></div>
                    <h3>Nenhum registro encontrado</h3>
                    <p>Tente ajustar os filtros para encontrar registros de auditoria.</p>
                  </div>
                </td>
              </tr>
            ) : (
              logs.map((log, i) => (
                <tr
                  key={log.id || i}
                  className="clickable"
                  onClick={() => setSelected(log)}
                >
                  <td className="td-muted">{formatDateTime(log.createdAt || log.dataHora)}</td>
                  <td>{log.usuarioNome || log.usuario || "—"}</td>
                  <td>
                    <span className="badge badge-secondary">
                      {MODULE_LABELS[log.modulo] || log.modulo || "—"}
                    </span>
                  </td>
                  <td>
                    <span className={`badge ${ACTION_COLORS[log.acao] || "badge-secondary"}`}>
                      {log.acao || "—"}
                    </span>
                  </td>
                  <td style={{ maxWidth: 320 }}>
                    <span className="truncate" style={{ display: "block", maxWidth: 300 }}>
                      {log.descricao || log.description || "—"}
                    </span>
                  </td>
                  <td className="td-muted">{log.ip || log.ipAddress || "—"}</td>
                </tr>
              ))
            )}
          </tbody>
        </table>

        <Pagination
          page={page}
          totalPages={totalPages}
          total={total}
          pageSize={PAGE_SIZE}
          onPageChange={(p) => load(p, filters)}
        />
      </div>

      {/* Detail Modal */}
      <Modal
        isOpen={!!selected}
        onClose={() => setSelected(null)}
        title="Detalhes do Registro"
        size="lg"
      >
        {selected && (
          <div className="form-grid" style={{ gap: 16 }}>
            <div className="form-grid-2">
              <div>
                <div className="form-label">Data / Hora</div>
                <div style={{ color: "var(--color-text)", marginTop: 4 }}>
                  {formatDateTime(selected.createdAt || selected.dataHora)}
                </div>
              </div>
              <div>
                <div className="form-label">IP</div>
                <div style={{ color: "var(--color-text)", marginTop: 4 }}>
                  {selected.ip || selected.ipAddress || "—"}
                </div>
              </div>
            </div>
            <div className="form-grid-2">
              <div>
                <div className="form-label">Usuário</div>
                <div style={{ color: "var(--color-text)", marginTop: 4 }}>
                  {selected.usuarioNome || selected.usuario || "—"}
                </div>
              </div>
              <div>
                <div className="form-label">E-mail</div>
                <div style={{ color: "var(--color-text)", marginTop: 4 }}>
                  {selected.usuarioEmail || "—"}
                </div>
              </div>
            </div>
            <div className="form-grid-2">
              <div>
                <div className="form-label">Módulo</div>
                <div style={{ marginTop: 6 }}>
                  <span className="badge badge-secondary">
                    {MODULE_LABELS[selected.modulo] || selected.modulo || "—"}
                  </span>
                </div>
              </div>
              <div>
                <div className="form-label">Ação</div>
                <div style={{ marginTop: 6 }}>
                  <span className={`badge ${ACTION_COLORS[selected.acao] || "badge-secondary"}`}>
                    {selected.acao || "—"}
                  </span>
                </div>
              </div>
            </div>
            <div>
              <div className="form-label">Descrição</div>
              <div style={{ color: "var(--color-text)", marginTop: 4 }}>
                {selected.descricao || selected.description || "—"}
              </div>
            </div>
            {(selected.payload || selected.detalhes) && (
              <div>
                <div className="form-label">Payload</div>
                <pre style={{
                  background: "var(--color-bg-3)",
                  border: "1px solid var(--color-border)",
                  borderRadius: "var(--radius-sm)",
                  padding: 12,
                  fontSize: 12,
                  color: "var(--color-text)",
                  overflowX: "auto",
                  marginTop: 6,
                  whiteSpace: "pre-wrap",
                  wordBreak: "break-all",
                }}>
                  {JSON.stringify(selected.payload || selected.detalhes, null, 2)}
                </pre>
              </div>
            )}
          </div>
        )}
      </Modal>
    </div>
  );
}
