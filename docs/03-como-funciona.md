# 03. Como o Sistema Funciona

## 📋 Visão Geral

Este documento explica o fluxo completo do sistema AlfaSchool, desde o momento em que um usuário acessa o site até a conclusão de uma ação.

## 🔄 Fluxo Completo: Do Login ao Dashboard

### Etapa 1: Usuário Acessa o Site

```
Usuário digit: http://localhost:8000
           ↓
      Route::get('/', ...)
           ↓
   Redireciona para /login
           ↓
Usuário vê formulário de login
```

**O que acontece:**
1. Usuário digita o endereço do site
2. Rota `/` redireciona para `/login`
3. View `login.blade.php` é exibida

**Arquivos envolvidos:**
- `routes/web.php` (Rota `/`)
- `routes/auth.php` (Rota `/login`)
- `resources/views/auth/login.blade.php` (Formulário)

---

### Etapa 2: Usuário Preenche Login

```
Usuário digita:
  - Email: admin@alfaschool.com
  - Senha: Senha@123
           ↓
   Clica em "Entrar"
           ↓
Formulário envia: POST /login
           ↓
Route: AuthenticatedSessionController@store
```

**O que acontece:**
1. Formulário envia email e senha
2. Middleware `throttle:5,1` verifica tentativas (limite 5/min)
3. Middleware `guest` verifica se NÃO está logado

**Arquivos envolvidos:**
- `app/Http/Controllers/Auth/AuthenticatedSessionController.php`
- `routes/auth.php`

---

### Etapa 3: Autenticação

```
AuthenticatedSessionController@store()
           ↓
Auth::attempt([...email, ...password])
           ↓
Busca usuário no banco via Model User
           ↓
Verifica senha com bcrypt()
           ↓
Email está correto? SIM
Senha está correta? SIM
           ↓
Cria sessão (login bem-sucedido)
```

**O que acontece:**
1. Laravel busca usuário no banco (via Model User)
2. Verifica se a senha corresponde ao hash
3. Se correto, cria sessão
4. Se incorreto, retorna erro

**Arquivos envolvidos:**
- `app/Models/User.php`
- Banco de dados MySQL
- Sistema de autenticação do Laravel

---

### Etapa 4: Redirecionamento

```
Após login bem-sucedido
           ↓
RedirectUsersTo middleware
           ↓
Redireciona para /dashboard
```

**O que acontece:**
1. Middleware verifica se login foi bem-sucedido
2. Redireciona automaticamente para `/dashboard`

**Arquivos envolvidos:**
- `bootstrap/app.php` (Configuração de redirecionamento)
- `routes/web.php`

---

### Etapa 5: Acesso ao Dashboard

```
Usuário é redirecionado para: /dashboard
           ↓
Route::get('/dashboard', DashboardController@index)
           ↓
Middleware verifica:
  - Está logado? SIM
  - Tem email verificado? SIM
           ↓
DashboardController@index() é executado
           ↓
Busca usuário: auth()->user()
Busca roles: $user->roles
Busca permissões: $user->getAllPermissions()
           ↓
View dashboard.blade.php é exibida
           ↓
Usuário vê: nome, roles, permissões
```

**O que acontece:**
1. Middleware `auth` verifica se usuário está logado
2. Middleware `verified` verifica se email foi confirmado
3. Controller busca dados do usuário, roles e permissões
4. View gera HTML com essas informações

**Arquivos envolvidos:**
- `app/Http/Controllers/DashboardController.php`
- `routes/web.php`
- `resources/views/dashboard.blade.php`
- `app/Providers/AppServiceProvider.php` (Gate before)

---

### Etapa 6: Usuário Acessa Área Admin

```
Usuário clica em link para /admin/users
           ↓
Route::get('/admin/users', UserController@index)
           ↓
Middleware verifica:
  - Está logado? SIM
  - Tem role:admin? SIM (via Gate)
  - Tem permissão:users.view? SIM
           ↓
UserController@index() é executado
           ↓
Busca usuários: User::with('roles')->paginate(10)
           ↓
View admin/users/index.blade.php é exibida
           ↓
Usuário vê tabela de usuários
```

**O que acontece:**
1. Usuário clica em link para área admin
2. Middleware `role:admin` verifica se tem role admin
3. Middleware `permission:users.view` verifica se tem permissão
4. Gate before: admin tem acesso TOTAL (retorna true imediatamente)
5. Controller busca usuários no banco
6. View gera tabela HTML

**Arquivos envolvidos:**
- `app/Http/Controllers/Admin/UserController.php`
- `routes/web.php`
- `resources/views/admin/users/index.blade.php`
- `app/Providers/AppServiceProvider.php` (Gate)

**Mecanismo do Gate:**
```php
Gate::before(function ($user, $ability) {
    // Se usuário tem role admin, retorna TRUE para TUDO
    return $user->hasRole('admin') ? true : null;
});
```
Isso significa que admin pula verificações de permissões!

---

## 🔐 Fluxo de Verificação de Permissões

### Cenário 1: Admin Acessando Qualquer Coisa

```
Usuário admin clica em /admin/roles
           ↓
Middleware role:admin
           ↓
Verifica: $user->hasRole('admin')
           ↓
Resultado: TRUE
           ↓
Permite acesso (não verifica mais nada)
```

### Cenário 2: Professor Acessando Notas

```
Usuário professor clica em /admin/notas
           ↓
Middleware permission:notas.view
           ↓
Verifica: $user->hasPermissionTo('notas.view')
           ↓
Resultado: TRUE (professor tem essa permissão)
           ↓
Permite acesso
```

### Cenário 3: Aluno Tentando Acessar Admin

```
Usuário aluno clica em /admin/users
           ↓
Middleware role:admin
           ↓
Verifica: $user->hasRole('admin')
           ↓
Resultado: FALSE
           ↓
Retorna erro 403 (Forbidden)
           ↓
Mensagem: "Você não tem permissão"
```

---

## 💾 Fluxo de Dados no Banco

### Criar Novo Usuário

```
1. Admin preenche formulário
   ↓
2. Formulário enviado: POST /admin/users
   ↓
3. Middleware audit: registra ação (ANTES de processar)
   ↓
4. UserController@store()
   ↓
5. Validação dos dados:
   - Nome obrigatório ✓
   - Email válido e único ✓
   - Senha forte (8+ maiúscula número especial) ✓
   ↓
6. Model User::create([...])
   ↓
   MySQL: INSERT INTO users (...)
   ↓
7. Retorna ID do usuário criado
   ↓
8. $user->assignRole(['admin'])
   ↓
   MySQL: INSERT INTO model_has_roles (...)
   ↓
9. Middleware audit: registra ação (DEPOIS de processar)
   ↓
10. Redireciona para /admin/users
   ↓
11. View lista novos usuários
```

**Query SQL executada:**
```sql
-- 1. Cria usuário
INSERT INTO users (name, email, password, created_at, updated_at)
VALUES ('João', 'joao@email.com', '$2y$10$...', NOW(), NOW());

-- 2. Atribui role
INSERT INTO model_has_roles (role_id, model_type, model_id)
VALUES (1, 'App\\Models\\User', 42);
```

---

## 🔄 Fluxo de Sessão

### Login (Criar Sessão)

```
Usuário faz login
           ↓
Auth::attempt([...])
           ↓
Laravel:
  1. Gera session_id aleatória
  2. Cria registro em sessions table
           ↓
MySQL: INSERT INTO sessions (id, user_id, ip_address, user_agent, payload, last_activity)
VALUES ('abc123...', 1, '192.168.1.1', 'Mozilla/5.0...', '...', NOW())
           ↓
Laravel envia cookie PHPSESSID=abc123...
           ↓
Navegador armazena cookie
           ↓
Próxima requisição: navegador envia cookie
           ↓
Laravel lê cookie, busca sessão
           ↓
Recupera usuário_id da sessão
           ↓
Usuário está autenticado!
```

### Logout (Destruir Sessão)

```
Usuário clica em "Sair"
           ↓
POST /logout
           ↓
Middleware audit: registra logout
           ↓
Auth::logout()
           ↓
Laravel:
  1. Remove sessão do banco
  2. Limpa dados da memória
           ↓
DELETE FROM sessions WHERE id = 'abc123...'
           ↓
Navegador remove cookie
           ↓
Próxima requisição: usuário NÃO está logado
           ↓
Middleware auth redireciona para /login
```

---

## 📊 Fluxo de Auditoria

### Middleware Audit: Registrando Tudo

```
Usuário faz ação (ex: criar usuário)
           ↓
Requisição HTTP chega ao servidor
           ↓
Middleware LogAuditableActions captura
           ↓
Coleta informações:
  - user_id: 1
  - user_email: admin@alfaschool.com
  - action: Criar usuário
  - route: admin.users.store
  - method: POST
  - ip: 192.168.1.1
  - user_agent: Mozilla/5.0...
  - timestamp: 2026-02-04 15:30:00
           ↓
Log::channel('audit')->info('Auditable action', [...])
           ↓
Grava em arquivo: storage/logs/audit-2026-02-04.log
           ↓
Formato do log:
[2026-02-04 15:30:00] local.INFO: Auditable action {"user_id":1,"user_email":"admin@...","action":"Criar usuário",...}
           ↓
Próxima requisição (controller executado)
           ↓
Resposta enviada ao navegador
```

**Por que registrar TUDO?**
- Rastrear problemas: "Quem excluiu o usuário X?"
- Auditoria: "O que o admin Y fez ontem?"
- Segurança: "Detectar atividades suspeitas"

---

## 🛡️ Fluxo de Rate Limiting

### Cenário: Ataque de Força Bruta

```
Hacker tenta fazer login 10 vezes em 1 minuto
           ↓
Tentativa 1: POST /login
  → Middleware throttle: permite (1/5)
  → Senha errada
           ↓
Tentativa 2: POST /login
  → Middleware throttle: permite (2/5)
  → Senha errada
           ↓
...
Tentativa 5: POST /login
  → Middleware throttle: permite (5/5)
  → Senha errada
           ↓
Tentativa 6: POST /login
  → Middleware throttle: BLOQUEIA (6/5)
  → Retorna erro 429 (Too Many Requests)
           ↓
Hacker vê: "Muitas tentativas. Tente novamente em 60 segundos."
```

**Como funciona:**
```php
Route::post('login', ...)->middleware('throttle:5,1')
//                       5        1
//                    tentativas  minuto
```

Laravel usa Redis/Database para contar tentativas por IP.

---

## 🎯 Fluxo de Verificação de Email

### Usuário Faz Registro

```
1. Usuário se cadastra
   ↓
Model User::create([...])
   ↓
Email enviado para usuário: mailto:joao@email.com
   Link: /email/verify/{id}/{hash}
   ↓
Usuário recebe email
           ↓
2. Usuário clica no link
   ↓
Route: /email/verify/{id}/{hash}
           ↓
Middleware signed: verifica se link foi gerado pelo sistema
           ↓
Verifica: link não expirou e não foi alterado
           ↓
Se válido: email_verified_at = NOW()
           ↓
Usuário pode acessar /dashboard (middleware verified)
```

**Por que verificar email?**
- Evita emails falsos
- Confirma que o usuário é o dono do email
- Segurança adicional

---

## 📋 Fluxo Completo: Criar Usuário

```
┌─────────────────────────────────────────────────────────┐
│ 1. ADMIN ACESSA /ADMIN/USUSARIOS               │
│    ↓                                                │
│ 2. RENDERIZA VIEW (LISTA DE USUÁRIOS)          │
│    ↓                                                │
│ 3. ADMIN CLICA EM "NOVO USUÁRIO"               │
│    ↓                                                │
│ 4. ROUTE: /ADMIN/USUSARIOS/CREATE                  │
│    ↓                                                │
│ 5. MIDDLEWARE (PERMISSION: USERS.CREATE)             │
│    - Tem permissão? SIM                              │
│    ↓                                                │
│ 6. RENDERIZA VIEW (FORMULÁRIO)                  │
│    ↓                                                │
│ 7. ADMIN PREENCHE FORMULÁRIO                      │
│    - Nome: João                                      │
│    - Email: joao@escola.com                         │
│    - Senha: Senha@123                               │
│    - Roles: [professor]                              │
│    ↓                                                │
│ 8. ENVIA: POST /ADMIN/USUSARIOS                     │
│    ↓                                                │
│ 9. MIDDLEWARE AUDIT (REGISTRA AÇÃO)               │
│    - Log: "Início da criação"                        │
│    ↓                                                │
│ 10. MIDDLEWARE THROTTLE (60/MIN)                   │
│    - Tolerou requisição? SIM                           │
│    ↓                                                │
│ 11. USERCONTROLLER@STORE()                          │
│    - Recebe Request                                  │
│    ↓                                                │
│ 12. VALIDAÇÃO (REQUEST->VALIDATE)                   │
│    - Nome válido? ✓                                   │
│    - Email válido e único? ✓                          │
│    - Senha forte? ✓                                  │
│    - Roles existem? ✓                                 │
│    ↓                                                │
│ 13. MODEL USER::CREATE([...])                       │
│    - MySQL: INSERT INTO users (...)                     │
│    - Retorna: User object (id=10)                     │
│    ↓                                                │
│ 14. USER->ASSIGNROLE(['PROFESSOR'])                │
│    - MySQL: INSERT INTO model_has_roles (...)           │
│    ↓                                                │
│ 15. MIDDLEWARE AUDIT (REGISTRA AÇÃO - FIM)        │
│    - Log: "Usuário criado com sucesso"                 │
│    ↓                                                │
│ 16. REDIRECT: /ADMIN/USUSARIOS                        │
│    ↓                                                │
│ 17. ROUTE: /ADMIN/USUSARIOS (GET)                   │
│    ↓                                                │
│ 18. USERCONTROLLER@INDEX()                           │
│    - User::with('roles')->paginate(10)                 │
│    - MySQL: SELECT * FROM users WHERE id IN (...)        │
│    - Retorna: Collection de 10 usuários               │
│    ↓                                                │
│ 19. VIEW ADMIN/USUSARIOS/INDEX.BLADE.PHP            │
│    - Gera HTML com tabela                             │
│    - Mostra novo usuário "João"                      │
│    ↓                                                │
│ 20. ENVIA RESPOSTA PARA NAVEGADOR                   │
│    ↓                                                │
│ 21. ADMIN VÊ LISTA COM NOVO USUÁRIO                │
└─────────────────────────────────────────────────────────┘
```

---

## 🔄 Fluxo de Atualização de Cache

### Quando Permissões Mudam

```
1. Admin edita role "Professor"
   ↓
2. Adiciona permissão "notas.create"
   ↓
3. PUT /admin/roles/{role}
   ↓
4. RoleController@update()
   ↓
5. $role->syncPermissions([...])
   ↓
6. Spatie Permission:
   - Atualiza banco (role_has_permissions)
   - Limpa cache de permissões
   ↓
7. Próxima requisição do professor
   ↓
8. Verifica permissão: $user->hasPermissionTo('notas.create')
   ↓
9. Spatie verifica banco (cache limpo)
   ↓
10. Permite acesso à nova permissão
```

---

## 📝 Resumo dos Fluxos Principais

| Fluxo | Entrada | Saída | Arquivos Principais |
|-------|---------|--------|---------------------|
| **Login** | Email/Senha | Sessão criada | AuthController, User Model |
| **Logout** | Botão "Sair" | Sessão destruída | AuthController |
| **Dashboard** | Usuário logado | Dados do usuário | DashboardController |
| **Listar Usuários** | Acesso à rota | Lista HTML | UserController |
| **Criar Usuário** | Formulário | Usuário criado | UserController, User Model |
| **Editar Usuário** | Acesso à rota | Usuário atualizado | UserController |
| **Excluir Usuário** | Botão "Excluir" | Usuário removido | UserController, User Model |
| **Gestão de Roles** | Acesso admin | CRUD de roles | RoleController |
| **Auditoria** | Qualquer ação | Log gerado | LogAuditableActions |

---

## 🎯 Ponto-chave do Sistema

Tudo funciona através de **3 conceitos fundamentais**:

1. **Routes** (Rotas): Definem endereços
2. **Middleware** (Guardas): Verificam permissões
3. **Controllers** (Controladores): Processam a lógica
4. **Models** (Modelos): Manipulam dados

```
Requisição HTTP
      ↓
Route (Endereço)
      ↓
Middleware (Verificação de permissão)
      ↓
Controller (Processamento)
      ↓
Model (Banco de dados)
      ↓
View (Resposta visual)
      ↓
Resposta HTTP
```

---

## 📝 Próximos Passos

Para entender melhor:
1. Leia [Guia para Iniciantes](./04-para-iniciantes.md)
2. Consulte [Guia de Permissões](./05-guia-de-permissoes.md)
3. Explore os arquivos do código
