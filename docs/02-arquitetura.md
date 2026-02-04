# 02. Arquitetura do Sistema

## 📋 Visão Geral

O AlfaSchool segue a arquitetura **MVC (Model-View-Controller)** do Laravel, que é um padrão amplamente utilizado e testado.

## 🏗️ O que é MVC?

### Explicação Simples

MVC é como organizar um restaurante:

| Componente | No Restaurante | No AlfaSchool |
|------------|----------------|---------------|
| **Model** | Ingredientes, receitas | Dados (usuários, alunos, notas) |
| **View** | Prato servido ao cliente | Página HTML que o usuário vê |
| **Controller** | Cozinheiro (prepara o prato) | Recebe pedido, busca dados, devolve página |

### Como funciona?

1. **Usuário** acessa `/admin/users`
2. **Route** envia para `UserController`
3. **Controller** busca usuários no banco (via Model)
4. **View** exibe os usuários em HTML
5. **Usuário** vê a lista

## 🗂️ Estrutura de Diretórios

```
alfaschool/
├── app/                          # Aplicação (lógica)
│   ├── Http/                      # Camada HTTP
│   │   ├── Controllers/            # Cérebro (processa pedidos)
│   │   │   ├── Admin/            # Controllers da área admin
│   │   │   ├── Auth/             # Controllers de autenticação
│   │   │   ├── DashboardController.php
│   │   │   └── ProfileController.php
│   │   ├── Middleware/            # Guardas (verifica permissões)
│   │   │   ├── LogAuditableActions.php  # Logs de auditoria
│   │   │   └── SecurityHeaders.php     # Headers de segurança
│   │   └── Requests/              # Validação de formulários
│   ├── Models/                    # Dados (molde do banco)
│   │   └── User.php              # Modelo de usuário
│   └── Providers/                 # Provedores de serviços
│       └── AppServiceProvider.php # Configurações globais
│
├── database/                     # Banco de dados
│   ├── migrations/                # Histórico de mudanças no banco
│   │   ├── create_users_table.php
│   │   └── create_permission_tables.php
│   └── seeders/                   # Dados iniciais
│       ├── DatabaseSeeder.php
│       ├── RolePermissionSeeder.php
│       └── AdminSeeder.php
│
├── resources/                    # Arquivos visuais
│   ├── views/                     # Páginas HTML (Blade)
│   │   ├── admin/                # Páginas da área admin
│   │   │   ├── users/           # Views de usuários
│   │   │   ├── roles/           # Views de roles
│   │   │   └── permissions/    # Views de permissões
│   │   ├── auth/                 # Páginas de autenticação
│   │   ├── layouts/               # Layouts (cabeçalho, rodapé)
│   │   └── components/            # Componentes reutilizáveis
│   ├── css/                       # Estilos (Tailwind CSS)
│   └── js/                        # JavaScript
│
├── routes/                       # Rotas (endereços do site)
│   ├── web.php                    # Rotas principais
│   ├── auth.php                   # Rotas de autenticação
│   └── console.php                # Rotas de linha de comando
│
├── config/                       # Configurações do sistema
│   ├── auth.php                   # Configurações de autenticação
│   ├── permission.php             # Configurações de permissões
│   └── logging.php               # Configurações de logs
│
├── storage/                      # Arquivos armazenados
│   ├── logs/                      # Logs do sistema
│   │   ├── laravel.log            # Logs gerais
│   │   └── audit-YYYY-MM-DD.log  # Logs de auditoria
│   └── framework/                 # Cache e sessões
│
└── public/                       # Arquivos públicos (acessíveis pelo navegador)
    └── index.php                 # Ponto de entrada
```

## 🧩 Componentes Principais

### 1. Models (Modelos de Dados)

**O que são?**
Representam as tabelas do banco de dados como classes PHP.

**Exemplo: `User.php`**
```php
class User extends Authenticatable
{
    use HasFactory, Notifiable, HasRoles;
    
    protected $fillable = ['name', 'email', 'password'];
}
```

**Por que usar?**
- Abstração: Não escrevemos SQL diretamente
- Validação: Garante dados consistentes
- Relacionamentos: Facilita conexão entre tabelas

**Models no AlfaSchool:**
- `User`: Usuários do sistema
- `Role`: Perfis (admin, professor, etc.)
- `Permission`: Permissões (users.view, etc.)

### 2. Controllers (Controladores)

**O que são?**
Recebem requisições HTTP, processam e devolvem respostas.

**Exemplo: `UserController.php`**
```php
class UserController extends Controller
{
    public function index()
    {
        $users = User::all();
        return view('admin.users.index', compact('users'));
    }
}
```

**Por que usar?**
- Organização: Separa lógica de cada funcionalidade
- Reutilização: Código pode ser usado em vários lugares
- Testabilidade: Fácil testar cada controller

**Controllers no AlfaSchool:**
- `DashboardController`: Página principal
- `UserController`: CRUD de usuários
- `RoleController`: Gestão de roles
- `ProfileController`: Edição de perfil

### 3. Views (Visões)

**O que são?**
Arquivos HTML que o usuário vê no navegador.

**Exemplo: `index.blade.php`**
```blade
@foreach($users as $user)
    <p>{{ $user->name }}</p>
@endforeach
```

**Por que usar Blade?**
- Simples: Mistura HTML e PHP facilmente
- Seguro: Escapa automaticamente (evita XSS)
- Reutilizável: Componentes e layouts

**Views no AlfaSchool:**
- `dashboard.blade.php`: Página principal
- `admin/users/index.blade.php`: Lista de usuários
- `auth/login.blade.php`: Formulário de login

### 4. Routes (Rotas)

**O que são?**
Definem os endereços (URLs) do site.

**Exemplo:**
```php
Route::get('/admin/users', [UserController::class, 'index'])
    ->name('admin.users.index');
```

**Por que usar?**
- Organização: Centraliza todos os endereços
- Middleware: Protege rotas facilmente
- Nomes: Facilita alteração de URLs

### 5. Middleware (Intermediários)

**O que são?**
Como "guardas" que verificam condições antes de acessar uma rota.

**Exemplo:**
```php
Route::middleware(['auth'])->get('/dashboard', ...);
```

**Como funciona?**
1. Usuário acessa `/dashboard`
2. Middleware verifica: "Está logado?"
3. Se NÃO → Redireciona para login
4. Se SIM → Permite acesso

**Middleware no AlfaSchool:**
- `auth`: Usuário deve estar logado
- `role:admin`: Usuário deve ter role admin
- `permission:users.view`: Usuário deve ter permissão
- `throttle:5,1`: Limita 5 tentativas por minuto
- `audit`: Registra ação em log
- `security-headers`: Adiciona headers de segurança

### 6. Migrations (Migrações)

**O que são?**
Arquivos que definem como criar/alterar tabelas do banco.

**Exemplo:**
```php
Schema::create('users', function (Blueprint $table) {
    $table->id();
    $table->string('name');
    $table->string('email')->unique();
    $table->timestamps();
});
```

**Por que usar?**
- Versionamento: Histórico de mudanças no banco
- Reprodução: Fácil replicar estrutura
- Reversão: Pode desfazer mudanças

### 7. Seeders (Semeadores)

**O que são?**
Arquivos que preenchem o banco com dados iniciais.

**Exemplo:**
```php
Role::create(['name' => 'admin']);
User::create(['name' => 'Administrador', 'email' => 'admin@...']);
```

**Por que usar?**
- Rapidez: Um comando preenche tudo
- Consistência: Sempre cria mesmos dados
- Testes: Facilita desenvolvimento

## 🔄 Fluxo de Dados

### Cenário: Listar Usuários

```
1. Usuário acessa /admin/users
   ↓
2. Route envia para UserController@index()
   ↓
3. Middleware verifica: 
   - Está logado? SIM
   - Tem role admin? SIM
   - Tem permissão users.view? SIM
   ↓
4. Controller executa:
   - Busca users: User::all()
   - Passa para view
   ↓
5. View gera HTML com a lista
   ↓
6. Resposta enviada ao navegador
   ↓
7. Usuário vê a tabela de usuários
```

### Cenário: Criar Usuário

```
1. Usuário clica em "Novo Usuário"
   ↓
2. Route: /admin/users/create
   ↓
3. Middleware verifica: Tem permissão users.create? SIM
   ↓
4. Controller: mostra formulário (view create)
   ↓
5. Usuário preenche e envia
   ↓
6. Route: POST /admin/users
   ↓
7. Controller: valida dados
   - Nome? Válido
   - Email? Válido e único
   - Senha? Válida e forte
   ↓
8. Model: cria usuário no banco
   - User::create([...])
   ↓
9. Controller: atribui roles
   - $user->assignRole(['admin'])
   ↓
10. Middleware audit: registra ação
   - Log: "Usuário X criou usuário Y"
   ↓
11. Controller: redireciona para /admin/users
   ↓
12. Usuário vê novo usuário na lista
```

## 🔐 Camadas de Segurança

### Camada 1: Middleware de Autenticação
```php
Route::middleware(['auth'])->group(...)
```
- Verifica se usuário está logado
- Se não, redireciona para login

### Camada 2: Middleware de Roles
```php
Route::middleware(['role:admin'])->group(...)
```
- Verifica se usuário tem role específico
- Se não, retorna erro 403

### Camada 3: Middleware de Permissões
```php
Route::middleware(['permission:users.view'])->get(...)
```
- Verifica se usuário tem permissão específica
- Se não, retorna erro 403

### Camada 4: Rate Limiting
```php
Route::middleware(['throttle:5,1'])->post('/login')
```
- Limita 5 tentativas por minuto
- Bloqueia ataques de força bruta

### Camada 5: Validations
```php
'password' => [
    'required',
    'min:8',
    'regex:/[A-Z]/',
    // ...
]
```
- Garante dados válidos
- Evita dados corrompidos

### Camada 6: Security Headers
```php
$response->headers->set('X-Frame-Options', 'SAMEORIGIN');
$response->headers->set('X-XSS-Protection', '1; mode=block');
```
- Protege contra ataques
- Configurações do navegador

### Camada 7: Logging
```php
Log::channel('audit')->info('Auditable action', [...]);
```
- Registra tudo que acontece
- Facilita investigação

### Camada 8: Database Encryption
```env
SESSION_ENCRYPT=true
```
- Criptografa sessão no banco
- Se alguém roupar o banco, não lê a sessão

## 📊 Padrões Utilizados

### 1. Separation of Concerns (Separação de Responsabilidades)
- Modelos cuidam dos dados
- Controllers cuidam da lógica
- Views cuidam do visual
- Routes cuidam dos endereços

### 2. Don't Repeat Yourself (Não se repita)
- Componentes reutilizáveis
- Helpers e utilities
- Traits compartilhadas

### 3. Convention over Configuration (Convenção sobre configuração)
- Nomes padrões (ex: `UserController.php`)
- Estrutura padrão de diretórios
- Facilita navegação no projeto

### 4. Single Responsibility Principle (Responsabilidade única)
- Cada classe faz uma coisa bem
- Ex: UserController apenas cuida de usuários
- Facilita manutenção

### 5. Dependency Injection (Injeção de dependência)
- Recebe dependências via construtor
- Facilita testes
- Mais flexível

## 🎯 Design Decisions (Decisões de Design)

### Por que Laravel?
- ✓ Popular e bem documentado
- ✓ Seguro e atualizado
- ✓ Comunidade ativa
- ✓ Fácil de aprender

### Por que Spatie Permission?
- ✓ Flexível e fácil de usar
- ✓ Bem mantido
- ✓ Usa cache (rápido)
- ✓ Sintaxe clara

### Por que Blade?
- ✓ Integrado ao Laravel
- ✓ Sintaxe simples
- ✓ Seguro (auto-escape)
- ✓ Componentes poderosos

### Por que MySQL?
- ✓ Popular e confiável
- ✓ Bom para relacionamentos
- ✓ Ferramentas de administração
- ✓ Performance adequada

### Por que Tailwind CSS?
- ✓ Classes utilitárias (rápido)
- ✓ Não precisa escrever CSS custom
- ✓ Consistente
- ✓ Mobile-first

## 📈 Escalabilidade

### Horizontal Scaling (Escalonamento Horizontal)
- Adicione mais servidores
- Balanceador de carga
- Banco separado

### Vertical Scaling (Escalonamento Vertical)
- Melhore o servidor atual
- Mais RAM, CPU, Disco

### Caching (Cache)
- Cache de configurações
- Cache de permissões
- Cache de views

### Queue (Filas)
- Processamento assíncrono
- Envio de emails
- Jobs pesados

## 📝 Próximos Passos

1. Entenda [Como o Sistema Funciona](./03-como-funciona.md)
2. Veja o [Guia para Iniciantes](./04-para-iniciantes.md)
3. Consulte o [Guia de Permissões](./05-guia-de-permissoes.md)
