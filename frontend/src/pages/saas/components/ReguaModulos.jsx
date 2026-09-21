import { Icon } from "../../../components/Icon";
import { MODULOS } from "../../../utils/saas";

/**
 * A "régua" de módulos: cinco casas, na ordem do catálogo, preenchidas
 * conforme o contrato. É a assinatura do painel — o negócio da Alfa é
 * vender blocos separados, e aqui dá para ver de relance quem tem o quê
 * sem ler uma lista.
 *
 * Em container estreito (card de plano) a casa mostra só o ícone; com
 * espaço, só o rótulo. Decidido por container query, não por página.
 */
export function ReguaModulos({ contratados = [], todos = false, compacta = false }) {
  return (
    <div className={`saas-regua ${compacta ? "compacta" : ""}`} aria-label="Módulos contratados">
      {MODULOS.map((m) => {
        const tem = todos || contratados.includes(m.codigo);
        return (
          <span key={m.codigo} className={`saas-regua-casa ${tem ? "tem" : ""}`} title={m.rotulo}>
            {!compacta && (
              <>
                <Icon name={m.icone} size={13} className="saas-regua-icone" />
                <span className="saas-regua-rotulo">{m.rotulo}</span>
              </>
            )}
          </span>
        );
      })}
    </div>
  );
}
