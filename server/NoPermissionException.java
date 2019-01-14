package server;

// Runtime exception to be thrown during loops over a file's lines.
public class NoPermissionException extends Exception {
	public NoPermissionException(String usr, String doc) {
		super("User \"" + usr + "\" doesn't have permission to edit " + doc);
	}
}
