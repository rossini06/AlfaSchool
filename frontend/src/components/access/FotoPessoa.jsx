import { useState } from "react";

/**
 * Foto de aluno ou responsável, com placeholder de silhueta.
 * Nunca mostra imagem quebrada: se `foto` for vazia ou o carregamento falhar,
 * cai para a silhueta desenhada em SVG.
 *
 * props:
 *   foto      — url ou data-uri da foto (pode ser null/undefined)
 *   nome      — usado apenas no alt/title
 *   size      — lado em px (a foto é sempre quadrada)
 *   quadrada  — cantos arredondados em vez de círculo
 *   tv        — paleta do painel de TV (fundo escuro)
 */
export function FotoPessoa({ foto, nome, size = 40, quadrada = false, tv = false, className = "" }) {
  // Guardamos QUAL url falhou, não um booleano: assim uma foto nova volta a
  // ser tentada sozinha, sem precisar de efeito para resetar o estado.
  const [urlQueFalhou, setUrlQueFalhou] = useState(null);
  const falhou = Boolean(foto) && urlQueFalhou === foto;

  const classes = [
    "ac-foto",
    quadrada ? "ac-foto-quadrada" : "",
    tv ? "ac-foto-tv" : "",
    className,
  ].filter(Boolean).join(" ");

  const style = { width: size, height: size };

  if (!foto || falhou) {
    return (
      <div className={classes} style={style} title={nome || undefined} aria-hidden="true">
        <Silhueta size={Math.round(size * 0.62)} />
      </div>
    );
  }

  return (
    <div className={classes} style={style}>
      <img
        className="ac-foto-img"
        src={foto}
        alt={nome ? `Foto de ${nome}` : ""}
        onError={() => setUrlQueFalhou(foto)}
        draggable={false}
      />
    </div>
  );
}

function Silhueta({ size }) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={1.6}
      strokeLinecap="round"
      strokeLinejoin="round"
      style={{ opacity: 0.55 }}
      aria-hidden="true"
    >
      <path d="M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8z" />
      <path d="M4 21a8 8 0 0 1 16 0" />
    </svg>
  );
}
