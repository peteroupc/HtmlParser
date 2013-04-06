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
 * Represents an HTML element.
 * 
 * @author Peter
 *
 */
public interface IElement extends INode {
	/**
	 * 
	 * Gets the name of the element as used on its HTML tags.
	 * 
	 * @return the element's tag name.  For HTML elements,
	 * an uppercase version of the name will be returned.
	 */
	String getTagName();
	/**
	 * 
	 * Gets the element's local name.  For elements with no
	 * namespace, this will equal the element's tag name.
	 * 
	 * @return the element's local name. This method doesn't
	 * convert it to uppercase even for HTML elements, unlike
	 * getTagName.
	 */
	String getLocalName();
	/**
	 * 
	 * Gets the namespace name of this element.  For HTML elements,
	 * it will equal "http://www.w3.org/1999/xhtml".
	 * 
	 * 
	 */
	String getNamespaceURI();
	/**
	 * 
	 * Gets an attribute declared on this element.
	 * 
	 * @param name an attribute name.
	 * @return the attribute's value, or null if the attribute doesn't
	 * exist.
	 */
	String getAttribute(String name);
	/**
	 * 
	 * Gets an attribute of this element, with the given namespace
	 * name and local name.
	 * 
	 * @param namespace the attribute's namespace name.
	 * @param name the attribute's local name.
	 * @return the attribute's value, or null if the attribute doesn't
	 * exist.
	 */
	String getAttributeNS(String namespace, String name);

	/**
	 * 
	 * Gets all descendents, both direct and indirect, that have
	 * the specified tag name, using ASCII case-insensitive matching.
	 * 
	 * @param tagName A tag name.
	 */
	List<IElement> getElementsByTagName(String tagName);
}
