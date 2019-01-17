package server.lib;

import java.util.*;

public enum OpKind {
	LOGIN, CREATE, EDIT, ENDEDIT, SHOWSEC, SHOWDOC, INVITE, LISTDOCS;
	// VALUES: array of these values

	public static byte getNum(OpKind o) {
		return Arrays.asList(VALUES).indexOf(o);
	}
}
