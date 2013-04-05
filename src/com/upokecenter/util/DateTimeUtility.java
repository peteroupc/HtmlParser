/*
Written in 2013 by Peter Occil.  Released to the public domain.
Public domain dedication: http://creativecommons.org/publicdomain/zero/1.0/
*/
package com.upokecenter.util;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public final class DateTimeUtility {
	private DateTimeUtility(){}

	public static long getCurrentDate(){
		return new Date().getTime();
	}

	public static int convertYear(int twoDigitYear){
		int[] c=getCurrentGmtDateComponents();
		int this2digityear=c[0]%100;
		int actualyear=twoDigitYear+(c[0]-this2digityear);
		if(twoDigitYear-this2digityear>50){
			actualyear-=100;
		}
		return actualyear;
	}

	public static int[] getCurrentGmtDateComponents(){
		return getGmtDateComponents(getCurrentDate());
	}

	public static int[] getGmtDateComponents(long date){
		Calendar c=Calendar.getInstance(TimeZone.getTimeZone("GMT"),
				Locale.US);
		c.setTimeInMillis(date);
		return new int[]{
				c.get(Calendar.YEAR),
				c.get(Calendar.MONTH)+1,
				c.get(Calendar.DAY_OF_MONTH),
				c.get(Calendar.HOUR_OF_DAY),
				c.get(Calendar.MINUTE),
				c.get(Calendar.SECOND),
				c.get(Calendar.MILLISECOND),
				c.get(Calendar.DAY_OF_WEEK)
		};
	}
	public static int[] getCurrentLocalDateComponents(){
		return getLocalDateComponents(getCurrentDate());
	}

	public static int[] getLocalDateComponents(long date){
		Calendar c=Calendar.getInstance();
		c.setTimeInMillis(date);
		return new int[]{
				c.get(Calendar.YEAR),
				c.get(Calendar.MONTH)+1,
				c.get(Calendar.DAY_OF_MONTH),
				c.get(Calendar.HOUR_OF_DAY),
				c.get(Calendar.MINUTE),
				c.get(Calendar.SECOND),
				c.get(Calendar.MILLISECOND),
				c.get(Calendar.DAY_OF_WEEK)
		};
	}

	public static long toLocalDate(int year, int month, int day,
			int hour, int minute, int second){
		Calendar c=Calendar.getInstance();
		c.set(year,month-1,day,hour,minute,second);
		return c.getTime().getTime();
	}

	public static long toGmtDate(int year, int month, int day,
			int hour, int minute, int second){
		Calendar c=Calendar.getInstance(TimeZone.getTimeZone("GMT"),
				Locale.US);
		c.set(year,month-1,day,hour,minute,second);
		return c.getTime().getTime();
	}
}
