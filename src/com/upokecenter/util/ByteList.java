package com.upokecenter.util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public final class ByteList {
	byte[] buffer;
	int ptr;
	public ByteList(){
		buffer=new byte[64];
		ptr=0;
	}

	public int get(int index){
		return buffer[index];
	}

	public void set(int index, byte value){
		buffer[index]=value;
	}

	public void append(byte v){
		if(ptr<buffer.length){
			buffer[ptr++]=v;
		} else {
			byte[] newbuffer=new byte[buffer.length*2];
			System.arraycopy(buffer,0,newbuffer,0,buffer.length);
			buffer=newbuffer;
			buffer[ptr++]=v;
		}
	}

	public byte[] array(){
		return buffer;
	}
	public void clear(){
		ptr=0;
	}
	public int size(){
		return ptr;
	}

	public InputStream toInputStream(){
		return new ByteArrayInputStream(buffer,0,ptr);
	}

	public byte[] toByteArray(){
		byte[] ret=new byte[ptr];
		System.arraycopy(buffer,0,ret,0,ptr);
		return ret;
	}
}
