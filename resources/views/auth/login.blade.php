<!DOCTYPE html>
<html lang="{{ str_replace('_', '-', app()->getLocale()) }}">
    <head>
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <meta name="csrf-token" content="{{ csrf_token() }}">

        <title>{{ config('app.name', 'AlfaSchool - Gestão Escolar') }}</title>

        <link rel="preconnect" href="https://fonts.bunny.net">
        <link href="https://fonts.bunny.net/css?family=figtree:400,500,600,700&display=swap" rel="stylesheet" />

        @vite(['resources/css/app.css', 'resources/js/app.js'])
    </head>
    <body class="min-h-screen bg-gradient-to-br from-indigo-700 via-indigo-600 to-teal-700 font-sans antialiased dark:from-slate-900 dark:via-slate-800 dark:to-slate-900">
        @php($schoolName = config('app.school_name', 'AlfaSchool'))

        <main class="grid min-h-screen grid-cols-1 md:grid-cols-2">
            <section class="relative z-10 flex flex-col justify-between p-6 text-white sm:p-8 md:p-12 lg:p-16" data-fade data-delay="60">
                <div>
                    <div class="flex justify-center md:justify-start">
                        <a href="{{ url('/') }}" class="inline-flex items-center rounded-xl bg-white/10 px-4 py-2 ring-1 ring-white/30 backdrop-blur-sm">
                            <x-application-logo class="h-12 w-auto max-w-[180px] object-contain" />
                        </a>
                    </div>

                    <div class="mx-auto mt-8 max-w-2xl text-center md:mx-0 md:mt-10 md:text-left">
                        <span class="inline-flex items-center rounded-full bg-white/15 px-3 py-1 text-xs font-semibold uppercase tracking-wider text-indigo-50 ring-1 ring-white/20">
                            Plataforma SaaS
                        </span>

                        <h1 class="mt-4 text-3xl font-bold leading-[1.08] tracking-tight sm:text-4xl lg:text-5xl">
                            Gestão Escolar <span class="font-semibold text-indigo-100">de Alto Nível</span>
                        </h1>
                        <p class="mt-3 text-base text-indigo-100/95 sm:text-lg sm:leading-relaxed">
                            Controle total, <span class="font-semibold text-white">segurança avançada</span> e organização em uma única plataforma.
                        </p>

                        <p class="mt-5 inline-flex rounded-lg bg-white/10 px-4 py-2 text-sm text-indigo-50 ring-1 ring-white/20">
                            Sistema com segurança avançada e controle por perfil
                        </p>
                    </div>

                    <div class="mt-8 grid grid-cols-1 gap-3 sm:grid-cols-2 sm:gap-4">
                        <article class="rounded-xl bg-white/10 p-3 ring-1 ring-white/20 backdrop-blur-sm sm:p-4">
                            <div class="flex items-start gap-3">
                                <svg class="mt-0.5 h-5 w-5 shrink-0 text-indigo-100" viewBox="0 0 24 24" fill="none" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8" d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>
                                <div>
                                    <h3 class="text-sm font-semibold sm:text-base">Gestão de Alunos</h3>
                                    <p class="mt-1 text-xs text-indigo-100/90 sm:text-sm">Organize matrículas e histórico em segundos.</p>
                                </div>
                            </div>
                        </article>

                        <article class="rounded-xl bg-white/10 p-3 ring-1 ring-white/20 backdrop-blur-sm sm:p-4">
                            <div class="flex items-start gap-3">
                                <svg class="mt-0.5 h-5 w-5 shrink-0 text-indigo-100" viewBox="0 0 24 24" fill="none" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8" d="M8 7h8m-8 5h8m-8 5h5M6 4h12a2 2 0 012 2v12a2 2 0 01-2 2H6a2 2 0 01-2-2V6a2 2 0 012-2z"/></svg>
                                <div>
                                    <h3 class="text-sm font-semibold sm:text-base">Controle de Turmas</h3>
                                    <p class="mt-1 text-xs text-indigo-100/90 sm:text-sm">Estruture classes e cronogramas com facilidade.</p>
                                </div>
                            </div>
                        </article>

                        <article class="rounded-xl bg-white/10 p-3 ring-1 ring-white/20 backdrop-blur-sm sm:p-4">
                            <div class="flex items-start gap-3">
                                <svg class="mt-0.5 h-5 w-5 shrink-0 text-indigo-100" viewBox="0 0 24 24" fill="none" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8" d="M9 17v-6m3 6V7m3 10v-4m4 8H5a2 2 0 01-2-2V5a2 2 0 012-2h14a2 2 0 012 2v14a2 2 0 01-2 2z"/></svg>
                                <div>
                                    <h3 class="text-sm font-semibold sm:text-base">Lançamento de Notas</h3>
                                    <p class="mt-1 text-xs text-indigo-100/90 sm:text-sm">Publique avaliações com fluxo simples e rápido.</p>
                                </div>
                            </div>
                        </article>

                        <article class="rounded-xl bg-white/10 p-3 ring-1 ring-white/20 backdrop-blur-sm sm:p-4">
                            <div class="flex items-start gap-3">
                                <svg class="mt-0.5 h-5 w-5 shrink-0 text-indigo-100" viewBox="0 0 24 24" fill="none" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>
                                <div>
                                    <h3 class="text-sm font-semibold sm:text-base">Frequência Digital</h3>
                                    <p class="mt-1 text-xs text-indigo-100/90 sm:text-sm">Controle presença com rapidez e precisão.</p>
                                </div>
                            </div>
                        </article>

                        <article class="rounded-xl bg-white/10 p-3 ring-1 ring-white/20 backdrop-blur-sm sm:p-4">
                            <div class="flex items-start gap-3">
                                <svg class="mt-0.5 h-5 w-5 shrink-0 text-indigo-100" viewBox="0 0 24 24" fill="none" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8" d="M17 9V7a5 5 0 10-10 0v2M5 9h14a2 2 0 012 2v8a2 2 0 01-2 2H5a2 2 0 01-2-2v-8a2 2 0 012-2z"/></svg>
                                <div>
                                    <h3 class="text-sm font-semibold sm:text-base">Financeiro Integrado</h3>
                                    <p class="mt-1 text-xs text-indigo-100/90 sm:text-sm">Visualize receitas e inadimplência com clareza.</p>
                                </div>
                            </div>
                        </article>

                        <article class="rounded-xl bg-white/10 p-3 ring-1 ring-white/20 backdrop-blur-sm sm:p-4">
                            <div class="flex items-start gap-3">
                                <svg class="mt-0.5 h-5 w-5 shrink-0 text-indigo-100" viewBox="0 0 24 24" fill="none" stroke="currentColor"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8" d="M12 3l7 4v6c0 5-3.2 8.8-7 10-3.8-1.2-7-5-7-10V7l7-4z"/><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.8" d="M9.5 12.5l1.8 1.8 3.2-3.2"/></svg>
                                <div>
                                    <h3 class="text-sm font-semibold sm:text-base">Permissões por Perfil</h3>
                                    <p class="mt-1 text-xs text-indigo-100/90 sm:text-sm">Garanta acessos seguros para cada função escolar.</p>
                                </div>
                            </div>
                        </article>
                    </div>
                </div>

                <div class="mt-10 space-y-3 text-sm text-indigo-100 md:mt-12">
                    <p class="rounded-lg bg-white/10 px-4 py-3 ring-1 ring-white/25">
                        Acesso restrito por perfil: Admin, Gestor, Professor, Secretaria e Aluno.
                    </p>
                    <p class="text-xs text-indigo-100/80">© {{ date('Y') }} AlfaSchool. Todos os direitos reservados.</p>
                </div>
            </section>

            <section class="relative isolate flex items-center justify-center overflow-hidden bg-gradient-to-br from-slate-100 via-white to-slate-50 p-5 sm:p-8" data-fade data-delay="160">
                <div class="pointer-events-none absolute inset-0 z-0 hidden md:block">
                    <img src="{{ asset('images/login-context-education.svg') }}" alt="Gestor escolar analisando dados e dashboards" class="h-full w-full object-cover opacity-35 blur-[0.8px]" />
                </div>
                <div class="pointer-events-none absolute inset-0 z-0 hidden md:block bg-gradient-to-br from-teal-950/45 via-slate-900/30 to-indigo-900/25"></div>
                <div class="pointer-events-none absolute left-1/2 top-1/2 z-0 h-80 w-80 -translate-x-1/2 -translate-y-1/2 rounded-full bg-indigo-500/10 blur-3xl"></div>
                <div class="pointer-events-none absolute inset-0 z-0 opacity-[0.03]" style="background-image: radial-gradient(circle at 1px 1px, #0f172a 1px, transparent 0); background-size: 14px 14px;"></div>

                <div class="relative z-10 w-full max-w-md rounded-2xl bg-white p-7 shadow-xl ring-1 ring-slate-200 backdrop-blur-sm" style="max-width: 420px;" data-fade data-delay="260">
                    <div class="mb-7">
                        <p class="text-xs font-semibold uppercase tracking-wider text-indigo-600 dark:text-indigo-400">{{ $schoolName }}</p>
                        <h2 class="mt-2 text-2xl font-bold text-slate-900 dark:text-slate-100">Acesse sua conta</h2>
                        <p class="mt-1 text-sm text-slate-600 dark:text-slate-300">Entre com suas credenciais para continuar</p>
                    </div>

                    <x-auth-session-status class="mb-4 text-sm text-emerald-700 dark:text-emerald-400" :status="session('status')" />

                    @if ($errors->has('email') && str_contains($errors->first('email'), 'bloqueada'))
                        <div class="mb-4 rounded-lg border border-red-300 bg-red-50 p-4 text-red-700 dark:border-red-800 dark:bg-red-900 dark:text-red-200" data-lockout-alert>
                            <div class="mb-1 flex items-center gap-2 text-sm font-semibold">
                                <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                                </svg>
                                Conta bloqueada
                            </div>
                            <p class="text-sm">{{ $errors->first('email') }}</p>
                        </div>
                    @elseif ($errors->has('email'))
                        <div class="mb-4 rounded-lg border border-orange-300 bg-orange-50 p-4 text-orange-700 dark:border-orange-800 dark:bg-orange-900 dark:text-orange-200">
                            <div class="mb-1 flex items-center gap-2 text-sm font-semibold">
                                <svg class="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
                                </svg>
                                Atenção
                            </div>
                            <p class="text-sm">{{ $errors->first('email') }}</p>
                        </div>
                    @endif

                    <form method="POST" action="{{ route('login') }}" id="login-form" class="space-y-5">
                        @csrf

                        <div>
                            <x-input-label for="email" :value="__('E-mail')" class="text-sm font-medium text-slate-600 dark:text-slate-300" />
                            <x-text-input
                                id="email"
                                class="mt-1.5 block w-full rounded-xl border bg-white px-3 py-2.5 text-base text-slate-900 placeholder:text-slate-400 transition duration-200 focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/60 dark:bg-slate-800 dark:text-slate-100 dark:placeholder:text-slate-400 {{ $errors->has('email') ? 'border-red-300 focus:border-red-400 focus:ring-red-300/60' : 'border-slate-300 dark:border-slate-700' }}"
                                type="email"
                                name="email"
                                :value="old('email')"
                                placeholder="seuemail@escola.com"
                                required
                                autofocus
                                autocomplete="username"
                            />
                            @error('email')
                                <p class="mt-1.5 text-xs text-red-500">{{ $message }}</p>
                            @enderror
                        </div>

                        <div>
                            <x-input-label for="password" :value="__('Senha')" class="text-sm font-medium text-slate-600 dark:text-slate-300" />
                            <x-text-input
                                id="password"
                                class="mt-1.5 block w-full rounded-xl border bg-white px-3 py-2.5 text-base text-slate-900 placeholder:text-slate-400 transition duration-200 focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/60 dark:bg-slate-800 dark:text-slate-100 dark:placeholder:text-slate-400 {{ $errors->has('password') ? 'border-red-300 focus:border-red-400 focus:ring-red-300/60' : 'border-slate-300 dark:border-slate-700' }}"
                                type="password"
                                name="password"
                                placeholder="••••••••••••"
                                required
                                autocomplete="current-password"
                            />
                            @error('password')
                                <p class="mt-1.5 text-xs text-red-500">{{ $message }}</p>
                            @enderror
                        </div>

                        <div class="flex items-center justify-between gap-3">
                            <label for="remember_me" class="inline-flex items-center gap-2 text-sm text-slate-600 dark:text-slate-300">
                                <input id="remember_me" type="checkbox" class="rounded border-slate-300 text-indigo-600 shadow-sm focus:ring-indigo-500 dark:border-slate-600 dark:bg-slate-800" name="remember" @checked(old('remember'))>
                                <span>Lembrar-me</span>
                            </label>

                            @if (Route::has('password.request'))
                                <a class="text-sm font-medium text-indigo-600 transition hover:text-indigo-700 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2 dark:text-indigo-400 dark:hover:text-indigo-300 dark:focus:ring-offset-slate-900" href="{{ route('password.request') }}">
                                    Esqueci minha senha
                                </a>
                            @endif
                        </div>

                        <button
                            id="login-submit"
                            type="submit"
                            class="inline-flex w-full items-center justify-center rounded-xl bg-gradient-to-r from-indigo-600 to-teal-600 px-4 py-3 text-sm font-semibold text-white shadow-md transition-all duration-200 hover:scale-105 hover:shadow-lg active:scale-[0.99] focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-indigo-500/60 focus-visible:ring-offset-2 focus-visible:ring-offset-white disabled:cursor-not-allowed disabled:opacity-75 disabled:hover:scale-100 dark:focus-visible:ring-offset-slate-900"
                            data-loading-text="Entrando..."
                        >
                            <svg id="login-spinner" class="mr-2 hidden h-4 w-4 animate-spin" viewBox="0 0 24 24" fill="none">
                                <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                                <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v4a4 4 0 00-4 4H4z"></path>
                            </svg>
                            <span id="login-button-text">Entrar</span>
                        </button>
                    </form>
                </div>
            </section>
        </main>

        <script>
            document.addEventListener('DOMContentLoaded', function () {
                document.querySelectorAll('[data-fade]').forEach((element, index) => {
                    const delay = Number(element.getAttribute('data-delay') || 80 + (index * 120));
                    element.classList.add('opacity-0', 'translate-y-2', 'transition-all', 'duration-700');
                    setTimeout(() => {
                        element.classList.remove('opacity-0', 'translate-y-2');
                    }, delay);
                });

                const form = document.getElementById('login-form');
                const submitButton = document.getElementById('login-submit');
                const spinner = document.getElementById('login-spinner');
                const buttonText = document.getElementById('login-button-text');
                if (form && submitButton && buttonText && spinner) {
                    form.addEventListener('submit', function () {
                        submitButton.setAttribute('disabled', 'disabled');
                        buttonText.textContent = submitButton.dataset.loadingText || 'Entrando...';
                        spinner.classList.remove('hidden');
                    });
                }

                const lockoutMessage = document.querySelector('[data-lockout-alert]');
                if (lockoutMessage && lockoutMessage.textContent.includes('bloqueada')) {
                    let minutes = 15;
                    const countdownElement = document.createElement('p');
                    countdownElement.className = 'mt-2 flex items-center text-xs font-semibold';
                    countdownElement.innerHTML = 'Tempo restante aproximado: <span id="lockout-countdown" class="ml-1">' + minutes + '</span> minutos';
                    lockoutMessage.appendChild(countdownElement);

                    function updateCountdown() {
                        const target = document.getElementById('lockout-countdown');
                        if (!target) {
                            return;
                        }

                        if (minutes <= 0) {
                            target.textContent = 'atualize a página';
                            return;
                        }

                        minutes -= 1;
                        target.textContent = String(minutes);
                        setTimeout(updateCountdown, 60000);
                    }

                    setTimeout(updateCountdown, 60000);
                }
            });
        </script>
    </body>
</html>
