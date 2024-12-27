package com.upokecenter.util;

import java.util.*;

/*

Licensed under the Expat License.

Copyright (C) 2013 Peter Occil

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
*/

  /**
   * * Represents an HTML document.
   */
  public interface IDocument extends INode {
    /**
     * Gets the character encoding used in this document.
     * @return The return value is not documented yet.
     */
    String GetCharset();

    /**
     * Gets the document type of this document, if any.
     * @return The return value is not documented yet.
     */
    IDocumentType GetDoctype();

    /**
     * Gets the root element of this document.
     * @return The return value is not documented yet.
     */
    IElement GetDocumentElement();

    /**
     * Not documented yet.
     * @param id The parameter {@code id} is a text string.
     * @return The return value is not documented yet.
     */
    IElement GetElementById (String id);

    /**
     * Gets all descendents, both direct and indirect, that have the specified tag
     * name, using a basic case-insensitive comparison. (Two strings are equal in
     * such a comparison, if they match after converting the basic uppercase
     * letters A to Z (U+0041 to U+005A) in both strings to basic lowercase
     * letters.) @param string A tag name.
     * @param _string The parameter {@code _string} is a text string.
     * @return The return value is not documented yet.
     */
    List<IElement> GetElementsByTagName (String _string);

    /**
     * Gets the document's address.
     * @return The return value is not documented yet.
     */
    String GetURL();
  }
