/*
If you like this, you should donate to Peter O.
at: http://upokecenter.com/d/



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
package com.upokecenter.net;

import java.io.IOException;
import java.io.InputStream;

public interface IResponseListener<T> {
	/**
	 * Processes the Web response on a background thread.
	 * Please note: For the response to be cacheable, the entire
	 * stream must be read to the end.
	 * @param url URL of the resource. This may not be the same
	 * as the URL that the resource actually resolves to. For that,
	 * call the getUrl() method of the _headers_ object.
	 * @param stream Input stream for the response body.
	 *   The listener must not close the stream.
	 * @param headers Contains the headers returned by the response.
	 * 
	 * @throws IOException
	 */
	public T processResponse(String url,
			InputStream stream, IHttpHeaders headers) throws IOException;
}