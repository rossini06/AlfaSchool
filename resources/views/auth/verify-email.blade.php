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
    <body class="min-h-screen bg-gradient-to-br from-indigo-700 via-indigo-600 to-blue-600 font-sans antialiased">
        @php($schoolName = config('app.school_name', 'AlfaSchool'))

        <main class="grid min-h-screen grid-cols-1 md:grid-cols-2">
            <section class="relative z-10 flex flex-col justify-between p-6 text-white sm:p-8 md:p-12 lg:p-16" data-fade data-delay="60">
                <div>
                    <div class="flex justify-center md:justify-start">
                        <a href="{{ url('/') }}" class="inline-flex items-center rounded-xl bg-white/10 px-4 py-2 ring-1 ring-white/30 backdrop-blur-sm">
                            <x-application-logo class="h-10 w-auto max-w-[150px] object-contain" />
                        </a>
                    </div>

                    <div class="mx-auto mt-8 max-w-2xl text-center md:mx-0 md:mt-10 md:text-left">
                        <span class="inline-flex items-center rounded-full bg-white/15 px-3 py-1 text-xs font-semibold uppercase tracking-wider text-indigo-50 ring-1 ring-white/20">
                            Verificação de Conta
                        </span>

                        <h1 class="mt-4 text-3xl font-bold leading-[1.08] tracking-tight sm:text-4xl lg:text-5xl">
                            Segurança Ativa <span class="font-semibold text-indigo-100">em Cada Acesso</span>
                        </h1>
                        <p class="mt-3 text-base text-indigo-100/95 sm:text-lg sm:leading-relaxed">
                            Confirme seu e-mail para proteger sua conta e liberar o acesso completo à plataforma.
                        </p>
                    </div>
                </div>

                <div class="mt-10 space-y-3 text-sm text-indigo-100 md:mt-12">
                    <p class="rounded-lg bg-white/10 px-4 py-3 ring-1 ring-white/25">
                        Acesso restrito por perfil: Admin, Gestor, Professor, Secretaria e Aluno.
                    </p>
                    <p class="text-xs text-indigo-100/80">© {{ date('Y') }} AlfaSchool. Todos os direitos reservados.</p>
                </div>
            </section>

            <section class="relative flex items-center justify-center overflow-hidden bg-gradient-to-br from-slate-100 via-white to-white p-6 sm:p-10" data-fade data-delay="180">
                <div class="pointer-events-none absolute -right-20 -top-20 h-72 w-72 rounded-full bg-indigo-300/25 blur-3xl"></div>
                <div class="pointer-events-none absolute -bottom-24 -left-20 h-80 w-80 rounded-full bg-blue-300/20 blur-3xl"></div>
                <div class="pointer-events-none absolute inset-0 bg-[radial-gradient(circle_at_85%_15%,rgba(4,156,172,0.13),transparent_42%)]"></div>

                <div class="relative w-full max-w-md rounded-2xl bg-white/95 p-9 shadow-2xl ring-1 ring-slate-200" style="max-width: 420px;">
                    <div class="mb-8">
                        <p class="text-xs font-semibold uppercase tracking-wider text-indigo-600">{{ $schoolName }}</p>
                        <h2 class="mt-2 text-2xl font-bold text-slate-900">Verificação de e-mail</h2>
                        <p class="mt-1 text-sm text-slate-600">Confirme seu endereço para continuar no sistema</p>
                    </div>

                    <div class="mb-4 rounded-lg border border-slate-200 bg-slate-50 p-3 text-sm text-slate-600">
                        {{ __('Obrigado por se cadastrar! Antes de começar, confirme seu e-mail clicando no link que acabamos de enviar. Se não recebeu o e-mail, podemos enviar novamente.') }}
                    </div>

                    @if (session('status') == 'verification-link-sent')
                        <div class="mb-4 rounded-lg border border-emerald-200 bg-emerald-50 p-3 font-medium text-sm text-emerald-700">
                            {{ __('Um novo link de verificação foi enviado para o e-mail informado no cadastro.') }}
                        </div>
                    @endif

                    <div class="mt-6 space-y-3">
                        <form method="POST" action="{{ route('verification.send') }}" id="verify-email-form">
                            @csrf

                            <button
                                id="verify-email-submit"
                                type="submit"
                                class="inline-flex w-full items-center justify-center rounded-xl bg-gradient-to-r from-indigo-600 to-blue-600 px-4 py-3 text-sm font-semibold text-white shadow-md transition duration-200 hover:scale-[1.03] hover:shadow-lg focus:outline-none focus:ring-4 focus:ring-indigo-300/70 focus:ring-offset-2 focus:ring-offset-white disabled:cursor-not-allowed disabled:opacity-75 disabled:hover:scale-100"
                                data-loading-text="Reenviando..."
                            >
                                <svg id="verify-email-spinner" class="mr-2 hidden h-4 w-4 animate-spin" viewBox="0 0 24 24" fill="none">
                                    <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                                    <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8v4a4 4 0 00-4 4H4z"></path>
                                </svg>
                                <span id="verify-email-button-text">Reenviar e-mail de verificação</span>
                            </button>
                        </form>

                        <form method="POST" action="{{ route('logout') }}">
                            @csrf

                            <button type="submit" class="inline-flex w-full items-center justify-center rounded-xl border border-indigo-200 bg-indigo-50 px-4 py-3 text-sm font-semibold text-indigo-700 transition duration-200 hover:bg-indigo-100 focus:outline-none focus:ring-2 focus:ring-indigo-400 focus:ring-offset-2">
                                {{ __('Sair') }}
                            </button>
                        </form>
                    </div>
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

                const form = document.getElementById('verify-email-form');
                const submitButton = document.getElementById('verify-email-submit');
                const spinner = document.getElementById('verify-email-spinner');
                const buttonText = document.getElementById('verify-email-button-text');

                if (form && submitButton && buttonText && spinner) {
                    form.addEventListener('submit', function () {
                        submitButton.setAttribute('disabled', 'disabled');
                        buttonText.textContent = submitButton.dataset.loadingText || 'Reenviando...';
                        spinner.classList.remove('hidden');
                    });
                }
            });
        </script>
    </body>
</html>
