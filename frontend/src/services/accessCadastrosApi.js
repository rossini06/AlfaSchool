import { api } from "./api";

/**
 * Camada fina sobre o cliente HTTP para o módulo de Controle de Acesso.
 *
 * O módulo está sendo construído aos poucos no backend: enquanto um endpoint
 * não existir, a tela precisa degradar com uma mensagem clara em vez de
 * quebrar. Por isso as funções abaixo nunca lançam — devolvem um envelope
 * { ok, data, erro, indisponivel }.
 */

const PADROES_INDISPONIVEL =
  /(^|\D)(404|405|501|502|503)(\D|$)|not found|no static resource|no handler|failed to fetch|não implement|nao implement/i;

export function endpointIndisponivel(err) {
  // O cliente base passou a carregar err.status; quando não houver, cai no texto.
  if (typeof err?.status === "number") return [404, 405, 501, 502, 503].includes(err.status);
  return PADROES_INDISPONIVEL.test(err?.message || "");
}

export const MSG_INDISPONIVEL =
  "Este recurso ainda não está publicado no servidor. A tela abriu normalmente, mas os dados só aparecem quando o endpoint existir.";

async function envelope(promise) {
  try {
    return { ok: true, data: await promise, erro: "", indisponivel: false };
  } catch (err) {
    const indisponivel = endpointIndisponivel(err);
    return {
      ok: false,
      data: null,
      indisponivel,
      erro: indisponivel ? MSG_INDISPONIVEL : err.message || "Falha inesperada.",
    };
  }
}

export const accessApi = {
  get: (url) => envelope(api.get(url)),
  post: (url, body) => envelope(api.post(url, body)),
  put: (url, body) => envelope(api.put(url, body)),
  patch: (url, body) => envelope(api.patch(url, body)),
  delete: (url) => envelope(api.delete(url)),
};

/** Monta a querystring ignorando valores vazios. */
export function qs(obj = {}) {
  const p = new URLSearchParams();
  Object.entries(obj).forEach(([k, v]) => {
    if (v === undefined || v === null || v === "") return;
    p.set(k, String(v));
  });
  return p.toString();
}

/** Normaliza Page<T> do Spring ou uma lista crua. */
export function comoLista(data) {
  if (Array.isArray(data)) return data;
  if (Array.isArray(data?.content)) return data.content;
  return [];
}

export function comoTotal(data) {
  if (typeof data?.totalElements === "number") return data.totalElements;
  return comoLista(data).length;
}

/** Carrega uma lista auxiliar (selects). Silencioso: select vazio é aceitável. */
export async function carregarAuxiliar(url) {
  const r = await accessApi.get(url);
  return r.ok ? comoLista(r.data) : [];
}
