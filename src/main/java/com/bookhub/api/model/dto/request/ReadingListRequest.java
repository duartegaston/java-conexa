package com.bookhub.api.model.dto.request;

import com.bookhub.api.model.entity.ReadingStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para agregar un libro a la lista de lectura o actualizar su estado.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReadingListRequest {

    @NotNull(message = "El ID del libro es obligatorio")
    private Long bookId;

    // @NotNull: el estado es obligatorio al agregar o actualizar
    @NotNull(message = "El estado de lectura es obligatorio")
    private ReadingStatus status;
}
