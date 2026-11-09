package com.bookhub.api.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * Entidad JPA que representa un usuario registrado en el sistema.
 *
 * <p>Implementa {@link UserDetails} de Spring Security para que pueda ser usada
 * directamente en el proceso de autenticación sin necesidad de una clase adaptadora.</p>
 *
 * <p>La contraseña se almacena como hash BCrypt, nunca en texto plano.</p>
 */
// @Entity: marca esta clase como una entidad JPA que se mapea a una tabla en la BD
@Entity
// @Table: especifica el nombre de la tabla y puede definir índices o constraints únicos
@Table(
    name = "users",
    // @UniqueConstraint: garantiza unicidad a nivel de BD (además de la validación en código)
    uniqueConstraints = {
        @UniqueConstraint(columnNames = "email"),
        @UniqueConstraint(columnNames = "username")
    }
)
// @Getter, @Setter: genera getters y setters para todos los campos (Lombok)
@Getter
@Setter
// @NoArgsConstructor: genera constructor vacío requerido por JPA
@NoArgsConstructor
// @AllArgsConstructor: genera constructor con todos los campos
@AllArgsConstructor
// @Builder: genera el patrón Builder para crear instancias de forma fluida
@Builder
public class User implements UserDetails {

    // @Id: marca este campo como clave primaria
    @Id
    // @GeneratedValue: delega la generación del valor al proveedor JPA
    // IDENTITY: usa el mecanismo de auto-incremento de PostgreSQL (SERIAL / BIGSERIAL)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // @Column: configura el mapeo de la columna; nullable = false → restricción NOT NULL en BD
    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    // La contraseña se almacena hasheada con BCrypt (formato: $2a$10$...)
    @Column(nullable = false)
    private String password;

    // @Enumerated(EnumType.STRING): almacena el enum como texto en la BD ("ROLE_USER", "ROLE_ADMIN")
    // Si usáramos EnumType.ORDINAL, guardaría el índice numérico (más frágil ante cambios de orden)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Role role = Role.ROLE_USER;

    // @Column(updatable = false): este campo se establece al crear y nunca se actualiza
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    // @PrePersist: este método se ejecuta automáticamente ANTES de insertar la entidad en la BD
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // ===== Implementación de UserDetails (Spring Security) =====
    // Spring Security llama a estos métodos para obtener la información de autenticación/autorización

    /**
     * Devuelve los permisos/roles del usuario para que Spring Security controle el acceso.
     * GrantedAuthority es la abstracción de Spring Security para representar un permiso.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // SimpleGrantedAuthority: implementación básica de GrantedAuthority con un String
        return List.of(new SimpleGrantedAuthority(role.name()));
    }

    /**
     * Indica si la cuenta del usuario no ha expirado.
     * Devuelve true = la cuenta está vigente.
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * Indica si la cuenta no está bloqueada.
     */
    @Override
    public boolean isAccountNonLocked() {
        return isActive;
    }

    /**
     * Indica si las credenciales (contraseña) no han expirado.
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * Indica si el usuario está habilitado en el sistema.
     */
    @Override
    public boolean isEnabled() {
        return isActive;
    }
}
