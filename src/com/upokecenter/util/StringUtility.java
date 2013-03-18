package com.upokecenter.util;

import java.util.ArrayList;

public final class StringUtility {
	private StringUtility(){}

	public static boolean isChar(int c, String asciiChars){
		return (c>=0 && c<=0x7F && asciiChars.indexOf((char)c)>=0);
	}

	public static boolean startsWith(String str, String o, int index){
		if(str==null || o==null || index<0 || index>=str.length())
			throw new IllegalArgumentException();
		int endpos=o.length()+index;
		if(endpos>str.length())return false;
		return str.substring(index,endpos).equals(o);
	}

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

	private static final String[] emptyStringArray=new String[0];

	public static String[] splitAt(String s, String delimiter){
		if(delimiter==null)throw new IllegalArgumentException();
		if(s==null || s.length()==0)return emptyStringArray;
		int index=0;
		boolean first=true;
		ArrayList<String> strings=null;
		int delimLength=delimiter.length();
		if(delimLength==0)return new String[]{s};
		while(true){
			int index2=s.indexOf(delimiter,index);
			if(index2<0){
				if(first)return new String[]{s};
				strings.add(s.substring(index));
				break;
			} else {
				if(first) {
					strings=new ArrayList<String>();
				}
				strings.add(s.substring(index,index2));
				index=index2+delimLength;
				first=false;
			}
		}
		if(strings==null)return emptyStringArray;
		return strings.toArray(emptyStringArray);
	}


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
}
