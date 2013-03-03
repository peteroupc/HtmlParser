package com.upokecenter.encoding;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

final class XUserDefinedEncoding implements ITextEncoder, ITextDecoder {

	@Override
	public int decode(InputStream stream, int[] buffer, int offset, int length)
			throws IOException {
		return decode(stream, buffer, offset, length, TextEncoding.ENCODING_ERROR_THROW);
	}

	@Override
	public int decode(InputStream stream, int[] buffer, int offset, int length, IEncodingError error)
			throws IOException {
		if(stream==null || buffer==null || offset<0 || length<0 ||
				offset+length>buffer.length)
			throw new IllegalArgumentException();
		byte[] tmp=new byte[1024];
		int i=length;
		int total=0;
		while(i>0){
			int count=stream.read(tmp,0,Math.min(i,tmp.length));
			if(count<0) {
				break;
			}
			total+=count;
			for(int j=0;j<count;j++){
				int c=(tmp[j]&0xFF);
				if(c<0x80){
					buffer[offset++]=(c);
				} else {
					buffer[offset++]=(0xF780+(c&0xFF)-0x80);
				}
			}
			i-=count;
		}
		return (total==0) ? -1 : total;
	}

	@Override
	public void encode(OutputStream stream, int[] array, int offset, int length, IEncodingError error)
			throws IOException {
		if(stream==null || array==null)throw new IllegalArgumentException();
		if(offset<0 || length<0 || offset+length>array.length)
			throw new IndexOutOfBoundsException();
		byte[] buffer=new byte[1024];
		int i=length;
		while(i>0){
			int count=Math.min(i,buffer.length);
			for(int j=0;j<count;j++){
				int c=array[offset++];
				if(c<0 || c>=0x110000){
					error.emitEncoderError(stream);
					continue;
				} else if(c<0x80){
					buffer[j]=(byte)c;
				} else if(c>=0xF780 && c<=0xF7FF){
					buffer[j]=(byte)(c-0xF780+0x80);
				} else {
					error.emitEncoderError(stream);
					continue;
				}
			}
			i-=count;
			stream.write(buffer,0,count);
		}
	}

	@Override
	public int decode(InputStream stream) throws IOException {
		return decode(stream, TextEncoding.ENCODING_ERROR_THROW);
	}

	@Override
	public int decode(InputStream stream, IEncodingError error) throws IOException {
		if(stream==null)throw new IllegalArgumentException();
		int c=stream.read();
		if(c<0)return -1;
		if(c<0x80)
			return c;
		else
			return 0xF780+c-0x80;
	}

	@Override
	public void encode(OutputStream stream, int[] buffer, int offset, int length)
			throws IOException {
		encode(stream, buffer, offset, length, TextEncoding.ENCODING_ERROR_THROW);
	}


}
