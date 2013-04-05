/*

Licensed under the Expat License.

Copyright (C) 2013 Peter Occil

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.

*/
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
				c=(c-0xf780+0x80)&0xFF;
				stream.write((byte)c);
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
