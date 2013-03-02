package com.upokecenter.encoding;

import java.io.IOException;
import java.io.InputStream;

class ReplacementDecoder implements ITextDecoder {

	boolean endofstream=false;

	@Override
	public int decode(InputStream stream, int[] buffer, int offset, int length)
			throws IOException {
		if(stream==null || buffer==null || offset<0 || length<0 ||
				offset+length>buffer.length)
			throw new IllegalArgumentException();
		if(length==0)return 0;
		if(!endofstream){
			endofstream=true;
			buffer[offset]=0xFFFD;
			return 1;
		}
		return -1;
	}

	@Override
	public int decode(InputStream stream) throws IOException {
		if(!endofstream){
			endofstream=true;
			return 0xFFFD;
		}
		return -1;
	}
}
