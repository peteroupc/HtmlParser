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