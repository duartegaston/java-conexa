package com.bookhub.api.model.dto.response;

import com.bookhub.api.model.entity.ReadingStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO de respuesta que representa una entrada en la lista de lectura personal.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReadingListResponse {

    private Long id;
    private ReadingStatus status;
    private LocalDateTime addedAt;
    private LocalDateTime updatedAt;

    /** Información del libro en la lista */
    private Long bookId;
    private String bookTitle;
    private String bookAuthor;
    private String coverUrl;
}
