/*
Written in 2013 by Peter Occil.
Any copyright to this work is released to the Public Domain.
In case this is not possible, this work is also
licensed under the Unlicense: https://unlicense.org/

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
   *
   * Reads the next Unicode character.
   *
   * @return A Unicode code point or -1 if the end of
   * the input is reached
   * @throws IOException if an I/O error occurs.
   */
  public int read() throws IOException;

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
}
