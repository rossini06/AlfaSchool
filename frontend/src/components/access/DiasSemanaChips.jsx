import { DIAS_SEMANA } from "../../utils/diasSemana";

/**
 * Seletor de dias da semana em chips (S T Q Q S S D).
 * Valor: array de números no padrão ISO — 1 = segunda ... 7 = domingo.
 */
export function DiasSemanaChips({ value = [], onChange, disabled = false, atalhos = true }) {
  const selecionados = Array.isArray(value) ? value : [];

  const alternar = (dia) => {
    if (disabled) return;
    onChange(
      selecionados.includes(dia)
        ? selecionados.filter((d) => d !== dia)
        : [...selecionados, dia].sort((a, b) => a - b)
    );
  };

  return (
    <div>
      <div className="ac-chips" role="group" aria-label="Dias da semana">
        {DIAS_SEMANA.map((d) => {
          const on = selecionados.includes(d.valor);
          return (
            <button
              key={d.valor}
              type="button"
              disabled={disabled}
              className={`ac-chip ${on ? "on" : ""}`}
              onClick={() => alternar(d.valor)}
              title={d.nome}
              aria-pressed={on}
            >
              {d.curto}
            </button>
          );
        })}
      </div>
      {atalhos && !disabled && (
        <div className="ac-chips-atalhos">
          <button type="button" className="btn btn-ghost btn-xs" onClick={() => onChange([1, 2, 3, 4, 5])}>
            Seg a Sex
          </button>
          <button type="button" className="btn btn-ghost btn-xs" onClick={() => onChange([1, 2, 3, 4, 5, 6, 7])}>
            Todos
          </button>
          <button type="button" className="btn btn-ghost btn-xs" onClick={() => onChange([])}>
            Nenhum
          </button>
        </div>
      )}
    </div>
  );
}
