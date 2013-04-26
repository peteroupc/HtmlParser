/*
Written in 2013 by Peter Occil.  Released to the public domain.
Public domain dedication: http://creativecommons.org/publicdomain/zero/1.0/
 */
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

	public int get(int index){
		return buffer[index];
	}
	public void set(int index, byte value){
		buffer[index]=value;
	}
	public int size(){
		return ptr;
	}

	public byte[] toByteArray(){
		byte[] ret=new byte[ptr];
		System.arraycopy(buffer,0,ret,0,ptr);
		return ret;
	}

	public InputStream toInputStream(){
		return new ByteArrayInputStream(buffer,0,ptr);
	}
}
