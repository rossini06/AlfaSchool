<?php

namespace Database\Seeders;

use Illuminate\Database\Seeder;
use Spatie\Permission\Models\Role;
use Spatie\Permission\Models\Permission;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Schema;
use Spatie\Permission\PermissionRegistrar;

class RolePermissionSeeder extends Seeder
{
    public function run(): void
    {
        app(PermissionRegistrar::class)->forgetCachedPermissions();

        Schema::disableForeignKeyConstraints();

        DB::table('role_has_permissions')->delete();
        DB::table('model_has_roles')->delete();
        DB::table('model_has_permissions')->delete();
        Role::query()->delete();
        Permission::query()->delete();

        Schema::enableForeignKeyConstraints();

        $roles = [
            'admin' => 'Administrador do sistema - acesso total',
            'gestor' => 'Gestor escolar - gestão pedagógica e administrativa',
            'professor' => 'Professor - lançamento de notas e frequência',
            'secretaria' => 'Secretaria - cadastros e matrículas',
            'aluno' => 'Aluno - visualização de dados pessoais e acadêmicos',
        ];

        $permissions = [
            'users.view' => 'Visualizar usuários',
            'users.create' => 'Criar usuários',
            'users.edit' => 'Editar usuários',
            'users.delete' => 'Excluir usuários',
            'roles.view' => 'Visualizar perfis de usuário',
            'roles.edit' => 'Editar perfis de usuário',
            'alunos.view' => 'Visualizar alunos',
            'alunos.create' => 'Cadastrar alunos',
            'alunos.edit' => 'Editar alunos',
            'alunos.delete' => 'Excluir alunos',
            'turmas.view' => 'Visualizar turmas',
            'turmas.create' => 'Criar turmas',
            'turmas.edit' => 'Editar turmas',
            'turmas.delete' => 'Excluir turmas',
            'notas.view' => 'Visualizar notas',
            'notas.create' => 'Lançar notas',
            'notas.edit' => 'Editar notas',
            'frequencia.view' => 'Visualizar frequência',
            'frequencia.create' => 'Registrar frequência',
            'financeiro.view' => 'Visualizar financeiro',
            'financeiro.edit' => 'Gerenciar financeiro',
            'meus_dados.view' => 'Visualizar meus dados',
        ];

        foreach ($roles as $name => $description) {
            Role::create(['name' => $name, 'guard_name' => 'web']);
        }

        foreach ($permissions as $name => $description) {
            Permission::create(['name' => $name, 'guard_name' => 'web']);
        }

        $adminRole = Role::findByName('admin');
        $adminRole->givePermissionTo(Permission::all());

        $gestorRole = Role::findByName('gestor');
        $gestorRole->givePermissionTo([
            'alunos.view', 'alunos.create', 'alunos.edit',
            'turmas.view', 'turmas.create', 'turmas.edit',
            'notas.view', 'frequencia.view', 'financeiro.view',
            'meus_dados.view',
        ]);

        $professorRole = Role::findByName('professor');
        $professorRole->givePermissionTo([
            'alunos.view', 'turmas.view', 'notas.view', 'notas.create', 'notas.edit',
            'frequencia.view', 'frequencia.create', 'meus_dados.view',
        ]);

        $secretariaRole = Role::findByName('secretaria');
        $secretariaRole->givePermissionTo([
            'alunos.view', 'alunos.create', 'alunos.edit',
            'turmas.view', 'meus_dados.view',
        ]);

        $alunoRole = Role::findByName('aluno');
        $alunoRole->givePermissionTo(['meus_dados.view']);

        app(PermissionRegistrar::class)->forgetCachedPermissions();
    }
}
