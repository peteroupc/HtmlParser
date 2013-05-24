/*
Written in 2013 by Peter Occil.  
Any copyright is dedicated to the Public Domain.
http://creativecommons.org/publicdomain/zero/1.0/

If you like this, you should donate to Peter O.
at: http://upokecenter.com/d/
 */
package com.upokecenter.util;

import java.util.ArrayList;

/**
 * 
 * Contains utility methods for working with strings.
 * 
 * @author Peter
 *
 */
public final class StringUtility {
	private static final String[] emptyStringArray=new String[0];

	/**
	 * Compares two strings in Unicode code point order. Unpaired
	 * surrogates are treated as individual code points.
	 * @param a The first string.
	 * @param b The second string.
	 * @return A value indicating which string is "less" or "greater".
	 *  0: Both strings are equal or null.
	 *  Less than 0: a is null and b isn't; or the first code point that's
	 *  different is less in A than in B; or b starts with a and is longer than a.
	 *  Greater than 0: b is null and a isn't; or the first code point that's
	 *  different is greater in A than in B; or a starts with b and is longer than b.
	 */
	public static int codePointCompare(String a, String b){
		if(a==null)return (b==null) ? 0 : -1;
		if(b==null)return 1;
		int len=Math.min(a.length(),b.length());
		for(int i=0;i<len;i++){
			int ca=a.charAt(i);
			int cb=b.charAt(i);
			if(ca==cb){
				// normal code units and illegal surrogates
				// are treated as single code points
				if((ca&0xF800)!=0xD800) {
					continue;
				}
				boolean incindex=false;
				if(i+1<a.length() && a.charAt(i+1)>=0xDC00 && a.charAt(i+1)<=0xDFFF){
					ca=0x10000+(ca-0xD800)*0x400+(a.charAt(i+1)-0xDC00);
					incindex=true;
				}
				if(i+1<b.length() && b.charAt(i+1)>=0xDC00 && b.charAt(i+1)<=0xDFFF){
					cb=0x10000+(cb-0xD800)*0x400+(b.charAt(i+1)-0xDC00);
					incindex=true;
				}
				if(ca!=cb)return ca-cb;
				if(incindex) {
					i++;
				}
			} else {
				if((ca&0xF800)!=0xD800 && (cb&0xF800)!=0xD800)
					return ca-cb;
				if(ca>=0xd800 && ca<=0xdbff && i+1<a.length() && a.charAt(i+1)>=0xDC00 && a.charAt(i+1)<=0xDFFF){
					ca=0x10000+(ca-0xD800)*0x400+(a.charAt(i+1)-0xDC00);
				}
				if(cb>=0xd800 && cb<=0xdbff && i+1<b.length() && b.charAt(i+1)>=0xDC00 && b.charAt(i+1)<=0xDFFF){
					cb=0x10000+(cb-0xD800)*0x400+(b.charAt(i+1)-0xDC00);
				}
				return ca-cb;
			}
		}
		if(a.length()==b.length())return 0;
		return (a.length()<b.length()) ? -1 : 1;
	}

	/**
	 * 
	 * Compares two strings in an ASCII case-insensitive
	 * manner.
	 * 
	 * @param a the first string
	 * @param b the second string
	 * @return true if both strings, when converted to ASCII
	 * lower-case, compare as equal; otherwise, false.
	 */
	public static boolean equalsIgnoreCaseAscii(String a, String b){
		return (a==null) ? (b==null) : toLowerCaseAscii(a).equals(toLowerCaseAscii(b));
	}

	/**
	 * Returns true if this string is null or empty.
	 */
	public static boolean isNullOrEmpty(String s){
		return (s==null || s.length()==0);
	}

	/**
	 * Returns true if this string is null, empty, or consists
	 * entirely of space characters. The space characters are
	 * U+0009, U+000A, U+000C, U+000D, and U+0020.
	 */
	public static boolean isNullOrSpaces(String s){
		if(s==null)return true;
		int len=s.length();
		int index=0;
		while(index<len){
			char c=s.charAt(index);
			if(c!=0x09 && c!=0x0a && c!=0x0c && c!=0x0d && c!=0x20)
				return false;
			index++;
		}
		return true;
	}

	/**
	 * 
	 * Splits a string by a delimiter.  If the string ends
	 * with the delimiter, the result will end with an
	 * empty string.  If the string begins with the
	 * delimiter, the result will start with an empty string.
	 * If the delimiter is null or empty, throws an exception.
	 * 
	 * 
	 * @param s a string to split.
	 * @param delimiter a string to signal where each substring
	 * begins and ends.
	 * @return An array containing strings that are split by the
	 * delimiter. If s is null or empty, returns an array whose
	 * sole element is the empty string.
	 */
	public static String[] splitAt(String s, String delimiter){
		if(delimiter==null ||
				delimiter.length()==0)throw new IllegalArgumentException();
		if(s==null || s.length()==0)return new String[]{""};
		int index=0;
		boolean first=true;
		ArrayList<String> strings=null;
		int delimLength=delimiter.length();
		while(true){
			int index2=s.indexOf(delimiter,index);
			if(index2<0){
				if(first)return new String[]{s};
				strings.add(s.substring(index));
				break;
			} else {
				if(first) {
					strings=new ArrayList<String>();
					first=false;
				}
				String newstr=s.substring(index,index2);
				strings.add(newstr);
				index=index2+delimLength;
			}
		}
		return strings.toArray(emptyStringArray);
	}
	/**
	 * 
	 * Splits a string separated by space characters other than
	 * form feed. This method acts as though it strips
	 * leading and trailing space
	 * characters from the string before splitting it.
	 * The space characters used
	 * here are U+0009, U+000A, U+000D, and U+0020.
	 * 
	 * @param s a string. Can be null.
	 * @return an array of all items separated by spaces. If string
	 * is null or empty, returns an empty array.
	 */
	public static String[] splitAtNonFFSpaces(String s){
		if(s==null || s.length()==0)return emptyStringArray;
		int index=0;
		int sLength=s.length();
		while(index<sLength){
			char c=s.charAt(index);
			if(c!=0x09 && c!=0x0a && c!=0x0d && c!=0x20){
				break;
			}
			index++;
		}
		if(index==s.length())return emptyStringArray;
		ArrayList<String> strings=null;
		int lastIndex=index;
		while(index<sLength){
			char c=s.charAt(index);
			if(c==0x09 || c==0x0a || c==0x0d || c==0x20){
				if(lastIndex>=0) {
					if(strings==null) {
						strings=new ArrayList<String>();
					}
					strings.add(s.substring(lastIndex,index));
					lastIndex=-1;
				}
			} else {
				if(lastIndex<0) {
					lastIndex=index;
				}
			}
			index++;
		}
		if(lastIndex>=0){
			if(strings==null)
				return new String[]{s.substring(lastIndex,index)};
			strings.add(s.substring(lastIndex,index));
		}
		return strings.toArray(emptyStringArray);
	}

	/**
	 * 
	 * Splits a string separated by space characters.
	 * This method acts as though it strips leading and
	 * trailing space
	 * characters from the string before splitting it.
	 * The space characters are
	 * U+0009, U+000A, U+000C, U+000D, and U+0020.
	 * 
	 * @param s a string. Can be null.
	 * @return an array of all items separated by spaces. If string
	 * is null or empty, returns an empty array.
	 */
	public static String[] splitAtSpaces(String s){
		if(s==null || s.length()==0)return emptyStringArray;
		int index=0;
		int sLength=s.length();
		while(index<sLength){
			char c=s.charAt(index);
			if(c!=0x09 && c!=0x0a && c!=0x0c && c!=0x0d && c!=0x20){
				break;
			}
			index++;
		}
		if(index==s.length())return emptyStringArray;
		ArrayList<String> strings=null;
		int lastIndex=index;
		while(index<sLength){
			char c=s.charAt(index);
			if(c==0x09 || c==0x0a || c==0x0c || c==0x0d || c==0x20){
				if(lastIndex>=0) {
					if(strings==null) {
						strings=new ArrayList<String>();
					}
					strings.add(s.substring(lastIndex,index));
					lastIndex=-1;
				}
			} else {
				if(lastIndex<0) {
					lastIndex=index;
				}
			}
			index++;
		}
		if(lastIndex>=0){
			if(strings==null)
				return new String[]{s.substring(lastIndex,index)};
			strings.add(s.substring(lastIndex,index));
		}
		return strings.toArray(emptyStringArray);
	}
	public static boolean startsWith(String str, String prefix, int index){
		if(str==null || prefix==null || index<0 || index>=str.length())
			throw new IllegalArgumentException();
		int endpos=prefix.length()+index;
		if(endpos>str.length())return false;
		return str.substring(index,endpos).equals(prefix);
	}

	/**
	 * Returns a string with all ASCII upper-case letters
	 * converted to lower-case.
	 * 
	 * @param s a string.
	 */
	public static String toLowerCaseAscii(String s){
		if(s==null)return null;
		int len=s.length();
		char c=0;
		boolean hasUpperCase=false;
		for(int i=0;i<len;i++){
			c=s.charAt(i);
			if(c>='A' && c<='Z'){
				hasUpperCase=true;
				break;
			}
		}
		if(!hasUpperCase)
			return s;
		StringBuilder builder=new StringBuilder();
		for(int i=0;i<len;i++){
			c=s.charAt(i);
			if(c>='A' && c<='Z'){
				builder.append((char)(c+0x20));
			} else {
				builder.append(c);
			}
		}
		return builder.toString();
	}

	/**
	 * Returns a string with all ASCII lower-case letters
	 * converted to upper-case.
	 * 
	 * @param s a string.
	 */

	public static String toUpperCaseAscii(String s) {
		if(s==null)return null;
		int len=s.length();
		char c=0;
		boolean hasLowerCase=false;
		for(int i=0;i<len;i++){
			c=s.charAt(i);
			if(c>='a' && c<='z'){
				hasLowerCase=true;
				break;
			}
		}
		if(!hasLowerCase)
			return s;
		StringBuilder builder=new StringBuilder();
		for(int i=0;i<len;i++){
			c=s.charAt(i);
			if(c>='a' && c<='z'){
				builder.append((char)(c-0x20));
			} else {
				builder.append(c);
			}
		}
		return builder.toString();
	}

	/**
	 * Returns a string with the leading and
	 * trailing space characters removed.  The space characters are
	 * U+0009, U+000A, U+000C, U+000D, and U+0020.
	 * @param s a string. Can be null.
	 * 
	 */
	public static String trimSpaces(String s){
		if(s==null || s.length()==0)return s;
		int index=0;
		int sLength=s.length();
		while(index<sLength){
			char c=s.charAt(index);
			if(c!=0x09 && c!=0x0a && c!=0x0c && c!=0x0d && c!=0x20){
				break;
			}
			index++;
		}
		if(index==sLength)return "";
		int startIndex=index;
		index=sLength-1;
		while(index>=0){
			char c=s.charAt(index);
			if(c!=0x09 && c!=0x0a && c!=0x0c && c!=0x0d && c!=0x20)
				return s.substring(startIndex,index+1);
			index--;
		}
		return "";
	}

	private StringUtility(){}
}
