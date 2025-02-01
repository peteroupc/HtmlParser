package com.upokecenter.html;
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

import java.util.*;

import com.upokecenter.io.*;
import com.upokecenter.net.*;
import com.upokecenter.util.*;
import com.upokecenter.util.*;
import com.upokecenter.text.*;

  final class HtmlParser {
    public class CommentToken implements IToken {
      public final StringBuilder getCommentValue() { return propVarcommentvalue; }
public final void setCommentValue(StringBuilder value) { propVarcommentvalue = value; }
private StringBuilder propVarcommentvalue;

      public CommentToken() {
        this.setCommentValue(new StringBuilder());
      }

      public void AppendStr(String str) {
        this.getCommentValue().append(str);
      }

      public void AppendChar(int ch) {
        if (ch <= 0xffff) {
          this.getCommentValue().append((char)ch);
        } else if (ch <= 0x10ffff) {
          this.getCommentValue().append((char)((((ch - 0x10000) >> 10) & 0x3ff) |
            0xd800));
          this.getCommentValue().append((char)(((ch - 0x10000) & 0x3ff) | 0xdc00));
        }
      }

      public int GetTokenType() {
        return TOKEN_COMMENT;
      }
    }

    static class DocTypeToken implements IToken {
      public final StringBuilder getName() { return propVarname; }
private final StringBuilder propVarname;

      public final StringBuilder getValuePublicID() { return propVarvaluepublicid; }
private final StringBuilder propVarvaluepublicid;

      public final StringBuilder getValueSystemID() { return propVarvaluesystemid; }
private final StringBuilder propVarvaluesystemid;

      public DocTypeToken() {
 this("", "", "");
      }

      public DocTypeToken(String name, String pid, String sid) {
        this.propVarname = new StringBuilder().append(name);
        this.propVarvaluepublicid = new StringBuilder().append(pid);
        this.propVarvaluesystemid = new StringBuilder().append(sid);
      }

      public final boolean getForceQuirks() { return propVarforcequirks; }
public final void setForceQuirks(boolean value) { propVarforcequirks = value; }
private boolean propVarforcequirks;

      public int GetTokenType() {
        return TOKEN_DOCTYPE;
      }
    }

    static class EndTagToken extends TagToken {
      public EndTagToken(char c) {
 super(c);
      }

      public EndTagToken(String name) {
 super(name);
      }

      @Override public final int GetTokenType() {
        return HtmlParser.TOKEN_END_TAG;
      }
    }

    private static class FormattingElement {
      public final boolean getValueMarker() { return propVarvaluemarker; }
public final void setValueMarker(boolean value) { propVarvaluemarker = value; }
private boolean propVarvaluemarker;

      public final IElement getElement() { return propVarelement; }
public final void setElement(IElement value) { propVarelement = value; }
private IElement propVarelement;

      public final StartTagToken getToken() { return propVartoken; }
public final void setToken(StartTagToken value) { propVartoken = value; }
private StartTagToken propVartoken;

      public boolean IsMarker() {
        return this.getValueMarker();
      }

      @Override public final String toString() {
        return "FormattingElement [" + "ValueMarker" + "=" + this.getValueMarker() +
          ", Token" + "=" + this.getToken() + "]\n";
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
      InTemplate,
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
      AfterAfterFrameset,
    }

    interface IToken {
      int GetTokenType();
    }

    static class StartTagToken extends TagToken {
      public StartTagToken(char c) {
 super(c);
      }

      public StartTagToken(String name) {
 super(name);
      }

      @Override public final int GetTokenType() {
        return HtmlParser.TOKEN_START_TAG;
      }

      public void SetName(String stringValue) {
        this.builder.setLength(0);
        this.builder.append(stringValue);
      }
    }

    static abstract class TagToken implements IToken, INameAndAttributes {
      protected StringBuilder builder;

      public final List<Attr> getAttributes() { return propVarattributes; }
public final void setAttributes(List<Attr> value) { propVarattributes = value; }
private List<Attr> propVarattributes;

      public final boolean getSelfClosing() { return propVarselfclosing; }
public final void setSelfClosing(boolean value) { propVarselfclosing = value; }
private boolean propVarselfclosing;

      public final boolean getValueSelfClosingAck() { return propVarvalueselfclosingack; }
public final void setValueSelfClosingAck(boolean value) { propVarvalueselfclosingack = value; }
private boolean propVarvalueselfclosingack;

      public TagToken(char ch) {
        this.builder = new StringBuilder();
        this.builder.append(ch);
        this.setAttributes(null);
        this.setSelfClosing(false);
        this.setValueSelfClosingAck(false);
      }

      public TagToken(String valueName) {
        this.builder = new StringBuilder();
        this.builder.append(valueName);
      }

      public void AckSelfClosing() {
        this.setValueSelfClosingAck(true);
      }

      public Attr AddAttribute(char ch) {
        this.setAttributes((this.getAttributes() == null) ? (new ArrayList<Attr>()) : this.getAttributes());
        Attr a = new Attr(ch);
        this.getAttributes().add(a);
        return a;
      }

      public Attr AddAttribute(int ch) {
        this.setAttributes((this.getAttributes() == null) ? (new ArrayList<Attr>()) : this.getAttributes());
        Attr a = new Attr(ch);
        this.getAttributes().add(a);
        return a;
      }

      public void AppendUChar(int ch) {
        if (ch < 0x10000) {
          this.builder.append((char)ch);
        } else {
          ch -= 0x10000;
          int lead = (ch >> 10) + 0xd800;
          int trail = (ch & 0x3ff) + 0xdc00;
          this.builder.append((char)lead);
          this.builder.append((char)trail);
        }
      }

      public void AppendChar(char ch) {
        this.builder.append(ch);
      }

      public boolean CheckAttributeName() {
        if (this.getAttributes() == null) {
          return true;
        }
        int size = this.getAttributes().size();
        if (size >= 2) {
          String thisname = this.getAttributes().get(size - 1).GetName();
          for (int i = 0; i < size - 1; ++i) {
            if (this.getAttributes().get(i).GetName().equals(thisname)) {
              // Attribute with this valueName already exists;
              // remove it
              this.getAttributes().remove(size - 1);
              return false;
            }
          }
        }
        return true;
      }

      public String GetAttribute(String valueName) {
        if (this.getAttributes() == null) {
          return null;
        }
        int size = this.getAttributes().size();
        for (int i = 0; i < size; ++i) {
          IAttr a = this.getAttributes().get(i);
          String thisname = a.GetName();
          if (thisname.equals(valueName)) {
            return a.GetValue();
          }
        }
        return null;
      }

      public String GetAttributeNS(String valueName, String namespaceValue) {
        if (this.getAttributes() == null) {
          return null;
        }
        int size = this.getAttributes().size();
        for (int i = 0; i < size; ++i) {
          Attr a = this.getAttributes().get(i);
          if (a.IsAttribute(valueName, namespaceValue)) {
            return a.GetValue();
          }
        }
        return null;
      }

      public List<Attr> GetAttributes() {
        if (this.getAttributes() == null) {
          return new ArrayList<Attr>();
        } else {
          return this.getAttributes();
        }
      }

      public String GetName() {
        return this.builder.toString();
      }

      public abstract int GetTokenType();

      public boolean IsAckSelfClosing() {
        return !this.getSelfClosing() || this.getValueSelfClosingAck();
      }

      public boolean IsSelfClosing() {
        return this.getSelfClosing();
      }

      public boolean IsSelfClosingAck() {
        return this.getValueSelfClosingAck();
      }

      public void SetAttribute(String attrname, String value) {
        if (this.getAttributes() == null) {
          this.setAttributes(new ArrayList<Attr>());
          this.getAttributes().add(new Attr(attrname, value));
        } else {
          int size = this.getAttributes().size();
          for (int i = 0; i < size; ++i) {
            Attr a = this.getAttributes().get(i);
            String thisname = a.GetName();
            if (thisname.equals(attrname)) {
              a.SetValue(value);
              return;
            }
          }
          this.getAttributes().add(new Attr(attrname, value));
        }
      }

      public void SetSelfClosing(boolean SelfClosing) {
        this.setSelfClosing(SelfClosing);
      }

      @Override public final String toString() {
        return "TagToken [" + this.builder.toString() + ", " +
          this.getAttributes() + (this.getSelfClosing() ? (", ValueSelfClosingAck=" +
          this.getValueSelfClosingAck()) : "") + "]";
      }
    }

    private static final class Html5Encoding implements ICharacterEncoding {
      private ICharacterDecoder decoder;

      public Html5Encoding(EncodingConfidence ec) {
        ICharacterDecoder icd = ec == null ? null :
          Encodings.GetEncoding(ec.GetEncoding()).GetDecoder();
        this.decoder = new Html5Decoder(icd);
      }

      public ICharacterEncoder GetEncoder() {
        throw new UnsupportedOperationException();
      }

      public ICharacterDecoder GetDecoder() {
        return this.decoder;
      }
    }

    interface IInputStream {
      void Rewind();

      void DisableBuffer();
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
      CData,
    }

    private static final int TOKEN_EOF = 0x10000000;

    private static final int TOKEN_START_TAG = 0x20000000;

    private static final int TOKEN_END_TAG = 0x30000000;

    private static final int TOKEN_COMMENT = 0x40000000;

    private static final int TOKEN_DOCTYPE = 0x50000000;
    private static final int TOKEN_TYPE_MASK = ((int)0xf0000000);
    private static final int TOKEN_CHARACTER = 0x00000000;
    private static final int TOKEN_INDEX_MASK = 0x0fffffff;

    private boolean checkErrorVar = false;

    private void AddToken(IToken token) {
      if (this.tokens.size() > TOKEN_INDEX_MASK) {
        throw new IllegalStateException();
      }
      this.tokens.add(token);
    }

    private static String[] quirksModePublicIdPrefixes = new String[] {
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
      "-//softquad software//dtd hotmetal pro 6.0::" +
      "19990601::extensions to Html 4.0//",
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
      "-//webtechs//dtd mozilla html//",
    };

    private ConditionalBufferReader inputReader;
    private IMarkableCharacterInput charInput = null;
    private EncodingConfidence encoding = null;

    private boolean error = false;
    private TokenizerState lastState = TokenizerState.Data;
    private CommentToken lastComment;
    private DocTypeToken docTypeToken;
    private List<IElement> integrationElements = new ArrayList<IElement>();
    private List<IToken> tokens = new ArrayList<IToken>();
    private TagToken lastStartTag = null;
    private TagToken currentEndTag = null;
    private TagToken currentTag = null;
    private Attr currentAttribute = null;
    private int bogusCommentCharacter = 0;
    private StringBuilder tempBuilder = new StringBuilder();
    private TokenizerState state = TokenizerState.Data;
    private boolean framesetOk = true;
    private List<Integer> tokenQueue = new ArrayList<Integer>();
    private InsertionMode insertionMode = InsertionMode.Initial;
    private InsertionMode originalInsertionMode = InsertionMode.Initial;
    private List<InsertionMode> templateModes = new ArrayList<InsertionMode>();
    private List<IElement> openElements = new ArrayList<IElement>();
    private List<FormattingElement> formattingElements = new
    ArrayList<FormattingElement>();

    private IElement headElement = null;
    private IElement formElement = null;
    private IElement inputElement = null;
    private String baseurl = null;
    private Document valueDocument = null;
    private boolean done = false;

    private StringBuilder pendingTableCharacters = new StringBuilder();
    private boolean doFosterParent;
    private IElement context;
    private boolean noforeign;
    private String address;

    private String[] contentLanguage;

    private static <T> T RemoveAtIndex(List<T> array, int index) {
      T ret = array.get(index);
      array.remove(index);
      return ret;
    }

    public HtmlParser(IReader s, String address) {
 this(s, address, null, null);
    }

    public HtmlParser(
      IReader s,
      String address,
      String charset) {
 this(s, address, charset, null);
    }

    public HtmlParser(
      IReader source,
      String address,
      String charset,
      String contentLanguage) {
      if (source == null) {
        throw new IllegalArgumentException();
      }
      if (address != null && address.length() > 0) {
        /* URL url = URL.Parse(address);
                if (url == null || ((url.GetScheme()) == null || (url.GetScheme()).length() == 0)) {
                  throw new IllegalArgumentException();
                }
        */
      }
      // TODO: Use a more sophisticated language parser here
      this.contentLanguage = new String[] { contentLanguage };
      this.address = address;
      this.Initialize();
      this.inputReader = new ConditionalBufferReader(source); // TODO: ???
      this.encoding = new EncodingConfidence(
        charset,
        EncodingConfidence.Certain);
      // TODO: Use the following below
      // this.encoding = CharsetSniffer.sniffEncoding(this.inputReader,
      // charset);
      this.inputReader.Rewind();
      ICharacterEncoding henc = new Html5Encoding(this.encoding);
      this.charInput = new StackableCharacterInput(
        Encodings.GetDecoderInput(henc, this.inputReader));
    }

    private void AddCommentNodeToCurrentNode(int valueToken) {
      this.InsertInCurrentNode(this.CreateCommentNode(valueToken));
    }

    private void AddCommentNodeToDocument(int valueToken) {
      ((Document)this.valueDocument)
      .AppendChild(this.CreateCommentNode(valueToken));
    }

    private void AddCommentNodeToFirst(int valueToken) {
      ((Node)this.openElements.get(0))
      .AppendChild(this.CreateCommentNode(valueToken));
    }

    private Element AddHtmlElement(StartTagToken tag) {
      Element valueElement = Element.FromToken(tag);
      IElement currentNode = this.GetCurrentNode();
      if (currentNode != null) {
        this.InsertInCurrentNode(valueElement);
      } else {
        this.valueDocument.AppendChild(valueElement);
      }
      this.openElements.add(valueElement);
      return valueElement;
    }

    private Element AddHtmlElementNoPush(StartTagToken tag) {
      Element valueElement = Element.FromToken(tag);
      IElement currentNode = this.GetCurrentNode();
      if (currentNode != null) {
        this.InsertInCurrentNode(valueElement);
      }
      return valueElement;
    }

    private void AdjustForeignAttributes(StartTagToken valueToken) {
      List<Attr> Attributes = valueToken.GetAttributes();
      for (Attr attr : Attributes) {
        String valueName = attr.GetName();
        if (valueName.equals("xlink:actuate") ||
          valueName.equals(
            "xlink:arcrole") ||
          valueName.equals("xlink:href") ||
          valueName.equals(
            "xlink:role") ||
          valueName.equals("xlink:show") ||
          valueName.equals(
            "xlink:title") ||
          valueName.equals("xlink:type")) {
          attr.SetNamespace(HtmlCommon.XLINK_NAMESPACE);
        } else if (valueName.equals("xml:base") ||
          valueName.equals(
            "xml:lang") ||
          valueName.equals("xml:space")) {
          attr.SetNamespace(HtmlCommon.XML_NAMESPACE);
        } else if (valueName.equals("xmlns") ||
          valueName.equals(
            "xmlns:xlink")) {
          attr.SetNamespace(HtmlCommon.XMLNS_NAMESPACE);
        }
      }
    }

    private boolean HasHtmlOpenElement(String name) {
      List<IElement> oe = this.openElements;
      for (IElement e : oe) {
        if (HtmlCommon.IsHtmlElement(e, name)) {
          return true;
        }
      }
      return false;
    }

    private void AdjustMathMLAttributes(StartTagToken valueToken) {
      List<Attr> Attributes = valueToken.GetAttributes();
      for (Attr attr : Attributes) {
        if (attr.GetName().equals("definitionurl")) {
          attr.SetName("definitionURL");
        }
      }
    }

    private void AdjustSvgAttributes(StartTagToken valueToken) {
      List<Attr> attributeList = valueToken.GetAttributes();
      for (Attr attr : attributeList) {
        String valueName = attr.GetName();
        if (valueName.equals("attributename")) {
          {
            attr.SetName("attributeName");
          }
        } else if (valueName.equals("attributetype")) {
          {
            attr.SetName("attributeType");
          }
        } else if (valueName.equals("basefrequency")) {
          {
            attr.SetName("baseFrequency");
          }
        } else if (valueName.equals("baseprofile")) {
          {
            attr.SetName("baseProfile");
          }
        } else if (valueName.equals("calcmode")) {
          {
            attr.SetName("calcMode");
          }
        } else if (valueName.equals("clippathunits")) {
          {
            attr.SetName("clipPathUnits");
          }
        } else if (valueName.equals("diffuseconstant")) {
          {
            attr.SetName("diffuseConstant");
          }
        } else if (valueName.equals("edgemode")) {
          {
            attr.SetName("edgeMode");
          }
        } else if (valueName.equals("filterunits")) {
          {
            attr.SetName("filterUnits");
          }
        } else if (valueName.equals("glyphref")) {
          {
            attr.SetName("glyphRef");
          }
        } else if (valueName.equals("gradienttransform")) {
          attr.SetName("gradientTransform");
        } else if (valueName.equals("gradientunits")) {
          {
            attr.SetName("gradientUnits");
          }
        } else if (valueName.equals("kernelmatrix")) {
          {
            attr.SetName("kernelMatrix");
          }
        } else if (valueName.equals("kernelunitlength")) {
          {
            attr.SetName("kernelUnitLength");
          }
        } else if (valueName.equals("keypoints")) {
          {
            attr.SetName("keyPoints");
          }
        } else if (valueName.equals("keysplines")) {
          {
            attr.SetName("keySplines");
          }
        } else if (valueName.equals("keytimes")) {
          {
            attr.SetName("keyTimes");
          }
        } else if (valueName.equals("lengthadjust")) {
          {
            attr.SetName("lengthAdjust");
          }
        } else if (valueName.equals("limitingconeangle")) {
          attr.SetName("limitingConeAngle");
        } else if (valueName.equals("markerheight")) {
          {
            attr.SetName("markerHeight");
          }
        } else if (valueName.equals("markerunits")) {
          {
            attr.SetName("markerUnits");
          }
        } else if (valueName.equals("markerwidth")) {
          {
            attr.SetName("markerWidth");
          }
        } else if (valueName.equals("maskcontentunits")) {
          {
            attr.SetName("maskContentUnits");
          }
        } else if (valueName.equals("maskunits")) {
          {
            attr.SetName("maskUnits");
          }
        } else if (valueName.equals("numoctaves")) {
          {
            attr.SetName("numOctaves");
          }
        } else if (valueName.equals("pathlength")) {
          {
            attr.SetName("pathLength");
          }
        } else if (valueName.equals("patterncontentunits")) {
          attr.SetName("patternContentUnits");
        } else if (valueName.equals("patterntransform")) {
          {
            attr.SetName("patternTransform");
          }
        } else if (valueName.equals("patternunits")) {
          {
            attr.SetName("patternUnits");
          }
        } else if (valueName.equals("pointsatx")) {
          {
            attr.SetName("pointsAtX");
          }
        } else if (valueName.equals("pointsaty")) {
          {
            attr.SetName("pointsAtY");
          }
        } else if (valueName.equals("pointsatz")) {
          {
            attr.SetName("pointsAtZ");
          }
        } else if (valueName.equals("preservealpha")) {
          {
            attr.SetName("preserveAlpha");
          }
        } else if (valueName.equals("preserveaspectratio")) {
          attr.SetName("preserveAspectRatio");
        } else if (valueName.equals("primitiveunits")) {
          {
            attr.SetName("primitiveUnits");
          }
        } else if (valueName.equals("refx")) {
          {
            attr.SetName("refX");
          }
        } else if (valueName.equals("refy")) {
          {
            attr.SetName("refY");
          }
        } else if (valueName.equals("repeatcount")) {
          {
            attr.SetName("repeatCount");
          }
        } else if (valueName.equals("repeatdur")) {
          {
            attr.SetName("repeatDur");
          }
        } else if (valueName.equals("requiredextensions")) {
          attr.SetName("requiredExtensions");
        } else if (valueName.equals("requiredfeatures")) {
          {
            attr.SetName("requiredFeatures");
          }
        } else if (valueName.equals("specularconstant")) {
          {
            attr.SetName("specularConstant");
          }
        } else if (valueName.equals("specularexponent")) {
          {
            attr.SetName("specularExponent");
          }
        } else if (valueName.equals("spreadmethod")) {
          {
            attr.SetName("spreadMethod");
          }
        } else if (valueName.equals("startoffset")) {
          {
            attr.SetName("startOffset");
          }
        } else if (valueName.equals("stddeviation")) {
          {
            attr.SetName("stdDeviation");
          }
        } else if (valueName.equals("stitchtiles")) {
          {
            attr.SetName("stitchTiles");
          }
        } else if (valueName.equals("surfacescale")) {
          {
            attr.SetName("surfaceScale");
          }
        } else if (valueName.equals("systemlanguage")) {
          {
            attr.SetName("systemLanguage");
          }
        } else if (valueName.equals("tablevalues")) {
          {
            attr.SetName("tableValues");
          }
        } else if (valueName.equals("targetx")) {
          {
            attr.SetName("targetX");
          }
        } else if (valueName.equals("targety")) {
          {
            attr.SetName("targetY");
          }
        } else if (valueName.equals("textlength")) {
          {
            attr.SetName("textLength");
          }
        } else if (valueName.equals("viewbox")) {
          {
            attr.SetName("viewBox");
          }
        } else if (valueName.equals("viewtarget")) {
          {
            attr.SetName("viewTarget");
          }
        } else if (valueName.equals("xchannelselector")) {
          {
            attr.SetName("xChannelSelector");
          }
        } else if (valueName.equals("ychannelselector")) {
          {
            attr.SetName("yChannelSelector");
          }
        } else if (valueName.equals("zoomandpan")) {
          {
            attr.SetName("zoomAndPan");
          }
        }
      }
    }

    private boolean ApplyEndTag(String valueName, InsertionMode insMode) {
      return this.ApplyInsertionMode(
          this.GetArtificialToken(TOKEN_END_TAG, valueName),
          insMode);
    }

    private boolean ApplyForeignContext(int valueToken) {
      if (valueToken == 0) {
        this.ParseError();
        this.InsertCharacter(this.GetCurrentNode(), 0xfffd);
        return true;
      } else if ((valueToken & TOKEN_TYPE_MASK) == TOKEN_CHARACTER) {
        this.InsertCharacter(this.GetCurrentNode(), valueToken);
        if (valueToken != 0x09 && valueToken != 0x0c && valueToken != 0x0a &&
          valueToken != 0x0d && valueToken != 0x20) {
          this.framesetOk = false;
        }
        return true;
      } else if ((valueToken & TOKEN_TYPE_MASK) == TOKEN_COMMENT) {
        this.AddCommentNodeToCurrentNode(valueToken);
        return true;
      } else if ((valueToken & TOKEN_TYPE_MASK) == TOKEN_DOCTYPE) {
        this.ParseError();
        return false;
      } else if ((valueToken & TOKEN_TYPE_MASK) == TOKEN_START_TAG) {
        StartTagToken tag = (StartTagToken)this.GetToken(valueToken);
        String valueName = tag.GetName();
        boolean specialStartTag = false;
        if (valueName.equals("font") && (tag.GetAttribute("color") != null ||
            tag.GetAttribute("size") !=

            null || tag.GetAttribute("face") != null)) {
          specialStartTag = true;
          this.ParseError();
        } else if (valueName.equals("b") ||
          valueName.equals("big") ||
          valueName.equals("blockquote") ||
          valueName.equals("body") ||
          valueName.equals("br") ||
          valueName.equals("center") ||
          valueName.equals("code") ||
          valueName.equals(
            "dd") ||
          valueName.equals("div") ||
          valueName.equals("dl") ||
          valueName.equals("dt") ||
          valueName.equals("em") ||
          valueName.equals("embed") ||
          valueName.equals("h1") ||
          valueName.equals("h2") ||
          valueName.equals("h3") ||
          valueName.equals("h4") ||
          valueName.equals("h5") ||
          valueName.equals("h6") ||
          valueName.equals("head") ||
          valueName.equals("hr") ||
          valueName.equals("i") ||
          valueName.equals("img") ||
          valueName.equals("li") ||
          valueName.equals("listing") ||
          valueName.equals("meta") ||
          valueName.equals(
            "nobr") ||
          valueName.equals("ol") ||
          valueName.equals("p") ||
          valueName.equals("pre") ||
          valueName.equals("ruby") ||
          valueName.equals("s") ||
          valueName.equals("small") ||
          valueName.equals("span") ||
          valueName.equals("strong") ||
          valueName.equals("strike") ||
          valueName.equals("sub") ||
          valueName.equals("sup") ||
          valueName.equals(
            "table") ||
          valueName.equals("tt") ||
          valueName.equals("u") ||
          valueName.equals("ul") ||
          valueName.equals("var")) {
          specialStartTag = true;
          this.ParseError();
        }
        if (specialStartTag && this.context == null) {
          while (true) {
            this.PopCurrentNode();
            IElement node = this.GetCurrentNode();
            if (node.GetNamespaceURI().equals(HtmlCommon.HTML_NAMESPACE) ||
              this.IsMathMLTextIntegrationPoint(node) ||
              this.IsHtmlIntegrationPoint(node)) {
              break;
            }
          }
          return this.ApplyThisInsertionMode(valueToken);
        }
        IElement adjustedCurrentNode = (this.context != null &&
            this.openElements.size() == 1) ?
          this.context : this.GetCurrentNode(); // adjusted current node

        String namespaceValue = adjustedCurrentNode.GetNamespaceURI();
        boolean mathml = false;
        if (HtmlCommon.SVG_NAMESPACE.equals(namespaceValue)) {
          if (valueName.equals("altglyph")) {
            tag.SetName("altGlyph");
          } else if (valueName.equals("altglyphdef")) {
            tag.SetName("altGlyphDef");
          } else if (valueName.equals("altglyphitem")) {
            tag.SetName("altGlyphItem");
          } else if (valueName.equals("animatecolor")) {
            tag.SetName("animateColor");
          } else if (valueName.equals("animatemotion")) {
            tag.SetName("animateMotion");
          } else if (valueName.equals("animatetransform")) {
            tag.SetName("animateTransform");
          } else if (valueName.equals("clippath")) {
            tag.SetName("clipPath");
          } else if (valueName.equals("feblend")) {
            tag.SetName("feBlend");
          } else if (valueName.equals("fecolormatrix")) {
            tag.SetName("feColorMatrix");
          } else if (valueName.equals("fecomponenttransfer")) {
            tag.SetName("feComponentTransfer");
          } else if (valueName.equals("fecomposite")) {
            tag.SetName("feComposite");
          } else if (valueName.equals("feconvolvematrix")) {
            tag.SetName("feConvolveMatrix");
          } else if (valueName.equals("fediffuselighting")) {
            tag.SetName("feDiffuseLighting");
          } else if (valueName.equals("fedisplacementmap")) {
            tag.SetName("feDisplacementMap");
          } else if (valueName.equals("fedistantlight")) {
            tag.SetName("feDistantLight");
          } else if (valueName.equals("feflood")) {
            tag.SetName("feFlood");
          } else if (valueName.equals("fefunca")) {
            tag.SetName("feFuncA");
          } else if (valueName.equals("fefuncb")) {
            tag.SetName("feFuncB");
          } else if (valueName.equals("fefuncg")) {
            tag.SetName("feFuncG");
          } else if (valueName.equals("fefuncr")) {
            tag.SetName("feFuncR");
          } else if (valueName.equals("fegaussianblur")) {
            tag.SetName("feGaussianBlur");
          } else if (valueName.equals("feimage")) {
            tag.SetName("feImage");
          } else if (valueName.equals("femerge")) {
            tag.SetName("feMerge");
          } else if (valueName.equals("femergenode")) {
            tag.SetName("feMergeNode");
          } else if (valueName.equals("femorphology")) {
            tag.SetName("feMorphology");
          } else if (valueName.equals("feoffset")) {
            tag.SetName("feOffset");
          } else if (valueName.equals("fepointlight")) {
            tag.SetName("fePointLight");
          } else if (valueName.equals("fespecularlighting")) {
            tag.SetName("feSpecularLighting");
          } else if (valueName.equals("fespotlight")) {
            tag.SetName("feSpotLight");
          } else if (valueName.equals("fetile")) {
            tag.SetName("feTile");
          } else if (valueName.equals("feturbulence")) {
            tag.SetName("feTurbulence");
          } else if (valueName.equals("foreignobject")) {
            tag.SetName("foreignObject");
          } else if (valueName.equals("glyphref")) {
            tag.SetName("glyphRef");
          } else if (valueName.equals("lineargradient")) {
            tag.SetName("linearGradient");
          } else if (valueName.equals("radialgradient")) {
            tag.SetName("radialGradient");
          } else if (valueName.equals("textpath")) {
            tag.SetName("textPath");
          }
          this.AdjustSvgAttributes(tag);
        } else if (HtmlCommon.MATHML_NAMESPACE.equals(namespaceValue)) {
          this.AdjustMathMLAttributes(tag);
          mathml = true;
        }
        this.AdjustForeignAttributes(tag);
        // System.out.println("openel " + (Implode(openElements)));
        // System.out.println("Inserting " + tag + ", " + namespaceValue);
        Element e = this.InsertForeignElement(tag, namespaceValue);
        if (mathml && tag.GetName().equals("annotation-xml")) {
          String encoding = tag.GetAttribute("encoding");
          if (encoding != null) {
            encoding = com.upokecenter.util.DataUtilities.ToLowerCaseAscii(encoding);
            if (encoding.equals("text/Html") ||
              encoding.equals("application/xhtml+xml")) {
              this.integrationElements.add(e);
            }
          }
        }
        if (tag.IsSelfClosing()) {
          if (valueName.equals("script") &&
            this.GetCurrentNode().GetNamespaceURI()
            .equals(HtmlCommon.SVG_NAMESPACE)) {
            tag.AckSelfClosing();
            this.ApplyEndTag("script", this.insertionMode);
          } else {
            this.PopCurrentNode();
            tag.AckSelfClosing();
          }
        }
        return true;
      } else if ((valueToken & TOKEN_TYPE_MASK) == TOKEN_END_TAG) {
        EndTagToken tag = (EndTagToken)this.GetToken(valueToken);
        String valueName = tag.GetName();
        if (valueName.equals("script") &&
          HtmlCommon.IsSvgElement(this.GetCurrentNode(), "script")) {
          this.PopCurrentNode();
        } else {
          if (!DataUtilities.ToLowerCaseAscii(
            this.GetCurrentNode().GetLocalName()).equals(valueName)) {
            this.ParseError();
          }
          int originalSize = this.openElements.size();
          for (int i1 = originalSize - 1; i1 >= 0; --i1) {
            if (i1 == 0) {
              return true;
            }
            IElement node = this.openElements.get(i1);
            if (i1 < originalSize - 1 &&
              HtmlCommon.HTML_NAMESPACE.equals(node.GetNamespaceURI())) {
              this.noforeign = true;
              return this.ApplyThisInsertionMode(valueToken);
            }
            String nodeName = com.upokecenter.util.DataUtilities.ToLowerCaseAscii(node.GetLocalName());
            if (valueName.equals(nodeName)) {
              while (true) {
                IElement node2 = this.PopCurrentNode();
                if (node2.equals(node)) {
                  break;
                }
              }
              break;
            }
          }
        }
        return false;
      } else {
        return (valueToken == TOKEN_EOF) ?
          this.ApplyThisInsertionMode(valueToken) :
          true;
      }
    }

    private static final String XhtmlStrict =
      "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd";

    private static <T> String Implode(List<T> list) {
      StringBuilder b = new StringBuilder();
      for (int i = 0; i < list.size(); ++i) {
        if (i > 0) {
          b.append(", ");
        }
        b.append(list.get(i).toString());
      }
      return b.toString();
    }

    private static final String Xhtml11 =
      "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd";

    private boolean ApplyThisInsertionMode(int token) {
      return this.ApplyInsertionMode(token, this.insertionMode);
    }

    private boolean ApplyInsertionMode(int token, InsertionMode insMode) {
      /*System.out.println("[[" + String.Format("{0:X8}" , token) + " " +
        this.GetToken(token) + " " + (insMode == null ? this.insertionMode :
         insMode) + " " + this.IsForeignContext(token) + "(" +
         this.noforeign + ")");
       if (this.openElements.size() > 0) {
      // System.out.println(Implode(this.openElements));
       }*/ if (!this.noforeign && this.IsForeignContext(token)) {
        return this.ApplyForeignContext(token);
      }
      this.noforeign = false;
      switch (insMode) {
        case Initial: {
          if (token == 0x09 || token == 0x0a ||
            token == 0x0c || token == 0x0d || token ==
            0x20) {
            return false;
          }
          if ((token & TOKEN_TYPE_MASK) == TOKEN_DOCTYPE) {
            DocTypeToken doctype = (DocTypeToken)this.GetToken(token);
            StringBuilder doctypeNameBuilder = doctype.getName();
            StringBuilder doctypePublicBuilder = doctype.getValuePublicID();
            StringBuilder doctypeSystemBuilder = doctype.getValueSystemID();
            String doctypeName = (doctypeNameBuilder == null) ? "" :
              doctypeNameBuilder.toString();
            String doctypePublic = (doctypePublicBuilder == null) ? null : doctypePublicBuilder.toString();
            String doctypeSystem = (doctypeSystemBuilder == null) ? null : doctypeSystemBuilder.toString();
            boolean matchesHtml = "html".equals(doctypeName);
            boolean hasSystemId = doctype.getValueSystemID() != null;
            if (!matchesHtml || doctypePublic != null ||
              (doctypeSystem != null && !"about:legacy-compat"
                .equals(doctypeSystem))) {
              String h4public = "-//W3C//dtd html 4.0//EN";
              String html401public = "-//W3C//dtd html 4.01//EN";
              String xhtmlstrictpublic = "-//W3C//DTD XHTML 1.0 Strict//EN";
              String html4system = "http://www.w3.org/TR/REC-html40/strict.dtd";
              String html401system = "http://www.w3.org/TR/html4/strict.dtd";
              boolean html4 = matchesHtml && h4public.equals(doctypePublic) && (doctypeSystem == null ||
                  html4system.equals(doctypeSystem));
              boolean html401 = matchesHtml && html401public.equals(doctypePublic) && (doctypeSystem ==
                  null || html401system.equals(doctypeSystem));
              boolean xhtml = matchesHtml &&
                xhtmlstrictpublic.equals(doctypePublic) &&
                XhtmlStrict.equals(doctypeSystem);
              String xhtmlPublic = "-//W3C//DTD XHTML 1.1//EN";
              boolean xhtml11 = matchesHtml &&
                xhtmlPublic.equals(doctypePublic) &&
                Xhtml11.equals(doctypeSystem);
              if (!html4 && !html401 && !xhtml && !xhtml11) {
                this.ParseError();
              }
            }
            doctypePublic = (doctypePublic == null) ? ("") : doctypePublic;
            doctypeSystem = (doctypeSystem == null) ? ("") : doctypeSystem;
            DocumentType doctypeNode = new DocumentType(
              doctypeName,
              doctypePublic,
              doctypeSystem);
            this.valueDocument.setDoctype(doctypeNode);
            this.valueDocument.AppendChild(doctypeNode);
            String doctypePublicLC = null;
            if (!"about:srcdoc".equals(this.valueDocument.getAddress())) {
              if (!matchesHtml || doctype.getForceQuirks()) {
                this.valueDocument.SetMode(DocumentMode.QuirksMode);
              } else {
                doctypePublicLC = com.upokecenter.util.DataUtilities.ToLowerCaseAscii(doctypePublic);
                if ("html".equals(doctypePublicLC) ||
                  "-//w3o//dtd w3 html strict 3.0//en//"
                  .equals(doctypePublicLC) ||
                  "-/w3c/dtd html 4.0 transitional/en".equals(
                    doctypePublicLC)) {
                  this.valueDocument.SetMode(DocumentMode.QuirksMode);
                } else if (doctypePublic.length() > 0) {
                  for (String id : quirksModePublicIdPrefixes) {
                    if (
                      doctypePublicLC.startsWith(
                        id)) {
                      this.valueDocument.SetMode(DocumentMode.QuirksMode);
                      break;
                    }
                  }
                }
              }
              if (this.valueDocument.GetMode() != DocumentMode.QuirksMode) {
                doctypePublicLC = (doctypePublicLC == null) ? (com.upokecenter.util.DataUtilities.ToLowerCaseAscii(doctypePublic)) : doctypePublicLC;
                if ("http://www.ibm.com/data/dtd/v11/ibmxhtml1-transitional.dtd"
                  .equals(
                    com.upokecenter.util.DataUtilities.ToLowerCaseAscii(doctypeSystem)) ||
                  (!hasSystemId && doctypePublicLC.startsWith(
                  "-//w3c//dtd html 4.01 frameset//")) || (!hasSystemId &&
                    doctypePublicLC.startsWith(
                      "-//w3c//dtd html 4.01 transitional//"))) {
                  this.valueDocument.SetMode(DocumentMode.QuirksMode);
                }
              }
              if (this.valueDocument.GetMode() != DocumentMode.QuirksMode) {
                doctypePublicLC = (doctypePublicLC == null) ? (com.upokecenter.util.DataUtilities.ToLowerCaseAscii(doctypePublic)) : doctypePublicLC;
                if (
                  doctypePublicLC.startsWith(
                    "-//w3c//dtd xhtml 1.0 frameset//") || doctypePublicLC.startsWith(
                    "-//w3c//dtd xhtml 1.0 transitional//") || (hasSystemId &&
                    doctypePublicLC.startsWith(
                      "-//w3c//dtd html 4.01 frameset//")) || (hasSystemId &&
                    doctypePublicLC.startsWith(
                      "-//w3c//dtd html 4.01 transitional//"))) {
                  this.valueDocument.SetMode(DocumentMode.LimitedQuirksMode);
                }
              }
            }
            this.insertionMode = InsertionMode.BeforeHtml;
            return true;
          }
          if ((token & TOKEN_TYPE_MASK) == TOKEN_COMMENT) {
            this.AddCommentNodeToDocument(token);

            return true;
          }
          if (!"about:srcdoc".equals(this.valueDocument.getAddress())) {
            this.ParseError();
            this.valueDocument.SetMode(DocumentMode.QuirksMode);
          }
          this.insertionMode = InsertionMode.BeforeHtml;
          return this.ApplyThisInsertionMode(token);
        }
        case BeforeHtml: {
          if ((token & TOKEN_TYPE_MASK) == TOKEN_DOCTYPE) {
            this.ParseError();
            return false;
          }
          if (token == 0x09 || token == 0x0a ||
            token == 0x0c || token == 0x0d || token ==
            0x20) {
            return false;
          }
          if ((token & TOKEN_TYPE_MASK) == TOKEN_COMMENT) {
            this.AddCommentNodeToDocument(token);

            return true;
          } else if ((token & TOKEN_TYPE_MASK) == TOKEN_START_TAG) {
            StartTagToken tag = (StartTagToken)this.GetToken(token);
            String valueName = tag.GetName();
            if ("html".equals(valueName)) {
              this.AddHtmlElement(tag);
              this.insertionMode = InsertionMode.BeforeHead;
              return true;
            }
          } else if ((token & TOKEN_TYPE_MASK) == TOKEN_END_TAG) {
            TagToken tag = (TagToken)this.GetToken(token);
            String valueName = tag.GetName();
            if (!"html".equals(valueName) &&
              !"br".equals(valueName) &&
              !"head".equals(valueName) &&
              !"body".equals(valueName)) {
              this.ParseError();
              return false;
            }
          }
          Element valueElement = new Element();
          valueElement.SetLocalName("html");
          valueElement.SetNamespace(HtmlCommon.HTML_NAMESPACE);
          this.valueDocument.AppendChild(valueElement);
          this.openElements.add(valueElement);
          this.insertionMode = InsertionMode.BeforeHead;
          return this.ApplyThisInsertionMode(token);
        }
        case BeforeHead: {
          if (token == 0x09 || token == 0x0a ||
            token == 0x0c || token == 0x0d || token ==
            0x20) {
            return false;
          }
          if ((token & TOKEN_TYPE_MASK) == TOKEN_COMMENT) {
            this.AddCommentNodeToCurrentNode(token);
            return true;
          }
          if ((token & TOKEN_TYPE_MASK) == TOKEN_DOCTYPE) {
            this.ParseError();
            return false;
          }
          if ((token & TOKEN_TYPE_MASK) == TOKEN_START_TAG) {
            StartTagToken tag = (StartTagToken)this.GetToken(token);
            String valueName = tag.GetName();
            if ("html".equals(valueName)) {
              this.ApplyInsertionMode(token, InsertionMode.InBody);
              return true;
            } else if ("head".equals(valueName)) {
              Element valueElement = this.AddHtmlElement(tag);
              this.headElement = valueElement;
              this.insertionMode = InsertionMode.InHead;
              return true;
            }
          } else if ((token & TOKEN_TYPE_MASK) == TOKEN_END_TAG) {
            TagToken tag = (TagToken)this.GetToken(token);
            String valueName = tag.GetName();
            if ("head".equals(valueName) ||
              "br".equals(valueName) ||
              "body".equals(valueName) ||
              "html".equals(valueName)) {
              this.ApplyStartTag("head", insMode);
              return this.ApplyThisInsertionMode(token);
            } else {
              this.ParseError();
              return false;
            }
          }
          this.ApplyStartTag("head", insMode);
          return this.ApplyThisInsertionMode(token);
        }
        case InHead: {
          if ((token & TOKEN_TYPE_MASK) == TOKEN_COMMENT) {
            this.AddCommentNodeToCurrentNode(token);
            return true;
          }
          if ((token & TOKEN_TYPE_MASK) == TOKEN_DOCTYPE) {
            this.ParseError();
            return false;
          }
          if (token == 0x09 || token == 0x0a ||
            token == 0x0c || token == 0x0d || token ==
            0x20) {
            this.InsertCharacter(this.GetCurrentNode(), token);
            return true;
          }
          if ((token & TOKEN_TYPE_MASK) == TOKEN_START_TAG) {
            StartTagToken tag = (StartTagToken)this.GetToken(token);
            String valueName = tag.GetName();
            if ("html".equals(valueName)) {
              this.ApplyInsertionMode(token, InsertionMode.InBody);
              return true;
            } else if ("base".equals(valueName) ||
              "bgsound".equals(valueName) ||
              "basefont".equals(valueName) ||
              "link".equals(valueName)) {
              Element e = this.AddHtmlElementNoPush(tag);
              if (this.baseurl == null && "base".equals(valueName)) {
                // Get the valueDocument _base URL
                this.baseurl = e.GetAttribute("href");
              }
              tag.AckSelfClosing();
              return true;
            } else if ("meta".equals(valueName)) {
              Element valueElement = this.AddHtmlElementNoPush(tag);
              tag.AckSelfClosing();
              if (this.encoding.GetConfidence() ==
                EncodingConfidence.Tentative) {
                String charset = valueElement.GetAttribute("charset");
                if (charset != null) {
                  charset = Encodings.ResolveAlias(charset);
                  /*(TextEncoding.isAsciiCompatible(charset) ||
                  "utf-16be".equals(charset) ||
                  "utf-16le".equals(charset)) */ this.ChangeEncoding(charset);
                  if (this.encoding.GetConfidence() ==
                    EncodingConfidence.Certain) {
                    this.inputReader.DisableBuffer();
                  }
                  return true;
                }
                String value = com.upokecenter.util.DataUtilities.ToLowerCaseAscii(
                    valueElement.GetAttribute("http-equiv"));
                if ("content-type".equals(value)) {
                  value = valueElement.GetAttribute("content");
                  if (value != null) {
                    value = com.upokecenter.util.DataUtilities.ToLowerCaseAscii(value);
                    charset = CharsetSniffer.ExtractCharsetFromMeta(value);
                    if (true) {
                      // TODO
                      this.ChangeEncoding(charset);
                      if (this.encoding.GetConfidence() ==
                        EncodingConfidence.Certain) {
                        this.inputReader.DisableBuffer();
                      }
                      return true;
                    }
                  }
                } else if ("content-language".equals(value)) {
                  // HTML5 requires us to use this algorithm
                  // to Parse the Content-Language, rather than
                  // use HTTP parsing (with HeaderParser.GetLanguages)
                  // NOTE: this pragma is nonconforming
                  value = valueElement.GetAttribute("content");
                  if (!((value)==null || (value).length()==0) && value.indexOf(',') <
                    0) {
                    String[] data = StringUtility.SplitAtSpTabCrLfFf(value);
                    String deflang = (data.length == 0) ? "" :
                      data[0];
                    if (!((deflang) == null || (deflang).length() == 0)) {
                      this.valueDocument.setDefaultLanguage(deflang);
                    }
                  }
                }
              }
              if (this.encoding.GetConfidence() == EncodingConfidence.Certain) {
                this.inputReader.DisableBuffer();
              }
              return true;
            } else if ("title".equals(valueName)) {
              this.AddHtmlElement(tag);
              this.state = TokenizerState.RcData;
              this.originalInsertionMode = this.insertionMode;
              this.insertionMode = InsertionMode.Text;
              return true;
            } else if ("noframes".equals(valueName) ||
              "style".equals(valueName)) {
              this.AddHtmlElement(tag);
              this.state = TokenizerState.RawText;
              this.originalInsertionMode = this.insertionMode;
              this.insertionMode = InsertionMode.Text;
              return true;
            } else if ("noscript".equals(valueName)) {
              this.AddHtmlElement(tag);
              this.insertionMode = InsertionMode.InHeadNoscript;
              return true;
            } else if ("script".equals(valueName)) {
              this.AddHtmlElement(tag);
              this.state = TokenizerState.ScriptData;
              this.originalInsertionMode = this.insertionMode;
              this.insertionMode = InsertionMode.Text;
              return true;
            } else if ("template".equals(valueName)) {
              Element e = this.AddHtmlElement(tag);
              this.InsertFormattingMarker(tag, e);
              this.framesetOk = false;
              this.insertionMode = InsertionMode.InTemplate;
              this.templateModes.add(InsertionMode.InTemplate);
              return true;
            } else if ("head".equals(valueName)) {
              this.ParseError();
              return false;
            } else {
              this.ApplyEndTag("head", insMode);
              return this.ApplyThisInsertionMode(token);
            }
          } else if ((token & TOKEN_TYPE_MASK) == TOKEN_END_TAG) {
            TagToken tag = (TagToken)this.GetToken(token);
            String valueName = tag.GetName();
            if ("head".equals(valueName)) {
              this.openElements.remove(this.openElements.size() - 1);
              this.insertionMode = InsertionMode.AfterHead;
              return true;
            } else if ("template".equals(valueName)) {
              if (!this.HasHtmlOpenElement("template")) {
                this.ParseError();
                return false;
              }
              this.GenerateImpliedEndTagsThoroughly();
              IElement ie = this.GetCurrentNode();
              if (!HtmlCommon.IsHtmlElement(ie, "template")) {
                this.ParseError();
              }
              this.PopUntilHtmlElementPopped("template");
              this.ClearFormattingToMarker();
              if (this.templateModes.size() > 0) {
                this.templateModes.remove(this.templateModes.size() - 1);
              }
              this.ResetInsertionMode();
              return true;
            } else if (!(
              "br".equals(valueName) ||
              "body".equals(valueName) ||
              "html".equals(valueName))) {
              this.ParseError();
              return false;
            }
            this.ApplyEndTag("head", insMode);
            return this.ApplyThisInsertionMode(token);
          } else {
            this.ApplyEndTag("head", insMode);
            return this.ApplyThisInsertionMode(token);
          }
        }
        case AfterHead: {
          if ((token & TOKEN_TYPE_MASK) == TOKEN_CHARACTER) {
            if (token == 0x20 || token == 0x09 || token == 0x0a ||
              token == 0x0c || token == 0x0d) {
              this.InsertCharacter(
                this.GetCurrentNode(),
                token);
            } else {
              this.ApplyStartTag("body", insMode);
              this.framesetOk = true;
              return this.ApplyThisInsertionMode(token);
            }
          } else if ((token & TOKEN_TYPE_MASK) == TOKEN_DOCTYPE) {
            this.ParseError();
            return false;
          } else if ((token & TOKEN_TYPE_MASK) == TOKEN_START_TAG) {
            StartTagToken tag = (StartTagToken)this.GetToken(token);
            String valueName = tag.GetName();
            if (valueName.equals("html")) {
              this.ApplyInsertionMode(token, InsertionMode.InBody);
              return true;
            } else if (valueName.equals("body")) {
              this.AddHtmlElement(tag);
              this.framesetOk = false;
              this.insertionMode = InsertionMode.InBody;
              return true;
            } else if (valueName.equals("frameset")) {
              this.AddHtmlElement(tag);
              this.insertionMode = InsertionMode.InFrameset;
              return true;
            } else if ("base".equals(valueName) ||
              "bgsound".equals(valueName) ||
              "basefont".equals(valueName) ||
              "link".equals(valueName) ||
              "noframes".equals(valueName) ||
              "script".equals(valueName) ||
              "template".equals(valueName) ||
              "style".equals(valueName) ||
              "title".equals(valueName) ||
              "meta".equals(valueName)) {
              this.ParseError();
              this.openElements.add(this.headElement);
              this.ApplyInsertionMode(token, InsertionMode.InHead);
              this.openElements.remove(this.headElement);
              return true;
            } else if ("head".equals(valueName)) {
              this.ParseError();
              return false;
            } else {
              this.ApplyStartTag("body", insMode);
              this.framesetOk = true;
              return this.ApplyThisInsertionMode(token);
            }
          } else if ((token & TOKEN_TYPE_MASK) == TOKEN_END_TAG) {
            EndTagToken tag = (EndTagToken)this.GetToken(token);
            String valueName = tag.GetName();
            if (valueName.equals("body") ||
              valueName.equals("html") ||
              valueName.equals("br")) {
              this.ApplyStartTag("body", insMode);
              this.framesetOk = true;
              return this.ApplyThisInsertionMode(token);
            } else if (valueName.equals("template")) {
              return this.ApplyInsertionMode(token, InsertionMode.InHead);
            } else {
              this.ParseError();
              return false;
            }
          } else if ((token & TOKEN_TYPE_MASK) == TOKEN_COMMENT) {
            this.AddCommentNodeToCurrentNode(token);

            return true;
          } else if (token == TOKEN_EOF) {
            this.ApplyStartTag("body", insMode);
            this.framesetOk = true;
            return this.ApplyThisInsertionMode(token);
          }
          return true;
        }
        case Text: {
          if ((token & TOKEN_TYPE_MASK) == TOKEN_CHARACTER) {
            if (insMode != this.insertionMode) {
              this.InsertCharacter(this.GetCurrentNode(), token);
            } else {
              Text textNode =
                this.GetTextNodeToInsert(this.GetCurrentNode());
              int ch = token;
              if (textNode == null) {
                throw new IllegalStateException();
              }
              while (true) {
                StringBuilder sb = textNode.getValueText();
                if (ch <= 0xffff) {
                  {
                    sb.append((char)ch);
                  }
                } else if (ch <= 0x10ffff) {
                  sb.append((char)((((ch - 0x10000) >> 10) & 0x3ff) |
                    0xd800));
                  sb.append((char)(((ch - 0x10000) & 0x3ff) | 0xdc00));
                }
                token = this.ParserRead();
                if ((token & TOKEN_TYPE_MASK) != TOKEN_CHARACTER) {
                  this.tokenQueue.add(0, token);
                  break;
                }
                ch = token;
              }
            }
            return true;
          } else if (token == TOKEN_EOF) {
            this.ParseError();
            this.openElements.remove(this.openElements.size() - 1);
            this.insertionMode = this.originalInsertionMode;
            return this.ApplyThisInsertionMode(token);
          } else if ((token & TOKEN_TYPE_MASK) == TOKEN_END_TAG) {
            this.openElements.remove(this.openElements.size() - 1);
            this.insertionMode = this.originalInsertionMode;
          }
          return true;
        }
        case InTemplate: {
          if ((token & TOKEN_TYPE_MASK) == TOKEN_DOCTYPE ||
            (token & TOKEN_TYPE_MASK) == TOKEN_CHARACTER ||
            (token & TOKEN_TYPE_MASK) == TOKEN_COMMENT) {
            return this.ApplyInsertionMode(
                token,
                InsertionMode.InBody);
          }
          if ((token & TOKEN_TYPE_MASK) == TOKEN_START_TAG) {
            StartTagToken tag = (StartTagToken)this.GetToken(token);
            String valueName = tag.GetName();
            if (valueName.equals("base") ||
              valueName.equals("title") ||
              valueName.equals("template") ||
              valueName.equals("basefont") ||
              valueName.equals("bgsound") ||
              valueName.equals("meta") ||
              valueName.equals("link") ||
              valueName.equals("noframes") ||
              valueName.equals("style") ||
              valueName.equals("script")) {
              return this.ApplyInsertionMode(
                  token,
                  InsertionMode.InHead);
            }
            InsertionMode newMode = InsertionMode.InBody;
            if (valueName.equals("caption") ||
              valueName.equals("tbody") ||
              valueName.equals("thead") ||
              valueName.equals("tfoot") ||
              valueName.equals("colgroup")) {
              newMode = InsertionMode.InTable;
            } else if (valueName.equals("col")) {
              newMode = InsertionMode.InColumnGroup;
            } else if (valueName.equals("tr")) {
              newMode = InsertionMode.InTableBody;
            } else if (valueName.equals("td") ||
              valueName.equals("th")) {
              newMode = InsertionMode.InRow;
            }
            if (this.templateModes.size() > 0) {
              this.templateModes.remove(this.templateModes.size() - 1);
            }
            this.templateModes.add(newMode);
            this.insertionMode = newMode;
            return this.ApplyThisInsertionMode(token);
          }
          if ((token & TOKEN_TYPE_MASK) == TOKEN_END_TAG) {
            EndTagToken tag = (EndTagToken)this.GetToken(token);
            String valueName = tag.GetName();
            if (valueName.equals("template")) {
              return this.ApplyInsertionMode(
                  token,
                  InsertionMode.InHead);
            } else {
              this.ParseError();
              return true;
            }
          }
          if (token == TOKEN_EOF) {
            if (!this.HasHtmlOpenElement("template")) {
              this.StopParsing();
              return true;
            } else {
              this.ParseError();
            }
            this.PopUntilHtmlElementPopped("template");
            this.ClearFormattingToMarker();
            if (this.templateModes.size() > 0) {
              this.templateModes.remove(this.templateModes.size() - 1);
            }
            this.ResetInsertionMode();
            return this.ApplyThisInsertionMode(token);
          }
          return false;
        }

        case InBody: {
          if (token == 0) {
            this.ParseError();
            return true;
          }
          if ((token & TOKEN_TYPE_MASK) == TOKEN_COMMENT) {
            this.AddCommentNodeToCurrentNode(token);

            return true;
          }
          if ((token & TOKEN_TYPE_MASK) == TOKEN_DOCTYPE) {
            this.ParseError();
            return true;
          }
          if ((token & TOKEN_TYPE_MASK) == TOKEN_CHARACTER) {
            // System.out.println("" + ((char)token) + " " +
            // (this.GetCurrentNode().GetTagName()));
            int ch = token;
            if (ch !=
              0) {
              this.ReconstructFormatting();
            }
            Text textNode = this.GetTextNodeToInsert(this.GetCurrentNode());
            if (textNode == null) {
              throw new IllegalStateException();
            }
            while (true) {
              // Read multiple characters at once
              if (ch == 0) {
                this.ParseError();
              } else {
                StringBuilder sb = textNode.getValueText();
                if (ch <= 0xffff) {
                  {
                    sb.append((char)ch);
                  }
                } else if (ch <= 0x10ffff) {
                  sb.append((char)((((ch - 0x10000) >> 10) & 0x3ff) |
                    0xd800));
                  sb.append((char)(((ch - 0x10000) & 0x3ff) | 0xdc00));
                }
              }
              if (this.framesetOk && token != 0x20 && token != 0x09 &&
                token != 0x0a && token != 0x0c && token != 0x0d) {
                this.framesetOk = false;
              }
              // If we're only processing under a different
              // insertion mode then break
              if (insMode != this.insertionMode) {
                break;
              }
              token = this.ParserRead();
              if ((token & TOKEN_TYPE_MASK) != TOKEN_CHARACTER) {
                this.tokenQueue.add(0, token);
                break;
              }
              // System.out.println("{0} {1}"
              // , (char)token, GetCurrentNode().GetTagName());
              ch = token;
            }
            return true;
          } else if (token == TOKEN_EOF) {
            if (this.templateModes.size() > 0) {
              return this.ApplyInsertionMode(token, InsertionMode.InTemplate);
            } else {
              for (IElement e : this.openElements) {
                if (!HtmlCommon.IsHtmlElement(e, "dd") &&
                  !HtmlCommon.IsHtmlElement(e, "dt") &&
                  !HtmlCommon.IsHtmlElement(e, "li") &&
                  !HtmlCommon.IsHtmlElement(e, "option") &&
                  !HtmlCommon.IsHtmlElement(e, "optgroup") &&
                  !HtmlCommon.IsHtmlElement(e, "p") &&
                  !HtmlCommon.IsHtmlElement(e, "tbody") &&
                  !HtmlCommon.IsHtmlElement(e, "td") &&
                  !HtmlCommon.IsHtmlElement(e, "tfoot") &&
                  !HtmlCommon.IsHtmlElement(e, "th") &&
                  !HtmlCommon.IsHtmlElement(e, "tr") &&
                  !HtmlCommon.IsHtmlElement(e, "thead") &&
                  !HtmlCommon.IsHtmlElement(e, "body") &&
                  !HtmlCommon.IsHtmlElement(e, "html")) {
                  this.ParseError();
                }
              }
              this.StopParsing();
            }
          }
          if ((token & TOKEN_TYPE_MASK) == TOKEN_START_TAG) {
            // START TAGS
            StartTagToken tag = (StartTagToken)this.GetToken(token);
            String valueName = tag.GetName();
            if ("html".equals(valueName)) {
              this.ParseError();
              if (this.HasHtmlOpenElement("template")) {
                return false;
              }
              ((Element)this.openElements.get(0)).MergeAttributes(tag);

              return true;
            } else if ("base".equals(valueName) ||
              "template".equals(valueName) ||
              "bgsound".equals(valueName) ||
              "basefont".equals(valueName) ||
              "link".equals(valueName) ||
              "noframes".equals(valueName) ||
              "script".equals(valueName) ||
              "style".equals(valueName) ||
              "title".equals(valueName) ||
              "meta".equals(valueName)) {
              this.ApplyInsertionMode(token, InsertionMode.InHead);
              return true;
            } else if ("body".equals(valueName)) {
              this.ParseError();
              if (this.openElements.size() <= 1 ||
                !HtmlCommon.IsHtmlElement(this.openElements.get(1), "body")) {
                return false;
              }
              if (this.HasHtmlOpenElement("template")) {
                return false;
              }
              this.framesetOk = false;
              ((Element)this.openElements.get(1)).MergeAttributes(tag);

              return true;
            } else if ("frameset".equals(valueName)) {
              this.ParseError();
              if (!this.framesetOk || this.openElements.size() <= 1 ||
                !HtmlCommon.IsHtmlElement(this.openElements.get(1), "body")) {
                return false;
              }
              Node parent = (Node)this.openElements.get(1).GetParentNode();
              if (parent != null) {
                parent.RemoveChild((Node)this.openElements.get(1));
              }
              while (this.openElements.size() > 1) {
                this.PopCurrentNode();
              }
              this.AddHtmlElement(tag);
              this.insertionMode = InsertionMode.InFrameset;
              return true;
            } else if ("address".equals(valueName) ||
              "article".equals(valueName) ||
              "aside".equals(valueName) ||
              "blockquote".equals(valueName) ||
              "center".equals(valueName) ||
              "details".equals(valueName) ||
              "dialog".equals(valueName) ||
              "dir".equals(valueName) ||
              "div".equals(valueName) ||
              "dl".equals(valueName) ||
              "fieldset".equals(valueName) ||
              "figcaption".equals(valueName) ||
              "figure".equals(valueName) ||
              "footer".equals(valueName) ||
              "header".equals(valueName) ||
              "main".equals(valueName) ||
              "nav".equals(valueName) ||
              "ol".equals(valueName) ||
              "p".equals(valueName) ||
              "section".equals(valueName) ||
              "summary".equals(valueName) ||
              "ul".equals(valueName)
) {
              this.CloseParagraph();
              this.AddHtmlElement(tag);
              return true;
            } else if ("h1".equals(valueName) ||
              "h2".equals(valueName) ||
              "h3".equals(valueName) ||
              "h4".equals(valueName) ||
              "h5".equals(valueName) ||
              "h6".equals(valueName)) {
              this.CloseParagraph();
              IElement node = this.GetCurrentNode();
              String name1 = node.GetLocalName();
              if ("h1".equals(name1) ||
                "h2".equals(name1) ||
                "h3".equals(name1) ||
                "h4".equals(name1) ||
                "h5".equals(name1) ||
                "h6".equals(name1)) {
                this.ParseError();
                this.openElements.remove(this.openElements.size() - 1);
              }
              this.AddHtmlElement(tag);
              return true;
            } else if ("pre".equals(valueName) ||
              "listing".equals(valueName)) {
              this.CloseParagraph();
              this.AddHtmlElement(tag);
              this.SkipLineFeed();
              this.framesetOk = false;
              return true;
            } else if ("form".equals(valueName)) {
              if (this.formElement != null && !this.HasHtmlOpenElement(
                "template")) {
                this.ParseError();
                return false;
              }
              this.CloseParagraph();
              Element formElem = this.AddHtmlElement(tag);
              if (!this.HasHtmlOpenElement("template")) {
                this.formElement = formElem;
              }
              return true;
            } else if ("li".equals(valueName)) {
              this.framesetOk = false;
              for (int i = this.openElements.size() - 1; i >= 0; --i) {
                IElement node = this.openElements.get(i);
                String nodeName = node.GetLocalName();
                if (HtmlCommon.IsHtmlElement(node, "li")) {
                  this.ApplyInsertionMode(
                    this.GetArtificialToken(TOKEN_END_TAG, "li"),
                    insMode);
                  break;
                }
                if (this.IsSpecialElement(node) &&
                  !"address".equals(nodeName) &&
                  !"div".equals(nodeName) &&
                  !"p".equals(nodeName)) {
                  break;
                }
              }
              this.CloseParagraph();
              this.AddHtmlElement(tag);
              return true;
            } else if ("dd".equals(valueName) ||
              "dt".equals(valueName)) {
              this.framesetOk = false;
              for (int i = this.openElements.size() - 1; i >= 0; --i) {
                IElement node = this.openElements.get(i);
                String nodeName = node.GetLocalName();
                // System.out.println("looping through %s",nodeName);
                if (nodeName.equals("dd") ||
                  nodeName.equals("dt")) {
                  this.ApplyEndTag(nodeName, insMode);
                  break;
                }
                if (this.IsSpecialElement(node) &&
                  !"address".equals(nodeName) &&
                  !"div".equals(nodeName) &&
                  !"p".equals(nodeName)) {
                  break;
                }
              }
              this.CloseParagraph();
              this.AddHtmlElement(tag);
              return true;
            } else if ("plaintext".equals(valueName)) {
              this.CloseParagraph();
              this.AddHtmlElement(tag);
              this.state = TokenizerState.PlainText;
              return true;
            } else if ("button".equals(valueName)) {
              if (this.HasHtmlElementInScope("button")) {
                this.ParseError();
                this.ApplyEndTag("button", insMode);
                return this.ApplyThisInsertionMode(token);
              }
              this.ReconstructFormatting();
              this.AddHtmlElement(tag);
              this.framesetOk = false;
              return true;
            } else if ("a".equals(valueName)) {
              while (true) {
                IElement node = null;
                for (int i = this.formattingElements.size() - 1; i >= 0; --i) {
                  FormattingElement fe = this.formattingElements.get(i);
                  if (fe.IsMarker()) {
                    break;
                  }
                  if (fe.getElement().GetLocalName().equals("a")) {
                    node = fe.getElement();
                    break;
                  }
                }
                if (node != null) {
                  this.ParseError();
                  this.ApplyEndTag("a", insMode);
                  this.RemoveFormattingElement(node);
                  this.openElements.remove(node);
                } else {
                  break;
                }
              }
              this.ReconstructFormatting();
              this.PushFormattingElement(tag);
            } else if ("b".equals(valueName) ||
              "big".equals(valueName) ||
              "code".equals(valueName) ||
              "em".equals(valueName) ||
              "font".equals(valueName) ||
              "i".equals(valueName) ||
              "s".equals(valueName) ||
              "small".equals(valueName) ||
              "strike".equals(valueName) ||
              "strong".equals(valueName) ||
              "tt".equals(valueName) ||
              "u".equals(valueName)) {
              this.ReconstructFormatting();
              this.PushFormattingElement(tag);
            } else if ("nobr".equals(valueName)) {
              this.ReconstructFormatting();
              if (this.HasHtmlElementInScope("nobr")) {
                this.ParseError();
                this.ApplyEndTag("nobr", insMode);
                this.ReconstructFormatting();
              }
              this.PushFormattingElement(tag);
            } else if ("table".equals(valueName)) {
              if (this.valueDocument.GetMode() != DocumentMode.QuirksMode) {
                this.CloseParagraph();
              }
              this.AddHtmlElement(tag);
              this.framesetOk = false;
              this.insertionMode = InsertionMode.InTable;
              return true;
            } else if ("area".equals(valueName) ||
              "br".equals(valueName) ||
              "embed".equals(valueName) ||
              "img".equals(valueName) ||
              "keygen".equals(valueName) ||
              "wbr".equals(valueName)
) {
              this.ReconstructFormatting();
              this.AddHtmlElementNoPush(tag);
              tag.AckSelfClosing();
              this.framesetOk = false;
            } else if ("input".equals(valueName)) {
              this.ReconstructFormatting();
              this.inputElement = this.AddHtmlElementNoPush(tag);
              tag.AckSelfClosing();
              String attr = this.inputElement.GetAttribute("type");
              if (attr == null || !"hidden"
                .equals(com.upokecenter.util.DataUtilities.ToLowerCaseAscii(attr))) {
                this.framesetOk = false;
              }
            } else if ("param".equals(valueName) ||
              "source".equals(valueName) ||
              "track".equals(valueName)
) {
              this.AddHtmlElementNoPush(tag);
              tag.AckSelfClosing();
            } else if ("hr".equals(valueName)) {
              this.CloseParagraph();
              this.AddHtmlElementNoPush(tag);
              tag.AckSelfClosing();
              this.framesetOk = false;
            } else if ("image".equals(valueName)) {
              this.ParseError();
              tag.SetName("img");
              return this.ApplyThisInsertionMode(token);
            } else if ("textarea".equals(valueName)) {
              this.AddHtmlElement(tag);
              this.SkipLineFeed();
              this.state = TokenizerState.RcData;
              this.originalInsertionMode = this.insertionMode;
              this.framesetOk = false;
              this.insertionMode = InsertionMode.Text;
            } else if ("xmp".equals(valueName)) {
              this.CloseParagraph();
              this.ReconstructFormatting();
              this.framesetOk = false;
              this.AddHtmlElement(tag);
              this.state = TokenizerState.RawText;
              this.originalInsertionMode = this.insertionMode;
              this.insertionMode = InsertionMode.Text;
            } else if ("iframe".equals(valueName)) {
              this.framesetOk = false;
              this.AddHtmlElement(tag);
              this.state = TokenizerState.RawText;
              this.originalInsertionMode = this.insertionMode;
              this.insertionMode = InsertionMode.Text;
            } else if ("noembed".equals(valueName)) {
              this.AddHtmlElement(tag);
              this.state = TokenizerState.RawText;
              this.originalInsertionMode = this.insertionMode;
              this.insertionMode = InsertionMode.Text;
            } else if ("select".equals(valueName)) {
              this.ReconstructFormatting();
              this.AddHtmlElement(tag);
              this.framesetOk = false;
              this.insertionMode = (this.insertionMode ==
                  InsertionMode.InTable ||
                  this.insertionMode == InsertionMode.InCaption ||
                  this.insertionMode == InsertionMode.InTableBody ||
                  this.insertionMode == InsertionMode.InRow ||
                  this.insertionMode == InsertionMode.InCell) ?
                InsertionMode.InSelectInTable : InsertionMode.InSelect;
            } else if ("option".equals(valueName) || "optgroup".equals(valueName)) {
              if (this.GetCurrentNode().GetLocalName().equals(
                "option")) {
                this.ApplyEndTag("option", insMode);
              }
              this.ReconstructFormatting();
              this.AddHtmlElement(tag);
            } else if ("rp".equals(valueName) ||
              "rt".equals(valueName)) {
              if (this.HasHtmlElementInScope("ruby")) {
                this.GenerateImpliedEndTagsExcept("rtc");
                if (!this.GetCurrentNode().GetLocalName().equals(
                  "ruby") &&
                  !this.GetCurrentNode().GetLocalName().equals(
                  "rtc")) {
                  this.ParseError();
                }
              }
              this.AddHtmlElement(tag);
            } else if ("rb".equals(valueName) ||
              "rtc".equals(valueName)) {
              if (this.HasHtmlElementInScope("ruby")) {
                this.GenerateImpliedEndTags();
                if (!this.GetCurrentNode().GetLocalName().equals(
                  "ruby")) {
                  this.ParseError();
                }
              }
              this.AddHtmlElement(tag);
            } else if ("applet".equals(valueName) ||
              "marquee".equals(valueName) ||
              "Object".equals(valueName)) {
              this.ReconstructFormatting();
              Element e = this.AddHtmlElement(tag);
              this.InsertFormattingMarker(tag, e);
              this.framesetOk = false;
            } else if ("math".equals(valueName)) {
              this.ReconstructFormatting();
              this.AdjustMathMLAttributes(tag);
              this.AdjustForeignAttributes(tag);
              this.InsertForeignElement(
                tag,
                HtmlCommon.MATHML_NAMESPACE);
              if (tag.IsSelfClosing()) {
                tag.AckSelfClosing();
                this.PopCurrentNode();
              } else {
                // this.hasForeignContent = true;
              }
            } else if ("svg".equals(valueName)) {
              this.ReconstructFormatting();
              this.AdjustSvgAttributes(tag);
              this.AdjustForeignAttributes(tag);
              this.InsertForeignElement(tag, HtmlCommon.SVG_NAMESPACE);
              if (tag.IsSelfClosing()) {
                tag.AckSelfClosing();
                this.PopCurrentNode();
              } else {
                // this.hasForeignContent = true;
              }
            } else if ("caption".equals(valueName) ||
              "col".equals(valueName) ||
              "colgroup".equals(valueName) ||
              "frame".equals(valueName) ||
              "head".equals(valueName) ||
              "tbody".equals(valueName) ||
              "td".equals(valueName) ||
              "tfoot".equals(valueName) ||
              "th".equals(valueName) ||
              "thead".equals(valueName) ||
              "tr".equals(valueName)
) {
              this.ParseError();
              return false;
            } else {
              // System.out.println("ordinary: " + tag);
              this.ReconstructFormatting();
              this.AddHtmlElement(tag);
            }
            return true;
          } else if ((token & TOKEN_TYPE_MASK) == TOKEN_END_TAG) {
            // END TAGS
            // NOTE: Have all cases
            EndTagToken tag = (EndTagToken)this.GetToken(token);
            String valueName = tag.GetName();
            if (valueName.equals("template")) {
              this.ApplyInsertionMode(token, InsertionMode.InHead);
              return true;
            }
            if (valueName.equals("body")) {
              if (!this.HasHtmlElementInScope("body")) {
                this.ParseError();
                return false;
              }
              for (IElement e : this.openElements) {
                String name2 = e.GetLocalName();
                if (!"dd".equals(name2) &&
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
                  !"html".equals(name2)) {
                  this.ParseError();
                  // token not ignored here
                }
              }
              this.insertionMode = InsertionMode.AfterBody;
            } else if (valueName.equals("a") ||
              valueName.equals("b") ||
              valueName.equals("big") ||
              valueName.equals("code") ||
              valueName.equals("em") ||
              valueName.equals("b") ||
              valueName.equals("font") ||
              valueName.equals("i") ||
              valueName.equals("nobr") ||
              valueName.equals("s") ||
              valueName.equals("small") ||
              valueName.equals("strike") ||
              valueName.equals("strong") ||
              valueName.equals("tt") ||
              valueName.equals("u")) {
              if (
                HtmlCommon.IsHtmlElement(
                  this.GetCurrentNode(),
                  valueName)) {
                boolean found = false;
                for (int j = this.formattingElements.size() - 1; j >= 0; --j) {
                  FormattingElement fe = this.formattingElements.get(j);
                  if (this.GetCurrentNode().equals(fe.getElement())) {
                    found = true;
                  }
                }
                if (!found) {
                  this.PopCurrentNode();
                  return true;
                }
              }
              for (int i = 0; i < 8; ++i) {
                // System.out.println("i=" + i);
                // System.out.println("format before=" +
                // this.openElements.get(0).GetOwnerDocument());
                FormattingElement formatting = null;
                for (int j = this.formattingElements.size() - 1; j >= 0; --j) {
                  FormattingElement fe = this.formattingElements.get(j);
                  if (fe.IsMarker()) {
                    break;
                  }
                  if (fe.getElement().GetLocalName().equals(valueName)) {
                    formatting = fe;
                    break;
                  }
                }
                if (formatting == null) {
                  // NOTE: Steps for "any other end tag"
                  // System.out.println("no such formatting element");
                  for (int k = this.openElements.size() - 1;
                    k >= 0; --k) {
                    IElement node = this.openElements.get(k);
                    if (HtmlCommon.IsHtmlElement(node, valueName)) {
                      this.GenerateImpliedEndTagsExcept(valueName);
                      if (!node.equals(this.GetCurrentNode())) {
                        this.ParseError();
                      }
                      while (true) {
                        IElement node2 = this.PopCurrentNode();
                        if (node2.equals(node)) {
                          break;
                        }
                      }
                      break;
                    } else if (this.IsSpecialElement(node)) {
                      this.ParseError();
                      return false;
                    }
                  }
                  return true;
                }
                int formattingElementPos =
                  this.openElements.indexOf(formatting.getElement());
                // System.out.println("Formatting Element: // " +
                // formatting.getElement());
                if (formattingElementPos < 0) { // not found
                  this.ParseError();
                  // System.out.println("Not in stack of open elements");
                  this.formattingElements.remove(formatting);
                  return true;
                }
                // System.out.println("Open elements.get(" + i + "):");
                // System.out.println(Implode(openElements));
                // System.out.println("Formatting elements:");
                // System.out.println(Implode(formattingElements));
                if (!this.HasHtmlElementInScope(formatting.getElement())) {
                  this.ParseError();
                  return true;
                }
                if (!formatting.getElement().equals(this.GetCurrentNode())) {
                  this.ParseError();
                }
                IElement furthestBlock = null;
                int furthestBlockPos = -1;
                for (int j = formattingElementPos + 1;
                  j < this.openElements.size(); ++j) {
                  IElement e = this.openElements.get(j);
                  // System.out.println("is special: // " + (// e) + "// " +
                  // (this.IsSpecialElement(e)));
                  if (this.IsSpecialElement(e)) {
                    furthestBlock = e;
                    furthestBlockPos = j;
                    break;
                  }
                }
                // System.out.println("furthest block: // " + furthestBlock);
                if (furthestBlock == null) {
                  // Pop up to and including the
                  // formatting element
                  while (this.openElements.size() > formattingElementPos) {
                    this.PopCurrentNode();
                  }
                  this.formattingElements.remove(formatting);
                  // System.out.println("Open elements now.get(" + i + "):");
                  // System.out.println(Implode(openElements));
                  // System.out.println("Formatting elements now:");
                  // System.out.println(Implode(formattingElements));
                  break;
                }
                IElement commonAncestor =
                  this.openElements.get(formattingElementPos -
                    1);
                int bookmark = this.formattingElements.indexOf(formatting);
                // System.out.println("formel: {0}"
                // , this.openElements.get(formattingElementPos));
                // System.out.println("common ancestor: " + commonAncestor);
                // System.out.println("Setting bookmark to {0} [len={1}]"
                // , bookmark, this.formattingElements.size());
                IElement myNode = furthestBlock;
                IElement superiorNode = this.openElements.get(furthestBlockPos -
                    1);
                IElement lastNode = furthestBlock;
                for (int j = 0; ; j = Math.min(j + 1, 4)) {
                  myNode = superiorNode;
                  FormattingElement nodeFE =
                    this.GetFormattingElement(myNode);
                  // System.out.println("j="+j);
                  // System.out.println("nodeFE="+nodeFE);
                  if (nodeFE == null) {
                    // System.out.println("node not a formatting element");
                    superiorNode =
                      this.openElements.get(this.openElements.indexOf(myNode) -
                        1);
                    this.openElements.remove(myNode);
                    continue;
                  } else if (myNode.equals(formatting.getElement())) {
                    // System.out.println("node is the formatting element");
                    break;
                  } else if (j >= 3) {
                    int nodeFEIndex = this.formattingElements.indexOf(nodeFE);
                    this.formattingElements.remove(nodeFE);
                    if (nodeFEIndex >= 0 && nodeFEIndex <= bookmark) {
                      --bookmark;
                    }
                    superiorNode =
                      this.openElements.get(this.openElements.indexOf(myNode) -
                        1);
                    this.openElements.remove(myNode);
                    continue;
                  }
                  IElement e = Element.FromToken(nodeFE.getToken());
                  nodeFE.setElement(e);
                  int io = this.openElements.indexOf(myNode);
                  superiorNode = this.openElements.get(io - 1);
                  this.openElements.set(io, e);
                  myNode = e;
                  if (lastNode.equals(furthestBlock)) {
                    bookmark = this.formattingElements.indexOf(nodeFE) + 1;
                    // System.out.println("Moving bookmark to {0} [len={1}]"
                    // , bookmark, this.formattingElements.size());
                  }
                  // NOTE: Because 'node' can only be a formatting
                  // element, the foster parenting rule doesn't
                  // apply here
                  if (lastNode.GetParentNode() != null) {
                    ((Node)lastNode.GetParentNode()).RemoveChild(
                      (Node)lastNode);
                  }
                  myNode.AppendChild(lastNode);
                  // System.out.println("lastNode now: "+myNode);
                  lastNode = myNode;
                }
                // System.out.println("lastNode: "+lastNode);
                if (HtmlCommon.IsHtmlElement(commonAncestor, "table") ||
                  HtmlCommon.IsHtmlElement(commonAncestor, "tr") ||
                  HtmlCommon.IsHtmlElement(commonAncestor, "tbody") ||
                  HtmlCommon.IsHtmlElement(commonAncestor, "thead") ||
                  HtmlCommon.IsHtmlElement(commonAncestor, "tfoot")
) {
                  if (lastNode.GetParentNode() != null) {
                    ((Node)lastNode.GetParentNode()).RemoveChild(
                      (Node)lastNode);
                  }
                  this.FosterParent(lastNode);
                } else {
                  if (lastNode.GetParentNode() != null) {
                    ((Node)lastNode.GetParentNode()).RemoveChild(
                      (Node)lastNode);
                  }
                  commonAncestor.AppendChild(lastNode);
                }
                Element e2 = Element.FromToken(formatting.getToken());
                ArrayList<INode> fbch = new ArrayList<INode>();
                for (INode child : furthestBlock.GetChildNodes()) {
                  fbch.add(child);
                }
                for (INode child : fbch) {
                  furthestBlock.RemoveChild((Node)child);
                  // NOTE: Because 'e' can only be a formatting
                  // element, the foster parenting rule doesn't
                  // apply here
                  e2.AppendChild(child);
                }
                // NOTE: Because intervening elements, including
                // formatting elements, are cleared between table
                // and tbody/thead/tfoot and between those three
                // elements and tr, the foster parenting rule
                // doesn't apply here
                furthestBlock.AppendChild(e2);
                FormattingElement newFE = new FormattingElement();
                newFE.setValueMarker(false);
                newFE.setElement(e2);
                newFE.setToken(formatting.getToken());

                // System.out.println("Adding formatting element at {0} [len={1}]"
                // , bookmark, this.formattingElements.size());
                this.formattingElements.add(bookmark, newFE);
                this.formattingElements.remove(formatting);
                // System.out.println("Replacing open element at %d"
                // , openElements.indexOf(furthestBlock)+1);
                int idx = this.openElements.indexOf(furthestBlock) + 1;
                this.openElements.add(idx, e2);
                this.openElements.remove(formatting.getElement());
              }
              // System.out.println("format after="
              // +this.openElements.get(0).GetOwnerDocument());
            } else if ("applet".equals(valueName) ||
              "marquee".equals(valueName) ||
              "Object".equals(valueName)) {
              if (!this.HasHtmlElementInScope(valueName)) {
                this.ParseError();
                return false;
              } else {
                this.GenerateImpliedEndTags();
                if (!this.GetCurrentNode().GetLocalName().equals(valueName)) {
                  this.ParseError();
                }
                this.PopUntilHtmlElementPopped(valueName);
                this.ClearFormattingToMarker();
              }
            } else if (valueName.equals("html")) {
              return this.ApplyEndTag("body", insMode) ?
                this.ApplyThisInsertionMode(token) : false;
            } else if ("address".equals(valueName) ||
              "article".equals(valueName) ||
              "aside".equals(valueName) ||
              "blockquote".equals(valueName) ||
              "button".equals(valueName) ||
              "center".equals(valueName) ||
              "details".equals(valueName) ||
              "dialog".equals(valueName) ||
              "dir".equals(valueName) ||
              "div".equals(valueName) ||
              "dl".equals(valueName) ||
              "fieldset".equals(valueName) ||
              "figcaption".equals(valueName) ||
              "figure".equals(valueName) ||
              "footer".equals(valueName) ||
              "header".equals(valueName) ||
              "listing".equals(valueName) ||
              "main".equals(valueName) ||
              "nav".equals(valueName) ||
              "ol".equals(valueName) ||
              "pre".equals(valueName) ||
              "section".equals(valueName) ||
              "summary".equals(valueName) ||
              "ul".equals(valueName)) {
              if (!this.HasHtmlElementInScope(valueName)) {
                this.ParseError();
                return true;
              } else {
                this.GenerateImpliedEndTags();
                if (!this.GetCurrentNode().GetLocalName().equals(valueName)) {
                  this.ParseError();
                }
                this.PopUntilHtmlElementPopped(valueName);
              }
            } else if (valueName.equals("form")) {
              if (this.HasHtmlOpenElement("template")) {
                if (!this.HasHtmlElementInScope("form")) {
                  this.ParseError();
                  return false;
                }
                this.GenerateImpliedEndTags();
                if (!HtmlCommon.IsHtmlElement(this.GetCurrentNode(),
                  "form")) {
                  this.ParseError();
                }
                this.PopUntilHtmlElementPopped("form");
              } else {
                IElement node = this.formElement;
                this.formElement = null;
                if (node == null || !this.HasHtmlElementInScope(node)) {
                  this.ParseError();
                  return false;
                }
                this.GenerateImpliedEndTags();
                if (this.GetCurrentNode() != node) {
                  this.ParseError();
                }
                this.openElements.remove(node);
              }
            } else if (valueName.equals("p")) {
              if (!this.HasHtmlElementInButtonScope(valueName)) {
                this.ParseError();
                this.ApplyStartTag("p", insMode);
                return this.ApplyThisInsertionMode(token);
              }
              this.GenerateImpliedEndTagsExcept(valueName);
              if (!this.GetCurrentNode().GetLocalName().equals(valueName)) {
                this.ParseError();
              }
              this.PopUntilHtmlElementPopped(valueName);
            } else if (valueName.equals("li")) {
              if (!this.HasHtmlElementInListItemScope(valueName)) {
                this.ParseError();
                return false;
              }
              this.GenerateImpliedEndTagsExcept(valueName);
              if (!this.GetCurrentNode().GetLocalName().equals(valueName)) {
                this.ParseError();
              }
              this.PopUntilHtmlElementPopped(valueName);
            } else if (valueName.equals("h1") ||
              valueName.equals("h2") ||
              valueName.equals("h3") ||
              valueName.equals("h4") ||
              valueName.equals("h5") ||
              valueName.equals("h6")) {
              if (!this.HasHtmlHeaderElementInScope()) {
                this.ParseError();
                return false;
              }
              this.GenerateImpliedEndTags();
              if (!this.GetCurrentNode().GetLocalName().equals(valueName)) {
                this.ParseError();
              }
              while (true) {
                IElement node = this.PopCurrentNode();
                if (HtmlCommon.IsHtmlElement(node, "h1") ||
                  HtmlCommon.IsHtmlElement(node, "h2") ||
                  HtmlCommon.IsHtmlElement(node, "h3") ||
                  HtmlCommon.IsHtmlElement(node, "h4") ||
                  HtmlCommon.IsHtmlElement(node, "h5") ||
                  HtmlCommon.IsHtmlElement(node, "h6")) {
                  break;
                }
              }
              return true;
            } else if (valueName.equals("dd") ||
              valueName.equals("dt")) {
              if (!this.HasHtmlElementInScope(valueName)) {
                this.ParseError();
                return false;
              }
              this.GenerateImpliedEndTagsExcept(valueName);
              if (!this.GetCurrentNode().GetLocalName().equals(valueName)) {
                this.ParseError();
              }
              this.PopUntilHtmlElementPopped(valueName);
            } else if ("br".equals(valueName)) {
              this.ParseError();
              this.ApplyStartTag("br", insMode);
              return false;
            } else {
              for (int i = this.openElements.size() - 1; i >= 0; --i) {
                IElement node = this.openElements.get(i);
                if (HtmlCommon.IsHtmlElement(node, valueName)) {
                  this.GenerateImpliedEndTagsExcept(valueName);
                  if (!node.equals(this.GetCurrentNode())) {
                    this.ParseError();
                  }
                  while (true) {
                    IElement node2 = this.PopCurrentNode();
                    if (node2.equals(node)) {
                      break;
                    }
                  }
                  break;
                } else if (this.IsSpecialElement(node)) {
                  this.ParseError();
                  return false;
                }
              }
            }
          }
          return true;
        }
        case InHeadNoscript: {
          if ((token & TOKEN_TYPE_MASK) == TOKEN_CHARACTER) {
            if (token == 0x09 || token == 0x0a || token == 0x0c ||
              token == 0x0d || token == 0x20) {
              return this.ApplyInsertionMode(
                  token,
                  InsertionMode.InBody);
            } else {
              this.ParseError();
              this.PopCurrentNode();
              this.insertionMode = InsertionMode.InHead;
              return this.ApplyThisInsertionMode(token);
            }
          } else if ((token & TOKEN_TYPE_MASK) == TOKEN_DOCTYPE) {
            this.ParseError();
            return false;
          } else if ((token & TOKEN_TYPE_MASK) == TOKEN_START_TAG) {
            StartTagToken tag = (StartTagToken)this.GetToken(token);
            String valueName = tag.GetName();
            if (valueName.equals("html")) {
              return this.ApplyInsertionMode(
                  token,
                  InsertionMode.InBody);
            } else if (valueName.equals("basefont") ||
              valueName.equals(
                "bgsound") ||
              valueName.equals("link") ||
              valueName.equals("meta") ||
              valueName.equals("noframes") ||
              valueName.equals("style")
) {
              return this.ApplyInsertionMode(
                  token,
                  InsertionMode.InHead);
            } else if (valueName.equals("head") ||
              valueName.equals(
                "noscript")) {
              this.ParseError();
              return true;
            } else {
              this.ParseError();
              this.PopCurrentNode();
              this.insertionMode = InsertionMode.InHead;
              return this.ApplyThisInsertionMode(token);
            }
          } else if ((token & TOKEN_TYPE_MASK) == TOKEN_END_TAG) {
            EndTagToken tag = (EndTagToken)this.GetToken(token);
            String valueName = tag.GetName();
            if (valueName.equals("noscript")) {
              this.PopCurrentNode();
              this.insertionMode = InsertionMode.InHead;
            } else if (valueName.equals("br")) {
              this.ParseError();
              this.PopCurrentNode();
              this.insertionMode = InsertionMode.InHead;
              return this.ApplyThisInsertionMode(token);
            } else {
              this.ParseError();
              return true;
            }
          } else if ((token & TOKEN_TYPE_MASK) == TOKEN_COMMENT) {
            return this.ApplyInsertionMode(
                token,
                InsertionMode.InHead);
          } else if (token == TOKEN_EOF) {
            this.ParseError();
            this.PopCurrentNode();
            this.insertionMode = InsertionMode.InHead;
            return this.ApplyThisInsertionMode(token);
          }
          return true;
        }
        case InTable: {
          if ((token & TOKEN_TYPE_MASK) == TOKEN_CHARACTER) {
            IElement currentNode = this.GetCurrentNode();
            if (HtmlCommon.IsHtmlElement(currentNode, "table") ||
              HtmlCommon.IsHtmlElement(currentNode, "tbody") ||
              HtmlCommon.IsHtmlElement(currentNode, "tfoot") ||
              HtmlCommon.IsHtmlElement(currentNode, "thead") ||
              HtmlCommon.IsHtmlElement(currentNode, "tr")) {
              this.pendingTableCharacters.delete(
                0, (
                0)+(this.pendingTableCharacters.length()));
              this.originalInsertionMode = this.insertionMode;
              this.insertionMode = InsertionMode.InTableText;
              return this.ApplyThisInsertionMode(token);
            } else {
              // NOTE: Foster parenting rules don't apply here, since
              // the current node isn't table, tbody, tfoot, thead, or
              // tr and won't change while In Body is being applied
              this.ParseError();
              return this.ApplyInsertionMode(
                  token,
                  InsertionMode.InBody);
            }
          } else if ((token & TOKEN_TYPE_MASK) == TOKEN_DOCTYPE) {
            this.ParseError();
            return false;
          } else if ((token & TOKEN_TYPE_MASK) == TOKEN_START_TAG) {
            StartTagToken tag = (StartTagToken)this.GetToken(token);
            String valueName = tag.GetName();
            if (valueName.equals("table")) {
              this.ParseError();
              return this.ApplyEndTag("table", insMode) ?
                this.ApplyThisInsertionMode(token) : false;
            } else if (valueName.equals("caption")) {
              while (true) {
                IElement node = this.GetCurrentNode();
                if (node == null || HtmlCommon.IsHtmlElement(node, "table") ||
                  HtmlCommon.IsHtmlElement(node, "html") ||
                  HtmlCommon.IsHtmlElement(node, "template")) {
                  break;
                }
                this.PopCurrentNode();
              }
              this.InsertFormattingMarker(
                tag,
                this.AddHtmlElement(tag));
              this.insertionMode = InsertionMode.InCaption;
              return true;
            } else if (valueName.equals("colgroup")) {
              while (true) {
                IElement node = this.GetCurrentNode();
                if (node == null || HtmlCommon.IsHtmlElement(node, "table") ||
                  HtmlCommon.IsHtmlElement(node, "html") ||
                  HtmlCommon.IsHtmlElement(node, "template")) {
                  break;
                }
                this.PopCurrentNode();
              }
              this.AddHtmlElement(tag);
              this.insertionMode = InsertionMode.InColumnGroup;
              return true;
            } else if (valueName.equals("col")) {
              this.ApplyStartTag("colgroup", insMode);
              return this.ApplyThisInsertionMode(token);
            } else if (valueName.equals("tbody") ||
              valueName.equals(
                "tfoot") ||
              valueName.equals("thead")) {
              while (true) {
                IElement node = this.GetCurrentNode();
                if (node == null || HtmlCommon.IsHtmlElement(node, "table") ||
                  HtmlCommon.IsHtmlElement(node, "html") ||
                  HtmlCommon.IsHtmlElement(node, "template")) {
                  break;
                }
                this.PopCurrentNode();
              }
              this.AddHtmlElement(tag);
              this.insertionMode = InsertionMode.InTableBody;
            } else if (valueName.equals("td") ||
              valueName.equals("th") ||
              valueName.equals("tr")) {
              this.ApplyStartTag("tbody", insMode);
              return this.ApplyThisInsertionMode(token);
            } else if (valueName.equals("style") ||
              valueName.equals("script") ||
              valueName.equals("template")) {
              return this.ApplyInsertionMode(token, InsertionMode.InHead);
            } else if (valueName.equals("input")) {
              String attr = tag.GetAttribute("type");
              if (attr == null || !"hidden"
                .equals(com.upokecenter.util.DataUtilities.ToLowerCaseAscii(attr))) {
                this.ParseError();
                this.doFosterParent = true;
                this.ApplyInsertionMode(
                  token,
                  InsertionMode.InBody);
                this.doFosterParent = false;
              } else {
                this.ParseError();
                this.AddHtmlElementNoPush(tag);
                tag.AckSelfClosing();
              }
            } else if (valueName.equals("form")) {
              this.ParseError();
              if (this.formElement != null) {
                return false;
              }
              this.formElement = this.AddHtmlElementNoPush(tag);
            } else {
              this.ParseError();
              this.doFosterParent = true;
              this.ApplyInsertionMode(token, InsertionMode.InBody);
              this.doFosterParent = false;
            }
          } else if ((token & TOKEN_TYPE_MASK) == TOKEN_END_TAG) {
            EndTagToken tag = (EndTagToken)this.GetToken(token);
            String valueName = tag.GetName();
            if (valueName.equals("table")) {
              if (!this.HasHtmlElementInTableScope(valueName)) {
                this.ParseError();
                return false;
              } else {
                this.PopUntilHtmlElementPopped(valueName);
                this.ResetInsertionMode();
              }
            } else if (valueName.equals("body") ||
              valueName.equals(
                "caption") ||
              valueName.equals("col") ||
              valueName.equals("colgroup") ||
              valueName.equals("html") ||
              valueName.equals("tbody") ||
              valueName.equals("td") ||
              valueName.equals("tfoot") ||
              valueName.equals("th") ||
              valueName.equals("thead") ||
              valueName.equals("tr")) {
              this.ParseError();
              return false;
            } else if (valueName.equals("template")) {
              return this.ApplyInsertionMode(token, InsertionMode.InHead);
            } else {
              this.doFosterParent = true;
              this.ApplyInsertionMode(token, InsertionMode.InBody);
              this.doFosterParent = false;
            }
          } else if ((token & TOKEN_TYPE_MASK) == TOKEN_COMMENT) {
            this.AddCommentNodeToCurrentNode(token);
            return true;
          } else {
            return (token == TOKEN_EOF) ?
              this.ApplyInsertionMode(token, InsertionMode.InBody) : true;
          }
          return true;
        }
        case InTableText: {
          if ((token & TOKEN_TYPE_MASK) == TOKEN_CHARACTER) {
            if (token == 0) {
              this.ParseError();
              return false;
            } else {
              if (token <= 0xffff) {
                this.pendingTableCharacters.append((char)token);
              } else if (token <= 0x10ffff) {
                this.pendingTableCharacters.append((char)((((token -
                  0x10000) >> 10) & 0x3ff) | 0xd800));
                this.pendingTableCharacters.append((char)(((token -
                  0x10000) & 0x3ff) | 0xdc00));
              }
            }
          } else {
            boolean nonspace = false;
            String str = this.pendingTableCharacters.toString();
            for (int i = 0; i < str.length(); ++i) {
              int c = com.upokecenter.util.DataUtilities.CodePointAt(str, i);
              if (c >= 0x10000) {
                ++c;
              }
              if (c != 0x9 && c != 0xa && c != 0xc && c != 0xd && c != 0x20) {
                nonspace = true;
                break;
              }
            }
            if (nonspace) {
              // See 'anything else' for 'in table'
              this.ParseError();
              this.doFosterParent = true;
              for (int i = 0; i < str.length(); ++i) {
                int c = com.upokecenter.util.DataUtilities.CodePointAt(str, i);
                if (c >= 0x10000) {
                  ++c;
                }
                this.ApplyInsertionMode(c, InsertionMode.InBody);
              }
              this.doFosterParent = false;
            } else {
              this.InsertString(
                this.GetCurrentNode(),
                this.pendingTableCharacters.toString());
            }
            this.insertionMode = this.originalInsertionMode;
            return this.ApplyThisInsertionMode(token);
          }
          return true;
        }
        case InCaption: {
          if ((token & TOKEN_TYPE_MASK) == TOKEN_START_TAG) {
            StartTagToken tag = (StartTagToken)this.GetToken(token);
            String valueName = tag.GetName();
            if (valueName.equals("caption") ||
              valueName.equals("col") ||
              valueName.equals("colgroup") ||
              valueName.equals("tbody") ||
              valueName.equals("thead") ||
              valueName.equals("td") ||
              valueName.equals("tfoot") ||
              valueName.equals("th") ||
              valueName.equals("tr")) {
              if (!this.HasHtmlElementInTableScope("caption")) {
                this.ParseError();
                return false;
              }
              this.GenerateImpliedEndTags();
              if (!HtmlCommon.IsHtmlElement(this.GetCurrentNode(),
                "caption")) {
                this.ParseError();
              }
              this.PopUntilHtmlElementPopped("caption");
              this.ClearFormattingToMarker();
              this.insertionMode = InsertionMode.InTable;
              return this.ApplyThisInsertionMode(token);
            } else {
              return this.ApplyInsertionMode(
                  token,
                  InsertionMode.InBody);
            }
          } else if ((token & TOKEN_TYPE_MASK) == TOKEN_END_TAG) {
            EndTagToken tag = (EndTagToken)this.GetToken(token);
            String valueName = tag.GetName();
            if (valueName.equals("caption") ||
              valueName.equals("table")) {
              if (!this.HasHtmlElementInTableScope(valueName)) {
                this.ParseError();
                return false;
              }
              this.GenerateImpliedEndTags();
              if (!HtmlCommon.IsHtmlElement(this.GetCurrentNode(),
                "caption")) {
                this.ParseError();
              }
              this.PopUntilHtmlElementPopped("caption");
              this.ClearFormattingToMarker();
              this.insertionMode = InsertionMode.InTable;
              if (valueName.equals("table")) {
                return this.ApplyThisInsertionMode(token);
              }
            } else if (valueName.equals("body") ||
              valueName.equals("col") ||
              valueName.equals("colgroup") ||
              valueName.equals("tbody") ||
              valueName.equals("thead") ||
              valueName.equals("td") ||
              valueName.equals("tfoot") ||
              valueName.equals("th") ||
              valueName.equals("tr") ||
              valueName.equals("html")) {
              this.ParseError();
            } else {
              return this.ApplyInsertionMode(
                  token,
                  InsertionMode.InBody);
            }
          } else {
            return this.ApplyInsertionMode(
                token,
                InsertionMode.InBody);
          }
          return true;
        }
        case InColumnGroup: {
          if ((token & TOKEN_TYPE_MASK) == TOKEN_CHARACTER &&
            (token == 0x20 || token == 0x0c || token ==
              0x0a || token == 0x0d || token == 0x09)) {
            this.InsertCharacter(
              this.GetCurrentNode(),
              token);
            return true;
          } else if ((token & TOKEN_TYPE_MASK) == TOKEN_DOCTYPE) {
            this.ParseError();
            return true;
          } else if ((token & TOKEN_TYPE_MASK) == TOKEN_COMMENT) {
            this.AddCommentNodeToCurrentNode(token);
            return true;
          } else if ((token & TOKEN_TYPE_MASK) == TOKEN_START_TAG) {
            StartTagToken tag = (StartTagToken)this.GetToken(token);
            String valueName = tag.GetName();
            if (valueName.equals("html")) {
              return this.ApplyInsertionMode(
                  token,
                  InsertionMode.InBody);
            } else if (valueName.equals("col")) {
              this.AddHtmlElementNoPush(tag);
              tag.AckSelfClosing();
              return true;
            } else if (valueName.equals("template")) {
              return this.ApplyInsertionMode(
                  token,
                  InsertionMode.InHead);
            }
          } else if ((token & TOKEN_TYPE_MASK) == TOKEN_END_TAG) {
            EndTagToken tag = (EndTagToken)this.GetToken(token);
            String valueName = tag.GetName();
            if (valueName.equals("colgroup")) {
              if (!HtmlCommon.IsHtmlElement(this.GetCurrentNode(),
                "colgroup")) {
                this.ParseError();
                return false;
              }
              this.PopCurrentNode();
              this.insertionMode = InsertionMode.InTable;
              return true;
            } else if (valueName.equals("col")) {
              this.ParseError();
              return true;
            } else if (valueName.equals("template")) {
              return this.ApplyInsertionMode(
                  token,
                  InsertionMode.InHead);
            }
          } else if (token == TOKEN_EOF) {
            return this.ApplyInsertionMode(token, InsertionMode.InBody);
          }
          if (!HtmlCommon.IsHtmlElement(this.GetCurrentNode(), "colgroup")) {
            this.ParseError();
            return false;
          }
          this.PopCurrentNode();
          this.insertionMode = InsertionMode.InTable;
          return this.ApplyThisInsertionMode(token);
        }
        case InTableBody: {
          if ((token & TOKEN_TYPE_MASK) == TOKEN_START_TAG) {
            StartTagToken tag = (StartTagToken)this.GetToken(token);
            String valueName = tag.GetName();
            if (valueName.equals("tr")) {
              while (true) {
                IElement node = this.GetCurrentNode();
                if (node == null || HtmlCommon.IsHtmlElement(node, "tbody") ||
                  HtmlCommon.IsHtmlElement(node, "tfoot") ||
                  HtmlCommon.IsHtmlElement(node, "thead") ||
                  HtmlCommon.IsHtmlElement(node, "template") ||
                  HtmlCommon.IsHtmlElement(node, "html")) {
                  break;
                }
                this.PopCurrentNode();
              }
              this.AddHtmlElement(tag);
              this.insertionMode = InsertionMode.InRow;
            } else if (valueName.equals("th") ||
              valueName.equals("td")) {
              this.ParseError();
              this.ApplyStartTag("tr", insMode);
              return this.ApplyThisInsertionMode(token);
            } else if (valueName.equals("caption") ||
              valueName.equals("col") ||
              valueName.equals("colgroup") ||
              valueName.equals("tbody") ||
              valueName.equals("tfoot") ||
              valueName.equals("thead")) {
              if (!this.HasHtmlElementInTableScope("tbody") &&
                !this.HasHtmlElementInTableScope("thead") &&
                !this.HasHtmlElementInTableScope("tfoot")
) {
                this.ParseError();
                return false;
              }
              while (true) {
                IElement node = this.GetCurrentNode();
                if (node == null || HtmlCommon.IsHtmlElement(node, "tbody") ||
                  HtmlCommon.IsHtmlElement(node, "tfoot") ||
                  HtmlCommon.IsHtmlElement(node, "thead") ||
                  HtmlCommon.IsHtmlElement(node, "template") ||
                  HtmlCommon.IsHtmlElement(node, "html")) {
                  break;
                }
                this.PopCurrentNode();
              }
              this.ApplyEndTag(
                this.GetCurrentNode().GetLocalName(),
                insMode);
              return this.ApplyThisInsertionMode(token);
            } else {
              return this.ApplyInsertionMode(
                  token,
                  InsertionMode.InTable);
            }
          } else if ((token & TOKEN_TYPE_MASK) == TOKEN_END_TAG) {
            EndTagToken tag = (EndTagToken)this.GetToken(token);
            String valueName = tag.GetName();
            if (valueName.equals("tbody") ||
              valueName.equals("tfoot") ||
              valueName.equals("thead")) {
              if (!this.HasHtmlElementInTableScope(valueName)) {
                this.ParseError();
                return false;
              }
              while (true) {
                IElement node = this.GetCurrentNode();
                if (node == null ||
                  HtmlCommon.IsHtmlElement(node, "tbody") ||
                  HtmlCommon.IsHtmlElement(node, "tfoot") ||
                  HtmlCommon.IsHtmlElement(node, "thead") ||
                  HtmlCommon.IsHtmlElement(node, "template") ||
                  HtmlCommon.IsHtmlElement(node, "html")) {
                  break;
                }
                this.PopCurrentNode();
              }
              this.PopCurrentNode();
              this.insertionMode = InsertionMode.InTable;
            } else if (valueName.equals("table")) {
              if (!this.HasHtmlElementInTableScope("tbody") &&
                !this.HasHtmlElementInTableScope("thead") &&
                !this.HasHtmlElementInTableScope("tfoot")
) {
                this.ParseError();
                return false;
              }
              while (true) {
                IElement node = this.GetCurrentNode();
                if (node == null || HtmlCommon.IsHtmlElement(node, "tbody") ||
                  HtmlCommon.IsHtmlElement(node, "tfoot") ||
                  HtmlCommon.IsHtmlElement(node, "thead") ||
                  HtmlCommon.IsHtmlElement(node, "template") ||
                  HtmlCommon.IsHtmlElement(node, "html")) {
                  break;
                }
                this.PopCurrentNode();
              }
              this.ApplyEndTag(
                this.GetCurrentNode().GetLocalName(),
                insMode);
              return this.ApplyThisInsertionMode(token);
            } else if (valueName.equals("body") ||
              valueName.equals("caption") ||
              valueName.equals("col") ||
              valueName.equals("colgroup") ||
              valueName.equals("html") ||
              valueName.equals("td") ||
              valueName.equals("th") ||
              valueName.equals("tr")) {
              this.ParseError();
              return false;
            } else {
              return this.ApplyInsertionMode(
                  token,
                  InsertionMode.InTable);
            }
          } else {
            return this.ApplyInsertionMode(
                token,
                InsertionMode.InTable);
          }
          return true;
        }
        case InRow: {
          if ((token & TOKEN_TYPE_MASK) == TOKEN_CHARACTER) {
            this.ApplyInsertionMode(token, InsertionMode.InTable);
          } else if ((token & TOKEN_TYPE_MASK) == TOKEN_DOCTYPE) {
            this.ApplyInsertionMode(token, InsertionMode.InTable);
          } else if ((token & TOKEN_TYPE_MASK) == TOKEN_START_TAG) {
            StartTagToken tag = (StartTagToken)this.GetToken(token);
            String valueName = tag.GetName();
            if (valueName.equals("th") ||
              valueName.equals("td")) {
              while (!HtmlCommon.IsHtmlElement(this.GetCurrentNode(), "tr") &&
                !HtmlCommon.IsHtmlElement(this.GetCurrentNode(), "html") &&
                !HtmlCommon.IsHtmlElement(this.GetCurrentNode(), "template")) {
                this.PopCurrentNode();
              }
              this.insertionMode = InsertionMode.InCell;
              this.InsertFormattingMarker(
                tag,
                this.AddHtmlElement(tag));
            } else if (valueName.equals("caption") ||
              valueName.equals(
                "col") ||
              valueName.equals("colgroup") ||
              valueName.equals("tbody") ||
              valueName.equals("tfoot") ||
              valueName.equals("thead") ||
              valueName.equals("tr")) {
              if (this.ApplyEndTag("tr", insMode)) {
                return this.ApplyThisInsertionMode(token);
              }
            } else {
              this.ApplyInsertionMode(token, InsertionMode.InTable);
            }
          } else if ((token & TOKEN_TYPE_MASK) == TOKEN_END_TAG) {
            EndTagToken tag = (EndTagToken)this.GetToken(token);
            String valueName = tag.GetName();
            if (valueName.equals("tr")) {
              if (!this.HasHtmlElementInTableScope(valueName)) {
                this.ParseError();
                return false;
              }
              while (!HtmlCommon.IsHtmlElement(this.GetCurrentNode(), "tr") &&
                !HtmlCommon.IsHtmlElement(this.GetCurrentNode(), "html") &&
                !HtmlCommon.IsHtmlElement(this.GetCurrentNode(), "template")) {
                this.PopCurrentNode();
              }
              this.PopCurrentNode();
              this.insertionMode = InsertionMode.InTableBody;
            } else if (valueName.equals("tbody") ||
              valueName.equals(
                "tfoot") ||
              valueName.equals("thead")) {
              if (!this.HasHtmlElementInTableScope(valueName)) {
                this.ParseError();
                return false;
              }
              this.ApplyEndTag("tr", insMode);
              return this.ApplyThisInsertionMode(token);
            } else if (valueName.equals("caption") ||
              valueName.equals("col") ||
              valueName.equals("colgroup") ||
              valueName.equals("html") ||
              valueName.equals("body") ||
              valueName.equals("td") ||
              valueName.equals("th")) {
              this.ParseError();
            } else {
              this.ApplyInsertionMode(token, InsertionMode.InTable);
            }
          } else if ((token & TOKEN_TYPE_MASK) == TOKEN_COMMENT) {
            this.ApplyInsertionMode(token, InsertionMode.InTable);
          } else if (token == TOKEN_EOF) {
            this.ApplyInsertionMode(token, InsertionMode.InTable);
          }
          return true;
        }
        case InCell: {
          if ((token & TOKEN_TYPE_MASK) == TOKEN_CHARACTER) {
            this.ApplyInsertionMode(token, InsertionMode.InBody);
          } else if ((token & TOKEN_TYPE_MASK) == TOKEN_DOCTYPE) {
            this.ApplyInsertionMode(token, InsertionMode.InBody);
          } else if ((token & TOKEN_TYPE_MASK) == TOKEN_START_TAG) {
            StartTagToken tag = (StartTagToken)this.GetToken(token);
            String valueName = tag.GetName();
            if (valueName.equals("caption") ||
              valueName.equals("col") ||
              valueName.equals("colgroup") ||
              valueName.equals("tbody") ||
              valueName.equals("td") ||
              valueName.equals("tfoot") ||
              valueName.equals("th") ||
              valueName.equals("thead") ||
              valueName.equals("tr")) {
              if (!this.HasHtmlElementInTableScope("td") &&
                !this.HasHtmlElementInTableScope("th")) {
                this.ParseError();
                return false;
              }
              this.ApplyEndTag(
                this.HasHtmlElementInTableScope("td") ? "td" : "th",
                insMode);
              return this.ApplyThisInsertionMode(token);
            } else {
              this.ApplyInsertionMode(token, InsertionMode.InBody);
            }
          } else if ((token & TOKEN_TYPE_MASK) == TOKEN_END_TAG) {
            EndTagToken tag = (EndTagToken)this.GetToken(token);
            String valueName = tag.GetName();
            if (valueName.equals("td") ||
              valueName.equals("th")) {
              if (!this.HasHtmlElementInTableScope(valueName)) {
                this.ParseError();
                return false;
              }
              this.GenerateImpliedEndTags();
              if (!this.GetCurrentNode().GetLocalName().equals(valueName)) {
                this.ParseError();
              }
              this.PopUntilHtmlElementPopped(valueName);
              this.ClearFormattingToMarker();
              this.insertionMode = InsertionMode.InRow;
            } else if (valueName.equals("caption") ||
              valueName.equals(
                "col") ||
              valueName.equals("colgroup") ||
              valueName.equals("body") ||
              valueName.equals("html")) {
              this.ParseError();
              return false;
            } else if (valueName.equals("table") ||
              valueName.equals("tbody") ||
              valueName.equals("tfoot") ||
              valueName.equals("thead") ||
              valueName.equals("tr")) {
              if (!this.HasHtmlElementInTableScope(valueName)) {
                this.ParseError();
                return false;
              }
              this.ApplyEndTag(
                this.HasHtmlElementInTableScope("td") ? "td" : "th",
                insMode);
              return this.ApplyThisInsertionMode(token);
            } else {
              this.ApplyInsertionMode(token, InsertionMode.InBody);
            }
          } else if ((token & TOKEN_TYPE_MASK) == TOKEN_COMMENT) {
            this.ApplyInsertionMode(token, InsertionMode.InBody);
          } else if (token == TOKEN_EOF) {
            this.ApplyInsertionMode(token, InsertionMode.InBody);
          }
          return true;
        }
        case InSelect: {
          if ((token & TOKEN_TYPE_MASK) == TOKEN_CHARACTER) {
            if (token == 0) {
              this.ParseError();
              return false;
            } else {
              this.InsertCharacter(
                this.GetCurrentNode(),
                token);
            }
          } else if ((token & TOKEN_TYPE_MASK) == TOKEN_DOCTYPE) {
            this.ParseError();
            return false;
          } else if ((token & TOKEN_TYPE_MASK) == TOKEN_START_TAG) {
            StartTagToken tag = (StartTagToken)this.GetToken(token);
            String valueName = tag.GetName();
            if (valueName.equals("html")) {
              this.ApplyInsertionMode(token, InsertionMode.InBody);
            } else if (valueName.equals("option")) {
              if (this.GetCurrentNode().GetLocalName().equals(
                "option")) {
                this.ApplyEndTag("option", insMode);
              }
              this.AddHtmlElement(tag);
            } else if (valueName.equals("optgroup")) {
              if (this.GetCurrentNode().GetLocalName().equals(
                "option")) {
                this.ApplyEndTag("option", insMode);
              }
              if (this.GetCurrentNode().GetLocalName().equals(
                "optgroup")) {
                this.ApplyEndTag("optgroup", insMode);
              }
              this.AddHtmlElement(tag);
            } else if (valueName.equals("select")) {
              this.ParseError();
              return this.ApplyEndTag("select", insMode);
            } else if (valueName.equals("input") ||
              valueName.equals(
                "keygen") ||
              valueName.equals("textarea")) {
              this.ParseError();
              if (!this.HasHtmlElementInSelectScope("select")) {
                return false;
              }
              this.ApplyEndTag("select", insMode);
              return this.ApplyThisInsertionMode(token);
            } else if (valueName.equals("script") || valueName.equals(
                "template")) {
              return this.ApplyInsertionMode(
                  token,
                  InsertionMode.InHead);
            } else {
              this.ParseError();
              return false;
            }
          } else if ((token & TOKEN_TYPE_MASK) == TOKEN_END_TAG) {
            EndTagToken tag = (EndTagToken)this.GetToken(token);
            String valueName = tag.GetName();
            if (valueName.equals("optgroup")) {
              if (this.GetCurrentNode().GetLocalName().equals(
                "option") && this.openElements.size() >= 2 &&
                this.openElements.get(this.openElements.size() -
                  2).GetLocalName().equals(
                "optgroup")) {
                this.ApplyEndTag("option", insMode);
              }
              if (this.GetCurrentNode().GetLocalName().equals(
                "optgroup")) {
                this.PopCurrentNode();
              } else {
                this.ParseError();
                return false;
              }
            } else if (valueName.equals("option")) {
              if (this.GetCurrentNode().GetLocalName().equals(
                "option")) {
                this.PopCurrentNode();
              } else {
                this.ParseError();
                return false;
              }
            } else if (valueName.equals("select")) {
              if (!this.HasHtmlElementInScope(valueName)) {
                this.ParseError();
                return false;
              }
              this.PopUntilHtmlElementPopped(valueName);
              this.ResetInsertionMode();
            } else if (valueName.equals("template")) {
              return this.ApplyInsertionMode(
                  token,
                  InsertionMode.InHead);
            } else {
              this.ParseError();
            }
          } else if ((token & TOKEN_TYPE_MASK) == TOKEN_COMMENT) {
            this.AddCommentNodeToCurrentNode(token);
          } else if (token == TOKEN_EOF) {
            return this.ApplyInsertionMode(
                token,
                InsertionMode.InBody);
          } else {
            this.ParseError();
          }
          return true;
        }
        case InSelectInTable: {
          if ((token & TOKEN_TYPE_MASK) == TOKEN_CHARACTER) {
            return this.ApplyInsertionMode(
                token,
                InsertionMode.InSelect);
          } else if ((token & TOKEN_TYPE_MASK) == TOKEN_DOCTYPE) {
            return this.ApplyInsertionMode(
                token,
                InsertionMode.InSelect);
          } else if ((token & TOKEN_TYPE_MASK) == TOKEN_START_TAG) {
            StartTagToken tag = (StartTagToken)this.GetToken(token);
            String valueName = tag.GetName();
            if (valueName.equals("caption") ||
              valueName.equals("table") ||
              valueName.equals("tbody") ||
              valueName.equals("tfoot") ||
              valueName.equals("thead") ||
              valueName.equals("tr") ||
              valueName.equals("td") ||
              valueName.equals("th")) {
              this.ParseError();
              this.PopUntilHtmlElementPopped("select");
              this.ResetInsertionMode();
              return this.ApplyThisInsertionMode(token);
            }
            return this.ApplyInsertionMode(
                token,
                InsertionMode.InSelect);
          } else if ((token & TOKEN_TYPE_MASK) == TOKEN_END_TAG) {
            EndTagToken tag = (EndTagToken)this.GetToken(token);
            String valueName = tag.GetName();
            if (valueName.equals("caption") ||
              valueName.equals("table") ||
              valueName.equals("tbody") ||
              valueName.equals("tfoot") ||
              valueName.equals("thead") ||
              valueName.equals("tr") ||
              valueName.equals("td") ||
              valueName.equals("th")) {
              this.ParseError();
              if (!this.HasHtmlElementInTableScope(valueName)) {
                return false;
              }
              this.ApplyEndTag("select", insMode);
              return this.ApplyThisInsertionMode(token);
            }
            return this.ApplyInsertionMode(
                token,
                InsertionMode.InSelect);
          } else if ((token & TOKEN_TYPE_MASK) == TOKEN_COMMENT) {
            return this.ApplyInsertionMode(
                token,
                InsertionMode.InSelect);
          } else {
            return (token == TOKEN_EOF) ?
              this.ApplyInsertionMode(token, InsertionMode.InSelect) :
              true;
          }
        }
        case AfterBody: {
          if ((token & TOKEN_TYPE_MASK) == TOKEN_CHARACTER) {
            if (token == 0x09 || token == 0x0a || token ==
              0x0c || token == 0x0d || token == 0x20) {
              this.ApplyInsertionMode(token, InsertionMode.InBody);
            } else {
              this.ParseError();
              this.insertionMode = InsertionMode.InBody;
              return this.ApplyThisInsertionMode(token);
            }
          } else if ((token & TOKEN_TYPE_MASK) == TOKEN_DOCTYPE) {
            this.ParseError();
            return true;
          } else if ((token & TOKEN_TYPE_MASK) == TOKEN_START_TAG) {
            StartTagToken tag = (StartTagToken)this.GetToken(token);
            String valueName = tag.GetName();
            if (valueName.equals("html")) {
              this.ApplyInsertionMode(token, InsertionMode.InBody);
            } else {
              this.ParseError();
              this.insertionMode = InsertionMode.InBody;
              return this.ApplyThisInsertionMode(token);
            }
          } else if ((token & TOKEN_TYPE_MASK) == TOKEN_END_TAG) {
            EndTagToken tag = (EndTagToken)this.GetToken(token);
            String valueName = tag.GetName();
            if (valueName.equals("html")) {
              if (this.context != null) {
                this.ParseError();
                return false;
              }
              this.insertionMode = InsertionMode.AfterAfterBody;
            } else {
              this.ParseError();
              this.insertionMode = InsertionMode.InBody;
              return this.ApplyThisInsertionMode(token);
            }
          } else if ((token & TOKEN_TYPE_MASK) == TOKEN_COMMENT) {
            this.AddCommentNodeToFirst(token);
          } else if (token == TOKEN_EOF) {
            this.StopParsing();

            return true;
          }
          return true;
        }
        case InFrameset: {
          if ((token & TOKEN_TYPE_MASK) == TOKEN_CHARACTER) {
            if (token == 0x09 || token == 0x0a || token == 0x0c ||
              token == 0x0d || token == 0x20) {
              this.InsertCharacter(
                this.GetCurrentNode(),
                token);
            } else {
              this.ParseError();
              return false;
            }
          } else if ((token & TOKEN_TYPE_MASK) == TOKEN_DOCTYPE) {
            this.ParseError();
            return false;
          } else if ((token & TOKEN_TYPE_MASK) == TOKEN_START_TAG) {
            StartTagToken tag = (StartTagToken)this.GetToken(token);
            String valueName = tag.GetName();
            if (valueName.equals("html")) {
              this.ApplyInsertionMode(token, InsertionMode.InBody);
            } else if (valueName.equals("frameset")) {
              this.AddHtmlElement(tag);
            } else if (valueName.equals("frame")) {
              this.AddHtmlElementNoPush(tag);
              tag.AckSelfClosing();
            } else if (valueName.equals("noframes")) {
              this.ApplyInsertionMode(token, InsertionMode.InHead);
            } else {
              this.ParseError();
            }
          } else if ((token & TOKEN_TYPE_MASK) == TOKEN_END_TAG) {
            if (this.GetCurrentNode().GetLocalName().equals("html")) {
              this.ParseError();
              return false;
            }
            EndTagToken tag = (EndTagToken)this.GetToken(token);
            String valueName = tag.GetName();
            if (valueName.equals("frameset")) {
              this.PopCurrentNode();
              if (this.context == null &&
                !HtmlCommon.IsHtmlElement(this.GetCurrentNode(),
                "frameset")) {
                this.insertionMode = InsertionMode.AfterFrameset;
              }
            } else {
              this.ParseError();
            }
          } else if ((token & TOKEN_TYPE_MASK) == TOKEN_COMMENT) {
            this.AddCommentNodeToCurrentNode(token);
          } else if (token == TOKEN_EOF) {
            if (!HtmlCommon.IsHtmlElement(this.GetCurrentNode(), "html")) {
              this.ParseError();
            }
            this.StopParsing();
          }
          return true;
        }
        case AfterFrameset: {
          if ((token & TOKEN_TYPE_MASK) == TOKEN_CHARACTER) {
            if (token == 0x09 || token == 0x0a || token ==
              0x0c || token == 0x0d || token == 0x20) {
              this.InsertCharacter(
                this.GetCurrentNode(),
                token);
            } else {
              this.ParseError();
            }
          } else if ((token & TOKEN_TYPE_MASK) == TOKEN_DOCTYPE) {
            this.ParseError();
          } else if ((token & TOKEN_TYPE_MASK) == TOKEN_START_TAG) {
            StartTagToken tag = (StartTagToken)this.GetToken(token);
            String valueName = tag.GetName();
            if (valueName.equals("html")) {
              return this.ApplyInsertionMode(
                  token,
                  InsertionMode.InBody);
            } else if (valueName.equals("noframes")) {
              return this.ApplyInsertionMode(
                  token,
                  InsertionMode.InHead);
            } else {
              this.ParseError();
            }
          } else if ((token & TOKEN_TYPE_MASK) == TOKEN_END_TAG) {
            EndTagToken tag = (EndTagToken)this.GetToken(token);
            String valueName = tag.GetName();
            if (valueName.equals("html")) {
              this.insertionMode = InsertionMode.AfterAfterFrameset;
            } else {
              this.ParseError();
            }
          } else if ((token & TOKEN_TYPE_MASK) == TOKEN_COMMENT) {
            this.AddCommentNodeToCurrentNode(token);
          } else if (token == TOKEN_EOF) {
            this.StopParsing();
          }
          return true;
        }
        case AfterAfterBody: {
          if ((token & TOKEN_TYPE_MASK) == TOKEN_CHARACTER) {
            if (token == 0x09 || token == 0x0a || token ==
              0x0c || token == 0x0d || token == 0x20) {
              this.ApplyInsertionMode(token, InsertionMode.InBody);
            } else {
              this.ParseError();
              this.insertionMode = InsertionMode.InBody;
              return this.ApplyThisInsertionMode(token);
            }
          } else if ((token & TOKEN_TYPE_MASK) == TOKEN_DOCTYPE) {
            this.ApplyInsertionMode(token, InsertionMode.InBody);
          } else if ((token & TOKEN_TYPE_MASK) == TOKEN_START_TAG) {
            StartTagToken tag = (StartTagToken)this.GetToken(token);
            String valueName = tag.GetName();
            if (valueName.equals("html")) {
              this.ApplyInsertionMode(token, InsertionMode.InBody);
            } else {
              this.ParseError();
              this.insertionMode = InsertionMode.InBody;
              return this.ApplyThisInsertionMode(token);
            }
          } else if ((token & TOKEN_TYPE_MASK) == TOKEN_END_TAG) {
            this.ParseError();
            this.insertionMode = InsertionMode.InBody;
            return this.ApplyThisInsertionMode(token);
          } else if ((token & TOKEN_TYPE_MASK) == TOKEN_COMMENT) {
            this.AddCommentNodeToDocument(token);
          } else if (token == TOKEN_EOF) {
            this.StopParsing();
          }
          return true;
        }
        case AfterAfterFrameset: {
          if ((token & TOKEN_TYPE_MASK) == TOKEN_CHARACTER) {
            if (token == 0x09 || token == 0x0a || token ==
              0x0c || token == 0x0d || token == 0x20) {
              this.ApplyInsertionMode(token, InsertionMode.InBody);
            } else {
              this.ParseError();
            }
          } else if ((token & TOKEN_TYPE_MASK) == TOKEN_DOCTYPE) {
            this.ApplyInsertionMode(token, InsertionMode.InBody);
          } else if ((token & TOKEN_TYPE_MASK) == TOKEN_START_TAG) {
            StartTagToken tag = (StartTagToken)this.GetToken(token);
            String valueName = tag.GetName();
            if ("html".equals(valueName)) {
              this.ApplyInsertionMode(token, InsertionMode.InBody);
            } else if ("noframes".equals(valueName)) {
              this.ApplyInsertionMode(token, InsertionMode.InHead);
            } else {
              this.ParseError();
            }
          } else if ((token & TOKEN_TYPE_MASK) == TOKEN_END_TAG) {
            this.ParseError();
          } else if ((token & TOKEN_TYPE_MASK) == TOKEN_COMMENT) {
            this.AddCommentNodeToDocument(token);
          } else if (token == TOKEN_EOF) {
            this.StopParsing();
          }
          return true;
        }
        default:
          throw new IllegalStateException();
      }
    }

    private boolean ApplyStartTag(String valueName, InsertionMode insMode) {
      return this.ApplyInsertionMode(
          this.GetArtificialToken(TOKEN_START_TAG, valueName),
          insMode);
    }

    private void ChangeEncoding(String charset) {
      String currentEncoding = this.encoding.GetEncoding();
      if (currentEncoding.equals("utf-16le") ||
        currentEncoding.equals("utf-16be")) {
        this.encoding = new EncodingConfidence(currentEncoding,
          EncodingConfidence.Certain);
        return;
      }
      if (charset.equals("utf-16le")) {
        charset = "utf-8";
      } else if (charset.equals("utf-16be")) {
        charset = "utf-8";
      }
      if (charset.equals(currentEncoding)) {
        this.encoding = new EncodingConfidence(currentEncoding,
          EncodingConfidence.Certain);
        return;
      }
      // Reinitialize all parser state
      this.Initialize();
      // Rewind the input stream and set the new encoding
      this.inputReader.Rewind();
      this.encoding = new EncodingConfidence(
        charset,
        EncodingConfidence.Certain);
      ICharacterEncoding henc = new Html5Encoding(this.encoding);
      // TODO
      // this.charInput = new StackableCharacterInput(
      // Encodings.GetDecoderInput(henc, this.inputReader));
    }

    private void ClearFormattingToMarker() {
      while (this.formattingElements.size() > 0) {
        FormattingElement fe = RemoveAtIndex(
            this.formattingElements,
            this.formattingElements.size() - 1);
        if (fe.IsMarker()) {
          break;
        }
      }
    }

    private void PopUntilHtmlElementPopped(String name) {
      while (!HtmlCommon.IsHtmlElement(this.GetCurrentNode(), name)) {
        this.PopCurrentNode();
      }
      this.PopCurrentNode();
    }

    private void CloseParagraph() {
      if (this.HasHtmlElementInButtonScope("p")) {
        this.GenerateImpliedEndTagsExcept("p");
        IElement node = this.GetCurrentNode();
        if (!HtmlCommon.IsHtmlElement(node, "p")) {
          this.ParseError();
        }
        this.PopUntilHtmlElementPopped("p");
      }
    }

    private Comment CreateCommentNode(int valueToken) {
      CommentToken comment = (CommentToken)this.GetToken(valueToken);
      Comment node = new Comment();
      StringBuilder cv = comment.getCommentValue();
      node.SetData(cv.toString());
      return node;
    }

    private int EmitCurrentTag() {
      int ret = this.tokens.size() | this.currentTag.GetTokenType();
      this.AddToken(this.currentTag);
      if (this.currentTag.GetTokenType() == TOKEN_START_TAG) {
        this.lastStartTag = this.currentTag;
      } else {
        if (this.currentTag.GetAttributes().size() > 0 ||
          this.currentTag.IsSelfClosing()) {
          this.ParseError();
        }
      }
      this.currentTag = null;
      return ret;
    }

    private void FosterParent(INode valueElement) {
      if (this.openElements.size() == 0) {
        return;
      }
      // System.out.println("Foster Parenting: " + valueElement);
      INode FosterParent = this.openElements.get(0);
      int lastTemplate = -1;
      int lastTable = -1;
      IElement e;
      for (int i = this.openElements.size() - 1; i >= 0; --i) {
        if (lastTemplate >= 0 && lastTable >= 0) {
          break;
        }
        e = this.openElements.get(i);
        if (lastTable < 0 && HtmlCommon.IsHtmlElement(e, "table")) {
          lastTable = i;
        }
        if (lastTemplate < 0 && HtmlCommon.IsHtmlElement(e, "template")) {
          lastTemplate = i;
        }
      }
      if (lastTemplate >= 0 && (lastTable < 0 || lastTemplate > lastTable)) {
        FosterParent = this.openElements.get(lastTemplate);
        ((Node)FosterParent).AppendChild(valueElement);

        return;
      }
      if (lastTable < 0) {
        FosterParent = this.openElements.get(0);
        ((Node)FosterParent).AppendChild(valueElement);

        return;
      }
      e = this.openElements.get(lastTable);
      Node parent = (Node)e.GetParentNode();
      boolean isElement = parent != null && parent.GetNodeType() ==
        NodeType.ELEMENT_NODE;
      if (!isElement) { // the parent is not an element
        if (lastTable <= 1) {
          // This usually won't happen
          throw new IllegalStateException();
        }
        // Append to the element before this table
        FosterParent = this.openElements.get(lastTable - 1);
        ((Node)FosterParent).AppendChild(valueElement);
      } else {
        // Parent of the table, insert before the table
        parent.InsertBefore((Node)valueElement, (Node)e);
      }
    }

    private void GenerateImpliedEndTags() {
      while (true) {
        IElement node = this.GetCurrentNode();
        if (HtmlCommon.IsHtmlElement(node, "dd") ||
          HtmlCommon.IsHtmlElement(node, "dt") ||
          HtmlCommon.IsHtmlElement(node, "li") ||
          HtmlCommon.IsHtmlElement(node, "option") ||
          HtmlCommon.IsHtmlElement(node, "optgroup") ||
          HtmlCommon.IsHtmlElement(node, "p") ||
          HtmlCommon.IsHtmlElement(node, "rp") ||
          HtmlCommon.IsHtmlElement(node, "rt") ||
          HtmlCommon.IsHtmlElement(node, "rb") ||
          HtmlCommon.IsHtmlElement(node, "rtc")) {
          this.PopCurrentNode();
        } else {
          break;
        }
      }
    }

    private void GenerateImpliedEndTagsThoroughly() {
      while (true) {
        IElement node = this.GetCurrentNode();
        if (HtmlCommon.IsHtmlElement(node, "dd") ||
          HtmlCommon.IsHtmlElement(node, "dd") ||
          HtmlCommon.IsHtmlElement(node, "dt") ||
          HtmlCommon.IsHtmlElement(node, "li") ||
          HtmlCommon.IsHtmlElement(node, "option") ||
          HtmlCommon.IsHtmlElement(node, "optgroup") ||
          HtmlCommon.IsHtmlElement(node, "p") ||
          HtmlCommon.IsHtmlElement(node, "rp") ||
          HtmlCommon.IsHtmlElement(node, "caption") ||
          HtmlCommon.IsHtmlElement(node, "colgroup") ||
          HtmlCommon.IsHtmlElement(node, "tbody") ||
          HtmlCommon.IsHtmlElement(node, "tfoot") ||
          HtmlCommon.IsHtmlElement(node, "thead") ||
          HtmlCommon.IsHtmlElement(node, "td") ||
          HtmlCommon.IsHtmlElement(node, "th") ||
          HtmlCommon.IsHtmlElement(node, "tr") ||
          HtmlCommon.IsHtmlElement(node, "rt") ||
          HtmlCommon.IsHtmlElement(node, "rb") ||
          HtmlCommon.IsHtmlElement(node, "rtc")) {
          this.PopCurrentNode();
        } else {
          break;
        }
      }
    }

    private void GenerateImpliedEndTagsExcept(String stringValue) {
      while (true) {
        IElement node = this.GetCurrentNode();
        if (HtmlCommon.IsHtmlElement(node, stringValue)) {
          break;
        }
        if (HtmlCommon.IsHtmlElement(node, "dd") ||
          HtmlCommon.IsHtmlElement(node, "dt") ||
          HtmlCommon.IsHtmlElement(node, "li") ||
          HtmlCommon.IsHtmlElement(node, "rb") ||
          HtmlCommon.IsHtmlElement(node, "rtc") ||
          HtmlCommon.IsHtmlElement(node, "option") ||
          HtmlCommon.IsHtmlElement(node, "optgroup") ||
          HtmlCommon.IsHtmlElement(
            node,
            "p") ||
          HtmlCommon.IsHtmlElement(
            node,
            "rp") ||
          HtmlCommon.IsHtmlElement(node, "rt")) {
          this.PopCurrentNode();
        } else {
          break;
        }
      }
    }

    private int GetArtificialToken(int type, String valueName) {
      if (type == TOKEN_END_TAG) {
        EndTagToken valueToken = new EndTagToken(valueName);
        int ret = this.tokens.size() | type;
        this.AddToken(valueToken);
        return ret;
      }
      if (type == TOKEN_START_TAG) {
        StartTagToken valueToken = new StartTagToken(valueName);
        int ret = this.tokens.size() | type;
        this.AddToken(valueToken);
        return ret;
      }
      throw new IllegalArgumentException();
    }

    private IElement GetCurrentNode() {
      return (this.openElements.size() == 0) ? null :
        this.openElements.get(this.openElements.size() - 1);
    }

    private FormattingElement GetFormattingElement(IElement node) {
      for (var fe : this.formattingElements) {
        if (!fe.IsMarker() && node.equals(fe.getElement())) {
          return fe;
        }
      }
      return null;
    }

    private Text GetFosterParentedTextNode() {
      if (this.openElements.size() == 0) {
        return null;
      }
      INode FosterParent = this.openElements.get(0);
      List<INode> childNodes;
      for (int i = this.openElements.size() - 1; i >= 0; --i) {
        IElement e = this.openElements.get(i);
        if (e.GetLocalName().equals("table")) {
          Node parent = (Node)e.GetParentNode();
          boolean isElement = parent != null && parent.GetNodeType() ==
            NodeType.ELEMENT_NODE;
          if (!isElement) { // the parent is not an valueElement
            if (i <= 1) {
              // This usually won't happen
              throw new IllegalStateException();
            }
            // Append to the valueElement before this table
            FosterParent = this.openElements.get(i - 1);
            break;
          } else {
            // Parent of the table, insert before the table
            childNodes = parent.GetChildNodesInternal();
            if (childNodes.size() == 0) {
              throw new IllegalStateException();
            }
            for (int j = 0; j < childNodes.size(); ++j) {
              if (childNodes.get(j).equals(e)) {
                if (j > 0 && childNodes.get(j - 1).GetNodeType() ==
                  NodeType.TEXT_NODE) {
                  return (Text)childNodes.get(j - 1);
                } else {
                  Text textNode = new Text();
                  parent.InsertBefore(textNode, (Node)e);
                  return textNode;
                }
              }
            }
            throw new IllegalStateException();
          }
        }
      }
      childNodes = FosterParent.GetChildNodes();
      INode lastChild = (childNodes.size() == 0) ? null :
        childNodes.get(childNodes.size() - 1);
      if (lastChild == null || lastChild.GetNodeType() != NodeType.TEXT_NODE) {
        Text textNode = new Text();
        FosterParent.AppendChild(textNode);
        return textNode;
      } else {
        return (Text)lastChild;
      }
    }

    private Text GetTextNodeToInsert(INode node) {
      if (this.doFosterParent && node.equals(this.GetCurrentNode())) {
        String valueName = ((IElement)node).GetLocalName();
        if ("table".equals(valueName) ||
          "tbody".equals(valueName) ||
          "tfoot".equals(valueName) ||
          "thead".equals(valueName) ||
          "tr".equals(valueName)) {
          return this.GetFosterParentedTextNode();
        }
      }
      List<INode> childNodes = ((INode)node).GetChildNodes();
      INode lastChild = (childNodes.size() == 0) ? null :
        childNodes.get(childNodes.size() - 1);
      if (lastChild == null || lastChild.GetNodeType() != NodeType.TEXT_NODE) {
        Text textNode = new Text();
        node.AppendChild(textNode);
        return textNode;
      } else {
        return (Text)lastChild;
      }
    }

    IToken GetToken(int valueToken) {
      if ((valueToken & TOKEN_TYPE_MASK) == TOKEN_CHARACTER ||
        (valueToken & TOKEN_TYPE_MASK) == TOKEN_EOF) {
        return null;
      } else {
        return this.tokens.get(valueToken & TOKEN_INDEX_MASK);
      }
    }

    private boolean HasHtmlElementInButtonScope(String valueName) {
      boolean found = false;
      for (IElement e : this.openElements) {
        if (e.GetLocalName().equals(valueName)) {
          found = true;
        }
      }
      if (!found) {
        return false;
      }
      for (int i = this.openElements.size() - 1; i >= 0; --i) {
        IElement e = this.openElements.get(i);
        String namespaceValue = e.GetNamespaceURI();
        String thisName = e.GetLocalName();
        if (HtmlCommon.HTML_NAMESPACE.equals(namespaceValue)) {
          if (thisName.equals(valueName)) {
            return true;
          }
          if (thisName.equals("applet") ||
            thisName.equals("caption") ||
            thisName.equals("html") ||
            thisName.equals("table") ||
            thisName.equals("td") ||
            thisName.equals("th") ||
            thisName.equals("marquee") ||
            thisName.equals("Object") ||
            thisName.equals("button")) {
            // System.out.println("not in scope: %s",thisName);
            return false;
          }
        }
        if (HtmlCommon.MATHML_NAMESPACE.equals(namespaceValue)) {
          if (thisName.equals("mi") ||
            thisName.equals("mo") ||
            thisName.equals("mn") ||
            thisName.equals("ms") ||
            thisName.equals("mtext") ||
            thisName.equals("annotation-xml")) {
            return false;
          }
        }
        if (HtmlCommon.SVG_NAMESPACE.equals(namespaceValue)) {
          if (thisName.equals("foreignObject") ||
            thisName.equals("desc") ||
            thisName.equals("title")) {
            return false;
          }
        }
      }
      return false;
    }

    private boolean HasHtmlElementInListItemScope(String valueName) {
      for (int i = this.openElements.size() - 1; i >= 0; --i) {
        IElement e = this.openElements.get(i);
        if (HtmlCommon.IsHtmlElement(e, valueName)) {
          return true;
        }
        if (HtmlCommon.IsHtmlElement(e, "applet") ||
          HtmlCommon.IsHtmlElement(e, "caption") ||
          HtmlCommon.IsHtmlElement(e, "html") ||
          HtmlCommon.IsHtmlElement(e, "table") ||
          HtmlCommon.IsHtmlElement(e, "td") ||
          HtmlCommon.IsHtmlElement(e, "th") ||
          HtmlCommon.IsHtmlElement(e, "ol") ||
          HtmlCommon.IsHtmlElement(e, "ul") ||
          HtmlCommon.IsHtmlElement(e, "marquee") ||
          HtmlCommon.IsHtmlElement(e, "Object") ||
          HtmlCommon.IsMathMLElement(e, "mi") ||
          HtmlCommon.IsMathMLElement(e, "mo") ||
          HtmlCommon.IsMathMLElement(e, "mn") ||
          HtmlCommon.IsMathMLElement(e, "ms") ||
          HtmlCommon.IsMathMLElement(e, "mtext") ||
          HtmlCommon.IsMathMLElement(e, "annotation-xml") ||
          HtmlCommon.IsSvgElement(e, "foreignObject") ||
          HtmlCommon.IsSvgElement(
            e,
            "desc") ||
          HtmlCommon.IsSvgElement(
            e,
            "title")
) {
          return false;
        }
      }
      return false;
    }

    private boolean HasHtmlElementInScope(IElement node) {
      for (int i = this.openElements.size() - 1; i >= 0; --i) {
        IElement e = this.openElements.get(i);
        if (e == node) {
          return true;
        }
        if (HtmlCommon.IsHtmlElement(e, "applet") ||
          HtmlCommon.IsHtmlElement(e, "caption") ||
          HtmlCommon.IsHtmlElement(e, "html") ||
          HtmlCommon.IsHtmlElement(e, "table") ||
          HtmlCommon.IsHtmlElement(e, "td") ||
          HtmlCommon.IsHtmlElement(e, "th") ||
          HtmlCommon.IsHtmlElement(e, "marquee") ||
          HtmlCommon.IsHtmlElement(e, "Object") ||
          HtmlCommon.IsMathMLElement(e, "mi") ||
          HtmlCommon.IsMathMLElement(e, "mo") ||
          HtmlCommon.IsMathMLElement(e, "mn") ||
          HtmlCommon.IsMathMLElement(e, "ms") ||
          HtmlCommon.IsMathMLElement(e, "mtext") ||
          HtmlCommon.IsMathMLElement(e, "annotation-xml") ||
          HtmlCommon.IsSvgElement(e, "foreignObject") ||
          HtmlCommon.IsSvgElement(
            e,
            "desc") ||
          HtmlCommon.IsSvgElement(
            e,
            "title")
) {
          return false;
        }
      }
      return false;
    }

    private boolean HasHtmlElementInScope(String valueName) {
      for (int i = this.openElements.size() - 1; i >= 0; --i) {
        IElement e = this.openElements.get(i);
        if (HtmlCommon.IsHtmlElement(e, valueName)) {
          return true;
        }
        if (HtmlCommon.IsHtmlElement(e, "applet") ||
          HtmlCommon.IsHtmlElement(e, "caption") ||
          HtmlCommon.IsHtmlElement(e, "html") ||
          HtmlCommon.IsHtmlElement(e, "table") ||
          HtmlCommon.IsHtmlElement(e, "td") ||
          HtmlCommon.IsHtmlElement(e, "th") ||
          HtmlCommon.IsHtmlElement(e, "marquee") ||
          HtmlCommon.IsHtmlElement(e, "Object") ||
          HtmlCommon.IsMathMLElement(e, "mi") ||
          HtmlCommon.IsMathMLElement(e, "mo") ||
          HtmlCommon.IsMathMLElement(e, "mn") ||
          HtmlCommon.IsMathMLElement(e, "ms") ||
          HtmlCommon.IsMathMLElement(e, "mtext") ||
          HtmlCommon.IsMathMLElement(e, "annotation-xml") ||
          HtmlCommon.IsSvgElement(e, "foreignObject") ||
          HtmlCommon.IsSvgElement(
            e,
            "desc") ||
          HtmlCommon.IsSvgElement(
            e,
            "title")
) {
          return false;
        }
      }
      return false;
    }

    private boolean HasHtmlElementInSelectScope(String valueName) {
      for (int i = this.openElements.size() - 1; i >= 0; --i) {
        IElement e = this.openElements.get(i);
        if (HtmlCommon.IsHtmlElement(e, valueName)) {
          return true;
        }
        if (!HtmlCommon.IsHtmlElement(e, "optgroup") &&
          !HtmlCommon.IsHtmlElement(e, "option")) {
          return false;
        }
      }
      return false;
    }

    private boolean HasHtmlElementInTableScope(String valueName) {
      for (int i = this.openElements.size() - 1; i >= 0; --i) {
        IElement e = this.openElements.get(i);
        if (HtmlCommon.IsHtmlElement(e, valueName)) {
          return true;
        }
        if (HtmlCommon.IsHtmlElement(e, "html") ||
          HtmlCommon.IsHtmlElement(e, "table")) {
          return false;
        }
      }
      return false;
    }

    private boolean HasHtmlHeaderElementInScope() {
      for (int i = this.openElements.size() - 1; i >= 0; --i) {
        IElement e = this.openElements.get(i);
        if (HtmlCommon.IsHtmlElement(e, "h1") ||
          HtmlCommon.IsHtmlElement(e, "h2") ||
          HtmlCommon.IsHtmlElement(e, "h3") ||
          HtmlCommon.IsHtmlElement(e, "h4") ||
          HtmlCommon.IsHtmlElement(e, "h5") ||
          HtmlCommon.IsHtmlElement(e, "h6")) {
          return true;
        }
        if (HtmlCommon.IsHtmlElement(e, "applet") ||
          HtmlCommon.IsHtmlElement(e, "caption") ||
          HtmlCommon.IsHtmlElement(e, "html") ||
          HtmlCommon.IsHtmlElement(e, "table") ||
          HtmlCommon.IsHtmlElement(e, "td") ||
          HtmlCommon.IsHtmlElement(e, "th") ||
          HtmlCommon.IsHtmlElement(e, "marquee") ||
          HtmlCommon.IsHtmlElement(e, "Object") ||
          HtmlCommon.IsMathMLElement(e, "mi") ||
          HtmlCommon.IsMathMLElement(e, "mo") ||
          HtmlCommon.IsMathMLElement(e, "mn") ||
          HtmlCommon.IsMathMLElement(e, "ms") ||
          HtmlCommon.IsMathMLElement(e, "mtext") ||
          HtmlCommon.IsMathMLElement(e, "annotation-xml") ||
          HtmlCommon.IsSvgElement(e, "foreignObject") ||
          HtmlCommon.IsSvgElement(e, "desc") ||
          HtmlCommon.IsSvgElement(e, "title")) {
          return false;
        }
      }
      return false;
    }

    private void Initialize() {
      this.noforeign = false;
      this.templateModes.clear();
      this.valueDocument = new Document();
      this.valueDocument.setAddress(this.address);
      this.valueDocument.SetBaseURI(this.address);
      this.context = null;
      this.openElements.clear();
      this.error = false;
      this.baseurl = null;
      // this.hasForeignContent = false; // performance optimization
      this.lastState = TokenizerState.Data;
      this.lastComment = null;
      this.docTypeToken = null;
      this.tokens.clear();
      this.lastStartTag = null;
      this.currentEndTag = null;
      this.currentTag = null;
      this.currentAttribute = null;
      this.bogusCommentCharacter = 0;
      this.tempBuilder.delete(0, this.tempBuilder.length());
      this.state = TokenizerState.Data;
      this.framesetOk = true;
      this.integrationElements.clear();
      this.tokenQueue.clear();
      this.insertionMode = InsertionMode.Initial;
      this.originalInsertionMode = InsertionMode.Initial;
      this.formattingElements.clear();
      this.doFosterParent = false;
      this.headElement = null;
      this.formElement = null;
      this.inputElement = null;
      this.done = false;
      this.pendingTableCharacters.delete(0, this.pendingTableCharacters.length());
    }

    private void InsertCharacter(INode node, int ch) {
      Text textNode = this.GetTextNodeToInsert(node);
      if (textNode != null) {
        StringBuilder builder = textNode.getValueText();
        if (ch <= 0xffff) {
          builder.append((char)ch);
        } else if (ch <= 0x10ffff) {
          builder.append((char)((((ch - 0x10000) >> 10) & 0x3ff) | 0xd800));
          builder.append((char)(((ch - 0x10000) & 0x3ff) | 0xdc00));
        }
      }
    }

    private Element InsertForeignElement(StartTagToken tag,
      String namespaceValue) {
      Element valueElement = Element.FromToken(tag, namespaceValue);
      String xmlns = valueElement.GetAttributeNS(
          HtmlCommon.XMLNS_NAMESPACE,
          "xmlns");
      String xlink = valueElement.GetAttributeNS(
          HtmlCommon.XMLNS_NAMESPACE,
          "xlink");
      if (xmlns != null && !xmlns.equals(namespaceValue)) {
        this.ParseError();
      }
      if (xlink != null && !xlink.equals(HtmlCommon.XLINK_NAMESPACE)) {
        this.ParseError();
      }
      IElement currentNode = this.GetCurrentNode();
      if (currentNode != null) {
        this.InsertInCurrentNode(valueElement);
      } else {
        this.valueDocument.AppendChild(valueElement);
      }
      this.openElements.add(valueElement);
      return valueElement;
    }

    private void InsertFormattingMarker(
      StartTagToken tag,
      Element AddHtmlElement) {
      FormattingElement fe = new FormattingElement();
      fe.setValueMarker(true);
      fe.setElement(AddHtmlElement);
      fe.setToken(tag);
      this.formattingElements.add(fe);
    }

    private void InsertInCurrentNode(Node valueElement) {
      IElement node = this.GetCurrentNode();
      if (this.doFosterParent) {
        String valueName = node.GetLocalName();
        if ("table".equals(valueName) ||
          "tbody".equals(valueName) ||
          "tfoot".equals(valueName) ||
          "thead".equals(valueName) ||
          "tr".equals(valueName)) {
          this.FosterParent(valueElement);
        } else {
          node.AppendChild(valueElement);
        }
      } else {
        node.AppendChild(valueElement);
      }
    }

    private void InsertString(INode node, String str) {
      Text textNode = this.GetTextNodeToInsert(node);
      if (textNode != null) {
        textNode.getValueText().append(str);
      }
    }

    private boolean IsAppropriateEndTag() {
      if (this.lastStartTag == null || this.currentEndTag == null) {
        return false;
      }
      return this.currentEndTag.GetName().equals(this.lastStartTag.GetName());
    }

    public HtmlParser CheckError(boolean ce) {
      this.checkErrorVar = ce;
      return this;
    }

    private void ParseError() {
      this.error = true;
      if (this.checkErrorVar) {
        throw new IllegalStateException();
      }
    }

    public boolean IsError() {
      return this.error;
    }

    private boolean IsForeignContext(int valueToken) {
      if (valueToken == TOKEN_EOF) {
        return false;
      }
      if (this.openElements.size() == 0) {
        return false;
      }
      IElement valueElement = (this.context != null &&
          this.openElements.size() == 1) ?
        this.context : this.GetCurrentNode(); // adjusted current node
      if (valueElement == null) {
        return false;
      }
      if (valueElement.GetNamespaceURI().equals(HtmlCommon.HTML_NAMESPACE)) {
        return false;
      }
      if ((valueToken & TOKEN_TYPE_MASK) == TOKEN_START_TAG) {
        StartTagToken tag = (StartTagToken)this.GetToken(valueToken);
        String valueName = valueElement.GetLocalName();
        // System.out.println("start tag " +valueName+","
        // +valueElement.GetNamespaceURI()+"," +tag);
        if (this.IsMathMLTextIntegrationPoint(valueElement)) {
          String tokenName = tag.GetName();
          if (!"mglyph".equals(tokenName) &&
            !"malignmark".equals(tokenName)) {
            return false;
          }
        }
        boolean annotationSVG =
          HtmlCommon.MATHML_NAMESPACE.equals(valueElement.GetNamespaceURI()) &&
          valueName.equals("annotation-xml") &&
          "svg".equals(tag.GetName());
        return !annotationSVG && !this.IsHtmlIntegrationPoint(valueElement);
      } else if ((valueToken & TOKEN_TYPE_MASK) == TOKEN_CHARACTER) {
        return !this.IsMathMLTextIntegrationPoint(valueElement) &&
          !this.IsHtmlIntegrationPoint(valueElement);
      } else {
        return true;
      }
    }

    private boolean IsHtmlIntegrationPoint(IElement valueElement) {
      if (this.integrationElements.contains(valueElement)) {
        return true;
      }
      String valueName = valueElement.GetLocalName();
      return HtmlCommon.SVG_NAMESPACE.equals(valueElement.GetNamespaceURI()) && (
          valueName.equals("foreignObject") || valueName.equals(
            "desc") ||
          valueName.equals("title"));
    }

    private boolean IsMathMLTextIntegrationPoint(IElement valueElement) {
      String valueName = valueElement.GetLocalName();
      return HtmlCommon.MATHML_NAMESPACE.equals(valueElement.GetNamespaceURI()) && (
          valueName.equals("mi") ||
          valueName.equals("mo") ||
          valueName.equals("mn") ||
          valueName.equals("ms") ||
          valueName.equals("mtext"));
    }

    private boolean IsSpecialElement(IElement node) {
      if (HtmlCommon.IsHtmlElement(node, "address") ||
        HtmlCommon.IsHtmlElement(node, "applet") ||
        HtmlCommon.IsHtmlElement(node, "area") ||
        HtmlCommon.IsHtmlElement(node, "article") ||
        HtmlCommon.IsHtmlElement(node, "aside") ||
        HtmlCommon.IsHtmlElement(node, "base") ||
        HtmlCommon.IsHtmlElement(node, "basefont") ||
        HtmlCommon.IsHtmlElement(node, "bgsound") ||
        HtmlCommon.IsHtmlElement(node, "blockquote") ||
        HtmlCommon.IsHtmlElement(node, "body") ||
        HtmlCommon.IsHtmlElement(node, "br") ||
        HtmlCommon.IsHtmlElement(node, "button") ||
        HtmlCommon.IsHtmlElement(node, "caption") ||
        HtmlCommon.IsHtmlElement(node, "center") ||
        HtmlCommon.IsHtmlElement(node, "col") ||
        HtmlCommon.IsHtmlElement(node, "colgroup") ||
        HtmlCommon.IsHtmlElement(node, "dd") ||
        HtmlCommon.IsHtmlElement(node, "details") ||
        HtmlCommon.IsHtmlElement(node, "dir") ||
        HtmlCommon.IsHtmlElement(node, "div") ||
        HtmlCommon.IsHtmlElement(node, "dl") ||
        HtmlCommon.IsHtmlElement(node, "dt") ||
        HtmlCommon.IsHtmlElement(node, "embed") ||
        HtmlCommon.IsHtmlElement(node, "fieldset") ||
        HtmlCommon.IsHtmlElement(node, "figcaption") ||
        HtmlCommon.IsHtmlElement(node, "figure") ||
        HtmlCommon.IsHtmlElement(node, "footer") ||
        HtmlCommon.IsHtmlElement(node, "form") ||
        HtmlCommon.IsHtmlElement(node, "frame") ||
        HtmlCommon.IsHtmlElement(node, "frameset") ||
        HtmlCommon.IsHtmlElement(node, "h1") ||
        HtmlCommon.IsHtmlElement(node, "h2") ||
        HtmlCommon.IsHtmlElement(node, "h3") ||
        HtmlCommon.IsHtmlElement(node, "h4") ||
        HtmlCommon.IsHtmlElement(node, "h5") ||
        HtmlCommon.IsHtmlElement(node, "h6") ||
        HtmlCommon.IsHtmlElement(node, "head") ||
        HtmlCommon.IsHtmlElement(node, "header") ||
        HtmlCommon.IsHtmlElement(node, "hr") ||
        HtmlCommon.IsHtmlElement(node, "html") ||
        HtmlCommon.IsHtmlElement(node, "iframe") ||
        HtmlCommon.IsHtmlElement(node, "img") ||
        HtmlCommon.IsHtmlElement(node, "input") ||
        HtmlCommon.IsHtmlElement(node, "isindex") ||
        HtmlCommon.IsHtmlElement(node, "li") ||
        HtmlCommon.IsHtmlElement(node, "link") ||
        HtmlCommon.IsHtmlElement(node, "listing") ||
        HtmlCommon.IsHtmlElement(node, "main") ||
        HtmlCommon.IsHtmlElement(node, "marquee") ||
        HtmlCommon.IsHtmlElement(node, "meta") ||
        HtmlCommon.IsHtmlElement(node, "nav") ||
        HtmlCommon.IsHtmlElement(node, "noembed") ||
        HtmlCommon.IsHtmlElement(node, "noframes") ||
        HtmlCommon.IsHtmlElement(node, "noscript") ||
        HtmlCommon.IsHtmlElement(node, "Object") ||
        HtmlCommon.IsHtmlElement(node, "ol") ||
        HtmlCommon.IsHtmlElement(node, "p") ||
        HtmlCommon.IsHtmlElement(node, "param") ||
        HtmlCommon.IsHtmlElement(node, "plaintext") ||
        HtmlCommon.IsHtmlElement(node, "pre") ||
        HtmlCommon.IsHtmlElement(node, "script") ||
        HtmlCommon.IsHtmlElement(node, "section") ||
        HtmlCommon.IsHtmlElement(node, "select") ||
        HtmlCommon.IsHtmlElement(node, "source") ||
        HtmlCommon.IsHtmlElement(node, "style") ||
        HtmlCommon.IsHtmlElement(node, "summary") ||
        HtmlCommon.IsHtmlElement(node, "table") ||
        HtmlCommon.IsHtmlElement(node, "tbody") ||
        HtmlCommon.IsHtmlElement(node, "td") ||
        HtmlCommon.IsHtmlElement(node, "textarea") ||
        HtmlCommon.IsHtmlElement(node, "tfoot") ||
        HtmlCommon.IsHtmlElement(node, "th") ||
        HtmlCommon.IsHtmlElement(node, "thead") ||
        HtmlCommon.IsHtmlElement(node, "title") ||
        HtmlCommon.IsHtmlElement(node, "tr") ||
        HtmlCommon.IsHtmlElement(node, "track") ||
        HtmlCommon.IsHtmlElement(node, "ul") ||
        HtmlCommon.IsHtmlElement(node, "wbr") ||
        HtmlCommon.IsHtmlElement(node, "xmp")) {
        return true;
      }
      if (HtmlCommon.IsMathMLElement(node, "mi") ||
        HtmlCommon.IsMathMLElement(node, "mo") ||
        HtmlCommon.IsMathMLElement(node, "mn") ||
        HtmlCommon.IsMathMLElement(node, "ms") ||
        HtmlCommon.IsMathMLElement(node, "mtext") ||
        HtmlCommon.IsMathMLElement(
          node,
          "annotation-xml")) {
        return true;
      }
      return (HtmlCommon.IsSvgElement(node, "foreignObject") ||
        HtmlCommon.IsSvgElement(
          node,
          "desc") || HtmlCommon.IsSvgElement(
          node,
          "title")) ? true : false;
    }
    String NodesToDebugString(List<Node>
      nodes) {
      StringBuilder builder = new StringBuilder();
      for (Node node : nodes) {
        String str = node.ToDebugString();
        String[] strarray = StringUtility.SplitAt(str, "\n");
        int len = strarray.length;
        if (len > 0 && ((strarray[len - 1]) == null || (strarray[len - 1]).length() == 0)) {
          --len; // ignore trailing empty String
        }
        for (int i = 0; i < len; ++i) {
          String el = strarray[i];
          builder.append("| ");
          builder.append(el.replace("~~~~", "\n"));
          builder.append("\n");
        }
      }
      return builder.toString();
    }

    public IDocument Parse() {
      while (true) {
        int valueToken = this.ParserRead();
        this.ApplyThisInsertionMode(valueToken);
        if ((valueToken & TOKEN_TYPE_MASK) == TOKEN_START_TAG) {
          StartTagToken tag = (StartTagToken)this.GetToken(valueToken);
          // System.out.println(tag);
          if (!tag.IsAckSelfClosing()) {
            this.ParseError();
          }
        }
        // System.out.println("valueToken=%08X, insertionMode=%s, error=%s"
        // , valueToken, insertionMode, error);
        if (this.done) {
          break;
        }
      }
      return this.valueDocument;
    }

    private int ParseCharacterReference(int allowedCharacter) {
      int markStart = this.charInput.SetSoftMark();
      int c1 = this.charInput.ReadChar();
      if (c1 < 0 || c1 == 0x09 || c1 == 0x0a || c1 == 0x0c ||
        c1 == 0x20 || c1 == 0x3c || c1 == 0x26 || (allowedCharacter >= 0 &&
          c1 == allowedCharacter)) {
        this.charInput.SetMarkPosition(markStart);
        return 0x26; // emit ampersand
      } else if (c1 == 0x23) {
        c1 = this.charInput.ReadChar();
        int value = 0;
        boolean haveHex = false;
        if (c1 == 0x78 || c1 == 0x58) {
          // Hex number
          while (true) { // skip zeros
            int c = this.charInput.ReadChar();
            if (c != '0') {
              if (c >= 0) {
                this.charInput.MoveBack(1);
              }
              break;
            }
            haveHex = true;
          }
          boolean overflow = false;
          while (true) {
            int number = this.charInput.ReadChar();
            if (number >= '0' && number <= '9') {
              if (!overflow) {
                value = (value << 4) + (number - '0');
              }
              haveHex = true;
            } else if (number >= 'a' && number <= 'f') {
              if (!overflow) {
                value = (value << 4) + (number - 'a') + 10;
              }
              haveHex = true;
            } else if (number >= 'A' && number <= 'F') {
              if (!overflow) {
                value = (value << 4) + (number - 'A') + 10;
              }
              haveHex = true;
            } else {
              if (number >= 0) {
                // move back character (except if it's EOF)
                this.charInput.MoveBack(1);
              }
              break;
            }
            if (value > 0x10ffff) {
              value = 0x110000;
              overflow = true;
            }
          }
        } else {
          if (c1 > 0) {
            this.charInput.MoveBack(1);
          }
          // Digits
          while (true) { // skip zeros
            int c = this.charInput.ReadChar();
            if (c != '0') {
              if (c >= 0) {
                this.charInput.MoveBack(1);
              }
              break;
            }
            haveHex = true;
          }
          boolean overflow = false;
          while (true) {
            int number = this.charInput.ReadChar();
            if (number >= '0' && number <= '9') {
              if (!overflow) {
                value = (value * 10) + (number - '0');
              }
              haveHex = true;
            } else {
              if (number >= 0) {
                // move back character (except if it's EOF)
                this.charInput.MoveBack(1);
              }
              break;
            }
            if (value > 0x10ffff) {
              value = 0x110000;
              overflow = true;
            }
          }
        }
        if (!haveHex) {
          // No digits: Parse error
          this.ParseError();
          this.charInput.SetMarkPosition(markStart);
          return 0x26; // emit ampersand
        }
        c1 = this.charInput.ReadChar();
        if (c1 != 0x3b) { // semicolon
          this.ParseError();
          if (c1 >= 0) {
            this.charInput.MoveBack(1); // Parse error
          }
        }
        if (value > 0x10ffff || ((value & 0xf800) == 0xd800)) {
          this.ParseError();
          value = 0xfffd; // Parse error
        } else if (value >= 0x80 && value < 0xa0) {
          this.ParseError();
          // Parse error
          int[] replacements = new int[] {
            0x20ac, 0x81, 0x201a, 0x192,
            0x201e, 0x2026, 0x2020, 0x2021, 0x2c6, 0x2030, 0x160, 0x2039, 0x152, 0x8d,
            0x17d, 0x8f, 0x90, 0x2018, 0x2019, 0x201c, 0x201d, 0x2022, 0x2013, 0x2014,
            0x2dc, 0x2122, 0x161, 0x203a, 0x153, 0x9d, 0x17e, 0x178,
          };
          value = replacements[value - 0x80];
        } else if (value == 0x0d) {
          // Parse error
          this.ParseError();
        } else if (value == 0x00) {
          // Parse error
          this.ParseError();
          value = 0xfffd;
        }
        if (value == 0x08 || value == 0x0b ||
          (value & 0xfffe) == 0xfffe || (value >= 0x0e && value <= 0x1f) ||
          value == 0x7f || (value >= 0xfdd0 && value <= 0xfdef)) {
          // Parse error
          this.ParseError();
        }
        return value;
      } else if ((c1 >= 'A' && c1 <= 'Z') || (c1 >= 'a' && c1 <= 'z') ||
        (c1 >= '0' && c1 <= '9')) {
        int[] data = null;
        // check for certain well-known entities
        if (c1 == 'g') {
          if (this.charInput.ReadChar() == 't' && this.charInput.ReadChar() ==
            ';') {
            return '>';
          }
          this.charInput.SetMarkPosition(markStart + 1);
        } else if (c1 == 'l') {
          if (this.charInput.ReadChar() == 't' && this.charInput.ReadChar() ==
            ';') {
            return '<';
          }
          this.charInput.SetMarkPosition(markStart + 1);
        } else if (c1 == 'a') {
          if (this.charInput.ReadChar() == 'm' && this.charInput.ReadChar()
            == 'p' && this.charInput.ReadChar() == ';') {
            return '&';
          }
          this.charInput.SetMarkPosition(markStart + 1);
        } else if (c1 == 'n') {
          if (this.charInput.ReadChar() == 'b' && this.charInput.ReadChar() ==
            's' &&
            this.charInput.ReadChar() == 'p' && this.charInput.ReadChar() ==
            ';') {
            return 0xa0;
          }
          this.charInput.SetMarkPosition(markStart + 1);
        }
        int count = 0;
        for (int index = 0; index < HtmlEntities.GetEntities().length;
          ++index) {
          String entity = HtmlEntities.GetEntities()[index];
          if (entity.charAt(0) == c1) {
            if (data == null) {
              // Read the rest of the character reference
              // (the entities are sorted by length, so
              // we get the maximum length possible starting
              // with the first matching character)
              data = new int[entity.length() - 1];
              count = this.charInput.Read(data, 0, data.length);
              // System.out.println("markposch=%c",(char)data[0]);
            }
            // if fewer bytes were read than the
            // entity's remaining length, this
            // can't match
            // System.out.println("data count=%s %s"
            // , count, stream.getMarkPosition());
            if (count < entity.length() - 1) {
              continue;
            }
            boolean matched = true;
            for (int i = 1; i < entity.length(); ++i) {
              // System.out.println("%c %c | markpos=%d",
              // (char)data[i-1], entity.charAt(i), stream.getMarkPosition());
              if (data[i - 1] != entity.charAt(i)) {
                matched = false;
                break;
              }
            }
            if (matched) {
              // Move back the difference between the
              // number of bytes actually read and
              // this entity's length
              this.charInput.MoveBack(count - (entity.length() - 1));
              // System.out.println("lastchar=%c",entity.charAt(entity.length()-1));
              if (allowedCharacter >= 0 && entity.charAt(entity.length() - 1) != ';') {
                // Get the next character after the entity
                int ch2 = this.charInput.ReadChar();
                if (ch2 == '=' || (ch2 >= 'A' && ch2 <= 'Z') ||
                  (ch2 >= 'a' && ch2 <= 'z') || (ch2 >= '0' && ch2 <= '9')) {
                  if (ch2 == '=') {
                    this.ParseError();
                  }
                  this.charInput.SetMarkPosition(markStart);
                  return 0x26; // return ampersand rather than entity
                } else {
                  if (ch2 >= 0) {
                    this.charInput.MoveBack(1);
                  }
                  if (entity.charAt(entity.length() - 1) != ';') {
                    this.ParseError();
                  }
                }
              } else {
                if (entity.charAt(entity.length() - 1) != ';') {
                  this.ParseError();
                }
              }
              return HtmlEntities.GetEntityValues()[index];
            }
          }
        }
        // no match
        this.charInput.SetMarkPosition(markStart);
        while (true) {
          int ch2 = this.charInput.ReadChar();
          if (ch2 == ';') {
            this.ParseError();
            break;
          } else if (!((ch2 >= 'A' && ch2 <= 'Z') || (ch2 >= 'a' && ch2 <= 'z'
) || (ch2 >= '0' && ch2 <= '9'))) {
            break;
          }
        }
        this.charInput.SetMarkPosition(markStart);
        return 0x26;
      } else {
        // not a character reference
        this.charInput.SetMarkPosition(markStart);
        return 0x26; // emit ampersand
      }
    }

    public List<INode> ParseFragment(IElement context) {
      if (context == null) {
        throw new IllegalArgumentException();
      }
      this.Initialize();
      this.valueDocument = new Document();
      INode ownerDocument = context;
      INode lastForm = null;
      while (ownerDocument != null) {
        if (lastForm == null && ownerDocument.GetNodeType() ==
          NodeType.ELEMENT_NODE) {
          if (HtmlCommon.IsHtmlElement((IElement)ownerDocument, "form")) {
            lastForm = ownerDocument;
          }
        }
        ownerDocument = ownerDocument.GetParentNode();
        if (ownerDocument == null ||
          ownerDocument.GetNodeType() == NodeType.DOCUMENT_NODE) {
          break;
        }
      }
      Document ownerDoc = null;
      if (ownerDocument != null &&
        ownerDocument.GetNodeType() == NodeType.DOCUMENT_NODE) {
        ownerDoc = (Document)ownerDocument;
        this.valueDocument.SetMode(ownerDoc.GetMode());
      }
      this.state = TokenizerState.Data;
      if (HtmlCommon.IsHtmlElement(context, "title") ||
        HtmlCommon.IsHtmlElement(context, "textarea")) {
        this.state = TokenizerState.RcData;
      } else if (HtmlCommon.IsHtmlElement(context, "style") ||
        HtmlCommon.IsHtmlElement(context, "xmp") ||
        HtmlCommon.IsHtmlElement(context, "iframe") ||
        HtmlCommon.IsHtmlElement(context, "noembed") ||
        HtmlCommon.IsHtmlElement(context, "noframes")) {
        this.state = TokenizerState.RawText;
      } else if (HtmlCommon.IsHtmlElement(context, "script")) {
        this.state = TokenizerState.ScriptData;
      } else if (HtmlCommon.IsHtmlElement(context, "noscript")) {
        this.state = TokenizerState.Data;
      } else if (HtmlCommon.IsHtmlElement(context, "plaintext")) {
        this.state = TokenizerState.PlainText;
      }
      Element valueElement = new Element();
      valueElement.SetLocalName("html");
      valueElement.SetNamespace(HtmlCommon.HTML_NAMESPACE);
      this.valueDocument.AppendChild(valueElement);
      this.done = false;
      this.openElements.clear();
      this.openElements.add(valueElement);
      if (HtmlCommon.IsHtmlElement(context, "template")) {
        this.templateModes.add(InsertionMode.InTemplate);
      }
      this.context = context;
      this.ResetInsertionMode();
      this.formElement = (lastForm == null) ? null : ((Element)lastForm);
      if (this.encoding.GetConfidence() != EncodingConfidence.Irrelevant) {
        this.encoding = new EncodingConfidence(
          this.encoding.GetEncoding(),
          EncodingConfidence.Irrelevant);
      }
      this.Parse();
      return new ArrayList<INode>(valueElement.GetChildNodes());
    }

    public List<INode> ParseFragment(String contextName) {
      Element valueElement = new Element();
      valueElement.SetLocalName(contextName);
      valueElement.SetNamespace(HtmlCommon.HTML_NAMESPACE);
      return this.ParseFragment(valueElement);
    }

    public List<String[]> ParseTokens(String s, String lst) {
      this.Initialize();
      ArrayList<String[]> ret = new ArrayList<String[]>();
      StringBuilder characters = new StringBuilder();
      if (lst != null) {
        this.lastStartTag = new StartTagToken(lst);
      }
      if (s.equals("PLAINTEXT state")) {
        this.state = TokenizerState.PlainText;
      }
      if (s.equals("RCDATA state")) {
        this.state = TokenizerState.RcData;
      }
      if (s.equals("RAWTEXT state")) {
        this.state = TokenizerState.RawText;
      }
      if (s.equals("Script data state")) {
        this.state = TokenizerState.ScriptData;
      }
      if (s.equals("CDATA section state")) {
        this.state = TokenizerState.CData;
      }
      // System.out.println("tok state="+this.state+ ","+s);
      // System.out.println("" + (this.lastStartTag));
      while (true) {
        int valueToken = this.ParserRead();
        if ((valueToken & TOKEN_TYPE_MASK) != TOKEN_CHARACTER) {
          if (characters.length() > 0) {
            ret.add(new String[] { "Character", characters.toString() });
            characters.delete(0, characters.length());
          }
        } else {
          if (valueToken <= 0xffff) {
            {
              characters.append((char)valueToken);
            }
          } else if (valueToken <= 0x10ffff) {
            characters.append((char)((((valueToken - 0x10000) >> 10) &
              0x3ff) | 0xd800));
            characters.append((char)(((valueToken - 0x10000) & 0x3ff) |
              0xdc00));
          }
          continue;
        }
        if (valueToken == TOKEN_EOF) {
          break;
        }
        if ((valueToken & TOKEN_TYPE_MASK) == TOKEN_START_TAG) {
          StartTagToken tag = (StartTagToken)this.GetToken(valueToken);
          List<Attr> attributes = tag.GetAttributes();
          var stlen = 2 + (attributes.size() * 2);
          if (tag.IsSelfClosing()) {
            ++stlen;
          }
          String[] tagarray = new String[stlen];
          tagarray[0] = "StartTag";
          tagarray[1] = tag.GetName();
          int index = 2;
          for (Attr attribute : attributes) {
            tagarray[index] = attribute.GetName();
            tagarray[index + 1] = attribute.GetValue();
            index += 2;
          }
          if (tag.IsSelfClosing()) {
            tagarray[index] = "true";
          }
          ret.add(tagarray);
          continue;
        }
        if ((valueToken & TOKEN_TYPE_MASK) == TOKEN_END_TAG) {
          EndTagToken tag = (EndTagToken)this.GetToken(valueToken);
          ret.add(new String[] { "EndTag", tag.GetName() });
          continue;
        }
        if ((valueToken & TOKEN_TYPE_MASK) == TOKEN_DOCTYPE) {
          DocTypeToken tag = (DocTypeToken)this.GetToken(valueToken);
          StringBuilder doctypeNameBuilder = tag.getName();
          StringBuilder doctypePublicBuilder = tag.getValuePublicID();
          StringBuilder doctypeSystemBuilder = tag.getValueSystemID();
          String doctypeName = (doctypeNameBuilder == null) ? "" :
            doctypeNameBuilder.toString();
          String doctypePublic = (doctypePublicBuilder == null) ? null : doctypePublicBuilder.toString();
          String doctypeSystem = (doctypeSystemBuilder == null) ? null : doctypeSystemBuilder.toString();
          ret.add(new String[] {
            "DOCTYPE", doctypeName, doctypePublic, doctypeSystem,
            tag.getForceQuirks() ? "false" : "true",
          });
          continue;
        }
        if ((valueToken & TOKEN_TYPE_MASK) == TOKEN_COMMENT) {
          CommentToken tag = (CommentToken)this.GetToken(valueToken);
          StringBuilder cv = tag.getCommentValue();
          ret.add(new String[] { "Comment", cv.toString() });
          continue;
        }
        throw new IllegalStateException();
      }
      return ret;
    }

    int ParserRead() {
      int valueToken = this.ParserReadInternal();
      // System.out.println("valueToken=%08X.get(%c)",valueToken,valueToken&0xFF);
      if (valueToken <= -2) {
        this.ParseError();
        return 0xfffd;
      }
      return valueToken;
    }

    private int ParserReadInternal() {
      if (this.tokenQueue.size() > 0) {
        return RemoveAtIndex(this.tokenQueue, 0);
      }
      while (true) {
        // System.out.println("" + state);
        switch (this.state) {
          case Data:
            int c = this.charInput.ReadChar();
            if (c == 0x26) {
              this.state = TokenizerState.CharacterRefInData;
            } else if (c == 0x3c) {
              this.state = TokenizerState.TagOpen;
            } else if (c == 0) {
              this.error = true;
              return c;
            } else if (c < 0) {
              return TOKEN_EOF;
            } else {
              int ret = c;
              // Keep reading characters to
              // reduce the need to re-call
              // this method
              int mark = this.charInput.SetSoftMark();
              for (int i = 0; i < 100; ++i) {
                c = this.charInput.ReadChar();
                if (c > 0 && c != 0x26 && c != 0x3c) {
                  this.tokenQueue.add(c);
                } else {
                  this.charInput.SetMarkPosition(mark + i);
                  break;
                }
              }
              return ret;
            }
            break;
          case CharacterRefInData: {
            this.state = TokenizerState.Data;
            int charref = this.ParseCharacterReference(-1);
            if (charref < 0) {
              // more than one character in this reference
              int index = Math.abs(charref + 1);
              this.tokenQueue.add(HtmlEntities.GetEntityDoubles()[(index * 2) +
                1]);
              return HtmlEntities.GetEntityDoubles()[index * 2];
            }
            return charref;
          }
          case CharacterRefInRcData: {
            this.state = TokenizerState.RcData;
            int charref = this.ParseCharacterReference(-1);
            if (charref < 0) {
              // more than one character in this reference
              int index = Math.abs(charref + 1);
              this.tokenQueue.add(HtmlEntities.GetEntityDoubles()[(index * 2) +
                1]);
              return HtmlEntities.GetEntityDoubles()[index * 2];
            }
            return charref;
          }
          case RcData:
            int c1 = this.charInput.ReadChar();
            if (c1 == 0x26) {
              this.state = TokenizerState.CharacterRefInRcData;
            } else if (c1 == 0x3c) {
              this.state = TokenizerState.RcDataLessThan;
            } else if (c1 == 0) {
              this.error = true;
              return 0xfffd;
            } else if (c1 < 0) {
              return TOKEN_EOF;
            } else {
              return c1;
            }
            break;
          case RawText:
          case ScriptData: {
            int c11 = this.charInput.ReadChar();
            if (c11 == 0x3c) {
              this.state = (this.state == TokenizerState.RawText) ?
                TokenizerState.RawTextLessThan :
                TokenizerState.ScriptDataLessThan;
            } else if (c11 == 0) {
              this.ParseError();
              return 0xfffd;
            } else if (c11 < 0) {
              return TOKEN_EOF;
            } else {
              return c11;
            }
            break;
          }
          case ScriptDataLessThan: {
            this.charInput.SetHardMark();
            int c11 = this.charInput.ReadChar();
            if (c11 == 0x2f) {
              this.tempBuilder.delete(0, this.tempBuilder.length());
              this.state = TokenizerState.ScriptDataEndTagOpen;
            } else if (c11 == 0x21) {
              this.state = TokenizerState.ScriptDataEscapeStart;
              this.tokenQueue.add(0x21);
              return '<';
            } else {
              this.state = TokenizerState.ScriptData;
              if (c11 >= 0) {
                this.charInput.MoveBack(1);
              }
              return 0x3c;
            }
            break;
          }
          case ScriptDataEndTagOpen:
          case ScriptDataEscapedEndTagOpen: {
            this.charInput.SetHardMark();
            int ch = this.charInput.ReadChar();
            if (ch >= 'A' && ch <= 'Z') {
              EndTagToken valueToken = new EndTagToken((char)(ch + 0x20));
              if (ch <= 0xffff) {
                this.tempBuilder.append((char)ch);
              } else if (ch <= 0x10ffff) {
                this.tempBuilder.append((char)((((ch - 0x10000) >> 10) &
                  0x3ff) | 0xd800));
                this.tempBuilder.append((char)(((ch - 0x10000) & 0x3ff) |
                  0xdc00));
              }
              this.currentTag = valueToken;
              this.currentEndTag = valueToken;
              this.state = (this.state == TokenizerState.ScriptDataEndTagOpen) ?
                TokenizerState.ScriptDataEndTagName :
                TokenizerState.ScriptDataEscapedEndTagName;
            } else if (ch >= 'a' && ch <= 'z') {
              EndTagToken valueToken = new EndTagToken((char)ch);
              if (ch <= 0xffff) {
                this.tempBuilder.append((char)ch);
              } else if (ch <= 0x10ffff) {
                this.tempBuilder.append((char)((((ch - 0x10000) >> 10) &
                  0x3ff) | 0xd800));
                this.tempBuilder.append((char)(((ch - 0x10000) & 0x3ff) |
                  0xdc00));
              }
              this.currentTag = valueToken;
              this.currentEndTag = valueToken;
              this.state = (
                  this.state == TokenizerState.ScriptDataEndTagOpen) ?
                TokenizerState.ScriptDataEndTagName :
                TokenizerState.ScriptDataEscapedEndTagName;
            } else {
              this.state = (this.state ==
                  TokenizerState.ScriptDataEndTagOpen) ?
                TokenizerState.ScriptData : TokenizerState.ScriptDataEscaped;
              this.tokenQueue.add(0x2f);
              if (ch >= 0) {
                this.charInput.MoveBack(1);
              }
              return 0x3c;
            }
            break;
          }
          case ScriptDataEndTagName:
          case ScriptDataEscapedEndTagName: {
            this.charInput.SetHardMark();
            int ch = this.charInput.ReadChar();
            if ((ch == 0x09 || ch == 0x0a || ch == 0x0c || ch == 0x20) &&
              this.IsAppropriateEndTag()) {
              this.state = TokenizerState.BeforeAttributeName;
            } else if (ch == 0x2f && this.IsAppropriateEndTag()) {
              this.state = TokenizerState.SelfClosingStartTag;
            } else if (ch == 0x3e && this.IsAppropriateEndTag()) {
              this.state = TokenizerState.Data;
              return this.EmitCurrentTag();
            } else if (ch >= 'A' && ch <= 'Z') {
              this.currentTag.AppendChar((char)(ch + 0x20));
              this.tempBuilder.append((char)ch);
            } else if (ch >= 'a' && ch <= 'z') {
              this.currentTag.AppendChar((char)ch);
              this.tempBuilder.append((char)ch);
            } else {
              this.state = (this.state ==
                  TokenizerState.ScriptDataEndTagName) ?
                TokenizerState.ScriptData : TokenizerState.ScriptDataEscaped;
              this.tokenQueue.add(0x2f);
              String tbs = this.tempBuilder.toString();
              for (int i = 0; i < tbs.length(); ++i) {
                int c2 = com.upokecenter.util.DataUtilities.CodePointAt(tbs, i);
                if (c2 >= 0x10000) {
                  ++i;
                }
                this.tokenQueue.add(c2);
              }
              if (ch >= 0) {
                this.charInput.MoveBack(1);
              }
              return '<';
            }
            break;
          }
          case ScriptDataDoubleEscapeStart: {
            this.charInput.SetHardMark();
            int ch = this.charInput.ReadChar();
            if (ch == 0x09 || ch == 0x0a || ch == 0x0c || ch == 0x20 ||
              ch == 0x2f || ch == 0x3e) {
              String bufferString = this.tempBuilder.toString();
              this.state = bufferString.equals("script") ?
                TokenizerState.ScriptDataDoubleEscaped :
                TokenizerState.ScriptDataEscaped;
              return ch;
            } else if (ch >= 'A' && ch <= 'Z') {
              if (ch + 0x20 <= 0xffff) {
                this.tempBuilder.append((char)(ch + 0x20));
              } else if (ch + 0x20 <= 0x10ffff) {
                this.tempBuilder.append((char)((((ch + 0x20 - 0x10000) >>
                  10) & 0x3ff) | 0xd800));
                this.tempBuilder.append((char)(((ch + 0x20 - 0x10000) &
                  0x3ff) | 0xdc00));
              }
              return ch;
            } else if (ch >= 'a' && ch <= 'z') {
              if (ch <= 0xffff) {
                this.tempBuilder.append((char)ch);
              } else if (ch <= 0x10ffff) {
                this.tempBuilder.append((char)((((ch - 0x10000) >> 10) &
                  0x3ff) | 0xd800));
                this.tempBuilder.append((char)(((ch - 0x10000) & 0x3ff) |
                  0xdc00));
              }
              return ch;
            } else {
              this.state = TokenizerState.ScriptDataEscaped;
              if (ch >= 0) {
                this.charInput.MoveBack(1);
              }
            }
            break;
          }
          case ScriptDataDoubleEscapeEnd: {
            this.charInput.SetHardMark();
            int ch = this.charInput.ReadChar();
            if (ch == 0x09 || ch == 0x0a || ch == 0x0c || ch == 0x20 ||
              ch == 0x2f || ch == 0x3e) {
              String bufferString = this.tempBuilder.toString();
              this.state = bufferString.equals("script") ? TokenizerState.ScriptDataEscaped :
                TokenizerState.ScriptDataDoubleEscaped;
              return ch;
            } else if (ch >= 'A' && ch <= 'Z') {
              if (ch + 0x20 <= 0xffff) {
                this.tempBuilder.append((char)(ch + 0x20));
              } else if (ch + 0x20 <= 0x10ffff) {
                this.tempBuilder.append((char)((((ch + 0x20 - 0x10000) >>
                  10) & 0x3ff) | 0xd800));
                this.tempBuilder.append((char)(((ch + 0x20 - 0x10000) &
                  0x3ff) | 0xdc00));
              }
              return ch;
            } else if (ch >= 'a' && ch <= 'z') {
              if (ch <= 0xffff) {
                this.tempBuilder.append((char)ch);
              } else if (ch <= 0x10ffff) {
                this.tempBuilder.append((char)((((ch - 0x10000) >> 10) &
                  0x3ff) | 0xd800));
                this.tempBuilder.append((char)(((ch - 0x10000) & 0x3ff) |
                  0xdc00));
              }
              return ch;
            } else {
              this.state = TokenizerState.ScriptDataDoubleEscaped;
              if (ch >= 0) {
                this.charInput.MoveBack(1);
              }
            }
            break;
          }
          case ScriptDataEscapeStart:
          case ScriptDataEscapeStartDash: {
            this.charInput.SetHardMark();
            int ch = this.charInput.ReadChar();
            if (ch == 0x2d) {
              this.state = (this.state ==
                  TokenizerState.ScriptDataEscapeStart) ?
                TokenizerState.ScriptDataEscapeStartDash :
                TokenizerState.ScriptDataEscapedDashDash;
              return '-';
            } else {
              if (ch >= 0) {
                this.charInput.MoveBack(1);
              }
              this.state = TokenizerState.ScriptData;
            }
            break;
          }
          case ScriptDataEscaped: {
            int ch = this.charInput.ReadChar();
            if (ch == 0x2d) {
              this.state = TokenizerState.ScriptDataEscapedDash;
              return '-';
            } else if (ch == 0x3c) {
              this.state = TokenizerState.ScriptDataEscapedLessThan;
            } else if (ch == 0) {
              this.ParseError();
              return 0xfffd;
            } else if (ch < 0) {
              this.ParseError();
              this.state = TokenizerState.Data;
            } else {
              return ch;
            }
            break;
          }
          case ScriptDataDoubleEscaped: {
            int ch = this.charInput.ReadChar();
            if (ch == 0x2d) {
              this.state = TokenizerState.ScriptDataDoubleEscapedDash;
              return '-';
            } else if (ch == 0x3c) {
              this.state = TokenizerState.ScriptDataDoubleEscapedLessThan;
              return '<';
            } else if (ch == 0) {
              this.ParseError();
              return 0xfffd;
            } else if (ch < 0) {
              this.ParseError();
              this.state = TokenizerState.Data;
            } else {
              return ch;
            }
            break;
          }
          case ScriptDataEscapedDash: {
            int ch = this.charInput.ReadChar();
            if (ch == 0x2d) {
              this.state = TokenizerState.ScriptDataEscapedDashDash;
              return '-';
            } else if (ch == 0x3c) {
              this.state = TokenizerState.ScriptDataEscapedLessThan;
            } else if (ch == 0) {
              this.ParseError();
              this.state = TokenizerState.ScriptDataEscaped;
              return 0xfffd;
            } else if (ch < 0) {
              this.ParseError();
              this.state = TokenizerState.Data;
            } else {
              this.state = TokenizerState.ScriptDataEscaped;
              return ch;
            }
            break;
          }
          case ScriptDataDoubleEscapedDash: {
            int ch = this.charInput.ReadChar();
            if (ch == 0x2d) {
              this.state = TokenizerState.ScriptDataDoubleEscapedDashDash;
              return '-';
            } else if (ch == 0x3c) {
              this.state = TokenizerState.ScriptDataDoubleEscapedLessThan;
              return '<';
            } else if (ch == 0) {
              this.ParseError();
              this.state = TokenizerState.ScriptDataDoubleEscaped;
              return 0xfffd;
            } else if (ch < 0) {
              this.ParseError();
              this.state = TokenizerState.Data;
            } else {
              this.state = TokenizerState.ScriptDataDoubleEscaped;
              return ch;
            }
            break;
          }
          case ScriptDataEscapedDashDash: {
            int ch = this.charInput.ReadChar();
            if (ch == 0x2d) {
              return '-';
            } else if (ch == 0x3c) {
              this.state = TokenizerState.ScriptDataEscapedLessThan;
            } else if (ch == 0x3e) {
              this.state = TokenizerState.ScriptData;
              return '>';
            } else if (ch == 0) {
              this.ParseError();
              this.state = TokenizerState.ScriptDataEscaped;
              return 0xfffd;
            } else if (ch < 0) {
              this.ParseError();
              this.state = TokenizerState.Data;
            } else {
              this.state = TokenizerState.ScriptDataEscaped;
              return ch;
            }
            break;
          }
          case ScriptDataDoubleEscapedDashDash: {
            int ch = this.charInput.ReadChar();
            if (ch == 0x2d) {
              return '-';
            } else if (ch == 0x3c) {
              this.state = TokenizerState.ScriptDataDoubleEscapedLessThan;
              return '<';
            } else if (ch == 0x3e) {
              this.state = TokenizerState.ScriptData;
              return '>';
            } else if (ch == 0) {
              this.ParseError();
              this.state = TokenizerState.ScriptDataDoubleEscaped;
              return 0xfffd;
            } else if (ch < 0) {
              this.ParseError();
              this.state = TokenizerState.Data;
            } else {
              this.state = TokenizerState.ScriptDataDoubleEscaped;
              return ch;
            }
            break;
          }
          case ScriptDataDoubleEscapedLessThan: {
            this.charInput.SetHardMark();
            int ch = this.charInput.ReadChar();
            if (ch == 0x2f) {
              this.tempBuilder.delete(0, this.tempBuilder.length());
              this.state = TokenizerState.ScriptDataDoubleEscapeEnd;
              return 0x2f;
            } else {
              this.state = TokenizerState.ScriptDataDoubleEscaped;
              if (ch >= 0) {
                this.charInput.MoveBack(1);
              }
            }
            break;
          }
          case ScriptDataEscapedLessThan: {
            this.charInput.SetHardMark();
            int ch = this.charInput.ReadChar();
            if (ch == 0x2f) {
              this.tempBuilder.delete(0, this.tempBuilder.length());
              this.state = TokenizerState.ScriptDataEscapedEndTagOpen;
            } else if (ch >= 'A' && ch <= 'Z') {
              this.tempBuilder.delete(0, this.tempBuilder.length());
              this.tempBuilder.append((char)(ch + 0x20));
              this.state = TokenizerState.ScriptDataDoubleEscapeStart;
              this.tokenQueue.add(ch);
              return 0x3c;
            } else if (ch >= 'a' && ch <= 'z') {
              this.tempBuilder.delete(0, this.tempBuilder.length());
              this.tempBuilder.append((char)ch);
              this.state = TokenizerState.ScriptDataDoubleEscapeStart;
              this.tokenQueue.add(ch);
              return 0x3c;
            } else {
              this.state = TokenizerState.ScriptDataEscaped;
              if (ch >= 0) {
                this.charInput.MoveBack(1);
              }
              return 0x3c;
            }
            break;
          }
          case PlainText: {
            int c11 = this.charInput.ReadChar();
            if (c11 == 0) {
              this.ParseError();
              return 0xfffd;
            } else if (c11 < 0) {
              return TOKEN_EOF;
            } else {
              return c11;
            }
          }
          case TagOpen: {
            this.charInput.SetHardMark();
            int c11 = this.charInput.ReadChar();
            // System.out.println("In tagopen " + ((char)c11));
            if (c11 == 0x21) {
              this.state = TokenizerState.MarkupDeclarationOpen;
            } else if (c11 == 0x2f) {
              this.state = TokenizerState.EndTagOpen;
            } else if (c11 >= 'A' && c11 <= 'Z') {
              TagToken valueToken = new StartTagToken((char)(c11 + 0x20));
              this.currentTag = valueToken;
              this.state = TokenizerState.TagName;
            } else if (c11 >= 'a' && c11 <= 'z') {
              TagToken valueToken = new StartTagToken((char)c11);
              this.currentTag = valueToken;
              this.state = TokenizerState.TagName;
            } else if (c11 == 0x3f) {
              this.ParseError();
              this.bogusCommentCharacter = c11;
              this.state = TokenizerState.BogusComment;
            } else {
              this.ParseError();
              this.state = TokenizerState.Data;
              if (c11 >= 0) {
                this.charInput.MoveBack(1);
              }
              return '<';
            }
            break;
          }
          case EndTagOpen: {
            int ch = this.charInput.ReadChar();
            if (ch >= 'A' && ch <= 'Z') {
              TagToken valueToken = new EndTagToken((char)(ch + 0x20));
              this.currentEndTag = valueToken;
              this.currentTag = valueToken;
              this.state = TokenizerState.TagName;
            } else if (ch >= 'a' && ch <= 'z') {
              TagToken valueToken = new EndTagToken((char)ch);
              this.currentEndTag = valueToken;
              this.currentTag = valueToken;
              this.state = TokenizerState.TagName;
            } else if (ch == 0x3e) {
              this.ParseError();
              this.state = TokenizerState.Data;
            } else if (ch < 0) {
              this.ParseError();
              this.state = TokenizerState.Data;
              this.tokenQueue.add(0x2f); // solidus
              return 0x3c; // Less than
            } else {
              this.ParseError();
              this.bogusCommentCharacter = ch;
              this.state = TokenizerState.BogusComment;
            }
            break;
          }
          case RcDataEndTagOpen:
          case RawTextEndTagOpen: {
            this.charInput.SetHardMark();
            int ch = this.charInput.ReadChar();
            if (ch >= 'A' && ch <= 'Z') {
              TagToken valueToken = new EndTagToken((char)(ch + 0x20));
              if (ch <= 0xffff) {
                this.tempBuilder.append((char)ch);
              } else if (ch <= 0x10ffff) {
                this.tempBuilder.append((char)((((ch - 0x10000) >> 10) &
                  0x3ff) | 0xd800));
                this.tempBuilder.append((char)(((ch - 0x10000) & 0x3ff) |
                  0xdc00));
              }
              this.currentEndTag = valueToken;
              this.currentTag = valueToken;
              this.state = (this.state == TokenizerState.RcDataEndTagOpen) ?
                TokenizerState.RcDataEndTagName :
                TokenizerState.RawTextEndTagName;
            } else if (ch >= 'a' && ch <= 'z') {
              TagToken valueToken = new EndTagToken((char)ch);
              if (ch <= 0xffff) {
                this.tempBuilder.append((char)ch);
              } else if (ch <= 0x10ffff) {
                this.tempBuilder.append((char)((((ch - 0x10000) >> 10) &
                  0x3ff) | 0xd800));
                this.tempBuilder.append((char)(((ch - 0x10000) & 0x3ff) |
                  0xdc00));
              }
              this.currentEndTag = valueToken;
              this.currentTag = valueToken;
              this.state = (
                  this.state == TokenizerState.RcDataEndTagOpen) ?
                TokenizerState.RcDataEndTagName :
                TokenizerState.RawTextEndTagName;
            } else {
              if (ch >= 0) {
                this.charInput.MoveBack(1);
              }
              this.state = TokenizerState.RcData;
              this.tokenQueue.add(0x2f); // solidus
              return 0x3c; // Less than
            }
            break;
          }
          case RcDataEndTagName:
          case RawTextEndTagName: {
            this.charInput.SetHardMark();
            int ch = this.charInput.ReadChar();
            if ((ch == 0x09 || ch == 0x0a || ch == 0x0c || ch == 0x20) &&
              this.IsAppropriateEndTag()) {
              this.state = TokenizerState.BeforeAttributeName;
            } else if (ch == 0x2f && this.IsAppropriateEndTag()) {
              this.state = TokenizerState.SelfClosingStartTag;
            } else if (ch == 0x3e && this.IsAppropriateEndTag()) {
              this.state = TokenizerState.Data;
              return this.EmitCurrentTag();
            } else if (ch >= 'A' && ch <= 'Z') {
              this.currentTag.AppendUChar(ch + 0x20);
              if (ch + 0x20 <= 0xffff) {
                this.tempBuilder.append((char)(ch + 0x20));
              } else if (ch + 0x20 <= 0x10ffff) {
                this.tempBuilder.append((char)((((ch + 0x20 - 0x10000) >>
                  10) & 0x3ff) | 0xd800));
                this.tempBuilder.append((char)(((ch + 0x20 - 0x10000) &
                  0x3ff) | 0xdc00));
              }
            } else if (ch >= 'a' && ch <= 'z') {
              this.currentTag.AppendUChar(ch);
              if (ch <= 0xffff) {
                this.tempBuilder.append((char)ch);
              } else if (ch <= 0x10ffff) {
                this.tempBuilder.append((char)((((ch - 0x10000) >> 10) &
                  0x3ff) | 0xd800));
                this.tempBuilder.append((char)(((ch - 0x10000) & 0x3ff) |
                  0xdc00));
              }
            } else {
              if (ch >= 0) {
                this.charInput.MoveBack(1);
              }
              this.state = (this.state == TokenizerState.RcDataEndTagName) ?
                TokenizerState.RcData : TokenizerState.RawText;
              this.tokenQueue.add(0x2f); // solidus
              String tbs = this.tempBuilder.toString();
              for (int i = 0; i < tbs.length(); ++i) {
                int c2 = com.upokecenter.util.DataUtilities.CodePointAt(tbs, i);
                if (c2 >= 0x10000) {
                  ++i;
                }
                this.tokenQueue.add(c2);
              }
              return 0x3c; // Less than
            }
            break;
          }
          case BeforeAttributeName: {
            int ch = this.charInput.ReadChar();
            if (ch == 0x09 || ch == 0x0a || ch == 0x0c || ch == 0x20) {
              // ignored
            } else if (ch == 0x2f) {
              this.state = TokenizerState.SelfClosingStartTag;
            } else if (ch == 0x3e) {
              this.state = TokenizerState.Data;
              return this.EmitCurrentTag();
            } else if (ch >= 'A' && ch <= 'Z') {
              this.currentAttribute = this.currentTag.AddAttribute((char)(ch +
                0x20));
              this.state = TokenizerState.AttributeName;
            } else if (ch == 0) {
              this.ParseError();
              this.currentAttribute =
                this.currentTag.AddAttribute((char)0xfffd);
              this.state = TokenizerState.AttributeName;
            } else if (ch < 0) {
              this.ParseError();
              this.state = TokenizerState.Data;
            } else {
              if (ch == 0x22 || ch == 0x27 || ch == 0x3c || ch == 0x3d) {
                this.ParseError();
              }
              this.currentAttribute = this.currentTag.AddAttribute(ch);
              this.state = TokenizerState.AttributeName;
            }
            break;
          }
          case AttributeName: {
            int ch = this.charInput.ReadChar();
            if (ch == 0x09 || ch == 0x0a || ch == 0x0c || ch == 0x20) {
              if (!this.currentTag.CheckAttributeName()) {
                this.ParseError();
              }
              this.state = TokenizerState.AfterAttributeName;
            } else if (ch == 0x2f) {
              if (!this.currentTag.CheckAttributeName()) {
                this.ParseError();
              }
              this.state = TokenizerState.SelfClosingStartTag;
            } else if (ch == 0x3d) {
              if (!this.currentTag.CheckAttributeName()) {
                this.ParseError();
              }
              this.state = TokenizerState.BeforeAttributeValue;
            } else if (ch == 0x3e) {
              if (!this.currentTag.CheckAttributeName()) {
                this.ParseError();
              }
              this.state = TokenizerState.Data;
              return this.EmitCurrentTag();
            } else if (ch >= 'A' && ch <= 'Z') {
              this.currentAttribute.AppendToName(ch + 0x20);
            } else if (ch == 0) {
              this.ParseError();
              this.currentAttribute.AppendToName(0xfffd);
            } else if (ch < 0) {
              this.ParseError();
              if (!this.currentTag.CheckAttributeName()) {
                this.ParseError();
              }
              this.state = TokenizerState.Data;
            } else if (ch == 0x22 || ch == 0x27 || ch == 0x3c) {
              this.ParseError();
              this.currentAttribute.AppendToName(ch);
            } else {
              this.currentAttribute.AppendToName(ch);
            }
            break;
          }
          case AfterAttributeName: {
            int ch = this.charInput.ReadChar();
            while (ch == 0x09 || ch == 0x0a || ch == 0x0c || ch == 0x20) {
              ch = this.charInput.ReadChar();
            }
            if (ch == 0x2f) {
              this.state = TokenizerState.SelfClosingStartTag;
            } else if (ch == '=') {
              this.state = TokenizerState.BeforeAttributeValue;
            } else if (ch == '>') {
              this.state = TokenizerState.Data;
              return this.EmitCurrentTag();
            } else if (ch >= 'A' && ch <= 'Z') {
              this.currentAttribute = this.currentTag.AddAttribute((char)(ch +
                0x20));
              this.state = TokenizerState.AttributeName;
            } else if (ch == 0) {
              this.ParseError();
              this.currentAttribute =
                this.currentTag.AddAttribute((char)0xfffd);
              this.state = TokenizerState.AttributeName;
            } else if (ch < 0) {
              this.ParseError();
              this.state = TokenizerState.Data;
            } else {
              if (ch == 0x22 || ch == 0x27 || ch == 0x3c) {
                this.ParseError();
              }
              this.currentAttribute = this.currentTag.AddAttribute(ch);
              this.state = TokenizerState.AttributeName;
            }
            break;
          }
          case BeforeAttributeValue: {
            this.charInput.SetHardMark();
            int ch = this.charInput.ReadChar();
            while (ch == 0x09 || ch == 0x0a || ch == 0x0c || ch == 0x20) {
              ch = this.charInput.ReadChar();
            }
            if (ch == 0x22) {
              this.state = TokenizerState.AttributeValueDoubleQuoted;
            } else if (ch == 0x26) {
              this.charInput.MoveBack(1);
              this.state = TokenizerState.AttributeValueUnquoted;
            } else if (ch == 0x27) {
              this.state = TokenizerState.AttributeValueSingleQuoted;
            } else if (ch == 0) {
              this.ParseError();
              this.currentAttribute.AppendToValue(0xfffd);
              this.state = TokenizerState.AttributeValueUnquoted;
            } else if (ch == 0x3e) {
              this.ParseError();
              this.state = TokenizerState.Data;
              return this.EmitCurrentTag();
            } else if (ch == 0x3c || ch == 0x3d || ch == 0x60) {
              this.ParseError();
              this.currentAttribute.AppendToValue(ch);
              this.state = TokenizerState.AttributeValueUnquoted;
            } else if (ch < 0) {
              this.ParseError();
              this.state = TokenizerState.Data;
            } else {
              this.currentAttribute.AppendToValue(ch);
              this.state = TokenizerState.AttributeValueUnquoted;
            }
            break;
          }
          case AttributeValueDoubleQuoted: {
            int ch = this.charInput.ReadChar();
            if (ch == 0x22) {
              this.currentAttribute.CommitValue();
              this.state = TokenizerState.AfterAttributeValueQuoted;
            } else if (ch == 0x26) {
              this.lastState = this.state;
              this.state = TokenizerState.CharacterRefInAttributeValue;
            } else if (ch == 0) {
              this.ParseError();
              this.currentAttribute.AppendToValue(0xfffd);
            } else if (ch < 0) {
              this.ParseError();
              this.state = TokenizerState.Data;
            } else {
              this.currentAttribute.AppendToValue(ch);
              // Keep reading characters to
              // reduce the need to re-call
              // this method
              int mark = this.charInput.SetSoftMark();
              for (int i = 0; i < 100; ++i) {
                ch = this.charInput.ReadChar();
                if (ch > 0 && ch != 0x26 && ch != 0x22) {
                  this.currentAttribute.AppendToValue(ch);
                } else if (ch == 0x22) {
                  this.currentAttribute.CommitValue();
                  this.state = TokenizerState.AfterAttributeValueQuoted;
                  break;
                } else {
                  this.charInput.SetMarkPosition(mark + i);
                  break;
                }
              }
            }
            break;
          }
          case AttributeValueSingleQuoted: {
            int ch = this.charInput.ReadChar();
            if (ch == 0x27) {
              this.currentAttribute.CommitValue();
              this.state = TokenizerState.AfterAttributeValueQuoted;
            } else if (ch == 0x26) {
              this.lastState = this.state;
              this.state = TokenizerState.CharacterRefInAttributeValue;
            } else if (ch == 0) {
              this.ParseError();
              this.currentAttribute.AppendToValue(0xfffd);
            } else if (ch < 0) {
              this.ParseError();
              this.state = TokenizerState.Data;
            } else {
              this.currentAttribute.AppendToValue(ch);
              // Keep reading characters to
              // reduce the need to re-call
              // this method
              int mark = this.charInput.SetSoftMark();
              for (int i = 0; i < 100; ++i) {
                ch = this.charInput.ReadChar();
                if (ch > 0 && ch != 0x26 && ch != 0x27) {
                  this.currentAttribute.AppendToValue(ch);
                } else if (ch == 0x27) {
                  this.currentAttribute.CommitValue();
                  this.state = TokenizerState.AfterAttributeValueQuoted;
                  break;
                } else {
                  this.charInput.SetMarkPosition(mark + i);
                  break;
                }
              }
            }
            break;
          }
          case AttributeValueUnquoted: {
            int ch = this.charInput.ReadChar();
            if (ch == 0x09 || ch == 0x0a || ch == 0x0c || ch == 0x20) {
              this.currentAttribute.CommitValue();
              this.state = TokenizerState.BeforeAttributeName;
            } else if (ch == 0x26) {
              this.lastState = this.state;
              this.state = TokenizerState.CharacterRefInAttributeValue;
            } else if (ch == 0x3e) {
              this.currentAttribute.CommitValue();
              this.state = TokenizerState.Data;
              return this.EmitCurrentTag();
            } else if (ch == 0) {
              this.ParseError();
              this.currentAttribute.AppendToValue(0xfffd);
            } else if (ch < 0) {
              this.ParseError();
              this.state = TokenizerState.Data;
            } else {
              if (ch == 0x22 || ch == 0x27 || ch == 0x3c || ch == 0x3d || ch
                == 0x60) {
                this.ParseError();
              }
              this.currentAttribute.AppendToValue(ch);
            }
            break;
          }
          case AfterAttributeValueQuoted: {
            int mark = this.charInput.SetSoftMark();
            int ch = this.charInput.ReadChar();
            if (ch == 0x09 || ch == 0x0a || ch == 0x0c || ch == 0x20) {
              this.state = TokenizerState.BeforeAttributeName;
            } else if (ch == 0x2f) {
              this.state = TokenizerState.SelfClosingStartTag;
            } else if (ch == 0x3e) {
              this.state = TokenizerState.Data;
              return this.EmitCurrentTag();
            } else if (ch < 0) {
              this.ParseError();
              this.state = TokenizerState.Data;
            } else {
              this.ParseError();
              this.state = TokenizerState.BeforeAttributeName;
              this.charInput.SetMarkPosition(mark);
            }
            break;
          }
          case SelfClosingStartTag: {
            int mark = this.charInput.SetSoftMark();
            int ch = this.charInput.ReadChar();
            if (ch == 0x3e) {
              this.currentTag.SetSelfClosing(true);
              this.state = TokenizerState.Data;
              return this.EmitCurrentTag();
            } else if (ch < 0) {
              this.ParseError();
              this.state = TokenizerState.Data;
            } else {
              this.ParseError();
              this.state = TokenizerState.BeforeAttributeName;
              this.charInput.SetMarkPosition(mark);
            }
            break;
          }
          case MarkupDeclarationOpen: {
            int mark = this.charInput.SetSoftMark();
            int ch = this.charInput.ReadChar();
            if (ch == '-' && this.charInput.ReadChar() == '-') {
              CommentToken valueToken = new CommentToken();
              this.lastComment = valueToken;
              this.state = TokenizerState.CommentStart;
              break;
            } else if (ch == 'D' || ch == 'd') {
              if (((ch = this.charInput.ReadChar()) == 'o' || ch == 'O') &&
                ((ch = this.charInput.ReadChar()) == 'c' || ch == 'C') &&
                ((ch = this.charInput.ReadChar()) == 't' || ch == 'T') &&
                ((ch = this.charInput.ReadChar()) == 'y' || ch == 'Y') &&
                ((ch = this.charInput.ReadChar()) == 'p' || ch == 'P') &&
                ((ch = this.charInput.ReadChar()) == 'e' || ch == 'E')) {
                this.state = TokenizerState.DocType;
                break;
              }
            } else if (ch == '[' && true) {
              if (this.charInput.ReadChar() == 'C' && this.charInput.ReadChar()
                == 'D' && this.charInput.ReadChar() == 'A' &&
                this.charInput.ReadChar() == 'T' &&
                this.charInput.ReadChar() == 'A' && this.charInput.ReadChar() ==
                '[' && this.GetCurrentNode() != null &&
                !HtmlCommon.HTML_NAMESPACE.equals(this.GetCurrentNode()
                .GetNamespaceURI())) {
                this.state = TokenizerState.CData;
                break;
              }
            }
            this.ParseError();
            this.charInput.SetMarkPosition(mark);
            this.bogusCommentCharacter = -1;
            this.state = TokenizerState.BogusComment;
            break;
          }
          case CommentStart: {
            int ch = this.charInput.ReadChar();
            if (ch == '-') {
              this.state = TokenizerState.CommentStartDash;
            } else if (ch == 0) {
              this.ParseError();
              this.lastComment.AppendChar((char)0xfffd);
              this.state = TokenizerState.Comment;
            } else if (ch == 0x3e || ch < 0) {
              this.ParseError();
              this.state = TokenizerState.Data;
              int ret = this.tokens.size() | this.lastComment.GetTokenType();
              this.AddToken(this.lastComment);
              return ret;
            } else {
              this.lastComment.AppendChar(ch);
              this.state = TokenizerState.Comment;
            }
            break;
          }
          case CommentStartDash: {
            int ch = this.charInput.ReadChar();
            if (ch == '-') {
              this.state = TokenizerState.CommentEnd;
            } else if (ch == 0) {
              this.ParseError();
              this.lastComment.AppendChar('-');
              this.lastComment.AppendChar(0xfffd);
              this.state = TokenizerState.Comment;
            } else if (ch == 0x3e || ch < 0) {
              this.ParseError();
              this.state = TokenizerState.Data;
              int ret = this.tokens.size() | this.lastComment.GetTokenType();
              this.AddToken(this.lastComment);
              return ret;
            } else {
              this.lastComment.AppendChar('-');
              this.lastComment.AppendChar(ch);
              this.state = TokenizerState.Comment;
            }
            break;
          }
          case Comment: {
            int ch = this.charInput.ReadChar();
            if (ch == '-') {
              this.state = TokenizerState.CommentEndDash;
            } else if (ch == 0) {
              this.ParseError();
              this.lastComment.AppendChar(0xfffd);
            } else if (ch < 0) {
              this.ParseError();
              this.state = TokenizerState.Data;
              int ret = this.tokens.size() | this.lastComment.GetTokenType();
              this.AddToken(this.lastComment);
              return ret;
            } else {
              this.lastComment.AppendChar(ch);
            }
            break;
          }
          case CommentEndDash: {
            int ch = this.charInput.ReadChar();
            if (ch == '-') {
              this.state = TokenizerState.CommentEnd;
            } else if (ch == 0) {
              this.ParseError();
              this.lastComment.AppendStr("-\ufffd");
              this.state = TokenizerState.Comment;
            } else if (ch < 0) {
              this.ParseError();
              this.state = TokenizerState.Data;
              int ret = this.tokens.size() | this.lastComment.GetTokenType();
              this.AddToken(this.lastComment);
              return ret;
            } else {
              this.lastComment.AppendChar('-');
              this.lastComment.AppendChar(ch);
              this.state = TokenizerState.Comment;
            }
            break;
          }
          case CommentEnd: {
            int ch = this.charInput.ReadChar();
            if (ch == 0x3e) {
              this.state = TokenizerState.Data;
              int ret = this.tokens.size() | this.lastComment.GetTokenType();
              this.AddToken(this.lastComment);
              return ret;
            } else if (ch == 0) {
              this.ParseError();
              this.lastComment.AppendStr("--\ufffd");
              this.state = TokenizerState.Comment;
            } else if (ch == 0x21) { // --!>
              this.ParseError();
              this.state = TokenizerState.CommentEndBang;
            } else if (ch == 0x2d) {
              this.ParseError();
              this.lastComment.AppendChar('-');
            } else if (ch < 0) {
              this.ParseError();
              this.state = TokenizerState.Data;
              int ret = this.tokens.size() | this.lastComment.GetTokenType();
              this.AddToken(this.lastComment);
              return ret;
            } else {
              this.ParseError();
              this.lastComment.AppendChar('-');
              this.lastComment.AppendChar('-');
              this.lastComment.AppendChar(ch);
              this.state = TokenizerState.Comment;
            }
            break;
          }
          case CommentEndBang: {
            int ch = this.charInput.ReadChar();
            if (ch == 0x3e) {
              this.state = TokenizerState.Data;
              int ret = this.tokens.size() | this.lastComment.GetTokenType();
              this.AddToken(this.lastComment);
              return ret;
            } else if (ch == 0) {
              this.ParseError();
              this.lastComment.AppendStr("--!\ufffd");
              this.state = TokenizerState.Comment;
            } else if (ch == 0x2d) {
              this.lastComment.AppendStr("--!");
              this.state = TokenizerState.CommentEndDash;
            } else if (ch < 0) {
              this.ParseError();
              this.state = TokenizerState.Data;
              int ret = this.tokens.size() | this.lastComment.GetTokenType();
              this.AddToken(this.lastComment);
              return ret;
            } else {
              this.ParseError();
              this.lastComment.AppendStr("--!");
              this.lastComment.AppendChar(ch);
              this.state = TokenizerState.Comment;
            }
            break;
          }
          case CharacterRefInAttributeValue: {
            int allowed = 0x3e;
            if (this.lastState == TokenizerState.AttributeValueDoubleQuoted) {
              allowed = '"';
            }
            if (this.lastState == TokenizerState.AttributeValueSingleQuoted) {
              allowed = '\'';
            }
            int ch = this.ParseCharacterReference(allowed);
            if (ch < 0) {
              // more than one character in this reference
              int index = Math.abs(ch + 1);

              this.currentAttribute.AppendToValue(
                HtmlEntities.GetEntityDoubles()[index
                  * 2]);
              this.currentAttribute.AppendToValue(
                HtmlEntities.GetEntityDoubles()[(index * 2) + 1]);
            } else {
              this.currentAttribute.AppendToValue(ch);
            }
            this.state = this.lastState;
            break;
          }
          case TagName: {
            int ch = this.charInput.ReadChar();
            if (ch == 0x09 || ch == 0x0a || ch == 0x0c || ch == 0x20) {
              this.state = TokenizerState.BeforeAttributeName;
            } else if (ch == 0x2f) {
              this.state = TokenizerState.SelfClosingStartTag;
            } else if (ch == 0x3e) {
              this.state = TokenizerState.Data;
              return this.EmitCurrentTag();
            } else if (ch >= 'A' && ch <= 'Z') {
              this.currentTag.AppendChar((char)(ch + 0x20));
            } else if (ch == 0) {
              this.ParseError();
              this.currentTag.AppendChar((char)0xfffd);
            } else if (ch < 0) {
              this.ParseError();
              this.state = TokenizerState.Data;
            } else {
              this.currentTag.AppendUChar(ch);
            }
            break;
          }
          case RawTextLessThan: {
            this.charInput.SetHardMark();
            int ch = this.charInput.ReadChar();
            if (ch == 0x2f) {
              this.tempBuilder.delete(0, this.tempBuilder.length());
              this.state = TokenizerState.RawTextEndTagOpen;
            } else {
              this.state = TokenizerState.RawText;
              if (ch >= 0) {
                this.charInput.MoveBack(1);
              }
              return 0x3c;
            }
            break;
          }
          case BogusComment: {
            CommentToken comment = new CommentToken();
            if (this.bogusCommentCharacter >= 0) {
              var bogusChar = this.bogusCommentCharacter == 0 ? 0xfffd :
                this.bogusCommentCharacter;
              comment.AppendChar(bogusChar);
            }
            while (true) {
              int ch = this.charInput.ReadChar();
              if (ch < 0 || ch == '>') {
                break;
              }
              if (ch == 0) {
                ch = 0xfffd;
              }
              comment.AppendChar(ch);
            }
            int ret = this.tokens.size() | comment.GetTokenType();
            this.AddToken(comment);
            this.state = TokenizerState.Data;
            return ret;
          }
          case DocType: {
            this.charInput.SetHardMark();
            int ch = this.charInput.ReadChar();
            if (ch == 0x09 || ch == 0x0a || ch == 0x0c || ch == 0x20) {
              this.state = TokenizerState.BeforeDocTypeName;
            } else if (ch < 0) {
              this.ParseError();
              this.state = TokenizerState.Data;
              DocTypeToken valueToken = new DocTypeToken();
              valueToken.setForceQuirks(true);
              int ret = this.tokens.size() | valueToken.GetTokenType();
              this.AddToken(valueToken);
              return ret;
            } else {
              this.ParseError();
              this.charInput.MoveBack(1);
              this.state = TokenizerState.BeforeDocTypeName;
            }
            break;
          }
          case BeforeDocTypeName: {
            int ch = this.charInput.ReadChar();
            if (ch == 0x09 || ch == 0x0a || ch == 0x0c || ch == 0x20) {
              break;
            } else if (ch >= 'A' && ch <= 'Z') {
              this.docTypeToken = new DocTypeToken();
              if (ch + 0x20 <= 0xffff) {
                this.docTypeToken.getName().append((char)(ch +
                  0x20));
              } else if (ch + 0x20 <= 0x10ffff) {
                this.docTypeToken.getName().append((char)((((ch + 0x20 -
                  0x10000) >> 10) & 0x3ff) | 0xd800));
                this.docTypeToken.getName().append((char)(((ch + 0x20 -
                  0x10000) & 0x3ff) | 0xdc00));
              }
              this.state = TokenizerState.DocTypeName;
            } else if (ch == 0) {
              this.ParseError();
              this.docTypeToken = new DocTypeToken();
              this.docTypeToken.getName().append((char)0xfffd);
              this.state = TokenizerState.DocTypeName;
            } else if (ch == 0x3e || ch < 0) {
              this.ParseError();
              this.state = TokenizerState.Data;
              DocTypeToken valueToken = new DocTypeToken();
              valueToken.setForceQuirks(true);
              int ret = this.tokens.size() | valueToken.GetTokenType();
              this.AddToken(valueToken);
              return ret;
            } else {
              this.docTypeToken = new DocTypeToken();
              if (ch <= 0xffff) {
                this.docTypeToken.getName().append((char)ch);
              } else if (ch <= 0x10ffff) {
                this.docTypeToken.getName().append((char)((((ch - 0x10000) >>
                  10) & 0x3ff) | 0xd800));
                this.docTypeToken.getName().append((char)(((ch - 0x10000) &
                  0x3ff) | 0xdc00));
              }
              this.state = TokenizerState.DocTypeName;
            }
            break;
          }
          case DocTypeName: {
            int ch = this.charInput.ReadChar();
            if (ch == 0x09 || ch == 0x0a || ch == 0x0c || ch == 0x20) {
              this.state = TokenizerState.AfterDocTypeName;
            } else if (ch == 0x3e) {
              this.state = TokenizerState.Data;
              int ret = this.tokens.size() | this.docTypeToken.GetTokenType();
              this.AddToken(this.docTypeToken);
              return ret;
            } else if (ch >= 'A' && ch <= 'Z') {
              if (ch + 0x20 <= 0xffff) {
                this.docTypeToken.getName().append((char)(ch +
                  0x20));
              } else if (ch + 0x20 <= 0x10ffff) {
                this.docTypeToken.getName().append((char)((((ch + 0x20 -
                  0x10000) >> 10) & 0x3ff) | 0xd800));
                this.docTypeToken.getName().append((char)(((ch + 0x20 -
                  0x10000) & 0x3ff) | 0xdc00));
              }
            } else if (ch == 0) {
              this.ParseError();
              this.docTypeToken.getName().append((char)0xfffd);
            } else if (ch < 0) {
              this.ParseError();
              this.docTypeToken.setForceQuirks(true);
              this.state = TokenizerState.Data;
              int ret = this.tokens.size() | this.docTypeToken.GetTokenType();
              this.AddToken(this.docTypeToken);
              return ret;
            } else {
              if (ch <= 0xffff) {
                this.docTypeToken.getName().append((char)ch);
              } else if (ch <= 0x10ffff) {
                this.docTypeToken.getName().append((char)((((ch - 0x10000) >>
                  10) & 0x3ff) | 0xd800));
                this.docTypeToken.getName().append((char)(((ch - 0x10000) &
                  0x3ff) | 0xdc00));
              }
            }
            break;
          }
          case AfterDocTypeName: {
            int ch = this.charInput.ReadChar();
            if (ch == 0x09 || ch == 0x0a || ch == 0x0c || ch == 0x20) {
              break;
            } else if (ch == 0x3e) {
              this.state = TokenizerState.Data;
              int ret = this.tokens.size() | this.docTypeToken.GetTokenType();
              this.AddToken(this.docTypeToken);
              return ret;
            } else if (ch < 0) {
              this.ParseError();
              this.docTypeToken.setForceQuirks(true);
              this.state = TokenizerState.Data;
              int ret = this.tokens.size() | this.docTypeToken.GetTokenType();
              this.AddToken(this.docTypeToken);
              return ret;
            } else {
              int ch2 = 0;
              int pos = this.charInput.SetSoftMark();
              if (ch == 'P' || ch == 'p') {
                if (((ch2 = this.charInput.ReadChar()) == 'u' || ch2 == 'U'
) && ((ch2 = this.charInput.ReadChar()) == 'b' || ch2 ==
                    'B') &&
                  ((ch2 = this.charInput.ReadChar()) == 'l' || ch2 == 'L') &&
                  ((ch2 = this.charInput.ReadChar()) == 'i' || ch2 == 'I') &&
                  ((ch2 = this.charInput.ReadChar()) == 'c' || ch2 == 'C')
) {
                  this.state = TokenizerState.AfterDocTypePublic;
                } else {
                  this.ParseError();
                  this.charInput.SetMarkPosition(pos);
                  this.docTypeToken.setForceQuirks(true);
                  this.state = TokenizerState.BogusDocType;
                }
              } else if (ch == 'S' || ch == 's') {
                if (((ch2 = this.charInput.ReadChar()) == 'y' || ch2 == 'Y'
) && ((ch2 = this.charInput.ReadChar()) == 's' || ch2 ==
                    'S') &&
                  ((ch2 = this.charInput.ReadChar()) == 't' || ch2 == 'T') &&
                  ((ch2 = this.charInput.ReadChar()) == 'e' || ch2 == 'E') &&
                  ((ch2 = this.charInput.ReadChar()) == 'm' || ch2 == 'M')
) {
                  this.state = TokenizerState.AfterDocTypeSystem;
                } else {
                  this.ParseError();
                  this.charInput.SetMarkPosition(pos);
                  this.docTypeToken.setForceQuirks(true);
                  this.state = TokenizerState.BogusDocType;
                }
              } else {
                this.ParseError();
                this.charInput.SetMarkPosition(pos);
                this.docTypeToken.setForceQuirks(true);
                this.state = TokenizerState.BogusDocType;
              }
            }
            break;
          }
          case AfterDocTypePublic:
          case BeforeDocTypePublicID: {
            int ch = this.charInput.ReadChar();
            if (ch == 0x09 || ch == 0x0a || ch == 0x0c || ch == 0x20) {
              if (this.state == TokenizerState.AfterDocTypePublic) {
                this.state = TokenizerState.BeforeDocTypePublicID;
              }
            } else if (ch == 0x22) {
              if (this.state == TokenizerState.AfterDocTypePublic) {
                this.ParseError();
              }
              this.state = TokenizerState.DocTypePublicIDDoubleQuoted;
            } else if (ch == 0x27) {
              if (this.state == TokenizerState.AfterDocTypePublic) {
                this.ParseError();
              }
              this.state = TokenizerState.DocTypePublicIDSingleQuoted;
            } else if (ch == 0x3e || ch < 0) {
              this.ParseError();
              this.docTypeToken.setForceQuirks(true);
              this.state = TokenizerState.Data;
              int ret = this.tokens.size() | this.docTypeToken.GetTokenType();
              this.AddToken(this.docTypeToken);
              return ret;
            } else {
              this.ParseError();
              this.docTypeToken.setForceQuirks(true);
              this.state = TokenizerState.BogusDocType;
            }
            break;
          }
          case AfterDocTypeSystem:
          case BeforeDocTypeSystemID: {
            int ch = this.charInput.ReadChar();
            if (ch == 0x09 || ch == 0x0a || ch == 0x0c || ch == 0x20) {
              if (this.state == TokenizerState.AfterDocTypeSystem) {
                this.state = TokenizerState.BeforeDocTypeSystemID;
              }
            } else if (ch == 0x22) {
              if (this.state == TokenizerState.AfterDocTypeSystem) {
                this.ParseError();
              }
              this.state = TokenizerState.DocTypeSystemIDDoubleQuoted;
            } else if (ch == 0x27) {
              if (this.state == TokenizerState.AfterDocTypeSystem) {
                this.ParseError();
              }
              this.state = TokenizerState.DocTypeSystemIDSingleQuoted;
            } else if (ch == 0x3e || ch < 0) {
              this.ParseError();
              this.docTypeToken.setForceQuirks(true);
              this.state = TokenizerState.Data;
              int ret = this.tokens.size() | this.docTypeToken.GetTokenType();
              this.AddToken(this.docTypeToken);
              return ret;
            } else {
              this.ParseError();
              this.docTypeToken.setForceQuirks(true);
              this.state = TokenizerState.BogusDocType;
            }
            break;
          }
          case DocTypePublicIDDoubleQuoted:
          case DocTypePublicIDSingleQuoted: {
            int ch = this.charInput.ReadChar();
            if (ch == (this.state ==
              TokenizerState.DocTypePublicIDDoubleQuoted ?
              0x22 : 0x27)) {
              this.state = TokenizerState.AfterDocTypePublicID;
            } else if (ch == 0) {
              this.ParseError();
              this.docTypeToken.getValuePublicID().append((char)0xfffd);
            } else if (ch == 0x3e || ch < 0) {
              this.ParseError();
              this.docTypeToken.setForceQuirks(true);
              this.state = TokenizerState.Data;
              int ret = this.tokens.size() | this.docTypeToken.GetTokenType();
              this.AddToken(this.docTypeToken);
              return ret;
            } else {
              if (ch <= 0xffff) {
                this.docTypeToken.getValuePublicID().append((char)ch);
              } else if (ch <= 0x10ffff) {
                this.docTypeToken.getValuePublicID().append((char)((((ch -
                  0x10000) >> 10) & 0x3ff) | 0xd800));
                this.docTypeToken.getValuePublicID().append((char)(((ch -
                  0x10000) & 0x3ff) | 0xdc00));
              }
            }
            break;
          }
          case DocTypeSystemIDDoubleQuoted:
          case DocTypeSystemIDSingleQuoted: {
            int ch = this.charInput.ReadChar();
            if (ch == (this.state ==
              TokenizerState.DocTypeSystemIDDoubleQuoted ?
              0x22 : 0x27)) {
              this.state = TokenizerState.AfterDocTypeSystemID;
            } else if (ch == 0) {
              this.ParseError();
              this.docTypeToken.getValueSystemID().append((char)0xfffd);
            } else if (ch == 0x3e || ch < 0) {
              this.ParseError();
              this.docTypeToken.setForceQuirks(true);
              this.state = TokenizerState.Data;
              int ret = this.tokens.size() | this.docTypeToken.GetTokenType();
              this.AddToken(this.docTypeToken);
              return ret;
            } else {
              if (ch <= 0xffff) {
                this.docTypeToken.getValueSystemID().append((char)ch);
              } else if (ch <= 0x10ffff) {
                this.docTypeToken.getValueSystemID().append((char)((((ch -
                  0x10000) >> 10) & 0x3ff) | 0xd800));
                this.docTypeToken.getValueSystemID().append((char)(((ch -
                  0x10000) & 0x3ff) | 0xdc00));
              }
            }
            break;
          }
          case AfterDocTypePublicID:
          case BetweenDocTypePublicAndSystem: {
            int ch = this.charInput.ReadChar();
            if (ch == 0x09 || ch == 0x0a || ch == 0x0c || ch == 0x20) {
              if (this.state == TokenizerState.AfterDocTypePublicID) {
                this.state = TokenizerState.BetweenDocTypePublicAndSystem;
              }
            } else if (ch == 0x3e) {
              this.state = TokenizerState.Data;
              int ret = this.tokens.size() | this.docTypeToken.GetTokenType();
              this.AddToken(this.docTypeToken);
              return ret;
            } else if (ch == 0x22) {
              if (this.state == TokenizerState.AfterDocTypePublicID) {
                this.ParseError();
              }
              this.state = TokenizerState.DocTypeSystemIDDoubleQuoted;
            } else if (ch == 0x27) {
              if (this.state == TokenizerState.AfterDocTypePublicID) {
                this.ParseError();
              }
              this.state = TokenizerState.DocTypeSystemIDSingleQuoted;
            } else if (ch < 0) {
              this.ParseError();
              this.docTypeToken.setForceQuirks(true);
              this.state = TokenizerState.Data;
              int ret = this.tokens.size() | this.docTypeToken.GetTokenType();
              this.AddToken(this.docTypeToken);
              return ret;
            } else {
              this.ParseError();
              this.docTypeToken.setForceQuirks(true);
              this.state = TokenizerState.BogusDocType;
            }
            break;
          }
          case AfterDocTypeSystemID: {
            int ch = this.charInput.ReadChar();
            if (ch == 0x09 || ch == 0x0a || ch == 0x0c || ch == 0x20) {
              break;
            } else if (ch == 0x3e) {
              this.state = TokenizerState.Data;
              int ret = this.tokens.size() | this.docTypeToken.GetTokenType();
              this.AddToken(this.docTypeToken);
              return ret;
            } else if (ch < 0) {
              this.ParseError();
              this.docTypeToken.setForceQuirks(true);
              this.state = TokenizerState.Data;
              int ret = this.tokens.size() | this.docTypeToken.GetTokenType();
              this.AddToken(this.docTypeToken);
              return ret;
            } else {
              this.ParseError();
              this.state = TokenizerState.BogusDocType;
            }
            break;
          }
          case BogusDocType: {
            int ch = this.charInput.ReadChar();
            if (ch == 0x3e || ch < 0) {
              this.state = TokenizerState.Data;
              int ret = this.tokens.size() | this.docTypeToken.GetTokenType();
              this.AddToken(this.docTypeToken);
              return ret;
            }
            break;
          }
          case CData: {
            StringBuilder buffer = new StringBuilder();
            int phase = 0;
            this.state = TokenizerState.Data;
            while (true) {
              int ch = this.charInput.ReadChar();
              if (ch < 0) {
                break;
              }
              if (ch <= 0xffff) {
                buffer.append((char)ch);
              } else if (ch <= 0x10ffff) {
                buffer.append((char)((((ch - 0x10000) >> 10) & 0x3ff) |
                  0xd800));
                buffer.append((char)(((ch - 0x10000) & 0x3ff) | 0xdc00));
              }
              if (phase == 0) {
                if (ch == ']') {
                  ++phase;
                } else {
                  phase = 0;
                }
              } else if (phase == 1) {
                if (ch == ']') {
                  ++phase;
                } else {
                  phase = 0;
                }
              } else if (phase == 2) {
                if (ch == '>') {
                  ++phase;
                  break;
                } else {
                  phase = (ch == ']') ? 2 : 0;
                }
              }
            }
            String str = buffer.toString();
            int size = buffer.length();
            if (phase == 3) {
              if (size < 0) {
                throw new IllegalStateException();
              }
              size -= 3; // don't count the ']]>'
            }
            if (size > 0) {
              // Emit the tokens
              int ret1 = 0;
              for (int i = 0; i < size; ++i) {
                int c2 = com.upokecenter.util.DataUtilities.CodePointAt(str, i);
                if (i > 0) {
                  this.tokenQueue.add(c2);
                } else {
                  ret1 = c2;
                }
                if (c2 >= 0x10000) {
                  ++i;
                }
              }
              return ret1;
            }
            break;
          }
          case RcDataLessThan: {
            this.charInput.SetHardMark();
            int ch = this.charInput.ReadChar();
            if (ch == 0x2f) {
              this.tempBuilder.delete(0, this.tempBuilder.length());
              this.state = TokenizerState.RcDataEndTagOpen;
            } else {
              this.state = TokenizerState.RcData;
              if (ch >= 0) {
                this.charInput.MoveBack(1);
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

    private IElement PopCurrentNode() {
      return (this.openElements.size() > 0) ?
        RemoveAtIndex(this.openElements, this.openElements.size() - 1) : null;
    }

    private void PushFormattingElement(StartTagToken tag) {
      Element valueElement = this.AddHtmlElement(tag);
      int matchingElements = 0;
      int lastMatchingElement = -1;
      String valueName = valueElement.GetLocalName();
      for (int i = this.formattingElements.size() - 1; i >= 0; --i) {
        FormattingElement fe = this.formattingElements.get(i);
        if (fe.IsMarker()) {
          break;
        }
        if (fe.getElement().GetLocalName().equals(valueName) &&
          fe.getElement().GetNamespaceURI().equals(valueElement.GetNamespaceURI())) {
          List<IAttr> attribs = fe.getElement().GetAttributes();
          List<IAttr> myAttribs = valueElement.GetAttributes();
          if (attribs.size() == myAttribs.size()) {
            boolean match = true;
            for (int j = 0; j < myAttribs.size(); ++j) {
              String name1 = myAttribs.get(j).GetName();
              String namespaceValue = myAttribs.get(j).GetNamespaceURI();
              String value = myAttribs.get(j).GetValue();
              String otherValue = fe.getElement().GetAttributeNS(
                  namespaceValue,
                  name1);
              if (otherValue == null || !otherValue.equals(value)) {
                match = false;
              }
            }
            if (match) {
              ++matchingElements;
              lastMatchingElement = i;
            }
          }
        }
      }
      if (matchingElements >= 3) {
        this.formattingElements.remove(lastMatchingElement);
      }
      FormattingElement fe2 = new FormattingElement();
      fe2.setValueMarker(false);
      fe2.setToken(tag);
      fe2.setElement(valueElement);
      this.formattingElements.add(fe2);
    }

    private void ReconstructFormatting() {
      if (this.formattingElements.size() == 0) {
        return;
      }
      // System.out.println("reconstructing elements");
      // System.out.println(formattingElements);
      FormattingElement fe =
        this.formattingElements.get(this.formattingElements.size() - 1);
      if (fe.IsMarker() || this.openElements.contains(fe.getElement())) {
        return;
      }
      int i = this.formattingElements.size() - 1;
      while (i > 0) {
        fe = this.formattingElements.get(i - 1);
        --i;
        if (!fe.IsMarker() && !this.openElements.contains(fe.getElement())) {
          continue;
        }
        ++i;
        break;
      }
      for (int j = i; j < this.formattingElements.size(); ++j) {
        fe = this.formattingElements.get(j);
        Element valueElement = this.AddHtmlElement(fe.getToken());
        fe.setElement(valueElement);
        fe.setValueMarker(false);
      }
    }

    private void RemoveFormattingElement(IElement valueAElement) {
      FormattingElement f = null;
      for (var fe : this.formattingElements) {
        if (!fe.IsMarker() && valueAElement.equals(fe.getElement())) {
          f = fe;
          break;
        }
      }
      if (f != null) {
        this.formattingElements.remove(f);
      }
    }

    private void ResetInsertionMode() {
      boolean last = false;
      for (int i = this.openElements.size() - 1; i >= 0; --i) {
        IElement e = this.openElements.get(i);
        if (this.context != null && i == 0) {
          e = this.context;
          last = true;
        }
        if (!last && (HtmlCommon.IsHtmlElement(e, "th") ||
          HtmlCommon.IsHtmlElement(e, "td"))) {
          this.insertionMode = InsertionMode.InCell;
          break;
        }
        if (HtmlCommon.IsHtmlElement(e, "select")) {
          this.insertionMode = InsertionMode.InSelect;
          if (!last) {
            for (int j = i - 1; j >= 0; --j) {
              e = this.openElements.get(j);
              if (HtmlCommon.IsHtmlElement(e, "template")) {
                break;
              }
              if (HtmlCommon.IsHtmlElement(e, "table")) {
                this.insertionMode = InsertionMode.InSelectInTable;
                break;
              }
            }
          }
          break;
        }
        if (HtmlCommon.IsHtmlElement(e, "colgroup")) {
          this.insertionMode = InsertionMode.InColumnGroup;
          break;
        }
        if (HtmlCommon.IsHtmlElement(e, "tr")) {
          this.insertionMode = InsertionMode.InRow;
          break;
        }
        if (HtmlCommon.IsHtmlElement(e, "caption")) {
          this.insertionMode = InsertionMode.InCaption;
          break;
        }
        if (HtmlCommon.IsHtmlElement(e, "table")) {
          this.insertionMode = InsertionMode.InTable;
          break;
        }
        if (HtmlCommon.IsHtmlElement(e, "template")) {
          this.insertionMode = this.templateModes.get(this.templateModes.size() - 1);
          break;
        }
        if (HtmlCommon.IsHtmlElement(e, "frameset")) {
          this.insertionMode = InsertionMode.InFrameset;
          break;
        }
        if (HtmlCommon.IsHtmlElement(e, "html")) {
          this.insertionMode = (this.headElement == null) ?
            InsertionMode.BeforeHead : InsertionMode.AfterHead;
          break;
        }
        if (HtmlCommon.IsHtmlElement(e, "head")) {
          this.insertionMode = InsertionMode.InHead;
          break;
        }

        if (HtmlCommon.IsHtmlElement(e, "body")) {
          this.insertionMode = InsertionMode.InBody;
          break;
        }
        if (HtmlCommon.IsHtmlElement(e, "thead") ||
          HtmlCommon.IsHtmlElement(e, "tbody") ||
          HtmlCommon.IsHtmlElement(e, "tfoot")) {
          this.insertionMode = InsertionMode.InTableBody;
          break;
        }
        if (last) {
          this.insertionMode = InsertionMode.InBody;
          break;
        }
      }
    }

    void SetCData() {
      this.state = TokenizerState.CData;
    }

    void SetPlainText() {
      this.state = TokenizerState.PlainText;
    }

    void SetRawText() {
      this.state = TokenizerState.RawText;
    }

    void SetRcData() {
      this.state = TokenizerState.RcData;
    }

    private void SkipLineFeed() {
      int mark = this.charInput.SetSoftMark();
      int nextToken = this.charInput.ReadChar();
      if (nextToken == 0x0a) {
        return; // ignore the valueToken if it's 0x0A
      } else if (nextToken == 0x26) { // start of character reference
        int charref = this.ParseCharacterReference(-1);
        if (charref < 0) {
          // more than one character in this reference
          int index = Math.abs(charref + 1);
          this.tokenQueue.add(HtmlEntities.GetEntityDoubles()[index * 2]);
          this.tokenQueue.add(HtmlEntities.GetEntityDoubles()[(index * 2) + 1]);
        } else if (charref == 0x0a) {
          return; // ignore the valueToken
        } else {
          this.tokenQueue.add(charref);
        }
      } else {
        // anything else; reset the input stream
        this.charInput.SetMarkPosition(mark);
      }
    }

    private void StopParsing() {
      this.done = true;
      if (((this.valueDocument.getDefaultLanguage()) == null || (this.valueDocument.getDefaultLanguage()).length() == 0)) {
        String[] contLang = this.contentLanguage;
        if (contLang.length == 1) {
          // set the fallback language if there is
          // only one language defined and no meta valueElement
          // defines the language
          this.valueDocument.setDefaultLanguage(contLang[0]);
        }
      }
      this.valueDocument.setEncoding(this.encoding.GetEncoding());
      String docbase = this.valueDocument.GetBaseURI();
      if (docbase == null || docbase.length() == 0) {
        docbase = this.baseurl;
      } else {
        if (this.baseurl != null && this.baseurl.length() > 0) {
          this.valueDocument.SetBaseURI(
            HtmlCommon.ResolveURL(
              this.valueDocument,
              this.baseurl,
              docbase));
        }
      }
      this.openElements.clear();
      this.formattingElements.clear();
    }
  }
