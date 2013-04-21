package com.upokecenter.html.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.upokecenter.html.IAttr;
import com.upokecenter.html.IComment;
import com.upokecenter.html.IDocument;
import com.upokecenter.html.IElement;
import com.upokecenter.html.INode;
import com.upokecenter.html.IProcessingInstruction;
import com.upokecenter.html.IText;
import com.upokecenter.html.NodeType;
import com.upokecenter.util.StringUtility;
import com.upokecenter.util.URIUtility;

/**
 * 
 * Implements Exclusive XML Canonicalization
 * as specified at:
 * http://www.w3.org/TR/xml-exc-c14n/
 * 
 * @author Peter
 *
 */
public final class ExclusiveCanonicalXML {
	private ExclusiveCanonicalXML(){}
	private static final class NamespaceAttrComparer implements Comparator<IAttr> {
		@Override
		public int compare(IAttr arg0, IAttr arg1) {
			return StringUtility.codePointCompare(arg0.getName(),arg1.getName());
		}
	}

	private static final class AttrComparer implements Comparator<IAttr> {
		@Override
		public int compare(IAttr arg0, IAttr arg1) {
			String namespace1=StringUtility.isNullOrEmpty(arg0.getPrefix()) ?
					"" : arg0.getNamespaceURI();
			String namespace2=StringUtility.isNullOrEmpty(arg1.getPrefix()) ?
					"" : arg1.getNamespaceURI();
			// compare namespace URIs (attributes without a prefix
			// are considered to have no namespace URI)
			int cmp=StringUtility.codePointCompare(namespace1,namespace2);
			if(cmp==0){
				// then compare their local names
				cmp=StringUtility.codePointCompare(arg0.getLocalName(),
						arg1.getLocalName());
			}
			return cmp;
		}
	}

	private static final Comparator<IAttr> attrComparer=new AttrComparer();
	private static final Comparator<IAttr> attrNamespaceComparer=new NamespaceAttrComparer();

	public static String canonicalize(
			INode node,
			boolean includeRoot,
			Map<String,String> prefixList
			){
		return canonicalize(node,includeRoot,prefixList,false);
	}
	private static void checkNamespacePrefix(String prefix, String nsvalue){
		if(prefix.equals("xmlns"))
			throw new IllegalArgumentException("'xmlns' namespace declared");
		if(prefix.equals("xml") && !"http://www.w3.org/XML/1998/namespace".equals(nsvalue))
			throw new IllegalArgumentException("'xml' bound to wrong namespace name");
		if(!"xml".equals(prefix) && "http://www.w3.org/XML/1998/namespace".equals(nsvalue))
			throw new IllegalArgumentException("'xml' bound to wrong namespace name");
		if("http://www.w3.org/2000/xmlns/".equals(nsvalue))
			throw new IllegalArgumentException("'prefix' bound to xmlns namespace name");
		if(!StringUtility.isNullOrEmpty(nsvalue)){
			if(!URIUtility.hasSchemeForURI(nsvalue))
				throw new IllegalArgumentException(nsvalue+" is not a valid namespace URI.");
		} else if(!"".equals(prefix))
			throw new IllegalArgumentException("can't undeclare a prefix");
	}
	public static String canonicalize(
			INode node,
			boolean includeRoot,
			Map<String,String> prefixList,
			boolean withComments
			){
		if((node)==null)throw new NullPointerException("node");
		StringBuilder builder=new StringBuilder();
		List<Map<String,String>> stack=new ArrayList<Map<String,String>>();
		if(prefixList==null) {
			prefixList=new HashMap<String,String>();
		} else {
			for(String prefix : prefixList.keySet()){
				String nsvalue=prefixList.get(prefix);
				checkNamespacePrefix(prefix,nsvalue);
			}
		}
		HashMap<String,String> item=new HashMap<String,String>();
		stack.add(item);
		if(node instanceof IDocument){
			boolean beforeElement=true;
			for(INode child : node.getChildNodes()){
				if(child instanceof IElement){
					beforeElement=false;
					canonicalize(child,builder,stack,prefixList,true,withComments);
				} else if(withComments || child.getNodeType()!=NodeType.COMMENT_NODE){
					canonicalizeOutsideElement(child,builder,beforeElement);
				}
			}
		} else if(includeRoot){
			canonicalize(node,builder,stack,prefixList,true,withComments);
		} else {
			for(INode child : node.getChildNodes()){
				canonicalize(child,builder,stack,prefixList,true,withComments);
			}
		}
		return builder.toString();
	}

	private static boolean isVisiblyUtilized(IElement element, String s){
		String prefix=element.getPrefix();
		if(prefix==null) {
			prefix="";
		}
		if(s.equals(prefix))return true;
		if(s.length()>0){
			for(IAttr attr : element.getAttributes()){
				prefix=attr.getPrefix();
				if(prefix==null) {
					continue;
				}
				if(s.equals(prefix))return true;
			}
		}
		return false;
	}

	private static void renderAttribute(StringBuilder builder,
			String prefix, String name, String value){
		builder.append(' ');
		if(!StringUtility.isNullOrEmpty(prefix)){
			builder.append(prefix);
			builder.append(":");
		}
		builder.append(name);
		builder.append("=\"");
		for(int i=0;i<value.length();i++){
			char c=value.charAt(i);
			if(c==0x0d) {
				builder.append("&#xD;");
			} else if(c==0x09) {
				builder.append("&#x9;");
			} else if(c==0x0a) {
				builder.append("&#xA;");
			} else if(c=='"') {
				builder.append("&quot;");
			} else if(c=='<') {
				builder.append("&lt;");
			} else if(c=='&') {
				builder.append("&amp;");
			} else {
				builder.append(c);
			}
		}
		builder.append('"');
	}

	private static void canonicalizeOutsideElement(
			INode node, StringBuilder builder, boolean beforeDocument){
		int nodeType=node.getNodeType();
		if(nodeType==NodeType.COMMENT_NODE){
			if(!beforeDocument) {
				builder.append('\n');
			}
			builder.append("<!--");
			builder.append(((IComment)node).getData());
			builder.append("-->");
			if(beforeDocument) {
				builder.append('\n');
			}
		} else if(nodeType==NodeType.PROCESSING_INSTRUCTION_NODE){
			if(!beforeDocument) {
				builder.append('\n');
			}
			builder.append("<?");
			builder.append(((IProcessingInstruction)node).getTarget());
			String data=((IProcessingInstruction)node).getData();
			if(data.length()>0){
				builder.append(' ');
				builder.append(data);
			}
			builder.append("?>");
			if(beforeDocument) {
				builder.append('\n');
			}
		}
	}

	private static final class NamespaceAttr implements IAttr {
		String prefix;
		String localName;
		String value;
		String name;
		public NamespaceAttr(String prefix, String value){
			if(prefix.length()==0){
				this.prefix="";
				this.localName="xmlns";
				this.value=value;
				this.name="xmlns";
			} else {
				this.prefix="xmlns";
				this.localName=prefix;
				this.name="xmlns:"+value;
				this.value=value;
			}
		}
		@Override
		public String getPrefix() {
			return prefix;
		}
		@Override
		public String getLocalName() {
			return localName;
		}
		@Override
		public String getName() {
			return name;
		}
		@Override
		public String getNamespaceURI() {
			return "http://www.w3.org/2000/xmlns/";
		}
		@Override
		public String getValue() {
			return value;
		}
	}

	private static void canonicalize(
			INode node,
			StringBuilder builder,
			List<Map<String,String>> namespaceStack,
			Map<String,String> prefixList,
			boolean addPrefixes,
			boolean withComments
			){
		int nodeType=node.getNodeType();
		if(nodeType==NodeType.COMMENT_NODE){
			if(withComments){
				builder.append("<!--");
				builder.append(((IComment)node).getData());
				builder.append("-->");
			}
		} else if(nodeType==NodeType.PROCESSING_INSTRUCTION_NODE){
			builder.append("<?");
			builder.append(((IProcessingInstruction)node).getTarget());
			String data=((IProcessingInstruction)node).getData();
			if(data.length()>0){
				builder.append(' ');
				builder.append(data);
			}
			builder.append("?>");
		} else if(nodeType==NodeType.ELEMENT_NODE){
			IElement e=((IElement)node);
			Map<String,String> nsRendered=namespaceStack.get(namespaceStack.size()-1);
			boolean copied=false;
			builder.append('<');
			if(e.getPrefix()!=null && e.getPrefix().length()>0){
				builder.append(e.getPrefix());
				builder.append(':');
			}
			builder.append(e.getLocalName());
			ArrayList<IAttr> attrs=new ArrayList<IAttr>();
			Set<String> declaredNames=null;
			if(addPrefixes && prefixList.size()>0){
				declaredNames=new HashSet<String>();
			}
			for(IAttr attr : e.getAttributes()){
				String name=attr.getName();
				String nsvalue=null;
				if("xmlns".equals(name)){
					attrs.add(attr); // add default namespace
					if(declaredNames!=null) {
						declaredNames.add("");
					}
					nsvalue=attr.getValue();
					checkNamespacePrefix("",nsvalue);
				} else if(name.startsWith("xmlns:") && name.length()>6){
					attrs.add(attr); // add prefix namespace
					if(declaredNames!=null) {
						declaredNames.add(attr.getLocalName());
					}
					nsvalue=attr.getValue();
					checkNamespacePrefix(attr.getLocalName(),nsvalue);
				}
			}
			if(declaredNames!=null){
				// add declared prefixes to list
				for(String prefix : prefixList.keySet()){
					if(prefix==null || declaredNames.contains(prefix)) {
						continue;
					}
					String value=prefixList.get(prefix);
					if(value==null) {
						value="";
					}
					attrs.add(new NamespaceAttr(prefix,value));
				}
			}
			Collections.sort(attrs,attrNamespaceComparer);
			for(IAttr attr : attrs){
				String prefix=attr.getLocalName();
				if(attr.getPrefix().length()==0){
					prefix="";
				}
				String value=attr.getValue();
				boolean isEmpty=StringUtility.isNullOrEmpty(prefix);
				boolean isEmptyDefault=(isEmpty && StringUtility.isNullOrEmpty(value));
				boolean renderNamespace=false;
				if(isEmptyDefault){
					///
					// condition used for Canonical XML
					//renderNamespace=(
					//		(e.getParentNode() instanceof IElement) &&
					//		!StringUtility.isNullOrEmpty(((IElement)e.getParentNode()).getAttribute("xmlns"))
					//		);
					///
					// changed condition for Exclusive XML Canonicalization
					renderNamespace=(isVisiblyUtilized(e,"") ||
							prefixList.containsKey("")) &&
							nsRendered.containsKey("");
				} else {
					String renderedValue=nsRendered.get(prefix);
					renderNamespace=(renderedValue==null ||
							!renderedValue.equals(value));
					// added condition for Exclusive XML Canonicalization
					renderNamespace=renderNamespace && (isVisiblyUtilized(e,prefix) ||
							prefixList.containsKey(prefix));
				}
				if(renderNamespace){
					renderAttribute(builder,
							(isEmpty ? null : "xmlns"),
							(isEmpty ? "xmlns" : prefix),
							value);
					if(!copied){
						copied=true;
						nsRendered=new HashMap<String,String>(nsRendered);
					}
					nsRendered.put(prefix, value);
				}
			}
			namespaceStack.add(nsRendered);
			attrs.clear();
			// All other attributes
			for(IAttr attr : e.getAttributes()){
				String name=attr.getName();
				if(!("xmlns".equals(name) ||
						(name.startsWith("xmlns:") && name.length()>6))){
					// non-namespace node
					attrs.add(attr);
				}
			}
			Collections.sort(attrs,attrComparer);
			for(IAttr attr : attrs){
				renderAttribute(builder,
						attr.getPrefix(),attr.getLocalName(),attr.getValue());
			}
			builder.append('>');
			for(INode child : node.getChildNodes()){
				canonicalize(child,builder,namespaceStack,prefixList,false,withComments);
			}
			namespaceStack.remove(namespaceStack.size()-1);
			builder.append("</");
			if(e.getPrefix()!=null && e.getPrefix().length()>0){
				builder.append(e.getPrefix());
				builder.append(':');
			}
			builder.append(e.getLocalName());
			builder.append('>');
		} else if(nodeType==NodeType.TEXT_NODE){
			String comment=((IText)node).getData();
			for(int i=0;i<comment.length();i++){
				char c=comment.charAt(i);
				if(c==0x0d) {
					builder.append("&#xD;");
				} else if(c=='>') {
					builder.append("&gt;");
				} else if(c=='<') {
					builder.append("&lt;");
				} else if(c=='&') {
					builder.append("&amp;");
				} else {
					builder.append(c);
				}
			}
		}
	}

}
