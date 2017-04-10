package atlas.config;

public class CorruptedConfigException extends Exception {

	private static final long serialVersionUID = 1L;

	public CorruptedConfigException() {
		
		super("A configuration file may be corrupted");
	}

	public CorruptedConfigException(String message) {
		super(message);
		
	}

	public CorruptedConfigException(Throwable cause) {
		super("A configuration file may be corrupted", cause);
		
	}

	public CorruptedConfigException(String message, Throwable cause) {
		super(message, cause);
		
	}

	public CorruptedConfigException(String message, Throwable cause, 
									boolean enableSuppression, boolean writableStackTrace) {
		
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
