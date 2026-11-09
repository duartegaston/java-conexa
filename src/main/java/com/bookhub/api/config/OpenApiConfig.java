package com.bookhub.api.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de Swagger UI / OpenAPI 3 para la documentación interactiva de la API.
 *
 * <p>Una vez levantada la app, la documentación está disponible en:</p>
 * <ul>
 *   <li>Swagger UI: http://localhost:8080/api/v1/swagger-ui.html</li>
 *   <li>OpenAPI JSON: http://localhost:8080/api/v1/v3/api-docs</li>
 * </ul>
 *
 * <p>Para probar endpoints protegidos en Swagger UI:
 * Hacer login → copiar el token → click en "Authorize" → pegar el token.</p>
 */
// @OpenAPIDefinition: configura los metadatos globales de la especificación OpenAPI
@OpenAPIDefinition(
    info = @Info(
        title = "BookHub API",
        version = "1.0.0",
        description = "API REST para gestión de libros con integración a Open Library. " +
                      "Proyecto de referencia para entrevistas técnicas con Spring Boot.",
        contact = @Contact(
            name = "BookHub Team",
            email = "contact@bookhub.com"
        )
    ),
    // @Server: define los servidores donde está disponible la API (útil para múltiples ambientes)
    servers = {
        @Server(url = "http://localhost:8080/api/v1", description = "Servidor local de desarrollo")
    }
)
// @SecurityScheme: define el esquema de autenticación usado en la API.
// Permite que Swagger UI muestre el botón "Authorize" para ingresar el token JWT.
@SecurityScheme(
    name = "bearerAuth",          // nombre del esquema, referenciado desde los controllers
    type = SecuritySchemeType.HTTP, // tipo: HTTP (en lugar de apiKey u OAuth2)
    scheme = "bearer",             // esquema HTTP específico: Bearer Token
    bearerFormat = "JWT",          // formato del token (solo informativo para Swagger UI)
    description = "Ingresa el token JWT obtenido del endpoint /auth/login. " +
                  "Formato: Bearer <token>"
)
@Configuration
public class OpenApiConfig {
    // Esta clase solo necesita las anotaciones. Spring y springdoc hacen el resto.
}
