package server.lib;

import java.util.*;
import java.util.stream.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.nio.charset.*;
import static java.nio.file.StandardOpenOption.*;

/**
 * Class of IO utility functions.
 */
public final class IOUtils {
	public static final String linesep = System.getProperty("line.separator");
	public static final Charset utf8 = StandardCharsets.UTF_8;

	// ================================ FILES ================================
	/**
	 * Add a row to a file.
	 *
	 * @param f path to the file relative to root
	 * @param text the line of text to add. There's no check against newlines
	 *             in this string
	 */
	public static void addRow(Path f, String text) throws IOException {
		byte[] bytes = (text + linesep).getBytes(StandardCharsets.UTF_8);
		Files.write(f, bytes, StandardOpenOption.APPEND);
	}

	/**
	 * Look for a row in a file.
	 *
	 * @param f path to the file relative to root
	 * @param text the line of text to look for. A line of the file matches text
	 *             excluding leading and trailing blanks (ie: both are trimmed)
	 * @param skip number of lines at the begining of the file to skip
	 * @return true iff the row is found
	 */
	public static boolean searchRow(Path f, String text, int skip) throws IOException {
		final String trim_text = text.trim();
		try (
			FileLineReader reader = new FileLineReader(f);
		) {
			return StreamSupport.stream(reader.spliterator(), false)
						.skip(skip)
						.anyMatch(line -> trim_text.equals(line.trim()));
		}
	}

	/**
	 * Remove all row equals to a given text from a file. Not synchronized.
	 *
	 * @param f path to the file relative to root
	 * @param text the line to remove. Comparisions are as in searchRow
	 * @param skip number of lines at the begining of the file to skip
	 *
	 * TODO: replace writer with nio
	 */
	public static void deleteRows(Path f, String text, int skip) throws IOException {
		final String trim_text = text.trim();
		// Implementation: the file is read line by line and lines not matching
		// are written to a temporary file. At the end, the temporary file is
		// moved to replace the original one
		Path tmp_file = f.getParent().resolve(f.getFileName().toString() + "_tmp");
		try (
			BufferedWriter writer = Files.newBufferedWriter(tmp_file);
			FileLineReader reader = new FileLineReader(f);
		) {
			int i = 0;
			for (String line : reader) {
				if (i < skip || !trim_text.equals(line.trim())) {
					writer.write(line + System.getProperty("line.separator"));
				}
				++i;
			}
		}
		catch (IOException e) {
			Files.delete(tmp_file);
			throw e;
		}
		Files.move(tmp_file, f, StandardCopyOption.REPLACE_EXISTING);
	}

	/**
	 * Creates a file with the specified content.
	 *
	 * @param f path to file to create
	 * @param content content of the file at its creation
	 */
	public static void createFileContent(Path f, String content) throws IOException {
		Files.write(f, (content + linesep).getBytes(utf8), CREATE_NEW, WRITE);
	}

	// =============================== CHANNELS ===============================
	/**
	 * Copies a channel to a file using nio and direct channels tranfer. The
	 * file is overwritten with the new content. The channel copied should
	 * start with a long (Long.BYTES bytes) with the length of the
	 * following file.
	 *
	 * @param chnl channel to copy from
	 * @param f path to the file.
	 */
	public static void channelToFile(ReadableByteChannel chnl, Path f) throws IOException {
		ByteBuffer sizebuff = ByteBuffer.allocate(Long.BYTES);
		sizebuff.clear();
		while (sizebuff.remaining() > 0) {
			chnl.read(sizebuff);
		}
		sizebuff.flip();
		long filesize = sizebuff.getLong();
		try (
			FileChannel outFile = FileChannel.open(f, WRITE, CREATE, TRUNCATE_EXISTING);
		) {
			long pos = 0;
			while (pos < filesize) {
				long count = outFile.transferFrom(chnl, pos, filesize - pos);
				pos += count;
			}
		}
	}

	/**
	 * Copies a whole file to a channel using nio and direct channels tranfer.
	 * Before file's content, in the channel is written a long with the length
	 * of the following file.
	 *
	 * @param f path to the file.
	 * @param chnl channel to copy to
	 */
	public static void fileToChannel(Path f, WritableByteChannel chnl) throws IOException {
		try (
			FileChannel inFile = FileChannel.open(f, READ);
		) {
			long filesize = inFile.size();
			ByteBuffer sizebuff = ByteBuffer.allocate(Long.BYTES);
			sizebuff.clear();
	    	sizebuff.putLong(filesize);
			sizebuff.flip();
			while (sizebuff.remaining() > 0) {
				chnl.write(sizebuff);
			}
			long pos = 0;
			while (pos < filesize) {
				long count = inFile.transferTo(pos, filesize - pos, chnl);
				pos += count;
			}
		}
	}

	// ================================= TEST =================================
	public static void testRowsOps() {
		String randomline = "oibruaiovbrowa", randomline2 = "giancarlo";
		try {
			Path totf = Files.createTempFile(randomline, null);
			addRow(totf, randomline);
			addRow(totf, randomline2);
			if (!searchRow(totf, randomline, 0)) {
				throw new RuntimeException("First line written not found!");
			}
			if (!searchRow(totf, randomline2, 1)) {
				throw new RuntimeException("Second line written not found!");
			}
			if (searchRow(totf, randomline, 1)) {
				throw new RuntimeException("Line found, should have skipped!");
			}
		}
		catch (IOException e) {
			throw new RuntimeException("IOException :c");
		}
	}

}
