/** Dias da semana no padrão ISO — 1 = segunda ... 7 = domingo. */

export const DIAS_SEMANA = [
  { valor: 1, curto: "S", nome: "Segunda" },
  { valor: 2, curto: "T", nome: "Terça" },
  { valor: 3, curto: "Q", nome: "Quarta" },
  { valor: 4, curto: "Q", nome: "Quinta" },
  { valor: 5, curto: "S", nome: "Sexta" },
  { valor: 6, curto: "S", nome: "Sábado" },
  { valor: 7, curto: "D", nome: "Domingo" },
];

/**
 * A API guarda os dias em CSV ("1,3,5"); a interface trabalha com lista,
 * que é o natural para um grupo de caixas de seleção. A conversão fica
 * aqui, na fronteira, e não espalhada por tela.
 *
 * Enviar o array cru quebrava a desserialização ANTES da validação, então
 * a resposta vinha "JSON inválido" — sem dizer qual campo. Era o motivo de
 * nenhuma autorização e nenhum vínculo turma-sala poder ser salvo.
 */
export function diasParaCsv(dias) {
  if (!dias || dias.length === 0) return null;
  return [...dias].map(Number).filter(Boolean).sort((a, b) => a - b).join(",");
}

/** Caminho inverso: aceita CSV, lista, ou nada. */
export function diasDeCsv(valor) {
  if (!valor) return [];
  if (Array.isArray(valor)) return valor.map(Number).filter(Boolean);
  return String(valor)
    .split(",")
    .map((d) => Number(d.trim()))
    .filter(Boolean);
}

/** Resume a seleção em texto curto para tabelas e cartões. */
export function nomesDosDias(dias) {
  const lista = diasDeCsv(dias);
  if (lista.length === 0) return "—";
  const ordenados = [...lista].sort((a, b) => a - b);
  if (ordenados.join(",") === "1,2,3,4,5") return "Seg a Sex";
  if (ordenados.join(",") === "1,2,3,4,5,6,7") return "Todos os dias";
  return ordenados
    .map((d) => DIAS_SEMANA.find((x) => x.valor === d)?.nome.slice(0, 3))
    .filter(Boolean)
    .join(", ");
}
