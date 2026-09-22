import { createContext, useContext, useState, useEffect, useCallback } from "react";

const AuthContext = createContext(null);

function decodeJwt(token) {
  try {
    const base64 = token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/");
    const json = decodeURIComponent(
      atob(base64)
        .split("")
        .map((c) => "%" + ("00" + c.charCodeAt(0).toString(16)).slice(-2))
        .join("")
    );
    return JSON.parse(json);
  } catch {
    return {};
  }
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    try {
      const stored = localStorage.getItem("alfaschool_user");
      const salvo = stored ? JSON.parse(stored) : null;
      // Sessao gravada antes de o login devolver `modulos`: sem essa lista o
      // menu esconderia o Access inteiro (fail-closed) e a pessoa acharia
      // que perdeu acesso. Um novo login resolve, e custa menos que explicar.
      if (salvo && !Array.isArray(salvo.modulos)) {
        localStorage.removeItem("alfaschool_token");
        localStorage.removeItem("alfaschool_refresh_token");
        localStorage.removeItem("alfaschool_user");
        return null;
      }
      return salvo;
    } catch {
      return null;
    }
  });

  const [token, setToken] = useState(() => {
    return localStorage.getItem("alfaschool_token") || null;
  });

  const [loading, setLoading] = useState(false);

  // Auto-set initial data-theme if none set
  useEffect(() => {
    const saved = localStorage.getItem("alfaschool-theme");
    if (!saved) {
      document.documentElement.setAttribute("data-theme", "dark");
    }
  }, []);

  const login = useCallback(async (email, password, tenantId) => {
    setLoading(true);
    try {
      const body = { email, password };
      if (tenantId) body.tenantId = tenantId;

      const res = await fetch("/api/v1/auth/login", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body),
      });

      const raw = await res.text();
      let data = null;
      try { data = raw ? JSON.parse(raw) : null; } catch { data = null; }

      if (!res.ok) {
        throw new Error(data?.message || "Falha ao realizar login. Verifique suas credenciais.");
      }

      const {
        accessToken, refreshToken, userId, tenantId: tid, roles,
        permissoes, modulos, mustChangePassword,
      } = data.data;

      const payload = decodeJwt(accessToken);
      const userObj = {
        id: userId || payload.sub,
        nome: payload.nome || payload.name || email.split("@")[0],
        email: payload.email || email,
        roles: roles || payload.roles || [],
        // Permissões efetivas: é com elas que o menu e as telas decidem o
        // que mostrar. Mudança de permissão só vale no próximo login —
        // elas viajam no token, e é o que mantém o filtro sem ida ao banco.
        permissoes: permissoes || payload.perms || [],
        // Módulos contratados pelo tenant (ACCESS, PORTAL...). Menu e rotas
        // escondem o que a escola não contratou em vez de mostrar 403.
        modulos: Array.isArray(modulos) ? modulos : [],
        tenantId: tid || payload.tenantId,
        unitId: payload.unitId || null,
        unitNome: payload.unitNome || null,
        mustChangePassword: mustChangePassword || false,
      };

      localStorage.setItem("alfaschool_token", accessToken);
      if (refreshToken) localStorage.setItem("alfaschool_refresh_token", refreshToken);
      localStorage.setItem("alfaschool_user", JSON.stringify(userObj));

      setToken(accessToken);
      setUser(userObj);

      return userObj;
    } finally {
      setLoading(false);
    }
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem("alfaschool_token");
    localStorage.removeItem("alfaschool_refresh_token");
    localStorage.removeItem("alfaschool_user");
    setToken(null);
    setUser(null);
    window.location.href = "/login";
  }, []);

  /**
   * Superadmin entra numa rede específica (ou volta ao tenant mestre com
   * `null`). Só o access token muda; o refresh continua o do login. É o
   * "selecionar cliente" do AlfaControl: "Painel da escola" precisa dizer
   * QUAL escola, porque o tenant mestre não é escola nenhuma.
   */
  const selecionarRede = useCallback(async (tenantId) => {
    const tokenAtual = localStorage.getItem("alfaschool_token");
    const res = await fetch("/api/v1/auth/selecionar-rede", {
      method: "POST",
      headers: { "Content-Type": "application/json", Authorization: `Bearer ${tokenAtual}` },
      body: JSON.stringify({ tenantId: tenantId || null }),
    });
    const raw = await res.text();
    let data = null;
    try { data = raw ? JSON.parse(raw) : null; } catch { data = null; }
    if (!res.ok) {
      // Sessão inválida/velha: 401 sempre, e 403 aqui também — selecionar-rede
      // é ação de superadmin, então um 403 significa que o token não vale mais
      // (usuário/rede recriados, deploy, etc.). Derruba a sessão e manda pro
      // login em vez de deixar a tela presa num erro que não se recupera.
      if (res.status === 401 || res.status === 403) {
        localStorage.removeItem("alfaschool_token");
        localStorage.removeItem("alfaschool_refresh_token");
        localStorage.removeItem("alfaschool_user");
        window.location.href = "/login";
        throw new Error("Sessão expirada. Faça login novamente.");
      }
      throw new Error(data?.message || "Não foi possível entrar nesta rede.");
    }
    const r = data.data;
    localStorage.setItem("alfaschool_token", r.accessToken);
    setToken(r.accessToken);
    setUser((prev) => {
      const atualizado = {
        ...prev,
        tenantId: r.tenantId,
        tenantNome: r.mestre ? null : r.tenantNome,
        roles: r.roles || prev?.roles || [],
        permissoes: r.permissoes || [],
        modulos: r.modulos || [],
        unitId: null,
        unitNome: null,
      };
      localStorage.setItem("alfaschool_user", JSON.stringify(atualizado));
      return atualizado;
    });
    return r;
  }, []);

  const updateUserInfo = useCallback((updates) => {
    setUser((prev) => {
      const updated = { ...prev, ...updates };
      localStorage.setItem("alfaschool_user", JSON.stringify(updated));
      return updated;
    });
  }, []);

  const isAuthenticated = !!token && !!user;

  const value = {
    user,
    token,
    loading,
    isAuthenticated,
    login,
    logout,
    updateUserInfo,
    selecionarRede,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used inside AuthProvider");
  return ctx;
}
