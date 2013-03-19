package com.upokecenter.net;

import com.upokecenter.util.DateTimeImpl;
import com.upokecenter.util.StringUtility;



public final class HeaderParser {

	private HeaderParser(){}
	public static String formatHttpDate(long date){
		int[] components=DateTimeImpl.getDateComponents(date);
		int dow=components[7]; // 1 to 7
		int month=components[1]; // 1 to 12
		String dayofweek=null;
		if(dow==1) {
			dayofweek="Sun, ";
		}
		else if(dow==2) {
			dayofweek="Mon, ";
		}
		else if(dow==3) {
			dayofweek="Tue, ";
		}
		else if(dow==4) {
			dayofweek="Wed, ";
		}
		else if(dow==5) {
			dayofweek="Thu, ";
		}
		else if(dow==6) {
			dayofweek="Fri, ";
		}
		else if(dow==7) {
			dayofweek="Sat, ";
		}
		if(dayofweek==null)return "";
		String[] months={
				""," Jan "," Feb "," Mar "," Apr ",
				" May "," Jun "," Jul "," Aug ",
				" Sep "," Oct "," Nov "," Dec "
		};
		if(month<1||month>12)return "";
		String monthstr=months[month];
		StringBuilder builder=new StringBuilder();
		builder.append(dayofweek);
		builder.append((char)('0'+((components[2]/10)%10)));
		builder.append((char)('0'+((components[2])%10)));
		builder.append(monthstr);
		builder.append((char)('0'+((components[0]/1000)%10)));
		builder.append((char)('0'+((components[0]/100)%10)));
		builder.append((char)('0'+((components[0]/10)%10)));
		builder.append((char)('0'+((components[0])%10)));
		builder.append(' ');
		builder.append((char)('0'+((components[3]/10)%10)));
		builder.append((char)('0'+((components[3])%10)));
		builder.append(':');
		builder.append((char)('0'+((components[4]/10)%10)));
		builder.append((char)('0'+((components[4])%10)));
		builder.append(':');
		builder.append((char)('0'+((components[5]/10)%10)));
		builder.append((char)('0'+((components[5])%10)));
		builder.append(" GMT");
		return builder.toString();
	}

	private static int parseMonth(String v, int index){
		if(v.startsWith("Jan",index))return 1;
		if(v.startsWith("Feb",index))return 2;
		if(v.startsWith("Mar",index))return 3;
		if(v.startsWith("Apr",index))return 4;
		if(v.startsWith("May",index))return 5;
		if(v.startsWith("Jun",index))return 6;
		if(v.startsWith("Jul",index))return 7;
		if(v.startsWith("Aug",index))return 8;
		if(v.startsWith("Sep",index))return 9;
		if(v.startsWith("Oct",index))return 10;
		if(v.startsWith("Nov",index))return 11;
		if(v.startsWith("Dec",index))return 12;
		return -1;
	}

	private static int parse2Digit(String v, int index){
		int value=0;
		char c=0;
		if(index<v.length() && (c=v.charAt(index))>='0' && c<='9'){
			value+=10*(c-'0'); index++;
		} else return -1;
		if(index<v.length() && (c=v.charAt(index))>='0' && c<='9'){
			value+=(c-'0'); index++;
		} else return -1;
		return value;
	}

	private static int parse4Digit(String v, int index){
		int value=0;
		char c=0;
		if(index<v.length() && (c=v.charAt(index))>='0' && c<='9'){
			value+=1000*(c-'0'); index++;
		} else return -1;
		if(index<v.length() && (c=v.charAt(index))>='0' && c<='9'){
			value+=100*(c-'0'); index++;
		} else return -1;
		if(index<v.length() && (c=v.charAt(index))>='0' && c<='9'){
			value+=10*(c-'0'); index++;
		} else return -1;
		if(index<v.length() && (c=v.charAt(index))>='0' && c<='9'){
			value+=(c-'0'); index++;
		} else return -1;
		return value;
	}

	private static int parsePadded2Digit(String v, int index){
		int value=0;
		char c=0;
		if(index<v.length() && v.charAt(index)==' '){
			value=0; index++;
		} else if(index<v.length() && (c=v.charAt(index))>='0' && c<='9'){
			value+=10*(c-'0'); index++;
		} else return -1;
		if(index<v.length() && (c=v.charAt(index))>='0' && c<='9'){
			value+=(c-'0'); index++;
		} else return -1;
		return value;
	}

	public static long parseHttpDate(String v, long defaultValue){
		if(v==null)return defaultValue;
		int index=0;
		boolean rfc850=false;
		boolean asctime=false;
		if(v.startsWith("Mon")||v.startsWith("Sun")||v.startsWith("Fri")){
			if(v.startsWith("day, ",3)){
				rfc850=true;
				index=8;
			} else {
				index=3;
			}
		} else if(v.startsWith("Tue")){
			if(v.startsWith("sday, ",3)){
				rfc850=true;
				index=9;
			} else {
				index=3;
			}
		} else if(v.startsWith("Wed")){
			if(v.startsWith("nesday, ",3)){
				rfc850=true;
				index=11;
			} else {
				index=3;
			}
		} else if(v.startsWith("Thu")){
			if(v.startsWith("rsday, ",3)){
				rfc850=true;
				index=10;
			} else {
				index=3;
			}
		} else if(v.startsWith("Sat")){
			if(v.startsWith("urday, ",3)){
				rfc850=true;
				index=11;
			} else {
				index=3;
			}
		} else return defaultValue;
		int length=v.length();
		int month=0,day=0,year=0;
		int hour=0,minute=0,second=0;
		if(rfc850){
			day=parse2Digit(v,index);
			if(day<0)return defaultValue;
			index+=2;
			if(index<length && v.charAt(index)!='-')return defaultValue;
			index++;
			month=parseMonth(v,index);
			if(month<0)return defaultValue;
			index+=3;
			if(index<length && v.charAt(index)!='-')return defaultValue;
			index++;
			year=parse2Digit(v,index);
			if(day<0)return defaultValue;
			index+=2;
			if(index<length && v.charAt(index)!=' ')return defaultValue;
			index++;
			year=DateTimeImpl.convertYear(year);
		} else if(v.startsWith(", ",index)){
			index+=2;
			day=parse2Digit(v,index);
			if(day<0)return defaultValue;
			index+=2;
			if(index<length && v.charAt(index)!=' ')return defaultValue;
			index++;
			month=parseMonth(v,index);
			index+=3;
			if(month<0)return defaultValue;
			if(index<length && v.charAt(index)!=' ')return defaultValue;
			index++;
			year=parse4Digit(v,index);
			if(day<0)return defaultValue;
			index+=4;
			if(index<length && v.charAt(index)!=' ')return defaultValue;
			index++;
		} else if(v.startsWith(" ",index)){
			index+=1;
			asctime=true;
			month=parseMonth(v,index);
			if(month<0)return defaultValue;
			index+=3;
			if(index<length && v.charAt(index)!=' ')return defaultValue;
			index++;
			day=parsePadded2Digit(v,index);
			if(day<0)return defaultValue;
			index+=2;
			if(index<length && v.charAt(index)!=' ')return defaultValue;
			index++;
		} else return defaultValue;
		hour=parse2Digit(v,index);
		if(hour<0)return defaultValue;
		index+=2;
		if(index<length && v.charAt(index)!=':')return defaultValue;
		index++;
		minute=parse2Digit(v,index);
		if(minute<0)return defaultValue;
		index+=2;
		if(index<length && v.charAt(index)!=':')return defaultValue;
		index++;
		second=parse2Digit(v,index);
		if(second<0)return defaultValue;
		index+=2;
		if(index<length && v.charAt(index)!=' ')return defaultValue;
		index++;
		if(asctime){
			year=parse4Digit(v,index);
			if(day<0)return defaultValue;
			index+=4;
		} else {
			if(!v.startsWith("GMT",index))return defaultValue;
			index+=3;
		}
		if(index!=length)return defaultValue;
		// NOTE: Month is one-based
		return DateTimeImpl.toDate(year,month,day,hour,minute,second);
	}

	private static int skipQuotedString(String v, int index){
		// assumes index points to quotation mark
		index++;
		int length=v.length();
		char c=0;
		while(index<length){
			c=v.charAt(index);
			if(c=='\\'){
				if(index+1>=length)
					return length;
				else {
					index++;
				}
			} else if(c=='"')
				return index+1;
			else if(c=='\r'){
				if(index+2>=length ||
						v.charAt(index+1)!='\n' ||
						(v.charAt(index+2)!=' ' && v.charAt(index+2)!='\t'))
					// ill-formed whitespace
					return length;
				index+=2;
			} else if(c=='\n'){
				if(index+1>=length ||
						(v.charAt(index+1)!=' ' && v.charAt(index+1)!='\t'))
					// ill-formed whitespace
					return length;
				index+=1;
			} else if(c==127 || (c<32 && c!='\t' && c!=' '))
				// ill-formed
				return length;
			index++;
		}
		return index;
	}

	private static int skipQuotedStringNoLws(String v, int index){
		// assumes index points to quotation mark
		index++;
		int length=v.length();
		char c=0;
		while(index<length){
			c=v.charAt(index);
			if(c=='\\'){
				if(index+1>=length)
					return length;
				else {
					index++;
				}
			} else if(c=='"')
				return index+1;
			else if(c==127 || (c<32))
				// ill-formed
				return length;
			index++;
		}
		return index;
	}

	private static int getPositiveNumber(String v, int index){
		int length=v.length();
		char c=0;
		boolean haveNumber=false;
		int startIndex=index;
		String number=null;
		while(index<length){ // skip whitespace
			c=v.charAt(index);
			if(c<'0' || c>'9'){
				if(!haveNumber)return -1;
				try {
					number=v.substring(startIndex,index);
					return Integer.parseInt(number);
				} catch(NumberFormatException e){
					return Integer.MAX_VALUE;
				}
			} else {
				haveNumber=true;
			}
			index++;
		}
		try {
			number=v.substring(startIndex,length);
			return Integer.parseInt(number);
		} catch(NumberFormatException e){
			return Integer.MAX_VALUE;
		}
	}

	 static int getResponseCode(String s){
		int index=0;
		int length=s.length();
		if(s.indexOf("HTTP/",index)!=index)
			return -1;
		index+=5;
		index=skipZeros(s,index);
		if(index>=length || s.charAt(index)!='1')
			return -1;
		index++;
		if(index>=length || s.charAt(index)!='.')
			return -1;
		index++;
		index=skipZeros(s,index);
		if(index<length && s.charAt(index)=='1') {
			index++;
		}
		if(index>=length || s.charAt(index)!=' ')
			return -1;
		index++;
		if(index+3>=length)return -1;
		if(skipDigits(s,index)!=index+3 ||
				s.charAt(index+3)!=' ')return -1;
		int num=getPositiveNumber(s,index);
		return num;
	}

	static int skipZeros(String v, int index){
		char c=0;
		int length=v.length();
		while(index<length){
			c=v.charAt(index);
			if(c!='0')return index;
			index++;
		}
		return index;
	}
	static int skipDigits(String v, int index){
		char c=0;
		int length=v.length();
		while(index<length){
			c=v.charAt(index);
			if(c<'0' || c>'9')return index;
			index++;
		}
		return index;
	}
	static int skipSpace(String v, int index){
		char c=0;
		int length=v.length();
		while(index<length){
			c=v.charAt(index);
			if(c!=' ')return index;
			index++;
		}
		return index;
	}
	static int skipSpaceOrTab(String v, int index){
		char c=0;
		int length=v.length();
		while(index<length){
			c=v.charAt(index);
			if(c!=' ' && c!='\t')return index;
			index++;
		}
		return index;
	}
	static int skipLinearWhitespace(String v, int index){
		char c=0;
		int length=v.length();
		while(index<length){ // skip whitespace
			c=v.charAt(index);
			if(c=='\r'){
				if(index+2>=length ||
						v.charAt(index+1)!='\n' ||
						(v.charAt(index+2)!=' ' && v.charAt(index+2)!='\t'))
					return index;
				index+=2;
			} else if(c=='\n'){
				// HTTP usually allows only '\r\n' in linear whitespace,
				// but we're being tolerant here
				if(index+1>=length ||
						(v.charAt(index+1)!=' ' && v.charAt(index+1)!='\t'))
					return index;
				index+=1;

			} else if(c!='\t' && c!=' ')
				return index;
			index++;
		}
		return index;
	}


	 static int skipDirective(String str, int io){
		int length=str.length();
		char c=0;
		while(io<length){ // skip non-separator
			c=str.charAt(io);
			if(c=='=' || c==',' || c==127 || c<32) {
				break;
			}
			io++;
		}
		io=skipLinearWhitespace(str,io);
		if(io<length && str.charAt(io)=='='){
			io++;
			io=skipLinearWhitespace(str,io);
			if(io<length && str.charAt(io)=='"') {
				io=skipQuotedString(str,io);
			} else {
				while(io<length){ // skip non-separator
					c=str.charAt(io);
					if(c==',' || c==127 || c<32) {
						break;
					}
					io++;
				}
			}
			io=skipLinearWhitespace(str,io);
		}
		if(io<length && str.charAt(io)==','){
			io++;
			io=skipLinearWhitespace(str,io);
		} else {
			io=length;
		}
		return io;
	}

	 static int parseTokenWithDelta(String str, int index, String token, int[] result){
		int length=str.length();
		int j=0;
		int startIndex=index;
		result[0]=-1;
		for(int i=index;i<length && j<token.length();i++,j++){
			char c=str.charAt(i);
			char cj=token.charAt(j);
			if(c!=cj && c!=(cj>='a' && cj<='z' ? cj-0x20 : cj))
				return startIndex;
		}
		index+=token.length();
		index=skipLinearWhitespace(str,index);
		if(index<length && str.charAt(index)=='='){
			index++;
			index=skipLinearWhitespace(str,index);
			int number=getPositiveNumber(str,index);
			while(index<length){
				char c=str.charAt(index);
				if(c<'0' || c>'9') {
					break;
				}
				index++;
			}
			result[0]=number;
			if(number<-1)
				return startIndex;
			index=skipLinearWhitespace(str,index);
		} else
			return startIndex;
		if(index>=length)return index;
		if(str.charAt(index)==','){
			index++;
			index=skipLinearWhitespace(str,index);
			return index;
		}
		return startIndex;
	}

	 static int parseToken(String str, int index, String token, boolean optionalQuoted){
		int length=str.length();
		int j=0;
		int startIndex=index;
		for(int i=index;i<length && j<token.length();i++,j++){
			char c=str.charAt(i);
			char cj=token.charAt(j);
			if(c!=cj && c!=(cj>='a' && cj<='z' ? cj-0x20 : cj))
				return startIndex;
		}
		index+=token.length();
		index=skipLinearWhitespace(str,index);
		if(optionalQuoted){
			if(index<length && str.charAt(index)=='='){
				index++;
				index=skipLinearWhitespace(str,index);
				if(index<length && str.charAt(index)=='"'){
					index=skipQuotedString(str,index);
				} else return startIndex;
				index=skipLinearWhitespace(str,index);
			}
		}
		if(index>=length)return index;
		if(str.charAt(index)==','){
			index++;
			index=skipLinearWhitespace(str,index);
			return index;
		}
		return startIndex;
	}

	static String getQuotedString(String v, int index){
		// assumes index points to quotation mark
		index++;
		int length=v.length();
		char c=0;
		StringBuilder builder=new StringBuilder();
		while(index<length){
			c=v.charAt(index);
			if(c=='\\'){
				if(index+1>=length)
					// ill-formed
					return "";
				builder.append(v.charAt(index+1));
				index+=2;
				continue;
			} else if(c=='\r' || c=='\n' || c==' ' || c=='\t'){
				int newIndex=skipLinearWhitespace(v,index);
				if(newIndex==index)
					// ill-formed whitespace
					return "";
				builder.append(' ');
				index=newIndex;
				continue;
			} else if(c=='"')
				// done
				return builder.toString();
			else if(c==127 || c<32)
				// ill-formed
				return "";
			else {
				builder.append(c);
				index++;
				continue;
			}
		}
		// ill-formed
		return "";
	}

	private static String getDefaultCharset(String contentType){
		if(contentType.length()>=5){
			char c;
			c=contentType.charAt(0);
			if(c!='T' && c!='t')return "";
			c=contentType.charAt(1);
			if(c!='E' && c!='e')return "";
			c=contentType.charAt(2);
			if(c!='X' && c!='x')return "";
			c=contentType.charAt(3);
			if(c!='T' && c!='t')return "";
			c=contentType.charAt(4);
			if(c!='/')return "";
			return "ISO-8859-1";
		}
		return "";
	}

	static int skipMimeToken(String str, int index){
		int i=index;
		// type
		while(i<str.length()){
			char c=str.charAt(i);
			if(c<=0x20 || c>=0x7F || StringUtility.isChar(c,"()<>@,;:\\\"/[]?=")) {
				break;
			}
			i++;
		}
		return i;

	}
	static String getMimeToken(String str, int index){
		int i=skipMimeToken(str,index);
		return str.substring(index,i);

	}
	public static String getMediaType(String str, int index){
		int i=skipMimeToken(str,index);
		if(i>=str.length() || str.charAt(i)!='/')
			return "";
		i++;
		i=skipMimeToken(str,i);
		return str.substring(index,i);
	}

	public static int skipContentType(String data, int index){
		String mediaType=getMediaType(data,index);
		// NOTE: Media type can be omitted
		index+=mediaType.length();
		while(true){
			int oldindex=index;
			if(index>=data.length() || data.charAt(index)!=';')
				return oldindex;
			index++;
			int index2=skipMimeToken(data,index);
			if(index==index2)
				return oldindex;
			index=index2;
			if(index>=data.length() || data.charAt(index)!='=')
				return oldindex;
			index++;
			if(index>=data.length() || data.charAt(index)=='\"'){
				index=skipQuotedStringNoLws(data,index);
			} else {
				index2=skipMimeToken(data,index);
				if(index==index2)
					return oldindex;
				index=index2;
			}
		}
	}


	 static int toHexNumber(int c) {
		if(c>='A' && c<='Z')
			return 10+c-'A';
		else if(c>='a' && c<='z')
			return 10+c-'a';
		else if (c>='0' && c<='9')
			return c-'0';
		return -1;
	}

	private static int skipAndAppendQuoted(
			String str, int index, StringBuilder builder){
		int i=index;
		boolean slash=false;
		while(i<str.length()){
			char c=str.charAt(i);
			//DebugUtility.log(c);
			if(c=='%' && i+2<str.length()){
				int hex1=toHexNumber(str.charAt(i+1));
				int hex2=toHexNumber(str.charAt(i+2));
				c=(char)(hex1*16+hex2);
				if(i==index && c!='"')
					return index;
				if(!slash){
					if(i!=index && c=='"'){
						builder.append('"');
						return i+1;
					}
					if(c<=0x20 || c>=0x7F)
						return index;
				}
				if(c=='\\' && !slash){
					slash=true;
				} else if(c=='\\'){
					slash=false;
				}
				builder.append(c);
				i+=3;
				continue;
			}
			if(c<=0x20 || c>=0x7F)
				return index;
			if(!StringUtility.isChar(c,"-_.!~*'()") &&
					!(c>='A' && c<='Z') &&
					!(c>='a' && c<='z') &&
					!(c>='0' && c<='9'))
				return index;
			// NOTE: Impossible for '"' and '\' to appear
			// here
			if(i==index)
				return index;
			builder.append(c);
			i++;
		}
		return index;
	}

	private static boolean appendUnescapedValue(
			String str, int index, int length, StringBuilder builder){
		int i=index;
		int io=str.indexOf('%',index);
		boolean doquote=true;
		if(io<0 || io>=index+length){
			doquote=false;
		}
		if(doquote)
		{
			builder.append('\"'); // quote the string for convenience
		}
		while(i<str.length()){
			char c=str.charAt(i);
			//DebugUtility.log(c);
			if(c=='%' && i+2<str.length()){
				int hex1=toHexNumber(str.charAt(i+1));
				int hex2=toHexNumber(str.charAt(i+2));
				c=(char)(hex1*16+hex2);
				if(c<=0x20 || c>=0x7F)
					return false;
				if(doquote && (c=='\\' || c=='"')) {
					builder.append('\\');
				}
				builder.append(c);
				i+=3;
				continue;
			}
			if(c<=0x20 || c>=0x7F || StringUtility.isChar(c,"()<>@,;:\\\"/[]?=")){
				if(doquote) {
					builder.append('\"');
				}
				return true;
			}
			if(!StringUtility.isChar(c,"-_.!~*'()") &&
					!(c>='A' && c<='Z') &&
					!(c>='a' && c<='z') &&
					!(c>='0' && c<='9'))
				return false;
			builder.append(c);
			i++;
		}
		if(doquote) {
			builder.append('\"');
		}
		return true;
	}

	private static boolean appendUnescaped(
			String str, int index, int length, StringBuilder builder){
		int i=index;
		// type
		while(i<str.length()){
			char c=str.charAt(i);
			if(c=='%' && i+2<str.length()){
				int hex1=toHexNumber(str.charAt(i+1));
				int hex2=toHexNumber(str.charAt(i+2));
				c=(char)(hex1*16+hex2);
				builder.append(c);
				if(c<=0x20 || c>=0x7F || StringUtility.isChar(c,"()<>@,;:\\\"/[]?="))
					return false;
				i+=3;
				continue;
			}
			if(c<=0x20 || c>=0x7F || StringUtility.isChar(c,"()<>@,;:\\\"/[]?="))
				return true;
			if(!StringUtility.isChar(c,"-_.!~*'()") &&
					!(c>='A' && c<='Z') &&
					!(c>='a' && c<='z') &&
					!(c>='0' && c<='9'))
				return false;
			builder.append(c);
			i++;
		}
		return true;
	}

	public static String unescapeContentType(String data, int index){
		int index2=skipMimeToken(data,index);
		int indexlast=-1;
		StringBuilder builder=new StringBuilder();
		if(index2<data.length() && data.charAt(index2)=='/'){
			index2++;
			indexlast=index2;
			index2=skipMimeToken(data,index2);
		} else {
			index2=index;
		}
		if(index!=index2){
			if(!appendUnescaped(data,index,indexlast-1-index,builder))
				return "";
			builder.append('/');
			if(!appendUnescaped(data,indexlast,index2-indexlast,builder))
				return "";
		}
		index=index2;
		while(true){
			if(index>=data.length() || data.charAt(index)!=';')
				return builder.toString();
			index++;
			index2=skipMimeToken(data,index);
			if(index==index2)
				return builder.toString();
			int currentLength=builder.length();
			builder.append(';');
			if(!appendUnescaped(data,index,index2-index,builder)){
				builder.setLength(currentLength);
				return builder.toString();
			}
			index=index2;
			if(index>=data.length() || data.charAt(index)!='='){
				builder.setLength(currentLength);
				return builder.toString();
			}
			builder.append('=');
			index++;
			if(data.startsWith("%22",index)){
				index2=skipAndAppendQuoted(data,index,builder);
				if(index==index2){
					builder.setLength(currentLength);
					return builder.toString();
				}
			} else {
				index2=skipMimeToken(data,index);
				if(index==index2){
					builder.setLength(currentLength);
					return builder.toString();
				}
				if(!appendUnescapedValue(data,index,index2-index,builder)){
					builder.setLength(currentLength);
					return builder.toString();
				}
				index=index2;
			}
		}
	}

	public static String getCharset(String data, int index){
		if(data==null)
			return "";
		String mediaType=getMediaType(data,index);
		// NOTE: if media type is omitted,
		// text/plain is assumed by default
		index+=mediaType.length();
		while(true){
			// Note that we skip linear whitespace here,
			// since it doesn't appear to be disallowed
			// in HTTP/1.1 (unlike whitespace between the
			// type/subtype and between attribute/value
			// of a media type)
			index=skipLinearWhitespace(data,index);
			if(index>=data.length() || data.charAt(index)!=';')
				return getDefaultCharset(mediaType);
			index++;
			index=skipLinearWhitespace(data,index);
			String attribute=getMimeToken(data,index);
			if(attribute.length()==0)
				return getDefaultCharset(mediaType);
			index+=attribute.length();
			if(index>=data.length() || data.charAt(index)!='=')
				return getDefaultCharset(mediaType);
			boolean isCharset=(attribute.length()==7 &&
					(attribute.charAt(0)=='c' || attribute.charAt(0)=='C') ||
					(attribute.charAt(1)=='h' || attribute.charAt(1)=='H') ||
					(attribute.charAt(2)=='a' || attribute.charAt(2)=='A') ||
					(attribute.charAt(3)=='r' || attribute.charAt(3)=='R') ||
					(attribute.charAt(4)=='s' || attribute.charAt(4)=='S') ||
					(attribute.charAt(5)=='e' || attribute.charAt(5)=='E') ||
					(attribute.charAt(6)=='t' || attribute.charAt(6)=='T')
					);
			index++;
			if(index>=data.length() || data.charAt(index)=='\"'){
				if(isCharset){
					String str=getQuotedString(data,index);
					return (str.length()>0) ? str :  getDefaultCharset(mediaType);
				} else {
					index=skipQuotedString(data,index);
				}
			} else {
				if(isCharset){
					String str=getMimeToken(data,index);
					return (str.length()>0) ? str :  getDefaultCharset(mediaType);
				} else {
					int index2=skipMimeToken(data,index);
					if(index==index2)
						return getDefaultCharset(mediaType);
					index=index2;
				}
			}
		}
	}
}
