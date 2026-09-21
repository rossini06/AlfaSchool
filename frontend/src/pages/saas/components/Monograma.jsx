import { monograma, tomDoMonograma } from "../../../utils/saas";

/** Selo da rede: iniciais numa moeda tingida. Substitui logo que não temos. */
export function Monograma({ nome, mestre = false, tamanho = 40 }) {
  return (
    <span
      className={`saas-monograma tom-${mestre ? "alfa" : tomDoMonograma(nome)}`}
      style={{ width: tamanho, height: tamanho, fontSize: Math.round(tamanho * 0.36) }}
      aria-hidden="true"
    >
      {mestre ? "α" : monograma(nome)}
    </span>
  );
}
