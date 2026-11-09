package com.bookhub.api.repository;

import com.bookhub.api.model.entity.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio Spring Data JPA para la entidad {@link Book}.
 *
 * <p>Incluye búsqueda por OLID y búsqueda paginada por título/autor usando JPQL.</p>
 */
@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

    /**
     * Busca un libro por su identificador de Open Library.
     *
     * @param olid identificador único de Open Library (ej: "OL7353617M")
     * @return Optional con el libro si ya fue importado
     */
    Optional<Book> findByOlid(String olid);

    /**
     * Verifica si un libro ya fue importado desde Open Library.
     *
     * @param olid identificador único de Open Library
     * @return true si el libro ya existe en la BD local
     */
    boolean existsByOlid(String olid);

    /**
     * Busca libros locales por título o autor con paginación.
     *
     * <p>Usa JPQL (@Query): permite escribir queries orientadas a objetos
     * usando nombres de clases y campos Java en lugar de nombres de tablas SQL.
     * LOWER() + LIKE con % permite búsqueda insensible a mayúsculas.</p>
     *
     * @param query  término de búsqueda
     * @param pageable configuración de página (número, tamaño, ordenamiento)
     * @return página de libros que coinciden con la búsqueda
     */
    @Query("SELECT b FROM Book b WHERE LOWER(b.title) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(b.author) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Book> searchByTitleOrAuthor(@Param("query") String query, Pageable pageable);
}
