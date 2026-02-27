<!DOCTYPE html>
<html lang="{{ str_replace('_', '-', app()->getLocale()) }}">
    <head>
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <meta name="csrf-token" content="{{ csrf_token() }}">

        <title>{{ config('app.name', 'AlfaSchool - Gestão Escolar') }}</title>

        <link rel="preconnect" href="https://fonts.bunny.net">
        <link href="https://fonts.bunny.net/css?family=figtree:400,500,600,700,800&display=swap" rel="stylesheet" />

        @vite(['resources/css/app.css', 'resources/js/app.js'])
    </head>
    <body class="bg-slate-50 font-sans text-slate-900 antialiased">
        <header class="z-40 border-b border-slate-200/80 bg-white">
            <div class="mx-auto flex w-full max-w-7xl flex-wrap items-center justify-between gap-3 px-4 py-4 sm:px-6 lg:px-8">
                <a href="{{ route('landing') }}" class="inline-flex items-center">
                    <x-application-logo class="h-12 w-auto max-w-[260px] object-contain object-left sm:h-14 sm:max-w-[320px]" />
                </a>

                <div class="ml-auto flex items-center gap-2 sm:gap-4">
                    <a
                        href="{{ route('login') }}"
                        class="text-sm font-semibold text-slate-700 transition hover:text-indigo-600"
                    >
                        Login
                    </a>
                    <a
                        href="#cta"
                        class="inline-flex items-center rounded-2xl bg-indigo-600 px-4 py-2 text-sm font-semibold text-white shadow-sm transition hover:bg-indigo-700 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-indigo-600"
                    >
                        Solicitar Demonstração
                    </a>
                </div>
            </div>
        </header>

        <main class="w-full">
            @yield('content')
        </main>
    </body>
</html>
