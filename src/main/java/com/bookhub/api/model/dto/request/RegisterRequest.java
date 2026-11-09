package com.bookhub.api.model.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO (Data Transfer Object) para la solicitud de registro de un nuevo usuario.
 *
 * <p>Los DTOs son objetos simples que transportan datos entre capas.
 * Se usan para desacoplar la representación de la API (lo que el cliente envía)
 * de la entidad interna (lo que se guarda en la BD).</p>
 *
 * <p>Las anotaciones de Bean Validation se activan cuando el controller usa {@code @Valid}.</p>
 */
// @Data: genera getters, setters, equals(), hashCode() y toString() (Lombok)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    // @NotBlank: valida que el String no sea null, vacío ni solo espacios
    // message: mensaje de error personalizado que se devuelve en la respuesta
    @NotBlank(message = "El nombre de usuario es obligatorio")
    // @Size: valida la longitud mínima y máxima del String
    @Size(min = 3, max = 50, message = "El nombre de usuario debe tener entre 3 y 50 caracteres")
    private String username;

    // @Email: valida que el String tenga formato de email válido
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El formato del email no es válido")
    @Size(max = 100, message = "El email no puede superar los 100 caracteres")
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, max = 100, message = "La contraseña debe tener entre 8 y 100 caracteres")
    private String password;
}
