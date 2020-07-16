/*
Written in 2013 by Peter Occil.
Any copyright is dedicated to the Public Domain.
http://creativecommons.org/publicdomain/zero/1.0/

If you like this, you should donate to Peter O.
at: http://peteroupc.github.io/
*/
package com.upokecenter.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;

public final class StreamUtility {
  public static void copyStream(InputStream stream, OutputStream output)
      throws IOException {
    byte[] buffer=new byte[8192];
    while(true){
      int count=stream.read(buffer,0,buffer.length);
      if(count<0) {
        break;
      }
      output.write(buffer,0,count);
    }
  }

  public static String fileToString(File file)
      throws IOException {
    FileInputStream input=null;
    try {
      input=new FileInputStream(file);
      return streamToString(input);
    } finally {
      if(input!=null) {
        input.close();
      }
    }
  }

  public static void inputStreamToFile(InputStream stream, File file)
      throws IOException {
    FileOutputStream output=null;
    try {
      output=new FileOutputStream(file);
      copyStream(stream,output);
    } finally {
      if(output!=null) {
        output.close();
      }
    }
  }

  public static void skipToEnd(InputStream stream){
    if(stream==null)return;
    while(true){
      byte[] x=new byte[1024];
      try {
        int c=stream.read(x,0,x.length);
        if(c<0) {
          break;
        }
      } catch(IOException e){
        break; // maybe this stream is already closed
      }
    }
  }

  public static String streamToString(InputStream stream)
      throws IOException {
    return streamToString("UTF-8",stream);
  }

  public static String streamToString(String charset, InputStream stream)
      throws IOException {
    Reader reader = new InputStreamReader(stream, charset);
    StringBuilder builder=new StringBuilder();
    char[] buffer = new char[4096];
    while(true){
      int count=reader.read(buffer);
      if(count<0) {
        break;
      }
      builder.append(buffer,0,count);
    }
    return builder.toString();
  }

  /**
   *
   * Writes a string in UTF-8 to the specified file.
   * If the file exists, it will be overwritten
   *
   * @param s a string to write. Illegal code unit
   * sequences are replaced with
   * with U+FFFD REPLACEMENT CHARACTER when writing to the stream.
   * @param file a filename
   * @throws IOException if the file can't be created
   * or another I/O error occurs.
   */
  public static void stringToFile(String s, File file) throws IOException{
    OutputStream os=null;
    try {
      os=new FileOutputStream(file);
      stringToStream(s,os);
    } finally {
      if(os!=null) {
        os.close();
      }
    }
  }

  /**
   *
   * Writes a string in UTF-8 to the specified output stream.
   *
   * @param s a string to write. Illegal code unit
   * sequences are replaced with
   * U+FFFD REPLACEMENT CHARACTER when writing to the stream.
   * @param stream an output stream to write to.
   * @throws IOException if an I/O error occurs
   */
  public static void stringToStream(String s, OutputStream stream) throws IOException{
    byte[] bytes=new byte[4];
    for(int index=0;index<s.length();index++){
      int c=s.charAt(index);
      if(c>=0xD800 && c<=0xDBFF && index+1<s.length() &&
          s.charAt(index+1)>=0xDC00 && s.charAt(index+1)<=0xDFFF){
        // Get the Unicode code point for the surrogate pair
        c=0x10000+(c-0xD800)*0x400+(s.charAt(index+1)-0xDC00);
        index++;
      } else if(c>=0xD800 && c<=0xDFFF){
        // unpaired surrogate, write U+FFFD instead
        c=0xFFFD;
      }
      if(c<=0x7F){
        stream.write(c);
      } else if(c<=0x7FF){
        bytes[0]=((byte)(0xC0|((c>>6)&0x1F)));
        bytes[1]=((byte)(0x80|(c   &0x3F)));
        stream.write(bytes,0,2);
      } else if(c<=0xFFFF){
        bytes[0]=((byte)(0xE0|((c>>12)&0x0F)));
        bytes[1]=((byte)(0x80|((c>>6 )&0x3F)));
        bytes[2]=((byte)(0x80|(c      &0x3F)));
        stream.write(bytes,0,3);
      } else {
        bytes[0]=((byte)(0xF0|((c>>18)&0x07)));
        bytes[1]=((byte)(0x80|((c>>12)&0x3F)));
        bytes[2]=((byte)(0x80|((c>>6 )&0x3F)));
        bytes[3]=((byte)(0x80|(c      &0x3F)));
        stream.write(bytes,0,4);
      }
    }
  }

  private StreamUtility(){}

}
