package com.bookhub.api.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidad JPA que representa la reseña de un usuario sobre un libro.
 *
 * <p>Un usuario solo puede escribir una reseña por libro (constraint único en BD).
 * La calificación es un número entero del 1 al 5.</p>
 */
@Entity
@Table(
    name = "reviews",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_review_user_book",
        columnNames = {"user_id", "book_id"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Calificación del 1 al 5 (validada también con Bean Validation)
    @Min(1)
    @Max(5)
    @Column(nullable = false)
    private Integer rating;

    // Texto de la reseña (opcional, puede ser solo calificación)
    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // ===== RELACIONES MUCHOS A UNO =====

    // @ManyToOne: muchas reseñas pertenecen a un usuario
    // @JoinColumn: especifica la columna de clave foránea en la tabla "reviews"
    // FetchType.LAZY: NO carga el objeto User hasta que se acceda explícitamente (optimización)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // @ManyToOne: muchas reseñas pertenecen a un libro
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
