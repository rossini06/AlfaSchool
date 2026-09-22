#!/usr/bin/env bash
# =====================================================================
# Seed completo do cenario de demonstracao (Colegio Mundo do Saber).
#
# Alimenta as areas que o seed SQL nao cobre: pedagogico (professores,
# disciplinas, vinculos, avaliacoes, notas, frequencia, conteudo),
# financeiro (planos, contratos, cobrancas) e a movimentacao do Access
# (entradas, retiradas, ocorrencias) pelo simulador de leitor.
#
# Pela API, e nao por SQL: assim cada enum, chave e regra de negocio e'
# validada pelo proprio sistema. RODE UMA VEZ apos o seed base — recriar
# duplicaria professores, disciplinas e planos (o cadastro nao deduplica).
#
# Uso:   ./scripts/seed-completo.sh
#        API=http://localhost:8085 ./scripts/seed-completo.sh
# Requer o cenario base ja carregado (scripts/seed-mundo-do-saber.sql) e
# a API com ACCESS_SIMULADOR=true.
# =====================================================================
set -euo pipefail
API="${API:-http://localhost:8085}"
B="$API/api/v1"

jqp() { python3 -c "import sys,json;d=json.load(sys.stdin);print(eval('d'+''.join(['[\"%s\"]'%k if not k.isdigit() else '[%s]'%k for k in sys.argv[1].split('.')])) if sys.argv[1] else d)" "$1" 2>/dev/null || echo ""; }
tok=$(curl -s -X POST "$B/auth/login" -H 'Content-Type: application/json' \
      -d '{"email":"diretor@mundodosaber.com","password":"100%Alfa@"}' | sed -nE 's/.*"accessToken":"([^"]+)".*/\1/p')
[ -n "$tok" ] || { echo "!! login do diretor falhou (o cenario base foi carregado?)"; exit 1; }
AUTH="Authorization: Bearer $tok"
post() { curl -s -H "$AUTH" -H 'Content-Type: application/json' -X POST "$B$1" -d "$2"; }
get()  { curl -s -H "$AUTH" "$B$1"; }
id_of(){ grep -oE '"id":"[^"]+"' | head -1 | cut -d'"' -f4; }
ok(){ printf "  %-42s %s\n" "$1" "$2"; }

echo ">> lendo o cenario base"
CURSO=$(get "/cursos?size=1" | id_of)
UNIDADE=$(get "/unidades?size=1" | id_of)
TURMAS=( $(get "/turmas?size=20" | grep -oE '"id":"[^"]+"' | cut -d'"' -f4) )
ALUNOS=( $(get "/alunos?size=50" | python3 -c "import sys,json;[print(a['id']) for a in json.load(sys.stdin)['data']['content']]") )
T1=${TURMAS[0]}; echo "  curso=$CURSO unidade=$UNIDADE turmas=${#TURMAS[@]} alunos=${#ALUNOS[@]}"

echo ">> professores"
declare -a PROF
for n in "Marina Costa|Pedagogia" "Rafael Lima|Matematica" "Juliana Alves|Letras"; do
  nome=${n%%|*}; esp=${n##*|}
  pid=$(post /professores "{\"nome\":\"$nome\",\"especialidade\":\"$esp\",\"unitId\":\"$UNIDADE\",\"status\":\"ativo\",\"dataContratacao\":\"2025-02-01\"}" | id_of)
  [ -n "$pid" ] && PROF+=("$pid")
done
ok "professores criados" "${#PROF[@]}"

echo ">> disciplinas"
declare -a DISC
for d in "Linguagem" "Matematica" "Natureza e Sociedade" "Artes" "Movimento"; do
  did=$(post /disciplinas "{\"nome\":\"$d\",\"cursoId\":\"$CURSO\",\"ativa\":true}" | id_of)
  [ -n "$did" ] && DISC+=("$did")
done
ok "disciplinas criadas" "${#DISC[@]}"

echo ">> vinculos professor-turma-disciplina"
v=0
for tu in "${TURMAS[@]}"; do
  i=0
  for di in "${DISC[@]}"; do
    pr=${PROF[$((i % ${#PROF[@]}))]}
    code=$(curl -s -o /dev/null -w "%{http_code}" -H "$AUTH" -H 'Content-Type: application/json' -X POST "$B/vinculos" \
           -d "{\"professorId\":\"$pr\",\"turmaId\":\"$tu\",\"disciplinaId\":\"$di\"}")
    [ "$code" = "200" ] || [ "$code" = "201" ] && v=$((v+1)); i=$((i+1))
  done
done
ok "vinculos" "$v"

echo ">> conteudo ministrado (diario)"
c=0
for tu in "${TURMAS[@]}"; do
  for dia in 2026-08-04 2026-08-11 2026-08-18; do
    di=${DISC[0]}
    code=$(curl -s -o /dev/null -w "%{http_code}" -H "$AUTH" -H 'Content-Type: application/json' -X POST "$B/conteudos-ministrados" \
       -d "{\"turmaId\":\"$tu\",\"disciplinaId\":\"$di\",\"data\":\"$dia\",\"descricao\":\"Roda de conversa e leitura compartilhada\",\"objetivos\":\"Desenvolver oralidade\"}")
    [ "$code" = "200" ] || [ "$code" = "201" ] && c=$((c+1))
  done
done
ok "conteudos" "$c"

echo ">> frequencia (lote por turma/disciplina)"
f=0
for tu in "${TURMAS[@]}"; do
  # alunos da turma via matriculas
  AT=( $(get "/matriculas?turmaId=$tu&size=50" | python3 -c "import sys,json;print('\n'.join(m['alunoId'] for m in json.load(sys.stdin)['data']['content']))" 2>/dev/null) )
  [ ${#AT[@]} -eq 0 ] && continue
  itens=$(printf '{"alunoId":"%s"},' "${AT[@]}"); itens="[${itens%,}]"
  for dia in 2026-08-04 2026-08-05 2026-08-06; do
    code=$(curl -s -o /dev/null -w "%{http_code}" -H "$AUTH" -H 'Content-Type: application/json' -X POST "$B/frequencias/lote" \
       -d "{\"turmaId\":\"$tu\",\"disciplinaId\":\"${DISC[0]}\",\"data\":\"$dia\",\"numeroAula\":1,\"frequencias\":$itens}")
    [ "$code" = "200" ] || [ "$code" = "201" ] && f=$((f+1))
  done
done
ok "lotes de frequencia" "$f"

echo ">> avaliacoes + notas"
a=0; nt=0
for tu in "${TURMAS[@]}"; do
  AT=( $(get "/matriculas?turmaId=$tu&size=50" | python3 -c "import sys,json;print('\n'.join(m['alunoId'] for m in json.load(sys.stdin)['data']['content']))" 2>/dev/null) )
  for nome in "Sondagem 1o Bimestre|1BIMESTRE" "Portfolio 2o Bimestre|2BIMESTRE"; do
    av=${nome%%|*}; per=${nome##*|}
    aid=$(post /avaliacoes "{\"turmaId\":\"$tu\",\"disciplinaId\":\"${DISC[0]}\",\"nome\":\"$av\",\"tipo\":\"PROVA\",\"periodo\":\"$per\",\"dataAvaliacao\":\"2026-08-20\",\"notaMaxima\":10,\"notaMinima\":6,\"peso\":1}" | id_of)
    [ -n "$aid" ] && a=$((a+1)) || continue
    nota=7
    for al in "${AT[@]}"; do
      code=$(curl -s -o /dev/null -w "%{http_code}" -H "$AUTH" -H 'Content-Type: application/json' -X POST "$B/notas" \
         -d "{\"alunoId\":\"$al\",\"avaliacaoId\":\"$aid\",\"nota\":$nota.5}")
      [ "$code" = "200" ] || [ "$code" = "201" ] && nt=$((nt+1))
      nota=$(( (nota % 4) + 6 ))
    done
  done
done
ok "avaliacoes" "$a"; ok "notas lancadas" "$nt"

echo ">> financeiro (planos + contratos -> cobrancas)"
P1=$(post /financeiro/planos '{"nome":"Mensalidade Integral","valor":1200,"periodicidade":"mensal","ativo":true}' | id_of)
P2=$(post /financeiro/planos '{"nome":"Meio Periodo","valor":780,"periodicidade":"mensal","ativo":true}' | id_of)
ok "planos" "2"
ct=0
i=0
for al in "${ALUNOS[@]}"; do
  pl=$([ $((i%2)) -eq 0 ] && echo "$P1" || echo "$P2")
  code=$(curl -s -o /dev/null -w "%{http_code}" -H "$AUTH" -H 'Content-Type: application/json' -X POST "$B/financeiro/contratos" \
     -d "{\"alunoId\":\"$al\",\"planoId\":\"$pl\",\"dataInicio\":\"2026-02-01\"}")
  [ "$code" = "200" ] || [ "$code" = "201" ] && ct=$((ct+1)); i=$((i+1))
done
ok "contratos (geram cobrancas)" "$ct"

echo ">> restricao judicial (sobre um aluno)"
PA=$(get "/access/pessoas-autorizadas?size=1" | id_of)
AL0=${ALUNOS[0]}
code=$(curl -s -o /dev/null -w "%{http_code}" -H "$AUTH" -H 'Content-Type: application/json' -X POST "$B/access/restricoes" \
   -d "{\"alunoId\":\"$AL0\",\"pessoaAutorizadaId\":\"$PA\",\"tipo\":\"JUDICIAL\",\"descricao\":\"Medida protetiva: genitor sem direito de retirada.\",\"numeroProcesso\":\"0012345-67.2026.8.08.0000\",\"orgaoEmissor\":\"Vara da Infancia\",\"vigenciaInicio\":\"2026-08-01\",\"ativo\":true}")
ok "restricao" "$code"

echo ">> movimentacao do Access (simulador)"
code=$(curl -s -o /dev/null -w "%{http_code}" -H "$AUTH" -H 'Content-Type: application/json' -X POST "$B/access/simulador/rotina-dia" -d "{\"unitId\":\"$UNIDADE\"}")
ok "rotina-dia" "$code"
DISP=$(get "/dispositivos?size=1" | id_of)
if [ -n "$DISP" ]; then
  curl -s -o /dev/null -H "$AUTH" -H 'Content-Type: application/json' -X POST "$B/access/simulador/acesso-negado" -d "{\"dispositivoId\":\"$DISP\",\"motivo\":\"Fora do horario autorizado\"}"
  curl -s -o /dev/null -H "$AUTH" -H 'Content-Type: application/json' -X POST "$B/access/simulador/pessoa-desconhecida" -d "{\"dispositivoId\":\"$DISP\"}"
  ok "ocorrencias (negado + desconhecida)" "ok"
fi

echo ">> pronto."
