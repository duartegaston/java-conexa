package com.bookhub.api.repository;

import com.bookhub.api.model.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio Spring Data JPA para la entidad {@link Review}.
 */
@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    /**
     * Obtiene todas las reseñas de un libro específico, paginadas.
     *
     * @param bookId   identificador del libro
     * @param pageable configuración de paginación
     * @return página de reseñas del libro
     */
    Page<Review> findByBookId(Long bookId, Pageable pageable);

    /**
     * Busca la reseña de un usuario específico sobre un libro específico.
     * Útil para verificar si el usuario ya escribió una reseña antes de permitir otra.
     *
     * @param userId id del usuario
     * @param bookId id del libro
     * @return Optional con la reseña si existe
     */
    Optional<Review> findByUserIdAndBookId(Long userId, Long bookId);

    /**
     * Calcula el promedio de calificaciones de un libro usando una query JPQL de agregación.
     *
     * <p>AVG() es una función de agregación SQL/JPQL que devuelve el promedio.
     * Devuelve null si no hay reseñas para el libro.</p>
     *
     * @param bookId id del libro
     * @return promedio de calificaciones o null si no hay reseñas
     */
    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.book.id = :bookId")
    Double findAverageRatingByBookId(@Param("bookId") Long bookId);

    /**
     * Cuenta cuántas reseñas tiene un libro.
     *
     * @param bookId id del libro
     * @return cantidad de reseñas
     */
    long countByBookId(Long bookId);
}
