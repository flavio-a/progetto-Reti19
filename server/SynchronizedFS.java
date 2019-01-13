package server;

// import java.util.*;
import java.util.stream.*;
import java.nio.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.io.IOException;

/**
 * This class handles synchronized interactions with filesystem.
 *
 * synchronization matters because fs structure itself is used to store
 * informations that may be accessed concurrently. This class operates only on
 * the subtree rooter at root, and consider it as the base of the storage
 * system of the server
 */
public class SynchronizedFS {
	public static final String user_info_file = "user_info";
	public static final String docs_info_file = "docs_info";
	public static final String section_file_prefix = "section";

	private final Path root;

	/**
	 * Creates a new instance of SynchronizedFS. If the passed root directory
	 * doesn't exists, creates it.
	 *
	 * @param dir path to the root directory
	 */
	public SynchronizedFS(String dir) throws IOException {
		root = Paths.get(dir);
		try {
			Files.createDirectory(root);
		}
		catch (FileAlreadyExistsException e) {
			// Directory already exists, nothing to do
		}
	}

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
	 * Creates a user if it doesn't exists.
	 *
	 * @param usr the username to create
	 * @return true iff the user was created (ie: didn't exist)
	 */
	public synchronized boolean createUser(String usr, String pwd) throws IOException {
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
	}
}
