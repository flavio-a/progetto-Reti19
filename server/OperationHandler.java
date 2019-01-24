package server;

import java.util.*;
import java.util.concurrent.*;
import java.io.IOException;
import java.net.*;
import java.nio.*;
import java.nio.file.*;
import java.nio.channels.*;
import server.lib.*;

/**
 * Task of TURING server to handle a single operation on a connection.
 * <p>
 *
 */
public class OperationHandler implements Runnable {
	private final ConcurrentSocketChannel chnl;
	// private final SocketChannel chnl;
	private final DBInterface db_interface;
	private final BlockingQueue<ConcurrentSocketChannel> freesc;
	private final Map<ConcurrentSocketChannel, String> socket_to_user;
	private final Map<String, ConcurrentSocketChannel> user_to_socket;
	private final Selector selector;
	private final String usr;

	public OperationHandler(ConcurrentSocketChannel chnl_set, DBInterface db_interface_set, BlockingQueue<ConcurrentSocketChannel> freesc_set, Map<ConcurrentSocketChannel, String> socket_to_user_set, Map<String, ConcurrentSocketChannel> user_to_socket_set, Selector selector_set) {
		if (chnl_set == null) {
			throw new NullPointerException();
		}
		chnl = chnl_set;
		db_interface = db_interface_set;
		freesc = freesc_set;
		socket_to_user = socket_to_user_set;
		user_to_socket = user_to_socket_set;
		selector = selector_set;
		usr = socket_to_user.getOrDefault(chnl, null);
	}

	/**
	 * Utility function to handle logging. May become something finer than a
	 * println in the future.
	 *
	 * @param s the string to log
	 */
	private void log(String s) {
		System.out.println("User " + usr + " - " + s);
	}

	private void lockedWriteOp(OpKind op) throws IOException, ChannelClosedException {
		try {
			chnl.lock();
			IOUtils.writeOpKind(op, chnl);
		}
		finally {
			chnl.unlock();
		}
	}

	// ============================= OP HANDLERS =============================
	// Set of utility functions just to split handleOperation
	private void handleCreate() throws IOException, ChannelClosedException {
		String docname = IOUtils.readString(chnl);
		int nsec = IOUtils.readInt(chnl);
		if (db_interface.createDocument(usr, docname, nsec)) {
			log("Created document " + usr + "/" + docname);
			lockedWriteOp(OpKind.RESP_OK);
		}
		else {
			log("Creation of document " + usr + "/" + docname + " failed: already exists");
			lockedWriteOp(OpKind.ERR_DOCUMENT_EXISTS);
		}
	}

	private void handleEdit() throws IOException, ChannelClosedException {
		String fulldocname = IOUtils.readString(chnl);
		int nsec = IOUtils.readInt(chnl);
		try {
			Section sec = new Section(fulldocname, nsec);
			Path section_path = db_interface.editSection(usr, sec);
			try {
				chnl.lock();
				IOUtils.writeOpKind(OpKind.RESP_OK, chnl);
				IOUtils.fileToChannel(section_path, chnl);
			}
			finally {
				chnl.unlock();
			}
			log("Started edit of section " + sec.getDebugRepr() + " succesfull");
		}
		catch (IllegalArgumentException e) {
			log("Wrongly formatted full document name " + fulldocname);
			lockedWriteOp(OpKind.ERR_NO_DOCUMENT);
		}
		catch (NoSuchDocumentException e) {
			log("Edit of " + fulldocname + " failed: no such document");
			lockedWriteOp(OpKind.ERR_NO_DOCUMENT);
		}
		catch (NoPermissionException e) {
			log("Edit of " + fulldocname + " failed: no permissions");
			lockedWriteOp(OpKind.ERR_PERMISSION);
		}
		catch (NoSuchSectionException e) {
			log("Edit of section " + Integer.toString(nsec) + " failed: no such section");
			lockedWriteOp(OpKind.ERR_NO_SECTION);
		}
		catch (SectionBusyException e) {
			log("Edit of section " + Integer.toString(nsec) + " failed: section busy");
			lockedWriteOp(OpKind.ERR_SECTION_BUSY);
		}
		catch (UserBusyException e) {
			log("Edit failed: user busy");
			lockedWriteOp(OpKind.ERR_USER_BUSY);
		}
	}

	private void handleEndEdit() throws IOException, ChannelClosedException {
		Section sec = db_interface.userIsModifying(usr);
		if (sec == null) {
			log("End of edit failed: user isn't editing anything");
			lockedWriteOp(OpKind.ERR_USER_FREE);
		}
		else {
			log("End of edit succesfull");
			try {
				chnl.lock();
				IOUtils.writeOpKind(OpKind.RESP_OK, chnl);
				db_interface.finishEditSection(usr, sec, chnl);
			}
			finally {
				chnl.unlock();
			}
			log("File received succesfully");
		}
	}

	private void handleShowSec() throws IOException, ChannelClosedException {
		String fulldocname = IOUtils.readString(chnl);
		int nsec = IOUtils.readInt(chnl);
		try {
			Section sec = new Section(fulldocname, nsec);
			if (!db_interface.documentExist(sec.getDocumentPath())) {
				log("Showing of " + sec.getFullDocumentName() + " failed: doesn't exists");
				lockedWriteOp(OpKind.ERR_NO_DOCUMENT);
			}
			else if (!db_interface.sectionExist(sec)) {
				log("Showing of " + sec.getDebugRepr() + " failed: doesn't exists");
				lockedWriteOp(OpKind.ERR_NO_SECTION);
			}
			else {
				log("Showing " + sec.getDebugRepr() + " succesful");
				try {
					chnl.lock();
					IOUtils.writeOpKind(OpKind.RESP_OK, chnl);
					IOUtils.writeBool(db_interface.isBeingModified(sec), chnl);
					IOUtils.fileToChannel(db_interface.getAbsolutePath(sec.getFullPath()), chnl);
				}
				finally {
					chnl.unlock();
				}
			}
		}
		catch (IllegalArgumentException e) {
			log("Wrongly formatted full document name " + fulldocname);
			IOUtils.writeOpKind(OpKind.ERR_NO_DOCUMENT, chnl);
		}
	}

	private void handleShowDoc() throws IOException, ChannelClosedException {
		String fulldocname = IOUtils.readString(chnl);
		Path docpath = Paths.get(fulldocname);
		if (!db_interface.documentExist(docpath)) {
			log("Show of document " + fulldocname + " failed: doesn't exists");
			lockedWriteOp(OpKind.ERR_NO_DOCUMENT);
		}
		else {
			log("Show of document " + fulldocname + " succesful");
			int numsec = db_interface.sectionNumber(docpath); // TODO fix it
			log("Numsec = " + Integer.toString(numsec));
			try {
				chnl.lock();
				IOUtils.writeOpKind(OpKind.RESP_OK, chnl);
				IOUtils.writeInt(numsec, chnl);
				for (int i = 0; i < numsec; ++i) {
					log("Sending section " + Integer.toString(i));
					Section sec = new Section(fulldocname, i);
					IOUtils.writeBool(db_interface.isBeingModified(sec), chnl);
					IOUtils.fileToChannel(db_interface.getAbsolutePath(sec.getFullPath()), chnl);
				}
			}
			finally {
				chnl.unlock();
			}
		}
	}

	private void handleInvite() throws IOException, ChannelClosedException {
		String invited_usr = IOUtils.readString(chnl);
		String docname = IOUtils.readString(chnl);
		if (!db_interface.documentExist(Paths.get(usr).resolve(docname))) {
			lockedWriteOp(OpKind.ERR_NO_DOCUMENT);
		}
		else {
			ConcurrentSocketChannel notifyChnl = user_to_socket.getOrDefault(invited_usr, null);
			boolean invitedOnline = notifyChnl != null;
			if (db_interface.invite(usr, docname, invited_usr, !invitedOnline) && invitedOnline) {
				// notify invitation on notifyChnl
				try {
					// No risk of deadlock because I'm not holding any other lock
					notifyChnl.lock();
					IOUtils.writeOpKind(OpKind.OP_INVITE, notifyChnl);
					IOUtils.writeString(usr + "/" + docname, notifyChnl);
				}
				finally {
					notifyChnl.unlock();
				}
			}
			lockedWriteOp(OpKind.RESP_OK);
		}
	}

	private void handleListDocs() throws IOException, ChannelClosedException {
		try {
			chnl.lock();
			IOUtils.writeOpKind(OpKind.RESP_OK, chnl);
			Collection<String> documents = db_interface.userModificableDocuments(usr);
			IOUtils.writeInt(documents.size(), chnl);
			for(String doc : documents) {
				IOUtils.writeString(doc, chnl);
			}
		}
		finally {
			chnl.unlock();
		}
	}


	// ========================================================================

	/**
	 * Main logic of the class. Handle this instance's SocketChannel.
	 *<p>
	 * This function is intended to be the {@link run} body, moved in another
	 * function in order to be wrapped and ensure the correct end of this
	 * object lifespan: either return the SocketChannel to the listener or
	 * close it.
	 *
	 * @return whether this instance's SocketChannel should be returned to the
	 *         listener (true) or not (false).
	 * @throws ChannelClosedException if the channel is closed by the other end
	 *                                during the communication
	 */
	private boolean handleOperation() throws IOException, ChannelClosedException {
		chnl.configureBlocking(true);
		OpKind op = IOUtils.readOpKind(chnl);
		if (usr == null) {
			// No need to synchronize those writes because if the user isn't
			// connected noone can send invitations to him
			if (op == OpKind.OP_LOGIN) {
				log("Requested login");
				String login_usr, login_pwd;
				login_usr = IOUtils.readString(chnl);
				login_pwd = IOUtils.readString(chnl);
				if (!db_interface.checkUser(login_usr, login_pwd)) {
					log("User/pwd mismatch");
					IOUtils.writeOpKind(OpKind.ERR_INVALID_LOGIN, chnl);
					return true;
				}
				ConcurrentSocketChannel other_chnl = user_to_socket.get(login_usr);
				if (other_chnl != null && other_chnl != chnl) {
					log("User already in use");
					IOUtils.writeOpKind(OpKind.ERR_USERNAME_BUSY, chnl);
					return true;
				}
				log("Login sucessfull with username \"" + login_usr + "\"");
				socket_to_user.put(chnl, login_usr);
				user_to_socket.put(login_usr, chnl);
				IOUtils.writeOpKind(OpKind.RESP_OK, chnl);
				return true;
			}
			else {
				log("Requested non login on unlogged socket");
				IOUtils.writeOpKind(OpKind.ERR_UNLOGGED, chnl);
				return false;
			}
		}
		// usr != null
		switch (op) {
			case OP_LOGIN:
				log("Requested login on logged socket");
				lockedWriteOp(OpKind.ERR_ALREADY_LOGGED);
			break;
			case OP_CREATE:
				handleCreate();
				break;
			case OP_EDIT:
				handleEdit();
				break;
			case OP_ENDEDIT:
				handleEndEdit();
				break;
			case OP_SHOWSEC:
				handleShowSec();
				break;
			case OP_SHOWDOC:
				handleShowDoc();
				break;
			case OP_INVITE:
				handleInvite();
				break;
			case OP_LISTDOCS:
				handleListDocs();
				break;
			default:
				log("Requested unknown operation: " + op.toString());
				lockedWriteOp(OpKind.ERR_UNKNOWN_OP);
				break;
		}
		return true;
	}

	@Override
	public void run() {
		boolean shouldReturn;
		try {
			shouldReturn = this.handleOperation();
		}
		catch (IOException e) {
			log("I/O exception while handling operation: " + e.getMessage());
			shouldReturn = false;
		}
		catch (ChannelClosedException e) {
			log("Channel closed by the other end");
			shouldReturn = false;
		}
		if (shouldReturn) {
			freesc.add(chnl);
			selector.wakeup();
		}
		else {
			// Disconnect the channel and frees the user again
			socket_to_user.remove(chnl);
			if (usr != null) {
				log("Disconnecting " + usr + " and freeing their edit");
				user_to_socket.remove(usr);
				db_interface.cleanUserEdit(usr);
			}
			try {
				chnl.close();
			}
			catch (IOException e) {
				log("Error closing a connection: PANIC");
				e.printStackTrace();
			}
		}
	}
}
