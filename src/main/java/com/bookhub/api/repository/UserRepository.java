package com.bookhub.api.repository;

import com.bookhub.api.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio Spring Data JPA para la entidad {@link User}.
 *
 * <p>Al extender {@link JpaRepository}, obtenemos automáticamente los métodos CRUD:
 * save(), findById(), findAll(), deleteById(), count(), etc.
 * No es necesario escribir ninguna implementación.</p>
 *
 * <p>Los métodos "findBy..." son Query Methods de Spring Data: Spring genera la query SQL
 * automáticamente a partir del nombre del método siguiendo convenciones de nomenclatura.</p>
 */
// @Repository: marca esta interfaz como componente de acceso a datos.
// En Spring Data JPA es opcional (JpaRepository ya la incluye), pero es buena práctica para claridad.
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Busca un usuario por su email.
     * Spring Data genera automáticamente: SELECT * FROM users WHERE email = ?
     *
     * @param email email del usuario a buscar
     * @return Optional con el usuario si existe, o vacío si no
     */
    Optional<User> findByEmail(String email);

    /**
     * Busca un usuario por su nombre de usuario.
     *
     * @param username nombre de usuario a buscar
     * @return Optional con el usuario si existe, o vacío si no
     */
    Optional<User> findByUsername(String username);

    /**
     * Verifica si existe un usuario con el email dado.
     * Más eficiente que findByEmail() cuando solo necesitamos saber si existe.
     *
     * @param email email a verificar
     * @return true si el email ya está registrado
     */
    boolean existsByEmail(String email);

    /**
     * Verifica si existe un usuario con el nombre de usuario dado.
     *
     * @param username nombre de usuario a verificar
     * @return true si el username ya está en uso
     */
    boolean existsByUsername(String username);
}
