import { useCallback, useEffect, useMemo, useState } from "react";
import { api } from "../services/api";
import { Modal } from "../components/Modal";
import { Icon } from "../components/Icon";
import { Aviso } from "../components/access/Feedback";

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

/** Icone e cor por perfil padrao. Perfil criado pela escola cai no generico. */
const PERFIL_VISUAL = {
  SUPER_ADMIN: { icone: "Shield", tom: "brand" },
  DIRETOR: { icone: "Award", tom: "brand" },
  COORDENACAO: { icone: "UserCheck", tom: "info" },
  SECRETARIA: { icone: "ClipboardList", tom: "info" },
  PROFESSOR: { icone: "GraduationCap", tom: "success" },
  PORTARIA: { icone: "DoorOpen", tom: "warning" },
  FINANCEIRO: { icone: "DollarSign", tom: "success" },
  RESPONSAVEL: { icone: "Users", tom: "info" },
};

/** O backend devolve o nome tecnico quando o perfil nao tem rotulo gravado. */
const ROTULO_PADRAO = { SUPER_ADMIN: "Super Admin" };

function rotuloDe(perfil) {
  if (perfil.rotulo && perfil.rotulo !== perfil.nome) return perfil.rotulo;
  return ROTULO_PADRAO[perfil.nome] || perfil.rotulo || perfil.nome;
}

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
  const totalCatalogo = useMemo(
    () => areasOrdenadas.reduce((n, [, itens]) => n + itens.length, 0),
    [areasOrdenadas]
  );

  return (
    <div className="page">
      <header className="page-header">
        <div>
          <h1 className="page-title">Perfis e Permissões</h1>
          <p className="page-subtitle">
            Marque o que cada função pode ver e fazer. As mudanças valem a partir do
            próximo login de quem tem o perfil.
          </p>
        </div>
        {totalCatalogo > 0 && (
          <span className="badge badge-secondary">
            {perfis.length} perfis · {totalCatalogo} permissões no catálogo
          </span>
        )}
      </header>

      {erro && <Aviso tipo="erro">{erro}</Aviso>}

      {carregando ? (
        <p className="empty-state">Carregando…</p>
      ) : perfis.length === 0 ? (
        <p className="empty-state">Nenhum perfil cadastrado nesta escola.</p>
      ) : (
        <div className="perfis-grid">
          {perfis.map((p) => {
            const total = p.nome === "SUPER_ADMIN" ? totalCatalogo : (p.permissoes || []).length;
            const pct = totalCatalogo ? Math.round((total / totalCatalogo) * 100) : 0;
            const visual = PERFIL_VISUAL[p.nome] || { icone: "UserCog", tom: "brand" };
            const travado = p.nome === "SUPER_ADMIN";
            return (
              <article className="card perfil-card" key={p.id}>
                <div className="perfil-card-topo">
                  <span className={`kpi-icon ${visual.tom}`}>
                    <Icon name={visual.icone} size={18} />
                  </span>
                  <div className="perfil-card-titulo">
                    <h2>{rotuloDe(p)}</h2>
                    {p.sistema && <span className="badge badge-secondary">padrão</span>}
                  </div>
                </div>
                <p className="perfil-card-desc">{p.descricao}</p>

                {/* Medidor: "23 de 46" diz mais que "23 permissões" solto —
                    mostra o quanto do sistema a função enxerga. */}
                <div className="perfil-card-medidor" aria-hidden="true">
                  <span style={{ width: `${pct}%` }} />
                </div>
                <p className="perfil-card-contagem">
                  {travado ? (
                    <>Acesso total</>
                  ) : (
                    <>
                      <strong>{total}</strong> de {totalCatalogo} permissões
                    </>
                  )}
                </p>

                <div className="perfil-card-rodape">
                  {travado ? (
                    <span className="perfil-card-travado">
                      <Icon name="Lock" size={13} /> Não editável
                    </span>
                  ) : (
                    <button className="btn btn-ghost btn-sm" onClick={() => abrir(p)}>
                      <Icon name="Edit3" size={14} /> Editar permissões
                    </button>
                  )}
                </div>
              </article>
            );
          })}
        </div>
      )}

      <Modal
        isOpen={!!editando}
        onClose={() => setEditando(null)}
        title={editando ? `Permissões de ${editando.rotulo}` : ""}
        size="lg"
        footer={
          <>
            <span className="perm-resumo">
              <strong>{marcadas.size}</strong> de {totalCatalogo} marcadas
            </span>
            <button className="btn btn-ghost" onClick={() => setEditando(null)} disabled={salvando}>
              Cancelar
            </button>
            <button className="btn btn-brand" onClick={salvar} disabled={salvando}>
              <Icon name="Save" size={14} />
              {salvando ? "Salvando…" : "Salvar"}
            </button>
          </>
        }
      >
        {aviso && <Aviso tipo="erro">{aviso}</Aviso>}
        <p className="form-hint perm-hint">
          Marque o que a função <strong>{editando?.rotulo}</strong> pode fazer em cada
          área do sistema. Vale a partir do próximo login.
        </p>

        <div className="perm-areas">
          {areasOrdenadas.map(([area, itens]) => {
            const marcadasAqui = itens.filter((i) => marcadas.has(i.chave)).length;
            const todas = marcadasAqui === itens.length;
            return (
              <section
                className={`perm-area ${todas ? "completa" : marcadasAqui ? "parcial" : ""}`}
                key={area}
              >
                <header className="perm-area-topo">
                  <h3>
                    {AREA_ROTULO[area] || area}
                    <span className="perm-area-contagem">
                      {marcadasAqui}/{itens.length}
                    </span>
                  </h3>
                  <button
                    type="button"
                    className="btn-link"
                    onClick={() => alternarArea(itens, !todas)}
                  >
                    {todas ? "Limpar" : "Marcar todas"}
                  </button>
                </header>
                <div className="perm-itens">
                  {itens.map((i) => (
                    <label
                      className={`perm-item ${marcadas.has(i.chave) ? "marcada" : ""}`}
                      key={i.chave}
                    >
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
      </Modal>
    </div>
  );
}
