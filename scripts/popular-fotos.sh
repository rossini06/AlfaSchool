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
API_CONT="${API_CONT:-$(docker ps --format '{{.Names}}' 2>/dev/null | grep -Ei 'alfaschool.*(api|backend)' | head -1)}"
API_CONT="${API_CONT:-alfaschool-api-dev}"
TENANT="${TENANT:-b1000000-0000-0000-0000-000000000001}"
FOTO_DIR="${ACCESS_FOTO_DIR:-/tmp/alfaschool-fotos}"

OUT="$(mktemp -d)"
DEMO="$OUT/flat"        # arquivos planos que vao para a raiz do FotoStorage
mkdir -p "$DEMO"
SQLF="$OUT/updates.sql"
printf 'SET NAMES utf8mb4;\n' > "$SQLF"
trap 'rm -rf "$OUT"' EXIT

sql(){ docker exec -i "$MYSQL_CONT" mysql --default-character-set=utf8mb4 -uroot -palfaschool123 alfaschool -N -e "$1" 2>/dev/null; }

# Conjunto feminino explicito (nomes reais do seed); o resto e' masculino.
FEM=" Adriana Alice Ana Aurora Beatriz Cecilia Cristina Eloa Fernanda Isabella Julia Juliana Laura Livia Luiza Maite Maitê Manuela Maria Mariana Patricia Renata Simone Sofia Valentina Vanessa "
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

warn=0
echo ">> alunos (avatar ilustrado)"; na=0
while IFS=$'\t' read -r id sexo nome; do
  [ -z "$id" ] && continue
  first="${nome%% *}"; sx="$sexo"
  [ "$sx" = "M" ] || [ "$sx" = "F" ] || sx=$(sexo_nome "$first")
  if [ "$sx" = "F" ]; then hair="$FEM_HAIR"; else hair="$MAL_HAIR"; fi
  f="$DEMO/aluno-${id}.png"
  curl -s --retry 2 --max-time 25 -o "$f" \
    "https://api.dicebear.com/9.x/personas/png?seed=${id}&size=200&hair=${hair}&mouth=${MOUTH}&facialHairProbability=0&backgroundColor=${BG}"
  if ! is_png "$f"; then echo "  WARN aluno $id ($first) sem PNG"; warn=$((warn+1)); continue; fi
  b64=$(openssl base64 -A -in "$f")
  printf "UPDATE alunos SET foto='data:image/png;base64,%s' WHERE id='%s';\n" "$b64" "$id" >> "$SQLF"
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
  printf "UPDATE responsaveis SET foto='data:image/jpeg;base64,%s' WHERE id='%s';\n" "$b64" "$id" >> "$SQLF"
  nr=$((nr+1))
done < <(sql "SELECT id, nome FROM responsaveis WHERE tenant_id='$TENANT' AND (deleted=FALSE OR deleted IS NULL)")
echo "  $nr responsaveis"

echo ">> pessoas autorizadas (foto adulto -> arquivo)"; np=0
while IFS=$'\t' read -r id nome; do
  [ -z "$id" ] && continue
  first="${nome%% *}"; sx=$(sexo_nome "$first")
  f="$DEMO/pa-${id}.jpg"; adulto_foto "$sx" "$f"
  if ! is_jpg "$f"; then echo "  WARN pa $id ($first) sem JPG"; warn=$((warn+1)); continue; fi
  np=$((np+1))
done < <(sql "SELECT id, nome FROM acc_pessoas_autorizadas WHERE tenant_id='$TENANT' AND (deleted=FALSE OR deleted IS NULL)")
echo "  $np pessoas autorizadas"

# Chaves planas para os paineis/fila/TV (os arquivos ja foram gravados acima).
{
  printf "UPDATE acc_faces SET foto_key=CONCAT('aluno-', titular_id, '.png') WHERE tenant_id='%s' AND titular_tipo='ALUNO';\n" "$TENANT"
  printf "UPDATE acc_faces SET foto_key=CONCAT('pa-', titular_id, '.jpg') WHERE tenant_id='%s' AND titular_tipo='AUTORIZADA';\n" "$TENANT"
  printf "UPDATE acc_pessoas_autorizadas SET foto_key=CONCAT('pa-', id, '.jpg') WHERE tenant_id='%s';\n" "$TENANT"
} >> "$SQLF"

echo ">> aplicando UPDATEs no banco ($MYSQL_CONT)"
docker exec -i "$MYSQL_CONT" mysql --default-character-set=utf8mb4 -uroot -palfaschool123 alfaschool < "$SQLF"

echo ">> copiando arquivos para o FotoStorage ($API_CONT:$FOTO_DIR)"
if docker exec "$API_CONT" mkdir -p "$FOTO_DIR" 2>/dev/null; then
  docker cp "$DEMO/." "$API_CONT":"$FOTO_DIR"/ >/dev/null 2>&1 \
    && echo "  $(docker exec "$API_CONT" sh -c "ls '$FOTO_DIR' | wc -l" 2>/dev/null) arquivos no container" \
    || echo "  WARN: falha ao copiar para $API_CONT (paineis ficam sem foto; listas ok)"
else
  echo "  WARN: container '$API_CONT' indisponivel; paineis ficam sem foto (listas ok)"
fi

echo ">> fotos: $na alunos, $nr responsaveis, $np pessoas autorizadas (WARN: $warn)"
