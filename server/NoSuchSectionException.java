package server;

// Runtime exception to be thrown during loops over a file's lines.
public class NoSuchSectionException extends Exception {
	public NoSuchSectionException(Integer n) {
		super("Section " + n.toString() + " doesn't exist");
	}
}
