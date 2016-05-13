package se.lth.cs.docforia.exceptions;

/**
 * Created by csz-mkg on 2016-05-11.
 */
public class DocumentReaderException extends DocumentIOError {
    public DocumentReaderException() {
    }

    public DocumentReaderException(String message) {
        super(message);
    }

    public DocumentReaderException(String message, Throwable cause) {
        super(message, cause);
    }

    public DocumentReaderException(Throwable cause) {
        super(cause);
    }

    public DocumentReaderException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
