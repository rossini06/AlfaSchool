import { Icon } from "../Icon";

/**
 * Linhas de estado de uma tabela: carregando (skeleton), erro e vazio.
 * Devolve `null` quando há dados — a página renderiza as linhas reais.
 */
export function LinhasEstado({
  colSpan,
  carregando,
  erro,
  vazio,
  linhasSkeleton = 6,
  icone = "List",
  tituloVazio = "Nenhum registro encontrado",
  textoVazio = "Ajuste os filtros ou cadastre o primeiro registro.",
  onTentarNovamente,
}) {
  if (carregando) {
    return Array.from({ length: linhasSkeleton }).map((_, i) => (
      <tr key={`sk-${i}`}>
        {Array.from({ length: colSpan }).map((_, j) => (
          <td key={j}>
            <div className="skeleton skeleton-text" />
          </td>
        ))}
      </tr>
    ));
  }

  if (erro) {
    return (
      <tr>
        <td colSpan={colSpan}>
          <div className="empty-state">
            <div className="empty-state-icon">
              <Icon name="AlertCircle" size={28} />
            </div>
            <h3>Não foi possível carregar</h3>
            <p>{erro}</p>
            {onTentarNovamente && (
              <button className="btn btn-secondary btn-sm mt-3" onClick={onTentarNovamente}>
                <Icon name="RefreshCw" size={13} /> Tentar novamente
              </button>
            )}
          </div>
        </td>
      </tr>
    );
  }

  if (vazio) {
    return (
      <tr>
        <td colSpan={colSpan}>
          <div className="empty-state">
            <div className="empty-state-icon">
              <Icon name={icone} size={28} />
            </div>
            <h3>{tituloVazio}</h3>
            <p>{textoVazio}</p>
          </div>
        </td>
      </tr>
    );
  }

  return null;
}

/** Versão fora de tabela (cards, painéis). */
export function BlocoEstado({
  carregando,
  erro,
  vazio,
  icone = "List",
  tituloVazio = "Nada por aqui",
  textoVazio = "",
  onTentarNovamente,
}) {
  if (carregando) {
    return (
      <div className="ac-bloco-skeleton">
        {Array.from({ length: 3 }).map((_, i) => (
          <div key={i} className="skeleton skeleton-row" />
        ))}
      </div>
    );
  }
  if (erro) {
    return (
      <div className="empty-state">
        <div className="empty-state-icon">
          <Icon name="AlertCircle" size={28} />
        </div>
        <h3>Não foi possível carregar</h3>
        <p>{erro}</p>
        {onTentarNovamente && (
          <button className="btn btn-secondary btn-sm mt-3" onClick={onTentarNovamente}>
            <Icon name="RefreshCw" size={13} /> Tentar novamente
          </button>
        )}
      </div>
    );
  }
  if (vazio) {
    return (
      <div className="empty-state">
        <div className="empty-state-icon">
          <Icon name={icone} size={28} />
        </div>
        <h3>{tituloVazio}</h3>
        {textoVazio && <p>{textoVazio}</p>}
      </div>
    );
  }
  return null;
}
