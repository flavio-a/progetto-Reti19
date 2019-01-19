package server;

import java.io.IOException;
import java.net.*;
import java.nio.*;
import java.nio.file.*;
import java.nio.channels.*;
import java.util.concurrent.*;

public class ConnectionHandler implements Runnable {
	private final SocketChannel chnl;
	private final BlockingQueue<SocketChannel> freesc;
	private final ConcurrentMap<SocketChannel, String> socket_to_user;
	private final ConcurrentMap<String, SocketChannel> user_to_socket;
	private final Selector selector;

	public ConnectionHandler(SocketChannel chnl_set, BlockingQueue<SocketChannel> freesc_set, ConcurrentMap<SocketChannel, String> socket_to_user_set, ConcurrentMap<String, SocketChannel> user_to_socket_set, Selector selector_set) {
		chnl = chnl_set;
		freesc = freesc_set;
		socket_to_user = socket_to_user_set;
		user_to_socket = user_to_socket_set;
		selector = selector_set;
	}

	@Override
	public void run() {
		ByteBuffer buff = ByteBuffer.allocate(1);

		// Get filename from client
		StringBuilder filename_builder = new StringBuilder();
		try {
			// chnl.read returns -1 only after EOS, that is when
			// then client closes the connection in this direction
			while (chnl.read(buff) != -1) {
				buff.flip();
				while (buff.hasRemaining()) {
					filename_builder.append((char) buff.get());
				}
				buff.clear();
			}
			String filename = filename_builder.toString();
			chnl.shutdownInput();
			// println("Requested " + filename);

			// Read the file to the socketChannel, connecting
			// directly the socketChannel to the FileChannel
			try (
				FileChannel inFile = FileChannel.open(Paths.get(filename), StandardOpenOption.READ);
			) {
				long position = 0;
				long size = inFile.size();
				while (position < size) {
					long count = inFile.transferTo(position, size, chnl);
					position += count;
					size -= count;
				}
			}
			// println("File sent");
		}
		catch (IOException e) {
			// println("Error during comunication with the client");
		}
	}
}
