# AlfaSync School — gateway local

Agente que roda **dentro da escola**, numa maquina qualquer da mesma rede dos
leitores faciais Control iD.

O backend do AlfaSchool fica na nuvem e **nao alcanca a LAN da escola**. O
gateway alcanca os dois lados: conversa com os leitores por HTTP na rede local e
com o backend por polling. A divisao e simples e nao tem excecao:

> **o backend decide, o gateway executa.**

O gateway coleta a passagem e roda o comando que mandaram. Ele nunca decide
autorizacao por conta propria. (Regra offline — liberar acesso com a internet
caida — **nao esta implementada**.)

## O que ele faz, em cinco rotinas

| Rotina | Intervalo padrao | O que faz |
|---|---|---|
| heartbeat | 5s | diz ao backend que o gateway e cada leitor estao vivos |
| eventos | 5s | le `access_logs` dos leitores, converte e envia |
| fila | 15s | reenvia o que ficou pendente (internet caida) |
| pessoas | 30s | delta de pessoas e faces do backend para os leitores |
| tarefas | 10s | executa `sincronizar_pessoa`, `abrir_acesso`, `remover_pessoa` |

Nenhuma passagem se perde quando a internet cai: o evento entra numa fila SQLite
em disco **antes** de o envio ser tentado, e sai de la quando o backend confirma.

## Requisitos

- Python 3.11 ou mais novo
- `requests` (unica dependencia: `pip install -r requirements.txt`)
- A maquina precisa enxergar os leitores na LAN (porta 80) e a internet.
- Sem interface grafica: e um processo de terminal/servico.

## Instalacao e configuracao

1. Copie a pasta `alfasync/` para a maquina da escola.
2. `pip install -r requirements.txt`
3. Copie `config.example.json` para `config.json` **na mesma pasta** e preencha:

```jsonc
{
  "backend": {
    "base_url": "https://…",     // endereco do AlfaSchool
    "client_id": "…",            // identificador da unidade
    "username": "…",             // usuario de integracao
    "password": "…"              // senha de integracao
  },
  "devices": [
    { "id": "1", "nome": "PORTARIA", "host": "192.168.1.100",
      "username": "admin", "password": "senha-do-leitor" }
  ]
}
```

O `config.example.json` **nao contem credencial nenhuma** — e so o esqueleto, e
os campos de senha vem com `PREENCHER`. O `config.json` preenchido nao vai para
o repositorio.

A lista autoritativa de equipamentos vem de `GET /agent/devices`. O que esta em
`devices` serve para (a) guardar a senha de administracao do leitor, que o
backend nao precisa conhecer, e (b) manter a escola funcionando na primeira
subida ou com o backend fora do ar.

Campos opcionais (`intervalos`, `fila`, `timeouts`, `log`, `dados_dir`) tem
padrao razoavel; so mexa se precisar.

### Rodar no Linux

```bash
cd /opt/alfasync
python3 agent.py            # usa ./config.json
python3 agent.py /etc/alfasync/config.json   # ou um caminho explicito
```

Como servico (systemd), em `/etc/systemd/system/alfasync.service`:

```ini
[Unit]
Description=AlfaSync School — gateway local
After=network-online.target

[Service]
WorkingDirectory=/opt/alfasync
ExecStart=/usr/bin/python3 /opt/alfasync/agent.py
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

`systemctl enable --now alfasync` e pronto. `Ctrl+C` (SIGINT) e `systemctl stop`
(SIGTERM) encerram limpo: as threads param, o `state.json` e gravado e o SQLite
e fechado.

### Rodar no Windows

```bat
cd C:\AlfaSync
py -3.11 agent.py
```

Para subir junto com a maquina, o caminho mais simples e o Agendador de Tarefas:
tarefa "Ao iniciar o computador" → "Executar esteja o usuario conectado ou nao"
→ programa `py`, argumentos `-3.11 C:\AlfaSync\agent.py`, iniciar em
`C:\AlfaSync`.

Se a maquina fica em rede com IP dinamico, **fixe o IP dos leitores** no
roteador: o `host` do `config.json` aponta para eles.

## Onde ficam os arquivos

Tudo ao lado do `agent.py` (ou do executavel), a menos que `dados_dir` diga outra coisa:

| Arquivo | O que e |
|---|---|
| `config.json` | credenciais e intervalos (nunca versionado) |
| `alfasync.log` | log rotativo, 5 MB x 5 arquivos — tambem sai na tela |
| `event_queue.db` | fila SQLite de eventos ainda nao confirmados |
| `state.json` | cursor de eventos por leitor + hash das fotos ja enviadas |

Para ver a fila:

```bash
sqlite3 event_queue.db "select device_id, ext_id, attempts, ultimo_erro from pending_events;"
```

Eventos ficam na fila no maximo 72h (`fila.retencao_horas`).

## Quando o leitor nao responde

O log avisa **uma vez por transicao**, nao a cada ciclo:

```
PORTARIA (192.168.1.100) parou de responder na rede local: … Confira energia, cabo e o IP do leitor.
```

Na ordem:

1. `ping 192.168.1.100` da maquina do gateway — se nao responde, e rede/energia,
   nao e o AlfaSync.
2. Abra `http://192.168.1.100` no navegador da mesma maquina. Se a tela do leitor
   aparece, o equipamento esta vivo e o problema e credencial.
3. Confira `username`/`password` do leitor no `config.json`. Senha errada aparece
   como `login recusado pelo leitor (HTTP 401)`.
4. Leitor mudo **nao trava o gateway**: os outros equipamentos continuam sendo
   lidos, e o que ficou para tras e coletado quando ele voltar — o cursor de
   eventos esta em `state.json`, nao na memoria.

Outros sintomas comuns:

| No log | Significa |
|---|---|
| `Backend inacessivel: … — eventos vao para a fila local` | internet/servidor fora. Nada se perde; a fila drena sozinha. |
| `Backend respondeu 404 em /api/v1/access/agent/… — rota ausente` | backend mais velho que o gateway. Ele segue rodando e tenta de novo. |
| `backend recusou o acesso: HTTP 422 …` | recusa definitiva: o evento sai da fila para nao travar os seguintes. O motivo vem na mesma linha. |
| `relogio do leitor adiantado 372s` | acerte a hora do equipamento: horario errado corrompe a apuracao de permanencia. |
| `historico do leitor foi limpo. Reimportando do inicio` | leitor formatado/limpo. O gateway volta o cursor sozinho. |

## Testes

```bash
docker run --rm -v "$PWD/agent/alfasync":/app -w /app python:3.11-slim \
  sh -c "pip install -q requests pytest && python -m pytest -q"
```

Ou, com as dependencias instaladas, `python -m pytest -q` de dentro de
`agent/alfasync/`.

## Contrato com o backend

Rotas sob `/api/v1/access/agent`, com resposta embrulhada em
`{"timestamp", "status", "message", "data"}`:

| Rota | Uso |
|---|---|
| `POST /login` | `{clientId, username, password}` → token |
| `GET /devices` | equipamentos desta unidade |
| `GET /users?updatedAfter=` | delta paginado de pessoas + faces |
| `POST /events` | envia evento de passagem |
| `POST /heartbeat` | status e versao |
| `GET /tasks/pending` | fila de comandos |
| `PUT /tasks/{id}/result` | devolve resultado |
