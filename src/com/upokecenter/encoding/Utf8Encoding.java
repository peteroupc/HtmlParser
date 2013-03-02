package com.upokecenter.encoding;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.UnmappableCharacterException;

final class Utf8Encoding implements ITextEncoder, ITextDecoder {

	int cp=0;
	int bytesSeen=0;
	int bytesNeeded=0;
	int lower=0x80;
	int upper=0xBF;


	@Override
	public int decode(InputStream stream) throws IOException {
		if(stream==null)
			throw new IllegalArgumentException();
		while(true){
			int b=stream.read();
			if(b<0 && bytesNeeded!=0){
				bytesNeeded=0;
				return 0xFFFD;
			} else if(b<0)
				return -1;
			if(bytesNeeded==0){
				if(b<0x80)
					return b;
				else if(b>=0xc2 && b<=0xdf){
					stream.mark(4);
					bytesNeeded=1;
					cp=b-0xc0;
				} else if(b>=0xe0 && b<=0xef){
					stream.mark(4);
					lower=(b==0xe0) ? 0xa0 : 0x80;
					upper=(b==0xed) ? 0x9f : 0xbf;
					bytesNeeded=2;
					cp=b-0xe0;
				} else if(b>=0xf0 && b<=0xf4){
					stream.mark(4);
					lower=(b==0xf0) ? 0x90 : 0x80;
					upper=(b==0xf4) ? 0x8f : 0xbf;
					bytesNeeded=2;
					cp=b-0xe0;
				} else
					return 0xFFFD;
				cp<<=(6*bytesNeeded);
				continue;
			}
			if(b<lower || b>upper){
				cp=bytesNeeded=bytesSeen=0;
				lower=0x80;
				upper=0xbf;
				stream.reset(); // 'Decrease the byte pointer by one.'
				return 0xFFFD;
			}
			lower=0x80;
			upper=0xbf;
			bytesSeen++;
			cp+=(b-0x80)<<(6*(bytesNeeded-bytesSeen));
			stream.mark(4);
			if(bytesSeen!=bytesNeeded) {
				continue;
			}
			int ret=cp;
			cp=0;
			bytesSeen=0;
			bytesNeeded=0;
			return ret;
		}
	}

	@Override
	public int decode(InputStream stream, int[] buffer, int offset, int length)
			throws IOException {
		if(stream==null || buffer==null || offset<0 || length<0 ||
				offset+length>buffer.length)
			throw new IllegalArgumentException();
		int count=0;
		while(length>0){
			int b=stream.read();
			if(b<0 && bytesNeeded!=0){
				bytesNeeded=0;
				buffer[offset++]=(0xFFFD);
				count++;
				length--;
			} else if(b<0){
				break;
			}
			if(bytesNeeded==0){
				if(b<0x80){
					buffer[offset++]=(b);
					count++;
					length--;
					continue;
				} else if(b>=0xc2 && b<=0xdf){
					stream.mark(4);
					bytesNeeded=1;
					cp=b-0xc0;
				} else if(b>=0xe0 && b<=0xef){
					stream.mark(4);
					lower=(b==0xe0) ? 0xa0 : 0x80;
					upper=(b==0xed) ? 0x9f : 0xbf;
					bytesNeeded=2;
					cp=b-0xe0;
				} else if(b>=0xf0 && b<=0xf4){
					stream.mark(4);
					lower=(b==0xf0) ? 0x90 : 0x80;
					upper=(b==0xf4) ? 0x8f : 0xbf;
					bytesNeeded=2;
					cp=b-0xe0;
				} else {
					buffer[offset++]=(0xFFFD);
					count++;
					length--;
					continue;
				}
				cp<<=(6*bytesNeeded);
				continue;
			}
			if(b<lower || b>upper){
				cp=bytesNeeded=bytesSeen=0;
				lower=0x80;
				upper=0xbf;
				stream.reset(); // 'Decrease the byte pointer by one.'
				buffer[offset++]=(0xFFFD);
				count++;
				length--;
			}
			lower=0x80;
			upper=0xbf;
			bytesSeen++;
			cp+=(b-0x80)<<(6*(bytesNeeded-bytesSeen));
			stream.mark(4);
			if(bytesSeen!=bytesNeeded) {
				continue;
			}
			buffer[offset++]=(cp);
			count++;
			length--;
			cp=0;
			bytesSeen=0;
			bytesNeeded=0;
		}
		return (count<=0) ? -1 : count;
	}

	@Override
	public void encode(OutputStream stream, int[] array, int offset, int length)
			throws IOException {
		if(stream==null || array==null)throw new IllegalArgumentException();
		if(offset<0 || length<0 || offset+length>array.length)
			throw new IndexOutOfBoundsException();
		for(int i=0;i<array.length;i++){
			int cp=array[offset+i];
			if(cp<0 || cp>=0x10000 || (cp>=0xd800 && cp<=0xdfff))
				throw new UnmappableCharacterException(1);
			if(cp<=0x7F){
				stream.write(cp);
			} else if(cp<=0x7FF){
				stream.write((0xC0|((cp>>6)&0x1F)));
				stream.write((0x80|(cp   &0x3F)));
			} else if(cp<=0xFFFF){
				stream.write((0xE0|((cp>>12)&0x0F)));
				stream.write((0x80|((cp>>6 )&0x3F)));
				stream.write((0x80|(cp      &0x3F)));
			} else {
				stream.write((0xF0|((cp>>18)&0x07)));
				stream.write((0x80|((cp>>12)&0x3F)));
				stream.write((0x80|((cp>>6 )&0x3F)));
				stream.write((0x80|(cp      &0x3F)));
			}
		}
	}

}
