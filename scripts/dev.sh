#!/usr/bin/env bash
# =====================================================================
# AlfaSchool — ambiente de desenvolvimento local (WSL)
#
# Por que este script existe em vez de "docker compose up":
# o Docker Desktop, ao montar o contexto de build a partir do filesystem
# do WSL, serve uma copia obsoleta dos arquivos — o jar saia com uma
# migration antiga mesmo apos "build --no-cache". Bind mount le' o disco
# ao vivo e nao tem esse problema, entao compilamos por bind mount e
# rodamos o jar pronto.
#
# Uso:
#   ./scripts/dev.sh up        sobe banco + api + interface
#   ./scripts/dev.sh build     recompila o jar
#   ./scripts/dev.sh restart   recompila e reinicia a api
#   ./scripts/dev.sh reset-db  APAGA o banco e recria do zero
#   ./scripts/dev.sh logs      acompanha os logs da api
#   ./scripts/dev.sh test      roda os testes
#   ./scripts/dev.sh down      derruba tudo
#   ./scripts/dev.sh portproxy refaz o encaminhamento para o Windows
#   ./scripts/dev.sh remoto    UMA porta so' (8085), para acesso por tunel
# =====================================================================
set -euo pipefail

cd "$(dirname "$0")/.."
RAIZ="$PWD"

MAVEN_IMG="maven:3.9-eclipse-temurin-21"
M2_VOL="alfaschool-m2"
API="alfaschool-api-dev"
REDE="alfaschool_alfaschool-network"
PORTA="${BACKEND_PORT:-8083}"
JAR="backend-0.0.1-SNAPSHOT.jar"

mvn_run() {
  docker run --rm \
    -v "$RAIZ/backend":/app \
    -v "$M2_VOL":/root/.m2 \
    -w /app "$MAVEN_IMG" mvn "$@"
}

build() {
  echo ">> compilando o jar (bind mount, le o disco ao vivo)"
  docker volume create "$M2_VOL" >/dev/null
  mvn_run -B -DskipTests package
}

infra() {
  echo ">> subindo mysql e redis"
  docker compose up -d mysql redis
  echo -n ">> aguardando mysql"
  for _ in $(seq 1 40); do
    if docker exec alfaschool-mysql mysqladmin ping -h localhost \
         -uroot -p"${MYSQL_ROOT_PASSWORD:-alfaschool123}" >/dev/null 2>&1; then
      echo " pronto"; return 0
    fi
    echo -n "."; sleep 2
  done
  echo; echo "!! mysql nao respondeu a tempo"; exit 1
}

api() {
  echo ">> (re)iniciando a api na porta $PORTA"
  docker rm -f "$API" >/dev/null 2>&1 || true
  docker run -d --name "$API" \
    --network "$REDE" \
    -p "$PORTA":8080 \
    -v "$RAIZ/backend/target":/app \
    -w /app \
    -e SPRING_PROFILES_ACTIVE=docker \
    -e MYSQL_ROOT_PASSWORD="${MYSQL_ROOT_PASSWORD:-alfaschool123}" \
    -e APP_SECRET_KEY="${APP_SECRET_KEY:-alfaschool-dev-secret-key-trocar-em-producao-32+}" \
    -e ACCESS_SIMULADOR="${ACCESS_SIMULADOR:-true}" \
    -e APP_PERMITIR_SEGREDOS_PADRAO="${APP_PERMITIR_SEGREDOS_PADRAO:-true}" \
    -e CORS_ALLOWED_ORIGINS="${CORS_ALLOWED_ORIGINS:-http://localhost,http://localhost:80,http://localhost:$PORTA_WEB,http://localhost:$PORTA,http://localhost:$PORTA_REMOTA,http://127.0.0.1:$PORTA_WEB,http://127.0.0.1:$PORTA_REMOTA}" \
    "$MAVEN_IMG" java -jar "$JAR" >/dev/null

  echo -n ">> aguardando a api"
  for _ in $(seq 1 60); do
    if curl -sf "http://localhost:$PORTA/actuator/health" >/dev/null 2>&1; then
      echo " no ar"
      return 0
    fi
    echo -n "."; sleep 2
  done
  echo; echo "!! a api nao subiu. Ultimas linhas do log:"
  docker logs --tail 40 "$API"
  exit 1
}

WEB="alfaschool-web-dev"
PORTA_WEB="${FRONTEND_PORT:-5173}"
REMOTO="alfaschool-remoto"
PORTA_REMOTA="${REMOTE_PORT:-8085}"

# ---------------------------------------------------------------------
# Encaminhamento WSL -> Windows
#
# Esta maquina NAO usa o encaminhamento automatico de localhost do WSL:
# o acesso pelo navegador depende de entradas netsh portproxy explicitas.
# E o IP do WSL MUDA a cada reinicio, entao as entradas antigas passam a
# apontar para o vazio e o Chrome responde ERR_CONNECTION_REFUSED mesmo
# com tudo no ar do lado Linux.
#
# Por isso refazemos as entradas a cada "up", sempre apagando antes.
# ---------------------------------------------------------------------
# ---------------------------------------------------------------------
# Modo remoto: UMA porta so'.
#
# Serve o build estatico e faz proxy de /api na mesma origem. Para quem
# acessa de fora por tunel SSH (Mac), isto e' bem mais confiavel que o
# Vite: nao depende do websocket de HMR, que atravessa tunel mal, e exige
# encaminhar uma porta em vez de duas. O sintoma de tunel incompleto e'
# a pagina carregar e o login morrer com "Failed to fetch".
#
# O preco: nao ha hot reload. Depois de mexer no frontend, rode de novo.
# ---------------------------------------------------------------------
remoto() {
  echo ">> gerando o build do frontend"
  docker run --rm -v "$RAIZ/frontend":/app -w /app node:20-alpine \
    sh -c "npm install --silent && npm run build" >/dev/null

  echo ">> servindo em uma porta so' ($PORTA_REMOTA)"
  docker rm -f "$REMOTO" >/dev/null 2>&1 || true
  docker run -d --name "$REMOTO" \
    --network "$REDE" \
    -p "$PORTA_REMOTA":80 \
    -v "$RAIZ/frontend/dist":/usr/share/nginx/html:ro \
    -v "$RAIZ/infra/nginx/default.remoto.conf":/etc/nginx/conf.d/default.conf:ro \
    nginx:1.27-alpine >/dev/null

  sleep 2
  if curl -sf "http://localhost:$PORTA_REMOTA/" >/dev/null 2>&1; then
    portproxy
    cat <<TXT

   Tudo em http://localhost:$PORTA_REMOTA

   Do Mac, encaminhe SO' esta porta:
     ssh -N -L $PORTA_REMOTA:localhost:$PORTA_REMOTA dev

TXT
  else
    echo "!! nao subiu:"; docker logs --tail 20 "$REMOTO"; return 1
  fi
}

resumo() {
  cat <<TXT

   Interface .... http://localhost:$PORTA_WEB     <-- abra esta
   API .......... http://localhost:$PORTA
   Health ....... http://localhost:$PORTA/actuator/health
   phpMyAdmin ... http://localhost:8082

   Login do tenant Master: superadmin@alfaschool.com
   Cenario de demonstracao (Colegio Mundo do Saber):
     docker exec -i alfaschool-mysql mysql -uroot -palfaschool123 alfaschool \\
       < scripts/seed-mundo-do-saber.sql
     usuario: coordenacao@mundodosaber.com

TXT
}

portproxy() {
  local netsh="/mnt/c/Windows/System32/netsh.exe"
  [ -x "$netsh" ] || { echo ">> nao parece WSL com Windows; pulando portproxy"; return 0; }

  local ip
  ip=$(hostname -I | awk '{print $1}')
  [ -n "$ip" ] || { echo "!! nao consegui descobrir o IP do WSL"; return 1; }

  echo ">> encaminhando portas do Windows para o WSL ($ip)"
  for porta in "$PORTA" "$PORTA_WEB" "$PORTA_REMOTA"; do
    "$netsh" interface portproxy delete v4tov4 listenport="$porta" listenaddress=0.0.0.0 >/dev/null 2>&1 || true
    if "$netsh" interface portproxy add v4tov4 \
         listenport="$porta" listenaddress=0.0.0.0 \
         connectport="$porta" connectaddress="$ip" >/dev/null 2>&1; then
      echo "   porta $porta OK"
    else
      echo "   porta $porta FALHOU — rode um PowerShell como administrador:"
      echo "     netsh interface portproxy add v4tov4 listenport=$porta listenaddress=0.0.0.0 connectport=$porta connectaddress=$ip"
    fi
  done
}

web() {
  echo ">> (re)iniciando a interface na porta $PORTA_WEB"
  docker rm -f "$WEB" >/dev/null 2>&1 || true
  docker run -d --name "$WEB" \
    --network "$REDE" \
    -p "$PORTA_WEB":5173 \
    -v "$RAIZ/frontend":/app \
    -v /app/node_modules \
    -w /app \
    -e BACKEND_URL=http://"$API":8080 \
    node:20-alpine sh -c "npm install --silent && npm run dev -- --host 0.0.0.0" >/dev/null

  echo -n ">> aguardando a interface"
  for _ in $(seq 1 40); do
    if curl -sf "http://localhost:$PORTA_WEB" >/dev/null 2>&1; then
      echo " no ar"; return 0
    fi
    echo -n "."; sleep 3
  done
  echo; echo "!! a interface nao subiu. Ultimas linhas do log:"
  docker logs --tail 30 "$WEB"
  return 1
}

case "${1:-up}" in
  up)       infra; build; api; web; portproxy; resumo ;;
  build)    build ;;
  restart)  build; api ;;
  web)      web ;;
  portproxy) portproxy ;;
  remoto)   remoto ;;
  reset-db)
    echo "!! isto APAGA todos os dados locais de alfaschool"
    read -r -p "   confirmar? (digite SIM): " ok
    [ "$ok" = "SIM" ] || { echo "cancelado"; exit 1; }
    infra
    docker exec alfaschool-mysql mysql -uroot -p"${MYSQL_ROOT_PASSWORD:-alfaschool123}" \
      -e "DROP DATABASE IF EXISTS alfaschool; CREATE DATABASE alfaschool CHARACTER SET utf8mb4;"
    build; api; web; portproxy; resumo ;;
  logs)     docker logs -f "$API" ;;
  test)     mvn_run -B test ;;
  down)     docker rm -f "$API" "$WEB" "$REMOTO" >/dev/null 2>&1 || true; docker compose down ;;
  *)        sed -n '2,20p' "$0"; exit 1 ;;
esac
