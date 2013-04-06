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

package com.upokecenter.html;

import java.util.List;

/**
 * 
 * Represents an HTML document.  This is the root of
 * the document hierarchy.
 * 
 * @author Peter
 *
 */
public interface IDocument extends INode {
	/**
	 * 
	 * Gets all descendents, both direct and indirect, that have
	 * the specified tag name, using ASCII case-insensitive matching.
	 * 
	 * @param string A tag name.
	 * 
	 */
	List<IElement> getElementsByTagName(String string);
	/**
	 * 
	 * Gets the document type of this document, if any.
	 * 
	 * 
	 */
	public IDocumentType getDoctype();
	/**
	 * 
	 * Gets the character encoding used in this document.
	 * 
	 * @return A character encoding name.
	 */
	String getCharacterSet();
	/**
	 * 
	 * Gets the root element of this document.
	 * 
	 * 
	 */
	public IElement getDocumentElement();
}
