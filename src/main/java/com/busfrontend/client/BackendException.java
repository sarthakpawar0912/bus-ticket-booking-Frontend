package com.busfrontend.client;

/**
 * Surfaces a failed backend REST call to view controllers. Controllers
 * typically catch and flash {@code getMessage()} back to the user.
 */
public class BackendException extends RuntimeException {
    private final int status;

    public BackendException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int getStatus() { return status; }
}
