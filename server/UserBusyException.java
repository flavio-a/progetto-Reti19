package server;

// Runtime exception to be thrown during loops over a file's lines.
public class UserBusyException extends Exception {
	public UserBusyException(String usr) {
		super("User " + usr + " is editing something else");
	}
}
