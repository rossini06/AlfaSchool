package br.com.alfaschool.backend.security.filter;

import br.com.alfaschool.backend.security.jwt.AuthenticatedUser;
import br.com.alfaschool.backend.shared.response.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class TenantFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;

    public TenantFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser principal) {
                TenantContext.setTenantId(principal.tenantId());
                String requestedTenant = request.getHeader("X-Tenant-Id");
                if (StringUtils.hasText(requestedTenant) && !principal.tenantId().equals(UUID.fromString(requestedTenant))) {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");
                        response.getWriter().write(objectMapper.writeValueAsString(
                            ApiResponse.of(403, "Acesso negado para o tenant informado.", null)
                    ));
                    return;
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
