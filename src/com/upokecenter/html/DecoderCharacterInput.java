package com.upokecenter.html;

import java.io.IOException;
import java.io.InputStream;

import com.upokecenter.encoding.IEncodingError;
import com.upokecenter.encoding.ITextDecoder;
import com.upokecenter.encoding.TextEncoding;
import com.upokecenter.util.ICharacterInput;

final class DecoderCharacterInput implements ICharacterInput {
	private final InputStream input;
	private final ITextDecoder decoder;
	private IEncodingError error=TextEncoding.ENCODING_ERROR_REPLACE;

	public DecoderCharacterInput(InputStream input, ITextDecoder decoder) {
		this.input=input;
		this.decoder=decoder;
	}

	public DecoderCharacterInput(InputStream input, ITextDecoder decoder, IEncodingError error) {
		this.input=input;
		this.decoder=decoder;
		this.error=error;
	}

	@Override
	public int read(int[] buf, int offset, int unitCount) throws IOException {
		return decoder.decode(input,buf,offset,unitCount,error);
	}

	@Override
	public int read() throws IOException {
		return decoder.decode(input,error);
	}

}
