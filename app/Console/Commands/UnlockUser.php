<?php

namespace App\Console\Commands;

use App\Models\User;
use Illuminate\Console\Command;

class UnlockUser extends Command
{
    /**
     * The name and signature of the console command.
     */
    protected $signature = 'users:unlock {email : Email do usuário}';

    /**
     * The console command description.
     */
    protected $description = 'Desbloqueia um usuário específico';

    /**
     * Execute the console command.
     */
    public function handle(): int
    {
        $email = $this->argument('email');

        $user = User::where('email', $email)->first();

        if (!$user) {
            $this->error("Usuário com email '{$email}' não encontrado.");
            return Command::FAILURE;
        }

        $user->unlock();

        $this->info("Usuário '{$email}' desbloqueado com sucesso!");
        $this->line("Tentativas resetadas: {$user->login_attempts}");

        return Command::SUCCESS;
    }
}
