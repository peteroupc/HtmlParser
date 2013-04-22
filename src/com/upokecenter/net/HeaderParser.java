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

import java.util.ArrayList;
import java.util.List;

import com.upokecenter.util.DateTimeUtility;
import com.upokecenter.util.StringUtility;


/**
 * 
 * Contains methods useful for parsing header fields.
 * 
 * @author Peter
 *
 */
public final class HeaderParser {

	private HeaderParser(){}
	public static String formatHttpDate(long date){
		int[] components=DateTimeUtility.getGmtDateComponents(date);
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

	/**
	 * Parses a date string in one of the three formats
	 * allowed by RFC2616 (HTTP/1.1).
	 * 
	 * @param v a string to parse.
	 * @param defaultValue a value to return if the string
	 * isn't a valid date.
	 * @return number of milliseconds since midnight, January 1, 1970.
	 */
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
			year=DateTimeUtility.convertYear(year);
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
		// NOTE: Here, the month is one-based
		return DateTimeUtility.toGmtDate(year,month,day,hour,minute,second);
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

	private static int skipZeros(String v, int index){
		char c=0;
		int length=v.length();
		while(index<length){
			c=v.charAt(index);
			if(c!='0')return index;
			index++;
		}
		return index;
	}
	private static int skipDigits(String v, int index){
		char c=0;
		int length=v.length();
		while(index<length){
			c=v.charAt(index);
			if(c<'0' || c>'9')return index;
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
		io=skipLws(str,io,length,null);
		if(io<length && str.charAt(io)=='='){
			io++;
			io=skipLws(str,io,length,null);
			if(io<length && str.charAt(io)=='"') {
				io=skipQuotedString(str,io,length,null,true);
			} else {
				while(io<length){ // skip non-separator
					c=str.charAt(io);
					if(c==',' || c==127 || c<32) {
						break;
					}
					io++;
				}
			}
			io=skipLws(str,io,length,null);
		}
		if(io<length && str.charAt(io)==','){
			io++;
			io=skipLws(str,io,length,null);
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
		index=skipLws(str,index,length,null);
		if(index<length && str.charAt(index)=='='){
			index++;
			index=skipLws(str,index,length,null);
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
			index=skipLws(str,index,length,null);
		} else
			return startIndex;
		if(index>=length)return index;
		if(str.charAt(index)==','){
			index++;
			index=skipLws(str,index,length,null);
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
		index=skipLws(str,index,length,null);
		if(optionalQuoted){
			if(index<length && str.charAt(index)=='='){
				index++;
				index=skipLws(str,index,length,null);
				if(index<length && str.charAt(index)=='"'){
					index=skipQuotedString(str,index,length,null,true);
				} else return startIndex;
				index=skipLws(str,index,length,null);
			}
		}
		if(index>=length)return index;
		if(str.charAt(index)==','){
			index++;
			index=skipLws(str,index,length,null);
			return index;
		}
		return startIndex;
	}

	private static int skipDataUrlParameters(
			String str, int index, int endIndex, StringBuilder builder, boolean plain){
		assert str!=null;
		if(plain && builder!=null){
			builder.append("text/plain");
		}
		StringBuilder tmpbuilder=(builder==null) ? null : new StringBuilder();
		int builderStartPos=(builder==null) ? 0 : builder.length();
		int retval=-1;
		while(true){
			int oldindex=index;
			int builderOldPos=(builder==null) ? 0 : builder.length();
			if(index>=endIndex){
				retval=oldindex;
				break;
			}
			char c=str.charAt(index);
			if(c!=';'){
				// reached end of content type
				if(builder!=null && builder.length()==0){
					// no content type given; provide default
					builder.append("text/plain;charset=us-ascii");
				}
				retval=index;
				break;
			}
			index++;
			if(builder!=null) {
				builder.append(';');
			}
			// get parameter name
			int index2=skipEncodedMimeWord(str,index,endIndex,tmpbuilder,1);
			if(index==index2){
				if(builder!=null) {
					builder.delete(builderOldPos,builder.length());
				}
				retval=oldindex;
				break;
			}
			if(builder!=null){
				// append parameter name to builder
				builder.append(tmpbuilder.toString());
				tmpbuilder.delete(0, tmpbuilder.length());
			}
			index=index2;
			if(index>=endIndex || str.charAt(index)!='='){
				if(builder!=null) {
					builder.delete(builderOldPos,builder.length());
				}
				retval=oldindex;
				break;
			}
			index++;
			if(builder!=null) {
				builder.append('=');
			}
			if(index>=endIndex){
				if(builder!=null) {
					builder.delete(builderOldPos,builder.length());
				}
				retval=oldindex;
				break;
			}
			// get parameter value
			index2=skipEncodedMimeWord(str,index,endIndex,tmpbuilder,2);
			if(index==index2){
				if(builder!=null) {
					builder.delete(builderOldPos,builder.length());
				}
				retval=oldindex;
				break;
			}
			if(builder!=null){
				// append parameter value to builder
				appendParameterValue(tmpbuilder.toString(),builder);
				tmpbuilder.delete(0, tmpbuilder.length());
			}
			index=index2;
		}
		if(plain && builder!=null && builder.length()==builderStartPos){
			// nothing, so append default charset
			builder.append(";charset=us-ascii");
		}
		return retval;
	}
	public static int skipDataUrlContentType(
			String str, int index, StringBuilder builder){
		if(str==null)return index;
		return skipDataUrlContentType(str,index,str.length(),builder);
	}
	/**
	 * Extracts the MIME media type, including its parameters,
	 * from a Data URL path (RFC2397). This function should be used
	 * before calling getMediaType or getMimeParameter because
	 * there are several differences in the MIME media type in
	 * data URLs than in Content-Type headers:
	 * <ul>
	 * <li>Each part of a MIME content type can be URL-encoded
	 * in a data URL, while they can't in a Content-Type header.</li>
	 * <li>The type and subtype can be left out. If left out, the media
	 * type "text/plain" is assumed.</li>
	 * <li>No whitespace is allowed between semicolons of
	 * a MIME media type in a data URL.</li>
	 * </ul>
	 * @param string a string containing the path of the data URL (after
	 * the "data:"). Example: ",test" or "text/plain,test"
	 * @param index the index into the string where the data URL path
	 *  begins.
	 * @param endIndex the index into the string where the data
	 * URL path ends.
	 * @param builder a string builder to append the MIME media
	 * type to. Can be null.
	 * @return the index into the string where the MIME media
	 * type ends within the data URL.  If the MIME type is ill-formed
	 * or <i>string</i> is null, the return value will
	 * be equal to <i>index</i>, and nothing will be appended to
	 * the string builder.  If
	 * the MIME type is blank, the return value will be equal to
	 * <i>index</i>, and the string "text/plain;charset=us-ascii" is
	 * appended to the string builder, if any.
	 */
	public static int skipDataUrlContentType(
			String str, int index, int endIndex, StringBuilder builder){
		if(str==null)return index;
		int startIndex=index;
		int oldpos=(builder==null) ? 0 : builder.length();
		StringBuilder tmpbuilder=(builder==null) ? null : new StringBuilder();
		// Get the type
		int i2=skipEncodedMimeWord(str,index,endIndex,tmpbuilder,0);
		if(index!=i2){
			index=i2;
			if(index<endIndex && str.charAt(index)=='/'){
				index++;
				if(builder!=null){
					// append type to builder
					builder.append(tmpbuilder.toString());
					builder.append('/');
					tmpbuilder.delete(0, tmpbuilder.length());
				}
				// Get the subtype
				i2=skipEncodedMimeWord(str,index,endIndex,tmpbuilder,0);
				if(index!=i2){
					index=i2;
					if(builder!=null){
						// append subtype to builder
						builder.append(tmpbuilder.toString());
						tmpbuilder.delete(0, tmpbuilder.length());
					}
					return skipDataUrlParameters(str,index,endIndex,builder,false);
				} else {
					// invalid media type
					if(builder!=null) {
						builder.delete(oldpos,builder.length());
					}
					return startIndex;
				}
			} else {
				// invalid media type
				if(builder!=null) {
					builder.delete(oldpos,builder.length());
				}
				return startIndex;
			}
		} else {
			// No media type, try checking if it really is blank
			if(index<endIndex && (str.charAt(index)==',' || str.charAt(index)==';'))
				// it's blank; assume text/plain
				return skipDataUrlParameters(str,index,endIndex,builder,true);
			else {
				if(builder!=null) {
					builder.delete(oldpos,builder.length());
				}
				return startIndex;
			}
		}
	}

	private static void appendParameterValue(String str, StringBuilder builder){
		// if string is a valid MIME token, the return value
		// will be the end of the string
		if(skipMimeToken(str,0,str.length(),null)==str.length()){
			// append the string as is
			builder.append(str);
			return;
		} else {
			// otherwise, we must quote the string
			builder.append('"');
			int endIndex=str.length();
			for(int i=0;i<endIndex;i++){
				char c=str.charAt(i);
				if(c=='"' || c==0x7F || c<0x20){
					builder.append('\\');
				}
				builder.append(c);
			}
			builder.append('"');
		}
	}

	 static int skipEncodedMimeWord(
			String str, int index, int endIndex,
			StringBuilder builder, int kind
			){
		int i=index;
		boolean start=true;
		boolean quoted=false;
		int startIndex=index;
		int count=0;
		while(i<endIndex){
			char c=str.charAt(i);
			// check for percent-encoded characters
			if(i+2<endIndex && c=='%' &&
					toHexNumber(str.charAt(i+1))>=0 &&
					toHexNumber(str.charAt(i+2))>=0){
				int c2=toHexNumber(str.charAt(i+1))*16+toHexNumber(str.charAt(i+2));
				if(c2<=0x7F){ // this is an encoded ASCII character
					c=(char)c2;
					i+=2;
				}
			}
			if(start && c==0x22 && kind==2){ // if kind is parameter value
				// this is the start of a quoted string
				i++;
				start=false;
				quoted=true;
				continue;
			}
			start=false;
			if(quoted){
				// quoted string case
				if(c=='\\'){
					if(i+1>=endIndex)
						return startIndex;
					else {
						// get the next character of the
						// quoted pair
						i++;
						c=str.charAt(i);
						// check for percent-encoded characters
						if(i+2<endIndex && c=='%' &&
								toHexNumber(str.charAt(i+1))>=0 &&
								toHexNumber(str.charAt(i+2))>=0){
							int c2=toHexNumber(str.charAt(i+1))*16+toHexNumber(str.charAt(i+2));
							if(c2<=0x7F){ // this is an encoded ASCII character
								c=(char)c2;
								i+=2;
							}
						}
						if(builder!=null) {
							builder.append(c);
						}
					}
				} else if(c=='"')
					// end of quoted string
					return i+1;
				else if(c==127 || (c<32 && c!='\t'))
					// ill-formed
					return startIndex;
				else {
					if(builder!=null) {
						builder.append(c);
					}
				}
			} else {
				if(kind==1 || kind==2){ // kind is parameter name or parameter value
					// unquoted string case
					if(c<=0x20 || c>=0x7F || ((c&0x7F)==c && "()<>@,;:\\\"/[]?=".indexOf(c)>=0)) {
						break;
					}
					if(builder!=null) {
						builder.append(c);
					}
				} else { // kind is 0, type or subtype
					// See RFC6838
					if((c>='A' && c<='Z') || (c>='a' && c<='z') || (c>='0' && c<='9')){
						if(builder!=null) {
							builder.append(c);
						}
						count++;
					} else if(count>0 && ((c&0x7F)==c && "!#$&-^_.+".indexOf(c)>=0)){
						if(builder!=null) {
							builder.append(c);
						}
						count++;
					} else {
						break;
					}
					// type or subtype too long
					if(count>127)return startIndex;
				}
			}
			i++;
		}
		return i;
	}


	 static int skipMimeToken(String str, int index, int endIndex, StringBuilder builder){
		int i=index;
		while(i<endIndex){
			char c=str.charAt(i);
			if(c<=0x20 || c>=0x7F || ((c&0x7F)==c && "()<>@,;:\\\"/[]?=".indexOf(c)>=0)) {
				break;
			}
			if(builder!=null) {
				builder.append(c);
			}
			i++;
		}
		return i;
	}

	private static int skipMimeTypeSubtype(String str, int index, int endIndex, StringBuilder builder){
		int i=index;
		int count=0;
		while(i<str.length()){
			char c=str.charAt(i);
			// See RFC6838
			if((c>='A' && c<='Z') || (c>='a' && c<='z') || (c>='0' && c<='9')){
				if(builder!=null) {
					builder.append(c);
				}
				i++;
				count++;
			} else if(count>0 && ((c&0x7F)==c && "!#$&-^_.+".indexOf(c)>=0)){
				if(builder!=null) {
					builder.append(c);
				}
				i++;
				count++;
			} else {
				break;
			}
			// type or subtype too long
			if(count>127)return index;
		}
		return i;
	}

	/**
	 * Extracts the type and subtype from a MIME media
	 * type.  For example, in the string "text/plain;charset=utf-8",
	 * returns "text/plain".
	 * <br><br>
	 * Note that the default media type according to RFC2045
	 * section 2 is "text/plain"; this function will not return that
	 * value if the media type is ill-formed; rather, this function
	 * is useful more to check if a media type is well-formed.
	 * <br><br>
	 * This function should not be used to extract the media
	 * type from a data URL string; use skipDataUrlContentType
	 * instead on those strings.
	 *
	 * @param str a string containing a MIME media type.
	 * @param index the index into the string where the
	 * media type begins. Specify 0 for the beginning of the
	 * string.
	 * @param endIndex the index for the end of the string.
	 * @return the type and subtype, or an empty string
	 * if the string is not a valid MIME media type.
	 * The string will be normalized to ASCII lower-case.
	 */
	public static String getMediaType(String str, int index, int endIndex){
		if(str==null)return "";
		int i=skipMimeTypeSubtype(str,index,endIndex,null);
		if(i==index || i>=endIndex || str.charAt(i)!='/')
			return "";
		i++;
		int i2=skipMimeTypeSubtype(str,i,endIndex,null);
		if(i==i2)
			return "";
		if(i2<endIndex){
			// if not at end
			int i3=skipCFWS(str,i2,endIndex);
			if(i3==endIndex || (i3<endIndex && str.charAt(i3)!=';' && str.charAt(i3)!=','))
				// at end, or not followed by ";" or ",", so not a media type
				return "";
		}
		return StringUtility.toLowerCaseAscii(str.substring(index,i2));
	}

	public static String getMediaType(String str){
		if(str==null)return "";
		return getMediaType(str,0,str.length());
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

	public static String getCharset(String data){
		if(data==null)return "us-ascii";
		return getCharset(data, 0,data.length());
	}

	/**
	 * Extracts the charset parameter from a MIME media
	 * type.  For example, in the string "text/plain;charset=utf-8",
	 * returns "utf-8".	This method skips folding whitespace and
	 * comments where allowed under RFC5322.  For example,
	 * a string like "text/plain;\r\n  charset=utf-8" is allowed.
	 * @param index the index into the string where the
	 * media type begins.
	 * @param endIndex an index into the end of the string.
	 * @param data a string containing a MIME media type.
	 * 
	 * @return the charset parameter, converted to ASCII lower-case,
	 * if it exists, or "us-ascii" if the media type is null, absent, or
	 * ill-formed (RFC2045 sec. 5.2), or if the media type is
	 * "text/plain" or "text/xml" and doesn't have a charset parameter
	 * (see RFC2046 and RFC3023, respectively),
	 * or the empty string otherwise.
	 */
	public static String getCharset(String data, int index, int endIndex){
		if(data==null)
			return "us-ascii";
		String mediaType=getMediaType(data,index,endIndex);
		if(mediaType.length()==0)
			return "us-ascii";
		String charset=getMimeParameter(data,index,data.length(), "charset");
		if(charset!=null)return StringUtility.toLowerCaseAscii(charset);
		if("text/plain".equals(mediaType) || "text/xml".equals(mediaType))
			return "us-ascii";
		return "";
	}

	private static int skipCrLf(String s, int index, int endIndex){
		if(index+1<endIndex && s.charAt(index)==0x0d && s.charAt(index+1)==0x0a)
			return index+2;
		else
			return index;
	}

	private static int skipNewLine(String s, int index, int endIndex){
		if(index+1<endIndex && s.charAt(index)==0x0d && s.charAt(index+1)==0x0a)
			return index+2;
		else if(index<endIndex && (s.charAt(index)==0x0d || s.charAt(index)==0x0a))
			return index+1;
		else
			return index;
	}

	/* skip space and tab characters */
	private static int skipWsp(String s, int index, int endIndex){
		while(index<endIndex){
			char c=s.charAt(index);
			if(c!=0x20 && c!=0x09)return index;
			index++;
		}
		return index;
	}
	 static int skipLws(String s, int index, int endIndex, StringBuilder builder){
		int ret;
		// While HTTP usually only allows CRLF, it also allows
		// us to be tolerant here
		int i2=skipNewLine(s,index,endIndex);
		ret=skipWsp(s,i2,endIndex);
		if(ret!=i2){
			if(builder!=null) {
				// Note that folding LWS into a space is
				// currently optional under HTTP/1.1 sec. 2.2
				builder.append(' ');
			}
			return ret;
		}
		return index;
	}

	/* Folding white space (RFC5322 sec. 3.2.2) */
	 static int skipFws(String s, int index, int endIndex, StringBuilder builder){
		int ret=skipFws(s,index,endIndex);
		if(builder!=null && ret!=index){
			while(index<ret){
				char c=s.charAt(index);
				index++;
				if(c!=0x0d && c!=0x0a) {
					builder.append(c);
				}
			}
		}
		return ret;
	}
	/* obs-fws under RFC5322, same as LWSP in RFC5234*/
	private static int skipObsFws(String s, int index, int endIndex){
		// parse obs-fws (according to errata)
		while(true){
			int i2=skipCrLf(s,index,endIndex);
			if(i2<endIndex && (s.charAt(i2)==0x20 || s.charAt(i2)==0x09)){
				index=i2+1;
			} else
				return index;
		}
	}
	/* Folding white space (RFC5322 sec. 3.2.2) */
	 static int skipFws(String s, int index, int endIndex){
		int startIndex=index;
		int i2=skipWsp(s,index,endIndex);
		int i2crlf=skipCrLf(s,i2,endIndex);
		if(i2crlf!=i2){// means a CRLF was seen
			int i3=skipWsp(s,i2crlf,endIndex);
			if(i3==i2crlf)
				return skipObsFws(s,startIndex,endIndex);
			else
				return Math.max(i3,skipObsFws(s,startIndex,endIndex));
		} else
			return Math.max(i2,skipObsFws(s,startIndex,endIndex));
	}
	/* quoted-pair (RFC5322 sec. 3.2.1) */
	 static int skipQuotedPair(String s, int index, int endIndex){
		if(index+1<endIndex && s.charAt(index)=='\\'){
			char c=s.charAt(index+1);
			if(c==0x20 || c==0x09 || (c>=0x21 && c<=0x7e))
				return index+2;
			// obs-qp
			if((c<0x20 && c!=0x09)  || c==0x7F)
				return index+2;
		}
		return index;
	}
	/* quoted-string (RFC5322 sec. 3.2.4) */
	 static int skipQuotedString(String s, int index,
			int endIndex, StringBuilder builder){
		return skipQuotedString(s,index,endIndex,builder,false);
	}

	private static int skipCtextOrQuotedPairOrComment(String s, int index, int endIndex){
		if(index>=endIndex)return index;
		int i2;
		i2=skipCtext(s,index,endIndex);
		if(index!=i2)return i2;
		index=i2;
		i2=skipQuotedPair(s,index,endIndex);
		if(index!=i2)return i2;
		index=i2;
		i2=skipComment(s,index,endIndex);
		if(index!=i2)return i2;
		return i2;
	}

	private static int skipQtextOrQuotedPair(String s, int index, int endIndex, boolean httpRules){
		if(index>=endIndex)return index;
		int i2;
		if(httpRules){
			char c=s.charAt(index);
			if(c<0x100 && c>=0x21 && c!='\\' && c!='"')
				return index+1;
		} else {
			i2=skipQtext(s,index,endIndex);
			if(index!=i2)return i2;
			index=i2;
		}
		i2=skipQuotedPair(s,index,endIndex);
		if(index!=i2)return i2;
		return i2;
	}

	 static int skipQuotedString(
			String s,
			int index,
			int endIndex,
			StringBuilder builder, // receives the unescaped version of the string
			boolean httpRules // true: use RFC2616 (HTTP/1.1) rules; false: use RFC5322 rules
			){
		int startIndex=index;
		boolean haveString=false;
		index=(httpRules) ? index : skipCFWS(s,index,endIndex);
		if(!(index<endIndex && s.charAt(index)=='"'))
			return startIndex; // not a valid quoted-string
		index++;
		while(index<endIndex){
			int i2=(httpRules) ? skipLws(s,index,endIndex,builder) : skipFws(s,index,endIndex,builder);
			if(i2!=index) {
				haveString=true;
			}
			index=i2;
			char c=s.charAt(index);
			if(c=='"'){ // end of quoted-string
				index++;
				// RFC5322 requires at least one character in a quoted string
				// (although the published production for quoted-string is wrong
				// according to an erratum)
				if(!haveString && !httpRules)return startIndex;
				return (httpRules) ? index : skipCFWS(s,index,endIndex);
			}
			int oldIndex=index;
			index=skipQtextOrQuotedPair(s,index,endIndex,httpRules);
			if(index==oldIndex)return startIndex;
			haveString=true;
			if(builder!=null){
				// this is a qtext or quoted-pair, so
				// append the last character read
				builder.append(s.charAt(index-1));
			}
		}
		return startIndex; // not a valid quoted-string
	}
	/* atom (RFC5322 sec. 3.2.3) */
	 static int skipAtom(String s, int index,
			int endIndex, StringBuilder builder){
		int startIndex=index;
		index=skipCFWS(s,index,endIndex);
		boolean haveAtom=false;
		while(index<endIndex){
			char c=s.charAt(index);
			if((c>='A' && c<='Z') ||
					(c>='a' && c<='z')  ||
					((c&0x7F)==c && "0123456789!#$%&'*+-/=?^_`{}|~".indexOf(c)>=0)){
				if(builder!=null) {
					builder.append(c);
				}
				index++;
				haveAtom=true;
			}else {
				if(!haveAtom)return startIndex;
				return skipCFWS(s,index,endIndex);
			}
		}
		return (haveAtom) ? index : startIndex;
	}

	/* dot-atom (RFC5322 sec. 3.2.3) */
	/*  currently not used
	 static int skipDotAtom(String s, int index,
	 			int endIndex, StringBuilder builder){
		int startIndex=index;
		index=skipCFWS(s,index,endIndex);
		boolean haveAtom=false;
		boolean haveDot=false;
		while(index<endIndex){
			char c=s.charAt(index);
			if(c=='.'){
				// in case of "x..y"
				if(haveDot){
					builder.delete(builder.length()-1,1);
					return index-1; // index of previous dot
				}
				// in case of ".y"
				if(!haveAtom)return startIndex;
				if(builder!=null)builder.append(c);
				haveDot=true;
				continue;
			}
			if((c>='A' && c<='Z') ||
					(c>='a' && c<='z')  ||
					((c&0x7F)==c && "0123456789!#$%&'*+-/=?^_`{}|~".indexOf((char)c)>=0)){
				if(builder!=null)builder.append(c);
				index++;
				haveAtom=true;
				haveDot=false;
			}else {
				if(!haveAtom)return startIndex;
				if(haveDot){
					// move index to the dot
					builder.delete(builder.length()-1,1);
					return index-1;
				}
				return skipCFWS(s,index,endIndex);
			}
		}
		return (haveAtom) ? index : startIndex;
	}
	 */
	/* ctext (RFC5322 sec. 3.2.1) */
	 static int skipCtext(String s, int index, int endIndex){
		if(index<endIndex){
			char c=s.charAt(index);
			if(c>=33 && c<=126 && c!='(' && c!=')' && c!='\\')
				return index+1;
			// obs-ctext
			if((c<0x20 && c!=0x00 && c!=0x09 && c!=0x0a && c!=0x0d)  || c==0x7F)
				return index+2;
		}
		return index;
	}
	/* ctext (RFC5322 sec. 3.2.1) */
	 static int skipQtext(String s, int index, int endIndex){
		if(index<endIndex){
			char c=s.charAt(index);
			if(c>=33 && c<=126 && c!='\\' && c!='"')
				return index+1;
			// obs-ctext
			if((c<0x20 && c!=0x00 && c!=0x09 && c!=0x0a && c!=0x0d)  || c==0x7F)
				return index+2;
		}
		return index;
	}
	/* comment (RFC5322 sec. 3.2.1) */
	 static int skipComment(String s, int index, int endIndex){
		if(!(index<endIndex && s.charAt(index)=='('))
			return index;
		index++;
		int startIndex=0;
		while(index<endIndex){
			index=skipFws(s,index,endIndex);
			char c=s.charAt(index);
			if(c==')')return index+1;
			int oldIndex=index;
			index=skipCtextOrQuotedPairOrComment(s,index,endIndex);
			if(index==oldIndex)return startIndex;
		}
		return startIndex;
	}

	private static int skipLanguageTag(String str, int index, int endIndex){
		if(index==endIndex || str==null)return index;
		char c=str.charAt(index);
		if(!((c>='A' && c<='Z') || (c>='a' && c<='z')))
			return index; // not a valid language tag
		index++;
		while(index<endIndex){
			c=str.charAt(index);
			if(!((c>='A' && c<='Z') || (c>='a' && c<='z') || (c>='0' && c<='9') || c=='-')){
				break;
			}
			index++;
		}
		return index;
	}

	private static int lengthIfAllAlpha(String str){
		int len=(str==null) ? 0 : str.length();
		for(int i=0;i<len;i++){
			char c1=str.charAt(i);
			if(!((c1>='A' && c1<='Z') || (c1>='a' && c1<='z')))
				return 0;
		}
		return len;
	}
	private static int lengthIfAllAlphaNum(String str){
		int len=(str==null) ? 0 : str.length();
		for(int i=0;i<len;i++){
			char c1=str.charAt(i);
			if(!((c1>='A' && c1<='Z') || (c1>='a' && c1<='z') || (c1>='0' && c1<='9')))
				return 0;
		}
		return len;
	}
	private static int lengthIfAllDigit(String str){
		int len=(str==null) ? 0 : str.length();
		for(int i=0;i<len;i++){
			char c1=str.charAt(i);
			if(!((c1>='0' && c1<='9')))
				return 0;
		}
		return len;
	}


	public static boolean isValidLanguageTag(String str){
		int index=0;
		int endIndex=str.length();
		int startIndex=index;
		if(index+1<endIndex){
			char c1=str.charAt(index);
			char c2=str.charAt(index+1);
			if(
					((c1>='A' && c1<='Z') || (c1>='a' && c1<='z')) &&
					((c2>='A' && c2<='Z') || (c2>='a' && c2<='z'))
					){
				index+=2;
				if(index==endIndex)return true; // case AA
				index+=2;
				// convert the language tag to lower case
				// to simplify handling
				str=StringUtility.toLowerCaseAscii(str);
				c1=str.charAt(index);
				// Straightforward cases
				if((c1>='a' && c1<='z')){
					index++;
					// case AAA
					if(index==endIndex)return true;
					c1=str.charAt(index); // get the next character
				}
				if(c1=='-'){ // case AA- or AAA-
					index++;
					if(index+2==endIndex){ // case AA-?? or AAA-??
						c1=str.charAt(index);
						c2=str.charAt(index);
						if(((c1>='a' && c1<='z')) && ((c2>='a' && c2<='z')))
							return true; // case AA-BB or AAA-BB
					}
				}
				// match grandfathered language tags
				if(str.equals("sgn-be-fr") || str.equals("sgn-be-nl") || str.equals("sgn-ch-de") ||
						str.equals("en-gb-oed"))return true;
				// More complex cases
				String[] splitString=StringUtility.splitAt(
						str.substring(startIndex,endIndex),"-");
				if(splitString.length==0)return false;
				int splitIndex=0;
				int splitLength=splitString.length;
				int len=lengthIfAllAlpha(splitString[splitIndex]);
				if(len<2 || len>8)return false;
				if(len==2 || len==3){
					splitIndex++;
					// skip optional extended language subtags
					for(int i=0;i<3;i++){
						if(splitIndex<splitLength && lengthIfAllAlpha(splitString[splitIndex])==3){
							if(i>=1)
								// point 4 in section 2.2.2 renders two or
								// more extended language subtags invalid
								return false;
							splitIndex++;
						} else {
							break;
						}
					}
				}
				// optional script
				if(splitIndex<splitLength && lengthIfAllAlpha(splitString[splitIndex])==4) {
					splitIndex++;
				}
				// optional region
				if(splitIndex<splitLength && lengthIfAllAlpha(splitString[splitIndex])==2) {
					splitIndex++;
				} else if(splitIndex<splitLength && lengthIfAllDigit(splitString[splitIndex])==3) {
					splitIndex++;
				}
				// variant, any number
				List<String> variants=null;
				while(splitIndex<splitLength){
					String curString=splitString[splitIndex];
					len=lengthIfAllAlphaNum(curString);
					if(len>=5 && len<=8){
						if(variants==null){
							variants=new ArrayList<String>();
						}
						if(!variants.contains(curString)) {
							variants.add(curString);
						} else return false; // variant already exists; see point 5 in section 2.2.5
						splitIndex++;
					} else if(len==4 && (curString.charAt(0)>='0' && curString.charAt(0)<='9')){
						if(variants==null){
							variants=new ArrayList<String>();
						}
						if(!variants.contains(curString)) {
							variants.add(curString);
						} else return false; // variant already exists; see point 5 in section 2.2.5
						splitIndex++;
					} else {
						break;
					}
				}
				// extension, any number
				if(variants!=null) {
					variants.clear();
				}
				while(splitIndex<splitLength){
					String curString=splitString[splitIndex];
					int curIndex=splitIndex;
					if(lengthIfAllAlphaNum(curString)==1 &&
							!curString.equals("x")){
						if(variants==null){
							variants=new ArrayList<String>();
						}
						if(!variants.contains(curString)) {
							variants.add(curString);
						} else return false; // extension already exists
						splitIndex++;
						boolean havetoken=false;
						while(splitIndex<splitLength){
							curString=splitString[splitIndex];
							len=lengthIfAllAlphaNum(curString);
							if(len>=2 && len<=8){
								havetoken=true;
								splitIndex++;
							} else {
								break;
							}
						}
						if(!havetoken){
							splitIndex=curIndex;
							break;
						}
					} else {
						break;
					}
				}
				// optional private use
				if(splitIndex<splitLength){
					int curIndex=splitIndex;
					if(splitString[splitIndex].equals("x")){
						splitIndex++;
						boolean havetoken=false;
						while(splitIndex<splitLength){
							len=lengthIfAllAlphaNum(splitString[splitIndex]);
							if(len>=1 && len<=8){
								havetoken=true;
								splitIndex++;
							} else {
								break;
							}
						}
						if(!havetoken) {
							splitIndex=curIndex;
						}
					}
				}
				// check if all the tokens were used
				return (splitIndex==splitLength);
			} else if(c2=='-' && (c1=='x' || c1=='X')){
				// private use
				index++;
				while(index<endIndex){
					int count=0;
					if(str.charAt(index)!='-')return false;
					index++;
					while(index<endIndex){
						c1=str.charAt(index);
						if(((c1>='A' && c1<='Z') || (c1>='a' && c1<='z') || (c1>='0' && c1<='9'))){
							count++;
							if(count>8)return false;
						} else if(c1=='-') {
							break;
						} else return false;
						index++;
					}
					if(count<1)return false;
				}
				return true;
			} else if(c2=='-' && (c1=='i' || c1=='I')){
				// grandfathered language tags
				str=StringUtility.toLowerCaseAscii(str);
				return (str.equals("i-ami") || str.equals("i-bnn") ||
						str.equals("i-default") || str.equals("i-enochian") ||
						str.equals("i-hak") || str.equals("i-klingon") ||
						str.equals("i-lux") || str.equals("i-navajo") ||
						str.equals("i-mingo") || str.equals("i-pwn") ||
						str.equals("i-tao") || str.equals("i-tay") ||
						str.equals("i-tsu"));
			} else return false;
		} else
			return false;
	}

	private static String[] emptyStringArray=new String[0];

	/**
	 * 
	 * Parses a string consisting of language tags under
	 * Best Current Practice 47.  Examples include "en"
	 * for English, or "fr-ca" for Canadian French.
	 * 
	 * The string is treated as a Content-Language header
	 * value under RFC 3282.
	 * 
	 * @param str a string.
	 * @return an array of language tags within the given
	 * string, or an empty
	 * array if str is null, if there are no language tags,
	 * or at least one language tag in the given
	 * string is invalid under Best Current Practice 47.
	 * The language tags will be converted to ASCII lower-case.
	 */
	public static String[] getLanguages(String str){
		if(str==null)return emptyStringArray;
		return getLanguages(str,0,str.length(),false);
	}
	private static String[] getLanguages(String str, int index, int endIndex, boolean httpRules){
		if(index==endIndex || str==null)
			return emptyStringArray;
		List<String> strings=new ArrayList<String>();
		if(!httpRules) {
			index=skipCFWS(str,index,endIndex);
		}
		while(true){
			int i2=skipLanguageTag(str,index,endIndex);
			if(i2==index)return emptyStringArray;
			String tag=StringUtility.toLowerCaseAscii(str.substring(index,i2));
			i2=index;
			if(!isValidLanguageTag(tag))return emptyStringArray;
			strings.add(tag);
			if(!httpRules){ // RFC 3282 rules
				index=skipCFWS(str,index,endIndex);
				if(index>=endIndex) {
					break;
				}
				if(str.charAt(index)!=',')return emptyStringArray;
				index++;
				index=skipCFWS(str,index,endIndex);
			} else { // HTTP/1.1 rules
				i2=skipLws(str,index,endIndex,null);
				if(i2!=index && i2>=endIndex)return emptyStringArray;
				else if(i2>=endIndex) {
					break;
				}
				index=i2;
				if(str.charAt(index)!=',')return emptyStringArray;
				index++;
				index=skipLws(str,index,endIndex,null);
			}
		}
		return strings.toArray(new String[]{});
	}


	/**
	 * Skips comments and folding whitespace (CFWS) in a string,
	 * as specified in RFC5322 section 3.2.1.
	 *
	 * @param index the index into the beginning of the string
	 * for the purposes of this method.
	 * @param endIndex the index into the end of the string
	 * for the purposes of this method.
	 * @param the index where CFWS ends.  Will be the same
	 * as _index_ if _index_ doesn't point to a comment or folding
	 * whitespace.
	 */
	 static int skipCFWS(String s, int index, int endIndex){
		int retIndex=index;
		while(index<endIndex){
			index=skipFws(s,index,endIndex);
			retIndex=index;
			int oldIndex=index;
			index=skipComment(s,index,endIndex);
			if(index==oldIndex)return retIndex;
			retIndex=index;
		}
		return retIndex;
	}
	/**
	 * Extracts a parameter from a MIME media
	 * type.  For example, in the string "text/plain;charset=utf-8",
	 * returns "utf-8" if the parameter is "charset".
	 * This method skips folding whitespace and comments
	 * where allowed under RFC5322.  For example,
	 * a string like "text/plain;\r\n  charset=utf-8" is allowed.
	 * 
	 * @param str a string containing a MIME media type.
	 * Parameters are compared case-insensitively.
	 * @param index the index into the string where the
	 * media type begins.
	 * @param parameter a parameter name.
	 * @return the parameter, or null if the parameter
	 * doesn't exist or the media type string is ill-formed.
	 */
	public static String getMimeParameter(String data, int index, String parameter){
		if(data==null)return null;
		return getMimeParameter(data, index, data.length(), parameter);
	}
	public static String getMimeParameter(String data, int index, int endIndex, String parameter){
		if(data==null)return null;
		return getMimeParameter(data, index, data.length(), parameter,false);
	}
	/**
	 * Extracts a parameter from a MIME media
	 * type.  For example, in the string "text/plain;charset=utf-8",
	 * returns "utf-8" if the parameter is "charset".
	 * This method either skips folding whitespace and comments
	 * where allowed under RFC5322, or skips linear whitespace
	 * where allowed under HTTP/1.1.  For example,
	 * a string like "text/plain;\r\n  charset=utf-8" is allowed.
	 * @param index the index into the string where the
	 * media type begins.
	 * @param endIndex an index into the end of the string.
	 * @param parameter a parameter name.
	 * @param str a string containing a MIME media type.
	 * Parameters are compared case-insensitively.
	 * @param httpRules If false, the whitespace rules of RFC5322
	 * are used. If true, the whitespace rules of HTTP/1.1 (RFC2616)
	 * are used, and parameter continuations under RFC2231 sec. 3
	 * are supported.
	 * @return the parameter, or null if the parameter
	 * doesn't exist or the media type string is ill-formed.
	 */
	private static String getMimeParameter(
			String data, int index, int endIndex, String parameter, boolean httpRules){
		if(data==null || parameter==null)
			return null;
		String ret=getMimeParameterRaw(data,index,endIndex,parameter,httpRules);
		if(!httpRules && ret==null){
			ret=getMimeParameterRaw(data,index,endIndex,parameter+"*0",httpRules);
			if(ret!=null){
				int pindex=1;
				// Support parameter continuations under RFC2184 sec. 3
				while(true){
					String ret2=getMimeParameterRaw(
							data,index,endIndex,
							parameter+"*"+Integer.toString(pindex),httpRules);
					if(ret2==null) {
						break;
					}
					pindex++;
					ret+=ret2;
				}
			}
		}
		return ret;
	}
	private static String getMimeParameterRaw(
			String data, int index, int endIndex, String parameter, boolean httpRules){
		if(data==null || parameter==null)
			return null;
		if((endIndex-index)<parameter.length())
			return null;
		parameter=StringUtility.toLowerCaseAscii(parameter);
		String mediaType=getMediaType(data,index,endIndex);
		index+=mediaType.length();
		while(true){
			// RFC5322 uses skipCFWS when skipping whitespace;
			// HTTP currently uses skipLws, though that may change
			// to skipWsp in a future revision of HTTP
			if(httpRules) {
				index=skipLws(data,index,endIndex,null);
			} else {
				index=skipCFWS(data,index,endIndex);
			}
			if(index>=endIndex || data.charAt(index)!=';')
				return null;
			index++;
			if(httpRules) {
				index=skipLws(data,index,endIndex,null);
			} else {
				index=skipCFWS(data,index,endIndex);
			}
			StringBuilder builder=new StringBuilder();
			int afteratt=skipMimeToken(data,index,endIndex,builder);
			if(afteratt==index) // ill-formed attribute
				return null;
			String attribute=builder.toString();
			index=afteratt;
			if(index>=endIndex)
				return null;
			if(data.charAt(index)!='=')
				return null;
			boolean isToken=StringUtility.toLowerCaseAscii(attribute).equals(parameter);
			index++;
			if(index>=endIndex)
				return "";
			builder.delete(0,builder.length());
			// try getting the value quoted
			int qs=skipQuotedString(data,index,endIndex,isToken ? builder : null,httpRules);
			if(qs!=index){
				if(isToken)
					return builder.toString();
				index=qs;
				continue;
			}
			builder.delete(0,builder.length());
			// try getting the value unquoted
			// Note we don't use getAtom
			qs=skipMimeToken(data,index,endIndex,isToken ? builder : null);
			if(qs!=index){
				if(isToken)
					return builder.toString();
				index=qs;
				continue;
			}
			// no valid value, return
			return null;
		}
	}

	public static boolean isValidMediaType(String data){
		if(data==null)
			return false;
		return isValidMediaType(data,0,data.length(),true);
	}

	public static boolean isValidMediaType(
			String data,
			int index,
			int endIndex,
			boolean httpRules // true: use RFC2616 (HTTP/1.1) rules; false: use RFC5322 rules
			){
		if(data==null)
			return false;
		String mediaType=getMediaType(data,index,endIndex);
		index+=mediaType.length();
		while(true){
			if(index>=endIndex)
				return true;
			// RFC5322 uses skipCFWS when skipping whitespace;
			// HTTP currently uses skipLws, though that may change
			// to skipWsp in a future revision of HTTP
			if(httpRules) {
				index=skipLws(data,index,endIndex,null);
			} else {
				index=skipCFWS(data,index,endIndex);
			}
			if(index>=endIndex)
				return false;
			if(data.charAt(index)!=';')
				return false;
			index++;
			if(httpRules) {
				index=skipLws(data,index,endIndex,null);
			} else {
				index=skipCFWS(data,index,endIndex);
			}
			int afteratt=skipMimeToken(data,index,endIndex,null);
			if(afteratt==index) // ill-formed attribute
				return false;
			index=afteratt;
			if(index>=endIndex)
				return false;
			if(data.charAt(index)!='=')
				return false;
			index++;
			if(index>=endIndex)
				return false;
			// try getting the value quoted
			int qs=skipQuotedString(data,index,endIndex,null,httpRules);
			if(qs!=index){
				index=qs;
				continue;
			}
			// try getting the value unquoted
			qs=skipMimeToken(data,index,endIndex,null);
			if(qs!=index){
				index=qs;
				continue;
			}
			// no valid value, return
			return false;
		}
	}

}
