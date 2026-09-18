/** Utilidades de hora/minuto usadas pelo módulo de Controle de Acesso. */

/** "07:30" -> 450 ; aceita "07:30:00". Retorna null se inválido. */
export function horaParaMinutos(hora) {
  if (!hora) return null;
  const m = /^(\d{1,2}):(\d{2})/.exec(String(hora).trim());
  if (!m) return null;
  const h = Number(m[1]);
  const min = Number(m[2]);
  if (h > 23 || min > 59) return null;
  return h * 60 + min;
}

/** 450 -> "07:30" (suporta virar o dia, ex.: saída depois da meia-noite). */
export function minutosParaHora(minutos) {
  if (minutos === null || minutos === undefined || Number.isNaN(minutos)) return "";
  const total = ((Math.round(minutos) % 1440) + 1440) % 1440;
  const h = Math.floor(total / 60);
  const m = total % 60;
  return `${String(h).padStart(2, "0")}:${String(m).padStart(2, "0")}`;
}

/** 625 -> "10h25" ; 60 -> "1h" ; 25 -> "25min" */
export function formatarDuracao(minutos) {
  if (minutos === null || minutos === undefined || Number.isNaN(minutos)) return "—";
  const neg = minutos < 0;
  const total = Math.abs(Math.round(minutos));
  const h = Math.floor(total / 60);
  const m = total % 60;
  let txt;
  if (h === 0) txt = `${m}min`;
  else if (m === 0) txt = `${h}h`;
  else txt = `${h}h${String(m).padStart(2, "0")}`;
  return neg ? `-${txt}` : txt;
}

/** Carga entre entrada e saída, tratando jornada que cruza a meia-noite. */
export function calcularCarga(entrada, saida) {
  const ini = horaParaMinutos(entrada);
  const fim = horaParaMinutos(saida);
  if (ini === null || fim === null) return null;
  let diff = fim - ini;
  if (diff < 0) diff += 1440;
  return diff;
}

/** "2026-09-18" -> "18/09/2026" (sem depender de fuso). */
export function formatarData(iso) {
  if (!iso) return "—";
  const d = String(iso).slice(0, 10).split("-");
  if (d.length !== 3) return String(iso);
  return `${d[2]}/${d[1]}/${d[0]}`;
}

/** "2026-09-18T07:03:11" -> "18/09/2026 07:03" */
export function formatarDataHora(iso) {
  if (!iso) return "—";
  const s = String(iso);
  const data = formatarData(s.slice(0, 10));
  const hora = s.slice(11, 16);
  return hora ? `${data} ${hora}` : data;
}

/** "07:03:11" ou ISO -> "07:03" */
export function formatarHora(valor) {
  if (!valor) return "—";
  const s = String(valor);
  if (s.includes("T")) return s.slice(11, 16);
  return s.slice(0, 5);
}

export function hojeIso() {
  const d = new Date();
  const mes = String(d.getMonth() + 1).padStart(2, "0");
  const dia = String(d.getDate()).padStart(2, "0");
  return `${d.getFullYear()}-${mes}-${dia}`;
}

export function isoComDiferencaDeDias(dias) {
  const d = new Date();
  d.setDate(d.getDate() + dias);
  const mes = String(d.getMonth() + 1).padStart(2, "0");
  const dia = String(d.getDate()).padStart(2, "0");
  return `${d.getFullYear()}-${mes}-${dia}`;
}

/** Minutos desde uma marcação até agora. Aceita ISO completo ou só "HH:mm". */
export function minutosDesde(valor) {
  if (!valor) return null;
  const s = String(valor);
  if (/^\d{1,2}:\d{2}/.test(s)) {
    const agora = new Date();
    const minutosAgora = agora.getHours() * 60 + agora.getMinutes();
    const marcacao = horaParaMinutos(s);
    if (marcacao === null) return null;
    return Math.max(0, minutosAgora - marcacao);
  }
  const t = new Date(s).getTime();
  if (Number.isNaN(t)) return null;
  return Math.max(0, Math.round((Date.now() - t) / 60000));
}
