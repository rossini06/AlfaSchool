package br.com.alfaschool.backend.security.filter;

import br.com.alfaschool.backend.security.jwt.AuthenticatedUser;
import br.com.alfaschool.backend.security.jwt.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Claims claims = jwtTokenProvider.parseToken(token);
                if ("access".equals(claims.get("type", String.class))) {
                    UUID userId = UUID.fromString(claims.get("userId", String.class));
                    UUID tenantId = UUID.fromString(claims.get("tenantId", String.class));
                        String unitIdRaw = claims.get("unitId", String.class);
                        UUID unitId = unitIdRaw == null || unitIdRaw.isBlank() ? null : UUID.fromString(unitIdRaw);
                        Object rawRoles = claims.get("roles");
                        List<String> roles = rawRoles instanceof List<?> list
                            ? list.stream().map(String::valueOf).toList()
                            : List.of();

                    List<SimpleGrantedAuthority> authorities = roles.stream()
                            .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                            .map(SimpleGrantedAuthority::new)
                            .toList();

                    AuthenticatedUser principal = new AuthenticatedUser(userId, tenantId, unitId, roles);
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(principal, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception ignored) {
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
