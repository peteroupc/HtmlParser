package com.upokecenter.util;

import java.io.FilterInputStream;
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
public final class ConditionalBufferInputStream extends FilterInputStream {

	private byte[] buffer=new byte[1024];
	private int pos=0;
	private int endpos=0;
	private boolean disabled=false;
	private long markpos=-1;
	private int posAtMark=0;
	private long marklimit=0;


	public ConditionalBufferInputStream(InputStream input) {
		super(input);
	}

	@Override
	public synchronized void close() throws IOException{
		disabled=true;
		pos=0;
		endpos=0;
		buffer=null;
		markpos=-1;
		super.close();
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
	public int available() throws IOException{
		if(isDisabled())
			return super.available();
		return 0;
	}

	@Override
	public boolean markSupported(){
		return true;
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
	public synchronized void mark(int limit){
		//DebugUtility.log("mark %d: %s",limit,isDisabled());
		if(isDisabled()){
			super.mark(limit);
			return;
		}
		if(limit<0)
			throw new IllegalArgumentException();
		markpos=0;
		posAtMark=pos;
		marklimit=limit;
	}

	private int readInternal(byte[] buf, int offset, int unitCount) throws IOException{
		if(buf==null)throw new IllegalArgumentException();
		if(offset<0 || unitCount<0 || offset+unitCount>buf.length)
			throw new IndexOutOfBoundsException();
		if(unitCount==0)return 0;
		int total=0;
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
				int c=super.read(buf,offset,unitCount);
				if(c>0) {
					total+=c;
				}
			}
			return (total==0) ? -1 : total;
		}
		// End pos is smaller than buffer size, fill
		// entire buffer if possible
		if(endpos<buffer.length){
			int count=super.read(buffer,endpos,buffer.length-endpos);
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
		int count=super.read(buffer, endpos, Math.min(unitCount,buffer.length-endpos));
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

	private int readInternal() throws IOException {
		// Read from buffer
		if(pos<endpos)
			return (buffer[pos++]&0xFF);
		//if(buffer!=null)
		//DebugUtility.log("buffer %s end=%s len=%s",pos,endpos,buffer.length);
		if(disabled)
			// Buffering disabled, so read directly from stream
			return super.read();
		// End pos is smaller than buffer size, fill
		// entire buffer if possible
		if(endpos<buffer.length){
			int count=super.read(buffer,endpos,buffer.length-endpos);
			if(count>0) {
				endpos+=count;
			}
		}
		// Try reading from buffer again
		if(pos<endpos)
			return (buffer[pos++]&0xFF);
		// No room, read next byte and put it in buffer
		int c=super.read();
		if(pos>=buffer.length){
			byte[] newBuffer=new byte[buffer.length*2];
			System.arraycopy(buffer,0,newBuffer,0,buffer.length);
			buffer=newBuffer;
		}
		buffer[pos++]=(byte)c;
		endpos++;
		return c;
	}

	@Override
	public synchronized int read() throws IOException{
		if(markpos<0)
			return readInternal();
		else {
			if(isDisabled())
				return super.read();
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
	public synchronized long skip(long byteCount) throws IOException{
		if(isDisabled())
			return super.skip(byteCount);
		byte[] data=new byte[1024];
		long ret=0;
		while(byteCount<0){
			int bc=(int)Math.min(byteCount,data.length);
			int c=read(data,0,bc);
			if(c<=0) {
				break;
			}
			ret+=c;
			byteCount-=c;
		}
		return ret;
	}

	@Override
	public synchronized int read(byte[] buffer, int offset, int byteCount) throws IOException{
		if(markpos<0)
			return readInternal(buffer,offset,byteCount);
		else {
			if(isDisabled())
				return super.read(buffer,offset,byteCount);
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

	@Override
	public synchronized void reset() throws IOException {
		//DebugUtility.log("reset: %s",isDisabled());
		if(isDisabled()){
			super.reset();
			return;
		}
		if(markpos<0)
			throw new IOException();
		pos=posAtMark;
	}
}
