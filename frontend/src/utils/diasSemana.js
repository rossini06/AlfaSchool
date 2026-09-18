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

/** Resume a seleção em texto curto para tabelas e cartões. */
export function nomesDosDias(dias) {
  if (!dias || dias.length === 0) return "—";
  const ordenados = [...dias].sort((a, b) => a - b);
  if (ordenados.join(",") === "1,2,3,4,5") return "Seg a Sex";
  if (ordenados.join(",") === "1,2,3,4,5,6,7") return "Todos os dias";
  return ordenados
    .map((d) => DIAS_SEMANA.find((x) => x.valor === d)?.nome.slice(0, 3))
    .filter(Boolean)
    .join(", ");
}
