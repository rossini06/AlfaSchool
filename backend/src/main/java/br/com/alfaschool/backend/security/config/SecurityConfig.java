package br.com.alfaschool.backend.security.config;

import br.com.alfaschool.backend.security.filter.JwtAuthenticationFilter;
import br.com.alfaschool.backend.security.filter.TenantFilter;
import br.com.alfaschool.backend.security.jwt.JwtProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import br.com.alfaschool.backend.shared.response.ApiResponse;

import java.util.List;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${app.cors.allowed-origins:http://localhost,http://localhost:80,http://localhost:5173}")
    private String allowedOrigins;

    /**
     * Simulador de leitor Control iD. Quando ligado, expoe endpoints .fcgi
     * que imitam o firmware para permitir teste de ponta a ponta sem
     * hardware. NUNCA ligar em producao: e' um leitor falso que aceita
     * comando sem autenticar.
     */
    @Value("${app.access.simulador-habilitado:false}")
    private boolean simuladorHabilitado;

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final TenantFilter tenantFilter;
    private final ObjectMapper objectMapper;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, TenantFilter tenantFilter, ObjectMapper objectMapper) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.tenantFilter = tenantFilter;
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception
                    .authenticationEntryPoint(unauthorizedEntryPoint())
                    .accessDeniedHandler(accessDeniedHandler())
                )
                .authorizeHttpRequests(auth -> {
                    auth
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login", "/api/v1/auth/refresh").permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/api/v1/health").permitAll()
                        // metrics e info expunham JVM/HTTP a qualquer autenticado
                        // (ate a portaria e o token de agente). So' o superadmin.
                        .requestMatchers("/actuator/**").hasRole("SUPER_ADMIN")

                        // O firmware Control iD nao emite JWT: ele so' faz POST
                        // no endereco configurado. A autenticacao acontece
                        // dentro do controller, por token POR DISPOSITIVO
                        // (SHA-256 em dispositivos.webhook_token_hash, comparado
                        // em tempo constante) e falha fechada sem token. Um
                        // segredo global aqui deixaria qualquer um postar evento
                        // para qualquer escola.
                        .requestMatchers(HttpMethod.POST, "/api/v1/access/webhook/**").permitAll()

                        // Somente o login do agente. As demais rotas de
                        // /api/v1/access/agent/** continuam exigindo o token
                        // emitido aqui, com hasRole('AGENT').
                        .requestMatchers(HttpMethod.POST, "/api/v1/access/agent/login").permitAll()

                        // A TV do painel nao faz login: autentica por token
                        // proprio e revogavel, validado no controller.
                        .requestMatchers(HttpMethod.GET, "/api/v1/access/paineis/*/stream", "/api/v1/access/paineis/*/estado").permitAll()
                        // O botao "Preparar aluno para saida" da TV da sala.
                        // Preparar nao entrega crianca: o ato de
                        // responsabilidade e' ENTREGAR, que continua exigindo
                        // colaborador autenticado. A rota so' aceita retirada
                        // dentro do recorte daquele painel.
                        // Cuidado ao mexer: NAO usar /api/v1/access/paineis/**,
                        // que abriria o CRUD de painel e o resumo da
                        // coordenacao.
                        .requestMatchers(HttpMethod.POST, "/api/v1/access/paineis/*/retiradas/*/preparar").permitAll()

                        // Foto de referencia. Uma tag <img> nao manda
                        // cabecalho e a TV nao faz login, entao quem autoriza
                        // e' a assinatura HMAC da propria URL, com prazo de
                        // minutos e presa a chave da foto. Sem assinatura
                        // valida, o endpoint devolve 404.
                        .requestMatchers(HttpMethod.GET, "/api/v1/access/fotos/**").permitAll();

                    if (simuladorHabilitado) {
                        // Firmware falso do simulador, so' em laboratorio.
                        auth.requestMatchers("/login.fcgi", "/create_objects.fcgi", "/user_set_image.fcgi",
                                "/destroy_objects.fcgi", "/delete_objects.fcgi", "/load_objects.fcgi",
                                "/execute_actions.fcgi", "/user_destroy_image.fcgi").permitAll();
                    }

                    auth.anyRequest().authenticated();
                })
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(tenantFilter, JwtAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationEntryPoint unauthorizedEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(401);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            ApiResponse<Object> payload = ApiResponse.of(401, "Não autorizado.", null);
            response.getWriter().write(objectMapper.writeValueAsString(payload));
        };
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setStatus(403);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            ApiResponse<Object> payload = ApiResponse.of(403, "Acesso negado.", null);
            response.getWriter().write(objectMapper.writeValueAsString(payload));
        };
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(allowedOrigins.split(",")));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
