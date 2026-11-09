package com.bookhub.api.model.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para crear o actualizar una reseña de un libro.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewRequest {

    // @NotNull: valida que el valor no sea null (a diferencia de @NotBlank, aplica a cualquier tipo)
    @NotNull(message = "La calificación es obligatoria")
    // @Min y @Max: validan que el número esté dentro del rango permitido
    @Min(value = 1, message = "La calificación mínima es 1")
    @Max(value = 5, message = "La calificación máxima es 5")
    private Integer rating;

    // El comentario es opcional (puede ser null o vacío)
    @Size(max = 2000, message = "El comentario no puede superar los 2000 caracteres")
    private String comment;
}
