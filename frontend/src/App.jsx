import { useState } from 'react'

function App() {
  const [email, setEmail] = useState('superadmin@alfaschool.com')
  const [password, setPassword] = useState('SuperAdmin@2024!@#$')
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
      setSuccess(`Bem-vindo, ${body.nome}! Login realizado com sucesso.`)
    } catch (requestError) {
      setError(requestError.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <main className="container">
      <section className="card">
        <h1>AlfaSchool</h1>
        <p className="subtitle">Acesso da plataforma (Spring + React)</p>

        <form onSubmit={handleSubmit}>
          <label htmlFor="email">E-mail</label>
          <input
            id="email"
            type="email"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            required
          />

          <label htmlFor="password">Senha</label>
          <input
            id="password"
            type="password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            required
          />

          <button type="submit" disabled={loading}>
            {loading ? 'Entrando...' : 'Entrar'}
          </button>
        </form>

        {error && <p className="feedback error">{error}</p>}
        {success && <p className="feedback success">{success}</p>}
      </section>
    </main>
  )
}

export default App
