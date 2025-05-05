package tw.com.topbs.exception;

public class RegistrationClosedException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public RegistrationClosedException(String message) {
        super(message);
    }
}
