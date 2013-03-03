package com.upokecenter.encoding;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

final class ShiftJISEncoding implements ITextEncoder, ITextDecoder {

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
		while(length>0){
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
				int thisoffset=(b<0x7F) ? 0x40 : 0x41;
				int leadOffset=(thislead<0xA0) ? 0x81 : 0xC1;
				if((b>=0x40 && b<=0xFC) && b!=0x7F){
					pointer=(thislead-leadOffset)*188+b-thisoffset;
				}
				int cp=-1;
				cp=JIS0208.indexToCodePoint(pointer);
				if(pointer<0){
					stream.reset();
				}
				if(cp<=0){
					int o=error.emitDecoderError(buffer, offset, length);
					offset+=o;
					count+=o;
					length-=o;
				} else {
					buffer[offset++]=cp;
					count++;
					length--;					
				}
				continue;
			}
			if(b<=0x80){
				buffer[offset++]=b;
				count++;
				length--;									
			} else if(b>=0xA1 && b<=0xDF){
				buffer[offset++]=0xFF61+b-0xA1;
				count++;
				length--;													
			} else if((b>=0x81 && b<=0x9F) || (b>=0xE0 && b<=0xFC)){
				lead=b;
				stream.mark(2);
				continue;
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
				error.emitEncoderError(stream);
				continue;
			}
			if(cp<0x80){
				stream.write(cp);
				continue;
			}
			if(cp==0xa5){
				stream.write(0x5c);
				continue;
			}
			if(cp==0x203e){
				stream.write(0x7e);
				continue;
			}
			if(cp>=0xff61 && cp<=0xff9f){
				stream.write(cp-0xff61+0xa1);
				continue;
			}
			int pointer=JIS0208.codePointToIndex(cp);
			if(pointer<=0){
				error.emitEncoderError(stream);
				continue;
			}
			int lead=pointer/188;
			lead+=(lead<0x1f) ? 0x81 : 0xc1;
			int trail=pointer%188;
			trail+=(lead<0x3f) ? 0x40 : 0x41;
			stream.write(lead);
			stream.write(trail);
		}
	}

}
