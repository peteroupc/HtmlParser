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
   * Represents a node in the document object model (DOM). All DOM objects
   * implement this interface.
   */
  public interface INode {
    /**
     * Returns the _base URL of this node. URLs on this node are resolved relative
     * to this URL.
     * @return The return value is not documented yet.
     */
    String GetBaseURI();

    /**
     * Gets the direct children of this node.
     * @return The return value is not documented yet.
     */
    List<INode> GetChildNodes();

    /**
     * Gets the language of this node. Not defined in the DOM specification.
     * @return The return value is not documented yet.
     */
    String GetLanguage();

    /**
     * Gets the name of this node. For HTML elements, this will be the same as the
     * tag name.
     * @return The return value is not documented yet.
     */
    String GetNodeName();

    /**
     * Returns the type of node represented by this object.
     * @return The return value is not documented yet.
     */
    int GetNodeType();

    /**
     * Gets the document that owns this node.
     * @return The return value is not documented yet.
     */
    IDocument GetOwnerDocument();

    /**
     * Gets the parent node of this node.
     * @return The return value is not documented yet.
     */
    INode GetParentNode();

    /**
     * Gets all the text found within this element.
     * @return The return value is not documented yet.
     */
    String GetTextContent();

    /**
     * Not documented yet.
     * @param node The parameter {@code node} is a.getUpokecenter().getHtml().INode object.
     */
    void AppendChild (INode node);

    /**
     * Not documented yet.
     * @param node The parameter {@code node} is a.getUpokecenter().getHtml().INode object.
     */
    void RemoveChild (INode node);
  }
