package com.upokecenter.html;

import java.util.Locale;


final class DocumentType extends Node implements IDocumentType {

	String publicId;
	String systemId;
	String name;

	public DocumentType() {
		super(NodeType.DOCUMENT_TYPE_NODE);
	}
	@Override
	String toDebugString(){
		StringBuilder builder=new StringBuilder();
		builder.append(String.format(Locale.US,"<!DOCTYPE %s>\n",
				name));
		return builder.toString();
	}
	@Override
	public String getName() {
		return name;
	}
	@Override
	public String getPublicId() {
		return publicId;
	}
	@Override
	public String getSystemId() {
		return systemId;
	}


	@Override
	public String getTextContent(){
		return null;
	}
}