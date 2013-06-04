/*
Written in 2013 by Peter Occil.  
Any copyright is dedicated to the Public Domain.
http://creativecommons.org/publicdomain/zero/1.0/

If you like this, you should donate to Peter O.
at: http://upokecenter.com/d/
*/
package com.upokecenter.rdf;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.upokecenter.io.StackableCharacterInput;
import com.upokecenter.io.StringCharacterInput;
import com.upokecenter.io.Utf8CharacterInput;
import com.upokecenter.util.URIUtility;
public class TurtleParser implements IRDFParser {

	private static class TurtleObject {
		public static final int SIMPLE=0;
		public static final int COLLECTION=1;
		public static final int PROPERTIES=2;
		public static TurtleObject fromTerm(RDFTerm term){
			TurtleObject tobj=new TurtleObject();
			tobj.term=term;
			tobj.kind=TurtleObject.SIMPLE;
			return tobj;
		}
		public static TurtleObject newCollection(){
			TurtleObject tobj=new TurtleObject();
			tobj.objects=new ArrayList<TurtleObject>();
			tobj.kind=TurtleObject.COLLECTION;
			return tobj;
		}
		public static TurtleObject newPropertyList(){
			TurtleObject tobj=new TurtleObject();
			tobj.properties=new ArrayList<TurtleProperty>();
			tobj.kind=TurtleObject.PROPERTIES;
			return tobj;
		}
		public RDFTerm term;

		public int kind;
		List<TurtleObject> objects;

		List<TurtleProperty> properties;
		public List<TurtleObject> getObjects(){
			return objects;
		}
		public List<TurtleProperty> getProperties(){
			return properties;
		}
	}
	private static class TurtleProperty {
		public RDFTerm pred;
		public TurtleObject obj;
	}
	private final Map<String,RDFTerm> bnodeLabels;
	private final Map<String,String> namespaces;

	private String baseURI;

	private TurtleObject curSubject;

	private RDFTerm curPredicate;


	private final StackableCharacterInput input;
	private int curBlankNode=0;

	public TurtleParser(InputStream stream){
		this(stream,"about:blank");
	}

	public TurtleParser(InputStream stream, String baseURI){
		if((stream)==null)throw new NullPointerException("stream");
		if(baseURI==null)throw new NullPointerException("baseURI");
		if(!URIUtility.hasScheme(baseURI))
			throw new IllegalArgumentException("baseURI");
		this.input=new StackableCharacterInput(
				new Utf8CharacterInput(stream));
		this.baseURI=baseURI;
		bnodeLabels=new HashMap<String,RDFTerm>();
		namespaces=new HashMap<String,String>();
	}

	public TurtleParser(String str){
		this(str,"about:blank");
	}

	public TurtleParser(String str, String baseURI){
		if((str)==null)throw new NullPointerException("str");
		if(baseURI==null)throw new NullPointerException("baseURI");
		if(!URIUtility.hasScheme(baseURI))
			throw new IllegalArgumentException("baseURI");
		this.input=new StackableCharacterInput(
				new StringCharacterInput(str,true));
		this.baseURI=baseURI;
		bnodeLabels=new HashMap<String,RDFTerm>();
		namespaces=new HashMap<String,String>();
	}
	private RDFTerm allocateBlankNode(){
		curBlankNode++;
		// A period is included so as not to conflict
		// with user-defined blank node labels (this is allowed
		// because the syntax for blank node identifiers is
		// not concretely defined)
		String label="."+Integer.toString(curBlankNode);
		RDFTerm node=RDFTerm.fromBlankNode(label);
		bnodeLabels.put(label,node);
		return node;
	}

	private void emitRDFTriple(RDFTerm subj, RDFTerm pred, RDFTerm obj, Set<RDFTriple> triples){
		RDFTriple triple=new RDFTriple(subj,pred,obj);
		triples.add(triple);
	}

	private void emitRDFTriple(RDFTerm subj, RDFTerm pred,
			TurtleObject obj, Set<RDFTriple> triples){
		if(obj.kind==TurtleObject.SIMPLE){
			emitRDFTriple(subj,pred,obj.term,triples);
		} else if(obj.kind==TurtleObject.PROPERTIES){
			List<TurtleProperty> props=obj.getProperties();
			if(props.size()==0){
				emitRDFTriple(subj,pred,allocateBlankNode(),triples);
			} else {
				RDFTerm blank=allocateBlankNode();
				emitRDFTriple(subj,pred,blank,triples);
				for(int i=0;i<props.size();i++){
					emitRDFTriple(blank,props.get(i).pred,props.get(i).obj,triples);
				}
			}
		} else if(obj.kind==TurtleObject.COLLECTION){
			List<TurtleObject> objs=obj.getObjects();
			if(objs.size()==0){
				emitRDFTriple(subj,pred,RDFTerm.NIL,triples);
			} else {
				RDFTerm curBlank=allocateBlankNode();
				RDFTerm firstBlank=curBlank;
				emitRDFTriple(curBlank,RDFTerm.FIRST,objs.get(0),triples);
				for(int i=1;i<=objs.size();i++){
					if(i==objs.size()){
						emitRDFTriple(curBlank,RDFTerm.REST,RDFTerm.NIL,triples);
					} else {
						RDFTerm nextBlank=allocateBlankNode();
						emitRDFTriple(curBlank,RDFTerm.REST,nextBlank,triples);
						emitRDFTriple(nextBlank,RDFTerm.FIRST,objs.get(i),triples);
						curBlank=nextBlank;
					}
				}
				emitRDFTriple(subj,pred,firstBlank,triples);
			}
		}
	}

	private void emitRDFTriple(TurtleObject subj, RDFTerm pred,
			TurtleObject obj, Set<RDFTriple> triples){
		if(subj.kind==TurtleObject.SIMPLE){
			emitRDFTriple(subj.term,pred,obj,triples);
		} else if(subj.kind==TurtleObject.PROPERTIES){
			List<TurtleProperty> props=subj.getProperties();
			if(props.size()==0){
				emitRDFTriple(allocateBlankNode(),pred,obj,triples);
			} else {
				RDFTerm blank=allocateBlankNode();
				emitRDFTriple(blank,pred,obj,triples);
				for(int i=0;i<props.size();i++){
					emitRDFTriple(blank,props.get(i).pred,props.get(i).obj,triples);
				}
			}
		} else if(subj.kind==TurtleObject.COLLECTION){
			List<TurtleObject> objs=subj.getObjects();
			if(objs.size()==0){
				emitRDFTriple(RDFTerm.NIL,pred,obj,triples);
			} else {
				RDFTerm curBlank=allocateBlankNode();
				RDFTerm firstBlank=curBlank;
				emitRDFTriple(curBlank,RDFTerm.FIRST,objs.get(0),triples);
				for(int i=1;i<=objs.size();i++){
					if(i==objs.size()){
						emitRDFTriple(curBlank,RDFTerm.REST,RDFTerm.NIL,triples);
					} else {
						RDFTerm nextBlank=allocateBlankNode();
						emitRDFTriple(curBlank,RDFTerm.REST,nextBlank,triples);
						emitRDFTriple(nextBlank,RDFTerm.FIRST,objs.get(i),triples);
						curBlank=nextBlank;
					}
				}
				emitRDFTriple(firstBlank,pred,obj,triples);
			}
		}
	}

	private RDFTerm finishStringLiteral(String str) throws IOException {
		int mark=input.setHardMark();
		int ch=input.read();
		if(ch=='@')
			return RDFTerm.fromLangString(str,readLanguageTag());
		else if(ch=='^' && input.read()=='^'){
			ch=input.read();
			if(ch=='<')
				return RDFTerm.fromTypedString(str,readIriReference());
			else if(ch==':'){ // prefixed name with current prefix
				String scope=namespaces.get("");
				if(scope==null)throw new ParserException();
				return RDFTerm.fromTypedString(str,scope+readOptionalLocalName());
			} else if(isNameStartChar(ch)){ // prefix
				String prefix=readPrefix(ch);
				String scope=namespaces.get(prefix);
				if(scope==null)throw new ParserException();
				return RDFTerm.fromTypedString(str,scope+readOptionalLocalName());
			} else throw new ParserException();
		} else {
			input.setMarkPosition(mark);
			return RDFTerm.fromTypedString(str);
		}
	}

	private boolean isNameChar(int ch) {
		return (ch>='a' && ch<='z') ||
				(ch>='0' && ch<='9') ||
				(ch>='A' && ch<='Z') || ch=='_' || ch=='-' ||
				ch==0xb7 ||
				(ch>=0xc0 && ch<=0xd6) ||
				(ch>=0xd8 && ch<=0xf6) ||
				(ch>=0xf8 && ch<=0x37d) ||
				(ch>=0x37f && ch<=0x1fff) ||
				(ch>=0x200c && ch<=0x200d) ||
				ch==0x203f || ch==0x2040 ||
				(ch>=0x2070 && ch<=0x218f) ||
				(ch>=0x2c00 && ch<=0x2fef) ||
				(ch>=0x3001 && ch<=0xd7ff) ||
				(ch>=0xf900 && ch<=0xfdcf) ||
				(ch>=0xfdf0 && ch<=0xfffd) ||
				(ch>=0x10000 && ch<=0xeffff);
	}

	private boolean isNameStartChar(int ch) {
		return (ch>='a' && ch<='z') ||
				(ch>='A' && ch<='Z') ||
				(ch>=0xc0 && ch<=0xd6) ||
				(ch>=0xd8 && ch<=0xf6) ||
				(ch>=0xf8 && ch<=0x2ff) ||
				(ch>=0x370 && ch<=0x37d) ||
				(ch>=0x37f && ch<=0x1fff) ||
				(ch>=0x200c && ch<=0x200d) ||
				(ch>=0x2070 && ch<=0x218f) ||
				(ch>=0x2c00 && ch<=0x2fef) ||
				(ch>=0x3001 && ch<=0xd7ff) ||
				(ch>=0xf900 && ch<=0xfdcf) ||
				(ch>=0xfdf0 && ch<=0xfffd) ||
				(ch>=0x10000 && ch<=0xeffff);
	}

	private boolean isNameStartCharU(int ch) {
		return (ch>='a' && ch<='z') ||
				(ch>='A' && ch<='Z') || ch=='_' ||
				(ch>=0xc0 && ch<=0xd6) ||
				(ch>=0xd8 && ch<=0xf6) ||
				(ch>=0xf8 && ch<=0x2ff) ||
				(ch>=0x370 && ch<=0x37d) ||
				(ch>=0x37f && ch<=0x1fff) ||
				(ch>=0x200c && ch<=0x200d) ||
				(ch>=0x2070 && ch<=0x218f) ||
				(ch>=0x2c00 && ch<=0x2fef) ||
				(ch>=0x3001 && ch<=0xd7ff) ||
				(ch>=0xf900 && ch<=0xfdcf) ||
				(ch>=0xfdf0 && ch<=0xfffd) ||
				(ch>=0x10000 && ch<=0xeffff);
	}
	@Override
	public Set<RDFTriple> parse() throws IOException {
		Set<RDFTriple> triples=new HashSet<RDFTriple>();
		while(true){
			skipWhitespace();
			int mark=input.setHardMark();
			int ch=input.read();
			if(ch<0){
				RDFInternal.replaceBlankNodes(triples,bnodeLabels);
				return triples;
			}
			if(ch=='@'){
				ch=input.read();
				if(ch=='p' && input.read()=='r' && input.read()=='e' &&
						input.read()=='f' && input.read()=='i' &&
						input.read()=='x' && skipWhitespace()){
					readPrefixStatement(false);
					continue;
				}
				else if(ch=='b' && input.read()=='a' && input.read()=='s' &&
						input.read()=='e' && skipWhitespace()){
					readBase(false);
					continue;
				}
				else throw new ParserException();
			} else if(ch=='b' || ch=='B'){
				int c2=0;
				if(((c2=input.read())=='A' || c2=='a') &&
						((c2=input.read())=='S' || c2=='s') &&
						((c2=input.read())=='E' || c2=='e') && skipWhitespace()){
					readBase(true);
					continue;
				} else {
					input.setMarkPosition(mark);
				}
			} else if(ch=='p' || ch=='P'){
				int c2=0;
				if(((c2=input.read())=='R' || c2=='r') &&
						((c2=input.read())=='E' || c2=='e') &&
						((c2=input.read())=='F' || c2=='f') &&
						((c2=input.read())=='I' || c2=='i') &&
						((c2=input.read())=='X' || c2=='x') && skipWhitespace()){
					readPrefixStatement(true);
					continue;
				} else {
					input.setMarkPosition(mark);
				}
			} else {
				input.setMarkPosition(mark);
			}
			readTriples(triples);
		}
	}

	private void readBase(boolean sparql) throws IOException {
		if(input.read()!='<')
			throw new ParserException();
		baseURI=readIriReference();
		if(!sparql){
			skipWhitespace();
			if(input.read()!='.')
				throw new ParserException();
		} else {
			skipWhitespace();
		}
	}

	private String readBlankNodeLabel() throws IOException {
		StringBuilder ilist=new StringBuilder();
		int startChar=input.read();
		if(!isNameStartCharU(startChar) &&
				(startChar<'0' || startChar>'9'))
			throw new ParserException();
		ilist.appendCodePoint(startChar);
		boolean lastIsPeriod=false;
		input.setSoftMark();
		while(true){
			int ch=input.read();
			if(ch=='.'){
				int position=input.getMarkPosition();
				int ch2=input.read();
				if(!isNameChar(ch2) && ch2!=':' && ch2!='.'){
					input.setMarkPosition(position-1);
					return ilist.toString();
				} else {
					input.moveBack(1);
				}
				ilist.appendCodePoint(ch);
				lastIsPeriod=true;
			} else if(isNameChar(ch)){
				ilist.appendCodePoint(ch);
				lastIsPeriod=false;
			} else {
				if(ch>=0) {
					input.moveBack(1);
				}
				if(lastIsPeriod)
					throw new ParserException();
				return ilist.toString();
			}
		}
	}

	private TurtleObject readBlankNodePropertyList() throws IOException {
		TurtleObject obj=TurtleObject.newPropertyList();
		boolean havePredObject=false;
		while(true){
			skipWhitespace();
			int ch;
			if(havePredObject){
				boolean haveSemicolon=false;
				while(true){
					input.setSoftMark();
					ch=input.read();
					if(ch==';'){
						skipWhitespace();
						haveSemicolon=true;
					} else {
						if(ch>=0) {
							input.moveBack(1);
						}
						break;
					}
				}
				if(!haveSemicolon) {
					break;
				}
			}
			RDFTerm pred=readPredicate();
			if(pred==null){
				break;
			}
			havePredObject=true;
			readObjectListToProperties(pred,obj);
		}
		if(input.read()!=']')
			throw new ParserException();
		return obj;
	}
	private TurtleObject readCollection() throws IOException {
		TurtleObject obj=TurtleObject.newCollection();
		while(true){
			skipWhitespace();
			input.setHardMark();
			int ch=input.read();
			if(ch==')'){
				break;
			} else {
				if(ch>=0) {
					input.moveBack(1);
				}
				TurtleObject subobj=readObject(true);
				obj.getObjects().add(subobj);
			}
		}
		return obj;
	}

	private String readIriReference() throws IOException {
		StringBuilder ilist=new StringBuilder();
		while(true){
			int ch=input.read();
			if(ch<0)
				throw new ParserException();
			if(ch=='>'){
				String iriref=ilist.toString();
				// Resolve the IRI reference relative
				// to the base URI
				iriref=URIUtility.relativeResolve(iriref, baseURI);
				if(iriref==null)
					throw new ParserException();
				return iriref;
			}
			else if(ch=='\\'){
				ch=readUnicodeEscape(false);
			}
			if(ch<=0x20 || ((ch&0x7F)==ch && "><\\\"{}|^`".indexOf((char)ch)>=0))
				throw new ParserException();
			ilist.appendCodePoint(ch);
		}
	}

	private String readLanguageTag() throws IOException {
		StringBuilder ilist=new StringBuilder();
		boolean hyphen=false;
		boolean haveHyphen=false;
		boolean haveString=false;
		input.setSoftMark();
		while(true){
			int c2=input.read();
			if(c2>='A' && c2<='Z'){
				ilist.appendCodePoint(c2);
				haveString=true;
				hyphen=false;
			} else if(c2>='a' && c2<='z'){
				ilist.appendCodePoint(c2);
				haveString=true;
				hyphen=false;
			} else if(haveHyphen && (c2>='0' && c2<='9')){
				ilist.appendCodePoint(c2);
				haveString=true;
				hyphen=false;
			} else if(c2=='-'){
				if(hyphen||!haveString)throw new ParserException();
				ilist.appendCodePoint(c2);
				hyphen=true;
				haveHyphen=true;
				haveString=true;
			} else {
				if(c2>=0) {
					input.moveBack(1);
				}
				if(hyphen||!haveString)throw new ParserException();
				return ilist.toString();
			}
		}
	}

	// Reads a number literal starting with
	// the given character (assumes it's plus, minus,
	// a dot, or a digit)
	private RDFTerm readNumberLiteral(int ch) throws IOException {
		// buffer to hold the literal
		StringBuilder ilist=new StringBuilder();
		// include the first character
		ilist.appendCodePoint(ch);
		boolean haveDigits=(ch>='0' && ch<='9');
		boolean haveDot=(ch=='.');
		input.setHardMark();
		while(true){
			int ch1=input.read();
			if(haveDigits && (ch1=='e' || ch1=='E')){
				// Parse exponent
				ilist.appendCodePoint(ch1);
				ch1=input.read();
				haveDigits=false;
				if(ch1=='+' || ch1=='-' || (ch1>='0' && ch1<='9')){
					ilist.appendCodePoint(ch1);
					if(ch1>='0' && ch1<='9') {
						haveDigits=true;
					}
				} else
					throw new ParserException();
				input.setHardMark();
				while(true){
					ch1=input.read();
					if(ch1>='0' && ch1<='9'){
						haveDigits=true;
						ilist.appendCodePoint(ch1);
					} else {
						if(ch1>=0) {
							input.moveBack(1);
						}
						if(!haveDigits)throw new ParserException();
						return RDFTerm.fromTypedString(ilist.toString(),
								"http://www.w3.org/2001/XMLSchema#double");
					}
				}
			} else if(ch1>='0' && ch1<='9'){
				haveDigits=true;
				ilist.appendCodePoint(ch1);
			} else if(!haveDot && ch1=='.'){
				haveDot=true;
				// check for non-digit and non-E
				int markpos=input.getMarkPosition();
				int ch2=input.read();
				if(ch2!='e' && ch2!='E' && (ch2<'0' || ch2>'9')){
					// move to just at the period and return
					input.setMarkPosition(markpos-1);
					if(!haveDigits)
						throw new ParserException();
					return RDFTerm.fromTypedString(ilist.toString(),
							haveDot ? "http://www.w3.org/2001/XMLSchema#decimal" :
							"http://www.w3.org/2001/XMLSchema#integer");
				} else {
					input.moveBack(1);
				}
				ilist.appendCodePoint(ch1);
			} else { // no more digits
				if(ch1>=0) {
					input.moveBack(1);
				}
				if(!haveDigits)
					throw new ParserException();
				return RDFTerm.fromTypedString(ilist.toString(),
						haveDot ? "http://www.w3.org/2001/XMLSchema#decimal" :
						"http://www.w3.org/2001/XMLSchema#integer");
			}
		}
	}

	private TurtleObject readObject(boolean acceptLiteral) throws IOException {
		int ch=input.read();
		int mark=input.setSoftMark();
		if(ch<0)
			throw new ParserException();
		else if(ch=='<')
			return TurtleObject.fromTerm(
					RDFTerm.fromIRI(readIriReference()));
		else if(acceptLiteral && (ch=='-' || ch=='+' || ch=='.' || (ch>='0' && ch<='9')))
			return TurtleObject.fromTerm(readNumberLiteral(ch));
		else if(acceptLiteral && (ch=='\'' || ch=='\"')){ // start of quote literal
			String str=readStringLiteral(ch);
			return TurtleObject.fromTerm(finishStringLiteral(str));
		} else if(ch=='_'){ // Blank Node Label
			if(input.read()!=':')
				throw new ParserException();
			String label=readBlankNodeLabel();
			RDFTerm term=bnodeLabels.get(label);
			if(term==null){
				term=RDFTerm.fromBlankNode(label);
				bnodeLabels.put(label,term);
			}
			return TurtleObject.fromTerm(term);
		} else if(ch=='[')
			return readBlankNodePropertyList();
		else if(ch=='(')
			return readCollection();
		else if(ch==':'){ // prefixed name with current prefix
			String scope=namespaces.get("");
			if(scope==null)throw new ParserException();
			return TurtleObject.fromTerm(
					RDFTerm.fromIRI(scope+readOptionalLocalName()));
		} else if(isNameStartChar(ch)){ // prefix
			if(acceptLiteral && (ch=='t' || ch=='f')){
				mark=input.setHardMark();
				if(ch=='t' && input.read()=='r' && input.read()=='u' &&
						input.read()=='e' && skipWhitespace())
					return TurtleObject.fromTerm(RDFTerm.TRUE);
				else if(ch=='f' && input.read()=='a' && input.read()=='l' &&
						input.read()=='s' && input.read()=='e' && skipWhitespace())
					return TurtleObject.fromTerm(RDFTerm.FALSE);
				else {
					input.setMarkPosition(mark);
				}
			}
			String prefix=readPrefix(ch);
			String scope=namespaces.get(prefix);
			if(scope==null)throw new ParserException();
			return TurtleObject.fromTerm(
					RDFTerm.fromIRI(scope+readOptionalLocalName()));
		} else {
			input.setMarkPosition(mark);
			return null;
		}
	}



	private void readObjectList(Set<RDFTriple> triples) throws IOException {
		boolean haveObject=false;
		while(true){
			input.setSoftMark();
			int ch;
			if(haveObject){
				ch=input.read();
				if(ch!=','){
					if(ch>=0) {
						input.moveBack(1);
					}
					break;
				}
				skipWhitespace();
			}
			// Read object
			TurtleObject obj=readObject(true);
			if(obj==null){
				if(!haveObject)
					throw new ParserException();
				else
					return;
			}
			haveObject=true;
			emitRDFTriple(curSubject,curPredicate,obj,triples);
			skipWhitespace();
		}
		if(!haveObject)
			throw new ParserException();
		return;
	}


	private void readObjectListToProperties(
			RDFTerm predicate,
			TurtleObject propertyList
			) throws IOException {
		boolean haveObject=false;
		while(true){
			input.setSoftMark();
			int ch;
			if(haveObject){
				ch=input.read();
				if(ch!=','){
					if(ch>=0) {
						input.moveBack(1);
					}
					break;
				}
				skipWhitespace();
			}
			// Read object
			TurtleObject obj=readObject(true);
			if(obj==null){
				if(!haveObject)
					throw new ParserException();
				else
					return;
			}
			TurtleProperty prop=new TurtleProperty();
			prop.pred=predicate;
			prop.obj=obj;
			propertyList.getProperties().add(prop);
			skipWhitespace();
			haveObject=true;
		}
		if(!haveObject)
			throw new ParserException();
		return;
	}

	private String readOptionalLocalName() throws IOException {
		StringBuilder ilist=new StringBuilder();
		boolean lastIsPeriod=false;
		boolean first=true;
		input.setSoftMark();
		while(true){
			int ch=input.read();
			if(ch<0)
				return ilist.toString();
			if(ch=='%'){
				int a=input.read();
				int b=input.read();
				if(toHexValue(a)<0 ||
						toHexValue(b)<0)throw new ParserException();
				ilist.appendCodePoint(ch);
				ilist.appendCodePoint(a);
				ilist.appendCodePoint(b);
				lastIsPeriod=false;
				first=false;
				continue;
			} else if(ch=='\\'){
				ch=input.read();
				if(((ch&0x7F)==ch && "_~.-!$&'()*+,;=/?#@%".indexOf((char)ch)>=0)){
					ilist.appendCodePoint(ch);
				} else throw new ParserException();
				lastIsPeriod=false;
				first=false;
				continue;
			}
			if(first){
				if(!isNameStartCharU(ch) && ch!=':' && (ch<'0' || ch>'9')){
					input.moveBack(1);
					return ilist.toString();
				}
			} else {
				if(!isNameChar(ch) && ch!=':' && ch!='.'){
					input.moveBack(1);
					if(lastIsPeriod)throw new ParserException();
					return ilist.toString();
				}
			}
			lastIsPeriod=(ch=='.');
			if(lastIsPeriod && !first){
				// if a period was just read, check
				// if the next character is valid before
				// adding the period.
				int position=input.getMarkPosition();
				int ch2=input.read();
				if(!isNameChar(ch2) && ch2!=':' && ch2!='.'){
					input.setMarkPosition(position-1);
					return ilist.toString();
				} else {
					input.moveBack(1);
				}
			}
			first=false;
			ilist.appendCodePoint(ch);
		}

	}

	private RDFTerm readPredicate() throws IOException {
		int mark=input.setHardMark();
		int ch=input.read();
		RDFTerm predicate=null;
		if(ch=='a'){
			mark=input.setHardMark();
			if(skipWhitespace())
				return RDFTerm.A;
			else {
				input.setMarkPosition(mark);
				String prefix=readPrefix('a');
				String scope=namespaces.get(prefix);
				if(scope==null)throw new ParserException();
				predicate=RDFTerm.fromIRI(scope+readOptionalLocalName());
				skipWhitespace();
				return predicate;
			}
		} else if(ch=='<'){
			predicate=RDFTerm.fromIRI(readIriReference());
			skipWhitespace();
			return predicate;
		} else if(ch==':'){ // prefixed name with current prefix
			String scope=namespaces.get("");
			if(scope==null)throw new ParserException();
			predicate=RDFTerm.fromIRI(scope+readOptionalLocalName());
			skipWhitespace();
			return predicate;
		} else if(isNameStartChar(ch)){ // prefix
			String prefix=readPrefix(ch);
			String scope=namespaces.get(prefix);
			if(scope==null)throw new ParserException();
			predicate=RDFTerm.fromIRI(scope+readOptionalLocalName());
			skipWhitespace();
			return predicate;
		} else {
			input.setMarkPosition(mark);
			return null;
		}
	}


	private void readPredicateObjectList(Set<RDFTriple> triples) throws IOException {
		boolean havePredObject=false;
		while(true){
			int ch;
			skipWhitespace();
			if(havePredObject){
				boolean haveSemicolon=false;
				while(true){
					input.setSoftMark();
					ch=input.read();
					//DebugUtility.log("nextchar %c",(char)ch);
					if(ch==';'){
						skipWhitespace();
						haveSemicolon=true;
					} else {
						if(ch>=0) {
							input.moveBack(1);
						}
						break;
					}
				}
				if(!haveSemicolon) {
					break;
				}
			}
			curPredicate=readPredicate();
			//DebugUtility.log("predobjlist %s",curPredicate);
			if(curPredicate==null){
				if(!havePredObject)
					throw new ParserException();
				else {
					break;
				}
			}
			// Read object
			havePredObject=true;
			readObjectList(triples);
		}
		if(!havePredObject)
			throw new ParserException();
		return;
	}

	private String readPrefix(int startChar) throws IOException {
		StringBuilder ilist=new StringBuilder();
		boolean lastIsPeriod=false;
		boolean first=true;
		if(startChar>=0){
			ilist.appendCodePoint(startChar);
			first=false;
		}
		while(true){
			int ch=input.read();
			if(ch<0)
				throw new ParserException();
			if(ch==':'){
				if(lastIsPeriod)
					throw new ParserException();
				return ilist.toString();
			}
			else if(first && !isNameStartChar(ch))
				throw new ParserException();
			else if(ch!='.' && !isNameChar(ch))
				throw new ParserException();
			first=false;
			ilist.appendCodePoint(ch);
			lastIsPeriod=(ch=='.');
		}
	}

	private void readPrefixStatement(boolean sparql) throws IOException {
		String prefix=readPrefix(-1);
		skipWhitespace();
		if(input.read()!='<')
			throw new ParserException();
		String iri=readIriReference();
		namespaces.put(prefix, iri);
		if(!sparql){
			skipWhitespace();
			if(input.read()!='.')
				throw new ParserException();
		} else {
			skipWhitespace();
		}
	}
	private String readStringLiteral(int ch) throws IOException {
		StringBuilder ilist=new StringBuilder();
		boolean first=true;
		boolean longQuote=false;
		int quotecount=0;
		while(true){
			int c2=input.read();
			if(first && c2==ch){
				input.setHardMark();
				c2=input.read();
				if(c2!=ch){
					if(c2>=0) {
						input.moveBack(1);
					}
					return "";
				}
				longQuote=true;
				c2=input.read();
			}
			first=false;
			if(!longQuote && (c2==0x0a || c2==0x0d))
				throw new ParserException();
			else if(c2=='\\'){
				c2=readUnicodeEscape(true);
				if(quotecount>=2) {
					ilist.appendCodePoint(ch);
				}
				if(quotecount>=1) {
					ilist.appendCodePoint(ch);
				}
				ilist.appendCodePoint(c2);
				quotecount=0;
			} else if(c2==ch){
				if(!longQuote)
					return ilist.toString();
				quotecount++;
				if(quotecount>=3)
					return ilist.toString();
			} else {
				if(c2<0)
					throw new ParserException();
				if(quotecount>=2) {
					ilist.appendCodePoint(ch);
				}
				if(quotecount>=1) {
					ilist.appendCodePoint(ch);
				}
				ilist.appendCodePoint(c2);
				quotecount=0;
			}
		}
	}
	private void readTriples(Set<RDFTriple> triples) throws IOException {
		int mark=input.setHardMark();
		int ch=input.read();
		if(ch<0)
			return;
		input.setMarkPosition(mark);
		TurtleObject subject=readObject(false);
		if(subject==null)
			throw new ParserException();
		curSubject=subject;
		if(!(subject.kind==TurtleObject.PROPERTIES &&
				subject.getProperties().size()>0)){
			skipWhitespace();
			readPredicateObjectList(triples);
		} else {
			skipWhitespace();
			input.setHardMark();
			ch=input.read();
			if(ch=='.'){
				// just a blank node property list;
				// generate a blank node as the subject
				RDFTerm blankNode=allocateBlankNode();
				for(TurtleProperty prop : subject.getProperties()){
					emitRDFTriple(blankNode,prop.pred,prop.obj,triples);
				}
				return;
			} else if(ch<0)
				throw new ParserException();
			input.moveBack(1);
			readPredicateObjectList(triples);
		}
		skipWhitespace();
		if(input.read()!='.')
			throw new ParserException();
	}

	private int readUnicodeEscape(boolean extended) throws IOException {
		int ch=input.read();
		if(ch=='U'){
			if(input.read()!='0')
				throw new ParserException();
			if(input.read()!='0')
				throw new ParserException();
			int a=toHexValue(input.read());
			int b=toHexValue(input.read());
			int c=toHexValue(input.read());
			int d=toHexValue(input.read());
			int e=toHexValue(input.read());
			int f=toHexValue(input.read());
			if(a<0||b<0||c<0||d<0||e<0||f<0)
				throw new ParserException();
			ch=(a<<20)|(b<<16)|(c<<12)|(d<<8)|(e<<4)|(f);
		} else if(ch=='u'){
			int a=toHexValue(input.read());
			int b=toHexValue(input.read());
			int c=toHexValue(input.read());
			int d=toHexValue(input.read());
			if(a<0||b<0||c<0||d<0)
				throw new ParserException();
			ch=(a<<12)|(b<<8)|(c<<4)|(d);
		} else if(extended && ch=='t')
			return '\t';
		else if(extended && ch=='b')
			return '\b';
		else if(extended && ch=='n')
			return '\n';
		else if(extended && ch=='r')
			return '\r';
		else if(extended && ch=='f')
			return '\f';
		else if(extended && ch=='\'')
			return '\'';
		else if(extended && ch=='\\')
			return '\\';
		else if(extended && ch=='"')
			return '\"';
		else throw new ParserException();
		// Reject surrogate code points
		// as Unicode escapes
		if(ch>=0xD800 && ch<=0xDFFF)
			throw new ParserException();
		return ch;
	}

	private boolean skipWhitespace() throws IOException {
		boolean haveWhitespace=false;
		input.setSoftMark();
		while(true){
			int ch=input.read();
			if(ch=='#'){
				while(true){
					ch=input.read();
					if(ch<0)return true;
					if(ch==0x0d || ch==0x0a) {
						break;
					}
				}
			} else if(ch!=0x09 && ch!=0x0a && ch!=0x0d && ch!=0x20){
				if(ch>=0) {
					input.moveBack(1);
				}
				return haveWhitespace;
			}
			haveWhitespace=true;
		}
	}

	private int toHexValue(int a) {
		if(a>='0' && a<='9')return a-'0';
		if(a>='a' && a<='f')return a+10-'a';
		if(a>='A' && a<='F')return a+10-'A';
		return -1;
	}
}
