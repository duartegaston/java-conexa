package com.bookhub.api.controller;

import com.bookhub.api.model.dto.request.ReviewRequest;
import com.bookhub.api.model.dto.response.ReviewResponse;
import com.bookhub.api.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * Controller REST para la gestión de reseñas de libros.
 *
 * <p>Ruta base: /api/v1/reviews</p>
 */
@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
@Tag(name = "Reseñas", description = "Gestión de reseñas de libros")
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * Obtiene todas las reseñas de un libro específico, paginadas.
     * Endpoint público, no requiere autenticación.
     *
     * @param bookId   ID del libro
     * @param pageable configuración de paginación
     * @return página de reseñas del libro
     */
    @Operation(summary = "Listar reseñas de un libro", description = "Devuelve las reseñas de un libro con paginación")
    @GetMapping("/book/{bookId}")
    public ResponseEntity<Page<ReviewResponse>> getBookReviews(
            @PathVariable Long bookId,
            @PageableDefault(size = 10) Pageable pageable) {

        return ResponseEntity.ok(reviewService.getReviewsByBook(bookId, pageable));
    }

    /**
     * Crea una nueva reseña para un libro.
     * Requiere autenticación. Un usuario solo puede reseñar un libro una vez.
     *
     * @param bookId      ID del libro a reseñar
     * @param request     DTO con calificación y comentario
     * @param userDetails datos del usuario autenticado (inyectados por Spring Security)
     * @return 201 Created con los datos de la reseña creada
     */
    @Operation(
        summary = "Crear reseña",
        description = "Crea una reseña para un libro (máximo una por usuario por libro)",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping("/book/{bookId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReviewResponse> createReview(
            @PathVariable Long bookId,
            @Valid @RequestBody ReviewRequest request,
            // @AuthenticationPrincipal: inyecta el objeto UserDetails del usuario autenticado
            // Spring Security lo obtiene del SecurityContext sin necesidad de llamar a la BD
            @AuthenticationPrincipal UserDetails userDetails) {

        ReviewResponse response = reviewService.createReview(
            bookId,
            request,
            userDetails.getUsername()  // getUsername() devuelve el email (ver User.java)
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Elimina una reseña. Solo el autor puede eliminar su propia reseña.
     *
     * @param reviewId    ID de la reseña a eliminar
     * @param userDetails datos del usuario autenticado
     * @return 204 No Content si se eliminó correctamente
     */
    @Operation(
        summary = "Eliminar reseña",
        description = "Elimina una reseña. Solo el autor puede eliminarla.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    // @DeleteMapping: mapea peticiones HTTP DELETE
    @DeleteMapping("/{reviewId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> deleteReview(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal UserDetails userDetails) {

        reviewService.deleteReview(reviewId, userDetails.getUsername());
        // ResponseEntity.noContent(): devuelve HTTP 204 No Content (sin body, operación exitosa)
        return ResponseEntity.noContent().build();
    }
}
