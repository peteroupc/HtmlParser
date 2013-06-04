/*
If you like this, you should donate to Peter O.
at: http://upokecenter.com/d/



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

final class Utf32Encoding implements ITextEncoder, ITextDecoder {

	private final boolean utf32be;
	private int b1=-1,b2=-1,b3=-1;

	public Utf32Encoding(boolean utf16be){
		this.utf32be=utf16be;
	}


	@Override
	public int decode(InputStream stream) throws IOException {
		return decode(stream, TextEncoding.ENCODING_ERROR_THROW);
	}

	@Override
	public int decode(InputStream stream, IEncodingError error) throws IOException {
		int[] value=new int[1];
		int c=decode(stream,value,0,1,error);
		if(c<=0)return -1;
		return value[0];
	}

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
		int cp=0;
		int count=0;
		while(length>0){
			int b=stream.read();
			if(b<0 && (b1>=0 || b2>=0 || b3>=0)){
				b1=b2=b3=-1;
				int o=error.emitDecoderError(buffer, offset, length);
				offset+=o;
				count+=o;
				length-=o;
				break;
			} else if(b<0){
				break;
			}
			if(b3>=0){
				if((utf32be && b1!=0) || (!utf32be && b!=0)){
					b1=b2=b3=-1;
					int o=error.emitDecoderError(buffer, offset, length);
					offset+=o;
					count+=o;
					length-=o;
				} else {
					cp=(utf32be) ? (b1<<24)|(b2<<16)|(b3<<8)|b : (b<<24)|(b3<<16)|(b2<<8)|b1;
					b1=b2=b3=-1;
					if((cp>=0xD800 && cp<=0xDFFF) || cp<0 || cp>=0x110000){
						// Surrogate and out-of-range code points are illegal
						int o=error.emitDecoderError(buffer, offset, length);
						offset+=o;
						count+=o;
						length-=o;
					} else {
						buffer[offset++]=(cp);
						count++;
						length--;
					}
				}
				continue;
			} else if(b2>=0){
				b3=b;
				continue;
			} else if(b1>=0){
				b2=b;
				continue;
			} else {
				b1=b;
				continue;
			}
		}
		return (count<=0) ? -1 : count;
	}

	@Override
	public void encode(OutputStream stream, int[] array, int offset, int length)
			throws IOException {
		encode(stream, array, offset, length, TextEncoding.ENCODING_ERROR_THROW);
	}


	@Override
	public void encode(OutputStream stream, int[] array, int offset, int length, IEncodingError error)
			throws IOException {
		if(stream==null || array==null)throw new IllegalArgumentException();
		if(offset<0 || length<0 || offset+length>array.length)
			throw new IndexOutOfBoundsException();
		for(int i=0;i<array.length;i++){
			int cp=array[offset+i];
			if(cp<0 || cp>=0x110000 || (cp>=0xd800 && cp<=0xdfff)){
				error.emitEncoderError(stream, cp);
				continue;
			}
			int byte1=(cp>>24)&0xFF;
			int byte2=(cp>>16)&0xFF;
			int byte3=(cp>>8)&0xFF;
			int byte4=(cp)&0xFF;
			stream.write(utf32be ? byte1 : byte4);
			stream.write(utf32be ? byte2 : byte3);
			stream.write(utf32be ? byte3 : byte2);
			stream.write(utf32be ? byte4 : byte1);
		}
	}

}
