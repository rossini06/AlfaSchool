import { useCallback, useEffect, useState } from "react";
import { api } from "../../services/api";
import { Modal } from "../../components/Modal";
import { Pagination } from "../../components/Pagination";
import { Icon } from "../../components/Icon";
import { Feedback } from "../../components/access/Feedback";
import { ConfirmarModal } from "../../components/access/ConfirmarModal";

const PAGE_SIZE = 20;

const FORM_VAZIO = {
  name: "", document: "",
  adminNome: "", adminEmail: "", adminSenha: "",
  modulos: [],
};

/**
 * Redes de ensino (tenants), pela administração da Alfa.
 *
 * Criar uma rede aqui deixa ela pronta: perfis padrão, um administrador
 * que troca a senha no primeiro acesso e os módulos contratados. Antes a
 * tela mostrava campos que o backend não tinha e chamava endpoints que não
 * existiam — parecia cadastro, mas nada era gravado.
 */
export function RedesEnsinoPage() {
  const [itens, setItens] = useState([]);
  const [total, setTotal] = useState(0);
  const [pagina, setPagina] = useState(0);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState("");
  const [feedback, setFeedback] = useState(null);
  const [catalogo, setCatalogo] = useState([]);

  const [modal, setModal] = useState(false);
  const [editando, setEditando] = useState(null);
  const [form, setForm] = useState(FORM_VAZIO);
  const [salvando, setSalvando] = useState(false);
  const [mostrarSenha, setMostrarSenha] = useState(false);

  const [modulosDe, setModulosDe] = useState(null);
  const [modulosMarcados, setModulosMarcados] = useState(new Set());
  const [salvandoModulos, setSalvandoModulos] = useState(false);

  const [mudandoStatus, setMudandoStatus] = useState(null);
  const [processandoStatus, setProcessandoStatus] = useState(false);

  const carregar = useCallback(async (p = 0) => {
    setCarregando(true);
    setErro("");
    try {
      const data = await api.get(`/tenants?page=${p}&size=${PAGE_SIZE}`);
      setItens(data?.content || []);
      setTotal(data?.totalElements ?? 0);
      setPagina(p);
    } catch (e) {
      setErro(e.message);
    } finally {
      setCarregando(false);
    }
  }, []);

  useEffect(() => { carregar(0); }, [carregar]);

  // O catálogo de módulos é global; buscamos uma vez pelo tenant mestre
  // (qualquer rede serviria — a lista é a mesma, só muda o "contratado").
  const carregarCatalogo = useCallback(async (redeId) => {
    if (catalogo.length || !redeId) return;
    try {
      const lista = await api.get(`/tenants/${redeId}/modulos`);
      setCatalogo(lista || []);
    } catch {
      // sem catálogo a rede nasce sem módulos; dá para contratar depois
    }
  }, [catalogo.length]);

  useEffect(() => {
    if (itens.length) carregarCatalogo(itens[0].id);
  }, [itens, carregarCatalogo]);

  const abrirNova = () => {
    setEditando(null);
    setForm({ ...FORM_VAZIO, modulos: catalogo.map((m) => m.codigo) });
    setMostrarSenha(false);
    setModal(true);
  };

  const abrirEdicao = (rede) => {
    setEditando(rede);
    setForm({ ...FORM_VAZIO, name: rede.name || "", document: rede.document || "" });
    setModal(true);
  };

  const salvar = async () => {
    setSalvando(true);
    try {
      if (editando) {
        await api.put(`/tenants/${editando.id}`, { name: form.name, document: form.document });
        setFeedback({ tipo: "sucesso", mensagem: "Rede atualizada." });
      } else {
        await api.post("/tenants", form);
        setFeedback({
          tipo: "sucesso",
          mensagem: `Rede criada. ${form.adminNome} entra com ${form.adminEmail} e troca a senha no primeiro acesso.`,
        });
      }
      setModal(false);
      carregar(pagina);
    } catch (e) {
      setFeedback({ tipo: "erro", mensagem: e.message });
    } finally {
      setSalvando(false);
    }
  };

  const abrirModulos = async (rede) => {
    setModulosDe(rede);
    try {
      const lista = await api.get(`/tenants/${rede.id}/modulos`);
      setCatalogo(lista || []);
      setModulosMarcados(new Set((lista || []).filter((m) => m.contratado).map((m) => m.codigo)));
    } catch (e) {
      setFeedback({ tipo: "erro", mensagem: e.message });
      setModulosDe(null);
    }
  };

  const salvarModulos = async () => {
    setSalvandoModulos(true);
    try {
      await api.put(`/tenants/${modulosDe.id}/modulos`, { modulos: Array.from(modulosMarcados) });
      setFeedback({ tipo: "sucesso", mensagem: `Módulos de ${modulosDe.name} atualizados. Valem no próximo login.` });
      setModulosDe(null);
      carregar(pagina);
    } catch (e) {
      setFeedback({ tipo: "erro", mensagem: e.message });
    } finally {
      setSalvandoModulos(false);
    }
  };

  const confirmarStatus = async () => {
    setProcessandoStatus(true);
    try {
      await api.put(`/tenants/${mudandoStatus.id}/status`, { status: mudandoStatus.active ? "INATIVO" : "ATIVO" });
      setFeedback({ tipo: "sucesso", mensagem: mudandoStatus.active ? "Rede suspensa." : "Rede reativada." });
      setMudandoStatus(null);
      carregar(pagina);
    } catch (e) {
      setFeedback({ tipo: "erro", mensagem: e.message });
    } finally {
      setProcessandoStatus(false);
    }
  };

  const campo = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }));
  const alternarModuloForm = (codigo) =>
    setForm((f) => ({
      ...f,
      modulos: f.modulos.includes(codigo) ? f.modulos.filter((c) => c !== codigo) : [...f.modulos, codigo],
    }));

  const formValido = editando
    ? form.name.trim() && form.document.trim()
    : form.name.trim() && form.document.trim() && form.adminNome.trim() && form.adminEmail.trim() && form.adminSenha.length >= 8;

  const totalPaginas = Math.ceil(total / PAGE_SIZE);

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h1 className="page-title">Redes de ensino</h1>
          <p className="page-subtitle">Cada rede é um cliente da Alfa, com seus próprios usuários e módulos.</p>
        </div>
        <button className="btn btn-brand" onClick={abrirNova}>
          <Icon name="Plus" size={14} /> Nova rede
        </button>
      </div>

      <Feedback tipo={feedback?.tipo} mensagem={feedback?.mensagem} onFechar={() => setFeedback(null)} />
      {erro && <div className="login-error"><Icon name="AlertCircle" size={14} /> {erro}</div>}

      <div className="table-wrapper">
        <table className="data-table">
          <thead>
            <tr>
              <th>Rede</th>
              <th>CNPJ</th>
              <th>Módulos</th>
              <th>Situação</th>
              <th>Criada em</th>
              <th>Ações</th>
            </tr>
          </thead>
          <tbody>
            {carregando ? (
              Array.from({ length: 6 }).map((_, i) => (
                <tr key={i}>{Array.from({ length: 6 }).map((_, j) => <td key={j}><div className="skeleton skeleton-text" /></td>)}</tr>
              ))
            ) : itens.length === 0 ? (
              <tr><td colSpan={6}>
                <div className="empty-state">
                  <div className="empty-state-icon"><Icon name="Network" size={28} /></div>
                  <h3>Nenhuma rede cadastrada</h3>
                  <p>Cadastre a primeira rede de ensino para começar.</p>
                </div>
              </td></tr>
            ) : (
              itens.map((rede) => (
                <tr key={rede.id}>
                  <td>
                    <strong>{rede.name}</strong>
                    {rede.mestre && <span className="badge badge-secondary" style={{ marginLeft: 8 }}>Alfa</span>}
                  </td>
                  <td className="td-muted">{rede.mestre ? "—" : rede.document}</td>
                  <td>
                    {rede.mestre ? (
                      <span className="td-muted">todos</span>
                    ) : (rede.modulos || []).length === 0 ? (
                      <span className="badge badge-warning">nenhum</span>
                    ) : (
                      <div className="saas-modulos-chips">
                        {rede.modulos.map((m) => <span key={m} className="badge badge-brand">{m}</span>)}
                      </div>
                    )}
                  </td>
                  <td>
                    <span className={`badge ${rede.active ? "badge-success" : "badge-danger"}`}>
                      {rede.active ? "Ativa" : "Suspensa"}
                    </span>
                  </td>
                  <td className="td-muted">{rede.createdAt ? new Date(rede.createdAt).toLocaleDateString("pt-BR") : "—"}</td>
                  <td>
                    {!rede.mestre && (
                      <div className="td-actions">
                        <button className="btn btn-ghost btn-sm" onClick={() => abrirModulos(rede)} title="Módulos contratados">
                          <Icon name="LayoutGrid" size={13} /> Módulos
                        </button>
                        <button className="btn btn-ghost btn-sm" onClick={() => abrirEdicao(rede)} title="Editar" aria-label="Editar">
                          <Icon name="Edit" size={13} />
                        </button>
                        <button
                          className={`btn btn-ghost btn-sm ${rede.active ? "text-danger" : ""}`}
                          onClick={() => setMudandoStatus(rede)}
                          title={rede.active ? "Suspender" : "Reativar"}
                          aria-label={rede.active ? "Suspender" : "Reativar"}
                        >
                          <Icon name={rede.active ? "Lock" : "Unlock"} size={13} />
                        </button>
                      </div>
                    )}
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
        <Pagination page={pagina} totalPages={totalPaginas} total={total} pageSize={PAGE_SIZE} onPageChange={(p) => carregar(p)} />
      </div>

      {/* Nova / editar */}
      <Modal
        isOpen={modal}
        onClose={() => setModal(false)}
        title={editando ? "Editar rede de ensino" : "Nova rede de ensino"}
        size="lg"
        footer={
          <>
            <button className="btn btn-secondary" onClick={() => setModal(false)}>Cancelar</button>
            <button className="btn btn-brand" onClick={salvar} disabled={salvando || !formValido}>
              {salvando ? "Salvando..." : editando ? "Salvar" : "Criar rede"}
            </button>
          </>
        }
      >
        <div className="form-grid">
          <div className="form-grid-2">
            <div className="form-field">
              <label className="form-label required">Nome da rede</label>
              <input className="form-input" value={form.name} onChange={campo("name")} placeholder="Colégio Mundo do Saber" />
            </div>
            <div className="form-field">
              <label className="form-label required">CNPJ</label>
              <input className="form-input" value={form.document} onChange={campo("document")} placeholder="00.000.000/0000-00" />
            </div>
          </div>

          {!editando && (
            <>
              <div className="saas-form-secao">
                <h3>Administrador da rede</h3>
                <p>Entra com este e-mail e troca a senha no primeiro acesso.</p>
              </div>
              <div className="form-grid-2">
                <div className="form-field">
                  <label className="form-label required">Nome</label>
                  <input className="form-input" value={form.adminNome} onChange={campo("adminNome")} placeholder="Nome completo" />
                </div>
                <div className="form-field">
                  <label className="form-label required">E-mail</label>
                  <input className="form-input" type="email" value={form.adminEmail} onChange={campo("adminEmail")} placeholder="diretor@escola.com.br" />
                </div>
              </div>
              <div className="form-field">
                <label className="form-label required">Senha inicial</label>
                <div className="saas-senha">
                  <input
                    className="form-input"
                    type={mostrarSenha ? "text" : "password"}
                    value={form.adminSenha}
                    onChange={campo("adminSenha")}
                    placeholder="Mínimo de 8 caracteres"
                    autoComplete="new-password"
                  />
                  <button type="button" className="btn btn-ghost btn-sm" onClick={() => setMostrarSenha((v) => !v)} title={mostrarSenha ? "Ocultar" : "Mostrar"}>
                    <Icon name="Eye" size={14} />
                  </button>
                </div>
              </div>

              <div className="saas-form-secao">
                <h3>Módulos contratados</h3>
                <p>Dá para mudar depois, em "Módulos" na lista.</p>
              </div>
              <div className="saas-modulos-grid">
                {catalogo.map((m) => (
                  <label key={m.codigo} className={`saas-modulo ${form.modulos.includes(m.codigo) ? "marcado" : ""}`}>
                    <input type="checkbox" checked={form.modulos.includes(m.codigo)} onChange={() => alternarModuloForm(m.codigo)} />
                    <span>
                      <strong>{m.nome}</strong>
                      <small>{m.descricao}</small>
                    </span>
                  </label>
                ))}
              </div>
            </>
          )}
        </div>
      </Modal>

      {/* Módulos de uma rede existente */}
      <Modal
        isOpen={!!modulosDe}
        onClose={() => setModulosDe(null)}
        title={modulosDe ? `Módulos de ${modulosDe.name}` : ""}
        footer={
          <>
            <span className="perm-resumo"><strong>{modulosMarcados.size}</strong> de {catalogo.length} contratados</span>
            <button className="btn btn-secondary" onClick={() => setModulosDe(null)}>Cancelar</button>
            <button className="btn btn-brand" onClick={salvarModulos} disabled={salvandoModulos}>
              {salvandoModulos ? "Salvando..." : "Salvar"}
            </button>
          </>
        }
      >
        <p className="form-hint" style={{ marginBottom: 12 }}>
          O que não estiver marcado deixa de aparecer para a escola no próximo login.
        </p>
        <div className="saas-modulos-grid">
          {catalogo.map((m) => (
            <label key={m.codigo} className={`saas-modulo ${modulosMarcados.has(m.codigo) ? "marcado" : ""}`}>
              <input
                type="checkbox"
                checked={modulosMarcados.has(m.codigo)}
                onChange={() =>
                  setModulosMarcados((atual) => {
                    const prox = new Set(atual);
                    if (prox.has(m.codigo)) prox.delete(m.codigo); else prox.add(m.codigo);
                    return prox;
                  })
                }
              />
              <span>
                <strong>{m.nome}</strong>
                <small>{m.descricao}</small>
              </span>
            </label>
          ))}
        </div>
      </Modal>

      <ConfirmarModal
        aberto={!!mudandoStatus}
        titulo={mudandoStatus?.active ? "Suspender esta rede?" : "Reativar esta rede?"}
        textoConfirmar={mudandoStatus?.active ? "Suspender" : "Reativar"}
        processando={processandoStatus}
        onCancelar={() => setMudandoStatus(null)}
        onConfirmar={confirmarStatus}
      >
        <p>
          {mudandoStatus?.active
            ? <><strong>{mudandoStatus?.name}</strong> fica suspensa: os dados continuam guardados, mas ninguém da rede consegue entrar.</>
            : <><strong>{mudandoStatus?.name}</strong> volta a aceitar logins normalmente.</>}
        </p>
      </ConfirmarModal>
    </div>
  );
}
