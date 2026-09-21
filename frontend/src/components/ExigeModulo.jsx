import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "../contexts/AuthContext";
import { Icon } from "./Icon";

const NOMES = {
  ACCESS: "Controle de Acesso",
  PORTAL: "Portal da Família",
  NOTIFICACOES: "Notificações",
  PEDAGOGICO: "Pedagógico",
  FINANCEIRO: "Financeiro",
};

/**
 * Rota-mãe de um bloco contratável.
 *
 *   <Route element={<ExigeModulo codigo="ACCESS" />}>
 *     <Route path="access/salas" element={<SalasPage />} />
 *   </Route>
 *
 * O menu já esconde o que a escola não contratou; isto cobre quem chega
 * pela URL (favorito, link colado, histórico). Sem isto a tela abria vazia
 * e disparava um 403 por requisição — parecia sistema quebrado, quando era
 * só módulo não contratado. A defesa de verdade continua sendo o
 * `@moduloGuard` do backend; aqui é explicação, não proteção.
 */
export function ExigeModulo({ codigo }) {
  const { user } = useAuth();
  const modulos = user?.modulos ?? [];

  if (modulos.includes(codigo)) {
    return <Outlet />;
  }

  // Responsável só tem o Portal; mandá-lo para uma tela de "módulo não
  // contratado" do administrativo não ajuda ninguém.
  const roles = user?.roles ?? [];
  if (roles.length > 0 && roles.every((r) => r === "RESPONSAVEL")) {
    return <Navigate to="/portal" replace />;
  }

  return (
    <div className="modulo-ausente" role="status">
      <div className="modulo-ausente-icone">
        <Icon name="Lock" size={26} />
      </div>
      <h2>{NOMES[codigo] ?? codigo} não está ativo nesta escola</h2>
      <p>
        Este bloco é contratado separadamente. Quando a escola ativar o módulo,
        as telas aparecem no menu sem precisar de nenhuma configuração.
      </p>
      <p className="modulo-ausente-dica">
        Fale com a administração da escola ou com a equipe Alfa para contratar.
      </p>
    </div>
  );
}
