import { api } from "./api";

/* ==========================================================================
   Controle de Acesso — cliente HTTP.

   Duas autenticações convivem aqui:
   - A Central de Coordenação usa o JWT normal, via `api` (services/api.js).
   - O Painel de sala roda numa Smart TV que NÃO faz login: ele manda o token
     do dispositivo no header `X-Painel-Token` (e na query `?token=` no SSE,
     porque o EventSource do navegador não permite headers). Por isso o painel
     não pode passar pelo `api`: um 401 lá redireciona para /login, o que numa
     TV significaria uma tela de login presa para sempre.

   Contrato esperado do backend (camelCase — o projeto não configura
   PropertyNamingStrategy no Jackson). Os normalizadores abaixo aceitam
   algumas grafias alternativas para não quebrar se o backend divergir.

   GET /api/v1/access/paineis/{slug}/estado
     { painel: { id, nome, slug, tipo, exibeFoto, retencaoSeg },
       turmaNome, salaNome, unidadeNome,
       retiradas: [ <retirada> ] }

   <retirada>
     { id, status, ordemChegada, solicitadoEm, preparandoEm, prontoEm,
       entregueEm, retiradaManual, motivo, observacao,
       turmaNome, salaNome, portariaNome, unidadeNome,
       aluno: { id, nome, fotoUrl, turmaNome, salaNome },
       pessoaAutorizada: { id, nome, fotoUrl, parentesco } }

   status ∈ SOLICITADA | PREPARANDO | PRONTO | ENTREGUE | CANCELADA | NEGADA
   ========================================================================== */

const API_BASE = "/api/v1";

export const PAINEL_TOKEN_KEY = "alfaschool_painel_token";

/* ------------------------------ util ----------------------------------- */

function mensagemHttp(status, statusText) {
  if (status === 401 || status === 403) {
    return "Token do painel inválido, expirado ou revogado.";
  }
  if (status === 404) {
    return "Recurso não encontrado. Verifique o endereço do painel.";
  }
  if (status >= 500) {
    return "O servidor não conseguiu responder agora.";
  }
  return `Erro ${status}${statusText ? `: ${statusText}` : ""}`;
}

/**
 * Heurística para "esse endpoint ainda não existe no backend".
 * O módulo de acesso está sendo construído em fatias; até o backend subir,
 * as telas precisam degradar com uma mensagem honesta em vez de quebrar.
 */
export function pareceEndpointAusente(err) {
  if (!err) return false;
  if (err.status === 404 || err.status === 501) return true;
  return /\b(404|501)\b|not found|no handler/i.test(err.message || "");
}

/* --------------------------- painel (TV) -------------------------------- */

async function painelRequest(endpoint, { method = "GET", body, token } = {}) {
  const headers = { "Content-Type": "application/json" };
  if (token) headers["X-Painel-Token"] = token;

  let res;
  try {
    res = await fetch(`${API_BASE}${endpoint}`, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
    });
  } catch {
    const err = new Error("Sem conexão com o servidor da escola.");
    err.status = 0;
    throw err;
  }

  const raw = await res.text();
  let data = null;
  try {
    data = raw ? JSON.parse(raw) : null;
  } catch {
    data = null;
  }

  if (!res.ok) {
    const err = new Error(
      data?.message || data?.error || mensagemHttp(res.status, res.statusText)
    );
    err.status = res.status;
    throw err;
  }

  // Desembrulha o ApiResponse { timestamp, status, message, data }.
  if (data && typeof data === "object" && "data" in data) return data.data;
  return data;
}

export function painelEstado(slug, token) {
  return painelRequest(`/access/paineis/${encodeURIComponent(slug)}/estado`, { token });
}

export function painelStreamUrl(slug, token) {
  if (!slug || !token) return null;
  return `${API_BASE}/access/paineis/${encodeURIComponent(slug)}/stream?token=${encodeURIComponent(token)}`;
}

/**
 * Botão "Preparar aluno para saída" da TV da sala.
 *
 * Usa a rota do PAINEL, não a de retiradas: a TV autentica por token de
 * dispositivo e não por login, e o servidor só aceita retirada que esteja
 * dentro do recorte daquele painel — a TV da 101 não prepara aluno da 102.
 *
 * Preparar não entrega criança nenhuma. A entrega continua exigindo
 * colaborador autenticado, pela Central de Coordenação.
 */
export function painelPrepararRetirada(retiradaId, token, slug) {
  return painelRequest(
    `/access/paineis/${encodeURIComponent(slug)}/retiradas/${encodeURIComponent(retiradaId)}/preparar`,
    { method: "POST", token, body: {} }
  );
}

/* ------------------------ coordenação (com JWT) ------------------------- */

export function coordenacaoResumo(unitId) {
  const params = new URLSearchParams();
  if (unitId) params.set("unitId", unitId);
  const qs = params.toString();
  return api.get(`/access/paineis/coordenacao/resumo${qs ? `?${qs}` : ""}`);
}

export function filaRetiradas(filtros = {}) {
  const params = new URLSearchParams();
  Object.entries(filtros).forEach(([chave, valor]) => {
    if (valor !== "" && valor !== null && valor !== undefined) {
      params.set(chave, valor);
    }
  });
  const qs = params.toString();
  return api.get(`/access/retiradas/fila${qs ? `?${qs}` : ""}`);
}

/** acao ∈ preparar | pronto | entregar | cancelar | negar */
export function acaoRetirada(retiradaId, acao, body = {}) {
  return api.post(`/access/retiradas/${encodeURIComponent(retiradaId)}/${acao}`, body);
}

export function criarRetiradaManual(body) {
  return api.post("/access/retiradas/manual", body);
}

export function coordenacaoStreamUrl(token) {
  if (!token) return null;
  return `${API_BASE}/access/paineis/coordenacao/stream?token=${encodeURIComponent(token)}`;
}

/* ---------------------------- normalizadores ---------------------------- */

function primeiro(...valores) {
  for (const v of valores) {
    if (v !== undefined && v !== null && v !== "") return v;
  }
  return null;
}

function normalizarPessoa(pessoa, prefixo, bruto) {
  const p = pessoa || {};
  // Aceita tanto o objeto aninhado (`aluno: { nome }`) quanto o achatado
  // (`alunoNome`), porque os DTOs do backend ainda não estão fechados.
  const achatado = (sufixo) => (bruto && prefixo ? bruto[`${prefixo}${sufixo}`] : undefined);

  return {
    id: primeiro(p.id, achatado("Id")),
    nome: primeiro(p.nome, p.name, achatado("Nome")),
    foto: primeiro(p.fotoUrl, p.foto, p.fotoKey, achatado("FotoUrl"), achatado("Foto")),
    parentesco: primeiro(p.parentesco, p.parentescoDescricao, p.vinculo, achatado("Parentesco")),
    turmaNome: primeiro(p.turmaNome, p.turma?.nome, achatado("TurmaNome")),
    salaNome: primeiro(p.salaNome, p.sala?.nome, achatado("SalaNome")),
  };
}

/** Deixa uma retirada do backend no formato que as telas consomem. */
export function normalizarRetirada(bruto) {
  if (!bruto) return null;

  const aluno = normalizarPessoa(bruto.aluno, "aluno", bruto);
  const retirante = normalizarPessoa(
    bruto.pessoaAutorizada || bruto.retirante || bruto.responsavel,
    "pessoaAutorizada",
    bruto
  );

  // Aliases comuns para o nome de quem veio buscar.
  if (!retirante.nome) {
    retirante.nome = primeiro(
      bruto.retiranteNome,
      bruto.responsavelNome,
      bruto.pessoaNome
    );
  }
  if (!retirante.foto) {
    retirante.foto = primeiro(
      bruto.retiranteFotoUrl,
      bruto.responsavelFotoUrl,
      bruto.pessoaFotoUrl
    );
  }
  if (!retirante.parentesco) {
    retirante.parentesco = primeiro(bruto.parentesco, bruto.vinculo);
  }

  return {
    id: primeiro(bruto.id, bruto.retiradaId),
    status: primeiro(bruto.status, bruto.statusRetirada) || "SOLICITADA",
    ordemChegada: bruto.ordemChegada ?? null,
    solicitadoEm: primeiro(bruto.solicitadoEm, bruto.chegadaEm, bruto.createdAt),
    preparandoEm: bruto.preparandoEm ?? null,
    prontoEm: bruto.prontoEm ?? null,
    entregueEm: bruto.entregueEm ?? null,
    canceladoEm: bruto.canceladoEm ?? null,
    retiradaManual: bruto.retiradaManual === true,
    motivo: bruto.motivo ?? null,
    observacao: bruto.observacao ?? null,
    turmaNome: primeiro(bruto.turmaNome, bruto.turma?.nome, aluno.turmaNome),
    salaNome: primeiro(bruto.salaNome, bruto.sala?.nome, aluno.salaNome),
    portariaNome: primeiro(bruto.portariaNome, bruto.portaria?.nome),
    unidadeNome: primeiro(bruto.unidadeNome, bruto.unidade?.nome),
    aluno,
    retirante,
  };
}

/** Normaliza a resposta de /paineis/{slug}/estado. */
export function normalizarEstadoPainel(bruto) {
  const painel = bruto?.painel || bruto || {};
  const lista = bruto?.retiradas || bruto?.fila || [];

  return {
    painelNome: primeiro(painel.nome, bruto?.painelNome),
    slug: primeiro(painel.slug, bruto?.slug),
    tipo: primeiro(painel.tipo, bruto?.tipo),
    // Aceita também snake_case: a coluna no banco é `retencao_seg` e nem todo
    // endpoint pode acabar passando pelo Jackson padrão do projeto.
    exibeFoto: (painel.exibeFoto ?? painel.exibe_foto ?? bruto?.exibeFoto) !== false,
    retencaoSeg:
      Number(primeiro(painel.retencaoSeg, painel.retencao_seg, bruto?.retencaoSeg, bruto?.retencao_seg, 20)) || 20,
    turmaNome: primeiro(bruto?.turmaNome, bruto?.turma?.nome),
    salaNome: primeiro(bruto?.salaNome, bruto?.sala?.nome),
    unidadeNome: primeiro(bruto?.unidadeNome, bruto?.unidade?.nome),
    retiradas: (Array.isArray(lista) ? lista : []).map(normalizarRetirada).filter(Boolean),
  };
}

/** Normaliza o resumo de contadores da coordenação. */
export function normalizarResumo(bruto) {
  const b = bruto || {};
  return {
    alunosPresentes: primeiro(b.alunosPresentes, b.presentes, b.totalPresentes) ?? 0,
    aguardandoRetirada: primeiro(b.aguardandoRetirada, b.aguardando, b.totalAguardando) ?? 0,
    horarioExcedido: primeiro(b.horarioExcedido, b.excedentes, b.totalExcedido) ?? 0,
    saidasConcluidas: primeiro(b.saidasConcluidas, b.saidas, b.totalSaidas) ?? 0,
  };
}
