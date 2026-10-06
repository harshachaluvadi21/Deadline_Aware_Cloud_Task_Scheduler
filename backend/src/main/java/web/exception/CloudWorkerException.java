package web.exception;

/**
 * Exception thrown when communication with the real execution worker node fails,
 * is rejected, or times out.
 */
public class CloudWorkerException extends RuntimeException {

    public CloudWorkerException(String message) {
        super(message);
    }

    public CloudWorkerException(String message, Throwable cause) {
        super(message, cause);
    }
}
