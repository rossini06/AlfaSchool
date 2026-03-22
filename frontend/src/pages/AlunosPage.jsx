import { useState, useEffect, useCallback, useRef } from "react";
import { api } from "../services/api";
import { Modal } from "../components/Modal";
import { Pagination } from "../components/Pagination";
import { Icon } from "../components/Icon";
import { Avatar } from "../components/Avatar";
import { useCepLookup } from "../hooks/useCepLookup";

const PAGE_SIZE = 20;

const ESTADOS_BR = ["AC","AL","AP","AM","BA","CE","DF","ES","GO","MA","MT","MS","MG","PA","PB","PR","PE","PI","RJ","RN","RS","RO","RR","SC","SP","SE","TO"];

const PARENTESCO_OPTS = [
  "Pai", "Mãe", "Padrasto", "Madrasta", "Avô", "Avó",
  "Tio", "Tia", "Irmão", "Irmã", "Tutor Legal", "Responsável Legal", "Outro"
];

const EMPTY_FORM = {
  nome: "", cpf: "", rg: "", email: "", telefone: "",
  dataNascimento: "", sexo: "", ativo: true,
  endereco: "", cidade: "", estado: "", cep: "",
  nomeResponsavel: "", telefoneResponsavel: "", emailResponsavel: "",
  foto: "", observacoesMedicas: "",
  responsaveis: [],
};

const EMPTY_LINK = {
  responsavelId: null,
  responsavel: null,
  parentesco: "outro",
  parentescoDescricao: "",
  responsavelFinanceiro: false,
  responsavelAcademico: false,
  autorizadoBuscar: true,
  principal: false,
  observacoes: "",
  _linkId: null,
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

  const { cepLoading, handleCepBlur: cepLookup } = useCepLookup(setForm, (data, prev) => ({
    endereco: [data.logradouro, data.bairro].filter(Boolean).join(", ") || prev.endereco,
    cidade: data.localidade || prev.cidade,
    estado: data.uf || prev.estado,
  }));

  const [searchRespOpen, setSearchRespOpen] = useState(false);
  const [searchRespQuery, setSearchRespQuery] = useState("");
  const [searchRespResults, setSearchRespResults] = useState([]);
  const [searchRespLoading, setSearchRespLoading] = useState(false);
  const [selectedResp, setSelectedResp] = useState(null);
  const [linkForm, setLinkForm] = useState(EMPTY_LINK);
  const [linkConfigOpen, setLinkConfigOpen] = useState(false);
  const [editLinkIndex, setEditLinkIndex] = useState(null);
  const searchRespTimer = useRef(null);

  useEffect(() => () => clearTimeout(searchRespTimer.current), []);

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

  const loadLinkedResponsaveis = async (alunoId) => {
    try {
      const data = await api.get(`/alunos/${alunoId}/responsaveis`);
      return (data || []).map((link) => ({
        responsavelId: link.responsavelId,
        responsavel: link.responsavel,
        parentesco: link.parentesco || "outro",
        parentescoDescricao: link.parentescoDescricao || "",
        responsavelFinanceiro: link.responsavelFinanceiro || false,
        responsavelAcademico: link.responsavelAcademico || false,
        autorizadoBuscar: link.autorizadoBuscar !== false,
        principal: link.principal || false,
        observacoes: link.observacoes || "",
        _linkId: link.id,
      }));
    } catch {
      return [];
    }
  };

  const openNew = () => {
    setEditItem(null);
    setForm(EMPTY_FORM);
    setActiveTab("pessoal");
    setModalOpen(true);
  };

  const openEdit = async (item) => {
    setEditItem(item);
    const responsaveis = await loadLinkedResponsaveis(item.id);
    setForm({
      nome: item.nome || "",
      cpf: item.cpf || "",
      rg: item.rg || "",
      email: item.email || "",
      telefone: item.telefone || "",
      dataNascimento: item.dataNascimento?.split("T")[0] || "",
      sexo: item.sexo || "",
      ativo: item.ativo !== false,
      endereco: item.endereco || "",
      cidade: item.cidade || "",
      estado: item.estado || "",
      cep: item.cep || "",
      nomeResponsavel: item.nomeResponsavel || "",
      telefoneResponsavel: item.telefoneResponsavel || "",
      emailResponsavel: item.emailResponsavel || "",
      foto: item.foto || "",
      observacoesMedicas: item.observacoesMedicas || "",
      responsaveis,
    });
    setActiveTab("pessoal");
    setModalOpen(true);
  };

  const save = async () => {
    if (!form.nome.trim()) { alert("Nome é obrigatório"); return; }
    setSaving(true);
    try {
      let savedAluno;
      const payload = { ...form };
      delete payload.responsaveis;
      if (editItem) {
        savedAluno = await api.put(`/alunos/${editItem.id}`, payload);
      } else {
        savedAluno = await api.post("/alunos", payload);
      }

      const alunoId = savedAluno?.id || editItem?.id;
      if (alunoId && form.responsaveis.length > 0) {
        await Promise.all(form.responsaveis.map((link) => {
          const linkPayload = {
            responsavelId: link.responsavelId,
            parentesco: link.parentesco,
            parentescoDescricao: link.parentescoDescricao || null,
            responsavelFinanceiro: link.responsavelFinanceiro,
            responsavelAcademico: link.responsavelAcademico,
            autorizadoBuscar: link.autorizadoBuscar,
            principal: link.principal,
            observacoes: link.observacoes || null,
          };
          return link._linkId
            ? api.put(`/alunos/${alunoId}/responsaveis/${link._linkId}`, linkPayload)
            : api.post(`/alunos/${alunoId}/responsaveis`, linkPayload);
        }));
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

  // Responsável search
  const searchResponsaveis = async (q) => {
    if (!q.trim()) { setSearchRespResults([]); return; }
    setSearchRespLoading(true);
    try {
      const data = await api.get(`/responsaveis?search=${encodeURIComponent(q)}&page=0&size=10`);
      setSearchRespResults(data?.content || data || []);
    } catch {
      setSearchRespResults([]);
    } finally {
      setSearchRespLoading(false);
    }
  };

  const onSearchRespChange = (e) => {
    const val = e.target.value;
    setSearchRespQuery(val);
    clearTimeout(searchRespTimer.current);
    searchRespTimer.current = setTimeout(() => searchResponsaveis(val), 350);
  };

  const selectResponsavel = (resp) => {
    setSelectedResp(resp);
    setLinkForm({ ...EMPTY_LINK, responsavelId: resp.id, responsavel: resp });
    setSearchRespOpen(false);
    setLinkConfigOpen(true);
  };

  const confirmLink = () => {
    if (!linkForm.responsavelId) return;
    if (editLinkIndex !== null) {
      setForm((prev) => {
        const updated = [...prev.responsaveis];
        updated[editLinkIndex] = { ...linkForm };
        return { ...prev, responsaveis: updated };
      });
      setEditLinkIndex(null);
    } else {
      setForm((prev) => ({
        ...prev,
        responsaveis: [...prev.responsaveis, { ...linkForm }],
      }));
    }
    setLinkConfigOpen(false);
    setSelectedResp(null);
    setLinkForm(EMPTY_LINK);
  };

  const editLink = (idx) => {
    const link = form.responsaveis[idx];
    setLinkForm({ ...link });
    setSelectedResp(link.responsavel);
    setEditLinkIndex(idx);
    setLinkConfigOpen(true);
  };

  const removeLink = async (idx) => {
    const link = form.responsaveis[idx];
    if (link._linkId && editItem) {
      try {
        await api.delete(`/alunos/${editItem.id}/responsaveis/${link._linkId}`);
      } catch (err) {
        alert(err.message);
        return;
      }
    }
    setForm((prev) => {
      const updated = prev.responsaveis.filter((_, i) => i !== idx);
      return { ...prev, responsaveis: updated };
    });
  };

  const lf = (k) => (e) => setLinkForm((prev) => ({
    ...prev,
    [k]: e.target.type === "checkbox" ? e.target.checked : e.target.value,
  }));

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
    { key: "endereco", label: "Endereço" },
    { key: "responsavel", label: `Responsáveis${form.responsaveis.length > 0 ? ` (${form.responsaveis.length})` : ""}` },
    { key: "foto", label: "Foto" },
    { key: "saude", label: "Saúde" },
  ];

  const parentescoBadgeColor = (p) => {
    const lower = (p || "").toLowerCase();
    if (["pai", "mãe"].includes(lower)) return "badge-success";
    if (["avô", "avó"].includes(lower)) return "badge-warning";
    if (["tutor legal", "responsável legal"].includes(lower)) return "badge-danger";
    return "badge-info";
  };

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
                      <Avatar foto={item.foto} nome={item.nome} />
                      <strong>{item.nome}</strong>
                    </div>
                  </td>
                  <td className="td-muted">{formatCpf(item.cpf)}</td>
                  <td className="td-muted">{item.email || "—"}</td>
                  <td className="td-muted">{item.telefone || "—"}</td>
                  <td className="td-muted">
                    {item.dataNascimento
                      ? new Date(item.dataNascimento + "T00:00:00").toLocaleDateString("pt-BR")
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

      {/* Main Modal */}
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
                    <label className="form-label">Cidade</label>
                    <input className="form-input" value={form.cidade} onChange={f("cidade")} />
                  </div>
                </div>
                <div className="form-field">
                  <label className="form-label">Endereço</label>
                  <input className="form-input" value={form.endereco} onChange={f("endereco")} placeholder="Rua, número, bairro" />
                </div>
                <div className="form-field">
                  <label className="form-label">Estado</label>
                  <select className="form-select" value={form.estado} onChange={f("estado")}>
                    <option value="">UF</option>
                    {ESTADOS_BR.map((e) => <option key={e} value={e}>{e}</option>)}
                  </select>
                </div>
              </div>
            )}

            {activeTab === "responsavel" && (
              <div className="form-grid">
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 8 }}>
                  <span style={{ fontSize: 13, color: "var(--color-text-2)" }}>
                    {form.responsaveis.length === 0
                      ? "Nenhum responsável vinculado"
                      : `${form.responsaveis.length} responsável(is) vinculado(s)`}
                  </span>
                  <button
                    className="btn btn-brand btn-sm"
                    onClick={() => {
                      setSearchRespQuery("");
                      setSearchRespResults([]);
                      setSearchRespOpen(true);
                    }}
                  >
                    <Icon name="Plus" size={13} />
                    Vincular Responsável
                  </button>
                </div>

                {form.responsaveis.length === 0 ? (
                  <div style={{
                    textAlign: "center", padding: "32px 0",
                    color: "var(--color-text-2)", border: "2px dashed var(--color-border)",
                    borderRadius: 8,
                  }}>
                    <Icon name="Users2" size={32} />
                    <p style={{ marginTop: 8, fontSize: 13 }}>
                      Nenhum responsável vinculado. Clique em "Vincular Responsável" para adicionar.
                    </p>
                  </div>
                ) : (
                  <div style={{ display: "flex", flexDirection: "column", gap: 10 }}>
                    {form.responsaveis.map((link, idx) => (
                      <div key={idx} style={{
                        display: "flex", alignItems: "center", gap: 12,
                        padding: "10px 14px", borderRadius: 8,
                        background: "var(--color-bg-3)",
                        border: "1px solid var(--color-border)",
                      }}>
                        {link.responsavel?.foto ? (
                          <img src={link.responsavel.foto} alt="" style={{
                            width: 36, height: 36, borderRadius: "50%", objectFit: "cover", flexShrink: 0,
                          }} />
                        ) : (
                          <div style={{
                            width: 36, height: 36, borderRadius: "50%",
                            background: "var(--color-brand-dim)", color: "var(--color-brand)",
                            display: "flex", alignItems: "center", justifyContent: "center",
                            fontWeight: 700, fontSize: 14, flexShrink: 0,
                          }}>
                            {link.responsavel?.nome?.[0]?.toUpperCase() || "R"}
                          </div>
                        )}
                        <div style={{ flex: 1, minWidth: 0 }}>
                          <div style={{ fontWeight: 600, fontSize: 13, color: "var(--color-text)" }}>
                            {link.responsavel?.nome || "Responsável"}
                          </div>
                          <div style={{ display: "flex", gap: 6, flexWrap: "wrap", marginTop: 4 }}>
                            <span className={`badge ${parentescoBadgeColor(link.parentesco)}`} style={{ fontSize: 10 }}>
                              {link.parentesco}
                            </span>
                            {link.responsavelFinanceiro && (
                              <span className="badge badge-warning" style={{ fontSize: 10 }}>Financeiro</span>
                            )}
                            {link.responsavelAcademico && (
                              <span className="badge badge-info" style={{ fontSize: 10 }}>Acadêmico</span>
                            )}
                            {link.principal && (
                              <span className="badge badge-success" style={{ fontSize: 10 }}>Principal</span>
                            )}
                            {link.autorizadoBuscar && (
                              <span className="badge badge-secondary" style={{ fontSize: 10 }}>
                                <Icon name="CheckCircle" size={9} /> Autorizado buscar
                              </span>
                            )}
                          </div>
                        </div>
                        <div className="td-actions">
                          <button className="btn btn-ghost btn-sm" onClick={() => editLink(idx)}>
                            <Icon name="Edit" size={13} />
                          </button>
                          <button className="btn btn-ghost btn-sm text-danger" onClick={() => removeLink(idx)}>
                            <Icon name="Trash" size={13} />
                          </button>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
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

            {activeTab === "saude" && (
              <div className="form-grid">
                <div className="form-field">
                  <label className="form-label">Observações Médicas</label>
                  <textarea
                    className="form-textarea"
                    value={form.observacoesMedicas}
                    onChange={f("observacoesMedicas")}
                    rows={6}
                    placeholder="Alergias, medicamentos em uso, condições especiais, necessidades de saúde..."
                  />
                  <span className="form-hint">Estas informações são confidenciais e visíveis apenas à equipe da escola.</span>
                </div>
              </div>
            )}
          </div>
        </div>
      </Modal>

      {/* Search Responsável Modal */}
      <Modal
        isOpen={searchRespOpen}
        onClose={() => setSearchRespOpen(false)}
        title="Buscar Responsável"
        size="md"
        footer={
          <button className="btn btn-secondary" onClick={() => setSearchRespOpen(false)}>Cancelar</button>
        }
      >
        <div className="form-field" style={{ marginBottom: 16 }}>
          <input
            className="form-input"
            placeholder="Digite o nome ou CPF do responsável..."
            value={searchRespQuery}
            onChange={onSearchRespChange}
            autoFocus
          />
        </div>
        {searchRespLoading && (
          <div style={{ textAlign: "center", color: "var(--color-text-2)", padding: 16 }}>
            Buscando...
          </div>
        )}
        {!searchRespLoading && searchRespQuery && searchRespResults.length === 0 && (
          <div style={{ textAlign: "center", color: "var(--color-text-2)", padding: 16, fontSize: 13 }}>
            Nenhum responsável encontrado. Cadastre na página de Responsáveis primeiro.
          </div>
        )}
        {searchRespResults.length > 0 && (
          <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
            {searchRespResults.map((resp) => (
              <button
                key={resp.id}
                style={{
                  display: "flex", alignItems: "center", gap: 12,
                  padding: "10px 14px", borderRadius: 8,
                  background: "var(--color-bg-3)",
                  border: "1px solid var(--color-border)",
                  cursor: "pointer", textAlign: "left", width: "100%",
                }}
                onClick={() => selectResponsavel(resp)}
              >
                {resp.foto ? (
                  <img src={resp.foto} alt="" style={{ width: 36, height: 36, borderRadius: "50%", objectFit: "cover" }} />
                ) : (
                  <div style={{
                    width: 36, height: 36, borderRadius: "50%",
                    background: "var(--color-brand-dim)", color: "var(--color-brand)",
                    display: "flex", alignItems: "center", justifyContent: "center",
                    fontWeight: 700, fontSize: 14, flexShrink: 0,
                  }}>
                    {resp.nome?.[0]?.toUpperCase() || "R"}
                  </div>
                )}
                <div>
                  <div style={{ fontWeight: 600, fontSize: 13, color: "var(--color-text)" }}>{resp.nome}</div>
                  <div style={{ fontSize: 11, color: "var(--color-text-2)" }}>
                    {resp.cpf ? resp.cpf.replace(/(\d{3})(\d{3})(\d{3})(\d{2})/, "$1.$2.$3-$4") : ""}
                    {resp.email ? ` • ${resp.email}` : ""}
                  </div>
                </div>
              </button>
            ))}
          </div>
        )}
      </Modal>

      {/* Link Config Modal */}
      <Modal
        isOpen={linkConfigOpen}
        onClose={() => { setLinkConfigOpen(false); setEditLinkIndex(null); }}
        title={editLinkIndex !== null ? "Editar Vínculo" : "Configurar Vínculo"}
        size="md"
        footer={
          <>
            <button className="btn btn-secondary" onClick={() => { setLinkConfigOpen(false); setEditLinkIndex(null); }}>Cancelar</button>
            <button className="btn btn-brand" onClick={confirmLink}>Confirmar</button>
          </>
        }
      >
        {selectedResp && (
          <div style={{
            display: "flex", alignItems: "center", gap: 12,
            padding: "10px 14px", borderRadius: 8,
            background: "var(--color-bg-3)", border: "1px solid var(--color-border)",
            marginBottom: 20,
          }}>
            {selectedResp.foto ? (
              <img src={selectedResp.foto} alt="" style={{ width: 40, height: 40, borderRadius: "50%", objectFit: "cover" }} />
            ) : (
              <div style={{
                width: 40, height: 40, borderRadius: "50%",
                background: "var(--color-brand-dim)", color: "var(--color-brand)",
                display: "flex", alignItems: "center", justifyContent: "center",
                fontWeight: 700, fontSize: 16, flexShrink: 0,
              }}>
                {selectedResp.nome?.[0]?.toUpperCase() || "R"}
              </div>
            )}
            <div>
              <div style={{ fontWeight: 600, fontSize: 14 }}>{selectedResp.nome}</div>
              {selectedResp.email && (
                <div style={{ fontSize: 12, color: "var(--color-text-2)" }}>{selectedResp.email}</div>
              )}
            </div>
          </div>
        )}
        <div className="form-grid">
          <div className="form-grid-2">
            <div className="form-field">
              <label className="form-label">Parentesco</label>
              <select className="form-select" value={linkForm.parentesco} onChange={lf("parentesco")}>
                {PARENTESCO_OPTS.map((p) => (
                  <option key={p} value={p.toLowerCase()}>{p}</option>
                ))}
              </select>
            </div>
            <div className="form-field">
              <label className="form-label">Descrição (opcional)</label>
              <input
                className="form-input"
                value={linkForm.parentescoDescricao}
                onChange={lf("parentescoDescricao")}
                placeholder="Ex: Tio paterno"
              />
            </div>
          </div>
          <div style={{ display: "flex", flexDirection: "column", gap: 8 }}>
            <label className="form-checkbox">
              <input type="checkbox" checked={linkForm.responsavelFinanceiro} onChange={lf("responsavelFinanceiro")} />
              <span>Responsável Financeiro</span>
            </label>
            <label className="form-checkbox">
              <input type="checkbox" checked={linkForm.responsavelAcademico} onChange={lf("responsavelAcademico")} />
              <span>Responsável Acadêmico</span>
            </label>
            <label className="form-checkbox">
              <input type="checkbox" checked={linkForm.autorizadoBuscar} onChange={lf("autorizadoBuscar")} />
              <span>Autorizado a buscar o aluno</span>
            </label>
            <label className="form-checkbox">
              <input type="checkbox" checked={linkForm.principal} onChange={lf("principal")} />
              <span>Responsável principal</span>
            </label>
          </div>
          <div className="form-field">
            <label className="form-label">Observações</label>
            <textarea
              className="form-input"
              value={linkForm.observacoes}
              onChange={lf("observacoes")}
              rows={3}
              style={{ resize: "vertical" }}
            />
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
