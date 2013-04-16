/*

Licensed under the Expat License.

Copyright (C) 2013 Peter Occil

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.

 */

package com.upokecenter.html;

import java.util.ArrayList;
import java.util.List;

import com.upokecenter.util.StringUtility;

class Document extends Node implements IDocument {
	 DocumentType doctype;
	 String encoding;
	private DocumentMode docmode=DocumentMode.NoQuirksMode;

	 Document() {
		super(NodeType.DOCUMENT_NODE);
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
	public String getCharacterSet() {
		return (encoding==null) ? "utf-8" : encoding;
	}


	@Override
	public IDocumentType getDoctype(){
		return doctype;
	}

	@Override
	public IElement getDocumentElement() {
		for(INode node : getChildNodes()){
			if(node instanceof IElement)
				return (IElement)node;
		}
		return null;
	}

	@Override
	public IElement getElementById(String id) {
		if(id==null)
			throw new IllegalArgumentException();
		for(INode node : getChildNodes()){
			if(node instanceof IElement){
				if(id.equals(((IElement)node).getId()))
					return (IElement)node;
				IElement element=((IElement)node).getElementById(id);
				if(element!=null)return element;
			}
		}
		return null;
	}


	@Override
	public List<IElement> getElementsByTagName(String tagName) {
		if(tagName==null)
			throw new IllegalArgumentException();
		if(tagName.equals("*")) {
			tagName=null;
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

	 DocumentMode getMode() {
		return docmode;
	}
	@Override
	public  IDocument getOwnerDocument(){
		return null;
	}

	 boolean isHtmlDocument(){
		return true;
	}

	 void setMode(DocumentMode mode) {
		docmode=mode;
	}

	@Override
	 String toDebugString(){
		StringBuilder builder=new StringBuilder();
		for(Node node : getChildNodesInternal()){
			String str=node.toDebugString();
			if(str==null) {
				continue;
			}
			String[] strarray=StringUtility.splitAt(str,"\n");
			for(String el : strarray){
				builder.append("| ");
				builder.append(el.replace("~~~~","\n"));
				builder.append("\n");
			}
		}
		return builder.toString();
	}

	@Override
	public  String getNodeName(){
		return "#document";
	}

	 String address;

	@Override
	public String getURL() {
		return address;
	}
}