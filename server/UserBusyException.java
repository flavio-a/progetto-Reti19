package server;

public class UserBusyException extends Exception {
	public UserBusyException(String usr) {
		super("User " + usr + " is editing something else");
	}
}
