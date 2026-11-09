package com.bookhub.api.service;

import com.bookhub.api.exception.BusinessException;
import com.bookhub.api.exception.ResourceNotFoundException;
import com.bookhub.api.model.dto.response.BookResponse;
import com.bookhub.api.model.dto.response.OpenLibraryBookResponse;
import com.bookhub.api.model.entity.Book;
import com.bookhub.api.repository.BookRepository;
import com.bookhub.api.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para {@link BookService}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Tests del servicio de libros")
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private OpenLibraryService openLibraryService;

    @InjectMocks
    private BookService bookService;

    private Book testBook;

    @BeforeEach
    void setUp() {
        testBook = Book.builder()
                .id(1L)
                .olid("OL7353617M")
                .title("The Lord of the Rings")
                .author("J.R.R. Tolkien")
                .publishYear(1954)
                .isbn("9780544003415")
                .build();
    }

    @Test
    @DisplayName("Debería importar un libro nuevo exitosamente")
    void importBook_deberiaImportarLibroExitosamente() {
        // Arrange
        OpenLibraryBookResponse.BookDoc bookDoc = new OpenLibraryBookResponse.BookDoc();

        when(bookRepository.existsByOlid("OL7353617M")).thenReturn(false);
        when(openLibraryService.getBookByOlid("OL7353617M")).thenReturn(bookDoc);
        when(bookRepository.save(any(Book.class))).thenReturn(testBook);
        when(reviewRepository.findAverageRatingByBookId(1L)).thenReturn(4.5);
        when(reviewRepository.countByBookId(1L)).thenReturn(10L);

        // Act
        BookResponse response = bookService.importBook("OL7353617M");

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("The Lord of the Rings");
        assertThat(response.getOlid()).isEqualTo("OL7353617M");
        assertThat(response.getAverageRating()).isEqualTo(4.5);
        assertThat(response.getReviewCount()).isEqualTo(10L);

        verify(bookRepository).save(any(Book.class));
    }

    @Test
    @DisplayName("Debería lanzar BusinessException si el libro ya fue importado")
    void importBook_deberiaLanzarExcepcionSiLibroYaExiste() {
        // Arrange: simulamos que el libro ya está en la BD
        when(bookRepository.existsByOlid("OL7353617M")).thenReturn(true);

        // Assert + Act
        assertThatThrownBy(() -> bookService.importBook("OL7353617M"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("OL7353617M");

        // Verificamos que nunca se llamó a Open Library ni se intentó guardar
        verify(openLibraryService, never()).getBookByOlid(any());
        verify(bookRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debería lanzar ResourceNotFoundException si el OLID no existe en Open Library")
    void importBook_deberiaLanzarExcepcionSiOlidNoExiste() {
        // Arrange: el libro no existe ni en la BD ni en Open Library
        when(bookRepository.existsByOlid("INVALID_OLID")).thenReturn(false);
        when(openLibraryService.getBookByOlid("INVALID_OLID")).thenReturn(null);

        // Assert + Act
        assertThatThrownBy(() -> bookService.importBook("INVALID_OLID"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Debería devolver todos los libros paginados")
    void getAllBooks_deberiaDevolverLibrosPaginados() {
        // Arrange: creamos una página de libros simulada
        // PageImpl: implementación concreta de Page para usar en tests
        Page<Book> bookPage = new PageImpl<>(List.of(testBook));
        PageRequest pageable = PageRequest.of(0, 10);

        when(bookRepository.findAll(pageable)).thenReturn(bookPage);
        when(reviewRepository.findAverageRatingByBookId(1L)).thenReturn(null);
        when(reviewRepository.countByBookId(1L)).thenReturn(0L);

        // Act
        Page<BookResponse> result = bookService.getAllBooks(pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("The Lord of the Rings");
    }

    @Test
    @DisplayName("Debería obtener un libro por ID exitosamente")
    void getBookById_deberiaDevolverLibro() {
        // Arrange
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
        when(reviewRepository.findAverageRatingByBookId(1L)).thenReturn(null);
        when(reviewRepository.countByBookId(1L)).thenReturn(0L);

        // Act
        BookResponse response = bookService.getBookById(1L);

        // Assert
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getTitle()).isEqualTo("The Lord of the Rings");
    }

    @Test
    @DisplayName("Debería lanzar ResourceNotFoundException si el libro no existe")
    void getBookById_deberiaLanzarExcepcionSiNoExiste() {
        // Arrange: Optional.empty() simula que no se encontró el recurso en la BD
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        // Assert + Act
        assertThatThrownBy(() -> bookService.getBookById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }
}
