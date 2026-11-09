package com.bookhub.api.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Manejador centralizado de excepciones para toda la aplicación.
 *
 * <p>Sin esta clase, cada excepción no capturada devolvería un error genérico de Spring.
 * Con este manejador, controlamos exactamente qué código HTTP y qué mensaje devolvemos
 * para cada tipo de error, dando respuestas consistentes y descriptivas al cliente.</p>
 *
 * <p>El flujo es: Controller lanza excepción → Spring la intercepta → la delega aquí →
 * devolvemos ResponseEntity con el formato de error estándar.</p>
 */
// @RestControllerAdvice: combina @ControllerAdvice + @ResponseBody
//   - @ControllerAdvice: indica que esta clase maneja excepciones de todos los controllers
//   - @ResponseBody: las respuestas se serializan como JSON automáticamente
@RestControllerAdvice
// @Slf4j: genera un logger estático `log` (equivalente a LoggerFactory.getLogger(...)) (Lombok)
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Maneja errores de validación de Bean Validation (@Valid en los controllers).
     * Devuelve HTTP 400 Bad Request con el detalle de cada campo inválido.
     *
     * @param ex      excepción lanzada por Spring cuando @Valid falla
     * @param request información de la request que causó el error
     * @return mapa con los campos que fallaron y sus mensajes de error
     */
    // @ExceptionHandler: indica qué tipo de excepción maneja este método
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(
            MethodArgumentNotValidException ex, WebRequest request) {

        // Extraemos los errores de cada campo y los agrupamos en un mapa
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            // FieldError: representa el error de un campo específico del DTO
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });

        return buildErrorResponse(HttpStatus.BAD_REQUEST, "Error de validación", fieldErrors, request);
    }

    /**
     * Maneja el caso en que un recurso buscado no existe en la base de datos.
     * Devuelve HTTP 404 Not Found.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(
            ResourceNotFoundException ex, WebRequest request) {
        log.warn("Recurso no encontrado: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage(), null, request);
    }

    /**
     * Maneja violaciones de reglas de negocio (ej: email duplicado, reseña duplicada).
     * Devuelve HTTP 409 Conflict.
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessException(
            BusinessException ex, WebRequest request) {
        log.warn("Violación de regla de negocio: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.CONFLICT, ex.getMessage(), null, request);
    }

    /**
     * Maneja intentos de acceso a recursos ajenos.
     * Devuelve HTTP 403 Forbidden.
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorized(
            UnauthorizedException ex, WebRequest request) {
        log.warn("Acceso no autorizado: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.FORBIDDEN, ex.getMessage(), null, request);
    }

    /**
     * Maneja credenciales inválidas (email/contraseña incorrectos en el login).
     * Devuelve HTTP 401 Unauthorized.
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(
            BadCredentialsException ex, WebRequest request) {
        log.warn("Intento de login fallido: credenciales inválidas");
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, "Credenciales inválidas", null, request);
    }

    /**
     * Manejador genérico para cualquier excepción no capturada específicamente.
     * Devuelve HTTP 500 Internal Server Error. El error real se loguea internamente
     * pero NO se expone al cliente (seguridad).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(
            Exception ex, WebRequest request) {
        log.error("Error inesperado: {}", ex.getMessage(), ex);
        return buildErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Error interno del servidor",
            null,
            request
        );
    }

    /**
     * Método auxiliar que construye la estructura estándar de respuesta de error.
     * Todos los errores tienen el mismo formato para consistencia en la API.
     *
     * @param status  código de estado HTTP
     * @param message mensaje de error principal
     * @param details detalles adicionales (ej: mapa de errores de campo)
     * @param request información de la request original
     * @return ResponseEntity con el cuerpo de error estructurado
     */
    private ResponseEntity<Map<String, Object>> buildErrorResponse(
            HttpStatus status, String message, Object details, WebRequest request) {

        Map<String, Object> errorBody = new HashMap<>();
        errorBody.put("timestamp", LocalDateTime.now().toString());
        errorBody.put("status", status.value());
        errorBody.put("error", status.getReasonPhrase());
        errorBody.put("message", message);
        errorBody.put("path", request.getDescription(false).replace("uri=", ""));

        if (details != null) {
            errorBody.put("details", details);
        }

        return ResponseEntity.status(status).body(errorBody);
    }
}
