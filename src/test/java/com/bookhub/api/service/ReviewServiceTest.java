package com.bookhub.api.service;

import com.bookhub.api.exception.BusinessException;
import com.bookhub.api.exception.ResourceNotFoundException;
import com.bookhub.api.exception.UnauthorizedException;
import com.bookhub.api.model.dto.request.ReviewRequest;
import com.bookhub.api.model.dto.response.ReviewResponse;
import com.bookhub.api.model.entity.Book;
import com.bookhub.api.model.entity.Review;
import com.bookhub.api.model.entity.Role;
import com.bookhub.api.model.entity.User;
import com.bookhub.api.repository.BookRepository;
import com.bookhub.api.repository.ReviewRepository;
import com.bookhub.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para {@link ReviewService}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Tests del servicio de reseñas")
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ReviewService reviewService;

    private User testUser;
    private Book testBook;
    private Review testReview;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .role(Role.ROLE_USER)
                .isActive(true)
                .build();

        testBook = Book.builder()
                .id(1L)
                .olid("OL7353617M")
                .title("The Lord of the Rings")
                .author("J.R.R. Tolkien")
                .build();

        testReview = Review.builder()
                .id(1L)
                .rating(5)
                .comment("¡Un clásico imprescindible!")
                .user(testUser)
                .book(testBook)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    @DisplayName("Debería crear una reseña exitosamente")
    void createReview_deberiaCrearReseniaExitosamente() {
        // Arrange
        ReviewRequest request = new ReviewRequest(5, "¡Un clásico imprescindible!");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
        when(reviewRepository.findByUserIdAndBookId(1L, 1L)).thenReturn(Optional.empty());
        when(reviewRepository.save(any(Review.class))).thenReturn(testReview);

        // Act
        ReviewResponse response = reviewService.createReview(1L, request, "test@example.com");

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getRating()).isEqualTo(5);
        assertThat(response.getComment()).isEqualTo("¡Un clásico imprescindible!");
        assertThat(response.getUsername()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("Debería lanzar BusinessException si el usuario ya reseñó el libro")
    void createReview_deberiaLanzarExcepcionSiYaReseno() {
        // Arrange: simulamos que ya existe una reseña del usuario para este libro
        ReviewRequest request = new ReviewRequest(4, "Segunda reseña");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(bookRepository.findById(1L)).thenReturn(Optional.of(testBook));
        when(reviewRepository.findByUserIdAndBookId(1L, 1L)).thenReturn(Optional.of(testReview));

        // Assert + Act
        assertThatThrownBy(() -> reviewService.createReview(1L, request, "test@example.com"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ya escribiste");

        verify(reviewRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debería eliminar la reseña si el usuario es el autor")
    void deleteReview_deberiaEliminarSiEsElAutor() {
        // Arrange
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(testReview));

        // Act
        reviewService.deleteReview(1L, "test@example.com");

        // Assert: verificamos que se llamó a delete con la reseña correcta
        verify(reviewRepository).delete(testReview);
    }

    @Test
    @DisplayName("Debería lanzar UnauthorizedException si el usuario no es el autor")
    void deleteReview_deberiaLanzarExcepcionSiNoEsElAutor() {
        // Arrange
        when(reviewRepository.findById(1L)).thenReturn(Optional.of(testReview));

        // Act + Assert: el usuario "otro@email.com" no es el autor de la reseña
        assertThatThrownBy(() -> reviewService.deleteReview(1L, "otro@email.com"))
                .isInstanceOf(UnauthorizedException.class);

        // Verificamos que nunca se eliminó la reseña
        verify(reviewRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Debería lanzar ResourceNotFoundException si la reseña no existe")
    void deleteReview_deberiaLanzarExcepcionSiReseniaNoExiste() {
        // Arrange
        when(reviewRepository.findById(99L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> reviewService.deleteReview(99L, "test@example.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
