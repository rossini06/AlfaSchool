import { useState } from "react";
import { Modal } from "../Modal";
import { Feedback } from "./Feedback";

/**
 * Confirmação em Modal — substitui o confirm() nativo.
 * Quando `exigeMotivo` é true, o botão só age com um motivo preenchido e o
 * erro aparece no próprio campo.
 *
 * O conteúdo fica num componente separado, montado só enquanto aberto: assim
 * cada abertura começa com o formulário limpo, sem efeito de reset.
 */
export function ConfirmarModal({ aberto, ...props }) {
  if (!aberto) return null;
  return <ConteudoConfirmar {...props} />;
}

function ConteudoConfirmar({
  titulo,
  children,
  textoConfirmar = "Confirmar",
  variante = "btn-danger",
  exigeMotivo = false,
  rotuloMotivo = "Motivo",
  dicaMotivo = "Fica registrado no histórico e é visível para a coordenação.",
  erro = "",
  processando = false,
  onConfirmar,
  onCancelar,
}) {
  const [motivo, setMotivo] = useState("");
  const [erroMotivo, setErroMotivo] = useState("");

  const confirmar = () => {
    if (exigeMotivo && motivo.trim().length < 5) {
      setErroMotivo("Descreva o motivo com pelo menos 5 caracteres.");
      return;
    }
    onConfirmar(motivo.trim());
  };

  return (
    <Modal
      isOpen
      onClose={onCancelar}
      title={titulo}
      size="sm"
      footer={
        <>
          <button className="btn btn-secondary" onClick={onCancelar} disabled={processando}>
            Cancelar
          </button>
          <button className={`btn ${variante}`} onClick={confirmar} disabled={processando}>
            {processando ? "Processando..." : textoConfirmar}
          </button>
        </>
      }
    >
      <div style={{ color: "var(--color-text)" }}>{children}</div>
      {exigeMotivo && (
        <div className="form-field mt-3">
          <label className="form-label required">{rotuloMotivo}</label>
          <textarea
            className={`form-textarea ${erroMotivo ? "error" : ""}`}
            rows={3}
            value={motivo}
            onChange={(e) => {
              setMotivo(e.target.value);
              if (erroMotivo) setErroMotivo("");
            }}
            placeholder="Ex.: solicitação da coordenação em 18/09"
          />
          {erroMotivo ? (
            <span className="form-error">{erroMotivo}</span>
          ) : (
            <span className="form-hint">{dicaMotivo}</span>
          )}
        </div>
      )}
      <Feedback tipo="erro" mensagem={erro} />
    </Modal>
  );
}
