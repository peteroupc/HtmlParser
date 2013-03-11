package com.upokecenter.html;

/**
 * 
 * Represents the HTML <!DOCTYPE> tag.
 * 
 * @author Peter
 *
 */
public interface IDocumentType extends INode {
	/**
	 * Gets the name of this document type.  For HTML documents,
	 * this should be "html".
	 * 
	 * 
	 */
	public String getName();
	/**
	 * 
	 * Gets the public identifier of this document type.
	 * 
	 * 
	 */
	public String getPublicId();
	/**
	 * Gets the system identifier of this document type.
	 * 
	 * 
	 */
	public String getSystemId();
}
