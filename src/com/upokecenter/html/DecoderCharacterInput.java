package com.upokecenter.html;

import java.io.IOException;
import java.io.InputStream;

import com.upokecenter.encoding.ITextDecoder;
import com.upokecenter.encoding.TextEncoding;
import com.upokecenter.util.ICharacterInput;

public final class DecoderCharacterInput implements ICharacterInput {
	private InputStream input;
	private ITextDecoder decoder;

	public DecoderCharacterInput(InputStream input, ITextDecoder decoder) {
		this.input=input;
		this.decoder=decoder;
	}


	@Override
	public int read(int[] buf, int offset, int unitCount) throws IOException {
		return decoder.decode(input,buf,offset,unitCount, TextEncoding.ENCODING_ERROR_REPLACE);
	}

	@Override
	public int read() throws IOException {
		return decoder.decode(input, TextEncoding.ENCODING_ERROR_REPLACE);
	}

}
