import './AlertList.css'

function severityClass(severity) {
  if (severity === 'WARNING') return 'warning'
  if (severity === 'ERROR') return 'danger'
  if (severity === 'SUCCESS') return 'success'
  return 'info'
}

function iconPath(severity) {
  if (severity === 'WARNING') return 'M12 9v4m0 4h.01M10.29 3.86 1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z'
  if (severity === 'SUCCESS') return 'M20 6 9 17l-5-5'
  return 'M12 8h.01M12 12v4m0 8a10 10 0 1 0 0-20 10 10 0 0 0 0 20z'
}

function AlertList({ alerts }) {
  return (
    <section className="alert-list">
      <h2>Alertas</h2>
      <ul>
        {alerts.map((alert) => (
          <li key={`${alert.type}-${alert.message}`} className={`alert-item ${severityClass(alert.severity)}`}>
            <span className="alert-icon" aria-hidden="true">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d={iconPath(alert.severity)} />
              </svg>
            </span>
            <div className="alert-content">
              <p>{alert.message}</p>
              <span className={`alert-badge ${severityClass(alert.severity)}`}>{alert.severity}</span>
            </div>
          </li>
        ))}
      </ul>
    </section>
  )
}

export default AlertList
