/**
 * Switch rotulado e com explicação própria.
 * Usado onde cada permissão é independente e precisa ficar óbvio na tela.
 */
export function SwitchCampo({ checked, onChange, rotulo, descricao, disabled = false, id }) {
  const inputId = id || `sw-${rotulo.replace(/\s+/g, "-").toLowerCase()}`;
  return (
    <label className={`ac-switch-row ${disabled ? "is-disabled" : ""}`} htmlFor={inputId}>
      <span className="ac-switch">
        <input
          id={inputId}
          type="checkbox"
          checked={!!checked}
          disabled={disabled}
          onChange={(e) => onChange(e.target.checked)}
        />
        <span className="ac-switch-track" aria-hidden="true">
          <span className="ac-switch-thumb" />
        </span>
      </span>
      <span className="ac-switch-texto">
        <strong>{rotulo}</strong>
        {descricao && <small>{descricao}</small>}
      </span>
    </label>
  );
}
