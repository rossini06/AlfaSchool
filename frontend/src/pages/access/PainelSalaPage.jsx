import { useState, useEffect, useCallback, useMemo, useRef } from "react";
import { useParams, useSearchParams } from "react-router-dom";
import { Icon } from "../../components/Icon";
import { FotoPessoa } from "../../components/access/FotoPessoa";
import { IndicadorConexao } from "../../components/access/IndicadorConexao";
import { StatusRetiradaBadge } from "../../components/access/StatusRetiradaBadge";
import { useSseAoVivo } from "../../hooks/useSseAoVivo";
import {
  PAINEL_TOKEN_KEY,
  painelEstado,
  painelStreamUrl,
  normalizarEstadoPainel,
  normalizarRetirada,
  pareceEndpointAusente,
} from "../../services/accessApi";
import "../../styles/access.css";

const FINALIZADAS = ["ENTREGUE", "CANCELADA", "NEGADA", "EXPIRADA"];

/**
 * Painel de sala — a tela da Smart TV na porta da sala.
 *
 * Rota pública: /painel/:slug. Não há login: a TV se identifica com o token do
 * dispositivo, lido de `?token=` e guardado no localStorage para sobreviver a
 * uma recarga ou a uma queda de energia.
 *
 * <h2>A tela NÃO opera nada</h2>
 * Ela só informa. Não há botão, não há ação, e nada que se toque nela muda o
 * estado de uma retirada. Havia um botão "Preparar aluno para saída" na mão
 * da professora; foi retirado por decisão de operação — a sala não comanda a
 * fila.
 *
 * O cartão do aluno fica na tela, com a foto dele e a de quem veio buscar,
 * até o sistema receber a leitura do rosto dele no leitor de SAÍDA. É esse
 * evento que fecha a retirada (ver RetiradaService.marcarSaidaPorEvento) e,
 * só então, o cartão sai — depois de alguns segundos mostrando que a criança
 * passou, para a professora ver que terminou.
 *
 * Como a TV fica pendurada à vista de outras crianças e de quem passa no
 * corredor, ela é o lugar mais exposto do sistema: por isso não tem login,
 * não tem ação e o token é revogável a qualquer momento.
 */
export function PainelSalaPage() {
  const { slug } = useParams();
  const [searchParams] = useSearchParams();
  const tokenDaUrl = searchParams.get("token");

  const [token, setToken] = useState(() => {
    try {
      return localStorage.getItem(PAINEL_TOKEN_KEY) || "";
    } catch {
      return "";
    }
  });
  const [tokenDigitado, setTokenDigitado] = useState("");

  const [estado, setEstado] = useState(null);
  const [retiradas, setRetiradas] = useState([]);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState("");
  const [semBackend, setSemBackend] = useState(false);

  const [agora, setAgora] = useState(() => new Date());

  const retencaoRef = useRef(20);
  const timersRef = useRef(new Map());

  /* --------- token: da URL para o armazenamento local da TV ---------- */
  useEffect(() => {
    if (!tokenDaUrl || tokenDaUrl === token) return;
    try {
      localStorage.setItem(PAINEL_TOKEN_KEY, tokenDaUrl);
    } catch {
      /* modo privado / storage bloqueado: o token vale só para esta sessão */
    }
    setToken(tokenDaUrl);
    // A URL continua carregando o token de propósito: ela é o "bookmark" da TV
    // e precisa voltar a funcionar mesmo se o armazenamento do aparelho sumir.
  }, [tokenDaUrl, token]);

  /* --------------------------- relógio ------------------------------ */
  useEffect(() => {
    const t = setInterval(() => setAgora(new Date()), 15000);
    return () => clearInterval(t);
  }, []);

  /* ----------------- remoção de cartões já entregues ------------------ */
  const agendarSaida = useCallback((id) => {
    if (!id || timersRef.current.has(id)) return;
    const ms = Math.max(1, retencaoRef.current) * 1000;
    const t = setTimeout(() => {
      timersRef.current.delete(id);
      setRetiradas((prev) => prev.filter((r) => r.id !== id));
    }, ms);
    timersRef.current.set(id, t);
  }, []);

  useEffect(() => {
    const timers = timersRef.current;
    return () => {
      timers.forEach((t) => clearTimeout(t));
      timers.clear();
    };
  }, []);

  /* ------------------------- estado completo ------------------------- */
  const buscarEstado = useCallback(async () => {
    if (!slug || !token) return;
    try {
      const dados = await painelEstado(slug, token);
      const normalizado = normalizarEstadoPainel(dados);
      retencaoRef.current = normalizado.retencaoSeg;
      setEstado(normalizado);
      setRetiradas(normalizado.retiradas);
      setErro("");
      setSemBackend(false);
      // Uma retirada já entregue que veio no estado também precisa expirar.
      normalizado.retiradas
        .filter((r) => normalizarStatus(r.status) === "ENTREGUE")
        .forEach((r) => agendarSaida(r.id));
    } catch (err) {
      setErro(err.message || "Falha ao carregar o painel.");
      setSemBackend(pareceEndpointAusente(err));
      throw err;
    } finally {
      setCarregando(false);
    }
  }, [slug, token, agendarSaida]);

  /* ------------------------ eventos ao vivo -------------------------- */
  const aplicar = useCallback(
    (payload, statusForcado) => {
      const corpo = payload?.retirada || payload;
      const nova = normalizarRetirada(corpo);

      // Payload que não identifica a retirada: não dá para aplicar em cima do
      // que está na tela, então rebuscamos o estado inteiro.
      if (!nova?.id) {
        buscarEstado().catch(() => {});
        return;
      }

      const status = statusForcado || normalizarStatus(nova.status);
      setRetiradas((prev) => {
        const indice = prev.findIndex((r) => r.id === nova.id);
        if (indice === -1) return [...prev, { ...nova, status }];
        const copia = [...prev];
        copia[indice] = mesclarRetirada(copia[indice], { ...nova, status });
        return copia;
      });

      if (status === "ENTREGUE") agendarSaida(nova.id);
      if (status === "CANCELADA" || status === "NEGADA") {
        setRetiradas((prev) => prev.filter((r) => r.id !== nova.id));
      }
    },
    [buscarEstado, agendarSaida]
  );

  const eventos = useMemo(
    () => ({
      conectado: () => {},
      "retirada.aberta": (p) => aplicar(p, "SOLICITADA"),
      "retirada.preparando": (p) => aplicar(p, "PREPARANDO"),
      "retirada.pronto": (p) => aplicar(p, "PRONTO"),
      "retirada.entregue": (p) => aplicar(p, "ENTREGUE"),
      "retirada.cancelada": (p) => aplicar(p, "CANCELADA"),
    }),
    [aplicar]
  );

  const urlStream = useMemo(() => painelStreamUrl(slug, token), [slug, token]);

  const { conexao, ultimaAtualizacao, reconectarAgora } = useSseAoVivo({
    url: urlStream,
    eventos,
    buscarEstado,
    habilitado: Boolean(slug && token),
  });

  /* ----------------------------- derivados --------------------------- */
  const ordenadas = useMemo(() => ordenarFila(retiradas), [retiradas]);
  const destaque = ordenadas[0] || null;
  const proximas = useMemo(
    () => ordenadas.slice(1).filter((r) => !FINALIZADAS.includes(normalizarStatus(r.status))),
    [ordenadas]
  );

  /* ------------------------------ telas ------------------------------ */

  if (!token) {
    return (
      <TelaCentral
        icone="Lock"
        titulo="Painel não autorizado"
        texto="Esta TV ainda não tem um token de dispositivo. Abra o endereço do painel com o token fornecido pela coordenação, ou digite-o abaixo uma única vez."
      >
        <form
          className="painel-tv-token-form"
          onSubmit={(e) => {
            e.preventDefault();
            const limpo = tokenDigitado.trim();
            if (!limpo) return;
            try {
              localStorage.setItem(PAINEL_TOKEN_KEY, limpo);
            } catch {
              /* segue só em memória */
            }
            setToken(limpo);
          }}
        >
          <input
            value={tokenDigitado}
            onChange={(e) => setTokenDigitado(e.target.value)}
            placeholder="Token do dispositivo"
            aria-label="Token do dispositivo"
            autoComplete="off"
          />
          <button type="submit">Ativar painel</button>
        </form>
      </TelaCentral>
    );
  }

  if (carregando && !estado) {
    return <TelaCentral icone="RefreshCw" titulo="Carregando painel…" texto="Buscando a fila desta sala." />;
  }

  if (!estado && semBackend) {
    return (
      <TelaCentral
        icone="AlertCircle"
        titulo="Painel indisponível"
        texto="O controle de acesso ainda não está publicado neste servidor. A tela volta sozinha assim que o serviço subir."
      />
    );
  }

  if (!estado && erro) {
    return (
      <TelaCentral icone="WifiOff" titulo="Sem comunicação" texto={erro}>
        <div className="painel-tv-token-form" style={{ justifyContent: "center" }}>
          <button type="button" onClick={reconectarAgora}>
            Tentar de novo
          </button>
        </div>
      </TelaCentral>
    );
  }

  const localDescricao = [
    "Painel de retirada",
    estado?.salaNome || estado?.painelNome,
    estado?.unidadeNome,
  ]
    .filter(Boolean)
    .join(" · ");

  return (
    <div className="painel-tv">
      <header className="painel-tv-header">
        <div>
          <div className="painel-tv-turma">
            {estado?.turmaNome || estado?.painelNome || "Painel de retirada"}
          </div>
          <div className="painel-tv-local">{localDescricao}</div>
        </div>
        <div className="painel-tv-header-dir">
          <div className="painel-tv-relogio">
            {agora.toLocaleTimeString("pt-BR", { hour: "2-digit", minute: "2-digit" })}
          </div>
          <IndicadorConexao estado={conexao} ultimaAtualizacao={ultimaAtualizacao} tv />
        </div>
      </header>

      {erro && (
        <div className="painel-tv-erro" role="alert" style={{ marginTop: "1.2rem" }}>
          <Icon name="AlertCircle" size={22} />
          <span>{erro}</span>
        </div>
      )}

      {destaque ? (
        <main className="painel-tv-main">
          <section className="painel-tv-destaque">
            <div className="painel-tv-secao-titulo">
              <Icon name="Bell" size={20} />
              Próxima retirada
              {destaque.retiradaManual && (
                <span style={{ color: "var(--pn-text-2)", letterSpacing: 0 }}>· registro manual</span>
              )}
            </div>

            <div className="painel-tv-cartoes">
              <CartaoPessoa
                papel="Aluno"
                nome={destaque.aluno?.nome}
                foto={estado?.exibeFoto ? destaque.aluno?.foto : null}
                info={destaque.turmaNome || destaque.aluno?.turmaNome}
              />
              <CartaoPessoa
                papel="Quem veio buscar"
                nome={destaque.retirante?.nome}
                foto={estado?.exibeFoto ? destaque.retirante?.foto : null}
                info={rotuloParentesco(destaque.retirante?.parentesco)}
              />
            </div>

            <SituacaoRetirada retirada={destaque} />
          </section>

          <section>
            <div className="painel-tv-fila-titulo">
              <span>Próximas retiradas</span>
              <span className="painel-tv-fila-contador">
                {proximas.length} na fila
              </span>
            </div>

            {proximas.length === 0 ? (
              <div
                className="painel-tv-fila-item"
                style={{ justifyContent: "center", color: "var(--pn-text-2)", fontSize: "1.1rem" }}
              >
                Ninguém mais aguardando no momento.
              </div>
            ) : (
              <div className="painel-tv-fila">
                {proximas.slice(0, 6).map((r) => (
                  <div className="painel-tv-fila-item" key={r.id}>
                    <FotoPessoa
                      foto={estado?.exibeFoto ? r.aluno?.foto : null}
                      nome={r.aluno?.nome}
                      size={56}
                      tv
                    />
                    <div style={{ minWidth: 0 }}>
                      <div className="painel-tv-fila-nome">{r.aluno?.nome || "Aluno"}</div>
                      <div className="painel-tv-fila-sub">
                        {[r.retirante?.nome, rotuloParentesco(r.retirante?.parentesco)]
                          .filter(Boolean)
                          .join(" · ") || "Responsável a confirmar"}
                      </div>
                    </div>
                    <div className="painel-tv-fila-dir">
                      <StatusRetiradaBadge status={r.status} tv />
                      <div className="painel-tv-fila-sub">{horaDe(r.solicitadoEm) || "—"}</div>
                    </div>
                  </div>
                ))}
                {proximas.length > 6 && (
                  <div
                    className="painel-tv-fila-item"
                    style={{ justifyContent: "center", color: "var(--pn-text-2)" }}
                  >
                    + {proximas.length - 6} aguardando
                  </div>
                )}
              </div>
            )}
          </section>
        </main>
      ) : (
        <main className="painel-tv-main">
          <div className="painel-tv-vazio">
            <div className="painel-tv-vazio-icone">
              <Icon name="CheckCircle" size={48} />
            </div>
            <h2>Nenhuma retirada agora</h2>
            <p>
              Assim que um responsável for reconhecido na portaria, o aluno aparece aqui
              automaticamente.
            </p>
          </div>
        </main>
      )}

      <footer className="painel-tv-rodape">
        <span>As fotografias são exibidas somente em dispositivos autorizados.</span>
      </footer>
    </div>
  );
}

/* ============================ subcomponentes ============================ */

function CartaoPessoa({ papel, nome, foto, info }) {
  return (
    <div className="painel-tv-cartao">
      <FotoPessoa foto={foto} nome={nome} size={124} tv />
      <div className="painel-tv-cartao-txt">
        <div className="painel-tv-cartao-papel">{papel}</div>
        <div className="painel-tv-cartao-nome">{nome || "Não identificado"}</div>
        {info && <div className="painel-tv-cartao-info">{info}</div>}
      </div>
    </div>
  );
}

/**
 * Linha de situação — informa, não age.
 *
 * Era um <button> que a professora tocava para avisar a coordenação. Virou
 * um bloco de leitura: a TV da sala não comanda a fila, e o que move a
 * retirada é o rosto da criança no leitor de saída.
 */
function SituacaoRetirada({ retirada }) {
  const status = normalizarStatus(retirada.status);
  const chegada = horaDe(retirada.solicitadoEm);
  const legendaChegada = chegada ? `Responsável chegou às ${chegada}` : "Responsável na portaria";

  if (status === "ENTREGUE") {
    const saida = horaDe(retirada.saidaEm) || horaDe(retirada.entregueEm);
    return (
      <div className="painel-tv-botao is-feito" role="status">
        <span>Aluno saiu</span>
        <span className="painel-tv-botao-legenda">
          {saida ? `Rosto lido na saída às ${saida}` : "Saída registrada"}
        </span>
      </div>
    );
  }

  return (
    <div className="painel-tv-botao is-aguardando" role="status">
      <span>Aguardando o aluno na saída</span>
      <span className="painel-tv-botao-legenda">{legendaChegada}</span>
    </div>
  );
}

function TelaCentral({ icone, titulo, texto, children }) {
  return (
    <div className="painel-tv">
      <div className="painel-tv-centro">
        <div className="painel-tv-vazio-icone">
          <Icon name={icone} size={48} />
        </div>
        <h1>{titulo}</h1>
        <p>{texto}</p>
        {children}
      </div>
      <footer className="painel-tv-rodape">
        <span>As fotografias são exibidas somente em dispositivos autorizados.</span>
      </footer>
    </div>
  );
}

/* ================================ helpers =============================== */

function normalizarStatus(status) {
  if (!status) return "";
  return String(status)
    .normalize("NFD")
    .replace(/[̀-ͯ]/g, "")
    .toUpperCase()
    .replace(/[^A-Z]/g, "");
}

function ordenarFila(lista) {
  return [...lista].sort((a, b) => {
    const oa = a.ordemChegada ?? Number.MAX_SAFE_INTEGER;
    const ob = b.ordemChegada ?? Number.MAX_SAFE_INTEGER;
    if (oa !== ob) return oa - ob;
    return String(a.solicitadoEm || "").localeCompare(String(b.solicitadoEm || ""));
  });
}

function mesclarRetirada(anterior, nova) {
  if (!anterior) return nova;
  const saida = { ...anterior };
  Object.entries(nova).forEach(([chave, valor]) => {
    if (valor === null || valor === undefined || valor === "") return;
    if (chave === "aluno" || chave === "retirante") {
      const limpo = Object.fromEntries(
        Object.entries(valor).filter(([, v]) => v !== null && v !== undefined && v !== "")
      );
      saida[chave] = { ...(anterior[chave] || {}), ...limpo };
    } else {
      saida[chave] = valor;
    }
  });
  return saida;
}

function horaDe(iso) {
  if (!iso) return null;
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) return null;
  return d.toLocaleTimeString("pt-BR", { hour: "2-digit", minute: "2-digit" });
}

function rotuloParentesco(parentesco) {
  if (!parentesco) return null;
  const texto = String(parentesco).replace(/_/g, " ").toLowerCase();
  const capitalizado = texto.charAt(0).toUpperCase() + texto.slice(1);
  return `${capitalizado} autorizado`;
}
