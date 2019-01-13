package server.lib;

public class InternalServerException extends Exception {
	public InternalServerException(String msg) {
		super("Internal server error: " + msg);
	}
}
