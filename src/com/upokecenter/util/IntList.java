/*
Written in 2013 by Peter Occil.  
Any copyright is dedicated to the Public Domain.
http://creativecommons.org/publicdomain/zero/1.0/

If you like this, you should donate to Peter O.
at: http://upokecenter.com/d/
*/
package com.upokecenter.util;

/**
 * 
 * Represents a list of integers or Unicode characters.
 * 
 * @author Peter
 *
 */
public final class IntList {
  int[] buffer;
  int ptr;
  public IntList(){
    buffer=new int[64];
    ptr=0;
  }

  public void appendInt(int v){
    if(ptr<buffer.length){
      buffer[ptr++]=v;
    } else {
      int[] newbuffer=new int[buffer.length*2];
      System.arraycopy(buffer,0,newbuffer,0,buffer.length);
      buffer=newbuffer;
      buffer[ptr++]=v;
    }
  }

  public void appendInts(int[] array, int offset, int length){
    if((array)==null)throw new NullPointerException("array");
    if((offset)<0)throw new IndexOutOfBoundsException("offset"+" not greater or equal to "+"0"+" ("+Integer.toString(offset)+")");
    if((length)<0)throw new IndexOutOfBoundsException("length"+" not greater or equal to "+"0"+" ("+Integer.toString(length)+")");
    if((offset+length)>array.length)throw new IndexOutOfBoundsException("offset+length"+" not less or equal to "+Integer.toString(array.length)+" ("+Integer.toString(offset+length)+")");
    if(ptr+length>buffer.length){
      int[] newbuffer=new int[Math.max(buffer.length*2, buffer.length+length)];
      System.arraycopy(buffer,0,newbuffer,0,buffer.length);
      buffer=newbuffer;
    }
    System.arraycopy(array, offset, buffer, ptr, length);
    ptr+=length;
  }

  public void appendString(String str) {
    for(int i=0;i<str.length();i++){
      int c=str.charAt(i);
      if(c>=0xD800 && c<=0xDBFF && i+1<str.length() &&
          str.charAt(i+1)>=0xDC00 && str.charAt(i+1)<=0xDFFF){
        // Append a UTF-16 surrogate pair
        int cp2=0x10000+(c-0xD800)*0x400+(str.charAt(i+1)-0xDC00);
        appendInt(cp2);
        i++;
      } else if(c>=0xD800 && c<=0xDFFF)
        // illegal surrogate
        throw new IllegalArgumentException();
      else {
        appendInt(c);
      }
    }
  }

  public int[] array(){
    return buffer;
  }
  public void clearAll(){
    ptr=0;
  }
  public int get(int index){
    return buffer[index];
  }
  /**
   * Sets the integer at a specified position to a new value.
   * @param index an index into the list.
   * @param value the integer's new value.
   */
  public void set(int index, int value){
    buffer[index]=value;
  }
  public int size(){
    return ptr;
  }
  @Override
  public String toString(){
    StringBuilder builder=new StringBuilder();
    for(int i=0;i<ptr;i++){
      if(buffer[i]<=0xFFFF){
        builder.append((char)buffer[i]);
      } else {
        int ch=buffer[i]-0x10000;
        int lead=ch/0x400+0xd800;
        int trail=(ch&0x3FF)+0xdc00;
        builder.append((char)lead);
        builder.append((char)trail);
      }
    }
    return builder.toString();
  }
}