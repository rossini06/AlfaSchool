/**
 * Conteúdo do tutorial, por tela.
 *
 * A CHAVE é o `path` do menu. É de propósito: a página de Ajuda percorre
 * `menuItems` e busca o texto por aqui, então uma tela nova sem ajuda
 * aparece na lista com um aviso visível em vez de sumir — e uma ajuda para
 * uma tela que não existe mais fica órfã e detectável.
 *
 * O que NÃO fica aqui: quem pode ver cada tela. Isso já está em
 * `menuConfig.js` (campo `perm`) e é lido das permissões reais de quem está
 * logado. Repetir aqui criaria duas versões da mesma verdade, e uma delas
 * envelheceria.
 *
 * `regras` são as que mordem: o que o sistema recusa, o que não tem desfazer
 * e o que parece bug e não é.
 */

export const ajudaPorTela = {
  "/": {
    oQueE: "A primeira tela depois do login. Mostra os números da escola e os alertas do dia.",
    comoUsar: [
      "Os cartões mudam conforme o seu perfil: quem cuida do financeiro vê inadimplência, quem está na portaria vê o movimento do dia.",
      "Os alertas apontam o que precisa de atenção hoje — pagamento em atraso, nenhuma presença registrada.",
    ],
    regras: [
      "Você não vê os mesmos cartões que um colega de outro perfil. Não é erro: cada bloco depende da sua permissão.",
    ],
  },

  "/access/coordenacao": {
    oQueE: "A mesa da coordenação na hora da saída: a fila de retirada da escola inteira, ao vivo.",
    comoUsar: [
      "A fila enche sozinha quando um responsável é reconhecido no leitor da portaria.",
      "Você acompanha cada criança do momento em que o responsável chega até ela sair.",
      "Dá para abrir uma retirada manual quando o leitor falha ou quem veio buscar não está cadastrado.",
    ],
    regras: [
      "Reconhecer o responsável NÃO entrega a criança. São três momentos com horas próprias: o responsável chegou, a criança foi preparada, a criança saiu.",
      "Retirada manual sempre gera ocorrência — é o caminho que contorna o controle e por isso deixa rastro, mesmo quando é legítimo.",
      "Se houver restrição judicial, o sistema recusa. Não há como liberar por nenhum caminho.",
    ],
  },

  "/access/presentes-agora": {
    oQueE: "Quem está dentro da escola neste momento, com turma, sala e a hora da entrada.",
    comoUsar: [
      "A tela se atualiza sozinha; não precisa recarregar.",
      "Filtre por turma ou sala para conferir uma turma inteira de uma vez.",
      "Serve para evacuação, para chamada e para responder 'o fulano está aí?' por telefone.",
    ],
    regras: [
      "A criança sai desta lista quando passa o rosto no leitor de saída — não quando é entregue.",
    ],
  },

  "/access/ocorrencias": {
    oQueE: "O registro do que saiu do esperado: tentativa não autorizada, pessoa não reconhecida, aluno não retirado no horário, equipamento fora do ar.",
    comoUsar: [
      "Parte das ocorrências é criada pelo próprio sistema quando algo é barrado.",
      "Você também pode registrar uma à mão, informando quando o fato aconteceu — que não é a hora em que você está digitando.",
      "Ao tratar, descreva o que foi feito: é o que a escola mostra se for questionada depois.",
    ],
    regras: [
      "Ocorrência de restrição judicial nasce CRÍTICA e não é gerada por engano: alguém com medida protetiva apareceu.",
    ],
  },

  "/access/permanencia": {
    oQueE: "Quanto tempo cada aluno ficou na escola, dia a dia, contra a jornada que a família contratou.",
    comoUsar: [
      "Escolha o aluno e o período para ver o extrato.",
      "O excedente é a base da cobrança de hora extra.",
      "Dia com marcação faltando pode ser corrigido informando entrada e saída, sempre com motivo.",
    ],
    regras: [
      "Dia inconsistente (faltou registrar uma saída) NÃO entra em nenhum total — o número seria fantasia.",
      "Mês já fechado não é recalculado: fatura emitida não muda sozinha.",
      "O ajuste manual fica na auditoria com o seu nome e o seu motivo. Meses depois alguém vai perguntar por que aquele dia tem 40 minutos a mais.",
    ],
  },

  "/access/relatorios": {
    oQueE: "Sete relatórios do controle de acesso: movimentações, permanência, excedentes, retiradas, tempo de espera, acessos negados e ocorrências.",
    comoUsar: [
      "Escolha a aba, o período e os filtros; depois exporte em CSV se precisar levar para a planilha.",
      "O CSV sai exatamente com as colunas que estão na tela.",
    ],
    regras: [
      "O período é limitado a 6 meses por consulta — um ano de portaria são centenas de milhares de linhas.",
      "O relatório de excedentes exclui dia inconsistente, porque ele vira cobrança.",
    ],
  },

  "/access/autorizacoes": {
    oQueE: "Quem pode retirar cada aluno, com prazo, dias da semana e faixa de horário.",
    comoUsar: [
      "Uma autorização liga um ALUNO a uma PESSOA já cadastrada em Pessoas Autorizadas.",
      "Permanente vale até alguém revogar. Temporária exige data de fim.",
      "Pedido que chega pelo portal da família nasce pendente e espera aprovação da escola.",
    ],
    regras: [
      "Autorização temporária EXIGE data de fim — para nunca virar permanente por esquecimento.",
      "Aprovar é o que libera de verdade na portaria; cadastrar não basta.",
      "Restrição judicial vence qualquer autorização, mesmo aprovada e dentro do prazo.",
    ],
  },

  "/access/pessoas-autorizadas": {
    oQueE: "O cadastro das pessoas que podem se apresentar na portaria: avó, tia, vizinha, motorista.",
    comoUsar: [
      "Cadastre a pessoa uma vez; depois ligue a ela quantos alunos forem necessários, em Autorizações.",
      "A foto ajuda a portaria a comparar com quem está na frente dela.",
      "O parentesco é como a portaria reconhece: 'a tia do Pedro'.",
    ],
    regras: [
      "Retirar aluno, acessar o portal e receber avisos são TRÊS permissões independentes. Quem recebe aviso não necessariamente pode buscar a criança.",
      "CPF não se repete: duas linhas para a mesma pessoa fariam uma restrição acertar uma e deixar a outra liberada.",
    ],
  },

  "/access/restricoes": {
    oQueE: "Medidas judiciais que impedem uma pessoa de retirar um aluno.",
    comoUsar: [
      "Informe o aluno, quem está impedido, o número do processo e o órgão que expediu.",
      "Funciona mesmo contra quem ainda não está cadastrado: o bloqueio casa pelo CPF.",
      "Encerrar a restrição não apaga o registro — ele continua no histórico.",
    ],
    regras: [
      "É a regra mais forte do sistema. Prevalece sobre qualquer autorização, inclusive na retirada manual.",
      "Tentativa de retirada de aluno com restrição vigente gera ocorrência CRÍTICA na hora.",
      "O documento anexado só é aberto por quem tem permissão — não fica exposto na listagem.",
    ],
  },

  "/alunos": {
    oQueE: "O cadastro das crianças: dados pessoais, endereço, observações médicas e os responsáveis vinculados.",
    comoUsar: [
      "O vínculo com o responsável define quem é financeiro, quem é acadêmico e quem pode buscar.",
      "Observações médicas ficam aqui — alergia, medicação, restrição alimentar.",
    ],
    regras: [
      "Desvincular um responsável leva junto a autorização de retirada, o acesso ao portal e os avisos daquela pessoa.",
    ],
  },

  "/responsaveis": {
    oQueE: "Pais, mães e quem responde legalmente pelo aluno.",
    comoUsar: [
      "Um responsável pode estar ligado a vários alunos — irmãos na mesma escola.",
      "O e-mail cadastrado é o login dele no portal da família.",
    ],
    regras: [],
  },

  "/professores": {
    oQueE: "O cadastro dos professores e as turmas e disciplinas de cada um.",
    comoUsar: ["O vínculo professor-turma-disciplina é o que libera o diário de classe para ele."],
    regras: [],
  },

  "/turmas": {
    oQueE: "As turmas do ano letivo, ligadas a um curso.",
    comoUsar: ["A turma define quem aparece junto na frequência, nas notas e nos painéis de sala."],
    regras: [],
  },

  "/matriculas": {
    oQueE: "A ligação entre o aluno e a turma, com número, data e situação.",
    comoUsar: [
      "Uma matrícula pode ser trancada, cancelada, reativada ou concluída.",
      "É a matrícula ativa que coloca o aluno nas listas de frequência e de notas.",
    ],
    regras: [
      "Cancelar tira o aluno da turma e das listas. Dá para reativar depois, mas o período cancelado fica registrado.",
    ],
  },

  "/cursos": {
    oQueE: "Os cursos oferecidos, com carga horária e critérios de aprovação.",
    comoUsar: ["A nota e a frequência mínimas definidas aqui valem para o boletim das turmas do curso."],
    regras: [],
  },

  "/disciplinas": {
    oQueE: "As disciplinas e a matriz curricular de cada curso.",
    comoUsar: ["A matriz define quais disciplinas cada turma tem, e em qual período."],
    regras: [],
  },

  "/frequencia": {
    oQueE: "A chamada: presença por turma, disciplina e data.",
    comoUsar: ["Escolha turma, disciplina e data; a lista vem com os alunos matriculados."],
    regras: ["A frequência é diferente da permanência: uma é a chamada da aula, a outra é o tempo na escola medido pelos leitores."],
  },

  "/conteudo-ministrado": {
    oQueE: "O registro do que foi dado em cada aula.",
    comoUsar: ["Serve de comprovação do conteúdo trabalhado e ajuda quem assume a turma depois."],
    regras: [],
  },

  "/avaliacoes": {
    oQueE: "As avaliações de cada turma e disciplina, com peso e data.",
    comoUsar: ["Crie a avaliação antes de lançar as notas — é ela que dá a coluna no diário."],
    regras: [],
  },

  "/notas": {
    oQueE: "O lançamento das notas dos alunos por avaliação.",
    comoUsar: ["Escolha a avaliação e lance as notas da turma de uma vez."],
    regras: [
      "Toda alteração de nota fica registrada: quem mudou, de quanto para quanto e quando. É o que a escola apresenta se um responsável contestar.",
    ],
  },

  "/boletim": {
    oQueE: "O boletim do aluno, com médias, frequência e situação final.",
    comoUsar: ["Gere por matrícula; o cálculo usa os critérios do curso."],
    regras: [],
  },

  "/financeiro": {
    oQueE: "Planos, contratos e cobranças.",
    comoUsar: [
      "O plano é o que se oferece; o contrato liga um aluno a um plano; a cobrança é a parcela.",
      "As horas excedentes apuradas na permanência são a base da cobrança extra.",
    ],
    regras: ["Encerrar um contrato não tem desfazer pela tela — seria preciso lançar um contrato novo."],
  },

  "/access/jornadas": {
    oQueE: "As jornadas contratadas: integral, meio período manhã, meio período tarde.",
    comoUsar: [
      "Cada jornada define, por dia da semana, o horário previsto de entrada e saída e a carga em minutos.",
      "A regra de excedente diz como a hora extra é contada: por duração, por horário, ou o maior dos dois.",
    ],
    regras: [
      "Por DURAÇÃO e por HORÁRIO dão resultados diferentes. Quem cobra por hora de creche usa duração; quem cobra porque a funcionária teve de ficar usa horário.",
      "Jornada sem horário previsto faz a apuração de excedente perder o parâmetro.",
    ],
  },

  "/access/aluno-jornadas": {
    oQueE: "Qual jornada cada aluno cumpre, e desde quando.",
    comoUsar: [
      "Dá para vincular aluno por aluno ou aplicar uma jornada a uma turma inteira de uma vez.",
      "Filtre por jornada para responder 'quem está em meio período?'.",
    ],
    regras: ["É esse vínculo que diz quantos minutos são esperados por dia — sem ele não há excedente a apurar."],
  },

  "/access/calendario": {
    oQueE: "O calendário do ano letivo: feriados, recessos, dias letivos e eventos.",
    comoUsar: [
      "A grade mostra o mês inteiro; clique num dia para marcá-lo.",
      "Dá para marcar um intervalo de uma vez — as férias, por exemplo.",
    ],
    regras: [
      "Dia não letivo não entra na apuração de permanência. Marcar o calendário errado desalinha a cobrança do mês.",
      "Dia sem marcação segue o padrão da semana; a grade só destaca o que foge dele.",
    ],
  },

  "/access/portarias": {
    oQueE: "Os pontos de entrada e saída da escola.",
    comoUsar: ["Cada leitor é instalado numa portaria; é assim que os relatórios dizem por onde a pessoa passou."],
    regras: [],
  },

  "/access/zonas": {
    oQueE: "Áreas da escola, para agrupar salas e portarias.",
    comoUsar: ["Serve para organizar a estrutura quando a escola tem blocos ou andares."],
    regras: [],
  },

  "/access/salas": {
    oQueE: "As salas físicas, com bloco, andar e capacidade.",
    comoUsar: ["A sala é o que a TV da porta usa para saber quais alunos mostrar."],
    regras: [],
  },

  "/access/turma-salas": {
    oQueE: "Em qual sala cada turma fica, em quais dias e horários.",
    comoUsar: ["Uma turma pode mudar de sala ao longo do ano; o vínculo tem vigência."],
    regras: [
      "O sistema recusa dois vínculos da mesma turma no mesmo período, dias e horário — e diz qual é o conflito.",
    ],
  },

  "/access/equipamentos": {
    oQueE: "Os leitores de acesso instalados na escola.",
    comoUsar: [
      "Cadastre com IP, porta, portaria, função (quem ele lê) e sentido (entrada ou saída).",
      "'Testar conexão' fala com o equipamento de verdade — se responder, ele está na rede e a senha está certa.",
      "O token de webhook é exibido UMA vez. Copie na hora.",
    ],
    regras: [
      "Leitor sem portaria e sem sentido não serve: é o sentido que permite parear entrada com saída e medir a permanência.",
      "A sincronização é puxada pelo agente local no ritmo dele — não há comando de 'sincronize tudo agora'.",
    ],
  },

  "/access/paineis": {
    oQueE: "As telas que ficam penduradas: a TV na porta da sala e a mesa da coordenação.",
    comoUsar: [
      "Cada painel tem um recorte (uma turma, uma sala, a unidade inteira) e mostra só o que está nele.",
      "Cadastre uma TV para gerar o link com o código. Copie e abra na TV — o código fica guardado nela.",
      "'Exibir foto' liga e desliga a foto naquela tela.",
    ],
    regras: [
      "A TV não opera nada: não tem botão e nada nela muda o estado de uma retirada. O cartão da criança sai quando ela passa o rosto no leitor de saída.",
      "Com 'exibir foto' desligada, a foto não é enviada ao painel — não é só deixar de desenhar na tela.",
      "O código de cada TV é revogável. Se uma TV sumir ou for trocada, revogue o código dela.",
    ],
  },

  "/dispositivos": {
    oQueE: "Todos os equipamentos cadastrados, com estado de conexão.",
    comoUsar: ["Use para ver rapidamente o que está online e ativar ou desativar um equipamento."],
    regras: ["Desativar não apaga o histórico de passagens; o leitor é que deixa de aceitar eventos."],
  },

  "/usuarios": {
    oQueE: "Quem entra no sistema e com qual perfil.",
    comoUsar: [
      "O perfil define o que a pessoa vê. Sem nenhum perfil marcado, ela entra e não vê nenhuma tela.",
      "Definir uma senha aqui obriga a pessoa a trocá-la no primeiro acesso.",
    ],
    regras: [
      "Você não consegue desativar, excluir nem mudar os próprios perfis. É proteção: um administrador único poderia se trancar para fora sem ninguém para devolver o acesso.",
      "Excluir é lógico — a trilha de auditoria continua apontando para quem fez o quê.",
    ],
  },

  "/perfis": {
    oQueE: "O que cada perfil da escola pode fazer, permissão por permissão.",
    comoUsar: [
      "Marque e desmarque permissões por área.",
      "Dá também para conceder uma permissão avulsa a UMA pessoa, sem inventar um perfil novo.",
    ],
    regras: [
      "A mudança vale a partir do PRÓXIMO login de quem tem aquele perfil — as permissões viajam no token.",
      "Permissão extra só ACRESCENTA. Ela nunca tira algo que o perfil dá.",
    ],
  },

  "/auditoria": {
    oQueE: "O registro de quem fez o quê no sistema, com data, hora e IP.",
    comoUsar: ["Use para investigar uma alteração: quem mudou uma nota, quem emitiu um relatório de dados, quem ajustou uma permanência."],
    regras: ["A trilha não é editável. É o que responde à pergunta 'quem fez isso?' meses depois."],
  },

  "/redes": {
    oQueE: "As redes de ensino atendidas pela plataforma.",
    comoUsar: ["Tela da administração da Alfa, não da escola."],
    regras: [],
  },

  "/escolas": {
    oQueE: "As unidades da rede: nome, endereço e contato.",
    comoUsar: ["Uma rede pode ter várias unidades; cada uma tem seu próprio telefone e e-mail."],
    regras: ["A unidade pertence à escola em que você está logado."],
  },

  "/saas": {
    oQueE: "O painel de administração da plataforma.",
    comoUsar: ["Tela da Alfa: planos comerciais e visão geral das redes."],
    regras: [],
  },
};

/**
 * O Portal da Família.
 *
 * Fica separado porque quem lê é pai, mãe ou responsável — geralmente no
 * celular, com pressa, e sem nenhum interesse em como o sistema funciona
 * por dentro. O texto responde ao que ELE quer saber: meu filho está na
 * escola? quem pode buscá-lo? por que fui avisado disso?
 */
export const ajudaDoPortal = [
  {
    titulo: "Início",
    caminho: "/portal",
    oQueE: "O dia de hoje de cada filho seu: se está na escola, a que horas entrou e quanto tempo já ficou.",
    comoUsar: [
      "A tela mostra um cartão por criança.",
      "O selo diz se ela está na escola neste momento.",
      "A barra compara o tempo de hoje com a jornada contratada.",
    ],
  },
  {
    titulo: "Histórico",
    caminho: "/portal/historico",
    oQueE: "Os dias anteriores: entrada, saída e tempo de permanência.",
    comoUsar: [
      "Escolha o filho e o período.",
      "Serve para conferir a cobrança de horas extras antes de questionar.",
    ],
  },
  {
    titulo: "Autorizações",
    caminho: "/portal/autorizacoes",
    oQueE: "Quem pode buscar o seu filho, e o pedido de incluir mais alguém.",
    comoUsar: [
      "A lista mostra quem já está autorizado e a situação de cada um.",
      "Para incluir alguém, informe nome, documento, parentesco e até quando vale.",
      "Você pode pedir dias da semana e faixa de horário — por exemplo, só nas sextas, das 17h às 18h.",
    ],
    aviso:
      "O pedido NÃO libera ninguém sozinho: ele chega à escola como pendente e alguém precisa aprovar. " +
      "Enquanto isso, a pessoa não retira a criança.",
  },
  {
    titulo: "Avisos",
    caminho: "/portal/notificacoes",
    oQueE: "As mensagens que a escola enviou sobre o seu filho.",
    comoUsar: [
      "Marque como lido para tirar da lista de pendentes.",
      "O filtro mostra só os que você ainda não abriu.",
    ],
    aviso:
      "Os avisos nunca trazem foto nem dado biométrico da criança — só a informação de que algo aconteceu.",
  },
];

/**
 * As regras que o sistema não deixa violar, em linguagem de quem opera.
 * Aparecem para todo mundo: elas explicam a maior parte dos "por que não
 * consigo fazer isso?".
 */
export const regrasDoSistema = [
  {
    titulo: "Reconhecer o responsável não entrega a criança",
    texto:
      "São três momentos com horas próprias: o responsável chegou, a criança foi preparada, a criança saiu pelo leitor. " +
      "A permanência só encerra na saída. É o que permite à escola dizer exatamente o que aconteceu e quando.",
  },
  {
    titulo: "Restrição judicial vence tudo",
    texto:
      "Medida protetiva prevalece sobre qualquer autorização, aprovada ou não, inclusive na retirada manual. " +
      "Não há caminho no sistema que libere.",
  },
  {
    titulo: "Autorização temporária exige data de fim",
    texto: "Para nunca virar permanente por esquecimento.",
  },
  {
    titulo: "Na dúvida, o sistema nega",
    texto:
      "Se a verificação falha ou um módulo está fora do ar, a resposta é não liberar. " +
      "Fila parada é problema; porta aberta por engano é outro.",
  },
  {
    titulo: "Dia inconsistente não entra em conta nenhuma",
    texto:
      "Faltou registrar uma saída? O número daquele dia é fantasia da paridade e fica de fora dos totais — " +
      "mas os minutos apurados ficam visíveis para quem for corrigir.",
  },
  {
    titulo: "Mês fechado não é recalculado",
    texto: "Fatura emitida não muda sozinha.",
  },
  {
    titulo: "Retirar, acessar o portal e receber avisos são permissões separadas",
    texto: "Quem recebe o aviso de que a aula acabou não necessariamente pode buscar a criança.",
  },
  {
    titulo: "A TV da sala só informa",
    texto:
      "Ela mostra só os alunos daquela sala, não tem botão e não opera nada. Cada TV tem um código próprio e revogável.",
  },
  {
    titulo: "Biometria de menor exige base legal e consentimento",
    texto:
      "Sem os dois registrados, o rosto não vai para equipamento nenhum. A família pode revogar a qualquer momento, " +
      "e a revogação tira o rosto dos leitores onde ele foi gravado.",
  },
  {
    titulo: "Aviso automático não carrega foto",
    texto: "A mensagem que a família recebe diz que algo aconteceu; ela não leva imagem nem dado biométrico.",
  },
];
