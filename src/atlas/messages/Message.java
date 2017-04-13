package atlas.messages;

public interface Message {

	String getName();
	MessageType getType();
	
	public enum MessageType {
		
		NONE, MESSAGE, ACTIONBAR, TITLE
	}
}
