package server.lib;

import java.util.*;

/**
 * Kind of operations that client and server may exchange. Contains both client
 * operation and server responses.
 */
public enum OpKind {
	// Operations, requested by client
	OP_LOGIN, OP_CREATE, OP_EDIT, OP_ENDEDIT, OP_SHOWSEC, OP_SHOWDOC,
		OP_INVITE, OP_LISTDOCS,
	// Responses of the server
	RESP_OK, ERR_RETRY, ERR_UNKNOWN_OP,
		ERR_UNLOGGED, ERR_INVALID_LOGIN, ERR_USERNAME_BUSY, ERR_ALREADY_LOGGED;

	// ATTENTION: there's a max of 127 OpKind or the cast won't work
	/**
	 * Get the number of the specified OpKind in a single byte, in order to be
	 * sent through a channel.
	 *
	 * @param o OpKind to convert
	 * @return a single byte representing the OpKind o
	 */
	public static byte getNum(OpKind o) {
		return (byte)Arrays.asList(values()).indexOf(o);
	}

	/**
	 * Get the OpKind given a single byte that represents it.
	 *
	 * @param i byte to convert
	 * @return OpKind corresponding to that byte
	 */
	public static OpKind getOp(byte i) {
		return values()[i];
	}
}
