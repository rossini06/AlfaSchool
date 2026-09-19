/**
 * Busca do tutorial por PERGUNTA, não por palavra exata.
 *
 * <h2>O problema</h2>
 * Substring simples só acha quem já conhece o vocabulário do sistema. Quem
 * digita "por que não consigo entregar o aluno" não acha nada, porque
 * ninguém escreveu essa frase — e é justamente quem mais precisa de ajuda.
 *
 * <h2>Como funciona</h2>
 * Normaliza (acento, caixa), joga fora palavra vazia ("por", "que", "não")
 * e pontua cada resposta pelo número de palavras da pergunta que ela cobre.
 * Palavra do SINTOMA vale mais que palavra do texto: sintoma é como a
 * pessoa fala quando está com o problema na frente.
 *
 * <h2>Por que não é um modelo de linguagem</h2>
 * Este sistema decide se uma criança pode sair com um adulto. Uma resposta
 * inventada com confiança — "sim, a coordenação pode liberar" — é pior do
 * que "não encontrei". Aqui nada é gerado: a busca só ESCOLHE entre
 * respostas escritas e conferidas. Quando não tem certeza, ela diz que não
 * achou, em vez de arriscar.
 */

const VAZIAS = new Set([
  "a", "as", "o", "os", "um", "uma", "de", "do", "da", "dos", "das", "em", "no", "na",
  "nos", "nas", "por", "que", "porque", "pra", "para", "com", "sem", "se", "e", "ou",
  "mas", "meu", "minha", "eu", "ele", "ela", "isso", "esse", "essa", "esta", "este",
  "ao", "aos", "the", "como", "qual", "quais", "quando", "onde", "ser", "estar", "ter",
  "foi", "e'", "é", "ja", "já", "nao", "não", "mais", "muito", "tudo", "todo", "toda",
]);

/** Tira acento e caixa: "restrição" e "restricao" precisam casar. */
export function normalizar(texto) {
  return String(texto || "")
    .toLowerCase()
    .normalize("NFD")
    .replace(/[̀-ͯ]/g, "")
    .replace(/[^a-z0-9\s]/g, " ")
    .replace(/\s+/g, " ")
    .trim();
}

/**
 * Palavras que valem para a busca.
 *
 * Corta as vazias e as de uma letra. Se sobrar nada — alguém digitou só
 * "por que não" — devolve a frase inteira, porque nesse caso é melhor
 * procurar literalmente do que não procurar.
 */
export function palavras(texto) {
  const lista = normalizar(texto)
    .split(" ")
    .filter((p) => p.length > 2 && !VAZIAS.has(p));
  return lista.length > 0 ? lista : normalizar(texto).split(" ").filter(Boolean);
}

/**
 * Casamento com raiz: "entregar" casa com "entrega" e "entregue";
 * "restrição" com "restricoes". Evita exigir que a pessoa acerte a
 * conjugação.
 */
function cobre(campo, palavra) {
  if (campo.includes(palavra)) return true;
  if (palavra.length >= 6) {
    const raiz = palavra.slice(0, Math.max(5, palavra.length - 2));
    return campo.includes(raiz);
  }
  return false;
}

/**
 * Pontua as perguntas frequentes contra o que foi digitado.
 *
 * O piso de 40% existe para a busca CALAR quando não entendeu. Devolver a
 * resposta errada com ar de certeza é pior do que dizer "não achei" — quem
 * lê está resolvendo um problema real na portaria.
 */
export function perguntasQueCasam(perguntas, consulta, { minimo = 0.4, maximo = 4 } = {}) {
  const termos = palavras(consulta);
  if (termos.length === 0) return [];

  const pontuadas = perguntas
    .map((p) => {
      const sintomas = normalizar((p.sintomas || []).join(" "));
      const titulo = normalizar(p.pergunta);
      const corpo = normalizar(p.resposta);

      let pontos = 0;
      let cobertos = 0;
      termos.forEach((t) => {
        // Sintoma vale mais: é como a pessoa fala com o problema na frente.
        if (cobre(sintomas, t)) { pontos += 3; cobertos++; return; }
        if (cobre(titulo, t)) { pontos += 2; cobertos++; return; }
        if (cobre(corpo, t)) { pontos += 1; cobertos++; }
      });

      return { pergunta: p, pontos, cobertura: cobertos / termos.length };
    })
    .filter((x) => x.cobertura >= minimo && x.pontos > 0)
    .sort((a, b) => b.pontos - a.pontos || b.cobertura - a.cobertura);

  return pontuadas.slice(0, maximo).map((x) => x.pergunta);
}

/** Mesma lógica, aplicada ao texto das telas. */
export function telaCasa(textoDaTela, consulta, { minimo = 0.35 } = {}) {
  const termos = palavras(consulta);
  if (termos.length === 0) return true;
  const campo = normalizar(textoDaTela);
  const cobertos = termos.filter((t) => cobre(campo, t)).length;
  return cobertos / termos.length >= minimo;
}
