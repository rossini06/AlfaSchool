<?php

namespace App\Http\Controllers\Admin;

use App\Http\Controllers\Controller;
use App\Models\User;
use Illuminate\Http\Request;
use Illuminate\Validation\Rule;
use Spatie\Permission\Models\Role;
use Spatie\Permission\Models\Permission;

class UserController extends Controller
{
    public function index()
    {
        $users = User::with('roles')->where('is_superadmin', false)->latest()->paginate(10);
        return view('admin.users.index', compact('users'));
    }

    public function create()
    {
        $roles = Role::all();
        $permissions = Permission::all()->groupBy(function ($item) {
            return explode('.', $item->name)[0];
        });
        return view('admin.users.create', compact('roles', 'permissions'));
    }

    public function store(Request $request)
    {
        
        $validated = $request->validate([
            'name' => 'required|string|max:255',
            'email' => 'required|string|email|max:255|unique:users',
            'password' => [
                'required',
                'string',
                'min:8',
                'confirmed',
                'regex:/[a-z]/',
                'regex:/[A-Z]/',
                'regex:/[0-9]/',
                'regex:/[@$!%*#?&]/',
            ],
            'roles' => 'nullable|array',
            'roles.*' => 'exists:roles,name',
            'permissions' => 'nullable|array',
            'permissions.*' => 'exists:permissions,name',
        ], [
            'password.regex' => 'A senha deve conter pelo menos uma letra maiúscula, uma minúscula, um número e um caractere especial (@$!%*#?&)',
        ]);

        $user = User::create([
            'name' => $validated['name'],
            'email' => $validated['email'],
            'password' => bcrypt($validated['password']),
        ]);

        if (!empty($validated['roles'])) {
            $user->assignRole($validated['roles']);
        }

        if (!empty($validated['permissions'])) {
            $user->givePermissionTo($validated['permissions']);
        }

        return redirect()->route('admin.users.index')
            ->with('success', 'Usuário criado com sucesso!');
    }

    public function edit(User $user)
    {
        // Proteger superadmin
        if ($user->isSuperAdmin() && auth()->id() !== $user->id) {
            return redirect()->route('admin.users.index')
                ->with('error', 'Você não pode editar o superadmin!');
        }
        
        $roles = Role::all();
        $userRoles = $user->roles->pluck('name')->toArray();
        
        $permissions = Permission::all()->groupBy(function ($item) {
            return explode('.', $item->name)[0];
        });
        $userPermissions = $user->getDirectPermissions()->pluck('name')->toArray();
        
        return view('admin.users.edit', compact('user', 'roles', 'userRoles', 'permissions', 'userPermissions'));
    }

    public function update(Request $request, User $user)
    {
        // Proteger superadmin
        if ($user->isSuperAdmin() && auth()->id() !== $user->id) {
            return back()->with('error', 'Você não pode editar o superadmin!')
                ->withInput();
        }
        
        $validated = $request->validate([
            'name' => 'required|string|max:255',
            'email' => 'required|string|email|max:255|' . Rule::unique('users')->ignore($user->id),
            'password' => [
                'nullable',
                'string',
                'min:8',
                'confirmed',
                'regex:/[a-z]/',
                'regex:/[A-Z]/',
                'regex:/[0-9]/',
                'regex:/[@$!%*#?&]/',
            ],
            'roles' => 'nullable|array',
            'roles.*' => 'exists:roles,name',
            'permissions' => 'nullable|array',
            'permissions.*' => 'exists:permissions,name',
        ], [
            'password.regex' => 'A senha deve conter pelo menos uma letra maiúscula, uma minúscula, um número e um caractere especial (@$!%*#?&)',
        ]);

        $user->update([
            'name' => $validated['name'],
            'email' => $validated['email'],
        ]);

        if (!empty($validated['password'])) {
            $user->update(['password' => bcrypt($validated['password'])]);
        }

        $user->syncRoles($validated['roles'] ?? []);
        $user->syncPermissions($validated['permissions'] ?? []);

        return redirect()->route('admin.users.index')
            ->with('success', 'Usuário atualizado com sucesso!');
    }

    public function destroy(User $user)
    {
        // Proteger superadmin
        if ($user->isSuperAdmin()) {
            return back()->with('error', 'O superadmin não pode ser excluído!');
        }
        
        if ($user->id === auth()->id()) {
            return back()->with('error', 'Você não pode excluir sua própria conta!');
        }

        $user->delete();

        return redirect()->route('admin.users.index')
            ->with('success', 'Usuário excluído com sucesso!');
    }
}
