import { useState, useEffect, useRef, useCallback } from "react";

// Backoff de reconexão: 1s, 2s, 4s, 8s, 16s e daí em diante 30s.
const BACKOFF_MS = [1000, 2000, 4000, 8000, 16000, 30000];

/**
 * EventSource com reconexão, backoff e rebusca de estado.
 *
 * Regra central: depois de qualquer queda, o fluxo de eventos perdidos não
 * volta. Então, ao reconectar, o estado COMPLETO é buscado de novo — nunca
 * confiamos só nos eventos que chegarem depois.
 *
 * Se `url` for nula e `pollingMs` for maior que zero, o hook cai para polling
 * chamando `buscarEstado` no intervalo indicado. Os dois caminhos entregam a
 * mesma interface para a tela.
 *
 * @param {object}   opts
 * @param {string?}  opts.url          URL do stream SSE (com token na query). Null → polling.
 * @param {object}   opts.eventos      Mapa { "nome.do.evento": (payload, ev) => void }.
 * @param {Function} opts.buscarEstado Async. Rebusca o estado completo da tela.
 * @param {number}   opts.pollingMs    Intervalo do modo polling (0 desliga).
 * @param {boolean}  opts.habilitado   Liga/desliga tudo (ex.: enquanto não há token).
 *
 * @returns {{ conexao: string, modo: string, ultimaAtualizacao: Date|null,
 *             marcarAtualizado: Function, reconectarAgora: Function }}
 */
export function useSseAoVivo({
  url = null,
  eventos = null,
  buscarEstado = null,
  pollingMs = 0,
  habilitado = true,
} = {}) {
  const [conexao, setConexao] = useState("conectando");
  const [ultimaAtualizacao, setUltimaAtualizacao] = useState(null);
  const [gatilho, setGatilho] = useState(0);

  const eventosRef = useRef(eventos);
  const buscarRef = useRef(buscarEstado);

  useEffect(() => { eventosRef.current = eventos; });
  useEffect(() => { buscarRef.current = buscarEstado; });

  const marcarAtualizado = useCallback(() => setUltimaAtualizacao(new Date()), []);

  // Força uma reconexão imediata (botão "Tentar de novo").
  const reconectarAgora = useCallback(() => setGatilho((g) => g + 1), []);

  const usandoSse = Boolean(habilitado && url);
  const usandoPolling = Boolean(habilitado && !url && pollingMs > 0);

  /* ------------------------------ SSE ------------------------------ */
  useEffect(() => {
    if (!usandoSse) return undefined;

    let cancelado = false;
    let jaConectou = false;
    let tentativas = 0;
    let es = null;
    let timer = null;

    const fecharAtual = () => {
      if (es) {
        es.onopen = null;
        es.onerror = null;
        es.close();
        es = null;
      }
    };

    const rebuscar = async () => {
      if (!buscarRef.current) return;
      try {
        await buscarRef.current();
        if (!cancelado) setUltimaAtualizacao(new Date());
      } catch {
        /* a própria tela mostra o erro da busca; aqui só não derrubamos o stream */
      }
    };

    const agendarReconexao = () => {
      const atraso = BACKOFF_MS[Math.min(tentativas, BACKOFF_MS.length - 1)];
      tentativas += 1;
      timer = setTimeout(() => { if (!cancelado) conectar(); }, atraso);
    };

    const conectar = () => {
      if (cancelado) return;
      fecharAtual();
      setConexao((atual) => (atual === "desconectado" ? atual : "conectando"));

      let novo;
      try {
        novo = new EventSource(url);
      } catch {
        setConexao("desconectado");
        agendarReconexao();
        return;
      }
      es = novo;

      Object.keys(eventosRef.current || {}).forEach((nome) => {
        novo.addEventListener(nome, (ev) => {
          if (cancelado) return;
          let payload = null;
          try {
            payload = ev.data ? JSON.parse(ev.data) : null;
          } catch {
            payload = ev.data ?? null;
          }
          const fn = eventosRef.current?.[nome];
          if (fn) fn(payload, ev);
          setUltimaAtualizacao(new Date());
        });
      });

      novo.onopen = () => {
        if (cancelado) return;
        tentativas = 0;
        setConexao("conectado");
        // Reconexão: o que passou enquanto estávamos fora não volta pelo stream.
        if (jaConectou) rebuscar();
        jaConectou = true;
      };

      novo.onerror = () => {
        if (cancelado) return;
        setConexao("desconectado");
        fecharAtual();
        agendarReconexao();
      };
    };

    // Estado inicial antes mesmo do stream abrir: a tela não fica em branco
    // se o SSE demorar ou nem existir no backend.
    rebuscar();
    conectar();

    return () => {
      cancelado = true;
      clearTimeout(timer);
      fecharAtual();
    };
  }, [url, usandoSse, gatilho]);

  /* ---------------------------- Polling ---------------------------- */
  useEffect(() => {
    if (!usandoPolling) return undefined;

    let cancelado = false;
    let timer = null;

    const ciclo = async () => {
      try {
        if (buscarRef.current) await buscarRef.current();
        if (!cancelado) {
          setConexao("conectado");
          setUltimaAtualizacao(new Date());
        }
      } catch {
        if (!cancelado) setConexao("desconectado");
      } finally {
        if (!cancelado) timer = setTimeout(ciclo, pollingMs);
      }
    };

    setConexao("conectando");
    ciclo();

    return () => {
      cancelado = true;
      clearTimeout(timer);
    };
  }, [usandoPolling, pollingMs, gatilho]);

  /* --------------------------- Desligado --------------------------- */
  useEffect(() => {
    if (!habilitado) setConexao("desconectado");
  }, [habilitado]);

  return {
    conexao,
    modo: usandoSse ? "sse" : usandoPolling ? "polling" : "off",
    ultimaAtualizacao,
    marcarAtualizado,
    reconectarAgora,
  };
}
