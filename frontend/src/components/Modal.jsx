import { useEffect, useId, useRef } from "react";
import { Icon } from "./Icon";

/**
 * Janela de diálogo do sistema. Todo formulário de cadastro acontece aqui.
 *
 * <h2>O que faltava</h2>
 * O modal era um par de <div> sem semântica: para um leitor de tela o
 * formulário aparecia como conteúdo solto no meio da página anterior, que
 * continuava tabulável atrás. Quem navega por teclado saía do formulário
 * sem perceber, e ao fechar o foco não voltava para onde estava.
 *
 * Agora ele se anuncia como diálogo, prende o foco enquanto está aberto e
 * devolve o foco ao elemento que o abriu.
 */
export function Modal({ isOpen, onClose, title, children, footer, size = "" }) {
  const caixaRef = useRef(null);
  const focoAnterior = useRef(null);
  const tituloId = useId();

  useEffect(() => {
    if (!isOpen) return;
    const handler = (e) => { if (e.key === "Escape") onClose(); };
    document.addEventListener("keydown", handler);
    return () => document.removeEventListener("keydown", handler);
  }, [isOpen, onClose]);

  useEffect(() => {
    document.body.style.overflow = isOpen ? "hidden" : "";
    return () => { document.body.style.overflow = ""; };
  }, [isOpen]);

  // Foco: guarda de onde veio, move para dentro, devolve ao fechar.
  useEffect(() => {
    if (!isOpen) return;
    focoAnterior.current = document.activeElement;
    const alvo =
      caixaRef.current?.querySelector(
        "input:not([type=hidden]), select, textarea, button:not([aria-label='Fechar'])"
      ) || caixaRef.current;
    alvo?.focus?.();

    return () => {
      // O elemento pode ter saído da tela junto com a linha da tabela que o
      // abriu; nesse caso não há para onde voltar e o navegador decide.
      const voltar = focoAnterior.current;
      if (voltar && document.contains(voltar)) voltar.focus?.();
    };
  }, [isOpen]);

  /**
   * Prende o Tab dentro do diálogo. Sem isto o foco passa por trás do
   * fundo escurecido, para campos que a pessoa não consegue ver.
   */
  const aoTeclar = (e) => {
    if (e.key !== "Tab" || !caixaRef.current) return;
    const focaveis = caixaRef.current.querySelectorAll(
      'a[href], button:not([disabled]), input:not([type=hidden]):not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])'
    );
    if (focaveis.length === 0) return;
    const primeiro = focaveis[0];
    const ultimo = focaveis[focaveis.length - 1];
    if (e.shiftKey && document.activeElement === primeiro) {
      e.preventDefault();
      ultimo.focus();
    } else if (!e.shiftKey && document.activeElement === ultimo) {
      e.preventDefault();
      primeiro.focus();
    }
  };

  if (!isOpen) return null;

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div
        ref={caixaRef}
        className={`modal ${size ? `modal-${size}` : ""}`}
        role="dialog"
        aria-modal="true"
        aria-labelledby={tituloId}
        tabIndex={-1}
        onClick={(e) => e.stopPropagation()}
        onKeyDown={aoTeclar}
      >
        <div className="modal-header">
          <h3 id={tituloId}>{title}</h3>
          <button className="btn btn-ghost btn-icon" onClick={onClose} aria-label="Fechar">
            <Icon name="X" size={16} />
          </button>
        </div>
        <div className="modal-body">{children}</div>
        {footer && <div className="modal-footer">{footer}</div>}
      </div>
    </div>
  );
}
