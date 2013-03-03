package com.upokecenter.encoding;

import java.io.IOException;
import java.io.OutputStream;

public interface IEncodingError {
	public int emitDecoderError(int[] buffer, int offset, int length) throws IOException;
	public void emitEncoderError(OutputStream stream) throws IOException;
}
