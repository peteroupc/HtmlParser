package com.upokecenter.html;

import java.util.List;

public interface IDocument extends INode {
	List<IElement> getElementsByTagName(String string);
	public IDocumentType getDoctype();
	String getCharacterSet();
	public IElement getDocumentElement();
}
