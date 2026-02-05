<x-app-layout>
    <x-slot name="header">
        <h2 class="font-semibold text-xl text-gray-800 leading-tight">Novo Usuário</h2>
    </x-slot>

    <div class="py-12">
        <div class="max-w-5xl mx-auto sm:px-6 lg:px-8">
            
            <!-- Ajuda Inicial -->
            <div class="bg-blue-50 border border-blue-200 rounded-lg p-4 mb-6">
                <div class="flex">
                    <div class="flex-shrink-0">
                        <svg class="h-5 w-5 text-blue-600" fill="currentColor" viewBox="0 0 20 20">
                            <path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clip-rule="evenodd"/>
                        </svg>
                    </div>
                    <div class="ml-3">
                        <p class="text-sm text-blue-700">
                            <strong>Como funciona:</strong> 
                            Primeiro, preencha os <strong>Dados Pessoais</strong>. 
                            Depois, escolha o <strong>Perfil</strong> e adicione <strong>Permissões Extras</strong> se necessário.
                        </p>
                    </div>
                </div>
            </div>

            <div class="bg-white shadow-sm sm:rounded-lg">
                <div class="p-6">
                    <form method="POST" action="{{ route('admin.users.store') }}">
                        @csrf
                        
                        <!-- Dados Pessoais -->
                        <h3 class="text-lg font-medium text-gray-900 mb-4 flex items-center">
                            <svg class="h-5 w-5 mr-2 text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"/>
                            </svg>
                            Dados Pessoais
                        </h3>
                        
                        <div class="grid grid-cols-1 md:grid-cols-2 gap-4 mb-6">
                            <div>
                                <label for="name" class="block text-sm font-medium text-gray-700 mb-1">Nome Completo *</label>
                                <input type="text" name="name" id="name" value="{{ old('name') }}" required
                                    class="block w-full border border-gray-300 rounded-md shadow-sm py-2 px-3 focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm">
                                @error('name')
                                    <p class="mt-1 text-sm text-red-600">{{ $message }}</p>
                                @enderror
                            </div>

                            <div>
                                <label for="email" class="block text-sm font-medium text-gray-700 mb-1">E-mail *</label>
                                <input type="email" name="email" id="email" value="{{ old('email') }}" required
                                    class="block w-full border border-gray-300 rounded-md shadow-sm py-2 px-3 focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
                                    placeholder="Digite o email do usuário"
                                    autocomplete="off">
                                @error('email')
                                    <p class="mt-1 text-sm text-red-600">{{ $message }}</p>
                                @enderror
                            </div>

                            <div>
                                <label for="password" class="block text-sm font-medium text-gray-700 mb-1">Senha *</label>
                                <div class="relative">
                                    <input type="password" name="password" id="password" required minlength="8"
                                        class="block w-full border border-gray-300 rounded-md shadow-sm py-2 px-3 pr-10 focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
                                        placeholder="Digite sua senha"
                                        autocomplete="new-password"
                                        onfocus="this.removeAttribute('readonly');" readonly>
                                    <button type="button" onclick="togglePassword('password')"
                                        class="absolute inset-y-0 right-0 px-3 flex items-center text-gray-400 hover:text-gray-600">
                                        <svg id="password-eye" class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/>
                                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"/>
                                        </svg>
                                    </button>
                                </div>
                                
                                <!-- Indicador de Força da Senha -->
                                <div id="password-strength" class="mt-2 hidden">
                                    <div class="flex items-center mb-1">
                                        <span class="text-sm font-medium mr-2">Força da senha:</span>
                                        <span id="strength-text" class="text-sm font-bold">--</span>
                                    </div>
                                    <div class="h-2 bg-gray-200 rounded-full overflow-hidden">
                                        <div id="strength-bar" class="h-full transition-all duration-300" style="width: 0%"></div>
                                    </div>
                                </div>
                                
                                <!-- Requisitos da Senha -->
                                <div id="password-requirements" class="mt-3 p-3 bg-gray-50 rounded-lg">
                                    <p class="text-xs font-medium text-gray-600 mb-2">Requisitos da senha:</p>
                                    <div class="space-y-1">
                                        <div id="req-length" class="flex items-center text-sm text-gray-400">
                                            <span class="req-icon w-5 h-5 mr-2 flex items-center justify-center rounded-full bg-gray-200 text-xs"></span>
                                            <span class="req-text">Mínimo 8 caracteres</span>
                                        </div>
                                        <div id="req-uppercase" class="flex items-center text-sm text-gray-400">
                                            <span class="req-icon w-5 h-5 mr-2 flex items-center justify-center rounded-full bg-gray-200 text-xs"></span>
                                            <span class="req-text">Letra maiúscula (A-Z)</span>
                                        </div>
                                        <div id="req-lowercase" class="flex items-center text-sm text-gray-400">
                                            <span class="req-icon w-5 h-5 mr-2 flex items-center justify-center rounded-full bg-gray-200 text-xs"></span>
                                            <span class="req-text">Letra minúscula (a-z)</span>
                                        </div>
                                        <div id="req-number" class="flex items-center text-sm text-gray-400">
                                            <span class="req-icon w-5 h-5 mr-2 flex items-center justify-center rounded-full bg-gray-200 text-xs"></span>
                                            <span class="req-text">Número (0-9)</span>
                                        </div>
                                        <div id="req-special" class="flex items-center text-sm text-gray-400">
                                            <span class="req-icon w-5 h-5 mr-2 flex items-center justify-center rounded-full bg-gray-200 text-xs"></span>
                                            <span class="req-text">Símbolo especial (@$!%*#?&)</span>
                                        </div>
                                    </div>
                                </div>
                                
                                @error('password')
                                    <p class="mt-1 text-sm text-red-600">{{ $message }}</p>
                                @enderror
                            </div>

                            <div>
                                <label for="password_confirmation" class="block text-sm font-medium text-gray-700 mb-1">Confirmar Senha *</label>
                                <div class="relative">
                                    <input type="password" name="password_confirmation" id="password_confirmation" required
                                        class="block w-full border border-gray-300 rounded-md shadow-sm py-2 px-3 pr-10 focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
                                        placeholder="Digite a mesma senha"
                                        autocomplete="new-password"
                                        oninput="checkPasswordMatch()">
                                    <button type="button" onclick="togglePassword('password_confirmation')"
                                        class="absolute inset-y-0 right-0 px-3 flex items-center text-gray-400 hover:text-gray-600">
                                        <svg id="password_confirmation-eye" class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/>
                                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"/>
                                        </svg>
                                    </button>
                                </div>
                                <div id="password-match" class="mt-2 text-sm hidden">
                                    <span id="match-text"></span>
                                </div>
                            </div>
                        </div>

                        <hr class="my-6 border-gray-200">

                        <!-- Perfis -->
                        <div class="mb-8">
                            <div class="flex items-center mb-4">
                                <svg class="h-6 w-6 mr-2 text-purple-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z"/>
                                </svg>
                                <h3 class="text-lg font-medium text-gray-900">1. Escolher o Perfil do Usuário</h3>
                            </div>
                            
                            <p class="text-sm text-gray-600 mb-4">
                                O <strong>Perfil</strong> define o cargo do usuário na escola. 
                                Cada perfil já vem com permissões padrões definidas.
                            </p>

                            <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
                                @foreach($roles as $role)
                                    @php
                                        $roleDescriptions = [
                                            'admin' => 'Acesso total ao sistema. Pode fazer tudo.',
                                            'gestor' => 'Gerencia alunos, turmas e visualiza relatórios.',
                                            'professor' => 'Lança notas e frequência dos alunos.',
                                            'secretaria' => 'Cadastra alunos e mantém registros.',
                                            'aluno' => 'Visualiza suas próprias notas e frequência.',
                                        ];
                                    @endphp
                                    <label class="relative flex items-start p-4 border rounded-lg hover:bg-gray-50 cursor-pointer border-gray-200">
                                        <div class="flex items-center h-5">
                                            <input type="checkbox" name="roles[]" value="{{ $role->name }}"
                                                {{ in_array($role->name, old('roles', [])) ? 'checked' : '' }}
                                                class="h-5 w-5 text-purple-600 focus:ring-purple-500 border-gray-300 rounded">
                                        </div>
                                        <div class="ml-3 flex-1">
                                            <span class="block text-sm font-medium text-gray-900 capitalize">{{ $role->name }}</span>
                                            <span class="block text-sm text-gray-500 mt-1">{{ $roleDescriptions[$role->name] ?? 'Perfil do sistema' }}</span>
                                        </div>
                                    </label>
                                @endforeach
                            </div>
                        </div>

                        <hr class="my-6 border-gray-200">

                        <!-- Permissões Individuais -->
                        <div class="mb-6">
                            <div class="flex items-center mb-4">
                                <svg class="h-6 w-6 mr-2 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z"/>
                                </svg>
                                <h3 class="text-lg font-medium text-gray-900">2. Permissões Extras (Opcional)</h3>
                            </div>
                            
                            <div class="bg-yellow-50 border border-yellow-200 rounded-lg p-4 mb-4">
                                <div class="flex">
                                    <div class="flex-shrink-0">
                                        <svg class="h-5 w-5 text-yellow-600" fill="currentColor" viewBox="0 0 20 20">
                                            <path fill-rule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clip-rule="evenodd"/>
                                        </svg>
                                    </div>
                                    <div class="ml-3">
                                        <p class="text-sm text-yellow-700">
                                            <strong>O que são Permissões Extras?</strong><br>
                                            São acessos adicionais que você pode dar a um usuário, 
                                            além do que o perfil já permite. 
                                            Por exemplo: Dar a um professor o acesso para ver o financeiro.
                                        </p>
                                    </div>
                                </div>
                            </div>

                            @if(isset($permissions) && $permissions->isNotEmpty())
                                @foreach($permissions as $module => $modulePermissions)
                                    @php
                                        $moduleTitles = [
                                            'users' => '👥 Módulo de Usuários',
                                            'roles' => '🔑 Módulo de Perfis',
                                            'alunos' => '🎓 Módulo de Alunos',
                                            'turmas' => '📚 Módulo de Turmas',
                                            'notas' => '📝 Módulo de Notas',
                                            'frequencia' => '📅 Módulo de Frequência',
                                            'financeiro' => '💰 Módulo Financeiro',
                                            'meus_dados' => '👤 Meus Dados',
                                        ];
                                        
                                        $moduleHelp = [
                                            'users' => 'Gerenciar usuários do sistema',
                                            'roles' => 'Gerenciar perfis e permissões',
                                            'alunos' => 'Gerenciar dados dos alunos',
                                            'turmas' => 'Gerenciar turmas e disciplinas',
                                            'notas' => 'Lançar e visualizar notas',
                                            'frequencia' => 'Registrar presença dos alunos',
                                            'financeiro' => 'Ver e editar dados financeiros',
                                            'meus_dados' => 'Visualizar próprios dados',
                                        ];
                                        
                                        $permissionHelp = [
                                            'view' => 'Pode ver esta função',
                                            'create' => 'Pode criar novos registros',
                                            'edit' => 'Pode editar registros existentes',
                                            'delete' => 'Pode excluir registros',
                                        ];
                                    @endphp
                                    
                                    <div class="mb-6 p-4 bg-gray-50 rounded-lg">
                                        <h4 class="text-md font-medium text-gray-800 mb-2 flex items-center">
                                            {{ $moduleTitles[$module] ?? '📁 ' . ucfirst($module) }}
                                        </h4>
                                        <p class="text-sm text-gray-600 mb-3">{{ $moduleHelp[$module] ?? '' }}</p>
                                        
                                        <div class="grid grid-cols-1 md:grid-cols-2 gap-2">
                                            @foreach($modulePermissions as $permission)
                                                @php
                                                    $parts = explode('.', $permission->name);
                                                    $action = $parts[1] ?? 'view';
                                                @endphp
                                                <label class="flex items-start p-3 bg-white border rounded-lg hover:bg-gray-100 cursor-pointer">
                                                    <div class="flex items-center h-5">
                                                        <input type="checkbox" name="permissions[]" value="{{ $permission->name }}"
                                                            {{ in_array($permission->name, old('permissions', [])) ? 'checked' : '' }}
                                                            class="h-5 w-5 text-green-600 focus:ring-green-500 border-gray-300 rounded mt-0.5">
                                                    </div>
                                                    <div class="ml-3">
                                                        <span class="block text-sm font-medium text-gray-900 capitalize">
                                                            {{ $permissionHelp[$action] ?? ucfirst($action) }}
                                                        </span>
                                                    </div>
                                                </label>
                                            @endforeach
                                        </div>
                                    </div>
                                @endforeach
                            @else
                                <div class="bg-gray-100 rounded-lg p-4 text-center">
                                    <p class="text-gray-500">Nenhuma permissão disponível no sistema.</p>
                                </div>
                            @endif
                        </div>

                        <div class="mt-8 pt-4 border-t border-gray-200">
                            <button type="submit" class="w-full sm:w-auto px-8 py-4 bg-green-600 text-white text-lg font-bold rounded-lg shadow-md hover:bg-green-700 flex items-center justify-center">
                                <svg class="h-6 w-6 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7"/>
                                </svg>
                                CRIAR USUÁRIO
                            </button>
                        </div>
                    </form>
                    
                    <script>
                        // Configuração dos requisitos
                        const requirements = {
                            length: { regex: /.{8,}/, element: 'req-length' },
                            uppercase: { regex: /[A-Z]/, element: 'req-uppercase' },
                            lowercase: { regex: /[a-z]/, element: 'req-lowercase' },
                            number: { regex: /[0-9]/, element: 'req-number' },
                            special: { regex: /[@$!%*#?&]/, element: 'req-special' }
                        };

                        // Cores para força da senha
                        const strengthConfig = {
                            0: { color: 'bg-gray-300', text: '--', textColor: 'text-gray-400' },
                            1: { color: 'bg-red-500', text: 'Fraca', textColor: 'text-red-600' },
                            2: { color: 'bg-orange-500', text: 'Média', textColor: 'text-orange-600' },
                            3: { color: 'bg-yellow-500', text: 'Boa', textColor: 'text-yellow-600' },
                            4: { color: 'bg-green-500', text: 'Forte', textColor: 'text-green-600' }
                        };

                        // Mostrar/ocultar senha
                        function togglePassword(fieldId) {
                            const field = document.getElementById(fieldId);
                            const eyeIcon = document.getElementById(fieldId + '-eye');
                            
                            if (field.type === 'password') {
                                field.type = 'text';
                                eyeIcon.innerHTML = '<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0112 5c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 01-4.132 5.411m0 0L21 21"/>';
                            } else {
                                field.type = 'password';
                                eyeIcon.innerHTML = '<path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"/>';
                            }
                        }

                        // Atualizar indicador de requisitos
                        function updateRequirements(password) {
                            let metCount = 0;
                            
                            for (const [key, config] of Object.entries(requirements)) {
                                const element = document.getElementById(config.element);
                                const icon = element.querySelector('.req-icon');
                                const text = element.querySelector('.req-text');
                                const isMet = config.regex.test(password);
                                
                                if (isMet) {
                                    metCount++;
                                    icon.textContent = '✓';
                                    icon.className = 'req-icon w-5 h-5 mr-2 flex items-center justify-center rounded-full bg-green-200 text-xs text-green-600';
                                    text.className = 'req-text text-green-600 font-medium';
                                } else {
                                    icon.textContent = '○';
                                    icon.className = 'req-icon w-5 h-5 mr-2 flex items-center justify-center rounded-full bg-gray-200 text-xs text-gray-400';
                                    text.className = 'req-text text-gray-400';
                                }
                            }
                            
                            // Atualizar força da senha
                            const strengthBar = document.getElementById('strength-bar');
                            const strengthText = document.getElementById('strength-text');
                            const strengthContainer = document.getElementById('password-strength');
                            
                            if (password.length > 0) {
                                strengthContainer.classList.remove('hidden');
                                const strength = strengthConfig[metCount];
                                strengthBar.className = 'h-full transition-all duration-300 ' + strength.color;
                                strengthBar.style.width = (metCount * 20) + '%';
                                strengthText.textContent = strength.text;
                                strengthText.className = 'text-sm font-bold ' + strength.textColor;
                            } else {
                                strengthContainer.classList.add('hidden');
                            }
                        }

                        // Verificar se senhas coincidem
                        function checkPasswordMatch() {
                            const password = document.getElementById('password').value;
                            const confirmation = document.getElementById('password_confirmation').value;
                            const matchDiv = document.getElementById('password-match');
                            const matchText = document.getElementById('match-text');
                            
                            if (confirmation.length > 0) {
                                matchDiv.classList.remove('hidden');
                                if (password === confirmation) {
                                    matchDiv.className = 'mt-2 text-sm text-green-600';
                                    matchText.innerHTML = '<span class="font-medium">✓</span> As senhas coincidem!';
                                } else {
                                    matchDiv.className = 'mt-2 text-sm text-red-600';
                                    matchText.innerHTML = '<span class="font-medium">✗</span> As senhas não coincidem!';
                                }
                            } else {
                                matchDiv.classList.add('hidden');
                            }
                        }

                        // Event listeners
                        document.getElementById('password').addEventListener('input', function() {
                            updateRequirements(this.value);
                            checkPasswordMatch();
                        });

                        document.getElementById('password_confirmation').addEventListener('input', checkPasswordMatch);
                    </script>
                </div>
            </div>
        </div>
    </div>
</x-app-layout>
