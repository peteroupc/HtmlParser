package com.upokecenter.html;

import java.util.ArrayList;
import java.util.List;

class Node implements INode {
	List<Node> childNodes;
	Node parentNode=null;
	IDocument ownerDocument=null;

	@Override
	public IDocument getOwnerDocument(){
		return ownerDocument;
	}

	void setOwnerDocument(IDocument document){
		ownerDocument=document;
	}

	int nodeType;
	public Node(int nodeType){
		this.nodeType=nodeType;
		childNodes=new ArrayList<Node>();
	}
	String toDebugString() {
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

	public void appendChild(Node node){
		if(node==this)
			throw new IllegalArgumentException();
		node.parentNode=this;
		node.ownerDocument=(this instanceof IDocument) ? (IDocument)this : ownerDocument;
		childNodes.add(node);
	}

	List<Node> getChildNodesInternal(){
		return childNodes;
	}

	@Override
	public int getNodeType(){
		return nodeType;
	}
	@Override
	public Node getParentNode() {
		return parentNode;
	}
	public void removeChild(Node node){
		node.parentNode=null;
		childNodes.remove(node);
	}
	@Override
	public List<INode> getChildNodes() {
		return new ArrayList<INode>(childNodes);
	}

	void setData(String string) {
		// TODO Auto-generated method stub

	}

	@Override
	public String getBaseURI() {
		IDocument doc=getOwnerDocument();
		if(doc==null)return "";
		return doc.getBaseURI();
	}

	public String getTextContent(){
		return null;
	}
}