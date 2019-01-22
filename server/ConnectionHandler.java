package server;

import java.util.*;
import java.util.concurrent.*;
import java.io.IOException;
import java.net.*;
import java.nio.*;
import java.nio.file.*;
import java.nio.channels.*;
import server.lib.*;

public class ConnectionHandler implements Runnable {
	private final SocketChannel chnl;
	private final DBInterface db_interface;
	private final BlockingQueue<SocketChannel> freesc;
	private final Map<SocketChannel, String> socket_to_user;
	private final Map<String, SocketChannel> user_to_socket;
	private final Selector selector;

	public ConnectionHandler(SocketChannel chnl_set, DBInterface db_interface_set, BlockingQueue<SocketChannel> freesc_set, Map<SocketChannel, String> socket_to_user_set, Map<String, SocketChannel> user_to_socket_set, Selector selector_set) {
		chnl = chnl_set;
		db_interface = db_interface_set;
		freesc = freesc_set;
		socket_to_user = socket_to_user_set;
		user_to_socket = user_to_socket_set;
		selector = selector_set;
	}

	/**
	 * Utility function to handle logging. May become something finer than a
	 * println in the future.
	 *
	 * @param s the string to log
	 */
	private void log(String s) {
		System.out.println(s);
	}

	@Override
	public void run() {
		try {
			OpKind op = IOUtils.readOpKind(chnl);
			String usr = socket_to_user.get(chnl);
			if (usr == null) {
				if (op == OpKind.OP_LOGIN) {
					// Login
					String login_usr, login_pwd;
					login_usr = IOUtils.readString(chnl);
					login_pwd = IOUtils.readString(chnl);
					if (!db_interface.checkUser(login_usr, login_pwd)) {
						// Wrong usr/pwd combination
						IOUtils.writeOpKind(OpKind.ERR_INVALID_LOGIN, chnl);
						chnl.close();
						return;
					}
					if (user_to_socket.get(usr) != null && user_to_socket.get(usr) != chnl) {
						// Username already in use on another connection
						IOUtils.writeOpKind(OpKind.ERR_USERNAME_BUSY, chnl);
						chnl.close();
						return;
					}
					socket_to_user.put(chnl, usr);
					user_to_socket.put(usr, chnl);
				}
				else {
					// Not logged connection with a non LOGIN operation
					IOUtils.writeOpKind(OpKind.ERR_UNLOGGED, chnl);
					chnl.close();
					return;
				}
			}
			// usr != null
			switch (op) {
				case OP_LOGIN:
					IOUtils.writeOpKind(OpKind.ERR_ALREADY_LOGGED, chnl);
					break;
				default:
					IOUtils.writeOpKind(OpKind.ERR_UNKNOWN_OP, chnl);
					break;
			}
		}
		catch (IOException e) {
			log("Exception handling connection: " + e.getMessage());
		}
	}
}
