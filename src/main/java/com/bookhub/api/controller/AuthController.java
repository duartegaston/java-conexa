package com.bookhub.api.controller;

import com.bookhub.api.model.dto.request.LoginRequest;
import com.bookhub.api.model.dto.request.RegisterRequest;
import com.bookhub.api.model.dto.response.AuthResponse;
import com.bookhub.api.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller REST para los endpoints de autenticación (registro y login).
 *
 * <p>Estos endpoints son públicos (configurado en SecurityConfig) y no requieren JWT.</p>
 *
 * <p>Ruta base: /api/v1/auth</p>
 */
// @RestController: combina @Controller + @ResponseBody
//   - @Controller: registra esta clase como componente MVC (maneja requests HTTP)
//   - @ResponseBody: los valores retornados se serializan como JSON directamente en el body
@RestController
// @RequestMapping: define la ruta base para todos los métodos de este controller
@RequestMapping("/auth")
// @RequiredArgsConstructor: genera constructor con todos los campos final (inyección por constructor)
@RequiredArgsConstructor
// @Tag: agrupa este controller bajo una etiqueta en la documentación de Swagger UI
@Tag(name = "Autenticación", description = "Endpoints para registro e inicio de sesión")
public class AuthController {

    private final AuthService authService;

    /**
     * Registra un nuevo usuario en el sistema.
     *
     * <p>Si el email o username ya existen, devuelve 409 Conflict.
     * Si la validación del body falla, devuelve 400 Bad Request.</p>
     *
     * @param request DTO con username, email y contraseña del nuevo usuario
     * @return 201 Created con el token JWT y datos del usuario registrado
     */
    // @Operation: documenta este endpoint en Swagger UI (nombre y descripción)
    @Operation(summary = "Registrar usuario", description = "Crea una nueva cuenta de usuario")
    // @ApiResponses: documenta los posibles códigos de respuesta HTTP en Swagger UI
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Usuario registrado exitosamente"),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
        @ApiResponse(responseCode = "409", description = "El email o username ya están en uso")
    })
    // @PostMapping: mapea peticiones HTTP POST a este método
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            // @Valid: activa la validación de Bean Validation sobre el DTO RegisterRequest
            // @RequestBody: deserializa el JSON del body HTTP al objeto RegisterRequest
            @Valid @RequestBody RegisterRequest request) {

        AuthResponse response = authService.register(request);
        // ResponseEntity.status(): construye una respuesta HTTP con código de estado personalizado
        // HttpStatus.CREATED (201): indica que un recurso fue creado exitosamente
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Autentica un usuario existente y devuelve un token JWT.
     *
     * @param request DTO con email y contraseña
     * @return 200 OK con el token JWT y datos del usuario autenticado
     */
    @Operation(summary = "Iniciar sesión", description = "Autentica al usuario y devuelve un JWT")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login exitoso"),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
        @ApiResponse(responseCode = "401", description = "Credenciales incorrectas")
    })
    // @PostMapping: mapea peticiones HTTP POST /auth/login
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        // ResponseEntity.ok(): devuelve HTTP 200 OK con el body indicado
        return ResponseEntity.ok(response);
    }
}
