package atlas.commands;

public class CommandExecutionException extends Exception {

	private static final long serialVersionUID = 1L;

	public CommandExecutionException(Throwable cause) {
		super("Unhandled exception while perfoming a command", cause, false, true);
	}
}
