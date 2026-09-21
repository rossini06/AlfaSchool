import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../contexts/AuthContext";
import { useTheme } from "../hooks/useTheme";
import { Icon } from "../components/Icon";
import icone from "/alfaschool-icon.svg";
import wordmark from "/alfaschool-logo.png";

export function LoginPage() {
  const { login } = useAuth();
  const { theme, toggleTheme } = useTheme();
  const navigate = useNavigate();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [tenantId, setTenantId] = useState("");
  const [showTenant, setShowTenant] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [showPass, setShowPass] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setLoading(true);
    try {
      const usuario = await login(email, password, tenantId || undefined);
      // Superadmin é da Alfa: o lugar dele é o painel SaaS, não o Dashboard
      // de uma escola. De lá ele abre o painel da escola quando quiser.
      const destino = usuario?.roles?.includes("SUPER_ADMIN") ? "/saas" : "/";
      navigate(destino, { replace: true });
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-page">
      <div className="login-bg-shapes" />

      {/* Theme toggle */}
      <div className="login-theme-toggle">
        <button
          className="btn btn-secondary"
          onClick={toggleTheme}
          title={theme === "dark" ? "Modo claro" : "Modo escuro"}
        >
          <Icon name={theme === "dark" ? "Sun" : "Moon"} size={15} />
        </button>
      </div>

      <div className="login-card" style={{ position: "relative", zIndex: 1 }}>
        {/*
          Lockup da família Alfa: símbolo + wordmark, lado a lado.

          As duas imagens vêm do disco em vez de serem desenhadas aqui —
          é o mesmo arquivo que vai para o favicon e para qualquer material
          impresso, então a marca não tem duas versões que podem divergir.

          A proporção entre os dois segue a regra escrita em `.login-brand`
          (ver brand/README.md). `width` e `height` explícitos no <img>
          reservam a caixa antes de a imagem chegar: sem isso o card pinta
          e a logo "pula" um frame depois.
        */}
        <div className="login-logo">
          <div className="login-brand">
            <img
              src={icone}
              alt=""
              aria-hidden="true"
              decoding="sync"
              className="login-brand-icon"
            />
            <img
              src={wordmark}
              alt="AlfaSchool"
              decoding="sync"
              width={2053}
              height={332}
              className="login-brand-logo"
            />
          </div>
          <p>Gestão escolar e controle de acesso</p>
        </div>

        {/* Error */}
        {error && (
          <div className="login-error" style={{ marginBottom: 16 }}>
            <Icon name="AlertCircle" size={14} /> {error}
          </div>
        )}

        {/* Form */}
        <form className="login-form" onSubmit={handleSubmit}>
          <div className="form-field">
            <label className="form-label" htmlFor="email">E-mail</label>
            <input
              id="email"
              type="email"
              className="form-input"
              placeholder="seuemail@escola.com"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              autoFocus
            />
          </div>

          <div className="form-field">
            <label className="form-label" htmlFor="password">Senha</label>
            <div style={{ position: "relative" }}>
              <input
                id="password"
                type={showPass ? "text" : "password"}
                className="form-input"
                placeholder="••••••••••••"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
                style={{ paddingRight: 38 }}
              />
              <button
                type="button"
                onClick={() => setShowPass((v) => !v)}
                style={{
                  position: "absolute",
                  right: 10,
                  top: "50%",
                  transform: "translateY(-50%)",
                  color: "var(--color-text-2)",
                  background: "none",
                  border: "none",
                  cursor: "pointer",
                  padding: 0,
                }}
               title="Ver detalhes" aria-label="Ver detalhes">
                <Icon name="Eye" size={14} />
              </button>
            </div>
          </div>

          {/* Tenant ID (optional, for super admin) */}
          <div>
            <button
              type="button"
              className="btn btn-ghost"
              style={{ fontSize: 12, padding: "2px 0", color: "var(--color-text-2)" }}
              onClick={() => setShowTenant((v) => !v)}
            >
              {showTenant ? "▼" : "▶"} Tenant ID (opcional — Super Admin)
            </button>
            {showTenant && (
              <div className="form-field" style={{ marginTop: 8 }}>
                <input
                  type="text"
                  className="form-input"
                  placeholder="UUID do tenant"
                  value={tenantId}
                  onChange={(e) => setTenantId(e.target.value)}
                />
              </div>
            )}
          </div>

          <button type="submit" className="login-btn" disabled={loading}>
            {loading ? (
              <span style={{ display: "flex", alignItems: "center", justifyContent: "center", gap: 8 }}>
                <span className="skeleton" style={{ width: 14, height: 14, borderRadius: "50%", flexShrink: 0 }} />
                Entrando...
              </span>
            ) : (
              "Entrar"
            )}
          </button>
        </form>

        <div style={{ marginTop: 20, textAlign: "center", fontSize: 12, color: "var(--color-text-2)" }}>
          © {new Date().getFullYear()} AlfaSchool — Gestão Escolar
        </div>
      </div>
    </div>
  );
}
