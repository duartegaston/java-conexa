package com.bookhub.api.exception;

/**
 * Excepción para violaciones de reglas de negocio.
 * Ejemplos: usuario ya registrado, libro ya en la lista, reseña duplicada.
 * Se mapea a HTTP 409 Conflict en el GlobalExceptionHandler.
 */
public class BusinessException extends RuntimeException {

    /**
     * Crea la excepción con el mensaje de la regla de negocio violada.
     *
     * @param message descripción clara de qué regla de negocio se violó
     */
    public BusinessException(String message) {
        super(message);
    }
}
