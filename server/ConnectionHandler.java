package server;

import java.io.IOException;
import java.net.*;
import java.nio.*;
import java.nio.file.*;
import java.nio.channels.*;

public class ConnectionHandler implements Runnable {
	private final SocketChannel chnl;

	public ConnectionHandler(TURINGServer server, SocketChannel chnl_set) {
		chnl = chnl_set;
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
