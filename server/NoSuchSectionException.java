package server;

public class NoSuchSectionException extends Exception {
	public NoSuchSectionException(Integer n) {
		super("Section " + n.toString() + " doesn't exist");
	}
}
