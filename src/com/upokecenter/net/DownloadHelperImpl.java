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
package com.upokecenter.net;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.CacheRequest;
import java.net.CacheResponse;
import java.net.HttpURLConnection;
import java.net.ResponseCache;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.upokecenter.net.DownloadHelper.CacheResponseInfo;
import com.upokecenter.util.DateTimeUtility;
import com.upokecenter.util.Reflection;
import com.upokecenter.util.StringUtility;

final class DownloadHelperImpl {
	private DownloadHelperImpl(){}

	private static class HttpHeaders implements IHttpHeaders {

		URLConnection connection;
		public HttpHeaders(URLConnection connection){
			this.connection=connection;
		}

		@Override
		public String getHeaderField(String name) {
			if(name==null){
				try {
					return connection.getHeaderField(name);
				} catch(NullPointerException e){
					// Earlier versions of Android, particularly
					// Froyo (API level 8), do not support getting
					// the status line by passing a header field key
					// of null, so we use this call instead
					return connection.getHeaderField(0);
				}
			}
			return connection.getHeaderField(name);
		}

		@Override
		public String getHeaderField(int name) {
			return connection.getHeaderField(name);
		}

		@Override
		public String getHeaderFieldKey(int name) {
			return connection.getHeaderFieldKey(name);
		}

		@Override
		public int getResponseCode() {
			try {
				if(connection instanceof HttpURLConnection)
					return (((HttpURLConnection)connection).getResponseCode());
				else
					return 0;
			} catch (IOException e) {
				return -1;
			}
		}

		@Override
		public long getHeaderFieldDate(String field, long defaultValue) {
			return connection.getHeaderFieldDate(field,defaultValue);
		}

		@Override
		public Map<String, List<String>> getHeaderFields() {
			return connection.getHeaderFields();
		}

		@Override
		public String getRequestMethod() {
			if(connection instanceof HttpURLConnection)
				return (((HttpURLConnection)connection).getRequestMethod());
			else
				return "";
		}

		@Override
		public String getUrl() {
			return connection.getURL().toString();
		}

	}

	private static class LegacyHttpResponseCache extends ResponseCache {

		private static final class LegacyCacheRequest extends CacheRequest {
			private final CacheResponseInfo crinfo;
			private final CacheControl cc;

			private LegacyCacheRequest(CacheResponseInfo crinfo,
					CacheControl cc) {
				this.crinfo = crinfo;
				this.cc = cc;
			}

			@Override
			public void abort() {
				//DebugUtility.log("deleted, aborted: %s %s",crinfo.trueCachedFile,
				//	crinfo.trueCacheInfoFile);
				crinfo.trueCachedFile.delete();
				crinfo.trueCacheInfoFile.delete();
			}

			@Override
			public OutputStream getBody() throws IOException {
				//DebugUtility.log("getting request body %s %s",crinfo.trueCachedFile,
				//	crinfo.trueCacheInfoFile);
				try {
					CacheControl.toFile(cc,crinfo.trueCacheInfoFile);
					return new FileOutputStream(crinfo.trueCachedFile);
				} catch(IOException e){
					//DebugUtility.log("IOException");
					e.printStackTrace();
					throw e;
				}
			}
		}

		File cachePath;
		public LegacyHttpResponseCache(File cachePath){
			this.cachePath=cachePath;
		}

		@Override
		public CacheResponse get(URI arg0, String arg1,
				Map<String, List<String>> arg2) throws IOException {
			if(arg0==null || arg1==null || arg2==null)
				throw new IllegalArgumentException();
			arg1=StringUtility.toLowerCaseAscii(arg1);
			if(!"get".equals(arg1))return null;
			CacheResponseInfo crinfo=DownloadHelper.getCachedResponse(arg0.toString(),cachePath,true);
			return (CacheResponse)crinfo.cr;
		}

		@Override
		public CacheRequest put(URI uri, final URLConnection connection)
				throws IOException {
			if(uri==null || connection==null)throw new IllegalArgumentException();
			if(cachePath==null)return null;
			boolean isPrivate=(cachePath==null) ? false : cachePath.toString().startsWith("/data/");
			final CacheControl cc=CacheControl.getCacheControl(
					new HttpHeaders(connection),DateTimeUtility.getCurrentDate());
			//DebugUtility.log("CacheRequest put %s -> %s",uri.toString(),
			//	connection.getURL().toString());
			final CacheResponseInfo crinfo=DownloadHelper.getCachedResponse(
					connection.getURL().toString(),cachePath,false);
			if(cc!=null && (cc.getCacheability()==2 || (isPrivate && cc.getCacheability()==1)) &&
					!cc.isNoTransform() && !cc.isNoStore())
				return new LegacyCacheRequest(crinfo, cc);
			return null;
		}

	}

	public static Object newCacheResponse(InputStream body, IHttpHeaders headers){
		return new LegacyHttpCacheResponse(body,headers);
	}

	private static class LegacyHttpCacheResponse extends CacheResponse {

		IHttpHeaders headers;
		InputStream body;
		public LegacyHttpCacheResponse(InputStream body, IHttpHeaders headers){
			this.body=body;
			this.headers=headers;
		}

		@Override
		public InputStream getBody() throws IOException {
			return body;
		}

		@Override
		public Map<String, List<String>> getHeaders() throws IOException {
			return headers.getHeaderFields();
		}
	}


	public static <T> T downloadUrl(
			String urlString,
			final IResponseListener<T> callback,
			boolean handleErrorResponses
			) throws IOException{
		final boolean isEventHandler=(callback!=null && callback instanceof IDownloadEventListener<?>);
		if(isEventHandler && callback!=null) {
			((IDownloadEventListener<T>)callback).onConnecting(urlString);
		}
		//
		// Other URLs
		//
		if(!"false".equals(System.getProperty("http.keepAlive"))){
			int androidVersion=(Integer)Reflection.getStaticFieldByName(
					Reflection.getClassForName("android.os.Build$VERSION"),"SDK_INT",-1);
			if(androidVersion>=0 && androidVersion<8){
				// See documentation for HttpURLConnection for why this
				// is necessary in Android 2.1 (Eclair) and earlier
				System.setProperty("http.keepAlive","false");
			}
		}
		URL url=null;
		InputStream stream=null;
		int network=(Integer)Reflection.invokeStaticByName(
				Reflection.getClassForName("com.upokecenter.android.net.ConnectivityHelper"),
				"getConnectedNetworkType",-1);
		String requestMethod="GET";
		boolean calledConnecting=false;
		if(network==0){
			ResponseCache cache=ResponseCache.getDefault();
			if(cache!=null){
				CacheResponse response=null;
				try {
					if(isEventHandler && callback!=null) {
						((IDownloadEventListener<T>)callback).onConnecting(urlString);
					}
					calledConnecting=true;
					response=cache.get(new URI(urlString),
							requestMethod,
							new HashMap<String,List<String>>());
				} catch (URISyntaxException e) {
					throw new NoConnectionException();
				} catch (IOException e) {
					throw new NoConnectionException();
				}
				if(response!=null){
					if(isEventHandler && callback!=null) {
						((IDownloadEventListener<T>)callback).onConnected(urlString);
					}
					IHttpHeaders headers=new HttpHeadersFromMap(urlString,
							requestMethod,response.getHeaders());
					InputStream streamBody=response.getBody();
					T ret=(callback==null) ? null : callback.processResponse(
							urlString,streamBody,headers);
					streamBody.close();
					return ret;
				}
			}
			throw new NoConnectionException();
		}
		URLConnection connection=null;
		try {
			url=new URL(urlString);
			if(isEventHandler && callback!=null && !calledConnecting){
				((IDownloadEventListener<T>)callback).onConnecting(urlString);
				calledConnecting=true;
			}
			connection = url.openConnection();
			connection.setUseCaches(true);
			connection.setDoInput(true);
			connection.setReadTimeout(10000);
			connection.setConnectTimeout(20000);
			if(connection instanceof HttpURLConnection){
				((HttpURLConnection)connection).setRequestMethod(requestMethod);
			}
			connection.connect();
			if(isEventHandler && callback!=null) {
				((IDownloadEventListener<T>)callback).onConnected(urlString);
			}
			stream = null;
			IHttpHeaders headers=new HttpHeaders(connection);
			try {
				stream = connection.getInputStream();
			} catch(IOException ex){
				if(!handleErrorResponses)
					throw ex;
				if(connection instanceof HttpURLConnection){
					stream=((HttpURLConnection)connection).getErrorStream();
				}
			}
			if(stream!=null) {
				stream=new BufferedInputStream(stream);
			}
			T ret=(callback==null) ? null : callback.processResponse(urlString,stream,headers);
			return ret;
		} finally {
			if(stream!=null){
				try {
					stream.close();
				} catch (IOException e) {}
			}
			if(connection!=null && connection instanceof HttpURLConnection){
				((HttpURLConnection)connection).disconnect();
			}
		}
	}


	public static Object newResponseCache(File cachePath) {
		return new LegacyHttpResponseCache(cachePath);
	}


}
