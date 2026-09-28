package org.jcerdeira.saijknowledgegraphapi.model;

import java.time.LocalDateTime;

/**
 * DTO for standardizing error responses across the API.
 * Implemented as a Record for immutability and conciseness.
 */
public record ErrorResponse(LocalDateTime timestamp, int status, String error, String message, String path) {

    /**
     * Canonical constructor wrapper to automatically set the timestamp.
     */
    public ErrorResponse(int status, String error, String message, String path) {
        this(LocalDateTime.now(), status, error, message, path);
    }
}