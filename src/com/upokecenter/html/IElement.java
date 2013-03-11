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
