package com.upokecenter.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import com.upokecenter.encoding.IEncodingError;
import com.upokecenter.encoding.ITextDecoder;
import com.upokecenter.encoding.ITextEncoder;
import com.upokecenter.encoding.TextEncoding;

public final class URL {

	private String scheme="";
	private String schemeData="";
	private String username="";
	private String password=null;
	private String host=null;
	private String path="";
	private String query=null;
	private String fragment=null;
	private String port="";

	public String getScheme(){
		return scheme;
	}

	public String getSchemeData(){
		return schemeData;
	}

	public String getPath(){
		return path;
	}

	public String getPort(){
		return port;
	}

	public String getProtocol(){
		return scheme + ":";
	}

	public String getUsername(){
		return username==null ? "" : username;
	}

	public String getPassword(){
		return password==null ? "" : password;
	}

	public String getQueryString(){
		return query==null ? "" : query;
	}

	public String getFragment(){
		return fragment==null ? "" : fragment;
	}

	public String getHash(){
		return (fragment==null || fragment.length()==0) ? "" : "#" + fragment;
	}

	public String getSearch(){
		return (query==null || query.length()==0) ? "" : "?" + query;
	}

	public String getHost(){
		if(port.length()==0)
			return hostSerialize(host);
		return hostSerialize(host) + ":" + port;
	}

	public String getHostname(){
		return hostSerialize(host);
	}

	public String getPathname(){
		if(schemeData.length()>0)
			return schemeData;
		else
			return path;
	}

	private enum ParseState {
		SchemeStart,
		Scheme,
		SchemeData,
		NoScheme,
		RelativeOrAuthority,
		Relative,
		RelativeSlash,
		AuthorityFirstSlash,
		AuthoritySecondSlash,
		AuthorityIgnoreSlashes,
		Authority, Query, Fragment, Host, FileHost,
		RelativePathStart, RelativePath, HostName, Port
	}

	private static String hex="0123456789ABCDEF";

	private static IEncodingError encodingError=new IEncodingError(){

		@Override
		public int emitDecoderError(int[] buffer, int offset, int length)
				throws IOException {
			return 0;
		}

		@Override
		public void emitEncoderError(OutputStream stream, int codePoint) throws IOException {
			stream.write('?');
		}

	};

	private static IEncodingError querySerializerError=new IEncodingError(){

		@Override
		public int emitDecoderError(int[] buffer, int offset, int length)
				throws IOException {
			return 0;
		}

		@Override
		public void emitEncoderError(OutputStream stream, int codePoint) throws IOException {
			stream.write(0x26);
			stream.write(0x23);
			if(codePoint<0 || codePoint>=0x110000) {
				codePoint=0xFFFD;
			}
			if(codePoint==0){
				stream.write('0');
				stream.write(0x3B);
				return;
			}
			byte[] data=new byte[8];
			int count=data.length;
			while(codePoint>0){
				count--;
				data[count]=(byte)('0'+(codePoint%10));
				codePoint/=10;
			}
			stream.write(data,count,data.length-count);
			stream.write(0x3B);
		}

	};

	private static void percentEncode(IntList buffer, int b){
		buffer.append('%');
		buffer.append(hex.charAt((b>>4)&0x0F));
		buffer.append(hex.charAt((b)&0x0F));
	}

	private static void percentEncodeUtf8(IntList buffer, int cp){
		if(cp<=0x7F){
			buffer.append('%');
			buffer.append(hex.charAt((cp>>4)&0x0F));
			buffer.append(hex.charAt((cp)&0x0F));
		} else if(cp<=0x7FF){
			percentEncode(buffer,(0xC0|((cp>>6)&0x1F)));
			percentEncode(buffer,(0x80|(cp   &0x3F)));
		} else if(cp<=0xFFFF){
			percentEncode(buffer,(0xE0|((cp>>12)&0x0F)));
			percentEncode(buffer,(0x80|((cp>>6 )&0x3F)));
			percentEncode(buffer,(0x80|(cp      &0x3F)));
		} else {
			percentEncode(buffer,(0xF0|((cp>>18)&0x07)));
			percentEncode(buffer,(0x80|((cp>>12)&0x3F)));
			percentEncode(buffer,(0x80|((cp>>6 )&0x3F)));
			percentEncode(buffer,(0x80|(cp      &0x3F)));
		}
	}

	@Override
	public String toString(){
		StringBuilder builder=new StringBuilder();
		builder.append(scheme);
		builder.append(':');
		if(scheme.equals("file") ||
				scheme.equals("http") ||
				scheme.equals("https") ||
				scheme.equals("ftp") ||
				scheme.equals("gopher") ||
				scheme.equals("ws") ||
				scheme.equals("wss")){
			// NOTE: We check relative schemes here
			// rather than have a relative flag,
			// as specified in the URL Standard
			// (since the protocol can't be changed
			// as this class is immutable, we can
			// do this variation).
			builder.append("//");
			if(username.length()!=0 || password!=null){
				builder.append(username);
				if(password!=null){
					builder.append(':');
					builder.append(password);
				}
				builder.append('@');
			}
			builder.append(hostSerialize(host));
			if(port.length()>0){
				builder.append(':');
				builder.append(port);
			}
			builder.append(path);
		} else {
			builder.append(schemeData);
		}
		if(query!=null){
			builder.append('?');
			builder.append(query);
		}
		if(fragment!=null){
			builder.append('#');
			builder.append(fragment);
		}
		return builder.toString();
	}

	private static void appendOutputBytes(StringBuilder output,
			MemoryOutputStream baos){
		for(int i=0;i<baos.length();i++){
			int c=baos.get(i);
			if(c==0x20) {
				output.append((char)0x2b);
			} else if(c==0x2a || c==0x2d || c==0x2e ||
					(c>=0x30 && c<=0x39) ||
					(c>=0x41 && c<=0x5a) ||
					(c>=0x5f) || (c>=0x61 && c<=0x7a)){
				output.append((char)c);
			} else {
				output.append('%');
				output.append(hex.charAt((c>>4)&0x0F));
				output.append(hex.charAt((c)&0x0F));
			}
		}
	}

	private static void percentDecode(String str, OutputStream output) throws IOException{
		int len=str.length();
		for(int i=0;i<len;i++){
			int c=str.charAt(i);
			if(c=='%'){
				if(i+2<len){
					int a=toHexNumber(str.charAt(i+1));
					int b=toHexNumber(str.charAt(i+2));
					if(a>=0 && b>=0){
						output.write(a*16+b);
						i+=2;
						continue;
					}
				}
			}
			output.write(c&0xFF);
		}
	}

	public static String toQueryString(List<String[]> pairs,
			String delimiter, String encoding){
		if(encoding==null) {
			encoding="utf-8";
		}
		ITextEncoder encoder=TextEncoding.getEncoder(encoding);
		if(encoder==null)
			throw new IllegalArgumentException();
		StringBuilder output=new StringBuilder();
		boolean first=true;
		MemoryOutputStream baos=new MemoryOutputStream();
		try {
			for(String[] pair : pairs){
				if(!first){
					output.append(delimiter==null ? "&" : delimiter);
				}
				first=false;
				if(pair==null || pair.length<2)
					throw new IllegalArgumentException();
				baos.reset();
				TextEncoding.encodeString(pair[0], baos, encoder, querySerializerError);
				appendOutputBytes(output,baos);
				output.append('=');
				baos.reset();
				TextEncoding.encodeString(pair[1], baos, encoder, querySerializerError);
				appendOutputBytes(output,baos);
			}
		} catch(IOException e){
			throw new RuntimeException(e);
		}
		return output.toString();
	}

	public static List<String[]> parseQueryString(
			String input, String delimiter, String encoding, boolean useCharset, boolean isindex){
		if(input==null)
			throw new IllegalArgumentException();
		if(delimiter==null) {
			delimiter="&";
		}
		if(encoding==null) {
			encoding="utf-8";
		}
		for(int i=0;i<input.length();i++){
			if(input.charAt(i)>0x7F)
				throw new IllegalArgumentException();
		}
		String[] strings=input.split("&");
		List<String[]> pairs=new ArrayList<String[]>();
		for(String str : strings){
			if(str.length()==0) {
				continue;
			}
			int index=str.indexOf('=');
			String name=str;
			String value="";
			if(index>=0){
				name=str.substring(0,index);
				value=str.substring(index+1);
			}
			name=name.replace('+',' ');
			value=value.replace('+',' ');
			if(useCharset && "_charset_".equals(name)){
				String ch=TextEncoding.resolveEncoding(value);
				if(ch!=null){
					useCharset=false;
					encoding=ch;
				}
			}
			pairs.add(new String[]{name,value});
		}
		MemoryOutputStream mos=new MemoryOutputStream();
		ITextDecoder decoder=TextEncoding.getDecoder(encoding);
		try {
			for(String[] pair : pairs){
				mos.reset();
				percentDecode(pair[0],mos);
				pair[0]=TextEncoding.decodeString(mos.toInputStream(),
						decoder, TextEncoding.ENCODING_ERROR_REPLACE);
				mos.reset();
				percentDecode(pair[1],mos);
				pair[1]=TextEncoding.decodeString(mos.toInputStream(),
						decoder, TextEncoding.ENCODING_ERROR_REPLACE);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return pairs;
	}

	public static List<String> pathList(String s){
		List<String> str=new ArrayList<String>();
		if(s==null || s.length()==0)
			return str;
		if(s.charAt(0)!='/')
			throw new IllegalArgumentException();
		int i=1;
		while(i<=s.length()){
			int io=s.indexOf('/',i);
			if(io>=0){
				str.add(s.substring(i,io));
				i=io+1;
			} else {
				str.add(s.substring(i));
				break;
			}
		}
		return str;
	}

	public static URL parse(String s){
		return parse(s,null,null, false);
	}
	public static URL parse(String s, URL baseurl){
		return parse(s,baseurl,null, false);
	}
	public static URL parse(String s, URL baseurl, String encoding){
		return parse(s, baseurl, encoding, false);
	}

	public static URL parse(String s, URL baseurl, String encoding, boolean strict){
		if(s==null)
			throw new IllegalArgumentException();
		int beginning=0;
		int ending=s.length()-1;
		boolean relative=false;
		URL url=new URL();
		ITextEncoder encoder=null;
		ParseState state=ParseState.SchemeStart;
		if(encoding!=null){
			encoder=TextEncoding.getEncoder(encoding);
		}
		if(s.indexOf("http://")==0){
			state=ParseState.AuthorityIgnoreSlashes;
			url.scheme="http";
			beginning=7;
			relative=true;
		} else {
			while(beginning<s.length()){
				char c=s.charAt(beginning);
				if(c!=0x09 && c!=0x0a && c!=0x0c && c!=0x0d && c!=0x20){
					break;
				}
				beginning++;
			}
		}
		while(ending>=beginning){
			char c=s.charAt(ending);
			if(c!=0x09 && c!=0x0a && c!=0x0c && c!=0x0d && c!=0x20){
				ending++;
				break;
			}
			ending--;
		}
		if(ending<beginning) {
			ending=beginning;
		}
		boolean atflag=false;
		boolean bracketflag=false;
		IntList buffer=new IntList();
		IntList query=null;
		IntList fragment=null;
		IntList password=null;
		IntList username=null;
		IntList schemeData=null;
		boolean error=false;
		List<String> path=new ArrayList<String>();
		int index=beginning;
		int hostStart=-1;
		int portstate=0;
		while(index<=ending){
			int oldindex=index;
			int c=-1;
			if(index>=ending){
				c=-1;
				index++;
			} else {
				c=s.charAt(index);
				if(c>=0xD800 && c<=0xDBFF && index+1<ending &&
						s.charAt(index+1)>=0xDC00 && s.charAt(index+1)<=0xDFFF){
					// Get the Unicode code point for the surrogate pair
					c=0x10000+(c-0xD800)*0x400+(s.charAt(index+1)-0xDC00);
					index++;
				} else if(c>=0xD800 && c<=0xDFFF)
					// illegal surrogate
					throw new IllegalArgumentException();
				index++;
			}
			switch(state){
			case SchemeStart:
				if(c>='A' && c<='Z'){
					buffer.append(c+0x20);
					state=ParseState.Scheme;
				} else if(c>='a' && c<='z'){
					buffer.append(c);
					state=ParseState.Scheme;
				} else {
					index=oldindex;
					state=ParseState.NoScheme;
				}
				break;
			case Scheme:
				if(c>='A' && c<='Z'){
					buffer.append(c+0x20);
				} else if((c>='a' && c<='z') || c=='.' || c=='=' || c=='+'){
					buffer.append(c);
				} else if(c==':'){
					url.scheme=buffer.toString();
					buffer.clear();
					if(url.scheme.equals("http") ||
							url.scheme.equals("https") ||
							url.scheme.equals("ftp") ||
							url.scheme.equals("gopher") ||
							url.scheme.equals("ws") ||
							url.scheme.equals("wss") ||
							url.scheme.equals("file")){
						relative=true;
					}
					if(url.scheme.equals("file")){
						state=ParseState.Relative;
						relative=true;
					} else if(relative && baseurl!=null && url.scheme.equals(baseurl.scheme)){
						state=ParseState.RelativeOrAuthority;
					} else if(relative){
						state=ParseState.AuthorityFirstSlash;
					} else {
						schemeData=new IntList();
						state=ParseState.SchemeData;
					}
				} else {
					buffer.clear();
					index=beginning;
					state=ParseState.NoScheme;
				}
				break;
			case SchemeData:
				if(c=='?'){
					query=new IntList();
					state=ParseState.Query;
					break;
				} else if(c=='#'){
					fragment=new IntList();
					state=ParseState.Fragment;
					break;
				}
				if(!isUrlCodePoint(c) || (c=='%' &&
						(index+2>ending ||
								!isHexDigit(s.charAt(index)) ||
								!isHexDigit(s.charAt(index+1))))){
					error=true;
				}
				if(c>=0 && c!=0x09 && c!=0x0a && c!=0x0d){
					if(c<0x20 || c==0x7F){
						percentEncode(schemeData,c);
					} else if(c<0x7F){
						schemeData.append(c);
					} else {
						percentEncodeUtf8(schemeData,c);
					}
				}
				break;
			case NoScheme:
				if(baseurl==null)
					return null;
				//DebugUtility.log("no scheme: [%s] [%s]",s,baseurl);
				if(!(baseurl.scheme.equals("http") ||
						baseurl.scheme.equals("https") ||
						baseurl.scheme.equals("ftp") ||
						baseurl.scheme.equals("gopher") ||
						baseurl.scheme.equals("ws") ||
						baseurl.scheme.equals("wss") ||
						baseurl.scheme.equals("file")
						))
					return null;
				state=ParseState.Relative;
				index=oldindex;
				break;
			case RelativeOrAuthority:
				if(c=='/' && index<ending && s.charAt(index)=='/'){
					index++;
					state=ParseState.AuthorityIgnoreSlashes;
				} else {
					error=true;
					state=ParseState.Relative;
					index=oldindex;
				}
				break;
			case Relative:{
				relative=true;
				if(baseurl==null) {
					baseurl=url;
				}
				url.scheme=baseurl.scheme;
				if(c<0){
					url.host=baseurl.host;
					url.port=baseurl.port;
					path=pathList(baseurl.path);
					url.query=baseurl.query;
				} else if(c=='/' || c=='\\'){
					if(c=='\\') {
						error=true;
					}
					state=ParseState.RelativeSlash;
				} else if(c=='?'){
					url.host=baseurl.host;
					url.port=baseurl.port;
					path=pathList(baseurl.path);
					query=new IntList();
					state=ParseState.Query;
				} else if(c=='#'){
					url.host=baseurl.host;
					url.port=baseurl.port;
					path=pathList(baseurl.path);
					url.query=baseurl.query;
					fragment=new IntList();
					state=ParseState.Fragment;
				} else {
					url.host=baseurl.host;
					url.port=baseurl.port;
					path=pathList(baseurl.path);
					DebugUtility.log(path);
					if(path.size()>0) { // Pop path
						path.remove(path.size()-1);
					}
					DebugUtility.log(path);
					state=ParseState.RelativePath;
					index=oldindex;
				}
				break;
			}
			case RelativeSlash:
				if(c=='/' || c=='\\'){
					if(c=='\\') {
						error=true;
					}
					if("file".equals(url.scheme)){
						state=ParseState.FileHost;
					} else {
						state=ParseState.AuthorityIgnoreSlashes;
					}
				} else {
					url.host=baseurl.host;
					url.port=baseurl.port;
					state=ParseState.RelativePath;
					index=oldindex;
				}
				break;
			case AuthorityFirstSlash:
				if(c=='/'){
					state=ParseState.AuthoritySecondSlash;
				} else {
					error=true;
					state=ParseState.AuthorityIgnoreSlashes;
					index=oldindex;
				}
				break;
			case AuthoritySecondSlash:
				if(c=='/'){
					state=ParseState.AuthorityIgnoreSlashes;
				} else {
					error=true;
					state=ParseState.AuthorityIgnoreSlashes;
					index=oldindex;
				}
				break;
			case AuthorityIgnoreSlashes:
				if(c!='/' && c!='\\'){
					username=new IntList();
					index=oldindex;
					hostStart=index;
					state=ParseState.Authority;
				} else {
					error=true;
				}
				break;
			case Authority:
				if(c=='@'){
					if(atflag){
						IntList result=(password==null) ? username : password;
						error=true;
						result.append('%');
						result.append('4');
						result.append('0');
					}
					atflag=true;
					int[] array=buffer.array();
					for(int i=0;i<buffer.size();i++){
						int cp=array[i];
						if(cp==0x9 || cp==0xa || cp==0xd){
							error=true;
							continue;
						}
						if(!isUrlCodePoint(cp) || (cp=='%' &&
								(i+3>buffer.size() ||
										!isHexDigit(array[index+1]) ||
										!isHexDigit(array[index+2])))){
							error=true;
						}
						if(cp==':' && password==null){
							password=new IntList();
							continue;
						}
						IntList result=(password==null) ? username : password;
						if(cp<=0x20 || cp>=0x7F || "#<>?`\"".indexOf((char)cp)>=0){
							percentEncodeUtf8(result,cp);
						} else {
							result.append(cp);
						}
					}
					buffer.clear();
					hostStart=index;
				} else if(c<0 || "/\\?#".indexOf(c)>=0){
					buffer.clear();
					state=ParseState.Host;
					index=hostStart;
				} else {
					buffer.append(c);
				}
				break;
			case FileHost:
				if(c<0 || "/\\?#".indexOf(c)>=0){
					index=oldindex;
					if(buffer.size()==2){
						int c1=buffer.get(0);
						int c2=buffer.get(1);
						if((c2=='|' || c2==':') && ((c1>='A' && c1<='Z') || (c1>='a' && c1<='z'))){
							state=ParseState.RelativePath;
							break;
						}
					}
					String host=hostParse(buffer.toString());
					if(host==null)
						throw new IllegalArgumentException();
					url.host=host;
					buffer.clear();
					state=ParseState.RelativePathStart;
				} else if(c==0x09 || c==0x0a || c==0x0d){
					error=true;
				} else {
					buffer.append(c);
				}
				break;
			case Host:
			case HostName:
				if(c==':' && !bracketflag){
					String host=hostParse(buffer.toString());
					if(host==null)
						throw new IllegalArgumentException();
					url.host=host;
					buffer.clear();
					state=ParseState.Port;
				} else if(c<0 || "/\\?#".indexOf(c)>=0){
					String host=hostParse(buffer.toString());
					if(host==null)
						throw new IllegalArgumentException();
					url.host=host;
					buffer.clear();
					index=oldindex;
					state=ParseState.RelativePathStart;
				} else if(c==0x09 || c==0x0a || c==0x0d){
					error=true;
				} else {
					if(c=='[') {
						bracketflag=true;
					} else if(c==']') {
						bracketflag=false;
					}
					buffer.append(c);
				}
				break;
			case Port:
				if(c>='0' && c<='9'){
					if(c!='0') {
						portstate=2; // first non-zero found
					} else if(portstate==0){
						portstate=1; // have a port number
					}
					if(portstate==2) {
						buffer.append(c);
					}
				} else if(c<0 || "/\\?#".indexOf(c)>=0){
					String bufport="";
					if(portstate==1) {
						bufport="0";
					} else if(portstate==2) {
						bufport=buffer.toString();
					}
					//DebugUtility.log("port: [%s]",buffer.toString());
					if((url.scheme.equals("http") || url.scheme.equals("ws"))
							&& bufport.equals("80")) {
						bufport="";
					}
					if((url.scheme.equals("https") || url.scheme.equals("wss"))
							&& bufport.equals("443")) {
						bufport="";
					}
					if((url.scheme.equals("gopher"))
							&& bufport.equals("70")) {
						bufport="";
					}
					if((url.scheme.equals("ftp"))
							&& bufport.equals("21")) {
						bufport="";
					}
					url.port=bufport;
					buffer.clear();
					state=ParseState.RelativePathStart;
					index=oldindex;
				} else if(c==0x09 || c==0x0a || c==0x0d){
					error=true;
				} else
					return null;
				break;
			case Query:
				if(c<0 || c=='#'){
					boolean utf8=true;
					if(relative){
						utf8=true;
					}
					if(utf8 || encoder==null){
						// NOTE: Encoder errors can never happen in
						// this case
						for(int i=0;i<buffer.size();i++){
							int ch=buffer.get(i);
							if(ch<0x21 || ch>0x7e || ch==0x22 || ch==0x23 ||
									ch==0x3c || ch==0x3e || ch==0x60){
								percentEncodeUtf8(query,ch);
							} else {
								query.append(ch);
							}
						}
					} else {
						try {
							ByteArrayOutputStream baos=new ByteArrayOutputStream();
							encoder.encode(baos,buffer.array(),0,buffer.size(),encodingError);
							byte[] bytes=baos.toByteArray();
							for (byte ch : bytes) {
								if(ch<0x21 || ch>0x7e || ch==0x22 || ch==0x23 ||
										ch==0x3c || ch==0x3e || ch==0x60){
									percentEncode(query,ch);
								} else {
									query.append(ch);
								}
							}
							baos.close();
						} catch (IOException e) {
							throw new RuntimeException(e);
						}
						throw new IllegalStateException();
					}
					buffer.clear();
					if(c=='#'){
						fragment=new IntList();
						state=ParseState.Fragment;
					}
				} else if(c==0x09 || c==0x0a || c==0x0d){
					error=true;
				} else {
					if(!isUrlCodePoint(c) || (c=='%' &&
							(index+2>ending ||
									!isHexDigit(s.charAt(index)) ||
									!isHexDigit(s.charAt(index+1))))){
						error=true;
					}
					buffer.append(c);
				}
				break;
			case RelativePathStart:
				if(c=='\\'){
					error=true;
				}
				state=ParseState.RelativePath;
				if((c!='/' && c!='\\')){
					index=oldindex;
				}
				break;
			case RelativePath:
				if((c<0 || c=='/' || c=='\\') ||
						(c=='?' || c=='#')){
					if(c=='\\') {
						error=true;
					}
					if(buffer.size()==2 && buffer.get(0)=='.'
							&& buffer.get(1)=='.'){
						if(path.size()>0){
							path.remove(path.size()-1);
						}
						if((c!='/' && c!='\\')){
							path.add("");
						}
					} else if(buffer.size()==1 && buffer.get(0)=='.'){
						if((c!='/' && c!='\\')){
							path.add("");
						}
					} else {
						if("file".equals(url.scheme) && path.size()==0 &&
								buffer.size()==2){
							int c1=buffer.get(0);
							int c2=buffer.get(1);
							if((c2=='|' || c2==':') && ((c1>='A' && c1<='Z') || (c1>='a' && c1<='z'))){
								buffer.set(1,':');
							}
						}
						path.add(buffer.toString());
					}
					buffer.clear();
					if(c=='?'){
						query=new IntList();
						state=ParseState.Query;
					}
					if(c=='#'){
						fragment=new IntList();
						state=ParseState.Fragment;
					}
				} else if(c=='%' && index+2<=ending &&
						s.charAt(index)=='2' &&
						(s.charAt(index+1)=='e' || s.charAt(index+1)=='E')){
					index+=2;
					buffer.append('.');
				} else if(c==0x09 || c==0x0a || c==0x0d){
					error=true;
				} else {
					if(!isUrlCodePoint(c) || (c=='%' &&
							(index+2>ending ||
									!isHexDigit(s.charAt(index)) ||
									!isHexDigit(s.charAt(index+1))))){
						error=true;
					}
					if(c<=0x20 || c>=0x7F || "#<>?`\"".indexOf((char)c)>=0){
						percentEncodeUtf8(buffer,c);
					} else {
						buffer.append(c);
					}
				}
				break;
			case Fragment:
				if(c<0) {
					break;
				}
				if(c==0x09 || c==0x0a || c==0x0d) {
					error=true;
				} else {
					if(!isUrlCodePoint(c) || (c=='%' &&
							(index+2>ending ||
									!isHexDigit(s.charAt(index)) ||
									!isHexDigit(s.charAt(index+1))))){
						error=true;
					}
					if(c<0x20 || c==0x7F){
						percentEncode(fragment,c);
					} else if(c<0x7F){
						fragment.append(c);
					} else {
						percentEncodeUtf8(fragment,c);
					}
				}
				break;
			default:
				throw new IllegalStateException();
			}
		}
		if(error && strict)
			return null;
		if(schemeData!=null) {
			url.schemeData=schemeData.toString();
		}
		StringBuilder builder=new StringBuilder();
		if(path.size()==0){
			builder.append('/');
		} else {
			for(String segment : path){
				builder.append('/');
				builder.append(segment);
			}
		}
		url.path=builder.toString();
		if(query!=null) {
			url.query=query.toString();
		}
		if(fragment!=null) {
			url.fragment=fragment.toString();
		}
		if(password!=null) {
			url.password=password.toString();
		}
		if(username!=null) {
			url.username=username.toString();
		}
		return url;
	}

	private static String hostSerialize(String string) {
		if(string==null)return "";
		return string;
	}
	private static String hostParse(String string) {
		if(string.length()>0 && string.charAt(0)=='['){
			if(string.charAt(string.length()-1)!=']'){
				int[] ipv6=new int[8];
				int piecePointer=0;
				int index=1;
				int compress=-1;
				int ending=string.length()-1;
				int c=(index>=ending) ? -1 : string.charAt(index);
				if(c==':'){
					if(index+1>=ending || string.charAt(index+1)!=':')
						return null;
					index+=2;
					piecePointer++;
					compress=piecePointer;
				}
				while(index<ending){
					if(piecePointer>=8)return null;
					c=string.charAt(index);
					if(c>=0xD800 && c<=0xDBFF && index+1<ending &&
							string.charAt(index+1)>=0xDC00 && string.charAt(index+1)<=0xDFFF){
						// Get the Unicode code point for the surrogate pair
						c=0x10000+(c-0xD800)*0x400+(string.charAt(index+1)-0xDC00);
						index++;
					} else if(c>=0xD800 && c<=0xDFFF)
						// illegal surrogate
						throw new IllegalArgumentException();
					index++;
					if(c==':'){
						if(compress>=0)return null;
						piecePointer++;
						compress=piecePointer;
						continue;
					}
					int value=0;
					int length=0;
					while(length<4){
						if(c>='A' && c<='Z'){
							value=value*16+(c-'A')+10;
							index++;
							length++;
							c=(index>=ending) ? -1 : string.charAt(index);
						} else if(c>='a' && c<='z'){
							value=value*16+(c-'a')+10;
							index++;
							length++;
							c=(index>=ending) ? -1 : string.charAt(index);
						} else if(c>='0' && c<='9'){
							value=value*16+(c-'0');
							index++;
							length++;
							c=(index>=ending) ? -1 : string.charAt(index);
						} else {
							break;
						}
					}
					if(c=='.'){
						if(length==0)return null;
						index-=length;
						break;
					} else if(c==':'){
						index++;
						c=(index>=ending) ? -1 : string.charAt(index);
						if(c<0)return null;
					} else if(c>=0)
						return null;
					ipv6[piecePointer]=value;
					piecePointer++;
				}
				// IPv4
				if(c>=0){
					if(piecePointer>6)
						return null;
					int dotsSeen=0;
					while(index<ending){
						int value=0;
						while(c>='0' && c<='9'){
							value=value*10+(c-'0');
							if(value>255)return null;
							index++;
							c=(index>=ending) ? -1 : string.charAt(index);
						}
						if(dotsSeen<3 && c!='.')
							return null;
						else if(dotsSeen==3 && c=='.')
							return null;
						ipv6[piecePointer]=ipv6[piecePointer]*256+value;
						if(dotsSeen==0 || dotsSeen==2){
							piecePointer++;
						}
						dotsSeen++;
					}
				}
				if(compress>=0){
					int swaps=piecePointer-compress;
					piecePointer=7;
					while(piecePointer!=0 && swaps!=0){
						int ptr=compress-swaps+1;
						int tmp=ipv6[piecePointer];
						ipv6[piecePointer]=ipv6[ptr];
						ipv6[ptr]=tmp;
						piecePointer--;
						swaps--;
					}
				} else if(compress<0 && piecePointer!=8)
					return null;
			}
		}
		MemoryOutputStream mos=new MemoryOutputStream();
		try {
			percentDecode(string,mos);
			string=TextEncoding.decodeString(mos.toInputStream(),
					TextEncoding.getDecoder("utf-8"),
					TextEncoding.ENCODING_ERROR_REPLACE);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return string;
	}
	private static int toHexNumber(int c) {
		if(c>='A' && c<='Z')
			return 10+c-'A';
		else if(c>='a' && c<='z')
			return 10+c-'a';
		else if (c>='0' && c<='9')
			return c-'0';
		return -1;
	}

	private static boolean isHexDigit(int c) {
		return (c>='A' && c<='Z') || (c>='a' && c<='z') || (c>='0' && c<='9');
	}

	private static boolean isUrlCodePoint(int c) {
		if(c<=0x20)return false;
		if(c<0x80)
			return((c>='a' && c<='z') ||
					(c>='A' && c<='Z') ||
					(c>='0' && c<='9') ||
					"!$&'()*+,-./:;=?@_~".indexOf(c)>=0);
		else if((c&0xFFFE)==0xFFFE)
			return false;
		else if((c>=0xa0 && c<=0xd7ff) ||
				(c>=0xe000 && c<=0xfdcf) ||
				(c>=0xfdf0 && c<=0xffef) ||
				(c>=0x10000 && c<=0x10fffd))
			return true;
		return false;
	}

}
