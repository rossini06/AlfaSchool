import { STATUS_AUTORIZACAO } from "../../utils/statusAutorizacao";

export function StatusAutorizacaoBadge({ status }) {
  const cfg = STATUS_AUTORIZACAO[status] || { label: status || "—", classe: "badge-secondary" };
  return <span className={`badge ${cfg.classe}`}>{cfg.label}</span>;
}
