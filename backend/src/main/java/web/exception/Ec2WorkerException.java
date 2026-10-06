package web.exception;

/**
 * Backward-compatibility alias for {@link CloudWorkerException}.
 */
public class Ec2WorkerException extends CloudWorkerException {

    public Ec2WorkerException(String message) {
        super(message);
    }

    public Ec2WorkerException(String message, Throwable cause) {
        super(message, cause);
    }
}
