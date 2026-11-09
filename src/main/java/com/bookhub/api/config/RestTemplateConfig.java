package com.bookhub.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configuración del cliente HTTP para consumir APIs externas.
 *
 * <p>RestTemplate es el cliente HTTP síncrono de Spring MVC para consumir APIs REST externas.
 * En Spring Boot 3+ existe también WebClient (reactivo/asíncrono), pero RestTemplate
 * sigue siendo válido para aplicaciones síncronas tradicionales.</p>
 */
@Configuration
public class RestTemplateConfig {

    /**
     * Crea y registra un RestTemplate como bean de Spring.
     *
     * <p>Al tenerlo como bean, podemos inyectarlo en cualquier service con @Autowired
     * o inyección por constructor, y facilita el mockeo en tests.</p>
     *
     * @return instancia de RestTemplate configurada
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
