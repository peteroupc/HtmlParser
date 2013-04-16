/*

Licensed under the Expat License.

Copyright (C) 2013 Peter Occil

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.

 */

package com.upokecenter.html;

import java.io.IOException;
import java.io.InputStream;

import com.upokecenter.encoding.IEncodingError;
import com.upokecenter.encoding.ITextDecoder;
import com.upokecenter.encoding.TextEncoding;
import com.upokecenter.io.ICharacterInput;

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
	public int read() throws IOException {
		return decoder.decode(input,error);
	}

	@Override
	public int read(int[] buf, int offset, int unitCount) throws IOException {
		return decoder.decode(input,buf,offset,unitCount,error);
	}

}
