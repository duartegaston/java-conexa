package com.bookhub.api.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO de respuesta que representa un libro en la API.
 * Incluye métricas calculadas como promedio de calificaciones.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookResponse {

    private Long id;
    private String olid;
    private String title;
    private String author;
    private Integer publishYear;
    private String isbn;
    private String coverUrl;
    private String description;

    /** Promedio de calificaciones de las reseñas del libro (null si no tiene reseñas). */
    private Double averageRating;

    /** Total de reseñas escritas para este libro. */
    private Long reviewCount;
}
