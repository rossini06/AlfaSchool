# AlfaSchool — sistema de interface

Fonte da verdade para telas novas. Os tokens vivem em
`frontend/src/styles/global.css`; este arquivo diz **como usá-los** e o que
já está decidido. Quando uma regra daqui e o CSS divergirem, corrija o CSS.

## Direção

Dois painéis, uma família:

- **Painel da escola** — denso e calmo, teal (`--color-brand`) como único
  acento. Quem usa está no balcão da secretaria, na portaria ou na
  coordenação, com pressa e interrupções. Nada grita; hierarquia por
  tamanho e peso, não por cor.
- **Painel SaaS** (`/saas`) — a Alfa olhando a carteira. Mesmos tokens, mais
  ar. O âmbar (`--color-warning`) marca a identidade do painel (badge
  "PAINEL SAAS", monograma da Alfa) e **nada mais**; o teal continua sendo o
  acento do que está contratado.

Fonte: Inter Variable, carregada em `main.jsx`. Não trocar por tela.

## Profundidade: bordas, não sombras

Uma estratégia só: **superfície + borda de 1px**. `--shadow-*` existe para
dropdown, toast e modal (coisas que flutuam), não para card.

| Camada | Token | Uso |
|---|---|---|
| página | `--color-bg` | fundo do conteúdo e da sidebar (mesma cor; a sidebar se separa por borda) |
| card | `--color-bg-2` | card, header, modal |
| inset / hover | `--color-bg-3` | input, linha em hover, chip, trilho de barra |

Dentro de um card, separe seções com **filete** (`border-top: 1px solid
var(--color-border)`), nunca com outro card.

## Raio: uma escala só

Escrita no topo do `global.css`. Literal (8px, 12px, 999px, 0.5rem) não
entra em regra nova.

| token | px | onde |
|---|---|---|
| `--radius-xs` | 4 | tag, amostra de cor, anel de foco, scrollbar |
| `--radius-sm` | 6 | input, botão, ícone pequeno, célula, chip de menu |
| `--radius-md` | 10 | item de menu, dropdown, aviso, caixa de módulo |
| `--radius-lg` | 14 | card, tabela, modal |
| `--radius-xl` | 20 | card do login, sala de espera |
| `--radius-full` | pílula | badge, switch, barra de progresso, chip de filtro |

## Espaçamento

Base **4px**. Dentro de card: 18–20px de padding; entre cards: 14–16px;
entre seções: 20–24px. Header e sidebar: `--header-height` (64px).

## Tipografia

- Título de página: `.page-title` (20px/700). **Nunca** `<h1>` solto.
- Subtítulo: `.page-subtitle` (13px, `--color-text-2`).
- Título de card: 14–15px/700. Rótulo de tabela: 11px caixa alta, +0.6px.
- Número herói (carteira, preço): 30px/800, `letter-spacing: -0.02em`,
  `font-variant-numeric: tabular-nums`.
- Quatro níveis de texto: `--color-text`, `--color-text-2`, e os dois
  apelidos (`--color-text-muted` = text-2). Placeholder e desabilitado usam
  opacidade sobre text-2, não uma cor nova.

## Cor com significado

- Teal = ação primária e "contratado/ativo no sentido de produto".
- Verde = ok/ativa; vermelho = suspensa/perigo; âmbar = atenção e
  identidade do SaaS; azul = informação.
- Status é **ponto + palavra** (`.saas-status`), não badge colorido de
  fundo. Badge de fundo (`.badge-*`) fica para contagem e rótulo curto.
- Tema escuro é o padrão e tem `color-scheme: dark`; controles nativos
  (date, select, scrollbar) seguem o tema.

## Padrões prontos (reuse, não reinvente)

**Card de entidade** (`.perfil-card`, `.saas-rede`, `.saas-plano`): topo com
ícone/monograma + título + status; corpo curto; rodapé com filete e ações
alinhadas à direita (`.td-actions`). Grid `auto-fill, minmax(280–320px, 1fr)`.

**Régua de módulos** (`ReguaModulos.jsx`): cinco casas na ordem de
`MODULOS` em `utils/saas.js`. Casa preenchida = `--color-brand-dim` +
texto teal; vazia = borda tracejada a 55%. Estreita (<320px, container
query) vira ícone; `compacta` vira cinco traços de 14×6px. Use sempre que
a tela falar de módulos contratados.

**Monograma** (`Monograma.jsx`): iniciais numa moeda tingida; cor estável
pelo nome (`tomDoMonograma`). Substitui logo que não temos.

**Medidor** (`.perfil-card-medidor`, `.saas-cobertura-barra`): trilho
`--color-bg-3` de 6px, preenchimento teal, `--radius-full`. Sempre com o
número ao lado; barra sem número não informa.

**Números em linha** (`.saas-carteira-numeros`): três ou quatro números
separados por filete vertical dentro de UM card. Não faça um card por
número quando os números são poucos e pequenos.

**Filtros** (`.saas-filtros`): busca com ícone à esquerda + chips
`--radius-full`; chip ativo inverte (`--color-text` sobre `--color-bg`).

**Modal**: rodapé via prop `footer` do `Modal.jsx` (fica na barra com
fundo `--color-bg-3`, ações à direita). Resumo do que está marcado à
esquerda do rodapé (`.perm-resumo`). Seções curtas com `.saas-form-secao`
em vez de abas quando são dois blocos pequenos.

**Módulo não contratado / permissão ausente**: tela inteira centrada
(`.modulo-ausente`), ícone em `--color-brand-dim`, texto que explica o que
fazer. Nunca lista vazia com 403 no console.

**Marca**: lockup ícone + wordmark na regra de `frontend/brand/README.md`
(ícone = 1,8 × cap-height, gap = 25% do ícone). 124px de wordmark → ícone
32px, gap 8px. Mesmos dois arquivos em login, sidebar e SaaS.

## Não faça

- `alert()`/`confirm()` nativos; use `Modal.jsx`/`ConfirmarModal.jsx`.
- Cor de fundo diferente para a sidebar.
- Sombra em card. Gradiente decorativo. Segundo acento.
- Tabela para menos de ~10 itens com poucos campos: card lê melhor.
- Badge colorido para situação: ponto + palavra.
- Campo que a API não conhece (aconteceu duas vezes no SaaS). Leia o DTO
  antes de desenhar o formulário.
