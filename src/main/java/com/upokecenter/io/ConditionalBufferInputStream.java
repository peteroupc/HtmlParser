/*
Written in 2013 by Peter Occil.
Any copyright to this work is released to the Public Domain.
In case this is not possible, this work is also
licensed under the Unlicense: https://unlicense.org/

*/
package com.upokecenter.io;

import java.io.IOException;
import java.io.InputStream;

/**
 *
 * An input stream that stores the first bytes of the stream
 * in a buffer and supports rewinding to the beginning of the stream.
 * However, when the buffer is disabled, no further bytes
 * are put into the buffer, but any remaining bytes in the buffer
 * will still be used until it's exhausted.
 *
 * @author Peter O.
 *
 */
public final class ConditionalBufferInputStream extends InputStream {

  private byte[] buffer=null;
  private int pos=0;
  private int endpos=0;
  private boolean disabled=false;
  private long markpos=-1;
  private int posAtMark=0;
  private long marklimit=0;
  private InputStream stream=null;

  public ConditionalBufferInputStream(InputStream input) {
    this.stream=input;
    this.buffer=new byte[1024];
  }

  @Override
  public int available() throws IOException{
    if(isDisabled())
      return stream.available();
    return 0;
  }

  @Override
  public synchronized void close() throws IOException{
    disabled=true;
    pos=0;
    endpos=0;
    buffer=null;
    markpos=-1;
    stream.close();
  }

  /**
   * Disables buffering of future bytes read from
   * the underlying stream.  However, any bytes already
   * buffered can still be read until the buffer is exhausted.
   * After the buffer is exhausted, this stream will fully
   * delegate to the underlying stream.
   *
   */
  public synchronized void disableBuffer(){
    disabled=true;
    if(buffer!=null && isDisabled()){
      buffer=null;
    }
  }

  public synchronized int doRead(byte[] buffer, int offset, int byteCount) throws IOException{
    if(markpos<0)
      return readInternal(buffer,offset,byteCount);
    else {
      if(isDisabled())
        return stream.read(buffer,offset,byteCount);
      int c=readInternal(buffer,offset,byteCount);
      if(c>0 && markpos>=0){
        markpos+=c;
        if(markpos>marklimit){
          marklimit=0;
          markpos=-1;
          if(this.buffer!=null && isDisabled()){
            this.buffer=null;
          }
        }
      }
      return c;
    }
  }

  private boolean isDisabled(){
    if(disabled){
      if(markpos>=0 && markpos<marklimit)
        return false;
      if(pos<endpos)
        return false;
      return true;
    }
    return false;
  }

  @Override
  public synchronized void mark(int limit){
    //DebugUtility.log("mark %d: %s",limit,isDisabled());
    if(isDisabled()){
      stream.mark(limit);
      return;
    }
    if(limit<0)
      throw new IllegalArgumentException();
    markpos=0;
    posAtMark=pos;
    marklimit=limit;
  }

  @Override
  public boolean markSupported(){
    return true;
  }

  @Override
  public synchronized int read() throws IOException{
    if(markpos<0)
      return readInternal();
    else {
      if(isDisabled())
        return stream.read();
      int c=readInternal();
      if(c>=0 && markpos>=0){
        markpos++;
        if(markpos>marklimit){
          marklimit=0;
          markpos=-1;
          if(buffer!=null && isDisabled()){
            buffer=null;
          }
        }
      }
      return c;
    }
  }

  @Override
  public synchronized int read(byte[] buffer, int offset, int byteCount) throws IOException{
    return doRead(buffer,offset,byteCount);
  }

  private int readInternal() throws IOException {
    // Read from buffer
    if(pos<endpos)
      return (buffer[pos++]&0xFF);
    //if(buffer!=null)
    //DebugUtility.log("buffer %s end=%s len=%s",pos,endpos,buffer.length);
    if(disabled)
      // Buffering disabled, so read directly from stream
      return stream.read();
    // End pos is smaller than buffer size, fill
    // entire buffer if possible
    if(endpos<buffer.length){
      int count=stream.read(buffer,endpos,buffer.length-endpos);
      if(count>0) {
        endpos+=count;
      }
    }
    // Try reading from buffer again
    if(pos<endpos)
      return (buffer[pos++]&0xFF);
    // No room, read next byte and put it in buffer
    int c=stream.read();
    if(c<0)return c;
    if(pos>=buffer.length){
      byte[] newBuffer=new byte[buffer.length*2];
      System.arraycopy(buffer,0,newBuffer,0,buffer.length);
      buffer=newBuffer;
    }
    buffer[pos++]=(byte)c;
    endpos++;
    return c;
  }

  private int readInternal(byte[] buf, int offset, int unitCount) throws IOException{
    if(buf==null)throw new IllegalArgumentException();
    if(offset<0 || unitCount<0 || offset+unitCount>buf.length)
      throw new IndexOutOfBoundsException();
    if(unitCount==0)return 0;
    int total=0;
    int count=0;
    // Read from buffer
    if(pos+unitCount<=endpos){
      System.arraycopy(buffer,pos,buf,offset,unitCount);
      pos+=unitCount;
      return unitCount;
    }
    //if(buffer!=null)
    //DebugUtility.log("buffer(3arg) %s end=%s len=%s",pos,endpos,buffer.length);
    if(disabled){
      // Buffering disabled, read as much as possible from the buffer
      if(pos<endpos){
        int c=Math.min(unitCount,endpos-pos);
        System.arraycopy(buffer,pos,buf,offset,c);
        pos=endpos;
        offset+=c;
        unitCount-=c;
        total+=c;
      }
      // Read directly from the stream for the rest
      if(unitCount>0){
        int c=stream.read(buf,offset,unitCount);
        if(c>0) {
          total+=c;
        }
      }
      return (total==0) ? -1 : total;
    }
    // End pos is smaller than buffer size, fill
    // entire buffer if possible
    if(endpos<buffer.length){
      count=stream.read(buffer,endpos,buffer.length-endpos);
      //DebugUtility.log("%s",this);
      if(count>0) {
        endpos+=count;
      }
    }
    // Try reading from buffer again
    if(pos+unitCount<=endpos){
      System.arraycopy(buffer,pos,buf,offset,unitCount);
      pos+=unitCount;
      return unitCount;
    }
    // expand the buffer
    if(pos+unitCount>buffer.length){
      byte[] newBuffer=new byte[(buffer.length*2)+unitCount];
      System.arraycopy(buffer,0,newBuffer,0,buffer.length);
      buffer=newBuffer;
    }
    count=stream.read(buffer, endpos, Math.min(unitCount,buffer.length-endpos));
    if(count>0) {
      endpos+=count;
    }
    // Try reading from buffer a third time
    if(pos+unitCount<=endpos){
      System.arraycopy(buffer,pos,buf,offset,unitCount);
      pos+=unitCount;
      total+=unitCount;
    } else if(endpos>pos){
      System.arraycopy(buffer,pos,buf,offset,endpos-pos);
      total+=(endpos-pos);
      pos=endpos;
    }
    return (total==0) ? -1 : total;
  }

  @Override
  public synchronized void reset() throws IOException {
    //DebugUtility.log("reset: %s",isDisabled());
    if(isDisabled()){
      stream.reset();
      return;
    }
    if(markpos<0)
      throw new IOException();
    pos=posAtMark;
  }
  /**
   *
   * Resets the stream to the beginning of the input.  This will
   * invalidate the mark placed on the stream, if any.
   *
   * @throws IOException if disableBuffer() was already called.
   */
  public synchronized void rewind() throws IOException {
    if(disabled)
      throw new IOException();
    pos=0;
    markpos=-1;
  }

  @Override
  public synchronized long skip(long byteCount) throws IOException{
    if(isDisabled())
      return stream.skip(byteCount);
    byte[] data=new byte[1024];
    long ret=0;
    while(byteCount<0){
      int bc=(int)Math.min(byteCount,data.length);
      int c=doRead(data,0,bc);
      if(c<=0) {
        break;
      }
      ret+=c;
      byteCount-=c;
    }
    return ret;
  }
}
