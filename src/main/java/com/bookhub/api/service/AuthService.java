package com.bookhub.api.service;

import com.bookhub.api.exception.BusinessException;
import com.bookhub.api.model.dto.request.LoginRequest;
import com.bookhub.api.model.dto.request.RegisterRequest;
import com.bookhub.api.model.dto.response.AuthResponse;
import com.bookhub.api.model.entity.User;
import com.bookhub.api.repository.UserRepository;
import com.bookhub.api.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio de autenticación y registro de usuarios.
 *
 * <p>Gestiona el ciclo de vida de la autenticación:</p>
 * <ul>
 *   <li>Registro: valida unicidad, hashea la contraseña y persiste el usuario</li>
 *   <li>Login: delega la verificación de credenciales a Spring Security y genera JWT</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    // AuthenticationManager: interfaz de Spring Security que orquesta el proceso de autenticación
    // Delega al AuthenticationProvider configurado (DaoAuthenticationProvider en nuestro caso)
    private final AuthenticationManager authenticationManager;

    /**
     * Registra un nuevo usuario en el sistema.
     *
     * <p>Verifica que el email y username sean únicos, hashea la contraseña
     * con BCrypt y persiste el usuario con rol USER por defecto.</p>
     *
     * @param request DTO con los datos del nuevo usuario
     * @return AuthResponse con el JWT generado y los datos del usuario creado
     * @throws BusinessException si el email o username ya están en uso
     */
    // @Transactional: envuelve el método en una transacción de BD.
    // Si cualquier operación falla, se hace rollback automático (atomicidad).
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Verificamos unicidad antes de intentar insertar (más claro que capturar excepción de BD)
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Ya existe una cuenta con el email: " + request.getEmail());
        }

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException("El nombre de usuario ya está en uso: " + request.getUsername());
        }

        // Construimos la entidad User usando el patrón Builder generado por Lombok
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                // passwordEncoder.encode(): hashea la contraseña con BCrypt antes de persistir
                // NUNCA se guarda la contraseña en texto plano
                .password(passwordEncoder.encode(request.getPassword()))
                .build();

        // save(): persiste la entidad en la BD y devuelve la entidad con el ID generado
        User savedUser = userRepository.save(user);
        log.info("Nuevo usuario registrado: {}", savedUser.getEmail());

        // Generamos el JWT directamente sin pasar por el flujo de autenticación
        String token = jwtTokenProvider.generateTokenFromEmail(savedUser.getEmail());

        return buildAuthResponse(savedUser, token);
    }

    /**
     * Autentica un usuario existente y devuelve un JWT.
     *
     * <p>El flujo de autenticación de Spring Security:
     * AuthenticationManager → DaoAuthenticationProvider →
     * UserDetailsService.loadUserByUsername() → BCrypt.matches()</p>
     *
     * @param request DTO con email y contraseña
     * @return AuthResponse con el JWT y datos del usuario
     * @throws org.springframework.security.authentication.BadCredentialsException si las credenciales son incorrectas
     */
    public AuthResponse login(LoginRequest request) {
        // authenticate(): verifica las credenciales contra la BD usando el DaoAuthenticationProvider
        // Si las credenciales son incorrectas, lanza BadCredentialsException (capturada en GlobalExceptionHandler)
        Authentication authentication = authenticationManager.authenticate(
            // UsernamePasswordAuthenticationToken: contenedor de credenciales para el proceso de auth
            new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        // Si llegamos aquí, las credenciales son correctas
        User user = (User) authentication.getPrincipal();

        // Generamos el JWT con los datos del usuario autenticado
        String token = jwtTokenProvider.generateToken(authentication);
        log.info("Login exitoso para: {}", user.getEmail());

        return buildAuthResponse(user, token);
    }

    /**
     * Construye el DTO de respuesta de autenticación con el token y datos del usuario.
     *
     * @param user  entidad del usuario autenticado
     * @param token token JWT generado
     * @return AuthResponse con toda la información de autenticación
     */
    private AuthResponse buildAuthResponse(User user, String token) {
        return AuthResponse.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .build();
    }
}
