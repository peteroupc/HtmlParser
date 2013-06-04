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

final class KoreanEUCEncoding implements ITextEncoder, ITextDecoder {


	int lead=0;

	@Override
	public int decode(InputStream stream) throws IOException {
		return decode(stream, TextEncoding.ENCODING_ERROR_THROW);
	}

	@Override
	public int decode(InputStream stream, IEncodingError error) throws IOException {
		int[] value=new int[1];
		int c=decode(stream,value,0,1, error);
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
		if(length==0)return 0;
		int count=0;
		int o=0;
		while(length>0){
			int b=stream.read();
			if(b<0 && lead==0){
				break;
			} else if(b<0){
				lead=0;
				o=error.emitDecoderError(buffer, offset, length);
				offset+=o;
				count+=o;
				length-=o;
				break;
			}
			if(lead!=0){
				int thislead=lead;
				lead=0;
				int pointer=-1;
				if(thislead>=0x81 && thislead<=0xc6){
					pointer=(26+26+126)*(thislead-0x81);
					if(b>=0x41 && b<=0x5a) {
						pointer+=b-0x41;
					} else if(b>=0x61 && b<=0x7a) {
						pointer+=26+b-0x61;
					} else if(b>=0x81 && b<=0xfe) {
						pointer+=26+26+b-0x81;
					}
				}
				if(thislead>=0xc7 && thislead<=0xfe &&
						b>=0xa1 && b<=0xfe){
					pointer=(26+26+126)*(0xc7-0x81)+(thislead-0xC7)*94+(b-0xA1);
				}
				int cp=Korean.indexToCodePoint(pointer);
				if(pointer<0){
					stream.reset();
				}
				if(cp<=0){
					o=error.emitDecoderError(buffer, offset, length);
					offset+=o;
					count+=o;
					length-=o;
					continue;
				} else {
					buffer[offset++]=cp;
					count++;
					length--;
					continue;
				}
			}
			if(b<0x80){
				buffer[offset++]=b;
				count++;
				length--;
				continue;
			}
			if(b>=0x81 && b<=0xFE){
				lead=b;
				stream.mark(2);
				continue;
			}
			o=error.emitDecoderError(buffer, offset, length);
			offset+=o;
			count+=o;
			length-=o;
		}
		return (count==0) ? -1 : count;
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
			if(cp<0 || cp>=0x110000){
				error.emitEncoderError(stream, cp);
				continue;
			}
			if(cp<=0x7f){
				stream.write(cp);
			} else {
				int pointer=Korean.codePointToIndex(cp);
				if(pointer<0){
					error.emitEncoderError(stream, cp);
					continue;
				}
				if(pointer<(26+26+126)*(0xc7-0x81)){
					int lead=pointer/(26+26+126)+0x81;
					int trail=pointer%(26+26+126);
					int o=0x4d;
					if(trail<26) {
						o=0x41;
					} else if(trail<26+26) {
						o=0x47;
					}
					trail+=o;
					stream.write(lead);
					stream.write(trail);
				} else {
					pointer-=(26+26+126)*(0xc7-0x81);
					int lead=pointer/94+0xc7;
					int trail=pointer%94+0xa1;
					stream.write(lead);
					stream.write(trail);
				}
			}
		}
	}

}
