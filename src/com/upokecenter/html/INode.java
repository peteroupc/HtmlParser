package com.upokecenter.html;

import java.util.List;

/**
 * 
 * Represents a node in the document object model (DOM).  All DOM
 * objects implement this interface.
 * 
 * @author Peter
 *
 */
public interface INode {
	/**
	 * 
	 * Gets the direct children of this node.
	 * 
	 * @return A list of the direct children of this node.
	 */
	List<INode> getChildNodes();
	/**
	 * 
	 * Returns the type of node represented by this object.
	 * 
	 * @return A node type integer; see NodeType.
	 */
	int getNodeType();
	/**
	 * 
	 * Returns the base URL of this node.  URLs on this
	 * element are resolved relative to this URL.
	 * 
	 * 
	 */
	String getBaseURI();
	/**
	 * 
	 * Gets the parent node of this node.
	 * 
	 * @return the parent node, or null if this is the root node.
	 */
	INode getParentNode();
	/**
	 * 
	 * Gets the document that owns this node.
	 * 
	 * @return the owner document, or null if this is a document object.
	 */
	IDocument getOwnerDocument();
	/**
	 * 
	 * Gets all the text found within this element.
	 * 
	 * @return All the concatenated text, except comments, for Element
	 * nodes; or the text of Comment nodes; or null otherwise.
	 */
	String getTextContent();
}
