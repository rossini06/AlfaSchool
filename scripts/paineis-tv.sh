#!/usr/bin/env bash
# Gera um token de TV para cada painel do cenario de demonstracao e imprime a
# URL pronta para abrir na parede da sala (rota /painel/<slug>?token=...).
#
# O token da TV so' aparece no momento da criacao (o backend guarda so' o hash),
# entao este script CRIA um dispositivo novo a cada execucao. Rode quando
# precisar de uma URL nova; os antigos podem ser revogados na tela de Paineis.
#
# Uso:  API=http://localhost:8085 ./scripts/paineis-tv.sh
set -uo pipefail
API="${API:-http://localhost:8085}"
B="$API/api/v1"
MYSQL_CONT="${MYSQL_CONT:-alfaschool-mysql}"
TENANT="${TENANT:-b1000000-0000-0000-0000-000000000001}"

sql(){ docker exec -i "$MYSQL_CONT" mysql --default-character-set=utf8mb4 -uroot -palfaschool123 alfaschool -N -e "$1" 2>/dev/null; }
tok=$(curl -s -X POST "$B/auth/login" -H 'Content-Type: application/json' \
      -d '{"email":"diretor@mundodosaber.com","password":"100%Alfa@"}' | sed -nE 's/.*"accessToken":"([^"]+)".*/\1/p')
[ -n "$tok" ] || { echo "!! login do diretor falhou"; exit 1; }
AUTH="Authorization: Bearer $tok"

echo "Painéis de TV (abra cada URL na TV da sala/coordenação):"
echo
while IFS=$'\t' read -r id slug tipo; do
  [ -z "$id" ] && continue
  resp=$(curl -s -X POST "$B/access/paineis/$id/dispositivos" -H "$AUTH" -H 'Content-Type: application/json' \
         -d "{\"nome\":\"TV ${slug} (demo)\"}")
  t=$(echo "$resp" | grep -oE '"token":"[^"]+"' | head -1 | cut -d'"' -f4)
  if [ -n "$t" ]; then
    printf "  %-14s %s\n" "$tipo" "$API/painel/${slug}?token=${t}"
  else
    printf "  %-14s (falha ao gerar token: %s)\n" "$tipo" "$(echo "$resp" | head -c 120)"
  fi
done < <(sql "SELECT id, slug, tipo FROM acc_paineis WHERE tenant_id='$TENANT' ORDER BY tipo")
echo
