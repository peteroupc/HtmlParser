package com.upokecenter.encoding;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

final class Big5Encoding implements ITextEncoder, ITextDecoder {

	int lead=0;
	int nextChar=-1;

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
		int count=0;
		while(length>0){
			if(nextChar>=0){
				buffer[offset++]=nextChar;
				count++;
				length--;
				nextChar=-1;
				continue;
			}
			int b=stream.read();
			if(b<0 && lead==0){
				break;
			} else if(b<0){
				lead=0;
				int o=error.emitDecoderError(buffer, offset, length);
				offset+=o;
				count+=o;
				length-=o;
				continue;
			}
			if(lead!=0){
				int thislead=lead;
				int pointer=-1;
				lead=0;
				int thisoffset=(b<0x7F) ? 0x40 : 0x62;
				if((b>=0x40 && b<=0x7E) || (b>=0xA1 && b<=0xFE)){
					pointer=(thislead-0x81)*157+(b-thisoffset);
					if(pointer==1133 || pointer==1135){
						buffer[offset++]=(0xca);
						count++;
						length--;
						if(length<=0){
							nextChar=((pointer==1133) ? 0x304 : 0x30C);
							break;
						} else {
							buffer[offset++]=((pointer==1133) ? 0x304 : 0x30C);
							count++;
							length--;
							continue;
						}
					} else if(pointer==1164 || pointer==1166){
						buffer[offset++]=(0xea);
						count++;
						length--;
						if(length<=0){
							nextChar=((pointer==1164) ? 0x304 : 0x30C);
							break;
						} else {
							buffer[offset++]=((pointer==1164) ? 0x304 : 0x30C);
							count++;
							length--;
							continue;
						}
					}
				}
				int cp=Big5.indexToCodePoint(pointer);
				if(pointer<0){
					stream.reset();
				}
				if(cp<=0){
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
			if(b<0x7F){
				buffer[offset++]=(b);
				count++;
				length--;
			} else if(b>=0x81 && b<=0xFE){
				stream.mark(2);
				lead=b;
				continue;
			} else {
				int o=error.emitDecoderError(buffer, offset, length);
				offset+=o;
				count+=o;
				length-=o;
			}
		}
		return count>0 ? count : -1;
	}

	@Override
	public void encode(OutputStream stream, int[] buffer, int offset, int length, 
			IEncodingError error)
					throws IOException {
		if(stream==null || buffer==null)throw new IllegalArgumentException();
		if(offset<0 || length<0 || offset+length>buffer.length)
			throw new IndexOutOfBoundsException();
		for(int i=0;i<buffer.length;i++){
			int cp=buffer[offset+i];
			if(cp<0 || cp>=0x110000){
				error.emitEncoderError(stream);
				continue;
			}
			if(cp<=0x7F){
				stream.write(cp);
				break;
			}
			int pointer=Big5.codePointToIndex(cp);
			if(pointer<0){
				error.emitEncoderError(stream);
				continue;
			}
			int lead=pointer/157+0x81;
			if(lead<0xa1){
				// NOTE: Encoding specification says to
				// "[a]void emitting Hong Kong Supplementary 
				// Character Set extensions literally."
				error.emitEncoderError(stream);
			}
			int trail=pointer%157;
			if(trail<0x3f) {
				trail+=0x40;
			} else {
				trail+=0x62;
			}
			stream.write(lead);
			stream.write(trail);
		}
	}

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
	public void encode(OutputStream stream, int[] buffer, int offset, int length)
			throws IOException {
		encode(stream,buffer,offset,length,TextEncoding.ENCODING_ERROR_THROW);
	}
}