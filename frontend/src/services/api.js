const API_BASE = "/api/v1";

async function request(endpoint, options = {}) {
  const token = localStorage.getItem("alfaschool_token");

  const headers = {
    "Content-Type": "application/json",
    ...(options.headers || {}),
  };

  if (token) {
    headers["Authorization"] = `Bearer ${token}`;
  }

  const url = endpoint.startsWith("http") ? endpoint : `${API_BASE}${endpoint}`;

  const res = await fetch(url, {
    ...options,
    headers,
  });

  if (res.status === 401) {
    localStorage.removeItem("alfaschool_token");
    localStorage.removeItem("alfaschool_refresh_token");
    localStorage.removeItem("alfaschool_user");
    window.location.href = "/login";
    throw new Error("Sessão expirada. Faça login novamente.");
  }

  const raw = await res.text();
  let data = null;
  try {
    data = raw ? JSON.parse(raw) : null;
  } catch {
    data = null;
  }

  if (!res.ok) {
    const message =
      data?.message ||
      data?.error ||
      `Erro ${res.status}: ${res.statusText}`;
    throw new Error(message);
  }

  // Unwrap ApiResponse wrapper: { data: ..., message: ..., status: ... }
  if (data && typeof data === "object" && "data" in data) {
    return data.data;
  }

  return data;
}

export const api = {
  get: (url) => request(url),
  post: (url, body) =>
    request(url, { method: "POST", body: JSON.stringify(body) }),
  put: (url, body) =>
    request(url, { method: "PUT", body: JSON.stringify(body) }),
  patch: (url, body) =>
    request(url, { method: "PATCH", body: JSON.stringify(body) }),
  delete: (url) => request(url, { method: "DELETE" }),
};
