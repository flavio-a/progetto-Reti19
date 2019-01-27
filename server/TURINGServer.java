package server;

import java.util.*;
import java.util.concurrent.*;
import java.rmi.*;
import java.rmi.server.*;
import java.rmi.registry.*;
import java.io.IOException;
import java.net.*;
import server.lib.*;
// DEBUG
import java.nio.*;
import java.nio.file.*;
import java.nio.channels.*;

/**
 * Main TURING server class
 */
public class TURINGServer implements RegistrationInterface, Runnable {
	// ================================ STATIC ================================
	private static final int rmi_interaction_port = 24775;
	private static final String default_db_path = "./TURINGdb/";

	public static void main(String[] args) {
		try {
			TURINGServer server = new TURINGServer(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
			if (args.length > 2 && "test".equals(args[2])) {
				server.testLocal();
			}
			else {
				server.run();
			}
		}
		catch (IOException e) { // Catches also RemoteException
			System.out.println("Error creating server: " + e.getMessage());
		}
	}

	// ============================== NON STATIC ==============================
	private final DBInterface db_interface;
	private final ThreadPoolExecutor threadpool;

	private final BlockingQueue<ConcurrentSocketChannel> freesc;
	private final ConcurrentMap<ConcurrentSocketChannel, String> socket_to_user;
	private final ConcurrentMap<String, ConcurrentSocketChannel> user_to_socket;
	private final Selector selector;

	/**
	 * Creates a new instance of TURINGServer
	 *
	 * @param rmi_registry_port port on which listen for the RMI registry
	 * @param server_sock_port port on which the server listen for new
	 *                         connections
	 * @param db_path_set path to the db folder
	 */
	public TURINGServer(int rmi_registry_port, int server_sock_port, String db_path_set) throws RemoteException, IOException {
		super();
		bindRMIRegistry(rmi_registry_port);
		db_interface = new DBInterface(db_path_set);
		threadpool = new ThreadPoolExecutor(4, 10, 10, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
		ServerSocketChannel server_sock = ServerSocketChannel.open();
		server_sock.socket().bind(new InetSocketAddress(server_sock_port));
		server_sock.configureBlocking(false);

		freesc = new LinkedBlockingQueue<ConcurrentSocketChannel>();
		socket_to_user = new ConcurrentHashMap<ConcurrentSocketChannel, String>();
		user_to_socket = new ConcurrentHashMap<String, ConcurrentSocketChannel>();

		selector = Selector.open();
		server_sock.register(selector, SelectionKey.OP_ACCEPT);

		log("TURING server created");
	}

	/**
	 * Creates a new instance of TURINGServer specifying only non-defaulted
	 * parameters.
	 *
	 * @param rmi_registry_port port on which listen for the RMI registry
	 * @param server_sock_port port on which the server listen for new
	 *                         connections
	 */
	public TURINGServer(int rmi_registry_port, int server_sock_port) throws RemoteException, IOException {
		this(rmi_registry_port, server_sock_port, default_db_path);
	}

	/**
	 * Tries to create a new local RMI registry and bind this object to it.
	 *
	 * @param registry_port the port on which the registry should operate
	 * @throws RemoteException if something goes wrong during the process. The
	 *                         caller is required to handle the problem
	 */
	public void bindRMIRegistry(int registry_port) throws RemoteException {
		RegistrationInterface stub = (RegistrationInterface)UnicastRemoteObject.exportObject(this, registry_port);
		LocateRegistry.createRegistry(registry_port);
		Registry r = LocateRegistry.getRegistry(registry_port);
		r.rebind(Constants.rmi_registry_name, stub);
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

	/**
	 * Utility function to spawn an OperationHandler. Here just because of the
	 * number of parameters.
	 *
	 * @param chnl channel with an operation to be handled
	 */
	private void spawnOperationHandler(ConcurrentSocketChannel chnl) {
		threadpool.execute(new OperationHandler(chnl, db_interface, freesc, socket_to_user, user_to_socket, selector));
	}

	/**
	 * Run this instance of TURING server
	 */
	@Override
	public void run() {
		log("Server started");
		while (true) {
			try {
				selector.select();
				// Read all sc from freesc and add them back to selector
				Collection<ConcurrentSocketChannel> tmp = new ArrayList<ConcurrentSocketChannel>(freesc.size());
				freesc.drainTo(tmp);
				for (ConcurrentSocketChannel sc: tmp) {
					sc.configureBlocking(false);
					sc.register(selector, SelectionKey.OP_READ);
				}
				// Handle requests
				for (SelectionKey key: selector.selectedKeys()) {
					if (key.isReadable()) {
						// Remove this SocketChannel from the selector
						key.cancel();
						spawnOperationHandler((ConcurrentSocketChannel)key.attachment());
					}
					else if (key.isAcceptable()) {
						ConcurrentSocketChannel chnl = new ConcurrentSocketChannel(
							((ServerSocketChannel)key.channel()).accept()
						);
						log("Accepted connection by " + chnl.toString());
						spawnOperationHandler(chnl);
					}
				}
				selector.selectedKeys().clear();
			}
			catch (IOException e) {
				log("Exception caught while wating for accept: " + e.getMessage());
			}
		}
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public void register(String usr, String pwd) throws RemoteException, InternalServerException, UsernameAlreadyInUseException {
		log("Request: registration of username \"" + usr + "\"");
		try {
			if (db_interface.createUser(usr, pwd)) {
				log("User created succesfully");
			}
			else {
				log("User already exists");
				throw new UsernameAlreadyInUseException(usr);
			}
		}
		catch (java.io.IOException e) {
			log("IOException: " + e.getMessage());
			throw new InternalServerException("Try again in a few seconds.");
		}
	}

	// =============================== TESTING ===============================
	public void testLocal() throws IOException {
		log("======== TESTING =========");
		log("After each operation logs the operation an may ask for confirmation");

		try {
			IOUtils.testRowsOps();
			log("Row operations tested succesfully");
		}
		catch (RuntimeException e) {
			log("Error testing row operations: " + e.getMessage());
		}

		String usr1 = "cusu", pwd = "password", doc1 = "prova";
		int nsez1 = 5, sez1 = 2;
		String usr2 = "mano";
		try {
			register(usr1, pwd);
			log("Created user " + usr1 + ": worked?");
			System.console().readLine();
			if (db_interface.checkUser(usr1, pwd)) {
				log("User " + usr1 + " logged in");
			}
			else {
				log("ERROR: user " + usr1 + " not logged in");
			}
			db_interface.createDocument(usr1, doc1, nsez1);
			log("Created document " + doc1 + " of " + usr1 + ": worked?");
			System.console().readLine();

			Section sec = new Section(usr1, doc1, sez1);
			try (
				FileChannel prova_sez = FileChannel.open(db_interface.editSection(usr1, sec), StandardOpenOption.READ);
			) {
				log(usr1 + " is editing " + sec.getDebugRepr());
				log("================ content ================");
				byte[] buffer = new byte[1024];
				ByteBuffer byteBuffer = ByteBuffer.wrap(buffer);
			    for (int length = 0; (length = prova_sez.read(byteBuffer)) != -1;) {
			        System.out.write(buffer, 0, length);
			        byteBuffer.clear();
				}
			}
			log("================ end content ================");

			register(usr2, pwd);
			log("Created user " + usr2);

			// No permission
			try {
				Path prova_sez = db_interface.editSection(usr2, sec);
				log("ERROR: " + usr2 + " got editing of " + sec.getDebugRepr() + " but shouldn't have permission");
			}
			catch (SectionBusyException e) {
				log("ERROR: got SectionBusyException, but NoPermissionException expected");
			}
			catch (NoPermissionException e) {
				log(usr2 + " can't modify " + sec.getDebugRepr() + ": no permission");
			}

			db_interface.invite(usr1, doc1, usr2, false);
			log("Invited " + usr2 + " to " + usr1 + "/" + doc1);

			// Busy
			try {
				Path prova_sez = db_interface.editSection(usr2, sec);
				log("ERROR: " + usr2 + " got editing of " + sec.getDebugRepr() + " but should busy");
			}
			catch (SectionBusyException e) {
				log(usr2 + " can't modify " + sec.getDebugRepr() + ": busy");
			}
			catch (NoPermissionException e) {
				log("ERROR: got NoPermissionException, but SectionBusyException expected");
			}

			Pipe pipe = Pipe.open();
			IOUtils.fileToChannel(Paths.get("utils.sh"), pipe.sink());
			pipe.sink().close();
			db_interface.finishEditSection(usr1, db_interface.userIsModifying(usr1), pipe.source());
			pipe.source().close();
			log("Edit finished succesfully");

			// Success
			try {
				Path prova_sez = db_interface.editSection(usr2, sec);
				log(usr2 + " got editing of " + sec.getDebugRepr());
			}
			catch (SectionBusyException e) {
				log("ERROR: got SectionBusyException");
			}
			catch (NoPermissionException e) {
				log("ERROR: got NoPermissionException");
			}

			System.exit(0);
		}
		catch (Exception e) {
			log("ERROR: exception during testing:" + e.getMessage());
			e.printStackTrace();

			System.exit(1);
		}
	}
}
