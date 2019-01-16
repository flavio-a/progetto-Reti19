package server;

// import java.util.*;
import java.rmi.*;
import java.rmi.server.*;
import java.rmi.registry.*;
import java.io.IOException;
import server.lib.*;
// DEBUG
import java.nio.*;
import java.nio.file.*;
import java.nio.channels.*;
import java.nio.charset.*;

public class TURINGServer implements RegistrationInterface, Runnable {
	// ================================ STATIC ================================
	public static final String rmi_registry_name = "TURING-REGISTRATION";
	private static final int rmi_interaction_port = 24775;
	private static final String default_db_path = "./TURING_db/";

	public static void main(String[] args) {
		try {
			TURINGServer server = new TURINGServer(Integer.parseInt(args[0]));
			System.out.println("TURING server created");
			if ("test".equals(args[1])) {
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
	// private final String db_path;
	private final DBInterface db_interface;

	public TURINGServer(int rmi_registry_port, String db_path_set) throws RemoteException, IOException {
		super();
		bindRMIRegistry(rmi_registry_port);
		db_interface = new DBInterface(db_path_set);
	}

	public TURINGServer(int rmi_registry_port) throws RemoteException, IOException {
		this(rmi_registry_port, default_db_path);
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
		r.rebind(rmi_registry_name, stub);
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
		log("Server started");
		while (true) {
			try {
				Thread.sleep(10000);
			}
			catch (Exception e) {

			}
			System.out.println("Server running");
		}
	}

	// Description in the interface
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
			db_interface.testRowsOps();
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
				log("User " + usr1 + " not logged in");
			}
			db_interface.createDocument(usr1, doc1, nsez1);
			log("Created document " + doc1 + " of " + usr1 + ": worked?");
			System.console().readLine();

			Section sec = new Section(usr1, doc1, sez1);
			try (
				FileChannel prova_sez = db_interface.editSection(usr1, sec);
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
			log("Created user " + usr2 + ": worked?");
			System.console().readLine();

			try (
				FileChannel prova_sez = db_interface.editSection(usr2, sec);
			){
				log(usr2 + " got editing of " + sec.getDebugRepr());
			}
			catch (SectionBusyException e) {
				log(usr2 + " can't modify " + sec.getDebugRepr() + ": busy");
			}
			catch (NoPermissionException e) {
				log(usr2 + " can't modify " + sec.getDebugRepr() + ": no permission");
			}

			try (
				FileChannel newContent = FileChannel.open(Paths.get("server/TURINGServer.java"), StandardOpenOption.READ);
			) {
				db_interface.finishEditSection(usr1, newContent);
				log("Edit finished succesfully");
			}
			try (
				FileChannel prova_sez = db_interface.editSection(usr2, sec);
			) {
				log(usr2 + " got editing of " + sec.getDebugRepr());
			}
			catch (SectionBusyException e) {
				log(usr2 + " can't modify " + sec.getDebugRepr() + ": busy");
			}
			catch (NoPermissionException e) {
				log(usr2 + " can't modify " + sec.getDebugRepr() + ": no permission");
			}
		}
		catch (Exception e) {
			log("Exception during testing:" + e.getMessage());
			e.printStackTrace();
		}
	}
}
