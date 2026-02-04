<x-app-layout>
    <x-slot name="header">
        <h2 class="font-semibold text-xl text-gray-800 leading-tight">Dashboard</h2>
    </x-slot>

    <div class="py-12">
        <div class="max-w-7xl mx-auto sm:px-6 lg:px-8">
            <div class="bg-white overflow-hidden shadow-sm sm:rounded-lg">
                <div class="p-6 text-gray-900">
                    <h1 class="text-2xl font-bold mb-4">Bem-vindo, {{ $user->name }}!</h1>
                    <p class="mb-4">Login realizado com sucesso.</p>
                    
                    <div class="mt-6">
                        <h3 class="text-lg font-semibold mb-2">Seus Perfis:</h3>
                        <div class="flex flex-wrap gap-2">
                            @foreach($roles as $role)
                                <span class="px-3 py-1 bg-blue-100 text-blue-800 rounded-full text-sm font-medium">
                                    {{ ucfirst($role) }}
                                </span>
                            @endforeach
                        </div>
                    </div>

                    <div class="mt-6">
                        <h3 class="text-lg font-semibold mb-2">Suas Permissões:</h3>
                        <div class="grid grid-cols-2 md:grid-cols-3 gap-2">
                            @foreach($permissions as $permission)
                                <span class="px-3 py-1 bg-green-100 text-green-800 rounded text-sm">
                                    {{ $permission }}
                                </span>
                            @endforeach
                        </div>
                    </div>

                    <div class="mt-8 p-4 bg-yellow-50 border border-yellow-200 rounded-lg">
                        <p class="text-sm text-yellow-800">
                            Você está autenticado como <strong>{{ auth()->user()->email }}</strong>
                        </p>
                    </div>

                    <form method="POST" action="{{ route('logout') }}" class="mt-6">
                        @csrf
                        <button type="submit" class="px-4 py-2 bg-red-600 text-white rounded hover:bg-red-700">
                            Sair
                        </button>
                    </form>
                </div>
            </div>
        </div>
    </div>
</x-app-layout>
