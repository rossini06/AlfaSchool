# 04. Guia de Permissões e Roles

## 📋 O que é este documento?

Este documento explica **TUDO** sobre como funcionam roles e permissões no AlfaSchool, de forma simples e detalhada.

---

## 🎯 Conceitos Fundamentais

### O que é um Role (Papel/Função)?

**Explicação simples:**
Role é como um "cargo" ou "função" que um usuário tem na organização.

**Analogia:**
Pense em uma escola:
- **Diretor** = Role "admin"
- **Professor** = Role "professor"
- **Secretária** = Role "secretaria"
- **Aluno** = Role "aluno"

**No AlfaSchool:**
```
roles (5 tipos):
├── admin      (Administrador - acesso total)
├── gestor     (Gestor escolar)
├── professor   (Professor)
├── secretaria  (Secretaria)
└── aluno      (Aluno)
```

---

### O que é uma Permission (Permissão)?

**Explicação simples:**
Permission é uma **ação específica** que um usuário pode ou não pode fazer.

**Analogia:**
Pense em chaves:
- Chave do escritório = Pode entrar no escritório
- Chave da sala de aula = Pode entrar na sala de aula

**No AlfaSchool:**
```
permissions (22 tipos):
├── users.view        (Pode ver lista de usuários)
├── users.create      (Pode criar novos usuários)
├── users.edit        (Pode editar usuários)
├── users.delete      (Pode excluir usuários)
├── roles.view        (Pode ver lista de roles)
├── roles.edit        (Pode editar permissões de roles)
├── alunos.view       (Pode ver alunos)
├── alunos.create     (Pode cadastrar alunos)
├── alunos.edit       (Pode editar alunos)
├── alunos.delete     (Pode excluir alunos)
├── turmas.view       (Pode ver turmas)
├── turmas.create     (Pode criar turmas)
├── turmas.edit       (Pode editar turmas)
├── turmas.delete     (Pode excluir turmas)
├── notas.view        (Pode ver notas)
├── notas.create      (Pode lançar notas)
├── notas.edit        (Pode editar notas)
├── frequencia.view    (Pode ver frequência)
├── frequencia.create (Pode registrar frequência)
├── financeiro.view    (Pode ver financeiro)
└── financeiro.edit    (Pode editar financeiro)
```

---

### O que é RBAC?

**RBAC = Role-Based Access Control** (Controle de Acesso Baseado em Roles)

**Explicação simples:**
É um sistema onde você define o que CADA ROLE pode fazer, e depois só atribui roles aos usuários.

**Analogia:**
```
Em vez de definir:
- João pode ver usuários
- Maria pode ver usuários
- Pedro pode ver usuários
... (repetindo para cada usuário)

Você define:
- Role ADMIN pode ver usuários
- Role SECRETARIA pode ver usuários

E depois só faz:
- João = Role ADMIN
- Maria = Role SECRETARIA
```

**Vantagens do RBAC:**
1. ✅ **Fácil de gerenciar:** Altera permissions do role, afeta todos os usuários com aquele role
2. ✅ **Escalável:** Adiciona 100 usuários, só atribui role
3. ✅ **Seguro:** Menos chance de erro (não define permissões individualmente)
4. ✅ **Audito:** Fácil ver quem tem o quê

---

## 🗂️ Roles do AlfaSchool

### 1. Role: ADMIN (Administrador)

**Descrição:** Acesso TOTAL ao sistema.

**Permissões:** TODAS as 22 permissões

**O que PODE fazer:**
- ✅ Ver, criar, editar, excluir usuários
- ✅ Ver e editar perfis (roles) e permissões
- ✅ Ver, criar, editar, excluir alunos
- ✅ Ver, criar, editar, excluir turmas
- ✅ Ver, criar, editar notas
- ✅ Ver e registrar frequência
- ✅ Ver e editar financeiro
- ✅ Ver seus dados

**O que NÃO PODE fazer:**
- Nada! O admin pode fazer TUDO.

**No código:**
```php
// No seeder
$adminRole = Role::findByName('admin');
$adminRole->givePermissionTo(Permission::all());  // TODAS as permissões

// No middleware
Gate::before(function ($user, $ability) {
    return $user->hasRole('admin') ? true : null;
});
```
Isso significa que o admin **pula todas as verificações** de permissão!

---

### 2. Role: GESTOR (Gestor Escolar)

**Descrição:** Gestão pedagógica e administrativa, mas não total.

**Permissões (10):**
- alunos.view, alunos.create, alunos.edit
- turmas.view, turmas.create, turmas.edit
- notas.view
- frequencia.view
- financeiro.view
- meus_dados.view

**O que PODE fazer:**
- ✅ Ver, criar e editar alunos
- ✅ Ver, criar e editar turmas
- ✅ Ver notas (mas não pode criar/editar)
- ✅ Ver frequência (mas não pode registrar)
- ✅ Ver financeiro (mas não pode editar)
- ✅ Ver seus dados

**O que NÃO PODE fazer:**
- ❌ Excluir alunos ou turmas
- ❌ Criar ou editar notas (apenas ver)
- ❌ Registrar frequência (apenas ver)
- ❌ Editar financeiro (apenas ver)
- ❌ Gerenciar usuários
- ❌ Gerenciar roles

**No código:**
```php
$gestorRole = Role::findByName('gestor');
$gestorRole->givePermissionTo([
    'alunos.view', 'alunos.create', 'alunos.edit',
    'turmas.view', 'turmas.create', 'turmas.edit',
    'notas.view',
    'frequencia.view',
    'financeiro.view',
    'meus_dados.view',
]);
```

---

### 3. Role: PROFESSOR

**Descrição:** Professor - Lançamento de notas e frequência.

**Permissões (8):**
- alunos.view
- turmas.view
- notas.view, notas.create, notas.edit
- frequencia.view, frequencia.create
- meus_dados.view

**O que PODE fazer:**
- ✅ Ver alunos (apenas ver)
- ✅ Ver turmas (apenas ver)
- ✅ Ver, criar e editar notas
- ✅ Ver e registrar frequência
- ✅ Ver seus dados

**O que NÃO PODE fazer:**
- ❌ Criar ou editar alunos
- ❌ Criar ou editar turmas
- ❌ Ver ou editar financeiro
- ❌ Gerenciar usuários
- ❌ Gerenciar roles

**No código:**
```php
$professorRole = Role::findByName('professor');
$professorRole->givePermissionTo([
    'alunos.view',
    'turmas.view',
    'notas.view', 'notas.create', 'notas.edit',
    'frequencia.view', 'frequencia.create',
    'meus_dados.view',
]);
```

---

### 4. Role: SECRETARIA

**Descrição:** Cadastros e matrículas.

**Permissões (5):**
- alunos.view, alunos.create, alunos.edit
- turmas.view
- meus_dados.view

**O que PODE fazer:**
- ✅ Ver, criar e editar alunos
- ✅ Ver turmas (apenas ver)
- ✅ Ver seus dados

**O que NÃO PODE fazer:**
- ❌ Excluir alunos ou turmas
- ❌ Criar ou editar turmas
- ❌ Criar ou editar notas
- ❌ Ver ou registrar frequência
- ❌ Ver ou editar financeiro
- ❌ Gerenciar usuários
- ❌ Gerenciar roles

**No código:**
```php
$secretariaRole = Role::findByName('secretaria');
$secretariaRole->givePermissionTo([
    'alunos.view', 'alunos.create', 'alunos.edit',
    'turmas.view',
    'meus_dados.view',
]);
```

---

### 5. Role: ALUNO

**Descrição:** Visualização de dados pessoais e acadêmicos.

**Permissões (1):**
- meus_dados.view

**O que PODE fazer:**
- ✅ Ver seus próprios dados

**O que NÃO PODE fazer:**
- ❌ Ver outros alunos
- ❌ Ver turmas
- ❌ Ver notas
- ❌ Qualquer coisa de gestão

**No código:**
```php
$alunoRole = Role::findByName('aluno');
$alunoRole->givePermissionTo(['meus_dados.view']);
```

---

## 🔐 Como Funciona a Verificação

### Verificação de Role

**No código (Middleware):**
```php
Route::middleware(['role:admin'])->group(...)
```

**O que acontece:**
```php
// 1. Middleware intercepta requisição
$user = auth()->user();

// 2. Verifica se usuário tem role "admin"
if ($user->hasRole('admin')) {
    return $next($request);  // Permite acesso
} else {
    abort(403);  // Bloqueia (Forbidden)
}
```

**No código (Controller):**
```php
public function index()
{
    if (auth()->user()->hasRole('admin')) {
        return view('admin.dashboard');
    } else {
        return view('student.dashboard');
    }
}
```

**No código (Blade):**
```blade
@role('admin')
    <a href="/admin">Painel Admin</a>
@endrole

@role('professor')
    <a href="/professor">Painel Professor</a>
@endrole
```

---

### Verificação de Permission

**No código (Middleware):**
```php
Route::middleware(['permission:users.view'])->get('/admin/users', ...)
```

**O que acontece:**
```php
// 1. Middleware intercepta requisição
$user = auth()->user();

// 2. Verifica se usuário tem permissão "users.view"
if ($user->hasPermissionTo('users.view')) {
    return $next($request);  // Permite acesso
} else {
    abort(403);  // Bloqueia (Forbidden)
}
```

**No código (Controller):**
```php
public function destroy(User $user)
{
    if (auth()->user()->hasPermissionTo('users.delete')) {
        $user->delete();
        return redirect()->route('admin.users.index');
    } else {
        abort(403, 'Você não tem permissão');
    }
}
```

**No código (Blade):**
```blade
@can('users.create')
    <button>Novo Usuário</button>
@endcan

@can('users.delete', $user)
    <button>Excluir</button>
@endcan
```

---

### Gate: Super Admin

**No código (AppServiceProvider):**
```php
Gate::before(function ($user, $ability) {
    return $user->hasRole('admin') ? true : null;
});
```

**O que isso faz?**
- Admin RETORNA TRUE para TUDO
- PULA todas as verificações de permissão
- É como um "acesso VIP"

**Exemplo:**
```php
// Admin acessando /admin/users
// Gate::before retorna TRUE
// Nem verifica hasPermissionTo()
// Pula direto para o controller

// Professor acessando /admin/users
// Gate::before retorna NULL (continua)
// Verifica hasPermissionTo('users.view')
// Se FALSE → Bloqueia (403)
```

---

## 🔄 Como Adicionar Novo Role

### Passo 1: Adicionar ao Seeder

**Arquivo:** `database/seeders/RolePermissionSeeder.php`

```php
$roles = [
    'admin' => 'Administrador do sistema - acesso total',
    'gestor' => 'Gestor escolar - gestão pedagógica e administrativa',
    'professor' => 'Professor - lançamento de notas e frequência',
    'secretaria' => 'Secretaria - cadastros e matrículas',
    'aluno' => 'Aluno - visualização de dados pessoais e acadêmicos',
    'financeiro' => 'Financeiro - gestão financeira completa',  // NOVO!
];

foreach ($roles as $name => $description) {
    Role::firstOrCreate(['name' => $name]);
}
```

---

### Passo 2: Atribuir Permissões ao Novo Role

```php
$financeiroRole = Role::findByName('financeiro');

$financeiroRole->givePermissionTo([
    'financeiro.view',      // Pode ver financeiro
    'financeiro.edit',     // Pode editar financeiro
    'turmas.view',         // Pode ver turmas (para ver alunos)
    'alunos.view',         // Pode ver alunos (para ver financeiro)
    'meus_dados.view',     // Pode ver seus dados
]);
```

---

### Passo 3: Re-executar Seeder

```bash
php artisan db:seed --class=RolePermissionSeeder
```

**O que isso faz:**
1. Cria o novo role "financeiro"
2. Atribui as permissões definidas
3. Não altera os outros roles

---

### Passo 4: Atribuir Role a Usuário

**Via Interface Admin:**
1. Acesse `/admin/users`
2. Clique em "Editar" no usuário
3. Marque o checkbox "Financeiro"
4. Salvar

**Via Código:**
```php
$user = User::find(1);
$user->assignRole('financeiro');
```

**Via Tinker:**
```bash
php artisan tinker

$user = \App\Models\User::find(1);
$user->assignRole('financeiro');
```

---

### Passo 5: Proteger Rotas com Novo Role

```php
Route::middleware(['auth', 'role:financeiro'])->prefix('financeiro')->group(function () {
    Route::get('/relatorios', [FinanceiroController::class, 'relatorios']);
    Route::get('/contas', [FinanceiroController::class, 'contas']);
    Route::post('/lancar', [FinanceiroController::class, 'lancar']);
});
```

**O que isso faz:**
- Só usuários com role "financeiro" podem acessar essas rotas
- Se não tiver o role, retorna erro 403

---

## 📊 Tabela de Permissões por Role

| Permissão | Admin | Gestor | Professor | Secretaria | Aluno | Financeiro (exemplo) |
|------------|-------|--------|-----------|------------|--------|---------------------|
| **Usuários** |
| users.view | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| users.create | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| users.edit | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| users.delete | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| **Roles** |
| roles.view | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| roles.edit | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| **Alunos** |
| alunos.view | ✅ | ✅ | ✅ | ✅ | ❌ | ✅ |
| alunos.create | ✅ | ✅ | ❌ | ✅ | ❌ | ❌ |
| alunos.edit | ✅ | ✅ | ❌ | ✅ | ❌ | ❌ |
| alunos.delete | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| **Turmas** |
| turmas.view | ✅ | ✅ | ✅ | ✅ | ❌ | ✅ |
| turmas.create | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ |
| turmas.edit | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ |
| turmas.delete | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ |
| **Notas** |
| notas.view | ✅ | ✅ | ✅ | ❌ | ❌ | ❌ |
| notas.create | ✅ | ❌ | ✅ | ❌ | ❌ | ❌ |
| notas.edit | ✅ | ❌ | ✅ | ❌ | ❌ | ❌ |
| **Frequência** |
| frequencia.view | ✅ | ✅ | ✅ | ❌ | ❌ | ❌ |
| frequencia.create | ✅ | ❌ | ✅ | ❌ | ❌ | ❌ |
| **Financeiro** |
| financeiro.view | ✅ | ✅ | ❌ | ❌ | ❌ | ✅ |
| financeiro.edit | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ |
| **Meus Dados** |
| meus_dados.view | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |

**Legenda:**
- ✅ = Tem permissão
- ❌ = NÃO tem permissão

---

## 🔄 Fluxo Completo de Verificação

### Cenário: Professor tentando acessar /admin/users

```
1. Professor clica em /admin/users
   ↓
2. Rota define: middleware(['auth', 'role:admin', 'permission:users.view'])
   ↓
3. Middleware auth: Está logado? SIM
   ↓
4. Middleware role: Tem role admin? NÃO
   ↓
5. Resultado: FALSE
   ↓
6. Abort 403 (Forbidden)
   ↓
7. Usuário vê: "Você não tem permissão"
```

### Cenário: Admin acessando /admin/users

```
1. Admin clica em /admin/users
   ↓
2. Rota define: middleware(['auth', 'role:admin', 'permission:users.view'])
   ↓
3. Middleware auth: Está logado? SIM
   ↓
4. Gate::before: Tem role admin? SIM
   ↓
5. Resultado: TRUE
   ↓
6. Pula outras verificações!
   ↓
7. Controller é executado
   ↓
8. Usuário vê lista de usuários
```

---

## 🧩 Múltiplos Roles

### Como Atribuir Múltiplos Roles

**Via Interface Admin:**
1. Edite usuário
2. Marque múltiplos checkboxes (ex: Professor E Coordenador)
3. Salvar

**Via Código:**
```php
$user = User::find(1);
$user->assignRole(['professor', 'coordenador']);
```

**Como funciona:**
```
Usuário João:
├── Role: professor
│   ├── Pode ver notas
│   ├── Pode criar notas
│   └── Pode registrar frequência
│
└── Role: coordenador
    ├── Pode ver turmas
    ├── Pode criar turmas
    └── Pode editar turmas
```

**Resultado:**
- João tem a SOMA de permissões de AMBOS os roles
- Pode fazer tudo que professor PODE fazer
- E tudo que coordenador PODE fazer

---

## 📋 Validações e Erros

### Erro 403 (Forbidden)

**Quando acontece:**
- Usuário não tem permissão/role

**O que o usuário vê:**
- Página branca ou página de erro

**Como evitar:**
- Usar Blade directives para esconder links:
```blade
@can('users.create')
    <a href="/admin/users/create">Novo Usuário</a>
@endcan
```

---

### Erro de Validação de Senha

**Quando acontece:**
- Usuário cria senha fraca

**Mensagem:**
```
A senha deve conter pelo menos:
- 8 caracteres
- 1 letra maiúscula
- 1 letra minúscula
- 1 número
- 1 caractere especial (@$!%*#?&)
```

---

## 🚀 Best Practices

### 1. Princípio do Menor Privilégio

**O que é:**
Dar ao usuário o MÍNIMO de privilégios necessário para fazer seu trabalho.

**Exemplo CORRETO:**
```
Professor precisa de:
- Ver alunos (para ver notas)
- Criar notas
- Editar notas

NÃO precisa de:
- Criar alunos
- Excluir alunos
- Gerenciar financeiro
```

**Por que?**
- Se alguém roubar a conta de um professor, o dano é limitado
- Se o professor tivesse permissão de admin, o dano seria catastrófico

---

### 2. Princípio da Separação de Responsabilidades

**O que é:**
Cada parte do sistema deve ter sua própria lógica de permissões.

**Exemplo:**
```
Módulo Financeiro:
- Role financeiro gerencia financeiro
- NÃO depende de outras partes

Módulo Pedagógico:
- Role professor lança notas
- NÃO depende de outras partes
```

**Por que?**
- Fácil de manter
- Menos bugs
- Módulos podem ser desativados sem quebrar outros

---

### 3. Princípio da Auditoria

**O que é:**
Registrar TODAS as ações relacionadas a permissões.

**O que registrar:**
- Login/Logout
- Criação/Edição/Exclusão de usuários
- Mudanças de permissões de roles

**Por que?**
- Rastrear problemas: "Quem excluiu o usuário X?"
- Compliance: "Quem teve acesso a dados sensíveis?"
- Segurança: Detectar atividades suspeitas

---

## 💡 Exemplos Práticos

### Exemplo 1: Criar Usuário

**Quem pode:** Usuários com permissão `users.create`

**Como verificar:**
```php
// No controller
$this->authorize('users.create');

// Ou no middleware
Route::middleware(['permission:users.create'])->post(...)
```

---

### Exemplo 2: Ver Lista de Usuários

**Quem pode:** Usuários com permissão `users.view`

**Como verificar:**
```php
// No controller
$this->authorize('users.view');

// Ou no middleware
Route::middleware(['permission:users.view'])->get(...)
```

---

### Exemplo 3: Excluir Usuário

**Quem pode:** Usuários com permissão `users.delete`

**Como verificar:**
```php
// No controller
$this->authorize('users.delete');

// Ou no middleware
Route::middleware(['permission:users.delete'])->delete(...)
```

---

### Exemplo 4: Editar Permissões de Role

**Quem pode:** Usuários com permissão `roles.edit`

**Como verificar:**
```php
// No controller
$this->authorize('roles.edit');

// Ou no middleware
Route::middleware(['permission:roles.edit'])->put(...)
```

---

## 📊 Hierarquia de Acesso

```
Nível 1: ADMIN (Acesso Total)
├── 22 permissões
└── Acesso irrestrito

Nível 2: GESTOR (Gestão)
├── 10 permissões
└── Acesso restrito

Nível 3: PROFESSOR (Pedagógico)
├── 8 permissões
└── Acesso pedagógico

Nível 4: FINANCEIRO (Financeiro - exemplo futuro)
├── 5 permissões
└── Acesso financeiro

Nível 5: SECRETARIA (Cadastros)
├── 5 permissões
└── Acesso administrativo básico

Nível 6: ALUNO (Visualização)
├── 1 permissão
└── Acesso mínimo
```

---

## 📝 Resumo

| Conceito | Descrição |
|----------|------------|
| **Role** | Função do usuário (admin, professor, etc.) |
| **Permission** | Ação específica (users.view, etc.) |
| **RBAC** | Controle de acesso baseado em roles |
| **Gate** | Verificação centralizada de permissões |
| **Middleware** | Guardas que protegem rotas |
| **Spatie** | Pacote que gerencia roles/permissions |
| **Seeder** | Arquivo que cria roles/permissions iniciais |
| **Audit** | Registro de ações sensíveis |

---

## 🚀 Próximos Passos

1. Entenda o sistema atual
2. Identifique novos roles necessários
3. Adicione ao `RolePermissionSeeder.php`
4. Atribua permissões apropriadas
5. Execute o seeder
6. Teste com usuários diferentes
7. Documente as mudanças

---

## 📞 Suporte

Se tiver dúvidas sobre permissões:
1. Consulte a documentação do Spatie Permission
2. Veja os exemplos no código
3. Teste no ambiente de desenvolvimento
4. Abra uma issue no GitHub

---

**Última atualização:** 04/02/2026
**Versão do sistema:** 1.0.0
