package com.upokecenter.html.data;

import java.io.IOException;
import java.util.ArrayList;
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

public class RDFa implements IRDFParser {

	 enum ChainingDirection {
		None, Forward, Reverse
	}

	 static class IncompleteTriple {
		@Override
		public String toString() {
			return "IncompleteTriple [list=" + list + ", predicate="
					+ predicate + ", direction=" + direction + "]";
		}
		public List<RDFTerm> list;
		public RDFTerm predicate;
		public ChainingDirection direction;
	}

	 static class EvalContext {
		public String baseURI;
		public RDFTerm parentSubject;
		public RDFTerm parentObject;
		public String language;
		public Map<String,String> iriMap;
		public List<IncompleteTriple> incompleteTriples;
		public Map<String,List<RDFTerm>> listMap;
		public Map<String,String> termMap;
		public Map<String,String> namespaces;
		public String defaultVocab;
		public EvalContext copy(){
			EvalContext ec=new EvalContext();
			ec.baseURI=this.baseURI;
			ec.parentSubject=this.parentSubject;
			ec.parentObject=this.parentObject;
			ec.language=this.language;
			ec.defaultVocab=this.defaultVocab;
			ec.incompleteTriples=new ArrayList<IncompleteTriple>(incompleteTriples);
			ec.listMap=(listMap==null) ? null : new HashMap<String,List<RDFTerm>>(listMap);
			ec.namespaces=(namespaces==null) ? null : new HashMap<String,String>(namespaces);
			ec.termMap=(termMap==null) ? null : new HashMap<String,String>(termMap);
			return ec;
		}
	}

	private IRDFParser parser;
	private EvalContext context;
	private final Set<RDFTriple> outputGraph;
	private final IDocument document;
	private static boolean xhtml_rdfa11=false;

	public RDFa(IDocument document){
		this.document=document;
		this.parser=null;
		this.context=new EvalContext();
		this.context.defaultVocab=null;
		this.context.baseURI=document.getBaseURI();
		if(!URIUtility.hasScheme(this.context.baseURI))
			throw new IllegalArgumentException("baseURI: "+this.context.baseURI);
		this.context.parentSubject=RDFTerm.fromIRI(this.context.baseURI);
		this.context.parentObject=null;
		this.context.namespaces=new HashMap<String,String>();
		this.context.iriMap=new HashMap<String,String>();
		this.context.listMap=new HashMap<String,List<RDFTerm>>();
		this.context.termMap=new HashMap<String,String>();
		this.context.incompleteTriples=new ArrayList<IncompleteTriple>();
		this.context.language=null;
		this.outputGraph=new HashSet<RDFTriple>();
		this.context.termMap.put("describedby","http://www.w3.org/2007/05/powder-s#describedby");
		this.context.termMap.put("license","http://www.w3.org/1999/xhtml/vocab#license");
		this.context.termMap.put("role","http://www.w3.org/1999/xhtml/vocab#role");
		this.context.iriMap.put("cc","http://creativecommons.org/ns#");
		this.context.iriMap.put("ctag","http://commontag.org/ns#");
		this.context.iriMap.put("dc","http://purl.org/dc/terms/");
		this.context.iriMap.put("dcterms","http://purl.org/dc/terms/");
		this.context.iriMap.put("dc11","http://purl.org/dc/elements/1.1/");
		this.context.iriMap.put("foaf","http://xmlns.com/foaf/0.1/");
		this.context.iriMap.put("gr","http://purl.org/goodrelations/v1#");
		this.context.iriMap.put("ical","http://www.w3.org/2002/12/cal/icaltzd#");
		this.context.iriMap.put("og","http://ogp.me/ns#");
		this.context.iriMap.put("schema","http://schema.org/");
		this.context.iriMap.put("rev","http://purl.org/stuff/rev#");
		this.context.iriMap.put("sioc","http://rdfs.org/sioc/ns#");
		this.context.iriMap.put("grddl","http://www.w3.org/2003/g/data-view#");
		this.context.iriMap.put("ma","http://www.w3.org/ns/ma-ont#");
		this.context.iriMap.put("owl","http://www.w3.org/2002/07/owl#");
		this.context.iriMap.put("prov","http://www.w3.org/ns/prov#");
		this.context.iriMap.put("rdf","http://www.w3.org/1999/02/22-rdf-syntax-ns#");
		this.context.iriMap.put("rdfa","http://www.w3.org/ns/rdfa#");
		this.context.iriMap.put("rdfs","http://www.w3.org/2000/01/rdf-schema#");
		this.context.iriMap.put("rif","http://www.w3.org/2007/rif#");
		this.context.iriMap.put("rr","http://www.w3.org/ns/r2rml#");
		this.context.iriMap.put("sd","http://www.w3.org/ns/sparql-service-description#");
		this.context.iriMap.put("skos","http://www.w3.org/2004/02/skos/core#");
		this.context.iriMap.put("skosxl","http://www.w3.org/2008/05/skos-xl#");
		this.context.iriMap.put("v","http://rdf.data-vocabulary.org/#");
		this.context.iriMap.put("vcard","http://www.w3.org/2006/vcard/ns#");
		this.context.iriMap.put("void","http://rdfs.org/ns/void#");
		this.context.iriMap.put("wdr","http://www.w3.org/2007/05/powder#");
		this.context.iriMap.put("wdrs","http://www.w3.org/2007/05/powder-s#");
		this.context.iriMap.put("xhv","http://www.w3.org/1999/xhtml/vocab#");
		this.context.iriMap.put("xml","http://www.w3.org/XML/1998/namespace");
		this.context.iriMap.put("xsd","http://www.w3.org/2001/XMLSchema#");
		IElement docElement=document.getDocumentElement();
		if(docElement!=null && isHtmlElement(docElement,"html")){
			xhtml_rdfa11=true;
			String version=docElement.getAttribute("version");
			if(version!=null && "XHTML+RDFa 1.1".equals(version)){
				xhtml_rdfa11=true;
				String[] terms=new String[]{
						"alternate","appendix","cite",
						"bookmark","chapter","contents",
						"copyright","first","glossary",
						"help","icon","index","last",
						"license","meta","next","prev",
						"previous","section","start",
						"stylesheet","subsection","top",
						"up","p3pv1"
				};
				for(String term : terms){
					this.context.termMap.put(term,"http://www.w3.org/1999/xhtml/vocab#"+term);
				}
			}
			if(version!=null && "XHTML+RDFa 1.0".equals(version)){
				parser=new RDFa1(document);
			}
		}
		extraContext();
	}

	private void extraContext(){
		this.context.iriMap.put("bibo","http://purl.org/ontology/bibo/");
		this.context.iriMap.put("dbp","http://dbpedia.org/property/");
		this.context.iriMap.put("dbp-owl","http://dbpedia.org/ontology/");
		this.context.iriMap.put("dbr","http://dbpedia.org/resource/");
		this.context.iriMap.put("ex","http://example.org/");
	}

	private static boolean isHtmlElement(IElement element, String name){
		return element!=null &&
				"http://www.w3.org/1999/xhtml".equals(element.getNamespaceURI()) &&
				name.equals(element.getLocalName());
	}

	private static final RDFTerm RDFA_USES_VOCABULARY=
			RDFTerm.fromIRI("http://www.w3.org/ns/rdfa#usesVocabulary");

	private static final String RDF_XMLLITERAL="http://www.w3.org/1999/02/22-rdf-syntax-ns#XMLLiteral";

	private static final String[] emptyStringArray=new String[]{};

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
	private static boolean isTermChar(int c){
		return (c>='a' && c<='z') ||
				(c>='A' && c<='Z') ||
				c=='_' || c=='.' || c=='-' || c=='/' ||
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

	private static boolean isValidTerm(String s){
		if(s==null || s.length()==0)return false;
		if(!isNCNameStartChar(s.charAt(0)))return false;
		int index=1;
		int sLength=s.length();
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
				return false;
			else if(!isTermChar(c))return false;
			index++;
		}
		return true;
	}
	private static boolean isValidCurieReference(String s, int offset, int length){
		return URIUtility.isValidCurieReference(s, offset, length, false);
	}

	private static String[] splitPrefixList(String s){
		if(s==null || s.length()==0)return emptyStringArray;
		int index=0;
		int sLength=s.length();
		while(index<sLength){
			char c=s.charAt(index);
			if(c!=0x09 && c!=0x0a && c!=0x0d && c!=0x20){
				break;
			}
			index++;
		}
		if(index==s.length())return emptyStringArray;
		StringBuilder prefix=new StringBuilder();
		StringBuilder iri=new StringBuilder();
		int state=0; // Before NCName state
		ArrayList<String> strings=new ArrayList<String>();
		while(index<sLength){
			// Get the next Unicode character
			int c=s.charAt(index);
			if(c>=0xD800 && c<=0xDBFF && index+1<sLength &&
					s.charAt(index+1)>=0xDC00 && s.charAt(index+1)<=0xDFFF){
				// Get the Unicode code point for the surrogate pair
				c=0x10000+(c-0xD800)*0x400+(s.charAt(index+1)-0xDC00);
				index++;
			} else if(c>=0xD800 && c<=0xDFFF){
				// error
				break;
			}
			if(state==0){ // Before NCName
				if(c==0x09 || c==0x0a || c==0x0d || c==0x20){
					// ignore whitespace
					index++;
				} else if(isNCNameStartChar(c)){
					// start of NCName
					prefix.appendCodePoint(c);
					state=1;
					index++;
				} else {
					// error
					break;
				}
			} else if(state==1){ // NCName
				if(c==':'){
					state=2;
					index++;
				} else if(isNCNameChar(c)){
					// continuation of NCName
					prefix.appendCodePoint(c);
					index++;
				} else {
					// error
					break;
				}
			} else if(state==2){ // After NCName
				if(c==' '){
					state=3;
					index++;
				} else {
					// error
					break;
				}
			} else if(state==3){ // Before IRI
				if(c==' '){
					index++;
				} else {
					// start of IRI
					iri.appendCodePoint(c);
					state=4;
					index++;
				}
			} else if(state==4){ // IRI
				if(c==0x09 || c==0x0a || c==0x0d || c==0x20){
					String prefixString=StringUtility.toLowerCaseAscii(prefix.toString());
					// add prefix only if it isn't empty;
					// empty prefixes will not have a mapping
					if(prefixString.length()>0){
						strings.add(prefixString);
						strings.add(iri.toString());
					}
					prefix.delete(0, prefix.length());
					iri.delete(0, iri.length());
					state=0;
					index++;
				} else {
					// continuation of IRI
					iri.appendCodePoint(c);
					index++;
				}
			}
		}
		if(state==4){
			strings.add(StringUtility.toLowerCaseAscii(prefix.toString()));
			strings.add(iri.toString());
		}
		return strings.toArray(new String[]{});
	}

	private int blankNode;
	private final Map<String,RDFTerm> bnodeLabels=new HashMap<String,RDFTerm>();

	private RDFTerm getNamedBlankNode(String str){
		RDFTerm term=RDFTerm.fromBlankNode(str);
		bnodeLabels.put(str,term);
		return term;
	}

	private RDFTerm generateBlankNode(){
		// Use "//" as the prefix; according to the CURIE syntax,
		// "//" can never begin a valid CURIE reference, so it can
		// be used to guarantee that generated blank nodes will never
		// conflict with those stated explicitly
		String blankNodeString="//"+Integer.toString(blankNode);
		blankNode++;
		RDFTerm term=RDFTerm.fromBlankNode(blankNodeString);
		bnodeLabels.put(blankNodeString,term);
		return term;
	}

	@Override
	public Set<RDFTriple> parse() throws IOException {
		if(parser!=null)
			return parser.parse();
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
		List<IncompleteTriple> incompleteTriplesLocal=new ArrayList<IncompleteTriple>();
		String localLanguage=context.language;
		RDFTerm newSubject=null;
		boolean skipElement=false;
		RDFTerm currentProperty=null;

		RDFTerm currentObject=null;
		RDFTerm typedResource=null;
		Map<String,String> iriMapLocal=
				new HashMap<String,String>(context.iriMap);
		Map<String,String> namespacesLocal=
				new HashMap<String,String>(context.namespaces);
		Map<String,List<RDFTerm>> listMapLocal=context.listMap;
		Map<String,String> termMapLocal=
				new HashMap<String,String>(context.termMap);
		String localDefaultVocab=context.defaultVocab;
		String attr=null;
		//DebugUtility.log("cur parobj[%s]=%s",node.getTagName(),context.parentObject);
		//DebugUtility.log("base=%s",context.baseURI);
		attr=node.getAttribute("xml:base");
		if(attr!=null){
			context.baseURI=URIUtility.relativeResolve(attr, context.baseURI);
		}
		// Support deprecated XML namespaces
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
		attr=node.getAttribute("vocab");
		if(attr!=null){
			if(attr.length()==0){
				// set default vocabulary to null
				localDefaultVocab=null;
			} else {
				// set default vocabulary to vocab IRI
				RDFTerm defPrefix=relativeResolve(attr);
				localDefaultVocab=defPrefix.getValue();
				outputGraph.add(new RDFTriple(
						RDFTerm.fromIRI(context.baseURI),
						RDFA_USES_VOCABULARY,defPrefix
						));
			}
		}

		attr=node.getAttribute("prefix");
		if(attr!=null){
			String[] prefixList=splitPrefixList(attr);
			for(int i=0;i<prefixList.length;i+=2){
				// Add prefix and IRI to the map, unless the prefix
				// is "_"
				if(!"_".equals(prefixList[i])){
					iriMapLocal.put(prefixList[i], prefixList[i+1]);
				}
			}
		}
		attr=node.getAttribute("lang");
		if(attr!=null){
			localLanguage=attr;
		}
		attr=node.getAttribute("xml:lang");
		if(attr!=null){
			localLanguage=attr;
		}
		String rel=node.getAttribute("rel");
		String rev=node.getAttribute("rev");
		String property=node.getAttribute("property");
		String content=node.getAttribute("content");
		String datatype=node.getAttribute("datatype");
		if(rel==null && rev==null){
			// Step 5
			//DebugUtility.log("%s %s",property,node.getTagName());
			if(property!=null && content==null && datatype==null){
				RDFTerm about=getSafeCurieOrCurieOrIri(
						node.getAttribute("about"),iriMapLocal);
				if(about!=null){
					newSubject=about;
				} else if(root){
					newSubject=getSafeCurieOrCurieOrIri("",iriMapLocal);
				} else if(context.parentObject!=null){
					newSubject=context.parentObject;
				}
				String typeof=node.getAttribute("typeof");
				if(typeof!=null){
					if(about!=null){
						typedResource=about;
					} else if(root){
						typedResource=getSafeCurieOrCurieOrIri("",iriMapLocal);
					} else {
						RDFTerm resource=getSafeCurieOrCurieOrIri(
								node.getAttribute("resource"),iriMapLocal);
						if(resource==null){
							resource=relativeResolve(node.getAttribute("href"));
						}
						if(resource==null){
							resource=relativeResolve(node.getAttribute("src"));
						}
						//DebugUtility.log("resource=%s",resource);
						if((resource==null || resource.getKind()!=RDFTerm.IRI) &&
								xhtml_rdfa11){
							if(isHtmlElement(node, "head") ||
									isHtmlElement(node, "body")){
								newSubject=context.parentObject;
							}
						}
						if(resource==null){
							typedResource=generateBlankNode();
						} else {
							typedResource=resource;
						}
						currentObject=typedResource;
					}
				}
			} else {
				RDFTerm resource=getSafeCurieOrCurieOrIri(
						node.getAttribute("about"),iriMapLocal);
				if(resource==null){
					resource=getSafeCurieOrCurieOrIri(
							node.getAttribute("resource"),iriMapLocal);
					//DebugUtility.log("resource=%s %s %s",
					//	node.getAttribute("resource"),
					//resource,context.parentObject);
				}
				if(resource==null){
					resource=relativeResolve(node.getAttribute("href"));
				}
				if(resource==null){
					resource=relativeResolve(node.getAttribute("src"));
				}
				if((resource==null || resource.getKind()!=RDFTerm.IRI) &&
						xhtml_rdfa11){
					if(isHtmlElement(node, "head") ||
							isHtmlElement(node, "body")){
						resource=context.parentObject;
					}
				}
				if(resource==null){
					if(root){
						newSubject=getSafeCurieOrCurieOrIri("",iriMapLocal);
					} else if(node.getAttribute("typeof")!=null){
						newSubject=generateBlankNode();
					} else {
						if(context.parentObject!=null) {
							newSubject=context.parentObject;
						}
						if(node.getAttribute("property")==null){
							skipElement=true;
						}
					}
				} else {
					newSubject=resource;
				}
				if(node.getAttribute("typeof")!=null){
					typedResource=newSubject;
				}
			}
		} else {
			// Step 6
			RDFTerm about=getSafeCurieOrCurieOrIri(
					node.getAttribute("about"),iriMapLocal);
			if(about!=null){
				newSubject=about;
			}
			if(node.getAttribute("typeof")!=null){
				typedResource=newSubject;
			}
			if(about==null){
				if(root){
					about=getSafeCurieOrCurieOrIri("",iriMapLocal);
				} else if(context.parentObject!=null){
					newSubject=context.parentObject;
				}
			}
			RDFTerm resource=getSafeCurieOrCurieOrIri(
					node.getAttribute("resource"),iriMapLocal);
			if(resource==null){
				resource=relativeResolve(node.getAttribute("href"));
			}
			if(resource==null){
				resource=relativeResolve(node.getAttribute("src"));
			}
			if((resource==null || resource.getKind()!=RDFTerm.IRI) &&
					xhtml_rdfa11){
				if(isHtmlElement(node, "head") ||
						isHtmlElement(node, "body")){
					newSubject=context.parentObject;
				}
			}
			if(resource==null && node.getAttribute("typeof")!=null &&
					node.getAttribute("about")==null){
				currentObject=generateBlankNode();
			} else if(resource!=null){
				currentObject=resource;
			}
			if(node.getAttribute("typeof")!=null &&
					node.getAttribute("about")==null){
				typedResource=currentObject;
			}
		}
		// Step 7
		if(typedResource!=null){
			String[] types=StringUtility.splitAtNonFFSpaces(node.getAttribute("typeof"));
			for(String type : types){
				String iri=getTermOrCurieOrAbsIri(type,
						iriMapLocal,termMapLocal,localDefaultVocab);
				if(iri!=null){
					outputGraph.add(new RDFTriple(
							typedResource,RDFTerm.A,
							RDFTerm.fromIRI(iri)
							));
				}
			}
		}
		// Step 8
		if(newSubject!=null && !newSubject.equals(context.parentObject)){
			context.listMap.clear();
		}
		// Step 9
		if(currentObject!=null){
			String inlist=node.getAttribute("inlist");
			if(inlist!=null && rel!=null){
				String[] types=StringUtility.splitAtNonFFSpaces(rel);
				for(String type : types){
					String iri=getTermOrCurieOrAbsIri(type,
							iriMapLocal,termMapLocal,localDefaultVocab);
					if(iri!=null){
						if(!listMapLocal.containsKey(iri)){
							List<RDFTerm> newList=new ArrayList<RDFTerm>();
							newList.add(currentObject);
							listMapLocal.put(iri,newList);
						} else {
							List<RDFTerm> existingList=listMapLocal.get(iri);
							existingList.add(currentObject);
						}
					}
				}
			} else {
				String[] types=StringUtility.splitAtNonFFSpaces(rel);
				assert newSubject!=null;
				for(String type : types){
					String iri=getTermOrCurieOrAbsIri(type,
							iriMapLocal,termMapLocal,localDefaultVocab);
					if(iri!=null){
						outputGraph.add(new RDFTriple(
								newSubject,
								RDFTerm.fromIRI(iri),currentObject
								));
					}
				}
				types=StringUtility.splitAtNonFFSpaces(rev);
				for(String type : types){
					String iri=getTermOrCurieOrAbsIri(type,
							iriMapLocal,termMapLocal,localDefaultVocab);
					if(iri!=null){
						outputGraph.add(new RDFTriple(
								currentObject,
								RDFTerm.fromIRI(iri),
								newSubject
								));
					}
				}
			}
		} else {
			// Step 10
			String[] types=StringUtility.splitAtNonFFSpaces(rel);
			boolean inlist=(node.getAttribute("inlist"))!=null;
			boolean hasPredicates=false;
			// Defines predicates
			for(String type : types){
				String iri=getTermOrCurieOrAbsIri(type,
						iriMapLocal,termMapLocal,localDefaultVocab);
				if(iri!=null){
					if(!hasPredicates){
						hasPredicates=true;
						currentObject=generateBlankNode();
					}
					IncompleteTriple inc=new IncompleteTriple();
					if(inlist){
						if(!listMapLocal.containsKey(iri)){
							List<RDFTerm> newList=new ArrayList<RDFTerm>();
							listMapLocal.put(iri, newList);
							//NOTE: Should not be a copy
							inc.list=newList;
						} else {
							List<RDFTerm> existingList=listMapLocal.get(iri);
							inc.list=existingList;
						}
						inc.direction=ChainingDirection.None;
					} else {
						inc.predicate=RDFTerm.fromIRI(iri);
						inc.direction=ChainingDirection.Forward;
					}
					//DebugUtility.log(inc);
					incompleteTriplesLocal.add(inc);
				}
			}
			types=StringUtility.splitAtNonFFSpaces(rev);
			for(String type : types){
				String iri=getTermOrCurieOrAbsIri(type,
						iriMapLocal,termMapLocal,localDefaultVocab);
				if(iri!=null){
					if(!hasPredicates){
						hasPredicates=true;
						currentObject=generateBlankNode();
					}
					IncompleteTriple inc=new IncompleteTriple();
					inc.predicate=RDFTerm.fromIRI(iri);
					inc.direction=ChainingDirection.Reverse;
					incompleteTriplesLocal.add(inc);
				}
			}
		}
		// Step 11
		String[] preds=StringUtility.splitAtNonFFSpaces(property);
		String datatypeValue=getTermOrCurieOrAbsIri(datatype,
				iriMapLocal,termMapLocal,localDefaultVocab);
		if(datatype!=null && datatypeValue==null) {
			datatypeValue="";
		}
		//DebugUtility.log("datatype=[%s] prop=%s vocab=%s",
		//	datatype,property,localDefaultVocab);
		//DebugUtility.log("datatypeValue=[%s]",datatypeValue);
		for(String pred : preds){
			String iri=getTermOrCurieOrAbsIri(pred,
					iriMapLocal,termMapLocal,localDefaultVocab);
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
				} else if(datatypeValue!=null && datatypeValue.length()==0){
					String literal=content;
					if(literal==null) {
						literal=getTextNodeText(node);
					}
					currentProperty=(!StringUtility.isNullOrEmpty(localLanguage)) ?
							RDFTerm.fromLangString(literal, localLanguage) :
								RDFTerm.fromTypedString(literal);
				} else if(datatypeValue!=null && datatypeValue.equals(RDF_XMLLITERAL)){
					// XML literal
					try {
						String literal=ExclusiveCanonicalXML.canonicalize(node,
								false, namespacesLocal);
						currentProperty=RDFTerm.fromTypedString(literal,datatypeValue);
					} catch(IllegalArgumentException e){
						// failure to canonicalize
					}
				} else if(content!=null){
					String literal=content;
					currentProperty=(!StringUtility.isNullOrEmpty(localLanguage)) ?
							RDFTerm.fromLangString(literal, localLanguage) :
								RDFTerm.fromTypedString(literal);
				} else if(rel==null && content==null && rev==null){
					RDFTerm resource=getSafeCurieOrCurieOrIri(
							node.getAttribute("resource"),iriMapLocal);
					if(resource==null){
						resource=relativeResolve(node.getAttribute("href"));
					}
					if(resource==null){
						resource=relativeResolve(node.getAttribute("src"));
					}
					if(resource!=null){
						currentProperty=resource;
					}
				}
				if(currentProperty==null){
					if(node.getAttribute("typeof")!=null &&
							node.getAttribute("about")==null){
						currentProperty=typedResource;
					} else {
						String literal=content;
						if(literal==null) {
							literal=getTextNodeText(node);
						}
						currentProperty=(!StringUtility.isNullOrEmpty(localLanguage)) ?
								RDFTerm.fromLangString(literal, localLanguage) :
									RDFTerm.fromTypedString(literal);
					}
				}
				//DebugUtility.log("curprop: %s",currentProperty);
				if(node.getAttribute("inlist")!=null){
					if(!listMapLocal.containsKey(iri)){
						List<RDFTerm> newList=new ArrayList<RDFTerm>();
						newList.add(currentProperty);
						listMapLocal.put(iri,newList);
					} else {
						List<RDFTerm> existingList=listMapLocal.get(iri);
						existingList.add(currentProperty);
					}
				} else {
					assert newSubject!=null;
					outputGraph.add(new RDFTriple(
							newSubject,
							RDFTerm.fromIRI(iri),currentProperty
							));
				}
			}
		}
		// Step 12
		if(!skipElement && newSubject!=null){
			for(IncompleteTriple triple : context.incompleteTriples){
				if(triple.direction==ChainingDirection.None){
					List<RDFTerm> list=triple.list;
					list.add(newSubject);
				} else if(triple.direction==ChainingDirection.Forward){
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
		for(INode childNode : node.getChildNodes()){
			IElement childElement;
			EvalContext oldContext=context;
			if(childNode instanceof IElement){
				childElement=((IElement)childNode);
				//DebugUtility.log("skip=%s vocab=%s local=%s",
				//	skipElement,context.defaultVocab,
				//localDefaultVocab);
				if(skipElement){
					EvalContext ec=oldContext.copy();
					ec.language=localLanguage;
					ec.iriMap=iriMapLocal;
					context=ec;
					process(childElement,false);
				} else {
					EvalContext ec=new EvalContext();
					ec.baseURI=oldContext.baseURI;
					ec.namespaces=namespacesLocal;
					ec.iriMap=iriMapLocal;
					ec.incompleteTriples=incompleteTriplesLocal;
					ec.listMap=listMapLocal;
					ec.termMap=termMapLocal;
					ec.parentSubject=((newSubject==null) ? oldContext.parentSubject :
						newSubject);
					ec.parentObject=((currentObject==null) ?
							((newSubject==null) ? oldContext.parentSubject :
								newSubject) : currentObject);
					ec.defaultVocab=localDefaultVocab;
					ec.language=localLanguage;
					context=ec;
					process(childElement,false);
				}
			}
			context=oldContext;
		}
		// Step 14
		for(String iri : listMapLocal.keySet()){
			if(!context.listMap.containsKey(iri)){
				List<RDFTerm> list=listMapLocal.get(iri);
				if(list.size()==0){
					outputGraph.add(new RDFTriple(
							(newSubject==null ? newSubject : context.parentSubject),
							RDFTerm.fromIRI(iri),RDFTerm.NIL
							));
				} else {
					RDFTerm bnode=generateBlankNode();
					outputGraph.add(new RDFTriple(
							(newSubject==null ? newSubject : context.parentSubject),
							RDFTerm.fromIRI(iri),bnode
							));
					for(int i=0;i<list.size();i++){
						RDFTerm nextBnode=(i==list.size()-1) ?
								generateBlankNode() : RDFTerm.NIL;
								outputGraph.add(new RDFTriple(
										bnode,RDFTerm.FIRST,list.get(i)
										));
								outputGraph.add(new RDFTriple(
										bnode,RDFTerm.REST,nextBnode
										));
								bnode=nextBnode;
					}
				}
			}
		}
	}
	private String getCurie(
			String attribute, int offset, int length,
			Map<String,String> prefixMapping) {
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
					// in generateBlankNode for why "//" appears
					// at the beginning
					return getNamedBlankNode("//empty");
				return getNamedBlankNode(attribute.substring(refIndex,refIndex+refLength));
			}
			assert refIndex>=0 : attribute;
			assert refIndex+refLength<=attribute.length() : attribute;
			return relativeResolve(prefixIri+attribute.substring(refIndex,refIndex+refLength));
		} else
			return null;
	}

	private static <T> T getValueCaseInsensitive(
			Map<String,T> map,
			String key
			){
		if(key==null)
			return map.get(null);
		key=StringUtility.toLowerCaseAscii(key);
		for(String k : map.keySet()){
			if(key.equals(StringUtility.toLowerCaseAscii(k)))
				return map.get(k);
		}
		return null;
	}

	private String getTermOrCurieOrAbsIri(
			String attribute,
			Map<String,String> prefixMapping,
			Map<String,String> termMapping,
			String defaultVocab) {
		if(attribute==null)return null;
		if(isValidTerm(attribute)){
			if(defaultVocab!=null)
				return relativeResolve(defaultVocab+attribute).getValue();
			else if(termMapping.containsKey(attribute))
				return termMapping.get(attribute);
			else {
				String value=getValueCaseInsensitive(termMapping,attribute);
				return value;
			}
		}
		String curie=getCurie(attribute,0,attribute.length(),
				prefixMapping);
		if(curie==null){
			// evaluate as IRI if it's absolute
			if(URIUtility.hasScheme(attribute))
				//DebugUtility.log("has scheme: %s",attribute);
				return relativeResolve(attribute).getValue();
			return null;
		}
		return curie;
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
