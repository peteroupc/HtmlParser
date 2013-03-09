package com.upokecenter.util;

public final class IntList {
	int[] buffer;
	int ptr;
	public IntList(){
		buffer=new int[64];
		ptr=0;
	}

	public int get(int index){
		return buffer[index];
	}

	public void set(int index, int value){
		buffer[index]=value;
	}

	public void append(int v){
		if(ptr<buffer.length){
			buffer[ptr++]=v;
		} else {
			int[] newbuffer=new int[buffer.length*2];
			System.arraycopy(buffer,0,newbuffer,0,buffer.length);
			buffer=newbuffer;
			buffer[ptr++]=v;
		}
	}
	public void append(String str) {
		for(int i=0;i<str.length();i++){
			int c=str.charAt(i);
			if(c>=0xD800 && c<=0xDBFF && i+1<str.length() &&
					str.charAt(i+1)>=0xDC00 && str.charAt(i+1)<=0xDFFF){
				// Append a UTF-16 surrogate pair
				int cp2=0x10000+(c-0xD800)*0x400+(str.charAt(i+1)-0xDC00);
				append(cp2);
				i++;
			} else if(c>=0xD800 && c<=0xDFFF)
				// illegal surrogate
				throw new IllegalArgumentException();
			else {
				append(c);
			}
		}
	}
	public int[] array(){
		return buffer;
	}
	public void clear(){
		ptr=0;
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
				int trail=ch%0x400+0xdc00;
				builder.append((char)lead);
				builder.append((char)trail);
			}
		}
		return builder.toString();
	}
}