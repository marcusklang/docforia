package se.lth.cs.docforia.exceptions;

/**
 * Created by csz-mkg on 2016-05-11.
 */
public class DocumentIOError extends RuntimeException {
    public DocumentIOError() {
    }

    public DocumentIOError(String message) {
        super(message);
    }

    public DocumentIOError(String message, Throwable cause) {
        super(message, cause);
    }

    public DocumentIOError(Throwable cause) {
        super(cause);
    }

    public DocumentIOError(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
