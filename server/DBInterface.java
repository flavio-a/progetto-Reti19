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
 * Of course this may be enhanced.
 */
public class DBInterface {
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
	// Chat infos
	private byte new_chat_add = 0;
	private final Map<String, ChatInfo> doc_to_chat;

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

		doc_to_chat = new HashMap<String, ChatInfo>();
	}


	/**
	 * Get the absolute path of a db element.
	 *
	 * @param path path of the element relative to the db root directory
	 * @return the absolute path of the given element
	 */
	public Path getAbsolutePath(Path path) {
		return root.resolve(path);
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
			IOUtils.createFileContent(usr_path.resolve(pwd_file), pwd.trim());
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
	 * Check if a user exist. Not synchronized.
	 *
	 * @param usr username to check
	 * @return true iff usr exists
	 */
	public boolean userExists(String usr) throws IOException {
		return root.resolve(usr).toFile().exists();
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
			fs_rwlock.readLock().unlock();
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

	/**
	 * Get the documents that a certain user can modify.
	 *
	 * @param usr the username
	 * @return a collection of full document names
	 */
	public Collection<String> userModificableDocuments(String usr) throws IOException {
		Path docslist = root.resolve(usr).resolve(permissions_file);
		FileLineReader reader = new FileLineReader(docslist);
		Collection<String> lines = new ArrayList<>();
		reader.iterator().forEachRemaining(lines::add);
		return lines;
	}

	/**
	 * Check if a user is modifying something now. Synchronized.
	 *
	 * @param usr user to check
	 * @return the section being modified by usr or null if they aren't
	 *         modifying anything
	 */
	public Section userIsModifying(String usr) {
 		try {
 			edit_rwlock.readLock().lock();
 			return isEditing.getOrDefault(usr, null);
 		}
 		finally {
 			edit_rwlock.readLock().unlock();
 		}
	 }

	// ============================ DOCUMENTS ================================
	/**
	 * Check if a document exist. Not synchronized.
	 *
	 * @param doc_path path of the document relative to the db root directory
	 * @return true iff the document exist
	 */
	public boolean documentExist(Path doc_path) {
		return root.resolve(doc_path).toFile().exists();
	}

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
			IOUtils.createFileContent(doc_path.resolve(editors_file), usr);
			IOUtils.addRow(usr_path.resolve(permissions_file), name);
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

	/**
	 * Invites a user to collaborate on a document. If the user has already
	 * permission on the document, this function does nothing (is idempotent).
	 * Synchronized.
	 *
	 * @param owner owner of the document
	 * @param doc name of the document
	 * @param usr_invited user invited to collaborate on doc
	 * @param pending specifies whether this invitation is pending or not
	 * @return true iff the user was actually invited (ie: didn't have
	 *         permission on the file before this operation)
	 */
	public boolean invite(String owner, String doc, String usr_invited, boolean pending) throws IOException {
		Path doc_editors = root.resolve(owner).resolve(doc).resolve(editors_file);
		Path usr_folder = root.resolve(usr_invited);
		try {
			fs_rwlock.writeLock().lock();
			if (!IOUtils.searchRow(doc_editors, usr_invited, 0)) {
				IOUtils.addRow(doc_editors, usr_invited);
				IOUtils.addRow(usr_folder.resolve(permissions_file), owner + "/" + doc);
				if (pending) {
					IOUtils.addRow(usr_folder.resolve(invitations_file), owner + "/" + doc);
				}
				return true;
			}
			else {
				return false;
			}
		}
		finally {
			fs_rwlock.writeLock().unlock();
		}
	}

	/**
	 * Get the last byte of the IP addr of a document's chat. Not synchronized,
	 * should only be used by a thread that is sure the ChatInfo object is in
	 * the hashtable (for instance, by a thread handling a user that is
	 * modifying this document). Assumes the document is associated with a
	 * ChatInfo.
	 *
	 * @param fulldocname full name of the document
	 * @return the last byte of the IP addr
	 */
	public byte getLastChatByte(String fulldocname) {
		System.out.println(fulldocname);
		return doc_to_chat.get(fulldocname).getAddr();
	}

	// ============================= SECTIONS ================================
	/**
	 * Check if a section exist. If the document itself doesn't exits it returns
	 * false. Not synchronized.
	 *
	 * @param sec section to check
	 * @return true if the document and the section exists, false otherwise
	 */
	public boolean sectionExist(Section sec) {
		return this.documentExist(sec.getDocumentPath())
				&& root.resolve(sec.getFullPath()).toFile().exists();
	}

	/**
	 * Get the number of sections of the passed document. Not synchronized.
	 * <p>
	 * This function is not synchronized for two reasons: first and most
	 * important is never used in a context in which the number of section may
	 * change. Second even if it would do the worst thing that may happen is an
	 * IOException, that the server will report as "ERR_RETRY".
	 *
	 * @param doc the path to the doc
	 * @return the number of sections of that document
	 */
	public int sectionNumber(Path doc) throws IOException {
		try (
			DirectoryStream<Path> doc_files = Files.newDirectoryStream(
							root.resolve(doc), section_file_prefix + "*");
		) {
			int preflen = section_file_prefix.length();
			return StreamSupport.stream(doc_files.spliterator(), false)
					.mapToInt(p -> Integer.parseInt(
							p.getFileName().toString().substring(preflen)
						))
					.max().getAsInt() + 1;
		}
	}

	/**
	 * Get if a section is being modified.
	 * <p>
	 * There's no check on the existance of the section. If it doesn't, returns
	 * false.
	 *
	 * @param sec the section to check
	 * @return true iff the section exists and is currently being modified
	 */
	public boolean isBeingModified(Section sec) {
		try {
			edit_rwlock.readLock().lock();
			return beingEdited.getOrDefault(sec, false);
		}
		finally {
			edit_rwlock.readLock().unlock();
		}
	}

	/**
	 * Start the edit of a section. Check that the document exists, usr has
	 * permission on the document, that noone else is editing the section and
	 * that the user isn't editing something else at the same time.
	 *
	 * @param usr the username of the user requesting the edit
	 * @param sec section to edit
	 * @return Path of the required section
	 * @throws NoSuchDocumentException if the given document doesn't exists
	 * @throws NoPermissionException if the user doesn't have permission
	 * @throws NoSuchSectionException if the given section doesn't exists
	 * @throws SectionBusyException if the section is being edited
	 * @throws UserBusyException if the user is editing something else
	 * @throws IOException if document doesn't exist or if an IO error occurs
	 */
	public Path editSection(String usr, Section sec) throws IOException, NoSuchDocumentException, NoPermissionException, NoSuchSectionException, SectionBusyException, UserBusyException {
		// Those are here because sec_path is needed after
		Path doc_path = root.resolve(sec.getDocumentPath());
		Path sec_path = doc_path.resolve(sec.getSectionPath());
		try {
			fs_rwlock.readLock().lock();
			// Check document existance
			if (!this.documentExist(sec.getDocumentPath())) {
				throw new NoSuchDocumentException(sec.getFullDocumentName());
			}
			// Check permissions
			if (!IOUtils.searchRow(doc_path.resolve(editors_file), usr, 0)) {
				throw new NoPermissionException(usr, sec.getFullDocumentName());
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
			System.out.println(sec.getFullDocumentName());
			ChatInfo chat = doc_to_chat.get(sec.getFullDocumentName());
			if (chat == null) {
				chat = new ChatInfo(new_chat_add);
				doc_to_chat.put(sec.getFullDocumentName(), chat);
				++new_chat_add;
			}
			chat.connect();
			return sec_path;
			// Now the writeLock can be released because only the user can
			// modify the section file, then no need for synchronization
		}
		finally {
			edit_rwlock.writeLock().unlock();
		}
	}

	/**
	 * Does the cleanup after the end of a section edit.
	 * It's just to factorize code commono to both finishEditSection and
	 * cleanUserEdit. Not synchronized, expect the caller to do it.
	 */
	private void endcleanSectionEdit(String usr, Section sec) {
		// Edit itself
		isEditing.remove(usr);
		beingEdited.remove(sec);
		// Chat
		ChatInfo chat = doc_to_chat.get(sec.getFullDocumentName());
		if (chat.disconnect()) {
			doc_to_chat.remove(sec.getFullDocumentName());
		}
	}

	/**
	 * End the edit of a section, getting the new content from a channel.
	 * <p>
	 * Doesn't check that the given user is really modifying the given section.
	 * In this case there may be unexpected behaviors and concurrency problems.
	 *
	 * @param usr the username of the user ending the edit
	 * @param sec the section that was being edited by usr
	 * @param newContent a Channel to the new content of the section
	 * @throws IOException if document doesn't exist or if an IO error occurs
	 */
	public void finishEditSection(String usr, Section sec, ReadableByteChannel newContent) throws IOException {
		// Write on the file the whole Channel. No need to synchronize
		// because noone else can modify this section at this time.
		IOUtils.channelToFile(newContent, root.resolve(sec.getFullPath()));
		// Release edit lock on the section
		try {
			edit_rwlock.writeLock().lock();
			endcleanSectionEdit(usr, sec);
		}
		finally {
			edit_rwlock.writeLock().unlock();
		}
	}

	/**
	 * Terminate (without saving) any edit a user is doing.
	 * <p>
	 * This function is primarly used in case of abrupt disconnection of a
	 * client to free the section it (possibly) was editing.
	 * <p>
	 * If the user wasn't editing anything, this function doesn't do anything.
	 *
	 * @param usr the username of the user ending the edit
	 */
	public void cleanUserEdit(String usr) {
		try {
			edit_rwlock.writeLock().lock();
			Section sec = isEditing.get(usr);
			if (sec != null) {
				endcleanSectionEdit(usr, sec);
			}
		}
		finally {
			edit_rwlock.writeLock().unlock();
		}
	}
}
