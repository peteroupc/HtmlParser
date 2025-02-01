package com.upokecenter.rdf;

import java.util.*;

import com.upokecenter.io.*;
import com.upokecenter.util.*;
import com.upokecenter.text.*;

/*
Written in 2013 by Peter Occil.
Any copyright to this work is released to the Public Domain.
In case this is not possible, this work is also
licensed under the Unlicense: https://unlicense.org/

*/

  /**
   * Not documented yet.
   */
  public final class NTriplesParser implements IRDFParser {
    /**
     * Not documented yet.
     * @param c The parameter {@code c} is a 32-bit signed integer.
     * @param asciiChars The parameter {@code asciiChars} is a text string.
     * @return Either {@code true} or {@code false}.
     * @throws NullPointerException The parameter {@code asciiChars} is null.
     */
    public static boolean IsAsciiChar(int c, String asciiChars) {
      if (asciiChars == null) {
        throw new NullPointerException("asciiChars");
      }
      return c >= 0 && c <= 0x7f && asciiChars.indexOf((char)c) >= 0;
    }

    private Map<String, RDFTerm> bnodeLabels;

    private StackableCharacterInput input;

    /**
     * Initializes a new instance of the {@link com.upokecenter.rdf.NTriplesParser}
     * class.
     * @param stream A PeterO.IByteReader object.
     * @throws NullPointerException The parameter {@code stream} is null.
     */
    public NTriplesParser(IByteReader stream) {
      if (stream == null) {
        throw new NullPointerException("stream");
      }
      this.input = new StackableCharacterInput(
        Encodings.GetDecoderInput(
          Encodings.GetEncoding("us-ascii", true),
          stream));
      this.bnodeLabels = new HashMap<String, RDFTerm>();
    }

    /**
     * Initializes a new instance of the {@link com.upokecenter.rdf.NTriplesParser}
     * class.
     * @param str The parameter {@code str} is a text string.
     * @throws NullPointerException The parameter {@code str} is null.
     */
    public NTriplesParser(String str) {
      if (str == null) {
        throw new NullPointerException("str");
      }
      this.input = new StackableCharacterInput(
        Encodings.StringToInput(str));
      this.bnodeLabels = new HashMap<String, RDFTerm>();
    }

    private void EndOfLine(int ch) {
      if (ch == 0x0a) {
        return;
      } else if (ch == 0x0d) {
        ch = this.input.ReadChar();
        if (ch != 0x0a && ch >= 0) {
          this.input.MoveBack(1);
        }
      } else {
        throw new ParserException();
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
        } else {
          throw new ParserException();
        }
      } else {
        this.input.SetMarkPosition(mark);
        return RDFTerm.FromTypedString(str);
      }
    }

    /**
     * Not documented yet.
     * @return An ISet(RDFTriple) object.
     */
    public Set<RDFTriple> Parse() {
      Set<RDFTriple> rdf = new HashSet<RDFTriple>();
      while (true) {
        this.SkipWhitespace();
        this.input.SetHardMark();
        int ch = this.input.ReadChar();
        if (ch < 0) {
          return rdf;
        }
        if (ch == '#') {
          while (true) {
            ch = this.input.ReadChar();
            if (ch == 0x0a || ch == 0x0d) {
              this.EndOfLine(ch);
              break;
            } else if (ch < 0x20 || ch > 0x7e) {
              throw new ParserException();
            }
          }
        } else if (ch == 0x0a || ch == 0x0d) {
          this.EndOfLine(ch);
        } else {
          this.input.MoveBack(1);
          rdf.add(this.ReadTriples());
        }
      }
    }

    private String ReadBlankNodeLabel() {
      StringBuilder ilist = new StringBuilder();
      int startChar = this.input.ReadChar();
      if (!((startChar >= 'A' && startChar <= 'Z') ||
        (startChar >= 'a' && startChar <= 'z'))) {
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
      this.input.SetSoftMark();
      while (true) {
        int ch = this.input.ReadChar();
        if ((ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z') ||
          (ch >= '0' && ch <= '9')) {
          if (ch <= 0xffff) {
            {
              ilist.append((char)ch);
            }
          } else if (ch <= 0x10ffff) {
            ilist.append((char)((((ch - 0x10000) >> 10) & 0x3ff) | 0xd800));
            ilist.append((char)(((ch - 0x10000) & 0x3ff) | 0xdc00));
          }
        } else {
          if (ch >= 0) {
            this.input.MoveBack(1);
          }
          return ilist.toString();
        }
      }
    }

    private String ReadIriReference() {
      StringBuilder ilist = new StringBuilder();
      boolean haveString = false;
      boolean colon = false;
      while (true) {
        int c2 = this.input.ReadChar();
        if ((c2 <= 0x20 || c2 > 0x7e) || ((c2 & 0x7F) == c2 && "<\"{}|^`"
          .indexOf((char)c2) >= 0)) {
          throw new ParserException();
        } else if (c2 == '\\') {
          c2 = this.ReadUnicodeEscape(true);
          if (c2 <= 0x20 || (c2 >= 0x7f && c2 <= 0x9f) || ((c2 & 0x7f) == c2 &&
            "<\"{}|\\^`".indexOf((char)c2) >= 0)) {
            throw new ParserException();
          }
          if (c2 == ':') {
            colon = true;
          }
          if (c2 <= 0xffff) {
            {
              ilist.append((char)c2);
            }
          } else if (c2 <= 0x10ffff) {
            ilist.append((char)((((c2 - 0x10000) >> 10) & 0x3ff) | 0xd800));
            ilist.append((char)(((c2 - 0x10000) & 0x3ff) | 0xdc00));
          }
          haveString = true;
        } else if (c2 == '>') {
          if (!haveString || !colon) {
            throw new ParserException();
          }
          return ilist.toString();
        } else if (c2 == '\"') {
          // Should have been escaped
          throw new ParserException();
        } else {
          if (c2 == ':') {
            colon = true;
          }
          if (c2 <= 0xffff) {
            {
              ilist.append((char)c2);
            }
          } else if (c2 <= 0x10ffff) {
            ilist.append((char)((((c2 - 0x10000) >> 10) & 0x3ff) | 0xd800));
            ilist.append((char)(((c2 - 0x10000) & 0x3ff) | 0xdc00));
          }
          haveString = true;
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
        if (c2 >= 'a' && c2 <= 'z') {
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
        } else if (haveHyphen && (c2 >= '0' && c2 <= '9')) {
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

    private RDFTerm ReadObject(boolean acceptLiteral) {
      int ch = this.input.ReadChar();
      if (ch < 0) {
        throw new ParserException();
      } else if (ch == '<') {
        return RDFTerm.FromIRI(this.ReadIriReference());
      } else if (acceptLiteral && (ch == '\"')) { // start of quote literal
        String str = this.ReadStringLiteral(ch);
        return this.FinishStringLiteral(str);
      } else if (ch == '_') { // Blank Node Label
        if (this.input.ReadChar() != ':') {
          throw new ParserException();
        }
        String label = this.ReadBlankNodeLabel();
        RDFTerm term = this.bnodeLabels.get(label);
        if (term == null) {
          term = RDFTerm.FromBlankNode(label);
          this.bnodeLabels.put(label, term);
        }
        return term;
      } else {
        throw new ParserException();
      }
    }

    private String ReadStringLiteral(int ch) {
      StringBuilder ilist = new StringBuilder();
      while (true) {
        int c2 = this.input.ReadChar();
        if (c2 < 0x20 || c2 > 0x7e) {
          throw new ParserException();
        } else if (c2 == '\\') {
          c2 = this.ReadUnicodeEscape(true);
          if (c2 <= 0xffff) {
            {
              ilist.append((char)c2);
            }
          } else if (c2 <= 0x10ffff) {
            ilist.append((char)((((c2 - 0x10000) >> 10) & 0x3ff) | 0xd800));
            ilist.append((char)(((c2 - 0x10000) & 0x3ff) | 0xdc00));
          }
        } else if (c2 == ch) {
          return ilist.toString();
        } else {
          if (c2 <= 0xffff) {
            {
              ilist.append((char)c2);
            }
          } else if (c2 <= 0x10ffff) {
            ilist.append((char)((((c2 - 0x10000) >> 10) & 0x3ff) | 0xd800));
            ilist.append((char)(((c2 - 0x10000) & 0x3ff) | 0xdc00));
          }
        }
      }
    }

    private RDFTriple ReadTriples() {
      int mark = this.input.SetHardMark();
      int ch = this.input.ReadChar();

      this.input.SetMarkPosition(mark);
      RDFTerm subject = this.ReadObject(false);
      if (!this.SkipWhitespace()) {
        throw new ParserException();
      }
      if (this.input.ReadChar() != '<') {
        throw new ParserException();
      }
      RDFTerm predicate = RDFTerm.FromIRI(this.ReadIriReference());
      if (!this.SkipWhitespace()) {
        throw new ParserException();
      }
      RDFTerm obj = this.ReadObject(true);
      this.SkipWhitespace();
      if (this.input.ReadChar() != '.') {
        throw new ParserException();
      }
      this.SkipWhitespace();
      RDFTriple ret = new RDFTriple(subject, predicate, obj);
      this.EndOfLine(this.input.ReadChar());
      return ret;
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
        // NOTE: The following makes the code too strict
        // if (ch<0x10000) {
        // throw new ParserException();
        // }
      } else if (ch == 'u') {
        int a = ToHexValue(this.input.ReadChar());
        int b = ToHexValue(this.input.ReadChar());
        int c = ToHexValue(this.input.ReadChar());
        int d = ToHexValue(this.input.ReadChar());
        if (a < 0 || b < 0 || c < 0 || d < 0) {
          throw new ParserException();
        }
        ch = (a << 12) | (b << 8) | (c << 4) | d;
        // NOTE: The following makes the code too strict
        // if (ch == 0x09 || ch == 0x0a || ch == 0x0d ||
        // (ch >= 0x20 && ch <= 0x7e)) {
        // throw new ParserException();
        // }
      } else if (ch == 't') {
        return '\t';
      } else if (extended && ch == 'n') {
        return '\n';
      } else if (extended && ch == 'r') {
        return '\r';
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
        if (ch != 0x09 && ch != 0x20) {
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
        (a + 10 - 'A') : (-1));
    }
  }
