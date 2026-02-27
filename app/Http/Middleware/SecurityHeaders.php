<?php

namespace App\Http\Middleware;

use Closure;
use Illuminate\Http\Request;
use Symfony\Component\HttpFoundation\Response;

class SecurityHeaders
{
    /**
     * Handle an incoming request.
     *
     * @param  \Closure(\Illuminate\Http\Request): (\Symfony\Component\HttpFoundation\Response)  $next
     */
    public function handle(Request $request, Closure $next): Response
    {
        $response = $next($request);

        $response->headers->set('X-Content-Type-Options', 'nosniff');
        $response->headers->set('X-Frame-Options', 'SAMEORIGIN');
        $response->headers->set('X-XSS-Protection', '1; mode=block');
        $response->headers->set('Referrer-Policy', 'strict-origin-when-cross-origin');
        $response->headers->set('Permissions-Policy', 'geolocation=(), microphone=(), camera=()');
        $response->headers->set('Strict-Transport-Security', 'max-age=31536000; includeSubDomains');

        $scriptSrc = ["'self'", "'unsafe-inline'", "'unsafe-eval'"];
        $styleSrc = ["'self'", "'unsafe-inline'", 'https://fonts.bunny.net'];
        $connectSrc = ["'self'"];
        $frameSrc = ["'self'", 'http://localhost:*'];

        if (app()->isLocal()) {
            $viteHttpSources = [
                'http://127.0.0.1:5173',
                'http://localhost:5173',
                'http://127.0.0.1:5174',
                'http://localhost:5174',
                'http://[::1]:5173',
                'http://[::1]:5174',
            ];

            $viteWsSources = [
                'ws://127.0.0.1:5173',
                'ws://localhost:5173',
                'ws://127.0.0.1:5174',
                'ws://localhost:5174',
                'ws://[::1]:5173',
                'ws://[::1]:5174',
            ];

            $scriptSrc = array_merge($scriptSrc, $viteHttpSources);
            $styleSrc = array_merge($styleSrc, $viteHttpSources);
            $connectSrc = array_merge($connectSrc, $viteHttpSources, $viteWsSources);
        }

        $csp = sprintf(
            "default-src 'self'; script-src %s; style-src %s; img-src 'self' data: https:; font-src 'self' data: https://fonts.bunny.net; connect-src %s; frame-src %s; form-action 'self';",
            implode(' ', array_unique($scriptSrc)),
            implode(' ', array_unique($styleSrc)),
            implode(' ', array_unique($connectSrc)),
            implode(' ', array_unique($frameSrc))
        );
        $response->headers->set('Content-Security-Policy', $csp);

        return $response;
    }
}
