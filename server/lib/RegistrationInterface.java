package server.lib;

import java.rmi.*;

// Interface for RMI registration to a TURING server
public interface RegistrationInterface extends Remote {
	/**
	 * Register a new user, if the username is not in use.
	 *
	 * @param  usr  the name of the new user to register
	 * @param  pwd  the password of the new user
	 * @throws RemoteException specified by RMI
	 * @throws InternalServerException if an error occurrs on the server during
	 *                                 elaboration
	 * @throws UsernameAlreadyInUseException if the username "usr" is already
	 *                                       in use
	 */
	public void register(String usr, String pwd)
		throws RemoteException, InternalServerException, UsernameAlreadyInUseException;
}
