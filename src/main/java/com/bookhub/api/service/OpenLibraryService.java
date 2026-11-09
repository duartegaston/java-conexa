package com.bookhub.api.service;

import com.bookhub.api.model.dto.response.OpenLibraryBookResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.List;

/**
 * Servicio para consumir la Open Library API externa.
 *
 * <p>Open Library es una API pública y gratuita de Internet Archive que permite
 * buscar libros por título, autor, ISBN, etc.</p>
 *
 * <p>Documentación: https://openlibrary.org/developers/api</p>
 *
 * <p>Este servicio es el único punto de contacto con la API externa.
 * Al centralizarlo aquí, si la API externa cambia, solo modificamos este servicio.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OpenLibraryService {

    // RestTemplate: cliente HTTP de Spring para llamar a APIs REST externas de forma síncrona
    private final RestTemplate restTemplate;

    @Value("${open-library.search-url}")
    private String searchUrl;

    /**
     * Busca libros en Open Library API según un término de búsqueda.
     *
     * <p>Llama a: GET https://openlibrary.org/search.json?q={query}&limit={limit}</p>
     *
     * @param query término de búsqueda (título, autor, ISBN)
     * @param limit máximo de resultados a devolver
     * @return lista de libros encontrados en Open Library, vacía si hay error
     */
    public List<OpenLibraryBookResponse.BookDoc> searchBooks(String query, int limit) {
        try {
            // UriComponentsBuilder: construye URLs con parámetros de forma segura (escapa caracteres especiales)
            String url = UriComponentsBuilder.fromHttpUrl(searchUrl)
                    .queryParam("q", query)
                    .queryParam("limit", limit)
                    // fields: solicitamos solo los campos que necesitamos (optimización de ancho de banda)
                    .queryParam("fields", "key,title,author_name,first_publish_year,isbn,cover_i")
                    .toUriString();

            log.info("Buscando en Open Library: {}", url);

            // getForObject: realiza GET, deserializa el JSON de respuesta al tipo indicado
            // Si la respuesta no es 2xx, lanza RestClientException
            OpenLibraryBookResponse response = restTemplate.getForObject(
                url,
                OpenLibraryBookResponse.class
            );

            if (response == null || response.getDocs() == null) {
                log.warn("Open Library devolvió respuesta vacía para la búsqueda: {}", query);
                return Collections.emptyList();
            }

            log.info("Open Library encontró {} resultados para '{}'",
                response.getNumFound(), query);

            return response.getDocs();

        } catch (RestClientException ex) {
            // Registramos el error pero no lanzamos excepción para no romper la experiencia del usuario
            log.error("Error al conectar con Open Library API: {}", ex.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Obtiene los detalles de un libro específico por su OLID de Open Library.
     *
     * <p>Llama a: GET https://openlibrary.org/works/{olid}.json</p>
     *
     * @param olid identificador único de Open Library (ej: "OL7353617M")
     * @return los detalles del libro o null si no se encuentra
     */
    public OpenLibraryBookResponse.BookDoc getBookByOlid(String olid) {
        // Buscamos directamente por el OLID que tiene formato "OL...M"
        List<OpenLibraryBookResponse.BookDoc> results = searchBooks(olid, 1);

        if (results.isEmpty()) {
            log.warn("No se encontró libro con OLID: {}", olid);
            return null;
        }

        return results.get(0);
    }
}
