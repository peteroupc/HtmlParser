/*
Written in 2013 by Peter Occil.  Released to the public domain.
Public domain dedication: http://creativecommons.org/publicdomain/zero/1.0/
 */
package com.upokecenter.util;

import java.lang.reflect.Method;

/**
 * 
 * Contains methods useful in debugging.
 * 
 * @author Peter
 *
 */
public final class DebugUtility {
	private DebugUtility(){}

	private static Method androidLog=null;
	private static boolean checkedAndroidLog=false;
	private static Object syncRoot=new Object();

	private static Method getAndroidLog(){
		synchronized(syncRoot){
			if(!checkedAndroidLog){
				androidLog=Reflection.getStaticMethodWithTypes(
						Reflection.getClassForName("android.util.Log"),
						"i",new Class<?>[]{
							String.class,String.class
						});
				checkedAndroidLog=true;
			}
			return androidLog;
		}
	}

	/**
	 * Writes a formatted line of output.
	 * @param format a format string. Uses the same
	 * format as the String.format method.
	 * @param items an array of objects used as formatting
	 * parameters.
	 */
	public static void log(String format, Object... items){
		Method method=getAndroidLog();
		String message=String.format(format,items);
		if(method==null){
			System.out.println(message);
		} else {
			Reflection.invoke(null,method,null,"CWS",message);
		}
	}
	/**
	 * Converts an object to a string,
	 * and then writes that string as a line of output.
	 * @param item the item to convert to a string.
	 */
	public static void log(Object item){
		Method method=getAndroidLog();
		String message=String.format("%s",item==null ? "null" : item.toString());
		if(method==null){
			System.out.println(message);
		} else {
			Reflection.invoke(null,method,null,"CWS",message);
		}
	}
}
