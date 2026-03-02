import { useState } from 'react'

function App() {
  const schoolName = 'AlfaSchool'
  const [email, setEmail] = useState('superadmin@alfaschool.com')
  const [password, setPassword] = useState('SuperAdmin@2024!@#$')
  const [remember, setRemember] = useState(true)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')

  const handleSubmit = async (event) => {
    event.preventDefault()
    setLoading(true)
    setError('')
    setSuccess('')

    try {
      const response = await fetch('/api/auth/login', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ email, password }),
      })

      const body = await response.json()

      if (!response.ok) {
        throw new Error(body.mensagem ?? 'Falha ao realizar login')
      }

      localStorage.setItem('authToken', body.token)
      localStorage.setItem('remember', remember ? '1' : '0')
      setSuccess(`Bem-vindo, ${body.nome}! Login realizado com sucesso.`)
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <main className="login-page">
      <section className="hero-panel fade-in delay-1">
        <div className="hero-top">
          <div className="brand-badge">{schoolName}</div>
          <span className="hero-chip">Plataforma SaaS</span>
          <h1>
            Gestão Escolar <span>de Alto Nível</span>
          </h1>
          <p>
            Controle total, <strong>segurança avançada</strong> e organização em uma única plataforma.
          </p>
          <p className="hero-pill">Sistema com segurança avançada e controle por perfil</p>
        </div>

        <div className="feature-grid">
          {[
            ['Gestão de Alunos', 'Organize matrículas e histórico em segundos.'],
            ['Controle de Turmas', 'Estruture classes e cronogramas com facilidade.'],
            ['Lançamento de Notas', 'Publique avaliações com fluxo simples e rápido.'],
            ['Frequência Digital', 'Controle presença com rapidez e precisão.'],
            ['Financeiro Integrado', 'Visualize receitas e inadimplência com clareza.'],
            ['Permissões por Perfil', 'Garanta acessos seguros para cada função escolar.'],
          ].map(([title, description]) => (
            <article key={title} className="feature-card">
              <h3>{title}</h3>
              <p>{description}</p>
            </article>
          ))}
        </div>

        <div className="hero-bottom">
          <p className="hero-access">Acesso restrito por perfil: Admin, Gestor, Professor, Secretaria e Aluno.</p>
          <p className="hero-footer">© {new Date().getFullYear()} AlfaSchool. Todos os direitos reservados.</p>
        </div>
      </section>

      <section className="form-panel fade-in delay-2">
        <div className="login-card">
          <p className="school-label">{schoolName}</p>
          <h2>Acesse sua conta</h2>
          <p className="subtitle">Entre com suas credenciais para continuar</p>

          <form onSubmit={handleSubmit} className="form-grid">
            <div>
              <label htmlFor="email">E-mail</label>
              <input
                id="email"
                type="email"
                value={email}
                onChange={(event) => setEmail(event.target.value)}
                placeholder="seuemail@escola.com"
                required
              />
            </div>

            <div>
              <label htmlFor="password">Senha</label>
              <input
                id="password"
                type="password"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                placeholder="••••••••••••"
                required
              />
            </div>

            <div className="form-actions">
              <label className="remember-row" htmlFor="remember_me">
                <input
                  id="remember_me"
                  type="checkbox"
                  checked={remember}
                  onChange={(event) => setRemember(event.target.checked)}
                />
                <span>Lembrar-me</span>
              </label>

              <a href="#" onClick={(event) => event.preventDefault()}>
                Esqueci minha senha
              </a>
            </div>

            <button type="submit" disabled={loading}>
              {loading ? 'Entrando...' : 'Entrar'}
            </button>
          </form>

          {error && (
            <p className={`feedback ${error.includes('bloqueada') ? 'error-lockout' : 'error'}`}>
              {error}
            </p>
          )}
          {success && <p className="feedback success">{success}</p>}
        </div>
      </section>
    </main>
  )
}

export default App
