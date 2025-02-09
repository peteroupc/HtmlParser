package com.upokecenter.rdf;

/*
Written in 2013 by Peter Occil.
Any copyright to this work is released to the Public Domain.
In case this is not possible, this work is also
licensed under the Unlicense: https://unlicense.org/

*/

  /**
   * Not documented yet.
   */
  public final class RDFTriple {
    private final RDFTerm subject;
    private final RDFTerm predicate;
    private final RDFTerm objectRdf;

    /**
     * Initializes a new instance of the {@link com.upokecenter.rdf.RDFTriple}
     * class.
     * @param subject The subject term.
     * @param predicate The predicate term.
     * @param objectRdf The object term.
     * @throws NullPointerException The parameter {@code objectRdf} or {@code
     * predicate} or {@code subject} is null.
     */
    public RDFTriple(RDFTerm subject, RDFTerm predicate, RDFTerm objectRdf) {
      if (objectRdf == null) {
        throw new NullPointerException("objectRdf");
      }
      this.objectRdf = objectRdf;
      if (predicate == null) {
        throw new NullPointerException("predicate");
      }
      if (!(predicate.GetKind() == RDFTerm.IRI)) {
        throw new IllegalArgumentException("doesn't satisfy" +
          "\u0020predicate.kind==RDFTerm.IRI");
      }
      this.predicate = predicate;
      if (subject == null) {
        throw new NullPointerException("subject");
      }
      if (!(subject.GetKind() == RDFTerm.IRI ||
        subject.GetKind() == RDFTerm.BLANK)) {
        throw new
        IllegalArgumentException(
          "doesn't satisfy subject.kind==RDFTerm.IRI ||" +
          "\u0020subject.kind==RDFTerm.BLANK");
      }
      this.subject = subject;
    }

    /**
     * Initializes a new instance of the {@link com.upokecenter.rdf.RDFTriple}
     * class.
     * @param triple The parameter {@code triple} is a RDFTriple object.
     */
    public RDFTriple(RDFTriple triple) {
 this(
        Check(triple).subject,
        Check(triple).predicate,
        Check(triple).objectRdf);
    }

    private static RDFTriple Check(RDFTriple triple) {
      if (triple == null) {
        throw new NullPointerException("triple");
      }
      return triple;
    }

    /**
     * Not documented yet.
     * @param obj The parameter {@code obj} is a object object.
     * @return The return value is not documented yet.
     */
    @Override public final boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      RDFTriple other = ((obj instanceof RDFTriple) ? (RDFTriple)obj : null);
      if (other == null) {
        return false;
      }
      if (this.objectRdf == null) {
        if (other.objectRdf != null) {
          return false;
        }
      } else if (!this.objectRdf.equals(other.objectRdf)) {
        return false;
      }
      if (this.predicate == null) {
        if (other.predicate != null) {
          return false;
        }
      } else if (!this.predicate.equals(other.predicate)) {
        return false;
      }
      if (this.subject == null) {
        return other.subject == null;
      } else {
        return this.subject.equals(other.subject);
      }
    }

    /**
     * Not documented yet.
     * @return A RDFTerm object.
     */
    public RDFTerm GetObject() {
      return this.objectRdf;
    }

    /**
     * Not documented yet.
     * @return A RDFTerm object.
     */
    public RDFTerm GetPredicate() {
      return this.predicate;
    }

    /**
     * Not documented yet.
     * @return A RDFTerm object.
     */
    public RDFTerm GetSubject() {
      return this.subject;
    }

    /**
     * Not documented yet.
     * @return The return value is not documented yet.
     */
    @Override public final int hashCode() {
      {
        int prime = 31;
        int result = prime + ((this.objectRdf == null) ? 0 :
          this.objectRdf.hashCode());
        result = (prime * result) +
          ((this.predicate == null) ? 0 : this.predicate.hashCode());
        boolean subjnull = this.subject == null;
        result = (prime * result) + (subjnull ? 0 :
          this.subject.hashCode());
        return result;
      }
    }

    /**
     * Not documented yet.
     * @return The return value is not documented yet.
     */
    @Override public final String toString() {
      return this.subject.toString() + " " + this.predicate.toString() + " " +
        this.objectRdf.toString() + " .";
    }
  }
