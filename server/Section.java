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

	/**
	 * Create a section from the name of the owner, of the document and the
	 * number of section.
	 *
	 * @param owner_set name of the owner
	 * @param doc_name_set name of the document
	 * @param n_set number of section
	 */
	public Section(String owner_set, String doc_name_set, int n_set) {
		owner = owner_set.trim();
		doc_name = doc_name_set.trim();
		n = n_set;
	}

	/**
	 * Create a section from the full name of the document (ie: owner/name) and
	 * the number of section.
	 *
	 * @param full_doc_name full name of the document
	 * @param n_set number of section
	 * @throws IllegalArgumentException if the full_doc_nam isn't formatted
	 *                                  properly (ie: doesn't contain a '/')
	 */
	public Section(String full_doc_name, int n_set) throws IllegalArgumentException {
		int idx_slash = full_doc_name.indexOf('/');
		if (idx_slash == -1) {
			throw new IllegalArgumentException("Missing '/' in full document name");
		}
		owner = full_doc_name.substring(0, idx_slash);
		doc_name = full_doc_name.substring(idx_slash + 1);
		n = n_set;
	}

	/**
	 * Get the number of this section.
	 *
	 * @return the number of this section
	 */
	public int getN() {
		return n;
	}
	public String getOwner() {
		return owner;
	}
	public String getDocumentName() {
		return doc_name;
	}
	/**
	 * Return the full document name (ie: owner/document_name) of this instance.
	 *
	 * @return the full document name
	 */
	public String getFullDocumentName() {
		return owner + doc_name;
	}

	/**
	 * Get the path to this section's document (relative to the db root
	 * directory)
	 *
	 * @return the path to this section's document
	 */
	public Path getDocumentPath() {
		return Paths.get(owner).resolve(doc_name);
	}
	/**
	 * Get the path of this section relative to its document's directory.
	 *
	 * @return this section's path
	 */
	public Path getSectionPath() {
		return Paths.get(DBInterface.section_file_prefix + Integer.toString(n));
	}
	/**
	 * Get this section's full path (relative to the db root directory).
	 *
	 * @return this section's full path
	 */
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
