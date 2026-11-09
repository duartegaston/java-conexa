package com.bookhub.api.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro de seguridad que intercepta CADA request HTTP y valida el token JWT.
 *
 * <p>Este filtro se ejecuta en la cadena de filtros de Spring Security ANTES de que
 * la request llegue al Controller. El flujo es:</p>
 * <ol>
 *   <li>Extrae el token del header "Authorization: Bearer &lt;token&gt;"</li>
 *   <li>Valida la firma y expiración del token</li>
 *   <li>Carga los datos del usuario desde la base de datos</li>
 *   <li>Establece la autenticación en el SecurityContext</li>
 * </ol>
 *
 * <p>Si el token no existe o es inválido, la cadena continúa sin autenticación
 * y Spring Security bloqueará el acceso a endpoints protegidos.</p>
 */
// @Component: Spring registra este filtro como bean y lo agrega automáticamente a la filter chain
// @RequiredArgsConstructor: genera constructor con todos los campos 'final' (inyección por constructor)
@Component
@RequiredArgsConstructor
@Slf4j
// OncePerRequestFilter: garantiza que este filtro se ejecute exactamente una vez por request
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;

    /**
     * Lógica principal del filtro: extrae, valida y procesa el token JWT de cada request.
     *
     * @param request     request HTTP entrante
     * @param response    response HTTP que se enviará al cliente
     * @param filterChain cadena de filtros; se debe llamar a chain.doFilter() para continuar
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        try {
            // Paso 1: Extraer el token JWT del header de la request
            String jwt = extractJwtFromRequest(request);

            // Paso 2: Validar el token (si existe)
            if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)) {

                // Paso 3: Extraer el email del subject del token
                String email = jwtTokenProvider.getEmailFromToken(jwt);

                // Paso 4: Cargar el UserDetails desde la BD (verifica que el usuario sigue existiendo)
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                // Paso 5: Crear el objeto de autenticación de Spring Security
                // UsernamePasswordAuthenticationToken: representa una autenticación exitosa
                // null como credenciales: no necesitamos la contraseña después de verificar el JWT
                UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()  // roles del usuario para autorización
                    );

                // Agrega detalles adicionales de la request (IP, session ID) al objeto de auth
                authentication.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request)
                );

                // Paso 6: Establecer la autenticación en el SecurityContext
                // SecurityContextHolder: almacena la info de seguridad del thread actual
                // A partir de aquí, Spring Security sabe que el usuario está autenticado
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception ex) {
            // Si algo falla durante la validación del token, simplemente no autenticamos
            // Spring Security se encargará de devolver 401 o 403 al cliente
            log.error("No se pudo establecer la autenticación: {}", ex.getMessage());
        }

        // Paso 7: Continuar con el siguiente filtro en la cadena (siempre se debe llamar)
        filterChain.doFilter(request, response);
    }

    /**
     * Extrae el token JWT del header "Authorization" de la request.
     *
     * <p>El estándar Bearer Token requiere el formato: {@code Authorization: Bearer <token>}</p>
     *
     * @param request request HTTP de la cual extraer el token
     * @return el token JWT sin el prefijo "Bearer ", o null si no hay token
     */
    private String extractJwtFromRequest(HttpServletRequest request) {
        // Obtenemos el valor del header "Authorization"
        String bearerToken = request.getHeader("Authorization");

        // Verificamos que el header existe y tiene el prefijo correcto "Bearer "
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            // Retornamos solo el token, sin el prefijo "Bearer " (7 caracteres)
            return bearerToken.substring(7);
        }

        return null;
    }
}
