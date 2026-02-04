# 01. Introdução ao AlfaSchool

## 📋 O que é o AlfaSchool?

AlfaSchool é um **sistema de gestão escolar completo** desenvolvido em Laravel (PHP) com foco em segurança e controle de acessos. 

Pense nele como um **painel administrativo** para uma escola onde:

### Quem usa o sistema?

| Perfil | Função | Exemplo de uso |
|--------|---------|----------------|
| **Administrador** | Gerencia todo o sistema | Cria usuários, configura permissões |
| **Gestor Escolar** | Gerencia alunos e turmas | Cadastra novos alunos, cria turmas |
| **Professor** | Lança notas e frequência | Digita notas dos alunos, marca presença |
| **Secretaria** | Faz cadastros e matrículas | Cadastra alunos, emite boletins |
| **Aluno** | Vê seus dados acadêmicos | Consulta notas, vê frequência |

## 🎯 Objetivo do Sistema

O AlfaSchool foi criado para resolver estes problemas:

### Problema 1: Muitas planilhas
**Antes:**
- Planilhas Excel para cada coisa (alunos, notas, frequência)
- Dados perdidos quando o arquivo corrompe
- Difícil compartilhar entre professores

**Depois:**
- Tudo no banco de dados (centralizado)
- Backup automático
- Vários professores acessam ao mesmo tempo

### Problema 2: Falta de controle de acessos
**Antes:**
- Qualquer pessoa acessava o sistema
- Não sabia quem fez o quê
- Alunos podiam ver notas de outros

**Depois:**
- Cada usuário tem suas permissões específicas
- Logs de auditoria (sabe quem fez o quê)
- Alunos veem apenas seus dados

### Problema 3: Falta de segurança
**Antes:**
- Senhas fracas permitidas
- Sem proteção contra ataques
- Se alguém descobrisse senha de um professor, tinha acesso a tudo

**Depois:**
- Senhas fortes obrigatórias
- Rate limiting (limita tentativas de login)
- Proteção contra ataques (OWASP)

## 🚀 Funcionalidades Principais

### 1. Autenticação e Segurança
- ✅ Login seguro com email e senha
- ✅ Registro de novos usuários
- ✅ Recuperação de senha (esqueci a senha)
- ✅ Verificação de email obrigatória
- ✅ Sessão com timeout (15 minutos)
- ✅ Proteção contra ataques de força bruta

### 2. Controle de Acessos (RBAC)
- ✅ 5 perfis de usuário (roles)
- ✅ 22 permissões distribuídas por módulo
- ✅ Múltiplos roles por usuário
- ✅ Sistema granular de permissões

### 3. Gestão Administrativa
- ✅ CRUD completo de usuários
- ✅ Gestão de roles e permissões
- ✅ Atribuição visual de roles
- ✅ Logs de auditoria

### 4. Dashboard
- ✅ Informações do usuário logado
- ✅ Visualização de roles
- ✅ Visualização de permissões

## 📊 Escala do Sistema

### Pequeno (Até 100 alunos)
- 1-2 professores
- 1 secretária
- 1 administrador
- Requer: Servidor básico (1GB RAM)

### Médio (100-500 alunos)
- 5-10 professores
- 2-3 secretárias
- 1-2 gestores
- 1 administrador
- Requer: Servidor médio (2GB RAM)

### Grande (500+ alunos)
- 10+ professores
- 5+ secretárias
- 3+ gestores
- 1-2 administradores
- Requer: Servidor robusto (4GB+ RAM)

## 🎨 Interfaces do Sistema

### Área Pública
- `/login` - Formulário de login
- `/register` - Formulário de registro (se habilitado)
- `/forgot-password` - Recuperação de senha

### Área do Usuário (Autenticado)
- `/dashboard` - Painel principal
- `/profile` - Edição de perfil e senha

### Área Administrativa (Admin)
- `/admin/users` - Gestão de usuários
- `/admin/roles` - Gestão de perfis (roles)
- `/admin/permissions` - Lista de permissões

## 💡 Casos de Uso

### Cenário 1: Novo Professor
1. Administrador acessa `/admin/users`
2. Clica em "Novo Usuário"
3. Preenche: Nome, Email, Senha
4. Marca o checkbox "Professor"
5. Salva
6. Professor recebe email de verificação
7. Professor pode fazer login

### Cenário 2: Aluno Consulta Notas
1. Aluno acessa `/login`
2. Digita email e senha
3. Sistema verifica: Tem role "aluno"? Sim
4. Sistema verifica: Tem permissão "meus_dados.view"? Sim
5. Dashboard mostra notas e frequência
6. Aluno não pode acessar `/admin` (não tem permissão)

### Cenário 3: Professor com Função Extra
1. Coordenador quer que professor João também seja Coordenador
2. Administrador edita João
3. Marca checkboxes: "Professor" E "Coordenador" (criado futuramente)
4. Salva
5. João agora tem permissões de AMBOS os roles

## 🔐 Conceitos de Segurança

### O que é Autenticação?
**Simples:** Saber QUEM é você.
**Exemplo:** Você digita email e senha → Sistema verifica se está correto.

### O que é Autorização?
**Simples:** Saber O QUE você pode fazer.
**Exemplo:** Você está logado → Sistema verifica seu role → Libera ou bloqueia.

### O que é RBAC?
**Simples:** Controle de acesso baseado em ROLES.
**Exemplo:** Tem role "Professor" → Pode lançar notas. Não tem role "Admin" → Não pode excluir usuários.

## 📞 Suporte

- **Documentação:** Veja os outros arquivos nesta pasta
- **Issues:** Abra um ticket no GitHub
- **Email:** Contate a equipe de desenvolvimento

## 🚀 Próximos Passos

1. Leia a [Arquitetura do Sistema](./02-arquitetura.md)
2. Entenda [Como o Sistema Funciona](./03-como-funciona.md)
3. Veja o [Guia para Iniciantes](./04-para-iniciantes.md)
4. Consulte o [Guia de Permissões](./05-guia-de-permissoes.md)
