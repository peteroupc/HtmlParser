/*
Written in 2013 by Peter Occil.  Released to the public domain.
Public domain dedication: http://creativecommons.org/publicdomain/zero/1.0/
*/
package com.upokecenter.util;

import java.lang.reflect.Method;


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

	public static void log(String format, Object... items){
		Method method=getAndroidLog();
		String message=String.format(format,items);
		if(method==null){
			System.out.println(message);
		} else {
			Reflection.invoke(null,method,null,"CWS",message);
		}
	}
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
