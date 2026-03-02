<?php

namespace Database\Seeders;

use App\Models\User;
use Illuminate\Database\Seeder;
use Spatie\Permission\Models\Role;

class SuperAdminSeeder extends Seeder
{
    /**
     * Run the database seeds.
     */
    public function run(): void
    {
        $superadmin = User::withTrashed()->updateOrCreate(
            ['email' => 'superadmin@alfaschool.com'],
            [
                'name' => 'Super Admin',
                'password' => bcrypt('SuperAdmin@2024!@#$'),
                'is_superadmin' => true,
                'email_verified_at' => now(),
                'deleted_at' => null,
                'login_attempts' => 0,
                'locked_until' => null,
            ]
        );

        if (! Role::where('name', 'admin')->where('guard_name', 'web')->exists()) {
            Role::create(['name' => 'admin', 'guard_name' => 'web']);
        }

        if (! $superadmin->hasRole('admin')) {
            $superadmin->assignRole('admin');
        }

        $this->command->info('Superadmin configurado com sucesso!');
        $this->command->info('Email: superadmin@alfaschool.com');
        $this->command->info('Senha: SuperAdmin@2024!@#$');
    }
}
