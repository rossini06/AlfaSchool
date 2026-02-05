# AlfaSchool

Sistema de gestão escolar completo com autenticação, controle de permissões e segurança avançada.

## 📋 Índice

- [Sobre](#sobre)
- [Documentação Completa](#documentação-completa)
- [Funcionalidades](#funcionalidades)
- [Stack Tecnológica](#stack-tecnológica)
- [Requisitos](#requisitos)
- [Instalação](#instalação)
- [Configuração](#configuração)
- [Segurança](#segurança)
- [Permissões e Roles](#permissões-e-roles)
- [Estrutura do Projeto](#estrutura-do-projeto)
- [Licença](#licença)

## 📋 Índice

- [Sobre](#sobre)
- [Documentação Completa](#documentação-completa)
- [Funcionalidades](#funcionalidades)
- [Stack Tecnológica](#stack-tecnológica)
- [Requisitos](#requisitos)
- [Instalação](#instalação)
- [Configuração](#configuração)
- [Segurança](#segurança)
- [Permissões e Roles](#permissões-e-roles)
- [Estrutura do Projeto](#estrutura-do-projeto)
- [Licença](#licença)

## 📖 Sobre

AlfaSchool é um sistema de gestão escolar desenvolvido em Laravel, focado em segurança e controle de acessos. O sistema permite gerenciar alunos, turmas, notas, frequência e financeiro, com um robusto sistema de permissões baseado em roles.

AlfaSchool é um sistema de gestão escolar desenvolvido em Laravel, focado em segurança e controle de acessos. O sistema permite gerenciar alunos, turmas, notas, frequência e financeiro, com um robusto sistema de permissões baseado em roles.

## 📚 Documentação Completa

Para iniciantes e novos desenvolvedores, recomendamos fortemente a documentação detalhada disponível na pasta `docs/`:

### 📖 Documentação Disponível

| Documento | Descrição |
|-----------|-----------|
| [01. Introdução](./docs/01-introducao.md) | Visão geral do sistema, objetivos e casos de uso |
| [02. Arquitetura](./docs/02-arquitetura.md) | Estrutura técnica, padrões MVC e design decisions |
| [03. Como Funciona](./docs/03-como-funciona.md) | Fluxos de dados, autenticação e processos |
| [05. Guia de Permissões](./docs/04-guia-de-permissoes.md) | Como funciona RBAC, roles e permissions |

### 🚀 Começo Rápido

Se você é novo no projeto, comece aqui:

1. **Leia a Introdução** para entender o que é o AlfaSchool
2. **Siga a Stack Tecnológica** para configurar o ambiente
3. **Siga a Instalação** para colocar o sistema para rodar
4. **Consulte o Guia de Permissões** para entender o controle de acessos

### 📖 Conceitos Explicados Simplesmente

| Conceito | Explicação Simples | Exemplo |
|----------|-------------------|---------|
| **Framework** | Kit de ferramentas prontas | Laravel |
| **Model** | Molde de dados (como uma ficha de usuário) | User.php |
| **Controller** | Cérebro que processa pedidos | UserController.php |
| **View** | O que o usuário vê (HTML) | index.blade.php |
| **Route** | Endereço da página | /admin/users |
| **Middleware** | Porteiro que verifica permissões | auth, role |
| **Migration** | Instruções para criar tabelas | create_users_table.php |
| **Seeder** | Preenchimento automático do banco | RolePermissionSeeder.php |
| **Role** | Função do usuário (professor, aluno) | 'professor' |
| **Permission** | O que pode fazer (ver, criar, editar) | 'users.create' |
| **RBAC** | Controle de acesso baseado em roles | Spatie Permission |
| **Sessão** | "Memória" do servidor sobre quem está logado | $_SESSION |
| **Hash** | Transforma senha em código secreto | bcrypt('senha') |
| **CSRF** | Proteção contra formulários falsos | @csrf |
| **XSS** | Proteção contra injeção de código malicioso | Security Headers |

## ✨ Funcionalidades

### Autenticação
- Login e registro de usuários
- Recuperação de senha
- Verificação de email obrigatória
- Sessão segura com timeout
- Proteção contra ataques de força bruta

### Controle de Permissões (RBAC)
- **5 Perfis de Usuário:**
  - **Admin**: Acesso total ao sistema
  - **Gestor**: Gestão pedagógica e administrativa
  - **Professor**: Lançamento de notas e frequência
  - **Secretaria**: Cadastros e matrículas
  - **Aluno**: Visualização de dados pessoais e acadêmicos

- **22 Permissões** distribuídas por módulo:
  - Usuários (view, create, edit, delete)
  - Roles (view, edit)
  - Alunos (view, create, edit, delete)
  - Turmas (view, create, edit, delete)
  - Notas (view, create, edit)
  - Frequência (view, create)
  - Financeiro (view, edit)
  - Meus Dados (view)

### Dashboard
- Exibição de informações do usuário logado
- Lista de perfis e permissões
- Indicadores de segurança

### Interface Amigável
- **Sem nomes técnicos confusos**: O sistema usa descrições em português claro
- **Sem jargon técnico**: Usuários não-técnicos podem gerenciar acessos facilmente
- **Ajuda contextual**: Cada seção tem instruções claras

| O que o usuário vê | Significado |
|---------------------|-------------|
| "Pode ver usuários" | Acesso para visualizar lista de usuários |
| "Pode criar novos registros" | Acesso para cadastrar novos itens |
| "Pode editar registros" | Acesso para alterar dados existentes |
| "Pode excluir registros" | Acesso para remover itens |

### Gestão Administrativa
- CRUD completo de usuários
- Gestão de roles e permissões
- Atribuição múltipla de roles
- Logs de auditoria de ações sensíveis

## 🚀 Stack Tecnológica

| Componente | Versão |
|------------|---------|
| **Backend** | |
| Laravel | 12.x |
| PHP | 8.5+ |
| **Frontend** | |
| Blade | 4.x |
| Tailwind CSS | 4.x |
| **Banco de Dados** | |
| MySQL | 9.x |
| **Autenticação** | |
| Laravel Breeze | 2.x |
| Spatie Laravel Permission | 6.x |

## 📦 Requisitos

- PHP >= 8.5
- Composer
- MySQL >= 5.7 ou MariaDB >= 10.3
- Node.js >= 18
- NPM >= 9

## 🔧 Instalação

### 1. Clone o repositório

```bash
git clone https://github.com/rossini06/AlfaSchool.git
cd AlfaSchool
```

### 2. Instale as dependências

```bash
composer install
npm install
```

### 3. Configure o arquivo `.env`

```bash
cp .env.example .env
```

Edite o arquivo `.env` com suas credenciais de banco de dados:

```env
DB_CONNECTION=mysql
DB_HOST=127.0.0.1
DB_PORT=3306
DB_DATABASE=alfaschool
DB_USERNAME=seu_usuario
DB_PASSWORD=sua_senha
```

### 4. Gere a chave da aplicação

```bash
php artisan key:generate
```

### 5. Execute as migrations

```bash
php artisan migrate
```

### 6. Execute os seeders

```bash
php artisan db:seed
```

### 7. Compile os assets

```bash
npm run build
```

### 8. Inicie o servidor de desenvolvimento

```bash
php artisan serve
```

Acesse: `http://localhost:8000`

## ⚙️ Configuração

### Credenciais de Acesso

Após executar os seeders, use estas credenciais para acesso inicial:

- **Email:** `admin@alfaschool.com`
- **Senha:** `Admin@123`

### Variáveis de Ambiente

Principais variáveis configuráveis:

```env
APP_NAME=AlfaSchool
APP_ENV=local
APP_DEBUG=false

# Banco de Dados
DB_CONNECTION=mysql
DB_DATABASE=alfaschool
DB_USERNAME=root
DB_PASSWORD=

# Sessão
SESSION_LIFETIME=15
SESSION_ENCRYPT=true

# Cache
CACHE_STORE=database

# Queue
QUEUE_CONNECTION=database
```

## 🔒 Segurança

O sistema AlfaSchool implementa as melhores práticas de segurança (OWASP):

### 1. Autenticação Forte
- Senhas com hash bcrypt (12 rounds)
- Validação de força de senha:
  - Mínimo 8 caracteres
  - 1 letra maiúscula
  - 1 letra minúscula
  - 1 número
  - 1 caractere especial
- Sessão criptografada
- Timeout de sessão (15 minutos)

### 2. Rate Limiting
- **Login:** 5 tentativas por minuto
- **Registro:** 3 tentativas por minuto
- **Esqueci Senha:** 3 tentativas por minuto
- **Logout:** 10 tentativas por minuto
- **Rotas Admin:** 60 requisições por minuto

### 3. Proteção contra Ataques
- CSRF protection em todos os formulários
- XSS Protection headers
- SQL Injection protection via Eloquent ORM
- Content Security Policy (CSP)
- HTTP Security Headers (Helmet)

### 4. Auditoria
- Logs dedicados para ações sensíveis
- Registro de login/logout
- Registro de alterações em usuários
- Registro de alterações em permissões

### 5. Verificação de Email
- Verificação obrigatória antes do acesso
- Rotas assinadas para segurança
- Limite de envio de emails

### 6. HTTP Security Headers

```
X-Content-Type-Options: nosniff
X-Frame-Options: SAMEORIGIN
X-XSS-Protection: 1; mode=block
Referrer-Policy: strict-origin-when-cross-origin
Permissions-Policy: geolocation=(), microphone=(), camera=()
Strict-Transport-Security: max-age=31536000; includeSubDomains
Content-Security-Policy: default-src 'self'...
```

## 👥 Permissões e Roles

### Interface Amigável

O sistema foi projetado para ser usado por usuários **não-técnicos**. Ao gerenciar permissões, o usuário vê descrições claras em português, sem nomes técnicos confusos.

#### Exemplo de Interface

| Antes (confuso para leigos) | Depois (claro e intuitivo) |
|------------------------------|---------------------------|
| ✅ "Pode ver usuários" | ✅ "Pode ver usuários" |
| ❌ `users.view` | ~~`users.view`~~ (escondido) |
| ❌ `financeiro.edit` | ~~`financeiro.edit`~~ (escondido) |

#### Como Aparece para o Usuário

```
🎓 Módulo de Alunos
Gerenciar dados dos alunos

☑️ Pode ver esta função
☑️ Pode criar novos registros
☑️ Pode editar registros existentes
```

### Hierarquia de Perfis

```
Admin
├── Acesso total a todas as permissões
└── Não tem restrições

Gestor
├── alunos.view, create, edit
├── turmas.view, create, edit
├── notas.view
├── frequencia.view
├── financeiro.view
└── meus_dados.view

Professor
├── alunos.view
├── turmas.view
├── notas.view, create, edit
├── frequencia.view, create
└── meus_dados.view

Secretaria
├── alunos.view, create, edit
├── turmas.view
└── meus_dados.view

Aluno
└── meus_dados.view
```

### Permissões por Módulo

O usuário vê descrições amigáveis como "Pode ver usuários" em vez de `users.view`.

| Módulo | Descrição para Usuário | Roles que Acessam |
|--------|------------------------|-------------------|
| **Usuários** | Gerenciar usuários do sistema | Admin |
| **Perfis** | Gerenciar perfis e permissões | Admin |
| **Alunos** | Gerenciar dados dos alunos | Admin, Gestor, Secretaria |
| **Turmas** | Gerenciar turmas e disciplinas | Admin, Gestor, Professor |
| **Notas** | Lançar e visualizar notas | Admin, Gestor, Professor |
| **Frequência** | Registrar presença dos alunos | Admin, Gestor, Professor |
| **Financeiro** | Ver e editar dados financeiros | Admin, Gestor |

### Código Técnico (Interno)

Para referência da equipe de desenvolvimento, as permissões são armazenadas com nomes técnicos no banco:

| Permissão Técnica | Descrição | O que Controla |
|-------------------|-----------|---------------|
| `users.view` | Pode ver usuários | Lista e detalhes de usuários |
| `users.create` | Pode criar usuários | Cadastro de novos usuários |
| `users.edit` | Pode editar usuários | Alteração de dados |
| `users.delete` | Pode excluir usuários | Remoção de usuários |
| `alunos.view` | Pode ver alunos | Lista e detalhes de alunos |
| `alunos.create` | Pode criar alunos | Cadastro de novos alunos |
| `alunos.edit` | Pode editar alunos | Alteração de dados |
| `alunos.delete` | Pode excluir alunos | Remoção de alunos |
| `turmas.view` | Pode ver turmas | Lista e detalhes de turmas |
| `turmas.create` | Pode criar turmas | Cadastro de novas turmas |
| `turmas.edit` | Pode editar turmas | Alteração de dados |
| `turmas.delete` | Pode excluir turmas | Remoção de turmas |
| `notas.view` | Pode ver notas | Visualização de notas |
| `notas.create` | Pode criar notas | Lançamento de notas |
| `notas.edit` | Pode editar notas | Alteração de notas |
| `frequencia.view` | Pode ver frequência | Visualização de frequência |
| `frequencia.create` | Pode criar frequência | Registro de presença |
| `financeiro.view` | Pode ver financeiro | Visualização de dados financeiros |
| `financeiro.edit` | Pode editar financeiro | Alteração de dados financeiros |
| `meus_dados.view` | Pode ver meus dados | Visualização de próprios dados |
| `meus_dados.edit` | Pode editar meus dados | Alteração de próprios dados |
| `roles.view` | Pode ver perfis | Lista de perfis |
| `roles.edit` | Pode editar perfis | Alteração de permissões |

## 📁 Estrutura do Projeto

```
alfaschool/
├── app/
│   ├── Http/
│   │   ├── Controllers/
│   │   │   ├── Admin/
│   │   │   │   ├── UserController.php
│   │   │   │   ├── RoleController.php
│   │   │   │   └── PermissionController.php
│   │   │   ├── Auth/
│   │   │   ├── DashboardController.php
│   │   │   └── ProfileController.php
│   │   ├── Middleware/
│   │   │   ├── LogAuditableActions.php
│   │   │   └── SecurityHeaders.php
│   ├── Models/
│   │   └── User.php
│   └── Providers/
│       └── AppServiceProvider.php
├── database/
│   ├── migrations/
│   └── seeders/
│       ├── DatabaseSeeder.php
│       ├── RolePermissionSeeder.php
│       └── AdminSeeder.php
├── resources/
│   ├── views/
│   │   ├── admin/
│   │   │   ├── users/
│   │   │   ├── roles/
│   │   │   └── permissions/
│   │   ├── auth/
│   │   ├── layouts/
│   │   └── components/
│   ├── css/
│   └── js/
├── routes/
│   ├── web.php
│   ├── auth.php
│   └── console.php
├── config/
│   ├── auth.php
│   ├── permission.php
│   └── logging.php
├── public/
└── storage/
    ├── logs/
    │   ├── laravel.log
    │   └── audit-YYYY-MM-DD.log
```

## 🛠️ Comandos Úteis

### Desenvolvimento

```bash
# Iniciar servidor
php artisan serve

# Compilar assets em desenvolvimento
npm run dev

# Compilar assets para produção
npm run build

# Limpar cache
php artisan cache:clear
php artisan config:clear
php artisan route:clear
php artisan view:clear
```

### Banco de Dados

```bash
# Executar migrations
php artisan migrate

# Reverter última migration
php artisan migrate:rollback

# Executar seeders
php artisan db:seed

# Limpar banco e refazer migrations
php artisan migrate:fresh --seed
```

### Logs

```bash
# Visualizar logs gerais
tail -f storage/logs/laravel.log

# Visualizar logs de auditoria
tail -f storage/logs/audit-$(date +%Y-%m-%d).log
```

## 📊 Rotas Disponíveis

| Rota | Método | Middleware | Descrição |
|-------|---------|------------|------------|
| `/` | GET | - | Redireciona para login |
| `/login` | GET/POST | guest | Login de usuário |
| `/register` | GET/POST | guest | Registro de usuário |
| `/forgot-password` | GET/POST | guest | Recuperação de senha |
| `/dashboard` | GET | auth, verified | Dashboard principal |
| `/admin` | GET | auth, role:admin, throttle | Área administrativa |
| `/admin/users` | GET | auth, permission:users.view | Listar usuários |
| `/admin/users/create` | GET/POST | auth, permission:users.create | Criar usuário |
| `/admin/roles` | GET | auth, permission:roles.view | Listar roles |
| `/admin/permissions` | GET | auth, permission:roles.view | Listar permissões |

## 🔐 Segurança em Produção

Antes de colocar em produção:

1. **Alterar ambiente para produção**
```env
APP_ENV=production
APP_DEBUG=false
```

2. **Configurar HTTPS real** com certificado SSL válido

3. **Usar banco de dados dedicado** (não SQLite)

4. **Configurar backup automático** do banco de dados

5. **Revisar políticas de retenção** de logs

6. **Configurar firewall** e WAF

7. **Monitorar logs de auditoria** regularmente

## 🤝 Contribuindo

1. Faça um Fork do projeto
2. Crie uma branch para sua feature (`git checkout -b feature/NovaFuncionalidade`)
3. Commit suas mudanças (`git commit -m 'Adiciona nova funcionalidade'`)
4. Push para a branch (`git push origin feature/NovaFuncionalidade`)
5. Abra um Pull Request

## 📄 Licença

Este projeto está licenciado sob a Licença MIT.

## 👥 Suporte

Para suporte, abra uma issue no repositório do GitHub.

---

**Desenvolvido com ❤️ usando Laravel**
