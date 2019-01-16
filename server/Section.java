package server;

import java.nio.*;
import java.nio.file.*;
import server.*;

/**
 * Immutable class to describe a section of a document.
 */
public final class Section {
	private String owner, doc_name;
	private int n;

	public Section(String owner_set, String doc_name_set, int n_set) {
		owner = owner_set.trim();
		doc_name = doc_name_set.trim();
		n= n_set;
	}

	public int getN() {
		return n;
	}
	public String getOwner() {
		return owner;
	}
	public String getDocumentName() {
		return doc_name;
	}
	public String getQualifiedDocumentName() {
		return owner + doc_name;
	}

	public Path getDocumentPath() {
		return Paths.get(owner).resolve(doc_name);
	}
	public Path getSectionPath() {
		return Paths.get(DBInterface.section_file_prefix + Integer.toString(n));
	}
	public Path getFullPath() {
		return getDocumentPath().resolve(getSectionPath());
	}

	public boolean equals(Object obj) {
		if (!(obj instanceof Section)) {
            return false;
        }
		Section s = (Section)obj;
		return owner.equals(s.owner)
				&& doc_name.equals(s.doc_name)
				&& n == s.n;
	}

	public String getDebugRepr() {
		return owner + "/" + doc_name + "." + Integer.toString(n);
	}
}
