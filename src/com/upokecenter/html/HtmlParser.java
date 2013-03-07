package com.upokecenter.html;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.upokecenter.encoding.TextEncoding;
import com.upokecenter.util.ConditionalBufferInputStream;
import com.upokecenter.util.IMarkableCharacterInput;
import com.upokecenter.util.IntList;
import com.upokecenter.util.StackableInputStream;
import com.upokecenter.util.StringUtility;

final class HtmlParser {

	static class Attribute {
		StringBuilder name;
		IntList value;
		String prefix=null;
		String localName=null;
		String nameString=null;
		String valueString=null;
		String namespace=null;

		@Override
		public String toString(){
			return "[Attribute: "+getName()+"="+getValue()+"]";
		}

		public Attribute(char ch){
			name=new StringBuilder();
			value=new IntList();
			name.append(ch);
		}

		public Attribute(int ch){
			name=new StringBuilder();
			value=new IntList();
			if(ch<0x10000){
				name.append((char)ch);
			} else {
				ch-=0x10000;
				int lead=ch/0x400+0xd800;
				int trail=ch%0x400+0xdc00;
				name.append((char)lead);
				name.append((char)trail);
			}
		}
		public Attribute(Attribute attr){
			nameString=attr.getName();
			valueString=attr.getValue();
			prefix=attr.prefix;
			localName=attr.localName;
			namespace=attr.namespace;
		}

		public Attribute(String name, String value){
			nameString=name;
			valueString=value;
		}

		public void appendToName(int ch){
			if(nameString!=null)
				throw new IllegalStateException();
			if(ch<0x10000){
				name.append((char)ch);
			} else {
				ch-=0x10000;
				int lead=ch/0x400+0xd800;
				int trail=ch%0x400+0xdc00;
				name.append((char)lead);
				name.append((char)trail);
			}
		}

		public void appendToValue(int ch){
			if(valueString!=null)
				throw new IllegalStateException();
			value.append(ch);
		}

		void commitValue(){
			if(value==null)
				throw new IllegalStateException();
			valueString=value.toString();
			value=null;
		}

		public String getName(){
			return (nameString!=null) ? nameString : name.toString();
		}

		public String getValue() {
			return (valueString!=null) ? valueString : value.toString();
		}
		public String getNamespace(){
			return namespace;
		}

		public String getLocalName(){
			return (namespace==null) ? getName() : localName;
		}

		public boolean isAttribute(String name, String namespace){
			String thisname=this.getLocalName();
			boolean match=(name==null ? thisname==null : name.equals(thisname));
			if(!match)return false;
			match=(namespace==null ? this.namespace==null : namespace.equals(this.namespace));
			return match;
		}

		public void setNamespace(String value){
			if(value==null)
				throw new IllegalArgumentException();
			namespace=value;
			nameString=this.getName();
			int io=nameString.indexOf(':');
			if(io>=1){
				prefix=nameString.substring(0,io);
				localName=nameString.substring(io+1);
			} else {
				prefix="";
				localName=this.getName();
			}
		}

		public void setName(String value2) {
			if(value2==null)
				throw new IllegalArgumentException();
			nameString=value2;
			name=null;
		}

		public void setValue(String value2) {
			if(value2==null)
				throw new IllegalArgumentException();
			valueString=value2;
			value=null;
		}

	}

	static class CommentToken implements IToken {
		IntList value;
		public CommentToken(){
			value=new IntList();
		}

		public void append(int ch){
			value.append(ch);
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
		IntList name;
		IntList publicID;
		IntList systemID;
		boolean forceQuirks;
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
		public int getType() {
			return TOKEN_END_TAG;
		}
	}
	private static class FormattingElement {
		public boolean marker;
		public Element element;
		public StartTagToken token;
		@Override
		public String toString() {
			return "FormattingElement [marker=" + marker + ", token=" + token + "]\n";
		}
		public boolean isMarker() {
			return marker;
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
		public int getType() {
			return TOKEN_START_TAG;
		}
		public void setName(String string) {
			builder.delete(0,builder.length());
			builder.append(string);
		}
	}
	static abstract class TagToken implements IToken {

		protected StringBuilder builder;
		List<Attribute> attributes=null;
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

		public boolean isAckSelfClosing() {
			return !selfClosing || selfClosingAck;
		}

		public Attribute addAttribute(char ch){
			if(attributes==null){
				attributes=new ArrayList<Attribute>();
			}
			Attribute a=new Attribute(ch);
			attributes.add(a);
			return a;
		}

		public Attribute addAttribute(int ch){
			if(attributes==null){
				attributes=new ArrayList<Attribute>();
			}
			Attribute a=new Attribute(ch);
			attributes.add(a);
			return a;
		}

		public void append(int ch) {
			if(ch<0x10000){
				builder.append((char)ch);
			} else {
				ch-=0x10000;
				int lead=ch/0x400+0xd800;
				int trail=ch%0x400+0xdc00;
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
				Attribute a=attributes.get(i);
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
				Attribute a=attributes.get(i);
				if(a.isAttribute(name,namespace))
					return a.getValue();
			}
			return null;
		}

		public List<Attribute> getAttributes(){
			if(attributes==null)
				return Arrays.asList(new Attribute[0]);
			else
				return attributes;
		}

		public String getName(){
			return builder.toString();
		}
		@Override
		public abstract int getType();
		public boolean isSelfClosing() {
			return selfClosing;
		}

		public boolean isSelfClosingAck(){
			return selfClosingAck;
		}


		public void setAttribute(String attrname, String value) {
			if(attributes==null){
				attributes=new ArrayList<Attribute>();
				attributes.add(new Attribute(attrname,value));
			} else {
				int size=attributes.size();
				for(int i=0;i<size;i++){
					Attribute a=attributes.get(i);
					String thisname=a.getName();
					if(thisname.equals(attrname)){
						a.setValue(value);
						return;
					}
				}
				attributes.add(new Attribute(attrname,value));
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
	private static String[] entities=new String[]{"CounterClockwiseContourIntegral;", "ClockwiseContourIntegral;", "DoubleLongLeftRightArrow;", "NotNestedGreaterGreater;", "DiacriticalDoubleAcute;", "NotSquareSupersetEqual;", "CloseCurlyDoubleQuote;", "DoubleContourIntegral;", "FilledVerySmallSquare;", "NegativeVeryThinSpace;", "NotPrecedesSlantEqual;", "NotRightTriangleEqual;", "NotSucceedsSlantEqual;", "CapitalDifferentialD;", "DoubleLeftRightArrow;", "DoubleLongRightArrow;", "EmptyVerySmallSquare;", "NestedGreaterGreater;", "NotDoubleVerticalBar;", "NotGreaterSlantEqual;", "NotLeftTriangleEqual;", "NotSquareSubsetEqual;", "OpenCurlyDoubleQuote;", "ReverseUpEquilibrium;", "DoubleLongLeftArrow;", "DownLeftRightVector;", "LeftArrowRightArrow;", "NegativeMediumSpace;", "NotGreaterFullEqual;", "NotRightTriangleBar;", "RightArrowLeftArrow;", "SquareSupersetEqual;", "leftrightsquigarrow;", "DownRightTeeVector;", "DownRightVectorBar;", "LongLeftRightArrow;", "Longleftrightarrow;", "NegativeThickSpace;", "NotLeftTriangleBar;", "PrecedesSlantEqual;", "ReverseEquilibrium;", "RightDoubleBracket;", "RightDownTeeVector;", "RightDownVectorBar;", "RightTriangleEqual;", "SquareIntersection;", "SucceedsSlantEqual;", "blacktriangleright;", "longleftrightarrow;", "DoubleUpDownArrow;", "DoubleVerticalBar;", "DownLeftTeeVector;", "DownLeftVectorBar;", "FilledSmallSquare;", "GreaterSlantEqual;", "LeftDoubleBracket;", "LeftDownTeeVector;", "LeftDownVectorBar;", "LeftTriangleEqual;", "NegativeThinSpace;", "NotGreaterGreater;", "NotLessSlantEqual;", "NotNestedLessLess;", "NotReverseElement;", "NotSquareSuperset;", "NotTildeFullEqual;", "RightAngleBracket;", "RightUpDownVector;", "SquareSubsetEqual;", "VerticalSeparator;", "blacktriangledown;", "blacktriangleleft;", "leftrightharpoons;", "rightleftharpoons;", "twoheadrightarrow;", "DiacriticalAcute;", "DiacriticalGrave;", "DiacriticalTilde;", "DoubleRightArrow;", "DownArrowUpArrow;", "EmptySmallSquare;", "GreaterEqualLess;", "GreaterFullEqual;", "LeftAngleBracket;", "LeftUpDownVector;", "LessEqualGreater;", "NonBreakingSpace;", "NotPrecedesEqual;", "NotRightTriangle;", "NotSucceedsEqual;", "NotSucceedsTilde;", "NotSupersetEqual;", "RightTriangleBar;", "RightUpTeeVector;", "RightUpVectorBar;", "UnderParenthesis;", "UpArrowDownArrow;", "circlearrowright;", "downharpoonright;", "ntrianglerighteq;", "rightharpoondown;", "rightrightarrows;", "twoheadleftarrow;", "vartriangleright;", "CloseCurlyQuote;", "ContourIntegral;", "DoubleDownArrow;", "DoubleLeftArrow;", "DownRightVector;", "LeftRightVector;", "LeftTriangleBar;", "LeftUpTeeVector;", "LeftUpVectorBar;", "LowerRightArrow;", "NotGreaterEqual;", "NotGreaterTilde;", "NotHumpDownHump;", "NotLeftTriangle;", "NotSquareSubset;", "OverParenthesis;", "RightDownVector;", "ShortRightArrow;", "UpperRightArrow;", "bigtriangledown;", "circlearrowleft;", "curvearrowright;", "downharpoonleft;", "leftharpoondown;", "leftrightarrows;", "nLeftrightarrow;", "nleftrightarrow;", "ntrianglelefteq;", "rightleftarrows;", "rightsquigarrow;", "rightthreetimes;", "straightepsilon;", "trianglerighteq;", "vartriangleleft;", "DiacriticalDot;", "DoubleRightTee;", "DownLeftVector;", "GreaterGreater;", "HorizontalLine;", "InvisibleComma;", "InvisibleTimes;", "LeftDownVector;", "LeftRightArrow;", "Leftrightarrow;", "LessSlantEqual;", "LongRightArrow;", "Longrightarrow;", "LowerLeftArrow;", "NestedLessLess;", "NotGreaterLess;", "NotLessGreater;", "NotSubsetEqual;", "NotVerticalBar;", "OpenCurlyQuote;", "ReverseElement;", "RightTeeVector;", "RightVectorBar;", "ShortDownArrow;", "ShortLeftArrow;", "SquareSuperset;", "TildeFullEqual;", "UpperLeftArrow;", "ZeroWidthSpace;", "curvearrowleft;", "doublebarwedge;", "downdownarrows;", "hookrightarrow;", "leftleftarrows;", "leftrightarrow;", "leftthreetimes;", "longrightarrow;", "looparrowright;", "nshortparallel;", "ntriangleright;", "rightarrowtail;", "rightharpoonup;", "trianglelefteq;", "upharpoonright;", "ApplyFunction;", "DifferentialD;", "DoubleLeftTee;", "DoubleUpArrow;", "LeftTeeVector;", "LeftVectorBar;", "LessFullEqual;", "LongLeftArrow;", "Longleftarrow;", "NotEqualTilde;", "NotTildeEqual;", "NotTildeTilde;", "Poincareplane;", "PrecedesEqual;", "PrecedesTilde;", "RightArrowBar;", "RightTeeArrow;", "RightTriangle;", "RightUpVector;", "SucceedsEqual;", "SucceedsTilde;", "SupersetEqual;", "UpEquilibrium;", "VerticalTilde;", "VeryThinSpace;", "bigtriangleup;", "blacktriangle;", "divideontimes;", "fallingdotseq;", "hookleftarrow;", "leftarrowtail;", "leftharpoonup;", "longleftarrow;", "looparrowleft;", "measuredangle;", "ntriangleleft;", "shortparallel;", "smallsetminus;", "triangleright;", "upharpoonleft;", "varsubsetneqq;", "varsupsetneqq;", "DownArrowBar;", "DownTeeArrow;", "ExponentialE;", "GreaterEqual;", "GreaterTilde;", "HilbertSpace;", "HumpDownHump;", "Intersection;", "LeftArrowBar;", "LeftTeeArrow;", "LeftTriangle;", "LeftUpVector;", "NotCongruent;", "NotHumpEqual;", "NotLessEqual;", "NotLessTilde;", "Proportional;", "RightCeiling;", "RoundImplies;", "ShortUpArrow;", "SquareSubset;", "UnderBracket;", "VerticalLine;", "blacklozenge;", "exponentiale;", "risingdotseq;", "triangledown;", "triangleleft;", "varsubsetneq;", "varsupsetneq;", "CircleMinus;", "CircleTimes;", "Equilibrium;", "GreaterLess;", "LeftCeiling;", "LessGreater;", "MediumSpace;", "NotLessLess;", "NotPrecedes;", "NotSucceeds;", "NotSuperset;", "OverBracket;", "RightVector;", "Rrightarrow;", "RuleDelayed;", "SmallCircle;", "SquareUnion;", "SubsetEqual;", "UpDownArrow;", "Updownarrow;", "VerticalBar;", "backepsilon;", "blacksquare;", "circledcirc;", "circleddash;", "curlyeqprec;", "curlyeqsucc;", "diamondsuit;", "eqslantless;", "expectation;", "nRightarrow;", "nrightarrow;", "preccurlyeq;", "precnapprox;", "quaternions;", "straightphi;", "succcurlyeq;", "succnapprox;", "thickapprox;", "updownarrow;", "Bernoullis;", "CirclePlus;", "EqualTilde;", "Fouriertrf;", "ImaginaryI;", "Laplacetrf;", "LeftVector;", "Lleftarrow;", "NotElement;", "NotGreater;", "Proportion;", "RightArrow;", "RightFloor;", "Rightarrow;", "ThickSpace;", "TildeEqual;", "TildeTilde;", "UnderBrace;", "UpArrowBar;", "UpTeeArrow;", "circledast;", "complement;", "curlywedge;", "eqslantgtr;", "gtreqqless;", "lessapprox;", "lesseqqgtr;", "lmoustache;", "longmapsto;", "mapstodown;", "mapstoleft;", "nLeftarrow;", "nleftarrow;", "nsubseteqq;", "nsupseteqq;", "precapprox;", "rightarrow;", "rmoustache;", "sqsubseteq;", "sqsupseteq;", "subsetneqq;", "succapprox;", "supsetneqq;", "upuparrows;", "varepsilon;", "varnothing;", "Backslash;", "CenterDot;", "CircleDot;", "Congruent;", "Coproduct;", "DoubleDot;", "DownArrow;", "DownBreve;", "Downarrow;", "HumpEqual;", "LeftArrow;", "LeftFloor;", "Leftarrow;", "LessTilde;", "Mellintrf;", "MinusPlus;", "NotCupCap;", "NotExists;", "NotSubset;", "OverBrace;", "PlusMinus;", "Therefore;", "ThinSpace;", "TripleDot;", "UnionPlus;", "backprime;", "backsimeq;", "bigotimes;", "centerdot;", "checkmark;", "complexes;", "dotsquare;", "downarrow;", "gtrapprox;", "gtreqless;", "gvertneqq;", "heartsuit;", "leftarrow;", "lesseqgtr;", "lvertneqq;", "ngeqslant;", "nleqslant;", "nparallel;", "nshortmid;", "nsubseteq;", "nsupseteq;", "pitchfork;", "rationals;", "spadesuit;", "subseteqq;", "subsetneq;", "supseteqq;", "supsetneq;", "therefore;", "triangleq;", "varpropto;", "DDotrahd;", "DotEqual;", "Integral;", "LessLess;", "NotEqual;", "NotTilde;", "PartialD;", "Precedes;", "RightTee;", "Succeeds;", "SuchThat;", "Superset;", "Uarrocir;", "UnderBar;", "andslope;", "angmsdaa;", "angmsdab;", "angmsdac;", "angmsdad;", "angmsdae;", "angmsdaf;", "angmsdag;", "angmsdah;", "angrtvbd;", "approxeq;", "awconint;", "backcong;", "barwedge;", "bbrktbrk;", "bigoplus;", "bigsqcup;", "biguplus;", "bigwedge;", "boxminus;", "boxtimes;", "bsolhsub;", "capbrcup;", "circledR;", "circledS;", "cirfnint;", "clubsuit;", "cupbrcap;", "curlyvee;", "cwconint;", "doteqdot;", "dotminus;", "drbkarow;", "dzigrarr;", "elinters;", "emptyset;", "eqvparsl;", "fpartint;", "geqslant;", "gesdotol;", "gnapprox;", "hksearow;", "hkswarow;", "imagline;", "imagpart;", "infintie;", "integers;", "intercal;", "intlarhk;", "laemptyv;", "ldrushar;", "leqslant;", "lesdotor;", "llcorner;", "lnapprox;", "lrcorner;", "lurdshar;", "mapstoup;", "multimap;", "naturals;", "ncongdot;", "notindot;", "otimesas;", "parallel;", "plusacir;", "pointint;", "precneqq;", "precnsim;", "profalar;", "profline;", "profsurf;", "raemptyv;", "realpart;", "rppolint;", "rtriltri;", "scpolint;", "setminus;", "shortmid;", "smeparsl;", "sqsubset;", "sqsupset;", "subseteq;", "succneqq;", "succnsim;", "supseteq;", "thetasym;", "thicksim;", "timesbar;", "triangle;", "triminus;", "trpezium;", "ulcorner;", "urcorner;", "varkappa;", "varsigma;", "vartheta;", "Because;", "Cayleys;", "Cconint;", "Cedilla;", "Diamond;", "DownTee;", "Element;", "Epsilon;", "Implies;", "LeftTee;", "NewLine;", "NoBreak;", "NotLess;", "Omicron;", "OverBar;", "Product;", "UpArrow;", "Uparrow;", "Upsilon;", "alefsym;", "angrtvb;", "angzarr;", "asympeq;", "backsim;", "because;", "bemptyv;", "between;", "bigcirc;", "bigodot;", "bigstar;", "bnequiv;", "boxplus;", "ccupssm;", "cemptyv;", "cirscir;", "coloneq;", "congdot;", "cudarrl;", "cudarrr;", "cularrp;", "curarrm;", "dbkarow;", "ddagger;", "ddotseq;", "demptyv;", "diamond;", "digamma;", "dotplus;", "dwangle;", "epsilon;", "eqcolon;", "equivDD;", "gesdoto;", "gtquest;", "gtrless;", "harrcir;", "intprod;", "isindot;", "larrbfs;", "larrsim;", "lbrksld;", "lbrkslu;", "ldrdhar;", "lesdoto;", "lessdot;", "lessgtr;", "lesssim;", "lotimes;", "lozenge;", "ltquest;", "luruhar;", "maltese;", "minusdu;", "napprox;", "natural;", "nearrow;", "nexists;", "notinva;", "notinvb;", "notinvc;", "notniva;", "notnivb;", "notnivc;", "npolint;", "npreceq;", "nsqsube;", "nsqsupe;", "nsubset;", "nsucceq;", "nsupset;", "nvinfin;", "nvltrie;", "nvrtrie;", "nwarrow;", "olcross;", "omicron;", "orderof;", "orslope;", "pertenk;", "planckh;", "pluscir;", "plussim;", "plustwo;", "precsim;", "quatint;", "questeq;", "rarrbfs;", "rarrsim;", "rbrksld;", "rbrkslu;", "rdldhar;", "realine;", "rotimes;", "ruluhar;", "searrow;", "simplus;", "simrarr;", "subedot;", "submult;", "subplus;", "subrarr;", "succsim;", "supdsub;", "supedot;", "suphsol;", "suphsub;", "suplarr;", "supmult;", "supplus;", "swarrow;", "topfork;", "triplus;", "tritime;", "uparrow;", "upsilon;", "uwangle;", "vzigzag;", "zigrarr;", "Aacute;", "Abreve;", "Agrave;", "Assign;", "Atilde;", "Barwed;", "Bumpeq;", "Cacute;", "Ccaron;", "Ccedil;", "Colone;", "Conint;", "CupCap;", "Dagger;", "Dcaron;", "DotDot;", "Dstrok;", "Eacute;", "Ecaron;", "Egrave;", "Exists;", "ForAll;", "Gammad;", "Gbreve;", "Gcedil;", "HARDcy;", "Hstrok;", "Iacute;", "Igrave;", "Itilde;", "Jsercy;", "Kcedil;", "Lacute;", "Lambda;", "Lcaron;", "Lcedil;", "Lmidot;", "Lstrok;", "Nacute;", "Ncaron;", "Ncedil;", "Ntilde;", "Oacute;", "Odblac;", "Ograve;", "Oslash;", "Otilde;", "Otimes;", "Racute;", "Rarrtl;", "Rcaron;", "Rcedil;", "SHCHcy;", "SOFTcy;", "Sacute;", "Scaron;", "Scedil;", "Square;", "Subset;", "Supset;", "Tcaron;", "Tcedil;", "Tstrok;", "Uacute;", "Ubreve;", "Udblac;", "Ugrave;", "Utilde;", "Vdashl;", "Verbar;", "Vvdash;", "Yacute;", "Zacute;", "Zcaron;", "aacute;", "abreve;", "agrave;", "andand;", "angmsd;", "angsph;", "apacir;", "approx;", "atilde;", "barvee;", "barwed;", "becaus;", "bernou;", "bigcap;", "bigcup;", "bigvee;", "bkarow;", "bottom;", "bowtie;", "boxbox;", "bprime;", "brvbar;", "bullet;", "bumpeq;", "cacute;", "capand;", "capcap;", "capcup;", "capdot;", "ccaron;", "ccedil;", "circeq;", "cirmid;", "colone;", "commat;", "compfn;", "conint;", "coprod;", "copysr;", "cularr;", "cupcap;", "cupcup;", "cupdot;", "curarr;", "curren;", "cylcty;", "dagger;", "daleth;", "dcaron;", "dfisht;", "divide;", "divonx;", "dlcorn;", "dlcrop;", "dollar;", "drcorn;", "drcrop;", "dstrok;", "eacute;", "easter;", "ecaron;", "ecolon;", "egrave;", "egsdot;", "elsdot;", "emptyv;", "emsp13;", "emsp14;", "eparsl;", "eqcirc;", "equals;", "equest;", "female;", "ffilig;", "ffllig;", "forall;", "frac12;", "frac13;", "frac14;", "frac15;", "frac16;", "frac18;", "frac23;", "frac25;", "frac34;", "frac35;", "frac38;", "frac45;", "frac56;", "frac58;", "frac78;", "gacute;", "gammad;", "gbreve;", "gesdot;", "gesles;", "gtlPar;", "gtrarr;", "gtrdot;", "gtrsim;", "hairsp;", "hamilt;", "hardcy;", "hearts;", "hellip;", "hercon;", "homtht;", "horbar;", "hslash;", "hstrok;", "hybull;", "hyphen;", "iacute;", "igrave;", "iiiint;", "iinfin;", "incare;", "inodot;", "intcal;", "iquest;", "isinsv;", "itilde;", "jsercy;", "kappav;", "kcedil;", "kgreen;", "lAtail;", "lacute;", "lagran;", "lambda;", "langle;", "larrfs;", "larrhk;", "larrlp;", "larrpl;", "larrtl;", "latail;", "lbrace;", "lbrack;", "lcaron;", "lcedil;", "ldquor;", "lesdot;", "lesges;", "lfisht;", "lfloor;", "lharul;", "llhard;", "lmidot;", "lmoust;", "loplus;", "lowast;", "lowbar;", "lparlt;", "lrhard;", "lsaquo;", "lsquor;", "lstrok;", "lthree;", "ltimes;", "ltlarr;", "ltrPar;", "mapsto;", "marker;", "mcomma;", "midast;", "midcir;", "middot;", "minusb;", "minusd;", "mnplus;", "models;", "mstpos;", "nVDash;", "nVdash;", "nacute;", "nbumpe;", "ncaron;", "ncedil;", "nearhk;", "nequiv;", "nesear;", "nexist;", "nltrie;", "notinE;", "nparsl;", "nprcue;", "nrarrc;", "nrarrw;", "nrtrie;", "nsccue;", "nsimeq;", "ntilde;", "numero;", "nvDash;", "nvHarr;", "nvdash;", "nvlArr;", "nvrArr;", "nwarhk;", "nwnear;", "oacute;", "odblac;", "odsold;", "ograve;", "ominus;", "origof;", "oslash;", "otilde;", "otimes;", "parsim;", "percnt;", "period;", "permil;", "phmmat;", "planck;", "plankv;", "plusdo;", "plusdu;", "plusmn;", "preceq;", "primes;", "prnsim;", "propto;", "prurel;", "puncsp;", "qprime;", "rAtail;", "racute;", "rangle;", "rarrap;", "rarrfs;", "rarrhk;", "rarrlp;", "rarrpl;", "rarrtl;", "ratail;", "rbrace;", "rbrack;", "rcaron;", "rcedil;", "rdquor;", "rfisht;", "rfloor;", "rharul;", "rmoust;", "roplus;", "rpargt;", "rsaquo;", "rsquor;", "rthree;", "rtimes;", "sacute;", "scaron;", "scedil;", "scnsim;", "searhk;", "seswar;", "sfrown;", "shchcy;", "sigmaf;", "sigmav;", "simdot;", "smashp;", "softcy;", "solbar;", "spades;", "sqcaps;", "sqcups;", "sqsube;", "sqsupe;", "square;", "squarf;", "ssetmn;", "ssmile;", "sstarf;", "subdot;", "subset;", "subsim;", "subsub;", "subsup;", "succeq;", "supdot;", "supset;", "supsim;", "supsub;", "supsup;", "swarhk;", "swnwar;", "target;", "tcaron;", "tcedil;", "telrec;", "there4;", "thetav;", "thinsp;", "thksim;", "timesb;", "timesd;", "topbot;", "topcir;", "tprime;", "tridot;", "tstrok;", "uacute;", "ubreve;", "udblac;", "ufisht;", "ugrave;", "ulcorn;", "ulcrop;", "urcorn;", "urcrop;", "utilde;", "vangrt;", "varphi;", "varrho;", "veebar;", "vellip;", "verbar;", "vsubnE;", "vsubne;", "vsupnE;", "vsupne;", "wedbar;", "wedgeq;", "weierp;", "wreath;", "xoplus;", "xotime;", "xsqcup;", "xuplus;", "xwedge;", "yacute;", "zacute;", "zcaron;", "zeetrf;", "AElig;", "Aacute", "Acirc;", "Agrave", "Alpha;", "Amacr;", "Aogon;", "Aring;", "Atilde", "Breve;", "Ccedil", "Ccirc;", "Colon;", "Cross;", "Dashv;", "Delta;", "Eacute", "Ecirc;", "Egrave", "Emacr;", "Eogon;", "Equal;", "Gamma;", "Gcirc;", "Hacek;", "Hcirc;", "IJlig;", "Iacute", "Icirc;", "Igrave", "Imacr;", "Iogon;", "Iukcy;", "Jcirc;", "Jukcy;", "Kappa;", "Ntilde", "OElig;", "Oacute", "Ocirc;", "Ograve", "Omacr;", "Omega;", "Oslash", "Otilde", "Prime;", "RBarr;", "Scirc;", "Sigma;", "THORN;", "TRADE;", "TSHcy;", "Theta;", "Tilde;", "Uacute", "Ubrcy;", "Ucirc;", "Ugrave", "Umacr;", "Union;", "Uogon;", "UpTee;", "Uring;", "VDash;", "Vdash;", "Wcirc;", "Wedge;", "Yacute", "Ycirc;", "aacute", "acirc;", "acute;", "aelig;", "agrave", "aleph;", "alpha;", "amacr;", "amalg;", "angle;", "angrt;", "angst;", "aogon;", "aring;", "asymp;", "atilde", "awint;", "bcong;", "bdquo;", "bepsi;", "blank;", "blk12;", "blk14;", "blk34;", "block;", "boxDL;", "boxDR;", "boxDl;", "boxDr;", "boxHD;", "boxHU;", "boxHd;", "boxHu;", "boxUL;", "boxUR;", "boxUl;", "boxUr;", "boxVH;", "boxVL;", "boxVR;", "boxVh;", "boxVl;", "boxVr;", "boxdL;", "boxdR;", "boxdl;", "boxdr;", "boxhD;", "boxhU;", "boxhd;", "boxhu;", "boxuL;", "boxuR;", "boxul;", "boxur;", "boxvH;", "boxvL;", "boxvR;", "boxvh;", "boxvl;", "boxvr;", "breve;", "brvbar", "bsemi;", "bsime;", "bsolb;", "bumpE;", "bumpe;", "caret;", "caron;", "ccaps;", "ccedil", "ccirc;", "ccups;", "cedil;", "check;", "clubs;", "colon;", "comma;", "crarr;", "cross;", "csube;", "csupe;", "ctdot;", "cuepr;", "cuesc;", "cupor;", "curren", "cuvee;", "cuwed;", "cwint;", "dashv;", "dblac;", "ddarr;", "delta;", "dharl;", "dharr;", "diams;", "disin;", "divide", "doteq;", "dtdot;", "dtrif;", "duarr;", "duhar;", "eDDot;", "eacute", "ecirc;", "efDot;", "egrave", "emacr;", "empty;", "eogon;", "eplus;", "epsiv;", "eqsim;", "equiv;", "erDot;", "erarr;", "esdot;", "exist;", "fflig;", "filig;", "fjlig;", "fllig;", "fltns;", "forkv;", "frac12", "frac14", "frac34", "frasl;", "frown;", "gamma;", "gcirc;", "gescc;", "gimel;", "gneqq;", "gnsim;", "grave;", "gsime;", "gsiml;", "gtcir;", "gtdot;", "harrw;", "hcirc;", "hoarr;", "iacute", "icirc;", "iexcl;", "igrave", "iiint;", "iiota;", "ijlig;", "imacr;", "image;", "imath;", "imped;", "infin;", "iogon;", "iprod;", "iquest", "isinE;", "isins;", "isinv;", "iukcy;", "jcirc;", "jmath;", "jukcy;", "kappa;", "lAarr;", "lBarr;", "langd;", "laquo;", "larrb;", "lates;", "lbarr;", "lbbrk;", "lbrke;", "lceil;", "ldquo;", "lescc;", "lhard;", "lharu;", "lhblk;", "llarr;", "lltri;", "lneqq;", "lnsim;", "loang;", "loarr;", "lobrk;", "lopar;", "lrarr;", "lrhar;", "lrtri;", "lsime;", "lsimg;", "lsquo;", "ltcir;", "ltdot;", "ltrie;", "ltrif;", "mDDot;", "mdash;", "micro;", "middot", "minus;", "mumap;", "nabla;", "napid;", "napos;", "natur;", "nbump;", "ncong;", "ndash;", "neArr;", "nearr;", "nedot;", "nesim;", "ngeqq;", "ngsim;", "nhArr;", "nharr;", "nhpar;", "nlArr;", "nlarr;", "nleqq;", "nless;", "nlsim;", "nltri;", "notin;", "notni;", "npart;", "nprec;", "nrArr;", "nrarr;", "nrtri;", "nsime;", "nsmid;", "nspar;", "nsubE;", "nsube;", "nsucc;", "nsupE;", "nsupe;", "ntilde", "numsp;", "nvsim;", "nwArr;", "nwarr;", "oacute", "ocirc;", "odash;", "oelig;", "ofcir;", "ograve", "ohbar;", "olarr;", "olcir;", "oline;", "omacr;", "omega;", "operp;", "oplus;", "orarr;", "order;", "oslash", "otilde", "ovbar;", "parsl;", "phone;", "plusb;", "pluse;", "plusmn", "pound;", "prcue;", "prime;", "prnap;", "prsim;", "quest;", "rAarr;", "rBarr;", "radic;", "rangd;", "range;", "raquo;", "rarrb;", "rarrc;", "rarrw;", "ratio;", "rbarr;", "rbbrk;", "rbrke;", "rceil;", "rdquo;", "reals;", "rhard;", "rharu;", "rlarr;", "rlhar;", "rnmid;", "roang;", "roarr;", "robrk;", "ropar;", "rrarr;", "rsquo;", "rtrie;", "rtrif;", "sbquo;", "sccue;", "scirc;", "scnap;", "scsim;", "sdotb;", "sdote;", "seArr;", "searr;", "setmn;", "sharp;", "sigma;", "simeq;", "simgE;", "simlE;", "simne;", "slarr;", "smile;", "smtes;", "sqcap;", "sqcup;", "sqsub;", "sqsup;", "srarr;", "starf;", "strns;", "subnE;", "subne;", "supnE;", "supne;", "swArr;", "swarr;", "szlig;", "theta;", "thkap;", "thorn;", "tilde;", "times;", "trade;", "trisb;", "tshcy;", "twixt;", "uacute", "ubrcy;", "ucirc;", "udarr;", "udhar;", "ugrave", "uharl;", "uharr;", "uhblk;", "ultri;", "umacr;", "uogon;", "uplus;", "upsih;", "uring;", "urtri;", "utdot;", "utrif;", "uuarr;", "vBarv;", "vDash;", "varpi;", "vdash;", "veeeq;", "vltri;", "vnsub;", "vnsup;", "vprop;", "vrtri;", "wcirc;", "wedge;", "xcirc;", "xdtri;", "xhArr;", "xharr;", "xlArr;", "xlarr;", "xodot;", "xrArr;", "xrarr;", "xutri;", "yacute", "ycirc;", "AElig", "Acirc", "Aopf;", "Aring", "Ascr;", "Auml;", "Barv;", "Beta;", "Bopf;", "Bscr;", "CHcy;", "COPY;", "Cdot;", "Copf;", "Cscr;", "DJcy;", "DScy;", "DZcy;", "Darr;", "Dopf;", "Dscr;", "Ecirc", "Edot;", "Eopf;", "Escr;", "Esim;", "Euml;", "Fopf;", "Fscr;", "GJcy;", "Gdot;", "Gopf;", "Gscr;", "Hopf;", "Hscr;", "IEcy;", "IOcy;", "Icirc", "Idot;", "Iopf;", "Iota;", "Iscr;", "Iuml;", "Jopf;", "Jscr;", "KHcy;", "KJcy;", "Kopf;", "Kscr;", "LJcy;", "Lang;", "Larr;", "Lopf;", "Lscr;", "Mopf;", "Mscr;", "NJcy;", "Nopf;", "Nscr;", "Ocirc", "Oopf;", "Oscr;", "Ouml;", "Popf;", "Pscr;", "QUOT;", "Qopf;", "Qscr;", "Rang;", "Rarr;", "Ropf;", "Rscr;", "SHcy;", "Sopf;", "Sqrt;", "Sscr;", "Star;", "THORN", "TScy;", "Topf;", "Tscr;", "Uarr;", "Ucirc", "Uopf;", "Upsi;", "Uscr;", "Uuml;", "Vbar;", "Vert;", "Vopf;", "Vscr;", "Wopf;", "Wscr;", "Xopf;", "Xscr;", "YAcy;", "YIcy;", "YUcy;", "Yopf;", "Yscr;", "Yuml;", "ZHcy;", "Zdot;", "Zeta;", "Zopf;", "Zscr;", "acirc", "acute", "aelig", "andd;", "andv;", "ange;", "aopf;", "apid;", "apos;", "aring", "ascr;", "auml;", "bNot;", "bbrk;", "beta;", "beth;", "bnot;", "bopf;", "boxH;", "boxV;", "boxh;", "boxv;", "bscr;", "bsim;", "bsol;", "bull;", "bump;", "caps;", "cdot;", "cedil", "cent;", "chcy;", "cirE;", "circ;", "cire;", "comp;", "cong;", "copf;", "copy;", "cscr;", "csub;", "csup;", "cups;", "dArr;", "dHar;", "darr;", "dash;", "diam;", "djcy;", "dopf;", "dscr;", "dscy;", "dsol;", "dtri;", "dzcy;", "eDot;", "ecir;", "ecirc", "edot;", "emsp;", "ensp;", "eopf;", "epar;", "epsi;", "escr;", "esim;", "euml;", "euro;", "excl;", "flat;", "fnof;", "fopf;", "fork;", "fscr;", "gdot;", "geqq;", "gesl;", "gjcy;", "gnap;", "gneq;", "gopf;", "gscr;", "gsim;", "gtcc;", "gvnE;", "hArr;", "half;", "harr;", "hbar;", "hopf;", "hscr;", "icirc", "iecy;", "iexcl", "imof;", "iocy;", "iopf;", "iota;", "iscr;", "isin;", "iuml;", "jopf;", "jscr;", "khcy;", "kjcy;", "kopf;", "kscr;", "lArr;", "lHar;", "lang;", "laquo", "larr;", "late;", "lcub;", "ldca;", "ldsh;", "leqq;", "lesg;", "ljcy;", "lnap;", "lneq;", "lopf;", "lozf;", "lpar;", "lscr;", "lsim;", "lsqb;", "ltcc;", "ltri;", "lvnE;", "macr;", "male;", "malt;", "micro", "mlcp;", "mldr;", "mopf;", "mscr;", "nGtv;", "nLtv;", "nang;", "napE;", "nbsp;", "ncap;", "ncup;", "ngeq;", "nges;", "ngtr;", "nisd;", "njcy;", "nldr;", "nleq;", "nles;", "nmid;", "nopf;", "npar;", "npre;", "nsce;", "nscr;", "nsim;", "nsub;", "nsup;", "ntgl;", "ntlg;", "nvap;", "nvge;", "nvgt;", "nvle;", "nvlt;", "oast;", "ocir;", "ocirc", "odiv;", "odot;", "ogon;", "oint;", "omid;", "oopf;", "opar;", "ordf;", "ordm;", "oror;", "oscr;", "osol;", "ouml;", "para;", "part;", "perp;", "phiv;", "plus;", "popf;", "pound", "prap;", "prec;", "prnE;", "prod;", "prop;", "pscr;", "qint;", "qopf;", "qscr;", "quot;", "rArr;", "rHar;", "race;", "rang;", "raquo", "rarr;", "rcub;", "rdca;", "rdsh;", "real;", "rect;", "rhov;", "ring;", "ropf;", "rpar;", "rscr;", "rsqb;", "rtri;", "scap;", "scnE;", "sdot;", "sect;", "semi;", "sext;", "shcy;", "sime;", "simg;", "siml;", "smid;", "smte;", "solb;", "sopf;", "spar;", "squf;", "sscr;", "star;", "subE;", "sube;", "succ;", "sung;", "sup1;", "sup2;", "sup3;", "supE;", "supe;", "szlig", "tbrk;", "tdot;", "thorn", "times", "tint;", "toea;", "topf;", "tosa;", "trie;", "tscr;", "tscy;", "uArr;", "uHar;", "uarr;", "ucirc", "uopf;", "upsi;", "uscr;", "utri;", "uuml;", "vArr;", "vBar;", "varr;", "vert;", "vopf;", "vscr;", "wopf;", "wscr;", "xcap;", "xcup;", "xmap;", "xnis;", "xopf;", "xscr;", "xvee;", "yacy;", "yicy;", "yopf;", "yscr;", "yucy;", "yuml;", "zdot;", "zeta;", "zhcy;", "zopf;", "zscr;", "zwnj;", "AMP;", "Acy;", "Afr;", "And;", "Auml", "Bcy;", "Bfr;", "COPY", "Cap;", "Cfr;", "Chi;", "Cup;", "Dcy;", "Del;", "Dfr;", "Dot;", "ENG;", "ETH;", "Ecy;", "Efr;", "Eta;", "Euml", "Fcy;", "Ffr;", "Gcy;", "Gfr;", "Hat;", "Hfr;", "Icy;", "Ifr;", "Int;", "Iuml", "Jcy;", "Jfr;", "Kcy;", "Kfr;", "Lcy;", "Lfr;", "Lsh;", "Map;", "Mcy;", "Mfr;", "Ncy;", "Nfr;", "Not;", "Ocy;", "Ofr;", "Ouml", "Pcy;", "Pfr;", "Phi;", "Psi;", "QUOT", "Qfr;", "REG;", "Rcy;", "Rfr;", "Rho;", "Rsh;", "Scy;", "Sfr;", "Sub;", "Sum;", "Sup;", "Tab;", "Tau;", "Tcy;", "Tfr;", "Ucy;", "Ufr;", "Uuml", "Vcy;", "Vee;", "Vfr;", "Wfr;", "Xfr;", "Ycy;", "Yfr;", "Zcy;", "Zfr;", "acE;", "acd;", "acy;", "afr;", "amp;", "and;", "ang;", "apE;", "ape;", "ast;", "auml", "bcy;", "bfr;", "bne;", "bot;", "cap;", "cent", "cfr;", "chi;", "cir;", "copy", "cup;", "dcy;", "deg;", "dfr;", "die;", "div;", "dot;", "ecy;", "efr;", "egs;", "ell;", "els;", "eng;", "eta;", "eth;", "euml", "fcy;", "ffr;", "gEl;", "gap;", "gcy;", "gel;", "geq;", "ges;", "gfr;", "ggg;", "glE;", "gla;", "glj;", "gnE;", "gne;", "hfr;", "icy;", "iff;", "ifr;", "int;", "iuml", "jcy;", "jfr;", "kcy;", "kfr;", "lEg;", "lap;", "lat;", "lcy;", "leg;", "leq;", "les;", "lfr;", "lgE;", "lnE;", "lne;", "loz;", "lrm;", "lsh;", "macr", "map;", "mcy;", "mfr;", "mho;", "mid;", "nGg;", "nGt;", "nLl;", "nLt;", "nap;", "nbsp", "ncy;", "nfr;", "ngE;", "nge;", "ngt;", "nis;", "niv;", "nlE;", "nle;", "nlt;", "not;", "npr;", "nsc;", "num;", "ocy;", "ofr;", "ogt;", "ohm;", "olt;", "ord;", "ordf", "ordm", "orv;", "ouml", "par;", "para", "pcy;", "pfr;", "phi;", "piv;", "prE;", "pre;", "psi;", "qfr;", "quot", "rcy;", "reg;", "rfr;", "rho;", "rlm;", "rsh;", "scE;", "sce;", "scy;", "sect", "sfr;", "shy;", "sim;", "smt;", "sol;", "squ;", "sub;", "sum;", "sup1", "sup2", "sup3", "sup;", "tau;", "tcy;", "tfr;", "top;", "ucy;", "ufr;", "uml;", "uuml", "vcy;", "vee;", "vfr;", "wfr;", "xfr;", "ycy;", "yen;", "yfr;", "yuml", "zcy;", "zfr;", "zwj;", "AMP", "DD;", "ETH", "GT;", "Gg;", "Gt;", "Im;", "LT;", "Ll;", "Lt;", "Mu;", "Nu;", "Or;", "Pi;", "Pr;", "REG", "Re;", "Sc;", "Xi;", "ac;", "af;", "amp", "ap;", "dd;", "deg", "ee;", "eg;", "el;", "eth", "gE;", "ge;", "gg;", "gl;", "gt;", "ic;", "ii;", "in;", "it;", "lE;", "le;", "lg;", "ll;", "lt;", "mp;", "mu;", "ne;", "ni;", "not", "nu;", "oS;", "or;", "pi;", "pm;", "pr;", "reg", "rx;", "sc;", "shy", "uml", "wp;", "wr;", "xi;", "yen", "GT", "LT", "gt", "lt"};
	private static int[] entityValues=new int[]{8755, 8754, 10234, -1, 733, 8931, 8221, 8751, 9642, 8203, 8928, 8941, 8929, 8517, 8660, 10233, 9643, 8811, 8742, -2, 8940, 8930, 8220, 10607, 10232, 10576, 8646, 8203, -3, -4, 8644, 8850, 8621, 10591, 10583, 10231, 10234, 8203, -5, 8828, 8651, 10215, 10589, 10581, 8885, 8851, 8829, 9656, 10231, 8661, 8741, 10590, 10582, 9724, 10878, 10214, 10593, 10585, 8884, 8203, -6, -7, -8, 8716, -9, 8775, 10217, 10575, 8849, 10072, 9662, 9666, 8651, 8652, 8608, 180, 96, 732, 8658, 8693, 9723, 8923, 8807, 10216, 10577, 8922, 160, -10, 8939, -11, -12, 8841, 10704, 10588, 10580, 9181, 8645, 8635, 8642, 8941, 8641, 8649, 8606, 8883, 8217, 8750, 8659, 8656, 8641, 10574, 10703, 10592, 10584, 8600, 8817, 8821, -13, 8938, -14, 9180, 8642, 8594, 8599, 9661, 8634, 8631, 8643, 8637, 8646, 8654, 8622, 8940, 8644, 8605, 8908, 1013, 8885, 8882, 729, 8872, 8637, 10914, 9472, 8291, 8290, 8643, 8596, 8660, 10877, 10230, 10233, 8601, 8810, 8825, 8824, 8840, 8740, 8216, 8715, 10587, 10579, 8595, 8592, 8848, 8773, 8598, 8203, 8630, 8966, 8650, 8618, 8647, 8596, 8907, 10230, 8620, 8742, 8939, 8611, 8640, 8884, 8638, 8289, 8518, 10980, 8657, 10586, 10578, 8806, 10229, 10232, -15, 8772, 8777, 8460, 10927, 8830, 8677, 8614, 8883, 8638, 10928, 8831, 8839, 10606, 8768, 8202, 9651, 9652, 8903, 8786, 8617, 8610, 8636, 10229, 8619, 8737, 8938, 8741, 8726, 9657, 8639, -16, -17, 10515, 8615, 8519, 8805, 8819, 8459, 8782, 8898, 8676, 8612, 8882, 8639, 8802, -18, 8816, 8820, 8733, 8969, 10608, 8593, 8847, 9141, 124, 10731, 8519, 8787, 9663, 9667, -19, -20, 8854, 8855, 8652, 8823, 8968, 8822, 8287, -21, 8832, 8833, -22, 9140, 8640, 8667, 10740, 8728, 8852, 8838, 8597, 8661, 8739, 1014, 9642, 8858, 8861, 8926, 8927, 9830, 10901, 8496, 8655, 8603, 8828, 10937, 8461, 981, 8829, 10938, 8776, 8597, 8492, 8853, 8770, 8497, 8520, 8466, 8636, 8666, 8713, 8815, 8759, 8594, 8971, 8658, -23, 8771, 8776, 9183, 10514, 8613, 8859, 8705, 8911, 10902, 10892, 10885, 10891, 9136, 10236, 8615, 8612, 8653, 8602, -24, -25, 10935, 8594, 9137, 8849, 8850, 10955, 10936, 10956, 8648, 1013, 8709, 8726, 183, 8857, 8801, 8720, 168, 8595, 785, 8659, 8783, 8592, 8970, 8656, 8818, 8499, 8723, 8813, 8708, -26, 9182, 177, 8756, 8201, 8411, 8846, 8245, 8909, 10754, 183, 10003, 8450, 8865, 8595, 10886, 8923, -27, 9829, 8592, 8922, -28, -29, -30, 8742, 8740, 8840, 8841, 8916, 8474, 9824, 10949, 8842, 10950, 8843, 8756, 8796, 8733, 10513, 8784, 8747, 10913, 8800, 8769, 8706, 8826, 8866, 8827, 8715, 8835, 10569, 95, 10840, 10664, 10665, 10666, 10667, 10668, 10669, 10670, 10671, 10653, 8778, 8755, 8780, 8965, 9142, 10753, 10758, 10756, 8896, 8863, 8864, 10184, 10825, 174, 9416, 10768, 9827, 10824, 8910, 8754, 8785, 8760, 10512, 10239, 9191, 8709, 10725, 10765, 10878, 10884, 10890, 10533, 10534, 8464, 8465, 10717, 8484, 8890, 10775, 10676, 10571, 10877, 10883, 8990, 10889, 8991, 10570, 8613, 8888, 8469, -31, -32, 10806, 8741, 10787, 10773, 10933, 8936, 9006, 8978, 8979, 10675, 8476, 10770, 10702, 10771, 8726, 8739, 10724, 8847, 8848, 8838, 10934, 8937, 8839, 977, 8764, 10801, 9653, 10810, 9186, 8988, 8989, 1008, 962, 977, 8757, 8493, 8752, 184, 8900, 8868, 8712, 917, 8658, 8867, 10, 8288, 8814, 927, 8254, 8719, 8593, 8657, 933, 8501, 8894, 9084, 8781, 8765, 8757, 10672, 8812, 9711, 10752, 9733, -33, 8862, 10832, 10674, 10690, 8788, 10861, 10552, 10549, 10557, 10556, 10511, 8225, 10871, 10673, 8900, 989, 8724, 10662, 949, 8789, 10872, 10882, 10876, 8823, 10568, 10812, 8949, 10527, 10611, 10639, 10637, 10599, 10881, 8918, 8822, 8818, 10804, 9674, 10875, 10598, 10016, 10794, 8777, 9838, 8599, 8708, 8713, 8951, 8950, 8716, 8958, 8957, 10772, -34, 8930, 8931, -35, -36, -37, 10718, -38, -39, 8598, 10683, 959, 8500, 10839, 8241, 8462, 10786, 10790, 10791, 8830, 10774, 8799, 10528, 10612, 10638, 10640, 10601, 8475, 10805, 10600, 8600, 10788, 10610, 10947, 10945, 10943, 10617, 8831, 10968, 10948, 10185, 10967, 10619, 10946, 10944, 8601, 10970, 10809, 10811, 8593, 965, 10663, 10650, 8669, 193, 258, 192, 8788, 195, 8966, 8782, 262, 268, 199, 10868, 8751, 8781, 8225, 270, 8412, 272, 201, 282, 200, 8707, 8704, 988, 286, 290, 1066, 294, 205, 204, 296, 1032, 310, 313, 923, 317, 315, 319, 321, 323, 327, 325, 209, 211, 336, 210, 216, 213, 10807, 340, 10518, 344, 342, 1065, 1068, 346, 352, 350, 9633, 8912, 8913, 356, 354, 358, 218, 364, 368, 217, 360, 10982, 8214, 8874, 221, 377, 381, 225, 259, 224, 10837, 8737, 8738, 10863, 8776, 227, 8893, 8965, 8757, 8492, 8898, 8899, 8897, 10509, 8869, 8904, 10697, 8245, 166, 8226, 8783, 263, 10820, 10827, 10823, 10816, 269, 231, 8791, 10991, 8788, 64, 8728, 8750, 8720, 8471, 8630, 10822, 10826, 8845, 8631, 164, 9005, 8224, 8504, 271, 10623, 247, 8903, 8990, 8973, 36, 8991, 8972, 273, 233, 10862, 283, 8789, 232, 10904, 10903, 8709, 8196, 8197, 10723, 8790, 61, 8799, 9792, 64259, 64260, 8704, 189, 8531, 188, 8533, 8537, 8539, 8532, 8534, 190, 8535, 8540, 8536, 8538, 8541, 8542, 501, 989, 287, 10880, 10900, 10645, 10616, 8919, 8819, 8202, 8459, 1098, 9829, 8230, 8889, 8763, 8213, 8463, 295, 8259, 8208, 237, 236, 10764, 10716, 8453, 305, 8890, 191, 8947, 297, 1112, 1008, 311, 312, 10523, 314, 8466, 955, 10216, 10525, 8617, 8619, 10553, 8610, 10521, 123, 91, 318, 316, 8222, 10879, 10899, 10620, 8970, 10602, 10603, 320, 9136, 10797, 8727, 95, 10643, 10605, 8249, 8218, 322, 8907, 8905, 10614, 10646, 8614, 9646, 10793, 42, 10992, 183, 8863, 8760, 8723, 8871, 8766, 8879, 8878, 324, -40, 328, 326, 10532, 8802, 10536, 8708, 8940, -41, -42, 8928, -43, -44, 8941, 8929, 8772, 241, 8470, 8877, 10500, 8876, 10498, 10499, 10531, 10535, 243, 337, 10684, 242, 8854, 8886, 248, 245, 8855, 10995, 37, 46, 8240, 8499, 8463, 8463, 8724, 10789, 177, 10927, 8473, 8936, 8733, 8880, 8200, 8279, 10524, 341, 10217, 10613, 10526, 8618, 8620, 10565, 8611, 10522, 125, 93, 345, 343, 8221, 10621, 8971, 10604, 9137, 10798, 10644, 8250, 8217, 8908, 8906, 347, 353, 351, 8937, 10533, 10537, 8994, 1097, 962, 962, 10858, 10803, 1100, 9023, 9824, -45, -46, 8849, 8850, 9633, 9642, 8726, 8995, 8902, 10941, 8834, 10951, 10965, 10963, 10928, 10942, 8835, 10952, 10964, 10966, 10534, 10538, 8982, 357, 355, 8981, 8756, 977, 8201, 8764, 8864, 10800, 9014, 10993, 8244, 9708, 359, 250, 365, 369, 10622, 249, 8988, 8975, 8989, 8974, 361, 10652, 981, 1009, 8891, 8942, 124, -47, -48, -49, -50, 10847, 8793, 8472, 8768, 10753, 10754, 10758, 10756, 8896, 253, 378, 382, 8488, 198, 193, 194, 192, 913, 256, 260, 197, 195, 728, 199, 264, 8759, 10799, 10980, 916, 201, 202, 200, 274, 280, 10869, 915, 284, 711, 292, 306, 205, 206, 204, 298, 302, 1030, 308, 1028, 922, 209, 338, 211, 212, 210, 332, 937, 216, 213, 8243, 10512, 348, 931, 222, 8482, 1035, 920, 8764, 218, 1038, 219, 217, 362, 8899, 370, 8869, 366, 8875, 8873, 372, 8896, 221, 374, 225, 226, 180, 230, 224, 8501, 945, 257, 10815, 8736, 8735, 197, 261, 229, 8776, 227, 10769, 8780, 8222, 1014, 9251, 9618, 9617, 9619, 9608, 9559, 9556, 9558, 9555, 9574, 9577, 9572, 9575, 9565, 9562, 9564, 9561, 9580, 9571, 9568, 9579, 9570, 9567, 9557, 9554, 9488, 9484, 9573, 9576, 9516, 9524, 9563, 9560, 9496, 9492, 9578, 9569, 9566, 9532, 9508, 9500, 728, 166, 8271, 8909, 10693, 10926, 8783, 8257, 711, 10829, 231, 265, 10828, 184, 10003, 9827, 58, 44, 8629, 10007, 10961, 10962, 8943, 8926, 8927, 10821, 164, 8910, 8911, 8753, 8867, 733, 8650, 948, 8643, 8642, 9830, 8946, 247, 8784, 8945, 9662, 8693, 10607, 10871, 233, 234, 8786, 232, 275, 8709, 281, 10865, 1013, 8770, 8801, 8787, 10609, 8784, 8707, 64256, 64257, -51, 64258, 9649, 10969, 189, 188, 190, 8260, 8994, 947, 285, 10921, 8503, 8809, 8935, 96, 10894, 10896, 10874, 8919, 8621, 293, 8703, 237, 238, 161, 236, 8749, 8489, 307, 299, 8465, 305, 437, 8734, 303, 10812, 191, 8953, 8948, 8712, 1110, 309, 567, 1108, 954, 8666, 10510, 10641, 171, 8676, -52, 10508, 10098, 10635, 8968, 8220, 10920, 8637, 8636, 9604, 8647, 9722, 8808, 8934, 10220, 8701, 10214, 10629, 8646, 8651, 8895, 10893, 10895, 8216, 10873, 8918, 8884, 9666, 8762, 8212, 181, 183, 8722, 8888, 8711, -53, 329, 9838, -54, 8775, 8211, 8663, 8599, -55, -56, -57, 8821, 8654, 8622, 10994, 8653, 8602, -58, 8814, 8820, 8938, 8713, 8716, -59, 8832, 8655, 8603, 8939, 8772, 8740, 8742, -60, 8840, 8833, -61, 8841, 241, 8199, -62, 8662, 8598, 243, 244, 8861, 339, 10687, 242, 10677, 8634, 10686, 8254, 333, 969, 10681, 8853, 8635, 8500, 248, 245, 9021, 11005, 9742, 8862, 10866, 177, 163, 8828, 8242, 10937, 8830, 63, 8667, 10511, 8730, 10642, 10661, 187, 8677, 10547, 8605, 8758, 10509, 10099, 10636, 8969, 8221, 8477, 8641, 8640, 8644, 8652, 10990, 10221, 8702, 10215, 10630, 8649, 8217, 8885, 9656, 8218, 8829, 349, 10938, 8831, 8865, 10854, 8664, 8600, 8726, 9839, 963, 8771, 10912, 10911, 8774, 8592, 8995, -63, 8851, 8852, 8847, 8848, 8594, 9733, 175, 10955, 8842, 10956, 8843, 8665, 8601, 223, 952, 8776, 254, 732, 215, 8482, 10701, 1115, 8812, 250, 1118, 251, 8645, 10606, 249, 8639, 8638, 9600, 9720, 363, 371, 8846, 978, 367, 9721, 8944, 9652, 8648, 10985, 8872, 982, 8866, 8794, 8882, -64, -65, 8733, 8883, 373, 8743, 9711, 9661, 10234, 10231, 10232, 10229, 10752, 10233, 10230, 9651, 253, 375, 198, 194, 120120, 197, 119964, 196, 10983, 914, 120121, 8492, 1063, 169, 266, 8450, 119966, 1026, 1029, 1039, 8609, 120123, 119967, 202, 278, 120124, 8496, 10867, 203, 120125, 8497, 1027, 288, 120126, 119970, 8461, 8459, 1045, 1025, 206, 304, 120128, 921, 8464, 207, 120129, 119973, 1061, 1036, 120130, 119974, 1033, 10218, 8606, 120131, 8466, 120132, 8499, 1034, 8469, 119977, 212, 120134, 119978, 214, 8473, 119979, 34, 8474, 119980, 10219, 8608, 8477, 8475, 1064, 120138, 8730, 119982, 8902, 222, 1062, 120139, 119983, 8607, 219, 120140, 978, 119984, 220, 10987, 8214, 120141, 119985, 120142, 119986, 120143, 119987, 1071, 1031, 1070, 120144, 119988, 376, 1046, 379, 918, 8484, 119989, 226, 180, 230, 10844, 10842, 10660, 120146, 8779, 39, 229, 119990, 228, 10989, 9141, 946, 8502, 8976, 120147, 9552, 9553, 9472, 9474, 119991, 8765, 92, 8226, 8782, -66, 267, 184, 162, 1095, 10691, 710, 8791, 8705, 8773, 120148, 169, 119992, 10959, 10960, -67, 8659, 10597, 8595, 8208, 8900, 1106, 120149, 119993, 1109, 10742, 9663, 1119, 8785, 8790, 234, 279, 8195, 8194, 120150, 8917, 949, 8495, 8770, 235, 8364, 33, 9837, 402, 120151, 8916, 119995, 289, 8807, -68, 1107, 10890, 10888, 120152, 8458, 8819, 10919, -69, 8660, 189, 8596, 8463, 120153, 119997, 238, 1077, 161, 8887, 1105, 120154, 953, 119998, 8712, 239, 120155, 119999, 1093, 1116, 120156, 120000, 8656, 10594, 10216, 171, 8592, 10925, 123, 10550, 8626, 8806, -70, 1113, 10889, 10887, 120157, 10731, 40, 120001, 8818, 91, 10918, 9667, -71, 175, 9794, 10016, 181, 10971, 8230, 120158, 120002, -72, -73, -74, -75, 160, 10819, 10818, 8817, -76, 8815, 8954, 1114, 8229, 8816, -77, 8740, 120159, 8742, -78, -79, 120003, 8769, 8836, 8837, 8825, 8824, -80, -81, -82, -83, -84, 8859, 8858, 244, 10808, 8857, 731, 8750, 10678, 120160, 10679, 170, 186, 10838, 8500, 8856, 246, 182, 8706, 8869, 981, 43, 120161, 163, 10935, 8826, 10933, 8719, 8733, 120005, 10764, 120162, 120006, 34, 8658, 10596, -85, 10217, 187, 8594, 125, 10551, 8627, 8476, 9645, 1009, 730, 120163, 41, 120007, 93, 9657, 10936, 10934, 8901, 167, 59, 10038, 1096, 8771, 10910, 10909, 8739, 10924, 10692, 120164, 8741, 9642, 120008, 9734, 10949, 8838, 8827, 9834, 185, 178, 179, 10950, 8839, 223, 9140, 8411, 254, 215, 8749, 10536, 120165, 10537, 8796, 120009, 1094, 8657, 10595, 8593, 251, 120166, 965, 120010, 9653, 252, 8661, 10984, 8597, 124, 120167, 120011, 120168, 120012, 8898, 8899, 10236, 8955, 120169, 120013, 8897, 1103, 1111, 120170, 120014, 1102, 255, 380, 950, 1078, 120171, 120015, 8204, 38, 1040, 120068, 10835, 196, 1041, 120069, 169, 8914, 8493, 935, 8915, 1044, 8711, 120071, 168, 330, 208, 1069, 120072, 919, 203, 1060, 120073, 1043, 120074, 94, 8460, 1048, 8465, 8748, 207, 1049, 120077, 1050, 120078, 1051, 120079, 8624, 10501, 1052, 120080, 1053, 120081, 10988, 1054, 120082, 214, 1055, 120083, 934, 936, 34, 120084, 174, 1056, 8476, 929, 8625, 1057, 120086, 8912, 8721, 8913, 9, 932, 1058, 120087, 1059, 120088, 220, 1042, 8897, 120089, 120090, 120091, 1067, 120092, 1047, 8488, -86, 8767, 1072, 120094, 38, 8743, 8736, 10864, 8778, 42, 228, 1073, 120095, -87, 8869, 8745, 162, 120096, 967, 9675, 169, 8746, 1076, 176, 120097, 168, 247, 729, 1101, 120098, 10902, 8467, 10901, 331, 951, 240, 235, 1092, 120099, 10892, 10886, 1075, 8923, 8805, 10878, 120100, 8921, 10898, 10917, 10916, 8809, 10888, 120101, 1080, 8660, 120102, 8747, 239, 1081, 120103, 1082, 120104, 10891, 10885, 10923, 1083, 8922, 8804, 10877, 120105, 10897, 8808, 10887, 9674, 8206, 8624, 175, 8614, 1084, 120106, 8487, 8739, -88, -89, -90, -91, 8777, 160, 1085, 120107, -92, 8817, 8815, 8956, 8715, -93, 8816, 8814, 172, 8832, 8833, 35, 1086, 120108, 10689, 937, 10688, 10845, 170, 186, 10843, 246, 8741, 182, 1087, 120109, 966, 982, 10931, 10927, 968, 120110, 34, 1088, 174, 120111, 961, 8207, 8625, 10932, 10928, 1089, 167, 120112, 173, 8764, 10922, 47, 9633, 8834, 8721, 185, 178, 179, 8835, 964, 1090, 120113, 8868, 1091, 120114, 168, 252, 1074, 8744, 120115, 120116, 120117, 1099, 165, 120118, 255, 1079, 120119, 8205, 38, 8517, 208, 62, 8921, 8811, 8465, 60, 8920, 8810, 924, 925, 10836, 928, 10939, 174, 8476, 10940, 926, 8766, 8289, 38, 8776, 8518, 176, 8519, 10906, 10905, 240, 8807, 8805, 8811, 8823, 62, 8291, 8520, 8712, 8290, 8806, 8804, 8822, 8810, 60, 8723, 956, 8800, 8715, 172, 957, 9416, 8744, 960, 177, 8826, 174, 8478, 8827, 173, 168, 8472, 8768, 958, 165, 62, 60, 62, 60};
	private static int[] entityDoubles=new int[]{10914, 824, 10878, 824, 8807, 824, 10704, 824, 10703, 824, 8811, 824, 10877, 824, 10913, 824, 8848, 824, 10927, 824, 10928, 824, 8831, 824, 8782, 824, 8847, 824, 8770, 824, 10955, 65024, 10956, 65024, 8783, 824, 8842, 65024, 8843, 65024, 8810, 824, 8835, 8402, 8287, 8202, 10949, 824, 10950, 824, 8834, 8402, 8809, 65024, 8808, 65024, 10878, 824, 10877, 824, 10861, 824, 8949, 824, 8801, 8421, 10927, 824, 8834, 8402, 10928, 824, 8835, 8402, 8884, 8402, 8885, 8402, 8783, 824, 8953, 824, 11005, 8421, 10547, 824, 8605, 824, 8851, 65024, 8852, 65024, 10955, 65024, 8842, 65024, 10956, 65024, 8843, 65024, 102, 106, 10925, 65024, 8779, 824, 8782, 824, 8784, 824, 8770, 824, 8807, 824, 8806, 824, 8706, 824, 10949, 824, 10950, 824, 8764, 8402, 10924, 65024, 8834, 8402, 8835, 8402, 8745, 65024, 8746, 65024, 8923, 65024, 8809, 65024, 8922, 65024, 8808, 65024, 8811, 824, 8810, 824, 8736, 8402, 10864, 824, 10878, 824, 10877, 824, 10927, 824, 10928, 824, 8781, 8402, 8805, 8402, 62, 8402, 8804, 8402, 60, 8402, 8765, 817, 8766, 819, 61, 8421, 8921, 824, 8811, 8402, 8920, 824, 8810, 8402, 8807, 824, 8806, 824};



	public static final String MATHML_NAMESPACE = "http://www.w3.org/1998/Math/MathML";

	public static final String SVG_NAMESPACE = "http://www.w3.org/2000/svg";


	static int TOKEN_EOF= 0x10000000;

	static int TOKEN_START_TAG= 0x20000000;

	static int TOKEN_END_TAG= 0x30000000;

	static int TOKEN_COMMENT=0x40000000;

	static int TOKEN_DOCTYPE=0x50000000;
	static int TOKEN_TYPE_MASK=0xF0000000;
	static int TOKEN_CHARACTER=0x00000000;
	static int TOKEN_INDEX_MASK=0x0FFFFFFF;
	static String HTML_NAMESPACE="http://www.w3.org/1999/xhtml";

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


	enum DocumentMode {
		NoQuirksMode,
		LimitedQuirksMode,
		QuirksMode
	}


	private final ConditionalBufferInputStream inputStream;
	private IMarkableCharacterInput stream=null;
	private EncodingConfidence encoding=null;


	private boolean error=false;
	private TokenizerState lastState=null;
	private CommentToken lastComment;
	private DocTypeToken docTypeToken;
	private final List<Element> integrationElements=new ArrayList<Element>();
	private final List<IToken> tokens=new ArrayList<IToken>();
	private TagToken lastStartTag=null;
	private Html5Decoder decoder=null;
	private TagToken currentEndTag=null;
	private TagToken currentTag=null;
	private Attribute currentAttribute=null;
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
	Document document=new Document();
	private boolean done=false;

	private final IntList pendingTableCharacters=new IntList();
	private boolean doFosterParent;
	private Element context;
	private boolean noforeign;

	private void initialize(){
		noforeign=false;
		document=new Document();
		context=null;
		openElements.clear();
		error=false;
		baseurl=null;
		hasForeignContent=false; // performance optimization
		lastState=null;
		lastComment=null;
		docTypeToken=null;
		tokens.clear();
		lastStartTag=null;
		currentEndTag=null;
		currentTag=null;
		currentAttribute=null;
		bogusCommentCharacter=0;
		tempBuffer.clear();
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
		pendingTableCharacters.clear();
	}

	public HtmlParser(InputStream source) throws IOException{
		if(source==null)throw new IllegalArgumentException();
		initialize();
		inputStream=new ConditionalBufferInputStream(source);
		encoding=CharsetSniffer.sniffEncoding(inputStream,null);
		inputStream.rewind();
		decoder=new Html5Decoder(TextEncoding.getDecoder(encoding.getEncoding()));
		stream=new StackableInputStream(new DecoderCharacterInput(inputStream,decoder));
	}
	private boolean isMathMLIntegrationPoint(Element element) {
		String name=element.getLocalName();
		return MATHML_NAMESPACE.equals(element.getNamespaceURI()) && (
				name.equals("mi") ||
				name.equals("mo") ||
				name.equals("mn") ||
				name.equals("ms") ||
				name.equals("mtext"));
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



	private boolean isForeignContext(int token){
		if(hasForeignContent && token!=TOKEN_EOF){
			Element element=getCurrentNode();
			if(element==null)return false;
			if(element.getNamespaceURI().equals(HTML_NAMESPACE))
				return false;
			if((token&TOKEN_TYPE_MASK)==TOKEN_START_TAG){
				StartTagToken tag=(StartTagToken)getToken(token);
				String name=element.getLocalName();
				if(isMathMLIntegrationPoint(element)){
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
				if(isMathMLIntegrationPoint(element) ||
						isHtmlIntegrationPoint(element))
					return false;
				return true;
			} else
				return true;
		}
		return false;
	}


	private Text getFosterParentedTextNode() {
		if(openElements.size()==0)return null;
		Node fosterParent=openElements.get(0);
		List<Node> childNodes;
		for(int i=openElements.size()-1;i>=0;i--){
			Element e=openElements.get(i);
			if(e.getLocalName().equals("table")){
				Node parent=e.getParentNode();
				boolean isElement=(parent!=null && parent.getNodeType()==NodeType.ELEMENT_NODE);
				if(!isElement){ // the parent is not an element
					if(i<=1)
						// This usually won't happen
						throw new IllegalStateException();
					// append to the element before this table
					fosterParent=openElements.get(i-1);
					break;
				} else {
					// Parent of the table, insert before the table
					childNodes=parent.getChildNodesInternal();
					if(childNodes.size()==0)
						throw new IllegalStateException();
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
					throw new IllegalStateException();
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

	private Comment createCommentNode(int token){
		CommentToken comment=(CommentToken)getToken(token);
		Comment node=new Comment();
		node.setOwnerDocument(document);
		node.setData(comment.getValue());
		return node;
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

	private Element addHtmlElementNoPush(StartTagToken tag){
		Element element=Element.fromToken(tag);
		Element currentNode=getCurrentNode();
		if(currentNode!=null) {
			insertInCurrentNode(element);
		}
		return element;
	}

	private void insertCharacter(Node node, int ch){
		Text textNode=getTextNodeToInsert(node);
		if(textNode!=null) {
			textNode.text.append(ch);
		}
	}

	private void insertString(Node node, String str){
		Text textNode=getTextNodeToInsert(node);
		if(textNode!=null) {
			textNode.text.append(str);
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
								isMathMLIntegrationPoint(node) ||
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
				while(true){
					popCurrentNode();
					Element node=getCurrentNode();
					if(node.getNamespaceURI().equals(HTML_NAMESPACE) ||
							isMathMLIntegrationPoint(node) ||
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
								encoding.equals("application/xml")){
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
		if(!noforeign && isForeignContext(token))
			return applyForeignContext(token);
		noforeign=false;
		if(insMode==null) {
			insMode=insertionMode;
		}
		//	DebugUtility.log("[[%08X %s %s",token,getToken(token),insMode);
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
			error=true; // except if 'iframe srcdoc'
			document.setMode(DocumentMode.QuirksMode); // except if 'iframe srcdoc'
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
			element.setName("html");
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
						String value=element.getAttribute("http-equiv");
						if(value!=null && StringUtility.toLowerCaseAscii(value).equals("content-type")){
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
						throw new IllegalStateException();
					while(true){
						textNode.text.append(ch);
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
				reconstructFormatting();
				Text textNode=getTextNodeToInsert(getCurrentNode());
				int ch=token;
				if(textNode==null)
					throw new IllegalStateException();
				while(true){
					// Read multiple characters at once
					textNode.text.append(ch);
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
					Node parent=openElements.get(1).getParentNode();
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
					//
					// For convenience, read the next
					// character directly rather than
					// use the tokenizer
					//
					int mark=stream.markIfNeeded();
					int nextToken=stream.read();
					if(nextToken!=0x0a){
						// ignore the token if it's 0x0A (LF);
						// otherwise reset the input stream
						stream.setMarkPosition(mark);
					}

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
						Element aElement=null;
						for(int i=formattingElements.size()-1; i>=0; i--){
							FormattingElement fe=formattingElements.get(i);
							if(fe.isMarker()) {
								break;
							}
							if(fe.element.getLocalName().equals("a")){
								aElement=fe.element;
								break;
							}
						}
						if(aElement!=null){
							error=true;
							applyEndTag("a",insMode);
							removeFormattingElement(aElement);
							openElements.remove(aElement);
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
					for(Attribute attr : tag.getAttributes()){
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
					addHtmlElement(tag);					//
					// For convenience, read the next
					// character directly rather than
					// use the tokenizer
					//
					int mark=stream.markIfNeeded();
					int nextToken=stream.read();
					if(nextToken!=0x0a){
						// ignore the token if it's 0x0A (LF);
						// otherwise reset the input stream
						stream.setMarkPosition(mark);
					}

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
						Element node=furthestBlock;
						Element superiorNode=openElements.get(furthestBlockPos-1);
						Element lastNode=furthestBlock;
						for(int j=0;j<3;j++){
							node=superiorNode;
							FormattingElement nodeFE=getFormattingElement(node);
							if(nodeFE==null){
								//	DebugUtility.log("node not a formatting element");
								superiorNode=openElements.get(openElements.indexOf(node)-1);
								openElements.remove(node);
								continue;
							} else if(node.equals(formatting.element)){
								//	DebugUtility.log("node is the formatting element");
								break;
							}
							Element e=Element.fromToken(nodeFE.token);
							nodeFE.element=e;
							int io=openElements.indexOf(node);
							superiorNode=openElements.get(io-1);
							openElements.set(io,e);
							node=e;
							if(lastNode.equals(furthestBlock)){
								bookmark=formattingElements.indexOf(nodeFE)+1;
							}
							// NOTE: Because 'node' can only be a formatting
							// element, the foster parenting rule doesn't
							// apply here
							if(lastNode.getParentNode()!=null) {
								lastNode.getParentNode().removeChild(lastNode);
							}
							node.appendChild(lastNode);
							lastNode=node;
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
								lastNode.getParentNode().removeChild(lastNode);
							}
							fosterParent(lastNode);
						} else {
							if(lastNode.getParentNode()!=null) {
								lastNode.getParentNode().removeChild(lastNode);
							}
							commonAncestor.appendChild(lastNode);
						}
						Element e=Element.fromToken(formatting.token);
						for(Node child : new ArrayList<Node>(furthestBlock.getChildNodesInternal())){
							furthestBlock.removeChild(child);
							// NOTE: Because 'e' can only be a formatting
							// element, the foster parenting rule doesn't
							// apply here
							e.appendChild(child);
						}
						// NOTE: Because intervening elements, including
						// formatting elements, are cleared between table
						// and tbody/thead/tfoot and between those three
						// elements and tr, the foster parenting rule
						// doesn't apply here
						furthestBlock.appendChild(e);
						FormattingElement newFE=new FormattingElement();
						newFE.marker=false;
						newFE.element=e;
						newFE.token=formatting.token;
						//	DebugUtility.log("Adding formatting element at %d",bookmark);
						formattingElements.add(bookmark,newFE);
						formattingElements.remove(formatting);
						//	DebugUtility.log("Replacing open element at %d",openElements.indexOf(furthestBlock)+1);
						openElements.add(openElements.indexOf(furthestBlock)+1,e);
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
					pendingTableCharacters.clear();
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
					pendingTableCharacters.append(token);
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
					applyEndTag("tr",insMode);
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
			throw new IllegalStateException();
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
		stream=new StackableInputStream(new DecoderCharacterInput(inputStream,decoder));
	}

	private void clearFormattingToMarker() {
		while(formattingElements.size()>0){
			FormattingElement fe=formattingElements.remove(formattingElements.size()-1);
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

	private void fosterParent(Node element) {
		if(openElements.size()==0)return;
		Node fosterParent=openElements.get(0);
		for(int i=openElements.size()-1;i>=0;i--){
			Element e=openElements.get(i);
			if(e.getLocalName().equals("table")){
				Node parent=e.getParentNode();
				boolean isElement=(parent!=null && parent.getNodeType()==NodeType.ELEMENT_NODE);
				if(!isElement){ // the parent is not an element
					if(i<=1)
						// This usually won't happen
						throw new IllegalStateException();
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

	private void insertFormattingMarker(StartTagToken tag,
			Element addHtmlElement) {
		FormattingElement fe=new FormattingElement();
		fe.marker=true;
		fe.element=addHtmlElement;
		fe.token=tag;
		formattingElements.add(fe);
	}

	private boolean isAppropriateEndTag(){
		if(lastStartTag==null || currentEndTag==null)
			return false;
		//DebugUtility.log("lastStartTag=%s",lastStartTag.getName());
		//DebugUtility.log("currentEndTag=%s",currentEndTag.getName());
		return currentEndTag.getName().equals(lastStartTag.getName());
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

	private int parseCharacterReference(int allowedCharacter) throws IOException{
		int markStart=stream.markIfNeeded();
		int c1=stream.read();
		if(c1<0 || c1==0x09 || c1==0x0a || c1==0x0c || 
				c1==0x20 || c1==0x3c || c1==0x26 || (allowedCharacter>=0 && c1==allowedCharacter)){
			stream.setMarkPosition(markStart);
			return 0x26; // emit ampersand
		} else if(c1==0x23){
			c1=stream.read();
			int value=0;
			boolean haveHex=false;
			if(c1==0x78 || c1==0x58){
				// Hex number
				while(true){ // skip zeros
					int c=stream.read();
					if(c!='0'){
						if(c>=0) {
							stream.moveBack(1);
						}
						break;
					}
					haveHex=true;
				}
				boolean overflow=false;
				while(true){
					int number=stream.read();
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
							stream.moveBack(1);
						}
						break;
					}
					if(value>0x10FFFF){
						value=0x110000; overflow=true;
					}
				}
			} else {
				if(c1>0) {
					stream.moveBack(1);
				}
				// Digits
				while(true){ // skip zeros
					int c=stream.read();
					if(c!='0'){
						if(c>=0) {
							stream.moveBack(1);
						}
						break;
					}
					haveHex=true;
				}
				boolean overflow=false;
				while(true){
					int number=stream.read();
					if(number>='0' && number<='9'){
						if(!overflow) {
							value=(value*10)+(number-'0');
						}
						haveHex=true;
					} else {
						if(number>=0) {
							stream.moveBack(1);
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
				stream.setMarkPosition(markStart);
				return 0x26; // emit ampersand
			}
			c1=stream.read();
			if(c1!=0x3B){ // semicolon
				error=true;
				stream.moveBack(1); // parse error
			}
			if(value>0x10FFFF || (value>=0xD800 && value<=0xDFFF)){
				error=true;
				value=0xFFFD; // parse error
			} else if(value>=0x80 && value<0xA0){
				error=true;
				// parse error
				int replacements[]={
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
				if(stream.read()=='t' && stream.read()==';')
					return '>';
				stream.setMarkPosition(markStart+1);
			} else if(c1=='l'){
				if(stream.read()=='t' && stream.read()==';')
					return '<';
				stream.setMarkPosition(markStart+1);
			} else if(c1=='a'){
				if(stream.read()=='m' && stream.read()=='p' && stream.read()==';')
					return '&';
				stream.setMarkPosition(markStart+1);
			} else if(c1=='n'){
				if(stream.read()=='b' && stream.read()=='s' && stream.read()=='p' && stream.read()==';')
					return 0xa0;
				stream.setMarkPosition(markStart+1);
			}
			int count=0;
			for(int index=0;index<entities.length;index++){
				String entity=entities[index];
				if(entity.charAt(0)==c1){
					if(data==null){
						// Read the rest of the character reference
						// (the entities are sorted by length, so
						// we get the maximum length possible starting
						// with the first matching character)
						data=new int[entity.length()-1];
						count=stream.read(data,0,data.length);
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
						stream.moveBack(count-(entity.length()-1));
						//DebugUtility.log("lastchar=%c",entity.charAt(entity.length()-1));
						if(allowedCharacter>=0 &&
								entity.charAt(entity.length()-1)!=';'){
							// Get the next character after the entity
							int ch2=stream.read();
							if(ch2=='=' || (ch2>='A' && ch2<='Z') || 
									(ch2>='a' && ch2<='z') ||
									(ch2>='0' && ch2<='9')){
								if(ch2=='=') {
									error=true;
								}
								stream.setMarkPosition(markStart);
								return 0x26; // return ampersand rather than entity
							} else {
								stream.moveBack(1);
								if(entity.charAt(entity.length()-1)!=';'){
									error=true;
								}
							}
						} else {
							if(entity.charAt(entity.length()-1)!=';'){
								error=true;
							}
						}
						return entityValues[index];
					}
				}
			}
			// no match
			stream.setMarkPosition(markStart);
			while(true){
				int ch2=stream.read();
				if(ch2==';'){
					error=true;
					break;
				} else if(!((ch2>='A' && ch2<='Z') || 
						(ch2>='a' && ch2<='z') ||
						(ch2>='0' && ch2<='9'))){
					break;
				}
			}
			stream.setMarkPosition(markStart);
			return 0x26;
		} else {
			// not a character reference
			stream.setMarkPosition(markStart);
			return 0x26; // emit ampersand
		}
	}


	private void adjustSvgAttributes(StartTagToken token){
		List<Attribute> attributes=token.getAttributes();
		for(Attribute attr : attributes){
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
	private void adjustMathMLAttributes(StartTagToken token){
		List<Attribute> attributes=token.getAttributes();
		for(Attribute attr : attributes){
			if(attr.getName().equals("definitionurl")){
				attr.setName("definitionURL");
			}
		}
	}

	static final String XLINK_NAMESPACE="http://www.w3.org/1999/xlink";
	static final String XML_NAMESPACE="http://www.w3.org/XML/1998/namespace";
	private static final String XMLNS_NAMESPACE="http://www.w3.org/2000/xmlns/";

	private void adjustForeignAttributes(StartTagToken token){
		List<Attribute> attributes=token.getAttributes();
		for(Attribute attr : attributes){
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
			return tokenQueue.remove(0);
		while(true){
			switch(state){
			case Data:
				int c=stream.read();
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
					int mark=stream.markIfNeeded();
					for(int i=0;i<100;i++){
						c=stream.read();
						if(c>0 && c!=0x26 && c!=0x3c){
							tokenQueue.add(c);
						} else {
							stream.setMarkPosition(mark+i);
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
					tokenQueue.add(entityDoubles[index*2+1]);
					return entityDoubles[index*2];
				}
				return charref;
			}
			case CharacterRefInRcData:{
				state=TokenizerState.RcData;
				int charref=parseCharacterReference(-1);
				if(charref<0){
					// more than one character in this reference
					int index=Math.abs(charref+1);
					tokenQueue.add(entityDoubles[index*2+1]);
					return entityDoubles[index*2];
				}
				return charref;
			}
			case RcData:
				int c1=stream.read();
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
				int c11=stream.read();
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
				stream.markToEnd();
				int c11=stream.read();
				if(c11==0x2f){
					tempBuffer.clear();
					state=TokenizerState.ScriptDataEndTagOpen;
				} else if(c11==0x21){
					state=TokenizerState.ScriptDataEscapeStart;
					tokenQueue.add(0x21);
					return '<';
				} else {
					state=TokenizerState.ScriptData;
					if(c11>=0) {
						stream.moveBack(1);
					}
					return 0x3c;
				}
				break;
			}
			case ScriptDataEndTagOpen:
			case ScriptDataEscapedEndTagOpen:{
				stream.markToEnd();
				int ch=stream.read();
				if(ch>='A' && ch<='Z'){
					EndTagToken token=new EndTagToken((char) (ch+0x20));
					tempBuffer.append(ch);
					currentTag=token;
					currentEndTag=token;
					if(state==TokenizerState.ScriptDataEndTagOpen) {
						state=TokenizerState.ScriptDataEndTagName;
					} else {
						state=TokenizerState.ScriptDataEscapedEndTagName;
					}						
				} else if(ch>='a' && ch<='z'){
					EndTagToken token=new EndTagToken((char)ch);
					tempBuffer.append(ch);
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
						stream.moveBack(1);
					}
					return 0x3c;					
				}
				break;
			}
			case ScriptDataEndTagName:
			case ScriptDataEscapedEndTagName:{
				stream.markToEnd();
				int ch=stream.read();
				if((ch==0x09 || ch==0x0a || ch==0x0c || ch==0x20) &&
						isAppropriateEndTag()){
					state=TokenizerState.BeforeAttributeName;
				} else if(ch==0x2f && isAppropriateEndTag()){
					state=TokenizerState.SelfClosingStartTag;
				} else if(ch==0x3e && isAppropriateEndTag()){
					state=TokenizerState.Data;
					return emitCurrentTag();
				} else if(ch>='A' && ch<='Z'){
					currentTag.append((char) (ch+0x20));
					tempBuffer.append(ch);
				} else if(ch>='a' && ch<='z'){
					currentTag.append((char)ch);
					tempBuffer.append(ch);				
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
						stream.moveBack(1);
					}
					return '<';
				}
				break;
			}
			case ScriptDataDoubleEscapeStart:{
				stream.markToEnd();
				int ch=stream.read();
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
					tempBuffer.append(ch+0x20);
					return ch;
				} else if(ch>='a' && ch<='z'){
					tempBuffer.append(ch);
					return ch;
				} else {
					state=TokenizerState.ScriptDataEscaped;
					if(ch>=0) {
						stream.moveBack(1);
					}
				}
				break;
			}
			case ScriptDataDoubleEscapeEnd:{
				stream.markToEnd();
				int ch=stream.read();
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
					tempBuffer.append(ch+0x20);
					return ch;
				} else if(ch>='a' && ch<='z'){
					tempBuffer.append(ch);
					return ch;
				} else {
					state=TokenizerState.ScriptDataDoubleEscaped;
					if(ch>=0) {
						stream.moveBack(1);
					}
				}
				break;
			}
			case ScriptDataEscapeStart:
			case ScriptDataEscapeStartDash:{
				stream.markToEnd();
				int ch=stream.read();
				if(ch==0x2d){
					if(state==TokenizerState.ScriptDataEscapeStart) {
						state=TokenizerState.ScriptDataEscapeStartDash;
					} else {
						state=TokenizerState.ScriptDataEscapedDashDash;
					}
					return '-';
				} else {
					if(ch>=0) {
						stream.moveBack(1);
					}
					state=TokenizerState.ScriptData;
				}
				break;
			}
			case ScriptDataEscaped:{
				int ch=stream.read();
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
				int ch=stream.read();
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
				int ch=stream.read();
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
				int ch=stream.read();
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
				int ch=stream.read();
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
				int ch=stream.read();
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
				stream.markToEnd();
				int ch=stream.read();
				if(ch==0x2f){
					tempBuffer.clear();
					state=TokenizerState.ScriptDataDoubleEscapeEnd;
				} else {
					state=TokenizerState.ScriptDataDoubleEscaped;
					if(ch>=0) {
						stream.moveBack(1);
					}
				}
				break;
			}
			case ScriptDataEscapedLessThan:{
				stream.markToEnd();
				int ch=stream.read();
				if(ch==0x2f){
					tempBuffer.clear();
					state=TokenizerState.ScriptDataEscapedEndTagOpen;
				} else if(ch>='A' && ch<='Z'){
					tempBuffer.clear();
					tempBuffer.append(ch+0x20);
					state=TokenizerState.ScriptDataDoubleEscapeStart;
					tokenQueue.add(ch);
					return 0x3c;
				} else if(ch>='a' && ch<='z'){
					tempBuffer.clear();
					tempBuffer.append(ch);
					state=TokenizerState.ScriptDataDoubleEscapeStart;
					tokenQueue.add(ch);
					return 0x3c;
				} else {
					state=TokenizerState.ScriptDataEscaped;
					if(ch>=0) {
						stream.moveBack(1);
					}
					return 0x3c;
				}
				break;
			}
			case PlainText:{
				int c11=stream.read();
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
				stream.markToEnd();
				int c11=stream.read();
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
						stream.moveBack(1);
					}
					return '<';
				}
				break;
			}
			case EndTagOpen:{
				int ch=stream.read();
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
				stream.markToEnd();
				int ch=stream.read();
				if(ch>='A' && ch<='Z'){
					TagToken token=new EndTagToken((char) (ch+0x20));
					tempBuffer.append(ch);
					currentEndTag=token;
					currentTag=token;
					state=(state==TokenizerState.RcDataEndTagOpen) ?
							TokenizerState.RcDataEndTagName :
								TokenizerState.RawTextEndTagName;
				}
				else if(ch>='a' && ch<='z'){
					TagToken token=new EndTagToken((char) (ch));
					tempBuffer.append(ch);
					currentEndTag=token;
					currentTag=token;
					state=(state==TokenizerState.RcDataEndTagOpen) ?
							TokenizerState.RcDataEndTagName :
								TokenizerState.RawTextEndTagName;
				}
				else {
					if(ch>=0) {
						stream.moveBack(1);
					}
					state=TokenizerState.RcData;
					tokenQueue.add(0x2F); // solidus
					return 0x3C; // Less than
				}
				break;
			}
			case RcDataEndTagName:
			case RawTextEndTagName:{
				stream.markToEnd();
				int ch=stream.read();
				if((ch==0x09 || ch==0x0a || ch==0x0c || ch==0x20) && isAppropriateEndTag()){
					state=TokenizerState.BeforeAttributeName;
				} else if(ch==0x2f && isAppropriateEndTag()){
					state=TokenizerState.SelfClosingStartTag;
				} else if(ch==0x3e && isAppropriateEndTag()){
					state=TokenizerState.Data;
					return emitCurrentTag();
				} else if(ch>='A' && ch<='Z'){
					currentTag.append(ch+0x20);
					tempBuffer.append(ch+0x20);
				} else if(ch>='a' && ch<='z'){
					currentTag.append(ch);
					tempBuffer.append(ch);
				} else {
					if(ch>=0) {
						stream.moveBack(1);
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
				int ch=stream.read();
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
				int ch=stream.read();
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
				int ch=stream.read();
				while(ch==0x09 || ch==0x0a || ch==0x0c || ch==0x20){
					ch=stream.read();
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
				stream.markToEnd();
				int ch=stream.read();
				while(ch==0x09 || ch==0x0a || ch==0x0c || ch==0x20){
					ch=stream.read();
				}
				if(ch==0x22){
					state=TokenizerState.AttributeValueDoubleQuoted;
				} else if(ch==0x26){
					stream.moveBack(1);
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
				int ch=stream.read();
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
					int mark=stream.markIfNeeded();
					for(int i=0;i<100;i++){
						ch=stream.read();
						if(ch>0 && ch!=0x26 && ch!=0x22){
							currentAttribute.appendToValue(ch);
						} else if(ch==0x22){
							currentAttribute.commitValue();
							state=TokenizerState.AfterAttributeValueQuoted;
							break;
						} else {
							stream.setMarkPosition(mark+i);
							break;
						}
					}
				}
				break;
			}
			case AttributeValueSingleQuoted:{
				int ch=stream.read();
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
					int mark=stream.markIfNeeded();
					for(int i=0;i<100;i++){
						ch=stream.read();
						if(ch>0 && ch!=0x26 && ch!=0x27){
							currentAttribute.appendToValue(ch);
						} else if(ch==0x27){
							currentAttribute.commitValue();
							state=TokenizerState.AfterAttributeValueQuoted;
							break;
						} else {
							stream.setMarkPosition(mark+i);
							break;
						}
					}
				}
				break;
			}
			case AttributeValueUnquoted:{
				int ch=stream.read();
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
				int mark=stream.markIfNeeded();
				int ch=stream.read();
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
					stream.setMarkPosition(mark);
				}
				break;
			}
			case SelfClosingStartTag:{
				int mark=stream.markIfNeeded();
				int ch=stream.read();
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
					stream.setMarkPosition(mark);
				}
				break;
			}
			case MarkupDeclarationOpen:{
				int mark=stream.markIfNeeded();
				int ch=stream.read();
				if(ch=='-' && stream.read()=='-'){
					CommentToken token=new CommentToken();
					lastComment=token;
					state=TokenizerState.CommentStart;
					break;
				} else if(ch=='D' || ch=='d'){
					if(((ch=stream.read())=='o' || ch=='O') &&
							((ch=stream.read())=='c' || ch=='C') &&
							((ch=stream.read())=='t' || ch=='T') &&
							((ch=stream.read())=='y' || ch=='Y') &&
							((ch=stream.read())=='p' || ch=='P') &&
							((ch=stream.read())=='e' || ch=='E')){
						state=TokenizerState.DocType;
						break;
					}			
				} else if(ch=='[' && true){
					if(stream.read()=='C' &&
							stream.read()=='D' &&
							stream.read()=='A' &&
							stream.read()=='T' &&
							stream.read()=='A' &&
							stream.read()=='[' &&
							getCurrentNode()!=null &&
							HTML_NAMESPACE.equals(getCurrentNode().getNamespaceURI())
							){
						state=TokenizerState.CData;
						break;
					}
				}
				error=true;
				stream.setMarkPosition(mark);
				bogusCommentCharacter=-1;
				state=TokenizerState.BogusComment;
				break;
			}
			case CommentStart:{
				int ch=stream.read();
				if(ch=='-'){
					state=TokenizerState.CommentStartDash;
				} else if(ch==0){
					error=true;
					lastComment.append(0xFFFD);
					state=TokenizerState.Comment;
				} else if(ch==0x3e || ch<0){
					error=true;
					state=TokenizerState.Data;					
					int ret=tokens.size()|lastComment.getType();
					tokens.add(lastComment);
					return ret;
				} else {
					lastComment.append(ch);
					state=TokenizerState.Comment;
				}
				break;
			}
			case CommentStartDash:{
				int ch=stream.read();
				if(ch=='-'){
					state=TokenizerState.CommentEnd;
				} else if(ch==0){
					error=true;
					lastComment.append('-');
					lastComment.append(0xFFFD);
					state=TokenizerState.Comment;
				} else if(ch==0x3e || ch<0){
					error=true;
					state=TokenizerState.Data;					
					int ret=tokens.size()|lastComment.getType();
					tokens.add(lastComment);
					return ret;
				} else {
					lastComment.append('-');
					lastComment.append(ch);
					state=TokenizerState.Comment;
				}
				break;
			}
			case Comment:{
				int ch=stream.read();
				if(ch=='-'){
					state=TokenizerState.CommentEndDash;
				} else if(ch==0){
					error=true;
					lastComment.append(0xFFFD);
				} else if(ch<0){
					error=true;
					state=TokenizerState.Data;					
					int ret=tokens.size()|lastComment.getType();
					tokens.add(lastComment);
					return ret;
				} else {
					lastComment.append(ch);
				}
				break;
			}
			case CommentEndDash:{
				int ch=stream.read();
				if(ch=='-'){
					state=TokenizerState.CommentEnd;
				} else if(ch==0){
					error=true;
					lastComment.append('-');
					lastComment.append(0xFFFD);
					state=TokenizerState.Comment;
				} else if(ch<0){
					error=true;
					state=TokenizerState.Data;					
					int ret=tokens.size()|lastComment.getType();
					tokens.add(lastComment);
					return ret;
				} else {
					lastComment.append('-');
					lastComment.append(ch);
					state=TokenizerState.Comment;
				}
				break;
			}
			case CommentEnd:{
				int ch=stream.read();
				if(ch==0x3e){
					state=TokenizerState.Data;
					int ret=tokens.size()|lastComment.getType();
					tokens.add(lastComment);
					return ret;
				} else if(ch==0){
					error=true;
					lastComment.append('-');
					lastComment.append('-');
					lastComment.append(0xFFFD);
					state=TokenizerState.Comment;
				} else if(ch==0x21){ // --!>
					error=true;
					state=TokenizerState.CommentEndBang;
				} else if(ch==0x2D){
					error=true;
					lastComment.append('-');
				} else if(ch<0){
					error=true;
					state=TokenizerState.Data;					
					int ret=tokens.size()|lastComment.getType();
					tokens.add(lastComment);
					return ret;
				} else {
					error=true;
					lastComment.append('-');
					lastComment.append('-');
					lastComment.append(ch);
					state=TokenizerState.Comment;
				}
				break;
			}
			case CommentEndBang:{
				int ch=stream.read();
				if(ch==0x3e){
					state=TokenizerState.Data;
					int ret=tokens.size()|lastComment.getType();
					tokens.add(lastComment);
					return ret;
				} else if(ch==0){
					error=true;
					lastComment.append('-');
					lastComment.append('-');
					lastComment.append('!');
					lastComment.append(0xFFFD);
					state=TokenizerState.Comment;
				} else if(ch==0x2D){
					lastComment.append('-');
					lastComment.append('-');
					lastComment.append('!');
					state=TokenizerState.CommentEndDash;
				} else if(ch<0){
					error=true;
					state=TokenizerState.Data;					
					int ret=tokens.size()|lastComment.getType();
					tokens.add(lastComment);
					return ret;
				} else {
					error=true;
					lastComment.append('-');
					lastComment.append('-');
					lastComment.append('!');
					lastComment.append(ch);
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
					currentAttribute.appendToValue(entityDoubles[index*2]);
					currentAttribute.appendToValue(entityDoubles[index*2+1]);
				} else {
					currentAttribute.appendToValue(ch);					
				}
				state=lastState;
				break;
			}
			case TagName:{
				int ch=stream.read();
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
				stream.markToEnd();
				int ch=stream.read();
				if(ch==0x2f){
					tempBuffer.clear();
					state=TokenizerState.RawTextEndTagOpen;
				} else {
					state=TokenizerState.RawText;
					if(ch>=0) {
						stream.moveBack(1);
					}
					return 0x3c;
				}
				break;				
			}
			case BogusComment:{
				CommentToken comment=new CommentToken();
				if(bogusCommentCharacter>=0) {
					comment.append(bogusCommentCharacter==0 ? 0xFFFD : bogusCommentCharacter);
				}
				while(true){
					int ch=stream.read();
					if(ch<0 || ch=='>') {
						break;
					}
					if(ch==0) {
						ch=0xFFFD;
					}
					comment.append(ch);
				}
				int ret=tokens.size()|comment.getType();
				tokens.add(comment);
				state=TokenizerState.Data;
				return ret;
			}
			case DocType:{
				stream.markToEnd();
				int ch=stream.read();
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
					stream.moveBack(1);
					state=TokenizerState.BeforeDocTypeName;
				}
				break;
			}
			case BeforeDocTypeName:{
				int ch=stream.read();
				if(ch==0x09||ch==0x0a||ch==0x0c||ch==0x20){
					break;
				} else if(ch>='A' && ch<='Z'){
					docTypeToken=new DocTypeToken();
					docTypeToken.name=new IntList();
					docTypeToken.name.append(ch+0x20);
					state=TokenizerState.DocTypeName;
				} else if(ch==0){
					error=true;
					docTypeToken=new DocTypeToken();
					docTypeToken.name=new IntList();
					docTypeToken.name.append(0xFFFD);
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
					docTypeToken.name.append(ch);					
					state=TokenizerState.DocTypeName;					
				}
				break;
			}
			case DocTypeName:{
				int ch=stream.read();
				if(ch==0x09||ch==0x0a||ch==0x0c||ch==0x20){
					state=TokenizerState.AfterDocTypeName;
				} else if(ch==0x3e){
					state=TokenizerState.Data;
					int ret=tokens.size()|docTypeToken.getType();
					tokens.add(docTypeToken);
					return ret;
				} else if(ch>='A' && ch<='Z'){
					docTypeToken.name.append(ch+0x20);
				} else if(ch==0){
					error=true;
					docTypeToken.name.append(0xfffd);
				} else if(ch<0){
					error=true;
					docTypeToken.forceQuirks=true;
					state=TokenizerState.Data;
					int ret=tokens.size()|docTypeToken.getType();
					tokens.add(docTypeToken);
					return ret;					
				} else {
					docTypeToken.name.append(ch);					
				}
				break;
			}
			case AfterDocTypeName:{
				int ch=stream.read();
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
					int pos=stream.markIfNeeded();
					if(ch=='P' || ch=='p'){
						if(((ch2=stream.read())=='u' || ch2=='U') && 
								((ch2=stream.read())=='b' || ch2=='B') &&  
								((ch2=stream.read())=='l' || ch2=='L') && 
								((ch2=stream.read())=='i' || ch2=='I') && 
								((ch2=stream.read())=='c' || ch2=='C') 
								){
							state=TokenizerState.AfterDocTypePublic;
						} else {
							error=true;
							stream.setMarkPosition(pos);
							docTypeToken.forceQuirks=true;
							state=TokenizerState.BogusDocType;
						}
					} else if(ch=='S' || ch=='s'){
						if(((ch2=stream.read())=='y' || ch2=='Y') && 
								((ch2=stream.read())=='s' || ch2=='S') &&  
								((ch2=stream.read())=='t' || ch2=='T') && 
								((ch2=stream.read())=='e' || ch2=='E') && 
								((ch2=stream.read())=='m' || ch2=='M') 
								){
							state=TokenizerState.AfterDocTypeSystem;
						} else {
							error=true;
							stream.setMarkPosition(pos);
							docTypeToken.forceQuirks=true;
							state=TokenizerState.BogusDocType;
						}						
					} else {
						error=true;
						stream.setMarkPosition(pos);
						docTypeToken.forceQuirks=true;
						state=TokenizerState.BogusDocType;
					}	
				}
				break;
			}
			case AfterDocTypePublic:
			case BeforeDocTypePublicID:{
				int ch=stream.read();
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
				int ch=stream.read();
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
				int ch=stream.read();
				if(ch==(state==TokenizerState.DocTypePublicIDDoubleQuoted ? 0x22 : 0x27)){
					state=TokenizerState.AfterDocTypePublicID;
				} else if(ch==0){
					error=true;
					docTypeToken.publicID.append(0xFFFD);
				} else if(ch==0x3e || ch<0){
					error=true;
					docTypeToken.forceQuirks=true;
					state=TokenizerState.Data;
					int ret=tokens.size()|docTypeToken.getType();
					tokens.add(docTypeToken);
					return ret;					
				} else {
					docTypeToken.publicID.append(ch);
				}
				break;
			}
			case DocTypeSystemIDDoubleQuoted:
			case DocTypeSystemIDSingleQuoted:{
				int ch=stream.read();
				if(ch==(state==TokenizerState.DocTypeSystemIDDoubleQuoted ? 0x22 : 0x27)){
					state=TokenizerState.AfterDocTypeSystemID;
				} else if(ch==0){
					error=true;
					docTypeToken.systemID.append(0xFFFD);
				} else if(ch==0x3e || ch<0){
					error=true;
					docTypeToken.forceQuirks=true;
					state=TokenizerState.Data;
					int ret=tokens.size()|docTypeToken.getType();
					tokens.add(docTypeToken);
					return ret;					
				} else {
					docTypeToken.systemID.append(ch);
				}
				break;
			}
			case AfterDocTypePublicID:
			case BetweenDocTypePublicAndSystem:{
				int ch=stream.read();
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
				int ch=stream.read();
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
				int ch=stream.read();
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
					int ch=stream.read();
					if(ch<0) {
						break;
					}
					buffer.append(ch);
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
				stream.markToEnd();
				int ch=stream.read();
				if(ch==0x2f){
					tempBuffer.clear();
					state=TokenizerState.RcDataEndTagOpen;
				} else {
					state=TokenizerState.RcData;
					if(ch>=0) {
						stream.moveBack(1);
					}
					return 0x3c;
				}
				break;
			}
			default:
				throw new IllegalStateException();
			}
		}
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

	private Element popCurrentNode(){
		if(openElements.size()>0)
			return openElements.remove(openElements.size()-1);
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
				List<Attribute> attribs=fe.element.getAttributes();
				List<Attribute> myAttribs=element.getAttributes();
				if(attribs.size()==myAttribs.size()){
					boolean match=true;
					for(int j=0;j<myAttribs.size();j++){
						String name1=myAttribs.get(j).getName();
						String namespace=myAttribs.get(j).getNamespace();
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

	void setRcData(){
		state=TokenizerState.RcData;
	}
	void setPlainText(){
		state=TokenizerState.PlainText;
	}
	void setRawText(){
		state=TokenizerState.RawText;
	}
	void setCData(){
		state=TokenizerState.CData;
	}

	private void stopParsing() {
		done=true;
		document.encoding=encoding.getEncoding();
		document.baseurl=baseurl;
		openElements.clear();
		formattingElements.clear();
	}

	public boolean isError() {
		return error;
	}

	////////////////////////////////////////////////////

	String nodesToDebugString(List<Node> nodes){
		StringBuilder builder=new StringBuilder();
		for(Node node : nodes){
			String str=node.toDebugString();
			String[] strarray=str.split("\n");
			for(String el : strarray){
				builder.append("| ");
				builder.append(el.replace("~~~~","\n"));
				builder.append("\n");
			}
		}
		return builder.toString();
	}

	public List<Node> parseFragment(String contextName) throws IOException{
		Element element=new Element();
		element.setName(contextName);
		element.setNamespace(HTML_NAMESPACE);
		return parseFragment(element);
	}

	public List<Node> parseFragment(Element context) throws IOException{
		if(context==null)
			throw new IllegalArgumentException();
		initialize();
		document=new Document();
		Node ownerDocument=context;
		Node lastForm=null;
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
		String name=context.getLocalName();
		state=TokenizerState.Data;
		if(name.equals("title")||name.equals("textarea")){
			state=TokenizerState.RcData;
		} else if(name.equals("style") || name.equals("xmp") ||
				name.equals("iframe") || name.equals("noembed") ||
				name.equals("noframes")){
			state=TokenizerState.RawText;
		} else if(name.equals("script")){
			state=TokenizerState.ScriptData;
		} else if(name.equals("noscript")){
			state=TokenizerState.Data;
		} else if(name.equals("plaintext")){
			state=TokenizerState.PlainText;
		}
		Element element=new Element();
		element.setName("html");
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


}
