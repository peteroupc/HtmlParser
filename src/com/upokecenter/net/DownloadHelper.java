package com.upokecenter.net;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
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
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


import com.upokecenter.util.DebugUtility;
import com.upokecenter.util.IStreamObjectSerializer;
import com.upokecenter.util.Reflection;
import com.upokecenter.util.StreamUtility;

public final class DownloadHelper {

	private DownloadHelper(){}

	private interface ICacheControl {
		public int getCacheability();
		public boolean isNoStore();
		public boolean isNoTransform();
		public boolean isMustRevalidate();
		public boolean isFresh();
		public String getUri();
		public String getRequestMethod();
		public IHttpHeaders getHeaders(long contentLength);
	}

	private static String getCacheFileName(String uri, boolean[] incomplete){
		StringBuilder builder=new StringBuilder();
		for(int i=0;i<uri.length();i++){
			char c=uri.charAt(i);
			if(c<=0x20 || c==127 ||
					c=='$' || c=='/' || c=='\\' || c==':' ||
					c=='"' || c=='\'' || c=='|' || c=='<' ||
					c=='>' || c=='*' || c=='?'){
				builder.append(String.format(Locale.US,"$%02X",(int)c));
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

	private static class CacheControl implements ICacheControl {

		@Override
		public String toString() {
			return "CacheControl [cacheability=" + cacheability + ", noStore="
					+ noStore + ", noTransform=" + noTransform
					+ ", mustRevalidate=" + mustRevalidate + ", requestTime="
					+ requestTime + ", responseTime=" + responseTime + ", maxAge="
					+ maxAge + ", date=" + date + ", age=" + age + ", code=" + code
					+ ", headerFields=" + headers + "]";
		}
		private int cacheability=0;
		// Client must not store the response
		// to disk and must remove it from memory
		// as soon as it's finished with it
		private boolean noStore=false;
		// Client must not convert the response
		// to a different format before caching it
		private boolean noTransform=false;
		// Client must re-check the server
		// after the response becomes stale
		private boolean mustRevalidate=false;
		private long requestTime=0;
		private long responseTime=0;
		private long maxAge=0;
		private long date=0;
		private long age=0;
		private int code=0;
		private String uri="";
		private String requestMethod="";
		private List<String> headers;

		@Override
		public int getCacheability() {
			return cacheability;
		}
		@Override
		public boolean isNoStore() {
			return noStore;
		}
		@Override
		public boolean isNoTransform() {
			return noTransform;
		}
		@Override
		public boolean isMustRevalidate() {
			return mustRevalidate;
		}

		private long getAge(){
			long now=new Date().getTime();
			long age=Math.max(0,Math.max(now-date,this.age));
			age+=(responseTime-requestTime);
			age+=(now-responseTime);
			age=(age>Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int)age;
			return age;
		}

		@Override
		public boolean isFresh() {
			if(cacheability==0 || noStore)return false;
			return (maxAge>getAge());
		}

		private CacheControl(){
			headers=new ArrayList<String>();
		}

		public static ICacheControl getCacheControl(IHttpHeaders headers, long requestTime){
			CacheControl cc=new CacheControl();
			boolean proxyRevalidate=false;
			int sMaxAge=0;
			boolean publicCache=false;
			boolean privateCache=false;
			boolean noCache=false;
			long expires=0;
			boolean hasExpires=false;
			cc.uri=headers.getUrl();
			String cacheControl=headers.getHeaderField("cache-control");
			if(cacheControl!=null){
				int index=0;
				int[] intval=new int[1];
				while(index<cacheControl.length()){
					int current=index;
					if((index=HeaderParser.parseToken(cacheControl,current,"private",true))!=current){
						privateCache=true;
					} else if((index=HeaderParser.parseToken(cacheControl,current,"no-cache",true))!=current){
						noCache=true;
						//DebugUtility.log("returning early because it saw no-cache");
						return null; // return immediately, this is not cacheable
					} else if((index=HeaderParser.parseToken(
							cacheControl,current,"no-store",false))!=current){
						cc.noStore=true;
						DebugUtility.log("returning early because it saw no-store");
						return null; // return immediately, this is not cacheable or storable
					} else if((index=HeaderParser.parseToken(
							cacheControl,current,"public",false))!=current){
						publicCache=true;
					} else if((index=HeaderParser.parseToken(
							cacheControl,current,"no-transform",false))!=current){
						cc.noTransform=true;
					} else if((index=HeaderParser.parseToken(
							cacheControl,current,"must-revalidate",false))!=current){
						cc.mustRevalidate=true;
					} else if((index=HeaderParser.parseToken(
							cacheControl,current,"proxy-revalidate",false))!=current){
						proxyRevalidate=true;
					} else if((index=HeaderParser.parseTokenWithDelta(
							cacheControl,current,"max-age",intval))!=current){
						cc.maxAge=intval[0];
					} else if((index=HeaderParser.parseTokenWithDelta(
							cacheControl,current,"s-maxage",intval))!=current){
						sMaxAge=intval[0];
					} else {
						index=HeaderParser.skipDirective(cacheControl,current);
					}
				}
				if(!publicCache && !privateCache && !noCache){
					noCache=true;
				}
			} else {
				int code=headers.getResponseCode();
				if((code==200 || code==203 || code==300 || code==301 || code==410) &&
						headers.getHeaderField("authorization")==null){
					publicCache=true;
					privateCache=false;
				} else {
					noCache=true;
				}
			}
			if(headers.getResponseCode()==206) {
				noCache=true;
			}
			String pragma=headers.getHeaderField("pragma");
			if(pragma!=null && "no-cache".equals(pragma.toLowerCase(Locale.US))){
				noCache=true;
				//DebugUtility.log("returning early because it saw pragma no-cache");
				return null;
			}
			long now=new Date().getTime();
			cc.code=headers.getResponseCode();
			cc.date=now;
			cc.responseTime=now;
			cc.requestTime=requestTime;
			if(proxyRevalidate){
				// Enable must-revalidate for simplicity;
				// proxyRevalidate usually only applies to shared caches
				cc.mustRevalidate=true;
			}
			if(headers.getHeaderField("date")!=null){
				cc.date=headers.getHeaderFieldDate("date",Long.MIN_VALUE);
				if(cc.date==Long.MIN_VALUE) {
					noCache=true;
				}
			} else {
				noCache=true;
			}
			String expiresHeader=headers.getHeaderField("expires");
			if(expiresHeader!=null){
				expires=headers.getHeaderFieldDate("expires",Long.MIN_VALUE);
				hasExpires=(cc.date!=Long.MIN_VALUE);
			}
			if(headers.getHeaderField("age")!=null){
				try {
					cc.age=Integer.parseInt(headers.getHeaderField("age"));
					if(cc.age<0) {
						cc.age=0;
					}
				} catch(NumberFormatException e){
					cc.age=-1;
				}
			}
			if(cc.maxAge>0 || sMaxAge>0){
				long maxAge=cc.maxAge; // max age in seconds
				if(maxAge==0) {
					maxAge=sMaxAge;
				}
				if(cc.maxAge>0 && sMaxAge>0){
					maxAge=Math.max(cc.maxAge,sMaxAge);
				}
				cc.maxAge=maxAge*1000L; // max-age and s-maxage are in seconds
				hasExpires=false;
			} else if(hasExpires && !noCache){
				long maxAge=expires-cc.date;
				cc.maxAge=(maxAge>Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int)maxAge;
			} else if(noCache || cc.noStore){
				cc.maxAge=0;
			} else {
				cc.maxAge=24L*3600L*1000L;
			}
			String reqmethod=headers.getRequestMethod();
			if(reqmethod==null || (
					!reqmethod.toUpperCase(Locale.US).equals("GET")))
				// caching responses other than GET responses not supported
				return null;
			cc.requestMethod=reqmethod.toLowerCase(Locale.US);
			cc.cacheability=2;
			if(noCache) {
				cc.cacheability=0;
			} else if(privateCache) {
				cc.cacheability=1;
			}
			int i=0;
			cc.headers.add(headers.getHeaderField(null));
			while(true){
				String newValue=headers.getHeaderField(i);
				if(newValue==null) {
					break;
				}
				String key=headers.getHeaderFieldKey(i);
				i++;
				if(key==null){
					//DebugUtility.log("null key");
					continue;
				}
				key=key.toLowerCase(Locale.US);
				// to simplify matters, don't include Age header fields;
				// so-called hop-by-hop headers are also not included
				if(!"age".equals(key) &&
						!"connection".equals(key) &&
						!"keep-alive".equals(key) &&
						!"proxy-authenticate".equals(key) &&
						!"proxy-authorization".equals(key) &&
						!"te".equals(key) &&
						!"trailers".equals(key) &&
						!"transfer-encoding".equals(key) &&
						!"upgrade".equals(key)){
					cc.headers.add(key);
					cc.headers.add(newValue);
				}
			}
			//DebugUtility.log("final cc: %s",cc);
			return cc;
		}

		public static ICacheControl fromFile(File f) throws IOException{
			InputStream fs=new FileInputStream(f);
			try {
				return new CacheControlSerializer().readObjectFromStream(fs);
			} finally {
				if(fs!=null) {
					fs.close();
				}
			}
		}

		public static void toFile(ICacheControl o, File file) throws IOException{
			OutputStream fs=new FileOutputStream(file);
			try {
				new CacheControlSerializer().writeObjectToStream((CacheControl)o,fs);
			} finally {
				if(fs!=null) {
					fs.close();
				}
			}
		}

		private static class CacheControlSerializer implements IStreamObjectSerializer<CacheControl>{
			@Override
			public CacheControl readObjectFromStream(InputStream stream) throws IOException {
				try {
					JSONObject obj=new JSONObject(StreamUtility.streamToString(stream));
					CacheControl cc=new CacheControl();
					cc.cacheability=obj.getInt("cacheability");
					cc.noStore=obj.getBoolean("noStore");
					cc.noTransform=obj.getBoolean("noTransform");
					cc.mustRevalidate=obj.getBoolean("mustRevalidate");
					cc.requestTime=Long.parseLong(obj.getString("requestTime"));
					cc.responseTime=Long.parseLong(obj.getString("responseTime"));
					cc.maxAge=Long.parseLong(obj.getString("maxAge"));
					cc.date=Long.parseLong(obj.getString("date"));
					cc.code=obj.getInt("code");
					cc.age=Long.parseLong(obj.getString("age"));
					cc.uri=obj.getString("uri");
					cc.requestMethod=obj.getString("requestMethod");
					if(cc.requestMethod!=null) {
						cc.requestMethod=cc.requestMethod.toLowerCase(Locale.US);
					}
					cc.headers=new ArrayList<String>();
					JSONArray arr=obj.getJSONArray("headers");
					for(int i=0;i<arr.length();i++){
						String str=arr.getString(i);
						if(str!=null && i%2==1){
							str=str.toLowerCase(Locale.US);
							if("age".equals(str) ||
									"connection".equals(str) ||
									"keep-alive".equals(str) ||
									"proxy-authenticate".equals(str) ||
									"proxy-authorization".equals(str) ||
									"te".equals(str) ||
									"trailers".equals(str) ||
									"transfer-encoding".equals(str) ||
									"upgrade".equals(str)){
								// Skip "age" header field and
								// hop-by-hop header fields
								i++;
								continue;
							}
						}
						cc.headers.add(str);
					}
					return cc;
				} catch(ClassCastException e){
					e.printStackTrace();
					return null;
				} catch(NumberFormatException e){
					e.printStackTrace();
					return null;
				} catch (ParseException e) {
					e.printStackTrace();
					return null;
				}
			}
			@Override
			public void writeObjectToStream(CacheControl o, OutputStream stream)
					throws IOException {
				JSONObject obj=new JSONObject();
				obj.put("cacheability",o.cacheability);
				obj.put("noStore",o.noStore);
				obj.put("noTransform",o.noTransform);
				obj.put("mustRevalidate",o.mustRevalidate);
				obj.put("requestTime",Long.toString(o.requestTime));
				obj.put("responseTime",Long.toString(o.responseTime));
				obj.put("maxAge",Long.toString(o.maxAge));
				obj.put("date",Long.toString(o.date));
				obj.put("uri",o.uri);
				obj.put("requestMethod",o.requestMethod);
				obj.put("code",o.code);
				obj.put("age",Long.toString(o.age));
				JSONArray arr=new JSONArray();
				for(String header : o.headers){
					arr.put(header);
				}
				obj.put("headers",arr);
				StreamUtility.stringToStream(obj.toString(),stream);
			}
		}

		@Override
		public IHttpHeaders getHeaders(long length) {
			return new AgedHeaders(this,getAge(),length);
		}

		private static class AgedHeaders implements IHttpHeaders {

			CacheControl cc=null;
			long age=0;
			List<String> list=new ArrayList<String>();

			public AgedHeaders(CacheControl cc, long age, long length){
				list.add(cc.headers.get(0));
				for(int i=1;i<cc.headers.size();i+=2){
					String key=cc.headers.get(i);
					if(key!=null){
						key=key.toLowerCase(Locale.US);
						if("content-length".equals(key)||"age".equals(key)) {
							continue;
						}
					}
					list.add(cc.headers.get(i));
					list.add(cc.headers.get(i+1));
				}
				this.age=age/1000; // convert age to seconds
				list.add("age");
				list.add(Long.toString(this.age));
				list.add("content-length");
				list.add(Long.toString(length));
				//DebugUtility.log("aged=%s",list);
				this.cc=cc;
			}

			@Override
			public String getRequestMethod() {
				return cc.requestMethod;
			}
			@Override
			public String getHeaderField(String name) {
				if(name==null)return list.get(0);
				name=name.toLowerCase(Locale.US);
				for(int i=1;i<list.size();i+=2){
					String key=list.get(i);
					if(name.equals(key))
						return list.get(i+1);
				}
				return null;
			}
			@Override
			public String getHeaderField(int index) {
				index=(index)*2+1+1;
				if(index<0 || index>=list.size())
					return null;
				return list.get(index+1);
			}
			@Override
			public String getHeaderFieldKey(int index) {
				index=(index)*2+1;
				if(index<0 || index>=list.size())
					return null;
				return list.get(index);
			}
			@Override
			public int getResponseCode() {
				return cc.code;
			}
			@Override
			public long getHeaderFieldDate(String field, long defaultValue) {
				return HeaderParser.parseHttpDate(getHeaderField(field),defaultValue);
			}
			@Override
			public Map<String, List<String>> getHeaderFields() {
				Map<String, List<String>> map=new HashMap<String, List<String>>();
				map.put(null,Arrays.asList(new String[]{list.get(0)}));
				for(int i=1;i<list.size();i+=2){
					String key=list.get(i);
					List<String> templist=map.get(key);
					if(templist==null){
						templist=new ArrayList<String>();
						map.put(key,templist);
					}
					templist.add(list.get(i+1));
				}
				// Make lists unmodifiable
				for(String key : new ArrayList<String>(map.keySet())){
					map.put(key,Collections.unmodifiableList(map.get(key)));
				}
				return Collections.unmodifiableMap(map);
			}

			@Override
			public String getUrl() {
				return cc.uri;
			}
		}

		@Override
		public String getRequestMethod() {
			return requestMethod;
		}
		@Override
		public String getUri() {
			return uri;
		}
	}

	private static class HttpHeadersFromMap implements IHttpHeaders {

		Map<String,List<String>> map;
		List<String> list;
		String requestMethod;
		String urlString;

		public HttpHeadersFromMap(String urlString, String requestMethod, Map<String,List<String>> map){
			this.map=map;
			this.urlString=urlString;
			this.requestMethod=requestMethod;
			list=new ArrayList<String>();
			ArrayList<String> keyset=new ArrayList<String>();
			for(String s : this.map.keySet()){
				if(s==null){
					// Add status line (also has the side
					// effect that it will appear first in the list)
					List<String> v=this.map.get(s);
					if(v!=null && v.size()>0){
						list.add(v.get(0));
					} else {
						list.add("HTTP/1.1 200 OK");
					}
				} else {
					keyset.add(s);
				}
			}
			Collections.sort(keyset);
			// Add the remaining headers in sorted order
			for(String s : keyset){
				List<String> v=this.map.get(s);
				if(v!=null && v.size()>0){
					for(String ss : v){
						list.add(s);
						list.add(ss);
					}
				}
			}
		}

		@Override
		public String getRequestMethod() {
			return requestMethod;
		}

		@Override
		public String getHeaderField(String name) {
			if(name==null)return list.get(0);
			name=name.toLowerCase(Locale.US);
			for(int i=1;i<list.size();i+=2){
				String key=list.get(i);
				if(name.equals(key))
					return list.get(i+1);
			}
			return null;
		}
		@Override
		public String getHeaderField(int index) {
			index=(index)*2+1+1;
			if(index<0 || index>=list.size())
				return null;
			return list.get(index+1);
		}
		@Override
		public String getHeaderFieldKey(int index) {
			index=(index)*2+1;
			if(index<0 || index>=list.size())
				return null;
			return list.get(index);
		}
		@Override
		public int getResponseCode() {
			String status=getHeaderField(null);
			if(status==null)return -1;
			return HeaderParser.getResponseCode(status);
		}
		@Override
		public long getHeaderFieldDate(String field, long defaultValue) {
			return HeaderParser.parseHttpDate(getHeaderField(field),defaultValue);
		}

		@Override
		public Map<String, List<String>> getHeaderFields() {
			return Collections.unmodifiableMap(map);
		}

		@Override
		public String getUrl() {
			return urlString;
		}
	}

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
	private static class FileBasedHeaders implements IHttpHeaders {

		long date,length;
		String urlString;

		public FileBasedHeaders(String urlString, long length){
			date=new Date().getTime();
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
			if("date".equals(name.toLowerCase(Locale.US)))
				return HeaderParser.formatHttpDate(date);
			if("content-length".equals(name.toLowerCase(Locale.US)))
				return Long.toString(length);
			return null;
		}

		@Override
		public String getHeaderField(int name) {
			if(name==0)
				return getHeaderField("date");
			if(name==1)
				return getHeaderField("content-length");
			return null;
		}

		@Override
		public String getHeaderFieldKey(int name) {
			if(name==0)
				return "date";
			if(name==1)
				return "content-length";
			return null;
		}

		@Override
		public int getResponseCode() {
			return 200;
		}

		@Override
		public long getHeaderFieldDate(String field, long defaultValue) {
			if(field!=null && "date".equals(field.toLowerCase(Locale.US)))
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
			long threshold=oldest+Math.abs(oldest-new Date().getTime())/2;
			for(File file : files){
				if(file.lastModified()<threshold){
					if(file.isDirectory()){
						file.delete();
					} else {
						length-=file.length();
						file.delete();
						if(length<maximumSize)
							return;
					}
				}
			}
		}
	}

	private static class CacheResponseInfo {
		LegacyHttpCacheResponse cr=null;
		File trueCachedFile=null;
		File trueCacheInfoFile=null;
	}

	public static ResponseCache getLegacyResponseCache(File cachePath){
		return new LegacyHttpResponseCache(cachePath);
	}

	private static class LegacyHttpResponseCache extends ResponseCache {

		File cachePath;
		public LegacyHttpResponseCache(File cachePath){
			this.cachePath=cachePath;
		}

		@Override
		public CacheResponse get(URI arg0, String arg1,
				Map<String, List<String>> arg2) throws IOException {
			if(arg0==null || arg1==null || arg2==null)
				throw new IllegalArgumentException();
			CacheResponseInfo crinfo=getCachedResponse(arg0.toString(),cachePath,true);
			return crinfo.cr;
		}

		@Override
		public CacheRequest put(URI uri, final URLConnection connection)
				throws IOException {
			if(uri==null || connection==null)throw new IllegalArgumentException();
			if(cachePath==null)return null;
			boolean isPrivate=(cachePath==null) ? false : cachePath.toString().startsWith("/data/");
			final ICacheControl cc=CacheControl.getCacheControl(
					new HttpHeaders(connection),new Date().getTime());
			//DebugUtility.log("CacheRequest put %s -> %s",uri.toString(),
			//	connection.getURL().toString());
			final CacheResponseInfo crinfo=getCachedResponse(
					connection.getURL().toString(),cachePath,false);
			if(cc!=null && (cc.getCacheability()==2 || (isPrivate && cc.getCacheability()==1)) &&
					!cc.isNoTransform() && !cc.isNoStore())
				return new CacheRequest(){
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
			};
			return null;
		}

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

	private static CacheResponseInfo getCachedResponse(
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
				cacheFiles=pathForCache.listFiles(new FilenameFilter(){
					@Override
					public boolean accept(File dir, String filename) {
						return filename.startsWith(cacheFileName+"-") &&
								!filename.endsWith(".cache");
					}
				});
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
							ICacheControl cc=CacheControl.fromFile(cacheInfoFile);
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
						long timeDiff=Math.abs(cacheFile.lastModified()-(new Date().getTime()));
						fresh=(timeDiff<=maxAgeMillis);
						headers=new FileBasedHeaders(urlString,cacheFile.length());
					}
					DebugUtility.log("fresh=%s",fresh);
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
							stream=new BufferedInputStream(new FileInputStream(cacheFile),8192);
							crinfo.cr=new LegacyHttpCacheResponse(stream,
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
						trueCachedFile=new File(pathForCache,String.format("%s-%d",cacheFileName,i));
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

	public static <T> T downloadUrl(
			String urlString,
			final IResponseListener<T> callback
			) throws IOException{
		if(urlString==null)throw new NullPointerException();
		final boolean isEventHandler=(callback!=null && callback instanceof IDownloadEventListener);
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
		HttpURLConnection connection=null;
		try {
			url=new URL(urlString);
			if(isEventHandler && callback!=null && !calledConnecting){
				((IDownloadEventListener<T>)callback).onConnecting(urlString);
				calledConnecting=true;
			}
			connection = (HttpURLConnection)url.openConnection();
			connection.setUseCaches(true);
			connection.setDoInput(true);
			connection.setReadTimeout(10000);
			connection.setConnectTimeout(20000);
			connection.setRequestMethod(requestMethod);
			connection.connect();
			stream = new BufferedInputStream(connection.getInputStream(),8192);
			if(isEventHandler && callback!=null) {
				((IDownloadEventListener<T>)callback).onConnected(urlString);
			}
			//DebugUtility.log(connection.getHeaderFields());
			T ret=(callback==null) ? null : callback.processResponse(urlString,stream,
					new HttpHeaders(connection));
			stream.close();
			return ret;
		} finally {
			if(stream!=null){
				try {
					stream.close();
				} catch (IOException e) {}
			}
			if(connection!=null){
				connection.disconnect();
			}
		}
	}

}
