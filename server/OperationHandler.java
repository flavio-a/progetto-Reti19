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
	private final SocketChannel chnl;
	private final DBInterface db_interface;
	private final BlockingQueue<SocketChannel> freesc;
	private final Map<SocketChannel, String> socket_to_user;
	private final Map<String, SocketChannel> user_to_socket;
	private final Selector selector;
	private final String usr;

	public OperationHandler(SocketChannel chnl_set, DBInterface db_interface_set, BlockingQueue<SocketChannel> freesc_set, Map<SocketChannel, String> socket_to_user_set, Map<String, SocketChannel> user_to_socket_set, Selector selector_set) {
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
			if (op == OpKind.OP_LOGIN) {
				log("Requested login");
				// Login
				String login_usr, login_pwd;
				login_usr = IOUtils.readString(chnl);
				login_pwd = IOUtils.readString(chnl);
				if (!db_interface.checkUser(login_usr, login_pwd)) {
					log("User/pwd mismatch");
					// Wrong usr/pwd combination
					IOUtils.writeOpKind(OpKind.ERR_INVALID_LOGIN, chnl);
					return false;
				}
				SocketChannel other_chnl = user_to_socket.get(login_usr);
				if (other_chnl != null && other_chnl != chnl) {
					log("User already in use");
					// Username already in use on another connection
					IOUtils.writeOpKind(OpKind.ERR_USERNAME_BUSY, chnl);
					return false;
				}
				log("Login sucessfull with username \"" + login_usr + "\"");
				socket_to_user.put(chnl, login_usr);
				user_to_socket.put(login_usr, chnl);
				IOUtils.writeOpKind(OpKind.RESP_OK, chnl);
				return true;
			}
			else {
				// Not logged connection with a non LOGIN operation
				log("Requested non login on unlogged socket");
				IOUtils.writeOpKind(OpKind.ERR_UNLOGGED, chnl);
				return false;
			}
		}
		// usr != null
		switch (op) {
			case OP_LOGIN: {
				log("Requested login on logged socket");
				IOUtils.writeOpKind(OpKind.ERR_ALREADY_LOGGED, chnl);
			}
			break;
			case OP_CREATE: {
				String docname = IOUtils.readString(chnl);
				int nsec = IOUtils.readInt(chnl);
				if (db_interface.createDocument(usr, docname, nsec)) {
					log("Created document " + usr + "/" + docname);
					IOUtils.writeOpKind(OpKind.RESP_OK, chnl);
				}
				else {
					log("Creation of document " + usr + "/" + docname + " failed: already exists");
					IOUtils.writeOpKind(OpKind.ERR_DOCUMENT_EXISTS, chnl);
				}
			}
			break;
			case OP_EDIT: {
				String fulldocname = IOUtils.readString(chnl);
				int nsec = IOUtils.readInt(chnl);
				try {
					Section sec = new Section(fulldocname, nsec);
					Path section_path = db_interface.editSection(usr, sec);
					IOUtils.writeOpKind(OpKind.RESP_OK, chnl);
					IOUtils.fileToChannel(section_path, chnl);
					log("Started edit of section " + sec.getDebugRepr() + " succesfull");
				}
				catch (IllegalArgumentException e) {
					log("Wrongly formatted full document name " + fulldocname);
					IOUtils.writeOpKind(OpKind.ERR_NO_DOCUMENT, chnl);
				}
				catch (NoSuchDocumentException e) {
					log("Edit of " + fulldocname + " failed: no such document");
					IOUtils.writeOpKind(OpKind.ERR_NO_DOCUMENT, chnl);
				}
				catch (NoPermissionException e) {
					log("Edit of " + fulldocname + " failed: no permissions");
					IOUtils.writeOpKind(OpKind.ERR_PERMISSION, chnl);
				}
				catch (NoSuchSectionException e) {
					log("Edit of section " + Integer.toString(nsec) + " failed: no such section");
					IOUtils.writeOpKind(OpKind.ERR_NO_SECTION, chnl);
				}
				catch (SectionBusyException e) {
					IOUtils.writeOpKind(OpKind.ERR_SECTION_BUSY, chnl);
					log("Edit of section " + Integer.toString(nsec) + " failed: section busy");
				}
                catch (UserBusyException e) {
					IOUtils.writeOpKind(OpKind.ERR_USER_BUSY, chnl);
					log("Edit failed: user busy");
				}
			}
			break;
			case OP_ENDEDIT: {
				Section sec = db_interface.userIsModifying(usr);
				if (sec == null) {
					log("End of edit failed: user isn't editing anything");
					IOUtils.writeOpKind(OpKind.ERR_USER_FREE, chnl);
				}
				else {
					log("End of edit succesfull");
					IOUtils.writeOpKind(OpKind.RESP_OK, chnl);
					db_interface.finishEditSection(usr, sec, chnl);
					log("File received succesfully");
				}
			}
			break;
			case OP_SHOWSEC: {
				String fulldocname = IOUtils.readString(chnl);
				int nsec = IOUtils.readInt(chnl);
				try {
					Section sec = new Section(fulldocname, nsec);
					if (!db_interface.documentExist(sec.getDocumentPath())) {
						IOUtils.writeOpKind(OpKind.ERR_NO_DOCUMENT, chnl);
					}
					else if (!db_interface.sectionExist(sec)) {
						IOUtils.writeOpKind(OpKind.ERR_NO_SECTION, chnl);
					}
					else {
						IOUtils.writeOpKind(OpKind.RESP_OK, chnl);
						IOUtils.writeBool(db_interface.isBeingModified(sec), chnl);
						IOUtils.fileToChannel(
						db_interface.getAbsolutePath(sec.getFullPath()), chnl
						);
					}
				}
				catch (IllegalArgumentException e) {
					log("Wrongly formatted full document name " + fulldocname);
					IOUtils.writeOpKind(OpKind.ERR_NO_DOCUMENT, chnl);
				}
			}
			break;
			case OP_SHOWDOC: {
				String fulldocname = IOUtils.readString(chnl);
				Path docpath = Paths.get(fulldocname);
				if (!db_interface.documentExist(docpath)) {
					IOUtils.writeOpKind(OpKind.ERR_NO_DOCUMENT, chnl);
				}
				else {
					int numsec = db_interface.sectionNumber(docpath);
					IOUtils.writeOpKind(OpKind.RESP_OK, chnl);
					IOUtils.writeInt(numsec, chnl);
					for (int i = 0; i < numsec; ++i) {
						Section sec = new Section(fulldocname, i);
						IOUtils.writeBool(db_interface.isBeingModified(sec), chnl);
						IOUtils.fileToChannel(
							db_interface.getAbsolutePath(sec.getFullPath()), chnl
						);
					}
				}
			}
			break;
			case OP_INVITE: {
				String invited_usr = IOUtils.readString(chnl);
				String docname = IOUtils.readString(chnl);
				if (!db_interface.documentExist(Paths.get(usr).resolve(docname))) {
					IOUtils.writeOpKind(OpKind.ERR_NO_DOCUMENT, chnl);
				}
				else {
					SocketChannel notifyChnl = user_to_socket.getOrDefault(invited_usr, null);
					boolean invitedOnline = notifyChnl != null;
					if (db_interface.invite(usr, docname, invited_usr, !invitedOnline) && invitedOnline) {
						// notify invitation on notifyChnl
						IOUtils.writeOpKind(OpKind.OP_INVITE, notifyChnl);
						IOUtils.writeString(docname, notifyChnl);
					}
					IOUtils.writeOpKind(OpKind.RESP_OK, chnl);
				}
			}
			break;
			case OP_LISTDOCS: {
				IOUtils.writeOpKind(OpKind.RESP_OK, chnl);
				Collection<String> documents = db_interface.userModificableDocuments(usr);
				IOUtils.writeInt(documents.size(), chnl);
				for(String doc : documents) {
					IOUtils.writeString(doc, chnl);
				}
			}
			break;
			default:
				log("Requested unknown operation: " + op.toString());
				IOUtils.writeOpKind(OpKind.ERR_UNKNOWN_OP, chnl);
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
			log("Exception handling operation: " + e.getMessage());
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
