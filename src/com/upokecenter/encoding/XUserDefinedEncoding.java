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
		if((stream)==null)throw new NullPointerException("stream");
		if((error)==null)throw new NullPointerException("error");
		if((buffer)==null)throw new NullPointerException("buffer");
if((offset)<0)throw new IndexOutOfBoundsException("offset"+" not greater or equal to "+"0"+" ("+Integer.toString(offset)+")");
if((length)<0)throw new IndexOutOfBoundsException("length"+" not greater or equal to "+"0"+" ("+Integer.toString(length)+")");
if((offset+length)>buffer.length)throw new IndexOutOfBoundsException("offset+length"+" not less or equal to "+Integer.toString(buffer.length)+" ("+Integer.toString(offset+length)+")");
		if(length==0)return 0;
		int total=0;
		for(int i=0;i<length;i++){
			int c=stream.read();
			if(c<0){
				break;
			} else if(c<0x80){
				buffer[offset++]=c;
				total++;
			} else {
				buffer[offset++]=(0xF780+(c&0xFF)-0x80);
				total++;
			}
		}
		return (total==0) ? -1 : total;
	}

	@Override
	public void encode(OutputStream stream, int[] array, int offset, int length, IEncodingError error)
			throws IOException {
		if((stream)==null)throw new NullPointerException("stream");
		if((error)==null)throw new NullPointerException("error");
		if((array)==null)throw new NullPointerException("array");
if((offset)<0)throw new IndexOutOfBoundsException("offset"+" not greater or equal to "+"0"+" ("+Integer.toString(offset)+")");
if((length)<0)throw new IndexOutOfBoundsException("length"+" not greater or equal to "+"0"+" ("+Integer.toString(length)+")");
if((offset+length)>array.length)throw new IndexOutOfBoundsException("offset+length"+" not less or equal to "+Integer.toString(array.length)+" ("+Integer.toString(offset+length)+")");
		for(int i=0;i<length;i++){
			int c=array[offset++];
			if(c<0 || c>=0x110000){
				error.emitEncoderError(stream, c);
			} else if(c<0x80){
				stream.write((byte)c);
			} else if(c>=0xF780 && c<=0xF7FF){
				stream.write((byte)(c-0xF780+0x80));
			} else {
				error.emitEncoderError(stream, c);
			}
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
