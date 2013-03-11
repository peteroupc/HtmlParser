package com.upokecenter.html;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import com.upokecenter.html.HtmlParser.Attribute;
import com.upokecenter.html.HtmlParser.StartTagToken;
import com.upokecenter.util.StringUtility;

class Element extends Node implements IElement {
	static Element fromToken(StartTagToken token){
		return fromToken(token,HtmlParser.HTML_NAMESPACE);
	}

	static Element fromToken(
			StartTagToken token, String namespace){
		Element ret=new Element();
		ret.name=token.getName();
		ret.attributes=new ArrayList<Attribute>();
		for(Attribute attribute : token.getAttributes()){
			ret.attributes.add(new Attribute(attribute));
		}
		ret.namespace=namespace;
		return ret;
	}

	String name, namespace, prefix=null;

	List<Attribute> attributes;

	Element() {
		super(NodeType.ELEMENT_NODE);
		attributes=new ArrayList<Attribute>();
	}

	@Override
	public String getAttribute(String name) {
		for(Attribute attr : getAttributes()){
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
		for(Attribute attr : getAttributes()){
			if(attr.isAttribute(localName,namespace))
				return attr.getValue();
		}
		return null;
	}
	public List<Attribute> getAttributes() {
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
	void mergeAttributes(StartTagToken token){
		for(Attribute attr : token.getAttributes()){
			String s=getAttribute(attr.getName());
			if(s==null){
				setAttribute(attr.getName(),attr.getValue());
			}
		}
	}

	public void setAttribute(String string, String value) {
		for(Attribute attr : getAttributes()){
			if(attr.getName().equals(string)){
				attr.setValue(value);
			}
		}
		attributes.add(new Attribute(string,value));
	}
	void setName(String name) {
		this.name = name;
	}

	void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	@Override
	public String toDebugString(){
		StringBuilder builder=new StringBuilder();
		String extra="";
		if(HtmlParser.MATHML_NAMESPACE.equals(namespace)) {
			extra="math ";
		}
		if(HtmlParser.SVG_NAMESPACE.equals(namespace)) {
			extra="svg ";
		}
		builder.append(String.format(Locale.US,"<%s%s>\n",extra,name.toString()));
		List<Attribute> attribs=new ArrayList<Attribute>(getAttributes());
		Collections.sort(attribs,new Comparator<Attribute>(){

			@Override
			public int compare(Attribute arg0, Attribute arg1) {
				return arg0.getName().compareTo(arg1.getName());
			}

		});
		for(Attribute attribute : attribs){
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
				builder.append(String.format(Locale.US,"  %s=\"%s\"\n",
						extra1,attribute.getValue().toString()));
			} else {
				builder.append(String.format(Locale.US,"  %s=\"%s\"\n",
						attribute.getName().toString(),attribute.getValue().toString()));
			}
		}
		for(Node node : childNodes){
			String str=node.toDebugString();
			if(str==null) {
				continue;
			}
			String[] strarray=str.split("\n");
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
		builder.append(String.format(Locale.US,"Element: %s, %s\n",name.toString(),
				namespace.toString()));
		for(Attribute attribute : getAttributes()){
			builder.append(String.format(Locale.US,"Attribute: %s=%s\n",
					attribute.getName().toString(),attribute.getValue().toString()));
		}
		for(Node node : childNodes){
			String str=node.toString();
			String[] strarray=str.split("\n");
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
	

	public String getTextContent(){
		StringBuilder builder=new StringBuilder();
		for(INode node : getChildNodes()){
			if(node.getNodeType()!=NodeType.COMMENT_NODE){
				builder.append(node.getTextContent());
			}
		}
		return builder.toString();
	}
}