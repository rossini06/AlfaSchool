# Changelog

O que mudou no AlfaSchool, escrito para quem usa o sistema — não para quem
programa. Cada entrada diz o que a pessoa via antes, o que vê agora e por
quê. O registro começa em 21/09/2026; o que veio antes está no histórico
do repositório.

## AlfaSchool — 22/09/2026 — Horários no fuso certo, foto que carrega, nome no financeiro e boletim do Infantil

Uma rodada de correções deixa a exibição mais fiel ao que acontece na escola: os horários passam a seguir o fuso de Brasília, o boletim do Ensino Infantil deixa de sair vazio e algumas telas voltam a mostrar o que deviam. Nada exige ação de quem usa; as mudanças valem assim que o sistema é atualizado.

### Correções
- **Os horários voltaram a bater com o relógio de Brasília.** Em várias telas — a fila de retirada e o Painel de Coordenação, o painel de sala na TV, Presentes Agora, Permanência, o Portal da Família e ainda Usuários e Auditoria — os horários apareciam adiantados em algumas horas. Agora mostram o horário local correto.
- **O Portal da Família parou de mostrar tempo negativo.** O "tempo do dia" de um aluno presente e o percentual da jornada podiam aparecer negativos (algo como "-5h59" e "-120% cumprido"). Agora mostram o tempo real de permanência, contado desde a entrada registrada na portaria.
- **A foto da pessoa autorizada agora aparece.** Na tela de Pessoas Autorizadas a foto ficava quebrada; passou a carregar normalmente. E, em qualquer tela, quando uma foto não carrega, aparecem as iniciais da pessoa no lugar da imagem quebrada.
- **O Financeiro mostra o nome do aluno.** Nas cobranças e nos contratos aparecia um código no lugar do nome; agora aparece o nome do aluno.
- **O boletim do Ensino Infantil deixou de sair vazio.** Como o Infantil é avaliado por conceito, e não por nota numérica, o boletim não trazia nada nas disciplinas. Agora mostra o conceito de cada disciplina (Ótimo, Bom, Regular), a frequência e a situação do aluno.

## AlfaSchool — 21/09/2026 — Reforço de segurança: isolamento entre escolas, controle de permissões e trilha de auditoria

Depois de uma verificação de segurança conduzida pela equipe Alfa, uma
rodada de correções fecha problemas de isolamento entre escolas e de
controle de acesso. Nenhum exige ação de quem usa; as mudanças valem assim
que o sistema é atualizado.

### Segurança
- **Uma escola nunca mais enxerga dados de aluno de outra.** O sistema passou a conferir, em cada cadastro que aponta para um aluno, uma turma ou uma disciplina, que essa referência é da própria escola. Antes, uma matrícula ou ocorrência registrada com o identificador de um aluno de outra escola fazia o nome da criança, a turma e o curso aparecerem no boletim, no painel e nos relatórios da escola errada. Agora a operação é recusada na origem.
- **Ninguém concede a si mesmo mais poder do que tem.** Quem administra usuários não consegue mais atribuir a si próprio — nem a outra pessoa — um perfil ou uma permissão que ele mesmo não possui. E o perfil de Super Administrador da plataforma deixou de ser atribuível pela tela: ele existe apenas para a administração da Alfa.
- **As ações administrativas passam a deixar rastro.** Criar, editar, desativar e excluir usuário, redefinir a senha de outra pessoa, atribuir um perfil, mudar o que um perfil pode fazer, conceder permissões extras e, no lado da Alfa, criar ou suspender uma rede: tudo isso agora aparece na trilha de auditoria, com quem fez e de onde. Antes, várias dessas ações não ficavam registradas.
- **Revogar todas as permissões de um perfil agora tranca de verdade.** Esvaziar as permissões de um perfil padrão pela tela deixava, por um efeito colateral, o perfil voltar ao conjunto original. Agora o que a escola define é o que vale, inclusive "nada".
- **O documento de uma restrição judicial fica reservado à gestão.** A coordenação continua vendo que existe uma restrição sobre um aluno, mas ler o documento em si (o mandado, a decisão) passou a exigir perfil de gestão.
- **Métricas internas do sistema deixaram de ficar visíveis a qualquer pessoa conectada.** Passaram a ser restritas à administração da plataforma. A verificação de "o sistema está no ar" continua pública.

### Correções
- **O portal da família não dá mais erro ao abrir o dia de um aluno sem movimento.** Ver o "hoje" de um filho que ainda não passou pela portaria naquele dia devolvia um erro; agora abre normalmente.
- **A renovação de sessão voltou a funcionar** e passou a recusar quem foi desativado, excluído ou bloqueado, em vez de prolongar o acesso.
- **A entrada da administração da Alfa numa escola agora fica registrada** na trilha daquela escola, o que antes falhava silenciosamente.

## AlfaSchool — 21/09/2026 — Painel SaaS próprio, redes que nascem prontas e o fim dos 403 do Controle de Acesso

### Novidades
- **A administração da Alfa ganhou um painel próprio, fora do painel da escola.** "Redes de Ensino" e "Painel SaaS" apareciam na barra lateral da escola, num grupo "Administração Alfa" ao lado de "Alunos", como se a plataforma fosse uma tela da escola. Agora o superadministrador entra direto em `/saas`, um painel com layout próprio: cabeçalho com a marca, o badge "PAINEL SAAS" em âmbar para ninguém confundir com o painel da escola, e três seções — Visão geral, Redes de ensino e Planos. De lá, "Painel da escola" abre o painel administrativo; de volta, "Painel SaaS" fica no menu do perfil. É o mesmo desenho do AlfaControl e do AlfaJornada.
- **Criar uma rede de ensino deixa ela pronta para usar.** A tela antiga de Redes mostrava campos que não existiam no sistema e chamava operações que o servidor não conhecia: parecia cadastro, mas nada era gravado. Agora, ao criar uma rede, o sistema já cria os perfis padrão (Diretor, Coordenação, Secretaria, Professor, Portaria, Financeiro, Responsável), o administrador da rede — que entra com o e-mail informado e troca a senha no primeiro acesso — e os módulos contratados, tudo de uma vez. Uma rede sem usuário e sem módulo não servia para ninguém.
- **Dá para contratar e descontratar módulos de cada rede pela tela.** Não havia lugar nenhum para isso; só um script de banco fazia. No card da rede, "Módulos" abre o catálogo com o que está contratado; o que for desmarcado some do menu da escola no próximo login.
- **Suspender uma rede bloqueia o acesso de todo mundo dela.** Antes, "inativa" era só um rótulo: os usuários continuavam entrando. Agora login e renovação de sessão são recusados com a mensagem "Esta rede de ensino está suspensa. Fale com a Alfa para reativar." Os dados ficam guardados, e reativar devolve o acesso na hora.
- **O superadministrador escolhe em qual escola vai entrar.** "Painel da escola" abria o painel do tenant da Alfa, que não é escola nenhuma. Agora o botão vira "Entrar numa escola", com a lista das redes e busca; cada card de rede também tem "Entrar". Dentro da escola, o cabeçalho mostra "Super Admin · nome da rede" e o menu do perfil oferece "Voltar ao painel SaaS". A sessão passa a valer para aquela rede, com todas as permissões, e a entrada fica registrada na auditoria da rede. Mesmo mecanismo do "selecionar cliente" do AlfaControl.
- **Cadastro de escolas (unidades) passa a ser da própria rede.** A tela "Escolas" estava presa ao superadministrador, mas ela lista as unidades da rede em que a pessoa está logada. Agora aparece em "Sistema" para quem tem a permissão de ver escolas.

### Melhorias
- **O painel SaaS segue o mesmo desenho do AlfaControl.** A família fica com uma cara só: cabeçalho com a marca, o badge "PAINEL SAAS" e "Entrar numa escola"; abaixo, o título "Painel SaaS" e as abas sublinhadas Dashboard, Redes de ensino e Planos. O Dashboard mostra os números em grupos (Redes de ensino, Módulos contratados, Planos), cada um com o ícone à esquerda e o número em destaque. Redes e Planos ficam em tabela, com busca, filtro de situação e as ações na própria linha.
- **A régua de módulos.** Cada rede e cada plano mostram cinco traços, um por módulo, preenchidos conforme o contrato, ao lado de "3 de 5". Dá para ver de relance quem tem o quê sem abrir nada.
- **Monograma da rede.** Como não há logotipo, cada rede recebe suas iniciais numa moeda colorida, sempre da mesma cor para o mesmo nome. O tenant da Alfa leva o alfa.
- **Planos com os campos que existem de verdade.** A tela antiga pedia "preço" e "limite de alunos", e o servidor esperava preço mensal e anual, limites de escolas, usuários e leitores e um identificador. Nada salvava. Agora o formulário segue o contrato real: o identificador é gerado do nome, os módulos incluídos aparecem na régua do card e o preço é o destaque.
- **Perfis e Permissões ficou legível.** O título era maior que o de todas as outras telas; os cards diziam "23 permissões" sem referência; o Super Admin aparecia com o nome técnico e "0 permissões" ao lado de "acesso total". Agora cada perfil tem ícone, medidor "23 de 46" e o botão no rodapé; o Super Admin diz "Acesso total". No modal, as áreas ficam em duas colunas com contador "2/2", borda destacada quando a área está toda marcada, e o rodapé com "45 de 46 marcadas" ao lado de Cancelar e Salvar.
- **A marca da família Alfa está na barra lateral.** Era um "A" num quadrado e o nome em texto; agora é o mesmo símbolo e wordmark do login, na proporção da marca. Recolhida, a barra mostra só o símbolo.
- **Cantos arredondados iguais em todo o sistema.** Havia quinze valores soltos no CSS e treze inline nas telas, cada canto diferente do vizinho. Passou a existir uma escala só (4, 6, 10, 14, 20 e pílula), e item de menu, chips e campos seguem a mesma régua.
- **A fonte do sistema não "pisca" mais ao abrir.** No primeiro carregamento o texto do login aparecia numa fonte e trocava para a Inter um instante depois. Agora a Inter é pré-carregada junto com a página. Continua servida pelo próprio AlfaSchool, sem Google Fonts, e só com os dois arquivos que o português usa.
- **Ícone do calendário visível no tema escuro.** O campo de data mostrava o ícone nativo preto sobre fundo escuro; sumia. Controles nativos (data, seleção, barra de rolagem) agora seguem o tema.
- **Tela de "módulo não contratado".** Quem chegar por link a uma tela do Controle de Acesso numa escola que não contratou o módulo vê uma explicação e o que fazer, em vez de uma lista vazia com erros no console.

### Correções
- **As telas do Controle de Acesso respondiam 403 para o superadministrador.** Coordenação, Ocorrências, Jornadas, Salas, Painéis e as demais falhavam em cada requisição. O superadministrador vive no tenant da Alfa, que nunca teve o módulo contratado, e o menu não sabia de módulos — só de permissões. Agora o tenant da Alfa tem todos os módulos, o login informa quais estão contratados, e o menu esconde o que a escola não tem.
- **A lista de redes só mostrava a própria Alfa.** O filtro por escola, correto em todo o resto do sistema, também estava sendo aplicado à lista de redes: só aparecia o tenant da Alfa, e "total de redes" dava 1. As telas da administração passam a listar todas.
- **A tela de Avaliações quebrava ao abrir.** Um estado de mensagem estava declarado num modal e lido pela página; a rota `/avaliacoes` morria na primeira renderização com "feedback is not defined". O modal de exclusão tinha o mesmo defeito ao tratar erro.

### Para quem opera o ambiente
- **O projeto roda no Mac.** `docker-compose.mac.yml` remapeia as portas que já eram do MySQL e do Redis do Homebrew; `COMPOSE_FILE=docker-compose.yml:docker-compose.mac.yml BACKEND_PORT=8093 ./scripts/dev.sh boot` sobe tudo.
- **Após aplicar o seed da escola de demonstração pela primeira vez, reinicie a API uma vez e aplique o seed de novo.** Os perfis de uma rede só são criados no boot; sem isso o diretor da demonstração entra sem permissão nenhuma.
- **Decisões de design ficaram registradas** em `.interface-design/system.md`: profundidade por borda, escala de raio, cor com significado e os padrões prontos (card de entidade, régua de módulos, monograma, medidor).
