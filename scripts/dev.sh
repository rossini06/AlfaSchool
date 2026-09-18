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
#   ./scripts/dev.sh up        sobe banco + api
#   ./scripts/dev.sh build     recompila o jar
#   ./scripts/dev.sh restart   recompila e reinicia a api
#   ./scripts/dev.sh reset-db  APAGA o banco e recria do zero
#   ./scripts/dev.sh logs      acompanha os logs da api
#   ./scripts/dev.sh test      roda os testes
#   ./scripts/dev.sh down      derruba tudo
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
    "$MAVEN_IMG" java -jar "$JAR" >/dev/null

  echo -n ">> aguardando a api"
  for _ in $(seq 1 60); do
    if curl -sf "http://localhost:$PORTA/actuator/health" >/dev/null 2>&1; then
      echo " no ar"
      echo
      echo "   API .......... http://localhost:$PORTA"
      echo "   Health ....... http://localhost:$PORTA/actuator/health"
      echo "   phpMyAdmin ... http://localhost:8082"
      echo "   Login ........ superadmin@alfaschool.com"
      return 0
    fi
    echo -n "."; sleep 2
  done
  echo; echo "!! a api nao subiu. Ultimas linhas do log:"
  docker logs --tail 40 "$API"
  exit 1
}

case "${1:-up}" in
  up)       infra; build; api ;;
  build)    build ;;
  restart)  build; api ;;
  reset-db)
    echo "!! isto APAGA todos os dados locais de alfaschool"
    read -r -p "   confirmar? (digite SIM): " ok
    [ "$ok" = "SIM" ] || { echo "cancelado"; exit 1; }
    infra
    docker exec alfaschool-mysql mysql -uroot -p"${MYSQL_ROOT_PASSWORD:-alfaschool123}" \
      -e "DROP DATABASE IF EXISTS alfaschool; CREATE DATABASE alfaschool CHARACTER SET utf8mb4;"
    build; api ;;
  logs)     docker logs -f "$API" ;;
  test)     mvn_run -B test ;;
  down)     docker rm -f "$API" >/dev/null 2>&1 || true; docker compose down ;;
  *)        sed -n '2,20p' "$0"; exit 1 ;;
esac
