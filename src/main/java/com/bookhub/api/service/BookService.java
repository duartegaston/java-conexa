package com.bookhub.api.service;

import com.bookhub.api.exception.BusinessException;
import com.bookhub.api.exception.ResourceNotFoundException;
import com.bookhub.api.model.dto.response.BookResponse;
import com.bookhub.api.model.dto.response.OpenLibraryBookResponse;
import com.bookhub.api.model.entity.Book;
import com.bookhub.api.repository.BookRepository;
import com.bookhub.api.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio para la gestión de libros en el sistema.
 *
 * <p>Coordina las operaciones entre el repositorio local de libros,
 * la integración con Open Library API y el cálculo de métricas (calificación promedio).</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BookService {

    private final BookRepository bookRepository;
    private final ReviewRepository reviewRepository;
    private final OpenLibraryService openLibraryService;

    /**
     * Busca libros en la Open Library API externa por título, autor o ISBN.
     *
     * <p>Los resultados vienen directamente de la API externa, no de la BD local.
     * Para guardar un libro localmente se usa {@link #importBook(String)}.</p>
     *
     * @param query término de búsqueda
     * @param limit máximo de resultados (por defecto 20)
     * @return lista de DTOs con resultados de Open Library
     */
    public List<OpenLibraryBookResponse.BookDoc> searchInOpenLibrary(String query, int limit) {
        log.info("Buscando en Open Library: '{}'", query);
        return openLibraryService.searchBooks(query, limit);
    }

    /**
     * Importa un libro desde Open Library a la base de datos local.
     *
     * <p>Si el libro ya fue importado previamente, lanza BusinessException para
     * evitar duplicados. Usa el OLID como identificador único del libro.</p>
     *
     * @param olid identificador único de Open Library (ej: "OL7353617M")
     * @return BookResponse con los datos del libro importado
     * @throws BusinessException si el libro ya fue importado
     * @throws ResourceNotFoundException si el OLID no existe en Open Library
     */
    @Transactional
    public BookResponse importBook(String olid) {
        // Verificamos que no esté ya importado
        if (bookRepository.existsByOlid(olid)) {
            throw new BusinessException("El libro con OLID '" + olid + "' ya fue importado.");
        }

        // Consultamos la API externa para obtener los datos del libro
        OpenLibraryBookResponse.BookDoc bookDoc = openLibraryService.getBookByOlid(olid);

        if (bookDoc == null) {
            throw new ResourceNotFoundException("Libro", "OLID", olid);
        }

        // Mapeamos los datos de la API al formato de nuestra entidad
        Book book = Book.builder()
                .olid(olid)
                .title(bookDoc.getTitle())
                .author(bookDoc.getPrimaryAuthor())
                .publishYear(bookDoc.getFirstPublishYear())
                .isbn(bookDoc.getPrimaryIsbn())
                .coverUrl(bookDoc.buildCoverUrl())
                .build();

        Book savedBook = bookRepository.save(book);
        log.info("Libro importado: '{}' (OLID: {})", savedBook.getTitle(), olid);

        return mapToBookResponse(savedBook);
    }

    /**
     * Obtiene todos los libros importados en la BD local con paginación.
     *
     * <p>La paginación evita cargar todos los registros en memoria de una vez.
     * El cliente puede solicitar: /books?page=0&size=10&sort=title,asc</p>
     *
     * @param pageable objeto con número de página, tamaño y ordenamiento
     * @return página de libros con métricas calculadas
     */
    @Transactional(readOnly = true)
    public Page<BookResponse> getAllBooks(Pageable pageable) {
        // map(): aplica la transformación a cada elemento de la página sin perder los metadatos
        return bookRepository.findAll(pageable).map(this::mapToBookResponse);
    }

    /**
     * Obtiene el detalle de un libro específico por su ID en la BD local.
     *
     * @param id identificador del libro en la BD local
     * @return BookResponse con los datos del libro y sus métricas
     * @throws ResourceNotFoundException si el ID no existe en la BD
     */
    @Transactional(readOnly = true)
    public BookResponse getBookById(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Libro", "id", id));
        return mapToBookResponse(book);
    }

    /**
     * Mapea una entidad Book a su DTO de respuesta, calculando las métricas.
     *
     * @param book entidad a transformar
     * @return DTO con los datos y métricas calculadas del libro
     */
    private BookResponse mapToBookResponse(Book book) {
        // Consultamos las métricas en la BD (promedio de calificaciones y total de reseñas)
        Double averageRating = reviewRepository.findAverageRatingByBookId(book.getId());
        long reviewCount = reviewRepository.countByBookId(book.getId());

        return BookResponse.builder()
                .id(book.getId())
                .olid(book.getOlid())
                .title(book.getTitle())
                .author(book.getAuthor())
                .publishYear(book.getPublishYear())
                .isbn(book.getIsbn())
                .coverUrl(book.getCoverUrl())
                .description(book.getDescription())
                .averageRating(averageRating != null
                    ? Math.round(averageRating * 10.0) / 10.0
                    : null)
                .reviewCount(reviewCount)
                .build();
    }
}
