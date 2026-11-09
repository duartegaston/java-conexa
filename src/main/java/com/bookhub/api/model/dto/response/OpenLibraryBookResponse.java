package com.bookhub.api.model.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO que modela la respuesta de búsqueda de la Open Library API externa.
 *
 * <p>La estructura JSON de Open Library es:
 * <pre>
 * {
 *   "numFound": 100,
 *   "docs": [
 *     { "key": "/works/OL...", "title": "...", "author_name": [...], ... }
 *   ]
 * }
 * </pre></p>
 *
 * <p>Jackson (la librería de serialización JSON incluida en Spring Boot) mapea
 * automáticamente el JSON a estos objetos Java.</p>
 */
// @JsonIgnoreProperties(ignoreUnknown = true): Jackson ignora campos JSON que no están en la clase.
// Es importante para APIs externas que pueden agregar nuevos campos sin romper nuestra aplicación.
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenLibraryBookResponse {

    /** Total de resultados encontrados en Open Library */
    @JsonProperty("numFound")
    private Integer numFound;

    /** Lista de documentos (libros) encontrados */
    @JsonProperty("docs")
    private List<BookDoc> docs;

    /**
     * Clase interna que representa un libro individual en los resultados de búsqueda.
     * Mapeamos solo los campos que nos interesan.
     */
    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BookDoc {

        // @JsonProperty: mapea el campo JSON "key" al campo Java "key"
        // Útil cuando el nombre del JSON no sigue las convenciones Java (camelCase)
        @JsonProperty("key")
        private String key;

        @JsonProperty("title")
        private String title;

        // author_name es un array en el JSON (un libro puede tener múltiples autores)
        @JsonProperty("author_name")
        private List<String> authorName;

        @JsonProperty("first_publish_year")
        private Integer firstPublishYear;

        @JsonProperty("isbn")
        private List<String> isbn;

        // ID de la portada en Open Library (se usa para construir la URL de la imagen)
        @JsonProperty("cover_i")
        private Long coverId;

        /**
         * Extrae el OLID (Open Library ID) del campo "key" que tiene formato "/works/OL7353617M".
         *
         * @return el OLID limpio, ej: "OL7353617M", o null si no tiene key
         */
        public String extractOlid() {
            if (key == null) return null;
            // El key tiene formato "/works/OL7353617M", extraemos la última parte
            String[] parts = key.split("/");
            return parts[parts.length - 1];
        }

        /**
         * Devuelve el primer autor de la lista o "Desconocido" si no hay autores.
         *
         * @return nombre del primer autor
         */
        public String getPrimaryAuthor() {
            if (authorName == null || authorName.isEmpty()) return "Desconocido";
            return authorName.get(0);
        }

        /**
         * Devuelve el primer ISBN de la lista o null si no tiene.
         *
         * @return primer ISBN disponible
         */
        public String getPrimaryIsbn() {
            if (isbn == null || isbn.isEmpty()) return null;
            return isbn.get(0);
        }

        /**
         * Construye la URL de la portada del libro usando el ID de cobertura de Open Library.
         *
         * @return URL de la imagen de portada en tamaño mediano, o null si no tiene portada
         */
        public String buildCoverUrl() {
            if (coverId == null) return null;
            return "https://covers.openlibrary.org/b/id/" + coverId + "-M.jpg";
        }
    }
}
