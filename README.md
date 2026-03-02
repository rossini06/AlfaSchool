# AlfaSchool

> ⚠️ **Migração em andamento para Java + React**
>
> A nova base da aplicação está em:
>
> - `backend/` → Spring Boot (Java 21)
> - `frontend/` → React + Vite
>
> Guia rápido: [MIGRACAO-JAVA-REACT.md](./MIGRACAO-JAVA-REACT.md)

Sistema de gestão escolar completo com autenticação, controle de permissões e segurança avançada.

## 📋 Índice

- [Sobre](#sobre)
- [Começo Rápido](#começo-rápido)
- [Stack Tecnológica](#stack-tecnológica)
- [Credenciais de Acesso](#credenciais-de-acesso)
- [Funcionalidades](#funcionalidades)
- [Sistema Superadmin](#sistema-superadmin)
- [Segurança](#segurança)
- [Permissões e Roles](#permissões-e-roles)
- [phpMyAdmin](#phpmyadmin)
- [Comandos Úteis](#comandos-úteis)
- [Rotas Disponíveis](#rotas-disponíveis)
- [Segurança em Produção](#segurança-em-produção)
- [Contribuindo](#contribuindo)
- [Licença](#licença)

---

## 📖 Sobre

AlfaSchool é um sistema de gestão escolar desenvolvido em Laravel, focado em segurança e controle de acessos. O sistema permite gerenciar alunos, turmas, notas, frequência e financeiro, com um robusto sistema de permissões baseado em roles.

### Documentação Completa

Para iniciantes e novos desenvolvedores, recomendamos fortemente a documentação detalhada disponível na pasta `docs/`:

| Documento | Descrição |
|-----------|-----------|
| [01. Introdução](./docs/01-introducao.md) | Visão geral do sistema, objetivos e casos de uso |
| [02. Arquitetura](./docs/02-arquitetura.md) | Estrutura técnica, padrões MVC e design decisions |
| [03. Como Funciona](./docs/03-como-funciona.md) | Fluxos de dados, autenticação e processos |
| [04. Guia de Permissões](./docs/04-guia-de-permissoes.md) | Como funciona RBAC, roles e permissions |

---

## 🚀 Começo Rápido

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

**Acesse:** `http://localhost:8000`

---

## 🛠️ Stack Tecnológica

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

### Requisitos

- PHP >= 8.5
- Composer
- MySQL >= 5.7 ou MariaDB >= 10.3
- Node.js >= 18
- NPM >= 9

---

## 🔑 Credenciais de Acesso

### Administrador do Sistema

| Campo | Valor |
|-------|-------|
| **Email** | admin@alfaschool.com |
| **Senha** | Admin@123 |

### Superadmin (Desenvolvedor)

| Campo | Valor |
|-------|-------|
| **Email** | superadmin@alfaschool.com |
| **Senha** | SuperAdmin@2024!@#$ |

> **Nota:** A senha do superadmin é apenas para uso do desenvolvedor. Não compartilhe esta credencial.

---

## ✨ Funcionalidades

### Autenticação

- Login e logout seguro
- Recuperação de senha
- Verificação de email obrigatória
- Sessão segura com timeout (15 minutos)
- Proteção contra ataques de força bruta

### Controle de Permissões (RBAC)

**5 Perfis de Usuário:**

| Perfil | Descrição |
|--------|-----------|
| **Admin** | Acesso total ao sistema |
| **Gestor** | Gestão pedagógica e administrativa |
| **Professor** | Lançamento de notas e frequência |
| **Secretaria** | Cadastros e matrículas |
| **Aluno** | Visualização de dados pessoais e acadêmicos |

**Módulos com Permissões:**

| Módulo | Permissões |
|--------|------------|
| Usuários | view, create, edit, delete |
| Perfis | view, edit |
| Alunos | view, create, edit, delete |
| Turmas | view, create, edit, delete |
| Notas | view, create, edit |
| Frequência | view, create |
| Financeiro | view, edit |
| Meus Dados | view |

### Interface Amigável

- Descrições em português claro
- Sem nomes técnicos confusos
- Ajuda contextual em cada seção
- Indicadores visuais de força de senha
- Validação em tempo real

### Gestão Administrativa

- CRUD completo de usuários
- Gestão de roles e permissões
- Atribuição múltipla de roles
- Logs de auditoria de ações sensíveis

---

## 👑 Sistema Superadmin

O sistema inclui um usuário **superadmin** especial para uso exclusivo do desenvolvedor.

### Características

| Característica | Descrição |
|----------------|-----------|
| **Acesso** | Total ao sistema |
| **Visibilidade** | Não aparece na lista de usuários do admin |
| **Edição** | Apenas o próprio superadmin pode editar |
| **Exclusão** | Impossível via interface |

### Criar/Recriar Superadmin

```bash
php artisan db:seed --class=SuperAdminSeeder
```

### Verificar Superadmin

```bash
php artisan tinker --execute="\$u = \App\Models\User::where('email', 'superadmin@alfaschool.com')->first(); echo \$u->name . ' - Superadmin: ' . (\$u->isSuperAdmin() ? 'SIM' : 'NÃO');"
```

---

## 🔒 Segurança

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

### 2. Bloqueio de Conta por Tentativas Falhas

O sistema implementa proteção contra ataques de força bruta, bloqueando temporariamente contas após múltiplas tentativas de login incorretas.

#### Sistema de Bloqueio Progressivo

| Tentativas Falhas | Bloqueio | Tempo |
|-------------------|----------|-------|
| 5 tentativas | 🔒 Leve | 15 minutos |
| 10 tentativas | 🔒 Médio | 1 hora |
| 15+ tentativas | 🔒 Alto | 24 horas |

#### Mensagens de Erro

```
1-4 tentativas: "Email ou senha incorretos. Você tem X tentativas restantes."
5-9 tentativas: "Conta bloqueada por 15 minutos. Tente novamente mais tarde."
10-14 tentativas: "Conta bloqueada por 1 hora. Tente novamente mais tarde."
15+ tentativas: "Conta bloqueada por 24 horas. Por favor, entre em contato."
```

#### Comandos de Gerenciamento

```bash
# Listar usuários bloqueados
php artisan users:locked --all

# Limpar bloqueios expirados
php artisan users:clear-locks

# Desbloquear usuário específico
php artisan users:unlock email@exemplo.com

# Resetar tentativas de login
php artisan users:reset email@exemplo.com
```

#### Reset de Contador

O contador de tentativas é resetado quando:
- Login bem-sucedido é realizado
- Tempo de bloqueio expira

### 3. Rate Limiting

| Rota | Limite |
|------|--------|
| Login | 5 tentativas/minuto |
| Registro | 3 tentativas/minuto |
| Esqueci Senha | 3 tentativas/minuto |
| Logout | 10 tentativas/minuto |
| Rotas Admin | 60 requisições/minuto |

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

### 5. HTTP Security Headers

```
X-Content-Type-Options: nosniff
X-Frame-Options: SAMEORIGIN
X-XSS-Protection: 1; mode=block
Referrer-Policy: strict-origin-when-cross-origin
Permissions-Policy: geolocation=(), microphone=(), camera=()
Strict-Transport-Security: max-age=31536000; includeSubDomains
Content-Security-Policy: default-src 'self'...
```

---

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

### Interface Amigável

O sistema foi projetado para ser usado por usuários **não-técnicos**:

| Nomes Técnicos | Para o Usuário |
|----------------|----------------|
| `users.view` | "Pode ver usuários" |
| `users.create` | "Pode criar novos registros" |
| `users.edit` | "Pode editar registros existentes" |
| `users.delete` | "Pode excluir registros" |

---

## 🗄️ phpMyAdmin

O sistema inclui o **phpMyAdmin** para gerenciamento visual do banco de dados MySQL.

### Como Acessar

**URL:** `http://localhost:8000/phpmyadmin/`

### Credenciais

| Campo | Valor |
|-------|-------|
| **Servidor** | 127.0.0.1 |
| **Usuário** | adminer |
| **Senha** | adminer123 |
| **Banco** | alfaschool |

### O que você pode fazer

- Visualizar todas as tabelas do banco
- Ver e editar dados de usuários
- Gerenciar papéis e permissões
- Executar consultas SQL
- Fazer backups (exportar dados)

---

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

---

## 📍 Rotas Disponíveis

### Autenticação

| Rota | Método | Descrição |
|------|--------|------------|
| `/login` | GET/POST | Login de usuário |
| `/logout` | POST | Logout de usuário |
| `/forgot-password` | GET/POST | Recuperação de senha |
| `/reset-password/{token}` | GET/POST | Redefinir senha |

### Dashboard

| Rota | Descrição |
|------|------------|
| `/dashboard` | Dashboard principal |
| `/` | Redireciona para dashboard ou login |

### Área Administrativa

| Rota | Middleware | Descrição |
|------|------------|------------|
| `/admin` | role:admin | Painel administrativo |
| `/admin/users` | permission:users.view | Listar usuários |
| `/admin/users/create` | permission:users.create | Criar usuário |
| `/admin/users/{user}/edit` | permission:users.edit | Editar usuário |
| `/admin/users/{user}` | permission:users.delete | Excluir usuário |
| `/admin/roles` | permission:roles.view | Listar perfis |
| `/admin/roles/{role}/edit` | permission:roles.edit | Editar perfil |
| `/admin/permissions` | permission:roles.view | Listar permissões |

### Atalhos Diretos

```bash
# Login
http://localhost:8000/login

# Dashboard
http://localhost:8000/dashboard

# Admin - Usuários
http://localhost:8000/admin/users
http://localhost:8000/admin/users/create

# Admin - Perfis
http://localhost:8000/admin/roles

# phpMyAdmin
http://localhost:8000/phpmyadmin/
```

---

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

---

## 🤝 Contribuindo

1. Faça um Fork do projeto
2. Crie uma branch para sua feature (`git checkout -b feature/NovaFuncionalidade`)
3. Commit suas mudanças (`git commit -m 'Adiciona nova funcionalidade'`)
4. Push para a branch (`git push origin feature/NovaFuncionalidade`)
5. Abra um Pull Request

---

## 📄 Licença

Este projeto está licenciado sob a Licença MIT.

---

**Desenvolvido com ❤️ usando Laravel**
