#!/usr/bin/env bash
# =====================================================================
# Seed COMPLETO e CHEIO do cenario de demonstracao (Mundo do Saber).
#
# Alimenta TODO o sistema para uma demonstracao: dezenas de alunos com
# responsaveis, matriculas, contratos e biometria; pedagogico inteiro
# (frequencia, avaliacoes, notas, conteudo); financeiro (planos, contratos
# e cobrancas); pessoas autorizadas e autorizacoes; e a operacao do Access
# pelo simulador de leitor — alunos presentes (enche os PAINEIS de sala e a
# Coordenacao), fila de retirada e ocorrencias.
#
# Estrategia: cadastro pela API (valida enum, chave e regra); rostos e
# consentimento por SQL (o fluxo LGPD real exige consentimento manual, que
# nao cabe num seed); operacao do Access pelo simulador (gera evento,
# presenca e retirada com as regras de negocio corretas).
#
# Uso:   API=http://localhost:8085 ./scripts/seed-completo.sh
# Requer: seed base (seed-mundo-do-saber.sql) e ACCESS_SIMULADOR=true.
# Rode UMA VEZ (o cadastro nao deduplica).
# =====================================================================
set -uo pipefail
API="${API:-http://localhost:8085}"
B="$API/api/v1"
MYSQL_CONT="${MYSQL_CONT:-alfaschool-mysql}"
MYSQL_PW="${MYSQL_PW:-alfaschool123}"   # staging usa senha propria; passe MYSQL_PW=...
TENANT="b1000000-0000-0000-0000-000000000001"
UNIDADE="b1000000-0000-0000-0000-000000009001"
TURMA_A="b1000000-0000-0000-0000-000000006001"   # Infantil 2A (painel infantil-2a)
TURMA_B="b1000000-0000-0000-0000-000000006002"   # Infantil 2B (painel infantil-2b)
DISP_ENTRADA="b1000000-0000-0000-0000-000000019001"  # Catraca 1 - Entrada
PORTARIA="b1000000-0000-0000-0000-000000002001"      # Portaria Principal
POR_TURMA="${POR_TURMA:-12}"                      # alunos por turma

sql(){ docker exec -i "$MYSQL_CONT" mysql --default-character-set=utf8mb4 -uroot -p"$MYSQL_PW" alfaschool -N -e "$1" 2>/dev/null; }
tok=$(curl -s -X POST "$B/auth/login" -H 'Content-Type: application/json' \
      -d '{"email":"diretor@mundodosaber.com","password":"100%Alfa@"}' | sed -nE 's/.*"accessToken":"([^"]+)".*/\1/p')
[ -n "$tok" ] || { echo "!! login do diretor falhou (o seed base foi carregado?)"; exit 1; }
AUTH="Authorization: Bearer $tok"
post(){ curl -s -H "$AUTH" -H 'Content-Type: application/json' -X POST "$B$1" -d "$2"; }
get(){ curl -s -H "$AUTH" "$B$1"; }
idof(){ grep -oE '"id":"[^"]+"' | head -1 | cut -d'"' -f4; }
ok(){ printf "  %-40s %s\n" "$1" "$2"; }

NOMES_M="Miguel Arthur Gael Heitor Theo Davi Bernardo Noah Ravi Anthony Pedro Lucas Enzo Benicio Samuel"
NOMES_F="Helena Alice Laura Maria Sophia Isabella Manuela Cecilia Eloa Antonella Valentina Heloisa Liz Aurora Maite"
SOBRE="Silva Souza Oliveira Santos Lima Costa Pereira Almeida Nunes Rocha Gomes Ribeiro Carvalho Araujo Mendes"
arr_m=($NOMES_M); arr_f=($NOMES_F); arr_s=($SOBRE)
# nomes de adultos (responsaveis e pessoas autorizadas)
ADULTOS="Mariana Fernanda Patricia Juliana Adriana Simone Cristina Vanessa Renata Luciana Roberto Carlos Marcelo Fernando Ricardo Andre Paulo Rogerio Sergio Eduardo"
PARENTES="Avó Avô Tia Tio Madrinha Padrinho Vizinha Motorista"
arr_ad=($ADULTOS); arr_pt=($PARENTES)
adulto(){ echo "${arr_ad[$((RANDOM%${#arr_ad[@]}))]} ${arr_s[$((RANDOM%${#arr_s[@]}))]}"; }
NOME=""; SEXO="M"
rnd_nome(){ if [ $((RANDOM%2)) -eq 0 ]; then NOME="${arr_m[$((RANDOM%${#arr_m[@]}))]}"; SEXO=M; else NOME="${arr_f[$((RANDOM%${#arr_f[@]}))]}"; SEXO=F; fi; NOME="$NOME ${arr_s[$((RANDOM%${#arr_s[@]}))]}"; }
cpf(){ printf "%03d.%03d.%03d-%02d" $((RANDOM%999)) $((RANDOM%999)) $((RANDOM%999)) $((RANDOM%99)); }

echo ">> cursos/disciplinas/professores"
CURSO=$(get "/cursos?size=1" | idof)
declare -a PROF DISC
for n in "Marina Costa|Pedagogia" "Rafael Lima|Matematica" "Juliana Alves|Letras" "Camila Rocha|Artes"; do
  pid=$(post /professores "{\"nome\":\"${n%%|*}\",\"especialidade\":\"${n##*|}\",\"unitId\":\"$UNIDADE\",\"status\":\"ativo\",\"dataContratacao\":\"2025-02-01\"}" | idof)
  [ -n "$pid" ] && PROF+=("$pid")
done
for d in "Linguagem" "Matematica" "Natureza e Sociedade" "Artes" "Movimento"; do
  did=$(post /disciplinas "{\"nome\":\"$d\",\"cursoId\":\"$CURSO\",\"ativa\":true}" | idof)
  [ -n "$did" ] && DISC+=("$did")
done
ok "professores/disciplinas" "${#PROF[@]}/${#DISC[@]}"
for tu in "$TURMA_A" "$TURMA_B"; do i=0; for di in "${DISC[@]}"; do
  post /vinculos "{\"professorId\":\"${PROF[$((i%${#PROF[@]}))]}\",\"turmaId\":\"$tu\",\"disciplinaId\":\"$di\"}" >/dev/null; i=$((i+1)); done; done
ok "vinculos professor-turma-disciplina" "$(( ${#DISC[@]} * 2 ))"

DUSEQ=$(sql "SELECT COALESCE(MAX(device_user_id),101)+1 FROM acc_faces WHERE tenant_id='$TENANT'"); DUSEQ=${DUSEQ:-200}
declare -a AL_A AL_B ALL_AL PRESENTES
FACE_SQL=""; AUTFACE_SQL=""
criar_turma(){
  local turma="$1" ini="$2" arrname="$3" n=0
  while [ $n -lt $POR_TURMA ]; do
    rnd_nome; nome="$NOME"; nasc="20$((19+RANDOM%2))-0$((1+RANDOM%9))-$((10+RANDOM%18))"
    al=$(post /alunos "{\"nome\":\"$nome\",\"cpf\":\"$(cpf)\",\"dataNascimento\":\"$nasc\",\"sexo\":\"$SEXO\",\"cidade\":\"Serra\",\"estado\":\"ES\",\"ativo\":true,\"unitId\":\"$UNIDADE\"}" | idof)
    [ -z "$al" ] && { n=$((n+1)); continue; }
    # responsavel + vinculo (a rota /alunos/{id}/responsaveis popula aluno_responsaveis,
    # que e' o que a retirada consulta para achar os filhos do responsavel).
    rid=$(post /responsaveis "{\"nome\":\"$(adulto)\",\"telefone\":\"(27) 9$((10000000+RANDOM%89999999))\",\"email\":\"resp.${al:0:8}@exemplo.com\"}" | idof)
    [ -n "$rid" ] && post "/alunos/$al/responsaveis" "{\"responsavelId\":\"$rid\",\"parentesco\":\"Mae\",\"autorizadoBuscar\":true,\"principal\":true,\"responsavelFinanceiro\":true}" >/dev/null
    # matricula na turma
    post /matriculas "{\"alunoId\":\"$al\",\"turmaId\":\"$turma\",\"dataMatricula\":\"2026-02-01\"}" >/dev/null
    # contrato (gera cobrancas)
    post /financeiro/contratos "{\"alunoId\":\"$al\",\"planoId\":\"$PLANO\",\"dataInicio\":\"2026-02-01\"}" >/dev/null
    # rosto via SQL (consentimento de menor nao cabe em seed automatico)
    FACE_SQL+="(UUID(),'$TENANT','ALUNO','$al','demo/aluno.jpg',$DUSEQ,'CONSENTIMENTO_RESPONSAVEL',TRUE,NOW(6),'1.0','SECRETARIA','Responsavel',TRUE,NOW(6),NOW(6),FALSE),"
    DUSEQ=$((DUSEQ+1))
    eval "$arrname+=(\"$al\")"; ALL_AL+=("$al")
    n=$((n+1))
  done
}
echo ">> gerando alunos, responsaveis, matriculas e contratos"
PLANO=$(post /financeiro/planos '{"nome":"Mensalidade Integral","valor":1200,"periodicidade":"mensal","ativo":true}' | idof)
PLANO2=$(post /financeiro/planos '{"nome":"Meio Periodo","valor":780,"periodicidade":"mensal","ativo":true}' | idof)
criar_turma "$TURMA_A" "manha" AL_A
criar_turma "$TURMA_B" "tarde" AL_B
ok "alunos criados" "${#ALL_AL[@]}"

echo ">> gravando biometria (SQL)"
FACE_SQL="${FACE_SQL%,}"
sql "INSERT INTO acc_faces (id,tenant_id,titular_tipo,titular_id,foto_key,device_user_id,base_legal,consentimento_obtido,consentimento_em,consentimento_versao,consentimento_origem,consentimento_por,ativo,created_at,updated_at,deleted) VALUES $FACE_SQL;"
ok "rostos de aluno gravados" "${#ALL_AL[@]}"

echo ">> pessoas autorizadas + autorizacoes (subconjunto) + rosto p/ chegada"
declare -a PESSOAS PES_ALU
count=0
# Intercala 2A e 2B para as retiradas (e a atividade das TVs) caírem nas DUAS
# salas, e nao so' na primeira turma criada.
INTERCAL=(); na=${#AL_A[@]}; nb=${#AL_B[@]}; mx=$(( na>nb ? na : nb ))
for ((i=0; i<mx; i++)); do
  [ $i -lt $na ] && INTERCAL+=("${AL_A[$i]}")
  [ $i -lt $nb ] && INTERCAL+=("${AL_B[$i]}")
done
for al in "${INTERCAL[@]}"; do
  [ $count -ge 8 ] && break
  RESP=$(get "/alunos/$al/responsaveis" | grep -oE "\"responsavelId\":\"[^\"]+\"" | head -1 | cut -d'"' -f4)
  [ -z "$RESP" ] && continue
  pa=$(post /access/pessoas-autorizadas "{\"responsavelId\":\"$RESP\",\"nome\":\"$(adulto)\",\"parentesco\":\"${arr_pt[$((RANDOM%${#arr_pt[@]}))]}\",\"telefone\":\"(27) 98888-$((1000+RANDOM%8999))\",\"podeRetirar\":true}" | idof)
  [ -z "$pa" ] && continue
  post /access/autorizacoes "{\"alunoId\":\"$al\",\"pessoaAutorizadaId\":\"$pa\",\"permanente\":true,\"motivo\":\"Retirada habitual\"}" >/dev/null
  AUTFACE_SQL+="(UUID(),'$TENANT','AUTORIZADA','$pa','demo/pessoa.jpg',$DUSEQ,'CONSENTIMENTO_TITULAR',TRUE,NOW(6),'1.0','SECRETARIA','Titular',TRUE,NOW(6),NOW(6),FALSE),"
  # garante que ESTE aluno esteja presente, para poder ter retirada
  curl -s -o /dev/null -H "$AUTH" -H 'Content-Type: application/json' -X POST "$B/access/simulador/entrada-aluno" -d "{\"alunoId\":\"$al\",\"dispositivoId\":\"$DISP_ENTRADA\"}"
  DUSEQ=$((DUSEQ+1)); PESSOAS+=("$pa"); PES_ALU+=("$pa|$al"); count=$((count+1))
done
if [ -n "$AUTFACE_SQL" ]; then
  AUTFACE_SQL="${AUTFACE_SQL%,}"
  sql "INSERT INTO acc_faces (id,tenant_id,titular_tipo,titular_id,foto_key,device_user_id,base_legal,consentimento_obtido,consentimento_em,consentimento_versao,consentimento_origem,consentimento_por,ativo,created_at,updated_at,deleted) VALUES $AUTFACE_SQL;"
fi
ok "pessoas autorizadas" "${#PESSOAS[@]}"

echo ">> pedagogico: conteudo, frequencia, avaliacoes, notas (todas as disciplinas)"
for tu in "$TURMA_A" "$TURMA_B"; do
  if [ "$tu" = "$TURMA_A" ]; then als=("${AL_A[@]}"); else als=("${AL_B[@]}"); fi
  itens=$(printf '{"alunoId":"%s"},' "${als[@]}"); itens="[${itens%,}]"
  post /conteudos-ministrados "{\"turmaId\":\"$tu\",\"disciplinaId\":\"${DISC[0]}\",\"data\":\"2026-08-04\",\"descricao\":\"Roda de conversa e leitura compartilhada\",\"objetivos\":\"Oralidade\"}" >/dev/null
  # Avalia TODAS as disciplinas para o boletim sair cheio (nota vira conceito
  # no infantil). Uma avaliacao por disciplina + frequencia de alguns dias.
  for di in "${DISC[@]}"; do
    for dia in 2026-08-04 2026-08-05 2026-08-06 2026-08-07; do
      post /frequencias/lote "{\"turmaId\":\"$tu\",\"disciplinaId\":\"$di\",\"data\":\"$dia\",\"numeroAula\":1,\"frequencias\":$itens}" >/dev/null
    done
    aid=$(post /avaliacoes "{\"turmaId\":\"$tu\",\"disciplinaId\":\"$di\",\"nome\":\"Sondagem 1o Bimestre\",\"tipo\":\"PROVA\",\"periodo\":\"1BIMESTRE\",\"dataAvaliacao\":\"2026-08-20\",\"notaMaxima\":10,\"notaMinima\":6,\"peso\":1}" | idof)
    for al in "${als[@]}"; do post /notas "{\"alunoId\":\"$al\",\"avaliacaoId\":\"$aid\",\"nota\":$((6+RANDOM%4)).$((RANDOM%10))}" >/dev/null; done
  done
done
# O boletim/media casam a nota pela MATRICULA; /notas e /frequencias gravam so'
# o alunoId. Sem matricula_id a media/conceito nao computa e o boletim fica "—".
# Backfill pela matricula do aluno.
sql "UPDATE notas n JOIN matriculas m ON m.aluno_id=n.aluno_id AND m.tenant_id=n.tenant_id AND (m.deleted=FALSE OR m.deleted IS NULL) SET n.matricula_id=m.id WHERE n.tenant_id='$TENANT' AND (n.matricula_id IS NULL OR n.matricula_id='')"
sql "UPDATE frequencias f JOIN matriculas m ON m.aluno_id=f.aluno_id AND m.tenant_id=f.tenant_id AND (m.deleted=FALSE OR m.deleted IS NULL) SET f.matricula_id=m.id WHERE f.tenant_id='$TENANT' AND (f.matricula_id IS NULL OR f.matricula_id='')"
ok "frequencia/avaliacoes/notas" "ok"

# Gera as MEDIAS do boletim. Sem isso a tabela medias fica vazia e o boletim
# sai "—" apesar das notas — /medias/calcular cria+persiste o registro (media,
# conceito, situacao, frequencia). Atencao: usa QUERY PARAMS, nao corpo JSON.
echo ">> boletim: calculando medias (matricula x disciplina)"
nmed=0
for mid in $(sql "SELECT id FROM matriculas WHERE tenant_id='$TENANT' AND (deleted=FALSE OR deleted IS NULL)"); do
  for did in "${DISC[@]}"; do
    curl -s -o /dev/null -H "$AUTH" -X POST "$B/medias/calcular?matriculaId=$mid&disciplinaId=$did&periodo=1BIMESTRE" && nmed=$((nmed+1))
  done
done
ok "medias/boletim" "$nmed"

# Matriz curricular (disciplina x curso) — sem isto a tela de Matriz fica vazia.
echo ">> matriz curricular"
nmat=0
for di in "${DISC[@]}"; do
  post /matriz-curricular "{\"cursoId\":\"$CURSO\",\"disciplinaId\":\"$di\",\"periodo\":\"ANUAL\",\"cargaHoraria\":80,\"obrigatoria\":true}" >/dev/null && nmat=$((nmat+1))
done
# Mais conteudo ministrado, para o diario nao ficar raso.
for tu in "$TURMA_A" "$TURMA_B"; do for di in "${DISC[@]}"; do for dia in 2026-08-11 2026-08-18; do
  post /conteudos-ministrados "{\"turmaId\":\"$tu\",\"disciplinaId\":\"$di\",\"data\":\"$dia\",\"descricao\":\"Atividade em grupo e registro no caderno\",\"objetivos\":\"Desenvolvimento da autonomia\"}" >/dev/null
done; done; done
ok "matriz + conteudo" "$nmat"

# Notificacoes: um canal configurado + templates por evento. Sem isto o motor
# de avisos a familia nao tem o que montar. canal e' obrigatorio na config.
echo ">> notificacoes (config + templates)"
post /access/notificacoes/configs '{"canal":"EMAIL","provider":"smtp","remetente":"avisos@mundodosaber.com","limiteDiario":500,"ativo":true}' >/dev/null
nnt=0
for ev in ENTRADA_CONFIRMADA SAIDA_CONFIRMADA RETIRADA_SOLICITADA HORARIO_EXCEDIDO; do
  post /access/notificacoes/templates "{\"evento\":\"$ev\",\"canal\":\"EMAIL\",\"assunto\":\"AlfaSchool — aviso\",\"corpo\":\"Ola {{responsavel}}, evento $ev de {{aluno}} as {{hora}}.\",\"ativo\":true}" >/dev/null && nnt=$((nnt+1))
done
ok "notificacoes (config+templates)" "$nnt"

# Fechamento de permanencia: fecha a competencia para virar hora-extra no
# financeiro. competencia = YYYY-MM.
post /access/permanencia/fechamentos "{\"unitId\":\"$UNIDADE\",\"competencia\":\"2026-08\"}" >/dev/null && ok "fechamento permanencia" "2026-08"

echo ">> restricao judicial"
post /access/restricoes "{\"alunoId\":\"${ALL_AL[0]}\",\"pessoaAutorizadaId\":\"${PESSOAS[0]}\",\"tipo\":\"JUDICIAL\",\"descricao\":\"Medida protetiva: genitor sem direito de retirada.\",\"numeroProcesso\":\"0012345-67.2026.8.08.0000\",\"orgaoEmissor\":\"Vara da Infancia\",\"vigenciaInicio\":\"2026-08-01\",\"ativo\":true}" >/dev/null
ok "restricao" "1"

echo ">> Access ao vivo: entradas (enche paineis e Coordenacao)"
p=0
for al in "${ALL_AL[@]}"; do
  [ $((RANDOM%5)) -eq 0 ] && continue   # ~20% ausentes
  code=$(curl -s -o /dev/null -w "%{http_code}" -H "$AUTH" -H 'Content-Type: application/json' -X POST "$B/access/simulador/entrada-aluno" -d "{\"alunoId\":\"$al\",\"dispositivoId\":\"$DISP_ENTRADA\"}")
  [ "$code" = "200" ] && { p=$((p+1)); PRESENTES+=("$al"); }
done
ok "alunos presentes agora" "$p"

echo ">> fila de retirada + entregas"
# Chegada na portaria gera o evento; a retirada e' aberta pela coordenacao
# (rota manual), que e' o fluxo confiavel para popular a fila na demo. O
# aluno com restricao judicial (o primeiro) e' recusado de proposito — e' a
# regra de negocio funcionando.
f=0; ab=0
for par in "${PES_ALU[@]:0:6}"; do
  pa=${par%%|*}; al=${par##*|}
  curl -s -o /dev/null -H "$AUTH" -H 'Content-Type: application/json' -X POST "$B/access/simulador/chegada-responsavel" -d "{\"pessoaAutorizadaId\":\"$pa\",\"dispositivoId\":\"$DISP_ENTRADA\"}"; f=$((f+1))
  code=$(curl -s -o /dev/null -w "%{http_code}" -H "$AUTH" -H 'Content-Type: application/json' -X POST "$B/access/retiradas/manual" \
     -d "{\"alunoId\":\"$al\",\"unitId\":\"$UNIDADE\",\"portariaId\":\"$PORTARIA\",\"pessoaAutorizadaId\":\"$pa\",\"motivo\":\"Retirada de rotina\"}")
  [ "$code" = "201" ] && ab=$((ab+1))
done
# prepara e marca pronta as duas primeiras da fila
FILA=$(get "/access/retiradas/fila?page=0&size=5" | grep -oE '"id":"[^"]+"' | cut -d'"' -f4)
i=0; for rid in $FILA; do [ $i -ge 2 ] && break
  post "/access/retiradas/$rid/preparar" '{}' >/dev/null
  post "/access/retiradas/$rid/pronto" '{}' >/dev/null
  i=$((i+1)); done
ok "chegadas na portaria" "$f"; ok "retiradas abertas" "$ab"

echo ">> ocorrencias"
# via endpoint direto (o simulador so' gera ocorrencia em alguns fluxos)
post /access/ocorrencias "{\"tipo\":\"HORARIO_EXCEDIDO\",\"gravidade\":\"MEDIA\",\"alunoId\":\"${PRESENTES[0]}\",\"descricao\":\"Aluno permaneceu alem da jornada contratada.\"}" >/dev/null
post /access/ocorrencias "{\"tipo\":\"TENTATIVA_NAO_AUTORIZADA\",\"gravidade\":\"ALTA\",\"descricao\":\"Pessoa sem autorizacao tentou retirar um aluno.\"}" >/dev/null
post /access/ocorrencias "{\"tipo\":\"EQUIPAMENTO_OFFLINE\",\"gravidade\":\"BAIXA\",\"descricao\":\"Leitor da portaria ficou offline por 3 minutos.\"}" >/dev/null
# evento de acesso negado no historico da portaria
curl -s -o /dev/null -H "$AUTH" -H 'Content-Type: application/json' -X POST "$B/access/simulador/acesso-negado" -d "{\"deviceUserId\":999,\"dispositivoId\":\"$DISP_ENTRADA\",\"motivo\":\"Fora do horario autorizado\"}"
curl -s -o /dev/null -H "$AUTH" -H 'Content-Type: application/json' -X POST "$B/access/simulador/pessoa-desconhecida" -d "{\"dispositivoId\":\"$DISP_ENTRADA\"}"
ok "ocorrencias" "2"

echo ">> fotos (avatar ilustrado p/ alunos, foto real p/ adultos e paineis)"
# Best-effort: depende de rede (dicebear/randomuser). Se falhar, o seed
# continua valido — so' fica sem foto. Reaproveita MYSQL_CONT e TENANT.
MYSQL_CONT="$MYSQL_CONT" TENANT="$TENANT" bash "$(dirname "$0")/popular-fotos.sh" \
  || echo "  (fotos: pulado — rode ./scripts/popular-fotos.sh depois)"

echo ">> tokens de TV dos paineis (URLs prontas para abrir na parede)"
API="$API" MYSQL_CONT="$MYSQL_CONT" TENANT="$TENANT" bash "$(dirname "$0")/paineis-tv.sh" \
  || echo "  (tokens: pulado — rode ./scripts/paineis-tv.sh depois)"

echo ">> pronto. Alunos: ${#ALL_AL[@]} | Presentes: $p"
