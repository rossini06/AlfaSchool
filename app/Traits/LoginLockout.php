<?php

namespace App\Traits;

use Carbon\Carbon;

trait LoginLockout
{
    /**
     * Verificar se usuário está bloqueado
     */
    public function isLockedOut(): bool
    {
        if (!$this->locked_until) {
            return false;
        }

        return Carbon::now()->isBefore($this->locked_until);
    }

    /**
     * Obter tempo restante de bloqueio em minutos
     */
    public function getLockoutTimeRemaining(): ?int
    {
        if (!$this->locked_until) {
            return null;
        }

        if (Carbon::now()->isAfter($this->locked_until)) {
            return 0;
        }

        return Carbon::now()->diffInMinutes($this->locked_until);
    }

    /**
     * Obter tempo de bloqueio baseado nas tentativas
     */
    public function getLockoutDuration(): int
    {
        $attempts = $this->login_attempts;

        if ($attempts <= 5) {
            return 15; // 15 minutos
        } elseif ($attempts <= 10) {
            return 60; // 1 hora
        } else {
            return 1440; // 24 horas
        }
    }

    /**
     * Incrementar tentativas de login
     */
    public function incrementLoginAttempts(): void
    {
        $this->increment('login_attempts');
        $this->update(['last_failed_at' => now()]);

        $this->setLockoutTime();
    }

    /**
     * Definir tempo de bloqueio
     */
    public function setLockoutTime(): void
    {
        $duration = $this->getLockoutDuration();
        $this->update(['locked_until' => now()->addMinutes($duration)]);
    }

    /**
     * Resetar tentativas de login
     */
    public function resetLoginAttempts(): void
    {
        $this->update([
            'login_attempts' => 0,
            'locked_until' => null,
            'last_failed_at' => null,
        ]);
    }

    /**
     * Desbloquear usuário
     */
    public function unlock(): void
    {
        $this->resetLoginAttempts();
    }

    /**
     * Obter mensagem de bloqueio
     */
    public function getLockoutMessage(): string
    {
        $attempts = $this->login_attempts;
        $remaining = $this->getLockoutTimeRemaining();

        if ($attempts <= 5) {
            return "Conta bloqueada por {$remaining} minutos devido a tentativas de login incorretas.";
        } elseif ($attempts <= 10) {
            return "Conta bloqueada por " . ($remaining / 60) . " hora(s) devido a tentativas de login incorretas.";
        } else {
            return "Conta bloqueada por 24 horas devido a múltiplas tentativas de login incorretas. Por favor, entre em contato com o suporte.";
        }
    }

    /**
     * Scope para usuários bloqueados
     */
    public function scopeLocked($query)
    {
        return $query->whereNotNull('locked_until')
            ->where('locked_until', '>', now());
    }

    /**
     * Scope para usuários desbloqueados
     */
    public function scopeNotLocked($query)
    {
        return $query->whereNull('locked_until')
            ->orWhere('locked_until', '<=', now());
    }

    /**
     * Limpar bloqueios expirados
     */
    public static function clearExpiredLocks(): int
    {
        return static::whereNotNull('locked_until')
            ->where('locked_until', '<=', now())
            ->update([
                'login_attempts' => 0,
                'locked_until' => null,
                'last_failed_at' => null,
            ]);
    }

    /**
     * Obter usuários bloqueados
     */
    public static function getLockedUsers()
    {
        return static::locked()->get();
    }

    /**
     * Obter tentativas de login
     */
    public function getLoginAttemptsCount(): int
    {
        return $this->login_attempts;
    }

    /**
     * Verificar se precisa mostrar captcha
     */
    public function shouldShowCaptcha(): bool
    {
        return $this->login_attempts >= 3;
    }
}
