/** Utilidades do painel SaaS: rótulos de módulo, CNPJ e monograma. */

/** Ordem e rótulo curto de cada módulo, como aparecem na "régua" do card. */
export const MODULOS = [
  { codigo: "ACCESS", rotulo: "Acesso", icone: "ScanFace" },
  { codigo: "PEDAGOGICO", rotulo: "Pedagógico", icone: "BookOpen" },
  { codigo: "FINANCEIRO", rotulo: "Financeiro", icone: "DollarSign" },
  { codigo: "PORTAL", rotulo: "Portal", icone: "Users" },
  { codigo: "NOTIFICACOES", rotulo: "Avisos", icone: "Bell" },
];

export function rotuloModulo(codigo) {
  return MODULOS.find((m) => m.codigo === codigo)?.rotulo || codigo;
}

/** 12345678000199 → 12.345.678/0001-99. Fora do padrão, devolve como veio. */
export function formatarCnpj(valor) {
  const d = String(valor || "").replace(/\D/g, "");
  if (d.length !== 14) return valor || "—";
  return d.replace(/^(\d{2})(\d{3})(\d{3})(\d{4})(\d{2})$/, "$1.$2.$3/$4-$5");
}

/** "Colégio Mundo do Saber" → "CM". Ignora artigos e preposições. */
export function monograma(nome) {
  const partes = String(nome || "")
    .split(/\s+/)
    .filter((p) => p && !/^(de|da|do|das|dos|e|a|o)$/i.test(p));
  if (partes.length === 0) return "?";
  return (partes[0][0] + (partes[1]?.[0] || "")).toUpperCase();
}

/** Cor estável por nome, para o monograma não ser sempre igual. */
export function tomDoMonograma(nome) {
  const tons = ["brand", "info", "success", "warning"];
  let h = 0;
  for (const c of String(nome || "")) h = (h * 31 + c.charCodeAt(0)) >>> 0;
  return tons[h % tons.length];
}
