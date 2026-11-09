package com.bookhub.api.repository;

import com.bookhub.api.model.entity.ReadingListEntry;
import com.bookhub.api.model.entity.ReadingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio Spring Data JPA para la entidad {@link ReadingListEntry}.
 */
@Repository
public interface ReadingListRepository extends JpaRepository<ReadingListEntry, Long> {

    /**
     * Obtiene toda la lista de lectura de un usuario.
     *
     * @param userId id del usuario
     * @return lista de entradas de lectura del usuario
     */
    List<ReadingListEntry> findByUserId(Long userId);

    /**
     * Filtra la lista de lectura de un usuario por estado.
     *
     * @param userId id del usuario
     * @param status estado de lectura a filtrar
     * @return entradas que coinciden con el estado dado
     */
    List<ReadingListEntry> findByUserIdAndStatus(Long userId, ReadingStatus status);

    /**
     * Busca si un usuario ya tiene un libro específico en su lista.
     *
     * @param userId id del usuario
     * @param bookId id del libro
     * @return Optional con la entrada si existe
     */
    Optional<ReadingListEntry> findByUserIdAndBookId(Long userId, Long bookId);

    /**
     * Verifica si un libro ya está en la lista de lectura de un usuario.
     *
     * @param userId id del usuario
     * @param bookId id del libro
     * @return true si el libro ya está en la lista
     */
    boolean existsByUserIdAndBookId(Long userId, Long bookId);
}
