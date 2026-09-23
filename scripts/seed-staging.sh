#!/usr/bin/env bash
# =====================================================================
# seed-staging.sh — poe o cenario "Colegio Mundo do Saber" no STAGING
# num comando so'. Roda DENTRO do LXC do staging (usa docker exec no
# container do banco e a API local); nao serve para o ambiente local
# (esse tem o dev.sh + seed-completo direto).
#
# Uso, no LXC 118:   cd /opt/alfaschool && ./scripts/seed-staging.sh
#
# Por que existe: o mesmo seed do local nao roda igual aqui — o staging
# tem senha propria de banco, os usuarios demo nascem com a senha do
# superadmin (que aqui e' aleatoria, nao 100%Alfa@), os PERFIS so' sao
# criados pelo PermissaoSeeder no boot (e o tenant do cenario nasce
# DEPOIS do boot), e as fotos precisam cair no volume que o backend
# serve. Este script faz essa sequencia na ordem certa.
#
# Rode UMA VEZ por banco: o cadastro pela API NAO deduplica. Para
# re-semear, resete o banco antes:
#   docker compose --env-file .env.staging -f docker-compose.staging.yml down
#   docker volume rm alfaschool_staging_mysql_data
#   docker compose --env-file .env.staging -f docker-compose.staging.yml up -d
# (ou rode este script com FORCE=1, ciente de que vai duplicar dados).
# =====================================================================
set -uo pipefail
cd "$(dirname "$0")/.."   # raiz do repo (/opt/alfaschool)

ENV_FILE="${ENV_FILE:-.env.staging}"
MYSQL_CONT="${MYSQL_CONT:-staging-mysql}"
BACKEND_CONT="${BACKEND_CONT:-staging-backend}"
API="${API:-http://localhost:80}"
FOTO_DIR="${ACCESS_FOTO_DIR:-/var/lib/alfaschool-fotos}"
BASE_SQL="scripts/seed-mundo-do-saber.sql"
TENANT='b1000000-0000-0000-0000-000000000001'
# bcrypt ($2a$, custo 10) de "100%Alfa@" — a senha demo documentada, a mesma
# do local. Fixo de proposito: qualquer hash valido dessa senha serve, e
# assim o script nao depende de ter python-bcrypt/htpasswd no LXC.
DEMO_HASH='$2a$10$l4FBCvINqhgZfcFqo/sye.ApwZwp61tdBQw0SSl/GTmH5ECE4nArS'

[ -f "$ENV_FILE" ] || { echo "!! $ENV_FILE nao encontrado — rode este script no LXC do staging (/opt/alfaschool)."; exit 1; }
[ -f "$BASE_SQL" ] || { echo "!! $BASE_SQL ausente."; exit 1; }
PW=$(grep -m1 '^MYSQL_ROOT_PASSWORD=' "$ENV_FILE" | cut -d= -f2-)
DOMAIN=$(grep -m1 '^APP_DOMAIN=' "$ENV_FILE" | cut -d= -f2-)
[ -n "$PW" ] || { echo "!! MYSQL_ROOT_PASSWORD ausente em $ENV_FILE."; exit 1; }

mysqlq(){ docker exec -i "$MYSQL_CONT" mysql --default-character-set=utf8mb4 -uroot -p"$PW" alfaschool "$@" 2>/tmp/.seedstg.err; local rc=$?; grep -viE 'Using a password' /tmp/.seedstg.err >&2 || true; return $rc; }
health(){ curl -s "$API/actuator/health" 2>/dev/null | grep -q '"status":"UP"'; }
wait_up(){ local i; for i in $(seq 1 50); do health && { echo "   backend UP"; return 0; }; sleep 6; done; return 1; }

# Simulador ligado? Sem ele os endpoints /access/simulador/* nao existem e a
# operacao ao vivo (presenca/fila/retirada) nao e' gerada.
if [ "$(docker exec "$BACKEND_CONT" printenv ACCESS_SIMULADOR 2>/dev/null)" != "true" ]; then
  echo "!! ACCESS_SIMULADOR nao esta 'true' no $BACKEND_CONT."
  echo "   O compose de staging ja define isso; recrie o backend:"
  echo "     docker compose --env-file $ENV_FILE -f docker-compose.staging.yml up -d"
  exit 1
fi

# Guarda contra re-semeadura: o cenario base tem 3 alunos; o seed cheio leva
# a >20. Se ja passou disso, seed-completo duplicaria tudo.
JA=$(mysqlq -N -e "SELECT COUNT(*) FROM alunos WHERE tenant_id='$TENANT';" 2>/dev/null | tr -dc '0-9')
if [ "${JA:-0}" -gt 5 ] && [ "${FORCE:-0}" != "1" ]; then
  echo "!! o tenant do cenario ja tem ${JA} alunos — parece semeado."
  echo "   Re-semear DUPLICA (o cadastro nao deduplica). Resete o banco antes"
  echo "   (ver cabecalho) ou rode com FORCE=1 se for proposital."
  exit 1
fi

echo "== 1/6  carrega o cenario base (tenant, unidade, portarias, paineis, usuarios) =="
mysqlq < "$BASE_SQL" >/dev/null || { echo "!! falha ao carregar $BASE_SQL"; exit 1; }

echo "== 2/6  reinicia o backend para o PermissaoSeeder criar os perfis do tenant novo =="
docker restart "$BACKEND_CONT" >/dev/null
wait_up || { echo "!! o backend nao voltou a responder UP"; exit 1; }

echo "== 3/6  recarrega o cenario base (agora liga cada usuario ao seu perfil) =="
mysqlq < "$BASE_SQL" >/dev/null || { echo "!! falha ao religar usuarios/perfis"; exit 1; }

echo "== 4/6  define a senha demo (100%Alfa@) nos usuarios do cenario =="
mysqlq -e "UPDATE users SET password='$DEMO_HASH' WHERE tenant_id='$TENANT';" >/dev/null \
  || { echo "!! falha ao definir a senha demo"; exit 1; }

echo "== 5/6  seed cheio: cadastro pela API + Access pelo simulador + fotos + paineis =="
SEED_OUT=$(mktemp)
MYSQL_PW="$PW" API="$API" MYSQL_CONT="$MYSQL_CONT" API_CONT="$BACKEND_CONT" ACCESS_FOTO_DIR="$FOTO_DIR" \
  bash scripts/seed-completo.sh 2>&1 | tee "$SEED_OUT"

echo "== 6/6  garante que o backend le as fotos do volume =="
UIDBE=$(docker exec "$BACKEND_CONT" id -u 2>/dev/null)
docker exec -u 0 "$BACKEND_CONT" sh -c "chown -R ${UIDBE:-0} '$FOTO_DIR' 2>/dev/null; chmod -R a+rX '$FOTO_DIR' 2>/dev/null" || true

# Catalogo de planos SaaS: e' da PLATAFORMA (global, nao do tenant) e so' o
# superadmin cria. Sem isto a aba Planos do painel SaaS fica vazia. Idempotente:
# so' cria se ainda nao houver plano (o slug e' unico).
echo "== extra  catalogo de planos SaaS (superadmin) =="
if [ "$(mysqlq -N -e 'SELECT COUNT(*) FROM saas_plans;' 2>/dev/null | tr -dc '0-9')" -gt 0 ]; then
  echo "   ja existem planos SaaS — nao recria."
else
  SUPERPW=$(grep -m1 '^APP_SUPERADMIN_PASSWORD=' "$ENV_FILE" | cut -d= -f2-)
  STOK=$(curl -s -X POST "$API/api/v1/auth/login" -H "Content-Type: application/json" -d "{\"email\":\"superadmin@alfaschool.com\",\"password\":\"$SUPERPW\"}" | grep -oE '"accessToken":"[^"]+' | cut -d'"' -f4)
  addplan(){ curl -s -o /dev/null -H "Authorization: Bearer $STOK" -H "Content-Type: application/json" -X POST "$API/api/v1/saas/plans" -d "$1"; }
  addplan '{"nome":"Controle de Acesso","slug":"acesso","descricao":"Retirada segura, biometria, paineis de sala e portal da familia.","maxEscolas":1,"maxUsuarios":50,"maxDispositivos":10,"recursos":"[\"ACCESS\",\"PORTAL\",\"NOTIFICACOES\"]","ativo":true}'
  addplan '{"nome":"Pedagogico","slug":"pedagogico","descricao":"Cursos, turmas, matriculas, diario, boletim e financeiro.","maxEscolas":1,"maxUsuarios":50,"maxDispositivos":0,"recursos":"[\"PEDAGOGICO\"]","ativo":true}'
  addplan '{"nome":"Plataforma Completa","slug":"completa","descricao":"Controle de acesso + pedagogico, tudo no mesmo lugar.","maxEscolas":5,"maxUsuarios":200,"maxDispositivos":30,"recursos":"[\"ACCESS\",\"PORTAL\",\"NOTIFICACOES\",\"PEDAGOGICO\"]","ativo":true}'
  echo "   planos SaaS: $(mysqlq -N -e 'SELECT COUNT(*) FROM saas_plans;' 2>/dev/null | tr -dc '0-9')"
fi

# Se o storage e' MinIO, as fotos que o popular-fotos gravou no disco NAO sao
# lidas pelo backend (ele le do bucket). Espelha o disco -> bucket.
if [ "$(docker exec "$BACKEND_CONT" printenv ACCESS_STORAGE 2>/dev/null)" = "minio" ]; then
  echo "== extra  espelhando as fotos para o bucket do MinIO =="
  MU=$(grep '^MINIO_ROOT_USER=' "$ENV_FILE" | cut -d= -f2-)
  MP=$(grep '^MINIO_ROOT_PASSWORD=' "$ENV_FILE" | cut -d= -f2-)
  MB=$(grep '^ACCESS_MINIO_BUCKET=' "$ENV_FILE" | cut -d= -f2-); MB="${MB:-alfaschool-fotos}"
  docker run --rm --network alfaschool-staging -v alfaschool_staging_fotos:/src:ro \
    --entrypoint sh quay.io/minio/mc -c "
      mc alias set m http://minio:9000 '$MU' '$MP' >/dev/null &&
      mc mb --ignore-existing m/$MB >/dev/null &&
      mc cp --recursive /src/ m/$MB/ >/dev/null 2>&1 &&
      echo \"   objetos no bucket: \$(mc ls m/$MB | wc -l)\"" || echo "   (falha ao espelhar; verifique o MinIO)"
fi

echo
echo "================= PRONTO ================="
echo "Logins demo (senha 100%Alfa@):"
echo "  diretor@ coordenador@ secretaria@ professor@ portaria@ financeiro@ responsavel@mundodosaber.com"
echo
echo "Paineis de TV (URLs publicas — abra na parede):"
if [ -n "$DOMAIN" ]; then
  grep -E '/painel/' "$SEED_OUT" | sed "s#http://localhost:80#https://${DOMAIN}#g; s#http://localhost#https://${DOMAIN}#g"
else
  grep -E '/painel/' "$SEED_OUT"
fi
rm -f "$SEED_OUT" /tmp/.seedstg.err
