package com.upokecenter.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class MemoryOutputStream extends OutputStream {

	byte[] buffer=new byte[16];
	int pos=0;

	@Override
	public void write(int b) throws IOException {
		if(pos>=buffer.length){
			byte[] newbuffer=new byte[buffer.length*2];
			System.arraycopy(buffer,0,newbuffer,0,buffer.length);
		}
		buffer[pos++]=(byte)(b&0xFF);
	}

	@Override
	public void write(byte[] buf, int off, int len){
		if(off<0 || len<0 || off+len>buf.length)
			throw new IndexOutOfBoundsException();
		if(pos+len>buffer.length){
			byte[] newbuffer=new byte[Math.max(pos+len+1024, buffer.length*2)];
			System.arraycopy(buffer,0,newbuffer,0,buffer.length);			
		}
		System.arraycopy(buf,off,buffer,pos,len);
	}
	
	public InputStream toInputStream(){
		return new ByteArrayInputStream(buffer,0,pos);
	}

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
}
