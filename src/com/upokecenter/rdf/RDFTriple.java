// Written by Peter Occil, 2013. In the public domain.
// Public domain dedication: http://creativecommons.org/publicdomain/zero/1.0/
package com.upokecenter.rdf;

public final class RDFTriple {
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((object == null) ? 0 : object.hashCode());
		result = prime * result
				+ ((predicate == null) ? 0 : predicate.hashCode());
		result = prime * result + ((subject == null) ? 0 : subject.hashCode());
		return result;
	}

	@Override
	public String toString(){
		return subject.toString()+" "+predicate.toString()+" "+object.toString()+" .";
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RDFTriple other = (RDFTriple) obj;
		if (object == null) {
			if (other.object != null)
				return false;
		} else if (!object.equals(other.object))
			return false;
		if (predicate == null) {
			if (other.predicate != null)
				return false;
		} else if (!predicate.equals(other.predicate))
			return false;
		if (subject == null) {
			if (other.subject != null)
				return false;
		} else if (!subject.equals(other.subject))
			return false;
		return true;
	}

	public RDFTriple(RDFTerm subject, RDFTerm predicate, RDFTerm object) {
		setSubject(subject);
		setPredicate(predicate);
		setObject(object);
	}

	public RDFTriple(RDFTriple triple){
		if(triple==null)throw new NullPointerException("triple");
		setSubject(triple.subject);
		setPredicate(triple.predicate);
		setObject(triple.object);
	}

	public RDFTerm getSubject() {
		return subject;
	}

	private void setSubject(RDFTerm subject) {
		if((subject)==null)throw new NullPointerException("subject");
		if(!(subject.getKind()==RDFTerm.IRI ||
				subject.getKind()==RDFTerm.BLANK))throw new IllegalArgumentException("doesn't satisfy subject.kind==RDFTerm.IRI || subject.kind==RDFTerm.BLANK");
		this.subject = subject;
	}

	public RDFTerm getPredicate() {
		return predicate;
	}

	private void setPredicate(RDFTerm predicate) {
		if((predicate)==null)throw new NullPointerException("predicate");
		if(!(predicate.getKind()==RDFTerm.IRI))throw new IllegalArgumentException("doesn't satisfy predicate.kind==RDFTerm.IRI");
		this.predicate = predicate;
	}

	public RDFTerm getObject() {
		return object;
	}

	private void setObject(RDFTerm object) {
		if((object)==null)throw new NullPointerException("object");
		this.object = object;
	}

	private RDFTerm subject, predicate, object;
}
