# AlfaSchool

Plataforma escolar modular. Dois blocos que compartilham os cadastros e se
contratam separadamente:

- **Pedagogico** (pre-existente): cursos, disciplinas, turmas, matriculas,
  diario de classe, frequencia, avaliacoes, notas, boletim, financeiro.
- **Access** (em construcao): controle de acesso, permanencia, retirada
  segura, paineis ao vivo, portal da familia.

## Stack

Backend Spring Boot 3.5.6 / Java 21 / MySQL 8 / Flyway / JWT / Redis.
Frontend React 19 + **JSX puro** (sem TypeScript) + Vite 7, com `fetch`
envelopado em `src/services/api.js`, `react-router-dom` e `lucide-react`.
CSS proprio, sem biblioteca de UI.
Pacote raiz: `br.com.alfaschool.backend`.

## Arquitetura

```
backend/src/main/java/br/com/alfaschool/backend/
├── domain/           entidades JPA; estendem domain/shared/BaseEntity
├── application/      services + controllers + dto (por area)
├── infrastructure/   persistence/repository, config, crypto
├── security/         SecurityConfig, filtros JWT e de tenant
└── shared/           ApiResponse, excecoes
```

**Multi-tenant**: `BaseEntity` tem `tenant_id` e um `@FilterDef`
`tenantFilter`; `security/filter/TenantFilter` popula o `TenantContext`
(ThreadLocal) a partir do JWT. Ainda assim, **filtre por tenant em toda
query** — nao confie num `findById` solto.

**Modulo contratavel**: `@PreAuthorize("isAuthenticated() and
@moduloGuard.has('ACCESS')")`. Tabelas `modulos` / `tenant_modulos`.

**Tempo real**: SSE via `application/access/shared/SseHub`, canais por
(tenant, topico). Nao ha WebSocket.

**Segredos**: senha de equipamento e credencial de provedor passam por
`infrastructure/crypto/SegredoCifrador` (AES-256-GCM, chave
`APP_SECRET_KEY`, minimo 32 caracteres). Nunca em texto plano.

## Ambiente local

```
./scripts/dev.sh up        # banco + api
./scripts/dev.sh restart   # recompila e reinicia
./scripts/dev.sh test      # testes
./scripts/dev.sh reset-db  # apaga e recria o banco

# cenario de demonstracao (Colegio Mundo do Saber)
docker exec -i alfaschool-mysql mysql -uroot -palfaschool123 alfaschool \
  < scripts/seed-mundo-do-saber.sql

# fluxo completo contra a API, pelo simulador de leitor
./scripts/smoke-fluxo-completo.sh
```

O smoke exercita entrada, chegada do responsavel, fila, preparo, entrega,
saida efetiva e replay. **Rode-o antes de dar qualquer entrega por
pronta**: ele ja pegou tres bugs de integracao que os testes unitarios
nao viam, porque cada fatia passava isolada.

Para o simulador funcionar, a API precisa subir com
`ACCESS_SIMULADOR=true` (o `dev.sh` ja faz isso em desenvolvimento).

API em http://localhost:8083 · phpMyAdmin em http://localhost:8082.
Usuario inicial: `superadmin@alfaschool.com`.

### Armadilha do Docker no WSL

`docker compose build backend` pode gerar uma imagem com **arquivos
antigos**: o Docker Desktop serve um contexto obsoleto do filesystem do
WSL, e isso persiste mesmo com `--no-cache`. O sintoma tipico e' o Flyway
falhando numa migration que ja foi corrigida no disco. Por isso
`scripts/dev.sh` compila por bind mount e roda o jar pronto. Se for
buildar a imagem, confira o conteudo antes de confiar nela.

## Convencoes

- Entidades **sem Lombok**: getters/setters explicitos.
- Enums de dominio do modulo de acesso em `domain/access/shared/`,
  mapeados com `@Enumerated(EnumType.STRING)`.
- DTOs sao `record` com factory `from(entity)`. Sem MapStruct.
- Controllers devolvem `ResponseEntity<ApiResponse<T>>`.
- Erros: `ResponseStatusException` com mensagem em portugues.
- Comentarios explicam o **porque** (regra de negocio, armadilha de
  producao), nao o obvio.
- No frontend, nada de `alert()` nem `confirm()` nativos: use `Modal.jsx`
  e mensagens na propria tela.
- O JSON da API e' **camelCase** (nao ha PropertyNamingStrategy).
- Migrations sequenciais `V<n>__<assunto>.sql`. Rode `./scripts/dev.sh
  reset-db` e confirme que aplicam do zero antes de commitar.

## Regras de negocio que nao podem ser violadas

1. **Reconhecer o responsavel nao entrega o aluno.** Chegada, entrega e
   saida efetiva sao tres eventos com tres carimbos de tempo. A
   permanencia so' encerra na saida efetiva.
2. **Autorizacao de retirada, acesso ao portal e recebimento de
   notificacao sao permissoes independentes.**
3. **Restricao judicial tem precedencia sobre qualquer autorizacao.**
4. **Autorizacao temporaria exige data de fim** — para nunca virar
   permanente por esquecimento.
5. **Verificacao de autorizacao falha fechada**: duvida ou erro = negar.
6. **Dia de permanencia inconsistente nao entra em nenhum total.**
7. **Permanencia congelada nao e' recalculada** — fatura emitida nao muda.
8. **Painel de sala mostra apenas os alunos daquela sala.** A URL nao
   autentica; cada TV tem token proprio e revogavel.
9. **Biometria de menor exige base legal e consentimento registrados**
   antes de ir para qualquer equipamento (LGPD Art. 11 e 14).
10. **Notificacao automatica nao carrega foto nem dado biometrico.**

## Armadilha de Spring que ja custou caro aqui

Listener anotado com `@TransactionalEventListener(AFTER_COMMIT)` roda
**depois** que a transacao de origem foi encerrada. Um `@Transactional`
comum chamado dali tenta aderir aquela transacao morta: o codigo executa,
o log diz que deu certo e **nada e' persistido**, em silencio.

Todo listener de AFTER_COMMIT que escreve precisa de
`@Transactional(propagation = REQUIRES_NEW)`. Foi assim que a permanencia
e a fila de retirada calculavam tudo sem gravar linha nenhuma.

Pelo mesmo motivo, metodo `@Scheduled` que toca repositorio precisa de
`@Transactional`: o `TenantRepositoryAspect` aplica o filtro de tenant do
Hibernate e exige um EntityManager transacional.
