import { useState, useEffect, useCallback } from "react";
import { api } from "../services/api";
import { Modal } from "../components/Modal";
import { Pagination } from "../components/Pagination";
import { Icon } from "../components/Icon";
import { Avatar } from "../components/Avatar";
import { useCepLookup } from "../hooks/useCepLookup";

const PAGE_SIZE = 20;

const ESTADOS_BR = ["AC","AL","AP","AM","BA","CE","DF","ES","GO","MA","MT","MS","MG","PA","PB","PR","PE","PI","RJ","RN","RS","RO","RR","SC","SP","SE","TO"];

const ESTADOS_CIVIS = ["Solteiro", "Casado", "Divorciado", "Viúvo", "União Estável", "Outro"];

const EMPTY_FORM = {
  nome: "", cpf: "", rg: "",
  dataNascimento: "", sexo: "", estadoCivil: "",
  profissao: "", empresa: "",
  email: "", emailAlternativo: "",
  telefone: "", telefone2: "", whatsapp: "",
  logradouro: "", numeroEndereco: "", complemento: "",
  bairro: "", cidade: "", estado: "", cep: "",
  foto: "", observacoes: "",
  tipo: "financeiro", principal: false,
};

export function ResponsaveisPage() {
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
  const { cepLoading, handleCepBlur: cepLookup } = useCepLookup(setForm);

  const load = useCallback(async (p = 0) => {
    setLoading(true);
    setError("");
    try {
      const params = new URLSearchParams({ page: p, size: PAGE_SIZE });
      if (search) params.set("search", search);
      const data = await api.get(`/responsaveis?${params}`);
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

  const openNew = () => {
    setEditItem(null);
    setForm(EMPTY_FORM);
    setActiveTab("pessoal");
    setModalOpen(true);
  };

  const openEdit = (item) => {
    setEditItem(item);
    setForm({
      nome: item.nome || "",
      cpf: item.cpf || "",
      rg: item.rg || "",
      dataNascimento: item.dataNascimento?.split("T")[0] || item.dataNascimento || "",
      sexo: item.sexo || "",
      estadoCivil: item.estadoCivil || "",
      profissao: item.profissao || "",
      empresa: item.empresa || "",
      email: item.email || "",
      emailAlternativo: item.emailAlternativo || "",
      telefone: item.telefone || "",
      telefone2: item.telefone2 || "",
      whatsapp: item.whatsapp || "",
      logradouro: item.logradouro || "",
      numeroEndereco: item.numeroEndereco || "",
      complemento: item.complemento || "",
      bairro: item.bairro || "",
      cidade: item.cidade || "",
      estado: item.estado || "",
      cep: item.cep || "",
      foto: item.foto || "",
      observacoes: item.observacoes || "",
      tipo: item.tipo || "financeiro",
      principal: item.principal || false,
    });
    setActiveTab("pessoal");
    setModalOpen(true);
  };

  const save = async () => {
    if (!form.nome.trim()) { alert("Nome é obrigatório"); return; }
    setSaving(true);
    try {
      const payload = { ...form };
      if (!payload.dataNascimento) payload.dataNascimento = null;
      if (editItem) {
        await api.put(`/responsaveis/${editItem.id}`, payload);
      } else {
        await api.post("/responsaveis", payload);
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
      await api.delete(`/responsaveis/${deleteId}`);
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
  const f = (k) => (e) => setForm((prev) => ({
    ...prev,
    [k]: e.target.type === "checkbox" ? e.target.checked : e.target.value,
  }));

  const formatCpf = (cpf) => {
    if (!cpf) return "—";
    return cpf.replace(/(\d{3})(\d{3})(\d{3})(\d{2})/, "$1.$2.$3-$4");
  };

  const TABS = [
    { key: "pessoal", label: "Dados Pessoais" },
    { key: "contato", label: "Contato" },
    { key: "endereco", label: "Endereço" },
    { key: "foto", label: "Foto" },
    { key: "observacoes", label: "Observações" },
  ];

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1 className="page-title">Responsáveis</h1>
          <p className="page-subtitle">Cadastro e gerenciamento de responsáveis</p>
        </div>
        <button className="btn btn-brand" onClick={openNew}>
          <Icon name="Plus" size={14} />
          Novo Responsável
        </button>
      </div>

      <div className="card">
        <div className="card-body">
          <div className="filter-bar">
            <div className="form-field" style={{ flex: 1 }}>
              <input
                className="form-input"
                placeholder="Buscar por nome, CPF ou e-mail..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                onKeyDown={(e) => e.key === "Enter" && load(0)}
              />
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

      {error && (
        <div className="login-error">
          <Icon name="AlertCircle" size={14} /> {error}
        </div>
      )}

      <div className="table-wrapper">
        <table className="data-table">
          <thead>
            <tr>
              <th>Nome</th>
              <th>CPF</th>
              <th>Telefone</th>
              <th>E-mail</th>
              <th>Cidade</th>
              <th>Ações</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              Array.from({ length: 10 }).map((_, i) => (
                <tr key={i}>
                  {Array.from({ length: 6 }).map((_, j) => (
                    <td key={j}><div className="skeleton skeleton-text" /></td>
                  ))}
                </tr>
              ))
            ) : items.length === 0 ? (
              <tr>
                <td colSpan={6}>
                  <div className="empty-state">
                    <div className="empty-state-icon"><Icon name="Users2" size={28} /></div>
                    <h3>Nenhum responsável encontrado</h3>
                    <p>Cadastre o primeiro responsável para começar.</p>
                  </div>
                </td>
              </tr>
            ) : (
              items.map((item) => (
                <tr key={item.id}>
                  <td>
                    <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
                      <Avatar foto={item.foto} nome={item.nome} />
                      <div>
                        <strong>{item.nome}</strong>
                        {item.profissao && (
                          <div style={{ fontSize: 11, color: "var(--color-text-2)" }}>{item.profissao}</div>
                        )}
                      </div>
                    </div>
                  </td>
                  <td className="td-muted">{formatCpf(item.cpf)}</td>
                  <td className="td-muted">{item.telefone || item.whatsapp || "—"}</td>
                  <td className="td-muted">{item.email || "—"}</td>
                  <td className="td-muted">
                    {item.cidade ? `${item.cidade}${item.estado ? `/${item.estado}` : ""}` : "—"}
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
        <Pagination
          page={page}
          totalPages={totalPages}
          total={total}
          pageSize={PAGE_SIZE}
          onPageChange={(p) => load(p)}
        />
      </div>

      {/* Create/Edit Modal */}
      <Modal
        isOpen={modalOpen}
        onClose={() => setModalOpen(false)}
        title={editItem ? "Editar Responsável" : "Novo Responsável"}
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
              <button
                key={t.key}
                className={`tab-btn ${activeTab === t.key ? "active" : ""}`}
                onClick={() => setActiveTab(t.key)}
              >
                {t.label}
              </button>
            ))}
          </div>
          <div className="tab-content">
            {/* Dados Pessoais */}
            {activeTab === "pessoal" && (
              <div className="form-grid">
                <div className="form-field">
                  <label className="form-label required">Nome Completo</label>
                  <input
                    className="form-input"
                    value={form.nome}
                    onChange={f("nome")}
                    placeholder="Nome completo do responsável"
                  />
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
                <div className="form-grid-2">
                  <div className="form-field">
                    <label className="form-label">Estado Civil</label>
                    <select className="form-select" value={form.estadoCivil} onChange={f("estadoCivil")}>
                      <option value="">Selecione</option>
                      {ESTADOS_CIVIS.map((e) => <option key={e} value={e}>{e}</option>)}
                    </select>
                  </div>
                  <div className="form-field">
                    <label className="form-label">Profissão</label>
                    <input className="form-input" value={form.profissao} onChange={f("profissao")} placeholder="Ex: Engenheiro, Professor..." />
                  </div>
                </div>
                <div className="form-field">
                  <label className="form-label">Empresa / Organização</label>
                  <input className="form-input" value={form.empresa} onChange={f("empresa")} placeholder="Nome da empresa ou organização" />
                </div>
              </div>
            )}

            {/* Contato */}
            {activeTab === "contato" && (
              <div className="form-grid">
                <div className="form-field">
                  <label className="form-label">E-mail Principal</label>
                  <input className="form-input" type="email" value={form.email} onChange={f("email")} placeholder="email@exemplo.com" />
                </div>
                <div className="form-field">
                  <label className="form-label">E-mail Alternativo</label>
                  <input className="form-input" type="email" value={form.emailAlternativo} onChange={f("emailAlternativo")} placeholder="outro@exemplo.com" />
                </div>
                <div className="form-grid-3">
                  <div className="form-field">
                    <label className="form-label">Telefone Principal</label>
                    <input className="form-input" value={form.telefone} onChange={f("telefone")} placeholder="(00) 00000-0000" />
                  </div>
                  <div className="form-field">
                    <label className="form-label">Telefone 2</label>
                    <input className="form-input" value={form.telefone2} onChange={f("telefone2")} placeholder="(00) 00000-0000" />
                  </div>
                  <div className="form-field">
                    <label className="form-label">WhatsApp</label>
                    <input className="form-input" value={form.whatsapp} onChange={f("whatsapp")} placeholder="(00) 00000-0000" />
                  </div>
                </div>
              </div>
            )}

            {/* Endereço */}
            {activeTab === "endereco" && (
              <div className="form-grid">
                <div className="form-grid-2">
                  <div className="form-field">
                    <label className="form-label">CEP</label>
                    <div style={{ position: "relative" }}>
                      <input
                        className="form-input"
                        value={form.cep}
                        onChange={f("cep")}
                        onBlur={(e) => cepLookup(e.target.value)}
                        placeholder="00000-000"
                        maxLength={9}
                      />
                      {cepLoading && (
                        <span style={{
                          position: "absolute", right: 10, top: "50%", transform: "translateY(-50%)",
                          fontSize: 11, color: "var(--color-text-2)",
                        }}>
                          Buscando...
                        </span>
                      )}
                    </div>
                  </div>
                  <div className="form-field">
                    <label className="form-label">Número</label>
                    <input className="form-input" value={form.numeroEndereco} onChange={f("numeroEndereco")} placeholder="Nº" />
                  </div>
                </div>
                <div className="form-field">
                  <label className="form-label">Logradouro</label>
                  <input className="form-input" value={form.logradouro} onChange={f("logradouro")} placeholder="Rua, Avenida, Travessa..." />
                </div>
                <div className="form-field">
                  <label className="form-label">Complemento</label>
                  <input className="form-input" value={form.complemento} onChange={f("complemento")} placeholder="Apto, Bloco, Sala..." />
                </div>
                <div className="form-grid-3">
                  <div className="form-field">
                    <label className="form-label">Bairro</label>
                    <input className="form-input" value={form.bairro} onChange={f("bairro")} />
                  </div>
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
                </div>
              </div>
            )}

            {/* Foto */}
            {activeTab === "foto" && (
              <div style={{ display: "flex", flexDirection: "column", alignItems: "center", gap: 20, padding: "20px 0" }}>
                {form.foto ? (
                  <img
                    src={form.foto}
                    alt="Foto do responsável"
                    style={{
                      width: 120, height: 120, borderRadius: "50%", objectFit: "cover",
                      border: "3px solid var(--color-brand)",
                    }}
                  />
                ) : (
                  <div style={{
                    width: 120, height: 120, borderRadius: "50%",
                    background: "var(--color-bg-3)",
                    display: "flex", alignItems: "center", justifyContent: "center",
                    color: "var(--color-text-2)",
                    border: "3px dashed var(--color-border)",
                  }}>
                    <Icon name="UserCog" size={42} />
                  </div>
                )}
                <div>
                  <label className="btn btn-secondary" style={{ cursor: "pointer" }}>
                    <Icon name="Upload" size={14} />
                    {form.foto ? "Alterar foto" : "Adicionar foto"}
                    <input
                      type="file"
                      accept="image/*"
                      onChange={handleFotoChange}
                      style={{ display: "none" }}
                    />
                  </label>
                </div>
                {form.foto && (
                  <button
                    className="btn btn-ghost btn-sm text-danger"
                    onClick={() => setForm((prev) => ({ ...prev, foto: "" }))}
                  >
                    <Icon name="Trash" size={13} />
                    Remover foto
                  </button>
                )}
                <p className="text-muted text-sm">Formatos aceitos: JPG, PNG, GIF. Tamanho máximo: 2MB.</p>
              </div>
            )}

            {/* Observações */}
            {activeTab === "observacoes" && (
              <div className="form-grid">
                <div className="form-field">
                  <label className="form-label">Observações</label>
                  <textarea
                    className="form-input"
                    value={form.observacoes}
                    onChange={f("observacoes")}
                    placeholder="Anotações sobre o responsável..."
                    rows={8}
                    style={{ resize: "vertical", minHeight: 160 }}
                  />
                </div>
              </div>
            )}
          </div>
        </div>
      </Modal>

      {/* Delete confirm */}
      <Modal
        isOpen={!!deleteId}
        onClose={() => setDeleteId(null)}
        title="Confirmar Exclusão"
        size="sm"
        footer={
          <>
            <button className="btn btn-secondary" onClick={() => setDeleteId(null)}>Cancelar</button>
            <button className="btn btn-danger" onClick={confirmDelete}>Excluir</button>
          </>
        }
      >
        <p style={{ color: "var(--color-text)" }}>
          Tem certeza que deseja excluir este responsável? Esta ação não pode ser desfeita.
        </p>
      </Modal>
    </div>
  );
}
