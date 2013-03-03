package com.upokecenter.encoding;

import java.io.IOException;
import java.io.InputStream;

class ReplacementDecoder implements ITextDecoder {

	boolean endofstream=false;

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
		if(!endofstream){
			endofstream=true;
			int o=error.emitDecoderError(buffer, offset, length);
			return o;
		}
		return -1;
	}

	@Override
	public int decode(InputStream stream) throws IOException {
		return decode(stream, TextEncoding.ENCODING_ERROR_THROW);
	}

	@Override
	public int decode(InputStream stream, IEncodingError error) throws IOException {
		if(!endofstream){
			endofstream=true;
			if(error.equals(TextEncoding.ENCODING_ERROR_REPLACE))
				return 0xFFFD;
			else {
				int[] data=new int[1];
				int o=error.emitDecoderError(data,0,1);
				return (o==0) ? -1 : data[0];
			}
		}
		return -1;
	}
}
