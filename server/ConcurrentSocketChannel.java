package server;

import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import java.io.IOException;
import java.nio.channels.*;
import java.nio.*;
// import server.lib.*;

/**
 * A concurrent implementation of SocketChannel.
 * <p>
 * This class allows writes and reads as a SocketChannel, but offers also
 * methods to synchronize writings so that a multithreaded process can write
 * avoiding mixing up different messages.
 * <p>
 * TODO: this should be a subclass of SocketChannel, but I don't know how to
 * do it.
 */
public class ConcurrentSocketChannel implements ByteChannel, Lock {
	private final SocketChannel chnl;
	private final Lock lock;

	/**
	 * Creates a new ConcurrentSocketChannel based on a given SocketChannel.
	 * <p>
	 * The instance passed to this constructor should not be accessed directly,
	 * otherwise there's no way to ensure synchronization.
	 *
	 * @param chnl_set the socket to use in this instance
	 */
	public ConcurrentSocketChannel(SocketChannel chnl_set) {
		chnl = chnl_set;
		lock = new ReentrantLock();
	}


	@Override
	public String toString() {
		return chnl.toString();
	}

	// Fake SelectableChannel interface
	public SelectableChannel configureBlocking(boolean block) throws IOException {
		return chnl.configureBlocking(block);
	}

	public SelectionKey register(Selector sel, int ops) throws ClosedChannelException {
		return chnl.register(sel, ops, this);
	}

	// Lock interface
	@Override
	public void lock() {
		lock.lock();
	}

	@Override
	public void lockInterruptibly() throws InterruptedException {
		lock.lockInterruptibly();
	}

	@Override
	public boolean tryLock() {
		return lock.tryLock();
	}

	@Override
	public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
		return lock.tryLock(time, unit);
	}

	@Override
	public void unlock() {
		lock.unlock();
	}

	@Override
	public Condition newCondition() {
		return lock.newCondition();
	}

	// ByteChannel interface
	@Override
	public int read(ByteBuffer dst) throws IOException {
		return chnl.read(dst);
	}

	@Override
	public int write(ByteBuffer src) throws IOException {
		return chnl.write(src);
	}

	@Override
	public boolean isOpen() {
		return chnl.isOpen();
	}

	@Override
	public void close() throws IOException {
		chnl.close();
	}
}
