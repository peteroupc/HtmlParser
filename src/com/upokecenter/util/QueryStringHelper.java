package com.upokecenter.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Written by Peter O. in 2013.
// Any copyright is dedicated to the Public Domain.
// http://creativecommons.org/publicdomain/zero/1.0/
//
// If you like this, you should donate to Peter O.
// at: http://peteroupc.github.io/
//

public final class QueryStringHelper
{
  private QueryStringHelper(){}
  private static String[] SplitAt(String s, String delimiter){
    if(delimiter==null ||
        delimiter.length()==0)throw new IllegalArgumentException();
    if(s==null || s.length()==0)return new String[]{""};
    int index=0;
    boolean first=true;
    ArrayList<String> strings=null;
    int delimLength=delimiter.length();
    while(true){
      int index2=s.indexOf(delimiter,index);
      if(index2<0){
        if(first)return new String[]{s};
        strings.add(s.substring(index));
        break;
      } else {
        if(first) {
          strings=new ArrayList<String>();
          first=false;
        }
        String newstr=s.substring(index,index2);
        strings.add(newstr);
        index=index2+delimLength;
      }
    }
    return strings.toArray(new String[]{});
  }

  private static int ToHexNumber(int c) {
    if(c>='A' && c<='Z')
      return 10+c-'A';
    else if(c>='a' && c<='z')
      return 10+c-'a';
    else if (c>='0' && c<='9')
      return c-'0';
    return -1;
  }
  private static String PercentDecodeUTF8(String str){
    int len=str.length();
    boolean percent=false;
    for(int i=0;i<len;i++){
      char c=str.charAt(i);
      if(c=='%') {
        percent=true;
      } else if(c>=0x80) // Non-ASCII characters not allowed
        return null;
    }
    if(!percent)return str;// return early if there are no percent decodings
    int cp=0;
    int bytesSeen=0;
    int bytesNeeded=0;
    int lower=0x80;
    int upper=0xBF;
    int markedPos=-1;
    StringBuilder retString=new StringBuilder();
    for(int i=0;i<len;i++){
      int c=str.charAt(i);
      if(c=='%'){
        if(i+2<len){
          int a=ToHexNumber(str.charAt(i+1));
          int b=ToHexNumber(str.charAt(i+2));
          if(a>=0 && b>=0){
            b=(byte) (a*16+b);
            i+=2;
            // b now contains the byte read
            if(bytesNeeded==0){
              // this is the lead byte
              if(b<0x80){
                retString.append((char)b);
                continue;
              } else if(b>=0xc2 && b<=0xdf){
                markedPos=i;
                bytesNeeded=1;
                cp=b-0xc0;
              } else if(b>=0xe0 && b<=0xef){
                markedPos=i;
                lower=(b==0xe0) ? 0xa0 : 0x80;
                upper=(b==0xed) ? 0x9f : 0xbf;
                bytesNeeded=2;
                cp=b-0xe0;
              } else if(b>=0xf0 && b<=0xf4){
                markedPos=i;
                lower=(b==0xf0) ? 0x90 : 0x80;
                upper=(b==0xf4) ? 0x8f : 0xbf;
                bytesNeeded=3;
                cp=b-0xf0;
              } else {
                // illegal byte in UTF-8
                retString.append('\uFFFD');
                continue;
              }
              cp<<=(6*bytesNeeded);
              continue;
            } else {
              // this is a second or further byte
              if(b<lower || b>upper){
                // illegal trailing byte
                cp=bytesNeeded=bytesSeen=0;
                lower=0x80;
                upper=0xbf;
                i=markedPos; // reset to the last marked position
                retString.append('\uFFFD');
                continue;
              }
              // reset lower and upper for the third
              // and further bytes
              lower=0x80;
              upper=0xbf;
              bytesSeen++;
              cp+=(b-0x80)<<(6*(bytesNeeded-bytesSeen));
              markedPos=i;
              if(bytesSeen!=bytesNeeded) {
                // continue if not all bytes needed
                // were read yet
                continue;
              }
              int ret=cp;
              cp=0;
              bytesSeen=0;
              bytesNeeded=0;
              // append the Unicode character
              retString.appendCodePoint(ret);
              continue;
            }
          }
        }
      }
      if(bytesNeeded>0){
        // we expected further bytes here,
        // so emit a replacement character instead
        bytesNeeded=0;
        retString.append('\uFFFD');
      }
      // append the code point as is (we already
      // checked for ASCII characters so this will
      // be simple
      retString.append((char)(c&0xFF));
    }
    if(bytesNeeded>0){
      // we expected further bytes here,
      // so emit a replacement character instead
      bytesNeeded=0;
      retString.append('\uFFFD');
    }
    return retString.toString();
  }
  public static List<String[]> ParseQueryString(
      String input
      ){
    return ParseQueryString(input,null);
  }

  public static List<String[]> ParseQueryString(
      String input, String delimiter
      ){
    if((input)==null)throw new NullPointerException("input");
    if(delimiter==null) {
      // set default delimiter to ampersand
      delimiter="&";
    }
    // Check input for non-ASCII characters
    for(int i=0;i<input.length();i++){
      if(input.charAt(i)>0x7F)
        throw new IllegalArgumentException("input contains a non-ASCII character");
    }
    // split on delimiter
    String[] strings=SplitAt(input,delimiter);
    ArrayList<String[]> pairs=new ArrayList<String[]>();
    for(String str : strings){
      if(str.length()==0) {
        continue;
      }
      // split on key
      int index=str.indexOf('=');
      String name=str;
      String value="";// value is empty if there is no key
      if(index>=0){
        name=str.substring(0,index);
        value=str.substring(index+1);
      }
      name=name.replace('+',' ');
      value=value.replace('+',' ');
      String[] pair=new String[]{name,value};
      pairs.add(pair);
    }
    for(String[] pair : pairs){
      // percent decode the key and value if necessary
      pair[0]=PercentDecodeUTF8(pair[0]);
      pair[1]=PercentDecodeUTF8(pair[1]);
    }
    return pairs;
  }

  private static String[] GetKeyPath(String s){
    int index=s.indexOf('[');
    if(index<0){// start bracket not found
      return new String[]{s};
    }
    ArrayList<String> path=new ArrayList<String>();
    path.add(s.substring(0,index));
    index++;// move to after the bracket
    while(true){
      int endBracket=s.indexOf(']',index);
      if(endBracket<0){ // end bracket not found
        path.add(s.substring(index));
        break;
      }
      path.add(s.substring(index,endBracket));
      index=endBracket+1; // move to after the end bracket
      index=s.indexOf('[',index);
      if(index<0){// start bracket not found
        break;
      }
      index++;// move to after the start bracket
    }
    return path.toArray(new String[]{});
  }

  private static boolean IsList(Map<String,Object> dict){
    if(dict==null)return false;
    int index=0;
    int count=dict.size();
    if(count==0)return false;
    while(true){
      if(index==count)
        return true;
      String indexString=Integer.toString(index);
      if(!dict.containsKey(indexString)){
        return false;
      }
      index++;
    }
  }
  private static List<Object> ConvertToList(Map<String,Object> dict){
    ArrayList<Object> ret=new ArrayList<Object>();
    int index=0;
    int count=dict.size();
    while(index<count){
      String indexString=Integer.toString(index);
      ret.add(dict.get(indexString));
      index++;
    }
    return ret;
  }

  private static void ConvertLists(List<Object> dict){
    for(int i=0;i<dict.size();i++){
      @SuppressWarnings("unchecked")
      Map<String,Object> value=((dict.get(i) instanceof Map<?,?>) ? (Map<String,Object>)dict.get(i) : null);
      // A list contains only indexes 0, 1, 2, and so on,
      // with no gaps.
      if(IsList(value)){
        List<Object> newList=ConvertToList(value);
        dict.set(i,newList);
        ConvertLists(newList);
      } else if(value!=null){
        // Convert the list's descendents
        // if they are lists
        ConvertLists(value);
      }
    }
  }

  private static void ConvertLists(Map<String,Object> dict){
    for(String key : new ArrayList<String>(dict.keySet())){
      @SuppressWarnings("unchecked")
      Map<String,Object> value=((dict.get(key) instanceof Map<?,?>) ? (Map<String,Object>)dict.get(key) : null);
      // A list contains only indexes 0, 1, 2, and so on,
      // with no gaps.
      if(IsList(value)){
        List<Object> newList=ConvertToList(value);
        dict.put(key,newList);
        ConvertLists(newList);
      } else if(value!=null){
        // Convert the dictionary's descendents
        // if they are lists
        ConvertLists(value);
      }
    }
  }

  public static Map<String,Object> QueryStringToDict(String query){
    return QueryStringToDict(query,"&");
  }

  public static Map<String,Object> QueryStringToDict(String query, String delimiter){
    Map<String,Object> root=new HashMap<String,Object>();
    for(String[] keyvalue : ParseQueryString(query,delimiter)){
      String[] path=GetKeyPath(keyvalue[0]);
      Map<String,Object> leaf=root;
      for(int i=0;i<path.length-1;i++){
        if(!leaf.containsKey(path[i])){
          // node doesn't exist so add it
          Map<String,Object> newLeaf=new HashMap<String,Object>();
          leaf.put(path[i],newLeaf);
          leaf=newLeaf;
        } else {
          @SuppressWarnings("unchecked")
          Map<String,Object> o=((leaf.get(path[i]) instanceof Map<?,?>) ? (Map<String,Object>)leaf.get(path[i]) : null);
          if(o!=null){
            leaf=o;
          } else {
            // error, not a dictionary
            leaf=null;
            break;
          }
        }
      }
      if(leaf!=null){
        leaf.put(path[path.length-1],keyvalue[1]);
      }
    }
    // Convert array-like dictionaries to lists
    ConvertLists(root);
    return root;
  }
}
