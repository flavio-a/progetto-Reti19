package server;

public class SectionBusyException extends Exception {
	public SectionBusyException(Integer n) {
		super("Section " + n.toString() + " already being modified");
	}
}
