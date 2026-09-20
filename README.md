# AlfaSchool

Plataforma de gestão escolar com **controle de acesso e retirada segura de alunos**.

Faz parte da família Alfa, junto com AlfaJornada (ponto), AlfaGym (academias) e
AlfaControl (controle de acesso).

---

## O problema que ele resolve

Na saída da escola, três coisas acontecem em momentos diferentes:

1. o responsável **chega** e é reconhecido na portaria;
2. a criança é **preparada** e entregue;
3. a criança **sai** de fato, cruzando o leitor.

A maioria dos sistemas trata isso como um evento só. O AlfaSchool trata como três,
cada um com hora própria. É o que permite à escola responder, meses depois e com
precisão, à pergunta que importa: *quem levou essa criança, quando, e com qual
autorização?*

Dessa decisão nasce o resto do sistema: a fila de retirada, os painéis nas salas,
o portal da família, a apuração de permanência e a cobrança de hora excedente.

---

## O que o sistema faz

### Controle de acesso e retirada

- **Fila de retirada ao vivo.** O responsável encosta o rosto no leitor da portaria e
  a criança aparece automaticamente na TV da sala dela.
- **Painéis de sala.** Uma Smart TV por sala, sem login, com o rosto da criança e o de
  quem veio buscar. A tela só informa: não tem botão, e o cartão sai quando a criança
  passa o rosto no leitor de saída.
- **Autorizações** por pessoa, com prazo, dias da semana e faixa de horário.
- **Restrições judiciais.** Medida protetiva prevalece sobre qualquer autorização, por
  qualquer caminho — inclusive a retirada manual da coordenação.
- **Permanência e excedente.** Quanto tempo cada aluno ficou contra a jornada
  contratada. É a base da cobrança de hora extra.
- **Portal da família.** O responsável vê se o filho está na escola, o histórico, quem
  pode buscá-lo, e pede a inclusão de alguém novo — que a escola aprova.
- **Sete relatórios**: movimentações, permanência, excedentes, retiradas, tempo de
  espera, acessos negados e ocorrências.

### Gestão escolar

Alunos, responsáveis, professores, cursos, disciplinas, turmas, matrículas, diário de
classe, frequência, avaliações, notas, boletim e financeiro (planos, contratos e
cobranças).

### Proteção de dados

O produto lida com dado de criança, então a LGPD não é um rodapé:

- **biometria de menor** exige base legal e consentimento registrados antes de o rosto
  ir para qualquer equipamento;
- **a família pode revogar** o consentimento a qualquer momento — e a revogação remove
  o rosto de cada leitor onde ele foi gravado;
- **relatório do titular** (Art. 18): a direção emite tudo o que a escola guarda sobre
  um aluno, sem o template biométrico;
- avisos automáticos **nunca carregam foto** nem dado biométrico.

---

## Como é feito

| Camada | Tecnologia |
|---|---|
| Backend | Java 21, Spring Boot 3.5, MySQL 8, Flyway, JWT, Redis |
| Frontend | React 19 (JSX puro, sem TypeScript), Vite 7, CSS próprio |
| Tempo real | SSE — sem WebSocket |
| Agente local | Python, fala o protocolo dos leitores Control iD |

**Multi-tenant:** uma instalação atende várias escolas, isoladas por `tenant_id` em
toda consulta.

**Módulo contratável:** o bloco de acesso é vendido à parte do pedagógico.

**Permissões:** 46 permissões e 7 perfis (Diretor, Coordenação, Secretaria, Professor,
Portaria, Financeiro, Responsável). A tela mostra o que o perfil permite — a portaria
não vê financeiro, o professor não vê permissões.

Tamanho de hoje: 538 classes Java, 46 telas React, 53 migrations, 68 tabelas,
410 testes automatizados.

---

## Como rodar

Precisa de Docker. Um comando sobe banco, API e interface:

```bash
./scripts/dev.sh up
```

| | |
|---|---|
| Interface | http://localhost:5173 |
| API | http://localhost:8083 |
| Banco (phpMyAdmin) | http://localhost:8082 |

Para carregar o cenário de demonstração (Colégio Mundo do Saber, com alunos,
responsáveis, turmas e equipamentos):

```bash
docker exec -i alfaschool-mysql mysql -uroot -palfaschool123 alfaschool \
  < scripts/seed-mundo-do-saber.sql
```

Depois entre com qualquer um destes, senha `100%Alfa@`:

| Usuário | Vê |
|---|---|
| `diretor@mundodosaber.com` | tudo dentro da escola |
| `coordenador@mundodosaber.com` | operação do dia: fila, presença, ocorrências |
| `secretaria@mundodosaber.com` | cadastros, matrículas e autorizações |
| `portaria@mundodosaber.com` | fila e ocorrências |
| `professor@mundodosaber.com` | diário, notas e frequência |
| `financeiro@mundodosaber.com` | planos, contratos e excedentes |
| `responsavel@mundodosaber.com` | só o portal da família |

**Dentro do sistema há um tutorial** em *Como usar o sistema* (menu do perfil). Ele é
montado a partir das permissões de quem está logado, então mostra exatamente o que
aquela pessoa pode fazer — e responde dúvida escrita em linguagem normal
("por que não consigo entregar o aluno?").

### Teste de aceitação

```bash
./scripts/smoke-fluxo-completo.sh
```

Exercita o fluxo inteiro contra a API, pelo simulador de leitor: entrada do aluno,
chegada do responsável, fila, entrega, saída efetiva e reenvio duplicado. **Rode antes
de dar qualquer coisa por pronta** — ele já pegou bugs de integração que os testes
unitários não viam, porque cada parte passava isolada.

---

## Onde está o resto

| Arquivo | Para quem |
|---|---|
| `CLAUDE.md` | quem for programar: arquitetura, convenções, armadilhas conhecidas e as regras de negócio invioláveis |
| `frontend/brand/README.md` | a marca: símbolo, wordmark e como regerar |
| `agent/alfasync/` | o agente que fala com os leitores |

---

## Estado

Funcionando e verificado contra a API: cadastros, fila de retirada, painéis,
permanência, relatórios, portal da família, permissões e os direitos de LGPD.

Em aberto: administração da plataforma (Redes de Ensino e Painel SaaS, telas que só
o superadministrador usa) e a política de retenção — por quanto tempo guardar registro
de portaria e histórico escolar é decisão da escola, não do sistema.

> **Ambiente local usa segredos de laboratório.** `APP_JWT_SECRET`, `APP_SECRET_KEY` e a
> senha do superadministrador têm valores públicos neste repositório. O sistema **recusa
> subir** com eles a menos que `APP_PERMITIR_SEGREDOS_PADRAO=true` esteja definido — o
> que o `dev.sh` faz. Em produção, defina os três.
