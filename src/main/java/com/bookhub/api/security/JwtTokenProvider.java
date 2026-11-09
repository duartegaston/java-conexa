package com.bookhub.api.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * Componente responsable de la generación, validación y extracción de datos de tokens JWT.
 *
 * <p>Un JWT (JSON Web Token) tiene tres partes separadas por puntos:</p>
 * <ol>
 *   <li><b>Header</b>: algoritmo de firma (ej: HS256) y tipo de token</li>
 *   <li><b>Payload</b>: claims (datos del usuario: sub, roles, fecha de expiración)</li>
 *   <li><b>Signature</b>: firma HMAC-SHA256 del header + payload con la clave secreta</li>
 * </ol>
 *
 * <p>Formato: {@code eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyQGVtYWlsLmNvbSJ9.FIRMA}</p>
 */
// @Component: registra esta clase como un bean de Spring (componente genérico)
// Spring la instanciará y la inyectará donde se necesite con @Autowired o constructor injection
@Component
@Slf4j
public class JwtTokenProvider {

    // @Value: inyecta el valor de la propiedad desde application.yml
    // Si la propiedad no existe, el valor entre ':' es el default
    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpirationMs;

    /**
     * Genera un token JWT para el usuario autenticado.
     *
     * <p>El token incluye como "subject" el email del usuario,
     * la fecha de emisión y la fecha de expiración.</p>
     *
     * @param authentication objeto de Spring Security con la info del usuario autenticado
     * @return token JWT firmado como String
     */
    public String generateToken(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return generateTokenFromEmail(userDetails.getUsername());
    }

    /**
     * Genera un token JWT a partir del email del usuario directamente.
     *
     * @param email email del usuario que se incluye como subject del token
     * @return token JWT firmado
     */
    public String generateTokenFromEmail(String email) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        // Jwts.builder(): patrón builder de la librería jjwt para construir el JWT
        return Jwts.builder()
                // subject: identificador principal del usuario en el token (usamos el email)
                .subject(email)
                // issuedAt: fecha y hora de creación del token
                .issuedAt(now)
                // expiration: fecha y hora de expiración (después de esto el token es inválido)
                .expiration(expiryDate)
                // signWith: firma el token con la clave secreta usando HMAC-SHA256
                .signWith(getSigningKey())
                // compact: construye y serializa el token como String
                .compact();
    }

    /**
     * Extrae el email del usuario desde el payload del token JWT.
     *
     * @param token token JWT del cual extraer el email
     * @return email del usuario almacenado en el campo "subject" del token
     */
    public String getEmailFromToken(String token) {
        // parseSignedClaims: parsea y verifica la firma del token
        // getPayload(): devuelve los claims (datos) del token
        // getSubject(): devuelve el campo "sub" (subject) que guardamos como email
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    /**
     * Valida si un token JWT es correcto: firma válida y no expirado.
     *
     * @param token token JWT a validar
     * @return true si el token es válido, false en cualquier otro caso
     */
    public boolean validateToken(String token) {
        try {
            // Si el parseo y verificación de firma tienen éxito, el token es válido
            Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException ex) {
            log.warn("Token JWT expirado: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            log.warn("Token JWT malformado: {}", ex.getMessage());
        } catch (SecurityException ex) {
            log.warn("Firma JWT inválida: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.warn("Token JWT vacío o nulo: {}", ex.getMessage());
        }
        return false;
    }

    /**
     * Construye la clave de firma HMAC-SHA256 a partir del secreto configurado.
     * La clave se genera desde el secreto codificado en Base64.
     *
     * @return SecretKey para firmar y verificar tokens
     */
    private SecretKey getSigningKey() {
        // Decoders.BASE64.decode: decodifica el secreto de Base64 a bytes
        // Keys.hmacShaKeyFor: crea una clave HMAC-SHA adecuada (HS256, HS384 o HS512 según longitud)
        byte[] keyBytes = Decoders.BASE64.decode(
            java.util.Base64.getEncoder().encodeToString(jwtSecret.getBytes())
        );
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
