# AlfaSchool

Sistema de gestão escolar completo com autenticação, controle de permissões e segurança avançada.

## 📋 Índice

- [Sobre](#sobre)
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

| Módulo | Permissões | Roles que Acessam |
|---------|-------------|-------------------|
| **Usuários** | view, create, edit, delete | Admin |
| **Roles** | view, edit | Admin |
| **Alunos** | view, create, edit, delete | Admin, Gestor, Secretaria |
| **Turmas** | view, create, edit, delete | Admin, Gestor, Professor |
| **Notas** | view, create, edit | Admin, Gestor, Professor |
| **Frequência** | view, create | Admin, Gestor, Professor |
| **Financeiro** | view, edit | Admin, Gestor |

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
