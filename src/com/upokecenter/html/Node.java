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

import com.upokecenter.util.URL;

class Node implements INode {
	private final List<Node> childNodes;
	private Node parentNode=null;
	private IDocument ownerDocument=null;

	int nodeType;

	public Node(int nodeType){
		this.nodeType=nodeType;
		childNodes=new ArrayList<Node>();
	}

	public void appendChild(Node node){
		if(node==this)
			throw new IllegalArgumentException();
		node.parentNode=this;
		node.ownerDocument=(this instanceof IDocument) ? (IDocument)this : ownerDocument;
		childNodes.add(node);
	}

	private String baseURI=null;

	@Override
	public  String getBaseURI() {
		INode parent=getParentNode();
		if(baseURI==null){
			if(parent==null)
				return "about:blank";
			else
				return parent.getBaseURI();
		} else {
			if(parent==null)
				return baseURI;
			else {
				URL ret=URL.parse(baseURI,URL.parse(parent.getBaseURI()));
				return (ret==null) ? parent.getBaseURI() : ret.toString();
			}
		}
	}

	 void setBaseURI(String value){
		INode parent=getParentNode();
		if(parent==null){
			baseURI=value;
		} else {
			String val=URL.parse(value,URL.parse(parent.getBaseURI())).toString();
			baseURI=(val==null) ? parent.getBaseURI() : val.toString();
		}
	}

	@Override
	public List<INode> getChildNodes() {
		return new ArrayList<INode>(childNodes);
	}

	 List<Node> getChildNodesInternal(){
		return childNodes;
	}

	@Override
	public int getNodeType(){
		return nodeType;
	}

	@Override
	public  IDocument getOwnerDocument(){
		return ownerDocument;
	}

	@Override
	public INode getParentNode() {
		return parentNode;
	}
	@Override
	public  String getTextContent(){
		return null;
	}
	public void insertBefore(Node child, Node sibling){
		if(sibling==null){
			appendChild(child);
			return;
		}
		if(childNodes.size()==0)
			throw new IllegalStateException();
		int childNodesSize=childNodes.size();
		for(int j=0;j<childNodesSize;j++){
			if(childNodes.get(j).equals(sibling)){
				child.parentNode=this;
				child.ownerDocument=(child instanceof IDocument) ? (IDocument)this : ownerDocument;
				childNodes.add(j,child);
				return;
			}
		}
		throw new IllegalArgumentException();
	}
	public void removeChild(Node node){
		node.parentNode=null;
		childNodes.remove(node);
	}

	void setOwnerDocument(IDocument document){
		ownerDocument=document;
	}

	 String toDebugString() {
		return null;
	}

	@Override
	public  String getNodeName() {
		return "";
	}

	@Override
	public String toString() {
		return getNodeName();
	}


}