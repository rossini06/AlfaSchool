import { useCallback, useMemo } from "react";
import { useAuth } from "../contexts/AuthContext";

/**
 * Permissões da pessoa logada, para esconder botão que daria 403.
 *
 * A defesa de verdade é o `@PreAuthorize` do backend — isto aqui evita
 * frustração, não protege nada. Nunca troque uma checagem de servidor por
 * esta: qualquer um edita o `localStorage`.
 *
 *   const { pode } = usePermissao();
 *   {pode("ACESSO_RETIRADA_ENTREGAR") && <button>Confirmar entrega</button>}
 */
export function usePermissao() {
  const { user } = useAuth();

  const permissoes = useMemo(() => user?.permissoes ?? [], [user]);
  const roles = useMemo(() => user?.roles ?? [], [user]);

  /** Tendo QUALQUER uma das informadas, devolve true. */
  const pode = useCallback(
    (...chaves) => chaves.flat().some((c) => permissoes.includes(c)),
    [permissoes]
  );

  /** Exige TODAS. Use quando a ação combina duas coisas de fato. */
  const podeTudo = useCallback(
    (...chaves) => chaves.flat().every((c) => permissoes.includes(c)),
    [permissoes]
  );

  const temPerfil = useCallback((...nomes) => nomes.flat().some((n) => roles.includes(n)), [roles]);

  return { pode, podeTudo, temPerfil, permissoes, roles };
}
