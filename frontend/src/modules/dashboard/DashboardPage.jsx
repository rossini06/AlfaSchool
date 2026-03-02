import { useEffect, useState } from 'react'
import { getDashboard } from './dashboardService'
import StatCard from './components/StatCard'
import AlertList from './components/AlertList'
import SystemHealth from './components/SystemHealth'
import './dashboard.css'

function DashboardPage() {
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [dashboard, setDashboard] = useState(null)

  useEffect(() => {
    async function loadDashboard() {
      try {
        const payload = await getDashboard()
        setDashboard(payload.data)
        setError('')
      } catch (requestError) {
        const apiMessage = requestError?.response?.data?.message
        setError(apiMessage || 'Não foi possível carregar a dashboard.')
      } finally {
        setLoading(false)
      }
    }

    loadDashboard()
  }, [])

  if (loading) {
    return (
      <section className="dashboard-shell">
        <div className="dashboard-loading">Carregando dashboard...</div>
      </section>
    )
  }

  if (error) {
    return (
      <section className="dashboard-shell">
        <div className="dashboard-error">{error}</div>
      </section>
    )
  }

  return (
    <section className="dashboard-shell">
      <header className="dashboard-header">
        <h1>Dashboard Institucional</h1>
        <p>Visão consolidada de operação da escola</p>
      </header>

      <div className="dashboard-grid-cards">
        {dashboard?.stats?.map((stat) => (
          <StatCard
            key={stat.key}
            title={stat.title}
            value={stat.value}
            icon={stat.key}
            variant={
              stat.key === 'overduePayments'
                ? 'warning'
                : stat.key === 'accessToday'
                  ? 'info'
                  : stat.key === 'attendanceToday'
                    ? 'success'
                    : 'default'
            }
          />
        ))}
      </div>

      <div className="dashboard-grid-panels">
        <AlertList alerts={dashboard?.alerts || []} />
        <SystemHealth health={dashboard?.systemHealth} />
      </div>
    </section>
  )
}

export default DashboardPage
