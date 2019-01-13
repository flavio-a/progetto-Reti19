package server;

import java.util.concurrent.locks.*;
import java.util.stream.*;
import java.nio.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.io.*;
import server.lib.*;

/**
 * This class handles synchronized interactions with filesystem.
 *
 * synchronization matters because fs structure itself is used to store
 * informations that may be accessed concurrently. This class operates only on
 * the subtree rooter at root, and consider it as the base of the storage
 * system of the server.
 *
 * Actual implementation uses a single read-write lock for all files in the db.
 * Of course this may be enhanced (not so much because the server multiplexes
 * inputs so there shouldn't be too many concurrency).
 */
public class SynchronizedFS {
	public static final String user_info_file = "user_info";
	public static final String docs_info_file = "docs_info";
	public static final String section_file_prefix = "section";

	private final Path root;
	private final ReadWriteLock rwlock;

	/**
	 * Creates a new instance of SynchronizedFS. If the passed root directory
	 * doesn't exists, creates it.
	 *
	 * @param dir path to the root directory
	 * @throws IOException if happens an error during the creation of root dir
	 */
	public SynchronizedFS(String dir) throws IOException {
		root = Paths.get(dir);
		try {
			Files.createDirectory(root);
		}
		catch (FileAlreadyExistsException e) {
			// Directory already exists, nothing to do
		}
		rwlock = new ReentrantReadWriteLock();
	}

	// ========================== ROW OPERATIONS ==============================

	/**
	 * Add a row to a file.
	 *
	 * @param f path to the file relative to root
	 * @param text the line of text to add. There's no check against newlines
	 *             in this string
	 */
	private void addRow(Path f, String text) throws IOException {
		byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
		Files.write(root.resolve(f), bytes, StandardOpenOption.APPEND);
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
	private boolean searchRow(Path f, String text, int skip) throws IOException {
		final String trim_text = text.trim();
		try (
			BufferedReader reader = Files.newBufferedReader(f);
		) {
			return reader.lines()
						.skip(skip)
						.anyMatch(line -> trim_text.equals(line.trim()));
		}
	}

	/**
	 * Remove all row equals to a given text from a file.
	 *
	 * @param f path to the file relative to root
	 * @param text the line to remove. Comparisions are as in searchRow
	 * @param skip number of lines at the begining of the file to skip
	 * @param limit maximum number of lines on which operates. If both skip and
	 *              limit are given, operates on lines [skip, skip + limit].
	 *              Negative values means no limit.
	 */
	@SuppressWarnings("unchecked")
	public void deleteRows(Path f, String text, int skip, int limit) throws IOException {
		final String trim_text = text.trim();
		// Implementation: the file is read line by line and lines not matching
		// are written to a temporary file. At the end, the temporary file is
		// moved to replace the original one
		Path tmp_file = f.getParent().resolve(f.getFileName().toString() + "_tmp");
		try (
			BufferedWriter writer = Files.newBufferedWriter(tmp_file);
			BufferedReader reader = Files.newBufferedReader(f);
		) {
			int i = 0;
			for (String line : (Iterable<String>)reader.lines()) {
				if (i >= skip
					&& (limit < 0 || i < skip + limit)
					&& trim_text.equals(line.trim())) {
					// Nothing
				}
				else {
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

	// ==================== SYNCHRONIZED PUBLIC OPERATIONS ====================

	// /**
	//  * Check if a user exists.
	//  *
	//  * @param usr the username to check
	//  * @return true iff the user exists
	//  */
	// public bool existsUser(String usr) {
	// 	try (DirectoryStream<Path> dir_stream = Files.newDirectoryStream(
	// 				Paths.get(dir), Files::isDirectory);
	// 	) {
	// 		return StreamSupport.stream(dir_stream.spliterator(), false)
	// 					.filter(Path::endsWith(usr))
	// 					.count() > 0;
	// 	}
	// }

	/**
	 * Creates a user if it doesn't exists. Synchronized.
	 *
	 * @param usr the username to create
	 * @return true iff the user was created (ie: didn't exist)
	 */
	public boolean createUser(String usr, String pwd) throws IOException {
		rwlock.writeLock().lock();
		try {
			Path usr_path = root.resolve(usr);
			Files.createDirectory(usr_path);
			// This also creates the file
			byte[] bytes = pwd.getBytes(StandardCharsets.UTF_8);
			Files.write(usr_path.resolve(user_info_file), bytes);
			return true;
		}
		catch (FileAlreadyExistsException e) {
			return false;
		}
		finally {
			rwlock.writeLock().unlock();
		}
	}

	public boolean checkUser(String usr, String pwd) throws IOException {
		rwlock.readLock().lock();
		try (
			FileLineReader reader = new FileLineReader(root.resolve(usr));
		) {
			return pwd.trim().equals(reader.readLine().trim());
		}
		catch (FileIteratorException e) {
			throw e.getCause();
		}
		finally {
			rwlock.readLock().unlock();
		}
	}
}
