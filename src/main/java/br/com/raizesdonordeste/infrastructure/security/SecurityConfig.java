package br.com.raizesdonordeste.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;

/**
 * Configuração de segurança:
 *  - API stateless (sem sessão): cada requisição se autentica pelo JWT;
 *  - rotas públicas: login, cadastro de cliente, unidades/cardápio/produtos (GET) e Swagger;
 *  - demais rotas exigem token (401) e as regras por perfil ficam nos controllers com
 *    @PreAuthorize (403).
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;
    private final RespostaErroSeguranca respostaErro;

    public SecurityConfig(JwtAuthenticationFilter jwtFilter, RespostaErroSeguranca respostaErro) {
        this.jwtFilter = jwtFilter;
        this.respostaErro = respostaErro;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CSRF desativado: não usamos cookie de sessão, o token vai no header Authorization.
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .headers(h -> h.frameOptions(f -> f.sameOrigin())) // permite o console H2 (apenas dev)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(antMatcher("/h2-console/**")).permitAll()
                .requestMatchers(antMatcher("/swagger-ui/**"), antMatcher("/swagger-ui.html"),
                                 antMatcher("/v3/api-docs/**"), antMatcher("/error")).permitAll()
                .requestMatchers(antMatcher(HttpMethod.GET, "/")).permitAll()
                .requestMatchers(antMatcher(HttpMethod.POST, "/auth/login")).permitAll()
                .requestMatchers(antMatcher(HttpMethod.POST, "/usuarios")).permitAll()
                .requestMatchers(antMatcher(HttpMethod.GET, "/unidades/**")).permitAll()
                .requestMatchers(antMatcher(HttpMethod.GET, "/produtos/**")).permitAll()
                .anyRequest().authenticated())
            .exceptionHandling(e -> e
                .authenticationEntryPoint(respostaErro)
                .accessDeniedHandler(respostaErro))
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /** BCrypt com custo 10: hash lento e com salt (proteção contra força bruta e rainbow table). */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }
}
