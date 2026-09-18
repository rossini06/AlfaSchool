import { useState } from "react";
import { Modal } from "../Modal";
import { Icon } from "../Icon";
import { Aviso } from "./Feedback";

/**
 * Exibição de um token que o servidor mostra UMA ÚNICA VEZ.
 * A tela precisa deixar isso explícito e oferecer a cópia — depois de fechar,
 * só resta gerar outro.
 */
export function TokenUnico({ aberto, token, titulo = "Token gerado", descricao, onFechar }) {
  const [copiado, setCopiado] = useState(false);

  const copiar = async () => {
    try {
      if (navigator.clipboard?.writeText) {
        await navigator.clipboard.writeText(token);
      } else {
        const ta = document.createElement("textarea");
        ta.value = token;
        ta.style.position = "fixed";
        ta.style.opacity = "0";
        document.body.appendChild(ta);
        ta.select();
        document.execCommand("copy");
        document.body.removeChild(ta);
      }
      setCopiado(true);
      setTimeout(() => setCopiado(false), 2500);
    } catch {
      setCopiado(false);
    }
  };

  return (
    <Modal
      isOpen={aberto && !!token}
      onClose={onFechar}
      title={titulo}
      footer={
        <button className="btn btn-brand" onClick={onFechar}>
          Já copiei, pode fechar
        </button>
      }
    >
      <Aviso tipo="alerta" titulo="Este token aparece uma única vez">
        Ao fechar esta janela o valor não poderá ser consultado de novo — o servidor guarda
        apenas o resumo criptográfico. Se você perder, será preciso gerar um novo, e o
        anterior deixa de funcionar.
      </Aviso>

      {descricao && <p className="form-hint mt-2">{descricao}</p>}

      <div className="ac-token-box mt-3">
        <code className="ac-token-valor">{token}</code>
        <button className="btn btn-secondary btn-sm" onClick={copiar}>
          <Icon name={copiado ? "Check" : "Copy"} size={13} />
          {copiado ? "Copiado" : "Copiar"}
        </button>
      </div>
    </Modal>
  );
}
