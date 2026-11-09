package com.bookhub.api.security;

import com.bookhub.api.exception.ResourceNotFoundException;
import com.bookhub.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación de {@link UserDetailsService} para Spring Security.
 *
 * <p>Spring Security necesita cargar los datos del usuario (roles, contraseña)
 * durante la autenticación. Esta clase le dice cómo buscarlos en nuestra BD.</p>
 *
 * <p>El método {@code loadUserByUsername} es llamado por Spring Security en dos momentos:</p>
 * <ol>
 *   <li>Durante el login: para verificar la contraseña contra el hash almacenado</li>
 *   <li>En el JWT filter: para cargar el usuario después de validar el token</li>
 * </ol>
 */
// @Service: especialización de @Component para la capa de servicio/negocio
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Carga los datos del usuario por email para el proceso de autenticación.
     *
     * <p>A pesar de llamarse "loadUserByUsername", en nuestro sistema el "username"
     * es el email, que es lo que usamos para identificar al usuario en el JWT.</p>
     *
     * @param email email del usuario (Spring Security lo llama "username" por convención)
     * @return UserDetails con la información del usuario para Spring Security
     * @throws UsernameNotFoundException si el email no existe en la BD
     */
    @Override
    // @Transactional(readOnly = true): abre una transacción de solo lectura (optimización)
    // Evita el overhead de gestión de cambios (dirty checking) de Hibernate
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // Nuestra entidad User implementa UserDetails directamente, por eso podemos retornarla
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                    "Usuario no encontrado con email: " + email
                ));
    }
}
