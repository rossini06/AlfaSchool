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
      return stored ? JSON.parse(stored) : null;
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
        permissoes, mustChangePassword,
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
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used inside AuthProvider");
  return ctx;
}
