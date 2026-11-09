package com.bookhub.api.config;

import com.bookhub.api.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuración central de Spring Security.
 *
 * <p>Define qué endpoints son públicos, cuáles requieren autenticación, cuáles
 * requieren roles específicos, cómo se codifican contraseñas y cómo funciona
 * el proveedor de autenticación.</p>
 *
 * <p>En Spring Security 6 (Spring Boot 3.x) la configuración se hace con beans
 * en lugar de extender {@code WebSecurityConfigurerAdapter} (que fue eliminado).</p>
 */
// @Configuration: marca esta clase como fuente de definiciones de beans de Spring
@Configuration
// @EnableWebSecurity: habilita el soporte de Spring Security para la web
@EnableWebSecurity
// @EnableMethodSecurity: habilita las anotaciones de seguridad a nivel de método:
//   - @PreAuthorize: verifica permisos ANTES de ejecutar el método
//   - @PostAuthorize: verifica permisos DESPUÉS de ejecutar el método
//   - @Secured: alternativa más simple a @PreAuthorize
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;

    /**
     * Define las reglas de seguridad HTTP: qué URLs están protegidas, política de sesiones,
     * y en qué posición de la filter chain se inserta el filtro JWT.
     *
     * @param http objeto builder de HttpSecurity proporcionado por Spring
     * @return la SecurityFilterChain configurada
     */
    // @Bean: indica a Spring que el valor retornado por este método debe registrarse como un bean
    // Spring lo instancia una vez y lo reutiliza en toda la aplicación
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Deshabilitamos CSRF (Cross-Site Request Forgery) porque usamos JWT stateless.
            // CSRF protege sesiones basadas en cookies; con JWT no aplica.
            .csrf(AbstractHttpConfigurer::disable)

            // Configuramos las reglas de autorización por URL
            .authorizeHttpRequests(auth -> auth

                // Endpoints completamente públicos (no requieren token)
                .requestMatchers("/auth/**").permitAll()
                .requestMatchers("/books/search").permitAll()
                .requestMatchers(HttpMethod.GET, "/books/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/reviews/**").permitAll()

                // Swagger UI: público para que sea accesible sin autenticar
                .requestMatchers(
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs/**"
                ).permitAll()

                // H2 Console (solo en tests/desarrollo)
                .requestMatchers("/h2-console/**").permitAll()

                // Endpoints de administración: solo ROLE_ADMIN
                .requestMatchers("/admin/**").hasRole("ADMIN")

                // Cualquier otra request requiere autenticación
                .anyRequest().authenticated()
            )

            // Política de sesiones STATELESS: Spring Security NO crea ni usa sesiones HTTP.
            // Cada request es independiente y debe incluir el JWT para autenticarse.
            // Esto es esencial para APIs REST escalables (el servidor no guarda estado del cliente).
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // Registramos el AuthenticationProvider que define cómo se autentica un usuario
            .authenticationProvider(authenticationProvider())

            // Insertamos nuestro filtro JWT ANTES del filtro estándar de usuario/contraseña.
            // Así, el JWT se valida antes de intentar autenticación tradicional.
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)

            // Permite renderizar H2 Console (que usa iframes) en desarrollo
            .headers(headers ->
                headers.frameOptions(frameOptions -> frameOptions.sameOrigin())
            );

        return http.build();
    }

    /**
     * Define el proveedor de autenticación: cómo Spring Security carga y verifica usuarios.
     * DaoAuthenticationProvider es el proveedor estándar que usa BD a través de UserDetailsService.
     *
     * @return AuthenticationProvider configurado con UserDetailsService y PasswordEncoder
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        // DaoAuthenticationProvider: proveedor que carga usuarios desde un DAO (Data Access Object)
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        // Indica cómo cargar los usuarios (nuestra implementación que usa la BD)
        authProvider.setUserDetailsService(userDetailsService);
        // Indica cómo verificar contraseñas (BCrypt)
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /**
     * Expone el AuthenticationManager como bean para que los services puedan usarlo
     * para autenticar manualmente (en el login con email/contraseña).
     *
     * @param config configuración de autenticación de Spring
     * @return AuthenticationManager del contexto de Spring Security
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Define el codificador de contraseñas BCrypt.
     *
     * <p>BCrypt es el algoritmo recomendado para contraseñas: es unidireccional (no se puede
     * revertir), agrega sal automáticamente (evita rainbow tables) y es lento
     * intencionalmente (dificulta ataques de fuerza bruta).</p>
     *
     * @return BCryptPasswordEncoder con factor de costo por defecto (10 rondas)
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
