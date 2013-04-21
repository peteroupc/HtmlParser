package com.upokecenter.html.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.upokecenter.html.IAttr;
import com.upokecenter.html.IDocument;
import com.upokecenter.html.IElement;
import com.upokecenter.html.INode;
import com.upokecenter.html.IText;
import com.upokecenter.html.NodeType;
import com.upokecenter.rdf.IRDFParser;
import com.upokecenter.rdf.RDFTerm;
import com.upokecenter.rdf.RDFTriple;
import com.upokecenter.util.StringUtility;
import com.upokecenter.util.URIUtility;

class RDFa1 implements IRDFParser {


	private RDFa.EvalContext context;
	private final Set<RDFTriple> outputGraph;
	private final IDocument document;
	private boolean xhtml=false;

	public RDFa1(IDocument document){
		this.document=document;
		this.context=new RDFa.EvalContext();
		this.context.baseURI=document.getBaseURI();
		this.context.namespaces=new HashMap<String,String>();
		if(!URIUtility.hasScheme(this.context.baseURI))
			throw new IllegalArgumentException("baseURI: "+this.context.baseURI);
		this.context.parentSubject=RDFTerm.fromIRI(this.context.baseURI);
		this.context.parentObject=null;
		this.context.iriMap=new HashMap<String,String>();
		this.context.listMap=new HashMap<String,List<RDFTerm>>();
		this.context.incompleteTriples=new ArrayList<RDFa.IncompleteTriple>();
		this.context.language=null;
		this.outputGraph=new HashSet<RDFTriple>();
		if(isHtmlElement(document.getDocumentElement(),"html")){
			xhtml=true;
		}
	}

	private static boolean isHtmlElement(IElement element, String name){
		return element!=null &&
				"http://www.w3.org/1999/xhtml".equals(element.getNamespaceURI()) &&
				name.equals(element.getLocalName());
	}

	private static final String RDF_XMLLITERAL="http://www.w3.org/1999/02/22-rdf-syntax-ns#XMLLiteral";
	private static final String RDF_NAMESPACE="http://www.w3.org/1999/02/22-rdf-syntax-ns#";

	private static String getTextNodeText(INode node){
		StringBuilder builder=new StringBuilder();
		for(INode child : node.getChildNodes()){
			if(child.getNodeType()==NodeType.TEXT_NODE){
				builder.append(((IText)child).getData());
			} else {
				builder.append(getTextNodeText(child));
			}
		}
		return builder.toString();
	}

	private void miniRdfXmlChild(IElement node, RDFTerm subject, String language){
		String nsname=node.getNamespaceURI();
		if(node.getAttribute("xml:lang")!=null){
			language=node.getAttribute("xml:lang");
		}
		String localname=node.getLocalName();
		RDFTerm predicate=relativeResolve(nsname+localname);
		if(!hasNonTextChildNodes(node)){
			String content=getTextNodeText(node);
			RDFTerm literal;
			if(!StringUtility.isNullOrEmpty(language)){
				literal=RDFTerm.fromLangString(content, language);
			} else {
				literal=RDFTerm.fromTypedString(content);
			}
			outputGraph.add(new RDFTriple(subject,predicate,literal));
		} else {
			String parseType=node.getAttributeNS(RDF_NAMESPACE, "parseType");
			if("Literal".equals(parseType))
				throw new UnsupportedOperationException();
			RDFTerm blank=generateBlankNode();
			context.language=language;
			miniRdfXml(node,context,blank);
			outputGraph.add(new RDFTriple(subject,predicate,blank));
		}
	}

	private void miniRdfXml(IElement node, RDFa.EvalContext context){
		miniRdfXml(node,context,null);
	}

	// Processes a subset of RDF/XML metadata
	// Doesn't implement RDF/XML completely
	private void miniRdfXml(IElement node, RDFa.EvalContext context, RDFTerm subject){
		String language=context.language;
		for(INode child : node.getChildNodes()){
			IElement childElement=(child instanceof IElement) ?
					((IElement)child) : null;
					if(childElement==null) {
						continue;
					}
					if(node.getAttribute("xml:lang")!=null){
						language=node.getAttribute("xml:lang");
					} else {
						language=context.language;
					}
					if(childElement.getLocalName().equals("Description") &&
							RDF_NAMESPACE.equals(childElement.getNamespaceURI())){
						RDFTerm about=relativeResolve(childElement.getAttributeNS(RDF_NAMESPACE,"about"));
						//DebugUtility.log("about=%s [%s]",about,childElement.getAttribute("about"));
						if(about==null){
							about=subject;
							if(about==null) {
								continue;
							}
						}
						for(INode child2 : child.getChildNodes()){
							IElement childElement2=
									((child2 instanceof IElement) ?
											((IElement)child2) : null);
							if(childElement2==null) {
								continue;
							}
							miniRdfXmlChild(childElement2,about,language);
						}
					} else if(RDF_NAMESPACE.equals(childElement.getNamespaceURI()))
						throw new UnsupportedOperationException();
		}
	}

	private static boolean isNCNameStartChar(int c){
		return (c>='a' && c<='z') ||
				(c>='A' && c<='Z') ||
				c=='_' ||
				(c>=0xc0 && c<=0xd6) ||
				(c>=0xd8 && c<=0xf6) ||
				(c>=0xf8 && c<=0x2ff) ||
				(c>=0x370 && c<=0x37d) ||
				(c>=0x37f && c<=0x1fff) ||
				(c>=0x200c && c<=0x200d) ||
				(c>=0x2070 && c<=0x218f) ||
				(c>=0x2c00 && c<=0x2fef) ||
				(c>=0x3001 && c<=0xd7ff) ||
				(c>=0xf900 && c<=0xfdcf) ||
				(c>=0xfdf0 && c<=0xfffd) ||
				(c>=0x10000 && c<=0xeffff);
	}
	private static boolean isNCNameChar(int c){
		return (c>='a' && c<='z') ||
				(c>='A' && c<='Z') ||
				c=='_' || c=='.' || c=='-' ||
				(c>='0' && c<='9') ||
				c==0xb7 ||
				(c>=0xc0 && c<=0xd6) ||
				(c>=0xd8 && c<=0xf6) ||
				(c>=0xf8 && c<=0x2ff) ||
				(c>=0x300 && c<=0x37d) ||
				(c>=0x37f && c<=0x1fff) ||
				(c>=0x200c && c<=0x200d) ||
				(c>=0x203f && c<=0x2040) ||
				(c>=0x2070 && c<=0x218f) ||
				(c>=0x2c00 && c<=0x2fef) ||
				(c>=0x3001 && c<=0xd7ff) ||
				(c>=0xf900 && c<=0xfdcf) ||
				(c>=0xfdf0 && c<=0xfffd) ||
				(c>=0x10000 && c<=0xeffff);
	}
	private static int getCuriePrefixLength(String s, int offset, int length){
		if(s==null || length==0)return -1;
		if(s.charAt(offset)==':')return 0;
		if(!isNCNameStartChar(s.charAt(offset)))return -1;
		int index=offset+1;
		int sLength=offset+length;
		while(index<sLength){
			// Get the next Unicode character
			int c=s.charAt(index);
			if(c>=0xD800 && c<=0xDBFF && index+1<sLength &&
					s.charAt(index+1)>=0xDC00 && s.charAt(index+1)<=0xDFFF){
				// Get the Unicode code point for the surrogate pair
				c=0x10000+(c-0xD800)*0x400+(s.charAt(index+1)-0xDC00);
				index++;
			} else if(c>=0xD800 && c<=0xDFFF)
				// error
				return -1;
			if(c==':')return index-offset;
			else if(!isNCNameChar(c))return -1;
			index++;
		}
		return -1;
	}

	private static boolean hasNonTextChildNodes(INode node){
		for(INode child : node.getChildNodes()){
			if(child.getNodeType()!=NodeType.TEXT_NODE)
				return true;
		}
		return false;
	}

	private static boolean isValidCurieReference(String s, int offset, int length){
		if(s==null)return false;
		if(length==0)return true;
		int[] indexes=URIUtility.splitIRI(s,offset,length,URIUtility.ParseMode.IRIStrict);
		if(indexes==null)
			return false;
		if(indexes[0]!=-1) // check if scheme component is present
			return false;
		return true;
	}

	private int blankNode;
	private final Map<String,RDFTerm> bnodeLabels=new HashMap<String,RDFTerm>();

	private RDFTerm getNamedBlankNode(String str){
		RDFTerm term=RDFTerm.fromBlankNode(str);
		bnodeLabels.put(str,term);
		return term;
	}

	private RDFTerm generateBlankNode(){
		// Use "b:" as the prefix; according to the CURIE syntax,
		// "b:" can never begin a valid CURIE reference (in RDFa 1.0,
		// the reference has the broader production irelative-ref),
		// so it can
		// be used to guarantee that generated blank nodes will never
		// conflict with those stated explicitly
		String blankNodeString="b:"+Integer.toString(blankNode);
		blankNode++;
		RDFTerm term=RDFTerm.fromBlankNode(blankNodeString);
		bnodeLabels.put(blankNodeString,term);
		return term;
	}

	@Override
	public Set<RDFTriple> parse() throws IOException {
		process(document.getDocumentElement(),true);
		RDFInternal.replaceBlankNodes(outputGraph, bnodeLabels);
		return outputGraph;
	}

	private RDFTerm relativeResolve(String iri){
		if(iri==null)return null;
		if(URIUtility.splitIRI(iri)==null)
			return null;
		return RDFTerm.fromIRI(URIUtility.relativeResolve(iri, context.baseURI));
	}


	private void process(IElement node, boolean root){
		List<RDFa.IncompleteTriple> incompleteTriplesLocal=new ArrayList<RDFa.IncompleteTriple>();
		String localLanguage=context.language;
		RDFTerm newSubject=null;
		boolean recurse=true;
		boolean skipElement=false;
		RDFTerm currentObject=null;
		Map<String,String> namespacesLocal=
				new HashMap<String,String>(context.namespaces);
		Map<String,String> iriMapLocal=
				new HashMap<String,String>(context.iriMap);
		String attr=null;
		if(!xhtml){
			attr=node.getAttribute("xml:base");
			if(attr!=null){
				context.baseURI=URIUtility.relativeResolve(attr, context.baseURI);
			}
		}
		// Support XML namespaces
		for(IAttr attrib : node.getAttributes()){
			String name=StringUtility.toLowerCaseAscii(attrib.getName());
			//DebugUtility.log(attrib);
			if(name.equals("xmlns")){
				//DebugUtility.log("xmlns %s",attrib.getValue());
				iriMapLocal.put("", attrib.getValue());
				namespacesLocal.put("", attrib.getValue());
			} else if(name.startsWith("xmlns:") && name.length()>6){
				String prefix=name.substring(6);
				//DebugUtility.log("xmlns %s %s",prefix,attrib.getValue());
				if(!"_".equals(prefix)){
					iriMapLocal.put(prefix, attrib.getValue());
				}
				namespacesLocal.put(prefix, attrib.getValue());
			}
		}
		attr=node.getAttribute("xml:lang");
		if(attr!=null){
			localLanguage=attr;
		}
		// Support RDF/XML metadata
		if(node.getLocalName().equals("RDF") &&
				RDF_NAMESPACE.equals(node.getNamespaceURI())){
			miniRdfXml(node,context);
			return;
		}
		String rel=node.getAttribute("rel");
		String rev=node.getAttribute("rev");
		String property=node.getAttribute("property");
		String content=node.getAttribute("content");
		String datatype=node.getAttribute("datatype");
		if(rel==null && rev==null){
			// Step 4
			RDFTerm resource=getSafeCurieOrCurieOrIri(
					node.getAttribute("about"),iriMapLocal);
			if(resource==null){
				resource=getSafeCurieOrCurieOrIri(
						node.getAttribute("resource"),iriMapLocal);
			}
			if(resource==null){
				resource=relativeResolve(node.getAttribute("href"));
			}
			if(resource==null){
				resource=relativeResolve(node.getAttribute("src"));
			}
			if((resource==null || resource.getKind()!=RDFTerm.IRI)){
				String rdfTypeof=getCurie(node.getAttribute("typeof"),iriMapLocal);
				if(isHtmlElement(node, "head") ||
						isHtmlElement(node, "body")){
					resource=getSafeCurieOrCurieOrIri("",iriMapLocal);
				}
				if(resource==null && !xhtml && root){
					resource=getSafeCurieOrCurieOrIri("",iriMapLocal);
				}
				if(resource==null && rdfTypeof!=null){
					resource=generateBlankNode();
				}
				if(resource==null){
					if(context.parentObject!=null) {
						resource=context.parentObject;
					}
					if(node.getAttribute("property")==null){
						skipElement=true;
					}
				}
				newSubject=resource;
			} else {
				newSubject=resource;
			}
		} else {
			// Step 5
			RDFTerm resource=getSafeCurieOrCurieOrIri(
					node.getAttribute("about"),iriMapLocal);
			if(resource==null){
				resource=relativeResolve(node.getAttribute("src"));
			}
			if((resource==null || resource.getKind()!=RDFTerm.IRI)){
				String rdfTypeof=getCurie(node.getAttribute("typeof"),iriMapLocal);
				if(isHtmlElement(node, "head") ||
						isHtmlElement(node, "body")){
					resource=getSafeCurieOrCurieOrIri("",iriMapLocal);
				}
				if(resource==null && !xhtml && root){
					resource=getSafeCurieOrCurieOrIri("",iriMapLocal);
				}
				if(resource==null && rdfTypeof!=null){
					resource=generateBlankNode();
				}
				if(resource==null){
					if(context.parentObject!=null) {
						resource=context.parentObject;
					}
				}
				newSubject=resource;
			} else {
				newSubject=resource;
			}
			resource=getSafeCurieOrCurieOrIri(
					node.getAttribute("resource"),iriMapLocal);
			if(resource==null){
				resource=relativeResolve(node.getAttribute("href"));
			}
			currentObject=resource;
		}
		// Step 6
		if(newSubject!=null){
			String[] types=StringUtility.splitAtNonFFSpaces(node.getAttribute("typeof"));
			for(String type : types){
				String iri=getCurie(type,iriMapLocal);
				if(iri!=null){
					outputGraph.add(new RDFTriple(
							newSubject,RDFTerm.A,
							RDFTerm.fromIRI(iri)
							));
				}
			}
		}
		// Step 7
		if(currentObject!=null){
			String[] types=StringUtility.splitAtNonFFSpaces(rel);
			for(String type : types){
				String iri=getRelTermOrCurie(type,
						iriMapLocal);
				assert newSubject!=null;
				if(iri!=null){
					outputGraph.add(new RDFTriple(
							newSubject,
							RDFTerm.fromIRI(iri),currentObject
							));
				}
			}
			types=StringUtility.splitAtNonFFSpaces(rev);
			for(String type : types){
				String iri=getRelTermOrCurie(type,
						iriMapLocal);
				if(iri!=null){
					outputGraph.add(new RDFTriple(
							currentObject,
							RDFTerm.fromIRI(iri),
							newSubject
							));
				}
			}
		} else {
			// Step 8
			String[] types=StringUtility.splitAtNonFFSpaces(rel);
			boolean hasPredicates=false;
			// Defines predicates
			for(String type : types){
				String iri=getRelTermOrCurie(type,
						iriMapLocal);
				if(iri!=null){
					if(!hasPredicates){
						hasPredicates=true;
						currentObject=generateBlankNode();
					}
					RDFa.IncompleteTriple inc=new RDFa.IncompleteTriple();
					inc.predicate=RDFTerm.fromIRI(iri);
					inc.direction=RDFa.ChainingDirection.Forward;
					incompleteTriplesLocal.add(inc);
				}
			}
			types=StringUtility.splitAtNonFFSpaces(rev);
			for(String type : types){
				String iri=getRelTermOrCurie(type,
						iriMapLocal);
				if(iri!=null){
					if(!hasPredicates){
						hasPredicates=true;
						currentObject=generateBlankNode();
					}
					RDFa.IncompleteTriple inc=new RDFa.IncompleteTriple();
					inc.predicate=RDFTerm.fromIRI(iri);
					inc.direction=RDFa.ChainingDirection.Reverse;
					incompleteTriplesLocal.add(inc);
				}
			}
		}
		// Step 9
		String[] preds=StringUtility.splitAtNonFFSpaces(property);
		String datatypeValue=getCurie(datatype,
				iriMapLocal);
		if(datatype!=null && datatypeValue==null) {
			datatypeValue="";
		}
		//DebugUtility.log("datatype=[%s] prop=%s vocab=%s",
		//	datatype,property,localDefaultVocab);
		//DebugUtility.log("datatypeValue=[%s]",datatypeValue);
		RDFTerm currentProperty=null;
		for(String pred : preds){
			String iri=getCurie(pred,
					iriMapLocal);
			if(iri!=null){
				//DebugUtility.log("iri=[%s]",iri);
				currentProperty=null;
				if(datatypeValue!=null && datatypeValue.length()>0 &&
						!datatypeValue.equals(RDF_XMLLITERAL)){
					String literal=content;
					if(literal==null) {
						literal=getTextNodeText(node);
					}
					currentProperty=RDFTerm.fromTypedString(literal,datatypeValue);
				} else if(node.getAttribute("content")!=null ||
						!hasNonTextChildNodes(node) ||
						(datatypeValue!=null && datatypeValue.length()==0)){
					String literal=node.getAttribute("content");
					if(literal==null) {
						literal=getTextNodeText(node);
					}
					currentProperty=(!StringUtility.isNullOrEmpty(localLanguage)) ?
							RDFTerm.fromLangString(literal, localLanguage) :
								RDFTerm.fromTypedString(literal);
				} else if(hasNonTextChildNodes(node) &&
						(datatypeValue==null || datatypeValue.equals(RDF_XMLLITERAL))){
					// XML literal
					recurse=false;
					if(datatypeValue==null) {
						datatypeValue=RDF_XMLLITERAL;
					}
					try {
						String literal=ExclusiveCanonicalXML.canonicalize(node,
								false, namespacesLocal);
						currentProperty=RDFTerm.fromTypedString(literal,datatypeValue);
					} catch(IllegalArgumentException e){
						// failure to canonicalize
					}
				}
				assert newSubject!=null;
				outputGraph.add(new RDFTriple(
						newSubject,
						RDFTerm.fromIRI(iri),currentProperty
						));
			}
		}
		// Step 10
		if(!skipElement && newSubject!=null){
			for(RDFa.IncompleteTriple triple : context.incompleteTriples){
				if(triple.direction==RDFa.ChainingDirection.Forward){
					outputGraph.add(new RDFTriple(
							context.parentSubject,
							triple.predicate,
							newSubject));
				} else {
					outputGraph.add(new RDFTriple(
							newSubject,triple.predicate,
							context.parentSubject));
				}
			}
		}
		// Step 13
		if(recurse){
			for(INode childNode : node.getChildNodes()){
				IElement childElement;
				RDFa.EvalContext oldContext=context;
				if(childNode instanceof IElement){
					childElement=((IElement)childNode);
					//DebugUtility.log("skip=%s vocab=%s local=%s",
					//	skipElement,context.defaultVocab,
					//localDefaultVocab);
					if(skipElement){
						RDFa.EvalContext ec=oldContext.copy();
						ec.language=localLanguage;
						ec.iriMap=iriMapLocal;
						ec.namespaces=namespacesLocal;
						context=ec;
						process(childElement,false);
					} else {
						RDFa.EvalContext ec=new RDFa.EvalContext();
						ec.baseURI=oldContext.baseURI;
						ec.iriMap=iriMapLocal;
						ec.namespaces=namespacesLocal;
						ec.incompleteTriples=incompleteTriplesLocal;
						ec.parentSubject=((newSubject==null) ? oldContext.parentSubject :
							newSubject);
						ec.parentObject=((currentObject==null) ?
								((newSubject==null) ? oldContext.parentSubject :
									newSubject) : currentObject);
						ec.language=localLanguage;
						context=ec;
						process(childElement,false);
					}
				}
				context=oldContext;
			}
		}
	}

	private static List<String> relterms=Arrays.asList(new String[]{
			"alternate","appendix","cite",
			"bookmark","chapter","contents",
			"copyright","first","glossary",
			"help","icon","index","last",
			"license","meta","next","prev",
			"role","section","start",
			"stylesheet","subsection","top",
			"up","p3pv1"
	});
	private String getRelTermOrCurie(String attribute,
			Map<String,String> prefixMapping){
		if(relterms.contains(StringUtility.toLowerCaseAscii(attribute)))
			return "http://www.w3.org/1999/xhtml/vocab#"+StringUtility.toLowerCaseAscii(attribute);
		return getCurie(attribute,prefixMapping);
	}

	private String getCurie(
			String attribute,
			Map<String,String> prefixMapping) {
		if(attribute==null)return null;
		return getCurie(attribute,0,attribute.length(),prefixMapping);
	}
	private String getCurie(
			String attribute, int offset, int length,
			Map<String,String> prefixMapping) {
		if(attribute==null)return null;
		int refIndex=offset;
		int refLength=length;
		int prefix=getCuriePrefixLength(attribute,refIndex,refLength);
		String prefixIri=null;
		if(prefix>=0){
			String prefixName=StringUtility.toLowerCaseAscii(
					attribute.substring(refIndex,refIndex+prefix));
			refIndex+=(prefix+1);
			refLength-=(prefix+1);
			prefixIri=prefixMapping.get(prefixName);
			if(prefix==0) {
				prefixIri=RDFA_DEFAULT_PREFIX;
			} else {
				prefixIri=prefixMapping.get(prefixName);
			}
			if(prefixIri==null || "_".equals(prefixName))
				return null;
		} else
			// RDFa doesn't define a mapping for an absent prefix
			return null;
		if(!isValidCurieReference(attribute,refIndex,refLength))
			return null;
		if(prefix>=0)
			return relativeResolve(prefixIri+attribute.substring(refIndex,refIndex+refLength)).getValue();
		else
			return null;
	}

	private static final String RDFA_DEFAULT_PREFIX = "http://www.w3.org/1999/xhtml/vocab#";

	private RDFTerm getCurieOrBnode(
			String attribute, int offset, int length,
			Map<String,String> prefixMapping) {
		int refIndex=offset;
		int refLength=length;
		int prefix=getCuriePrefixLength(attribute,refIndex,refLength);
		String prefixIri=null;
		String prefixName=null;
		if(prefix>=0){
			prefixName=StringUtility.toLowerCaseAscii(
					attribute.substring(refIndex,refIndex+prefix));
			refIndex+=(prefix+1);
			refLength-=(prefix+1);
			if(prefix==0) {
				prefixIri=RDFA_DEFAULT_PREFIX;
			} else {
				prefixIri=prefixMapping.get(prefixName);
			}
			if(prefixIri==null && !"_".equals(prefixName))return null;
		} else
			// RDFa doesn't define a mapping for an absent prefix
			return null;
		if(!isValidCurieReference(attribute,refIndex,refLength))
			return null;
		if(prefix>=0){
			if("_".equals(prefixName)){
				assert refIndex>=0 : attribute;
				assert refIndex+refLength<=attribute.length() : attribute;
				if(refLength==0)
					// use an empty blank node: the CURIE syntax
					// allows an empty reference; see the comment
					// in generateBlankNode for why "b:" appears
					// at the beginning
					return getNamedBlankNode("b:empty");
				return getNamedBlankNode(attribute.substring(refIndex,refIndex+refLength));
			}
			assert refIndex>=0 : attribute;
			assert refIndex+refLength<=attribute.length() : attribute;
			return relativeResolve(prefixIri+attribute.substring(refIndex,refIndex+refLength));
		} else
			return null;
	}

	private RDFTerm getSafeCurieOrCurieOrIri(
			String attribute, Map<String,String> prefixMapping) {
		if(attribute==null)return null;
		int lastIndex=attribute.length()-1;
		if(attribute.length()>=2 && attribute.charAt(0)=='[' && attribute.charAt(lastIndex)==']'){
			RDFTerm curie=getCurieOrBnode(attribute,1,attribute.length()-2,
					prefixMapping);
			return curie;
		} else {
			RDFTerm curie=getCurieOrBnode(attribute,0,attribute.length(),
					prefixMapping);
			if(curie==null)
				// evaluate as IRI
				return relativeResolve(attribute);
			return curie;
		}
	}
}
