/*
Written in 2013 by Peter Occil.
Any copyright to this work is released to the Public Domain.
In case this is not possible, this work is also
licensed under the Unlicense: https://unlicense.org/

*/
package com.upokecenter.io;

import java.io.IOException;
import java.nio.charset.MalformedInputException;

public final class StringCharacterInput implements ICharacterInput {

  String str=null;
  int pos=0;
  boolean strict=false;
  public StringCharacterInput(String str){
    this(str,false);
  }
  public StringCharacterInput(String str, boolean strict){
    if(str==null)
      throw new IllegalArgumentException();
    this.str=str;
    this.strict=strict;
  }

  @Override
  public int read() throws IOException {
    if(pos<str.length()){
      int c=str.charAt(pos);
      if(c>=0xD800 && c<=0xDBFF && pos+1<str.length() &&
          str.charAt(pos+1)>=0xDC00 && str.charAt(pos+1)<=0xDFFF){
        // Get the Unicode code point for the surrogate pair
        c=0x10000+(c-0xD800)*0x400+(str.charAt(pos+1)-0xDC00);
        pos++;
      } else if(strict && c>=0xD800 && c<=0xDFFF)
        throw new MalformedInputException(1);
      pos++;
      return c;
    }
    return -1;
  }

  @Override
  public int read(int[] buf, int offset, int unitCount) throws IOException {
    if(offset<0 || unitCount<0 || offset+unitCount>buf.length)
      throw new IndexOutOfBoundsException();
    if(unitCount==0)return 0;
    int count=0;
    while(pos<str.length() && unitCount>0){
      int c=str.charAt(pos);
      if(c>=0xD800 && c<=0xDBFF && pos+1<str.length() &&
          str.charAt(pos+1)>=0xDC00 && str.charAt(pos+1)<=0xDFFF){
        // Get the Unicode code point for the surrogate pair
        c=0x10000+(c-0xD800)*0x400+(str.charAt(pos+1)-0xDC00);
        pos++;
      } else if(strict && c>=0xD800 && c<=0xDFFF)
        throw new MalformedInputException(1);
      buf[offset]=c;
      offset++;
      unitCount--;
      count++;
      pos++;
    }
    return count==0 ? -1 : count;
  }

}
