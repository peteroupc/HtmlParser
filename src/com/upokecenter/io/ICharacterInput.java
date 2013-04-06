/*
Written in 2013 by Peter Occil.  Released to the public domain.
Public domain dedication: http://creativecommons.org/publicdomain/zero/1.0/
 */
package com.upokecenter.io;

import java.io.IOException;

/**
 * An abstract stream of Unicode characters.
 * 
 * @author Peter
 *
 */
public interface ICharacterInput {

	/**
	 * Reads multiple Unicode characters into a buffer.
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
