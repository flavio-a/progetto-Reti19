package server.lib;

import java.util.*;
import java.lang.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.nio.charset.*;

/**
 * A file reader working at level of lines. It's a wrapper around a
 * FileChannel with ByteBuffer to get a higher-level interface. Allows only
 * one iterator at a time.
 */
public class FileLineReader implements Iterable<String>, Closeable, AutoCloseable {
	private static final Charset utf8 = StandardCharsets.UTF_8;
	private static final String linesep = System.getProperty("line.separator");

	private final FileChannel filechannel;
	private final ByteBuffer buffer;
	private String buffer_string;
	private boolean finished = false;
	private boolean isIterated;

	public FileLineReader(Path f) throws IOException {
		filechannel = FileChannel.open(f, StandardOpenOption.READ);
		buffer = ByteBuffer.allocate(1024 * 4); // Up to 1024 UTF-8 characters
		buffer.clear();
		buffer_string = "";
		isIterated = false;
	}

	// Check if there are more lines to read
	public boolean isEmpty() {
		return finished;
	}

	// Reads the next line, ie. until next linesep (that is trimmed)
	public String readLine() throws IOException {
		int idx;
		while ((idx = buffer_string.indexOf(linesep)) == -1
				&& filechannel.read(buffer) != -1) {
			buffer_string += utf8.decode(buffer).toString();
			buffer.clear();
		}
		if (idx == -1) {
			finished = true;
			return buffer_string;
		}
		String line = buffer_string.substring(0, idx);
		buffer_string = buffer_string.substring(idx + 1);
		return line;
	}

	public Iterator<String> iterator() {
		if (isIterated) {
			throw new IllegalStateException("There's already an iterator on this FileLineReader");
		}
		else {
			return new LineIterator();
		}
	}

	// Inner iterator class
	private class LineIterator implements Iterator<String> {
		public LineIterator() { }

		public boolean hasNext() {
			return !FileLineReader.this.isEmpty();
		}

		public String next() {
			try {
				return FileLineReader.this.readLine();
			}
			catch (IOException e) {
				throw new FileIteratorException(e);
			}
		}

        public void remove() {
            throw new UnsupportedOperationException();
        }
	}

	// Closeable interface
	@Override
	public void close() throws IOException {
		filechannel.close();
	}
}

// try (
// 	FileChannel outFile = FileChannel.open(Paths.get("translated.log.txt"), StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
// ) {
// 	for (String result : results) {
// 		outFile.write(ByteBuffer.wrap(result.getBytes(utf8)));
// 	}
// }
// catch (IOException e) {
// 	System.out.println("Error while writing file");
// }
