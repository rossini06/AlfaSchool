# Homologação em campo — AlfaSchool Access

Roteiro para validar o módulo de controle de acesso dentro da escola, com
os equipamentos reais, **sem interromper a operação atual**.

Público: quem faz a implantação. Leve impresso ou no celular.

---

## Antes de sair (escritório)

- [ ] Sistema sobe do zero: `./scripts/dev.sh reset-db` termina com a API no ar
- [ ] Cenário de demonstração carregado: `scripts/seed-mundo-do-saber.sql`
- [ ] Fluxo completo validado no simulador (entrada → chegada do responsável →
      fila → preparo → entrega → saída → permanência fechada)
- [ ] Notebook com o backend rodando **na mesma rede** dos leitores
- [ ] Cabo de rede e adaptador USB-Ethernet (não confie no Wi-Fi da escola)
- [ ] Uma Smart TV ou tablet para testar o painel de sala
- [ ] Token de painel gerado e anotado
- [ ] Autorização por escrito da escola para acessar os equipamentos

---

## Etapa 0 — Levantamento (não mexe em nada)

O objetivo aqui é **só documentar**. Nenhuma configuração é alterada.

Para cada um dos 8 leitores, anote:

| Campo | Onde achar |
|---|---|
| Modelo | etiqueta física / interface web do equipamento |
| Firmware | interface web → Sistema/Sobre |
| Número de série | etiqueta / interface web |
| IP e porta | interface web → Rede |
| Usuário e senha de admin | com o responsável de TI da escola |
| Catraca a que pertence | observação física |
| Portaria | observação física |
| Sentido | entrada, saída ou ambos |
| Função | aluno ou exclusivo de responsável |

Anote também:

- [ ] Quantos alunos cadastrados hoje no sistema atual
- [ ] Faixa de IP da rede, gateway e se há VLAN separada
- [ ] Há internet estável? Qual o link?
- [ ] Onde ficaria o gateway local (PC da secretaria? mini-PC?)
- [ ] Modelo das Smart TVs e se o navegador em modo loja reabre sozinho
- [ ] Horários de pico de entrada e de saída
- [ ] Quem opera hoje: portaria, coordenação, professoras

**Pergunta comercial que precisa de resposta:** os R$ 3.000 do sistema atual
são mensais, anuais ou outro período? Sem isso não dá para calcular
viabilidade.

**Perguntas de regra de negócio que mudam o código:**

1. O excedente é cobrado pela **duração total** do dia ou por **sair depois
   do horário**? São contas diferentes. Um aluno contratado 07h–17h que
   entra 08h e sai 18h permaneceu 10h (sem excedente por duração) mas saiu
   1h depois (1h de excedente por horário).
2. Existe tolerância contratual? De quantos minutos?
3. O que acontece quando alguém não autorizado tenta retirar? Quem decide?
4. A escola tem catraca de **saída** de aluno, ou a saída é registrada
   manualmente pela coordenação?
5. Há caso de restrição judicial / guarda em vigor hoje?

---

## Etapa 1 — Um leitor em laboratório

Ainda **sem tocar na instalação de produção**. Se possível, use um leitor
reserva; se não houver, faça fora do horário de aula.

1. [ ] Ligue o leitor numa rede isolada (switch próprio + notebook)
2. [ ] Cadastre o equipamento no AlfaSchool com IP, porta, usuário e senha
3. [ ] "Testar conexão" responde OK
4. [ ] Cadastre uma pessoa de teste (você mesmo) com foto
5. [ ] Sincronize e confirme que o **veredito da foto** voltou ACEITA
       — se voltar RECUSADA, a mensagem diz o motivo (olhos fechados,
       rosto pequeno, baixa qualidade)
6. [ ] Aproxime o rosto do leitor e confirme que o evento chega ao sistema
7. [ ] Aproxime **duas vezes seguidas** e confirme que gera **um** registro
       (deduplicação funcionando)
8. [ ] Desligue a rede, aproxime o rosto, religue: o evento precisa chegar
       depois (fila offline)
9. [ ] Confira o horário do evento: o firmware reporta hora local, não UTC.
       Se aparecer 3 horas deslocado, pare e ajuste antes de seguir.

**Não avance enquanto o item 9 não estiver certo.** Horário errado
corrompe toda a apuração de permanência.

---

## Etapa 2 — Gateway local

1. [ ] Instale o gateway na máquina escolhida
2. [ ] Gere a credencial no AlfaSchool (a senha aparece **uma única vez**)
3. [ ] Configure e confirme o heartbeat chegando
4. [ ] Desligue o gateway e confirme que o equipamento vira **offline**
       em até 3 minutos (não pode ficar "online" para sempre)
5. [ ] Religue e confirme que volta a online
6. [ ] Reinicie a máquina e confirme que o gateway sobe sozinho

---

## Etapa 3 — Operação paralela (sem desligar o sistema atual)

Esta é a etapa mais importante e a mais demorada. O sistema atual
**continua operando normalmente**. O AlfaSchool apenas observa.

1. [ ] Cadastre a estrutura real: unidade, 2 portarias, salas, turmas
2. [ ] Importe os alunos e responsáveis
3. [ ] Cadastre as jornadas contratadas reais
4. [ ] Cadastre as autorizações de retirada reais
5. [ ] Rode **uma semana** em paralelo
6. [ ] Todo dia, compare com o sistema atual:
       - [ ] quantidade de entradas bate?
       - [ ] quantidade de saídas bate?
       - [ ] as horas de permanência batem?
       - [ ] algum aluno ficou com dia INCONSISTENTE? por quê?

Registre cada divergência com data, aluno e horário. Divergência não
explicada é bloqueio para a próxima etapa.

---

## Etapa 4 — Painel de sala numa TV real

1. [ ] Abra o painel na TV com o token
2. [ ] Confirme que **só aparecem alunos daquela sala**
3. [ ] Teste a legibilidade **da porta da sala**, em pé, como a professora vê
4. [ ] Desconecte a rede: o indicador precisa mudar para "Sem conexão"
5. [ ] Reconecte: a tela precisa voltar sozinha e **recarregar o estado**
6. [ ] Deixe a TV ligada **o dia inteiro** e volte no fim da tarde:
       - [ ] o navegador ainda está aberto?
       - [ ] a conexão ainda está viva?
       - [ ] a tela ainda está correta?
7. [ ] Revogue o token e confirme que a tela perde o acesso na hora

O item 6 é o que mais reprova em campo. Smart TV em modo loja costuma
suspender ou reiniciar o navegador.

---

## Etapa 5 — Fluxo de retirada com gente de verdade

Faça com um responsável voluntário, avisado, fora do horário de pico.

1. [ ] Responsável aproxima o rosto no leitor exclusivo
2. [ ] A retirada aparece na coordenação **e** no painel da sala
3. [ ] Professora toca em "Preparar aluno para saída"
4. [ ] Coordenação confirma a entrega, **identificada pelo login**
5. [ ] Saída é registrada
6. [ ] Confira: a permanência fechou no horário da **saída**, não no da
       chegada do responsável
7. [ ] Confira o tempo de espera registrado

Teste também o caminho ruim:

8. [ ] Pessoa **não autorizada** aproxima o rosto → nenhuma retirada é
       aberta e uma ocorrência é gerada
9. [ ] Pessoa autorizada **fora do dia/horário** permitido → negada com
       motivo claro
10. [ ] Retirada manual pela coordenação exige motivo e fica registrada

---

## Etapa 6 — Treinamento e virada

- [ ] Treinar portaria, coordenação e professoras (separadamente)
- [ ] Deixar um guia de uma página por função
- [ ] Definir quem aciona o suporte e por qual canal
- [ ] **Plano de reversão escrito**: como voltar ao sistema atual em
      minutos se algo der errado no primeiro dia
- [ ] Virar num dia de movimento baixo, nunca numa segunda-feira
- [ ] Ficar presencialmente no primeiro dia inteiro

---

## Critérios para dizer "está homologado"

Não declare pronto sem todos estes:

1. Uma semana em paralelo sem divergência inexplicada de horas
2. Painel de TV sobreviveu a um dia inteiro ligado
3. Fluxo de retirada validado, incluindo os caminhos de negação
4. Queda de rede testada e recuperada sozinha
5. Equipamento offline detectado em até 3 minutos
6. Relatório de permanência conferido com registro real
7. Plano de reversão escrito e testado
8. Operação treinada

---

## O que NÃO fazer

- Não substituir o sistema atual antes de uma semana em paralelo
- Não alterar a configuração dos equipamentos de produção na Etapa 0 ou 1
- Não exibir foto de aluno em tela sem dispositivo autorizado
- Não enviar foto ou dado biométrico por WhatsApp ou e-mail
- Não cadastrar biometria de criança sem consentimento registrado dos pais
- Não virar sem plano de reversão
