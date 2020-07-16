package com.upokecenter.util;

  /**
   * Represents one of the attributes within an HTML element.
   */
  public interface IAttr {
    /**
     * Gets the attribute name's local name (the part after the colon, if it's
     * bound to a _namespace).
     * @return The return value is not documented yet.
     */
    String GetLocalName();

    /**
     * Gets the attribute's qualified name.
     * @return The return value is not documented yet.
     */
    String GetName();

    /**
     * Gets the attribute's _namespace URI, if it's bound to a _namespace.
     * @return The return value is not documented yet.
     */
    String GetNamespaceURI();

    /**
     * Gets the attribute name's prefix (the part before the colon, if it's bound
     * to a _namespace).
     * @return The return value is not documented yet.
     */
    String GetPrefix();

    /**
     * Gets the attribute's value.
     * @return The return value is not documented yet.
     */
    String GetValue();
  }
