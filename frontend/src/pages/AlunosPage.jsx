import { useState, useEffect, useCallback } from "react";
import { api } from "../services/api";
import { Modal } from "../components/Modal";
import { Pagination } from "../components/Pagination";
import { Icon } from "../components/Icon";

const PAGE_SIZE = 20;

const ESTADOS_BR = ["AC","AL","AP","AM","BA","CE","DF","ES","GO","MA","MT","MS","MG","PA","PB","PR","PE","PI","RJ","RN","RS","RO","RR","SC","SP","SE","TO"];

const EMPTY_FORM = {
  // Dados Pessoais
  nome: "", cpf: "", rg: "", email: "", telefone: "",
  dataNascimento: "", sexo: "", ativo: true,
  // Endereço
  endereco: "", cidade: "", estado: "", cep: "",
  // Responsável
  nomeResponsavel: "", telefoneResponsavel: "", emailResponsavel: "",
  // Foto
  foto: "",
};

export function AlunosPage() {
  const [items, setItems] = useState([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [search, setSearch] = useState("");
  const [modalOpen, setModalOpen] = useState(false);
  const [editItem, setEditItem] = useState(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [saving, setSaving] = useState(false);
  const [deleteId, setDeleteId] = useState(null);
  const [activeTab, setActiveTab] = useState("pessoal");

  const load = useCallback(async (p = 0) => {
    setLoading(true);
    setError("");
    try {
      const params = new URLSearchParams({ page: p, size: PAGE_SIZE });
      if (search) params.set("search", search);
      const data = await api.get(`/alunos?${params}`);
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

  const openNew = () => { setEditItem(null); setForm(EMPTY_FORM); setActiveTab("pessoal"); setModalOpen(true); };

  const openEdit = (item) => {
    setEditItem(item);
    setForm({
      nome: item.nome || "", cpf: item.cpf || "", rg: item.rg || "",
      email: item.email || "", telefone: item.telefone || "",
      dataNascimento: item.dataNascimento?.split("T")[0] || "",
      sexo: item.sexo || "", ativo: item.ativo !== false,
      endereco: item.endereco || "", cidade: item.cidade || "",
      estado: item.estado || "", cep: item.cep || "",
      nomeResponsavel: item.nomeResponsavel || "",
      telefoneResponsavel: item.telefoneResponsavel || "",
      emailResponsavel: item.emailResponsavel || "",
      foto: item.foto || "",
    });
    setActiveTab("pessoal");
    setModalOpen(true);
  };

  const save = async () => {
    if (!form.nome.trim()) { alert("Nome é obrigatório"); return; }
    setSaving(true);
    try {
      if (editItem) {
        await api.put(`/alunos/${editItem.id}`, form);
      } else {
        await api.post("/alunos", form);
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
      await api.delete(`/alunos/${deleteId}`);
      setDeleteId(null);
      load(page);
    } catch (err) {
      alert(err.message);
    }
  };

  const handleFotoChange = (e) => {
    const file = e.target.files[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = (ev) => setForm((prev) => ({ ...prev, foto: ev.target.result }));
    reader.readAsDataURL(file);
  };

  const totalPages = Math.ceil(total / PAGE_SIZE);
  const f = (k) => (e) => setForm((prev) => ({ ...prev, [k]: e.target.type === "checkbox" ? e.target.checked : e.target.value }));

  const formatCpf = (cpf) => {
    if (!cpf) return "—";
    return cpf.replace(/(\d{3})(\d{3})(\d{3})(\d{2})/, "$1.$2.$3-$4");
  };

  const TABS = [
    { key: "pessoal", label: "Dados Pessoais" },
    { key: "endereco", label: "Endereço" },
    { key: "responsavel", label: "Responsável" },
    { key: "foto", label: "Foto" },
  ];

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1 className="page-title">Alunos</h1>
          <p className="page-subtitle">Cadastro e gerenciamento de alunos</p>
        </div>
        <button className="btn btn-brand" onClick={openNew}>
          <Icon name="Plus" size={14} />
          Novo Aluno
        </button>
      </div>

      <div className="card">
        <div className="card-body">
          <div className="filter-bar">
            <div className="form-field" style={{ flex: 1 }}>
              <input className="form-input" placeholder="Buscar por nome, CPF ou e-mail..."
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
              <th>CPF</th>
              <th>E-mail</th>
              <th>Telefone</th>
              <th>Nasc.</th>
              <th>Status</th>
              <th>Ações</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              Array.from({ length: 10 }).map((_, i) => (
                <tr key={i}>{Array.from({ length: 7 }).map((_, j) => <td key={j}><div className="skeleton skeleton-text" /></td>)}</tr>
              ))
            ) : items.length === 0 ? (
              <tr><td colSpan={7}>
                <div className="empty-state">
                  <div className="empty-state-icon"><Icon name="GraduationCap" size={28} /></div>
                  <h3>Nenhum aluno encontrado</h3>
                  <p>Cadastre o primeiro aluno para começar a gerenciar as matrículas.</p>
                </div>
              </td></tr>
            ) : (
              items.map((item) => (
                <tr key={item.id}>
                  <td>
                    <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
                      {item.foto ? (
                        <img src={item.foto} alt="" style={{ width: 28, height: 28, borderRadius: "50%", objectFit: "cover" }} />
                      ) : (
                        <div style={{
                          width: 28, height: 28, borderRadius: "50%",
                          background: "var(--color-brand-dim)", color: "var(--color-brand)",
                          display: "flex", alignItems: "center", justifyContent: "center",
                          fontWeight: 700, fontSize: 12, flexShrink: 0,
                        }}>
                          {item.nome?.[0]?.toUpperCase() || "A"}
                        </div>
                      )}
                      <strong>{item.nome}</strong>
                    </div>
                  </td>
                  <td className="td-muted">{formatCpf(item.cpf)}</td>
                  <td className="td-muted">{item.email || "—"}</td>
                  <td className="td-muted">{item.telefone || "—"}</td>
                  <td className="td-muted">
                    {item.dataNascimento
                      ? new Date(item.dataNascimento).toLocaleDateString("pt-BR")
                      : "—"}
                  </td>
                  <td>
                    <span className={`badge ${item.ativo !== false ? "badge-success" : "badge-danger"}`}>
                      {item.ativo !== false ? "Ativo" : "Inativo"}
                    </span>
                  </td>
                  <td>
                    <div className="td-actions">
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
        title={editItem ? "Editar Aluno" : "Novo Aluno"}
        size="lg"
        footer={
          <>
            <button className="btn btn-secondary" onClick={() => setModalOpen(false)}>Cancelar</button>
            <button className="btn btn-brand" onClick={save} disabled={saving}>
              {saving ? "Salvando..." : "Salvar"}
            </button>
          </>
        }
      >
        <div className="tabs">
          <div className="tabs-header">
            {TABS.map((t) => (
              <button key={t.key} className={`tab-btn ${activeTab === t.key ? "active" : ""}`} onClick={() => setActiveTab(t.key)}>
                {t.label}
              </button>
            ))}
          </div>
          <div className="tab-content">
            {activeTab === "pessoal" && (
              <div className="form-grid">
                <div className="form-field">
                  <label className="form-label required">Nome Completo</label>
                  <input className="form-input" value={form.nome} onChange={f("nome")} placeholder="Nome completo do aluno" />
                </div>
                <div className="form-grid-2">
                  <div className="form-field">
                    <label className="form-label">CPF</label>
                    <input className="form-input" value={form.cpf} onChange={f("cpf")} placeholder="000.000.000-00" />
                  </div>
                  <div className="form-field">
                    <label className="form-label">RG</label>
                    <input className="form-input" value={form.rg} onChange={f("rg")} placeholder="0000000-0" />
                  </div>
                </div>
                <div className="form-grid-2">
                  <div className="form-field">
                    <label className="form-label">E-mail</label>
                    <input className="form-input" type="email" value={form.email} onChange={f("email")} placeholder="aluno@escola.com" />
                  </div>
                  <div className="form-field">
                    <label className="form-label">Telefone</label>
                    <input className="form-input" value={form.telefone} onChange={f("telefone")} placeholder="(00) 00000-0000" />
                  </div>
                </div>
                <div className="form-grid-2">
                  <div className="form-field">
                    <label className="form-label">Data de Nascimento</label>
                    <input className="form-input" type="date" value={form.dataNascimento} onChange={f("dataNascimento")} />
                  </div>
                  <div className="form-field">
                    <label className="form-label">Sexo</label>
                    <select className="form-select" value={form.sexo} onChange={f("sexo")}>
                      <option value="">Selecione</option>
                      <option value="M">Masculino</option>
                      <option value="F">Feminino</option>
                      <option value="O">Outro</option>
                    </select>
                  </div>
                </div>
                <label className="form-checkbox">
                  <input type="checkbox" checked={form.ativo} onChange={f("ativo")} />
                  <span>Aluno ativo</span>
                </label>
              </div>
            )}

            {activeTab === "endereco" && (
              <div className="form-grid">
                <div className="form-field">
                  <label className="form-label">Endereço</label>
                  <input className="form-input" value={form.endereco} onChange={f("endereco")} placeholder="Rua, número, bairro" />
                </div>
                <div className="form-grid-3">
                  <div className="form-field">
                    <label className="form-label">Cidade</label>
                    <input className="form-input" value={form.cidade} onChange={f("cidade")} />
                  </div>
                  <div className="form-field">
                    <label className="form-label">Estado</label>
                    <select className="form-select" value={form.estado} onChange={f("estado")}>
                      <option value="">UF</option>
                      {ESTADOS_BR.map((e) => <option key={e} value={e}>{e}</option>)}
                    </select>
                  </div>
                  <div className="form-field">
                    <label className="form-label">CEP</label>
                    <input className="form-input" value={form.cep} onChange={f("cep")} placeholder="00000-000" />
                  </div>
                </div>
              </div>
            )}

            {activeTab === "responsavel" && (
              <div className="form-grid">
                <div className="form-field">
                  <label className="form-label">Nome do Responsável</label>
                  <input className="form-input" value={form.nomeResponsavel} onChange={f("nomeResponsavel")} placeholder="Nome completo do responsável" />
                </div>
                <div className="form-grid-2">
                  <div className="form-field">
                    <label className="form-label">Telefone do Responsável</label>
                    <input className="form-input" value={form.telefoneResponsavel} onChange={f("telefoneResponsavel")} placeholder="(00) 00000-0000" />
                  </div>
                  <div className="form-field">
                    <label className="form-label">E-mail do Responsável</label>
                    <input className="form-input" type="email" value={form.emailResponsavel} onChange={f("emailResponsavel")} />
                  </div>
                </div>
              </div>
            )}

            {activeTab === "foto" && (
              <div style={{ display: "flex", flexDirection: "column", alignItems: "center", gap: 20, padding: "20px 0" }}>
                {form.foto ? (
                  <img src={form.foto} alt="Foto do aluno" style={{
                    width: 120, height: 120, borderRadius: "50%", objectFit: "cover",
                    border: "3px solid var(--color-brand)",
                  }} />
                ) : (
                  <div style={{
                    width: 120, height: 120, borderRadius: "50%",
                    background: "var(--color-bg-3)",
                    display: "flex", alignItems: "center", justifyContent: "center",
                    color: "var(--color-text-2)", fontSize: 42,
                    border: "3px dashed var(--color-border)",
                  }}>
                    <Icon name="UserCog" size={42} />
                  </div>
                )}
                <div>
                  <label className="btn btn-secondary" style={{ cursor: "pointer" }}>
                    <Icon name="Upload" size={14} />
                    {form.foto ? "Alterar foto" : "Adicionar foto"}
                    <input type="file" accept="image/*" onChange={handleFotoChange} style={{ display: "none" }} />
                  </label>
                </div>
                {form.foto && (
                  <button className="btn btn-ghost btn-sm text-danger" onClick={() => setForm((prev) => ({ ...prev, foto: "" }))}>
                    <Icon name="Trash" size={13} />
                    Remover foto
                  </button>
                )}
                <p className="text-muted text-sm">Formatos aceitos: JPG, PNG, GIF. Tamanho máximo: 2MB.</p>
              </div>
            )}
          </div>
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
          Tem certeza que deseja excluir este aluno? Esta ação irá remover todos os dados associados.
        </p>
      </Modal>
    </div>
  );
}
