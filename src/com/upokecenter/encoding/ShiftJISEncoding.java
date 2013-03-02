package com.upokecenter.encoding;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.UnmappableCharacterException;

final class ShiftJISEncoding implements ITextEncoder, ITextDecoder {

	int lead=0;


	@Override
	public int decode(InputStream stream) throws IOException {
		int[] value=new int[1];
		int c=decode(stream,value,0,1);
		if(c<=0)return -1;
		return value[0];
	}

	@Override
	public int decode(InputStream stream, int[] buffer, int offset, int length)
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
				buffer[offset++]=0xFFFD;
				count++;
				length--;
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
					buffer[offset++]=0xFFFD;
					count++;
					length--;
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
		if(stream==null || array==null)throw new IllegalArgumentException();
		if(offset<0 || length<0 || offset+length>array.length)
			throw new IndexOutOfBoundsException();
		for(int i=0;i<array.length;i++){
			int cp=array[offset+i];
			if(cp<0 || cp>=0x110000)
				throw new UnmappableCharacterException(1);

		}
	}

}
