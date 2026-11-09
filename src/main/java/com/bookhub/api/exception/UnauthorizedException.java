package com.bookhub.api.exception;

/**
 * Excepción lanzada cuando un usuario intenta realizar una operación
 * sobre un recurso que no le pertenece (ej: eliminar la reseña de otro usuario).
 * Se mapea a HTTP 403 Forbidden en el GlobalExceptionHandler.
 */
public class UnauthorizedException extends RuntimeException {

    /**
     * Crea la excepción con el motivo del acceso denegado.
     *
     * @param message descripción del motivo de la denegación
     */
    public UnauthorizedException(String message) {
        super(message);
    }
}
