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
package com.upokecenter.encoding;

import java.io.IOException;
import java.io.OutputStream;

/**
 * 
 * An interface implemented by classes that handle errors that
 * occur when converting bytes to and from Unicode characters.
 * 
 * @author Peter
 *
 */
public interface IEncodingError {
	/**
	 * 
	 * Handles an error when decoding bytes into Unicode characters.
	 * 
	 * @param buffer an array to output Unicode characters
	 * @param offset the offset to the array to write characters
	 * @param length the number of characters available
	 * in the buffer
	 * @return the number of characters emitted.  Note that
	 * currently, the objects provided by this package do not
	 * fully support error handlers that emit more than one character
	 * as a decoder error, so that additional characters that
	 * would overflow the buffer passed to the decode methods
	 * may be ignored.
	 * @throws IOException if the method decides to handle the
	 * error by throwing an IOException or a derived class, or
	 * if another I/O error occurs.
	 */
	public int emitDecoderError(int[] buffer, int offset, int length) throws IOException;
	/**
	 * Handles an error when encoding Unicode characters into bytes.
	 * 
	 * @param stream a stream to write bytes
	 * @param codePoint the code point that caused the encoder error
	 * @throws IOException if the method decides to handle the
	 * error by throwing an IOException or a derived class, or
	 * if another I/O error occurs.
	 */
	public void emitEncoderError(OutputStream stream, int codePoint) throws IOException;
}
