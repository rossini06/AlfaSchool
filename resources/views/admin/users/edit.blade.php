<x-app-layout>
    <x-slot name="header">
        <h2 class="font-semibold text-xl text-gray-800 leading-tight">Editar Usuário: {{ $user->name }}</h2>
    </x-slot>

    <div class="py-12">
        <div class="max-w-4xl mx-auto sm:px-6 lg:px-8">
            <div class="bg-white shadow-sm sm:rounded-lg">
                <div class="p-6">
                    <form method="POST" action="{{ route('admin.users.update', $user) }}">
                        @csrf
                        @method('PUT')
                        
                        <div class="mb-4">
                            <label for="name" class="block text-sm font-medium text-gray-700">Nome</label>
                            <input type="text" name="name" id="name" value="{{ old('name', $user->name) }}" required
                                class="mt-1 block w-full border border-gray-300 rounded-md shadow-sm py-2 px-3 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm">
                            @error('name')
                                <p class="mt-2 text-sm text-red-600">{{ $message }}</p>
                            @enderror
                        </div>

                        <div class="mb-4">
                            <label for="email" class="block text-sm font-medium text-gray-700">Email</label>
                            <input type="email" name="email" id="email" value="{{ old('email', $user->email) }}" required
                                class="mt-1 block w-full border border-gray-300 rounded-md shadow-sm py-2 px-3 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm">
                            @error('email')
                                <p class="mt-2 text-sm text-red-600">{{ $message }}</p>
                            @enderror
                        </div>

                        <div class="mb-4">
                            <label for="password" class="block text-sm font-medium text-gray-700">Senha (deixe em branco para manter atual)</label>
                            <input type="password" name="password" id="password" minlength="8"
                                class="mt-1 block w-full border border-gray-300 rounded-md shadow-sm py-2 px-3 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm">
                            @error('password')
                                <p class="mt-2 text-sm text-red-600">{{ $message }}</p>
                            @enderror
                        </div>

                        <div class="mb-4">
                            <label for="password_confirmation" class="block text-sm font-medium text-gray-700">Confirmar Senha</label>
                            <input type="password" name="password_confirmation" id="password_confirmation"
                                class="mt-1 block w-full border border-gray-300 rounded-md shadow-sm py-2 px-3 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm">
                        </div>

                        <div class="mb-6">
                            <label class="block text-sm font-medium text-gray-700 mb-2">Perfis</label>
                            <div class="space-y-2">
                                @foreach($roles as $role)
                                    <label class="flex items-center">
                                        <input type="checkbox" name="roles[]" value="{{ $role->name }}"
                                            {{ in_array($role->name, $userRoles) ? 'checked' : '' }}
                                            class="h-4 w-4 text-indigo-600 focus:ring-indigo-500 border-gray-300 rounded">
                                        <span class="ml-2 text-sm text-gray-700">{{ ucfirst($role->name) }}</span>
                                    </label>
                                @endforeach
                            </div>
                        </div>

                        <div class="mb-6">
                            <div class="flex items-center justify-between mb-2">
                                <label class="block text-sm font-medium text-gray-700">Permissões Individuais</label>
                                <span class="text-xs text-gray-500">Permissões extras além dos perfis</span>
                            </div>
                            @if($permissions->isNotEmpty())
                                @foreach($permissions as $module => $modulePermissions)
                                    <div class="mb-4">
                                        <h4 class="text-sm font-medium text-gray-900 mb-2 capitalize">{{ $module }}</h4>
                                        <div class="grid grid-cols-2 gap-2">
                                            @foreach($modulePermissions as $permission)
                                                <label class="flex items-center p-2 border rounded hover:bg-gray-50">
                                                    <input type="checkbox" name="permissions[]" value="{{ $permission->name }}"
                                                        {{ in_array($permission->name, $userPermissions) ? 'checked' : '' }}
                                                        class="h-4 w-4 text-indigo-600 focus:ring-indigo-500 border-gray-300 rounded">
                                                    <span class="ml-2 text-sm text-gray-700">{{ $permission->name }}</span>
                                                </label>
                                            @endforeach
                                        </div>
                                    </div>
                                @endforeach
                            @else
                                <p class="text-sm text-gray-500">Nenhuma permissão disponível.</p>
                            @endif
                        </div>

                        <div class="flex items-center justify-between">
                            <a href="{{ route('admin.users.index') }}" class="text-gray-600 hover:text-gray-900">
                                Cancelar
                            </a>
                            <button type="submit" class="px-4 py-2 bg-indigo-600 text-white rounded hover:bg-indigo-700">
                                Atualizar
                            </button>
                        </div>
                    </form>
                </div>
            </div>
        </div>
    </div>
</x-app-layout>
