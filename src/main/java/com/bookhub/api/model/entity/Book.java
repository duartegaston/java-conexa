package com.bookhub.api.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Entidad JPA que representa un libro importado desde Open Library API.
 *
 * <p>Los libros no se crean manualmente: se importan desde la API externa
 * usando el identificador único de Open Library (OLID), por ejemplo: "OL7353617M".</p>
 */
@Entity
@Table(
    name = "books",
    uniqueConstraints = @UniqueConstraint(columnNames = "olid")
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Identificador único de Open Library (ej: "OL7353617M")
    @Column(nullable = false, unique = true, length = 50)
    private String olid;

    @Column(nullable = false)
    private String title;

    @Column(length = 500)
    private String author;

    // Año de primera publicación
    @Column(name = "publish_year")
    private Integer publishYear;

    @Column(length = 20)
    private String isbn;

    // URL de la portada del libro (construida con el ID de portada de Open Library)
    @Column(name = "cover_url")
    private String coverUrl;

    // Descripción o sinopsis del libro
    @Column(columnDefinition = "TEXT")
    private String description;

    // ===== RELACIONES =====

    // @OneToMany: un libro puede tener muchas reseñas
    // mappedBy = "book": indica que la relación es gestionada desde el lado Review (la FK está en reviews)
    // cascade = ALL: las operaciones (save, delete) se propagan a las reseñas asociadas
    // orphanRemoval = true: si se quita una reseña de la lista, se elimina de la BD
    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Review> reviews = new ArrayList<>();

    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ReadingListEntry> readingListEntries = new ArrayList<>();
}
