import { usePermissao } from "../hooks/usePermissao";

/**
 * Mostra os filhos só se a pessoa tiver a permissão.
 *
 *   <SePermitido perm="ACESSO_RETIRADA_ENTREGAR">
 *     <button>Confirmar entrega</button>
 *   </SePermitido>
 *
 * `perm` aceita string ou array (qualquer uma serve). `fallback` permite
 * explicar a ausência em vez de simplesmente sumir — útil quando o sumiço
 * faria a tela parecer quebrada.
 */
export function SePermitido({ perm, fallback = null, children }) {
  const { pode } = usePermissao();
  if (!perm) return children;
  return pode(perm) ? children : fallback;
}
