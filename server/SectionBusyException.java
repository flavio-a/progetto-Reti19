package server;

// Runtime exception to be thrown during loops over a file's lines.
public class SectionBusyException extends Exception {
	public SectionBusyException(Integer n) {
		super("Section " + n.toString() + " already being modified");
	}
}
