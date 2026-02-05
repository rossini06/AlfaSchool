<?php

namespace Database\Seeders;

use App\Models\User;
use Illuminate\Database\Seeder;

class SuperAdminSeeder extends Seeder
{
    /**
     * Run the database seeds.
     */
    public function run(): void
    {
        // Verificar se superadmin já existe
        $exists = User::where('email', 'superadmin@alfaschool.com')->exists();
        
        if (!$exists) {
            $superadmin = User::create([
                'name' => 'Super Admin',
                'email' => 'superadmin@alfaschool.com',
                'password' => bcrypt('SuperAdmin@2024!@#$'),
                'is_superadmin' => true,
                'email_verified_at' => now(),
            ]);
            
            // Atribuir perfil admin
            $superadmin->assignRole('admin');
            
            $this->command->info('Superadmin criado com sucesso!');
            $this->command->info('Email: superadmin@alfaschool.com');
            $this->command->info('Senha: SuperAdmin@2024!@#$');
        } else {
            $this->command->info('Superadmin já existe!');
        }
    }
}
