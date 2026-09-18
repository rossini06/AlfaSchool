import { Routes, Route, Navigate } from "react-router-dom";
import { useAuth } from "./contexts/AuthContext";
import { AdminLayout } from "./layouts/AdminLayout";
import { LoginPage } from "./pages/LoginPage";
import { DashboardPage } from "./pages/DashboardPage";
import { RedesEnsinoPage } from "./pages/RedesEnsinoPage";
import { EscolasPage } from "./pages/EscolasPage";
import { SaasAdminPage } from "./pages/SaasAdminPage";
import { CursosPage } from "./pages/CursosPage";
import { DisciplinasPage } from "./pages/DisciplinasPage";
import { TurmasPage } from "./pages/TurmasPage";
import { ProfessoresPage } from "./pages/ProfessoresPage";
import { AlunosPage } from "./pages/AlunosPage";
import { MatriculasPage } from "./pages/MatriculasPage";
import { DispositivosPage } from "./pages/DispositivosPage";
import { FrequenciaPage } from "./pages/FrequenciaPage";
import { ConteudoMinistradoPage } from "./pages/ConteudoMinistradoPage";
import { NotasPage } from "./pages/NotasPage";
import { BoletimPage } from "./pages/BoletimPage";
import { FinanceiroPage } from "./pages/FinanceiroPage";
import { UsuariosPage } from "./pages/UsuariosPage";
import { PerfisPermissoesPage } from "./pages/PerfisPermissoesPage";
import { AuditoriaPage } from "./pages/AuditoriaPage";
import { ResponsaveisPage } from "./pages/ResponsaveisPage";
import { AvaliacaoPage } from "./pages/AvaliacaoPage";
import { CoordenacaoPage } from "./pages/access/CoordenacaoPage";
import { PainelSalaPage } from "./pages/access/PainelSalaPage";
// ── Controle de Acesso — cadastros, permanência e relatórios (fatia H) ──
import { PortariasPage } from "./pages/access/PortariasPage";
import { ZonasPage } from "./pages/access/ZonasPage";
import { SalasPage } from "./pages/access/SalasPage";
import { TurmaSalasPage } from "./pages/access/TurmaSalasPage";
import { JornadasPage } from "./pages/access/JornadasPage";
import { AlunoJornadasPage } from "./pages/access/AlunoJornadasPage";
import { CalendarioPage } from "./pages/access/CalendarioPage";
import { PessoasAutorizadasPage } from "./pages/access/PessoasAutorizadasPage";
import { AutorizacoesPage } from "./pages/access/AutorizacoesPage";
import { RestricoesPage } from "./pages/access/RestricoesPage";
import { EquipamentosAccessPage } from "./pages/access/EquipamentosAccessPage";
import { PaineisPage } from "./pages/access/PaineisPage";
import { PermanenciaPage } from "./pages/access/PermanenciaPage";
import { PresentesAgoraPage } from "./pages/access/PresentesAgoraPage";
import { RelatoriosAccessPage } from "./pages/access/RelatoriosAccessPage";
import { OcorrenciasPage } from "./pages/access/OcorrenciasPage";
// ── Portal da Família (layout próprio, sem sidebar administrativa) ──
import { PortalLayout } from "./layouts/PortalLayout";
import { PortalHomePage } from "./pages/portal/PortalHomePage";
import { PortalHistoricoPage } from "./pages/portal/PortalHistoricoPage";
import { PortalAutorizacoesPage } from "./pages/portal/PortalAutorizacoesPage";
import { PortalNotificacoesPage } from "./pages/portal/PortalNotificacoesPage";

function ProtectedRoute({ children, requiredRoles }) {
  const { isAuthenticated, user } = useAuth();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  if (requiredRoles && requiredRoles.length > 0) {
    const hasRole = requiredRoles.some((r) => user?.roles?.includes(r));
    if (!hasRole) {
      return <Navigate to="/" replace />;
    }
  }

  return children;
}

/**
 * O responsável não é funcionário: o lugar dele é o Portal da Família.
 *
 * Sem este desvio ele caía no layout administrativo e via um Dashboard
 * vazio, porque a única permissão dele é PORTAL_ACESSAR — parecia sistema
 * quebrado, quando na verdade era a tela errada.
 */
function RotaPorPerfil({ children }) {
  const { user } = useAuth();
  const roles = user?.roles ?? [];
  const soResponsavel = roles.length > 0 && roles.every((r) => r === "RESPONSAVEL");

  if (soResponsavel) {
    return <Navigate to="/portal" replace />;
  }
  return children;
}

export default function App() {
  const { isAuthenticated } = useAuth();

  return (
    <Routes>
      {/* Public */}
      <Route
        path="/login"
        element={isAuthenticated ? <Navigate to="/" replace /> : <LoginPage />}
      />

      {/* Painel de sala (Smart TV): fora do ProtectedRoute e fora do
          AdminLayout. A TV não faz login — ela se identifica com o token do
          dispositivo em `?token=`. */}
      <Route path="/painel/:slug" element={<PainelSalaPage />} />

      {/* Protected — all under AdminLayout */}
      <Route
        path="/"
        element={
          <ProtectedRoute>
            <RotaPorPerfil>
              <AdminLayout />
            </RotaPorPerfil>
          </ProtectedRoute>
        }
      >
        <Route index element={<DashboardPage />} />
        {/* Acadêmico */}
        <Route path="cursos" element={<CursosPage />} />
        <Route path="disciplinas" element={<DisciplinasPage />} />
        <Route path="turmas" element={<TurmasPage />} />
        <Route path="professores" element={<ProfessoresPage />} />
        <Route path="alunos" element={<AlunosPage />} />
        <Route path="responsaveis" element={<ResponsaveisPage />} />
        <Route path="matriculas" element={<MatriculasPage />} />
        {/* Diário de Classe */}
        <Route path="frequencia" element={<FrequenciaPage />} />
        <Route
          path="conteudo-ministrado"
          element={<ConteudoMinistradoPage />}
        />
        <Route path="avaliacoes" element={<AvaliacaoPage />} />
        <Route path="notas" element={<NotasPage />} />
        <Route path="boletim" element={<BoletimPage />} />
        {/* Financeiro */}
        <Route path="financeiro" element={<FinanceiroPage />} />
        {/* Escola */}
        <Route path="dispositivos" element={<DispositivosPage />} />
        {/* Controle de acesso */}
        <Route path="access/coordenacao" element={<CoordenacaoPage />} />
        {/* Controle de Acesso — cadastros, permanência e relatórios (fatia H) */}
        <Route path="access/portarias" element={<PortariasPage />} />
        <Route path="access/zonas" element={<ZonasPage />} />
        <Route path="access/salas" element={<SalasPage />} />
        <Route path="access/turma-salas" element={<TurmaSalasPage />} />
        <Route path="access/jornadas" element={<JornadasPage />} />
        <Route path="access/aluno-jornadas" element={<AlunoJornadasPage />} />
        <Route path="access/calendario" element={<CalendarioPage />} />
        <Route
          path="access/pessoas-autorizadas"
          element={<PessoasAutorizadasPage />}
        />
        <Route path="access/autorizacoes" element={<AutorizacoesPage />} />
        <Route path="access/restricoes" element={<RestricoesPage />} />
        <Route
          path="access/equipamentos"
          element={<EquipamentosAccessPage />}
        />
        <Route path="access/paineis" element={<PaineisPage />} />
        <Route path="access/permanencia" element={<PermanenciaPage />} />
        <Route path="access/presentes-agora" element={<PresentesAgoraPage />} />
        <Route path="access/relatorios" element={<RelatoriosAccessPage />} />
        <Route path="access/ocorrencias" element={<OcorrenciasPage />} />

        {/* Sistema */}
        <Route path="usuarios" element={<UsuariosPage />} />
        <Route path="perfis" element={<PerfisPermissoesPage />} />
        <Route path="auditoria" element={<AuditoriaPage />} />

        {/* SUPER_ADMIN only */}
        <Route
          path="redes"
          element={
            <ProtectedRoute requiredRoles={["SUPER_ADMIN"]}>
              <RedesEnsinoPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="escolas"
          element={
            <ProtectedRoute requiredRoles={["SUPER_ADMIN"]}>
              <EscolasPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="saas"
          element={
            <ProtectedRoute requiredRoles={["SUPER_ADMIN"]}>
              <SaasAdminPage />
            </ProtectedRoute>
          }
        />
      </Route>

      {/* Portal da Família — layout próprio, sem sidebar administrativa (fatia H) */}
      <Route
        path="/portal"
        element={
          <ProtectedRoute>
            <PortalLayout />
          </ProtectedRoute>
        }
      >
        <Route index element={<PortalHomePage />} />
        <Route path="historico" element={<PortalHistoricoPage />} />
        <Route path="autorizacoes" element={<PortalAutorizacoesPage />} />
        <Route path="notificacoes" element={<PortalNotificacoesPage />} />
      </Route>

      {/* Catch all */}
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
