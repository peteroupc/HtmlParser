package com.upokecenter.html;

/**
 * 
 * Contains constants for node types.
 * 
 * @author Peter
 *
 */
public final class NodeType {
	private NodeType(){}
	/**
	 * A document node.
	 */
	public static final int DOCUMENT_NODE=9;
	/**
	 * A comment node.
	 */
	public static final int COMMENT_NODE=8;
	/**
	 * An HTML element node.
	 */
	public static final int ELEMENT_NODE=1;
	/**
	 * A node containing text.
	 */
	public static final int TEXT_NODE=3;
	/**
	 * A DOCTYPE node.
	 */
	public static final int DOCUMENT_TYPE_NODE = 10;
}