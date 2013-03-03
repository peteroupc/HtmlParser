package com.upokecenter.encoding;

import java.io.IOException;
import java.io.InputStream;

public interface ITextDecoder {
	public int decode(InputStream stream, int[] buffer, int offset, int length) throws IOException;
	public int decode(InputStream stream, int[] buffer, int offset, int length, IEncodingError error) throws IOException;
	public int decode(InputStream stream) throws IOException;
	public int decode(InputStream stream, IEncodingError error) throws IOException;
}
