package server;

import java.util.*;
import java.rmi.*;
import server.lib.*;

public class TURINGServer implements RegistrationInterface {

	public static void main(String[] args) {
		System.out.println("TURING server started");
	}

	@Override
	public void register(String usr, String pwd) throws RemoteException, UsernameAlreadyInUseException {
		throw new UsernameAlreadyInUseException(usr);
	}
}
