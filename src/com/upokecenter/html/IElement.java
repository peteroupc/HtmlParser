package com.upokecenter.html;

import java.util.List;

public interface IElement extends INode {
	String getTagName();
	String getLocalName();
	String getNamespaceURI();
	String getAttribute(String name);
	String getAttributeNS(String namespace, String name);
	List<IElement> getElementsByTagName(String tagName);
}
