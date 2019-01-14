package server;

// import java.util.*;
import java.rmi.*;
import java.rmi.server.*;
import java.rmi.registry.*;
import java.io.IOException;
import server.lib.*;

public class TURINGServer implements RegistrationInterface, Runnable {
	// -------------------------- STATIC ------------------------------------
	public static final String rmi_registry_name = "TURING-REGISTRATION";
	private static final int rmi_interaction_port = 24775;
	private static final String default_db_path = "./TURING_db/";

	public static void main(String[] args) {
		try {
			TURINGServer server = new TURINGServer(Integer.parseInt(args[0]));
			System.out.println("TURING server created");
			server.run();
		}
		catch (IOException e) { // Catches also RemoteException
			System.out.println("Error creating server: " + e.getMessage());
		}
	}

	// ------------------------ NON STATIC ----------------------------------
	// private final String db_path;
	private final DBInterface fs_interface;

	public TURINGServer(int rmi_registry_port, String db_path_set) throws RemoteException, IOException {
		super();
		bindRMIRegistry(rmi_registry_port);
		fs_interface = new DBInterface(db_path_set);
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
			// System.out.println("Server running");
		}
	}

	// Description in the interface
	@Override
	public void register(String usr, String pwd) throws RemoteException, InternalServerException, UsernameAlreadyInUseException {
		log("Request: registration of username \"" + usr + "\"");
		try {
			if (fs_interface.createUser(usr, pwd)) {
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
}
