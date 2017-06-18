/*
Written in 2013 by Peter Occil.
Any copyright is dedicated to the Public Domain.
http://creativecommons.org/publicdomain/zero/1.0/

If you like this, you should donate to Peter O.
at: http://peteroupc.github.io/
*/
package com.upokecenter.util;

/**
 *
 * Contains utility methods for processing Uniform
 * Resource Identifiers (URIs) and Internationalized
 * Resource Identifiers (IRIs) under RFC3986 and RFC3987,
 * respectively.  In the following documentation, URIs
 * and IRIs include URI references and IRI references,
 * for convenience.
 *
 * @author Peter
 *
 */
public final class URIUtility {

  /**
   * Specifies whether certain characters are allowed when
   * parsing IRIs and URIs.
   * @author Peter
   *
   */
  public enum ParseMode {
    /**
     *   The rules follow the syntax for parsing IRIs.
     *  In particular, many internationalized characters
     *  are allowed.  Strings with unpaired surrogate
     *  code points are considered invalid.
     */
    IRIStrict,
    /**
     * The rules follow the syntax for parsing IRIs,
     * except that non-ASCII characters are not allowed.
     */
    URIStrict,
    /**
     * The rules only check for the appropriate
     * delimiters when splitting the path, without checking if all the characters
     * in each component are valid.  Even with this mode, strings with unpaired
     * surrogate code points are considered invalid.
     */
    IRILenient,
    /**
     * The rules only check for the appropriate
     * delimiters when splitting the path, without checking if all the characters
     * in each component are valid.  Non-ASCII characters
     * are not allowed.
     */
    URILenient,
    /**
     * The rules only check for the appropriate
     * delimiters when splitting the path, without checking if all the characters
     * in each component are valid.  Unpaired surrogate code points
     * are treated as though they were replacement characters instead
     * for the purposes of these rules, so that strings with those code
     * points are not considered invalid strings.
     */
    IRISurrogateLenient
  }

  private static final String hex="0123456789ABCDEF";

  private static void appendAuthority(
      StringBuilder builder, String ref, int[] segments){
    if(segments[2]>=0){
      builder.append("//");
      builder.append(ref.substring(segments[2],segments[3]));
    }
  }

  private static void appendFragment(
      StringBuilder builder, String ref, int[] segments){
    if(segments[8]>=0){
      builder.append('#');
      builder.append(ref.substring(segments[8],segments[9]));
    }
  }

  private static void appendNormalizedPath(
      StringBuilder builder, String ref, int[] segments){
    builder.append(normalizePath(ref.substring(segments[4],segments[5])));
  }

  private static void appendPath(
      StringBuilder builder, String ref, int[] segments){
    builder.append(ref.substring(segments[4],segments[5]));
  }

  private static void appendQuery(
      StringBuilder builder, String ref, int[] segments){
    if(segments[6]>=0){
      builder.append('?');
      builder.append(ref.substring(segments[6],segments[7]));
    }
  }

  private static void appendScheme(
      StringBuilder builder, String ref, int[] segments){
    if(segments[0]>=0){
      builder.append(ref.substring(segments[0],segments[1]));
      builder.append(':');
    }
  }

  /**
   * Escapes characters that cannot appear in URIs or IRIs.
   * The function is idempotent; that is, calling the function
   * again on the result with the same mode doesn't change the result.
   *
   * @param s a string to escape.
   * @param mode One of the following values:
   * <ul>
   * <li>0 - Non-ASCII characters and other characters that
   * cannot appear in a URI are
   * escaped, whether or not the string is a valid URI.
   * Unpaired surrogates are treated as U+FFFD (Replacement Character).
   * (Note that square brackets "[" and "]" can only appear in the authority
   * component of a URI or IRI; elsewhere they will be escaped.)</li>
   * <li>1 - Only non-ASCII characters are escaped. If the
   * string is not a valid IRI, returns null instead.</li>
   * <li>2 - Only non-ASCII characters are escaped, whether or
   * not the string is a valid IRI.  Unpaired surrogates
   * are treated as U+FFFD (Replacement Character).</li>
   * <li>3 - Similar to 0, except that illegal percent encodings
   * are also escaped.</li>
   * </ul>
   * @return a string possibly containing escaped characters,
   * or null if s is null.
   */
  public static String escapeURI(String s, int mode){
    if(s==null)return null;
    int[] components=null;
    if(mode==1){
      components=splitIRI(s,ParseMode.IRIStrict);
      if(components==null)return null;
    } else {
      components=splitIRI(s,ParseMode.IRISurrogateLenient);
    }
    int index=0;
    int sLength=s.length();
    StringBuilder builder=new StringBuilder();
    while(index<sLength){
      int c=s.charAt(index);
      if(c>=0xD800 && c<=0xDBFF && index+1<sLength &&
          s.charAt(index+1)>=0xDC00 && s.charAt(index+1)<=0xDFFF){
        // Get the Unicode code point for the surrogate pair
        c=0x10000+(c-0xD800)*0x400+(s.charAt(index+1)-0xDC00);
        index++;
      } else if(c>=0xD800 && c<=0xDFFF){
        c=0xFFFD;
      }
      if(mode==0 || mode==3){
        if(c=='%' && mode==3){
          // Check for illegal percent encoding
          if(index+2>=sLength || !isHexChar(s.charAt(index+1)) ||
              !isHexChar(s.charAt(index+2))){
            percentEncodeUtf8(builder,c);
          } else {
            builder.appendCodePoint(c);
          }
          index++;
          continue;
        }
        if(c>=0x7F || c<=0x20 || ((c&0x7F)==c && "{}|^\\`<>\"".indexOf((char)c)>=0)){
          percentEncodeUtf8(builder,c);
        } else if(c=='[' || c==']'){
          if(components!=null && index>=components[2] && index<components[3]){
            // within the authority component, so don't percent-encode
            builder.appendCodePoint(c);
          } else {
            // percent encode
            percentEncodeUtf8(builder,c);
          }
        } else {
          builder.appendCodePoint(c);
        }
      } else if(mode==1 || mode==2){
        if(c>=0x80){
          percentEncodeUtf8(builder,c);
        } else if(c=='[' || c==']'){
          if(components!=null && index>=components[2] && index<components[3]){
            // within the authority component, so don't percent-encode
            builder.appendCodePoint(c);
          } else {
            // percent encode
            percentEncodeUtf8(builder,c);
          }
        } else {
          builder.appendCodePoint(c);
        }
      }
      index++;
    }
    return builder.toString();
  }

  /**
   * Determines whether the string is a valid IRI
   * with a scheme component.  This can be used
   * to check for relative IRI references.
   *
   * The following cases return true:
   * <pre>
   * example://y/z     xx-x:mm   example:/ww
   * </pre>
   * The following cases return false:
   * <pre>
   * x@y:/z    /x/y/z      example.xyz
   * </pre>
   *
   * @param ref A string
   * @return true if the string is a valid IRI and
   * has a scheme component, false otherwise
   */
  public static boolean hasScheme(String ref){
    int[] segments=splitIRI(ref);
    return segments!=null && segments[0]>=0;
  }
  /**
   * Determines whether the string is a valid URI
   * with a scheme component.  This can be used
   * to check for relative URI references.
   *
   * The following cases return true:
   * <pre>
   * example://y/z     xx-x:mm   example:/ww
   * </pre>
   * The following cases return false:
   * <pre>
   * x@y:/z    /x/y/z      example.xyz
   * </pre>
   *
   *
   * @param ref A string
   * @return true if the string is a valid URI (ASCII
   * characters only) and has a scheme component,
   * false otherwise.
   */
  public static boolean hasSchemeForURI(String ref){
    int[] segments=splitIRI(ref,ParseMode.URIStrict);
    return segments!=null && segments[0]>=0;
  }
  private static boolean isHexChar(char c) {
    return ((c>='a' && c<='f') ||
        (c>='A' && c<='F') ||
        (c>='0' && c<='9'));
  }
  private static boolean isIfragmentChar(int c){
    // '%' omitted
    return ((c>='a' && c<='z') ||
        (c>='A' && c<='Z') ||
        (c>='0' && c<='9') ||
        ((c&0x7F)==c && "/?-._~:@!$&'()*+,;=".indexOf((char)c)>=0) ||
        (c>=0xa0 && c<=0xd7ff) ||
        (c>=0xf900 && c<=0xfdcf) ||
        (c>=0xfdf0 && c<=0xffef) ||
        (c>=0x10000 && c<=0xefffd && (c&0xFFFE)!=0xFFFE));
  }
  private static boolean isIpchar(int c){
    // '%' omitted
    return ((c>='a' && c<='z') ||
        (c>='A' && c<='Z') ||
        (c>='0' && c<='9') ||
        ((c&0x7F)==c && "/-._~:@!$&'()*+,;=".indexOf((char)c)>=0) ||
        (c>=0xa0 && c<=0xd7ff) ||
        (c>=0xf900 && c<=0xfdcf) ||
        (c>=0xfdf0 && c<=0xffef) ||
        (c>=0x10000 && c<=0xefffd && (c&0xFFFE)!=0xFFFE));
  }

  private static boolean isIqueryChar(int c){
    // '%' omitted
    return ((c>='a' && c<='z') ||
        (c>='A' && c<='Z') ||
        (c>='0' && c<='9') ||
        ((c&0x7F)==c && "/?-._~:@!$&'()*+,;=".indexOf((char)c)>=0) ||
        (c>=0xa0 && c<=0xd7ff) ||
        (c>=0xe000 && c<=0xfdcf) ||
        (c>=0xfdf0 && c<=0xffef) ||
        (c>=0x10000 && c<=0x10fffd && (c&0xFFFE)!=0xFFFE));
  }

  private static boolean isIRegNameChar(int c){
    // '%' omitted
    return ((c>='a' && c<='z') ||
        (c>='A' && c<='Z') ||
        (c>='0' && c<='9') ||
        ((c&0x7F)==c && "-._~!$&'()*+,;=".indexOf((char)c)>=0) ||
        (c>=0xa0 && c<=0xd7ff) ||
        (c>=0xf900 && c<=0xfdcf) ||
        (c>=0xfdf0 && c<=0xffef) ||
        (c>=0x10000 && c<=0xefffd && (c&0xFFFE)!=0xFFFE));
  }
  private static boolean isIUserInfoChar(int c){
    // '%' omitted
    return ((c>='a' && c<='z') ||
        (c>='A' && c<='Z') ||
        (c>='0' && c<='9') ||
        ((c&0x7F)==c && "-._~:!$&'()*+,;=".indexOf((char)c)>=0) ||
        (c>=0xa0 && c<=0xd7ff) ||
        (c>=0xf900 && c<=0xfdcf) ||
        (c>=0xfdf0 && c<=0xffef) ||
        (c>=0x10000 && c<=0xefffd && (c&0xFFFE)!=0xFFFE));
  }

  /**
   *
   * Determines whether the substring is a valid CURIE
   * reference under RDFa 1.1. (The CURIE reference is
   * the part after the colon.)
   *
   * @param s A string.
   * @param offset Index of the first character of a substring
   * to check for a CURIE reference.
   * @param length Length of the substring to check for a
   * CURIE reference.
   *
   */
  public static boolean isValidCurieReference(String s, int offset, int length){
    if(s==null)return false;
    if(offset<0||length<0||offset+length>s.length())
      throw new IndexOutOfBoundsException();
    if(length==0)
      return true;
    int index=offset;
    int sLength=offset+length;
    int state=0;
    if(index+2<=sLength && s.charAt(index)=='/' && s.charAt(index+1)=='/')
      // has an authority, which is not allowed
      return false;
    state=0; // IRI Path
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
      if(c=='%'){
        // Percent encoded character
        if(index+2<sLength && isHexChar(s.charAt(index+1)) &&
            isHexChar(s.charAt(index+2))){
          index+=3;
          continue;
        } else
          return false;
      }
      if(state==0){ // Path
        if(c=='?'){
          state=1;//move to query state
        } else if(c=='#'){
          state=2;//move to fragment state
        } else if(!isIpchar(c))return false;
        index++;
      } else if(state==1){ // Query
        if(c=='#'){
          state=2;//move to fragment state
        } else if(!isIqueryChar(c))return false;
        index++;
      } else if(state==2){ // Fragment
        if(!isIfragmentChar(c))return false;
        index++;
      }
    }
    return true;
  }

  public static boolean isValidIRI(String s){
    return splitIRI(s)!=null;
  }

  private static String normalizePath(String path){
    int len=path.length();
    if(len==0 || path.equals("..") || path.equals("."))
      return "";
    if(path.indexOf("/.")<0 && path.indexOf("./")<0)
      return path;
    StringBuilder builder=new StringBuilder();
    int index=0;
    while(index<len){
      char c=path.charAt(index);
      if((index+3<=len && c=='/' &&
          path.charAt(index+1)=='.' &&
          path.charAt(index+2)=='/') ||
          (index+2==len && c=='.' &&
          path.charAt(index+1)=='.')){
        // begins with "/./" or is "..";
        // move index by 2
        index+=2;
        continue;
      } else if((index+3<=len && c=='.' &&
          path.charAt(index+1)=='.' &&
          path.charAt(index+2)=='/')){
        // begins with "../";
        // move index by 3
        index+=3;
        continue;
      } else if((index+2<=len && c=='.' &&
          path.charAt(index+1)=='/') ||
          (index+1==len && c=='.')){
        // begins with "./" or is ".";
        // move index by 1
        index+=1;
        continue;
      } else if(index+2==len && c=='/' &&
          path.charAt(index+1)=='.'){
        // is "/."; append '/' and break
        builder.append('/');
        break;
      } else if((index+3==len && c=='/' &&
          path.charAt(index+1)=='.' &&
          path.charAt(index+2)=='.')){
        // is "/.."; remove last segment,
        // append "/" and return
        int index2=builder.length()-1;
        while(index2>=0){
          if(builder.charAt(index2)=='/'){
            break;
          }
          index2--;
        }
        if(index2<0) {
          index2=0;
        }
        builder.setLength(index2);
        builder.append('/');
        break;
      } else if((index+4<=len && c=='/' &&
          path.charAt(index+1)=='.' &&
          path.charAt(index+2)=='.' &&
          path.charAt(index+3)=='/')){
        // begins with "/../"; remove last segment
        int index2=builder.length()-1;
        while(index2>=0){
          if(builder.charAt(index2)=='/'){
            break;
          }
          index2--;
        }
        if(index2<0) {
          index2=0;
        }
        builder.setLength(index2);
        index+=3;
        continue;
      } else {
        builder.append(c);
        index++;
        while(index<len){
          // Move the rest of the
          // path segment until the next '/'
          c=path.charAt(index);
          if(c=='/') {
            break;
          }
          builder.append(c);
          index++;
        }
      }
    }
    return builder.toString();
  }

  private static int parseDecOctet(String s, int index,
      int endOffset, int c, int delim){
    if(c>='1' && c<='9' && index+2<endOffset &&
        (s.charAt(index+1)>='0' && s.charAt(index+1)<='9') &&
        s.charAt(index+2)==delim)
      return (c-'0')*10+(s.charAt(index+1)-'0');
    else if(c=='2' && index+3<endOffset &&
        (s.charAt(index+1)=='5') &&
        (s.charAt(index+2)>='0' && s.charAt(index+2)<='5') &&
        s.charAt(index+3)==delim)
      return 250+(s.charAt(index+2)-'0');
    else if(c=='2' && index+3<endOffset &&
        (s.charAt(index+1)>='0' && s.charAt(index+1)<='4') &&
        (s.charAt(index+2)>='0' && s.charAt(index+2)<='9') &&
        s.charAt(index+3)==delim)
      return 200+(s.charAt(index+1)-'0')*10+(s.charAt(index+2)-'0');
    else if(c=='1' && index+3<endOffset &&
        (s.charAt(index+1)>='0' && s.charAt(index+1)<='9') &&
        (s.charAt(index+2)>='0' && s.charAt(index+2)<='9') &&
        s.charAt(index+3)==delim)
      return 100+(s.charAt(index+1)-'0')*10+(s.charAt(index+2)-'0');
    else if(c>='0' && c<='9' && index+1<endOffset &&
        s.charAt(index+1)==delim)
      return (c-'0');
    else return -1;
  }

  private static int parseIPLiteral(String s, int offset, int endOffset){
    int index=offset;
    if(offset==endOffset)
      return -1;
    // Assumes that the character before offset
    // is a '['
    if(s.charAt(index)=='v'){
      // IPvFuture
      index++;
      boolean hex=false;
      while(index<endOffset){
        char c=s.charAt(index);
        if(isHexChar(c)){
          hex=true;
        } else {
          break;
        }
        index++;
      }
      if(!hex)return -1;
      if(index>=endOffset || s.charAt(index)!='.')
        return -1;
      index++;
      hex=false;
      while(index<endOffset){
        char c=s.charAt(index);
        if((c>='a' && c<='z') ||
            (c>='A' && c<='Z') ||
            (c>='0' && c<='9') ||
            ((c&0x7F)==c && ":-._~!$&'()*+,;=".indexOf(c)>=0)){
          hex=true;
        } else {
          break;
        }
        index++;
      }
      if(!hex)return -1;
      if(index>=endOffset || s.charAt(index)!=']')
        return -1;
      index++;
      return index;
    } else if(s.charAt(index)==':' ||
        isHexChar(s.charAt(index))){
      // IPv6 Address
      int phase1=0;
      int phase2=0;
      boolean phased=false;
      boolean expectHex=false;
      boolean expectColon=false;
      while(index<endOffset){
        char c=s.charAt(index);
        if(c==':' && !expectHex){
          if((phase1+(phased ? 1 : 0)+phase2)>=8)
            return -1;
          index++;
          if(index<endOffset && s.charAt(index)==':'){
            if(phased)return -1;
            phased=true;
            index++;
          }
          expectHex=true;
          expectColon=false;
          continue;
        } else if((c>='0' && c<='9') && !expectColon &&
            (phased || (phase1+(phased ? 1 : 0)+phase2)==6)){
          // Check for IPv4 address
          int decOctet=parseDecOctet(s,index,endOffset,c,'.');
          if(decOctet>=0){
            if((phase1+(phased ? 1 : 0)+phase2)>6)
              // IPv4 address illegal at this point
              return -1;
            else {
              // Parse the rest of the IPv4 address
              phase2+=2;
              if(decOctet>=100) {
                index+=4;
              } else if(decOctet>=10) {
                index+=3;
              } else {
                index+=2;
              }
              decOctet=parseDecOctet(s,index,endOffset,
                  (index<endOffset) ? s.charAt(index) : '\0','.');
              if(decOctet>=100) {
                index+=4;
              } else if(decOctet>=10) {
                index+=3;
              } else if(decOctet>=0) {
                index+=2;
              } else return -1;
              decOctet=parseDecOctet(s,index,endOffset,
                  (index<endOffset) ? s.charAt(index) : '\0','.');
              if(decOctet>=100) {
                index+=4;
              } else if(decOctet>=10) {
                index+=3;
              } else if(decOctet>=0) {
                index+=2;
              } else return -1;
              decOctet=parseDecOctet(s,index,endOffset,
                  (index<endOffset) ? s.charAt(index) : '\0',']');
              if(decOctet<0) {
                decOctet=parseDecOctet(s,index,endOffset,
                    (index<endOffset) ? s.charAt(index) : '\0','%');
              }
              if(decOctet>=100) {
                index+=3;
              } else if(decOctet>=10) {
                index+=2;
              } else if(decOctet>=0) {
                index+=1;
              } else return -1;
              break;
            }
          }
        }
        if(isHexChar(c) && !expectColon){
          if(phased){
            phase2++;
          } else {
            phase1++;
          }
          index++;
          for(int i=0;i<3;i++){
            if(index<endOffset && isHexChar(s.charAt(index))) {
              index++;
            } else {
              break;
            }
          }
          expectHex=false;
          expectColon=true;
        } else {
          break;
        }
      }
      if((phase1+phase2)!=8 && !phased)
        return -1;
      if((phase1+1+phase2)>8 && phased)
        return -1;
      if(index>=endOffset)return -1;
      if(s.charAt(index)!=']' && s.charAt(index)!='%')
        return -1;
      if(s.charAt(index)=='%'){
        if(index+2<endOffset && s.charAt(index+1)=='2' &&
            s.charAt(index+2)=='5'){
          // Zone identifier in an IPv6 address
          // (see RFC6874)
          index+=3;
          boolean haveChar=false;
          while(index<endOffset){
            char c=s.charAt(index);
            if(c==']')
              return (haveChar) ? index+1 : -1;
            else if(c=='%'){
              if(index+2<endOffset && isHexChar(s.charAt(index+1)) &&
                  isHexChar(s.charAt(index+2))){
                index+=3;
                haveChar=true;
                continue;
              } else return -1;
            } else if((c>='a' && c<='z') || (c>='A' && c<='Z') ||
                (c>='0' && c<='9') || c=='.' || c=='_' || c=='-' || c=='~'){
              // unreserved character under RFC3986
              index++;
              haveChar=true;
              continue;
            } else return -1;
          }
          return -1;
        } else return -1;
      }
      index++;
      return index;
    } else
      return -1;
  }

  private static String pathParent(String ref, int startIndex, int endIndex){
    if(startIndex>endIndex)return "";
    endIndex--;
    while(endIndex>=startIndex){
      if(ref.charAt(endIndex)=='/')
        return ref.substring(startIndex,endIndex+1);
      endIndex--;
    }
    return "";
  }

  private static void percentEncode(StringBuilder buffer, int b){
    buffer.append('%');
    buffer.append(hex.charAt((b>>4)&0x0F));
    buffer.append(hex.charAt((b)&0x0F));
  }

  private static void percentEncodeUtf8(StringBuilder buffer, int cp){
    if(cp<=0x7F){
      buffer.append('%');
      buffer.append(hex.charAt((cp>>4)&0x0F));
      buffer.append(hex.charAt((cp)&0x0F));
    } else if(cp<=0x7FF){
      percentEncode(buffer,(0xC0|((cp>>6)&0x1F)));
      percentEncode(buffer,(0x80|(cp   &0x3F)));
    } else if(cp<=0xFFFF){
      percentEncode(buffer,(0xE0|((cp>>12)&0x0F)));
      percentEncode(buffer,(0x80|((cp>>6 )&0x3F)));
      percentEncode(buffer,(0x80|(cp      &0x3F)));
    } else {
      percentEncode(buffer,(0xF0|((cp>>18)&0x07)));
      percentEncode(buffer,(0x80|((cp>>12)&0x3F)));
      percentEncode(buffer,(0x80|((cp>>6 )&0x3F)));
      percentEncode(buffer,(0x80|(cp      &0x3F)));
    }
  }

  /**
   *
   * Resolves a URI or IRI relative to another URI or IRI.
   *
   * @param ref an absolute or relative URI reference
   * @param baseURI an absolute URI reference.
   * @return the resolved IRI, or null if ref is null or is not a
   * valid IRI.  If base
   * is null or is not a valid IRI, returns ref.
   */
  public static String relativeResolve(String ref, String baseURI){
    return relativeResolve(ref,baseURI,ParseMode.IRIStrict);
  }
  /**
   *
   * Resolves a URI or IRI relative to another URI or IRI.
   *
   * @param ref an absolute or relative URI reference
   * @param baseURI an absolute URI reference.
   * @param parseMode Specifies whether certain characters are allowed
   * in <i>ref</i> and <i>base</i>.
   * @return the resolved IRI, or null if ref is null or is not a
   * valid IRI.  If base
   * is null or is not a valid IRI, returns ref.
   */
  public static String relativeResolve(String ref, String baseURI, ParseMode parseMode){
    int[] segments=splitIRI(ref,parseMode);
    if(segments==null)return null;
    int[] segmentsBase=splitIRI(baseURI,parseMode);
    if(segmentsBase==null)return ref;
    StringBuilder builder=new StringBuilder();
    if(segments[0]>=0){ // scheme present
      appendScheme(builder,ref,segments);
      appendAuthority(builder,ref,segments);
      appendNormalizedPath(builder,ref,segments);
      appendQuery(builder,ref,segments);
      appendFragment(builder,ref,segments);
    } else if(segments[2]>=0){ // authority present
      appendScheme(builder,baseURI,segmentsBase);
      appendAuthority(builder,ref,segments);
      appendNormalizedPath(builder,ref,segments);
      appendQuery(builder,ref,segments);
      appendFragment(builder,ref,segments);
    } else if(segments[4]==segments[5]){
      appendScheme(builder,baseURI,segmentsBase);
      appendAuthority(builder,baseURI,segmentsBase);
      appendPath(builder,baseURI,segmentsBase);
      if(segments[6]>=0){
        appendQuery(builder,ref,segments);
      } else {
        appendQuery(builder,baseURI,segmentsBase);
      }
      appendFragment(builder,ref,segments);
    } else {
      appendScheme(builder,baseURI,segmentsBase);
      appendAuthority(builder,baseURI,segmentsBase);
      if(segments[4]<segments[5] && ref.charAt(segments[4])=='/'){
        appendNormalizedPath(builder,ref,segments);
      } else {
        StringBuilder merged=new StringBuilder();
        if(segmentsBase[2]>=0 && segmentsBase[4]==segmentsBase[5]){
          merged.append('/');
          appendPath(merged,ref,segments);
          builder.append(normalizePath(merged.toString()));
        } else {
          merged.append(pathParent(baseURI,segmentsBase[4],segmentsBase[5]));
          appendPath(merged,ref,segments);
          builder.append(normalizePath(merged.toString()));
        }
      }
      appendQuery(builder,ref,segments);
      appendFragment(builder,ref,segments);
    }
    return builder.toString();
  }

  /**
   * Parses an Internationalized Resource Identifier (IRI) reference
   * under RFC3987.  If the IRI reference is syntactically valid, splits
   * the string into its components and returns an array containing
   * the indices into the components.
   *
   * @param s A string.
   * @return If the string is a valid IRI reference, returns an array of 10
   * integers.  Each of the five pairs corresponds to the start
   * and end index of the IRI's scheme, authority, path, query,
   * or fragment component, respectively.  If a component is absent,
   * both indices in that pair will be -1.  If the string is null
   * or is not a valid IRI, returns null.
   */
  public static int[] splitIRI(String s){
    return splitIRI(s,ParseMode.IRIStrict);
  }

  /**
   * Parses a substring that represents an
   * Internationalized Resource Identifier (IRI)
   * under RFC3987.  If the IRI is syntactically valid, splits
   * the string into its components and returns an array containing
   * the indices into the components.
   *
   * @param s A string.
   * @param offset Index of the first character of a substring
   * to check for an IRI.
   * @param length Length of the substring to check for an IRI.
   * @param parseMode Specifies whether certain characters are allowed
   * in the string.
   * @return If the string is a valid IRI, returns an array of 10
   * integers.  Each of the five pairs corresponds to the start
   * and end index of the IRI's scheme, authority, path, query,
   * or fragment component, respectively.  If a component is absent,
   * both indices in that pair will be -1 (an index won't be less than
   * 0 in any other case).  If the string is null
   * or is not a valid IRI, returns null.
   */
  public static int[] splitIRI(String s,
      int offset, int length, ParseMode parseMode){
    if(s==null)return null;
    if(offset<0||length<0||offset+length>s.length())
      throw new IndexOutOfBoundsException();
    int[] retval=new int[]{-1,-1,-1,-1,-1,-1,-1,-1,-1,-1};
    if(length==0){
      retval[4]=0;
      retval[5]=0;
      return retval;
    }
    boolean asciiOnly=(parseMode==ParseMode.URILenient || parseMode==ParseMode.URIStrict);
    boolean strict=(parseMode==ParseMode.URIStrict || parseMode==ParseMode.IRIStrict);
    int index=offset;
    int sLength=offset+length;
    boolean scheme=false;
    // scheme
    while(index<sLength){
      int c=s.charAt(index);
      if(index>offset && c==':'){
        scheme=true;
        retval[0]=offset;
        retval[1]=index;
        index++;
        break;
      }
      if(strict && index==offset && !((c>='a' && c<='z') || (c>='A' && c<='Z'))){
        break;
      }
      else if(strict && index>offset && !((c>='a' && c<='z') || (c>='A' && c<='Z') || (c>='0' && c<='9') ||
          c=='+' && c=='-' && c=='.')){
        break;
      }
      else if(!strict && (c=='#' || c==':' || c=='?' || c=='/')){
        break;
      }
      index++;
    }
    if(!scheme) {
      index=offset;
    }
    int state=0;
    if(index+2<=sLength && s.charAt(index)=='/' && s.charAt(index+1)=='/'){
      // authority
      // (index+2, sLength)
      index+=2;
      int authorityStart=index;
      retval[2]=authorityStart;
      retval[3]=sLength;
      state=0; // userinfo
      // Check for userinfo
      while(index<sLength){
        int c=s.charAt(index);
        if(asciiOnly && c>=0x80)
          return null;
        if(c>=0xD800 && c<=0xDBFF && index+1<sLength &&
            s.charAt(index+1)>=0xDC00 && s.charAt(index+1)<=0xDFFF){
          // Get the Unicode code point for the surrogate pair
          c=0x10000+(c-0xD800)*0x400+(s.charAt(index+1)-0xDC00);
          index++;
        } else if(c>=0xD800 && c<=0xDFFF){
          if(parseMode==ParseMode.IRISurrogateLenient) {
            c=0xFFFD;
          } else
            return null;
        }
        if(c=='%' && (state==0 || state==1) && strict){
          // Percent encoded character (except in port)
          if(index+2<sLength && isHexChar(s.charAt(index+1)) &&
              isHexChar(s.charAt(index+2))){
            index+=3;
            continue;
          } else
            return null;
        }
        if(state==0){ // User info
          if(c=='/' || c=='?' || c=='#'){
            // not user info
            state=1;
            index=authorityStart;
            continue;
          } else if(strict && c=='@'){
            // is user info
            index++;
            state=1;
            continue;
          } else if(strict && isIUserInfoChar(c)){
            index++;
            if(index==sLength){
              // not user info
              state=1;
              index=authorityStart;
              continue;
            }
          } else {
            // not user info
            state=1;
            index=authorityStart;
            continue;
          }
        } else if(state==1){ // host
          if(c=='/' || c=='?' || c=='#'){
            // end of authority
            retval[3]=index;
            break;
          } else if(!strict){
            index++;
          } else if(c=='['){
            index++;
            index=parseIPLiteral(s,index,sLength);
            if(index<0)return null;
            continue;
          } else if(c==':'){
            // port
            state=2;
            index++;
          } else if(isIRegNameChar(c)){
            // is valid host name char
            // (note: IPv4 addresses included
            // in ireg-name)
            index++;
          } else
            return null;
        } else if(state==2){ // Port
          if(c=='/' || c=='?' || c=='#'){
            // end of authority
            retval[3]=index;
            break;
          } else if(c>='0' && c<='9'){
            index++;
          } else
            return null;
        }
      }
    }
    boolean colon=false;
    boolean segment=false;
    boolean fullyRelative=(index==offset);
    retval[4]=index; // path offsets
    retval[5]=sLength;
    state=0; // IRI Path
    while(index<sLength){
      // Get the next Unicode character
      int c=s.charAt(index);
      if(asciiOnly && c>=0x80)
        return null;
      if(c>=0xD800 && c<=0xDBFF && index+1<sLength &&
          s.charAt(index+1)>=0xDC00 && s.charAt(index+1)<=0xDFFF){
        // Get the Unicode code point for the surrogate pair
        c=0x10000+(c-0xD800)*0x400+(s.charAt(index+1)-0xDC00);
        index++;
      } else if(c>=0xD800 && c<=0xDFFF)
        // error
        return null;
      if(c=='%' && strict){
        // Percent encoded character
        if(index+2<sLength && isHexChar(s.charAt(index+1)) &&
            isHexChar(s.charAt(index+2))){
          index+=3;
          continue;
        } else
          return null;
      }
      if(state==0){ // Path
        if(c==':' && fullyRelative){
          colon=true;
        } else if(c=='/' && fullyRelative && !segment){
          // noscheme path can't have colon before slash
          if(strict && colon)return null;
          segment=true;
        }
        if(c=='?'){
          retval[5]=index;
          retval[6]=index+1;
          retval[7]=sLength;
          state=1;//move to query state
        } else if(c=='#'){
          retval[5]=index;
          retval[8]=index+1;
          retval[9]=sLength;
          state=2;//move to fragment state
        } else if(strict && !isIpchar(c))return null;
        index++;
      } else if(state==1){ // Query
        if(c=='#'){
          retval[7]=index;
          retval[8]=index+1;
          retval[9]=sLength;
          state=2;//move to fragment state
        } else if(strict && !isIqueryChar(c))return null;
        index++;
      } else if(state==2){ // Fragment
        if(strict && !isIfragmentChar(c))return null;
        index++;
      }
    }
    if(strict && fullyRelative && colon && !segment)
      return null; // ex. "x@y:z"
    return retval;
  }

  /**
   * Parses an Internationalized Resource Identifier (IRI) reference
   * under RFC3987.  If the IRI is syntactically valid, splits
   * the string into its components and returns an array containing
   * the indices into the components.
   *
   * @param s A string.
   * @param parseMode Specifies whether certain characters are allowed
   * in the string.
   * @return If the string is a valid IRI reference, returns an array of 10
   * integers.  Each of the five pairs corresponds to the start
   * and end index of the IRI's scheme, authority, path, query,
   * or fragment component, respectively.  If a component is absent,
   * both indices in that pair will be -1.  If the string is null
   * or is not a valid IRI, returns null.
   */
  public static int[] splitIRI(String s, ParseMode parseMode){
    if(s==null)return null;
    return splitIRI(s,0,s.length(),parseMode);
  }
  private URIUtility(){}
}
