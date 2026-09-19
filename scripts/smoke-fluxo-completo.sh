#!/usr/bin/env bash
# =====================================================================
# Teste de aceitacao do fluxo de retirada, ponta a ponta, pelo simulador.
#
# Prova a regra que da nome ao modulo: reconhecer o responsavel NAO
# entrega o aluno e NAO encerra a permanencia. Entre a chegada do pai e a
# saida efetiva da crianca existe tempo real, e e' esse tempo que o
# sistema precisa medir.
#
# Pre-requisitos:
#   ./scripts/dev.sh up
#   docker exec -i alfaschool-mysql mysql -uroot -palfaschool123 alfaschool \
#     < scripts/seed-mundo-do-saber.sql
#   A API precisa estar com ACCESS_SIMULADOR=true.
#
# Uso: ./scripts/smoke-fluxo-completo.sh
# =====================================================================
set -uo pipefail

API="${API:-http://localhost:8083}"
TENANT="b1000000-0000-0000-0000-000000000001"
EMAIL="coordenacao@mundodosaber.com"
SENHA="${SEED_SENHA:-100%Alfa@}"

ALUNO_PEDRO="b1000000-0000-0000-0000-000000012001"
PESSOA_CARLOS="b1000000-0000-0000-0000-000000017001"
CATRACA_ENTRADA="b1000000-0000-0000-0000-000000019001"
CATRACA_SAIDA="b1000000-0000-0000-0000-000000019002"
LEITOR_RESPONSAVEL="b1000000-0000-0000-0000-000000019007"

OK=0; FALHOU=0
passo() { printf '\n\033[1m%s\033[0m\n' "── $*"; }
ok()    { printf '  \033[32m✓\033[0m %s\n' "$*"; OK=$((OK+1)); }
falha() { printf '  \033[31m✗\033[0m %s\n' "$*"; FALHOU=$((FALHOU+1)); }

jqp() { python3 -c "import sys,json
try: d=json.load(sys.stdin)
except Exception: print(''); sys.exit()
for k in sys.argv[1].split('.'):
    if d is None: break
    d = d.get(k) if isinstance(d,dict) else (d[int(k)] if isinstance(d,list) and k.isdigit() and int(k)<len(d) else None)
print('' if d is None else d)" "$1"; }

api() { # metodo caminho [json]
  local m=$1 p=$2 body=${3:-}
  if [ -n "$body" ]; then
    curl -s -X "$m" "$API$p" -H "Authorization: Bearer $TOKEN" \
         -H 'Content-Type: application/json' -d "$body"
  else
    curl -s -X "$m" "$API$p" -H "Authorization: Bearer $TOKEN"
  fi
}

# =====================================================================
# Limpeza do dia
#
# O smoke simula SEMPRE o mesmo dia: entrada de manha, saida a tarde. Se
# ele ja' rodou hoje, os eventos da rodada anterior continuam no banco e
# a apuracao pareia a entrada de uma rodada com a entrada da outra — um
# dia de 3h59 apura 8 minutos, e a falha parece um bug de calculo que
# nao existe.
#
# Pior: o inverso tambem acontece. Uma regressao de verdade pode ficar
# escondida atras do lixo da rodada anterior. Um teste de aceitacao que
# so' vale na primeira execucao do dia nao serve para decidir se o
# sistema esta pronto, entao ele limpa o proprio rastro antes de comecar.
#
# So' apaga o que o SIMULADOR gerou hoje. Leitura de equipamento de
# verdade tem outra origem e nao e' tocada.
# =====================================================================
MYSQL_PASS="${MYSQL_ROOT_PASSWORD:-alfaschool123}"
if docker exec alfaschool-mysql true 2>/dev/null; then
  passo "Limpando o rastro de execucoes anteriores de hoje"
  docker exec -i alfaschool-mysql mysql -uroot -p"$MYSQL_PASS" alfaschool >/dev/null 2>&1 <<SQL
DELETE FROM acc_presenca_pares WHERE presenca_id IN (SELECT id FROM acc_presencas WHERE data=CURDATE());
DELETE FROM acc_presencas WHERE data=CURDATE();
DELETE FROM acc_retirada_historico WHERE retirada_id IN (SELECT id FROM acc_retiradas WHERE DATE(solicitado_em)=CURDATE());
DELETE FROM acc_retiradas WHERE DATE(solicitado_em)=CURDATE();
DELETE FROM acc_eventos WHERE origem='SIMULADOR' AND DATE(data_hora)=CURDATE();
SQL
  if [ $? -eq 0 ]; then ok "dia limpo — a rodada comeca do zero"
  else falha "nao foi possivel limpar; o resultado pode vir contaminado"; fi
else
  printf '  \033[33m!\033[0m sem acesso ao mysql: se o smoke ja rodou hoje, o resultado nao e confiavel\n'
fi

passo "Autenticando na escola"
TOKEN=$(curl -s -X POST "$API/api/v1/auth/login" -H 'Content-Type: application/json' \
  -d "{\"tenantId\":\"$TENANT\",\"email\":\"$EMAIL\",\"password\":\"$SENHA\"}" | jqp data.accessToken)
[ -n "$TOKEN" ] && ok "login de $EMAIL" || { falha "login falhou — o seed foi carregado?"; exit 1; }

HOJE=$(date +%Y-%m-%d)

# Os horarios sao calculados a partir de AGORA, nunca fixos. O sistema
# recusa leitura com data no futuro (relogio de leitor errado) e usa a
# hora do servidor no lugar — com horario fixo o teste mediria o clamp,
# nao o fluxo. Entrada 4h atras, chegada do responsavel 30min atras,
# saida 1min atras.
T_ENTRADA=$(date -d '-4 hours'    +%Y-%m-%dT%H:%M:%S%:z)
T_CHEGADA=$(date -d '-30 minutes' +%Y-%m-%dT%H:%M:%S%:z)
T_SAIDA=$(date -d '-1 minute'     +%Y-%m-%dT%H:%M:%S%:z)
MIN_ESPERADO=$(( 4 * 60 - 1 ))

passo "1. Aluno chega a escola (leitor da catraca de entrada)"
EVENTO_ENTRADA=$(api POST /api/v1/access/simulador/entrada-aluno \
  "{\"alunoId\":\"$ALUNO_PEDRO\",\"dispositivoId\":\"$CATRACA_ENTRADA\",\"dataHora\":\"$T_ENTRADA\"}" | jqp data.eventoId)
PRESENTES=$(api GET "/api/v1/access/permanencia/hoje?page=0&size=50" | jqp data.totalElements)
[ "${PRESENTES:-0}" -ge 1 ] && ok "permanencia aberta (presentes agora: $PRESENTES)" \
                            || falha "o aluno nao aparece como presente"

passo "2. Responsavel chega e e' reconhecido no leitor exclusivo"
api POST /api/v1/access/simulador/chegada-responsavel \
  "{\"pessoaAutorizadaId\":\"$PESSOA_CARLOS\",\"dispositivoId\":\"$LEITOR_RESPONSAVEL\",\"dataHora\":\"$T_CHEGADA\"}" >/dev/null
FILA=$(api GET "/api/v1/access/retiradas/fila?page=0&size=50")
RETIRADA_ID=$(echo "$FILA" | jqp data.0.id)
[ -n "$RETIRADA_ID" ] && ok "retirada aberta na fila ($RETIRADA_ID)" \
                      || { falha "nenhuma retirada foi aberta"; echo "$FILA" | head -c 400; }

passo "3. A chegada do pai NAO pode ter encerrado a permanencia"
ST=$(api GET "/api/v1/access/permanencia/aluno/$ALUNO_PEDRO?inicio=$HOJE&fim=$HOJE" | jqp data.dias.0.status)
[ "$ST" = "ABERTA" ] && ok "permanencia segue ABERTA — a regra foi respeitada" \
                     || falha "permanencia esta '$ST'; deveria seguir ABERTA"

if [ -n "${RETIRADA_ID:-}" ]; then
  passo "4. Professora prepara o aluno e a coordenacao confirma a entrega"
  api POST "/api/v1/access/retiradas/$RETIRADA_ID/preparar" >/dev/null
  api POST "/api/v1/access/retiradas/$RETIRADA_ID/pronto" >/dev/null
  api POST "/api/v1/access/retiradas/$RETIRADA_ID/entregar" '{"observacao":"smoke"}' >/dev/null
  STATUS=$(api GET "/api/v1/access/retiradas/$RETIRADA_ID" | jqp data.status)
  [ -z "$STATUS" ] && STATUS=$(api GET "/api/v1/access/retiradas/historico?alunoId=$ALUNO_PEDRO" | jqp data.0.status)
  [ "$STATUS" = "ENTREGUE" ] && ok "retirada ENTREGUE" || falha "status ficou '$STATUS'"

  passo "5. Entregar tambem NAO pode encerrar a permanencia"
  ST=$(api GET "/api/v1/access/permanencia/aluno/$ALUNO_PEDRO?inicio=$HOJE&fim=$HOJE" | jqp data.dias.0.status)
  [ "$ST" = "ABERTA" ] && ok "permanencia segue ABERTA apos a entrega" \
                       || falha "permanencia esta '$ST'; so' a saida efetiva pode fechar"
fi

passo "6. Saida efetiva na catraca — agora sim a permanencia fecha"
api POST /api/v1/access/simulador/saida-aluno \
  "{\"alunoId\":\"$ALUNO_PEDRO\",\"dispositivoId\":\"$CATRACA_SAIDA\",\"dataHora\":\"$T_SAIDA\"}" >/dev/null
DIA=$(api GET "/api/v1/access/permanencia/aluno/$ALUNO_PEDRO?inicio=$HOJE&fim=$HOJE")
ST=$(echo "$DIA" | jqp data.dias.0.status)
MIN=$(echo "$DIA" | jqp data.dias.0.minutosPermanencia)
EXC=$(echo "$DIA" | jqp data.dias.0.minutosExcedente)
[ "$ST" = "FECHADA" ] && ok "permanencia FECHADA" || falha "status ficou '$ST'"
# Tolerancia de 1 minuto: entrada e saida sao carimbadas em momentos
# distintos do relogio durante a execucao do proprio teste.
DIF=$(( MIN_ESPERADO - ${MIN:-0} )); DIF=${DIF#-}
[ "$DIF" -le 1 ] && ok "permanencia de ${MIN}min (esperado ~${MIN_ESPERADO}min)" \
                 || falha "permanencia deu ${MIN}min; esperado ~${MIN_ESPERADO}min"
printf '  \033[36mi\033[0m excedente apurado: %s min (jornada do Pedro e meio periodo)\n' "${EXC:-0}"

passo "7. Reenvio da MESMA leitura nao pode virar registro novo"
# Replay de verdade: o mesmo device_log_id chegando outra vez, que e' o
# que acontece quando a fila offline do gateway reenvia o que ja entregou.
# Repetir /entrada-aluno nao serve: aquilo gera um device_log_id novo e
# seria uma passagem legitima na catraca.
ANTES=$(api GET "/api/v1/access/permanencia/aluno/$ALUNO_PEDRO?inicio=$HOJE&fim=$HOJE" | jqp data.dias.0.minutosPermanencia)
REPLAY=$(api POST /api/v1/access/simulador/evento-duplicado "{\"eventoId\":\"$EVENTO_ENTRADA\"}" | jqp data.replay)
DEPOIS=$(api GET "/api/v1/access/permanencia/aluno/$ALUNO_PEDRO?inicio=$HOJE&fim=$HOJE" | jqp data.dias.0.minutosPermanencia)
[ "$REPLAY" = "True" ] && ok "reenvio reconhecido como replay" || falha "o reenvio gerou evento novo"
[ "$ANTES" = "$DEPOIS" ] && ok "permanencia intacta (${ANTES}min antes e depois)" \
                         || falha "permanencia mudou de ${ANTES} para ${DEPOIS}"

printf '\n\033[1m── Resultado:\033[0m %s verificacoes OK, %s falhas\n\n' "$OK" "$FALHOU"
[ "$FALHOU" -eq 0 ]
