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

final class Utf16Encoding implements ITextEncoder, ITextDecoder {

	private final boolean utf16be;
	private int leadByte=-1;
	private int leadSurrogate=-1;

	public Utf16Encoding(boolean utf16be){
		this.utf16be=utf16be;
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
			if(b<0 && (leadByte>=0 || leadSurrogate>=0)){
				leadByte=-1;
				leadSurrogate=-1;
				int o=error.emitDecoderError(buffer, offset, length);
				offset+=o;
				count+=o;
				length-=o;
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
					int o=error.emitDecoderError(buffer, offset, length);
					offset+=o;
					count+=o;
					length-=o;
				}
			} else if(cp>=0xD800 && cp<=0xDBFF){
				leadSurrogate=cp;
				stream.mark(4);
				continue;
			} else if(cp>=0xDC00 && cp<=0xDFFF){
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
