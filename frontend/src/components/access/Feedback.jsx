import { Icon } from "../Icon";

const ICONES = {
  info: "Info",
  sucesso: "CheckCircle",
  erro: "XCircle",
  alerta: "AlertCircle",
};

/**
 * Caixa de aviso fixa na tela (explicação de regra, alerta de bloqueio...).
 * Substitui o alert() nativo em tudo que é informativo.
 */
export function Aviso({ tipo = "info", titulo, children, icone }) {
  return (
    <div className={`ac-aviso ac-aviso-${tipo}`}>
      <Icon name={icone || ICONES[tipo] || "Info"} size={16} className="ac-aviso-icone" />
      <div>
        {titulo && <strong className="ac-aviso-titulo">{titulo}</strong>}
        <div className="ac-aviso-texto">{children}</div>
      </div>
    </div>
  );
}

/**
 * Mensagem de resultado de uma ação (salvou, falhou, endpoint indisponível).
 * `mensagem` vazia não renderiza nada.
 */
export function Feedback({ tipo = "info", mensagem, onFechar }) {
  if (!mensagem) return null;
  return (
    <div className={`ac-aviso ac-aviso-${tipo} ac-aviso-dismiss`} role="status">
      <Icon name={ICONES[tipo] || "Info"} size={16} className="ac-aviso-icone" />
      <div className="ac-aviso-texto">{mensagem}</div>
      {onFechar && (
        <button type="button" className="btn btn-ghost btn-xs ac-aviso-x" onClick={onFechar} aria-label="Fechar aviso">
          <Icon name="X" size={13} />
        </button>
      )}
    </div>
  );
}
