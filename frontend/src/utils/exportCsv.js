/**
 * Exportação CSV sem dependência externa (Blob + download).
 * Separador ";" e BOM UTF-8 para o Excel em pt-BR abrir sem sujar acento.
 */

function escapar(valor) {
  if (valor === null || valor === undefined) return "";
  const s = String(valor);
  if (/[";\n\r]/.test(s)) return `"${s.replace(/"/g, '""')}"`;
  return s;
}

/**
 * @param {string} nomeArquivo  ex.: "movimentacoes"
 * @param {Array<{key:string,label:string,format?:Function}>} colunas
 * @param {Array<object>} linhas
 */
export function exportarCsv(nomeArquivo, colunas, linhas) {
  const cabecalho = colunas.map((c) => escapar(c.label)).join(";");
  const corpo = (linhas || []).map((linha) =>
    colunas
      .map((c) => escapar(c.format ? c.format(linha[c.key], linha) : linha[c.key]))
      .join(";")
  );
  const conteudo = "﻿" + [cabecalho, ...corpo].join("\r\n");

  const blob = new Blob([conteudo], { type: "text/csv;charset=utf-8;" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  const carimbo = new Date().toISOString().slice(0, 10);
  a.href = url;
  a.download = `${nomeArquivo}-${carimbo}.csv`;
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
}
