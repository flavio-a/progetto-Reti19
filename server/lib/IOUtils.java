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
	public static final Charset encoding = Constants.encoding;

	// ================================ FILES ================================
	/**
	 * Add a row to a file.
	 *
	 * @param f path to the file relative to root
	 * @param text the line of text to add. There's no check against newlines
	 *             in this string
	 */
	public static void addRow(Path f, String text) throws IOException {
		byte[] bytes = (text + linesep).getBytes(encoding);
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
	 * Creates a file with the specified content.
	 *
	 * @param f path to file to create
	 * @param content content of the file at its creation
	 */
	public static void createFileContent(Path f, String content) throws IOException {
		Files.write(f, (content + linesep).getBytes(encoding), CREATE_NEW, WRITE);
	}

	// =============================== CHANNELS ===============================
	/**
	 * Utility function to read a fixed amount of unformatted bytes from a
	 * channel to a ByteBuffer.
	 */
	private static ByteBuffer readData(ReadableByteChannel chnl, int size) throws IOException, ChannelClosedException {
		ByteBuffer buff = ByteBuffer.allocate(size);
		buff.clear();
		while (buff.remaining() > 0) {
			if (chnl.read(buff) == -1) {
				throw new ChannelClosedException();
			}
		}
		buff.flip();
		return buff;
	}


	/**
	 * Get a byte from a channel.
	 *
	 * @param chnl channel to read from
	 * @return byte read from the chnl
	 * @throws ChannelClosedException if the read-end of the channel has been
	 *                                closed
	 */
	public static byte readByte(ReadableByteChannel chnl) throws IOException, ChannelClosedException {
		return readData(chnl, 1).get();
	}

	/**
	 * Write a byte on a channel
	 *
	 * @param b byte to write
	 * @param chnl channel to write to
	 */
	public static void writeByte(byte b, WritableByteChannel chnl) throws IOException {
		ByteBuffer buff = ByteBuffer.allocate(1);
		buff.clear();
		buff.put(b);
		buff.flip();
		while (buff.remaining() > 0) {
			chnl.write(buff);
		}
	}


	/**
	 * Get the kind of operation from a channel.
	 *
	 * @param chnl channel to read from
	 * @return OpKind read from the chnl
	 * @throws ChannelClosedException if the read-end of the channel has been
	 *                                closed
	 */
	public static OpKind readOpKind(ReadableByteChannel chnl) throws IOException, ChannelClosedException {
		return OpKind.getOp(readByte(chnl));
	}

	/**
	 * Get the kind of operation from a channel if it's already been
	 * transmitted.
	 * <p>
	 * Assumes that the channel is in non-blocking mode, and try to read an
	 * OpKind istantaneously. If there's nothing to read, return an empty
	 * optional.
	 *
	 * @param chnl channel to read from
	 * @return the Optional OpKind read from the chnl, or empty if it wasn't
	 *         available
	 * @throws ChannelClosedException if the read-end of the channel has been
	 *                                closed
	 */
	public static Optional<OpKind> tryReadOpKind(ReadableByteChannel chnl) throws IOException, ChannelClosedException {
		ByteBuffer buff = ByteBuffer.allocate(1);
		buff.clear();
		long read = chnl.read(buff);
		if (read == -1) {
			throw new ChannelClosedException();
		}
		else if (read == 0) {
			return Optional.empty();
		}
		else {
			buff.flip();
			return Optional.of(OpKind.getOp(buff.get()));
		}
	}

	/**
	 * Write an OpKind on a channel
	 *
	 * @param op kind of operation to write
	 * @param chnl channel to write to
	 */
	public static void writeOpKind(OpKind op, WritableByteChannel chnl) throws IOException {
		writeByte(OpKind.getNum(op), chnl);
	}


	/**
	 * Read an int from a channel.
	 *
	 * @param chnl channel to read from
	 * @return int read from the chnl
	 * @throws ChannelClosedException if the read-end of the channel has been
	 *                                closed
	 */
	public static int readInt(ReadableByteChannel chnl) throws IOException, ChannelClosedException {
		return readData(chnl, Integer.BYTES).getInt();
	}

	/**
	 * Write an int on a channel
	 *
	 * @param n int to write
	 * @param chnl channel to write to
	 */
	public static void writeInt(int n, WritableByteChannel chnl) throws IOException {
		ByteBuffer buff = ByteBuffer.allocate(Integer.BYTES);
		buff.clear();
		buff.putInt(n);
		buff.flip();
		while (buff.remaining() > 0) {
			chnl.write(buff);
		}
	}


	/**
	 * Read a boolean from a channel.
	 *
	 * @param chnl channel to read from
	 * @return boolean read from the chnl
	 * @throws ChannelClosedException if the read-end of the channel has been
	 *                                closed
	 */
	public static boolean readBool(ReadableByteChannel chnl) throws IOException, ChannelClosedException {
		return readByte(chnl) != 0;
	}

	/**
	 * Write a boolean on a channel
	 *
	 * @param b boolean to write
	 * @param chnl channel to write to
	 */
	public static void writeBool(boolean b, WritableByteChannel chnl) throws IOException {
		writeByte((byte)(b ? 1 : 0), chnl);
	}


	/**
	 * Read a string from a chnl (provided that the format is the one specified
	 * in the report)
	 *
	 * @param chnl channel to read from
	 * @return string read
	 * @throws ChannelClosedException if the read-end of the channel has been
	 *                                closed
	 */
	public static String readString(ReadableByteChannel chnl) throws IOException, ChannelClosedException {
		ByteBuffer buff = ByteBuffer.allocate(20);
		buff.clear();
		// Read an int
		buff.limit(Integer.BYTES);
		while (buff.remaining() > 0) {
			if (chnl.read(buff) == -1) {
				throw new ChannelClosedException();
			}
		}
		buff.flip();
		int len = buff.getInt();
		byte[] str_bytes = new byte[len];
		int read;
		while (len > 0) {
			buff.clear();
			buff.limit(Math.min(len, buff.capacity()));
			read = chnl.read(buff);
			buff.flip();
			buff.get(str_bytes, str_bytes.length - len, read);
			len -= read;
		}
		return new String(str_bytes, encoding);
	}

	/**
	 * Write a string to a chnl in the format specified in the report
	 *
	 * @param str string to write
	 * @param chnl channel to write to
	 */
	public static void writeString(String str, WritableByteChannel chnl) throws IOException {
		ByteBuffer buff = ByteBuffer.allocate(Integer.BYTES + str.getBytes().length);
		buff.clear();
		buff.putInt(str.getBytes().length);
		buff.put(str.getBytes());
		buff.flip();
		while (buff.hasRemaining()) {
			chnl.write(buff);
		}
	}


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
