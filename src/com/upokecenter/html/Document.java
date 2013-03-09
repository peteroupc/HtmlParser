package com.upokecenter.html;

import java.util.ArrayList;
import java.util.List;

import com.upokecenter.html.HtmlParser.DocumentMode;
import com.upokecenter.util.StringUtility;

class Document extends Node implements IDocument {
	DocumentType doctype;
	String encoding;
	String baseurl;
	private DocumentMode docmode=DocumentMode.NoQuirksMode;

	Document() {
		super(NodeType.DOCUMENT_NODE);
	}

	boolean isHtmlDocument(){
		return true;
	}

	@Override
	public IDocumentType getDoctype(){
		return doctype;
	}

	@Override
	public IDocument getOwnerDocument(){
		return null;
	}

	@Override
	String toDebugString(){
		StringBuilder builder=new StringBuilder();
		for(Node node : childNodes){
			String str=node.toDebugString();
			if(str==null) {
				continue;
			}
			String[] strarray=str.split("\n");
			for(String el : strarray){
				builder.append("| ");
				builder.append(el.replace("~~~~","\n"));
				builder.append("\n");
			}
		}
		return builder.toString();
	}


	@Override
	public String getBaseURI() {
		return (baseurl==null) ? "" : baseurl;
	}

	DocumentMode getMode() {
		return docmode;
	}

	void setMode(DocumentMode mode) {
		docmode=mode;
	}


	private void collectElements(INode c, String s, List<IElement> nodes){
		if(c.getNodeType()==NodeType.ELEMENT_NODE){
			Element e=(Element)c;
			if(s==null || e.getLocalName().equals(s)){
				nodes.add(e);
			}
		}
		for(INode node : c.getChildNodes()){
			collectElements(node,s,nodes);
		}
	}

	private void collectElementsHtml(INode c, String s,
			String sLowercase, List<IElement> nodes){
		if(c.getNodeType()==NodeType.ELEMENT_NODE){
			Element e=(Element)c;
			if(s==null){
				nodes.add(e);
			} else if(HtmlParser.HTML_NAMESPACE.equals(e.getNamespaceURI()) &&
					e.getLocalName().equals(sLowercase)){
				nodes.add(e);
			} else if(e.getLocalName().equals(s)){
				nodes.add(e);
			}
		}
		for(INode node : c.getChildNodes()){
			collectElements(node,s,nodes);
		}
	}
	@Override
	public List<IElement> getElementsByTagName(String tagName) {
		if(tagName==null)
			throw new IllegalArgumentException();
		if(tagName.equals("*")) {
			tagName="";
		}
		List<IElement> ret=new ArrayList<IElement>();
		if(isHtmlDocument()){
			collectElementsHtml(this,tagName,
					StringUtility.toLowerCaseAscii(tagName),ret);
		} else {
			collectElements(this,tagName,ret);
		}
		return ret;
	}

	@Override
	public String getCharacterSet() {
		return (encoding==null) ? "utf-8" : encoding;
	}

}