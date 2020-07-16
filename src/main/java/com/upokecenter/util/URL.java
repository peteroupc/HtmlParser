package com.upokecenter.util;
/*
Written in 2013 by Peter Occil.
Any copyright is dedicated to the Public Domain.
http://creativecommons.org/publicdomain/zero/1.0/

If you like this, you should donate to Peter O.
at: http://peteroupc.github.io/
*/

import java.util.*;
import java.io.*;

import com.upokecenter.util.*;
import com.upokecenter.text.*;

  /**
   * A URL object under the WHATWG's URL specification. See
   * http://url.spec.whatwg.org/.
   */
  public final class URL {
    private enum ParseState {
      SchemeStart,
      Scheme,
      SchemeData,
      NoScheme,
      RelativeOrAuthority,
      Relative,
      RelativeSlash,
      AuthorityFirstSlash,
      AuthoritySecondSlash,
      AuthorityIgnoreSlashes,
      Authority, Query, Fragment, Host, FileHost,
      RelativePathStart, RelativePath, HostName, Port,
    }

    private static String hex = "0123456789ABCDEF";
    // encodingerror uses ? as repl. ch. when encoding
    // querySerializerError uses decimal HTML entity of bad c.p. when encoding
    private static void AppendOutputBytes(
      StringBuilder builder,
      byte[] bytes) {
      for (int i = 0; i < bytes.length; ++i) {
        int c = bytes[i] & 0xff;
        if (c == 0x20) {
          builder.append ((char)0x2b);
        } else if (c == 0x2a || c == 0x2d || c == 0x2e ||
          (c >= 0x30 && c <= 0x39) || (c >= 0x41 && c <= 0x5a) ||
          (c >= 0x5f) || (c >= 0x61 && c <= 0x7a)) {
          builder.append ((char)c);
        } else {
          builder.append ('%');
          builder.append (hex.charAt((c >> 4) & 0x0f));
          builder.append (hex.charAt(c & 0x0f));
        }
      }
    }

    private static String HostParse(String stringValue) {
      if (stringValue.length() > 0 && stringValue.charAt(0) == '[') {
        if (stringValue.charAt(stringValue.length() - 1) != ']') {
          int[] ipv6 = new int[8];
          int piecePointer = 0;
          int index = 1;
          int compress = -1;
          int ending = stringValue.length() - 1;
          int c = (index >= ending) ? -1 : stringValue.charAt(index);
          if (c == ':') {
            if (index + 1 >= ending || stringValue.charAt(index + 1) != ':') {
              return null;
            }
            index += 2;
            ++piecePointer;
            compress = piecePointer;
          }
          while (index < ending) {
            if (piecePointer >= 8) {
              return null;
            }
            c = stringValue.charAt(index);
            if ((c & 0xfc00) == 0xd800 && index + 1 < ending &&
              (stringValue.charAt(index + 1) & 0xfc00) == 0xdc00) {
              // Get the Unicode code point for the surrogate pair
              c = 0x10000 + ((c & 0x3ff) << 10) + (stringValue.charAt(index + 1) &
                  0x3ff);
              ++index;
            } else if ((c & 0xf800) == 0xd800) {
              // illegal surrogate
              throw new IllegalArgumentException();
            }
            ++index;
            if (c == ':') {
              if (compress >= 0) {
                return null;
              }
              ++piecePointer;
              compress = piecePointer;
              continue;
            }
            int value = 0;
            int length = 0;
            while (length < 4) {
              if (c >= 'A' && c <= 'F') {
                value = value * 16 + (c - 'A') + 10;
                ++index;
                ++length;
                c = (index >= ending) ? -1 : stringValue.charAt(index);
              } else if (c >= 'a' && c <= 'f') {
                value = value * 16 + (c - 'a') + 10;
                ++index;
                ++length;
                c = (index >= ending) ? -1 : stringValue.charAt(index);
              } else if (c >= '0' && c <= '9') {
                value = value * 16 + (c - '0');
                ++index;
                ++length;
                c = (index >= ending) ? -1 : stringValue.charAt(index);
              } else {
                break;
              }
            }
            if (c == '.') {
              if (length == 0) {
                return null;
              }
              index -= length;
              break;
            } else if (c == ':') {
              ++index;
              c = (index >= ending) ? -1 : stringValue.charAt(index);
              if (c < 0) {
                return null;
              }
            } else if (c >= 0) {
              return null;
            }
            ipv6[piecePointer] = value;
            ++piecePointer;
          }
          // IPv4
          if (c >= 0) {
            if (piecePointer > 6) {
              return null;
            }
            int dotsSeen = 0;
            while (index < ending) {
              int value = 0;
              while (c >= '0' && c <= '9') {
                value = value * 10 + (c - '0');
                if (value > 255) {
                  return null;
                }
                ++index;
                c = (index >= ending) ? -1 : stringValue.charAt(index);
              }
              if (dotsSeen < 3 && c != '.') {
                return null;
              } else if (dotsSeen == 3 && c == '.') {
                return null;
              }
              ipv6[piecePointer] = (ipv6[piecePointer] * 256) + value;
              if (dotsSeen == 0 || dotsSeen == 2) {
                ++piecePointer;
              }
              ++dotsSeen;
            }
          }
          if (compress >= 0) {
            int swaps = piecePointer - compress;
            piecePointer = 7;
            while (piecePointer != 0 && swaps != 0) {
              int ptr = compress - swaps + 1;
              int tmp = ipv6[piecePointer];
              ipv6[piecePointer] = ipv6[ptr];
              ipv6[ptr] = tmp;
              --piecePointer;
              --swaps;
            }
          } else if (compress < 0 && piecePointer != 8) {
            return null;
          }
        }
      }
      try {
        // System.out.println("was: %s",stringValue);
        stringValue = PercentDecode (stringValue, "utf-8");
        // System.out.println("now: %s",stringValue);
      } catch (IOException ex) {
        return null;
      }
      return stringValue;
    }

    private static String HostSerialize(String stringValue) {
      return (stringValue == null) ? "" : stringValue;
    }

    private static boolean IsHexDigit(int c) {
      return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' &&
          c <= '9');
    }

    private static boolean IsUrlCodePoint(int c) {
      if (c <= 0x20) {
        return false;
      }
      if (c < 0x80) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >=
            '0' && c <= '9') || ((c & 0x7F) == c && "!$&'()*+,-./:;=?@_~"
            .indexOf((char)c) >= 0);
      } else if ((c & 0xfffe) == 0xfffe) {
        return false;
      } else return ((c >= 0xa0 && c <= 0xd7ff) || (c >= 0xe000 && c <=
              0xfdcf) ||
            (c >= 0xfdf0 && c <= 0xffef) || (c >= 0x10000 && c <= 0x10fffd)) ?
          true : false;
    }

    /**
     * Not documented yet.
     * @param s The parameter {@code s} is a text string.
     * @return An URL object.
     */
    public static URL Parse(String s) {
      return Parse (s, null, null, false);
    }

    /**
     * Not documented yet.
     * @param s The parameter {@code s} is a text string.
     * @param baseurl The parameter {@code baseurl} is a.getUpokecenter().getUtil().getURL()
     * object.
     * @return An URL object.
     */
    public static URL Parse(String s, URL baseurl) {
      return Parse (s, baseurl, null, false);
    }

    /**
     * Not documented yet.
     * @param s The parameter {@code s} is a text string.
     * @param baseurl The parameter {@code baseurl} is a.getUpokecenter().getUtil().getURL()
     * object.
     * @param encoding The parameter {@code encoding} is a text string.
     * @return An URL object.
     */
    public static URL Parse(String s, URL baseurl, String encoding) {
      return Parse (s, baseurl, encoding, false);
    }

    /**
     * Not documented yet.
     * @param s The parameter {@code s} is a text string.
     * @param baseurl The parameter {@code baseurl} is a.getUpokecenter().getUtil().getURL()
     * object.
     * @param encoding The parameter {@code encoding} is a text string.
     * @param strict The parameter {@code strict} is either {@code true} or {@code
     * false}.
     * @return An URL object.
     * @throws NullPointerException The parameter {@code baseurl} is null.
     */
    public static URL Parse(
      String s,
      URL baseurl,
      String encoding,
      boolean strict) {
      if (s == null) {
        throw new IllegalArgumentException();
      }
      int beginning = 0;
      int ending = s.length() - 1;
      boolean relative = false;
      URL url = new URL();
      ICharacterEncoder encoder = null;
      ParseState state = ParseState.SchemeStart;
      if (encoding != null) {
        encoder = Encodings.GetEncoding (encoding).GetEncoder();
      }
      if (s.indexOf("http://") == 0) {
        state = ParseState.AuthorityIgnoreSlashes;
        url.scheme = "http";
        beginning = 7;
        relative = true;
      } else {
        while (beginning < s.length()) {
          char c = s.charAt(beginning);
          if (c != 0x09 && c != 0x0a && c != 0x0c && c != 0x0d && c != 0x20) {
            break;
          }
          ++beginning;
        }
      }
      while (ending >= beginning) {
        char c = s.charAt(ending);
        if (c != 0x09 && c != 0x0a && c != 0x0c && c != 0x0d && c != 0x20) {
          ++ending;
          break;
        }
        --ending;
      }
      if (ending < beginning) {
        ending = beginning;
      }
      boolean atflag = false;
      boolean bracketflag = false;
      StringBuilder buffer = new StringBuilder();
      StringBuilder query = null;
      StringBuilder fragment = null;
      StringBuilder password = null;
      StringBuilder username = null;
      StringBuilder schemeData = null;
      boolean error = false;
      List<String> path = new ArrayList<String>();
      int index = beginning;
      int hostStart = -1;
      int portstate = 0;
      while (index <= ending) {
        int oldindex = index;
        int c = -1;
        if (index >= ending) {
          c = -1;
          ++index;
        } else {
          c = s.charAt(index);
          if ((c & 0xfc00) == 0xd800 && index + 1 < ending &&
            (s.charAt(index + 1) & 0xfc00) == 0xdc00) {
            // Get the Unicode code point for the surrogate pair
            c = 0x10000 + ((c & 0x3ff) << 10) + (s.charAt(index + 1) & 0x3ff);
            ++index;
          } else if ((c & 0xf800) == 0xd800) {
            // illegal surrogate
            throw new IllegalArgumentException();
          }
          ++index;
        }
        switch (state) {
          case SchemeStart:
            if (c >= 'A' && c <= 'Z') {
              if (c + 0x20 <= 0xffff) {
                { buffer.append ((char)(c + 0x20));
                }
              } else if (c + 0x20 <= 0x10ffff) {
                buffer.append ((char)((((c + 0x20 - 0x10000) >> 10) &
0x3ff) | 0xd800));
                buffer.append ((char)(((c + 0x20 - 0x10000) & 0x3ff) |
0xdc00));
              }
              state = ParseState.Scheme;
            } else if (c >= 'a' && c <= 'z') {
              if (c <= 0xffff) {
                { buffer.append ((char)c);
                }
              } else if (c <= 0x10ffff) {
                buffer.append ((char)((((c - 0x10000) >> 10) & 0x3ff) |
0xd800));
                buffer.append ((char)(((c - 0x10000) & 0x3ff) | 0xdc00));
              }
              state = ParseState.Scheme;
            } else {
              index = oldindex;
              state = ParseState.NoScheme;
            }
            break;
          case Scheme:
            if (c >= 'A' && c <= 'Z') {
              if (c + 0x20 <= 0xffff) {
                { buffer.append ((char)(c + 0x20));
                }
              } else if (c + 0x20 <= 0x10ffff) {
                buffer.append ((char)((((c + 0x20 - 0x10000) >> 10) &
0x3ff) | 0xd800));
                buffer.append ((char)(((c + 0x20 - 0x10000) & 0x3ff) |
0xdc00));
              }
            } else if ((c >= 'a' && c <= 'z') || c == '.' || c == '-' || c ==
              '+') {
              if (c <= 0xffff) {
                { buffer.append ((char)c);
                }
              } else if (c <= 0x10ffff) {
                buffer.append ((char)((((c - 0x10000) >> 10) & 0x3ff) |
0xd800));
                buffer.append ((char)(((c - 0x10000) & 0x3ff) | 0xdc00));
              }
            } else if (c == ':') {
              url.scheme = buffer.toString();
              buffer.delete(0, buffer.length());
              if (url.scheme.equals("http") ||
                url.scheme.equals("https") ||
                url.scheme.equals("ftp") ||
                url.scheme.equals("gopher") ||
                url.scheme.equals("ws") ||
                url.scheme.equals("wss") ||
                url.scheme.equals("file")) {
                relative = true;
              }
              if (url.scheme.equals("file")) {
                state = ParseState.Relative;
                relative = true;
              } else if (relative && baseurl != null &&
                url.scheme.equals(baseurl.scheme)) {
                state = ParseState.RelativeOrAuthority;
              } else if (relative) {
                state = ParseState.AuthorityFirstSlash;
              } else {
                schemeData = new StringBuilder();
                state = ParseState.SchemeData;
              }
            } else {
              buffer.delete(0, buffer.length());
              index = beginning;
              state = ParseState.NoScheme;
            }
            break;
          case SchemeData:
            if (c == '?') {
              query = new StringBuilder();
              state = ParseState.Query;
              break;
            } else if (c == '#') {
              fragment = new StringBuilder();
              state = ParseState.Fragment;
              break;
            }
            if (c >= 0 && (!IsUrlCodePoint (c) && c != '%') || (c == '%' &&
                (index + 2 > ending || !IsHexDigit (s.charAt(index)) ||
                  !IsHexDigit (s.charAt(index + 1))))) {
              error = true;
            }
            if (c >= 0 && c != 0x09 && c != 0x0a && c != 0x0d) {
              if (c < 0x20 || c == 0x7f) {
                PercentEncode (schemeData, c);
              } else if (c < 0x7f) {
                if (c <= 0xffff) {
                  { schemeData.append ((char)c);
                  }
                } else if (c <= 0x10ffff) {
                  schemeData.append ((char)((((c - 0x10000) >> 10) & 0x3ff)|
0xd800));
                  schemeData.append ((char)(((c - 0x10000) & 0x3ff) | 0xdc00));
                }
              } else {
                PercentEncodeUtf8 (schemeData, c);
              }
            }
            break;
          case NoScheme:
            if (baseurl == null) {
              return null;
            }
            // System.out.println("no scheme: [%s] [%s]",s,baseurl);
            if (! (baseurl.scheme.equals("http") ||
                baseurl.scheme.equals(
                  "https") || baseurl.scheme.equals("ftp") || baseurl.scheme.equals(
                    "gopher") || baseurl.scheme.equals("ws") || baseurl.scheme.equals("wss") ||
                baseurl.scheme.equals("file"))) {
              return null;
            }
            state = ParseState.Relative;
            index = oldindex;
            break;
          case RelativeOrAuthority:
            if (c == '/' && index < ending && s.charAt(index) == '/') {
              ++index;
              state = ParseState.AuthorityIgnoreSlashes;
            } else {
              error = true;
              state = ParseState.Relative;
              index = oldindex;
            }
            break;
          case Relative: {
            relative = true;
            if (!"file".equals(url.scheme)) {
              if (baseurl == null) {
                throw new NullPointerException("baseurl");
              }
              url.scheme = baseurl.scheme;
            }
            if (c < 0) {
              url.host = baseurl.host;
              url.port = baseurl.port;
              path = PathList (baseurl.path);
              url.query = baseurl.query;
            } else if (c == '/' || c == '\\') {
              if (c == '\\') {
                error = true;
              }
              state = ParseState.RelativeSlash;
            } else if (c == '?') {
              url.host = baseurl.host;
              url.port = baseurl.port;
              path = PathList (baseurl.path);
              query = new StringBuilder();
              state = ParseState.Query;
            } else if (c == '#') {
              url.host = baseurl.host;
              url.port = baseurl.port;
              path = PathList (baseurl.path);
              url.query = baseurl.query;
              fragment = new StringBuilder();
              state = ParseState.Fragment;
            } else {
              url.host = baseurl.host;
              url.port = baseurl.port;
              path = PathList (baseurl.path);
              if (path.size() > 0) { // Pop path
                path.remove(path.size() - 1);
              }
              state = ParseState.RelativePath;
              index = oldindex;
            }
            break;
          }
          case RelativeSlash:
            if (c == '/' || c == '\\') {
              if (c == '\\') {
                error = true;
              }
              state = "file".equals(url.scheme) ?
                ParseState.FileHost : ParseState.AuthorityIgnoreSlashes;
              } else {
              if (baseurl != null) {
                url.host = baseurl.host;
                url.port = baseurl.port;
              }
              state = ParseState.RelativePath;
              index = oldindex;
            }
            break;
          case AuthorityFirstSlash:
            if (c == '/') {
              state = ParseState.AuthoritySecondSlash;
            } else {
              error = true;
              state = ParseState.AuthorityIgnoreSlashes;
              index = oldindex;
            }
            break;
          case AuthoritySecondSlash:
            if (c == '/') {
              state = ParseState.AuthorityIgnoreSlashes;
            } else {
              error = true;
              state = ParseState.AuthorityIgnoreSlashes;
              index = oldindex;
            }
            break;
          case AuthorityIgnoreSlashes:
            if (c != '/' && c != '\\') {
              username = new StringBuilder();
              index = oldindex;
              hostStart = index;
              state = ParseState.Authority;
            } else {
              error = true;
            }
            break;
          case Authority:
            if (c == '@') {
              if (atflag) {
                StringBuilder result = (password == null) ? username : password;
                error = true;
                result.append ("%40");
              }
              atflag = true;
              String bstr = buffer.toString();
              for (int i = 0; i < bstr.length(); ++i) {
                int cp = DataUtilities.CodePointAt (bstr, i);
                if (cp >= 0x10000) {
                  ++i;
                }
                if (cp == 0x9 || cp == 0xa || cp == 0xd) {
                  error = true;
                  continue;
                }
                if ((!IsUrlCodePoint (c) && c != '%') || (cp == '%' &&
                    (i + 3 > bstr.length() || !IsHexDigit (bstr.charAt(index + 1)) ||
                      !IsHexDigit (bstr.charAt(index + 2))))) {
                  error = true;
                }
                if (cp == ':' && password == null) {
                  password = new StringBuilder();
                  continue;
                }
                StringBuilder result = (password == null) ? username : password;
                if (cp <= 0x20 || cp >= 0x7F || ((cp & 0x7F) == cp && "#<>?`\""
                    .indexOf((char)cp) >= 0)) {
                  PercentEncodeUtf8 (result, cp);
                } else {
                  if (cp <= 0xffff) {
                    { result.append ((char)cp);
                    }
                  } else if (cp <= 0x10ffff) {
                    result.append ((char)((((cp - 0x10000) >> 10) & 0x3ff)|
0xd800));
                    result.append ((char)(((cp - 0x10000) & 0x3ff) | 0xdc00));
                  }
                }
              }

              // System.out.println("username=%s",username);
              // System.out.println("password=%s",password);
              buffer.delete(0, buffer.length());
              hostStart = index;
            } else if (c < 0 || ((c & 0x7F) == c && "/\\?#".indexOf((char)c) >=
                0)) {
              buffer.delete(0, buffer.length());
              state = ParseState.Host;
              index = hostStart;
            } else {
              if (c <= 0xffff) {
                { buffer.append ((char)c);
                }
              } else if (c <= 0x10ffff) {
                buffer.append ((char)((((c - 0x10000) >> 10) & 0x3ff) |
0xd800));
                buffer.append ((char)(((c - 0x10000) & 0x3ff) | 0xdc00));
              }
            }
            break;
          case FileHost:
            if (c < 0 || ((c & 0x7F) == c && "/\\?#".indexOf((char)c) >= 0)) {
              index = oldindex;
              if (buffer.length() == 2) {
                int c1 = buffer.charAt(0);
                int c2 = buffer.charAt(1);
                if (
                  (c2 == '|' || c2 == ':') && ((c1 >= 'A' && c1 <= 'Z') ||
                (c1 >= 'a' && c1 <= 'z'))) {
                  state = ParseState.RelativePath;
                  break;
                }
              }
              String host = HostParse (buffer.toString());
              if (host == null) {
                throw new IllegalArgumentException();
              }
              url.host = host;
              buffer.delete(0, buffer.length());
              state = ParseState.RelativePathStart;
            } else if (c == 0x09 || c == 0x0a || c == 0x0d) {
              error = true;
            } else {
              if (c <= 0xffff) {
                { buffer.append ((char)c);
                }
              } else if (c <= 0x10ffff) {
                buffer.append ((char)((((c - 0x10000) >> 10) & 0x3ff) |
0xd800));
                buffer.append ((char)(((c - 0x10000) & 0x3ff) | 0xdc00));
              }
            }
            break;
          case Host:
          case HostName:
            if (c == ':' && !bracketflag) {
              String host = HostParse (buffer.toString());
              if (host == null) {
                return null;
              }
              url.host = host;
              buffer.delete(0, buffer.length());
              state = ParseState.Port;
            } else if (c < 0 || ((c & 0x7F) == c && "/\\?#".indexOf((char)c) >=
                0)) {
              String host = HostParse (buffer.toString());
              if (host == null) {
                return null;
              }
              url.host = host;
              buffer.delete(0, buffer.length());
              index = oldindex;
              state = ParseState.RelativePathStart;
            } else if (c == 0x09 || c == 0x0a || c == 0x0d) {
              error = true;
            } else {
              if (c == '[') {
                bracketflag = true;
              } else if (c == ']') {
                bracketflag = false;
              }
              if (c <= 0xffff) {
                { buffer.append ((char)c);
                }
              } else if (c <= 0x10ffff) {
                buffer.append ((char)((((c - 0x10000) >> 10) & 0x3ff) |
0xd800));
                buffer.append ((char)(((c - 0x10000) & 0x3ff) | 0xdc00));
              }
            }
            break;
          case Port:
            if (c >= '0' && c <= '9') {
              if (c != '0') {
                portstate = 2; // first non-zero found
              } else if (portstate == 0) {
                portstate = 1; // have a port number
              }
              if (portstate == 2) {
                if (c <= 0xffff) {
                  { buffer.append ((char)c);
                  }
                } else if (c <= 0x10ffff) {
                  buffer.append ((char)((((c - 0x10000) >> 10) & 0x3ff) |
0xd800));
                  buffer.append ((char)(((c - 0x10000) & 0x3ff) | 0xdc00));
                }
              }
            } else if (c < 0 || ((c & 0x7F) == c && "/\\?#".indexOf((char)c) >=
                0)) {
              String bufport = "";
              if (portstate == 1) {
                bufport = "0";
              } else if (portstate == 2) {
                bufport = buffer.toString();
              }
              // System.out.println("port: [%s]",buffer.toString());
              if ((url.scheme.equals("http") ||
                  url.scheme.equals("ws")) &&
                bufport.equals("80")) {
                bufport = "";
              }
              if ((url.scheme.equals("https") ||
                  url.scheme.equals("wss")) &&
                bufport.equals("443")) {
                bufport = "";
              }
              if ((url.scheme.equals("gopher")) &&
                bufport.equals("70")) {
                bufport = "";
              }
              if ((url.scheme.equals("ftp")) &&
                bufport.equals("21")) {
                bufport = "";
              }
              url.port = bufport;
              buffer.delete(0, buffer.length());
              state = ParseState.RelativePathStart;
              index = oldindex;
            } else if (c == 0x09 || c == 0x0a || c == 0x0d) {
              error = true;
            } else {
              return null;
            }
            break;
          case Query:
            if (c < 0 || c == '#') {
              boolean utf8 = true;
              if (relative) {
                utf8 = true;
              }
              if (utf8 || encoder == null) {
                // NOTE: Encoder errors can never happen in
                // this case
                String bstr = buffer.toString();
                for (int i = 0; i < bstr.length(); ++i) {
                  int ch = DataUtilities.CodePointAt (bstr, i);
                  if (ch >= 0x10000) {
                    ++i;
                  }
                  if (ch < 0x21 || ch > 0x7e || ch == 0x22 || ch == 0x23 ||
                    ch == 0x3c || ch == 0x3e || ch == 0x60) {
                    PercentEncodeUtf8 (query, ch);
                  } else {
                    { query.append ((char)ch);
                    }
                  }
                }
              } else {
                byte[] bytes =
                  Encodings.EncodeToBytes(
                    Encodings.StringToInput (buffer.toString()),
                    encoder);
                for (Object ch : bytes) {
                  if (ch < 0x21 || ch > 0x7e || ch == 0x22 || ch == 0x23 ||
                    ch == 0x3c || ch == 0x3e || ch == 0x60) {
                    PercentEncode (query, ch);
                  } else {
                    { query.append ((char)ch);
                    }
                  }
                }
              }
              buffer.delete(0, buffer.length());
              if (c == '#') {
                fragment = new StringBuilder();
                state = ParseState.Fragment;
              }
            } else if (c == 0x09 || c == 0x0a || c == 0x0d) {
              error = true;
            } else {
              if ((!IsUrlCodePoint (c) && c != '%') || (c == '%' &&
                  (index + 2 > ending || !IsHexDigit (s.charAt(index)) ||
                    !IsHexDigit (s.charAt(index + 1))))) {
                error = true;
              }
              if (c <= 0xffff) {
                { buffer.append ((char)c);
                }
              } else if (c <= 0x10ffff) {
                buffer.append ((char)((((c - 0x10000) >> 10) & 0x3ff) |
0xd800));
                buffer.append ((char)(((c - 0x10000) & 0x3ff) | 0xdc00));
              }
            }
            break;
          case RelativePathStart:
            if (c == '\\') {
              error = true;
            }
            state = ParseState.RelativePath;
            if (c != '/' && c != '\\') {
              index = oldindex;
            }
            break;
          case RelativePath:
            if ((c < 0 || c == '/' || c == '\\') || (c == '?' || c == '#')) {
              if (c == '\\') {
                error = true;
              }
              if (buffer.length() == 2 && buffer.charAt(0) == '.' &&
                buffer.charAt(1) == '.') {
                if (path.size() > 0) {
                  path.remove(path.size() - 1);
                }
                if (c != '/' && c != '\\') {
                  path.add("");
                }
              } else if (buffer.length() == 1 && buffer.charAt(0) == '.') {
                if (c != '/' && c != '\\') {
                  path.add("");
                }
              } else {
                if ("file".equals(url.scheme) &&
                path.size() == 0 && buffer.length() == 2) {
                  int c1 = buffer.charAt(0);
                  int c2 = buffer.charAt(1);
                  if (
                    (c2 == '|' || c2 == ':') && ((c1 >= 'A' && c1 <= 'Z') ||
                (c1 >= 'a' && c1 <= 'z'))) {
                    buffer.charAt(1) = ':';
                  }
                }
                path.add(buffer.toString());
              }
              buffer.delete(0, buffer.length());
              if (c == '?') {
                query = new StringBuilder();
                state = ParseState.Query;
              }
              if (c == '#') {
                fragment = new StringBuilder();
                state = ParseState.Fragment;
              }
            } else if (c == '%' && index + 2 <= ending && s.charAt(index) == '2' &&
              (s.charAt(index + 1) == 'e' || s.charAt(index + 1) == 'E')) {
              index += 2;
              buffer.append ((char)'.');
            } else if (c == 0x09 || c == 0x0a || c == 0x0d) {
              error = true;
            } else {
              if ((!IsUrlCodePoint (c) && c != '%') || (c == '%' &&
                  (index + 2 > ending || !IsHexDigit (s.charAt(index)) ||
                    !IsHexDigit (s.charAt(index + 1))))) {
                error = true;
              }
              if (c <= 0x20 || c >= 0x7F || ((c & 0x7F) == c && "#<>?`\""
                  .indexOf((char)c) >= 0)) {
                PercentEncodeUtf8 (buffer, c);
              } else {
                if (c <= 0xffff) {
                  { buffer.append ((char)c);
                  }
                } else if (c <= 0x10ffff) {
                  buffer.append ((char)((((c - 0x10000) >> 10) & 0x3ff) |
0xd800));
                  buffer.append ((char)(((c - 0x10000) & 0x3ff) | 0xdc00));
                }
              }
            }
            break;
          case Fragment:
            if (c < 0) {
              break;
            }
            if (c == 0x09 || c == 0x0a || c == 0x0d) {
              error = true;
            } else {
              if ((!IsUrlCodePoint (c) && c != '%') || (c == '%' &&
                  (index + 2 > ending || !IsHexDigit (s.charAt(index)) ||
                    !IsHexDigit (s.charAt(index + 1))))) {
                error = true;
              }
              if (c < 0x20 || c == 0x7f) {
                PercentEncode (fragment, c);
              } else if (c < 0x7f) {
                if (c <= 0xffff) {
                  { fragment.append ((char)c);
                  }
                } else if (c <= 0x10ffff) {
                  fragment.append ((char)((((c - 0x10000) >> 10) & 0x3ff) |
0xd800));
                  fragment.append ((char)(((c - 0x10000) & 0x3ff) | 0xdc00));
                }
              } else {
                PercentEncodeUtf8 (fragment, c);
              }
            }
            break;
          default: throw new IllegalStateException();
        }
      }
      if (error && strict) {
        return null;
      }
      if (schemeData != null) {
        url.schemeData = schemeData.toString();
      }
      StringBuilder builder = new StringBuilder();
      if (path.size() == 0) {
        builder.append ('/');
      } else {
        for (Object segment : path) {
          builder.append ('/');
          builder.append (segment);
        }
      }
      url.path = builder.toString();
      if (query != null) {
        url.query = query.toString();
      }
      if (fragment != null) {
        url.fragment = fragment.toString();
      }
      if (password != null) {
        url.password = password.toString();
      }
      if (username != null) {
        url.username = username.toString();
      }
      return url;
    }

    /**
     * Not documented yet.
     * @param input The parameter {@code input} is a text string.
     * @param delimiter The parameter {@code delimiter} is a text string.
     * @param encoding The parameter {@code encoding} is a text string.
     * @param useCharset The parameter {@code useCharset} is either {@code true} or
     * {@code false}.
     * @param isindex The parameter {@code isindex} is either {@code true} or
     * {@code false}.
     * @return An List(string[]) object.
     */
    public static List<String[]> ParseQueryString(
      String input,
      String delimiter,
      String encoding,
      boolean useCharset,
      boolean isindex) {
      if (input == null) {
        throw new IllegalArgumentException();
      }
      delimiter = (delimiter == null) ? ("&") : delimiter;
      encoding = (encoding == null) ? ("utf-8") : encoding;
      for (int i = 0; i < input.length(); ++i) {
        if (input.charAt(i) > 0x7f) {
          throw new IllegalArgumentException();
        }
      }
      String[] strings = StringUtility.splitAt (input, delimiter);
      List<String[]> pairs = new ArrayList<String[]>();
      for (String str : strings) {
        if (str.length() == 0) {
          continue;
        }
        int index = str.indexOf('=');
        String name = str;
        String value = "";
        if (index >= 0) {
          name = str.substring(0, index - 0);
          value = str.substring(index + 1);
        }
        name = name.replace('+', ' ');
        value = value.replace('+', ' ');
        if (useCharset && "_charset_".equals(name)) {
          String ch = Encodings.ResolveAlias (value);
          if (ch != null) {
            useCharset = false;
            encoding = ch;
          }
        }
        String[] pair = new String[] { name, value };
        pairs.add(pair);
      }
      try {
        for (Object pair : pairs) {
          pair[0] = PercentDecode (pair[0], encoding);
          pair[1] = PercentDecode (pair[1], encoding);
        }
      } catch (IOException e) {
        throw e;
      }
      return pairs;
    }

    /**
     * Not documented yet.
     * @param s The parameter {@code s} is a text string.
     * @return An List(string) object.
     */
    public static List<String> PathList(String s) {
      List<String> str = new ArrayList<String>();
      if (s == null || s.length() == 0) {
        return str;
      }
      if (s.charAt(0) != '/') {
        throw new IllegalArgumentException();
      }
      int i = 1;
      while (i <= s.length()) {
        int io = s.indexOf('/',i);
        if (io >= 0) {
          str.add(s.substring(i, (i)+(io - i)));
          i = io + 1;
        } else {
          str.add(s.substring(i));
          break;
        }
      }
      return str;
    }

    private static String PercentDecode(String str, String encoding) {
      int len = str.length();
      boolean percent = false;
      for (int i = 0; i < len; ++i) {
        char c = str.charAt(i);
        if (c == '%') {
          percent = true;
        } else if (c >= 0x80) {
          // Non-ASCII characters not allowed
          return null;
        }
      }
      if (!percent) {
        return str;
      }
      var enc = Encodings.GetEncoding (encoding);
      {
        java.io.ByteArrayOutputStream mos = null;
try {
mos = new java.io.ByteArrayOutputStream();

        for (int i = 0; i < len; ++i) {
          int c = str.charAt(i);
          if (c == '%') {
            if (i + 2 < len) {
              int a = ToHexNumber (str.charAt(i + 1));
              int b = ToHexNumber (str.charAt(i + 2));
              if (a >= 0 && b >= 0) {
                mos.write ((byte)((a * 16) + b));
                i += 2;
                continue;
              }
            }
          }
          mos.write ((byte)(c & 0xff));
        }
        return Encodings.DecodeToString (enc, mos.ToArray());
}
finally {
try { if (mos != null) { mos.close(); } } catch (java.io.IOException ex) {}
}
}
    }

    private static void PercentEncode(StringBuilder buffer, int b) {
      buffer.append ((char)'%');
      buffer.append (hex.charAt((b >> 4) & 0x0f));
      buffer.append (hex.charAt(b & 0x0f));
    }

    private static void PercentEncodeUtf8(StringBuilder buffer, int cp) {
      if (cp <= 0x7f) {
        buffer.append ((char)'%');
        buffer.append (hex.charAt((cp >> 4) & 0x0f));
        buffer.append (hex.charAt(cp & 0x0f));
      } else if (cp <= 0x7ff) {
        PercentEncode (buffer, 0xc0 | ((cp >> 6) & 0x1f));
        PercentEncode (buffer, 0x80 | (cp & 0x3f));
      } else if (cp <= 0xffff) {
        PercentEncode (buffer, 0xe0 | ((cp >> 12) & 0x0f));
        PercentEncode (buffer, 0x80 | ((cp >> 6) & 0x3f));
        PercentEncode (buffer, 0x80 | (cp & 0x3f));
      } else {
        PercentEncode (buffer, 0xf0 | ((cp >> 18) & 0x07));
        PercentEncode (buffer, 0x80 | ((cp >> 12) & 0x3f));
        PercentEncode (buffer, 0x80 | ((cp >> 6) & 0x3f));
        PercentEncode (buffer, 0x80 | (cp & 0x3f));
      }
    }

    private static int ToHexNumber(int c) {
      if (c >= 'A' && c <= 'Z') {
        return 10 + c - 'A';
      } else if (c >= 'a' && c <= 'z') {
        return 10 + c - 'a';
      } else {
        return (c >= '0' && c <= '9') ? (c - '0') : (-1);
      }
    }

    /**
     * Not documented yet.
     * @param input The parameter {@code input} is a.getText().ICharacterInput object.
     * @param encoder The parameter {@code encoder} is a.getText().getICharacterEncoder()
     * object.
     * @return A byte array.
     * @throws NullPointerException The parameter {@code encoder} or {@code input}
     * is null.
     * @throws IllegalArgumentException Code point out of range.
     */
    public static byte[] EncodeToBytesHtml(
      PeterO.Text.ICharacterInput input,
      ICharacterEncoder encoder) {
      if (encoder == null) {
        throw new NullPointerException("encoder");
      }
      if (input == null) {
        throw new NullPointerException("input");
      }
      PeterO.ArrayWriter writer = new PeterO.ArrayWriter();
      while (true) {
        int cp = input.ReadChar();
        int enc = encoder.Encode (cp, writer);
        if (enc == -2) {
          if (cp < 0 || cp >= 0x110000 || ((cp & 0xf800) == 0xd800)) {
            throw new IllegalArgumentException("code point out of range");
          }
          writer.write (0x26);
          writer.write (0x23);
          if (cp == 0) {
            writer.write (0x30);
          } else {
            while (cp > 0) {
              writer.write (0x30 + (cp % 10));
              cp /= 10;
            }
          }
          writer.write (0x3b);
        }
        if (enc == -1) {
          break;
        }
      }
      return writer.ToArray();
    }

    /**
     * Not documented yet.
     * @param pairs Not documented yet.
     * @param delimiter Not documented yet.
     * @param encoding Not documented yet.
     * @throws NullPointerException The parameter {@code pairs} is null.
     */
    public static String ToQueryString(
      List<String[]> pairs,
      String delimiter,
      String encoding) {
      encoding = (encoding == null) ? ("utf-8") : encoding;
      ICharacterEncoding ienc = Encodings.GetEncoding (encoding);
      if (ienc == null) {
        throw new IllegalArgumentException("encoding");
      }
      ICharacterEncoder encoder = ienc.GetEncoder();
      StringBuilder builder = new StringBuilder();
      boolean first = true;
      if (pairs == null) {
        throw new NullPointerException("pairs");
      }
      for (Object pair : pairs) {
        if (!first) {
          builder.append (delimiter == null ? "&" : delimiter);
        }
        first = false;
        if (pair == null || pair.length < 2) {
          throw new IllegalArgumentException();
        }
        // TODO: Use htmlFallback parameter in EncodeToBytes
        // added to next version of Encoding library
        AppendOutputBytes(
          builder,
          EncodeToBytesHtml (Encodings.StringToInput (pair[0]), encoder));
        builder.append ('=');
        {
          StringBuilder objectTemp = builder;
          byte[] objectTemp2 = EncodeToBytesHtml(
              Encodings.StringToInput (pair[1]),
              encoder);
          AppendOutputBytes (objectTemp, objectTemp2);
        }
      }
      return builder.toString();
    }

    private String scheme = "";

    private String schemeData = "";

    private String username = "";

    private String password = null;

    private String host = null;

    private String path = "";

    private String query = null;

    private String fragment = null;

    private String port = "";

    /**
     * Not documented yet.
     * @param obj The parameter {@code obj} is a object object.
     * @return Either {@code true} or {@code false}.
     */
    @Override public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (this.getClass() != obj.getClass()) {
        return false;
      }
      URL other = (URL)obj;
      if (this.fragment == null) {
        if (other.fragment != null) {
          return false;
        }
      } else if (!this.fragment.equals(other.fragment)) {
        return false;
      }
      if (this.host == null) {
        if (other.host != null) {
          return false;
        }
      } else if (!this.host.equals(other.host)) {
        return false;
      }
      if (this.password == null) {
        if (other.password != null) {
          return false;
        }
      } else if (!this.password.equals(other.password)) {
        return false;
      }
      if (this.path == null) {
        if (other.path != null) {
          return false;
        }
      } else if (!this.path.equals(other.path)) {
        return false;
      }
      if (this.port == null) {
        if (other.port != null) {
          return false;
        }
      } else if (!this.port.equals(other.port)) {
        return false;
      }
      if (this.query == null) {
        if (other.query != null) {
          return false;
        }
      } else if (!this.query.equals(other.query)) {
        return false;
      }
      if (this.scheme == null) {
        if (other.scheme != null) {
          return false;
        }
      } else if (!this.scheme.equals(other.scheme)) {
        return false;
      }
      if (this.schemeData == null) {
        if (other.schemeData != null) {
          return false;
        }
      } else if (!this.schemeData.equals(other.schemeData)) {
        return false;
      }
      if (this.username == null) {
        if (other.username != null) {
          return false;
        }
      } else {
        return !this.username.equals(other.username);
      }
      return true;
    }

    /**
     * Not documented yet.
     * @return A text string.
     */
    public String GetFragment() {
      return (this.fragment == null) ? ("") : this.fragment;
    }

    /**
     * Not documented yet.
     * @return A text string.
     */
    public String GetHash() {
      return ((this.fragment)==null || (this.fragment).length()==0) ? "" : "#" +
        this.fragment;
    }

    /**
     * Not documented yet.
     * @return A text string.
     */
    public String GetHost() {
      return (this.port.length() == 0) ? HostSerialize (this.host) :
        (HostSerialize (this.host) + ":" + this.port);
    }

    /**
     * Not documented yet.
     * @return A text string.
     */
    public String GetHostname() {
      return HostSerialize (this.host);
    }

    /**
     * Not documented yet.
     * @return A text string.
     */
    public String GetPassword() {
      return this.password == null ? "" : this.password;
    }

    /**
     * Not documented yet.
     * @return A text string.
     */
    public String GetPath() {
      return this.path;
    }

    /**
     * Not documented yet.
     * @return A text string.
     */
    public String GetPathname() {
      if (this.schemeData.length() > 0) {
        return this.schemeData;
      } else {
        return this.path;
      }
    }

    /**
     * Not documented yet.
     * @return A text string.
     */
    public String GetPort() {
      return this.port;
    }

    /**
     * Not documented yet.
     * @return A text string.
     */
    public String GetProtocol() {
      return this.scheme + ":";
    }

    /**
     * Not documented yet.
     * @return A text string.
     */
    public String GetQueryString() {
      return this.query == null ? "" : this.query;
    }

    /**
     * Not documented yet.
     * @return A text string.
     */
    public String GetScheme() {
      return this.scheme;
    }

    /**
     * Not documented yet.
     * @return A text string.
     */
    public String GetSchemeData() {
      return this.schemeData;
    }

    /**
     * Not documented yet.
     * @return A text string.
     */
    public String GetSearch() {
      return (this.query == null || this.query.length() == 0) ? "" :
        "?" + this.query;
    }

    /**
     * Not documented yet.
     * @return A text string.
     */
    public String GetUsername() {
      return this.username == null ? "" : this.username;
    }

    /**
     * Not documented yet.
     * @return The return value is not documented yet.
     */
    @Override public final int hashCode() {
      int prime = 31;
      int result = 17;
      if (this.fragment != null) {
        for (int i = 0; i < this.fragment.length(); ++i) {
          result = (prime * result) + this.fragment.charAt(i);
        }
      }
      if (this.host != null) {
        for (int i = 0; i < this.host.length(); ++i) {
          result = (prime * result) + this.host.charAt(i);
        }
      }
      if (this.password != null) {
        for (int i = 0; i < this.password.length(); ++i) {
          result = (prime * result) + this.password.charAt(i);
        }
      }
      if (this.path != null) {
        for (int i = 0; i < this.path.length(); ++i) {
          result = (prime * result) + this.path.charAt(i);
        }
      }
      if (this.port != null) {
        for (int i = 0; i < this.port.length(); ++i) {
          result = (prime * result) + this.port.charAt(i);
        }
      }
      if (this.query != null) {
        for (int i = 0; i < this.query.length(); ++i) {
          result = (prime * result) + this.query.charAt(i);
        }
      }
      if (this.scheme != null) {
        for (int i = 0; i < this.scheme.length(); ++i) {
          result = (prime * result) + this.scheme.charAt(i);
        }
      }
      if (this.schemeData != null) {
        for (int i = 0; i < this.schemeData.length(); ++i) {
          result = (prime * result) + this.schemeData.charAt(i);
        }
      }
      if (this.username != null) {
        for (int i = 0; i < this.username.length(); ++i) {
          result = (prime * result) + this.username.charAt(i);
        }
      }
      return result;
    }

    /**
     * Not documented yet.
     * @return The return value is not documented yet.
     */
    @Override public final String toString() {
      StringBuilder builder = new StringBuilder();
      builder.append (this.scheme);
      builder.append (':');
      if (this.scheme.equals("file") ||
        this.scheme.equals("http") ||
        this.scheme.equals("https") ||
        this.scheme.equals("ftp") ||
        this.scheme.equals("gopher") ||
        this.scheme.equals("ws") ||
        this.scheme.equals("wss")) {
        // NOTE: We check relative schemes here
        // rather than have a relative flag,
        // as specified in the URL Standard
        // (since the protocol can't be changed
        // as this class is immutable, we can
        // do this variation).
        builder.append ("//");
        if (this.username.length() != 0 || this.password != null) {
          builder.append (this.username);
          if (this.password != null) {
            builder.append (':');
            builder.append (this.password);
          }
          builder.append ('@');
        }
        builder.append (HostSerialize (this.host));
        if (this.port.length() > 0) {
          builder.append (':');
          builder.append (this.port);
        }
        builder.append (this.path);
      } else {
        builder.append (this.schemeData);
      }
      if (this.query != null) {
        builder.append ('?');
        builder.append (this.query);
      }
      if (this.fragment != null) {
        builder.append ('#');
        builder.append (this.fragment);
      }
      return builder.toString();
    }
  }
