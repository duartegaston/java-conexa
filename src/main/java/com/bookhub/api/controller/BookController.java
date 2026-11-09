package com.bookhub.api.controller;

import com.bookhub.api.model.dto.response.BookResponse;
import com.bookhub.api.model.dto.response.OpenLibraryBookResponse;
import com.bookhub.api.service.BookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller REST para los endpoints de gestión de libros.
 *
 * <p>Rutas base: /api/v1/books</p>
 *
 * <p>Endpoints públicos (sin JWT): GET /search, GET /*, GET /{id}</p>
 * <p>Endpoints protegidos (requieren JWT): POST /import/{olid}</p>
 */
@RestController
@RequestMapping("/books")
@RequiredArgsConstructor
@Tag(name = "Libros", description = "Búsqueda e importación de libros desde Open Library")
public class BookController {

    private final BookService bookService;

    /**
     * Busca libros en la Open Library API externa.
     * Este endpoint es público, no requiere autenticación.
     *
     * @param q     término de búsqueda (título, autor, ISBN)
     * @param limit máximo de resultados a devolver (por defecto 20)
     * @return lista de libros encontrados en Open Library
     */
    @Operation(
        summary = "Buscar en Open Library",
        description = "Busca libros en la API externa de Open Library por título, autor o ISBN"
    )
    // @GetMapping: mapea peticiones HTTP GET a este método
    @GetMapping("/search")
    public ResponseEntity<List<OpenLibraryBookResponse.BookDoc>> search(
            // @RequestParam: extrae el parámetro de la query string (?q=...)
            // required = false: el parámetro no es obligatorio
            // defaultValue: valor por defecto si no se proporciona
            @Parameter(description = "Término de búsqueda")
            @RequestParam String q,
            @RequestParam(defaultValue = "20") int limit) {

        List<OpenLibraryBookResponse.BookDoc> results = bookService.searchInOpenLibrary(q, limit);
        return ResponseEntity.ok(results);
    }

    /**
     * Importa un libro desde Open Library a la base de datos local.
     * Requiere autenticación JWT y rol USER o ADMIN.
     *
     * @param olid identificador único de Open Library (ej: "OL7353617M")
     * @return 201 Created con los datos del libro importado
     */
    @Operation(
        summary = "Importar libro",
        description = "Importa un libro de Open Library a la base de datos local usando su OLID",
        // @SecurityRequirement: indica en Swagger UI que este endpoint requiere autenticación
        security = @SecurityRequirement(name = "bearerAuth")
    )
    // @PostMapping: mapea peticiones HTTP POST a /books/import/{olid}
    @PostMapping("/import/{olid}")
    // @PreAuthorize: verifica el permiso ANTES de ejecutar el método.
    // isAuthenticated(): solo requiere que el usuario esté autenticado (cualquier rol)
    // Se puede usar: hasRole('ADMIN'), hasAnyRole('USER', 'ADMIN'), hasAuthority('ROLE_USER')
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BookResponse> importBook(
            // @PathVariable: extrae el valor del segmento {olid} de la URL
            @Parameter(description = "Identificador único de Open Library (ej: OL7353617M)")
            @PathVariable String olid) {

        BookResponse response = bookService.importBook(olid);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Lista todos los libros importados en la base de datos local, con paginación.
     * Este endpoint es público.
     *
     * @param pageable parámetros de paginación (page, size, sort) automáticamente extraídos de la URL
     * @return página de libros con sus métricas
     */
    @Operation(
        summary = "Listar libros locales",
        description = "Devuelve todos los libros importados con paginación. " +
                      "Ejemplo: /books?page=0&size=10&sort=title,asc"
    )
    @GetMapping
    public ResponseEntity<Page<BookResponse>> getAllBooks(
            // @PageableDefault: valores por defecto para la paginación si no se especifican en la URL
            // Spring convierte automáticamente los parámetros ?page=0&size=10 en un objeto Pageable
            @PageableDefault(size = 10, sort = "title") Pageable pageable) {

        return ResponseEntity.ok(bookService.getAllBooks(pageable));
    }

    /**
     * Obtiene el detalle de un libro específico por su ID en la BD local.
     *
     * @param id ID del libro en la base de datos local
     * @return 200 OK con los detalles del libro o 404 si no existe
     */
    @Operation(summary = "Obtener libro por ID", description = "Devuelve el detalle de un libro local")
    // @GetMapping("/{id}"): mapea GET /books/{id}
    @GetMapping("/{id}")
    public ResponseEntity<BookResponse> getBookById(
            @Parameter(description = "ID del libro en la BD local")
            @PathVariable Long id) {

        return ResponseEntity.ok(bookService.getBookById(id));
    }
}
