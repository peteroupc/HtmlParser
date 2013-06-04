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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.upokecenter.io.ICharacterInput;
import com.upokecenter.io.StackableCharacterInput;
import com.upokecenter.io.StringCharacterInput;

public final class NTriplesParser implements IRDFParser {


	public static class AsciiCharacterInput implements ICharacterInput {


		InputStream stream;

		public AsciiCharacterInput(InputStream stream){
			this.stream=stream;
		}

		@Override
		public int read() throws IOException {
			int c=stream.read();
			if(c>=0x80)throw new IOException("Invalid ASCII");
			return c;
		}

		@Override
		public int read(int[] buf, int offset, int unitCount)
				throws IOException {
			if((buf)==null)throw new NullPointerException("buf");
			if((offset)<0)throw new IndexOutOfBoundsException("offset"+" not greater or equal to "+"0"+" ("+Integer.toString(offset)+")");
			if((unitCount)<0)throw new IndexOutOfBoundsException("unitCount"+" not greater or equal to "+"0"+" ("+Integer.toString(unitCount)+")");
			if((offset+unitCount)>buf.length)throw new IndexOutOfBoundsException("offset+unitCount"+" not less or equal to "+Integer.toString(buf.length)+" ("+Integer.toString(offset+unitCount)+")");
			if(unitCount==0)return 0;
			for(int i=0;i<unitCount;i++){
				int c=read();
				if(c<0)
					return i==0 ? -1 : i;
				buf[offset++]=c;
			}
			return unitCount;
		}
	}

	public static boolean isAsciiChar(int c, String asciiChars){
		return (c>=0 && c<=0x7F && asciiChars.indexOf((char)c)>=0);
	}

	Map<String,RDFTerm> bnodeLabels;

	StackableCharacterInput input;

	public NTriplesParser(InputStream stream){
		if((stream)==null)throw new NullPointerException("stream");
		this.input=new StackableCharacterInput(
				new AsciiCharacterInput(stream));
		bnodeLabels=new HashMap<String,RDFTerm>();
	}

	public NTriplesParser(String str){
		if((str)==null)throw new NullPointerException("stream");
		this.input=new StackableCharacterInput(
				new StringCharacterInput(str));
		bnodeLabels=new HashMap<String,RDFTerm>();
	}


	private void endOfLine(int ch) throws IOException {
		if(ch==0x0a)
			return;
		else if(ch==0x0d){
			ch=input.read();
			if(ch!=0x0a && ch>=0){
				input.moveBack(1);
			}
		} else
			throw new ParserException();
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
			else throw new ParserException();
		} else {
			input.setMarkPosition(mark);
			return RDFTerm.fromTypedString(str);
		}
	}

	@Override
	public Set<RDFTriple> parse() throws IOException {
		Set<RDFTriple> rdf=new HashSet<RDFTriple>();
		while(true){
			skipWhitespace();
			input.setHardMark();
			int ch=input.read();
			if(ch<0)return rdf;
			if(ch=='#'){
				while(true){
					ch=input.read();
					if(ch==0x0a || ch==0x0d){
						endOfLine(ch);
						break;
					} else if(ch<0x20 || ch>0x7e)
						throw new ParserException();
				}
			} else if(ch==0x0a || ch==0x0d){
				endOfLine(ch);
			} else {
				input.moveBack(1);
				rdf.add(readTriples());
			}
		}
	}

	private String readBlankNodeLabel() throws IOException {
		StringBuilder ilist=new StringBuilder();
		int startChar=input.read();
		if(!((startChar>='A' && startChar<='Z') ||
				(startChar>='a' && startChar<='z')))
			throw new ParserException();
		ilist.appendCodePoint(startChar);
		input.setSoftMark();
		while(true){
			int ch=input.read();
			if((ch>='A' && ch<='Z') ||
					(ch>='a' && ch<='z') ||
					(ch>='0' && ch<='9')){
				ilist.appendCodePoint(ch);
			} else {
				if(ch>=0) {
					input.moveBack(1);
				}
				return ilist.toString();
			}
		}
	}

	private String readIriReference() throws IOException {
		StringBuilder ilist=new StringBuilder();
		boolean haveString=false;
		boolean colon=false;
		while(true){
			int c2=input.read();
			if((c2<=0x20 || c2>0x7e) || ((c2&0x7F)==c2 && "<\"{}|^`".indexOf((char)c2)>=0))
				throw new ParserException();
			else if(c2=='\\'){
				c2=readUnicodeEscape(true);
				if(c2<=0x20 || (c2>=0x7F && c2<=0x9F) || ((c2&0x7F)==c2 && "<\"{}|\\^`".indexOf((char)c2)>=0))
					throw new ParserException();
				if(c2==':') {
					colon=true;
				}
				ilist.appendCodePoint(c2);
				haveString=true;
			} else if(c2=='>'){
				if(!haveString || !colon)
					throw new ParserException();
				return ilist.toString();
			} else if(c2=='\"')
				// Should have been escaped
				throw new ParserException();
			else {
				if(c2==':') {
					colon=true;
				}
				ilist.appendCodePoint(c2);
				haveString=true;
			}
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
			if(c2>='a' && c2<='z'){
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

	private RDFTerm readObject(boolean acceptLiteral) throws IOException {
		int ch=input.read();
		if(ch<0)
			throw new ParserException();
		else if(ch=='<')
			return (RDFTerm.fromIRI(readIriReference()));
		else if(acceptLiteral && (ch=='\"')){ // start of quote literal
			String str=readStringLiteral(ch);
			return (finishStringLiteral(str));
		} else if(ch=='_'){ // Blank Node Label
			if(input.read()!=':')
				throw new ParserException();
			String label=readBlankNodeLabel();
			RDFTerm term=bnodeLabels.get(label);
			if(term==null){
				term=RDFTerm.fromBlankNode(label);
				bnodeLabels.put(label,term);
			}
			return (term);
		} else
			throw new ParserException();
	}
	private String readStringLiteral(int ch) throws IOException {
		StringBuilder ilist=new StringBuilder();
		while(true){
			int c2=input.read();
			if((c2<0x20 || c2>0x7e))
				throw new ParserException();
			else if(c2=='\\'){
				c2=readUnicodeEscape(true);
				ilist.appendCodePoint(c2);
			} else if(c2==ch)
				return ilist.toString();
			else {
				ilist.appendCodePoint(c2);
			}
		}
	}

	private RDFTriple readTriples() throws IOException {
		int mark=input.setHardMark();
		int ch=input.read();
		assert (ch>=0) : "ch>=0";
		input.setMarkPosition(mark);
		RDFTerm subject=readObject(false);
		if(!skipWhitespace())throw new ParserException();
		if(input.read()!='<')throw new ParserException();
		RDFTerm predicate=RDFTerm.fromIRI(readIriReference());
		if(!skipWhitespace())throw new ParserException();
		RDFTerm obj=readObject(true);
		skipWhitespace();
		if(input.read()!='.')throw new ParserException();
		skipWhitespace();
		RDFTriple ret=new RDFTriple(subject,predicate,obj);
		endOfLine(input.read());
		return ret;
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
			// NOTE: The following makes the code too strict
			//if(ch<0x10000)throw new ParserException();
		} else if(ch=='u'){
			int a=toHexValue(input.read());
			int b=toHexValue(input.read());
			int c=toHexValue(input.read());
			int d=toHexValue(input.read());
			if(a<0||b<0||c<0||d<0)
				throw new ParserException();
			ch=(a<<12)|(b<<8)|(c<<4)|(d);
			// NOTE: The following makes the code too strict
			//if(ch==0x09 || ch==0x0a || ch==0x0d ||
			//		(ch>=0x20 && ch<=0x7E))
			//	throw new ParserException();
		} else if(ch=='t')
			return '\t';
		else if(extended && ch=='n')
			return '\n';
		else if(extended && ch=='r')
			return '\r';
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
			if(ch!=0x09 && ch!=0x20){
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
