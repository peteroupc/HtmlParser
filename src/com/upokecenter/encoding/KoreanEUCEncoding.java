package com.upokecenter.encoding;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.upokecenter.util.DebugUtility;

final class KoreanEUCEncoding implements ITextEncoder, ITextDecoder {


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
			if(b<0 && lead==0){
				break;
			} else if(b<0){
				int o=error.emitDecoderError(buffer, offset, length);
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
					int temp=(26+26+126)*(thislead-0x81);
					if(b>=0x41 && b<=0x5a) {
						pointer=temp+b-0x41;
					} else if(b>=0x61 && b<=0x7a) {
						pointer=temp+26+b-0x61;
					} else if(b>=0x81 && b<=0xfe) {
						pointer=temp+26+26+b-0x81;
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
					int o=error.emitDecoderError(buffer, offset, length);
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
			if(b<0){
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
			int o=error.emitDecoderError(buffer, offset, length);
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
				error.emitEncoderError(stream);
				continue;
			}
			if(cp<=0x7f){
				stream.write(cp);
			} else {
				int pointer=Korean.codePointToIndex(cp);
				if(pointer<0){
					error.emitEncoderError(stream);
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
