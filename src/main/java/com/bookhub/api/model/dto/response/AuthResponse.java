package com.bookhub.api.model.dto.response;

import com.bookhub.api.model.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta para los endpoints de autenticación (register y login).
 * Contiene el token JWT y la información básica del usuario autenticado.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    /** Token JWT que el cliente debe incluir en el header Authorization de las siguientes requests. */
    private String token;

    /** Tipo de token. Siempre "Bearer" según el estándar OAuth2/JWT. */
    @Builder.Default
    private String tokenType = "Bearer";

    private Long userId;
    private String username;
    private String email;
    private Role role;
}
