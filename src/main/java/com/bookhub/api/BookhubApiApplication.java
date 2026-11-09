package com.bookhub.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Clase principal de la aplicación BookHub API.
 *
 * <p>Esta es la entrada (entry point) de la aplicación Spring Boot.
 * Al ejecutar este método main, Spring Boot:</p>
 * <ul>
 *   <li>Escanea todos los componentes anotados en el paquete base</li>
 *   <li>Configura el contenedor de IoC (Inversion of Control)</li>
 *   <li>Levanta el servidor Tomcat embebido</li>
 *   <li>Aplica toda la auto-configuración de Spring Boot</li>
 * </ul>
 */
// @SpringBootApplication: combina tres anotaciones en una:
//   - @Configuration: marca esta clase como fuente de definiciones de beans
//   - @EnableAutoConfiguration: activa la auto-configuración de Spring Boot según el classpath
//   - @ComponentScan: escanea el paquete actual y subpaquetes en busca de @Component, @Service, etc.
@SpringBootApplication
public class BookhubApiApplication {

    /**
     * Punto de entrada de la aplicación.
     *
     * @param args argumentos de línea de comandos (pueden sobrescribir propiedades de application.yml)
     */
    public static void main(String[] args) {
        SpringApplication.run(BookhubApiApplication.class, args);
    }
}
