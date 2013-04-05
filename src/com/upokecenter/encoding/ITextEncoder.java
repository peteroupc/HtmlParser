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
 * Converts Unicode characters to bytes.
 * @author Peter
 *
 */
public interface ITextEncoder {
	/**
	 * Writes Unicode characters as bytes in an output stream.
	 * 
	 * @param stream stream where bytes will be written
	 * @param buffer an array of Unicode characters
	 * @param offset offset into the array
	 * @param length number of characters to write
	 * @throws IOException if there are characters that can't be
	 * converted to bytes, or if another I/O error occurs.
	 */
	public void encode(OutputStream stream, int[] buffer, int offset, int length) throws IOException;

	/**
	 * Writes Unicode characters as bytes in an output stream.
	 * 
	 * @param stream stream where bytes will be written
	 * @param buffer an array of Unicode characters
	 * @param offset offset into the array
	 * @param length number of characters to write
	 * @param error error handler to use.  If there are characters
	 * that can't be converted to bytes, this object's emitEncoderError
	 * method is called.
	 * @throws IOException if an I/O error occurs
	 */
	public void encode(OutputStream stream, int[] buffer, int offset, int length, IEncodingError error) throws IOException;
}
