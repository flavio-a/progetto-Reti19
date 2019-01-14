package server;

// Runtime exception to be thrown during loops over a file's lines.
public class UserFreeException extends Exception {
	public UserFreeException(String usr) {
		super("User " + usr + " isn't editing anything");
	}
}
