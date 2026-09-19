import { Link } from "react-router-dom";
import { Icon } from "../../components/Icon";
import { ajudaDoPortal } from "../../config/ajudaConfig";

/**
 * Tutorial do Portal da Família.
 *
 * Separado do tutorial administrativo de propósito. Quem lê aqui é pai ou
 * mãe, quase sempre no celular e com pressa: não interessa a ele o que é
 * uma "permissão" nem como a escola configura um leitor. Interessa saber se
 * o filho está na escola, quem pode buscá-lo e por que recebeu um aviso.
 *
 * Por isso o texto é curto, sem seção de regras do sistema, e cada bloco
 * termina num link para a tela correspondente.
 */
export function PortalAjudaPage() {
  return (
    <div>
      <h1 className="ac-portal-titulo">Como usar o portal</h1>
      <p className="ac-portal-sub">
        Quatro telas, e o que dá para fazer em cada uma.
      </p>

      {ajudaDoPortal.map((t) => (
        <article className="ac-filho-card" key={t.caminho}>
          <header className="ac-filho-head">
            <div>
              <div className="ac-filho-nome">{t.titulo}</div>
              <div className="ac-filho-turma">{t.oQueE}</div>
            </div>
          </header>
          <div className="ac-filho-body">
            <ul style={{ margin: 0, padding: 0 }}>
              {t.comoUsar.map((p, i) => (
                <li
                  key={i}
                  style={{
                    position: "relative",
                    paddingLeft: 16,
                    marginBottom: 7,
                    fontSize: 13.5,
                    lineHeight: 1.65,
                  }}
                >
                  <span
                    style={{
                      position: "absolute",
                      left: 0,
                      top: 8,
                      width: 5,
                      height: 5,
                      borderRadius: "50%",
                      background: "var(--color-brand)",
                    }}
                  />
                  {p}
                </li>
              ))}
            </ul>

            {t.aviso && (
              <div className="ac-aviso ac-aviso-alerta" style={{ marginTop: 12 }}>
                <Icon name="AlertCircle" size={16} className="ac-aviso-icone" />
                <div className="ac-aviso-texto">{t.aviso}</div>
              </div>
            )}

            <Link className="btn btn-secondary btn-sm" to={t.caminho} style={{ marginTop: 14 }}>
              <Icon name="ArrowRight" size={13} /> Abrir {t.titulo}
            </Link>
          </div>
        </article>
      ))}

      <article className="ac-filho-card">
        <div className="ac-filho-body">
          <div className="ac-filho-sec-titulo">Sobre a foto e os dados do seu filho</div>
          <p style={{ fontSize: 13.5, lineHeight: 1.65 }}>
            A escola pode usar reconhecimento facial na portaria. Isso exige que você tenha
            autorizado, e a autorização fica registrada — com data e versão do termo.
            Você pode retirá-la a qualquer momento, sem custo: ao fazer isso, o rosto do seu
            filho é removido dos leitores onde estava gravado.
          </p>
          <p style={{ fontSize: 13.5, lineHeight: 1.65, marginTop: 10 }}>
            Você também pode pedir à escola a relação de tudo o que ela guarda sobre o seu
            filho. Fale com a direção.
          </p>
        </div>
      </article>
    </div>
  );
}
