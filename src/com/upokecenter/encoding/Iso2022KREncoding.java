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

final class Iso2022KREncoding implements ITextEncoder, ITextDecoder {


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

	int lead=0;
	int state=0;
	boolean initialization=false;

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
		while(length>0){
			int b=stream.read();
			if(state==0){ // ASCII state
				if(b==0x0e){
					state=5; // lead state
				} else if(b==0x0f){
					continue;
				} else if(b==0x1b){
					stream.mark(4);
					state=2; // escape start state
					continue;
				} else if(b<0){
					break;
				} else if(b<=0x7F){
					buffer[offset++]=(b);
					length--;
					count++;
				} else {
					int o=error.emitDecoderError(buffer, offset, length);
					offset+=o;
					count+=o;
					length-=o;

				}
			} else if(state==2){ // escape start state
				if(b==0x24){
					state=3; // escape middle state
					continue;
				} else {
					stream.reset(); // 'decrease by one'
					state=0;// ASCII state
					int o=error.emitDecoderError(buffer, offset, length);
					offset+=o;
					count+=o;
					length-=o;

				}
			} else if(state==3){ // escape middle state
				if(b==0x29){
					state=4;
					continue;
				} else {
					stream.reset();
					state=0;// ASCII state
					int o=error.emitDecoderError(buffer, offset, length);
					offset+=o;
					count+=o;
					length-=o;

				}
			} else if(state==4){ // final state
				if(b==0x43){
					state=0;
					continue;
				} else {
					stream.reset();
					state=0;// ASCII state
					int o=error.emitDecoderError(buffer, offset, length);
					offset+=o;
					count+=o;
					length-=o;

				}
			} else if(state==5){ // lead state
				if(b==0x0A){
					state=0;// ASCII state
					buffer[offset++]=0x0a;
					length--;
					count++;
				} else if(b==0x0e){
					continue;
				} else if(b==0x0f){
					state=0;
					continue;
				} else if(b<0){
					break;
				} else {
					lead=b;
					state=6;
					continue;
				}
			} else if(state==6){ // trail state
				state=5; // lead state
				if(b<0){
					int o=error.emitDecoderError(buffer, offset, length);
					offset+=o;
					count+=o;
					length-=o;

				} else {
					int cp=-1;
					if((lead>=0x21 && lead<=0x46) &&
							(b>=0x21 && b<=0x7e)){
						cp=Korean.indexToCodePoint((26+26+126)*(lead-1)+26+26+b-1);
					} else if((lead>=0x47 && lead<=0x7E) &&
							(b>=0x21 && b<=0x7e)){
						cp=Korean.indexToCodePoint((26+26+126)*(0xc7-0x81)+(lead-0x47)*94+(b-0x21));
					}
					if(cp<=0){
						int o=error.emitDecoderError(buffer, offset, length);
						offset+=o;
						count+=o;
						length-=o;

					} else {
						buffer[offset++]=(cp);
						length--;
						count++;
					}
				}
			}
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
			if(!initialization){
				initialization=true;
				stream.write(0x1b);
				stream.write(0x24);
				stream.write(0x29);
				stream.write(0x43);
			}
			if(state!=0 && cp<=0x7F){
				state=0;
				stream.write(0x0F);
			}
			if(cp<=0x7F){
				stream.write(cp);
				continue;
			}
			int pointer=Korean.codePointToIndex(cp);
			if(pointer<0){
				error.emitEncoderError(stream, cp);
				continue;
			}
			if(state!=5){
				state=5;
				stream.write(0x0e);
			}
			if(pointer<(26+26+126)*(0xc7-0x81)){
				int lead=pointer/(26+26+126)+1;
				int trail=pointer%(26+26+126)-26-26+1;
				if(lead<0x21 || trail<0x21){
					error.emitEncoderError(stream, cp);
					continue;
				}
				stream.write(lead);
				stream.write(trail);
			} else {
				pointer-=(26+26+126)*(0xc7-0x81);
				int lead=pointer/94+0x47;
				int trail=pointer%94+0x21;
				stream.write(lead);
				stream.write(trail);
			}
		}
		if(state!=0 && length>0){
			state=0;
			stream.write(0x0F);
		}
	}

}
