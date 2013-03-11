package com.upokecenter.net;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;



public final class HeaderParser {

	private HeaderParser(){}
	public static String formatHttpDate(long date){
		Calendar c=Calendar.getInstance(TimeZone.getTimeZone("GMT"),
				Locale.US);
		c.setTime(new Date(date));
		int dow=c.get(Calendar.DAY_OF_WEEK); // 1 to 7
		String dayofweek=null;
		int month=c.get(Calendar.MONTH); // 0 to 11
		if(dow==Calendar.SUNDAY) {
			dayofweek="Sun, ";
		}
		if(dow==Calendar.MONDAY) {
			dayofweek="Mon, ";
		}
		if(dow==Calendar.TUESDAY) {
			dayofweek="Tue, ";
		}
		if(dow==Calendar.WEDNESDAY) {
			dayofweek="Wed, ";
		}
		if(dow==Calendar.THURSDAY) {
			dayofweek="Thu, ";
		}
		if(dow==Calendar.FRIDAY) {
			dayofweek="Fri, ";
		}
		if(dow==Calendar.SATURDAY) {
			dayofweek="Sat, ";
		}
		if(dayofweek==null)return "";
		String[] months={
				" Jan "," Feb "," Mar "," Apr ",
				" May "," Jun "," Jul "," Aug ",
				" Sep "," Oct "," Nov "," Dec "
		};
		if(month<0||month>=12)return "";
		String monthstr=months[month];
		return String.format(Locale.US,
				"%s%02d%s%04d %02d:%02d:%02d GMT",
				dayofweek,c.get(Calendar.DAY_OF_MONTH),
				monthstr,
				c.get(Calendar.YEAR),
				c.get(Calendar.HOUR_OF_DAY),
				c.get(Calendar.MINUTE),
				c.get(Calendar.SECOND));
	}

	private static int parseMonth(String v, int index){
		if(v.startsWith("Jan",index))return 0;
		if(v.startsWith("Feb",index))return 1;
		if(v.startsWith("Mar",index))return 2;
		if(v.startsWith("Apr",index))return 3;
		if(v.startsWith("May",index))return 4;
		if(v.startsWith("Jun",index))return 5;
		if(v.startsWith("Jul",index))return 6;
		if(v.startsWith("Aug",index))return 7;
		if(v.startsWith("Sep",index))return 8;
		if(v.startsWith("Oct",index))return 9;
		if(v.startsWith("Nov",index))return 10;
		if(v.startsWith("Dec",index))return 11;
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
		Calendar c=Calendar.getInstance(TimeZone.getTimeZone("GMT"),
				Locale.US);
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
			c.setTimeInMillis(new Date().getTime());
			int thisyear=c.get(Calendar.YEAR);
			int this2digityear=thisyear%100;
			int actualyear=year+(thisyear-this2digityear);
			if(year-this2digityear>50){
				actualyear-=100;
			}
			year=actualyear;
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
		c.set(year,month,day,hour,minute,second);
		return c.getTime().getTime();
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

	private static int getPositiveNumber(String v, int index){
		int length=v.length();
		char c=0;
		boolean haveNumber=false;
		int startIndex=index;
		while(index<length){ // skip whitespace
			c=v.charAt(index);
			if(c<'0' || c>'9'){
				if(!haveNumber)return -1;
				try {
					return Integer.parseInt(v.substring(startIndex,index),10);
				} catch(NumberFormatException e){
					return Integer.MAX_VALUE;
				}
			} else {
				haveNumber=true;
			}
			index++;
		}
		try {
			return Integer.parseInt(v.substring(startIndex,length),10);
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
			if(c!=token.charAt(j) && c!=Character.toUpperCase(token.charAt(j)))
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
			if(c!=token.charAt(j) && c!=Character.toUpperCase(token.charAt(j)))
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

	public static String getCharset(String contentType){
		if(contentType==null)
			return "";
		int io=contentType.indexOf(";");
		int length=contentType.length();
		char c=0;
		if(io<0)return getDefaultCharset(contentType); // no charset
		io++;
		while(true){
			io=skipLinearWhitespace(contentType,io);
			if(io+8>length)
				return getDefaultCharset(contentType);
			// Find out if it's CHARSET
			int startio=io;
			boolean ischarset=true;
			c=contentType.charAt(io++);
			if(c!='C' && c!='c') {
				ischarset=false;
			}
			if(ischarset) {
				c=contentType.charAt(io++);
			}
			if(ischarset && c!='H' && c!='h') {
				ischarset=false;
			}
			if(ischarset) {
				c=contentType.charAt(io++);
			}
			if(ischarset && c!='A' && c!='a') {
				ischarset=false;
			}
			if(ischarset) {
				c=contentType.charAt(io++);
			}
			if(ischarset && c!='R' && c!='r') {
				ischarset=false;
			}
			if(ischarset) {
				c=contentType.charAt(io++);
			}
			if(ischarset && c!='S' && c!='s') {
				ischarset=false;
			}
			if(ischarset) {
				c=contentType.charAt(io++);
			}
			if(ischarset && c!='E' && c!='e') {
				ischarset=false;
			}
			if(ischarset) {
				c=contentType.charAt(io++);
			}
			if(ischarset && c!='T' && c!='t') {
				ischarset=false;
			}
			if(!ischarset){
				io=startio;
				while(io<length){ // skip non-separator
					c=contentType.charAt(io);
					if(c=='=' || c==127 || c<32) {
						break;
					}
					io++;
				}
			}
			if(io>=length || contentType.charAt(io)!='='){
				// not a charset
				ischarset=false;
				while(io<length){ // skip non-separator
					c=contentType.charAt(io);
					if(c=='=' || c==127 || c<32) {
						break;
					}
					io++;
				}
				if(io>=length || contentType.charAt(io)!='=')
					// ill-formed
					return getDefaultCharset(contentType);
				io++;
			} else {
				io++;
			}
			if(io<length && contentType.charAt(io)=='"'){
				if(ischarset){
					String str=getQuotedString(contentType,io);
					return (str.length()>0) ? str :  getDefaultCharset(contentType);
				} else {
					io=skipQuotedString(contentType,io);
				}
			} else {
				int startIndex=io;
				while(io<length){ // skip non-semicolon
					c=contentType.charAt(io);
					if(c==';' || c==127 || c<32) {
						break;
					}
					io++;
				}
				if(ischarset){
					String str=contentType.substring(startIndex,io);
					return (str.length()>0) ? str :  getDefaultCharset(contentType);
				}
			}
			io=skipLinearWhitespace(contentType,io);
			if(io<length){
				if(contentType.charAt(io)==';'){
					io++;
				} else
					// ill-formed
					return getDefaultCharset(contentType);
			}
			io=skipLinearWhitespace(contentType,io);
		}
	}


}
