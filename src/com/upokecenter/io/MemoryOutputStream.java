/*
Written in 2013 by Peter Occil.  
Any copyright is dedicated to the Public Domain.
http://creativecommons.org/publicdomain/zero/1.0/

If you like this, you should donate to Peter O.
at: http://upokecenter.com/d/
*/
package com.upokecenter.io;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class MemoryOutputStream extends OutputStream {

  byte[] buffer=new byte[16];
  int pos=0;

  public int get(int index){
    if(index>=pos)return -1;
    return buffer[index];
  }

  public int length(){
    return pos;
  }

  public void reset(){
    pos=0;
  }

  public byte[] toByteArray(){
    byte[] bytes=new byte[pos];
    System.arraycopy(buffer,0,bytes,0,pos);
    return bytes;
  }

  public InputStream toInputStream(){
    return new ByteArrayInputStream(buffer,0,pos);
  }

  @Override
  public void write(byte[] buf, int off, int len){
    if((buf)==null)throw new NullPointerException("buf");
    if((off)<0)throw new IndexOutOfBoundsException("off not greater or equal to 0 ("+Integer.toString(off)+")");
    if((len)<0)throw new IndexOutOfBoundsException("len not greater or equal to 0 ("+Integer.toString(len)+")");
    if((off+len)>buf.length)throw new IndexOutOfBoundsException("off+len not less or equal to "+Integer.toString(buf.length)+" ("+Integer.toString(off+len)+")");
    if(pos+len>buffer.length){
      byte[] newbuffer=new byte[Math.max(pos+len+1024, buffer.length*2)];
      System.arraycopy(buffer,0,newbuffer,0,buffer.length);
      buffer=newbuffer;
    }
    System.arraycopy(buf,off,buffer,pos,len);
  }

  @Override
  public void write(int b) throws IOException {
    if(pos>=buffer.length){
      byte[] newbuffer=new byte[Math.max(pos+10,buffer.length*2)];
      System.arraycopy(buffer,0,newbuffer,0,buffer.length);
      buffer=newbuffer;
    }
    buffer[pos++]=(byte)(b&0xFF);
  }
}
