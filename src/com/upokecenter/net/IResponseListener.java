package com.upokecenter.net;

import java.io.IOException;
import java.io.InputStream;

public interface IResponseListener<T> {
	/**
	 * Processes the HTTP response on a background thread.
	 * Please note: For the response to be cacheable, the entire
	 * stream must be read to the end.
	 * @param url URL of the resource.
	 * @param stream Input stream for the response body.
	 *   The listener must not close the stream.
	 * @param headers
	 * 
	 * @throws IOException
	 */
	public T processResponse(String url,
			InputStream stream, IHttpHeaders headers) throws IOException;
}