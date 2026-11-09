package com.bookhub.api.controller;

import com.bookhub.api.model.dto.request.ReadingListRequest;
import com.bookhub.api.model.dto.response.ReadingListResponse;
import com.bookhub.api.service.ReadingListService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller REST para la gestión de la lista de lectura personal del usuario.
 *
 * <p>Todos los endpoints de este controller requieren autenticación.</p>
 * <p>Ruta base: /api/v1/reading-list</p>
 */
@RestController
@RequestMapping("/reading-list")
@RequiredArgsConstructor
// @SecurityRequirement a nivel de clase: todos los endpoints de este controller requieren bearerAuth
@Tag(name = "Lista de Lectura", description = "Gestión de la lista de lectura personal")
public class ReadingListController {

    private final ReadingListService readingListService;

    /**
     * Agrega un libro a la lista de lectura del usuario autenticado.
     *
     * @param request     DTO con ID del libro y estado inicial
     * @param userDetails usuario autenticado
     * @return 201 Created con los datos de la entrada creada
     */
    @Operation(
        summary = "Agregar a lista de lectura",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReadingListResponse> addToList(
            @Valid @RequestBody ReadingListRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        ReadingListResponse response = readingListService.addToReadingList(
            request,
            userDetails.getUsername()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Obtiene la lista de lectura completa del usuario autenticado.
     *
     * @param userDetails usuario autenticado
     * @return lista de entradas de lectura del usuario
     */
    @Operation(
        summary = "Ver mi lista de lectura",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ReadingListResponse>> getMyList(
            @AuthenticationPrincipal UserDetails userDetails) {

        List<ReadingListResponse> list = readingListService.getMyReadingList(
            userDetails.getUsername()
        );

        return ResponseEntity.ok(list);
    }

    /**
     * Actualiza el estado de lectura de un libro en la lista del usuario.
     *
     * @param entryId     ID de la entrada en la lista
     * @param request     DTO con el nuevo estado
     * @param userDetails usuario autenticado
     * @return 200 OK con los datos actualizados
     */
    @Operation(
        summary = "Actualizar estado de lectura",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    // @PutMapping: mapea peticiones HTTP PUT (actualización completa del recurso)
    @PutMapping("/{entryId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReadingListResponse> updateStatus(
            @PathVariable Long entryId,
            @Valid @RequestBody ReadingListRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        ReadingListResponse response = readingListService.updateReadingStatus(
            entryId,
            request,
            userDetails.getUsername()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Elimina un libro de la lista de lectura del usuario.
     *
     * @param entryId     ID de la entrada a eliminar
     * @param userDetails usuario autenticado
     * @return 204 No Content
     */
    @Operation(
        summary = "Eliminar de lista de lectura",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @DeleteMapping("/{entryId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> removeFromList(
            @PathVariable Long entryId,
            @AuthenticationPrincipal UserDetails userDetails) {

        readingListService.removeFromReadingList(entryId, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}
