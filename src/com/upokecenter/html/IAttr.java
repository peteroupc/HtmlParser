package com.upokecenter.html;

/**
 * Represents one of the attributes within an HTML element.
 * 
 * @author Peter
 *
 */
public interface IAttr {

	/**
	 * Gets the attribute name's prefix (the part before the colon,
	 * if it's bound to a namespace).
	 */
	public String getPrefix();

	/**
	 * Gets the attribute name's local name (the part after the colon,
	 * if it's bound to a namespace).
	 */
	public String getLocalName();

	/**
	 * Gets the attribute's qualified name.
	 */
	public String getName();

	/**
	 * Gets the attribute's namespace URI, if it's bound to a namespace.
	 */
	public String getNamespaceURI();

	/**
	 * Gets the attribute's value.
	 */
	public String getValue();

}