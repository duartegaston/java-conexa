package com.bookhub.api.service;

import com.bookhub.api.exception.BusinessException;
import com.bookhub.api.exception.ResourceNotFoundException;
import com.bookhub.api.exception.UnauthorizedException;
import com.bookhub.api.model.dto.request.ReadingListRequest;
import com.bookhub.api.model.dto.response.ReadingListResponse;
import com.bookhub.api.model.entity.Book;
import com.bookhub.api.model.entity.ReadingListEntry;
import com.bookhub.api.model.entity.User;
import com.bookhub.api.repository.BookRepository;
import com.bookhub.api.repository.ReadingListRepository;
import com.bookhub.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio para la gestión de la lista de lectura personal de cada usuario.
 *
 * <p>Cada usuario tiene su propia lista donde puede agregar libros y
 * actualizar su estado de lectura (WANT_TO_READ, READING, COMPLETED).</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReadingListService {

    private final ReadingListRepository readingListRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;

    /**
     * Agrega un libro a la lista de lectura personal del usuario autenticado.
     *
     * @param request   DTO con el ID del libro y el estado inicial
     * @param userEmail email del usuario autenticado
     * @return ReadingListResponse con los datos de la entrada creada
     * @throws ResourceNotFoundException si el libro no existe
     * @throws BusinessException         si el libro ya está en la lista del usuario
     */
    @Transactional
    public ReadingListResponse addToReadingList(ReadingListRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "email", userEmail));

        Book book = bookRepository.findById(request.getBookId())
                .orElseThrow(() -> new ResourceNotFoundException("Libro", "id", request.getBookId()));

        // Un mismo libro no puede estar dos veces en la lista del mismo usuario
        if (readingListRepository.existsByUserIdAndBookId(user.getId(), book.getId())) {
            throw new BusinessException("El libro '" + book.getTitle() + "' ya está en tu lista de lectura.");
        }

        ReadingListEntry entry = ReadingListEntry.builder()
                .user(user)
                .book(book)
                .status(request.getStatus())
                .build();

        ReadingListEntry savedEntry = readingListRepository.save(entry);
        log.info("'{}' agregó '{}' a su lista con estado {}",
            userEmail, book.getTitle(), request.getStatus());

        return mapToResponse(savedEntry);
    }

    /**
     * Obtiene toda la lista de lectura del usuario autenticado.
     *
     * @param userEmail email del usuario autenticado
     * @return lista de entradas de lectura del usuario
     */
    @Transactional(readOnly = true)
    public List<ReadingListResponse> getMyReadingList(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", "email", userEmail));

        return readingListRepository.findByUserId(user.getId())
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Actualiza el estado de lectura de una entrada en la lista del usuario.
     *
     * @param entryId   ID de la entrada a actualizar
     * @param request   DTO con el nuevo estado
     * @param userEmail email del usuario autenticado
     * @return ReadingListResponse con los datos actualizados
     * @throws ResourceNotFoundException si la entrada no existe
     * @throws UnauthorizedException     si la entrada pertenece a otro usuario
     */
    @Transactional
    public ReadingListResponse updateReadingStatus(Long entryId, ReadingListRequest request,
                                                    String userEmail) {
        ReadingListEntry entry = readingListRepository.findById(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("Entrada de lista", "id", entryId));

        // Verificamos que el usuario solo pueda modificar sus propias entradas
        if (!entry.getUser().getEmail().equals(userEmail)) {
            throw new UnauthorizedException("No tienes permiso para modificar esta entrada.");
        }

        entry.setStatus(request.getStatus());
        ReadingListEntry updatedEntry = readingListRepository.save(entry);
        log.info("Estado actualizado a {} para la entrada {} del usuario '{}'",
            request.getStatus(), entryId, userEmail);

        return mapToResponse(updatedEntry);
    }

    /**
     * Elimina una entrada de la lista de lectura del usuario.
     *
     * @param entryId   ID de la entrada a eliminar
     * @param userEmail email del usuario autenticado
     * @throws ResourceNotFoundException si la entrada no existe
     * @throws UnauthorizedException     si la entrada pertenece a otro usuario
     */
    @Transactional
    public void removeFromReadingList(Long entryId, String userEmail) {
        ReadingListEntry entry = readingListRepository.findById(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("Entrada de lista", "id", entryId));

        if (!entry.getUser().getEmail().equals(userEmail)) {
            throw new UnauthorizedException("No tienes permiso para eliminar esta entrada.");
        }

        readingListRepository.delete(entry);
        log.info("'{}' eliminó la entrada {} de su lista", userEmail, entryId);
    }

    /**
     * Mapea una entidad ReadingListEntry a su DTO de respuesta.
     *
     * @param entry entidad a transformar
     * @return DTO con los datos de la entrada
     */
    private ReadingListResponse mapToResponse(ReadingListEntry entry) {
        return ReadingListResponse.builder()
                .id(entry.getId())
                .status(entry.getStatus())
                .addedAt(entry.getAddedAt())
                .updatedAt(entry.getUpdatedAt())
                .bookId(entry.getBook().getId())
                .bookTitle(entry.getBook().getTitle())
                .bookAuthor(entry.getBook().getAuthor())
                .coverUrl(entry.getBook().getCoverUrl())
                .build();
    }
}
