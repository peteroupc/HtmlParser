package com.upokecenter.html;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.upokecenter.util.StringUtility;

class Element extends Node implements IElement {
	private static final class AttributeNameComparator implements
	Comparator<HtmlParser.Attrib> {
		@Override
		public int compare(HtmlParser.Attrib arg0, HtmlParser.Attrib arg1) {
			String a=arg0.getName();
			String b=arg1.getName();
			return a.compareTo(b);
		}
	}


	 static Element fromToken(HtmlParser.StartTagToken token){
		return fromToken(token,HtmlParser.HTML_NAMESPACE);
	}

	 static Element fromToken(
			HtmlParser.StartTagToken token, String namespace){
		Element ret=new Element();
		ret.name=token.getName();
		ret.attributes=new ArrayList<HtmlParser.Attrib>();
		for(HtmlParser.Attrib attribute : token.getAttributes()){
			ret.attributes.add(new HtmlParser.Attrib(attribute));
		}
		ret.namespace=namespace;
		return ret;
	}

	private String name;

	private String namespace;

	private String prefix=null;

	private List<HtmlParser.Attrib> attributes;

	 Element() {
		super(NodeType.ELEMENT_NODE);
		attributes=new ArrayList<HtmlParser.Attrib>();
	}

	public Element(String name) {
		super(NodeType.ELEMENT_NODE);
		attributes=new ArrayList<HtmlParser.Attrib>();
		this.name=name;
	}

	@Override
	public String getAttribute(String name) {
		for(HtmlParser.Attrib attr : getAttributes()){
			if(attr.getName().equals(name))
				return attr.getValue();
		}
		return null;
	}

	public void setPrefix(String prefix){
		this.prefix=prefix;
	}

	@Override
	public String getAttributeNS(String namespace, String localName) {
		for(HtmlParser.Attrib attr : getAttributes()){
			if(attr.isAttribute(localName,namespace))
				return attr.getValue();
		}
		return null;
	}
	public List<HtmlParser.Attrib> getAttributes() {
		return attributes;
	}

	@Override
	public String getLocalName() {
		return name;
	}

	@Override
	public String getNamespaceURI() {
		return namespace;
	}

	 boolean isHtmlElement(String name){
		return name.equals(this.name) && HtmlParser.HTML_NAMESPACE.equals(namespace);
	}

	 boolean isMathMLElement(String name){
		return name.equals(this.name) && HtmlParser.MATHML_NAMESPACE.equals(namespace);
	}

	 boolean isSvgElement(String name){
		return name.equals(this.name) && HtmlParser.SVG_NAMESPACE.equals(namespace);
	}
	 void mergeAttributes(HtmlParser.StartTagToken token){
		for(HtmlParser.Attrib attr : token.getAttributes()){
			String s=getAttribute(attr.getName());
			if(s==null){
				setAttribute(attr.getName(),attr.getValue());
			}
		}
	}

	public void setAttribute(String string, String value) {
		for(HtmlParser.Attrib attr : getAttributes()){
			if(attr.getName().equals(string)){
				attr.setValue(value);
			}
		}
		attributes.add(new HtmlParser.Attrib(string,value));
	}
	 void setName(String name) {
		this.name = name;
	}

	 void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	@Override
	 String toDebugString(){
		StringBuilder builder=new StringBuilder();
		String extra="";
		if(HtmlParser.MATHML_NAMESPACE.equals(namespace)) {
			extra="math ";
		}
		if(HtmlParser.SVG_NAMESPACE.equals(namespace)) {
			extra="svg ";
		}
		builder.append("<"+extra+name.toString()+">\n");
		ArrayList<HtmlParser.Attrib> attribs=new ArrayList<HtmlParser.Attrib>(getAttributes());
		Collections.sort(attribs,new AttributeNameComparator());
		for(HtmlParser.Attrib attribute : attribs){
			//DebugUtility.log("%s %s",attribute.getNamespace(),attribute.getLocalName());
			if(attribute.getNamespace()!=null){
				String extra1="";
				if(HtmlParser.XLINK_NAMESPACE.equals(attribute.getNamespace())) {
					extra1="xlink ";
				}
				if(HtmlParser.XML_NAMESPACE.equals(attribute.getNamespace())) {
					extra1="xml ";
				}
				extra1+=attribute.getLocalName();
				builder.append("  "+extra1+"=\""+attribute.getValue().toString().replace("\n","~~~~")+"\"\n");
			} else {
				builder.append("  "+attribute.getName().toString()+"=\""+attribute.getValue().toString().replace("\n","~~~~")+"\"\n");
			}
		}
		for(Node node : getChildNodesInternal()){
			String str=node.toDebugString();
			if(str==null) {
				continue;
			}
			String[] strarray=StringUtility.splitAt(str,"\n");
			for(String el : strarray){
				builder.append("  ");
				builder.append(el);
				builder.append("\n");
			}
		}
		return builder.toString();
	}

	@Override
	public String toString(){
		StringBuilder builder=new StringBuilder();
		builder.append("Element: "+name.toString()+", "+namespace.toString()+"\n");
		for(HtmlParser.Attrib attribute : getAttributes()){
			builder.append("Attribute: "+attribute.getName().toString()+"="+
					attribute.getValue().toString()+"\n");
		}
		for(Node node : getChildNodesInternal()){
			String str=node.toString();
			String[] strarray=StringUtility.splitAt(str,"\n");
			for(String el : strarray){
				builder.append("  ");
				builder.append(el);
				builder.append("\n");
			}
		}
		return builder.toString();
	}

	@Override
	public String getTagName() {
		String tagName=name;
		if(prefix!=null){
			tagName=prefix+":"+name;
		}
		if((getOwnerDocument() instanceof Document) &&
				HtmlParser.HTML_NAMESPACE.equals(namespace))
			return StringUtility.toUpperCaseAscii(tagName);
		return tagName;
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
		if(((Document) getOwnerDocument()).isHtmlDocument()){
			collectElementsHtml(this,tagName,
					StringUtility.toLowerCaseAscii(tagName),ret);
		} else {
			collectElements(this,tagName,ret);
		}
		return ret;
	}


	@Override
	public  String getTextContent(){
		StringBuilder builder=new StringBuilder();
		for(INode node : getChildNodes()){
			if(node.getNodeType()!=NodeType.COMMENT_NODE){
				builder.append(node.getTextContent());
			}
		}
		return builder.toString();
	}
}