package com.bookhub.api.model.entity;

/**
 * Enum que representa el estado de un libro en la lista de lectura personal de un usuario.
 */
public enum ReadingStatus {

    /** El usuario quiere leer este libro en el futuro. */
    WANT_TO_READ,

    /** El usuario está leyendo este libro actualmente. */
    READING,

    /** El usuario ya terminó de leer este libro. */
    COMPLETED
}
