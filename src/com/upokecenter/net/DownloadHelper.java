package com.upokecenter.net;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.upokecenter.util.Base64;
import com.upokecenter.util.ByteList;
import com.upokecenter.util.DateTimeUtility;
import com.upokecenter.util.StringUtility;
import com.upokecenter.util.URL;

public final class DownloadHelper {

	private DownloadHelper(){}

	private static final class CacheFilter {
		private final String cacheFileName;

		public CacheFilter(String cacheFileName) {
			this.cacheFileName = cacheFileName;
		}

		public boolean accept(File dir, String filename) {
			return filename.startsWith(cacheFileName+"-") &&
					!filename.endsWith(".cache");
		}
	}

	private static String getCacheFileName(String uri, boolean[] incomplete){
		StringBuilder builder=new StringBuilder();
		for(int i=0;i<uri.length();i++){
			char c=uri.charAt(i);
			if(c<=0x20 || c==127 ||
					c=='$' || c=='/' || c=='\\' || c==':' ||
					c=='"' || c=='\'' || c=='|' || c=='<' ||
					c=='>' || c=='*' || c=='?'){
				builder.append('$');
				builder.append("0123456789ABCDEF".charAt((c>>4)&15));
				builder.append("0123456789ABCDEF".charAt((c)&15));
			} else {
				builder.append(c);
			}
			if(builder.length()>=190){
				if(incomplete!=null) {
					incomplete[0]=true;
				}
				return builder.toString();
			}
		}
		if(incomplete!=null) {
			incomplete[0]=false;
		}
		return builder.toString();
	}

	 static class FileBasedHeaders implements IHttpHeaders {

		long date,length;
		String urlString;

		public FileBasedHeaders(String urlString, long length){
			date=DateTimeUtility.getCurrentDate();
			this.length=length;
			this.urlString=urlString;
		}

		@Override
		public String getRequestMethod() {
			return "GET";
		}

		@Override
		public String getHeaderField(String name) {
			if(name==null)return "HTTP/1.1 200 OK";
			if("date".equals(StringUtility.toLowerCaseAscii(name)))
				return HeaderParser.formatHttpDate(date);
			if("content-length".equals(StringUtility.toLowerCaseAscii(name)))
				return Long.toString(length);
			return null;
		}

		@Override
		public String getHeaderField(int name) {
			if(name==0)
				return getHeaderField(null);
			else if(name==1)
				return getHeaderField("date");
			else if(name==2)
				return getHeaderField("content-length");
			return null;
		}

		@Override
		public String getHeaderFieldKey(int name) {
			if(name==0)
				return null;
			if(name==1)
				return "date";
			if(name==2)
				return "content-length";
			return null;
		}

		@Override
		public int getResponseCode() {
			return 200;
		}

		@Override
		public long getHeaderFieldDate(String field, long defaultValue) {
			if(field!=null && "date".equals(StringUtility.toLowerCaseAscii(field)))
				return date;
			return defaultValue;
		}

		private List<String> asReadOnlyList(String[] a){
			return Collections.unmodifiableList(Arrays.asList(a));
		}

		@Override
		public Map<String, List<String>> getHeaderFields() {
			Map<String, List<String>> map=new HashMap<String, List<String>>();
			map.put(null,asReadOnlyList(new String[]{getHeaderField(null)}));
			map.put("date",asReadOnlyList(new String[]{getHeaderField("date")}));
			map.put("content-length",asReadOnlyList(new String[]{getHeaderField("content-length")}));
			return Collections.unmodifiableMap(map);
		}

		@Override
		public String getUrl() {
			return urlString;
		}
	}

	private static void recursiveListFiles(File file, List<File> files){
		for(File f : file.listFiles()){
			if(f.isDirectory()){
				recursiveListFiles(f,files);
			}
			files.add(f);
		}
	}

	public static void pruneCache(File cache, long maximumSize){
		if(cache==null || !cache.isDirectory())return;
		while(true){
			long length=0;
			boolean exceeded=false;
			long oldest=Long.MAX_VALUE;
			int count=0;
			List<File> files=new ArrayList<File>();
			recursiveListFiles(cache,files);
			for(File file : files){
				if(file.isFile()){
					length+=file.length();
					if(length>maximumSize){
						exceeded=true;
					}
					oldest=file.lastModified();
					count++;
				}
			}
			if(count<=1||!exceeded)return;
			long threshold=oldest+Math.abs(oldest-DateTimeUtility.getCurrentDate())/2;
			count=0;
			for(File file : files){
				if(file.lastModified()<threshold){
					if(file.isDirectory()){
						if(file.delete()) {
							count++;
						}
					} else {
						length-=file.length();
						if(file.delete()) {
							count++;
						}
						if(length<maximumSize)
							return;
					}
				}
			}
			if(count==0)return;
		}
	}




	public static Object getLegacyResponseCache(File cachePath){
		return DownloadHelperImpl.newResponseCache(cachePath);
	}


	 static class CacheResponseInfo {
		public Object cr=null;
		public File trueCachedFile=null;
		public File trueCacheInfoFile=null;
	}
	 static CacheResponseInfo getCachedResponse(
			String urlString,
			File pathForCache,
			boolean getStream
			){
		boolean[] incompleteName=new boolean[1];
		final String cacheFileName=getCacheFileName(urlString,incompleteName)+".htm";
		File trueCachedFile=null;
		File trueCacheInfoFile=null;
		CacheResponseInfo crinfo=new CacheResponseInfo();
		if(pathForCache!=null && pathForCache.isDirectory()){
			File[] cacheFiles=new File[]{
					new File(pathForCache,cacheFileName)
			};
			if(incompleteName[0]){
				ArrayList<File> list=new ArrayList<File>();
				CacheFilter filter=new CacheFilter(cacheFileName);
				for(File f : pathForCache.listFiles()){
					if(filter.accept(pathForCache,f.getName())){
						list.add(f);
					}
				}
				cacheFiles=list.toArray(new File[]{});
			} else if(!getStream){
				crinfo.trueCachedFile=cacheFiles[0];
				crinfo.trueCacheInfoFile=new File(crinfo.trueCachedFile.toString()+".cache");
				return crinfo;
			}
			//DebugUtility.log("%s, getStream=%s",Arrays.asList(cacheFiles),getStream);
			for(File cacheFile : cacheFiles){
				if(cacheFile.isFile() && getStream){
					boolean fresh=false;
					IHttpHeaders headers=null;
					File cacheInfoFile=new File(cacheFile.toString()+".cache");
					if(cacheInfoFile.isFile()){
						try {
							CacheControl cc=CacheControl.fromFile(cacheInfoFile);
							//DebugUtility.log("havecache: %s",cc!=null);
							if(cc==null){
								fresh=false;
							} else {
								fresh=(cc==null) ? false : cc.isFresh();
								if(!urlString.equals(cc.getUri())){
									// Wrong URI
									continue;
								}
								//DebugUtility.log("reqmethod: %s",cc.getRequestMethod());
								if(!"get".equals(cc.getRequestMethod())){
									fresh=false;
								}
							}
							headers=(cc==null) ? new FileBasedHeaders(urlString,cacheFile.length()) : cc.getHeaders(cacheFile.length());
						} catch (IOException e) {
							e.printStackTrace();
							fresh=false;
							headers=new FileBasedHeaders(urlString,cacheFile.length());
						}
					} else {
						long maxAgeMillis=24L*3600L*1000L;
						long timeDiff=Math.abs(cacheFile.lastModified()-(DateTimeUtility.getCurrentDate()));
						fresh=(timeDiff<=maxAgeMillis);
						headers=new FileBasedHeaders(urlString,cacheFile.length());
					}
					//DebugUtility.log("fresh=%s",fresh);
					if(!fresh){
						// Too old, download again
						trueCachedFile=cacheFile;
						trueCacheInfoFile=cacheInfoFile;
						trueCachedFile.delete();
						trueCacheInfoFile.delete();
						break;
					} else {
						InputStream stream=null;
						try {
							stream=new BufferedInputStream(new FileInputStream(cacheFile.toString()),8192);
							crinfo.cr=DownloadHelperImpl.newCacheResponse(stream,
									headers);
							//DebugUtility.log("headerfields: %s",headers.getHeaderFields());
						} catch (IOException e) {
							// if we get an exception here, we download again
							crinfo.cr=null;
						} finally {
							if(stream!=null) {
								try { stream.close(); } catch(IOException e){}
							}
						}
					}
				}
			}
		}
		if(pathForCache!=null){
			if(trueCachedFile==null){
				if(incompleteName[0]){
					int i=0;
					do {
						trueCachedFile=new File(pathForCache,
								cacheFileName+"-"+Integer.toString(i));
						i++;
					} while(trueCachedFile.exists());
				} else {
					trueCachedFile=new File(pathForCache,cacheFileName);
				}
			}
			trueCacheInfoFile=new File(trueCachedFile.toString()+".cache");
		}
		crinfo.trueCachedFile=trueCachedFile;
		crinfo.trueCacheInfoFile=trueCacheInfoFile;
		return crinfo;
	}

	 static class ErrorHeader implements IHttpHeaders {
		String message;
		int code;
		String urlString;

		public ErrorHeader(String urlString, int code, String message){
			this.urlString=urlString;
			this.code=code;
			this.message=message;
		}

		@Override
		public String getRequestMethod() {
			return "GET";
		}

		@Override
		public String getHeaderField(String name) {
			if(name==null)return "HTTP/1.1 "+Integer.toString(code)+" "+message;
			return null;
		}

		@Override
		public String getHeaderField(int name) {
			if(name==0)
				return getHeaderField(null);
			return null;
		}

		@Override
		public String getHeaderFieldKey(int name) {
			if(name==0)
				return null;
			return null;
		}



		@Override
		public int getResponseCode() {
			return code;
		}

		@Override
		public long getHeaderFieldDate(String field, long defaultValue) {
			return defaultValue;
		}

		private List<String> asReadOnlyList(String[] a){
			return Collections.unmodifiableList(Arrays.asList(a));
		}

		@Override
		public Map<String, List<String>> getHeaderFields() {
			Map<String, List<String>> map=new HashMap<String, List<String>>();
			map.put(null,asReadOnlyList(new String[]{getHeaderField(null)}));
			return Collections.unmodifiableMap(map);
		}

		@Override
		public String getUrl() {
			return urlString;
		}

	}

	 static class DataURLHeaders implements IHttpHeaders {

		String urlString;
		String contentType;

		public DataURLHeaders(String urlString, long length, String contentType){
			this.urlString=urlString;
			this.contentType=contentType;
		}

		@Override
		public String getRequestMethod() {
			return "GET";
		}

		@Override
		public String getHeaderField(String name) {
			if(name==null)return "HTTP/1.1 200 OK";
			if("content-type".equals(StringUtility.toLowerCaseAscii(name)))
				return contentType;
			return null;
		}

		@Override
		public String getHeaderField(int name) {
			if(name==0)
				return getHeaderField(null);
			if(name==1)
				return getHeaderField("content-type");
			return null;
		}

		@Override
		public String getHeaderFieldKey(int name) {
			if(name==0)
				return null;
			if(name==1)
				return "content-type";
			return null;
		}



		@Override
		public int getResponseCode() {
			return 200;
		}

		@Override
		public long getHeaderFieldDate(String field, long defaultValue) {
			return defaultValue;
		}

		private List<String> asReadOnlyList(String[] a){
			return Collections.unmodifiableList(Arrays.asList(a));
		}

		@Override
		public Map<String, List<String>> getHeaderFields() {
			Map<String, List<String>> map=new HashMap<String, List<String>>();
			map.put(null,asReadOnlyList(new String[]{getHeaderField(null)}));
			map.put("content-type",asReadOnlyList(new String[]{getHeaderField("content-type")}));
			return Collections.unmodifiableMap(map);
		}

		@Override
		public String getUrl() {
			return urlString;
		}
	}

	private static String getDataURLContentType(String data){
		int index=HeaderParser.skipContentType(data, 0);
		String ctype=null;
		if(index==0)
			return "text/plain;charset=US-ASCII";
		else if(data.charAt(0)==';'){
			ctype="text/plain"+data.substring(0,index);
		} else {
			ctype=data.substring(0,index);
		}
		if(ctype.indexOf('%')<0)
			return ctype;
		ctype=HeaderParser.unescapeContentType(ctype,0);
		if(ctype==null || ctype.length()==0)
			return "text/plain;charset=US-ASCII";
		return ctype;
	}


	private static byte[] getDataURLBytes(String data){
		int index=HeaderParser.skipContentType(data, 0);
		if(data.startsWith(";base64,",index)){
			index+=8;
			try {
				data=data.substring(index);
				return Base64.decode(data,Base64.DONT_GUNZIP);
			} catch (IOException e) {
				return null;
			}
		} else if(index<data.length() && data.charAt(index)==','){
			index++;
			ByteList mos=new ByteList();
			int len=data.length();
			for(int j=index;j<len;j++){
				int c=data.charAt(j);
				if(!StringUtility.isChar(c,"-_.!~*'()") &&
						!(c>='A' && c<='Z') &&
						!(c>='a' && c<='z') &&
						!(c>='0' && c<='9'))
					return null;
				if(c=='%'){
					if(index+2<len){
						int a=HeaderParser.toHexNumber(data.charAt(index+1));
						int b=HeaderParser.toHexNumber(data.charAt(index+2));
						if(a>=0 && b>=0){
							mos.append((byte) (a*16+b));
							index+=2;
							continue;
						}
					}
				}
				mos.append((byte) (c&0xFF));
			}
			return mos.toByteArray();
		} else
			return null;
	}



	/**
	 * 
	 * Connects to a URL to download data from that URL.
	 * 
	 * @param urlString a URL string.  All schemes (protocols)
	 * supported by Java's URLConnection are supported.  Data
	 * URLs are also supported.
	 * @param callback an object to call back on, particularly
	 * when the data is ready to be downloaded. Can be null.  If the
	 * object also implements IDownloadEventListener, it will also
	 * have its onConnecting and onConnected methods called.
	 * @return the object returned by the callback's processResponse
	 * method.
	 * @throws IOException if an I/O error occurs, particularly
	 * network errors.
	 * @throws NullPointerException if urlString is null.
	 */
	public static <T> T downloadUrl(
			String urlString,
			final IResponseListener<T> callback
			) throws IOException{
		return downloadUrl(urlString, callback, false);
	}

	/**
	 * 
	 * Connects to a URL to download data from that URL.
	 * 
	 * @param urlString a URL string.  All schemes (protocols)
	 * supported by Java's URLConnection are supported.  Data
	 * URLs are also supported.
	 * @param callback an object to call back on, particularly
	 * when the data is ready to be downloaded. Can be null.  If the
	 * object also implements IDownloadEventListener, it will also
	 * have its onConnecting and onConnected methods called.
	 * @param handleErrorResponses if true, the processResponse method
	 * of the supplied callback object
	 * will also be called if an error response is returned. In this
	 * case, the _stream_ argument of that method will contain the error
	 * response body, if any, or null otherwise. If false and
	 * an error response is received, an IOException may be thrown instead
	 * of calling the processResponse method.
	 * This parameter does not affect whether an exception is thrown
	 * if the connection fails.
	 * @return the object returned by the callback's processResponse
	 * method.
	 * @throws IOException if an I/O error occurs, particularly
	 * network errors.
	 * @throws NullPointerException if urlString is null.
	 */
	public static <T> T downloadUrl(
			String urlString,
			final IResponseListener<T> callback,
			boolean handleErrorResponses
			) throws IOException{
		if(urlString==null)throw new NullPointerException();
		final boolean isEventHandler=(callback!=null && callback instanceof IDownloadEventListener<?>);
		URL uri=null;
		if(isEventHandler && callback!=null) {
			((IDownloadEventListener<T>)callback).onConnecting(urlString);
		}
		uri=URL.parse(urlString);
		if(uri==null)
			throw new IllegalArgumentException();
		//
		// About URIs
		//
		if("about".equals(uri.getScheme())){
			String ssp=uri.getSchemeData();
			if(!"blank".equals(ssp)){
				if(!handleErrorResponses)
					throw new IOException();
				if(isEventHandler && callback!=null) {
					((IDownloadEventListener<T>)callback).onConnected(urlString);
				}
				T ret=(callback==null) ? null : callback.processResponse(urlString,null,
						new ErrorHeader(urlString,400,"Bad Request"));
				return ret;
			} else {
				String contentType="text/html;charset=utf-8";
				InputStream stream=null;
				try {
					stream = new ByteArrayInputStream(new byte[]{});
					if(isEventHandler && callback!=null) {
						((IDownloadEventListener<T>)callback).onConnected(urlString);
					}
					T ret=(callback==null) ? null : callback.processResponse(urlString,stream,
							new DataURLHeaders(urlString,0,contentType));
					return ret;
				} finally {
					if(stream!=null){
						try {
							stream.close();
						} catch (IOException e) {}
					}
				}
			}
		}
		//
		// Data URLs
		//
		if("data".equals(uri.getScheme())){
			// NOTE: Only "GET" is allowed here
			String ssp=uri.getSchemeData();
			byte[] bytes=getDataURLBytes(ssp);
			if(bytes==null){
				if(!handleErrorResponses)
					throw new IOException();
				if(isEventHandler && callback!=null) {
					((IDownloadEventListener<T>)callback).onConnected(urlString);
				}
				T ret=(callback==null) ? null : callback.processResponse(urlString,null,
						new ErrorHeader(urlString,400,"Bad Request"));
				return ret;
			} else {
				String contentType=getDataURLContentType(ssp);
				InputStream stream=null;
				try {
					stream = new BufferedInputStream(
							new ByteArrayInputStream(bytes),
							Math.max(32,Math.min(8192, bytes.length)));
					if(isEventHandler && callback!=null) {
						((IDownloadEventListener<T>)callback).onConnected(urlString);
					}
					T ret=(callback==null) ? null : callback.processResponse(urlString,stream,
							new DataURLHeaders(urlString,bytes.length,contentType));
					return ret;
				} finally {
					if(stream!=null){
						try {
							stream.close();
						} catch (IOException e) {}
					}
				}
			}
		}
		//
		// Other URLs
		//
		return DownloadHelperImpl.downloadUrl(urlString, callback, handleErrorResponses);
	}

}
