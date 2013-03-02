package com.upokecenter.html;

import java.io.IOException;
import java.io.InputStream;
import com.upokecenter.encoding.ITextDecoder;

class Html5Decoder implements ITextDecoder {

	ITextDecoder decoder=null;
	boolean havebom=false;
	boolean havecr=false;
	boolean iserror=false;
	public Html5Decoder(ITextDecoder decoder){
		if(decoder==null)throw new IllegalArgumentException();
		this.decoder=decoder;
	}

	public boolean isError(){
		return iserror;
	}

	@Override
	public int decode(InputStream stream, int[] buffer, int offset, int length)
			throws IOException {
		if(stream==null || buffer==null || offset<0 || length<0 ||
				offset+length>buffer.length)
			throw new IllegalArgumentException();
		if(length==0)return 0;
		int count=0;
		while(length>0){
			int c=decoder.decode(stream);
			if(!havebom && !havecr && c>=0x20 && c<=0x7E){
				buffer[offset]=c;
				offset++;
				count++;
				length--;
				continue;
			}
			if(c<0) {
				break;
			}
			if(c==0x0D){
				// CR character
				havecr=true;
				c=0x0A;
			} else if(c==0x0A && havecr){
				havecr=false;
				continue;
			} else {
				havecr=false;
			}
			if(c==0xFEFF && !havebom){
				// leading BOM
				havebom=true;
				continue;
			} else if(c!=0xFEFF){
				havebom=false;
			}
			if(c<0x09 || (c>=0x0E && c<=0x1F) || (c>=0x7F && c<=0x9F) ||
					(c&0xFFFE)==0xFFFE || c>0x10FFFF || c==0x0B || (c>=0xFDD0 && c<=0xFDEF)){
				// control character or noncharacter
				iserror=true;
			}
			buffer[offset]=c;
			offset++;
			count++;
			length--;
		}
		return count==0 ? -1 : count;
	}

	@Override
	public int decode(InputStream stream) throws IOException {
		int[] value=new int[1];
		int c=decode(stream,value,0,1);
		if(c<=0)return -1;
		return value[0];
	}
}
