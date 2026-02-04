# 04. Guia Para Iniciantes

## 🎯 O que é este documento?

Este documento explica **TUDO** que foi feito no sistema AlfaSchool de forma simples e detalhada, para quem está começando a programar ou em Laravel.

**Público-alvo:**
- Estudantes aprendendo Laravel
- Desenvolvedores juniores
- Pessoas que querem entender o projeto

---

## 📚 Conceitos Fundamentais

### O que é um Framework?

**Explicação simples:**
Framework é um **kit de ferramentas pronto**. É como comprar um quebra-cabeça com todas as peças já separadas, ao invés de construir cada peça do zero.

**Analogia:**
- **Sem framework:** Construir uma casa do zero (madeira, tijolos, cimento)
- **Com framework:** Montar uma casa pré-fabricada (painéis, portas, janelas já prontos)

**No Laravel:**
- Autenticação (login) já vem pronta
- Validação de formulários já vem pronta
- Proteção CSRF já vem pronta
- SÓ configurar!

**Por que usar framework?**
1. **Economiza tempo:** O que levaria 1 mês para fazer, você faz em 1 dia
2. **Mais seguro:** Milhares de pessoas testaram, bugs já foram corrigidos
3. **Comunidade:** Se tiver problema, alguém já resolveu
4. **Padrões:** Segue boas práticas da indústria

---

### O que é MVC?

**Explicação simples:**
MVC é uma **forma de organizar o código** para não ficar uma bagunça.

**Analogia do Restaurante:**

| Componente | No Restaurante | No Sistema |
|------------|----------------|-------------|
| **Model** | Ingredientes e receitas | Dados do banco (usuários, alunos) |
| **View** | Prato servido ao cliente | Página HTML que o usuário vê |
| **Controller** | Cozinheiro (recebe pedido, prepara, entrega) | Lógica (busca dados, envia para view) |

**Exemplo prático:**

```
Cliente pede: "Quero a lista de usuários"
   ↓
Controller (Cozinheiro) recebe pedido
   ↓
Model (Ingredientes) busca usuários no banco
   ↓
View (Prato) gera tabela HTML com usuários
   ↓
Cliente vê lista de usuários
```

**Por que MVC?**
1. **Organização:** Separa responsabilidade de cada parte
2. **Reutilização:** Pode usar a mesma lógica em vários lugares
3. **Manutenção:** Se mudar o visual, só altera a View

---

### O que é um Banco de Dados?

**Explicação simples:**
Banco de dados é como uma **planilha gigante super organizada** que o sistema usa para guardar informações.

**Analogia:**
- **Arquivo de texto:** Guardar nome e email de 100 alunos em um .txt
- **Banco de dados:** Guardar em tabelas com relacionamentos

**No MySQL:**

| Tabela | Colunas | Exemplo |
|---------|----------|---------|
| `users` | id, name, email, password | 1, João, joao@email.com, (hash da senha) |
| `roles` | id, name | 1, admin |
| `permissions` | id, name | 1, users.view |

**Por que banco de dados?**
1. **Velozidade:** Encontrar dados em milissegundos
2. **Relacionamentos:** Joana está na turma X, que tem o professor Y
3. **Segurança:** Proteção contra perda de dados

---

### O que é Hash de Senha?

**Explicação simples:**
Hash é uma **forma de criptografar senhas** que NÃO pode ser desfeita.

**Analogia:**
- **Senha normal:** "Senha123" (pode ser lida por qualquer um)
- **Senha hasheada:** "$2y$10$..." (impossível saber a senha original)

**Como funciona:**

```
Usuário cria senha: "Senha123"
   ↓
Laravel hashea: bcrypt("Senha123")
   ↓
Resultado: "$2y$10$abc123xyz..."
   ↓
Salva no banco (só o hash)
   ↓
Usuário faz login com: "Senha123"
   ↓
Laravel hashea novamente: bcrypt("Senha123")
   ↓
Compara os hashes: Iguais?
   ↓
SIM → Login autorizado
```

**Por que usar hash?**
1. **Segurança:** Se alguém roupar o banco, NÃO vê as senhas
2. **Impossível reverter:** Não existe forma de "desfazer" o hash
3. **Padrão:** Todos os sistemas usam

**No AlfaSchool:**
```php
// Quando criar usuário
User::create([
    'password' => bcrypt($senhaDoUsuario)
]);
```

---

### O que é uma Sessão?

**Explicação simples:**
Sessão é como a **memória do servidor** sobre quem está logado.

**Analogia:**
- **Sessão:** Ticket de ingresso que você recebe ao entrar
- Enquanto você tem o ticket, pode entrar no show
- Quando sai, o ticket é invalidado

**Como funciona:**

```
1. Usuário faz login
   ↓
2. Servidor gera ID aleatório: "abc123xyz"
   ↓
3. Salva no banco: "abc123xyz" pertence ao usuário 1
   ↓
4. Envia cookie para navegador: "SESSID=abc123xyz"
   ↓
5. Navegador salva cookie
   ↓
6. Próxima requisição: navegador envia cookie
   ↓
7. Servidor lê cookie, busca sessão
   ↓
8. Sessão diz: "abc123xyz pertence ao usuário 1"
   ↓
9. Usuário está logado!
```

**Por que usar sessão?**
1. **Não precisa logar toda hora:** Cookie lembra
2. **Servidor controla:** Pode invalidar a qualquer momento
3. **Seguro:** Cookie tem informações criptografadas

**No AlfaSchool:**
```env
SESSION_LIFETIME=15  # 15 minutos de inatividade
SESSION_ENCRYPT=true  # Criptografa a sessão
```

---

### O que é CSRF Protection?

**Explicação simples:**
CSRF é uma **proteção contra formulários falsos**.

**Analogia:**
- **Sem CSRF:** Alguém pode enviar um formulário em seu nome, sem você saber
- **Com CSRF:** Cada formulário tem um "selo" que só o servidor pode gerar

**Como funciona:**

```
1. Laravel gera token CSRF aleatório
   ↓
2. Adiciona em todos os formulários:
<form>
   <input type="hidden" name="_token" value="abc123xyz">
   ...
</form>
   ↓
3. Usuário envia formulário
   ↓
4. Laravel verifica:
   - Token enviado é o mesmo que o esperado?
   ↓
5. Se SIM → Processa formulário
   Se NÃO → Rejeita (possível ataque)
```

**Por que CSRF?**
1. **Segurança:** Evita que hackers enviem formulários em seu nome
2. **Automático:** Laravel adiciona automaticamente
3. **Transparente:** Usuário nem vê

**No AlfaSchool:**
```blade
<form method="POST" action="{{ route('login') }}">
    @csrf  <!-- Isso adiciona o token CSRF -->
    <input type="email" name="email">
    <input type="password" name="password">
</form>
```

---

### O que é Rate Limiting?

**Explicação simples:**
Rate Limiting é **limitar tentativas** para evitar abusos.

**Analogia:**
- **Sem rate limiting:** Alguém pode tentar senha 100 vezes por segundo
- **Com rate limiting:** Depois de 5 tentativas, espera 1 minuto

**Como funciona:**

```
1. Usuário tenta login
   ↓
2. Middleware verifica: Quantas tentativas nos últimos 60s?
   ↓
3. Se < 5 → Permite
   Se ≥ 5 → Bloqueia (erro 429)
   ↓
4. Exibe: "Muitas tentativas. Tente em 60s."
```

**Por que rate limiting?**
1. **Evita brute force:** Tenta todas as senhas possíveis
2. **Protege servidor:** Não trava com muitos pedidos
3. **Segurança:** Bloqueia ataques de DDoS

**No AlfaSchool:**
```php
Route::post('login', ...)->middleware('throttle:5,1')
//                       5        1
//                   tentativas  minuto
```

---

## 🚀 Como o AlfaSchool foi Feito (Passo a Passo)

### PASSO 1: Criar o Projeto Laravel

**Comando:**
```bash
composer create-project laravel/laravel alfaschool
```

**O que isso faz?**
1. Baixa o Laravel (framework)
2. Cria toda a estrutura de pastas
3. Instala dependências
4. Gera chave de segurança (APP_KEY)

**Arquivos criados:**
- `app/` (aplicação)
- `routes/` (rotas)
- `resources/` (views)
- `config/` (configurações)
- `database/` (banco)
- E muito mais!

**Por que Composer?**
Composer é como um "instalador automático" de pacotes PHP.

---

### PASSO 2: Configurar Banco de Dados

**Arquivo:** `.env`

```env
DB_CONNECTION=mysql
DB_HOST=127.0.0.1
DB_PORT=3306
DB_DATABASE=alfaschool
DB_USERNAME=root
DB_PASSWORD=
```

**O que é .env?**
- Arquivo de **configurações**
- Contém senhas, chaves, etc.
- NÃO é commitado no Git (segurança)

**Por que .env?**
1. **Segurança:** Senhas não ficam no código
2. **Ambientes:** Diferentes configs para desenvolvimento e produção
3. **Flexibilidade:** Fácil mudar sem alterar código

---

### PASSO 3: Instalar Laravel Breeze

**Comandos:**
```bash
composer require laravel/breeze
php artisan breeze:install blade
```

**O que é Breeze?**
Pacote que já traz:
- Login pronto
- Registro pronto
- Reset de senha pronto
- Formulários bonitos
- Validações

**O que o comando breeze:install faz?**
1. Instala dependências
2. Cria controllers de autenticação
3. Cria views de login, registro
4. Cria rotas de autenticação
5. Instala Bootstrap/Tailwind

**Arquivos criados:**
- `app/Http/Controllers/Auth/` (AuthenticatedSessionController, etc.)
- `resources/views/auth/` (login, register, forgot-password)
- `routes/auth.php`
- Views de componentes

**Por que usar Breeze em vez de fazer do zero?**
- **Tempo:** Leva 2 minutos vs 2 semanas
- **Segurança:** Já vem testado e protegido
- **Padrão:** Segue as melhores práticas do Laravel

---

### PASSO 4: Instalar Spatie Permission

**Comandos:**
```bash
composer require spatie/laravel-permission
php artisan vendor:publish --provider="Spatie\Permission\PermissionServiceProvider"
php artisan migrate
```

**O que é Spatie Permission?**
Biblioteca que gerencia:
- Roles (perfis de usuário)
- Permissions (o que cada um pode fazer)
- Relacionamento entre usuários, roles e permissions

**O que vendor:publish faz?**
Copia arquivos de configuração do pacote para o projeto:
- `config/permission.php`

**O que o migrate faz?**
Cria tabelas no banco:
- `roles` (perfis)
- `permissions` (permissões)
- `model_has_roles` (usuário tem roles)
- `model_has_permissions` (usuário tem permissions)

**Por que Spatie?**
- **Simples:** `$user->hasRole('admin')` - fácil de usar
- **Rápido:** Usa cache para não consultar o banco toda hora
- **Flexível:** Roles e permissions dinâmicas
- **Popular:** Usado por grandes empresas

---

### PASSO 5: Modificar o Model User

**Arquivo:** `app/Models/User.php`

**Antes:**
```php
class User extends Authenticatable
{
    use HasFactory, Notifiable;
}
```

**Depois:**
```php
use Spatie\Permission\Traits\HasRoles;

class User extends Authenticatable
{
    use HasFactory, Notifiable, HasRoles;  // ADICIONADO!
}
```

**O que isso faz?**
Adiciona métodos ao User:
- `$user->hasRole('admin')` - verifica se tem role
- `$user->hasPermissionTo('users.view')` - verifica se tem permissão
- `$user->getAllPermissions()` - pega todas as permissões
- `$user->assignRole('professor')` - atribui role

**Por que trait?**
Trait é como "adicionar superpoderes" a uma classe.

---

### PASSO 6: Criar Seeder de Roles e Permissions

**Arquivo:** `database/seeders/RolePermissionSeeder.php`

**O que é Seeder?**
Arquivo que **preenche o banco** com dados iniciais.

**O que este seeder faz:**

1. **Limpa dados antigos:**
```php
Role::truncate();
Permission::truncate();
```

2. **Cria 5 roles:**
```php
$roles = [
    'admin' => 'Administrador',
    'gestor' => 'Gestor',
    'professor' => 'Professor',
    'secretaria' => 'Secretaria',
    'aluno' => 'Aluno',
];

foreach ($roles as $name => $description) {
    Role::create(['name' => $name]);
}
```

3. **Cria 22 permissões:**
```php
$permissions = [
    'users.view', 'users.create', 'users.edit', 'users.delete',
    'roles.view', 'roles.edit',
    'alunos.view', 'alunos.create', 'alunos.edit', 'alunos.delete',
    // ... e mais 14 permissões
];

foreach ($permissions as $name => $description) {
    Permission::create(['name' => $name]);
}
```

4. **Atribui permissões aos roles:**
```php
// Admin recebe TODAS as permissões
$adminRole = Role::findByName('admin');
$adminRole->givePermissionTo(Permission::all());

// Professor recebe algumas permissões
$professorRole = Role::findByName('professor');
$professorRole->givePermissionTo([
    'alunos.view',
    'notas.create',
    'frequencia.create',
    // ...
]);
```

**Comando para rodar:**
```bash
php artisan db:seed --class=RolePermissionSeeder
```

**Por que Seeder?**
- **Reprodução:** Fácil configurar produção
- **Consistência:** Sempre cria as mesmas roles/permissions
- **Rápido:** Um comando e pronto

---

### PASSO 7: Criar Controllers

**Arquivo:** `app/Http/Controllers/DashboardController.php`

**Código:**
```php
class DashboardController extends Controller
{
    public function index()
    {
        $user = auth()->user();           // Pega usuário logado
        $roles = $user->roles->pluck('name');  // Pega roles
        $permissions = $user->getAllPermissions()->pluck('name');  // Pega permissões
        
        return view('dashboard', compact('user', 'roles', 'permissions'));
    }
}
```

**O que isso faz?**
1. Recebe requisição para `/dashboard`
2. Busca usuário logado
3. Busca roles do usuário
4. Busca permissões do usuário
5. Envia tudo para a view

**Arquivo:** `app/Http/Controllers/Admin/UserController.php`

**Código:**
```php
class UserController extends Controller
{
    public function index()
    {
        $users = User::with('roles')->latest()->paginate(10);
        return view('admin.users.index', compact('users'));
    }
    
    public function store(Request $request)
    {
        // Valida
        $validated = $request->validate([
            'name' => 'required|string|max:255',
            'email' => 'required|string|email|max:255|unique:users',
            'password' => 'required|string|min:8|confirmed',
            'roles' => 'nullable|array',
        ]);
        
        // Cria usuário
        $user = User::create([
            'name' => $validated['name'],
            'email' => $validated['email'],
            'password' => bcrypt($validated['password']),
        ]);
        
        // Atribui roles
        if (!empty($validated['roles'])) {
            $user->assignRole($validated['roles']);
        }
        
        // Redireciona
        return redirect()->route('admin.users.index')
            ->with('success', 'Usuário criado com sucesso!');
    }
}
```

**Por que separar index() e store()?**
- **Organização:** Cada método faz uma coisa
- **Clareza:** index() apenas lista, store() apenas cria
- **Reutilização:** Pode chamar store() de outros lugares

---

### PASSO 8: Criar Views

**Arquivo:** `resources/views/dashboard.blade.php`

**Código:**
```blade
<h1>Bem-vindo, {{ $user->name }}!</h1>

<h3>Seus Perfis:</h3>
@foreach($roles as $role)
    <span>{{ ucfirst($role) }}</span>
@endforeach

<h3>Suas Permissões:</h3>
@foreach($permissions as $permission)
    <span>{{ $permission }}</span>
@endforeach
```

**O que é Blade?**
Blade é a linguagem de templates do Laravel. Permite misturar HTML com código PHP de forma simples.

**Diretivas Blade:**
- `{{ $var }}` - Exibe variável (escapa automaticamente - seguro)
- `{@foreach($items as $item)}` - Loop
- `@if($condition)` - Condicional
- `@csrf` - Token CSRF
- `@auth` - Verifica se está logado

**Por que Blade?**
- **Simples:** Sintaxe fácil de aprender
- **Seguro:** Escapa automaticamente (previne XSS)
- **Poderoso:** Componentes, layouts, herança

---

### PASSO 9: Configurar Rotas

**Arquivo:** `routes/web.php`

**Código:**
```php
Route::middleware(['auth', 'verified'])->group(function () {
    Route::get('/dashboard', [DashboardController::class, 'index'])->name('dashboard');
});

Route::middleware(['auth', 'role:admin', 'throttle:60,1'])
    ->prefix('admin')
    ->name('admin.')
    ->group(function () {
    
    Route::middleware(['permission:users.view'])->group(function () {
        Route::get('/users', [UserController::class, 'index'])->name('users.index');
        Route::post('/users', [UserController::class, 'store'])
            ->name('users.store')
            ->middleware(['permission:users.create', 'audit']);
    });
});
```

**O que isso faz?**
1. Cria rotas (endereços) do site
2. Aplica middleware (proteções)
3. Agrupa rotas com prefixo

**Por que agrupar?**
- **Organização:** Todas rotas de admin estão juntas
- **Prefixo:** `/admin/users`, `/admin/roles` (prefixo automático)
- **Middleware:** Aplica a todas as rotas do grupo

---

### PASSO 10: Configurar Middleware

**Arquivo:** `bootstrap/app.php`

**Código:**
```php
->withMiddleware(function (Middleware $middleware): void {
    $middleware->alias([
        'role' => \Spatie\Permission\Middleware\RoleMiddleware::class,
        'permission' => \Spatie\Permission\Middleware\PermissionMiddleware::class,
        'audit' => \App\Http\Middleware\LogAuditableActions::class,
        'security-headers' => \App\Http\Middleware\SecurityHeaders::class,
    ]);
})
```

**O que é Middleware?**
Middleware é como um "guarda" que fica entre a requisição e o controller.

**Como funciona:**
```
1. Requisição chega
   ↓
2. Middleware 1 verifica (está logado?)
   ↓
3. Middleware 2 verifica (tem permissão?)
   ↓
4. Controller é executado
   ↓
5. Resposta é enviada
```

**Por que usar middleware?**
- **Centralizado:** Define regras em um só lugar
- **Reutilizável:** Usa em várias rotas
- **Ordenado:** Define ordem de verificação

---

### PASSO 11: Configurar Segurança

**Arquivo:** `app/Http/Middleware/LogAuditableActions.php`

**Código:**
```php
public function handle(Request $request, Closure $next): Response
{
    $response = $next($request);  // Continua para controller
    
    // Se for uma ação auditável, registra
    $routeName = $request->route()?->getName();
    
    if ($routeName === 'login' || $routeName === 'admin.users.store') {
        Log::channel('audit')->info('Auditable action', [
            'user_id' => auth()->id(),
            'action' => $routeName,
            'ip' => $request->ip(),
            'timestamp' => now(),
        ]);
    }
    
    return $response;
}
```

**Arquivo:** `app/Http/Middleware/SecurityHeaders.php`

**Código:**
```php
public function handle(Request $request, Closure $next): Response
{
    $response = $next($request);  // Continua para controller
    
    // Adiciona headers de segurança
    $response->headers->set('X-Frame-Options', 'SAMEORIGIN');
    $response->headers->set('X-XSS-Protection', '1; mode=block');
    $response->headers->set('Content-Security-Policy', '...');
    
    return $response;
}
```

**Por que usar middleware para isso?**
- **Separado:** Lógica de segurança separada de negócio
- **Automático:** Aplica a todas as rotas
- **Centralizado:** Muda em um só lugar

---

## 💡 Resumo do Que Foi Feito

| Etapa | O que foi feito | Por que |
|-------|----------------|---------|
| 1 | Criar projeto Laravel | Base do sistema |
| 2 | Configurar banco de dados | Onde guardar informações |
| 3 | Instalar Breeze | Autenticação pronta |
| 4 | Instalar Spatie Permission | Controle de permissões |
| 5 | Modificar User Model | Usuário pode ter roles |
| 6 | Criar seeder | Criar roles e permissions iniciais |
| 7 | Criar controllers | Lógica do sistema |
| 8 | Criar views | Visual que o usuário vê |
| 9 | Criar rotas | Endereços do site |
| 10 | Criar middleware | Guardas de segurança |
| 11 | Adicionar segurança extras | Headers, logs, rate limiting |

---

## 🎯 Por Que Foi Feito Assim?

### Por que Laravel e não PHP puro?
**Resposta:** Laravel já tem 90% do que precisamos pronto.

**Sem Laravel:**
- Login do zero: 2 semanas
- Validações do zero: 1 semana
- CSRF protection do zero: 3 dias
- **Total: ~1 mês**

**Com Laravel:**
- Tudo pronto: 2 dias
- **Economia: ~95% do tempo**

### Por que Spatie Permission e não criar manualmente?
**Resposta:** Seria MUITO complexo criar RBAC do zero.

**Manual:**
- Criar tabelas de roles, permissions, junctions
- Criar métodos hasRole(), hasPermission()
- Criar lógica de cache
- Criar testes
- **Total: ~1 mês**

**Com Spatie:**
- Tudo pronto: 1 hora
- **Economia: ~99% do tempo**

### Por que usar as boas práticas?
**Resposta:** Código limpo é mais fácil de manter.

**Código ruim:**
```php
// Arquivo gigante de 2000 linhas
// Tudo misturado: HTML, PHP, SQL
// Impossível de entender
```

**Código bom:**
```php
// Separado em arquivos pequenos
// Cada arquivo faz uma coisa
// Fácil de ler e entender
```

---

## 📚 Recursos Para Aprender Mais

### Para Aprender Laravel
- [Documentação Oficial](https://laravel.com/docs) (melhor recurso!)
- [Laracasts](https://laracasts.com) (vídeo-tutoriais)
- [Laravel Daily](https://laraveldaily.com) (dicas diárias)

### Para Aprender PHP
- [PHP The Right Way](https://phptherightway.com/) (livro gratuito)
- [PHP Manual](https://www.php.net/manual/pt_BR/) (documentação oficial)

### Para Aprender Banco de Dados
- [MySQL Tutorial](https://www.mysqltutorial.com/)
- [W3Schools SQL](https://www.w3schools.com/sql/)

### Para Aprender Git
- [Git - Guia Simplificado](https://rogerdudler.github.io/git-guide/index.pt_BR.html)
- [Git Handbook](https://github.github.com/handbook/pt/)

---

## 🚀 Próximos Passos

1. Consulte [Guia de Permissões](./05-guia-de-permissoes.md)
2. Explore os arquivos do código
3. Tente fazer modificações pequenas
4. Leia a documentação do Laravel
5. Pratique criando features simples

---

## 💬 Dúvidas Frequentes

### P: Por que tantas camadas (Controller, Model, View)?
R: Cada camada tem uma responsabilidade. Se tudo junto, fica impossível de manter.

### P: Por que usar Composer?
R: Composer é o gerenciador de pacotes PHP. Como o NPM para JavaScript, mas para PHP.

### P: Por que usar Artisan?
R: Artisan é a ferramenta de linha de comando do Laravel. Facilita muitas tarefas (migrations, seeders, etc.).

### P: O que acontece se eu apagar um arquivo?
R: O sistema para de funcionar. Cada arquivo é importante.

### P: Posso mudar algo sem quebrar?
R: Depende. Se souber o que está fazendo, sim. Se não, pode quebrar.

---

## 📝 Conclusão

O AlfaSchool foi construído seguindo as melhores práticas do Laravel e desenvolvimento web.

**Principais pontos:**
1. ✅ Usa framework moderno (Laravel)
2. ✅ Usa pacotes testados (Breeze, Spatie)
3. ✅ Organização MVC (separado por responsabilidade)
4. ✅ Múltiplas camadas de segurança
5. ✅ Documentação completa
6. ✅ Código limpo e legível

Se você entendeu este documento, você tem uma boa base para trabalhar com o AlfaSchool!

---

**Próximo:** [Guia de Permissões](./05-guia-de-permissoes.md)
