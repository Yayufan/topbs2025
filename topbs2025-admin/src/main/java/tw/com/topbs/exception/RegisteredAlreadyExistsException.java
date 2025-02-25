package tw.com.topbs.exception;

public class RegisteredAlreadyExistsException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public RegisteredAlreadyExistsException(String message) {
        super(message);
    }

}
