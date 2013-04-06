/*
Written in 2013 by Peter Occil.  Released to the public domain.
Public domain dedication: http://creativecommons.org/publicdomain/zero/1.0/
 */
package com.upokecenter.io;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.MalformedInputException;
/**
 * 
 * Lightweight character input for UTF-8.
 * 
 * @author Peter
 *
 */
public class Utf8CharacterInput implements ICharacterInput {

	InputStream stream;

	public Utf8CharacterInput(InputStream stream){
		this.stream=stream;
	}

	@Override
	public int read(int[] buf, int offset, int unitCount)
			throws IOException {
		if((buf)==null)throw new NullPointerException("buf");
		if((offset)<0)throw new IndexOutOfBoundsException("offset"+" not greater or equal to "+"0"+" ("+Integer.toString(offset)+")");
		if((unitCount)<0)throw new IndexOutOfBoundsException("unitCount"+" not greater or equal to "+"0"+" ("+Integer.toString(unitCount)+")");
		if((offset+unitCount)>buf.length)throw new IndexOutOfBoundsException("offset+unitCount"+" not less or equal to "+Integer.toString(buf.length)+" ("+Integer.toString(offset+unitCount)+")");
		if(unitCount==0)return 0;
		for(int i=0;i<unitCount;i++){
			int c=read();
			if(c<0)
				return i==0 ? -1 : i;
			buf[offset++]=c;
		}
		return unitCount;
	}

	@Override
	public int read() throws IOException {
		int cp=0;
		int bytesSeen=0;
		int bytesNeeded=0;
		int lower=0x80;
		int upper=0xBF;
		while(true){
			int b=stream.read();
			if(b<0 && bytesNeeded!=0){
				bytesNeeded=0;
				throw new MalformedInputException(1);
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
					bytesNeeded=3;
					cp=b-0xf0;
				} else
					throw new MalformedInputException(1);
				cp<<=(6*bytesNeeded);
				continue;
			}
			if(b<lower || b>upper){
				cp=bytesNeeded=bytesSeen=0;
				lower=0x80;
				upper=0xbf;
				stream.reset();
				throw new MalformedInputException(1);
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
}