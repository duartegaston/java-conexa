package com.bookhub.api.repository;

import com.bookhub.api.model.entity.Book;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests de repositorio JPA usando base de datos H2 en memoria.
 *
 * <p>@DataJpaTest configura solo la capa de persistencia:
 * <ul>
 *   <li>Carga solo los componentes JPA (@Entity, @Repository)</li>
 *   <li>Configura H2 en memoria automáticamente</li>
 *   <li>Cada test se ejecuta en una transacción que se revierte al final (@Transactional)</li>
 *   <li>No carga @Service, @Controller ni @Component</li>
 * </ul></p>
 */
// @DataJpaTest: configuración mínima de Spring para testear la capa JPA con H2
@DataJpaTest
// @ActiveProfiles("test"): activa el perfil "test" para usar application-test.yml
@ActiveProfiles("test")
@DisplayName("Tests del repositorio de libros")
class BookRepositoryTest {

    // @Autowired: inyecta el repositorio real (no un mock) para testear contra H2
    @Autowired
    private BookRepository bookRepository;

    private Book testBook;

    @BeforeEach
    void setUp() {
        testBook = Book.builder()
                .olid("OL7353617M")
                .title("The Lord of the Rings")
                .author("J.R.R. Tolkien")
                .publishYear(1954)
                .isbn("9780544003415")
                .build();
    }

    @Test
    @DisplayName("Debería guardar y recuperar un libro por OLID")
    void deberiaGuardarYRecuperarLibroPorOlid() {
        // Arrange + Act
        Book savedBook = bookRepository.save(testBook);

        // Assert
        Optional<Book> found = bookRepository.findByOlid("OL7353617M");
        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("The Lord of the Rings");
        assertThat(found.get().getId()).isEqualTo(savedBook.getId());
    }

    @Test
    @DisplayName("Debería retornar vacío si el OLID no existe")
    void deberiaDevolverVacioSiOlidNoExiste() {
        Optional<Book> found = bookRepository.findByOlid("OLID_INEXISTENTE");
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("existsByOlid debería retornar true si el libro existe")
    void existsByOlid_deberiaRetornarTrueSiExiste() {
        // Arrange
        bookRepository.save(testBook);

        // Act + Assert
        assertThat(bookRepository.existsByOlid("OL7353617M")).isTrue();
        assertThat(bookRepository.existsByOlid("OL_OTRO")).isFalse();
    }

    @Test
    @DisplayName("Debería buscar libros por título o autor con paginación")
    void searchByTitleOrAuthor_deberiaBuscarPorTituloOAutor() {
        // Arrange: guardamos varios libros para testear la búsqueda
        bookRepository.save(testBook);
        bookRepository.save(Book.builder()
                .olid("OL123")
                .title("The Hobbit")
                .author("J.R.R. Tolkien")
                .publishYear(1937)
                .build());
        bookRepository.save(Book.builder()
                .olid("OL456")
                .title("Harry Potter")
                .author("J.K. Rowling")
                .publishYear(1997)
                .build());

        // Act: buscamos por autor "Tolkien"
        Page<Book> result = bookRepository.searchByTitleOrAuthor(
            "Tolkien",
            PageRequest.of(0, 10)
        );

        // Assert: deberían aparecer los 2 libros de Tolkien
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent())
            .extracting(Book::getAuthor)
            .allMatch(author -> author.contains("Tolkien"));
    }

    @Test
    @DisplayName("La búsqueda debería ser insensible a mayúsculas")
    void searchByTitleOrAuthor_deberiaSerInsensibleAMayusculas() {
        // Arrange
        bookRepository.save(testBook);

        // Act: buscamos en minúsculas aunque el título tiene mayúsculas
        Page<Book> result = bookRepository.searchByTitleOrAuthor(
            "lord of the rings",
            PageRequest.of(0, 10)
        );

        // Assert
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("The Lord of the Rings");
    }
}
