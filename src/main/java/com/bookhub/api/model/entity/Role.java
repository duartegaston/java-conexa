package com.bookhub.api.model.entity;

/**
 * Enum que representa los roles de autorización disponibles en la aplicación.
 *
 * <p>Spring Security utiliza estos roles para controlar el acceso a los endpoints.
 * Los roles se almacenan como texto en la tabla de usuarios.</p>
 *
 * <p>Convención Spring Security: los roles deben tener el prefijo "ROLE_"
 * cuando se usan con hasRole(). Con hasAuthority() se usa el nombre exacto.</p>
 */
public enum Role {

    /**
     * Usuario estándar: puede buscar libros, escribir reseñas y gestionar su lista de lectura.
     */
    ROLE_USER,

    /**
     * Administrador: tiene acceso completo incluyendo gestión de usuarios.
     */
    ROLE_ADMIN
}
