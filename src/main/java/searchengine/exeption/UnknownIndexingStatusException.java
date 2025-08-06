package searchengine.exeption;

public class UnknownIndexingStatusException extends RuntimeException {

    public UnknownIndexingStatusException(String message) {
        super(message);
    }

    public UnknownIndexingStatusException(String message, Throwable cause) {
        super(message, cause);
    }
}
