package com.upokecenter.html;

/**
 * Represents one of the attributes within an HTML element.
 * 
 * @author Peter
 *
 */
public interface IAttr {

	public String getPrefix();

	public String getLocalName();

	public String getName();

	public String getNamespaceURI();

	public String getValue();

}