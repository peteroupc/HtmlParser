package com.upokecenter.util;

import java.io.IOException;


public final class StringCharacterInput implements ICharacterInput {

	String str=null;
	int pos=0;
	public StringCharacterInput(String str){
		if(str==null)
			throw new IllegalArgumentException();
		this.str=str;
	}

	@Override
	public int read(int[] buf, int offset, int unitCount) throws IOException {
		if(offset<0 || unitCount<0 || offset+unitCount>buf.length)
			throw new IndexOutOfBoundsException();
		if(unitCount==0)return 0;
		int count=0;
		while(pos<str.length() && unitCount>0){
			int c=str.charAt(pos);
			if(c>=0xD800 && c<=0xDBFF && pos+1<str.length() &&
					str.charAt(pos+1)>=0xDC00 && str.charAt(pos+1)<=0xDFFF){
				// Get the Unicode code point for the surrogate pair
				c=0x10000+(c-0xD800)*0x400+(str.charAt(pos+1)-0xDC00);
				pos++;
			}
			buf[offset]=c;
			offset++;
			unitCount--;
			count++;
			pos++;
		}
		return count==0 ? -1 : count;
	}

	@Override
	public int read() throws IOException {
		if(pos<str.length()){
			int c=str.charAt(pos);
			if(c>=0xD800 && c<=0xDBFF && pos+1<str.length() &&
					str.charAt(pos+1)>=0xDC00 && str.charAt(pos+1)<=0xDFFF){
				// Get the Unicode code point for the surrogate pair
				c=0x10000+(c-0xD800)*0x400+(str.charAt(pos+1)-0xDC00);
				pos++;
			}
			pos++;
			return c;
		}
		return -1;
	}

}
