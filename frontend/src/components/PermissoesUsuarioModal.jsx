import { useCallback, useEffect, useMemo, useState } from "react";
import { api } from "../services/api";
import { Modal } from "./Modal";

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

/**
 * Permissões de UMA pessoa, além do que o perfil dela dá.
 *
 * O que vem do perfil aparece marcado e DESABILITADO, com a nota "já vem
 * do perfil". Um checkbox que não obedece é pior do que ausente: deixar
 * desmarcável daria a impressão de que dá para tirar, e não dá — só
 * somamos. Para tirar acesso, troca-se o perfil da pessoa.
 */
export function PermissoesUsuarioModal({ usuario, onClose }) {
  const [dados, setDados] = useState(null);
  const [areas, setAreas] = useState({});
  const [extras, setExtras] = useState(new Set());
  const [motivo, setMotivo] = useState("");
  const [carregando, setCarregando] = useState(true);
  const [salvando, setSalvando] = useState(false);
  const [aviso, setAviso] = useState("");

  const carregar = useCallback(async () => {
    if (!usuario) return;
    setCarregando(true);
    setAviso("");
    try {
      const [doUsuario, catalogo] = await Promise.all([
        api.get(`/perfis/usuarios/${usuario.id}/permissoes`),
        api.get("/perfis/catalogo"),
      ]);
      setDados(doUsuario);
      setExtras(new Set(doUsuario?.extras || []));
      setAreas(catalogo?.areas || {});
    } catch (e) {
      setAviso(e.message || "Não foi possível carregar as permissões.");
    } finally {
      setCarregando(false);
    }
  }, [usuario]);

  useEffect(() => {
    carregar();
  }, [carregar]);

  const doPerfil = useMemo(() => new Set(dados?.doPerfil || []), [dados]);

  const alternar = (chave) => {
    if (doPerfil.has(chave)) return; // já vem do perfil: não há o que alternar
    setExtras((atual) => {
      const proximo = new Set(atual);
      if (proximo.has(chave)) proximo.delete(chave);
      else proximo.add(chave);
      return proximo;
    });
  };

  const salvar = async () => {
    setSalvando(true);
    setAviso("");
    try {
      await api.put(`/perfis/usuarios/${usuario.id}/permissoes`, {
        permissoes: Array.from(extras),
        motivo: motivo.trim() || null,
      });
      onClose(true);
    } catch (e) {
      setAviso(e.message || "Não foi possível salvar.");
    } finally {
      setSalvando(false);
    }
  };

  return (
    <Modal
      isOpen={!!usuario}
      onClose={() => onClose(false)}
      title={`Permissões de ${usuario?.nome || usuario?.name || ""}`}
      size="lg"
    >
      {aviso && <div className="alert alert-error">{aviso}</div>}

      {carregando ? (
        <p className="empty-state">Carregando…</p>
      ) : (
        <>
          <p className="form-hint">
            Perfil: <strong>{(dados?.perfis || []).join(", ") || "nenhum"}</strong>. As
            marcadas em cinza já vêm do perfil — para tirar alguma delas, troque o
            perfil da pessoa. Aqui só é possível <strong>acrescentar</strong>.
          </p>

          <div className="perm-areas">
            {Object.entries(areas).map(([area, itens]) => (
              <section className="perm-area" key={area}>
                <header className="perm-area-topo">
                  <h3>{AREA_ROTULO[area] || area}</h3>
                </header>
                <div className="perm-itens">
                  {itens.map((i) => {
                    const fixa = doPerfil.has(i.chave);
                    return (
                      <label className={`perm-item ${fixa ? "fixa" : ""}`} key={i.chave}>
                        <input
                          type="checkbox"
                          checked={fixa || extras.has(i.chave)}
                          disabled={fixa}
                          onChange={() => alternar(i.chave)}
                        />
                        <span>
                          {i.descricao}
                          {fixa && <span className="perm-item-nota">já vem do perfil</span>}
                        </span>
                      </label>
                    );
                  })}
                </div>
              </section>
            ))}
          </div>

          <div className="field">
            <label className="field-label" htmlFor="motivo-permissao">
              Motivo (fica registrado)
            </label>
            <input
              id="motivo-permissao"
              className="field-input"
              value={motivo}
              onChange={(e) => setMotivo(e.target.value)}
              placeholder="Ex.: substitui a coordenação nas férias"
            />
          </div>
        </>
      )}

      <footer className="modal-actions">
        <button className="btn btn-ghost" onClick={() => onClose(false)} disabled={salvando}>
          Cancelar
        </button>
        <button className="btn btn-primary" onClick={salvar} disabled={salvando || carregando}>
          {salvando ? "Salvando…" : "Salvar permissões"}
        </button>
      </footer>
    </Modal>
  );
}
