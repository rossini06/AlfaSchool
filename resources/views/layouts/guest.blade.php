<!DOCTYPE html>
<html lang="{{ str_replace('_', '-', app()->getLocale()) }}">
    <head>
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <meta name="csrf-token" content="{{ csrf_token() }}">

        <title>{{ config('app.name', 'AlfaSchool - Gestão Escolar') }}</title>

        <!-- Fonts -->
        <link rel="preconnect" href="https://fonts.bunny.net">
        <link href="https://fonts.bunny.net/css?family=figtree:400,500,600&display=swap" rel="stylesheet" />

        <!-- Scripts -->
        @php
            $hasViteBuild = is_file(public_path('build/manifest.json')) || is_file(public_path('hot'));
        @endphp
        @if ($hasViteBuild)
            @vite(['resources/css/app.css', 'resources/js/app.js'])
        @endif
    </head>
    <body class="font-sans text-gray-900 antialiased">
        <div class="min-h-screen flex flex-col sm:justify-center items-center pt-6 sm:pt-0 bg-gray-100">
            <div>
                <a href="/">
                    <x-application-logo class="h-24 w-auto object-contain" />
                </a>
            </div>

            <div class="w-full sm:max-w-md mt-6 px-6 py-4 bg-white shadow-md overflow-hidden sm:rounded-lg">
                {{ $slot }}
            </div>
        </div>

        <script>
            document.addEventListener('DOMContentLoaded', function() {
                const lockoutMessage = document.querySelector('.bg-red-100');
                if (lockoutMessage && lockoutMessage.textContent.includes('bloqueada')) {
                    let minutes = 15;
                    const countdownElement = document.createElement('div');
                    countdownElement.className = 'mt-2 text-sm font-bold flex items-center';
                    countdownElement.innerHTML = '<svg class="w-4 h-4 mr-1 animate-spin" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"/></svg>Tempo restante: <span id="lockout-countdown" class="ml-1">' + minutes + '</span> minutos';
                    lockoutMessage.appendChild(countdownElement);

                    function updateCountdown() {
                        if (minutes <= 0) {
                            document.getElementById('lockout-countdown').textContent = 'Atualize a página';
                            return;
                        }
                        minutes--;
                        document.getElementById('lockout-countdown').textContent = minutes;
                        setTimeout(updateCountdown, 60000);
                    }
                    setTimeout(updateCountdown, 60000);
                }
            });
        </script>
    </body>
</html>
