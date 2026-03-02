import './SystemHealth.css'

function statusLabel(value) {
  return value === 'UP' ? 'OK' : 'DOWN'
}

function SystemHealth({ health }) {
  return (
    <section className="system-health">
      <h2>Saúde do Sistema</h2>
      <dl>
        <div>
          <dt>API</dt>
          <dd className={health?.status === 'UP' ? 'ok' : 'down'}>{statusLabel(health?.status)}</dd>
        </div>
        <div>
          <dt>Database</dt>
          <dd className={health?.db === 'UP' ? 'ok' : 'down'}>{statusLabel(health?.db)}</dd>
        </div>
        <div>
          <dt>Environment</dt>
          <dd>{health?.environment || 'DEV'}</dd>
        </div>
        <div>
          <dt>Versão API</dt>
          <dd>{health?.apiVersion || '0.0.1'}</dd>
        </div>
      </dl>
    </section>
  )
}

export default SystemHealth
