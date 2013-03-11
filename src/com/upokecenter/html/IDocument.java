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
