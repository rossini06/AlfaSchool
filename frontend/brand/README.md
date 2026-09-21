# Marca do AlfaSchool

Faz parte da família **Alfa** — AlfaJornada, AlfaGym, AlfaControl e AlfaSchool.
A gramática é a mesma nos quatro; o que muda é o símbolo e a cor de acento.

| Produto | Contorno | Símbolo | Acento |
|---|---|---|---|
| AlfaJornada | quadrado azul preenchido | α-percurso entre dois marcos | `#2563EB` |
| AlfaGym | sem contorno | arco α + halter | `#F97316` |
| AlfaControl | quadrado teal vazado | pessoa (círculo + triângulo) | `#E53E3E` |
| **AlfaSchool** | **quadrado teal vazado** | **barrete de formatura** | **`#F59E0B`** |

- **Teal da família:** `#029CAF` — é o traço do quadrado e a palavra "Alfa" do wordmark.
- **Âmbar do AlfaSchool:** `#F59E0B` — é o barrete e a palavra "School".
- **Fonte do wordmark:** Righteous 400, a mesma do AlfaJornada e do AlfaControl.

## Arquivos

| Arquivo | Para quê |
|---|---|
| `alfaschool-logo.svg` | **Fonte da verdade.** Lockup completo, com a fonte embutida em base64 |
| `alfaschool-simbolo.svg` | Só o símbolo, sem o wordmark |
| `../public/alfaschool-icon.svg` | Símbolo usado pelo app (login e sidebar) |
| `../public/favicon.svg` | Símbolo com traço mais grosso, para sobreviver a 16px |
| `../public/alfaschool-logo.png` | Wordmark 2053×332, usado no login e na sidebar |

Por que o favicon tem traço mais grosso: a 16px um traço de 5.5 some. O
AlfaControl faz o mesmo (5.9 no favicon, 5.5 no ícone do app).

## Por que o wordmark é PNG e não texto

Righteous não é carregada pelo app — só o wordmark usa. Carregar uma fonte
inteira para desenhar duas palavras custa mais do que a imagem, e o texto
renderizado varia entre navegadores. O AlfaControl entrega PNG pelo mesmo
motivo.

## A regra do lockup

Está no CSS, em `.login-brand`, e vale para qualquer lugar onde os dois
apareçam juntos:

> **altura do ícone = 1,8 × a cap-height do wordmark**
> **gap = 25% da altura do ícone**

Medidas deste wordmark: PNG 2053×332 (6,184:1); a cap-height do "A" mede
294px, ou 88,55% da altura da imagem. Daí:

| largura do wordmark | altura | cap | ícone | gap |
|---|---|---|---|---|
| 220px (padrão) | 35,6px | 31,5px | **57px** | **14px** |
| 178px (≤420px de tela) | 28,8px | 25,5px | **46px** | **12px** |
| 124px (sidebar) | 20,1px | 17,8px | **32px** | **8px** |

## Como regerar o PNG

O wordmark é renderizado a partir da fonte, não desenhado à mão:

```sh
# righteous-400-latin.woff2 vem de alfajornada/frontend/public/fonts/
docker run --rm -v "$PWD":/m node:20-alpine sh -c "
  apk add --no-cache imagemagick woff2 >/dev/null
  cd /m && woff2_decompress righteous-400-latin.woff2
  magick -background none -fill '#029CAF' -font righteous-400-latin.ttf -pointsize 420 label:'Alfa'   a.png
  magick -background none -fill '#F59E0B' -font righteous-400-latin.ttf -pointsize 420 label:'School' s.png
  magick a.png s.png -background none -gravity South +append -trim +repage alfaschool-logo.png"
```

`-gravity South` alinha pela base: "AlfaSchool" não tem nenhuma letra com
descendente, então o alinhamento inferior é o alinhamento de linha de base.

## Como conferir

1. Olhar **a 16px de verdade**, na aba do navegador — não num mockup ampliado.
   É onde a maioria dos logos morre.
2. Conferir nos dois temas, claro e escuro.
3. Olhar a `LoginPage`, onde a marca aparece grande.
