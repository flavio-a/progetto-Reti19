package server.lib;

public class UsernameAlreadyInUseException extends Exception {
	public UsernameAlreadyInUseException(String usr) {
		super("The username \"" + usr + "\" is alredy in use.");
	}
}
