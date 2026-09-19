import { useCallback, useEffect, useMemo, useState } from "react";
import { api } from "../services/api";
import { Modal } from "../components/Modal";
import { Icon } from "../components/Icon";

/**
 * Perfis e Permissões.
 *
 * A matriz é mostrada agrupada por ÁREA, não como uma lista de 46
 * checkboxes — quem organiza o trabalho da escola precisa raciocinar por
 * assunto ("o que a portaria faz com retirada?"), não decorar chaves.
 */

/** Traduz o prefixo técnico para a linguagem de quem usa. */
const AREA_ROTULO = {
  ESCOLA: "Escola e unidades",
  USUARIOS: "Usuários",
  PERFIS: "Perfis e permissões",
  AUDITORIA: "Auditoria",
  ALUNOS: "Alunos",
  RESPONSAVEIS: "Responsáveis",
  PROFESSORES: "Professores",
  CURSOS: "Cursos e disciplinas",
  TURMAS: "Turmas",
  MATRICULAS: "Matrículas",
  DIARIO: "Diário de classe",
  NOTAS: "Notas e boletim",
  FINANCEIRO: "Financeiro",
  ACESSO: "Controle de acesso",
  NOTIFICACOES: "Notificações",
  PORTAL: "Portal da família",
};

export function PerfisPermissoesPage() {
  const [perfis, setPerfis] = useState([]);
  const [areas, setAreas] = useState({});
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState("");
  const [aviso, setAviso] = useState("");
  const [editando, setEditando] = useState(null);
  const [marcadas, setMarcadas] = useState(new Set());
  const [salvando, setSalvando] = useState(false);

  const carregar = useCallback(async () => {
    setCarregando(true);
    setErro("");
    try {
      const [lista, catalogo] = await Promise.all([
        api.get("/perfis"),
        api.get("/perfis/catalogo"),
      ]);
      setPerfis(lista || []);
      setAreas(catalogo?.areas || {});
    } catch (e) {
      setErro(e.message || "Não foi possível carregar os perfis.");
    } finally {
      setCarregando(false);
    }
  }, []);

  useEffect(() => {
    carregar();
  }, [carregar]);

  const abrir = (perfil) => {
    setEditando(perfil);
    setMarcadas(new Set(perfil.permissoes || []));
    setAviso("");
  };

  const alternar = (chave) => {
    setMarcadas((atual) => {
      const proximo = new Set(atual);
      if (proximo.has(chave)) proximo.delete(chave);
      else proximo.add(chave);
      return proximo;
    });
  };

  const alternarArea = (itens, marcarTudo) => {
    setMarcadas((atual) => {
      const proximo = new Set(atual);
      itens.forEach((i) => (marcarTudo ? proximo.add(i.chave) : proximo.delete(i.chave)));
      return proximo;
    });
  };

  const salvar = async () => {
    if (!editando) return;
    setSalvando(true);
    setAviso("");
    try {
      await api.put(`/perfis/${editando.id}/permissoes`, {
        permissoes: Array.from(marcadas),
      });
      setEditando(null);
      await carregar();
    } catch (e) {
      setAviso(e.message || "Não foi possível salvar.");
    } finally {
      setSalvando(false);
    }
  };

  const areasOrdenadas = useMemo(() => Object.entries(areas), [areas]);

  return (
    <div className="page">
      <header className="page-header">
        <div>
          <h1>Perfis e Permissões</h1>
          <p className="page-subtitle">
            Marque o que cada função pode ver e fazer. As mudanças valem a partir do
            próximo login de quem tem o perfil.
          </p>
        </div>
      </header>

      {erro && <div className="alert alert-error">{erro}</div>}

      {carregando ? (
        <p className="empty-state">Carregando…</p>
      ) : perfis.length === 0 ? (
        <p className="empty-state">Nenhum perfil cadastrado nesta escola.</p>
      ) : (
        <div className="perfis-grid">
          {perfis.map((p) => (
            <article className="card perfil-card" key={p.id}>
              <div className="perfil-card-topo">
                <h2>{p.rotulo}</h2>
                {p.sistema && <span className="badge">padrão</span>}
              </div>
              <p className="perfil-card-desc">{p.descricao}</p>
              <p className="perfil-card-contagem">
                {(p.permissoes || []).length} permissõe{(p.permissoes || []).length === 1 ? "m" : "s"}
              </p>
              {p.nome === "SUPER_ADMIN" ? (
                <p className="perfil-card-travado">
                  <Icon name="Lock" size={14} /> Acesso total, não editável
                </p>
              ) : (
                <button className="btn btn-ghost" onClick={() => abrir(p)}>
                  Editar permissões
                </button>
              )}
            </article>
          ))}
        </div>
      )}

      <Modal
        isOpen={!!editando}
        onClose={() => setEditando(null)}
        title={editando ? `Permissões de ${editando.rotulo}` : ""}
        size="lg"
      >
        {aviso && <div className="alert alert-error">{aviso}</div>}
        <p className="form-hint">
          Marque o que a função <strong>{editando?.rotulo}</strong> pode fazer em cada
          área do sistema.
        </p>

        <div className="perm-areas">
          {areasOrdenadas.map(([area, itens]) => {
            const todas = itens.every((i) => marcadas.has(i.chave));
            return (
              <section className="perm-area" key={area}>
                <header className="perm-area-topo">
                  <h3>{AREA_ROTULO[area] || area}</h3>
                  <button
                    type="button"
                    className="btn-link"
                    onClick={() => alternarArea(itens, !todas)}
                  >
                    {todas ? "Desmarcar todas" : "Marcar todas"}
                  </button>
                </header>
                <div className="perm-itens">
                  {itens.map((i) => (
                    <label className="perm-item" key={i.chave}>
                      <input
                        type="checkbox"
                        checked={marcadas.has(i.chave)}
                        onChange={() => alternar(i.chave)}
                      />
                      <span>{i.descricao}</span>
                    </label>
                  ))}
                </div>
              </section>
            );
          })}
        </div>

        <footer className="modal-actions">
          <button className="btn btn-ghost" onClick={() => setEditando(null)} disabled={salvando}>
            Cancelar
          </button>
          <button className="btn btn-brand" onClick={salvar} disabled={salvando}>
            {salvando ? "Salvando…" : `Salvar ${marcadas.size} permissões`}
          </button>
        </footer>
      </Modal>
    </div>
  );
}
