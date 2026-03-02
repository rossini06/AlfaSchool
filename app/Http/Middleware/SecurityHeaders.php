<?php

namespace App\Http\Middleware;

use Closure;
use Illuminate\Http\Request;
use Symfony\Component\HttpFoundation\Response;

class SecurityHeaders
{
    private function parseViteOriginFromHotFile(): ?string
    {
        $hotFile = public_path('hot');

        if (! is_file($hotFile)) {
            return null;
        }

        $url = trim((string) file_get_contents($hotFile));

        if ($url === '') {
            return null;
        }

        $parts = parse_url($url);

        if (! isset($parts['scheme'], $parts['host'])) {
            return null;
        }

        $origin = $parts['scheme'].'://'.$parts['host'];

        if (isset($parts['port'])) {
            $origin .= ':'.$parts['port'];
        }

        return $origin;
    }

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
            $hotOrigin = $this->parseViteOriginFromHotFile();

            $viteHttpSources = [
                'http://127.0.0.1:5173',
                'http://localhost:5173',
                'http://127.0.0.1:5174',
                'http://localhost:5174',
                'http://[::1]:5173',
                'http://[::1]:5174',
            ];

            if ($hotOrigin) {
                $viteHttpSources[] = $hotOrigin;
            }

            $viteWsSources = [
                'ws://127.0.0.1:5173',
                'ws://localhost:5173',
                'ws://127.0.0.1:5174',
                'ws://localhost:5174',
                'ws://[::1]:5173',
                'ws://[::1]:5174',
            ];

            if ($hotOrigin && str_starts_with($hotOrigin, 'http://')) {
                $viteWsSources[] = 'ws://'.substr($hotOrigin, 7);
            }

            if ($hotOrigin && str_starts_with($hotOrigin, 'https://')) {
                $viteWsSources[] = 'wss://'.substr($hotOrigin, 8);
            }

            $scriptSrc = array_merge($scriptSrc, $viteHttpSources);
            $styleSrc = array_merge($styleSrc, $viteHttpSources);
            $connectSrc = array_merge($connectSrc, $viteHttpSources, $viteWsSources);
        }

        $scriptSrcList = implode(' ', array_unique($scriptSrc));
        $styleSrcList = implode(' ', array_unique($styleSrc));
        $connectSrcList = implode(' ', array_unique($connectSrc));
        $frameSrcList = implode(' ', array_unique($frameSrc));

        $csp = sprintf(
            "default-src 'self'; script-src %s; script-src-elem %s; style-src %s; style-src-elem %s; img-src 'self' data: https:; font-src 'self' data: https://fonts.bunny.net; connect-src %s; frame-src %s; form-action 'self';",
            $scriptSrcList,
            $scriptSrcList,
            $styleSrcList,
            $styleSrcList,
            $connectSrcList,
            $frameSrcList
        );
        $response->headers->set('Content-Security-Policy', $csp);

        return $response;
    }
}
