package com.upokecenter.util;

public final class StringUtility {
	private StringUtility(){}
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
		StringBuilder b=new StringBuilder();
		for(int i=0;i<len;i++){
			c=s.charAt(i);
			if(c>='A' && c<='Z'){
				b.append((char)(c+0x20));
			} else {
				b.append(c);
			}
		}
		return b.toString();
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
		StringBuilder b=new StringBuilder();
		for(int i=0;i<len;i++){
			c=s.charAt(i);
			if(c>='a' && c<='z'){
				b.append((char)(c-0x20));
			} else {
				b.append(c);
			}
		}
		return b.toString();

	}
}
