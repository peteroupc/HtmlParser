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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.upokecenter.util.StringUtility;

class Element extends Node implements IElement {
	private static final class AttributeNameComparator implements
	Comparator<IAttr> {
		@Override
		public int compare(IAttr arg0, IAttr arg1) {
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
		ret.attributes=new ArrayList<Attr>();
		for(Attr attribute : token.getAttributes()){
			ret.attributes.add(new Attr(attribute));
		}
		ret.namespace=namespace;
		return ret;
	}

	private String name;

	private String namespace;

	private String prefix=null;

	private List<Attr> attributes;

	 Element() {
		super(NodeType.ELEMENT_NODE);
		attributes=new ArrayList<Attr>();
	}

	public Element(String name) {
		super(NodeType.ELEMENT_NODE);
		attributes=new ArrayList<Attr>();
		this.name=name;
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
	public String getAttribute(String name) {
		for(IAttr attr : getAttributes()){
			if(attr.getName().equals(name))
				return attr.getValue();
		}
		return null;
	}

	@Override
	public String getAttributeNS(String namespace, String localName) {
		for(IAttr attr : getAttributes()){
			if((localName==null ? attr.getLocalName()==null : localName.equals(attr.getLocalName())) &&
					(namespace==null ? attr.getNamespaceURI()==null : namespace.equals(attr.getNamespaceURI())))
				return attr.getValue();
		}
		return null;
	}

	@Override
	public List<IAttr> getAttributes() {
		return new ArrayList<IAttr>(attributes);
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
		if(((Document) getOwnerDocument()).isHtmlDocument()){
			String lowerTagName=StringUtility.toLowerCaseAscii(tagName);
			for(INode node : getChildNodes()){
				collectElementsHtml(node,tagName,lowerTagName,ret);
			}
		} else {
			for(INode node : getChildNodes()){
				collectElements(node,tagName,ret);
			}
		}
		return ret;
	}

	@Override
	public String getId(){
		return getAttribute("id");
	}

	@Override
	public String getLocalName() {
		return name;
	}

	@Override
	public String getNamespaceURI() {
		return namespace;
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
		for(IAttr attr : token.getAttributes()){
			String s=getAttribute(attr.getName());
			if(s==null){
				setAttribute(attr.getName(),attr.getValue());
			}
		}
	}

	 void addAttribute(Attr value) {
		attributes.add(value);
	}


	 void setAttribute(String string, String value) {
		for(IAttr attr : getAttributes()){
			if(attr.getName().equals(string)){
				((Attr)attr).setValue(value);
			}
		}
		attributes.add(new Attr(string,value));
	}

	 void setLocalName(String name) {
		this.name = name;
	}

	 void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public void setPrefix(String prefix){
		this.prefix=prefix;
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
		ArrayList<IAttr> attribs=new ArrayList<IAttr>(getAttributes());
		Collections.sort(attribs,new AttributeNameComparator());
		for(IAttr attribute : attribs){
			//DebugUtility.log("%s %s",attribute.getNamespace(),attribute.getLocalName());
			if(attribute.getNamespaceURI()!=null){
				String extra1="";
				if(HtmlParser.XLINK_NAMESPACE.equals(attribute.getNamespaceURI())) {
					extra1="xlink ";
				}
				if(HtmlParser.XML_NAMESPACE.equals(attribute.getNamespaceURI())) {
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
			int len=strarray.length;
			if(len>0 && strarray[len-1].length()==0)
			{
				len--; // ignore trailing empty string
			}
			for(int i=0;i<len;i++){
				String el=strarray[i];
				builder.append("  ");
				builder.append(el);
				builder.append("\n");
			}
		}
		return builder.toString();
	}


	@Override
	public  String getNodeName(){
		return getTagName();
	}

	@Override
	public String getPrefix() {
		return prefix;
	}

	@Override public  String getLanguage(){
		INode parent=getParentNode();
		String a=getAttributeNS(HtmlParser.XML_NAMESPACE,"lang");
		if(a==null) {
			a=getAttribute("lang");
		}
		if(a!=null)return a;
		if(parent==null){
			parent=getOwnerDocument();
			if(parent==null)return "";
			return parent.getLanguage();
		} else
			return parent.getLanguage();
	}

	@Override
	public  String getInnerHTML() {
		return getInnerHtmlInternal();
	}

}