<?php

namespace App\Console\Commands;

use App\Models\User;
use Illuminate\Console\Command;

class ClearUserLocks extends Command
{
    /**
     * The name and signature of the console command.
     */
    protected $signature = 'users:clear-locks';

    /**
     * The console command description.
     */
    protected $description = 'Limpa bloqueios expirados de todos os usuários';

    /**
     * Execute the console command.
     */
    public function handle(): int
    {
        $this->info('=== Limpando Bloqueios Expirados ===');

        $count = User::clearExpiredLocks();

        $this->info("Bloqueios limpos: {$count}");

        return Command::SUCCESS;
    }
}
