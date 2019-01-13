package server.lib;

import java.io.IOException;

// Runtime exception to be thrown during loops over a file's lines.
public class FileIteratorException extends RuntimeException {
	IOException cause;

	public FileIteratorException(IOException cause_set) {
		cause = cause_set;
	}

	public IOException getCause() {
		return cause;
	}
}
