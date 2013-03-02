package com.upokecenter.encoding;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.UnmappableCharacterException;

final class Utf16Encoding implements ITextEncoder, ITextDecoder {

	private final boolean utf16be;
	private int leadByte=-1;
	private int leadSurrogate=-1;

	public Utf16Encoding(boolean utf16be){
		this.utf16be=utf16be;
	}


	@Override
	public int decode(InputStream stream) throws IOException {
		int[] value=new int[1];
		int c=decode(stream,value,0,1);
		if(c<=0)return -1;
		return value[0];
	}

	@Override
	public int decode(InputStream stream, int[] buffer, int offset, int length)
			throws IOException {
		if(stream==null || buffer==null || offset<0 || length<0 ||
				offset+length>buffer.length)
			throw new IllegalArgumentException();
		int cp=0;
		int count=0;
		while(length>0){
			int b=stream.read();
			if(b<0 && (leadByte>=0 || leadSurrogate>=0)){
				leadByte=-1;
				leadSurrogate=-1;
				buffer[offset++]=(0xFFFD);
				count++;
				length--;
				break;
			} else if(b<0){
				break;
			}
			if(leadByte==-1){
				leadByte=b;
				continue;
			}
			cp=(utf16be) ? (leadByte<<8)|b : (b<<8)|leadByte;
			leadByte=-1;
			if(leadSurrogate>=0){
				int lead=leadSurrogate;
				leadSurrogate=-1;
				if(cp>=0xDC00 && cp<=0xDFFF){
					int cp2=0x10000+(lead-0xD800)*0x400+(cp-0xDC00);
					buffer[offset++]=(cp2);
					count++;
					length--;					
				} else {
					stream.reset();
					buffer[offset++]=(0xFFFD);
					count++;
					length--;					
				}
			} else if(cp>=0xD800 && cp<=0xDBFF){
				leadSurrogate=cp;
				stream.mark(4);
				continue;
			} else if(cp>=0xDC00 && cp<=0xDFFF){
				buffer[offset++]=(0xFFFD);
				count++;
				length--;									
			} else {
				buffer[offset++]=(cp);
				count++;
				length--;								
			}
		}
		return (count<=0) ? -1 : count;
	}

	@Override
	public void encode(OutputStream stream, int[] array, int offset, int length)
			throws IOException {
		if(stream==null || array==null)throw new IllegalArgumentException();
		if(offset<0 || length<0 || offset+length>array.length)
			throw new IndexOutOfBoundsException();
		for(int i=0;i<array.length;i++){
			int cp=array[offset+i];
			if(cp<0 || cp>=0x110000 || (cp>=0xd800 && cp<=0xdfff))
				throw new UnmappableCharacterException(1);
			if(cp<=0xFFFF){
				int b1=(cp>>8)&0xFF;
				int b2=(cp)&0xFF;
				stream.write(utf16be ? b1 : b2);
				stream.write(utf16be ? b2 : b1);
			} else {
				cp-=0x10000;
				int lead=cp/0x400+0xd800;
				int trail=cp%0x400+0xdc00;
				int b1=(lead>>8)&0xFF;
				int b2=(lead)&0xFF;
				stream.write(utf16be ? b1 : b2);
				stream.write(utf16be ? b2 : b1);
				b1=(trail>>8)&0xFF;
				b2=(trail)&0xFF;
				stream.write(utf16be ? b1 : b2);
				stream.write(utf16be ? b2 : b1);
			}
		}
	}

}
