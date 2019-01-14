package server;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import java.util.stream.*;
import java.nio.*;
import java.nio.file.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.io.*;
import server.*;
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
 *
 * TODO: change readers with nio.
 */
public class DBInterface {
	public static final String linesep = System.getProperty("line.separator");
	public static final Charset utf8 = StandardCharsets.UTF_8;

	public static final String pwd_file = "pwd";
	public static final String invitations_file = "pending_invitations";
	public static final String permissions_file = "editable_docs";
	public static final String editors_file = "editors";
	public static final String section_file_prefix = "section";

	private final Path root;
	private final ReadWriteLock fs_rwlock;
	private final ReadWriteLock edit_rwlock;
	// Given a username, returns if they're editing
	private final Map<String, Section> isEditing;
	// Given a section, returns if it is being edited
	private final Map<Section, Boolean> beingEdited;

	/**
	 * Creates a new instance of DBInterface. If the passed root directory
	 * doesn't exists, creates it.
	 *
	 * @param dir path to the root directory
	 * @throws IOException if happens an error during the creation of root dir
	 */
	public DBInterface(String dir) throws IOException {
		root = Paths.get(dir);
		try {
			Files.createDirectory(root);
		}
		catch (FileAlreadyExistsException e) {
			// Directory already exists, nothing to do
		}
		fs_rwlock = new ReentrantReadWriteLock();
		edit_rwlock = new ReentrantReadWriteLock();
		// Both are lazily filled, and begin empty (no one's editing anything)
		isEditing = new HashMap<String, Section>();
		beingEdited = new HashMap<Section, Boolean>();

	}

	// =========================== FILE OPERATIONS ===========================
	/**
	 * Add a row to a file.
	 *
	 * @param f path to the file relative to root
	 * @param text the line of text to add. There's no check against newlines
	 *             in this string
	 */
	private void addRow(Path f, String text) throws IOException {
		byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
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
	private boolean searchRow(Path f, String text, int skip) throws IOException {
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
	 * Remove all row equals to a given text from a file.
	 *
	 * @param f path to the file relative to root
	 * @param text the line to remove. Comparisions are as in searchRow
	 * @param skip number of lines at the begining of the file to skip
	 *
	 * TODO: replace writer with nio
	 */
	public void deleteRows(Path f, String text, int skip) throws IOException {
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

	// ============================== USERS ==================================
	/**
	 * Creates a user if it doesn't exists. Synchronized.
	 *
	 * @param usr the username to create
	 * @return true iff the user was created (ie: didn't exist)
	 */
	public boolean createUser(String usr, String pwd) throws IOException {
		try {
			fs_rwlock.writeLock().lock();
			Path usr_path = root.resolve(usr);
			Files.createDirectory(usr_path);
			// This also creates the file
			byte[] bytes = pwd.trim().getBytes(StandardCharsets.UTF_8);
			Files.write(usr_path.resolve(pwd_file), bytes);
			Files.createFile(usr_path.resolve(invitations_file));
			Files.createFile(usr_path.resolve(permissions_file));
			return true;
		}
		catch (FileAlreadyExistsException e) {
			return false;
		}
		finally {
			fs_rwlock.writeLock().unlock();
		}
	}

	/**
	 * Checks login informations (username and password). Synchronized.
	 *
	 * @param usr username to check
	 * @param pwd password to checl
	 * @return true iff usr exists and pwd matches
	 */
	public boolean checkUser(String usr, String pwd) throws IOException {
		fs_rwlock.readLock().lock();
		Path usr_info = root.resolve(usr).resolve(pwd_file);
		if (!usr_info.toFile().exists()) {
			return false;
		}
		try (
			FileLineReader reader = new FileLineReader(usr_info);
		) {
			return pwd.trim().equals(reader.readLine());
		}
		catch (FileIteratorException e) {
			throw e.getCause();
		}
		finally {
			fs_rwlock.readLock().unlock();
		}
	}

	// ============================ DOCUMENTS ================================
	/**
	 * Creates a document if it doesn't exists. Synchronized. Assumes that the
	 * given username exists (if not, IOException)
	 *
	 * @param usr the username of the owner of the document
	 * @param name the name of the document
	 * @param n number of sections
	 * @return true iff the document was created (ie: didn't exist)
	 * @throws IOException if usr doesn't exist of if an IO error occurs
	 */
	public boolean createDocument(String usr, String name, int n) throws IOException {
		try {
			fs_rwlock.writeLock().lock();
			Path usr_path = root.resolve(usr);
			Path doc_path = usr_path.resolve(name);
			Files.createDirectory(doc_path);

			// A user has permission over its own documents
			byte[] bytes = usr.getBytes(utf8);
			Files.write(doc_path.resolve(editors_file), bytes);
			addRow(usr_path.resolve(permissions_file), name);
			for (Integer i = 0; i < n; ++i) {
				Files.createFile(doc_path.resolve(section_file_prefix + i.toString()));
			}
			return true;
		}
		catch (FileAlreadyExistsException e) {
			return false;
		}
		finally {
			fs_rwlock.writeLock().unlock();
		}
	}


	// ============================= SECTIONS ================================
	/**
	 * Get the number of sections of the passed document.
	 *
	 * TODO: synchronization? It may happen that this function is called during
	 * the creation of the document. In this case there may be problems? Not
	 * fatal problems, the worst is an internal server error, try again in a
	 * few moment for the user.
	 * Instead acquiring the fs_rwlock.readLock() may cause deadlock because
	 * this function is called within an edit_rwlock-locked section.
	 *
	 * @param name the path to the doc
	 * @return the number of sections of that document
	 */
	public int sectionNumber(Path doc) throws IOException {
		try (
			DirectoryStream<Path> doc_files = Files.newDirectoryStream(
							doc, section_file_prefix + "*");
		) {
			int preflen = section_file_prefix.length();
			return StreamSupport.stream(doc_files.spliterator(), false)
					.mapToInt(p -> Integer.parseInt(
							p.getFileName().toString().substring(preflen)
						))
					.max().getAsInt();
		}
	}

	/**
	 * Get the size of the section file being modified by this user.
	 *
	 * @param usr the user
	 * @return the size of the section this user is modifying, or -1 if they
	 *         aren't modifying anything
	 */
	public long modifiedSectionSize(String usr) throws IOException {
		Section sec = isEditing.getOrDefault(usr, null);
		if (sec == null) {
			return -1;
		}
		else {
			return Files.size(root.resolve(sec.getFullPath()));
		}
	}

	/**
	 * Start the edit of a section. Assumes the document exists (otherwise
	 * IOException), but check that usr has permission on the document, that no
	 * one else is editing the section and that the user isn't editing
	 * something else at the same time.
	 *
	 * @param usr the username of the user requesting the edit
	 * @param sec section to edit
	 * @return a FileChannel to the required section
	 * @throws NoPermissionException if the user doesn't have permission
	 * @throws NoSuchSectionException if the given section doesn't exists
	 * @throws SectionBusyException if the section is being edited
	 * @throws UserBusyException if the user is editing something else
	 * @throws IOException if document doesn't exist or if an IO error occurs
	 */
	public FileChannel editSection(String usr, Section sec) throws IOException, NoPermissionException, NoSuchSectionException, SectionBusyException, UserBusyException {
		// Those are here because sec_path is needed after
		Path doc_path = root.resolve(sec.getDocumentPath());
		Path sec_path = doc_path.resolve(sec.getSectionPath());
		try {
			fs_rwlock.readLock().lock();
			// Check permissions
			if (!searchRow(doc_path.resolve(editors_file), usr, 0)) {
				throw new NoPermissionException(usr, sec.getQualifiedDocumentName());
			}
			// Check existence
			if (!sec_path.toFile().exists()) {
				throw new NoSuchSectionException(sec.getN());
			}
		}
		finally {
			fs_rwlock.readLock().unlock();
		}
		try {
			edit_rwlock.writeLock().lock();
			// Check section free
			if (beingEdited.getOrDefault(sec, false)) {
				throw new SectionBusyException(sec.getN());
			}
			// Check user free
			if (isEditing.getOrDefault(usr, null) != null) {
				throw new UserBusyException(usr);
			}
			// Give edit to the user
			isEditing.put(usr, sec);
			beingEdited.put(sec, true);
			return FileChannel.open(sec_path, StandardOpenOption.READ);
		}
		finally {
			edit_rwlock.writeLock().unlock();
		}
	}

	/**
	 * End the edit of a section. If the user isn't editing anything, throws
	 * NoSuchSectionException.
	 *
	 * @param usr the username of the user ending the edit
	 * @param newContent a Channel to the new contet of the
	 * @throws UserFreeException if the user isn't editing anything
	 * @throws IOException if document doesn't exist or if an IO error occurs
	 */
	public void finishEditSection(String usr, ReadableByteChannel newContent) throws IOException, UserFreeException {
		Section sec;
		try {
			edit_rwlock.readLock().lock();
			sec = isEditing.getOrDefault(usr, null);
			if (sec == null) {
				throw new UserFreeException(usr);
			}
		}
		finally {
			edit_rwlock.readLock().unlock();
		}
		// Write on the file the whole Channel. No need to synchronize
		// because noone else can modify this section at this time.
		ByteBuffer sizebuff = ByteBuffer.allocate(Long.SIZE / Byte.SIZE);
		sizebuff.clear();
		while (sizebuff.remaining() > 0) {
			newContent.read(sizebuff);
		}
		sizebuff.flip();
		long filesize = sizebuff.getLong();
		try (
			FileChannel outFile = FileChannel.open(root.resolve(sec.getFullPath()), StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
		) {
			long pos = 0;
			while (filesize > 0) {
				long count = outFile.transferFrom(newContent, pos, filesize);
				pos += count;
				filesize -= count;
			}
		}
		// Release edit lock on the section
		try {
			edit_rwlock.writeLock().lock();
			isEditing.put(usr, null);
			beingEdited.put(sec, false);
		}
		finally {
			edit_rwlock.writeLock().unlock();
		}
	}
}
