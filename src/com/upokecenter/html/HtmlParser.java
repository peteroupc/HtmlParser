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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.upokecenter.encoding.TextEncoding;
import com.upokecenter.io.ConditionalBufferInputStream;
import com.upokecenter.io.IMarkableCharacterInput;
import com.upokecenter.io.StackableCharacterInput;
import com.upokecenter.net.HeaderParser;
import com.upokecenter.util.IntList;
import com.upokecenter.util.StringUtility;
import com.upokecenter.util.URL;
final class HtmlParser {

	 static class CommentToken implements IToken {
		IntList value;
		public CommentToken(){
			value=new IntList();
		}

		public void appendChar(int ch){
			value.appendInt(ch);
		}

		@Override
		public int getType() {
			return TOKEN_COMMENT;
		}

		public String getValue(){
			return value.toString();
		}

	}
	 static class DocTypeToken implements IToken {
		public IntList name;
		public IntList publicID;
		public IntList systemID;
		public boolean forceQuirks;
		@Override
		public int getType() {
			return TOKEN_DOCTYPE;
		}
	}
	 static class EndTagToken extends TagToken {
		public EndTagToken(char c){
			super(c);
		}
		public EndTagToken(String name){
			super(name);
		}
		@Override
		public  int getType() {
			return TOKEN_END_TAG;
		}
	}
	private static class FormattingElement {
		public boolean marker;
		public Element element;
		public StartTagToken token;
		public boolean isMarker() {
			return marker;
		}
		@Override
		public String toString() {
			return "FormattingElement [marker=" + marker + ", token=" + token + "]\n";
		}
	}


	private enum InsertionMode {
		Initial,
		BeforeHtml,
		BeforeHead,
		InHead,
		InHeadNoscript,
		AfterHead,
		InBody,
		Text,
		InTable,
		InTableText,
		InCaption,
		InColumnGroup,
		InTableBody,
		InRow,
		InCell,
		InSelect,
		InSelectInTable,
		AfterBody,
		InFrameset,
		AfterFrameset,
		AfterAfterBody,
		AfterAfterFrameset
	}

	 interface IToken {
		public int getType();
	}

	 static class StartTagToken extends TagToken {
		public StartTagToken(char c){
			super(c);
		}
		public StartTagToken(String name) {
			super(name);
		}
		@Override
		public  int getType() {
			return TOKEN_START_TAG;
		}
		public void setName(String string) {
			builder.setLength(0);
			builder.append(string);
		}
	}
	 static abstract class TagToken implements IToken {

		protected StringBuilder builder;
		List<Attr> attributes=null;
		boolean selfClosing=false;
		boolean selfClosingAck=false;
		public TagToken(char ch){
			builder=new StringBuilder();
			builder.append(ch);
		}

		public TagToken(String name){
			builder=new StringBuilder();
			builder.append(name);
		}

		public void ackSelfClosing(){
			selfClosingAck=true;
		}

		public Attr addAttribute(char ch){
			if(attributes==null){
				attributes=new ArrayList<Attr>();
			}
			Attr a=new Attr(ch);
			attributes.add(a);
			return a;
		}

		public Attr addAttribute(int ch){
			if(attributes==null){
				attributes=new ArrayList<Attr>();
			}
			Attr a=new Attr(ch);
			attributes.add(a);
			return a;
		}

		public void append(int ch) {
			if(ch<0x10000){
				builder.append((char)ch);
			} else {
				ch-=0x10000;
				int lead=ch/0x400+0xd800;
				int trail=(ch&0x3FF)+0xdc00;
				builder.append((char)lead);
				builder.append((char)trail);
			}
		}

		public void appendChar(char ch) {
			builder.append(ch);
		}

		public boolean checkAttributeName(){
			if(attributes==null)return true;
			int size=attributes.size();
			if(size>=2){
				String thisname=attributes.get(size-1).getName();
				for(int i=0;i<size-1;i++){
					if(attributes.get(i).getName().equals(thisname)){
						// Attribute with this name already exists;
						// remove it
						attributes.remove(size-1);
						return false;
					}
				}
			}
			return true;
		}

		public String getAttribute(String name){
			if(attributes==null)return null;
			int size=attributes.size();
			for(int i=0;i<size;i++){
				IAttr a=attributes.get(i);
				String thisname=a.getName();
				if(thisname.equals(name))
					return a.getValue();
			}
			return null;
		}

		public String getAttributeNS(String name, String namespace){
			if(attributes==null)return null;
			int size=attributes.size();
			for(int i=0;i<size;i++){
				Attr a=attributes.get(i);
				if(a.isAttribute(name,namespace))
					return a.getValue();
			}
			return null;
		}


		public List<Attr> getAttributes(){
			if(attributes==null)
				return Arrays.asList(new Attr[0]);
			else
				return attributes;
		}

		public String getName(){
			return builder.toString();
		}

		@Override
		public abstract int getType();
		public boolean isAckSelfClosing() {
			return !selfClosing || selfClosingAck;
		}
		public boolean isSelfClosing() {
			return selfClosing;
		}

		public boolean isSelfClosingAck(){
			return selfClosingAck;
		}


		public void setAttribute(String attrname, String value) {
			if(attributes==null){
				attributes=new ArrayList<Attr>();
				attributes.add(new Attr(attrname,value));
			} else {
				int size=attributes.size();
				for(int i=0;i<size;i++){
					Attr a=attributes.get(i);
					String thisname=a.getName();
					if(thisname.equals(attrname)){
						a.setValue(value);
						return;
					}
				}
				attributes.add(new Attr(attrname,value));
			}
		}

		public void setSelfClosing(boolean selfClosing) {
			this.selfClosing = selfClosing;
		}
		@Override
		public String toString() {
			return "TagToken [" + builder.toString() + ", "
					+ attributes +(selfClosing ? (", selfClosingAck=" + selfClosingAck) : "") + "]";
		}

	}
	private enum TokenizerState {
		Data,
		CharacterRefInData,
		RcData,
		CharacterRefInRcData,
		RawText,
		ScriptData,
		PlainText,
		TagOpen,
		EndTagOpen,
		TagName,
		RcDataLessThan,
		RcDataEndTagOpen,
		RcDataEndTagName,
		RawTextLessThan,
		RawTextEndTagOpen,
		RawTextEndTagName,
		ScriptDataLessThan,
		ScriptDataEndTagOpen,
		ScriptDataEndTagName,
		ScriptDataEscapeStart,
		ScriptDataEscapeStartDash,
		ScriptDataEscaped,
		ScriptDataEscapedDash,
		ScriptDataEscapedDashDash,
		ScriptDataEscapedLessThan,
		ScriptDataEscapedEndTagOpen,
		ScriptDataEscapedEndTagName,
		ScriptDataDoubleEscapeStart,
		ScriptDataDoubleEscaped,
		ScriptDataDoubleEscapedDash,
		ScriptDataDoubleEscapedDashDash,
		ScriptDataDoubleEscapedLessThan,
		ScriptDataDoubleEscapeEnd,
		BeforeAttributeName,
		AttributeName,
		AfterAttributeName,
		BeforeAttributeValue,
		AttributeValueDoubleQuoted,
		AttributeValueSingleQuoted,
		AttributeValueUnquoted,
		CharacterRefInAttributeValue,
		AfterAttributeValueQuoted,
		SelfClosingStartTag,
		BogusComment,
		MarkupDeclarationOpen,
		CommentStart,
		CommentStartDash,
		Comment,
		CommentEndDash,
		CommentEnd,
		CommentEndBang,
		DocType,
		BeforeDocTypeName,
		DocTypeName,
		AfterDocTypeName,
		AfterDocTypePublic,
		BeforeDocTypePublicID,
		DocTypePublicIDDoubleQuoted,
		DocTypePublicIDSingleQuoted,
		AfterDocTypePublicID,
		BetweenDocTypePublicAndSystem,
		AfterDocTypeSystem,
		BeforeDocTypeSystemID,
		DocTypeSystemIDDoubleQuoted,
		DocTypeSystemIDSingleQuoted,
		AfterDocTypeSystemID,
		BogusDocType,
		CData
	}



	public static final String MATHML_NAMESPACE = "http://www.w3.org/1998/Math/MathML";

	public static final String SVG_NAMESPACE = "http://www.w3.org/2000/svg";


	 static int TOKEN_EOF= 0x10000000;

	 static int TOKEN_START_TAG= 0x20000000;

	 static int TOKEN_END_TAG= 0x30000000;

	 static int TOKEN_COMMENT=0x40000000;

	 static int TOKEN_DOCTYPE=0x50000000;
	 static int TOKEN_TYPE_MASK=0xF0000000;
	 static int TOKEN_CHARACTER=0x00000000;
	private static int TOKEN_INDEX_MASK=0x0FFFFFFF;
	public static final String HTML_NAMESPACE="http://www.w3.org/1999/xhtml";

	private static String[] quirksModePublicIdPrefixes=new String[]{
		"+//silmaril//dtd html pro v0r11 19970101//",
		"-//advasoft ltd//dtd html 3.0 aswedit + extensions//",
		"-//as//dtd html 3.0 aswedit + extensions//",
		"-//ietf//dtd html 2.0 level 1//",
		"-//ietf//dtd html 2.0 level 2//",
		"-//ietf//dtd html 2.0 strict level 1//",
		"-//ietf//dtd html 2.0 strict level 2//",
		"-//ietf//dtd html 2.0 strict//",
		"-//ietf//dtd html 2.0//",
		"-//ietf//dtd html 2.1e//",
		"-//ietf//dtd html 3.0//",
		"-//ietf//dtd html 3.2 final//",
		"-//ietf//dtd html 3.2//",
		"-//ietf//dtd html 3//",
		"-//ietf//dtd html level 0//",
		"-//ietf//dtd html level 1//",
		"-//ietf//dtd html level 2//",
		"-//ietf//dtd html level 3//",
		"-//ietf//dtd html strict level 0//",
		"-//ietf//dtd html strict level 1//",
		"-//ietf//dtd html strict level 2//",
		"-//ietf//dtd html strict level 3//",
		"-//ietf//dtd html strict//",
		"-//ietf//dtd html//",
		"-//metrius//dtd metrius presentational//",
		"-//microsoft//dtd internet explorer 2.0 html strict//",
		"-//microsoft//dtd internet explorer 2.0 html//",
		"-//microsoft//dtd internet explorer 2.0 tables//",
		"-//microsoft//dtd internet explorer 3.0 html strict//",
		"-//microsoft//dtd internet explorer 3.0 html//",
		"-//microsoft//dtd internet explorer 3.0 tables//",
		"-//netscape comm. corp.//dtd html//",
		"-//netscape comm. corp.//dtd strict html//",
		"-//o'reilly and associates//dtd html 2.0//",
		"-//o'reilly and associates//dtd html extended 1.0//",
		"-//o'reilly and associates//dtd html extended relaxed 1.0//",
		"-//softquad software//dtd hotmetal pro 6.0::19990601::extensions to html 4.0//",
		"-//softquad//dtd hotmetal pro 4.0::19971010::extensions to html 4.0//",
		"-//spyglass//dtd html 2.0 extended//",
		"-//sq//dtd html 2.0 hotmetal + extensions//",
		"-//sun microsystems corp.//dtd hotjava html//",
		"-//sun microsystems corp.//dtd hotjava strict html//",
		"-//w3c//dtd html 3 1995-03-24//",
		"-//w3c//dtd html 3.2 draft//",
		"-//w3c//dtd html 3.2 final//",
		"-//w3c//dtd html 3.2//",
		"-//w3c//dtd html 3.2s draft//",
		"-//w3c//dtd html 4.0 frameset//",
		"-//w3c//dtd html 4.0 transitional//",
		"-//w3c//dtd html experimental 19960712//",
		"-//w3c//dtd html experimental 970421//",
		"-//w3c//dtd w3 html//",
		"-//w3o//dtd w3 html 3.0//",
		"-//webtechs//dtd mozilla html 2.0//",
		"-//webtechs//dtd mozilla html//"
	};


	private final ConditionalBufferInputStream inputStream;
	private IMarkableCharacterInput charInput=null;
	private EncodingConfidence encoding=null;


	private boolean error=false;
	private TokenizerState lastState=TokenizerState.Data;
	private CommentToken lastComment;
	private DocTypeToken docTypeToken;
	private final List<Element> integrationElements=new ArrayList<Element>();
	private final List<IToken> tokens=new ArrayList<IToken>();
	private TagToken lastStartTag=null;
	private Html5Decoder decoder=null;
	private TagToken currentEndTag=null;
	private TagToken currentTag=null;
	private Attr currentAttribute=null;
	private int bogusCommentCharacter=0;
	private final IntList tempBuffer=new IntList();
	private TokenizerState state=TokenizerState.Data;
	private boolean framesetOk=true;
	private final List<Integer> tokenQueue=new ArrayList<Integer>();
	private InsertionMode insertionMode=InsertionMode.Initial;
	private InsertionMode originalInsertionMode=InsertionMode.Initial;
	private final List<Element> openElements=new ArrayList<Element>();
	private final List<FormattingElement> formattingElements=new ArrayList<FormattingElement>();
	private Element headElement=null;
	private Element formElement=null;
	private Element inputElement=null;
	private String baseurl=null;
	private boolean hasForeignContent=false;
	 Document document=null;
	private boolean done=false;

	private final IntList pendingTableCharacters=new IntList();
	private boolean doFosterParent;
	private Element context;
	private boolean noforeign;
	private final String address;

	private final String[] contentLanguage;

	public static final String XLINK_NAMESPACE="http://www.w3.org/1999/xlink";

	public static final String XML_NAMESPACE="http://www.w3.org/XML/1998/namespace";
	private static final String XMLNS_NAMESPACE="http://www.w3.org/2000/xmlns/";


	private static <T> T removeAtIndex(List<T> array, int index){
		T ret=array.get(index);
		array.remove(index);
		return ret;
	}

	public HtmlParser(InputStream s, String address) throws IOException {
		this(s,address,null,null);
	}

	public HtmlParser(InputStream s, String address, String charset) throws IOException {
		this(s,address,charset,null);
	}


	public HtmlParser(InputStream source, String address,
			String charset, String contentLanguage) throws IOException{
		if(source==null)throw new IllegalArgumentException();
		if(address!=null && address.length()>0){
			URL url=URL.parse(address);
			if(url==null || url.getScheme().length()==0)
				throw new IllegalArgumentException();
		}
		this.contentLanguage=HeaderParser.getLanguages(contentLanguage);
		this.address=address;
		initialize();
		inputStream=new ConditionalBufferInputStream(source);
		encoding=CharsetSniffer.sniffEncoding(inputStream,charset);
		inputStream.rewind();
		decoder=new Html5Decoder(TextEncoding.getDecoder(encoding.getEncoding()));
		charInput=new StackableCharacterInput(new DecoderCharacterInput(inputStream,decoder));
	}


	private void addCommentNodeToCurrentNode(int token){
		insertInCurrentNode(createCommentNode(token));
	}

	private void addCommentNodeToDocument(int token){
		document.appendChild(createCommentNode(token));
	}

	private void addCommentNodeToFirst(int token){
		openElements.get(0).appendChild(createCommentNode(token));
	}

	private Element addHtmlElement(StartTagToken tag){
		Element element=Element.fromToken(tag);
		Element currentNode=getCurrentNode();
		if(currentNode!=null) {
			insertInCurrentNode(element);
		} else {
			document.appendChild(element);
		}
		openElements.add(element);
		return element;
	}

	private Element addHtmlElementNoPush(StartTagToken tag){
		Element element=Element.fromToken(tag);
		Element currentNode=getCurrentNode();
		if(currentNode!=null) {
			insertInCurrentNode(element);
		}
		return element;
	}

	private void adjustForeignAttributes(StartTagToken token){
		List<Attr> attributes=token.getAttributes();
		for(Attr attr : attributes){
			String name=attr.getName();
			if(name.equals("xlink:actuate") ||
					name.equals("xlink:arcrole") ||
					name.equals("xlink:href") ||
					name.equals("xlink:role") ||
					name.equals("xlink:show") ||
					name.equals("xlink:title") ||
					name.equals("xlink:type")
					){
				attr.setNamespace(XLINK_NAMESPACE);
			}
			else if(name.equals("xml:base") ||
					name.equals("xml:lang") ||
					name.equals("xml:space")
					){
				attr.setNamespace(XML_NAMESPACE);
			}
			else if(name.equals("xmlns") ||
					name.equals("xmlns:xlink")){
				attr.setNamespace(XMLNS_NAMESPACE);
			}
		}
	}


	private void adjustMathMLAttributes(StartTagToken token){
		List<Attr> attributes=token.getAttributes();
		for(Attr attr : attributes){
			if(attr.getName().equals("definitionurl")){
				attr.setName("definitionURL");
			}
		}
	}


	private void adjustSvgAttributes(StartTagToken token){
		List<Attr> attributes=token.getAttributes();
		for(Attr attr : attributes){
			String name=attr.getName();
			if(name.equals("attributename")){ attr.setName("attributeName"); }
			else if(name.equals("attributetype")){ attr.setName("attributeType");  }
			else if(name.equals("basefrequency")){ attr.setName("baseFrequency");  }
			else if(name.equals("baseprofile")){ attr.setName("baseProfile");  }
			else if(name.equals("calcmode")){ attr.setName("calcMode");  }
			else if(name.equals("clippathunits")){ attr.setName("clipPathUnits");  }
			else if(name.equals("contentscripttype")){ attr.setName("contentScriptType");  }
			else if(name.equals("contentstyletype")){ attr.setName("contentStyleType");  }
			else if(name.equals("diffuseconstant")){ attr.setName("diffuseConstant");  }
			else if(name.equals("edgemode")){ attr.setName("edgeMode");  }
			else if(name.equals("externalresourcesrequired")){ attr.setName("externalResourcesRequired");  }
			else if(name.equals("filterres")){ attr.setName("filterRes");  }
			else if(name.equals("filterunits")){ attr.setName("filterUnits");  }
			else if(name.equals("glyphref")){ attr.setName("glyphRef");  }
			else if(name.equals("gradienttransform")){ attr.setName("gradientTransform");  }
			else if(name.equals("gradientunits")){ attr.setName("gradientUnits");  }
			else if(name.equals("kernelmatrix")){ attr.setName("kernelMatrix");  }
			else if(name.equals("kernelunitlength")){ attr.setName("kernelUnitLength");  }
			else if(name.equals("keypoints")){ attr.setName("keyPoints");  }
			else if(name.equals("keysplines")){ attr.setName("keySplines");  }
			else if(name.equals("keytimes")){ attr.setName("keyTimes");  }
			else if(name.equals("lengthadjust")){ attr.setName("lengthAdjust");  }
			else if(name.equals("limitingconeangle")){ attr.setName("limitingConeAngle");  }
			else if(name.equals("markerheight")){ attr.setName("markerHeight");  }
			else if(name.equals("markerunits")){ attr.setName("markerUnits");  }
			else if(name.equals("markerwidth")){ attr.setName("markerWidth");  }
			else if(name.equals("maskcontentunits")){ attr.setName("maskContentUnits");  }
			else if(name.equals("maskunits")){ attr.setName("maskUnits");  }
			else if(name.equals("numoctaves")){ attr.setName("numOctaves");  }
			else if(name.equals("pathlength")){ attr.setName("pathLength");  }
			else if(name.equals("patterncontentunits")){ attr.setName("patternContentUnits");  }
			else if(name.equals("patterntransform")){ attr.setName("patternTransform");  }
			else if(name.equals("patternunits")){ attr.setName("patternUnits");  }
			else if(name.equals("pointsatx")){ attr.setName("pointsAtX");  }
			else if(name.equals("pointsaty")){ attr.setName("pointsAtY");  }
			else if(name.equals("pointsatz")){ attr.setName("pointsAtZ");  }
			else if(name.equals("preservealpha")){ attr.setName("preserveAlpha");  }
			else if(name.equals("preserveaspectratio")){ attr.setName("preserveAspectRatio");  }
			else if(name.equals("primitiveunits")){ attr.setName("primitiveUnits");  }
			else if(name.equals("refx")){ attr.setName("refX");  }
			else if(name.equals("refy")){ attr.setName("refY");  }
			else if(name.equals("repeatcount")){ attr.setName("repeatCount");  }
			else if(name.equals("repeatdur")){ attr.setName("repeatDur");  }
			else if(name.equals("requiredextensions")){ attr.setName("requiredExtensions");  }
			else if(name.equals("requiredfeatures")){ attr.setName("requiredFeatures");  }
			else if(name.equals("specularconstant")){ attr.setName("specularConstant");  }
			else if(name.equals("specularexponent")){ attr.setName("specularExponent");  }
			else if(name.equals("spreadmethod")){ attr.setName("spreadMethod");  }
			else if(name.equals("startoffset")){ attr.setName("startOffset");  }
			else if(name.equals("stddeviation")){ attr.setName("stdDeviation");  }
			else if(name.equals("stitchtiles")){ attr.setName("stitchTiles");  }
			else if(name.equals("surfacescale")){ attr.setName("surfaceScale");  }
			else if(name.equals("systemlanguage")){ attr.setName("systemLanguage");  }
			else if(name.equals("tablevalues")){ attr.setName("tableValues");  }
			else if(name.equals("targetx")){ attr.setName("targetX");  }
			else if(name.equals("targety")){ attr.setName("targetY");  }
			else if(name.equals("textlength")){ attr.setName("textLength");  }
			else if(name.equals("viewbox")){ attr.setName("viewBox");  }
			else if(name.equals("viewtarget")){ attr.setName("viewTarget");  }
			else if(name.equals("xchannelselector")){ attr.setName("xChannelSelector");  }
			else if(name.equals("ychannelselector")){ attr.setName("yChannelSelector");  }
			else if(name.equals("zoomandpan")){ attr.setName("zoomAndPan");  }
		}
	}

	private boolean applyEndTag(String name, InsertionMode insMode) throws IOException{
		return applyInsertionMode(getArtificialToken(TOKEN_END_TAG,name),insMode);
	}

	private boolean applyForeignContext(int token) throws IOException{
		if(token==0){
			error=true;
			insertCharacter(getCurrentNode(),0xFFFD);
			return true;
		} else if((token&TOKEN_TYPE_MASK)==TOKEN_CHARACTER){
			insertCharacter(getCurrentNode(),token);
			if(token!=0x09 && token!=0x0c && token!=0x0a &&
					token!=0x0d && token!=0x20){
				framesetOk=false;
			}
			return true;
		} else if((token&TOKEN_TYPE_MASK)==TOKEN_COMMENT){
			addCommentNodeToCurrentNode(token);
		} else if((token&TOKEN_TYPE_MASK)==TOKEN_DOCTYPE){
			error=true;
			return false;
		} else if((token&TOKEN_TYPE_MASK)==TOKEN_START_TAG){
			StartTagToken tag=(StartTagToken)getToken(token);
			String name=tag.getName();
			if(name.equals("font")){
				if(tag.getAttribute("color")!=null ||
						tag.getAttribute("size")!=null ||
						tag.getAttribute("face")!=null){
					error=true;
					while(true){
						popCurrentNode();
						Element node=getCurrentNode();
						if(node.getNamespaceURI().equals(HTML_NAMESPACE) ||
								isMathMLTextIntegrationPoint(node) ||
								isHtmlIntegrationPoint(node)){
							break;
						}
					}
					return applyInsertionMode(token,null);
				}
			} else if(name.equals("b") ||
					name.equals("big") || name.equals("blockquote") || name.equals("body") || name.equals("br") ||
					name.equals("center") || name.equals("code") || name.equals("dd") || name.equals("div") ||
					name.equals("dl") || name.equals("dt") || name.equals("em") || name.equals("embed") ||
					name.equals("h1") || name.equals("h2") || name.equals("h3") || name.equals("h4") ||
					name.equals("h5") || name.equals("h6") || name.equals("head") || name.equals("hr") ||
					name.equals("i") || name.equals("img") || name.equals("li") || name.equals("listing") ||
					name.equals("menu") || name.equals("meta") || name.equals("nobr") || name.equals("ol") ||
					name.equals("p") || name.equals("pre") || name.equals("ruby") || name.equals("s") ||
					name.equals("small") || name.equals("span") || name.equals("strong") || name.equals("strike") ||
					name.equals("sub") || name.equals("sup") || name.equals("table") || name.equals("tt") ||
					name.equals("u") || name.equals("ul") || name.equals("var")){
				error=true;
				if(context!=null && !hasNativeElementInScope()){
					noforeign=true;
					boolean ret=applyInsertionMode(token,InsertionMode.InBody);
					noforeign=false;
					return ret;
				}
				while(true){
					popCurrentNode();
					Element node=getCurrentNode();
					if(node.getNamespaceURI().equals(HTML_NAMESPACE) ||
							isMathMLTextIntegrationPoint(node) ||
							isHtmlIntegrationPoint(node)){
						break;
					}
				}
				return applyInsertionMode(token,null);
			} else {
				String namespace=getCurrentNode().getNamespaceURI();
				boolean mathml=false;
				if(SVG_NAMESPACE.equals(namespace)){
					if(name.equals("altglyph")) {
						tag.setName("altGlyph");
					} else if(name.equals("altglyphdef")) {
						tag.setName("altGlyphDef");
					} else if(name.equals("altglyphitem")) {
						tag.setName("altGlyphItem");
					} else if(name.equals("animatecolor")) {
						tag.setName("animateColor");
					} else if(name.equals("animatemotion")) {
						tag.setName("animateMotion");
					} else if(name.equals("animatetransform")) {
						tag.setName("animateTransform");
					} else if(name.equals("clippath")) {
						tag.setName("clipPath");
					} else if(name.equals("feblend")) {
						tag.setName("feBlend");
					} else if(name.equals("fecolormatrix")) {
						tag.setName("feColorMatrix");
					} else if(name.equals("fecomponenttransfer")) {
						tag.setName("feComponentTransfer");
					} else if(name.equals("fecomposite")) {
						tag.setName("feComposite");
					} else if(name.equals("feconvolvematrix")) {
						tag.setName("feConvolveMatrix");
					} else if(name.equals("fediffuselighting")) {
						tag.setName("feDiffuseLighting");
					} else if(name.equals("fedisplacementmap")) {
						tag.setName("feDisplacementMap");
					} else if(name.equals("fedistantlight")) {
						tag.setName("feDistantLight");
					} else if(name.equals("feflood")) {
						tag.setName("feFlood");
					} else if(name.equals("fefunca")) {
						tag.setName("feFuncA");
					} else if(name.equals("fefuncb")) {
						tag.setName("feFuncB");
					} else if(name.equals("fefuncg")) {
						tag.setName("feFuncG");
					} else if(name.equals("fefuncr")) {
						tag.setName("feFuncR");
					} else if(name.equals("fegaussianblur")) {
						tag.setName("feGaussianBlur");
					} else if(name.equals("feimage")) {
						tag.setName("feImage");
					} else if(name.equals("femerge")) {
						tag.setName("feMerge");
					} else if(name.equals("femergenode")) {
						tag.setName("feMergeNode");
					} else if(name.equals("femorphology")) {
						tag.setName("feMorphology");
					} else if(name.equals("feoffset")) {
						tag.setName("feOffset");
					} else if(name.equals("fepointlight")) {
						tag.setName("fePointLight");
					} else if(name.equals("fespecularlighting")) {
						tag.setName("feSpecularLighting");
					} else if(name.equals("fespotlight")) {
						tag.setName("feSpotLight");
					} else if(name.equals("fetile")) {
						tag.setName("feTile");
					} else if(name.equals("feturbulence")) {
						tag.setName("feTurbulence");
					} else if(name.equals("foreignobject")) {
						tag.setName("foreignObject");
					} else if(name.equals("glyphref")) {
						tag.setName("glyphRef");
					} else if(name.equals("lineargradient")) {
						tag.setName("linearGradient");
					} else if(name.equals("radialgradient")) {
						tag.setName("radialGradient");
					} else if(name.equals("textpath")) {
						tag.setName("textPath");
					}
					adjustSvgAttributes(tag);
				} else if(MATHML_NAMESPACE.equals(namespace)){
					adjustMathMLAttributes(tag);
					mathml=true;
				}
				adjustForeignAttributes(tag);
				Element e=insertForeignElement(tag,namespace);
				if(mathml && tag.getName().equals("annotation-xml")){
					String encoding=tag.getAttribute("encoding");
					if(encoding!=null){
						encoding=StringUtility.toLowerCaseAscii(encoding);
						if(encoding.equals("text/html") ||
								encoding.equals("application/xhtml+xml")){
							integrationElements.add(e);
						}
					}
				}
				if(tag.isSelfClosing()){
					if(name.equals("script")){
						tag.ackSelfClosing();
						applyEndTag("script",null);
					} else {
						popCurrentNode();
						tag.ackSelfClosing();
					}
				}
				return true;
			}
			return false;
		} else if((token&TOKEN_TYPE_MASK)==TOKEN_END_TAG){
			EndTagToken tag=(EndTagToken)getToken(token);
			String name=tag.getName();
			if(name.equals("script") &&
					getCurrentNode().getLocalName().equals("script") &&
					SVG_NAMESPACE.equals(getCurrentNode().getNamespaceURI())){
				popCurrentNode();
			} else {
				// NOTE: The HTML spec here is unfortunately too strict
				// in that it doesn't allow an ASCII case-insensitive
				// comparison (for example, with SVG foreignObject)
				if(!getCurrentNode().getLocalName().equals(name)) {
					error=true;
				}
				int originalSize=openElements.size();
				for(int i1=originalSize-1;i1>=0;i1--){
					if(i1==0)
						return true;
					Element node=openElements.get(i1);
					if(i1<originalSize-1 &&
							HTML_NAMESPACE.equals(node.getNamespaceURI())){
						noforeign=true;
						return applyInsertionMode(token,null);
					}
					String nodeName=StringUtility.toLowerCaseAscii(node.getLocalName());
					if(name.equals(nodeName)){
						while(true){
							Element node2=popCurrentNode();
							if(node2.equals(node)) {
								break;
							}
						}
						break;
					}
				}
			}
			return false;
		} else if(token==TOKEN_EOF)
			return applyInsertionMode(token,null);
		return true;
	}

	private boolean applyInsertionMode(int token, InsertionMode insMode) throws IOException{
		//DebugUtility.log("[[%08X %s %s %s(%s)",token,getToken(token),insMode==null ? insertionMode :
		//insMode,isForeignContext(token),noforeign);
		if(!noforeign && isForeignContext(token))
			return applyForeignContext(token);
		noforeign=false;
		if(insMode==null) {
			insMode=insertionMode;
		}
		switch(insMode){
		case Initial:{
			if(token==0x09 || token==0x0a ||
					token==0x0c || token==0x0d ||
					token==0x20)
				return false;
			if((token&TOKEN_TYPE_MASK)==TOKEN_DOCTYPE){
				DocTypeToken doctype=(DocTypeToken)getToken(token);
				String doctypeName=(doctype.name==null) ? "" : doctype.name.toString();
				String doctypePublic=(doctype.publicID==null) ? null : doctype.publicID.toString();
				String doctypeSystem=(doctype.systemID==null) ? null : doctype.systemID.toString();
				boolean matchesHtml="html".equals(doctypeName);
				boolean hasSystemId=(doctype.systemID!=null);
				if(!matchesHtml || doctypePublic!=null ||
						(doctypeSystem!=null && !"about:legacy-compat".equals(doctypeSystem))){
					boolean html4=(matchesHtml && "-//W3C//DTD HTML 4.0//EN".equals(doctypePublic) &&
							(doctypeSystem==null || "http://www.w3.org/TR/REC-html40/strict.dtd".equals(doctypeSystem)));
					boolean html401=(matchesHtml && "-//W3C//DTD HTML 4.01//EN".equals(doctypePublic) &&
							(doctypeSystem==null || "http://www.w3.org/TR/html4/strict.dtd".equals(doctypeSystem)));
					boolean xhtml=(matchesHtml && "-//W3C//DTD XHTML 1.0 Strict//EN".equals(doctypePublic) &&
							("http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd".equals(doctypeSystem)));
					boolean xhtml11=(matchesHtml && "-//W3C//DTD XHTML 1.1//EN".equals(doctypePublic) &&
							("http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd".equals(doctypeSystem)));
					if(!html4 && !html401 && !xhtml && !xhtml11){
						error=true;
					}
				}
				if(doctypePublic==null) {
					doctypePublic="";
				}
				if(doctypeSystem==null) {
					doctypeSystem="";
				}
				DocumentType doctypeNode=new DocumentType();
				doctypeNode.name=doctypeName;
				doctypeNode.publicId=doctypePublic;
				doctypeNode.systemId=doctypeSystem;
				document.doctype=doctypeNode;
				document.appendChild(doctypeNode);
				String doctypePublicLC=null;
				if(!matchesHtml||doctype.forceQuirks){
					document.setMode(DocumentMode.QuirksMode);
				}
				else {
					doctypePublicLC=StringUtility.toLowerCaseAscii(doctypePublic);
					if("html".equals(doctypePublicLC) ||
							"-//w3o//dtd w3 html strict 3.0//en//".equals(doctypePublicLC) ||
							"-/w3c/dtd html 4.0 transitional/en".equals(doctypePublicLC)
							){
						document.setMode(DocumentMode.QuirksMode);
					}
					else if(doctypePublic.length()>0){
						for(String id : quirksModePublicIdPrefixes){
							if(doctypePublicLC.startsWith(id)){
								document.setMode(DocumentMode.QuirksMode);
								break;
							}
						}
					}
				}
				if(document.getMode()!=DocumentMode.QuirksMode){
					if(doctypePublicLC==null) {
						doctypePublicLC=StringUtility.toLowerCaseAscii(doctypePublic);
					}
					if("http://www.ibm.com/data/dtd/v11/ibmxhtml1-transitional.dtd".equals(
							StringUtility.toLowerCaseAscii(doctypeSystem)) ||
							(!hasSystemId && doctypePublicLC.startsWith("-//w3c//dtd html 4.01 frameset//")) ||
							(!hasSystemId && doctypePublicLC.startsWith("-//w3c//dtd html 4.01 transitional//"))){
						document.setMode(DocumentMode.QuirksMode);
					}
				}
				if(document.getMode()!=DocumentMode.QuirksMode){
					if(doctypePublicLC==null) {
						doctypePublicLC=StringUtility.toLowerCaseAscii(doctypePublic);
					}
					if(doctypePublicLC.startsWith("-//w3c//dtd xhtml 1.0 frameset//") ||
							doctypePublicLC.startsWith("-//w3c//dtd xhtml 1.0 transitional//") ||
							(hasSystemId && doctypePublicLC.startsWith("-//w3c//dtd html 4.01 frameset//")) ||
							(hasSystemId && doctypePublicLC.startsWith("-//w3c//dtd html 4.01 transitional//"))){
						document.setMode(DocumentMode.LimitedQuirksMode);
					}
				}
				insertionMode=InsertionMode.BeforeHtml;
				return true;
			}
			if((token&TOKEN_TYPE_MASK)==TOKEN_COMMENT){
				addCommentNodeToDocument(token);

				return true;
			}
			if(!"about:srcdoc".equals(document.address)){
				error=true;
				document.setMode(DocumentMode.QuirksMode);
			}
			insertionMode=InsertionMode.BeforeHtml;
			return applyInsertionMode(token,null);
		}
		case BeforeHtml:{
			if((token&TOKEN_TYPE_MASK)==TOKEN_DOCTYPE){
				error=true;
				return false;
			}
			if(token==0x09 || token==0x0a ||
					token==0x0c || token==0x0d ||
					token==0x20)
				return false;
			if((token&TOKEN_TYPE_MASK)==TOKEN_COMMENT){
				addCommentNodeToDocument(token);

				return true;
			} else if((token&TOKEN_TYPE_MASK)==TOKEN_START_TAG){
				StartTagToken tag=(StartTagToken)getToken(token);
				String name=tag.getName();
				if("html".equals(name)){
					addHtmlElement(tag);
					insertionMode=InsertionMode.BeforeHead;
					return true;
				}
			} else if((token&TOKEN_TYPE_MASK)==TOKEN_END_TAG){
				TagToken tag=(TagToken)getToken(token);
				String name=tag.getName();
				if(!"html".equals(name) && !"br".equals(name) &&
						!"head".equals(name) && !"body".equals(name)){
					error=true;
					return false;
				}
			}
			Element element=new Element();
			element.setLocalName("html");
			element.setNamespace(HTML_NAMESPACE);
			document.appendChild(element);
			openElements.add(element);
			insertionMode=InsertionMode.BeforeHead;
			return applyInsertionMode(token,null);
		}
		case BeforeHead:{
			if(token==0x09 || token==0x0a ||
					token==0x0c || token==0x0d ||
					token==0x20)
				return false;
			if((token&TOKEN_TYPE_MASK)==TOKEN_COMMENT){
				addCommentNodeToCurrentNode(token);
				return true;
			}
			if((token&TOKEN_TYPE_MASK)==TOKEN_DOCTYPE){
				error=true;
				return false;
			}
			if((token&TOKEN_TYPE_MASK)==TOKEN_START_TAG){
				StartTagToken tag=(StartTagToken)getToken(token);
				String name=tag.getName();
				if("html".equals(name)){
					applyInsertionMode(token,InsertionMode.InBody);
					return true;
				} else if("head".equals(name)){
					Element element=addHtmlElement(tag);
					headElement=element;
					insertionMode=InsertionMode.InHead;
					return true;
				}
			} else if((token&TOKEN_TYPE_MASK)==TOKEN_END_TAG){
				TagToken tag=(TagToken)getToken(token);
				String name=tag.getName();
				if("head".equals(name) ||
						"br".equals(name) ||
						"body".equals(name) ||
						"html".equals(name)){
					applyStartTag("head",insMode);
					return applyInsertionMode(token,null);
				} else {
					error=true;
					return false;
				}
			}
			applyStartTag("head",insMode);
			return applyInsertionMode(token,null);
		}
		case InHead:{
			if((token&TOKEN_TYPE_MASK)==TOKEN_COMMENT){
				addCommentNodeToCurrentNode(token);
				return true;
			}
			if((token&TOKEN_TYPE_MASK)==TOKEN_DOCTYPE){
				error=true;
				return false;
			}
			if(token==0x09 || token==0x0a ||
					token==0x0c || token==0x0d ||
					token==0x20){
				insertCharacter(getCurrentNode(),token);
				return true;
			}
			if((token&TOKEN_TYPE_MASK)==TOKEN_START_TAG){
				StartTagToken tag=(StartTagToken)getToken(token);
				String name=tag.getName();
				if("html".equals(name)){
					applyInsertionMode(token,InsertionMode.InBody);
					return true;
				} else if("base".equals(name)||
						"bgsound".equals(name)||
						"basefont".equals(name)||
						"link".equals(name)){
					Element e=addHtmlElementNoPush(tag);
					if(baseurl==null && "base".equals(name)){
						// Get the document base URL
						baseurl=e.getAttribute("href");
					}
					tag.ackSelfClosing();
					return true;
				} else if("meta".equals(name)){
					Element element=addHtmlElementNoPush(tag);
					tag.ackSelfClosing();
					if(encoding.getConfidence()==EncodingConfidence.Tentative){
						String charset=element.getAttribute("charset");
						if(charset!=null){
							charset=TextEncoding.resolveEncoding(charset);
							if(TextEncoding.isAsciiCompatible(charset) ||
									"utf-16be".equals(charset) ||
									"utf-16le".equals(charset)){
								changeEncoding(charset);
								if(encoding.getConfidence()==EncodingConfidence.Certain){
									inputStream.disableBuffer();
								}
								return true;
							}
						}
						String value=StringUtility.toLowerCaseAscii(
								element.getAttribute("http-equiv"));
						if("content-type".equals(value)){
							value=element.getAttribute("content");
							if(value!=null){
								value=StringUtility.toLowerCaseAscii(value);
								charset=CharsetSniffer.extractCharsetFromMeta(value);
								if(TextEncoding.isAsciiCompatible(charset) ||
										"utf-16be".equals(charset) ||
										"utf-16le".equals(charset)){
									changeEncoding(charset);
									if(encoding.getConfidence()==EncodingConfidence.Certain){
										inputStream.disableBuffer();
									}
									return true;
								}
							}
						} else if("content-language".equals(value)){
							// HTML5 requires us to use this algorithm
							// to parse the Content-Language, rather than
							// use HTTP parsing (with HeaderParser.getLanguages)
							// NOTE: this pragma is non-conforming
							value=element.getAttribute("content");
							if(!StringUtility.isNullOrEmpty(value) &&
									value.indexOf(',')<0){
								String[] data=StringUtility.splitAtSpaces(value);
								document.defaultLanguage=(data.length==0) ? "" : data[0];
							}
						}

					}
					if(encoding.getConfidence()==EncodingConfidence.Certain){
						inputStream.disableBuffer();
					}
					return true;
				} else if("title".equals(name)){
					addHtmlElement(tag);
					state=TokenizerState.RcData;
					originalInsertionMode=insertionMode;
					insertionMode=InsertionMode.Text;
					return true;
				} else if("noframes".equals(name) ||
						"style".equals(name)){
					addHtmlElement(tag);
					state=TokenizerState.RawText;
					originalInsertionMode=insertionMode;
					insertionMode=InsertionMode.Text;
					return true;
				} else if("noscript".equals(name)){
					addHtmlElement(tag);
					insertionMode=InsertionMode.InHeadNoscript;
					return true;
				} else if("script".equals(name)){
					addHtmlElement(tag);
					state=TokenizerState.ScriptData;
					originalInsertionMode=insertionMode;
					insertionMode=InsertionMode.Text;
					return true;
				} else if("head".equals(name)){
					error=true;
					return false;
				} else {
					applyEndTag("head",insMode);
					return applyInsertionMode(token,null);
				}
			} else if((token&TOKEN_TYPE_MASK)==TOKEN_END_TAG){
				TagToken tag=(TagToken)getToken(token);
				String name=tag.getName();
				if("head".equals(name)){
					openElements.remove(openElements.size()-1);
					insertionMode=InsertionMode.AfterHead;
					return true;
				} else if(!(
						"br".equals(name) ||
						"body".equals(name) ||
						"html".equals(name))){
					error=true;
					return false;
				}
				applyEndTag("head",insMode);
				return applyInsertionMode(token,null);
			} else {
				applyEndTag("head",insMode);
				return applyInsertionMode(token,null);
			}
		}
		case AfterHead:{
			if((token&TOKEN_TYPE_MASK)==TOKEN_CHARACTER){
				if(token==0x20 || token==0x09 || token==0x0a ||
						token==0x0c || token==0x0d){
					insertCharacter(getCurrentNode(),token);
				} else {
					applyStartTag("body",insMode);
					framesetOk=true;
					return applyInsertionMode(token,null);
				}
			} else if((token&TOKEN_TYPE_MASK)==TOKEN_DOCTYPE){
				error=true;
				return false;
			} else if((token&TOKEN_TYPE_MASK)==TOKEN_START_TAG){
				StartTagToken tag=(StartTagToken)getToken(token);
				String name=tag.getName();
				if(name.equals("html")){
					applyInsertionMode(token,InsertionMode.InBody);
					return true;
				} else if(name.equals("body")){
					addHtmlElement(tag);
					framesetOk=false;
					insertionMode=InsertionMode.InBody;
					return true;
				} else if(name.equals("frameset")){
					addHtmlElement(tag);
					insertionMode=InsertionMode.InFrameset;
					return true;
				} else if("base".equals(name)||
						"bgsound".equals(name)||
						"basefont".equals(name)||
						"link".equals(name)||
						"noframes".equals(name)||
						"script".equals(name)||
						"style".equals(name)||
						"title".equals(name)||
						"meta".equals(name)){
					error=true;
					openElements.add(headElement);
					applyInsertionMode(token,InsertionMode.InHead);
					openElements.remove(headElement);
					return true;
				} else if("head".equals(name)){
					error=true;
					return false;
				} else {
					applyStartTag("body",insMode);
					framesetOk=true;
					return applyInsertionMode(token,null);
				}
			} else if((token&TOKEN_TYPE_MASK)==TOKEN_END_TAG){
				EndTagToken tag=(EndTagToken)getToken(token);
				String name=tag.getName();
				if(name.equals("body") || name.equals("html")||
						name.equals("br")){
					applyStartTag("body",insMode);
					framesetOk=true;
					return applyInsertionMode(token,null);
				} else {
					error=true;
					return false;
				}
			} else if((token&TOKEN_TYPE_MASK)==TOKEN_COMMENT){
				addCommentNodeToCurrentNode(token);

				return true;
			} else if(token==TOKEN_EOF){
				applyStartTag("body",insMode);
				framesetOk=true;
				return applyInsertionMode(token,null);
			}
			return true;
		}
		case Text:{
			if((token&TOKEN_TYPE_MASK)==TOKEN_CHARACTER){
				if(insMode!=insertionMode){
					insertCharacter(getCurrentNode(),token);
				} else {
					Text textNode=getTextNodeToInsert(getCurrentNode());
					int ch=token;
					if(textNode==null)
						throw new AssertionError();
					while(true){
						textNode.text.appendInt(ch);
						token=parserRead();
						if((token&TOKEN_TYPE_MASK)!=TOKEN_CHARACTER){
							tokenQueue.add(0,token);
							break;
						}
						ch=token;
					}
				}
				return true;
			} else if(token==TOKEN_EOF){
				error=true;
				openElements.remove(openElements.size()-1);
				insertionMode=originalInsertionMode;
				return applyInsertionMode(token,null);
			} else if((token&TOKEN_TYPE_MASK)==TOKEN_END_TAG){
				openElements.remove(openElements.size()-1);
				insertionMode=originalInsertionMode;
			}
			return true;
		}
		case InBody:{
			if(token==0){
				error=true;
				return true;
			}
			if((token&TOKEN_TYPE_MASK)==TOKEN_COMMENT){
				addCommentNodeToCurrentNode(token);

				return true;
			}
			if((token&TOKEN_TYPE_MASK)==TOKEN_DOCTYPE){
				error=true;
				return true;
			}
			if((token&TOKEN_TYPE_MASK)==TOKEN_CHARACTER){
				//DebugUtility.log("%c %s",token,getCurrentNode().getTagName());
				reconstructFormatting();
				Text textNode=getTextNodeToInsert(getCurrentNode());
				int ch=token;
				if(textNode==null)
					throw new AssertionError();
				while(true){
					// Read multiple characters at once
					if(ch==0){
						error=true;
					} else {
						textNode.text.appendInt(ch);
					}
					if(framesetOk && token!=0x20 && token!=0x09 &&
							token!=0x0a && token!=0x0c && token!=0x0d){
						framesetOk=false;
					}
					// If we're only processing under a different
					// insertion mode then break
					if(insMode!=insertionMode) {
						break;
					}
					token=parserRead();
					if((token&TOKEN_TYPE_MASK)!=TOKEN_CHARACTER){
						tokenQueue.add(0,token);
						break;
					}
					ch=token;
				}
				return true;
			} else if(token==TOKEN_EOF){
				for(Element e : openElements){
					String name=e.getLocalName();
					if(!"dd".equals(name) &&
							!"dt".equals(name) &&
							!"li".equals(name) &&
							!"p".equals(name) &&
							!"tbody".equals(name) &&
							!"td".equals(name) &&
							!"tfoot".equals(name) &&
							!"th".equals(name) &&
							!"tr".equals(name) &&
							!"thead".equals(name) &&
							!"body".equals(name) &&
							!"html".equals(name)){
						error=true;
					}
				}
				stopParsing();
			}
			if((token&TOKEN_TYPE_MASK)==TOKEN_START_TAG){
				//
				//  START TAGS
				//
				StartTagToken tag=(StartTagToken)getToken(token);
				String name=tag.getName();
				if("html".equals(name)){
					error=true;
					openElements.get(0).mergeAttributes(tag);
					return true;
				} else if("base".equals(name)||
						"bgsound".equals(name)||
						"basefont".equals(name)||
						"link".equals(name)||
						"menuitem".equals(name)||
						"noframes".equals(name)||
						"script".equals(name)||
						"style".equals(name)||
						"title".equals(name)||
						"meta".equals(name)){
					applyInsertionMode(token,InsertionMode.InHead);
					return true;
				} else if("body".equals(name)){
					error=true;
					if(openElements.size()<=1 ||
							!openElements.get(1).isHtmlElement("body"))
						return false;
					framesetOk=false;
					openElements.get(1).mergeAttributes(tag);
					return true;
				} else if("frameset".equals(name)){
					error=true;
					if(!framesetOk ||
							openElements.size()<=1 ||
							!openElements.get(1).isHtmlElement("body"))
						return false;
					Node parent=(Node) openElements.get(1).getParentNode();
					if(parent!=null){
						parent.removeChild(openElements.get(1));
					}
					while(openElements.size()>1){
						popCurrentNode();
					}
					addHtmlElement(tag);
					insertionMode=InsertionMode.InFrameset;
					return true;
				} else if("address".equals(name) ||
						"article".equals(name) ||
						"aside".equals(name) ||
						"blockquote".equals(name) ||
						"center".equals(name) ||
						"details".equals(name) ||
						"dialog".equals(name) ||
						"dir".equals(name) ||
						"div".equals(name) ||
						"dl".equals(name) ||
						"fieldset".equals(name) ||
						"figcaption".equals(name) ||
						"figure".equals(name) ||
						"footer".equals(name) ||
						"header".equals(name) ||
						"hgroup".equals(name) ||
						"menu".equals(name) ||
						"nav".equals(name) ||
						"ol".equals(name) ||
						"p".equals(name) ||
						"section".equals(name) ||
						"summary".equals(name) ||
						"ul".equals(name)
						){
					closeParagraph(insMode);
					addHtmlElement(tag);
					return true;
				} else if("h1".equals(name) ||
						"h2".equals(name) ||
						"h3".equals(name) ||
						"h4".equals(name) ||
						"h5".equals(name) ||
						"h6".equals(name)
						){
					closeParagraph(insMode);
					Element node=getCurrentNode();
					String name1=node.getLocalName();
					if("h1".equals(name1) ||
							"h2".equals(name1) ||
							"h3".equals(name1) ||
							"h4".equals(name1) ||
							"h5".equals(name1) ||
							"h6".equals(name1)
							){
						error=true;
						openElements.remove(openElements.size()-1);
					}
					addHtmlElement(tag);
					return true;
				} else if("pre".equals(name)||
						"listing".equals(name)){
					closeParagraph(insMode);
					addHtmlElement(tag);
					skipLineFeed();
					framesetOk=false;
					return true;
				} else if("form".equals(name)){
					if(formElement!=null){
						error=true;
						return true;
					}
					closeParagraph(insMode);
					formElement=addHtmlElement(tag);
					return true;
				} else if("li".equals(name)){
					framesetOk=false;
					for(int i=openElements.size()-1;i>=0;i--){
						Element node=openElements.get(i);
						String nodeName=node.getLocalName();
						if(nodeName.equals("li")){
							applyInsertionMode(
									getArtificialToken(TOKEN_END_TAG,"li"),
									insMode);
							break;
						}
						if(isSpecialElement(node) &&
								!"address".equals(nodeName) &&
								!"div".equals(nodeName) &&
								!"p".equals(nodeName)){
							break;
						}
					}
					closeParagraph(insMode);
					addHtmlElement(tag);
					return true;
				} else if("dd".equals(name) || "dt".equals(name)){
					framesetOk=false;
					for(int i=openElements.size()-1;i>=0;i--){
						Element node=openElements.get(i);
						String nodeName=node.getLocalName();
						//DebugUtility.log("looping through %s",nodeName);
						if(nodeName.equals("dd") || nodeName.equals("dt")){
							applyEndTag(nodeName,insMode);
							break;
						}
						if(isSpecialElement(node) &&
								!"address".equals(nodeName) &&
								!"div".equals(nodeName) &&
								!"p".equals(nodeName)){
							break;
						}
					}
					closeParagraph(insMode);
					addHtmlElement(tag);
					return true;
				} else if("plaintext".equals(name)){
					closeParagraph(insMode);
					addHtmlElement(tag);
					state=TokenizerState.PlainText;
					return true;
				} else if("button".equals(name)){
					if(hasHtmlElementInScope("button")){
						error=true;
						applyEndTag("button",insMode);
						return applyInsertionMode(token,null);
					}
					reconstructFormatting();
					addHtmlElement(tag);
					framesetOk=false;
					return true;
				} else if("a".equals(name)){
					while(true){
						Element node=null;
						for(int i=formattingElements.size()-1; i>=0; i--){
							FormattingElement fe=formattingElements.get(i);
							if(fe.isMarker()) {
								break;
							}
							if(fe.element.getLocalName().equals("a")){
								node=fe.element;
								break;
							}
						}
						if(node!=null){
							error=true;
							applyEndTag("a",insMode);
							removeFormattingElement(node);
							openElements.remove(node);
						} else {
							break;
						}
					}
					reconstructFormatting();
					pushFormattingElement(tag);
				} else if("b".equals(name) ||
						"big".equals(name)||
						"code".equals(name)||
						"em".equals(name)||
						"font".equals(name)||
						"i".equals(name)||
						"s".equals(name)||
						"small".equals(name)||
						"strike".equals(name)||
						"strong".equals(name)||
						"tt".equals(name)||
						"u".equals(name)){
					reconstructFormatting();
					pushFormattingElement(tag);
				} else if("nobr".equals(name)){
					reconstructFormatting();
					if(hasHtmlElementInScope("nobr")){
						error=true;
						applyEndTag("nobr",insMode);
						reconstructFormatting();
					}
					pushFormattingElement(tag);
				} else if("table".equals(name)){
					if(document.getMode()!=DocumentMode.QuirksMode) {
						closeParagraph(insMode);
					}
					addHtmlElement(tag);
					framesetOk=false;
					insertionMode=InsertionMode.InTable;
					return true;
				} else if("area".equals(name)||
						"br".equals(name)||
						"embed".equals(name)||
						"img".equals(name)||
						"keygen".equals(name)||
						"wbr".equals(name)
						){
					reconstructFormatting();
					addHtmlElementNoPush(tag);
					tag.ackSelfClosing();
					framesetOk=false;
				} else if("input".equals(name)){
					reconstructFormatting();
					inputElement=addHtmlElementNoPush(tag);
					tag.ackSelfClosing();
					String attr=inputElement.getAttribute("type");
					if(attr==null || !"hidden".equals(StringUtility.toLowerCaseAscii(attr))){
						framesetOk=false;
					}
				} else if("param".equals(name)||
						"source".equals(name)||
						"track".equals(name)
						){
					addHtmlElementNoPush(tag);
					tag.ackSelfClosing();
				} else if("hr".equals(name)){
					closeParagraph(insMode);
					addHtmlElementNoPush(tag);
					tag.ackSelfClosing();
					framesetOk=false;
				} else if("image".equals(name)){
					error=true;
					tag.setName("img");
					return applyInsertionMode(token,null);
				} else if("isindex".equals(name)){
					error=true;
					if(formElement!=null)return false;
					tag.ackSelfClosing();
					applyStartTag("form",insMode);
					String action=tag.getAttribute("action");
					if(action!=null) {
						formElement.setAttribute("action",action);
					}
					applyStartTag("hr",insMode);
					applyStartTag("label",insMode);
					StartTagToken isindex=new StartTagToken("input");
					for(IAttr attr : tag.getAttributes()){
						String attrname=attr.getName();
						if(!"name".equals(attrname) &&
								!"action".equals(attrname) &&
								!"prompt".equals(attrname)){
							isindex.setAttribute(attrname,attr.getValue());
						}
					}
					String prompt=tag.getAttribute("prompt");
					// NOTE: Because of the inserted hr elements,
					// the frameset-ok flag should have been set
					// to not-ok already, so we don't need to check
					// for whitespace here
					if(prompt!=null){
						reconstructFormatting();
						insertString(getCurrentNode(),prompt);
					} else {
						reconstructFormatting();
						insertString(getCurrentNode(),"Enter search keywords:");
					}
					int isindexToken=tokens.size()|isindex.getType();
					tokens.add(isindex);
					applyInsertionMode(isindexToken,insMode);
					inputElement.setAttribute("name","isindex");
					applyEndTag("label",insMode);
					applyStartTag("hr",insMode);
					applyEndTag("form",insMode);
				} else if("textarea".equals(name)){
					addHtmlElement(tag);
					skipLineFeed();
					state=TokenizerState.RcData;
					originalInsertionMode=insertionMode;
					framesetOk=false;
					insertionMode=InsertionMode.Text;
				} else if("xmp".equals(name)){
					closeParagraph(insMode);
					reconstructFormatting();
					framesetOk=false;
					addHtmlElement(tag);
					state=TokenizerState.RawText;
					originalInsertionMode=insertionMode;
					insertionMode=InsertionMode.Text;
				} else if("iframe".equals(name)){
					framesetOk=false;
					addHtmlElement(tag);
					state=TokenizerState.RawText;
					originalInsertionMode=insertionMode;
					insertionMode=InsertionMode.Text;
				} else if("noembed".equals(name)){
					addHtmlElement(tag);
					state=TokenizerState.RawText;
					originalInsertionMode=insertionMode;
					insertionMode=InsertionMode.Text;
				} else if("select".equals(name)){
					reconstructFormatting();
					addHtmlElement(tag);
					framesetOk=false;
					if(insertionMode==InsertionMode.InTable ||
							insertionMode==InsertionMode.InCaption ||
							insertionMode==InsertionMode.InTableBody ||
							insertionMode==InsertionMode.InRow ||
							insertionMode==InsertionMode.InCell ) {
						insertionMode=InsertionMode.InSelectInTable;
					} else {
						insertionMode=InsertionMode.InSelect;
					}
				} else if("option".equals(name) || "optgroup".equals(name)){
					if(getCurrentNode().getLocalName().equals("option")){
						applyEndTag("option",insMode);
					}
					reconstructFormatting();
					addHtmlElement(tag);
				} else if("rp".equals(name) || "rt".equals(name)){
					if(hasHtmlElementInScope("ruby")){
						generateImpliedEndTags();
						if(!getCurrentNode().getLocalName().equals("ruby")){
							error=true;
						}
					}
					addHtmlElement(tag);
				} else if("applet".equals(name) ||
						"marquee".equals(name) ||
						"object".equals(name)){
					reconstructFormatting();
					Element e=addHtmlElement(tag);
					insertFormattingMarker(tag,e);
					framesetOk=false;
				} else if("math".equals(name)){
					reconstructFormatting();
					adjustMathMLAttributes(tag);
					adjustForeignAttributes(tag);
					insertForeignElement(tag,MATHML_NAMESPACE);
					if(tag.isSelfClosing()){
						tag.ackSelfClosing();
						popCurrentNode();
					} else {
						hasForeignContent=true;
					}
				} else if("svg".equals(name)){
					reconstructFormatting();
					adjustSvgAttributes(tag);
					adjustForeignAttributes(tag);
					insertForeignElement(tag,SVG_NAMESPACE);
					if(tag.isSelfClosing()){
						tag.ackSelfClosing();
						popCurrentNode();
					} else {
						hasForeignContent=true;
					}
				} else if("caption".equals(name) ||
						"col".equals(name) ||
						"colgroup".equals(name) ||
						"frame".equals(name) ||
						"head".equals(name) ||
						"tbody".equals(name) ||
						"td".equals(name) ||
						"tfoot".equals(name) ||
						"th".equals(name) ||
						"thead".equals(name) ||
						"tr".equals(name)
						){
					error=true;
					return false;
				} else {
					//DebugUtility.log("ordinary: %s",tag);
					reconstructFormatting();
					addHtmlElement(tag);
				}
			} else if((token&TOKEN_TYPE_MASK)==TOKEN_END_TAG){
				//
				//  END TAGS
				// NOTE: Have all cases
				//
				EndTagToken tag=(EndTagToken)getToken(token);
				String name=tag.getName();
				if(name.equals("body")){
					if(!hasHtmlElementInScope("body")){
						error=true;
						return false;
					}
					for(Element e : openElements){
						String name2=e.getLocalName();
						if(!"dd".equals(name2) &&
								!"dt".equals(name2) &&
								!"li".equals(name2) &&
								!"option".equals(name2) &&
								!"optgroup".equals(name2) &&
								!"p".equals(name2) &&
								!"rb".equals(name2) &&
								!"tbody".equals(name2) &&
								!"td".equals(name2) &&
								!"tfoot".equals(name2) &&
								!"th".equals(name2) &&
								!"tr".equals(name2) &&
								!"thead".equals(name2) &&
								!"body".equals(name2) &&
								!"html".equals(name2)){
							error=true;
							// token not ignored here
						}
					}
					insertionMode=InsertionMode.AfterBody;
				} else if(name.equals("a") ||
						name.equals("b") ||
						name.equals("big") ||
						name.equals("code") ||
						name.equals("em") ||
						name.equals("b") ||
						name.equals("font") ||
						name.equals("i") ||
						name.equals("nobr") ||
						name.equals("s") ||
						name.equals("small") ||
						name.equals("strike") ||
						name.equals("strong") ||
						name.equals("tt") ||
						name.equals("u")
						){
					for(int i=0;i<8;i++){
						FormattingElement formatting=null;
						for(int j=formattingElements.size()-1; j>=0; j--){
							FormattingElement fe=formattingElements.get(j);
							if(fe.isMarker()) {
								break;
							}
							if(fe.element.getLocalName().equals(name)){
								formatting=fe;
								break;
							}
						}
						if(formatting==null){
							// NOTE: Steps for "any other end tag"
							//	DebugUtility.log("no such formatting element");
							for(int i1=openElements.size()-1;i1>=0;i1--){
								Element node=openElements.get(i1);
								if(name.equals(node.getLocalName())){
									generateImpliedEndTagsExcept(name);
									if(!name.equals(getCurrentNode().getLocalName())){
										error=true;
									}
									while(true){
										Element node2=popCurrentNode();
										if(node2.equals(node)) {
											break;
										}
									}
									break;
								} else if(isSpecialElement(node)){
									error=true;
									return false;
								}
							}
							break;
						}
						int formattingElementPos=openElements.indexOf(formatting.element);
						if(formattingElementPos<0){ // not found
							error=true;
							//	DebugUtility.log("Not in stack of open elements");
							formattingElements.remove(formatting);
							break;
						}
						//	DebugUtility.log("Open elements[%s]:",i);
						//	DebugUtility.log(openElements);
						//	DebugUtility.log("Formatting elements:");
						//	DebugUtility.log(formattingElements);
						if(!hasHtmlElementInScope(formatting.element)){
							error=true;
							return false;
						}
						if(!formatting.element.equals(getCurrentNode())){
							error=true;
						}
						Element furthestBlock=null;
						int furthestBlockPos=-1;
						for(int j=openElements.size()-1;j>formattingElementPos;j--){
							Element e=openElements.get(j);
							if(isSpecialElement(e)){
								furthestBlock=e;
								furthestBlockPos=j;
							}
						}
						//	DebugUtility.log("furthest block: %s",furthestBlock);
						if(furthestBlock==null){
							// Pop up to and including the
							// formatting element
							while(openElements.size()>formattingElementPos){
								popCurrentNode();
							}
							formattingElements.remove(formatting);
							//DebugUtility.log("Open elements now [%s]:",i);
							//DebugUtility.log(openElements);
							//DebugUtility.log("Formatting elements now:");
							//DebugUtility.log(formattingElements);
							break;
						}
						Element commonAncestor=openElements.get(formattingElementPos-1);
						//	DebugUtility.log("common ancestor: %s",commonAncestor);
						int bookmark=formattingElements.indexOf(formatting);
						//	DebugUtility.log("bookmark=%d",bookmark);
						Element myNode=furthestBlock;
						Element superiorNode=openElements.get(furthestBlockPos-1);
						Element lastNode=furthestBlock;
						for(int j=0;j<3;j++){
							myNode=superiorNode;
							FormattingElement nodeFE=getFormattingElement(myNode);
							if(nodeFE==null){
								//	DebugUtility.log("node not a formatting element");
								superiorNode=openElements.get(openElements.indexOf(myNode)-1);
								openElements.remove(myNode);
								continue;
							} else if(myNode.equals(formatting.element)){
								//	DebugUtility.log("node is the formatting element");
								break;
							}
							Element e=Element.fromToken(nodeFE.token);
							nodeFE.element=e;
							int io=openElements.indexOf(myNode);
							superiorNode=openElements.get(io-1);
							openElements.set(io,e);
							myNode=e;
							if(lastNode.equals(furthestBlock)){
								bookmark=formattingElements.indexOf(nodeFE)+1;
							}
							// NOTE: Because 'node' can only be a formatting
							// element, the foster parenting rule doesn't
							// apply here
							if(lastNode.getParentNode()!=null) {
								((Node) lastNode.getParentNode()).removeChild(lastNode);
							}
							myNode.appendChild(lastNode);
							lastNode=myNode;
						}
						//	DebugUtility.log("node: %s",node);
						//	DebugUtility.log("lastNode: %s",lastNode);
						if(commonAncestor.getLocalName().equals("table") ||
								commonAncestor.getLocalName().equals("tr") ||
								commonAncestor.getLocalName().equals("tbody") ||
								commonAncestor.getLocalName().equals("thead") ||
								commonAncestor.getLocalName().equals("tfoot")
								){
							if(lastNode.getParentNode()!=null) {
								((Node) lastNode.getParentNode()).removeChild(lastNode);
							}
							fosterParent(lastNode);
						} else {
							if(lastNode.getParentNode()!=null) {
								((Node) lastNode.getParentNode()).removeChild(lastNode);
							}
							commonAncestor.appendChild(lastNode);
						}
						Element e2=Element.fromToken(formatting.token);
						for(Node child : new ArrayList<Node>(furthestBlock.getChildNodesInternal())){
							furthestBlock.removeChild(child);
							// NOTE: Because 'e' can only be a formatting
							// element, the foster parenting rule doesn't
							// apply here
							e2.appendChild(child);
						}
						// NOTE: Because intervening elements, including
						// formatting elements, are cleared between table
						// and tbody/thead/tfoot and between those three
						// elements and tr, the foster parenting rule
						// doesn't apply here
						furthestBlock.appendChild(e2);
						FormattingElement newFE=new FormattingElement();
						newFE.marker=false;
						newFE.element=e2;
						newFE.token=formatting.token;
						//	DebugUtility.log("Adding formatting element at %d",bookmark);
						formattingElements.add(bookmark,newFE);
						formattingElements.remove(formatting);
						//	DebugUtility.log("Replacing open element at %d",openElements.indexOf(furthestBlock)+1);
						int idx=openElements.indexOf(furthestBlock)+1;
						openElements.add(idx,e2);
						openElements.remove(formatting.element);
					}
				} else if("applet".equals(name) ||
						"marquee".equals(name) ||
						"object".equals(name)){
					if(!hasHtmlElementInScope(name)){
						error=true;
						return false;
					} else {
						generateImpliedEndTags();
						if(!getCurrentNode().getLocalName().equals(name)){
							error=true;
						}
						while(true){
							Element node=popCurrentNode();
							if(node.getLocalName().equals(name)) {
								break;
							}
						}
						clearFormattingToMarker();

					}
				} else if(name.equals("html")){
					if(applyEndTag("body",insMode))
						return applyInsertionMode(token,null);
					return false;
				} else if("address".equals(name) ||
						"article".equals(name) ||
						"aside".equals(name) ||
						"blockquote".equals(name) ||
						"button".equals(name) ||
						"center".equals(name) ||
						"details".equals(name) ||
						"dialog".equals(name) ||
						"dir".equals(name) ||
						"div".equals(name) ||
						"dl".equals(name) ||
						"fieldset".equals(name) ||
						"figcaption".equals(name) ||
						"figure".equals(name) ||
						"footer".equals(name) ||
						"header".equals(name) ||
						"hgroup".equals(name) ||
						"listing".equals(name) ||
						"main".equals(name) ||
						"menu".equals(name) ||
						"nav".equals(name) ||
						"ol".equals(name) ||
						"pre".equals(name) ||
						"section".equals(name) ||
						"summary".equals(name) ||
						"ul".equals(name)
						){
					if(!hasHtmlElementInScope(name)){
						error=true;
						return true;
					} else {
						generateImpliedEndTags();
						if(!getCurrentNode().getLocalName().equals(name)){
							error=true;
						}
						while(true){
							Element node=popCurrentNode();
							if(node.getLocalName().equals(name)) {
								break;
							}
						}
					}
				} else if(name.equals("form")){
					Element node=formElement;
					formElement=null;
					if(node==null || hasHtmlElementInScope(node)){
						error=true;
						return true;
					}
					generateImpliedEndTags();
					if(getCurrentNode()!=node){
						error=true;
					}
					openElements.remove(node);
				} else if(name.equals("p")){
					if(!hasHtmlElementInButtonScope(name)){
						error=true;
						applyStartTag("p",insMode);
						return applyInsertionMode(token,null);
					}
					generateImpliedEndTagsExcept(name);
					if(!getCurrentNode().getLocalName().equals(name)){
						error=true;
					}
					while(true){
						Element node=popCurrentNode();
						if(node.getLocalName().equals(name)) {
							break;
						}
					}
				} else if(name.equals("li")){
					if(!hasHtmlElementInListItemScope(name)){
						error=true;
						return false;
					}
					generateImpliedEndTagsExcept(name);
					if(!getCurrentNode().getLocalName().equals(name)){
						error=true;
					}
					while(true){
						Element node=popCurrentNode();
						if(node.getLocalName().equals(name)) {
							break;
						}
					}
				} else if(name.equals("h1") || name.equals("h2") ||
						name.equals("h3") || name.equals("h4") ||
						name.equals("h5") || name.equals("h6")){
					if(!hasHtmlHeaderElementInScope()){
						error=true;
						return false;
					}
					generateImpliedEndTags();
					if(!getCurrentNode().getLocalName().equals(name)){
						error=true;
					}
					while(true){
						Element node=popCurrentNode();
						String name2=node.getLocalName();
						if(name2.equals("h1") ||
								name2.equals("h2") ||
								name2.equals("h3") ||
								name2.equals("h4") ||
								name2.equals("h5") ||
								name2.equals("h6")) {
							break;
						}
					}
					return true;
				} else if(name.equals("dd") || name.equals("dt")){
					if(!hasHtmlElementInScope(name)){
						error=true;
						return false;
					}
					generateImpliedEndTagsExcept(name);
					if(!getCurrentNode().getLocalName().equals(name)){
						error=true;
					}
					while(true){
						Element node=popCurrentNode();
						if(node.getLocalName().equals(name)) {
							break;
						}
					}
				} else if("br".equals(name)){
					error=true;
					applyStartTag("br",insMode);
					return false;
				} else {
					for(int i=openElements.size()-1;i>=0;i--){
						Element node=openElements.get(i);
						if(name.equals(node.getLocalName())){
							generateImpliedEndTagsExcept(name);
							if(!name.equals(getCurrentNode().getLocalName())){
								error=true;
							}
							while(true){
								Element node2=popCurrentNode();
								if(node2.equals(node)) {
									break;
								}
							}
							break;
						} else if(isSpecialElement(node)){
							error=true;
							return false;
						}
					}
				}
			}
			return true;
		}
		case InHeadNoscript:{
			if((token&TOKEN_TYPE_MASK)==TOKEN_CHARACTER){
				if(token==0x09 || token==0x0a || token==0x0c ||
						token==0x0d || token==0x20)
					return applyInsertionMode(token,InsertionMode.InBody);
				else {
					error=true;
					applyEndTag("noscript",insMode);
					return applyInsertionMode(token,null);
				}
			} else if((token&TOKEN_TYPE_MASK)==TOKEN_DOCTYPE){
				error=true;
				return false;
			} else if((token&TOKEN_TYPE_MASK)==TOKEN_START_TAG){
				StartTagToken tag=(StartTagToken)getToken(token);
				String name=tag.getName();
				if(name.equals("html"))
					return applyInsertionMode(token,InsertionMode.InBody);
				else if(name.equals("basefont") ||
						name.equals("bgsound") ||
						name.equals("link") ||
						name.equals("meta") ||
						name.equals("noframes") ||
						name.equals("style")
						)
					return applyInsertionMode(token,InsertionMode.InHead);
				else if(name.equals("head") ||
						name.equals("noscript")){
					error=true;
					return false;
				} else {
					error=true;
					applyEndTag("noscript",insMode);
					return applyInsertionMode(token,null);
				}
			} else if((token&TOKEN_TYPE_MASK)==TOKEN_END_TAG){
				EndTagToken tag=(EndTagToken)getToken(token);
				String name=tag.getName();
				if(name.equals("noscript")){
					popCurrentNode();
					insertionMode=InsertionMode.InHead;
				} else if(name.equals("br")){
					error=true;
					applyEndTag("noscript",insMode);
					return applyInsertionMode(token,null);
				} else {
					error=true;
					return false;
				}
			} else if((token&TOKEN_TYPE_MASK)==TOKEN_COMMENT)
				return applyInsertionMode(token,InsertionMode.InHead);
			else if(token==TOKEN_EOF){
				error=true;
				applyEndTag("noscript",insMode);
				return applyInsertionMode(token,null);
			}
			return true;
		}
		case InTable:{
			if((token&TOKEN_TYPE_MASK)==TOKEN_CHARACTER){
				Element currentNode=getCurrentNode();
				if(currentNode.getLocalName().equals("table") ||
						currentNode.getLocalName().equals("tbody") ||
						currentNode.getLocalName().equals("tfoot") ||
						currentNode.getLocalName().equals("thead") ||
						currentNode.getLocalName().equals("tr")
						){
					pendingTableCharacters.clearAll();
					originalInsertionMode=insertionMode;
					insertionMode=InsertionMode.InTableText;
					return applyInsertionMode(token,null);
				} else {
					// NOTE: Foster parenting rules don't apply here, since
					// the current node isn't table, tbody, tfoot, thead, or
					// tr and won't change while In Body is being applied
					error=true;
					return applyInsertionMode(token,InsertionMode.InBody);
				}
			} else if((token&TOKEN_TYPE_MASK)==TOKEN_DOCTYPE){
				error=true;
				return false;
			} else if((token&TOKEN_TYPE_MASK)==TOKEN_START_TAG){
				StartTagToken tag=(StartTagToken)getToken(token);
				String name=tag.getName();
				if(name.equals("table")){
					error=true;
					if(applyEndTag("table",insMode))
						return applyInsertionMode(token,null);
					return false;
				} else if(name.equals("caption")){
					while(true){
						Element node=getCurrentNode();
						if(node==null ||
								node.getLocalName().equals("table") ||
								node.getLocalName().equals("html")) {
							break;
						}
						popCurrentNode();
					}
					insertFormattingMarker(tag,addHtmlElement(tag));
					insertionMode=InsertionMode.InCaption;
					return true;
				} else if(name.equals("colgroup")){
					while(true){
						Element node=getCurrentNode();
						if(node==null ||
								node.getLocalName().equals("table") ||
								node.getLocalName().equals("html")) {
							break;
						}
						popCurrentNode();
					}
					addHtmlElement(tag);
					insertionMode=InsertionMode.InColumnGroup;
					return true;
				} else if(name.equals("col")){
					applyStartTag("colgroup",insMode);
					return applyInsertionMode(token,null);
				} else if(name.equals("tbody") ||
						name.equals("tfoot") ||
						name.equals("thead")){
					while(true){
						Element node=getCurrentNode();
						if(node==null ||
								node.getLocalName().equals("table") ||
								node.getLocalName().equals("html")) {
							break;
						}
						popCurrentNode();
					}
					addHtmlElement(tag);
					insertionMode=InsertionMode.InTableBody;
				} else if(name.equals("td") ||
						name.equals("th") ||
						name.equals("tr")){
					applyStartTag("tbody",insMode);
					return applyInsertionMode(token,null);
				} else if(name.equals("style") ||
						name.equals("script")){
					applyInsertionMode(token,InsertionMode.InHead);
				} else if(name.equals("input")){
					String attr=tag.getAttribute("type");
					if(attr==null || !"hidden".equals(StringUtility.toLowerCaseAscii(attr))){
						error=true;
						doFosterParent=true;
						applyInsertionMode(token,InsertionMode.InBody);
						doFosterParent=false;
					} else {
						error=true;
						addHtmlElementNoPush(tag);
						tag.ackSelfClosing();
					}
				} else if(name.equals("form")){
					error=true;
					if(formElement!=null)return false;
					formElement=addHtmlElementNoPush(tag);
				} else {
					error=true;
					doFosterParent=true;
					applyInsertionMode(token,InsertionMode.InBody);
					doFosterParent=false;
				}
			} else if((token&TOKEN_TYPE_MASK)==TOKEN_END_TAG){
				EndTagToken tag=(EndTagToken)getToken(token);
				String name=tag.getName();
				if(name.equals("table")){
					if(!hasHtmlElementInTableScope(name)){
						error=true;
						return false;
					} else {
						while(true){
							Element node=popCurrentNode();
							if(node.getLocalName().equals(name)) {
								break;
							}
						}
						resetInsertionMode();
					}
				} else if(name.equals("body") ||
						name.equals("caption") ||
						name.equals("col") ||
						name.equals("colgroup") ||
						name.equals("html") ||
						name.equals("tbody") ||
						name.equals("td") ||
						name.equals("tfoot") ||
						name.equals("th") ||
						name.equals("thead") ||
						name.equals("tr")){
					error=true;
					return false;
				} else {
					doFosterParent=true;
					applyInsertionMode(token,InsertionMode.InBody);
					doFosterParent=false;
				}
			} else if((token&TOKEN_TYPE_MASK)==TOKEN_COMMENT){
				addCommentNodeToCurrentNode(token);
				return true;
			} else if(token==TOKEN_EOF){
				if(getCurrentNode()==null || !getCurrentNode().getLocalName().equals("html")){
					error=true;
				}
				stopParsing();
			}
			return true;
		}
		case InTableText:{
			if((token&TOKEN_TYPE_MASK)==TOKEN_CHARACTER){
				if(token==0){
					error=true;
					return false;
				} else {
					pendingTableCharacters.appendInt(token);
				}
			} else {
				boolean nonspace=false;
				int[] array=pendingTableCharacters.array();
				int size=pendingTableCharacters.size();
				for(int i=0;i<size;i++){
					int c=array[i];
					if(c!=0x9 && c!=0xa && c!=0xc && c!=0xd && c!=0x20){
						nonspace=true;
						break;
					}
				}
				if(nonspace){
					// See 'anything else' for 'in table'
					error=true;
					doFosterParent=true;
					for(int i=0;i<size;i++){
						int c=array[i];
						applyInsertionMode(c,InsertionMode.InBody);
					}
					doFosterParent=false;
				} else {
					insertString(getCurrentNode(),pendingTableCharacters.toString());
				}
				insertionMode=originalInsertionMode;
				return applyInsertionMode(token,null);
			}
			return true;
		}
		case InCaption:{
			if((token&TOKEN_TYPE_MASK)==TOKEN_START_TAG){
				StartTagToken tag=(StartTagToken)getToken(token);
				String name=tag.getName();
				if(name.equals("caption") ||
						name.equals("col") ||
						name.equals("colgroup") ||
						name.equals("tbody") ||
						name.equals("thead") ||
						name.equals("td") ||
						name.equals("tfoot") ||
						name.equals("th") ||
						name.equals("tr")
						){
					error=true;
					if(applyEndTag("caption",insMode))
						return applyInsertionMode(token,null);
				} else
					return applyInsertionMode(token,InsertionMode.InBody);
			} else if((token&TOKEN_TYPE_MASK)==TOKEN_END_TAG){
				EndTagToken tag=(EndTagToken)getToken(token);
				String name=tag.getName();
				if(name.equals("caption")){
					if(!hasHtmlElementInScope(name)){
						error=true;
						return false;
					}
					generateImpliedEndTags();
					if(!getCurrentNode().getLocalName().equals("caption")){
						error=true;
					}
					while(true){
						Element node=popCurrentNode();
						if(node.getLocalName().equals("caption")){
							break;
						}
					}
					clearFormattingToMarker();
					insertionMode=InsertionMode.InTable;
				} else if(name.equals("table")){
					error=true;
					if(applyEndTag("caption",insMode))
						return applyInsertionMode(token,null);
				} else if(name.equals("body") ||
						name.equals("col") ||
						name.equals("colgroup") ||
						name.equals("tbody") ||
						name.equals("thead") ||
						name.equals("td") ||
						name.equals("tfoot") ||
						name.equals("th") ||
						name.equals("tr") ||
						name.equals("html")
						){
					error=true;
					return false;
				} else
					return applyInsertionMode(token,InsertionMode.InBody);
			} else
				return applyInsertionMode(token,InsertionMode.InBody);
			return true;
		}
		case InColumnGroup:{
			if((token&TOKEN_TYPE_MASK)==TOKEN_CHARACTER){
				if(token==0x20 || token==0x0c || token==0x0a || token==0x0d || token==0x09){
					insertCharacter(getCurrentNode(),token);
				} else {
					if(applyEndTag("colgroup",insMode))
						return applyInsertionMode(token,null);
				}
			} else if((token&TOKEN_TYPE_MASK)==TOKEN_DOCTYPE){
				error=true;
				return false;
			} else if((token&TOKEN_TYPE_MASK)==TOKEN_START_TAG){
				StartTagToken tag=(StartTagToken)getToken(token);
				String name=tag.getName();
				if(name.equals("html"))
					return applyInsertionMode(token,InsertionMode.InBody);
				else if(name.equals("col")){
					addHtmlElementNoPush(tag);
					tag.ackSelfClosing();
				} else {
					if(applyEndTag("colgroup",insMode))
						return applyInsertionMode(token,null);
				}
			} else if((token&TOKEN_TYPE_MASK)==TOKEN_END_TAG){
				EndTagToken tag=(EndTagToken)getToken(token);
				String name=tag.getName();
				if(name.equals("colgroup")){
					if(getCurrentNode().getLocalName().equals("html")){
						error=true;
						return false;
					}
					popCurrentNode();
					insertionMode=InsertionMode.InTable;
				} else if(name.equals("col")){
					error=true;
					return false;
				} else {
					if(applyEndTag("colgroup",insMode))
						return applyInsertionMode(token,null);
				}

			} else if((token&TOKEN_TYPE_MASK)==TOKEN_COMMENT){
				if(applyEndTag("colgroup",insMode))
					return applyInsertionMode(token,null);

			} else if(token==TOKEN_EOF){
				if(getCurrentNode().getLocalName().equals("html")){
					stopParsing();
					return true;
				}
				if(applyEndTag("colgroup",insMode))
					return applyInsertionMode(token,null);
			}
			return true;
		}
		case InTableBody:{
			if((token&TOKEN_TYPE_MASK)==TOKEN_START_TAG){
				StartTagToken tag=(StartTagToken)getToken(token);
				String name=tag.getName();
				if(name.equals("tr")){
					while(true){
						Element node=getCurrentNode();
						if(node==null ||
								node.getLocalName().equals("tbody") ||
								node.getLocalName().equals("tfoot") ||
								node.getLocalName().equals("thead") ||
								node.getLocalName().equals("html")) {
							break;
						}
						popCurrentNode();
					}
					addHtmlElement(tag);
					insertionMode=InsertionMode.InRow;
				} else if(name.equals("th") || name.equals("td")){
					error=true;
					applyStartTag("tr",insMode);
					return applyInsertionMode(token,null);
				} else if(name.equals("caption") ||
						name.equals("col") ||
						name.equals("colgroup") ||
						name.equals("tbody") ||
						name.equals("tfoot") ||
						name.equals("thead")){
					if(!hasHtmlElementInTableScope("tbody") &&
							!hasHtmlElementInTableScope("thead") &&
							!hasHtmlElementInTableScope("tfoot")
							){
						error=true;
						return false;
					}
					while(true){
						Element node=getCurrentNode();
						if(node==null ||
								node.getLocalName().equals("tbody") ||
								node.getLocalName().equals("tfoot") ||
								node.getLocalName().equals("thead") ||
								node.getLocalName().equals("html")) {
							break;
						}
						popCurrentNode();
					}
					applyEndTag(getCurrentNode().getLocalName(),insMode);
					return applyInsertionMode(token,null);
				} else
					return applyInsertionMode(token,InsertionMode.InTable);
			} else if((token&TOKEN_TYPE_MASK)==TOKEN_END_TAG){
				EndTagToken tag=(EndTagToken)getToken(token);
				String name=tag.getName();
				if(name.equals("tbody") ||
						name.equals("tfoot") ||
						name.equals("thead")){
					if(!hasHtmlElementInScope(name)){
						error=true;
						return false;
					}
					while(true){
						Element node=getCurrentNode();
						if(node==null ||
								node.getLocalName().equals("tbody") ||
								node.getLocalName().equals("tfoot") ||
								node.getLocalName().equals("thead") ||
								node.getLocalName().equals("html")) {
							break;
						}
						popCurrentNode();
					}
					popCurrentNode();
					insertionMode=InsertionMode.InTable;
				} else if(name.equals("table")){
					if(!hasHtmlElementInTableScope("tbody") &&
							!hasHtmlElementInTableScope("thead") &&
							!hasHtmlElementInTableScope("tfoot")
							){
						error=true;
						return false;
					}
					while(true){
						Element node=getCurrentNode();
						if(node==null ||
								node.getLocalName().equals("tbody") ||
								node.getLocalName().equals("tfoot") ||
								node.getLocalName().equals("thead") ||
								node.getLocalName().equals("html")) {
							break;
						}
						popCurrentNode();
					}
					applyEndTag(getCurrentNode().getLocalName(),insMode);
					return applyInsertionMode(token,null);
				} else if(name.equals("body") ||
						name.equals("caption") ||
						name.equals("col") ||
						name.equals("colgroup") ||
						name.equals("html") ||
						name.equals("td") ||
						name.equals("th") ||
						name.equals("tr")){
					error=true;
					return false;
				} else
					return applyInsertionMode(token,InsertionMode.InTable);
			} else
				return applyInsertionMode(token,InsertionMode.InTable);
			return true;
		}
		case InRow:{
			if((token&TOKEN_TYPE_MASK)==TOKEN_CHARACTER){
				applyInsertionMode(token,InsertionMode.InTable);

			} else if((token&TOKEN_TYPE_MASK)==TOKEN_DOCTYPE){
				applyInsertionMode(token,InsertionMode.InTable);

			} else if((token&TOKEN_TYPE_MASK)==TOKEN_START_TAG){
				StartTagToken tag=(StartTagToken)getToken(token);
				String name=tag.getName();
				if(name.equals("th")||name.equals("td")){
					while(!getCurrentNode().getLocalName().equals("tr") &&
							!getCurrentNode().getLocalName().equals("html")){
						popCurrentNode();
					}
					insertionMode=InsertionMode.InCell;
					insertFormattingMarker(tag,addHtmlElement(tag));
				} else if(name.equals("caption")||
						name.equals("col")||
						name.equals("colgroup")||
						name.equals("tbody")||
						name.equals("tfoot")||
						name.equals("thead")||
						name.equals("tr")){
					if(applyEndTag("tr",insMode))
						return applyInsertionMode(token,null);
				} else {
					applyInsertionMode(token,InsertionMode.InTable);
				}
			} else if((token&TOKEN_TYPE_MASK)==TOKEN_END_TAG){
				EndTagToken tag=(EndTagToken)getToken(token);
				String name=tag.getName();
				if(name.equals("tr")){
					if(!hasHtmlElementInTableScope(name)){
						error=true;
						return false;
					}
					while(!getCurrentNode().getLocalName().equals("tr") &&
							!getCurrentNode().getLocalName().equals("html")){
						popCurrentNode();
					}
					popCurrentNode();
					insertionMode=InsertionMode.InTableBody;
				} else if(name.equals("tbody") || name.equals("tfoot") ||
						name.equals("thead")){
					if(!hasHtmlElementInTableScope(name)){
						error=true;
						return false;
					}
					applyEndTag("tr",insMode);
					return applyInsertionMode(token,null);
				} else if(name.equals("caption")||
						name.equals("col")||
						name.equals("colgroup")||
						name.equals("html")||
						name.equals("body")||
						name.equals("td")||
						name.equals("th")){
					error=true;
				} else {
					applyInsertionMode(token,InsertionMode.InTable);
				}
			} else if((token&TOKEN_TYPE_MASK)==TOKEN_COMMENT){
				applyInsertionMode(token,InsertionMode.InTable);

			} else if(token==TOKEN_EOF){
				applyInsertionMode(token,InsertionMode.InTable);

			}
			return true;
		}
		case InCell:{
			if((token&TOKEN_TYPE_MASK)==TOKEN_CHARACTER){
				applyInsertionMode(token,InsertionMode.InBody);

			} else if((token&TOKEN_TYPE_MASK)==TOKEN_DOCTYPE){
				applyInsertionMode(token,InsertionMode.InBody);

			} else if((token&TOKEN_TYPE_MASK)==TOKEN_START_TAG){
				StartTagToken tag=(StartTagToken)getToken(token);
				String name=tag.getName();
				if(name.equals("caption")||
						name.equals("col")||
						name.equals("colgroup")||
						name.equals("tbody")||
						name.equals("td")||
						name.equals("tfoot")||
						name.equals("th")||
						name.equals("thead")||
						name.equals("tr")){
					if(!hasHtmlElementInTableScope("td") &&
							!hasHtmlElementInTableScope("th")){
						error=true;
						return false;
					}
					applyEndTag(hasHtmlElementInTableScope("td") ? "td" : "th",insMode);
					return applyInsertionMode(token,null);
				} else {
					applyInsertionMode(token,InsertionMode.InBody);
				}

			} else if((token&TOKEN_TYPE_MASK)==TOKEN_END_TAG){
				EndTagToken tag=(EndTagToken)getToken(token);
				String name=tag.getName();
				if(name.equals("td") || name.equals("th")){
					if(!hasHtmlElementInTableScope(name)){
						error=true;
						return false;
					}
					generateImpliedEndTags();
					if(!getCurrentNode().getLocalName().equals(name)){
						error=true;
					}
					while(true){
						Element node=popCurrentNode();
						if(node.getLocalName().equals(name)) {
							break;
						}
					}
					clearFormattingToMarker();
					insertionMode=InsertionMode.InRow;
				} else if(name.equals("caption")||
						name.equals("col")||
						name.equals("colgroup")||
						name.equals("body")||
						name.equals("html")){
					error=true;
					return false;
				} else if(name.equals("table")||
						name.equals("tbody")||
						name.equals("tfoot")||
						name.equals("thead")||
						name.equals("tr")){
					if(!hasHtmlElementInTableScope(name)){
						error=true;
						return false;
					}
					applyEndTag(hasHtmlElementInTableScope("td") ? "td" : "th",insMode);
					return applyInsertionMode(token,null);
				} else {
					applyInsertionMode(token,InsertionMode.InBody);

				}

			} else if((token&TOKEN_TYPE_MASK)==TOKEN_COMMENT){
				applyInsertionMode(token,InsertionMode.InBody);

			} else if(token==TOKEN_EOF){
				applyInsertionMode(token,InsertionMode.InBody);

			}
			return true;
		}
		case InSelect:{
			if((token&TOKEN_TYPE_MASK)==TOKEN_CHARACTER){
				if(token==0){
					error=true; return false;
				} else {
					insertCharacter(getCurrentNode(),token);
				}
			} else if((token&TOKEN_TYPE_MASK)==TOKEN_DOCTYPE){
				error=true; return false;

			} else if((token&TOKEN_TYPE_MASK)==TOKEN_START_TAG){
				StartTagToken tag=(StartTagToken)getToken(token);
				String name=tag.getName();
				if(name.equals("html")){
					applyInsertionMode(token,InsertionMode.InBody);
				} else if(name.equals("option")){
					if(getCurrentNode().getLocalName().equals("option")){
						applyEndTag("option",insMode);
					}
					addHtmlElement(tag);
				} else if(name.equals("optgroup")){
					if(getCurrentNode().getLocalName().equals("option")){
						applyEndTag("option",insMode);
					}
					if(getCurrentNode().getLocalName().equals("optgroup")){
						applyEndTag("optgroup",insMode);
					}
					addHtmlElement(tag);
				} else if(name.equals("select")){
					error=true;
					return applyEndTag("select",insMode);
				} else if(name.equals("input") || name.equals("keygen") ||
						name.equals("textarea")){
					error=true;
					if(!hasHtmlElementInSelectScope("select"))
						return false;
					applyEndTag("select",insMode);
					return applyInsertionMode(token,null);
				} else if(name.equals("script"))
					return applyInsertionMode(token,InsertionMode.InHead);
				else {
					error=true; return false;

				}
			} else if((token&TOKEN_TYPE_MASK)==TOKEN_END_TAG){
				EndTagToken tag=(EndTagToken)getToken(token);
				String name=tag.getName();
				if(name.equals("optgroup")){
					if(getCurrentNode().getLocalName().equals("option") &&
							openElements.size()>=2 &&
							openElements.get(openElements.size()-2).getLocalName().equals("optgroup")){
						applyEndTag("option",insMode);
					}
					if(getCurrentNode().getLocalName().equals("optgroup")){
						popCurrentNode();
					} else {
						error=true;
						return false;
					}
				} else if(name.equals("option")){
					if(getCurrentNode().getLocalName().equals("option")){
						popCurrentNode();
					} else {
						error=true;
						return false;
					}
				} else if(name.equals("select")){
					if(!hasHtmlElementInScope(name)){
						error=true;
						return false;
					}
					while(true){
						Element node=popCurrentNode();
						if(node.getLocalName().equals(name)) {
							break;
						}
					}
					resetInsertionMode();
				} else {
					error=true; return false;
				}
			} else if((token&TOKEN_TYPE_MASK)==TOKEN_COMMENT){
				addCommentNodeToCurrentNode(token);
			} else if(token==TOKEN_EOF){
				if(getCurrentNode()==null || !getCurrentNode().getLocalName().equals("html")){
					error=true;
				}
				stopParsing();
			}
			return true;
		}
		case InSelectInTable:{
			if((token&TOKEN_TYPE_MASK)==TOKEN_CHARACTER)
				return applyInsertionMode(token,InsertionMode.InSelect);
			else if((token&TOKEN_TYPE_MASK)==TOKEN_DOCTYPE)
				return applyInsertionMode(token,InsertionMode.InSelect);
			else if((token&TOKEN_TYPE_MASK)==TOKEN_START_TAG){
				StartTagToken tag=(StartTagToken)getToken(token);
				String name=tag.getName();
				if(name.equals("caption") ||
						name.equals("table") ||
						name.equals("tbody") ||
						name.equals("tfoot") ||
						name.equals("thead") ||
						name.equals("tr") ||
						name.equals("td") ||
						name.equals("th")
						){
					error=true;
					applyEndTag("select",insMode);
					return applyInsertionMode(token,null);
				}
				return applyInsertionMode(token,InsertionMode.InSelect);
			} else if((token&TOKEN_TYPE_MASK)==TOKEN_END_TAG){
				EndTagToken tag=(EndTagToken)getToken(token);
				String name=tag.getName();
				if(name.equals("caption") ||
						name.equals("table") ||
						name.equals("tbody") ||
						name.equals("tfoot") ||
						name.equals("thead") ||
						name.equals("tr") ||
						name.equals("td") ||
						name.equals("th")
						){
					error=true;
					if(!hasHtmlElementInTableScope(name))
						return false;
					applyEndTag("select",insMode);
					return applyInsertionMode(token,null);
				}
				return applyInsertionMode(token,InsertionMode.InSelect);
			} else if((token&TOKEN_TYPE_MASK)==TOKEN_COMMENT)
				return applyInsertionMode(token,InsertionMode.InSelect);
			else if(token==TOKEN_EOF)
				return applyInsertionMode(token,InsertionMode.InSelect);
			return true;
		}
		case AfterBody:{
			if((token&TOKEN_TYPE_MASK)==TOKEN_CHARACTER){
				if(token==0x09 || token==0x0a || token==0x0c || token==0x0d || token==0x20){
					applyInsertionMode(token,InsertionMode.InBody);
				} else {
					error=true;
					insertionMode=InsertionMode.InBody;
					return applyInsertionMode(token,null);
				}
			} else if((token&TOKEN_TYPE_MASK)==TOKEN_DOCTYPE){
				error=true;
				return true;
			} else if((token&TOKEN_TYPE_MASK)==TOKEN_START_TAG){
				StartTagToken tag=(StartTagToken)getToken(token);
				String name=tag.getName();
				if(name.equals("html")){
					applyInsertionMode(token,InsertionMode.InBody);
				} else {
					error=true;
					insertionMode=InsertionMode.InBody;
					return applyInsertionMode(token,null);
				}
			} else if((token&TOKEN_TYPE_MASK)==TOKEN_END_TAG){
				EndTagToken tag=(EndTagToken)getToken(token);
				String name=tag.getName();
				if(name.equals("html")){
					if(context!=null){
						error=true;
						return false;
					}
					insertionMode=InsertionMode.AfterAfterBody;
				} else {
					error=true;
					insertionMode=InsertionMode.InBody;
					return applyInsertionMode(token,null);
				}

			} else if((token&TOKEN_TYPE_MASK)==TOKEN_COMMENT){
				addCommentNodeToFirst(token);


			} else if(token==TOKEN_EOF){
				stopParsing();

				return true;
			}
			return true;
		}
		case InFrameset:{
			if((token&TOKEN_TYPE_MASK)==TOKEN_CHARACTER){
				if(token==0x09 || token==0x0a || token==0x0c ||
						token==0x0d || token==0x20) {
					insertCharacter(getCurrentNode(),token);
				} else {
					error=true;
					return false;
				}
			} else if((token&TOKEN_TYPE_MASK)==TOKEN_DOCTYPE){
				error=true;
				return false;
			} else if((token&TOKEN_TYPE_MASK)==TOKEN_START_TAG){
				StartTagToken tag=(StartTagToken)getToken(token);
				String name=tag.getName();
				if(name.equals("html")){
					applyInsertionMode(token,InsertionMode.InBody);
				} else if(name.equals("frameset")){
					addHtmlElement(tag);
				} else if(name.equals("frame")){
					addHtmlElementNoPush(tag);
					tag.ackSelfClosing();
				} else if(name.equals("noframes")){
					applyInsertionMode(token,InsertionMode.InHead);
				} else {
					error=true;
				}

			} else if((token&TOKEN_TYPE_MASK)==TOKEN_END_TAG){
				if(getCurrentNode().getLocalName().equals("html")){
					error=true;
					return false;
				}
				EndTagToken tag=(EndTagToken)getToken(token);
				String name=tag.getName();
				if(name.equals("frameset")){
					popCurrentNode();
					if(context==null &&
							!getCurrentNode().isHtmlElement("frameset")){
						insertionMode=InsertionMode.AfterFrameset;
					}
				} else {
					error=true;
				}

			} else if((token&TOKEN_TYPE_MASK)==TOKEN_COMMENT){
				addCommentNodeToCurrentNode(token);


			} else if(token==TOKEN_EOF){
				if(!getCurrentNode().isHtmlElement("html")) {
					error=true;
				}
				stopParsing();

			}
			return true;
		}
		case AfterFrameset:{
			if((token&TOKEN_TYPE_MASK)==TOKEN_CHARACTER){
				if(token==0x09 || token==0x0a || token==0x0c || token==0x0d || token==0x20){
					insertCharacter(getCurrentNode(),token);
				} else {
					error=true;
				}
			} else if((token&TOKEN_TYPE_MASK)==TOKEN_DOCTYPE){
				error=true;
			} else if((token&TOKEN_TYPE_MASK)==TOKEN_START_TAG){
				StartTagToken tag=(StartTagToken)getToken(token);
				String name=tag.getName();
				if(name.equals("html"))
					return applyInsertionMode(token,InsertionMode.InBody);
				else if(name.equals("noframes"))
					return applyInsertionMode(token,InsertionMode.InHead);
				else {
					error=true;
				}
			} else if((token&TOKEN_TYPE_MASK)==TOKEN_END_TAG){
				EndTagToken tag=(EndTagToken)getToken(token);
				String name=tag.getName();
				if(name.equals("html")){
					insertionMode=InsertionMode.AfterAfterFrameset;
				} else {
					error=true;
				}
			} else if((token&TOKEN_TYPE_MASK)==TOKEN_COMMENT){
				addCommentNodeToCurrentNode(token);

			} else if(token==TOKEN_EOF){
				stopParsing();

			}
			return true;
		}
		case AfterAfterBody:{
			if((token&TOKEN_TYPE_MASK)==TOKEN_CHARACTER){
				if(token==0x09 || token==0x0a || token==0x0c || token==0x0d || token==0x20){
					applyInsertionMode(token,InsertionMode.InBody);
				} else {
					error=true;
					insertionMode=InsertionMode.InBody;
					return applyInsertionMode(token,null);
				}
			} else if((token&TOKEN_TYPE_MASK)==TOKEN_DOCTYPE){
				applyInsertionMode(token,InsertionMode.InBody);
			} else if((token&TOKEN_TYPE_MASK)==TOKEN_START_TAG){
				StartTagToken tag=(StartTagToken)getToken(token);
				String name=tag.getName();
				if(name.equals("html")){
					applyInsertionMode(token,InsertionMode.InBody);
				} else {
					error=true;
					insertionMode=InsertionMode.InBody;
					return applyInsertionMode(token,null);
				}
			} else if((token&TOKEN_TYPE_MASK)==TOKEN_END_TAG){
				error=true;
				insertionMode=InsertionMode.InBody;
				return applyInsertionMode(token,null);

			} else if((token&TOKEN_TYPE_MASK)==TOKEN_COMMENT){
				addCommentNodeToDocument(token);

			} else if(token==TOKEN_EOF){
				stopParsing();

			}
			return true;
		}
		case AfterAfterFrameset:{
			if((token&TOKEN_TYPE_MASK)==TOKEN_CHARACTER){
				if(token==0x09 || token==0x0a || token==0x0c || token==0x0d || token==0x20){
					applyInsertionMode(token,InsertionMode.InBody);
				} else {
					error=true;
				}
			} else if((token&TOKEN_TYPE_MASK)==TOKEN_DOCTYPE){
				applyInsertionMode(token,InsertionMode.InBody);
			} else if((token&TOKEN_TYPE_MASK)==TOKEN_START_TAG){
				StartTagToken tag=(StartTagToken)getToken(token);
				String name=tag.getName();
				if("html".equals(name)){
					applyInsertionMode(token,InsertionMode.InBody);
				} else if("noframes".equals(name)){
					applyInsertionMode(token,InsertionMode.InHead);
				} else {
					error=true;
				}
			} else if((token&TOKEN_TYPE_MASK)==TOKEN_END_TAG){
				error=true;
			} else if((token&TOKEN_TYPE_MASK)==TOKEN_COMMENT){
				addCommentNodeToDocument(token);

			} else if(token==TOKEN_EOF){
				stopParsing();

			}
			return true;
		}
		default:
			throw new AssertionError();
		}
	}

	private boolean applyStartTag(String name, InsertionMode insMode) throws IOException {
		return applyInsertionMode(getArtificialToken(TOKEN_START_TAG,name),insMode);
	}

	private void changeEncoding(String charset) throws IOException {
		String currentEncoding=encoding.getEncoding();
		if(currentEncoding.equals("utf-16le") ||
				currentEncoding.equals("utf-16be")){
			encoding=new EncodingConfidence(currentEncoding,EncodingConfidence.Certain);
			return;
		}
		if(charset.equals("utf-16le")) {
			charset="utf-8";
		} else if(charset.equals("utf-16be")) {
			charset="utf-8";
		}
		if(charset.equals(currentEncoding)){
			encoding=new EncodingConfidence(currentEncoding,EncodingConfidence.Certain);
			return;
		}
		// Reinitialize all parser state
		initialize();
		// Rewind the input stream and set the new encoding
		inputStream.rewind();
		encoding=new EncodingConfidence(charset,EncodingConfidence.Certain);
		decoder=new Html5Decoder(TextEncoding.getDecoder(encoding.getEncoding()));
		charInput=new StackableCharacterInput(new DecoderCharacterInput(inputStream,decoder));
	}

	private void clearFormattingToMarker() {
		while(formattingElements.size()>0){
			FormattingElement fe=removeAtIndex(formattingElements,formattingElements.size()-1);
			if(fe.isMarker()) {
				break;
			}
		}
	}

	private void closeParagraph(InsertionMode insMode) throws IOException{
		if(hasHtmlElementInButtonScope("p")){
			applyEndTag("p",insMode);
		}
	}

	private Comment createCommentNode(int token){
		CommentToken comment=(CommentToken)getToken(token);
		Comment node=new Comment();
		node.setData(comment.getValue());
		return node;
	}

	private int emitCurrentTag() {
		int ret=tokens.size()|currentTag.getType();
		tokens.add(currentTag);
		if(currentTag.getType()==TOKEN_START_TAG) {
			lastStartTag=currentTag;
		} else {
			if(currentTag.getAttributes().size()>0 ||
					currentTag.isSelfClosing()){
				error=true;
			}
		}
		currentTag=null;
		return ret;
	}


	private void fosterParent(Node element) {
		if(openElements.size()==0)return;
		Node fosterParent=openElements.get(0);
		for(int i=openElements.size()-1;i>=0;i--){
			Element e=openElements.get(i);
			if(e.getLocalName().equals("table")){
				Node parent=(Node) e.getParentNode();
				boolean isElement=(parent!=null && parent.getNodeType()==NodeType.ELEMENT_NODE);
				if(!isElement){ // the parent is not an element
					if(i<=1)
						// This usually won't happen
						throw new AssertionError();
					// append to the element before this table
					fosterParent=openElements.get(i-1);
					break;
				} else {
					// Parent of the table, insert before the table
					parent.insertBefore(element,e);
					return;
				}
			}
		}
		fosterParent.appendChild(element);
	}

	private void generateImpliedEndTags(){
		while(true){
			Element node=getCurrentNode();
			String name=node.getLocalName();
			if("dd".equals(name)||
					"dd".equals(name)||
					"dt".equals(name)||
					"li".equals(name)||
					"option".equals(name)||
					"optgroup".equals(name)||
					"p".equals(name)||
					"rp".equals(name)||
					"rt".equals(name)){
				popCurrentNode();
			} else {
				break;
			}
		}
	}

	private void generateImpliedEndTagsExcept(String string) {
		while(true){
			Element node=getCurrentNode();
			String name=node.getLocalName();
			if(string.equals(name)) {
				break;
			}
			if("dd".equals(name)||
					"dd".equals(name)||
					"dt".equals(name)||
					"li".equals(name)||
					"option".equals(name)||
					"optgroup".equals(name)||
					"p".equals(name)||
					"rp".equals(name)||
					"rt".equals(name)){
				popCurrentNode();
			} else {
				break;
			}
		}
	}

	private int getArtificialToken(int type, String name){
		if(type==TOKEN_END_TAG){
			EndTagToken token=new EndTagToken(name);
			int ret=tokens.size()|type;
			tokens.add(token);
			return ret;
		}
		if(type==TOKEN_START_TAG){
			StartTagToken token=new StartTagToken(name);
			int ret=tokens.size()|type;
			tokens.add(token);
			return ret;
		}
		throw new IllegalArgumentException();
	}

	private Element getCurrentNode(){
		if(openElements.size()==0)return null;
		return openElements.get(openElements.size()-1);
	}

	private FormattingElement getFormattingElement(Element node) {
		for(FormattingElement fe : formattingElements){
			if(!fe.isMarker() && node.equals(fe.element))
				return fe;
		}
		return null;
	}


	private Text getFosterParentedTextNode() {
		if(openElements.size()==0)return null;
		Node fosterParent=openElements.get(0);
		List<Node> childNodes;
		for(int i=openElements.size()-1;i>=0;i--){
			Element e=openElements.get(i);
			if(e.getLocalName().equals("table")){
				Node parent=(Node) e.getParentNode();
				boolean isElement=(parent!=null && parent.getNodeType()==NodeType.ELEMENT_NODE);
				if(!isElement){ // the parent is not an element
					if(i<=1)
						// This usually won't happen
						throw new AssertionError();
					// append to the element before this table
					fosterParent=openElements.get(i-1);
					break;
				} else {
					// Parent of the table, insert before the table
					childNodes=parent.getChildNodesInternal();
					if(childNodes.size()==0)
						throw new AssertionError();
					for(int j=0;j<childNodes.size();j++){
						if(childNodes.get(j).equals(e)){
							if(j>0 && childNodes.get(j-1).getNodeType()==NodeType.TEXT_NODE)
								return (Text)childNodes.get(j-1);
							else {
								Text textNode=new Text();
								parent.insertBefore(textNode, e);
								return textNode;
							}
						}
					}
					throw new AssertionError();
				}
			}
		}
		childNodes=fosterParent.getChildNodesInternal();
		Node lastChild=(childNodes.size()==0) ? null : childNodes.get(childNodes.size()-1);
		if(lastChild==null || lastChild.getNodeType()!=NodeType.TEXT_NODE){
			Text textNode=new Text();
			fosterParent.appendChild(textNode);
			return textNode;
		} else
			return ((Text)lastChild);
	}
	private Text getTextNodeToInsert(Node node){
		if(doFosterParent && node.equals(getCurrentNode())){
			String name=((Element)node).getLocalName();
			if("table".equals(name) ||
					"tbody".equals(name) ||
					"tfoot".equals(name) ||
					"thead".equals(name) ||
					"tr".equals(name))
				return getFosterParentedTextNode();
		}
		List<Node> childNodes=node.getChildNodesInternal();
		Node lastChild=(childNodes.size()==0) ? null : childNodes.get(childNodes.size()-1);
		if(lastChild==null || lastChild.getNodeType()!=NodeType.TEXT_NODE){
			Text textNode=new Text();
			node.appendChild(textNode);
			return textNode;
		} else
			return ((Text)lastChild);
	}
	 IToken getToken(int token){
		if((token&TOKEN_TYPE_MASK)==TOKEN_CHARACTER ||
				(token&TOKEN_TYPE_MASK)==TOKEN_EOF)
			return null;
		else
			return tokens.get(token&TOKEN_INDEX_MASK);
	}
	private boolean hasHtmlElementInButtonScope(String name){
		boolean found=false;
		for(Element e : openElements){
			if(e.getLocalName().equals(name)){
				found=true;
			}
		}
		if(!found)return false;
		for(int i=openElements.size()-1;i>=0;i--){
			Element e=openElements.get(i);
			String namespace=e.getNamespaceURI();
			String thisName=e.getLocalName();
			if(HTML_NAMESPACE.equals(namespace)){
				if(thisName.equals(name))
					return true;
				if(thisName.equals("applet")||
						thisName.equals("caption")||
						thisName.equals("html")||
						thisName.equals("table")||
						thisName.equals("td")||
						thisName.equals("th")||
						thisName.equals("marquee")||
						thisName.equals("object")||
						thisName.equals("button"))
					//DebugUtility.log("not in scope: %s",thisName);
					return false;
			}
			if(MATHML_NAMESPACE.equals(namespace)){
				if(thisName.equals("mi")||
						thisName.equals("mo")||
						thisName.equals("mn")||
						thisName.equals("ms")||
						thisName.equals("mtext")||
						thisName.equals("annotation-xml"))
					return false;
			}
			if(SVG_NAMESPACE.equals(namespace)){
				if(thisName.equals("foreignObject")||
						thisName.equals("desc")||
						thisName.equals("title"))
					return false;
			}
		}
		return false;
	}
	private boolean hasHtmlElementInListItemScope(String name) {
		for(int i=openElements.size()-1;i>=0;i--){
			Element e=openElements.get(i);
			if(e.isHtmlElement(name))
				return true;
			if(e.isHtmlElement("applet")||
					e.isHtmlElement("caption")||
					e.isHtmlElement("html")||
					e.isHtmlElement("table")||
					e.isHtmlElement("td")||
					e.isHtmlElement("th")||
					e.isHtmlElement("ol")||
					e.isHtmlElement("ul")||
					e.isHtmlElement("marquee")||
					e.isHtmlElement("object")||
					e.isMathMLElement("mi")||
					e.isMathMLElement("mo")||
					e.isMathMLElement("mn")||
					e.isMathMLElement("ms")||
					e.isMathMLElement("mtext")||
					e.isMathMLElement("annotation-xml")||
					e.isSvgElement("foreignObject")||
					e.isSvgElement("desc")||
					e.isSvgElement("title")
					)
				return false;
		}
		return false;
	}
	private boolean hasHtmlElementInScope(Element node) {
		for(int i=openElements.size()-1;i>=0;i--){
			Element e=openElements.get(i);
			if(e==node)
				return true;
			if(e.isHtmlElement("applet")||
					e.isHtmlElement("caption")||
					e.isHtmlElement("html")||
					e.isHtmlElement("table")||
					e.isHtmlElement("td")||
					e.isHtmlElement("th")||
					e.isHtmlElement("marquee")||
					e.isHtmlElement("object")||
					e.isMathMLElement("mi")||
					e.isMathMLElement("mo")||
					e.isMathMLElement("mn")||
					e.isMathMLElement("ms")||
					e.isMathMLElement("mtext")||
					e.isMathMLElement("annotation-xml")||
					e.isSvgElement("foreignObject")||
					e.isSvgElement("desc")||
					e.isSvgElement("title")
					)
				return false;
		}
		return false;
	}
	private boolean hasHtmlElementInScope(String name){
		for(int i=openElements.size()-1;i>=0;i--){
			Element e=openElements.get(i);
			if(e.isHtmlElement(name))
				return true;
			if(e.isHtmlElement("applet")||
					e.isHtmlElement("caption")||
					e.isHtmlElement("html")||
					e.isHtmlElement("table")||
					e.isHtmlElement("td")||
					e.isHtmlElement("th")||
					e.isHtmlElement("marquee")||
					e.isHtmlElement("object")||
					e.isMathMLElement("mi")||
					e.isMathMLElement("mo")||
					e.isMathMLElement("mn")||
					e.isMathMLElement("ms")||
					e.isMathMLElement("mtext")||
					e.isMathMLElement("annotation-xml")||
					e.isSvgElement("foreignObject")||
					e.isSvgElement("desc")||
					e.isSvgElement("title")
					)
				return false;
		}
		return false;
	}
	private boolean hasHtmlElementInSelectScope(String name) {
		for(int i=openElements.size()-1;i>=0;i--){
			Element e=openElements.get(i);
			if(e.isHtmlElement(name))
				return true;
			if(!e.isHtmlElement("optgroup") && !e.isHtmlElement("option"))
				return false;
		}
		return false;
	}

	private boolean hasHtmlElementInTableScope(String name) {
		for(int i=openElements.size()-1;i>=0;i--){
			Element e=openElements.get(i);
			if(e.isHtmlElement(name))
				return true;
			if(e.isHtmlElement("html")||
					e.isHtmlElement("table")
					)
				return false;
		}
		return false;

	}



	private boolean hasHtmlHeaderElementInScope() {
		for(int i=openElements.size()-1;i>=0;i--){
			Element e=openElements.get(i);
			if(e.isHtmlElement("h1")||
					e.isHtmlElement("h2")||
					e.isHtmlElement("h3")||
					e.isHtmlElement("h4")||
					e.isHtmlElement("h5")||
					e.isHtmlElement("h6"))
				return true;
			if(e.isHtmlElement("applet")||
					e.isHtmlElement("caption")||
					e.isHtmlElement("html")||
					e.isHtmlElement("table")||
					e.isHtmlElement("td")||
					e.isHtmlElement("th")||
					e.isHtmlElement("marquee")||
					e.isHtmlElement("object")||
					e.isMathMLElement("mi")||
					e.isMathMLElement("mo")||
					e.isMathMLElement("mn")||
					e.isMathMLElement("ms")||
					e.isMathMLElement("mtext")||
					e.isMathMLElement("annotation-xml")||
					e.isSvgElement("foreignObject")||
					e.isSvgElement("desc")||
					e.isSvgElement("title")
					)
				return false;
		}
		return false;
	}

	private boolean hasNativeElementInScope() {
		for(int i=openElements.size()-1;i>=0;i--){
			Element e=openElements.get(i);
			//DebugUtility.log("%s %s",e.getLocalName(),e.getNamespaceURI());
			if(e.getNamespaceURI().equals(HTML_NAMESPACE) ||
					isMathMLTextIntegrationPoint(e) ||
					isHtmlIntegrationPoint(e))
				return true;
			if(e.isHtmlElement("applet")||
					e.isHtmlElement("caption")||
					e.isHtmlElement("html")||
					e.isHtmlElement("table")||
					e.isHtmlElement("td")||
					e.isHtmlElement("th")||
					e.isHtmlElement("marquee")||
					e.isHtmlElement("object")||
					e.isMathMLElement("mi")||
					e.isMathMLElement("mo")||
					e.isMathMLElement("mn")||
					e.isMathMLElement("ms")||
					e.isMathMLElement("mtext")||
					e.isMathMLElement("annotation-xml")||
					e.isSvgElement("foreignObject")||
					e.isSvgElement("desc")||
					e.isSvgElement("title")
					)
				return false;
		}
		return false;
	}

	private void initialize(){
		noforeign=false;
		document=new Document();
		document.address=address;
		document.setBaseURI(address);
		context=null;
		openElements.clear();
		error=false;
		baseurl=null;
		hasForeignContent=false; // performance optimization
		lastState=TokenizerState.Data;
		lastComment=null;
		docTypeToken=null;
		tokens.clear();
		lastStartTag=null;
		currentEndTag=null;
		currentTag=null;
		currentAttribute=null;
		bogusCommentCharacter=0;
		tempBuffer.clearAll();
		state=TokenizerState.Data;
		framesetOk=true;
		integrationElements.clear();
		tokenQueue.clear();
		insertionMode=InsertionMode.Initial;
		originalInsertionMode=InsertionMode.Initial;
		formattingElements.clear();
		doFosterParent=false;
		headElement=null;
		formElement=null;
		inputElement=null;
		done=false;
		pendingTableCharacters.clearAll();
	}

	private void insertCharacter(Node node, int ch){
		Text textNode=getTextNodeToInsert(node);
		if(textNode!=null) {
			textNode.text.appendInt(ch);
		}
	}

	private Element insertForeignElement(StartTagToken tag, String namespace) {
		Element element=Element.fromToken(tag,namespace);
		String xmlns=element.getAttributeNS(XMLNS_NAMESPACE,"xmlns");
		String xlink=element.getAttributeNS(XMLNS_NAMESPACE,"xlink");
		if(xmlns!=null && !xmlns.equals(namespace)){
			error=true;
		}
		if(xlink!=null && !xlink.equals(XLINK_NAMESPACE)){
			error=true;
		}
		Element currentNode=getCurrentNode();
		if(currentNode!=null) {
			insertInCurrentNode(element);
		} else {
			document.appendChild(element);
		}
		openElements.add(element);
		return element;
	}

	private void insertFormattingMarker(StartTagToken tag,
			Element addHtmlElement) {
		FormattingElement fe=new FormattingElement();
		fe.marker=true;
		fe.element=addHtmlElement;
		fe.token=tag;
		formattingElements.add(fe);
	}

	private void insertInCurrentNode(Node element){
		Element node=getCurrentNode();
		if(doFosterParent){
			String name=node.getLocalName();
			if("table".equals(name) ||
					"tbody".equals(name) ||
					"tfoot".equals(name) ||
					"thead".equals(name) ||
					"tr".equals(name)){
				fosterParent(element);
			} else {
				node.appendChild(element);
			}
		} else {
			node.appendChild(element);
		}
	}


	private void insertString(Node node, String str){
		Text textNode=getTextNodeToInsert(node);
		if(textNode!=null) {
			textNode.text.appendString(str);
		}
	}
	private boolean isAppropriateEndTag(){
		if(lastStartTag==null || currentEndTag==null)
			return false;
		//DebugUtility.log("lastStartTag=%s",lastStartTag.getName());
		//DebugUtility.log("currentEndTag=%s",currentEndTag.getName());
		return currentEndTag.getName().equals(lastStartTag.getName());
	}

	public boolean isError() {
		return error;
	}
	private boolean isForeignContext(int token){
		if(hasForeignContent && token!=TOKEN_EOF){
			Element element=(context!=null && openElements.size()==1) ?
					context : getCurrentNode(); // adjusted current node
			if(element==null)return false;
			if(element.getNamespaceURI().equals(HTML_NAMESPACE))
				return false;
			if((token&TOKEN_TYPE_MASK)==TOKEN_START_TAG){
				StartTagToken tag=(StartTagToken)getToken(token);
				String name=element.getLocalName();
				if(isMathMLTextIntegrationPoint(element)){
					String tokenName=tag.getName();
					if(!"mglyph".equals(tokenName) &&
							!"malignmark".equals(tokenName))
						return false;
				}
				if(MATHML_NAMESPACE.equals(element.getNamespaceURI()) && (
						name.equals("annotation-xml")) &&
						"svg".equals(tag.getName()))
					return false;
				if(isHtmlIntegrationPoint(element))
					return false;
				return true;
			} else if((token&TOKEN_TYPE_MASK)==TOKEN_CHARACTER){
				if(isMathMLTextIntegrationPoint(element) ||
						isHtmlIntegrationPoint(element))
					return false;
				return true;
			} else
				return true;
		}
		return false;
	}
	private boolean isHtmlIntegrationPoint(Element element) {
		if(integrationElements.contains(element))
			return true;
		String name=element.getLocalName();
		return SVG_NAMESPACE.equals(element.getNamespaceURI()) && (
				name.equals("foreignObject") ||
				name.equals("desc") ||
				name.equals("title"));
	}

	private boolean isMathMLTextIntegrationPoint(Element element) {
		String name=element.getLocalName();
		return MATHML_NAMESPACE.equals(element.getNamespaceURI()) && (
				name.equals("mi") ||
				name.equals("mo") ||
				name.equals("mn") ||
				name.equals("ms") ||
				name.equals("mtext"));
	}

	private boolean isSpecialElement(Element node) {
		if(node.isHtmlElement("address") || node.isHtmlElement("applet") || node.isHtmlElement("area") || node.isHtmlElement("article") || node.isHtmlElement("aside") || node.isHtmlElement("base") || node.isHtmlElement("basefont") || node.isHtmlElement("bgsound") || node.isHtmlElement("blockquote") || node.isHtmlElement("body") || node.isHtmlElement("br") || node.isHtmlElement("button") || node.isHtmlElement("caption") || node.isHtmlElement("center") || node.isHtmlElement("col") || node.isHtmlElement("colgroup") || node.isHtmlElement("dd") || node.isHtmlElement("details") || node.isHtmlElement("dir") || node.isHtmlElement("div") || node.isHtmlElement("dl") || node.isHtmlElement("dt") || node.isHtmlElement("embed") || node.isHtmlElement("fieldset") || node.isHtmlElement("figcaption") || node.isHtmlElement("figure")
				|| node.isHtmlElement("footer") || node.isHtmlElement("form") || node.isHtmlElement("frame") || node.isHtmlElement("frameset") || node.isHtmlElement("h1") || node.isHtmlElement("h2") || node.isHtmlElement("h3") || node.isHtmlElement("h4") || node.isHtmlElement("h5") || node.isHtmlElement("h6") || node.isHtmlElement("head") || node.isHtmlElement("header") || node.isHtmlElement("hgroup") || node.isHtmlElement("hr") || node.isHtmlElement("html") || node.isHtmlElement("iframe") || node.isHtmlElement("img") || node.isHtmlElement("input") || node.isHtmlElement("isindex") || node.isHtmlElement("li") || node.isHtmlElement("link") ||
				node.isHtmlElement("listing") || node.isHtmlElement("main") || node.isHtmlElement("marquee") || node.isHtmlElement("menu") || node.isHtmlElement("menuitem") || node.isHtmlElement("meta") || node.isHtmlElement("nav") || node.isHtmlElement("noembed") || node.isHtmlElement("noframes") || node.isHtmlElement("noscript") || node.isHtmlElement("object") || node.isHtmlElement("ol") || node.isHtmlElement("p") || node.isHtmlElement("param") || node.isHtmlElement("plaintext") || node.isHtmlElement("pre") || node.isHtmlElement("script") || node.isHtmlElement("section") ||
				node.isHtmlElement("select") || node.isHtmlElement("source") || node.isHtmlElement("style") || node.isHtmlElement("summary") || node.isHtmlElement("table") || node.isHtmlElement("tbody") || node.isHtmlElement("td") || node.isHtmlElement("textarea") || node.isHtmlElement("tfoot") || node.isHtmlElement("th") || node.isHtmlElement("thead") || node.isHtmlElement("title") || node.isHtmlElement("tr") || node.isHtmlElement("track") || node.isHtmlElement("ul") || node.isHtmlElement("wbr") || node.isHtmlElement("xmp"))
			return true;
		if(node.isMathMLElement("mi") || node.isMathMLElement("mo") || node.isMathMLElement("mn") || node.isMathMLElement("ms") || node.isMathMLElement("mtext") || node.isMathMLElement("annotation-xml"))
			return true;
		if(node.isSvgElement("foreignObject") || node.isSvgElement("desc") || node.isSvgElement("title"))
			return true;

		return false;
	}
	 String nodesToDebugString(List<Node> nodes){
		StringBuilder builder=new StringBuilder();
		for(Node node : nodes){
			String str=node.toDebugString();
			String[] strarray=StringUtility.splitAt(str,"\n");
			int len=strarray.length;
			if(len>0 && strarray[len-1].length()==0)
			{
				len--; // ignore trailing empty string
			}
			for(int i=0;i<len;i++){
				String el=strarray[i];
				builder.append("| ");
				builder.append(el.replace("~~~~","\n"));
				builder.append("\n");
			}
		}
		return builder.toString();
	}
	public IDocument parse() throws IOException {
		while(true){
			int token=parserRead();
			applyInsertionMode(token,null);
			if((token&TOKEN_TYPE_MASK)==TOKEN_START_TAG){
				StartTagToken tag=(StartTagToken)getToken(token);
				//	DebugUtility.log(tag);
				if(!tag.isAckSelfClosing()){
					error=true;
				}
			}
			//	DebugUtility.log("token=%08X, insertionMode=%s, error=%s",token,insertionMode,error);
			if(done){
				break;
			}
		}
		return document;
	}

	private int parseCharacterReference(int allowedCharacter) throws IOException{
		int markStart=charInput.setSoftMark();
		int c1=charInput.read();
		if(c1<0 || c1==0x09 || c1==0x0a || c1==0x0c ||
				c1==0x20 || c1==0x3c || c1==0x26 || (allowedCharacter>=0 && c1==allowedCharacter)){
			charInput.setMarkPosition(markStart);
			return 0x26; // emit ampersand
		} else if(c1==0x23){
			c1=charInput.read();
			int value=0;
			boolean haveHex=false;
			if(c1==0x78 || c1==0x58){
				// Hex number
				while(true){ // skip zeros
					int c=charInput.read();
					if(c!='0'){
						if(c>=0) {
							charInput.moveBack(1);
						}
						break;
					}
					haveHex=true;
				}
				boolean overflow=false;
				while(true){
					int number=charInput.read();
					if(number>='0' && number<='9'){
						if(!overflow) {
							value=(value<<4)+(number-'0');
						}
						haveHex=true;
					} else if(number>='a' && number<='f'){
						if(!overflow) {
							value=(value<<4)+(number-'a')+10;
						}
						haveHex=true;
					} else if(number>='A' && number<='F'){
						if(!overflow) {
							value=(value<<4)+(number-'A')+10;
						}
						haveHex=true;
					} else {
						if(number>=0) {
							// move back character (except if it's EOF)
							charInput.moveBack(1);
						}
						break;
					}
					if(value>0x10FFFF){
						value=0x110000; overflow=true;
					}
				}
			} else {
				if(c1>0) {
					charInput.moveBack(1);
				}
				// Digits
				while(true){ // skip zeros
					int c=charInput.read();
					if(c!='0'){
						if(c>=0) {
							charInput.moveBack(1);
						}
						break;
					}
					haveHex=true;
				}
				boolean overflow=false;
				while(true){
					int number=charInput.read();
					if(number>='0' && number<='9'){
						if(!overflow) {
							value=(value*10)+(number-'0');
						}
						haveHex=true;
					} else {
						if(number>=0) {
							// move back character (except if it's EOF)
							charInput.moveBack(1);
						}
						break;
					}
					if(value>0x10FFFF){
						value=0x110000; overflow=true;
					}
				}
			}
			if(!haveHex){
				// No digits: parse error
				error=true;
				charInput.setMarkPosition(markStart);
				return 0x26; // emit ampersand
			}
			c1=charInput.read();
			if(c1!=0x3B){ // semicolon
				error=true;
				if(c1>=0)
				{
					charInput.moveBack(1); // parse error
				}
			}
			if(value>0x10FFFF || (value>=0xD800 && value<=0xDFFF)){
				error=true;
				value=0xFFFD; // parse error
			} else if(value>=0x80 && value<0xA0){
				error=true;
				// parse error
				int replacements[]=new int[]{
						0x20ac,0x81,0x201a,0x192,0x201e,
						0x2026,0x2020,0x2021,0x2c6,0x2030,
						0x160,0x2039,0x152,0x8d,0x17d,
						0x8f,0x90,0x2018,0x2019,0x201c,0x201d,
						0x2022,0x2013,0x2014,0x2dc,0x2122,
						0x161,0x203a,0x153,0x9d,0x17e,0x178
				};
				value=replacements[value-0x80];
			} else if(value==0x0D){
				// parse error
				error=true;
			} else if(value==0x00){
				// parse error
				error=true;
				value=0xFFFD;
			}
			if(value==0x08 || value==0x0B ||
					(value&0xFFFE)==0xFFFE ||
					(value>=0x0e && value<=0x1f) ||
					value==0x7F || (value>=0xFDD0 && value<=0xFDEF)){
				// parse error
				error=true;
			}
			return value;
		} else if((c1>='A' && c1<='Z') ||
				(c1>='a' && c1<='z') ||
				(c1>='0' && c1<='9')){
			int[] data=null;
			// check for certain well-known entities
			if(c1=='g'){
				if(charInput.read()=='t' && charInput.read()==';')
					return '>';
				charInput.setMarkPosition(markStart+1);
			} else if(c1=='l'){
				if(charInput.read()=='t' && charInput.read()==';')
					return '<';
				charInput.setMarkPosition(markStart+1);
			} else if(c1=='a'){
				if(charInput.read()=='m' && charInput.read()=='p' && charInput.read()==';')
					return '&';
				charInput.setMarkPosition(markStart+1);
			} else if(c1=='n'){
				if(charInput.read()=='b' && charInput.read()=='s' && charInput.read()=='p' && charInput.read()==';')
					return 0xa0;
				charInput.setMarkPosition(markStart+1);
			}
			int count=0;
			for(int index=0;index<HtmlEntities.entities.length;index++){
				String entity=HtmlEntities.entities[index];
				if(entity.charAt(0)==c1){
					if(data==null){
						// Read the rest of the character reference
						// (the entities are sorted by length, so
						// we get the maximum length possible starting
						// with the first matching character)
						data=new int[entity.length()-1];
						count=charInput.read(data,0,data.length);
						//DebugUtility.log("markposch=%c",(char)data[0]);
					}
					// if fewer bytes were read than the
					// entity's remaining length, this
					// can't match
					//DebugUtility.log("data count=%s %s",count,stream.getMarkPosition());
					if(count<entity.length()-1) {
						continue;
					}
					boolean matched=true;
					for(int i=1;i<entity.length();i++){
						//DebugUtility.log("%c %c | markpos=%d",
						//	(char)data[i-1],entity.charAt(i),stream.getMarkPosition());
						if(data[i-1]!=entity.charAt(i)){
							matched=false;
							break;
						}
					}
					if(matched){
						// Move back the difference between the
						// number of bytes actually read and
						// this entity's length
						charInput.moveBack(count-(entity.length()-1));
						//DebugUtility.log("lastchar=%c",entity.charAt(entity.length()-1));
						if(allowedCharacter>=0 &&
								entity.charAt(entity.length()-1)!=';'){
							// Get the next character after the entity
							int ch2=charInput.read();
							if(ch2=='=' || (ch2>='A' && ch2<='Z') ||
									(ch2>='a' && ch2<='z') ||
									(ch2>='0' && ch2<='9')){
								if(ch2=='=') {
									error=true;
								}
								charInput.setMarkPosition(markStart);
								return 0x26; // return ampersand rather than entity
							} else {
								if(ch2>=0) {
									charInput.moveBack(1);
								}
								if(entity.charAt(entity.length()-1)!=';'){
									error=true;
								}
							}
						} else {
							if(entity.charAt(entity.length()-1)!=';'){
								error=true;
							}
						}
						return HtmlEntities.entityValues[index];
					}
				}
			}
			// no match
			charInput.setMarkPosition(markStart);
			while(true){
				int ch2=charInput.read();
				if(ch2==';'){
					error=true;
					break;
				} else if(!((ch2>='A' && ch2<='Z') ||
						(ch2>='a' && ch2<='z') ||
						(ch2>='0' && ch2<='9'))){
					break;
				}
			}
			charInput.setMarkPosition(markStart);
			return 0x26;
		} else {
			// not a character reference
			charInput.setMarkPosition(markStart);
			return 0x26; // emit ampersand
		}
	}

	public List<Node> parseFragment(Element context) throws IOException{
		if(context==null)
			throw new IllegalArgumentException();
		initialize();
		document=new Document();
		INode ownerDocument=context;
		INode lastForm=null;
		while(ownerDocument!=null){
			if(lastForm==null && ownerDocument.getNodeType()==NodeType.ELEMENT_NODE){
				String name=((Element)ownerDocument).getLocalName();
				if(name.equals("form")){
					lastForm=ownerDocument;
				}
			}
			ownerDocument=ownerDocument.getParentNode();
			if(ownerDocument==null ||
					ownerDocument.getNodeType()==NodeType.DOCUMENT_NODE){
				break;
			}
		}
		Document ownerDoc=null;
		if(ownerDocument!=null && ownerDocument.getNodeType()==NodeType.DOCUMENT_NODE){
			ownerDoc=(Document)ownerDocument;
			document.setMode(ownerDoc.getMode());
		}
		String name2=context.getLocalName();
		state=TokenizerState.Data;
		if(name2.equals("title")||name2.equals("textarea")){
			state=TokenizerState.RcData;
		} else if(name2.equals("style") || name2.equals("xmp") ||
				name2.equals("iframe") || name2.equals("noembed") ||
				name2.equals("noframes")){
			state=TokenizerState.RawText;
		} else if(name2.equals("script")){
			state=TokenizerState.ScriptData;
		} else if(name2.equals("noscript")){
			state=TokenizerState.Data;
		} else if(name2.equals("plaintext")){
			state=TokenizerState.PlainText;
		}
		Element element=new Element();
		element.setLocalName("html");
		element.setNamespace(HTML_NAMESPACE);
		document.appendChild(element);
		done=false;
		openElements.clear();
		openElements.add(element);
		this.context=context;
		resetInsertionMode();
		formElement=(lastForm==null) ? null : ((Element)lastForm);
		if(encoding.getConfidence()!=EncodingConfidence.Irrelevant){
			encoding=new EncodingConfidence(encoding.getEncoding(),
					EncodingConfidence.Irrelevant);
		}
		parse();
		return new ArrayList<Node>(element.getChildNodesInternal());
	}

	public List<Node> parseFragment(String contextName) throws IOException{
		Element element=new Element();
		element.setLocalName(contextName);
		element.setNamespace(HTML_NAMESPACE);
		return parseFragment(element);
	}

	 int parserRead() throws IOException{
		int token=parserReadInternal();
		//DebugUtility.log("token=%08X [%c]",token,token&0xFF);
		if(decoder.isError()) {
			error=true;
		}
		return token;
	}

	private int parserReadInternal() throws IOException{
		if(tokenQueue.size()>0)
			return removeAtIndex(tokenQueue,0);
		while(true){
			//DebugUtility.log(state);
			switch(state){
			case Data:
				int c=charInput.read();
				if(c==0x26){
					state=TokenizerState.CharacterRefInData;
				} else if(c==0x3c){
					state=TokenizerState.TagOpen;
				} else if(c==0){
					error=true;
					return c;
				} else if(c<0)
					return TOKEN_EOF;
				else {
					int ret=c;
					// Keep reading characters to
					// reduce the need to re-call
					// this method
					int mark=charInput.setSoftMark();
					for(int i=0;i<100;i++){
						c=charInput.read();
						if(c>0 && c!=0x26 && c!=0x3c){
							tokenQueue.add(c);
						} else {
							charInput.setMarkPosition(mark+i);
							break;
						}
					}
					return ret;
				}
				break;
			case CharacterRefInData:{
				state=TokenizerState.Data;
				int charref=parseCharacterReference(-1);
				if(charref<0){
					// more than one character in this reference
					int index=Math.abs(charref+1);
					tokenQueue.add(HtmlEntities.entityDoubles[index*2+1]);
					return HtmlEntities.entityDoubles[index*2];
				}
				return charref;
			}
			case CharacterRefInRcData:{
				state=TokenizerState.RcData;
				int charref=parseCharacterReference(-1);
				if(charref<0){
					// more than one character in this reference
					int index=Math.abs(charref+1);
					tokenQueue.add(HtmlEntities.entityDoubles[index*2+1]);
					return HtmlEntities.entityDoubles[index*2];
				}
				return charref;
			}
			case RcData:
				int c1=charInput.read();
				if(c1==0x26) {
					state=TokenizerState.CharacterRefInRcData;
				} else if(c1==0x3c) {
					state=TokenizerState.RcDataLessThan;
				} else if(c1==0){
					error=true;
					return 0xFFFD;
				}
				else if(c1<0)
					return TOKEN_EOF;
				else
					return c1;
				break;
			case RawText:
			case ScriptData:{
				int c11=charInput.read();
				if(c11==0x3c) {
					state=(state==TokenizerState.RawText) ?
							TokenizerState.RawTextLessThan :
								TokenizerState.ScriptDataLessThan;
				} else if(c11==0){
					error=true;
					return 0xFFFD;
				}
				else if(c11<0)
					return TOKEN_EOF;
				else
					return c11;
				break;
			}
			case ScriptDataLessThan:{
				charInput.setHardMark();
				int c11=charInput.read();
				if(c11==0x2f){
					tempBuffer.clearAll();
					state=TokenizerState.ScriptDataEndTagOpen;
				} else if(c11==0x21){
					state=TokenizerState.ScriptDataEscapeStart;
					tokenQueue.add(0x21);
					return '<';
				} else {
					state=TokenizerState.ScriptData;
					if(c11>=0) {
						charInput.moveBack(1);
					}
					return 0x3c;
				}
				break;
			}
			case ScriptDataEndTagOpen:
			case ScriptDataEscapedEndTagOpen:{
				charInput.setHardMark();
				int ch=charInput.read();
				if(ch>='A' && ch<='Z'){
					EndTagToken token=new EndTagToken((char) (ch+0x20));
					tempBuffer.appendInt(ch);
					currentTag=token;
					currentEndTag=token;
					if(state==TokenizerState.ScriptDataEndTagOpen) {
						state=TokenizerState.ScriptDataEndTagName;
					} else {
						state=TokenizerState.ScriptDataEscapedEndTagName;
					}
				} else if(ch>='a' && ch<='z'){
					EndTagToken token=new EndTagToken((char)ch);
					tempBuffer.appendInt(ch);
					currentTag=token;
					currentEndTag=token;
					if(state==TokenizerState.ScriptDataEndTagOpen) {
						state=TokenizerState.ScriptDataEndTagName;
					} else {
						state=TokenizerState.ScriptDataEscapedEndTagName;
					}
				} else {
					if(state==TokenizerState.ScriptDataEndTagOpen) {
						state=TokenizerState.ScriptData;
					} else {
						state=TokenizerState.ScriptDataEscaped;
					}
					tokenQueue.add(0x2f);
					if(ch>=0) {
						charInput.moveBack(1);
					}
					return 0x3c;
				}
				break;
			}
			case ScriptDataEndTagName:
			case ScriptDataEscapedEndTagName:{
				charInput.setHardMark();
				int ch=charInput.read();
				if((ch==0x09 || ch==0x0a || ch==0x0c || ch==0x20) &&
						isAppropriateEndTag()){
					state=TokenizerState.BeforeAttributeName;
				} else if(ch==0x2f && isAppropriateEndTag()){
					state=TokenizerState.SelfClosingStartTag;
				} else if(ch==0x3e && isAppropriateEndTag()){
					state=TokenizerState.Data;
					return emitCurrentTag();
				} else if(ch>='A' && ch<='Z'){
					currentTag.appendChar((char) (ch+0x20));
					tempBuffer.appendInt(ch);
				} else if(ch>='a' && ch<='z'){
					currentTag.appendChar((char)ch);
					tempBuffer.appendInt(ch);
				} else {
					if(state==TokenizerState.ScriptDataEndTagName) {
						state=TokenizerState.ScriptData;
					} else {
						state=TokenizerState.ScriptDataEscaped;
					}
					tokenQueue.add(0x2f);
					int[] array=tempBuffer.array();
					for(int i=0;i<tempBuffer.size();i++){
						tokenQueue.add(array[i]);
					}
					if(ch>=0) {
						charInput.moveBack(1);
					}
					return '<';
				}
				break;
			}
			case ScriptDataDoubleEscapeStart:{
				charInput.setHardMark();
				int ch=charInput.read();
				if(ch==0x09 || ch==0x0a || ch==0x0c || ch==0x20 ||
						ch==0x2f || ch==0x3e){
					String bufferString=tempBuffer.toString();
					if(bufferString.equals("script")){
						state=TokenizerState.ScriptDataDoubleEscaped;
					} else {
						state=TokenizerState.ScriptDataEscaped;
					}
					return ch;
				} else if(ch>='A' && ch<='Z'){
					tempBuffer.appendInt(ch+0x20);
					return ch;
				} else if(ch>='a' && ch<='z'){
					tempBuffer.appendInt(ch);
					return ch;
				} else {
					state=TokenizerState.ScriptDataEscaped;
					if(ch>=0) {
						charInput.moveBack(1);
					}
				}
				break;
			}
			case ScriptDataDoubleEscapeEnd:{
				charInput.setHardMark();
				int ch=charInput.read();
				if(ch==0x09 || ch==0x0a || ch==0x0c || ch==0x20 ||
						ch==0x2f || ch==0x3e){
					String bufferString=tempBuffer.toString();
					if(bufferString.equals("script")){
						state=TokenizerState.ScriptDataEscaped;
					} else {
						state=TokenizerState.ScriptDataDoubleEscaped;
					}
					return ch;
				} else if(ch>='A' && ch<='Z'){
					tempBuffer.appendInt(ch+0x20);
					return ch;
				} else if(ch>='a' && ch<='z'){
					tempBuffer.appendInt(ch);
					return ch;
				} else {
					state=TokenizerState.ScriptDataDoubleEscaped;
					if(ch>=0) {
						charInput.moveBack(1);
					}
				}
				break;
			}
			case ScriptDataEscapeStart:
			case ScriptDataEscapeStartDash:{
				charInput.setHardMark();
				int ch=charInput.read();
				if(ch==0x2d){
					if(state==TokenizerState.ScriptDataEscapeStart) {
						state=TokenizerState.ScriptDataEscapeStartDash;
					} else {
						state=TokenizerState.ScriptDataEscapedDashDash;
					}
					return '-';
				} else {
					if(ch>=0) {
						charInput.moveBack(1);
					}
					state=TokenizerState.ScriptData;
				}
				break;
			}
			case ScriptDataEscaped:{
				int ch=charInput.read();
				if(ch==0x2d){
					state=TokenizerState.ScriptDataEscapedDash;
					return '-';
				} else if(ch==0x3c){
					state=TokenizerState.ScriptDataEscapedLessThan;
				} else if(ch==0){
					error=true;
					return 0xFFFD;
				} else if(ch<0){
					error=true;
					state=TokenizerState.Data;
				} else
					return ch;
				break;
			}
			case ScriptDataDoubleEscaped:{
				int ch=charInput.read();
				if(ch==0x2d){
					state=TokenizerState.ScriptDataDoubleEscapedDash;
					return '-';
				} else if(ch==0x3c){
					state=TokenizerState.ScriptDataDoubleEscapedLessThan;
					return '<';
				} else if(ch==0){
					error=true;
					return 0xFFFD;
				} else if(ch<0){
					error=true;
					state=TokenizerState.Data;
				} else
					return ch;
				break;
			}
			case ScriptDataEscapedDash:{
				int ch=charInput.read();
				if(ch==0x2d){
					state=TokenizerState.ScriptDataEscapedDashDash;
					return '-';
				} else if(ch==0x3c){
					state=TokenizerState.ScriptDataEscapedLessThan;
				} else if(ch==0){
					error=true;
					state=TokenizerState.ScriptDataEscaped;
					return 0xFFFD;
				} else if(ch<0){
					error=true;
					state=TokenizerState.Data;
				} else {
					state=TokenizerState.ScriptDataEscaped;
					return ch;
				}
				break;
			}
			case ScriptDataDoubleEscapedDash:{
				int ch=charInput.read();
				if(ch==0x2d){
					state=TokenizerState.ScriptDataDoubleEscapedDashDash;
					return '-';
				} else if(ch==0x3c){
					state=TokenizerState.ScriptDataDoubleEscapedLessThan;
					return '<';
				} else if(ch==0){
					error=true;
					state=TokenizerState.ScriptDataDoubleEscaped;
					return 0xFFFD;
				} else if(ch<0){
					error=true;
					state=TokenizerState.Data;
				} else {
					state=TokenizerState.ScriptDataDoubleEscaped;
					return ch;
				}
				break;
			}
			case ScriptDataEscapedDashDash:{
				int ch=charInput.read();
				if(ch==0x2d)
					return '-';
				else if(ch==0x3c){
					state=TokenizerState.ScriptDataEscapedLessThan;
				} else if(ch==0x3e){
					state=TokenizerState.ScriptData;
					return '>';
				} else if(ch==0){
					error=true;
					state=TokenizerState.ScriptDataEscaped;
					return 0xFFFD;
				} else if(ch<0){
					error=true;
					state=TokenizerState.Data;
				} else {
					state=TokenizerState.ScriptDataEscaped;
					return ch;
				}
				break;
			}
			case ScriptDataDoubleEscapedDashDash:{
				int ch=charInput.read();
				if(ch==0x2d)
					return '-';
				else if(ch==0x3c){
					state=TokenizerState.ScriptDataDoubleEscapedLessThan;
					return '<';
				} else if(ch==0x3e){
					state=TokenizerState.ScriptData;
					return '>';
				} else if(ch==0){
					error=true;
					state=TokenizerState.ScriptDataDoubleEscaped;
					return 0xFFFD;
				} else if(ch<0){
					error=true;
					state=TokenizerState.Data;
				} else {
					state=TokenizerState.ScriptDataDoubleEscaped;
					return ch;
				}
				break;
			}
			case ScriptDataDoubleEscapedLessThan:{
				charInput.setHardMark();
				int ch=charInput.read();
				if(ch==0x2f){
					tempBuffer.clearAll();
					state=TokenizerState.ScriptDataDoubleEscapeEnd;
					return 0x2f;
				} else {
					state=TokenizerState.ScriptDataDoubleEscaped;
					if(ch>=0) {
						charInput.moveBack(1);
					}
				}
				break;
			}
			case ScriptDataEscapedLessThan:{
				charInput.setHardMark();
				int ch=charInput.read();
				if(ch==0x2f){
					tempBuffer.clearAll();
					state=TokenizerState.ScriptDataEscapedEndTagOpen;
				} else if(ch>='A' && ch<='Z'){
					tempBuffer.clearAll();
					tempBuffer.appendInt(ch+0x20);
					state=TokenizerState.ScriptDataDoubleEscapeStart;
					tokenQueue.add(ch);
					return 0x3c;
				} else if(ch>='a' && ch<='z'){
					tempBuffer.clearAll();
					tempBuffer.appendInt(ch);
					state=TokenizerState.ScriptDataDoubleEscapeStart;
					tokenQueue.add(ch);
					return 0x3c;
				} else {
					state=TokenizerState.ScriptDataEscaped;
					if(ch>=0) {
						charInput.moveBack(1);
					}
					return 0x3c;
				}
				break;
			}
			case PlainText:{
				int c11=charInput.read();
				if(c11==0){
					error=true;
					return 0xFFFD;
				}
				else if(c11<0)
					return TOKEN_EOF;
				else
					return c11;
			}
			case TagOpen:{
				charInput.setHardMark();
				int c11=charInput.read();
				if(c11==0x21) {
					state=TokenizerState.MarkupDeclarationOpen;
				} else if(c11==0x2F) {
					state=TokenizerState.EndTagOpen;
				} else if(c11>='A' && c11<='Z'){
					TagToken token=new StartTagToken((char) (c11+0x20));
					currentTag=token;
					state=TokenizerState.TagName;
				}
				else if(c11>='a' && c11<='z'){
					TagToken token=new StartTagToken((char) (c11));
					currentTag=token;
					state=TokenizerState.TagName;
				}
				else if(c11==0x3F){
					error=true;
					bogusCommentCharacter=c11;
					state=TokenizerState.BogusComment;
				} else {
					error=true;
					state=TokenizerState.Data;
					if(c11>=0) {
						charInput.moveBack(1);
					}
					return '<';
				}
				break;
			}
			case EndTagOpen:{
				int ch=charInput.read();
				if(ch>='A' && ch<='Z'){
					TagToken token=new EndTagToken((char) (ch+0x20));
					currentEndTag=token;
					currentTag=token;
					state=TokenizerState.TagName;
				}
				else if(ch>='a' && ch<='z'){
					TagToken token=new EndTagToken((char) (ch));
					currentEndTag=token;
					currentTag=token;
					state=TokenizerState.TagName;
				}
				else if(ch==0x3e){
					error=true;
					state=TokenizerState.Data;
				}
				else if(ch<0){
					error=true;
					state=TokenizerState.Data;
					tokenQueue.add(0x2F); // solidus
					return 0x3C; // Less than
				}
				else {
					error=true;
					bogusCommentCharacter=ch;
					state=TokenizerState.BogusComment;
				}
				break;
			}
			case RcDataEndTagOpen:
			case RawTextEndTagOpen:{
				charInput.setHardMark();
				int ch=charInput.read();
				if(ch>='A' && ch<='Z'){
					TagToken token=new EndTagToken((char) (ch+0x20));
					tempBuffer.appendInt(ch);
					currentEndTag=token;
					currentTag=token;
					state=(state==TokenizerState.RcDataEndTagOpen) ?
							TokenizerState.RcDataEndTagName :
								TokenizerState.RawTextEndTagName;
				}
				else if(ch>='a' && ch<='z'){
					TagToken token=new EndTagToken((char) (ch));
					tempBuffer.appendInt(ch);
					currentEndTag=token;
					currentTag=token;
					state=(state==TokenizerState.RcDataEndTagOpen) ?
							TokenizerState.RcDataEndTagName :
								TokenizerState.RawTextEndTagName;
				}
				else {
					if(ch>=0) {
						charInput.moveBack(1);
					}
					state=TokenizerState.RcData;
					tokenQueue.add(0x2F); // solidus
					return 0x3C; // Less than
				}
				break;
			}
			case RcDataEndTagName:
			case RawTextEndTagName:{
				charInput.setHardMark();
				int ch=charInput.read();
				if((ch==0x09 || ch==0x0a || ch==0x0c || ch==0x20) && isAppropriateEndTag()){
					state=TokenizerState.BeforeAttributeName;
				} else if(ch==0x2f && isAppropriateEndTag()){
					state=TokenizerState.SelfClosingStartTag;
				} else if(ch==0x3e && isAppropriateEndTag()){
					state=TokenizerState.Data;
					return emitCurrentTag();
				} else if(ch>='A' && ch<='Z'){
					currentTag.append(ch+0x20);
					tempBuffer.appendInt(ch+0x20);
				} else if(ch>='a' && ch<='z'){
					currentTag.append(ch);
					tempBuffer.appendInt(ch);
				} else {
					if(ch>=0) {
						charInput.moveBack(1);
					}
					state=(state==TokenizerState.RcDataEndTagName) ?
							TokenizerState.RcData :
								TokenizerState.RawText;
					tokenQueue.add(0x2F); // solidus
					int[] array=tempBuffer.array();
					for(int i=0;i<tempBuffer.size();i++){
						tokenQueue.add(array[i]);
					}
					return 0x3C; // Less than
				}
				break;
			}
			case BeforeAttributeName:{
				int ch=charInput.read();
				if(ch==0x09 || ch==0x0a || ch==0x0c || ch==0x20){
					// ignored
				} else if(ch==0x2f){
					state=TokenizerState.SelfClosingStartTag;
				} else if(ch==0x3e){
					state=TokenizerState.Data;
					return emitCurrentTag();
				} else if(ch>='A' && ch<='Z'){
					currentAttribute=currentTag.addAttribute((char)(ch+0x20));
					state=TokenizerState.AttributeName;
				} else if(ch==0){
					error=true;
					currentAttribute=currentTag.addAttribute((char)(0xFFFD));
					state=TokenizerState.AttributeName;
				} else if(ch<0){
					error=true;
					state=TokenizerState.Data;
				} else {
					if(ch==0x22 || ch==0x27 || ch==0x3c || ch==0x3d){
						error=true;
					}
					currentAttribute=currentTag.addAttribute(ch);
					state=TokenizerState.AttributeName;
				}
				break;
			}
			case AttributeName:{
				int ch=charInput.read();
				if(ch==0x09 || ch==0x0a || ch==0x0c || ch==0x20){
					if(!currentTag.checkAttributeName()) {
						error=true;
					}
					state=TokenizerState.AfterAttributeName;
				} else if(ch==0x2f){
					if(!currentTag.checkAttributeName()) {
						error=true;
					}
					state=TokenizerState.SelfClosingStartTag;
				} else if(ch==0x3d){
					if(!currentTag.checkAttributeName()) {
						error=true;
					}
					state=TokenizerState.BeforeAttributeValue;
				} else if(ch==0x3e){
					if(!currentTag.checkAttributeName()) {
						error=true;
					}
					state=TokenizerState.Data;
					return emitCurrentTag();

				} else if(ch>='A' && ch<='Z'){
					currentAttribute.appendToName(ch+0x20);
				} else if(ch==0){
					error=true;
					currentAttribute.appendToName(0xfffd);
				} else if(ch<0){
					error=true;
					if(!currentTag.checkAttributeName()) {
						error=true;
					}
					state=TokenizerState.Data;
				} else if(ch==0x22 || ch==0x27 || ch==0x3c){
					error=true;
					currentAttribute.appendToName(ch);
				} else {
					currentAttribute.appendToName(ch);
				}
				break;
			}
			case AfterAttributeName:{
				int ch=charInput.read();
				while(ch==0x09 || ch==0x0a || ch==0x0c || ch==0x20){
					ch=charInput.read();
				}
				if(ch==0x2f){
					state=TokenizerState.SelfClosingStartTag;
				} else if(ch=='='){
					state=TokenizerState.BeforeAttributeValue;
				} else if(ch=='>'){
					state=TokenizerState.Data;
					return emitCurrentTag();

				} else if(ch>='A' && ch<='Z'){
					currentAttribute=currentTag.addAttribute((char)(ch+0x20));
					state=TokenizerState.AttributeName;
				} else if(ch==0){
					error=true;
					currentAttribute=currentTag.addAttribute((char)(0xFFFD));
					state=TokenizerState.AttributeName;
				} else if(ch<0){
					error=true;
					state=TokenizerState.Data;
				} else {
					if(ch==0x22 || ch==0x27 || ch==0x3c){
						error=true;
					}
					currentAttribute=currentTag.addAttribute(ch);
					state=TokenizerState.AttributeName;
				}
				break;
			}
			case BeforeAttributeValue:{
				charInput.setHardMark();
				int ch=charInput.read();
				while(ch==0x09 || ch==0x0a || ch==0x0c || ch==0x20){
					ch=charInput.read();
				}
				if(ch==0x22){
					state=TokenizerState.AttributeValueDoubleQuoted;
				} else if(ch==0x26){
					charInput.moveBack(1);
					state=TokenizerState.AttributeValueUnquoted;
				} else if(ch==0x27){
					state=TokenizerState.AttributeValueSingleQuoted;
				} else if(ch==0){
					error=true;
					currentAttribute.appendToValue(0xFFFD);
					state=TokenizerState.AttributeValueUnquoted;
				} else if(ch==0x3e){
					error=true;
					state=TokenizerState.Data;
					return emitCurrentTag();
				} else if(ch==0x3c || ch==0x3d || ch==0x60){
					error=true;
					currentAttribute.appendToValue(ch);
					state=TokenizerState.AttributeValueUnquoted;
				} else if(ch<0){
					error=true;
					state=TokenizerState.Data;
				} else {
					currentAttribute.appendToValue(ch);
					state=TokenizerState.AttributeValueUnquoted;
				}
				break;
			}
			case AttributeValueDoubleQuoted:{
				int ch=charInput.read();
				if(ch==0x22){
					currentAttribute.commitValue();
					state=TokenizerState.AfterAttributeValueQuoted;
				} else if(ch==0x26){
					lastState=state;
					state=TokenizerState.CharacterRefInAttributeValue;
				} else if(ch==0){
					error=true;
					currentAttribute.appendToValue(0xfffd);
				} else if(ch<0){
					error=true;
					state=TokenizerState.Data;
				} else {
					currentAttribute.appendToValue(ch);
					// Keep reading characters to
					// reduce the need to re-call
					// this method
					int mark=charInput.setSoftMark();
					for(int i=0;i<100;i++){
						ch=charInput.read();
						if(ch>0 && ch!=0x26 && ch!=0x22){
							currentAttribute.appendToValue(ch);
						} else if(ch==0x22){
							currentAttribute.commitValue();
							state=TokenizerState.AfterAttributeValueQuoted;
							break;
						} else {
							charInput.setMarkPosition(mark+i);
							break;
						}
					}
				}
				break;
			}
			case AttributeValueSingleQuoted:{
				int ch=charInput.read();
				if(ch==0x27){
					currentAttribute.commitValue();
					state=TokenizerState.AfterAttributeValueQuoted;
				} else if(ch==0x26){
					lastState=state;
					state=TokenizerState.CharacterRefInAttributeValue;
				} else if(ch==0){
					error=true;
					currentAttribute.appendToValue(0xfffd);
				} else if(ch<0){
					error=true;
					state=TokenizerState.Data;
				} else {
					currentAttribute.appendToValue(ch);
					// Keep reading characters to
					// reduce the need to re-call
					// this method
					int mark=charInput.setSoftMark();
					for(int i=0;i<100;i++){
						ch=charInput.read();
						if(ch>0 && ch!=0x26 && ch!=0x27){
							currentAttribute.appendToValue(ch);
						} else if(ch==0x27){
							currentAttribute.commitValue();
							state=TokenizerState.AfterAttributeValueQuoted;
							break;
						} else {
							charInput.setMarkPosition(mark+i);
							break;
						}
					}
				}
				break;
			}
			case AttributeValueUnquoted:{
				int ch=charInput.read();
				if(ch==0x09 || ch==0x0a || ch==0x0c || ch==0x20){
					currentAttribute.commitValue();
					state=TokenizerState.BeforeAttributeName;
				} else if(ch==0x26){
					lastState=state;
					state=TokenizerState.CharacterRefInAttributeValue;
				} else if(ch==0x3e){
					currentAttribute.commitValue();
					state=TokenizerState.Data;
					return emitCurrentTag();

				} else if(ch==0){
					error=true;
					currentAttribute.appendToValue(0xfffd);
				} else if(ch<0){
					error=true;
					state=TokenizerState.Data;
				} else {
					if(ch==0x22||ch==0x27||ch==0x3c||ch==0x3d||ch==0x60){
						error=true;
					}
					currentAttribute.appendToValue(ch);
				}
				break;
			}
			case AfterAttributeValueQuoted:{
				int mark=charInput.setSoftMark();
				int ch=charInput.read();
				if(ch==0x09 || ch==0x0a || ch==0x0c || ch==0x20){
					state=TokenizerState.BeforeAttributeName;
				} else if(ch==0x2f){
					state=TokenizerState.SelfClosingStartTag;
				} else if(ch==0x3e){
					state=TokenizerState.Data;
					return emitCurrentTag();

				} else if(ch<0){
					error=true;
					state=TokenizerState.Data;
				} else {
					error=true;
					state=TokenizerState.BeforeAttributeName;
					charInput.setMarkPosition(mark);
				}
				break;
			}
			case SelfClosingStartTag:{
				int mark=charInput.setSoftMark();
				int ch=charInput.read();
				if(ch==0x3e){
					currentTag.setSelfClosing(true);
					state=TokenizerState.Data;
					return emitCurrentTag();
				} else if(ch<0){
					error=true;
					state=TokenizerState.Data;
				} else {
					error=true;
					state=TokenizerState.BeforeAttributeName;
					charInput.setMarkPosition(mark);
				}
				break;
			}
			case MarkupDeclarationOpen:{
				int mark=charInput.setSoftMark();
				int ch=charInput.read();
				if(ch=='-' && charInput.read()=='-'){
					CommentToken token=new CommentToken();
					lastComment=token;
					state=TokenizerState.CommentStart;
					break;
				} else if(ch=='D' || ch=='d'){
					if(((ch=charInput.read())=='o' || ch=='O') &&
							((ch=charInput.read())=='c' || ch=='C') &&
							((ch=charInput.read())=='t' || ch=='T') &&
							((ch=charInput.read())=='y' || ch=='Y') &&
							((ch=charInput.read())=='p' || ch=='P') &&
							((ch=charInput.read())=='e' || ch=='E')){
						state=TokenizerState.DocType;
						break;
					}
				} else if(ch=='[' && true){
					if(charInput.read()=='C' &&
							charInput.read()=='D' &&
							charInput.read()=='A' &&
							charInput.read()=='T' &&
							charInput.read()=='A' &&
							charInput.read()=='[' &&
							getCurrentNode()!=null &&
							!HTML_NAMESPACE.equals(getCurrentNode().getNamespaceURI())
							){
						state=TokenizerState.CData;
						break;
					}
				}
				error=true;
				charInput.setMarkPosition(mark);
				bogusCommentCharacter=-1;
				state=TokenizerState.BogusComment;
				break;
			}
			case CommentStart:{
				int ch=charInput.read();
				if(ch=='-'){
					state=TokenizerState.CommentStartDash;
				} else if(ch==0){
					error=true;
					lastComment.appendChar(0xFFFD);
					state=TokenizerState.Comment;
				} else if(ch==0x3e || ch<0){
					error=true;
					state=TokenizerState.Data;
					int ret=tokens.size()|lastComment.getType();
					tokens.add(lastComment);
					return ret;
				} else {
					lastComment.appendChar(ch);
					state=TokenizerState.Comment;
				}
				break;
			}
			case CommentStartDash:{
				int ch=charInput.read();
				if(ch=='-'){
					state=TokenizerState.CommentEnd;
				} else if(ch==0){
					error=true;
					lastComment.appendChar('-');
					lastComment.appendChar(0xFFFD);
					state=TokenizerState.Comment;
				} else if(ch==0x3e || ch<0){
					error=true;
					state=TokenizerState.Data;
					int ret=tokens.size()|lastComment.getType();
					tokens.add(lastComment);
					return ret;
				} else {
					lastComment.appendChar('-');
					lastComment.appendChar(ch);
					state=TokenizerState.Comment;
				}
				break;
			}
			case Comment:{
				int ch=charInput.read();
				if(ch=='-'){
					state=TokenizerState.CommentEndDash;
				} else if(ch==0){
					error=true;
					lastComment.appendChar(0xFFFD);
				} else if(ch<0){
					error=true;
					state=TokenizerState.Data;
					int ret=tokens.size()|lastComment.getType();
					tokens.add(lastComment);
					return ret;
				} else {
					lastComment.appendChar(ch);
				}
				break;
			}
			case CommentEndDash:{
				int ch=charInput.read();
				if(ch=='-'){
					state=TokenizerState.CommentEnd;
				} else if(ch==0){
					error=true;
					lastComment.appendChar('-');
					lastComment.appendChar(0xFFFD);
					state=TokenizerState.Comment;
				} else if(ch<0){
					error=true;
					state=TokenizerState.Data;
					int ret=tokens.size()|lastComment.getType();
					tokens.add(lastComment);
					return ret;
				} else {
					lastComment.appendChar('-');
					lastComment.appendChar(ch);
					state=TokenizerState.Comment;
				}
				break;
			}
			case CommentEnd:{
				int ch=charInput.read();
				if(ch==0x3e){
					state=TokenizerState.Data;
					int ret=tokens.size()|lastComment.getType();
					tokens.add(lastComment);
					return ret;
				} else if(ch==0){
					error=true;
					lastComment.appendChar('-');
					lastComment.appendChar('-');
					lastComment.appendChar(0xFFFD);
					state=TokenizerState.Comment;
				} else if(ch==0x21){ // --!>
					error=true;
					state=TokenizerState.CommentEndBang;
				} else if(ch==0x2D){
					error=true;
					lastComment.appendChar('-');
				} else if(ch<0){
					error=true;
					state=TokenizerState.Data;
					int ret=tokens.size()|lastComment.getType();
					tokens.add(lastComment);
					return ret;
				} else {
					error=true;
					lastComment.appendChar('-');
					lastComment.appendChar('-');
					lastComment.appendChar(ch);
					state=TokenizerState.Comment;
				}
				break;
			}
			case CommentEndBang:{
				int ch=charInput.read();
				if(ch==0x3e){
					state=TokenizerState.Data;
					int ret=tokens.size()|lastComment.getType();
					tokens.add(lastComment);
					return ret;
				} else if(ch==0){
					error=true;
					lastComment.appendChar('-');
					lastComment.appendChar('-');
					lastComment.appendChar('!');
					lastComment.appendChar(0xFFFD);
					state=TokenizerState.Comment;
				} else if(ch==0x2D){
					lastComment.appendChar('-');
					lastComment.appendChar('-');
					lastComment.appendChar('!');
					state=TokenizerState.CommentEndDash;
				} else if(ch<0){
					error=true;
					state=TokenizerState.Data;
					int ret=tokens.size()|lastComment.getType();
					tokens.add(lastComment);
					return ret;
				} else {
					error=true;
					lastComment.appendChar('-');
					lastComment.appendChar('-');
					lastComment.appendChar('!');
					lastComment.appendChar(ch);
					state=TokenizerState.Comment;
				}
				break;
			}
			case CharacterRefInAttributeValue:{
				int allowed=0x3E;
				if(lastState==TokenizerState.AttributeValueDoubleQuoted) {
					allowed='"';
				}
				if(lastState==TokenizerState.AttributeValueSingleQuoted) {
					allowed='\'';
				}
				int ch=parseCharacterReference(allowed);
				if(ch<0){
					// more than one character in this reference
					int index=Math.abs(ch+1);
					currentAttribute.appendToValue(HtmlEntities.entityDoubles[index*2]);
					currentAttribute.appendToValue(HtmlEntities.entityDoubles[index*2+1]);
				} else {
					currentAttribute.appendToValue(ch);
				}
				state=lastState;
				break;
			}
			case TagName:{
				int ch=charInput.read();
				if(ch==0x09 || ch==0x0a || ch==0x0c || ch==0x20){
					state=TokenizerState.BeforeAttributeName;
				} else if(ch==0x2f){
					state=TokenizerState.SelfClosingStartTag;
				} else if(ch==0x3e){
					state=TokenizerState.Data;
					return emitCurrentTag();

				} else if(ch>='A' && ch<='Z'){
					currentTag.appendChar((char)(ch+0x20));
				} else if(ch==0){
					error=true;
					currentTag.appendChar((char)0xFFFD);
				} else if(ch<0){
					error=true;
					state=TokenizerState.Data;
				} else {
					currentTag.append(ch);
				}
				break;
			}
			case RawTextLessThan:{
				charInput.setHardMark();
				int ch=charInput.read();
				if(ch==0x2f){
					tempBuffer.clearAll();
					state=TokenizerState.RawTextEndTagOpen;
				} else {
					state=TokenizerState.RawText;
					if(ch>=0) {
						charInput.moveBack(1);
					}
					return 0x3c;
				}
				break;
			}
			case BogusComment:{
				CommentToken comment=new CommentToken();
				if(bogusCommentCharacter>=0) {
					comment.appendChar(bogusCommentCharacter==0 ? 0xFFFD : bogusCommentCharacter);
				}
				while(true){
					int ch=charInput.read();
					if(ch<0 || ch=='>') {
						break;
					}
					if(ch==0) {
						ch=0xFFFD;
					}
					comment.appendChar(ch);
				}
				int ret=tokens.size()|comment.getType();
				tokens.add(comment);
				state=TokenizerState.Data;
				return ret;
			}
			case DocType:{
				charInput.setHardMark();
				int ch=charInput.read();
				if(ch==0x09||ch==0x0a||ch==0x0c||ch==0x20){
					state=TokenizerState.BeforeDocTypeName;
				} else if(ch<0){
					error=true;
					state=TokenizerState.Data;
					DocTypeToken token=new DocTypeToken();
					token.forceQuirks=true;
					int ret=tokens.size()|token.getType();
					tokens.add(token);
					return ret;
				} else {
					error=true;
					charInput.moveBack(1);
					state=TokenizerState.BeforeDocTypeName;
				}
				break;
			}
			case BeforeDocTypeName:{
				int ch=charInput.read();
				if(ch==0x09||ch==0x0a||ch==0x0c||ch==0x20){
					break;
				} else if(ch>='A' && ch<='Z'){
					docTypeToken=new DocTypeToken();
					docTypeToken.name=new IntList();
					docTypeToken.name.appendInt(ch+0x20);
					state=TokenizerState.DocTypeName;
				} else if(ch==0){
					error=true;
					docTypeToken=new DocTypeToken();
					docTypeToken.name=new IntList();
					docTypeToken.name.appendInt(0xFFFD);
					state=TokenizerState.DocTypeName;
				} else if(ch==0x3e || ch<0){
					error=true;
					state=TokenizerState.Data;
					DocTypeToken token=new DocTypeToken();
					token.forceQuirks=true;
					int ret=tokens.size()|token.getType();
					tokens.add(token);
					return ret;
				} else {
					docTypeToken=new DocTypeToken();
					docTypeToken.name=new IntList();
					docTypeToken.name.appendInt(ch);
					state=TokenizerState.DocTypeName;
				}
				break;
			}
			case DocTypeName:{
				int ch=charInput.read();
				if(ch==0x09||ch==0x0a||ch==0x0c||ch==0x20){
					state=TokenizerState.AfterDocTypeName;
				} else if(ch==0x3e){
					state=TokenizerState.Data;
					int ret=tokens.size()|docTypeToken.getType();
					tokens.add(docTypeToken);
					return ret;
				} else if(ch>='A' && ch<='Z'){
					docTypeToken.name.appendInt(ch+0x20);
				} else if(ch==0){
					error=true;
					docTypeToken.name.appendInt(0xfffd);
				} else if(ch<0){
					error=true;
					docTypeToken.forceQuirks=true;
					state=TokenizerState.Data;
					int ret=tokens.size()|docTypeToken.getType();
					tokens.add(docTypeToken);
					return ret;
				} else {
					docTypeToken.name.appendInt(ch);
				}
				break;
			}
			case AfterDocTypeName:{
				int ch=charInput.read();
				if(ch==0x09||ch==0x0a||ch==0x0c||ch==0x20){
					break;
				} else if(ch==0x3e){
					state=TokenizerState.Data;
					int ret=tokens.size()|docTypeToken.getType();
					tokens.add(docTypeToken);
					return ret;
				} else if(ch<0){
					error=true;
					docTypeToken.forceQuirks=true;
					state=TokenizerState.Data;
					int ret=tokens.size()|docTypeToken.getType();
					tokens.add(docTypeToken);
					return ret;
				} else {
					int ch2=0;
					int pos=charInput.setSoftMark();
					if(ch=='P' || ch=='p'){
						if(((ch2=charInput.read())=='u' || ch2=='U') &&
								((ch2=charInput.read())=='b' || ch2=='B') &&
								((ch2=charInput.read())=='l' || ch2=='L') &&
								((ch2=charInput.read())=='i' || ch2=='I') &&
								((ch2=charInput.read())=='c' || ch2=='C')
								){
							state=TokenizerState.AfterDocTypePublic;
						} else {
							error=true;
							charInput.setMarkPosition(pos);
							docTypeToken.forceQuirks=true;
							state=TokenizerState.BogusDocType;
						}
					} else if(ch=='S' || ch=='s'){
						if(((ch2=charInput.read())=='y' || ch2=='Y') &&
								((ch2=charInput.read())=='s' || ch2=='S') &&
								((ch2=charInput.read())=='t' || ch2=='T') &&
								((ch2=charInput.read())=='e' || ch2=='E') &&
								((ch2=charInput.read())=='m' || ch2=='M')
								){
							state=TokenizerState.AfterDocTypeSystem;
						} else {
							error=true;
							charInput.setMarkPosition(pos);
							docTypeToken.forceQuirks=true;
							state=TokenizerState.BogusDocType;
						}
					} else {
						error=true;
						charInput.setMarkPosition(pos);
						docTypeToken.forceQuirks=true;
						state=TokenizerState.BogusDocType;
					}
				}
				break;
			}
			case AfterDocTypePublic:
			case BeforeDocTypePublicID:{
				int ch=charInput.read();
				if(ch==0x09||ch==0x0a||ch==0x0c||ch==0x20){
					if(state==TokenizerState.AfterDocTypePublic) {
						state=TokenizerState.BeforeDocTypePublicID;
					}
				} else if(ch==0x22){
					docTypeToken.publicID=new IntList();
					if(state==TokenizerState.AfterDocTypePublic) {
						error=true;
					}
					state=TokenizerState.DocTypePublicIDDoubleQuoted;
				} else if(ch==0x27){
					docTypeToken.publicID=new IntList();
					if(state==TokenizerState.AfterDocTypePublic) {
						error=true;
					}
					state=TokenizerState.DocTypePublicIDSingleQuoted;
				} else if(ch==0x3e || ch<0){
					error=true;
					docTypeToken.forceQuirks=true;
					state=TokenizerState.Data;
					int ret=tokens.size()|docTypeToken.getType();
					tokens.add(docTypeToken);
					return ret;
				} else {
					error=true;
					docTypeToken.forceQuirks=true;
					state=TokenizerState.BogusDocType;
				}
				break;
			}
			case AfterDocTypeSystem:
			case BeforeDocTypeSystemID:{
				int ch=charInput.read();
				if(ch==0x09||ch==0x0a||ch==0x0c||ch==0x20){
					if(state==TokenizerState.AfterDocTypeSystem) {
						state=TokenizerState.BeforeDocTypeSystemID;
					}
				} else if(ch==0x22){
					docTypeToken.systemID=new IntList();
					if(state==TokenizerState.AfterDocTypeSystem) {
						error=true;
					}
					state=TokenizerState.DocTypeSystemIDDoubleQuoted;
				} else if(ch==0x27){
					docTypeToken.systemID=new IntList();
					if(state==TokenizerState.AfterDocTypeSystem) {
						error=true;
					}
					state=TokenizerState.DocTypeSystemIDSingleQuoted;
				} else if(ch==0x3e || ch<0){
					error=true;
					docTypeToken.forceQuirks=true;
					state=TokenizerState.Data;
					int ret=tokens.size()|docTypeToken.getType();
					tokens.add(docTypeToken);
					return ret;
				} else {
					error=true;
					docTypeToken.forceQuirks=true;
					state=TokenizerState.BogusDocType;
				}
				break;
			}
			case DocTypePublicIDDoubleQuoted:
			case DocTypePublicIDSingleQuoted:{
				int ch=charInput.read();
				if(ch==(state==TokenizerState.DocTypePublicIDDoubleQuoted ? 0x22 : 0x27)){
					state=TokenizerState.AfterDocTypePublicID;
				} else if(ch==0){
					error=true;
					docTypeToken.publicID.appendInt(0xFFFD);
				} else if(ch==0x3e || ch<0){
					error=true;
					docTypeToken.forceQuirks=true;
					state=TokenizerState.Data;
					int ret=tokens.size()|docTypeToken.getType();
					tokens.add(docTypeToken);
					return ret;
				} else {
					docTypeToken.publicID.appendInt(ch);
				}
				break;
			}
			case DocTypeSystemIDDoubleQuoted:
			case DocTypeSystemIDSingleQuoted:{
				int ch=charInput.read();
				if(ch==(state==TokenizerState.DocTypeSystemIDDoubleQuoted ? 0x22 : 0x27)){
					state=TokenizerState.AfterDocTypeSystemID;
				} else if(ch==0){
					error=true;
					docTypeToken.systemID.appendInt(0xFFFD);
				} else if(ch==0x3e || ch<0){
					error=true;
					docTypeToken.forceQuirks=true;
					state=TokenizerState.Data;
					int ret=tokens.size()|docTypeToken.getType();
					tokens.add(docTypeToken);
					return ret;
				} else {
					docTypeToken.systemID.appendInt(ch);
				}
				break;
			}
			case AfterDocTypePublicID:
			case BetweenDocTypePublicAndSystem:{
				int ch=charInput.read();
				if(ch==0x09||ch==0x0a||ch==0x0c||ch==0x20){
					if(state==TokenizerState.AfterDocTypePublicID) {
						state=TokenizerState.BetweenDocTypePublicAndSystem;
					}
				} else if(ch==0x3e){
					state=TokenizerState.Data;
					int ret=tokens.size()|docTypeToken.getType();
					tokens.add(docTypeToken);
					return ret;
				} else if(ch==0x22){
					docTypeToken.systemID=new IntList();
					if(state==TokenizerState.AfterDocTypePublicID) {
						error=true;
					}
					state=TokenizerState.DocTypeSystemIDDoubleQuoted;
				} else if(ch==0x27){
					docTypeToken.systemID=new IntList();
					if(state==TokenizerState.AfterDocTypePublicID) {
						error=true;
					}
					state=TokenizerState.DocTypeSystemIDSingleQuoted;
				} else if(ch<0){
					error=true;
					docTypeToken.forceQuirks=true;
					state=TokenizerState.Data;
					int ret=tokens.size()|docTypeToken.getType();
					tokens.add(docTypeToken);
					return ret;
				} else {
					error=true;
					docTypeToken.forceQuirks=true;
					state=TokenizerState.BogusDocType;
				}
				break;
			}
			case AfterDocTypeSystemID:{
				int ch=charInput.read();
				if(ch==0x09||ch==0x0a||ch==0x0c||ch==0x20){
					break;
				} else if(ch==0x3e){
					state=TokenizerState.Data;
					int ret=tokens.size()|docTypeToken.getType();
					tokens.add(docTypeToken);
					return ret;
				} else if(ch<0){
					error=true;
					docTypeToken.forceQuirks=true;
					state=TokenizerState.Data;
					int ret=tokens.size()|docTypeToken.getType();
					tokens.add(docTypeToken);
					return ret;
				} else {
					error=true;
					state=TokenizerState.BogusDocType;
				}
				break;
			}
			case BogusDocType:{
				int ch=charInput.read();
				if(ch==0x3e || ch<0){
					state=TokenizerState.Data;
					int ret=tokens.size()|docTypeToken.getType();
					tokens.add(docTypeToken);
					return ret;
				}
				break;
			}
			case CData:{
				IntList buffer=new IntList();
				int phase=0;
				state=TokenizerState.Data;
				while(true){
					int ch=charInput.read();
					if(ch<0) {
						break;
					}
					buffer.appendInt(ch);
					if(phase==0){
						if(ch==']') {
							phase++;
						} else {
							phase=0;
						}
					} else if(phase==1) {
						if(ch==']'){
							phase++;
						} else {
							phase=0;
						}
					} else if(phase==2) {
						if(ch=='>'){
							phase++;
							break;
						} else if(ch==']'){
							phase=2;
						} else {
							phase=0;
						}
					}
				}
				int[] arr=buffer.array();
				int size=buffer.size();
				if(phase==3)
				{
					size-=3; // don't count the ']]>'
				}
				if(size>0){
					// Emit the tokens
					int ret1=arr[0];
					for(int i=1;i<size;i++){
						tokenQueue.add(arr[i]);
					}
					return ret1;
				}
				break;
			}
			case RcDataLessThan:{
				charInput.setHardMark();
				int ch=charInput.read();
				if(ch==0x2f){
					tempBuffer.clearAll();
					state=TokenizerState.RcDataEndTagOpen;
				} else {
					state=TokenizerState.RcData;
					if(ch>=0) {
						charInput.moveBack(1);
					}
					return 0x3c;
				}
				break;
			}
			default:
				throw new AssertionError();
			}
		}
	}

	private Element popCurrentNode(){
		if(openElements.size()>0)
			return removeAtIndex(openElements,openElements.size()-1);
		return null;
	}

	private void pushFormattingElement(StartTagToken tag) {
		Element element=addHtmlElement(tag);
		int matchingElements=0;
		int lastMatchingElement=-1;
		String name=element.getLocalName();
		for(int i=formattingElements.size()-1;i>=0;i--){
			FormattingElement fe=formattingElements.get(i);
			if(fe.isMarker()) {
				break;
			}
			if(fe.element.getLocalName().equals(name) &&
					fe.element.getNamespaceURI().equals(element.getNamespaceURI())){
				List<IAttr> attribs=fe.element.getAttributes();
				List<IAttr> myAttribs=element.getAttributes();
				if(attribs.size()==myAttribs.size()){
					boolean match=true;
					for(int j=0;j<myAttribs.size();j++){
						String name1=myAttribs.get(j).getName();
						String namespace=myAttribs.get(j).getNamespaceURI();
						String value=myAttribs.get(j).getValue();
						String otherValue=fe.element.getAttributeNS(namespace,name1);
						if(otherValue==null || !otherValue.equals(value)){
							match=false;
						}
					}
					if(match){
						matchingElements++;
						lastMatchingElement=i;
					}
				}
			}
		}
		if(matchingElements>=3){
			formattingElements.remove(lastMatchingElement);
		}
		FormattingElement fe2=new FormattingElement();
		fe2.marker=false;
		fe2.token=tag;
		fe2.element=element;
		formattingElements.add(fe2);
	}
	private void reconstructFormatting(){
		if(formattingElements.size()==0)return;
		//DebugUtility.log("reconstructing elements");
		//DebugUtility.log(formattingElements);
		FormattingElement fe=formattingElements.get(formattingElements.size()-1);
		if(fe.isMarker() || openElements.contains(fe.element))
			return;
		int i=formattingElements.size()-1;
		while(i>0){
			fe=formattingElements.get(i-1);
			i--;
			if(!fe.isMarker() && !openElements.contains(fe.element)){
				continue;
			}
			i++;
			break;
		}
		for(int j=i;j<formattingElements.size();j++){
			fe=formattingElements.get(j);
			Element element=addHtmlElement(fe.token);
			fe.element=element;
			fe.marker=false;
		}
	}
	private void removeFormattingElement(Element aElement) {
		FormattingElement f=null;
		for(FormattingElement fe : formattingElements){
			if(!fe.isMarker() && aElement.equals(fe.element)){
				f=fe;
				break;
			}
		}
		if(f!=null) {
			formattingElements.remove(f);
		}
	}
	private void resetInsertionMode(){
		boolean last=false;
		for(int i=openElements.size()-1;i>=0;i--){
			Element e=openElements.get(i);
			if(context!=null && i==0){
				e=context;
				last=true;
			}
			String name=e.getLocalName();
			if(!last && (name.equals("th") || name.equals("td"))){
				insertionMode=InsertionMode.InCell;
				break;
			}
			if((name.equals("select"))){
				insertionMode=InsertionMode.InSelect;
				break;
			}
			if((name.equals("colgroup"))){
				insertionMode=InsertionMode.InColumnGroup;
				break;
			}
			if((name.equals("tr"))){
				insertionMode=InsertionMode.InRow;
				break;
			}
			if((name.equals("caption"))){
				insertionMode=InsertionMode.InCaption;
				break;
			}
			if((name.equals("table"))){
				insertionMode=InsertionMode.InTable;
				break;
			}
			if((name.equals("frameset"))){
				insertionMode=InsertionMode.InFrameset;
				break;
			}
			if((name.equals("html"))){
				insertionMode=InsertionMode.BeforeHead;
				break;
			}
			if((name.equals("head") || name.equals("body"))){
				insertionMode=InsertionMode.InBody;
				break;
			}
			if((name.equals("thead")||name.equals("tbody")||name.equals("tfoot"))){
				insertionMode=InsertionMode.InTableBody;
				break;
			}
			if(last){
				insertionMode=InsertionMode.InBody;
				break;
			}
		}
	}

	 void setCData(){
		state=TokenizerState.CData;
	}

	 void setPlainText(){
		state=TokenizerState.PlainText;
	}

	 void setRawText(){
		state=TokenizerState.RawText;
	}

	////////////////////////////////////////////////////

	 void setRcData(){
		state=TokenizerState.RcData;
	}

	private void skipLineFeed() throws IOException {
		int mark=charInput.setSoftMark();
		int nextToken=charInput.read();
		if(nextToken==0x0a)
			return; // ignore the token if it's 0x0A
		else if(nextToken==0x26){ // start of character reference
			int charref=parseCharacterReference(-1);
			if(charref<0){
				// more than one character in this reference
				int index=Math.abs(charref+1);
				tokenQueue.add(HtmlEntities.entityDoubles[index*2]);
				tokenQueue.add(HtmlEntities.entityDoubles[index*2+1]);
			} else if(charref==0x0a)
				return; // ignore the token
			else {
				tokenQueue.add(charref);
			}
		} else {
			// anything else; reset the input stream
			charInput.setMarkPosition(mark);
		}
	}

	private void stopParsing() {
		done=true;
		if(StringUtility.isNullOrEmpty(document.defaultLanguage)){
			if(contentLanguage.length==1){
				// set the fallback language if there is
				// only one language defined and no meta element
				// defines the language
				document.defaultLanguage=contentLanguage[0];
			}
		}
		document.encoding=encoding.getEncoding();
		String docbase=document.getBaseURI();
		if(docbase==null || docbase.length()==0){
			docbase=baseurl;
		} else {
			if(baseurl!=null && baseurl.length()>0){
				document.setBaseURI(HtmlDocument.resolveURL(document,baseurl,docbase));
			}
		}
		openElements.clear();
		formattingElements.clear();
	}


}
