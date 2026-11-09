package com.bookhub.api.service;

import com.bookhub.api.exception.ResourceNotFoundException;
import com.bookhub.api.model.dto.response.UserResponse;
import com.bookhub.api.model.entity.User;
import com.bookhub.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio para la gestión administrativa de usuarios.
 * Solo accesible para usuarios con rol ADMIN.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;

    /**
     * Obtiene todos los usuarios registrados con paginación.
     * Operación reservada para administradores.
     *
     * @param pageable configuración de paginación
     * @return página de usuarios en formato DTO (sin contraseña)
     */
    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::mapToUserResponse);
    }

    /**
     * Desactiva (soft delete) un usuario del sistema.
     *
     * <p>En lugar de eliminar el registro de la BD, lo marcamos como inactivo.
     * Esto preserva la integridad referencial con reseñas y listas de lectura
     * y permite auditoría posterior.</p>
     *
     * @param userId ID del usuario a desactivar
     * @throws ResourceNotFoundException si el usuario no existe
     */
    @Transactional
    public void deactivateUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "id", userId));

        user.setIsActive(false);
        userRepository.save(user);
        log.info("Usuario {} desactivado", userId);
    }

    /**
     * Mapea una entidad User a su DTO de respuesta, ocultando datos sensibles.
     *
     * @param user entidad de usuario
     * @return DTO sin contraseña ni datos internos de Spring Security
     */
    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
