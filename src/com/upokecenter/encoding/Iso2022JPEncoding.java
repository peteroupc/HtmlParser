package com.upokecenter.encoding;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.UnmappableCharacterException;

final class Iso2022JPEncoding implements ITextEncoder, ITextDecoder {

	@Override
	public int decode(InputStream stream) throws IOException {
		int[] value=new int[1];
		int c=decode(stream,value,0,1);
		if(c<=0)return -1;
		return value[0];
	}
	int lead=0;
	boolean jis0212=false;
	int state=0;
	@Override
	public int decode(InputStream stream, int[] buffer, int offset, int length)
			throws IOException {
		if(stream==null || buffer==null || offset<0 || length<0 ||
				offset+length>buffer.length)
			throw new IllegalArgumentException();
		int count=0;
		while(length>0){
			int b=stream.read();
			if(state==0){ // ASCII state
				if(b==0x1b){
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
					buffer[offset++]=(0xFFFD);
					length--;
					count++;		
				}
			} else if(state==2){ // escape start state
				if(b==0x24 || b==0x28){
					lead=b;
					state=3; // escape middle state
					continue;
				} else {
					stream.reset(); // 'decrease by one'
					state=0;// ASCII state
					buffer[offset++]=(0xFFFD);
					length--;
					count++;
				}
			} else if(state==3){ // escape middle state
				if(lead==0x24 && (b==0x40 || b==0x42)){
					jis0212=false;
					state=5; // lead state
				} else if(lead==0x24 && b==0x28){
					state=4; // escape final state
					continue;
				} else if(lead==0x28 && (b==0x42 || b==0x4a)){
					state=0; // ASCII state
					continue;
				} else if(lead==0x28 && (b==0x49)){
					state=7; // Katakana state
					continue;
				} else {
					stream.reset();
					state=0;// ASCII state
					buffer[offset++]=(0xFFFD);
					length--;
					count++;
				}
			} else if(state==4){ // final state
				if(b==0x44){
					jis0212=true;
					state=5;
					continue;
				} else {
					stream.reset();
					state=0;// ASCII state
					buffer[offset++]=(0xFFFD);
					length--;
					count++;
				}
			} else if(state==5){ // lead state
				if(b==0x0A){
					state=0;// ASCII state
					buffer[offset++]=0x0a;
					length--;
					count++;
				} else if(b==0x1B){
					state=1; // escape start state
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
					buffer[offset++]=(0xFFFD);
					length--;
					count++;					
				} else {
					int cp=-1;
					int index=(lead-0x21)*94+b-0x21;
					if((lead>=0x21 && lead<=0x7e) &&
							(b>=0x21 && b<=0x7e)){
						if(jis0212){
							cp=JIS0212.indexToCodePoint(index);
						} else {
							cp=JIS0208.indexToCodePoint(index);
						}
					}
					if(cp<=0){
						buffer[offset++]=(0xFFFD);
						length--;
						count++;											
					} else {
						buffer[offset++]=(cp);
						length--;
						count++;											
					}
				}
			} else { // Katakana state
				if(b==0x1b){
					state=1; // escape start state
					continue;
				} else if((b>=0x21 && b<=0x5F)){
					buffer[offset++]=(0xFF61+b-0x21);
					length--;
					count++;
				} else if(b<0){
					break;
				} else {
					buffer[offset++]=(0xFFFD);
					length--;
					count++;					
				}
			}
			break;
		}
		return (count==0) ? -1 : count;
	}

	@Override
	public void encode(OutputStream stream, int[] array, int offset, int length)
			throws IOException {
		if(stream==null || array==null)throw new IllegalArgumentException();
		if(offset<0 || length<0 || offset+length>array.length)
			throw new IndexOutOfBoundsException();
		for(int i=0;i<array.length;i++){
			int cp=array[offset+i];
			if(cp<0 || cp>=0x110000)
				throw new UnmappableCharacterException(1);
			if((cp<=0x7F || cp==0xa5 || cp==0x203e) && state!=0){
				// ASCII state
				state=0;
				stream.write(0x1b);
				stream.write(0x28);
				stream.write(0x42);
			}
			if(cp<=0x7F){
				stream.write(cp);
				continue;
			} else if(cp==0xa5){
				stream.write(0x5c);
				continue;
			} else if(cp==0x203e){
				stream.write(0x7e);
				continue;
			}
			if(cp>=0xFF61 && cp<=0xFF9F && state!=7){
				// Katakana state
				state=7;
				stream.write(0x1b);
				stream.write(0x28);
				stream.write(0x49);				
			}
			if(cp>=0xFF61 && cp<=0xFF9F){
				stream.write(cp-0xFF61+0x21);
				continue;
			}
			int pointer=JIS0208.codePointToIndex(cp);
			if(pointer<0)
				throw new UnmappableCharacterException(1);
			if(state!=5){ // lead state
				state=5;
				stream.write(0x1b);
				stream.write(0x24);
				stream.write(0x42);
			}
			int lead=pointer/94+0x21;
			int trail=pointer%94+0x21;
			stream.write(lead);
			stream.write(trail);
		}
	}

}
