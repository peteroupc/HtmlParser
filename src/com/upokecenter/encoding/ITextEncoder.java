package com.upokecenter.encoding;

import java.io.IOException;
import java.io.OutputStream;

public interface ITextEncoder {
	public void encode(OutputStream stream, int[] buffer, int offset, int length) throws IOException;

	public void encode(OutputStream stream, int[] buffer, int offset, int length, IEncodingError error) throws IOException;
}
