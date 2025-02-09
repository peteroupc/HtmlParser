package com.upokecenter.rdf;

import com.upokecenter.util.*;

/*
Written in 2013 by Peter Occil.
Any copyright to this work is released to the Public Domain.
In case this is not possible, this work is also
licensed under the Unlicense: https://unlicense.org/

*/

  /**
   * Not documented yet.
   */
  public final class RDFTerm {
    /**
     * Type value for a blank node.
     */
    public static final int BLANK = 0; // type is blank node name, literal is blank

    /**
     * Type value for an IRI (Internationalized Resource Identifier.).
     */
    public static final int IRI = 1; // type is IRI, literal is blank

    /**
     * Type value for a string with a language tag.
     */
    public static final int LANGSTRING = 2; // literal is given

    /**
     * Type value for a piece of data serialized to a string.
     */
    public static final int TYPEDSTRING = 3; // type is IRI, literal is given

    private static void EscapeBlankNode(String str, StringBuilder builder) {
      int length = str.length();
      String hex = "0123456789ABCDEF";
      for (int i = 0; i < length; ++i) {
        int c = str.charAt(i);
        if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') ||
          (c > 0 && c >= '0' && c <= '9')) {
          builder.append((char)c);
        } else if ((c & 0xfc00) == 0xd800 && i + 1 < length &&
          (str.charAt(i + 1) & 0xfc00) == 0xdc00) {
          // Get the Unicode code point for the surrogate pair
          c = 0x10000 + ((c & 0x3ff) << 10) + (str.charAt(i + 1) & 0x3ff);
          builder.append("U00");
          builder.append(hex.charAt((c >> 20) & 15));
          builder.append(hex.charAt((c >> 16) & 15));
          builder.append(hex.charAt((c >> 12) & 15));
          builder.append(hex.charAt((c >> 8) & 15));
          builder.append(hex.charAt((c >> 4) & 15));
          builder.append(hex.charAt(c & 15));
          ++i;
        } else {
          builder.append("u");
          builder.append(hex.charAt((c >> 12) & 15));
          builder.append(hex.charAt((c >> 8) & 15));
          builder.append(hex.charAt((c >> 4) & 15));
          builder.append(hex.charAt(c & 15));
        }
      }
    }

    private static void EscapeLanguageTag(String str, StringBuilder builder) {
      int length = str.length();
      boolean hyphen = false;
      for (int i = 0; i < length; ++i) {
        int c = str.charAt(i);
        if (c >= 'A' && c <= 'Z') {
          builder.append((char)(c + 0x20));
        } else if (c >= 'a' && c <= 'z') {
          builder.append((char)c);
        } else if (hyphen && c >= '0' && c <= '9') {
          builder.append((char)c);
        } else if (c == '-') {
          builder.append((char)c);
          hyphen = true;
          if (i + 1 < length && str.charAt(i + 1) == '-') {
            builder.append('x');
          }
        } else {
          builder.append('x');
        }
      }
    }

    private static void EscapeString(
      String str,
      StringBuilder builder,
      boolean uri) {
      int length = str.length();
      String Hex = "0123456789ABCDEF";
      for (int i = 0; i < length; ++i) {
        int c = str.charAt(i);
        if (c == 0x09) {
          builder.append("\\t");
        } else if (c == 0x0a) {
          builder.append("\\n");
        } else if (c == 0x0d) {
          builder.append("\\r");
        } else if (c == 0x22) {
          builder.append("\\\"");
        } else if (c == 0x5c) {
          builder.append("\\\\");
        } else if (uri && c == '>') {
          builder.append("%3E");
        } else if (c >= 0x20 && c <= 0x7e) {
          builder.append((char)c);
        } else if ((c & 0xfc00) == 0xd800 && i + 1 < length &&
          (str.charAt(i + 1) & 0xfc00) == 0xdc00) {
          // Get the Unicode code point for the surrogate pair
          c = 0x10000 + ((c & 0x3ff) << 10) + (str.charAt(i + 1) & 0x3ff);
          builder.append("\\U00");
          builder.append(Hex.charAt((c >> 20) & 15));
          builder.append(Hex.charAt((c >> 16) & 15));
          builder.append(Hex.charAt((c >> 12) & 15));
          builder.append(Hex.charAt((c >> 8) & 15));
          builder.append(Hex.charAt((c >> 4) & 15));
          builder.append(Hex.charAt(c & 15));
          ++i;
        } else {
          builder.append("\\u");
          builder.append(Hex.charAt((c >> 12) & 15));
          builder.append(Hex.charAt((c >> 8) & 15));
          builder.append(Hex.charAt((c >> 4) & 15));
          builder.append(Hex.charAt(c & 15));
        }
      }
    }

    private final String typeOrLanguage;
    private final String value;
    private final int kind;

    private RDFTerm(int kind, String typeOrLanguage, String value) {
      this.kind = kind;
      this.typeOrLanguage = typeOrLanguage;
      this.value = value;
    }

    /**
     * Predicate for RDF types.
     */
    public static final RDFTerm A =
      FromIRI("http://www.w3.org/1999/02/22-rdf-syntax-ns#type");

    /**
     * Predicate for the first object in a list.
     */
    public static final RDFTerm FIRST = FromIRI(
        "http://www.w3.org/1999/02/22-rdf-syntax-ns#first");

    /**
     * object for nil, the end of a list, or an empty list.
     */
    public static final RDFTerm NIL = FromIRI(
        "http://www.w3.org/1999/02/22-rdf-syntax-ns#nil");

    /**
     * Predicate for the remaining objects in a list.
     */
    public static final RDFTerm REST = FromIRI(
        "http://www.w3.org/1999/02/22-rdf-syntax-ns#rest");

    /**
     * object for false.
     */
    public static final RDFTerm FALSE = FromTypedString(
        "false",
        "http://www.w3.org/2001/XMLSchema#boolean");

    /**
     * object for true.
     */
    public static final RDFTerm TRUE = FromTypedString(
        "true",
        "http://www.w3.org/2001/XMLSchema#boolean");

    /**
     * Not documented yet.
     * @param name The parameter {@code name} is a text string.
     * @return A RDFTerm object.
     * @throws IllegalArgumentException Name is empty.
     * @throws NullPointerException The parameter {@code name} is null.
     */
    public static RDFTerm FromBlankNode(String name) {
      if (name == null) {
        throw new NullPointerException("name");
      }
      if (name.length() == 0) {
        throw new IllegalArgumentException("name is empty.");
      }
      return new RDFTerm(BLANK, null, name);
    }

    /**
     * Not documented yet.
     * @param iri The parameter {@code iri} is a text string.
     * @return A RDFTerm object.
     * @throws NullPointerException The parameter {@code iri} is null.
     */
    public static RDFTerm FromIRI(String iri) {
      if (iri == null) {
        throw new NullPointerException("iri");
      }
      return new RDFTerm(IRI, null, iri);
    }

    /**
     * Not documented yet.
     * @param str The parameter {@code str} is a text string.
     * @param languageTag The parameter {@code languageTag} is a text string.
     * @return A RDFTerm object.
     * @throws NullPointerException The parameter {@code str} or {@code
     * languageTag} is null.
     * @throws IllegalArgumentException LanguageTag is empty.
     */
    public static RDFTerm FromLangString(String str, String languageTag) {
      if (str == null) {
        throw new NullPointerException("str");
      }
      if (languageTag == null) {
        throw new NullPointerException("languageTag");
      }
      if (languageTag.length() == 0) {
        throw new IllegalArgumentException("languageTag is empty.");
      }
      return new RDFTerm(LANGSTRING, languageTag, str);
    }

    /**
     * Not documented yet.
     * @param str The parameter {@code str} is a text string.
     * @return A RDFTerm object.
     */
    public static RDFTerm FromTypedString(String str) {
      return FromTypedString(str, "http://www.w3.org/2001/XMLSchema#String");
    }

    /**
     * Not documented yet.
     * @param str The parameter {@code str} is a text string.
     * @param iri The parameter {@code iri} is a text string.
     * @return A RDFTerm object.
     * @throws NullPointerException The parameter {@code str} or {@code iri} is
     * null.
     * @throws IllegalArgumentException Iri is empty.
     */
    public static RDFTerm FromTypedString(String str, String iri) {
      if (str == null) {
        throw new NullPointerException("str");
      }
      if (iri == null) {
        throw new NullPointerException("iri");
      }
      if (iri.length() == 0) {
        throw new IllegalArgumentException("iri is empty.");
      }
      return new RDFTerm(TYPEDSTRING, iri, str);
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
      RDFTerm other = ((obj instanceof RDFTerm) ? (RDFTerm)obj : null);
      if (other == null) {
        return false;
      }
      if (this.kind != other.kind) {
        return false;
      }
      if (this.typeOrLanguage == null) {
        if (other.typeOrLanguage != null) {
          return false;
        }
      } else if (!this.typeOrLanguage.equals(other.typeOrLanguage)) {
        return false;
      }
      if (this.value == null) {
        return other.value == null;
      } else {
        return this.value.equals(other.value);
      }
    }

    /**
     * Not documented yet.
     * @return A 32-bit signed integer.
     */
    public int GetKind() {
      return this.kind;
    }

    /**
     * Gets the language tag or data type for this RDF literal.
     * @return A text string.
     */
    public String GetTypeOrLanguage() {
      return this.typeOrLanguage;
    }

    /**
     * Gets the IRI, blank node identifier, or lexical form of an RDF literal.
     * @return A text string.
     */
    public String GetValue() {
      return this.value;
    }

    /**
     * Not documented yet.
     * @return The return value is not documented yet.
     */
    @Override public final int hashCode() {
      {
        int prime = 31;
        int result = prime + this.kind;
        result = (prime * result) + ((this.typeOrLanguage == null) ? 0 :
          this.typeOrLanguage.hashCode());
        boolean isnull = this.value == null;
        result = (prime * result) + (isnull ? 0 : this.value.hashCode());
        return result;
      }
    }

    /**
     * Gets a value indicating whether this term is a blank node.
     * @return Either {@code true} or {@code false}.
     */
    public boolean IsBlank() {
      return this.kind == BLANK;
    }

    /**
     * Not documented yet.
     * @param str The parameter {@code str} is a text string.
     * @return Either {@code true} or {@code false}.
     */
    public boolean IsIRI(String str) {
      return this.kind == IRI && str != null && str.equals(this.value);
    }

    private static final String XmlSchemaString =
      "http://www.w3.org/2001/XMLSchema#String";

    /**
     * Not documented yet.
     * @return Either {@code true} or {@code false}.
     */
    public boolean IsOrdinaryString() {
      return this.kind == TYPEDSTRING &&
        XmlSchemaString.equals(this.typeOrLanguage);
    }

    /**
     * Gets a string representation of this RDF term in N-Triples format. The
     * string will not end in a line break.
     * @return A string representation of this object.
     */
    @Override public final String toString() {
      StringBuilder builder = null;
      if (this.kind == BLANK) {
        builder = new StringBuilder();
        builder.append("_:");
        EscapeBlankNode(this.value, builder);
      } else if (this.kind == LANGSTRING) {
        builder = new StringBuilder();
        builder.append("\"");
        EscapeString(this.value, builder, false);
        builder.append("\"@");
        EscapeLanguageTag(this.typeOrLanguage, builder);
      } else if (this.kind == TYPEDSTRING) {
        builder = new StringBuilder();
        builder.append("\"");
        EscapeString(this.value, builder, false);
        builder.append("\"");
        if (!XmlSchemaString.equals(this.typeOrLanguage)) {
          builder.append("^^<");
          EscapeString(this.typeOrLanguage, builder, true);
          builder.append(">");
        }
      } else if (this.kind == IRI) {
        builder = new StringBuilder();
        builder.append("<");
        EscapeString(this.value, builder, true);
        builder.append(">");
      } else {
        return "<>";
      }
      return builder.toString();
    }
  }
