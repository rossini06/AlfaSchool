import { Routes, Route, Navigate } from "react-router-dom";
import { useAuth } from "./contexts/AuthContext";
import { AdminLayout } from "./layouts/AdminLayout";
import { LoginPage } from "./pages/LoginPage";
import { DashboardPage } from "./pages/DashboardPage";
import { RedesEnsinoPage } from "./pages/RedesEnsinoPage";
import { EscolasPage } from "./pages/EscolasPage";
import { SaasAdminPage } from "./pages/SaasAdminPage";
import { CursosPage } from "./pages/CursosPage";
import { TurmasPage } from "./pages/TurmasPage";
import { AlunosPage } from "./pages/AlunosPage";
import { MatriculasPage } from "./pages/MatriculasPage";
import { DispositivosPage } from "./pages/DispositivosPage";
import { UsuariosPage } from "./pages/UsuariosPage";
import { AuditoriaPage } from "./pages/AuditoriaPage";

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

export default function App() {
  const { isAuthenticated } = useAuth();

  return (
    <Routes>
      {/* Public */}
      <Route
        path="/login"
        element={isAuthenticated ? <Navigate to="/" replace /> : <LoginPage />}
      />

      {/* Protected — all under AdminLayout */}
      <Route
        path="/"
        element={
          <ProtectedRoute>
            <AdminLayout />
          </ProtectedRoute>
        }
      >
        <Route index element={<DashboardPage />} />
        <Route path="cursos"       element={<CursosPage />} />
        <Route path="turmas"       element={<TurmasPage />} />
        <Route path="alunos"       element={<AlunosPage />} />
        <Route path="matriculas"   element={<MatriculasPage />} />
        <Route path="dispositivos" element={<DispositivosPage />} />
        <Route path="usuarios"     element={<UsuariosPage />} />
        <Route path="auditoria"    element={<AuditoriaPage />} />

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

      {/* Catch all */}
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
