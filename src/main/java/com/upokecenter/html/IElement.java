package com.upokecenter.util;
/*
If you like this, you should donate to Peter O.
at: http://peteroupc.github.io/

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

import java.util.*;

  /**
   * Represents an HTML element.
   */
  public interface IElement extends INode {
    /**
     * Gets an attribute declared on this element.
     * @param name An attribute name.
     * @return The return value is not documented yet.
     */
    String GetAttribute (String name);

    /**
     * Gets an attribute of this element, with the given _namespace name and local
     * name. @param _namespace the attribute's _namespace name. @param name
     * the attribute's local name.
     * @return The return value is not documented yet.
     */
    String GetAttributeNS (String _namespace, String name);

    /**
     * Gets a list of all attributes declared on this element.
     * @return The return value is not documented yet.
     */
    List<IAttr> GetAttributes();

    /**
     * Gets all descendents, both direct and indirect, that have the specified id,
     * using case-sensitive matching. @param id.
     * @param id The parameter {@code id} is a text string.
     * @return The return value is not documented yet.
     */
    IElement GetElementById (String id);

    /**
     * Gets all descendents, both direct and indirect, that have the specified tag
     * name, using a basic case-insensitive comparison. (Two strings are
     * equal in such a comparison, if they match after converting the basic
     * upper-case letters A to Z (U+0041 to U+005A) in both strings to
     * basic lower-case letters.).
     * @param tagName A tag name.
     * @return The return value is not documented yet.
     */
    List<IElement> GetElementsByTagName (String tagName);

    /**
     * Gets the value of the id attribute on this element.
     * @return The return value is not documented yet.
     */
    String GetId();

    /**
     * Gets a serialized form of this HTML element.
     * @return The return value is not documented yet.
     */
    String GetInnerHTML();

    /**
     * Gets the element's local name. For elements with no _namespace, this will
     * equal the element's tag name.
     * @return The return value is not documented yet.
     */
    String GetLocalName();

    /**
     * Gets the _namespace name of this element. For HTML elements, it will equal
     *  "http://www.w3.org/1999/xhtml".
     * @return The return value is not documented yet.
     */
    String GetNamespaceURI();

    /**
     * Not documented yet.
     * @return The return value is not documented yet.
     */
    String GetPrefix();

    /**
     * Gets the name of the element as used on its HTML tags.
     * @return The return value is not documented yet.
     */
    String GetTagName();
  }
