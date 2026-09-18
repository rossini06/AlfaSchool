// SOLICITADA é o primeiro estado no backend (domain/access/shared/StatusRetirada).
// Na tela ele aparece como "Aguardando", que é o vocabulário da coordenação.
const STATUS = {
  SOLICITADA: { label: "Aguardando", badge: "badge-warning", tv: { bg: "rgba(240,138,36,0.18)", fg: "#f7b26a" } },
  AGUARDANDO: { label: "Aguardando", badge: "badge-warning", tv: { bg: "rgba(240,138,36,0.18)", fg: "#f7b26a" } },
  PREPARANDO: { label: "Preparando", badge: "badge-info",    tv: { bg: "rgba(41,128,185,0.22)", fg: "#7fc2ec" } },
  PRONTO:     { label: "Pronto",     badge: "badge-success", tv: { bg: "rgba(53,208,127,0.18)", fg: "#6ce0a6" } },
  ENTREGUE:   { label: "Entregue",   badge: "badge-brand",   tv: { bg: "rgba(22,150,163,0.20)", fg: "#66d3dd" } },
  CANCELADA:  { label: "Cancelada",  badge: "badge-danger",  tv: { bg: "rgba(255,91,82,0.18)",  fg: "#ff9a94" } },
  NEGADA:     { label: "Negada",     badge: "badge-danger",  tv: { bg: "rgba(255,91,82,0.18)",  fg: "#ff9a94" } },
  EXPIRADA:   { label: "Expirada",   badge: "badge-secondary", tv: { bg: "rgba(155,180,192,0.16)", fg: "#c3d4dc" } },
};

/**
 * Etiqueta colorida do status de uma retirada.
 * Aceita qualquer grafia vinda do backend (maiúscula, minúscula, com acento).
 * Status desconhecido vira uma etiqueta neutra em vez de sumir da tela.
 */
export function StatusRetiradaBadge({ status, tv = false }) {
  const chave = normalizar(status);
  const def = STATUS[chave];
  const label = def ? def.label : (status ? String(status) : "—");

  if (tv) {
    const cores = def ? def.tv : { bg: "rgba(155,180,192,0.16)", fg: "#c3d4dc" };
    return (
      <span className="painel-tv-status" style={{ background: cores.bg, color: cores.fg }}>
        {label}
      </span>
    );
  }

  return <span className={`badge ${def ? def.badge : "badge-secondary"}`}>{label}</span>;
}

function normalizar(status) {
  if (!status) return "";
  return String(status)
    .normalize("NFD")
    .replace(/[̀-ͯ]/g, "")
    .toUpperCase()
    .replace(/[^A-Z]/g, "");
}
