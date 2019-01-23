package server;

public class NoSuchDocumentException extends Exception {
	public NoSuchDocumentException(String docname) {
		super("Document " + docname + " doesn't exist");
	}
}
