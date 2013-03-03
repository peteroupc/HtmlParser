package com.upokecenter.html;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import com.upokecenter.encoding.ITextDecoder;
import com.upokecenter.encoding.TextEncoding;

/**
 * 
 * Unicode character stream with more advanced
 * marking capabilities
 * 
 */

final class Html5InputStream {

	@Override
	public String toString() {
		return "Html5InputStream [pos=" + pos + ", endpos=" + endpos
				+ ", haveMark=" + haveMark + ", buffer="
				+ Arrays.toString(buffer) + "]";
	}

	int pos=0;
	int endpos=0;
	boolean haveMark=false;
	int[] buffer=null;
	InputStream input=null;
	ITextDecoder decoder=null;

	protected Html5InputStream(InputStream input, ITextDecoder decoder) {
		this.input=input;
		this.decoder=decoder;
	}

	public int getMarkPosition(){
		return pos;
	}

	public void setMarkPosition(int pos) throws IOException{
		if(!haveMark || pos<0 || pos>endpos)
			throw new IOException();
		this.pos=pos;
	}

	public int markIfNeeded(){
		if(!haveMark){
			markToEnd();
		}
		return getMarkPosition();
	}

	public void markToEnd(){
		if(buffer==null){
			buffer=new int[16];
			pos=0;
			endpos=0;
			haveMark=true;
		} else if(haveMark){
			// Already have a mark; shift buffer to the new mark
			if(pos>0 && pos<endpos){
				System.arraycopy(buffer,pos,buffer,0,endpos-pos);
			}
			endpos-=pos;
			pos=0;
		} else {
			pos=0;
			endpos=0;
			haveMark=true;
		}
	}

	public int read(int[] buf, int offset, int unitCount) throws IOException {
		if(haveMark){
			if(buf==null)throw new IllegalArgumentException();
			if(offset<0 || unitCount<0 || offset+unitCount>buf.length)
				throw new IndexOutOfBoundsException();
			if(unitCount==0)return 0;
			// Read from buffer
			if(pos+unitCount<=endpos){
				System.arraycopy(buffer,pos,buf,offset,unitCount);
				pos+=unitCount;
				return unitCount;
			}
			// End pos is smaller than buffer size, fill
			// entire buffer if possible
			if(endpos<buffer.length){
				int count=decoder.decode(input,buffer,endpos,buffer.length-endpos, TextEncoding.ENCODING_ERROR_REPLACE);
				//DebugUtility.log("%s",this);
				if(count>0) {
					endpos+=count;
				}
			}
			int total=0;
			// Try reading from buffer again
			if(pos+unitCount<=endpos){
				System.arraycopy(buffer,pos,buf,offset,unitCount);
				pos+=unitCount;
				return unitCount;
			}
			// expand the buffer
			if(pos+unitCount>buffer.length){
				int[] newBuffer=new int[(buffer.length*2)+unitCount];
				System.arraycopy(buffer,0,newBuffer,0,buffer.length);
				buffer=newBuffer;
			}
			int count=decoder.decode(input,buffer, endpos, Math.min(unitCount,buffer.length-endpos), TextEncoding.ENCODING_ERROR_REPLACE);
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
		} else
			return decoder.decode(input, buf, offset, unitCount, TextEncoding.ENCODING_ERROR_REPLACE);			
	}

	public int read() throws IOException{
		if(haveMark){
			// Read from buffer
			if(pos<endpos)
				return buffer[pos++];
			//DebugUtility.log(this);
			// End pos is smaller than buffer size, fill
			// entire buffer if possible
			if(endpos<buffer.length){
				int count=decoder.decode(input,buffer,endpos,buffer.length-endpos, TextEncoding.ENCODING_ERROR_REPLACE);
				if(count>0) {
					endpos+=count;
				}
			}
			//DebugUtility.log(this);
			// Try reading from buffer again
			if(pos<endpos)
				return buffer[pos++];
			//DebugUtility.log(this);
			// No room, read next character and put it in buffer
			int c=decoder.decode(input, TextEncoding.ENCODING_ERROR_REPLACE);
			if(pos>=buffer.length){
				int[] newBuffer=new int[buffer.length*2];
				System.arraycopy(buffer,0,newBuffer,0,buffer.length);
				buffer=newBuffer;
			}
			//DebugUtility.log(this);
			buffer[pos++]=(byte)c;
			endpos++;
			return c;
		} else
			return decoder.decode(input, TextEncoding.ENCODING_ERROR_REPLACE);
	}

	public void moveBack(int count) throws IOException {
		if(haveMark && pos>=count){
			pos-=count;
			return;
		}
		throw new IOException();
	}

}