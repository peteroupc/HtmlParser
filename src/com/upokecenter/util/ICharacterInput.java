package com.upokecenter.util;

import java.io.IOException;

public interface ICharacterInput {

	public int read(int[] buf, int offset, int unitCount)
			throws IOException;

	public int read() throws IOException;
}
