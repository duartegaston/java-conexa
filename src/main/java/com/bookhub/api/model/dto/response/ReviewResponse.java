package com.bookhub.api.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de respuesta que representa una reseña de un libro.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {

    private Long id;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;

    /** Información básica del autor de la reseña */
    private Long userId;
    private String username;

    /** Información básica del libro reseñado */
    private Long bookId;
    private String bookTitle;
}
