#!/usr/bin/env bash
# Popula fotos condizentes com idade e sexo no cenario de demonstracao.
#
#   - Alunos            -> avatar ilustrado (DiceBear "adventurer"), cabelo por
#                          sexo -> gravado como data-URI em alunos.foto
#   - Responsaveis      -> foto real de adulto (randomuser) -> data-URI em
#                          responsaveis.foto
#   - Pessoas autorizadas -> foto real de adulto -> arquivo no FotoStorage
#   - acc_faces (ALUNO/AUTORIZADA) -> mesma imagem via foto_key, para os
#                          paineis de sala, a fila de retirada e a TV.
#
# Por que a foto do painel e' arquivo em disco e nao data-URI: o pipeline de
# acesso guarda so' a CHAVE (acc_faces.foto_key) e serve a imagem por uma URL
# HMAC assinada (GET /api/v1/access/fotos/{chave}). A imagem em bytes vive no
# FotoStorage (dir ${ACCESS_FOTO_DIR:-/tmp/alfaschool-fotos} dentro do
# container da API).
#
# ARMADILHA: a chave NAO pode conter "/". O URLEncoder do backend transforma a
# barra em %2F e o StrictHttpFirewall/Tomcat rejeitam a URL com 400 (a app sobe
# sem ALLOW_ENCODED_SLASH). Por isso as chaves aqui sao planas: "aluno-<id>.png".
#
# Uso:
#   ./scripts/popular-fotos.sh
#   MYSQL_CONT=... API_CONT=... TENANT=... ./scripts/popular-fotos.sh
#
# Requer rede (api.dicebear.com e randomuser.me). Best-effort: se um download
# falhar, aquela pessoa fica sem foto e o script segue.
set -uo pipefail

MYSQL_CONT="${MYSQL_CONT:-alfaschool-mysql}"
MYSQL_PW="${MYSQL_PW:-alfaschool123}"   # staging usa senha propria; passe MYSQL_PW=...
API_CONT="${API_CONT:-$(docker ps --format '{{.Names}}' 2>/dev/null | grep -Ei 'alfaschool.*(api|backend)|staging-backend' | head -1)}"
API_CONT="${API_CONT:-alfaschool-api-dev}"
TENANT="${TENANT:-b1000000-0000-0000-0000-000000000001}"
FOTO_DIR="${ACCESS_FOTO_DIR:-/tmp/alfaschool-fotos}"

OUT="$(mktemp -d)"
DEMO="$OUT/flat"        # arquivos planos que vao para a raiz do FotoStorage
mkdir -p "$DEMO"
SQLF="$OUT/updates.sql"
PARF="$OUT/parentesco.sql"      # overrides de parentesco aplicados por ULTIMO
printf 'SET NAMES utf8mb4;\n' > "$SQLF"
: > "$PARF"
trap 'rm -rf "$OUT"' EXIT

sql(){ docker exec -i "$MYSQL_CONT" mysql --default-character-set=utf8mb4 -uroot -p"$MYSQL_PW" alfaschool -N -e "$1" 2>/dev/null; }

# Conjunto feminino explicito (nomes reais do seed); o resto e' masculino.
FEM=" Adriana Alice Ana Antonella Aurora Beatriz Cecilia Cristina Eloa Fernanda Helena Heloisa Isabella Julia Juliana Laura Liz Livia Luciana Luiza Maite Maitê Manuela Maria Mariana Patricia Renata Simone Sofia Sophia Valentina Vanessa "
sexo_nome(){ case "$FEM" in *" $1 "*) echo F;; *) echo M;; esac; }

# Estilo "personas": flat e sobrio (nao cartunesco). Sexo pelo cabelo.
FEM_HAIR="long,extraLong,bobCut,bobBangs,curlyBun,straightBun,pigtails,bunUndercut,curly"
MAL_HAIR="buzzcut,fade,shortCombover,shortComboverChops,mohawk,sideShave,curlyHighTop,cap"
MOUTH="smile,bigSmile,lips,smirk"   # so' expressoes agradaveis (nada de frown/surprise/pacifier)
BG="dbe6f0,e8edf4,eef2f7,f2ece4,ffe9ec,e7f3ee,fff3d6"

is_png(){ [ "$(od -An -tx1 -N4 "$1" 2>/dev/null | tr -d ' \n')" = "89504e47" ]; }
is_jpg(){ [ "$(od -An -tx1 -N3 "$1" 2>/dev/null | tr -d ' \n')" = "ffd8ff" ]; }

mIdx=0; wIdx=0
adulto_foto(){ # $1=sexo(F/M) $2=arquivo destino
  local grp idx
  if [ "$1" = "F" ]; then grp=women; idx=$((wIdx%100)); wIdx=$((wIdx+1)); else grp=men; idx=$((mIdx%100)); mIdx=$((mIdx+1)); fi
  curl -s --retry 2 --max-time 25 -o "$2" "https://randomuser.me/api/portraits/${grp}/${idx}.jpg"
}

# ---- Fonte da foto de aluno: Pexels (foto REAL) quando PEXELS_KEY existe; ----
# ---- senao cai para avatar ilustrado (DiceBear personas).                 ----
# Fotos reais de crianca vem de banco de stock (modelos licenciados), nunca de
# aluno real — a regra LGPD (biometria de menor) e' sobre dado de aluno de
# verdade, nao sobre foto de demonstracao.
ALUNO_EXT="png"; ALUNO_MIME="image/png"; USAR_PEXELS=0
declare -a POOL_M POOL_F; pxi_m=0; pxi_f=0
# Pool de URLs Pexels por sexo. Duas formas:
#  1) PEXELS_BOYS_FILE / PEXELS_GIRLS_FILE: arquivos com uma URL por linha
#     (util quando a chave nao pode ir para o shell — ex.: coletada no navegador).
#  2) PEXELS_KEY: busca na API por termos de crianca.
px_urls(){ local q; for q in "$@"; do
    curl -s -H "Authorization: $PEXELS_KEY" \
      "https://api.pexels.com/v1/search?query=$(printf '%s' "$q" | sed 's/ /%20/g')&per_page=80&orientation=square"
    sleep 0.3
  done | grep -oE '"original":"[^"]+"' | sed 's/"original":"//; s/"$//' | awk '!seen[$0]++'; }
# Por padrao usa as listas de URLs versionadas no repo (foto real, sem chave).
: "${PEXELS_BOYS_FILE:=$(cd "$(dirname "$0")" && pwd)/fotos-alunos-meninos.txt}"
: "${PEXELS_GIRLS_FILE:=$(cd "$(dirname "$0")" && pwd)/fotos-alunos-meninas.txt}"
if [ -f "${PEXELS_BOYS_FILE:-/nao}" ] && [ -f "${PEXELS_GIRLS_FILE:-/nao}" ]; then
  echo ">> pool de fotos reais de crianca (listas versionadas)"
  POOL_M=( $(grep -vE '^[[:space:]]*(#|$)' "$PEXELS_BOYS_FILE") )
  POOL_F=( $(grep -vE '^[[:space:]]*(#|$)' "$PEXELS_GIRLS_FILE") )
elif [ -n "${PEXELS_KEY:-}" ]; then
  echo ">> montando pool de fotos reais de crianca (Pexels API)"
  POOL_M=( $(px_urls "boy child portrait" "little boy face" "child boy smiling") )
  POOL_F=( $(px_urls "girl child portrait" "little girl face" "child girl smiling") )
fi
if [ ${#POOL_M[@]} -ge 3 ] && [ ${#POOL_F[@]} -ge 3 ]; then
  USAR_PEXELS=1; ALUNO_EXT="jpg"; ALUNO_MIME="image/jpeg"
  echo "   meninos=${#POOL_M[@]} meninas=${#POOL_F[@]} (foto real)"
elif [ -n "${PEXELS_KEY:-}${PEXELS_BOYS_FILE:-}" ]; then
  echo "   !! pool insuficiente — usando avatar ilustrado"
fi

foto_aluno(){ # $1=sexo(F/M) $2=arquivo $3=id (seed do fallback)
  if [ "$USAR_PEXELS" = "1" ]; then
    local url
    if [ "$1" = "F" ]; then url="${POOL_F[$((pxi_f % ${#POOL_F[@]}))]}"; pxi_f=$((pxi_f+1));
    else url="${POOL_M[$((pxi_m % ${#POOL_M[@]}))]}"; pxi_m=$((pxi_m+1)); fi
    # recorte quadrado centrado no rosto pela CDN do Pexels
    curl -s --retry 2 --max-time 25 -o "$2" "${url}?auto=compress&cs=tinysrgb&fit=crop&w=256&h=256"
    is_jpg "$2"
  else
    local hair; [ "$1" = "F" ] && hair="$FEM_HAIR" || hair="$MAL_HAIR"
    curl -s --retry 2 --max-time 25 -o "$2" \
      "https://api.dicebear.com/9.x/personas/png?seed=${3}&size=200&hair=${hair}&mouth=${MOUTH}&facialHairProbability=0&backgroundColor=${BG}"
    is_png "$2"
  fi
}

warn=0
echo ">> alunos ($([ "$USAR_PEXELS" = 1 ] && echo 'foto real Pexels' || echo 'avatar ilustrado'))"; na=0
while IFS=$'\t' read -r id sexo nome; do
  [ -z "$id" ] && continue
  first="${nome%% *}"; sx="$sexo"
  [ "$sx" = "M" ] || [ "$sx" = "F" ] || sx=$(sexo_nome "$first")
  f="$DEMO/aluno-${id}.${ALUNO_EXT}"
  if ! foto_aluno "$sx" "$f" "$id"; then echo "  WARN aluno $id ($first) sem foto"; warn=$((warn+1)); continue; fi
  b64=$(openssl base64 -A -in "$f")
  printf "UPDATE alunos SET foto='data:%s;base64,%s' WHERE id='%s';\n" "$ALUNO_MIME" "$b64" "$id" >> "$SQLF"
  na=$((na+1))
done < <(sql "SELECT id, COALESCE(sexo,''), nome FROM alunos WHERE tenant_id='$TENANT' AND (deleted=FALSE OR deleted IS NULL)")
echo "  $na alunos"

echo ">> responsaveis (foto adulto)"; nr=0
while IFS=$'\t' read -r id nome; do
  [ -z "$id" ] && continue
  first="${nome%% *}"; sx=$(sexo_nome "$first")
  f="$OUT/resp-${id}.jpg"; adulto_foto "$sx" "$f"
  if ! is_jpg "$f"; then echo "  WARN resp $id ($first) sem JPG"; warn=$((warn+1)); continue; fi
  b64=$(openssl base64 -A -in "$f")
  # grava tambem o sexo (o cadastro nasce sem) para o parentesco sair certo
  printf "UPDATE responsaveis SET sexo='%s', foto='data:image/jpeg;base64,%s' WHERE id='%s';\n" "$sx" "$b64" "$id" >> "$SQLF"
  nr=$((nr+1))
done < <(sql "SELECT id, nome FROM responsaveis WHERE tenant_id='$TENANT' AND (deleted=FALSE OR deleted IS NULL)")
echo "  $nr responsaveis"

echo ">> pessoas autorizadas (foto adulto -> arquivo)"; np=0
while IFS=$'\t' read -r id nome respid; do
  [ -z "$id" ] && continue
  first="${nome%% *}"; sx=$(sexo_nome "$first")
  f="$DEMO/pa-${id}.jpg"; adulto_foto "$sx" "$f"
  if ! is_jpg "$f"; then echo "  WARN pa $id ($first) sem JPG"; warn=$((warn+1)); continue; fi
  # A TV mostra o NOME desta pessoa autorizada, mas o parentesco vem do
  # aluno_responsaveis (via responsavel_id). Alinho o parentesco ao SEXO de
  # quem aparece, senao sai "Simone ... Pai autorizado".
  if [ -n "$respid" ]; then
    par=$([ "$sx" = "F" ] && echo "Mãe" || echo "Pai")
    printf "UPDATE aluno_responsaveis SET parentesco='%s' WHERE tenant_id='%s' AND responsavel_id='%s';\n" "$par" "$TENANT" "$respid" >> "$PARF"
  fi
  np=$((np+1))
done < <(sql "SELECT id, nome, COALESCE(responsavel_id,'') FROM acc_pessoas_autorizadas WHERE tenant_id='$TENANT' AND (deleted=FALSE OR deleted IS NULL)")
echo "  $np pessoas autorizadas"

# Parentesco do responsavel principal condizente com o sexo (o seed grava
# "Mae" fixo, o que deixa homem rotulado como "Mae" na fila/TV).
printf "UPDATE aluno_responsaveis ar JOIN responsaveis r ON r.id=ar.responsavel_id SET ar.parentesco = IF(r.sexo='F','Mãe','Pai') WHERE ar.tenant_id='%s';\n" "$TENANT" >> "$SQLF"

# Chaves planas para os paineis/fila/TV (os arquivos ja foram gravados acima).
{
  printf "UPDATE acc_faces SET foto_key=CONCAT('aluno-', titular_id, '.%s') WHERE tenant_id='%s' AND titular_tipo='ALUNO';\n" "$ALUNO_EXT" "$TENANT"
  printf "UPDATE acc_faces SET foto_key=CONCAT('pa-', titular_id, '.jpg') WHERE tenant_id='%s' AND titular_tipo='AUTORIZADA';\n" "$TENANT"
  printf "UPDATE acc_pessoas_autorizadas SET foto_key=CONCAT('pa-', id, '.jpg') WHERE tenant_id='%s';\n" "$TENANT"
} >> "$SQLF"

# Overrides de parentesco por ULTIMO: sobrepoem o baseline (por sexo do
# responsavel) alinhando ao sexo da pessoa autorizada que a TV exibe.
cat "$PARF" >> "$SQLF"

echo ">> aplicando UPDATEs no banco ($MYSQL_CONT)"
docker exec -i "$MYSQL_CONT" mysql --default-character-set=utf8mb4 -uroot -p"$MYSQL_PW" alfaschool < "$SQLF"

echo ">> copiando arquivos para o FotoStorage ($API_CONT:$FOTO_DIR)"
if docker exec "$API_CONT" mkdir -p "$FOTO_DIR" 2>/dev/null; then
  # limpa fotos antigas do demo (ex.: aluno-*.png ao trocar para .jpg do Pexels)
  docker exec "$API_CONT" sh -c "rm -f $FOTO_DIR/aluno-* $FOTO_DIR/pa-*" 2>/dev/null || true
  docker cp "$DEMO/." "$API_CONT":"$FOTO_DIR"/ >/dev/null 2>&1 \
    && echo "  $(docker exec "$API_CONT" sh -c "ls '$FOTO_DIR' | wc -l" 2>/dev/null) arquivos no container" \
    || echo "  WARN: falha ao copiar para $API_CONT (paineis ficam sem foto; listas ok)"
else
  echo "  WARN: container '$API_CONT' indisponivel; paineis ficam sem foto (listas ok)"
fi

echo ">> fotos: $na alunos, $nr responsaveis, $np pessoas autorizadas (WARN: $warn)"
