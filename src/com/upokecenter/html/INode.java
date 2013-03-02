package com.upokecenter.html;

import java.util.List;

public interface INode {
	List<INode> getChildNodes();
	int getNodeType();
	INode getParentNode();
	IDocument getOwnerDocument();
}
