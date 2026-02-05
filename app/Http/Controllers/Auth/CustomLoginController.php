<?php

namespace App\Http\Controllers\Auth;

use App\Http\Controllers\Controller;
use Illuminate\Foundation\Support\Providers\RouteServiceProvider;
use Illuminate\Http\RedirectResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Auth;
use Illuminate\Validation\ValidationException;
use App\Traits\LoginLockout;

class CustomLoginController extends Controller
{
    use LoginLockout;

    /**
     * Display the login view.
     */
    public function create(): \Illuminate\View\View
    {
        return view('auth.login');
    }

    /**
     * Handle an incoming authentication request.
     */
    public function store(Request $request): RedirectResponse
    {
        $request->validate([
            'email' => ['required', 'string', 'email'],
            'password' => ['required', 'string'],
        ]);

        $user = \App\Models\User::where('email', $request->email)->first();

        // Verificar se usuário está bloqueado
        if ($user && $user->isLockedOut()) {
            $remaining = $user->getLockoutTimeRemaining();
            
            if ($remaining > 60) {
                $hours = round($remaining / 60);
                $message = "Conta bloqueada por {$hours} hora(s). Tente novamente mais tarde.";
            } elseif ($remaining > 1) {
                $message = "Conta bloqueada por {$remaining} minutos. Tente novamente mais tarde.";
            } else {
                $message = "Conta bloqueada. Tente novamente em 1 minuto.";
            }

            return back()->withErrors([
                'email' => $message,
            ])->onlyInput('email');
        }

        // Attempt to authenticate the user
        $credentials = $request->only('email', 'password');

        if (Auth::attempt($credentials, $request->boolean('remember'))) {
            $request->session()->regenerate();

            // Resetar tentativas de login se for o usuário correto
            if ($user) {
                $user->resetLoginAttempts();
            }

            return redirect()->intended('/dashboard');
        }

        // Login falhou - incrementar tentativas
        if ($user) {
            $user->incrementLoginAttempts();

            if ($user->isLockedOut()) {
                $remaining = $user->getLockoutTimeRemaining();
                
                if ($remaining > 60) {
                    $hours = round($remaining / 60);
                    $message = "Conta bloqueada por {$hours} hora(s) devido a tentativas incorretas.";
                } elseif ($remaining > 1) {
                    $message = "Conta bloqueada por {$remaining} minutos.";
                } else {
                    $message = "Conta bloqueada. Tente novamente em 1 minuto.";
                }

                return back()->withErrors([
                    'email' => $message,
                ])->onlyInput('email');
            }

            $attempts = $user->login_attempts;
            $remaining = 5 - $attempts;

            if ($remaining > 0) {
                return back()->withErrors([
                    'email' => "Email ou senha incorretos. Você tem {$remaining} tentativas restantes antes do bloqueio.",
                ])->onlyInput('email');
            }
        }

        throw ValidationException::withMessages([
            'email' => trans('auth.failed'),
        ]);
    }

    /**
     * Destroy an authenticated session.
     */
    public function destroy(Request $request): RedirectResponse
    {
        Auth::guard('web')->logout();

        $request->session()->invalidate();

        $request->session()->regenerateToken();

        return redirect('/');
    }
}
