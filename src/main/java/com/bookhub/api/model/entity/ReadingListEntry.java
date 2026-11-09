package com.bookhub.api.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidad JPA que representa una entrada en la lista de lectura personal de un usuario.
 *
 * <p>Funciona como tabla de relación entre User y Book con estado adicional.
 * Un usuario puede tener un mismo libro en su lista solo una vez.</p>
 */
@Entity
@Table(
    name = "reading_list",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_reading_list_user_book",
        columnNames = {"user_id", "book_id"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReadingListEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Estado de lectura del libro en la lista de este usuario
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ReadingStatus status = ReadingStatus.WANT_TO_READ;

    @Column(name = "added_at", nullable = false, updatable = false)
    private LocalDateTime addedAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @PrePersist
    protected void onCreate() {
        this.addedAt = LocalDateTime.now();
    }

    // @PreUpdate: se ejecuta automáticamente ANTES de actualizar la entidad en la BD
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
