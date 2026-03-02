import './StatCard.css'

function iconPath(icon) {
  if (icon === 'totalStudents') return 'M4 19h16M4 15h16M9 11h6M7 7h10'
  if (icon === 'totalStaff') return 'M16 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2M12 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8'
  if (icon === 'attendanceToday') return 'M20 6 9 17l-5-5'
  if (icon === 'overduePayments') return 'M12 2v20M17 5H9a4 4 0 0 0 0 8h6a4 4 0 1 1 0 8H6'
  return 'M3 12h4l3 8 4-16 3 8h4'
}

function StatCard({ title, value, icon, variant }) {
  return (
    <article className={`stat-card ${variant}`}>
      <div className="stat-card-icon" aria-hidden="true">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <path d={iconPath(icon)} />
        </svg>
      </div>
      <div>
        <p className="stat-card-title">{title}</p>
        <h3 className="stat-card-value">{value}</h3>
      </div>
    </article>
  )
}

export default StatCard
