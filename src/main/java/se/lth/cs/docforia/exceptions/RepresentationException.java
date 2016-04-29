package se.lth.cs.docforia.exceptions;

/**
 * Representation exception, thrown if representation cannot be found or constructed.
 */
public class RepresentationException extends RuntimeException {
    public RepresentationException() {
    }

    public RepresentationException(String message) {
        super(message);
    }

    public RepresentationException(String message, Throwable cause) {
        super(message, cause);
    }

    public RepresentationException(Throwable cause) {
        super(cause);
    }

    public RepresentationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
