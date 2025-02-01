package com.upokecenter.rdf;
/*
Written in 2013 by Peter Occil.
Any copyright to this work is released to the Public Domain.
In case this is not possible, this work is also
licensed under the Unlicense: https://unlicense.org/

*/

import java.util.*;

import com.upokecenter.io.*;
import com.upokecenter.util.*;
import com.upokecenter.text.*;

  /**
   * Not documented yet.
   */
  public class TurtleParser implements IRDFParser {
    private static final class TurtleObject {
      public static final int SIMPLE = 0;
      public static final int COLLECTION = 1;
      public static final int PROPERTIES = 2;

      public static TurtleObject FromTerm(RDFTerm term) {
        TurtleObject tobj = new TurtleObject();
        tobj.setTerm(term);
        tobj.setKind(TurtleObject.SIMPLE);
        return tobj;
      }

      public static TurtleObject NewCollection() {
        TurtleObject tobj = new TurtleObject();
        tobj.objects = new ArrayList<TurtleObject>();
        tobj.setKind(TurtleObject.COLLECTION);
        return tobj;
      }

      public static TurtleObject NewPropertyList() {
        TurtleObject tobj = new TurtleObject();
        tobj.properties = new ArrayList<TurtleProperty>();
        tobj.setKind(TurtleObject.PROPERTIES);
        return tobj;
      }

      private List<TurtleObject> objects;

      private List<TurtleProperty> properties;

      public final RDFTerm getTerm() { return propVarterm; }
public final void setTerm(RDFTerm value) { propVarterm = value; }
private RDFTerm propVarterm;
      public final int getKind() { return propVarkind; }
public final void setKind(int value) { propVarkind = value; }
private int propVarkind;

      public List<TurtleObject> GetObjects() {
        return this.objects;
      }

      public List<TurtleProperty> GetProperties() {
        return this.properties;
      }
    }

    private static final class TurtleProperty {
      public final RDFTerm getPred() { return propVarpred; }
public final void setPred(RDFTerm value) { propVarpred = value; }
private RDFTerm propVarpred;
      public final TurtleObject getObj() { return propVarobj; }
public final void setObj(TurtleObject value) { propVarobj = value; }
private TurtleObject propVarobj;
    }

    private Map<String, RDFTerm> bnodeLabels;
    private Map<String, String> namespaces;

    private String baseURI;

    private TurtleObject curSubject;

    private RDFTerm curPredicate;

    private StackableCharacterInput input;
    private int curBlankNode = 0;

    private static String UriToString(java.net.URI baseURI) {
      if (baseURI == null) {
        throw new NullPointerException("baseURI");
      }
      return baseURI.toString();
    }

    /**
     * Initializes a new instance of the {@link com.upokecenter.rdf.TurtleParser}
     * class.
     * @param stream A PeterO.IByteReader object.
     */
    public TurtleParser(IByteReader stream) {
 this(stream, "about:blank");
    }

    /**
     * Initializes a new instance of the {@link com.upokecenter.rdf.TurtleParser}
     * class.
     * @param stream The parameter {@code stream} is an IByteReader object.
     * @param baseURI The parameter {@code baseURI} is an java.net.URI object.
     */
    public TurtleParser(IByteReader stream, java.net.URI baseURI) {
 this(stream, UriToString(baseURI));
    }

    /**
     * Initializes a new instance of the {@link com.upokecenter.rdf.TurtleParser}
     * class.
     * @param str The parameter {@code str} is a text string.
     * @param baseURI The parameter {@code baseURI} is an java.net.URI object.
     */
    public TurtleParser(String str, java.net.URI baseURI) {
 this(str, UriToString(baseURI));
    }

    /**
     * Initializes a new instance of the {@link com.upokecenter.rdf.TurtleParser}
     * class.
     * @param stream A PeterO.IByteReader object.
     * @param baseURI The parameter {@code baseURI} is a text string.
     * @throws NullPointerException The parameter {@code stream} or {@code
     * baseURI} is null.
     * @throws IllegalArgumentException BaseURI has no scheme.
     */
    public TurtleParser(IByteReader stream, String baseURI) {
      if (stream == null) {
        throw new NullPointerException("stream");
      }
      if (baseURI == null) {
        throw new NullPointerException("baseURI");
      }
      if (!URIUtility.HasScheme(baseURI)) {
        throw new IllegalArgumentException("baseURI has no scheme.");
      }
      this.input = new StackableCharacterInput(
        Encodings.GetDecoderInput(Encodings.UTF8, stream));
      this.baseURI = baseURI;
      this.bnodeLabels = new HashMap<String, RDFTerm>();
      this.namespaces = new HashMap<String, String>();
    }

    /**
     * Initializes a new instance of the {@link com.upokecenter.rdf.TurtleParser}
     * class.
     * @param str The parameter {@code str} is a text string.
     */
    public TurtleParser(String str) {
 this(str, "about:blank");
    }

    /**
     * Initializes a new instance of the {@link com.upokecenter.rdf.TurtleParser}
     * class.
     * @param str The parameter {@code str} is a text string.
     * @param baseURI The parameter {@code baseURI} is a text string.
     * @throws NullPointerException The parameter {@code str} or {@code baseURI}
     * is null.
     * @throws IllegalArgumentException BaseURI.
     */
    public TurtleParser(String str, String baseURI) {
      if (str == null) {
        throw new NullPointerException("str");
      }
      if (baseURI == null) {
        throw new NullPointerException("baseURI");
      }
      if (!URIUtility.HasScheme(baseURI)) {
        throw new IllegalArgumentException("baseURI has no scheme");
      }
      this.input = new StackableCharacterInput(
        Encodings.StringToInput(str));
      this.baseURI = baseURI;
      this.bnodeLabels = new HashMap<String, RDFTerm>();
      this.namespaces = new HashMap<String, String>();
    }

    private RDFTerm AllocateBlankNode() {
      ++this.curBlankNode;
      // A period is included so as not to conflict
      // with user-defined blank node labels (this is allowed
      // because the syntax for blank node identifiers is
      // not concretely defined)
      String label = "." + Integer.toString((int)this.curBlankNode);
      RDFTerm node = RDFTerm.FromBlankNode(label);
      this.bnodeLabels.put(label, node);
      return node;
    }

    private static void EmitRDFTriple(
      RDFTerm subj,
      RDFTerm pred,
      RDFTerm obj,
      Set<RDFTriple> triples) {
      RDFTriple triple = new RDFTriple(subj, pred, obj);
      triples.add(triple);
    }

    private void EmitRDFTriple(
      RDFTerm subj,
      RDFTerm pred,
      TurtleObject obj,
      Set<RDFTriple> triples) {
      if (obj.getKind() == TurtleObject.SIMPLE) {
        EmitRDFTriple(subj, pred, obj.getTerm(), triples);
      } else if (obj.getKind() == TurtleObject.PROPERTIES) {
        List<TurtleProperty> props = obj.GetProperties();
        if (props.size() == 0) {
          EmitRDFTriple(subj, pred, this.AllocateBlankNode(), triples);
        } else {
          RDFTerm blank = this.AllocateBlankNode();
          EmitRDFTriple(subj, pred, blank, triples);
          for (int i = 0; i < props.size(); ++i) {
            TurtleProperty prop = props.get(i);
            this.EmitRDFTriple(blank, prop.getPred(), props.get(i).getObj(), triples);
          }
        }
      } else if (obj.getKind() == TurtleObject.COLLECTION) {
        List<TurtleObject> objs = obj.GetObjects();
        if (objs.size() == 0) {
          EmitRDFTriple(subj, pred, RDFTerm.NIL, triples);
        } else {
          RDFTerm curBlank = this.AllocateBlankNode();
          RDFTerm firstBlank = curBlank;
          this.EmitRDFTriple(curBlank, RDFTerm.FIRST, objs.get(0), triples);
          for (int i = 1; i <= objs.size(); ++i) {
            if (i == objs.size()) {
              EmitRDFTriple(curBlank, RDFTerm.REST, RDFTerm.NIL, triples);
            } else {
              RDFTerm nextBlank = this.AllocateBlankNode();
              EmitRDFTriple(curBlank, RDFTerm.REST, nextBlank, triples);
              this.EmitRDFTriple(nextBlank, RDFTerm.FIRST, objs.get(i), triples);
              curBlank = nextBlank;
            }
          }
          EmitRDFTriple(subj, pred, firstBlank, triples);
        }
      }
    }

    private void EmitRDFTriple(
      TurtleObject subj,
      RDFTerm pred,
      TurtleObject obj,
      Set<RDFTriple> triples) {
      if (subj.getKind() == TurtleObject.SIMPLE) {
        this.EmitRDFTriple(subj.getTerm(), pred, obj, triples);
      } else if (subj.getKind() == TurtleObject.PROPERTIES) {
        List<TurtleProperty> props = subj.GetProperties();
        if (props.size() == 0) {
          this.EmitRDFTriple(this.AllocateBlankNode(), pred, obj, triples);
        } else {
          RDFTerm blank = this.AllocateBlankNode();
          this.EmitRDFTriple(blank, pred, obj, triples);
          for (int i = 0; i < props.size(); ++i) {
            this.EmitRDFTriple(blank, props.get(i).getPred(), props.get(i).getObj(), triples);
          }
        }
      } else if (subj.getKind() == TurtleObject.COLLECTION) {
        List<TurtleObject> objs = subj.GetObjects();
        if (objs.size() == 0) {
          this.EmitRDFTriple(RDFTerm.NIL, pred, obj, triples);
        } else {
          RDFTerm curBlank = this.AllocateBlankNode();
          RDFTerm firstBlank = curBlank;
          this.EmitRDFTriple(curBlank, RDFTerm.FIRST, objs.get(0), triples);
          for (int i = 1; i <= objs.size(); ++i) {
            if (i == objs.size()) {
              EmitRDFTriple(curBlank, RDFTerm.REST, RDFTerm.NIL, triples);
            } else {
              RDFTerm nextBlank = this.AllocateBlankNode();
              EmitRDFTriple(curBlank, RDFTerm.REST, nextBlank, triples);
              this.EmitRDFTriple(nextBlank, RDFTerm.FIRST, objs.get(i), triples);
              curBlank = nextBlank;
            }
          }
          this.EmitRDFTriple(firstBlank, pred, obj, triples);
        }
      }
    }

    private RDFTerm FinishStringLiteral(String str) {
      int mark = this.input.SetHardMark();
      int ch = this.input.ReadChar();
      if (ch == '@') {
        return RDFTerm.FromLangString(str, this.ReadLanguageTag());
      } else if (ch == '^' && this.input.ReadChar() == '^') {
        ch = this.input.ReadChar();
        if (ch == '<') {
          return RDFTerm.FromTypedString(str, this.ReadIriReference());
        } else if (ch == ':') { // prefixed name with current prefix
          String scope = this.namespaces.get("");
          if (scope == null) {
            throw new ParserException();
          }
          return RDFTerm.FromTypedString(
              str,
              scope + this.ReadOptionalLocalName());
        } else if (IsNameStartChar(ch)) { // prefix
          String prefix = this.ReadPrefix(ch);
          String scope = this.namespaces.get(prefix);
          if (scope == null) {
            throw new ParserException();
          }
          return RDFTerm.FromTypedString(
              str,
              scope + this.ReadOptionalLocalName());
        } else {
          throw new ParserException();
        }
      } else {
        this.input.SetMarkPosition(mark);
        return RDFTerm.FromTypedString(str);
      }
    }

    private static boolean IsNameChar(int ch) {
      return (ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9') ||
        (ch >= 'A' && ch <= 'Z') || ch == '_' || ch == '-' ||
        ch == 0xb7 || (ch >= 0xc0 && ch <= 0xd6) ||
        (ch >= 0xd8 && ch <= 0xf6) || (ch >= 0xf8 && ch <= 0x37d) ||
        (ch >= 0x37f && ch <= 0x1fff) || (ch >= 0x200c && ch <= 0x200d) ||
        ch == 0x203f || ch == 0x2040 || (ch >= 0x2070 && ch <= 0x218f) ||
        (ch >= 0x2c00 && ch <= 0x2fef) || (ch >= 0x3001 && ch <= 0xd7ff) ||
        (ch >= 0xf900 && ch <= 0xfdcf) || (ch >= 0xfdf0 && ch <= 0xfffd) ||
        (ch >= 0x10000 && ch <= 0xeffff);
    }

    private static boolean IsNameStartChar(int ch) {
      return (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') ||
        (ch >= 0xc0 && ch <= 0xd6) || (ch >= 0xd8 && ch <= 0xf6) ||
        (ch >= 0xf8 && ch <= 0x2ff) || (ch >= 0x370 && ch <= 0x37d) ||
        (ch >= 0x37f && ch <= 0x1fff) || (ch >= 0x200c && ch <= 0x200d) ||
        (ch >= 0x2070 && ch <= 0x218f) || (ch >= 0x2c00 && ch <= 0x2fef) ||
        (ch >= 0x3001 && ch <= 0xd7ff) || (ch >= 0xf900 && ch <= 0xfdcf) ||
        (ch >= 0xfdf0 && ch <= 0xfffd) || (ch >= 0x10000 && ch <= 0xeffff);
    }

    private static boolean IsNameStartCharU(int ch) {
      return (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z') || ch ==
        '_' || (ch >= 0xc0 && ch <= 0xd6) || (ch >= 0xd8 && ch <= 0xf6) ||
        (ch >= 0xf8 && ch <= 0x2ff) || (ch >= 0x370 && ch <= 0x37d) ||
        (ch >= 0x37f && ch <= 0x1fff) || (ch >= 0x200c && ch <= 0x200d) ||
        (ch >= 0x2070 && ch <= 0x218f) || (ch >= 0x2c00 && ch <= 0x2fef) ||
        (ch >= 0x3001 && ch <= 0xd7ff) || (ch >= 0xf900 && ch <= 0xfdcf) ||
        (ch >= 0xfdf0 && ch <= 0xfffd) || (ch >= 0x10000 && ch <= 0xeffff);
    }

    /**
     * Not documented yet.
     * @return An ISet(RDFTriple) object.
     */
    public Set<RDFTriple> Parse() {
      Set<RDFTriple> triples = new HashSet<RDFTriple>();
      while (true) {
        this.SkipWhitespace();
        int mark = this.input.SetHardMark();
        int ch = this.input.ReadChar();
        if (ch < 0) {
          RDFInternal.ReplaceBlankNodes(triples, this.bnodeLabels);
          return triples;
        }
        if (ch == '@') {
          ch = this.input.ReadChar();
          if (ch == 'p' && this.input.ReadChar() == 'r' &&
            this.input.ReadChar() == 'e' &&
            this.input.ReadChar() == 'f' && this.input.ReadChar() == 'i' &&
            this.input.ReadChar() == 'x' && this.SkipWhitespace()) {
            this.ReadPrefixStatement(false);
            continue;
          } else if (ch == 'b' && this.input.ReadChar() == 'a' &&
            this.input.ReadChar() == 's' &&
            this.input.ReadChar() == 'e' && this.SkipWhitespace()) {
            this.ReadBase(false);
            continue;
          } else {
            throw new ParserException();
          }
        } else if (ch == 'b' || ch == 'B') {
          int c2 = 0;
          if (((c2 = this.input.ReadChar()) == 'A' || c2 == 'a') &&
            ((c2 = this.input.ReadChar()) == 'S' || c2 == 's') &&
            ((c2 = this.input.ReadChar()) == 'E' || c2 == 'e') &&
            this.SkipWhitespace()) {
            this.ReadBase(true);
            continue;
          } else {
            this.input.SetMarkPosition(mark);
          }
        } else if (ch == 'p' || ch == 'P') {
          int c2 = 0;
          if (((c2 = this.input.ReadChar()) == 'R' || c2 == 'r') &&
            ((c2 = this.input.ReadChar()) == 'E' || c2 == 'e') &&
            ((c2 = this.input.ReadChar()) == 'F' || c2 == 'f') &&
            ((c2 = this.input.ReadChar()) == 'I' || c2 == 'i') &&
            ((c2 = this.input.ReadChar()) == 'X' || c2 == 'x') &&
            this.SkipWhitespace()) {
            this.ReadPrefixStatement(true);
            continue;
          } else {
            this.input.SetMarkPosition(mark);
          }
        } else {
          this.input.SetMarkPosition(mark);
        }
        this.ReadTriples(triples);
      }
    }

    private void ReadBase(boolean sparql) {
      if (this.input.ReadChar() != '<') {
        throw new ParserException();
      }
      this.baseURI = this.ReadIriReference();
      if (!sparql) {
        this.SkipWhitespace();
        if (this.input.ReadChar() != '.') {
          throw new ParserException();
        }
      } else {
        this.SkipWhitespace();
      }
    }

    private String ReadBlankNodeLabel() {
      StringBuilder ilist = new StringBuilder();
      int startChar = this.input.ReadChar();
      if (!IsNameStartCharU(startChar) &&
        (startChar < '0' || startChar > '9')) {
        throw new ParserException();
      }
      if (startChar <= 0xffff) {
        {
          ilist.append((char)startChar);
        }
      } else if (startChar <= 0x10ffff) {
        ilist.append((char)((((startChar - 0x10000) >> 10) & 0x3ff) |
          0xd800));
        ilist.append((char)(((startChar - 0x10000) & 0x3ff) | 0xdc00));
      }
      boolean lastIsPeriod = false;
      this.input.SetSoftMark();
      while (true) {
        int ch = this.input.ReadChar();
        if (ch == '.') {
          int position = this.input.GetMarkPosition();
          int ch2 = this.input.ReadChar();
          if (!IsNameChar(ch2) && ch2 != ':' && ch2 != '.') {
            this.input.SetMarkPosition(position - 1);
            return ilist.toString();
          } else {
            this.input.MoveBack(1);
          }
          if (ch <= 0xffff) {
            {
              ilist.append((char)ch);
            }
          } else if (ch <= 0x10ffff) {
            ilist.append((char)((((ch - 0x10000) >> 10) & 0x3ff) | 0xd800));
            ilist.append((char)(((ch - 0x10000) & 0x3ff) | 0xdc00));
          }
          lastIsPeriod = true;
        } else if (IsNameChar(ch)) {
          if (ch <= 0xffff) {
            {
              ilist.append((char)ch);
            }
          } else if (ch <= 0x10ffff) {
            ilist.append((char)((((ch - 0x10000) >> 10) & 0x3ff) | 0xd800));
            ilist.append((char)(((ch - 0x10000) & 0x3ff) | 0xdc00));
          }
          lastIsPeriod = false;
        } else {
          if (ch >= 0) {
            this.input.MoveBack(1);
          }
          if (lastIsPeriod) {
            throw new ParserException();
          }
          return ilist.toString();
        }
      }
    }

    private TurtleObject ReadBlankNodePropertyList() {
      TurtleObject obj = TurtleObject.NewPropertyList();
      boolean havePredObject = false;
      while (true) {
        this.SkipWhitespace();
        int ch;
        if (havePredObject) {
          boolean haveSemicolon = false;
          while (true) {
            this.input.SetSoftMark();
            ch = this.input.ReadChar();
            if (ch == ';') {
              this.SkipWhitespace();
              haveSemicolon = true;
            } else {
              if (ch >= 0) {
                this.input.MoveBack(1);
              }
              break;
            }
          }
          if (!haveSemicolon) {
            break;
          }
        }
        RDFTerm pred = this.ReadPredicate();
        if (pred == null) {
          break;
        }
        havePredObject = true;
        this.ReadObjectListToProperties(pred, obj);
      }
      if (this.input.ReadChar() != ']') {
        throw new ParserException();
      }
      return obj;
    }

    private TurtleObject ReadCollection() {
      TurtleObject obj = TurtleObject.NewCollection();
      while (true) {
        this.SkipWhitespace();
        this.input.SetHardMark();
        int ch = this.input.ReadChar();
        if (ch == ')') {
          break;
        } else {
          if (ch >= 0) {
            this.input.MoveBack(1);
          }
          TurtleObject subobj = this.ReadObject(true);
          List<TurtleObject> objects = obj.GetObjects();
          objects.add(subobj);
        }
      }
      return obj;
    }

    private String ReadIriReference() {
      StringBuilder ilist = new StringBuilder();
      while (true) {
        int ch = this.input.ReadChar();
        if (ch < 0) {
          throw new ParserException();
        }
        if (ch == '>') {
          String iriref = ilist.toString();
          // Resolve the IRI reference relative
          // to the _base URI
          iriref = com.upokecenter.util.URIUtility.RelativeResolve(iriref, this.baseURI);
          if (iriref == null) {
            throw new ParserException();
          }
          return iriref;
        } else if (ch == '\\') {
          ch = this.ReadUnicodeEscape(false);
        }
        if (ch <= 0x20 || ((ch & 0x7f) == ch &&
          "><\\\"{}|^`".indexOf((char)ch) >= 0)) {
          throw new ParserException();
        }
        if (ch <= 0xffff) {
          {
            ilist.append((char)ch);
          }
        } else if (ch <= 0x10ffff) {
          ilist.append((char)((((ch - 0x10000) >> 10) & 0x3ff) | 0xd800));
          ilist.append((char)(((ch - 0x10000) & 0x3ff) | 0xdc00));
        }
      }
    }

    private String ReadLanguageTag() {
      StringBuilder ilist = new StringBuilder();
      boolean hyphen = false;
      boolean haveHyphen = false;
      boolean haveString = false;
      this.input.SetSoftMark();
      while (true) {
        int c2 = this.input.ReadChar();
        if (c2 >= 'A' && c2 <= 'Z') {
          if (c2 <= 0xffff) {
            {
              ilist.append((char)c2);
            }
          } else if (c2 <= 0x10ffff) {
            ilist.append((char)((((c2 - 0x10000) >> 10) & 0x3ff) | 0xd800));
            ilist.append((char)(((c2 - 0x10000) & 0x3ff) | 0xdc00));
          }
          haveString = true;
          hyphen = false;
        } else if (c2 >= 'a' && c2 <= 'z') {
          ilist.append((char)c2);
          haveString = true;
          hyphen = false;
        } else if (haveHyphen && (c2 >= '0' && c2 <= '9')) {
          ilist.append((char)c2);
          haveString = true;
          hyphen = false;
        } else if (c2 == '-') {
          if (hyphen || !haveString) {
            throw new ParserException();
          }
          if (c2 <= 0xffff) {
            {
              ilist.append((char)c2);
            }
          } else if (c2 <= 0x10ffff) {
            ilist.append((char)((((c2 - 0x10000) >> 10) & 0x3ff) | 0xd800));
            ilist.append((char)(((c2 - 0x10000) & 0x3ff) | 0xdc00));
          }
          hyphen = true;
          haveHyphen = true;
          haveString = true;
        } else {
          if (c2 >= 0) {
            this.input.MoveBack(1);
          }
          if (hyphen || !haveString) {
            throw new ParserException();
          }
          return ilist.toString();
        }
      }
    }

    // Reads a number literal starting with
    // the specified character (assumes it's plus, minus,
    // a dot, or a digit)
    private RDFTerm ReadNumberLiteral(int ch) {
      // buffer to hold the literal
      StringBuilder ilist = new StringBuilder();
      // include the first character
      if (ch <= 0xffff) {
        {
          ilist.append((char)ch);
        }
      } else if (ch <= 0x10ffff) {
        ilist.append((char)((((ch - 0x10000) >> 10) & 0x3ff) | 0xd800));
        ilist.append((char)(((ch - 0x10000) & 0x3ff) | 0xdc00));
      }
      boolean haveDigits = ch >= '0' && ch <= '9';
      boolean haveDot = ch == '.';
      this.input.SetHardMark();
      while (true) {
        int ch1 = this.input.ReadChar();
        if (haveDigits && (ch1 == 'e' || ch1 == 'E')) {
          // Parse exponent
          if (ch1 <= 0xffff) {
            {
              ilist.append((char)ch1);
            }
          } else if (ch1 <= 0x10ffff) {
            ilist.append((char)((((ch1 - 0x10000) >> 10) & 0x3ff) | 0xd800));
            ilist.append((char)(((ch1 - 0x10000) & 0x3ff) | 0xdc00));
          }
          ch1 = this.input.ReadChar();
          haveDigits = false;
          if (ch1 == '+' || ch1 == '-' || (ch1 >= '0' && ch1 <= '9')) {
            if (ch1 <= 0xffff) {
              {
                ilist.append((char)ch1);
              }
            } else if (ch1 <= 0x10ffff) {
              ilist.append((char)((((ch1 - 0x10000) >> 10) & 0x3ff) |
                0xd800));
              ilist.append((char)(((ch1 - 0x10000) & 0x3ff) | 0xdc00));
            }
            if (ch1 >= '0' && ch1 <= '9') {
              haveDigits = true;
            }
          } else {
            throw new ParserException();
          }
          this.input.SetHardMark();
          while (true) {
            ch1 = this.input.ReadChar();
            if (ch1 >= '0' && ch1 <= '9') {
              haveDigits = true;
              if (ch1 <= 0xffff) {
                {
                  ilist.append((char)ch1);
                }
              } else if (ch1 <= 0x10ffff) {
                ilist.append((char)((((ch1 - 0x10000) >> 10) & 0x3ff) |
                  0xd800));
                ilist.append((char)(((ch1 - 0x10000) & 0x3ff) | 0xdc00));
              }
            } else {
              if (ch1 >= 0) {
                this.input.MoveBack(1);
              }
              if (!haveDigits) {
                throw new ParserException();
              }
              return RDFTerm.FromTypedString(
                  ilist.toString(),
                  "http://www.w3.org/2001/XMLSchema#double");
            }
          }
        } else if (ch1 >= '0' && ch1 <= '9') {
          haveDigits = true;
          if (ch1 <= 0xffff) {
            {
              ilist.append((char)ch1);
            }
          } else if (ch1 <= 0x10ffff) {
            ilist.append((char)((((ch1 - 0x10000) >> 10) & 0x3ff) | 0xd800));
            ilist.append((char)(((ch1 - 0x10000) & 0x3ff) | 0xdc00));
          }
        } else if (!haveDot && ch1 == '.') {
          haveDot = true;
          // check for nondigit and non-E
          int markpos = this.input.GetMarkPosition();
          int ch2 = this.input.ReadChar();
          if (ch2 != 'e' && ch2 != 'E' && (ch2 < '0' || ch2 > '9')) {
            // move to just at the period and return
            this.input.SetMarkPosition(markpos - 1);
            if (!haveDigits) {
              throw new ParserException();
            }
            String ns = haveDot ? "http://www.w3.org/2001/XMLSchema#decimal" :
              "http://www.w3.org/2001/XMLSchema#integer";
            return RDFTerm.FromTypedString(
                ilist.toString(),
                ns);
          } else {
            this.input.MoveBack(1);
          }
          if (ch1 <= 0xffff) {
            {
              ilist.append((char)ch1);
            }
          } else if (ch1 <= 0x10ffff) {
            ilist.append((char)((((ch1 - 0x10000) >> 10) & 0x3ff) | 0xd800));
            ilist.append((char)(((ch1 - 0x10000) & 0x3ff) | 0xdc00));
          }
        } else { // no more digits
          if (ch1 >= 0) {
            this.input.MoveBack(1);
          }
          if (!haveDigits) {
            throw new ParserException();
          }
          String ns = haveDot ? "http://www.w3.org/2001/XMLSchema#decimal" :
            "http://www.w3.org/2001/XMLSchema#integer";
          return RDFTerm.FromTypedString(
              ilist.toString(),
              ns);
        }
      }
    }

    private TurtleObject ReadObject(boolean acceptLiteral) {
      int ch = this.input.ReadChar();
      int mark = this.input.SetSoftMark();
      if (ch < 0) {
        throw new ParserException();
      } else if (ch == '<') {
        return TurtleObject.FromTerm(
            RDFTerm.FromIRI(this.ReadIriReference()));
      } else if (acceptLiteral && (ch == '-' || ch == '+' || ch == '.' ||
        (ch >= '0' && ch <= '9'))) {
        return TurtleObject.FromTerm(this.ReadNumberLiteral(ch));
      } else if (acceptLiteral && (ch == '\'' || ch == '\"')) {
        // start of quote literal
        String str = this.ReadStringLiteral(ch);
        return TurtleObject.FromTerm(this.FinishStringLiteral(str));
      } else if (ch == '_') { // Blank Node Label
        if (this.input.ReadChar() != ':') {
          throw new ParserException();
        }
        String label = this.ReadBlankNodeLabel();
        RDFTerm term = this.bnodeLabels.containsKey(label) ?
          this.bnodeLabels.get(label) : null;
        if (term == null) {
          term = RDFTerm.FromBlankNode(label);
          this.bnodeLabels.put(label, term);
        }
        return TurtleObject.FromTerm(term);
      } else if (ch == '[') {
        return this.ReadBlankNodePropertyList();
      } else if (ch == '(') {
        return this.ReadCollection();
      } else if (ch == ':') { // prefixed name with current prefix
        String scope = this.namespaces.get("");
        if (scope == null) {
          throw new ParserException();
        }
        return TurtleObject.FromTerm(
            RDFTerm.FromIRI(scope + this.ReadOptionalLocalName()));
      } else if (IsNameStartChar(ch)) { // prefix
        if (acceptLiteral && (ch == 't' || ch == 'f')) {
          mark = this.input.SetHardMark();
          if (ch == 't' && this.input.ReadChar() == 'r' &&
            this.input.ReadChar() == 'u' &&
            this.input.ReadChar() == 'e' && this.IsBooleanLiteralEnd()) {
            return TurtleObject.FromTerm(RDFTerm.TRUE);
          } else if (ch == 'f' && this.input.ReadChar() == 'a' &&
            this.input.ReadChar() == 'l' && this.input.ReadChar() == 's' &&
            this.input.ReadChar() == 'e' && this.IsBooleanLiteralEnd()) {
            return TurtleObject.FromTerm(RDFTerm.FALSE);
          } else {
            this.input.SetMarkPosition(mark);
          }
        }
        String prefix = this.ReadPrefix(ch);
        String scope = this.namespaces.containsKey(prefix) ?
          this.namespaces.get(prefix) : null;
        if (scope == null) {
          throw new ParserException();
        }
        return TurtleObject.FromTerm(
            RDFTerm.FromIRI(scope + this.ReadOptionalLocalName()));
      } else {
        this.input.SetMarkPosition(mark);
        return null;
      }
    }

    private void ReadObjectList(Set<RDFTriple> triples) {
      boolean haveObject = false;
      while (true) {
        this.input.SetSoftMark();
        int ch;
        if (haveObject) {
          ch = this.input.ReadChar();
          if (ch != ',') {
            if (ch >= 0) {
              this.input.MoveBack(1);
            }
            break;
          }
          this.SkipWhitespace();
        }
        // Read Object
        TurtleObject obj = this.ReadObject(true);
        if (obj == null) {
          if (!haveObject) {
            throw new ParserException();
          } else {
            return;
          }
        }
        haveObject = true;
        this.EmitRDFTriple(this.curSubject, this.curPredicate, obj, triples);
        this.SkipWhitespace();
      }
      if (!haveObject) {
        throw new ParserException();
      }
      return;
    }

    private void ReadObjectListToProperties(
      RDFTerm predicate,
      TurtleObject propertyList) {
      boolean haveObject = false;
      while (true) {
        this.input.SetSoftMark();
        int ch;
        if (haveObject) {
          ch = this.input.ReadChar();
          if (ch != ',') {
            if (ch >= 0) {
              this.input.MoveBack(1);
            }
            break;
          }
          this.SkipWhitespace();
        }
        // Read Object
        TurtleObject obj = this.ReadObject(true);
        if (obj == null) {
          if (!haveObject) {
            throw new ParserException();
          } else {
            return;
          }
        }
        TurtleProperty prop = new TurtleProperty();
        prop.setPred(predicate);
        prop.setObj(obj);
        List<TurtleProperty> props = propertyList.GetProperties();
        props.add(prop);
        this.SkipWhitespace();
        haveObject = true;
      }
      if (!haveObject) {
        throw new ParserException();
      }
      return;
    }

    private String ReadOptionalLocalName() {
      StringBuilder ilist = new StringBuilder();
      boolean lastIsPeriod = false;
      boolean first = true;
      this.input.SetSoftMark();
      while (true) {
        int ch = this.input.ReadChar();
        if (ch < 0) {
          return ilist.toString();
        }
        if (ch == '%') {
          int a = this.input.ReadChar();
          int b = this.input.ReadChar();
          if (ToHexValue(a) < 0 ||
            ToHexValue(b) < 0) {
            throw new ParserException();
          }
          if (ch <= 0xffff) {
            {
              ilist.append((char)ch);
            }
          } else if (ch <= 0x10ffff) {
            ilist.append((char)((((ch - 0x10000) >> 10) & 0x3ff) | 0xd800));
            ilist.append((char)(((ch - 0x10000) & 0x3ff) | 0xdc00));
          }
          if (a <= 0xffff) {
            {
              ilist.append((char)a);
            }
          } else if (a <= 0x10ffff) {
            ilist.append((char)((((a - 0x10000) >> 10) & 0x3ff) | 0xd800));
            ilist.append((char)(((a - 0x10000) & 0x3ff) | 0xdc00));
          }
          if (b <= 0xffff) {
            {
              ilist.append((char)b);
            }
          } else if (b <= 0x10ffff) {
            ilist.append((char)((((b - 0x10000) >> 10) & 0x3ff) | 0xd800));
            ilist.append((char)(((b - 0x10000) & 0x3ff) | 0xdc00));
          }
          lastIsPeriod = false;
          first = false;
          continue;
        } else if (ch == '\\') {
          ch = this.input.ReadChar();
          if ((ch & 0x7f) == ch &&
            "_~.-!$&'()*+,;=/?#@%".indexOf((char)ch) >= 0) {
            if (ch <= 0xffff) {
              {
                ilist.append((char)ch);
              }
            } else if (ch <= 0x10ffff) {
              ilist.append((char)((((ch - 0x10000) >> 10) & 0x3ff) | 0xd800));
              ilist.append((char)(((ch - 0x10000) & 0x3ff) | 0xdc00));
            }
          } else {
            throw new ParserException();
          }
          lastIsPeriod = false;
          first = false;
          continue;
        }
        if (first) {
          if (!IsNameStartCharU(ch) && ch != ':' &&
            (ch < '0' || ch > '9')) {
            this.input.MoveBack(1);
            return ilist.toString();
          }
        } else {
          if (!IsNameChar(ch) && ch != ':' && ch != '.') {
            this.input.MoveBack(1);
            if (lastIsPeriod) {
              throw new ParserException();
            }
            return ilist.toString();
          }
        }
        lastIsPeriod = ch == '.';
        if (lastIsPeriod && !first) {
          // if a period was just read, check
          // if the next character is valid before
          // adding the period.
          int position = this.input.GetMarkPosition();
          int ch2 = this.input.ReadChar();
          if (!IsNameChar(ch2) && ch2 != ':' && ch2 != '.') {
            this.input.SetMarkPosition(position - 1);
            return ilist.toString();
          } else {
            this.input.MoveBack(1);
          }
        }
        first = false;
        if (ch <= 0xffff) {
          {
            ilist.append((char)ch);
          }
        } else if (ch <= 0x10ffff) {
          ilist.append((char)((((ch - 0x10000) >> 10) & 0x3ff) | 0xd800));
          ilist.append((char)(((ch - 0x10000) & 0x3ff) | 0xdc00));
        }
      }
    }

    private RDFTerm ReadPredicate() {
      int mark = this.input.SetHardMark();
      int ch = this.input.ReadChar();
      RDFTerm predicate = null;
      if (ch == 'a') {
        mark = this.input.SetHardMark();
        if (this.SkipWhitespace()) {
          return RDFTerm.A;
        } else {
          this.input.SetMarkPosition(mark);
          String prefix = this.ReadPrefix('a');
          String scope = this.namespaces.get(prefix);
          if (scope == null) {
            throw new ParserException();
          }
          predicate = RDFTerm.FromIRI(scope + this.ReadOptionalLocalName());
          this.SkipWhitespace();
          return predicate;
        }
      } else if (ch == '<') {
        predicate = RDFTerm.FromIRI(this.ReadIriReference());
        this.SkipWhitespace();
        return predicate;
      } else if (ch == ':') { // prefixed name with current prefix
        String scope = this.namespaces.get("");
        if (scope == null) {
          throw new ParserException();
        }
        predicate = RDFTerm.FromIRI(scope + this.ReadOptionalLocalName());
        this.SkipWhitespace();
        return predicate;
      } else if (IsNameStartChar(ch)) { // prefix
        String prefix = this.ReadPrefix(ch);
        String scope = this.namespaces.get(prefix);
        if (scope == null) {
          throw new ParserException();
        }
        predicate = RDFTerm.FromIRI(scope + this.ReadOptionalLocalName());
        this.SkipWhitespace();
        return predicate;
      } else {
        this.input.SetMarkPosition(mark);
        return null;
      }
    }

    private void ReadPredicateObjectList(Set<RDFTriple> triples) {
      boolean havePredObject = false;
      while (true) {
        int ch;
        this.SkipWhitespace();
        if (havePredObject) {
          boolean haveSemicolon = false;
          while (true) {
            this.input.SetSoftMark();
            ch = this.input.ReadChar();
            // System.out.println("nextchar %c",(char)ch);
            if (ch == ';') {
              this.SkipWhitespace();
              haveSemicolon = true;
            } else {
              if (ch >= 0) {
                this.input.MoveBack(1);
              }
              break;
            }
          }
          if (!haveSemicolon) {
            break;
          }
        }
        this.curPredicate = this.ReadPredicate();
        // System.out.println("predobjlist %s",curPredicate);
        if (this.curPredicate == null) {
          if (!havePredObject) {
            throw new ParserException();
          } else {
            break;
          }
        }
        // Read _object
        havePredObject = true;
        this.ReadObjectList(triples);
      }
      if (!havePredObject) {
        throw new ParserException();
      }
      return;
    }

    private boolean IsBooleanLiteralEnd() {
      if (this.SkipWhitespace()) {
        return true;
      }
      this.input.SetSoftMark();
      int ch = this.input.ReadChar();
      if (ch < 0) {
        return true;
      }
      this.input.MoveBack(1);
      return IsNameChar(ch);
    }

    private String ReadPrefix(int startChar) {
      StringBuilder ilist = new StringBuilder();
      boolean lastIsPeriod = false;
      boolean first = true;
      if (startChar >= 0) {
        if (startChar <= 0xffff) {
          {
            ilist.append((char)startChar);
          }
        } else if (startChar <= 0x10ffff) {
          ilist.append((char)((((startChar - 0x10000) >> 10) & 0x3ff) |
            0xd800));
          ilist.append((char)(((startChar - 0x10000) & 0x3ff) | 0xdc00));
        }
        first = false;
      }
      while (true) {
        int ch = this.input.ReadChar();
        if (ch < 0) {
          throw new ParserException();
        }
        if (ch == ':') {
          if (lastIsPeriod) {
            throw new ParserException();
          }
          return ilist.toString();
        } else if (first && !IsNameStartChar(ch)) {
          throw new ParserException();
        } else if (ch != '.' && !IsNameChar(ch)) {
          throw new ParserException();
        }
        first = false;
        if (ch <= 0xffff) {
          {
            ilist.append((char)ch);
          }
        } else if (ch <= 0x10ffff) {
          ilist.append((char)((((ch - 0x10000) >> 10) & 0x3ff) | 0xd800));
          ilist.append((char)(((ch - 0x10000) & 0x3ff) | 0xdc00));
        }
        lastIsPeriod = ch == '.';
      }
    }

    private void ReadPrefixStatement(boolean sparql) {
      String prefix = this.ReadPrefix(-1);
      this.SkipWhitespace();
      if (this.input.ReadChar() != '<') {
        throw new ParserException();
      }
      String iri = this.ReadIriReference();
      this.namespaces.put(prefix, iri);
      if (!sparql) {
        this.SkipWhitespace();
        if (this.input.ReadChar() != '.') {
          throw new ParserException();
        }
      } else {
        this.SkipWhitespace();
      }
    }

    private String ReadStringLiteral(int ch) {
      StringBuilder ilist = new StringBuilder();
      boolean first = true;
      boolean longQuote = false;
      int quotecount = 0;
      while (true) {
        int c2 = this.input.ReadChar();
        if (first && c2 == ch) {
          this.input.SetHardMark();
          c2 = this.input.ReadChar();
          if (c2 != ch) {
            if (c2 >= 0) {
              this.input.MoveBack(1);
            }
            return "";
          }
          longQuote = true;
          c2 = this.input.ReadChar();
        }
        first = false;
        if (!longQuote && (c2 == 0x0a || c2 == 0x0d)) {
          throw new ParserException();
        } else if (c2 == '\\') {
          c2 = this.ReadUnicodeEscape(true);
          if (quotecount >= 2) {
            if (ch <= 0xffff) {
              {
                ilist.append((char)ch);
              }
            } else if (ch <= 0x10ffff) {
              ilist.append((char)((((ch - 0x10000) >> 10) & 0x3ff) | 0xd800));
              ilist.append((char)(((ch - 0x10000) & 0x3ff) | 0xdc00));
            }
          }
          if (quotecount >= 1) {
            if (ch <= 0xffff) {
              {
                ilist.append((char)ch);
              }
            } else if (ch <= 0x10ffff) {
              ilist.append((char)((((ch - 0x10000) >> 10) & 0x3ff) | 0xd800));
              ilist.append((char)(((ch - 0x10000) & 0x3ff) | 0xdc00));
            }
          }
          if (c2 <= 0xffff) {
            {
              ilist.append((char)c2);
            }
          } else if (c2 <= 0x10ffff) {
            ilist.append((char)((((c2 - 0x10000) >> 10) & 0x3ff) | 0xd800));
            ilist.append((char)(((c2 - 0x10000) & 0x3ff) | 0xdc00));
          }
          quotecount = 0;
        } else if (c2 == ch) {
          if (!longQuote) {
            return ilist.toString();
          }
          ++quotecount;
          if (quotecount >= 3) {
            return ilist.toString();
          }
        } else {
          if (c2 < 0) {
            throw new ParserException();
          }
          if (quotecount >= 2) {
            if (ch <= 0xffff) {
              {
                ilist.append((char)ch);
              }
            } else if (ch <= 0x10ffff) {
              ilist.append((char)((((ch - 0x10000) >> 10) & 0x3ff) | 0xd800));
              ilist.append((char)(((ch - 0x10000) & 0x3ff) | 0xdc00));
            }
          }
          if (quotecount >= 1) {
            if (ch <= 0xffff) {
              {
                ilist.append((char)ch);
              }
            } else if (ch <= 0x10ffff) {
              ilist.append((char)((((ch - 0x10000) >> 10) & 0x3ff) | 0xd800));
              ilist.append((char)(((ch - 0x10000) & 0x3ff) | 0xdc00));
            }
          }
          if (c2 <= 0xffff) {
            {
              ilist.append((char)c2);
            }
          } else if (c2 <= 0x10ffff) {
            ilist.append((char)((((c2 - 0x10000) >> 10) & 0x3ff) | 0xd800));
            ilist.append((char)(((c2 - 0x10000) & 0x3ff) | 0xdc00));
          }
          quotecount = 0;
        }
      }
    }

    private void ReadTriples(Set<RDFTriple> triples) {
      int mark = this.input.SetHardMark();
      int ch = this.input.ReadChar();
      if (ch < 0) {
        return;
      }
      this.input.SetMarkPosition(mark);
      TurtleObject subject = this.ReadObject(false);
      if (subject == null) {
        throw new ParserException();
      }
      this.curSubject = subject;
      if (!(subject.getKind() == TurtleObject.PROPERTIES &&
        subject.GetProperties().size() > 0)) {
        this.SkipWhitespace();
        this.ReadPredicateObjectList(triples);
      } else {
        this.SkipWhitespace();
        this.input.SetHardMark();
        ch = this.input.ReadChar();
        if (ch == '.') {
          // just a blank node property list;
          // generate a blank node as the subject
          RDFTerm blankNode = this.AllocateBlankNode();
          for (TurtleProperty prop : subject.GetProperties()) {
            this.EmitRDFTriple(blankNode, prop.getPred(), prop.getObj(), triples);
          }
          return;
        } else if (ch < 0) {
          throw new ParserException();
        }
        this.input.MoveBack(1);
        this.ReadPredicateObjectList(triples);
      }
      this.SkipWhitespace();
      if (this.input.ReadChar() != '.') {
        throw new ParserException();
      }
    }

    private int ReadUnicodeEscape(boolean extended) {
      int ch = this.input.ReadChar();
      if (ch == 'U') {
        if (this.input.ReadChar() != '0') {
          throw new ParserException();
        }
        if (this.input.ReadChar() != '0') {
          throw new ParserException();
        }
        int a = ToHexValue(this.input.ReadChar());
        int b = ToHexValue(this.input.ReadChar());
        int c = ToHexValue(this.input.ReadChar());
        int d = ToHexValue(this.input.ReadChar());
        int e = ToHexValue(this.input.ReadChar());
        int f = ToHexValue(this.input.ReadChar());
        if (a < 0 || b < 0 || c < 0 || d < 0 || e < 0 || f < 0) {
          throw new ParserException();
        }
        ch = (a << 20) | (b << 16) | (c << 12) | (d << 8) | (e << 4) | f;
      } else if (ch == 'u') {
        int a = ToHexValue(this.input.ReadChar());
        int b = ToHexValue(this.input.ReadChar());
        int c = ToHexValue(this.input.ReadChar());
        int d = ToHexValue(this.input.ReadChar());
        if (a < 0 || b < 0 || c < 0 || d < 0) {
          throw new ParserException();
        }
        ch = (a << 12) | (b << 8) | (c << 4) | d;
      } else if (extended && ch == 't') {
        return '\t';
      } else if (extended && ch == 'b') {
        return '\b';
      } else if (extended && ch == 'n') {
        return '\n';
      } else if (extended && ch == 'r') {
        return '\r';
      } else if (extended && ch == 'f') {
        return '\f';
      } else if (extended && ch == '\'') {
        return '\'';
      } else if (extended && ch == '\\') {
        return '\\';
      } else if (extended && ch == '"') {
        return '\"';
      } else {
        throw new ParserException();
      }
      // Reject surrogate code points
      // as Unicode escapes
      if ((ch & 0xf800) == 0xd800) {
        throw new ParserException();
      }
      return ch;
    }

    private boolean SkipWhitespace() {
      boolean haveWhitespace = false;
      this.input.SetSoftMark();
      while (true) {
        int ch = this.input.ReadChar();
        if (ch == '#') {
          while (true) {
            ch = this.input.ReadChar();
            if (ch < 0) {
              return true;
            }
            if (ch == 0x0d || ch == 0x0a) {
              break;
            }
          }
        } else if (ch != 0x09 && ch != 0x0a && ch != 0x0d && ch != 0x20) {
          if (ch >= 0) {
            this.input.MoveBack(1);
          }
          return haveWhitespace;
        }
        haveWhitespace = true;
      }
    }

    private static int ToHexValue(int a) {
      if (a >= '0' && a <= '9') {
        return a - '0';
      }
      return (a >= 'a' && a <= 'f') ? (a + 10 - 'a') : ((a >= 'A' && a <= 'F') ?
        (a + 10 - 'A') : -1);
    }
  }
