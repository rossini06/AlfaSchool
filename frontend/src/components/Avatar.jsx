import { useState } from "react";

function initials(nome) {
  if (!nome) return "?";
  const parts = nome.trim().split(" ");
  return parts.length >= 2
    ? (parts[0][0] + parts[parts.length - 1][0]).toUpperCase()
    : parts[0].slice(0, 2).toUpperCase();
}

export function Avatar({ foto, nome, size = 28, fallback }) {
  // Guardamos QUAL url falhou, nao um booleano: assim uma foto nova volta a
  // tentar carregar em vez de ficar presa nas iniciais.
  const [urlQueFalhou, setUrlQueFalhou] = useState(null);
  const falhou = Boolean(foto) && urlQueFalhou === foto;
  const letter = fallback ?? initials(nome);
  const style = {
    width: size, height: size, borderRadius: "50%",
    flexShrink: 0, objectFit: "cover",
  };

  if (foto && !falhou) {
    return (
      <img
        src={foto}
        alt=""
        style={style}
        onError={() => setUrlQueFalhou(foto)}
      />
    );
  }

  return (
    <div style={{
      ...style,
      background: "var(--color-brand-dim)", color: "var(--color-brand)",
      display: "flex", alignItems: "center", justifyContent: "center",
      fontWeight: 700, fontSize: size * 0.43,
    }}>
      {letter}
    </div>
  );
}
