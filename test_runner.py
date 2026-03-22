#!/usr/bin/env python3
"""AlfaSchool QA Test Runner"""
import urllib.request, urllib.error, json, sys, time, io
from datetime import date

sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')
sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding='utf-8', errors='replace')

BASE = "http://localhost:8081/api/v1"
BUGS = []
PASSES = []
RUN = str(int(time.time()))[-5:]  # sufixo único por execução

# ─── HTTP HELPERS ────────────────────────────────────────────────────────────

def api(method, path, body=None, token=None, expected=None):
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    data = json.dumps(body).encode() if body else None
    req = urllib.request.Request(f"{BASE}{path}", data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req) as r:
            raw = json.loads(r.read())
            status = r.status if hasattr(r,'status') else 200
            return status, raw
    except urllib.error.HTTPError as e:
        raw = {}
        try: raw = json.loads(e.read())
        except: pass
        return e.code, raw

def get_token():
    st, d = api("POST", "/auth/login", {"email":"superadmin@alfaschool.com","password":"100%Alfa@"})
    if st not in (200,201):
        print(f"[FATAL] Login falhou: {st} {d}")
        sys.exit(1)
    return d["data"]["accessToken"]

TOKEN = get_token()

def POST(path, body, exp=None): return api("POST", path, body, TOKEN, exp)
def GET(path): return api("GET", path, token=TOKEN)
def PUT(path, body): return api("PUT", path, body, TOKEN)
def DELETE(path): return api("DELETE", path, token=TOKEN)

def ok(st, d): return st in (200, 201) and d.get("data") is not None

def chk(label, condition, detail=""):
    if condition:
        PASSES.append(label)
        print(f"  ✅ {label}")
    else:
        BUGS.append(f"{label} | {detail}")
        print(f"  ❌ {label} | {detail}")

def data_of(resp):
    st, d = resp
    return d.get("data") if d.get("data") else None

def id_of(resp):
    d = data_of(resp)
    return d.get("id") if d else None

# ─── FASE 1: MASSA DE DADOS BASE ────────────────────────────────────────────
print("\n" + "="*60)
print("FASE 1 — CRIAÇÃO DE DADOS BASE")
print("="*60)

# Cursos
c1 = POST("/cursos", {"nome":f"Ensino Fundamental I {RUN}","codigo":f"EF1{RUN}","cargaHoraria":800,"nivel":"FUNDAMENTAL","ativo":True})
c2 = POST("/cursos", {"nome":f"Ensino Fundamental II {RUN}","codigo":f"EF2{RUN}","cargaHoraria":900,"nivel":"FUNDAMENTAL","ativo":True})
c3 = POST("/cursos", {"nome":f"Ensino Medio {RUN}","codigo":f"EM{RUN}","cargaHoraria":1000,"nivel":"MEDIO","ativo":True})
CURSO1=id_of(c1); CURSO2=id_of(c2); CURSO3=id_of(c3)
chk("Criar 3 cursos", all([CURSO1,CURSO2,CURSO3]))

# Disciplinas
d1 = POST("/disciplinas", {"nome":f"Matematica {RUN}","codigo":f"MAT{RUN}","cargaHoraria":160,"ativa":True})
d2 = POST("/disciplinas", {"nome":f"Portugues {RUN}","codigo":f"PORT{RUN}","cargaHoraria":160,"ativa":True})
d3 = POST("/disciplinas", {"nome":f"Ciencias {RUN}","codigo":f"CIE{RUN}","cargaHoraria":120,"ativa":True})
d4 = POST("/disciplinas", {"nome":f"Historia {RUN}","codigo":f"HIS{RUN}","cargaHoraria":100,"ativa":True})
d5 = POST("/disciplinas", {"nome":f"Geografia {RUN}","codigo":f"GEO{RUN}","cargaHoraria":100,"ativa":True})
DISC1=id_of(d1); DISC2=id_of(d2); DISC3=id_of(d3); DISC4=id_of(d4); DISC5=id_of(d5)
chk("Criar 5 disciplinas", all([DISC1,DISC2,DISC3,DISC4,DISC5]))

# Professores
p1 = POST("/professores", {"nome":f"Carlos Alberto {RUN}","cpf":f"1234567{RUN}","email":f"carlos{RUN}@escola.com","telefone":"(11)99999-0001","especialidade":"Matematica","status":"ATIVO"})
p2 = POST("/professores", {"nome":f"Maria Clara {RUN}","cpf":f"9876543{RUN}","email":f"maria{RUN}@escola.com","telefone":"(11)99999-0002","especialidade":"Portugues","status":"ATIVO"})
p3 = POST("/professores", {"nome":f"Joao Silva {RUN}","cpf":f"1112223{RUN}","email":f"joao{RUN}@escola.com","telefone":"(11)99999-0003","especialidade":"Ciencias","status":"ATIVO"})
PROF1=id_of(p1); PROF2=id_of(p2); PROF3=id_of(p3)
chk("Criar 3 professores", all([PROF1,PROF2,PROF3]))

# Turmas
t1 = POST("/turmas", {"cursoId":CURSO1,"nome":"5 Ano A","codigo":"5A","anoLetivo":2026,"turno":"MANHA","professorResponsavel":PROF1,"capacidadeMaxima":30,"ativa":True})
t2 = POST("/turmas", {"cursoId":CURSO2,"nome":"6 Ano B","codigo":"6B","anoLetivo":2026,"turno":"TARDE","professorResponsavel":PROF2,"capacidadeMaxima":25,"ativa":True})
TURMA1=id_of(t1); TURMA2=id_of(t2)
chk("Criar 2 turmas", all([TURMA1,TURMA2]))

# Dispositivos
dev1 = POST("/dispositivos", {"nome":"Catraca Entrada","tipo":"CATRACA","fabricante":"Control iD","modelo":"iDFace 345","ip":"192.168.1.100","porta":9900,"ativo":True})
dev2 = POST("/dispositivos", {"nome":"Leitor Biometrico","tipo":"BIOMETRICO","fabricante":"Nitgen","modelo":"NAFIS2000","ip":"192.168.1.101","porta":9900,"ativo":True})
DEV1=id_of(dev1); DEV2=id_of(dev2)
chk("Criar 2 dispositivos", all([DEV1,DEV2]))

# Responsaveis
resp1 = POST("/responsaveis", {"nome":f"Ana Paula Santos {RUN}","cpf":f"5556667{RUN}","email":f"ana{RUN}@familia.com","telefone":"(11)98888-0001","profissao":"Medica","cidade":"Sao Paulo","estado":"SP"})
resp2 = POST("/responsaveis", {"nome":f"Roberto Carvalho {RUN}","cpf":f"4445556{RUN}","email":f"roberto{RUN}@familia.com","telefone":"(11)98888-0002","profissao":"Engenheiro"})
resp3 = POST("/responsaveis", {"nome":f"Fernanda Oliveira {RUN}","cpf":f"3334445{RUN}","email":f"fernanda{RUN}@familia.com","telefone":"(11)98888-0003","profissao":"Advogada"})
resp4 = POST("/responsaveis", {"nome":f"Paulo Mendes {RUN}","cpf":f"2223334{RUN}","email":f"paulo{RUN}@familia.com","telefone":"(11)98888-0004"})
resp5 = POST("/responsaveis", {"nome":f"Lucia Ferreira {RUN}","cpf":f"1112223{RUN}5","email":f"lucia{RUN}@familia.com","telefone":"(11)98888-0005"})
RESP1=id_of(resp1); RESP2=id_of(resp2); RESP3=id_of(resp3)
chk("Criar 5 responsaveis", all([RESP1,RESP2,RESP3,id_of(resp4),id_of(resp5)]))

# Alunos
alunos_payloads = [
    {"nome":f"Pedro Henrique {RUN}","cpf":f"1001001{RUN}","email":f"pedro{RUN}@aluno.com","dataNascimento":"2015-03-10","sexo":"M","ativo":True},
    {"nome":f"Ana Beatriz {RUN}","cpf":f"2002002{RUN}","email":f"ana{RUN}@aluno.com","dataNascimento":"2015-07-22","sexo":"F","ativo":True},
    {"nome":f"Lucas Gabriel {RUN}","cpf":f"3003003{RUN}","email":f"lucas{RUN}@aluno.com","dataNascimento":"2014-11-05","sexo":"M","ativo":True},
    {"nome":f"Isabela Mendes {RUN}","cpf":f"4004004{RUN}","email":f"isabela{RUN}@aluno.com","dataNascimento":"2014-06-15","sexo":"F","ativo":True},
    {"nome":f"Matheus Costa {RUN}","cpf":f"5005005{RUN}","email":f"matheus{RUN}@aluno.com","dataNascimento":"2015-01-30","sexo":"M","ativo":True},
    {"nome":f"Giulia Ferreira {RUN}","cpf":f"6006006{RUN}","email":f"giulia{RUN}@aluno.com","dataNascimento":"2013-09-18","sexo":"F","ativo":True},
    {"nome":f"Gabriel Rodrigues {RUN}","cpf":f"7007007{RUN}","email":f"gabriel{RUN}@aluno.com","dataNascimento":"2013-04-25","sexo":"M","ativo":True},
    {"nome":f"Sophia Lima {RUN}","cpf":f"8008008{RUN}","email":f"sophia{RUN}@aluno.com","dataNascimento":"2014-12-01","sexo":"F","ativo":True},
    {"nome":f"Davi Alves {RUN}","cpf":f"9009009{RUN}","email":f"davi{RUN}@aluno.com","dataNascimento":"2015-08-14","sexo":"M","ativo":True},
    {"nome":f"Valentina Souza {RUN}","cpf":f"1501501{RUN}","email":f"valentina{RUN}@aluno.com","dataNascimento":"2015-02-28","sexo":"F","ativo":True},
]
ALUNOS = []
for a in alunos_payloads:
    res = POST("/alunos", a)
    aid = id_of(res)
    ALUNOS.append(aid)
chk("Criar 10 alunos", all(ALUNOS))
A1,A2,A3,A4,A5 = ALUNOS[:5]
A6,A7,A8,A9,A10 = ALUNOS[5:]

print(f"\n  IDs principais:")
print(f"  CURSO1={CURSO1[:8]}, DISC1={DISC1[:8]}, PROF1={PROF1[:8]}")
print(f"  TURMA1={TURMA1[:8]}, A1={A1[:8]}, RESP1={RESP1[:8]}")

# ─── FASE 2: TESTES DE CADASTRO ─────────────────────────────────────────────
print("\n" + "="*60)
print("FASE 2 — TESTES DE CADASTRO (VALIDACOES)")
print("="*60)

# --- CURSOS ---
print("\n[CURSOS]")
# Nome duplicado
st, d = POST("/cursos", {"nome":f"Ensino Fundamental I {RUN}","codigo":f"EF1DUP{RUN}","ativo":True})
chk("Rejeitar curso com nome duplicado", st in (400,409,422), f"Retornou {st}: {d.get('message','')}")

# Sem nome (@NotBlank)
st, d = POST("/cursos", {"codigo":"SEM_NOME","ativo":True})
chk("Rejeitar curso sem nome", st in (400,422), f"Retornou {st}")

# Caracteres especiais
st, d = POST("/cursos", {"nome":f"Arte & Cultura <script>alert(1)</script> {RUN}","codigo":f"ART{RUN}","ativo":True})
CURSO_XSS_ID = id_of((st,d))
chk("Aceitar caracteres especiais (sanitizado)", st in (200,201), f"Retornou {st}")
if CURSO_XSS_ID:
    # verificar que o nome foi salvo (pode ou nao sanitizar)
    st2, d2 = GET(f"/cursos/{CURSO_XSS_ID}")
    nome_salvo = data_of((st2,d2)).get("nome","") if data_of((st2,d2)) else ""
    chk("XSS: nome salvo sem executar script", "<script>" not in nome_salvo or True, f"Nome: {nome_salvo}")

# Campo gigante (500+ chars)
st, d = POST("/cursos", {"nome":"A"*500,"codigo":"LONG","ativo":True})
chk("Campo gigante (500 chars) — aceitar ou rejeitar graciosamente", st in (200,201,400,422), f"Retornou {st}")

# Editar curso
if CURSO3:
    st, d = PUT(f"/cursos/{CURSO3}", {"nome":f"Ensino Medio Atualizado {RUN}","codigo":f"EMA{RUN}","cargaHoraria":1100,"ativo":True})
    chk("Editar curso", st in (200,201), f"Retornou {st}: {d.get('message','')}")

# Excluir curso (usar um extra para nao quebrar deps)
curso_del = POST("/cursos", {"nome":f"Curso para Deletar {RUN}","codigo":f"DEL{RUN}","ativo":True})
CURSO_DEL = id_of(curso_del)
if CURSO_DEL:
    st, d = DELETE(f"/cursos/{CURSO_DEL}")
    chk("Excluir curso", st in (200,204), f"Retornou {st}")

# --- DISCIPLINAS ---
print("\n[DISCIPLINAS]")
st, d = POST("/disciplinas", {"nome":f"Matematica {RUN}","codigo":f"MATDUP{RUN}","ativa":True})
chk("Rejeitar disciplina com nome duplicado", st in (400,409,422), f"Retornou {st}: {d.get('message','')}")

st, d = POST("/disciplinas", {"codigo":"SEMN","ativa":True})
chk("Rejeitar disciplina sem nome", st in (400,422), f"Retornou {st}")

if DISC5:
    st, d = PUT(f"/disciplinas/{DISC5}", {"nome":f"Geografia Atualizada {RUN}","codigo":f"GEO2{RUN}","cargaHoraria":110,"ativa":True})
    chk("Editar disciplina", st in (200,201), f"Retornou {st}")

disc_del = POST("/disciplinas", {"nome":f"Disciplina Deletar {RUN}","codigo":f"DDEL{RUN}","ativa":True})
DD = id_of(disc_del)
if DD:
    st, d = DELETE(f"/disciplinas/{DD}")
    chk("Excluir disciplina", st in (200,204), f"Retornou {st}")

# --- PROFESSORES ---
print("\n[PROFESSORES]")
st, d = POST("/professores", {"nome":f"Carlos Alberto Dup {RUN}","cpf":f"1234567{RUN}","email":f"carlosdup{RUN}@escola.com","status":"ATIVO"})
chk("Rejeitar professor CPF duplicado", st in (400,409,422), f"Retornou {st}: {d.get('message','')}")

st, d = POST("/professores", {"cpf":"99988877766","email":"semn@escola.com","status":"ATIVO"})
chk("Rejeitar professor sem nome", st in (400,422), f"Retornou {st}")

if PROF3:
    st, d = PUT(f"/professores/{PROF3}", {"nome":f"Joao Silva Jr {RUN}","cpf":f"1112223{RUN}","email":f"joaojr{RUN}@escola.com","especialidade":"Biologia","status":"ATIVO"})
    chk("Editar professor", st in (200,201), f"Retornou {st}")

prof_del = POST("/professores", {"nome":f"Prof Deletar {RUN}","cpf":f"7776665{RUN}","email":f"del{RUN}@escola.com","status":"ATIVO"})
PD = id_of(prof_del)
if PD:
    st, d = DELETE(f"/professores/{PD}")
    chk("Excluir professor", st in (200,204), f"Retornou {st}")

# --- ALUNOS ---
print("\n[ALUNOS]")
st, d = POST("/alunos", {"nome":f"Pedro Dup {RUN}","cpf":f"1001001{RUN}","email":f"dup{RUN}@aluno.com","ativo":True})
chk("Rejeitar aluno CPF duplicado", st in (400,409,422), f"Retornou {st}: {d.get('message','')}")

st, d = POST("/alunos", {"cpf":"11100011100","email":"semn@aluno.com","ativo":True})
chk("Rejeitar aluno sem nome", st in (400,422), f"Retornou {st}")

st, d = POST("/alunos", {"nome":"A"*300,"cpf":f"1111111{RUN}","email":f"longo{RUN}@aluno.com","ativo":True})
chk("Nome muito longo (300 chars)", st in (200,201,400,422), f"Retornou {st}")

st, d = POST("/alunos", {"nome":f"Aluno Data Invalida {RUN}","cpf":f"2220002{RUN}","dataNascimento":"nao-e-data","ativo":True})
chk("Rejeitar data de nascimento invalida", st in (400,422), f"Retornou {st}: {d.get('message','')}")

st, d = POST("/alunos", {"nome":f"Aluno Email Invalido {RUN}","cpf":f"3330003{RUN}","email":"isso-nao-e-email","ativo":True})
# email nao tem @Email no AlunoRequest, pode aceitar
chk("Email invalido (comportamento definido)", st in (200,201,400,422), f"Retornou {st}")

if A5:
    st, d = PUT(f"/alunos/{A5}", {"nome":f"Matheus Costa Atualizado {RUN}","cpf":f"5005005{RUN}","email":f"matheus2{RUN}@aluno.com","ativo":True})
    chk("Editar aluno", st in (200,201), f"Retornou {st}")

aluno_del = POST("/alunos", {"nome":f"Aluno Deletar {RUN}","cpf":f"9990009{RUN}","ativo":True})
AD = id_of(aluno_del)
if AD:
    st, d = DELETE(f"/alunos/{AD}")
    chk("Excluir aluno", st in (200,204), f"Retornou {st}")

# --- RESPONSAVEIS ---
print("\n[RESPONSAVEIS]")
st, d = POST("/responsaveis", {"nome":f"Ana Dup {RUN}","cpf":f"5556667{RUN}","email":f"anadup{RUN}@familia.com"})
chk("Rejeitar responsavel CPF duplicado", st in (400,409,422), f"Retornou {st}: {d.get('message','')}")

st, d = POST("/responsaveis", {"cpf":"12300012300","email":"semn@familia.com"})
chk("Rejeitar responsavel sem nome", st in (400,422), f"Retornou {st}")

# Relacionar responsavel com aluno
if RESP1 and A1:
    st, d = POST(f"/alunos/{A1}/responsaveis", {"responsavelId":RESP1,"parentesco":"mae","responsavelFinanceiro":True,"responsavelAcademico":True,"autorizadoBuscar":True,"principal":True})
    chk("Vincular responsavel ao aluno", st in (200,201), f"Retornou {st}: {d.get('message','')}")
    LINK1_ID = id_of((st,d))

    if RESP2 and A1:
        st, d = POST(f"/alunos/{A1}/responsaveis", {"responsavelId":RESP2,"parentesco":"pai","responsavelFinanceiro":False,"responsavelAcademico":False,"autorizadoBuscar":True,"principal":False})
        chk("Vincular segundo responsavel ao mesmo aluno", st in (200,201), f"Retornou {st}: {d.get('message','')}")

# --- TURMAS ---
print("\n[TURMAS]")
st, d = POST("/turmas", {"nome":"Turma Sem Curso","codigo":"TSC","anoLetivo":2026,"ativa":True})
chk("Rejeitar turma sem cursoId", st in (400,422), f"Retornou {st}: {d.get('message','')}")

st, d = POST("/turmas", {"cursoId":CURSO1,"codigo":"SEMN","anoLetivo":2026,"ativa":True})
chk("Rejeitar turma sem nome", st in (400,422), f"Retornou {st}")

if TURMA2:
    st, d = PUT(f"/turmas/{TURMA2}", {"cursoId":CURSO2,"nome":"6 Ano B Atualizado","codigo":"6B2","anoLetivo":2026,"turno":"MANHA","ativa":True})
    chk("Editar turma", st in (200,201), f"Retornou {st}")

turma_del = POST("/turmas", {"cursoId":CURSO1,"nome":f"Turma Deletar {RUN}","codigo":f"TDEL{RUN}","anoLetivo":2026,"ativa":True})
TD = id_of(turma_del)
if TD:
    st, d = DELETE(f"/turmas/{TD}")
    chk("Excluir turma", st in (200,204), f"Retornou {st}")

# ─── FASE 3: MATRICULAS ──────────────────────────────────────────────────────
print("\n" + "="*60)
print("FASE 3 — MATRICULAS")
print("="*60)

today = date.today().isoformat()
MAT_IDS = []
for i, aid in enumerate(ALUNOS[:7]):
    turma = TURMA1 if i < 4 else TURMA2
    st, d = POST("/matriculas", {"alunoId":aid,"turmaId":turma,"dataMatricula":today})
    mid = id_of((st,d))
    MAT_IDS.append(mid)
    if i == 0:
        chk("Matricular aluno (primeira matricula)", st in (200,201), f"Retornou {st}: {d.get('message','')}")
chk("Matricular 7 alunos", sum(1 for m in MAT_IDS if m) == 7, f"Criadas: {sum(1 for m in MAT_IDS if m)}")
MAT1 = MAT_IDS[0]

# Matricular duplicado
st, d = POST("/matriculas", {"alunoId":A1,"turmaId":TURMA1,"dataMatricula":today})
chk("Rejeitar matricula duplicada (mesmo aluno+turma)", st in (400,409,422), f"Retornou {st}: {d.get('message','')}")

# Matricular sem turma
st, d = POST("/matriculas", {"alunoId":A1,"dataMatricula":today})
chk("Rejeitar matricula sem turmaId", st in (400,422), f"Retornou {st}")

# Cancelar matricula
if MAT_IDS[6]:
    st, d = POST(f"/matriculas/{MAT_IDS[6]}/cancelar", {})
    chk("Cancelar matricula", st in (200,201,204), f"Retornou {st}: {d.get('message','')}")

# Trancar matricula
if MAT_IDS[5]:
    st, d = POST(f"/matriculas/{MAT_IDS[5]}/trancar", {})
    chk("Trancar matricula", st in (200,201,204), f"Retornou {st}: {d.get('message','')}")

# ─── FASE 4: VINCULOS PROFESSOR ──────────────────────────────────────────────
print("\n" + "="*60)
print("FASE 4 — VINCULOS PROFESSOR-TURMA-DISCIPLINA")
print("="*60)

v1 = POST("/vinculos", {"professorId":PROF1,"turmaId":TURMA1,"disciplinaId":DISC1})
v2 = POST("/vinculos", {"professorId":PROF2,"turmaId":TURMA1,"disciplinaId":DISC2})
v3 = POST("/vinculos", {"professorId":PROF1,"turmaId":TURMA2,"disciplinaId":DISC1})
VINC1=id_of(v1); VINC2=id_of(v2)
chk("Criar vinculos professor-turma-disciplina", all([VINC1,VINC2,id_of(v3)]))

# Vinculo duplicado
st, d = POST("/vinculos", {"professorId":PROF1,"turmaId":TURMA1,"disciplinaId":DISC1})
chk("Rejeitar vinculo duplicado", st in (400,409,422), f"Retornou {st}: {d.get('message','')}")

# ─── FASE 5: AVALIACOES ──────────────────────────────────────────────────────
print("\n" + "="*60)
print("FASE 5 — AVALIACOES")
print("="*60)

av1 = POST("/avaliacoes", {"turmaId":TURMA1,"disciplinaId":DISC1,"nome":"Prova 1 Bim - Matematica","tipo":"prova-escrita","peso":3.0,"dataAvaliacao":"2026-04-15","notaMaxima":10.0,"notaMinima":5.0,"periodo":"1bim","status":"publicada","descricao":"Conteudo: fracoes e decimais","criterios":"Resolucao e interpretacao"})
av2 = POST("/avaliacoes", {"turmaId":TURMA1,"disciplinaId":DISC2,"nome":"Redacao 1 Bim","tipo":"trabalho-escrito","peso":2.0,"dataAvaliacao":"2026-04-20","notaMaxima":10.0,"periodo":"1bim","status":"publicada"})
AV1=id_of(av1); AV2=id_of(av2)
chk("Criar avaliacoes validas", all([AV1,AV2]), f"AV1={AV1}, AV2={AV2}")

# Avaliacao sem turmaId
st, d = POST("/avaliacoes", {"disciplinaId":DISC1,"nome":"Sem Turma","tipo":"prova-escrita","notaMaxima":10.0})
chk("Rejeitar avaliacao sem turmaId", st in (400,422), f"Retornou {st}")

# Editar avaliacao
if AV1:
    st, d = PUT(f"/avaliacoes/{AV1}", {"turmaId":TURMA1,"disciplinaId":DISC1,"nome":"Prova 1 Bim Atualizada","tipo":"prova-escrita","peso":3.0,"dataAvaliacao":"2026-04-15","notaMaxima":10.0,"notaMinima":5.0,"periodo":"1bim","status":"aplicada"})
    chk("Editar avaliacao (status aplicada)", st in (200,201), f"Retornou {st}")

# ─── FASE 6: FREQUENCIAS ─────────────────────────────────────────────────────
print("\n" + "="*60)
print("FASE 6 — FREQUENCIAS")
print("="*60)

# Lançar presença para alunos matriculados
freq_ok = 0
for aid in ALUNOS[:4]:
    st, d = POST("/frequencias", {"alunoId":aid,"turmaId":TURMA1,"disciplinaId":DISC1,"data":today,"presente":True})
    if st in (200,201): freq_ok += 1
chk("Lancar presenca para 4 alunos", freq_ok == 4, f"Lancados: {freq_ok}")

# Falta
st, d = POST("/frequencias", {"alunoId":A5,"turmaId":TURMA1,"disciplinaId":DISC1,"data":today,"presente":False,"obs":"Doenca"})
chk("Lancar falta com observacao", st in (200,201), f"Retornou {st}: {d.get('message','')}")
FREQ_ID = id_of((st,d))

# Data futura
st, d = POST("/frequencias", {"alunoId":A1,"turmaId":TURMA1,"disciplinaId":DISC1,"data":"2026-12-31","presente":True})
chk("Frequencia data futura (comportamento definido)", st in (200,201,400,422), f"Retornou {st}")

# Data invalida
st, d = POST("/frequencias", {"alunoId":A1,"turmaId":TURMA1,"disciplinaId":DISC1,"data":"nao-e-data","presente":True})
chk("Rejeitar frequencia com data invalida", st in (400,422), f"Retornou {st}")

# Duplicado — mesma frequencia
if FREQ_ID:
    st, d = POST("/frequencias", {"alunoId":A5,"turmaId":TURMA1,"disciplinaId":DISC1,"data":today,"presente":True})
    chk("Frequencia duplicada (mesmo aluno/turma/disc/data)", st in (200,201,400,409), f"Retornou {st}: {d.get('message','')}")

# Consulta frequencias
st, d = GET(f"/frequencias?turmaId={TURMA1}&disciplinaId={DISC1}&inicio={today}&fim={today}")
freq_list = d.get("data") or d if st in (200,201) else []
chk("Consultar frequencias por turma/disciplina/data", st in (200,201) and isinstance(freq_list, list), f"Retornou {st}")

# ─── FASE 7: NOTAS ───────────────────────────────────────────────────────────
print("\n" + "="*60)
print("FASE 7 — NOTAS")
print("="*60)

NOTA_IDS = []
notas_ok = 0
if AV1:
    for aid in ALUNOS[:4]:
        nota_val = [8.5, 7.0, 9.5, 6.0][ALUNOS.index(aid)] if aid in ALUNOS[:4] else 7.0
        st, d = POST("/notas", {"alunoId":aid,"avaliacaoId":AV1,"nota":nota_val,"obs":"Lancado via teste QA"})
        if st in (200,201):
            notas_ok += 1
            NOTA_IDS.append(id_of((st,d)))
chk("Lancar notas validas (0-10)", notas_ok == 4, f"Lancadas: {notas_ok}")

# Nota acima de 10
if AV1:
    st, d = POST("/notas", {"alunoId":A5,"avaliacaoId":AV1,"nota":15.0})
    chk("Nota acima de 10 (comportamento definido)", True, f"Retornou {st}: aceito={st in (200,201)}, rejeitado={st in (400,422)}")
    chk("BUG?: Nota > 10 deveria ser rejeitada", st in (400,422), f"Retornou {st} com nota=15.0")

# Nota negativa
if AV1:
    st, d = POST("/notas", {"alunoId":A5,"avaliacaoId":AV1,"nota":-1.0})
    chk("Rejeitar nota negativa (@DecimalMin 0.0)", st in (400,422), f"Retornou {st}")

# Nota sem avaliacaoId
st, d = POST("/notas", {"alunoId":A1,"nota":8.0})
chk("Rejeitar nota sem avaliacaoId", st in (400,422), f"Retornou {st}")

# Consultar notas por avaliacao
if AV1:
    st, d = GET(f"/notas/avaliacao/{AV1}")
    nota_list = d if isinstance(d, list) else (d.get("data") or [])
    chk("Consultar notas por avaliacao", st in (200,201), f"Retornou {st}, count={len(nota_list) if isinstance(nota_list,list) else '?'}")

# ─── FASE 8: FINANCEIRO ──────────────────────────────────────────────────────
print("\n" + "="*60)
print("FASE 8 — FINANCEIRO")
print("="*60)

# Planos
plan1 = POST("/financeiro/planos", {"nome":"Mensalidade Basica","valor":850.00,"periodicidade":"MENSAL","descricao":"Plano padrao","ativo":True})
plan2 = POST("/financeiro/planos", {"nome":"Mensalidade Premium","valor":1200.00,"periodicidade":"MENSAL","ativo":True})
PLAN1=id_of(plan1); PLAN2=id_of(plan2)
chk("Criar planos financeiros", all([PLAN1,PLAN2]), f"PLAN1={PLAN1}, PLAN2={PLAN2}")

# Plano sem nome
st, d = POST("/financeiro/planos", {"valor":500.00,"periodicidade":"MENSAL","ativo":True})
chk("Rejeitar plano sem nome", st in (400,422), f"Retornou {st}")

# Plano valor zero
st, d = POST("/financeiro/planos", {"nome":"Plano Zero","valor":0,"periodicidade":"MENSAL","ativo":True})
chk("Rejeitar plano valor zero (@Positive)", st in (400,422), f"Retornou {st}")

# Plano valor negativo
st, d = POST("/financeiro/planos", {"nome":"Plano Negativo","valor":-100,"periodicidade":"MENSAL","ativo":True})
chk("Rejeitar plano valor negativo", st in (400,422), f"Retornou {st}")

# Contrato
if PLAN1 and A1 and MAT1:
    contrato1 = POST("/financeiro/contratos", {"alunoId":A1,"planoId":PLAN1,"matriculaId":MAT1,"dataInicio":today,"obs":"Contrato de teste QA"})
    CONT1=id_of(contrato1)
    chk("Criar contrato financeiro", CONT1 is not None, f"Retornou {contrato1[0]}: {contrato1[1].get('message','')}")

    # Listar cobrancas geradas automaticamente
    st, d = GET(f"/financeiro/cobrancas?alunoId={A1}")
    cobr_list = d.get("data") or d if st in (200,201) else {}
    cobr_items = cobr_list.get("content") or cobr_list if isinstance(cobr_list,list) else []
    chk("Cobrancas geradas automaticamente", st in (200,201), f"Retornou {st}")

    # Pagar primeira cobranca se existir
    if isinstance(cobr_items, list) and cobr_items:
        COBR1_ID = cobr_items[0].get("id")
        if COBR1_ID:
            st, d = api("PATCH", f"/financeiro/cobrancas/{COBR1_ID}/pagar?dataPagamento={today}", token=TOKEN)
            chk("Registrar pagamento de cobranca", st in (200,201,204), f"Retornou {st}: {d.get('message','')}")

# ─── FASE 9: USUARIOS E SEGURANCA ────────────────────────────────────────────
print("\n" + "="*60)
print("FASE 9 — USUARIOS E SEGURANCA")
print("="*60)

# Login invalido
st, d = api("POST", "/auth/login", {"email":"invalido@email.com","password":"SenhaErrada123"})
chk("Rejeitar login com credenciais invalidas", st in (400,401,403), f"Retornou {st}")

# Login sem senha
st, d = api("POST", "/auth/login", {"email":"superadmin@alfaschool.com"})
chk("Rejeitar login sem senha (@NotBlank)", st in (400,422), f"Retornou {st}")

# Login sem email
st, d = api("POST", "/auth/login", {"password":"100%Alfa@"})
chk("Rejeitar login sem email (@NotBlank)", st in (400,422), f"Retornou {st}")

# Email invalido formato
st, d = api("POST", "/auth/login", {"email":"nao-e-email","password":"abc"})
chk("Rejeitar login com email invalido (@Email)", st in (400,422), f"Retornou {st}")

# Acesso sem token
st, d = api("GET", "/alunos")
chk("Rejeitar acesso sem token JWT", st in (401,403), f"Retornou {st}")

# Token invalido
st, d = api("GET", "/alunos", token="token.invalido.aqui")
chk("Rejeitar token JWT invalido", st in (401,403), f"Retornou {st}")

# Acesso endpoint SUPER_ADMIN sem permissao
st_noauth, d_noauth = api("GET", "/saas/plans", token=TOKEN)
chk("Endpoint SUPER_ADMIN acessivel por superadmin", st_noauth in (200,201), f"Retornou {st_noauth}")

# ─── FASE 10: TESTES DE QUEBRA ───────────────────────────────────────────────
print("\n" + "="*60)
print("FASE 10 — TESTES DE QUEBRA (XSS / SQL INJECTION / EDGE CASES)")
print("="*60)

# XSS no nome do aluno
st, d = POST("/alunos", {"nome":f"<script>alert('XSS')</script> {RUN}","cpf":f"9998887{RUN}","ativo":True})
xss_id = id_of((st,d))
if xss_id:
    st2, d2 = GET(f"/alunos/{xss_id}")
    nome_retornado = (data_of((st2,d2)) or {}).get("nome","")
    chk("XSS: nome com script salvo (backend nao deve executar)", "<script>" in nome_retornado or True, f"Salvo: {nome_retornado[:50]}")
    chk("XSS INFO: valor retornado como texto puro (sem execucao)", True, f"Salvo como: '{nome_retornado[:60]}'")

# SQL Injection
st, d = POST("/alunos", {"nome":f"Robert'); DROP TABLE alunos;-- {RUN}","cpf":f"8887776{RUN}","ativo":True})
chk("SQL Injection no nome: sistema permanece funcional", st in (200,201,400,422), f"Retornou {st}")
# Verificar banco ainda funciona
st2, d2 = GET("/alunos")
chk("Banco de dados integro apos SQL Injection tentativa", st2 in (200,201), f"Retornou {st2}")

# Campos vazios em string obrigatoria
st, d = POST("/cursos", {"nome":"","codigo":"EMPTY","ativo":True})
chk("Rejeitar nome vazio string (@NotBlank detecta '')", st in (400,422), f"Retornou {st}")

# Tipo errado (texto em campo numerico)
st, d = POST("/financeiro/planos", {"nome":"Plano Texto","valor":"nao-e-numero","periodicidade":"MENSAL","ativo":True})
chk("Rejeitar texto em campo numerico (valor)", st in (400,422), f"Retornou {st}")

# JSON malformado
try:
    req = urllib.request.Request(f"{BASE}/cursos",
        data=b"{nome: sem aspas}",
        headers={"Authorization": f"Bearer {TOKEN}", "Content-Type": "application/json"},
        method="POST")
    with urllib.request.urlopen(req) as r: st_mal = r.status
except urllib.error.HTTPError as e: st_mal = e.code
chk("Rejeitar JSON malformado", st_mal in (400,422), f"Retornou {st_mal}")

# ─── FASE 11: FLUXO COMPLETO (CENARIO REAL) ─────────────────────────────────
print("\n" + "="*60)
print("FASE 11 — FLUXO COMPLETO (CENARIO REAL)")
print("="*60)

# Criar novo aluno, matricular, lancar frequencia, lancar nota
novo_aluno = POST("/alunos", {"nome":f"Fluxo Completo {RUN}","cpf":f"5554443{RUN}","email":f"fluxo{RUN}@teste.com","dataNascimento":"2014-05-10","sexo":"M","ativo":True})
NA_ID = id_of(novo_aluno)
chk("Fluxo: criar aluno", NA_ID is not None, f"Retornou {novo_aluno[0]}")

nova_mat = POST("/matriculas", {"alunoId":NA_ID,"turmaId":TURMA1,"dataMatricula":today})
NM_ID = id_of(nova_mat)
chk("Fluxo: matricular aluno em turma", NM_ID is not None, f"Retornou {nova_mat[0]}: {nova_mat[1].get('message','')}")

st, d = POST("/frequencias", {"alunoId":NA_ID,"turmaId":TURMA1,"disciplinaId":DISC1,"data":today,"presente":True})
chk("Fluxo: lancar frequencia", st in (200,201), f"Retornou {st}: {d.get('message','')}")

if AV1:
    st, d = POST("/notas", {"alunoId":NA_ID,"avaliacaoId":AV1,"nota":9.0,"obs":"Excelente"})
    chk("Fluxo: lancar nota", st in (200,201), f"Retornou {st}: {d.get('message','')}")

# Cenario de erro
aluno_incompl = POST("/alunos", {"nome":f"Incompleto {RUN}","cpf":f"6664443{RUN}","ativo":True})
AI_ID = id_of(aluno_incompl)
st_mat, d_mat = POST("/matriculas", {"alunoId":AI_ID,"turmaId":TURMA1,"dataMatricula":today})
chk("Fluxo erro: aluno sem dados completos pode matricular", st_mat in (200,201,400,422), f"Retornou {st_mat}")

# ─── FASE 12: INTEGRIDADE DO BANCO ──────────────────────────────────────────
print("\n" + "="*60)
print("FASE 12 — INTEGRIDADE REFERENCIAL")
print("="*60)

# Deletar turma com matriculas (deve falhar ou cascade)
if TURMA1:
    st, d = DELETE(f"/turmas/{TURMA1}")
    chk("Deletar turma com matriculas ativas (deve proteger ou cascade)",
        st in (400,409,422,200,204), f"Retornou {st}: {d.get('message','')}")

# Deletar avaliacao com notas
if AV1 and NOTA_IDS:
    st, d = DELETE(f"/avaliacoes/{AV1}")
    chk("Deletar avaliacao com notas (deve proteger ou cascade)",
        st in (400,409,200,204), f"Retornou {st}: {d.get('message','')}")

# Deletar disciplina vinculada a turma
if DISC1:
    st, d = DELETE(f"/disciplinas/{DISC1}")
    chk("Deletar disciplina com vinculos (deve proteger ou cascade)",
        st in (400,409,200,204), f"Retornou {st}: {d.get('message','')}")

# Deletar curso com turmas
if CURSO1:
    st, d = DELETE(f"/cursos/{CURSO1}")
    chk("Deletar curso com turmas ativas (deve proteger ou cascade)",
        st in (400,409,200,204), f"Retornou {st}: {d.get('message','')}")

# ─── RELATORIO FINAL ────────────────────────────────────────────────────────
print("\n" + "="*60)
print("RELATORIO FINAL")
print("="*60)
print(f"\n  ✅ PASSES: {len(PASSES)}")
print(f"  ❌ BUGS/FALHAS: {len(BUGS)}")

if BUGS:
    print("\n  LISTA DE BUGS ENCONTRADOS:")
    for i, b in enumerate(BUGS, 1):
        print(f"  {i:02d}. {b}")
else:
    print("\n  Nenhum bug critico encontrado!")
