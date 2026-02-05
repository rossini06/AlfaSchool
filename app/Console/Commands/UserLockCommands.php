<?php

namespace App\Console\Commands;

use App\Models\User;
use Illuminate\Console\Command;

class UserLockCommands extends Command
{
    /**
     * The name and signature of the console command.
     */
    protected $signature = 'users:locked {--all : Listar todos os bloqueados}';

    /**
     * The console command description.
     */
    protected $description = 'Gerenciar bloqueios de usuários';

    /**
     * Execute the console command.
     */
    public function handle(): int
    {
        if ($this->option('all')) {
            return $this->listAllLocked();
        }

        $this->info('=== Comandos de Gerenciamento de Bloqueios ===');
        $this->comment('Comandos disponíveis:');
        $this->line('  php artisan users:locked --all    # Lista todos os usuários bloqueados');
        $this->line('  php artisan users:clear-locks   # Limpa bloqueios expirados');
        $this->line('  php artisan users:unlock {email} # Desbloqueia usuário específico');
        $this->line('  php artisan users:reset {email}  # Zera tentativas de login');

        return Command::SUCCESS;
    }

    /**
     * Listar todos os usuários bloqueados
     */
    private function listAllLocked(): int
    {
        $users = User::locked()->get();

        if ($users->isEmpty()) {
            $this->info('Nenhum usuário bloqueado no momento.');
            return Command::SUCCESS;
        }

        $this->info("=== Usuários Bloqueados ({$users->count()}) ===");
        $headers = ['Email', 'Nome', 'Bloqueado Desde', 'Tempo Restante'];
        $rows = [];

        foreach ($users as $user) {
            $lockedSince = $user->locked_until ? $user->locked_until->diffForHumans() : 'N/A';
            $remaining = $user->getLockoutTimeRemaining();

            if ($remaining > 60) {
                $timeRemaining = round($remaining / 60) . ' hora(s)';
            } elseif ($remaining > 1) {
                $timeRemaining = $remaining . ' minutos';
            } else {
                $timeRemaining = 'Menos de 1 minuto';
            }

            $rows[] = [
                $user->email,
                $user->name,
                $lockedSince,
                $timeRemaining,
            ];
        }

        $this->table($headers, $rows);

        return Command::SUCCESS;
    }
}
