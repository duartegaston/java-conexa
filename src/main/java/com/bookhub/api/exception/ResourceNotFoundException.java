package com.bookhub.api.exception;

/**
 * Excepción lanzada cuando se busca un recurso por ID y no existe en la base de datos.
 * Se mapea automáticamente a HTTP 404 Not Found en el GlobalExceptionHandler.
 */
public class ResourceNotFoundException extends RuntimeException {

    /**
     * Crea la excepción con un mensaje descriptivo sobre qué recurso no fue encontrado.
     *
     * @param resourceName nombre del recurso (ej: "Libro", "Usuario")
     * @param fieldName    nombre del campo de búsqueda (ej: "id", "email")
     * @param fieldValue   valor buscado que no existe
     */
    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s no encontrado con %s: '%s'", resourceName, fieldName, fieldValue));
    }
}
