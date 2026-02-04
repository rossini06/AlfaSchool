<?php

namespace App\Http\Controllers;

use Illuminate\Http\Request;

class DashboardController extends Controller
{
    public function index()
    {
        $user = auth()->user();
        $roles = $user->roles->pluck('name');
        $permissions = $user->getAllPermissions()->pluck('name');
        
        return view('dashboard', compact('user', 'roles', 'permissions'));
    }
}
