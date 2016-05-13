package se.lth.cs.docforia.exceptions;

/**
 * Created by csz-mkg on 2016-05-11.
 */
public class DocumentWriterException extends DocumentIOError {
    public DocumentWriterException() {
    }

    public DocumentWriterException(String message) {
        super(message);
    }

    public DocumentWriterException(String message, Throwable cause) {
        super(message, cause);
    }

    public DocumentWriterException(Throwable cause) {
        super(cause);
    }

    public DocumentWriterException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
