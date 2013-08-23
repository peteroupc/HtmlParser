/*
Written in 2013 by Peter Occil.  
Any copyright is dedicated to the Public Domain.
http://creativecommons.org/publicdomain/zero/1.0/

If you like this, you should donate to Peter O.
at: http://upokecenter.com/d/
*/
package com.upokecenter.rdf;

public final class RDFTriple {
  private RDFTerm subject, predicate, object;

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

  public RDFTerm getObject() {
    return object;
  }

  public RDFTerm getPredicate() {
    return predicate;
  }

  public RDFTerm getSubject() {
    return subject;
  }

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

  private void setObject(RDFTerm object) {
    if((object)==null)throw new NullPointerException("object");
    this.object = object;
  }

  private void setPredicate(RDFTerm predicate) {
    if((predicate)==null)throw new NullPointerException("predicate");
    if(!(predicate.getKind()==RDFTerm.IRI))throw new IllegalArgumentException("doesn't satisfy predicate.kind==RDFTerm.IRI");
    this.predicate = predicate;
  }

  private void setSubject(RDFTerm subject) {
    if((subject)==null)throw new NullPointerException("subject");
    if(!(subject.getKind()==RDFTerm.IRI ||
        subject.getKind()==RDFTerm.BLANK))throw new IllegalArgumentException("doesn't satisfy subject.kind==RDFTerm.IRI || subject.kind==RDFTerm.BLANK");
    this.subject = subject;
  }

  @Override
  public String toString(){
    return subject.toString()+" "+predicate.toString()+" "+object.toString()+" .";
  }
}
