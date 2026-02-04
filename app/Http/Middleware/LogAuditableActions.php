<?php

namespace App\Http\Middleware;

use Closure;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Auth;
use Illuminate\Support\Facades\Log;
use Symfony\Component\HttpFoundation\Response;

class LogAuditableActions
{
    /**
     * Handle an incoming request.
     *
     * @param  \Closure(\Illuminate\Http\Request): (\Symfony\Component\HttpFoundation\Response)  $next
     */
    public function handle(Request $request, Closure $next): Response
    {
        $user = Auth::user();
        
        $response = $next($request);
        
        $auditableActions = [
            'users.create' => 'Criar usuário',
            'users.update' => 'Editar usuário',
            'users.delete' => 'Excluir usuário',
            'roles.update' => 'Editar permissões de perfil',
            'login' => 'Login',
            'logout' => 'Logout',
        ];
        
        $routeName = $request->route()?->getName();
        
        if ($user && $routeName && isset($auditableActions[$routeName])) {
            $ip = $request->ip();
            $userAgent = $request->userAgent();
            
            $context = [
                'user_id' => $user->id,
                'user_email' => $user->email,
                'user_roles' => $user->roles->pluck('name')->toArray(),
                'action' => $auditableActions[$routeName],
                'route' => $routeName,
                'method' => $request->method(),
                'ip' => $ip,
                'user_agent' => $userAgent,
                'url' => $request->fullUrl(),
                'timestamp' => now()->toIso8601String(),
            ];
            
            if ($routeName === 'login' || $routeName === 'logout') {
                Log::channel('audit')->info('Authentication event', $context);
            } else {
                Log::channel('audit')->info('Auditable action', $context);
            }
        }
        
        return $response;
    }
}
