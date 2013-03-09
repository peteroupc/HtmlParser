package com.upokecenter.util;

import java.io.IOException;

/**
 * An abstract stream of Unicode characters.
 * 
 * @author Peter
 *
 */
public interface ICharacterInput {

	/**
	 * Reads several Unicode characters into a buffer.
	 * 
	 * @param buf
	 * @param offset
	 * @param unitCount
	 * @return The number of Unicode characters read,
	 * or -1 if the end of the input is reached
	 * @throws IOException if an I/O error occurs.
	 */
	public int read(int[] buf, int offset, int unitCount)
			throws IOException;

	/**
	 * 
	 * Reads the next Unicode character.
	 * 
	 * @return A Unicode code point or -1 if the end of
	 * the input is reached
	 * @throws IOException if an I/O error occurs.
	 */
	public int read() throws IOException;
}
