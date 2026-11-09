package com.bookhub.api.controller;

import com.bookhub.api.model.dto.response.UserResponse;
import com.bookhub.api.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controller REST para las operaciones de administración del sistema.
 *
 * <p>Todos los endpoints de este controller requieren rol ADMIN.
 * La verificación se hace en dos niveles:</p>
 * <ol>
 *   <li>SecurityConfig: la ruta /admin/** requiere rol ADMIN</li>
 *   <li>@PreAuthorize: verificación adicional a nivel de método</li>
 * </ol>
 *
 * <p>Ruta base: /api/v1/admin</p>
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Tag(name = "Administración", description = "Operaciones administrativas (solo ADMIN)")
public class AdminController {

    private final UserService userService;

    /**
     * Lista todos los usuarios registrados en el sistema con paginación.
     *
     * @param pageable configuración de paginación
     * @return página de usuarios en formato DTO (sin contraseñas)
     */
    @Operation(
        summary = "Listar usuarios",
        description = "Devuelve todos los usuarios registrados (solo para administradores)",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping("/users")
    // hasRole('ADMIN'): Spring Security agrega automáticamente el prefijo "ROLE_" → verifica "ROLE_ADMIN"
    // Es equivalente a hasAuthority('ROLE_ADMIN')
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UserResponse>> getAllUsers(
            @PageableDefault(size = 20) Pageable pageable) {

        return ResponseEntity.ok(userService.getAllUsers(pageable));
    }

    /**
     * Desactiva (soft delete) un usuario del sistema.
     *
     * <p>No elimina el registro de la BD, solo lo marca como inactivo.
     * El usuario no podrá autenticarse hasta que sea reactivado.</p>
     *
     * @param userId ID del usuario a desactivar
     * @return 204 No Content si se desactivó correctamente
     */
    @Operation(
        summary = "Desactivar usuario",
        description = "Desactiva un usuario (soft delete). El usuario no podrá iniciar sesión.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    // @DeleteMapping: mapea peticiones HTTP DELETE /admin/users/{userId}
    @DeleteMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivateUser(@PathVariable Long userId) {
        userService.deactivateUser(userId);
        return ResponseEntity.noContent().build();
    }
}
