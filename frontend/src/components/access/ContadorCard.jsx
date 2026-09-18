import { Icon } from "../Icon";

/**
 * Contador grande da Central de Coordenação: número em destaque,
 * rótulo pequeno embaixo.
 *
 * props:
 *   label    — rótulo curto
 *   valor    — número (null/undefined vira "—")
 *   icone    — nome de ícone do componente Icon
 *   tom      — "" | "warning" | "danger" | "success"
 *   loading  — mostra esqueleto no lugar do número
 */
export function ContadorCard({ label, valor, icone, tom = "", loading = false, hint }) {
  return (
    <div className={`ac-contador ${tom ? `tom-${tom}` : ""}`}>
      <div className="ac-contador-topo">
        {loading ? (
          <div className="skeleton" style={{ width: 72, height: 44, borderRadius: 8 }} />
        ) : (
          <div className="ac-contador-valor">{valor ?? "—"}</div>
        )}
        {icone && (
          <div className="ac-contador-icone">
            <Icon name={icone} size={17} />
          </div>
        )}
      </div>
      <div className="ac-contador-label">{label}</div>
      {hint && <div className="text-xs text-muted mt-1">{hint}</div>}
    </div>
  );
}
