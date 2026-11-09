package com.bookhub.api.service;

import com.bookhub.api.exception.BusinessException;
import com.bookhub.api.exception.ResourceNotFoundException;
import com.bookhub.api.exception.UnauthorizedException;
import com.bookhub.api.model.dto.request.ReviewRequest;
import com.bookhub.api.model.dto.response.ReviewResponse;
import com.bookhub.api.model.entity.Book;
import com.bookhub.api.model.entity.Review;
import com.bookhub.api.model.entity.User;
import com.bookhub.api.repository.BookRepository;
import com.bookhub.api.repository.ReviewRepository;
import com.bookhub.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio para la gestión de reseñas de libros.
 *
 * <p>Implementa las reglas de negocio para reseñas:</p>
 * <ul>
 *   <li>Un usuario solo puede tener una reseña por libro</li>
 *   <li>Solo el autor de la reseña (o un admin) puede eliminarla</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;

    /**
     * Crea una nueva reseña de un libro por parte de un usuario autenticado.
     *
     * @param bookId    ID del libro a reseñar
     * @param request   DTO con calificación y comentario
     * @param userEmail email del usuario autenticado (extraído del JWT en el controller)
     * @return ReviewResponse con los datos de la reseña creada
     * @throws ResourceNotFoundException si el libro no existe
     * @throws BusinessException         si el usuario ya escribió una reseña para este libro
     */
    @Transactional
    public ReviewResponse createReview(Long bookId, ReviewRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "email", userEmail));

        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Libro", "id", bookId));

        // Verificamos la regla de negocio: una reseña por usuario por libro
        if (reviewRepository.findByUserIdAndBookId(user.getId(), bookId).isPresent()) {
            throw new BusinessException("Ya escribiste una reseña para este libro.");
        }

        Review review = Review.builder()
                .rating(request.getRating())
                .comment(request.getComment())
                .user(user)
                .book(book)
                .build();

        Review savedReview = reviewRepository.save(review);
        log.info("Reseña creada por '{}' para el libro '{}'", userEmail, book.getTitle());

        return mapToReviewResponse(savedReview);
    }

    /**
     * Obtiene todas las reseñas de un libro específico, con paginación.
     *
     * @param bookId   ID del libro
     * @param pageable configuración de paginación
     * @return página de reseñas del libro
     * @throws ResourceNotFoundException si el libro no existe
     */
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getReviewsByBook(Long bookId, Pageable pageable) {
        // Verificamos que el libro existe antes de buscar sus reseñas
        if (!bookRepository.existsById(bookId)) {
            throw new ResourceNotFoundException("Libro", "id", bookId);
        }
        return reviewRepository.findByBookId(bookId, pageable).map(this::mapToReviewResponse);
    }

    /**
     * Elimina una reseña. Solo el autor de la reseña puede eliminarla.
     *
     * @param reviewId  ID de la reseña a eliminar
     * @param userEmail email del usuario que solicita la eliminación
     * @throws ResourceNotFoundException si la reseña no existe
     * @throws UnauthorizedException     si el usuario no es el autor de la reseña
     */
    @Transactional
    public void deleteReview(Long reviewId, String userEmail) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Reseña", "id", reviewId));

        // Verificamos que el usuario que pide eliminar sea el autor de la reseña
        if (!review.getUser().getEmail().equals(userEmail)) {
            throw new UnauthorizedException("No tienes permiso para eliminar esta reseña.");
        }

        reviewRepository.delete(review);
        log.info("Reseña {} eliminada por '{}'", reviewId, userEmail);
    }

    /**
     * Mapea una entidad Review a su DTO de respuesta.
     *
     * @param review entidad a transformar
     * @return DTO con los datos de la reseña
     */
    private ReviewResponse mapToReviewResponse(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .rating(review.getRating())
                .comment(review.getComment())
                .createdAt(review.getCreatedAt())
                .userId(review.getUser().getId())
                .username(review.getUser().getUsername())
                .bookId(review.getBook().getId())
                .bookTitle(review.getBook().getTitle())
                .build();
    }
}
