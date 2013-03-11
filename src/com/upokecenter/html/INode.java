package com.upokecenter.html;

import java.util.List;

public interface INode {
	List<INode> getChildNodes();
	int getNodeType();
	String getBaseURI();
	INode getParentNode();
	IDocument getOwnerDocument();
	String getTextContent();
}
