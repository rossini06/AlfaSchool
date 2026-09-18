/**
 * Selo "Ao vivo" / "Sem conexão".
 * Sempre visível: a professora precisa saber quando a tela está velha.
 *
 * props:
 *   estado          — "conectando" | "conectado" | "desconectado"
 *   ultimaAtualizacao — Date (ou null) do último dado aplicado na tela
 *   tv              — paleta do painel de TV
 */
export function IndicadorConexao({ estado, ultimaAtualizacao, tv = false, style, rotuloConectado }) {
  const mapa = {
    conectado: { cls: "is-live", label: rotuloConectado || "Ao vivo" },
    conectando: { cls: "is-wait", label: "Conectando…" },
    desconectado: { cls: "is-down", label: "Sem conexão" },
  };
  const { cls, label } = mapa[estado] || mapa.desconectado;

  return (
    <span
      className={`ac-conexao ${cls} ${tv ? "ac-conexao-tv" : ""}`}
      style={style}
      role="status"
      aria-live="polite"
    >
      <span className="ac-conexao-dot" />
      <span className="ac-conexao-label">{label}</span>
      <span className="ac-conexao-hora">
        {ultimaAtualizacao
          ? `atualizado ${formatarHora(ultimaAtualizacao)}`
          : "sem dados ainda"}
      </span>
    </span>
  );
}

function formatarHora(data) {
  try {
    return data.toLocaleTimeString("pt-BR", { hour: "2-digit", minute: "2-digit", second: "2-digit" });
  } catch {
    return "—";
  }
}
