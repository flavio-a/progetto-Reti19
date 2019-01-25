package server;

/**
 * Generic informations on a chat.
 * <p>
 * This class holds informations on a chat state, like the last byte of the
 * address and the number of connected people.
 */
public class ChatInfo {
	private byte addr;
	private int connected;

	/**
	 * Creates a new ChatInfo with the specified address. Initially it is empty.
	 *
	 * @param addr_set the last byte of the IP address of this chat
	 */
	public ChatInfo(byte addr_set) {
		addr = addr_set;
		connected = 0;
	}

	/**
	 * Get the last byte of the IP address.
	 *
	 * @return the last byte of the IP address
	 */
	public byte getAddr() {
		return addr;
	}

	/**
	 * Add a person to the number of people connected to the chat.
	 */
	public void connect() {
		++connected;
	}

	/**
	 * Remove a person from the number of people connected to the chat.
	 *
	 * @return true iff the chat is empty after the disconnection
	 */
	public boolean disconnect() {
		--connected;
		return isEmpty();
	}

	/**
	 * Check whether the chat is empty or not.
	 *
	 * @return true iff the chat is empty
	 */
	public boolean isEmpty() {
		return connected == 0;
	}
}
