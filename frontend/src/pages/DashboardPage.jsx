import { useState, useEffect } from "react";
import { useAuth } from "../contexts/AuthContext";
import { api } from "../services/api";
import { Icon } from "../components/Icon";

function KpiSkeleton() {
  return (
    <div className="kpi-grid">
      {Array.from({ length: 4 }).map((_, i) => (
        <div key={i} className="skeleton skeleton-kpi" />
      ))}
    </div>
  );
}

function ActivitySkeleton() {
  return (
    <div>
      {Array.from({ length: 5 }).map((_, i) => (
        <div key={i} className="skeleton skeleton-row" style={{ marginBottom: 1 }} />
      ))}
    </div>
  );
}

const KPI_META = [
  { key: "totalAlunos",      label: "Total de Alunos",    icon: "GraduationCap", color: "brand"   },
  { key: "totalTurmas",      label: "Turmas Ativas",      icon: "Users",         color: "success" },
  { key: "matriculasAtivas", label: "Matrículas Ativas",  icon: "ClipboardList", color: "info"    },
  { key: "totalCursos",      label: "Cursos Cadastrados", icon: "BookOpen",      color: "warning" },
];

export function DashboardPage() {
  const { user } = useAuth();
  const [kpis, setKpis] = useState(null);
  const [stats, setStats] = useState([]);
  const [activity, setActivity] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    const load = async () => {
      setLoading(true);
      try {
        const data = await api.get("/dashboard");
        setKpis(data?.schoolKpis || {});
        setStats(data?.stats || []);
        setActivity(data?.recentMatriculas || []);
      } catch (err) {
        // Fallback to mock data if API not available
        setKpis({
          totalAlunos: "—",
          turmasAtivas: "—",
          matriculasAtivas: "—",
          totalCursos: "—",
        });
        setActivity([]);
        setError("");
      } finally {
        setLoading(false);
      }
    };
    load();
  }, []);

  const getGreeting = () => {
    const h = new Date().getHours();
    if (h < 12) return "Bom dia";
    if (h < 18) return "Boa tarde";
    return "Boa noite";
  };

  return (
    <div className="page">
      {/* Welcome */}
      <div className="page-header">
        <div>
          <h1 className="page-title">
            {getGreeting()}, {user?.nome?.split(" ")[0] || "Usuário"}!
          </h1>
          <p className="page-subtitle">
            Aqui está um resumo do sistema — {new Date().toLocaleDateString("pt-BR", { weekday: "long", day: "numeric", month: "long", year: "numeric" })}
          </p>
        </div>
        <div style={{ display: "flex", gap: 8 }}>
          <button
            className="btn btn-secondary"
            onClick={() => window.location.reload()}
          >
            <Icon name="RefreshCw" size={14} />
            Atualizar
          </button>
        </div>
      </div>

      {error && (
        <div className="login-error">
          <Icon name="AlertCircle" size={14} /> {error}
        </div>
      )}

      {/* KPIs — principais */}
      <div className="card">
        <div className="card-header">
          <span className="card-title">Visão Geral</span>
        </div>
        <div className="card-body">
          {loading ? (
            <KpiSkeleton />
          ) : (
            <div className="kpi-grid">
              {KPI_META.map((m) => (
                <div className="kpi-card" key={m.key}>
                  <div className="kpi-card-header">
                    <span className="kpi-label">{m.label}</span>
                    <div className={`kpi-icon ${m.color}`}>
                      <Icon name={m.icon} size={18} />
                    </div>
                  </div>
                  <div className="kpi-value">{kpis?.[m.key] ?? "—"}</div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Stats operacionais */}
      {!loading && stats.length > 0 && (
        <div className="card">
          <div className="card-header">
            <span className="card-title">Operacional do Dia</span>
          </div>
          <div className="card-body">
            <div className="kpi-grid" style={{ gridTemplateColumns: "repeat(auto-fill, minmax(180px, 1fr))" }}>
              {stats.filter(s => s.key !== "accessToday").map((s) => {
                const meta = {
                  totalStudents:  { icon: "GraduationCap", color: "brand"   },
                  totalStaff:     { icon: "UserCog",       color: "info"    },
                  attendanceToday:{ icon: "CheckSquare",   color: "success" },
                  overduePayments:{ icon: "AlertCircle",   color: "danger"  },
                }[s.key] || { icon: "BarChart2", color: "secondary" };
                return (
                  <div className="kpi-card" key={s.key}>
                    <div className="kpi-card-header">
                      <span className="kpi-label">{s.title}</span>
                      <div className={`kpi-icon ${meta.color}`}>
                        <Icon name={meta.icon} size={18} />
                      </div>
                    </div>
                    <div className="kpi-value">{s.value}</div>
                  </div>
                );
              })}
            </div>
          </div>
        </div>
      )}

      {/* Recent Activity */}
      <div className="card">
        <div className="card-header">
          <span className="card-title">Últimas Matrículas</span>
          <a href="/matriculas" className="btn btn-ghost btn-sm">
            Ver todas <Icon name="ArrowRight" size={13} />
          </a>
        </div>
        <div className="card-body-flush">
          {loading ? (
            <div style={{ padding: "12px 20px" }}>
              <ActivitySkeleton />
            </div>
          ) : activity.length === 0 ? (
            <div className="empty-state" style={{ padding: "40px 20px" }}>
              <div className="empty-state-icon">
                <Icon name="ClipboardList" size={28} />
              </div>
              <h3>Nenhuma matrícula recente</h3>
              <p>As últimas matrículas realizadas aparecerão aqui.</p>
            </div>
          ) : (
            <div className="activity-list" style={{ padding: "0 20px" }}>
              {activity.map((item, i) => (
                <div className="activity-item" key={item.id || i}>
                  <div className="activity-dot" />
                  <div className="activity-content">
                    <div className="activity-title">
                      {item.aluno_nome || item.alunoNome || "Aluno"}
                    </div>
                    <div className="activity-meta">
                      {item.turma_nome || item.turmaNome || ""}
                      {item.status ? ` · ${item.status}` : ""}
                    </div>
                  </div>
                  <div className="activity-time">
                    {item.data_matricula
                      ? new Date(item.data_matricula + "T00:00:00").toLocaleDateString("pt-BR")
                      : item.dataMatricula
                      ? new Date(item.dataMatricula + "T00:00:00").toLocaleDateString("pt-BR")
                      : ""}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Quick links */}
      <div className="card">
        <div className="card-header">
          <span className="card-title">Acesso Rápido</span>
        </div>
        <div className="card-body">
          <div style={{ display: "flex", gap: 10, flexWrap: "wrap" }}>
            {[
              { label: "Nova Matrícula", path: "/matriculas", icon: "ClipboardList", color: "btn-brand" },
              { label: "Novo Aluno",     path: "/alunos",     icon: "GraduationCap", color: "btn-primary" },
              { label: "Nova Turma",     path: "/turmas",     icon: "Users",         color: "btn-secondary" },
              { label: "Novo Curso",     path: "/cursos",     icon: "BookOpen",      color: "btn-secondary" },
            ].map((link) => (
              <a key={link.path} href={link.path} className={`btn ${link.color}`}>
                <Icon name={link.icon} size={14} />
                {link.label}
              </a>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
