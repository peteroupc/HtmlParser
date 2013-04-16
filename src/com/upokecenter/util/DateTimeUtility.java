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
	public static String toXmlSchemaDate(int[] components){
		StringBuilder b=new StringBuilder();
		// Date
		b.append(String.format(Locale.US,
				"%04d-%02d-%02dT",components[0],components[1],components[2]));
		// Time
		if(components[3]!=0 ||
				components[4]!=0 ||
				components[5]!=0 ||
				components[6]!=0){
			b.append(String.format(Locale.US,
					"%02d:%02d:%02d",components[3],components[4],components[5]));
			// Milliseconds
			if(components[6]!=0){
				b.append(String.format(Locale.US,".%03d",components[6]));
			}
		}
		// Time zone offset
		if(components[8]==0){
			b.append('Z');
		} else {
			int tzabs=Math.abs(components[8]);
			b.append(components[8]<0 ? '-' : '+');
			b.append(String.format(Locale.US,
					"%02d:%02d",tzabs/60,tzabs%60));
		}
		return b.toString();
	}
	public static String toXmlSchemaGmtDate(long time){
		return toXmlSchemaDate(getGmtDateComponents(time));
	}
	public static String toXmlSchemaLocalDate(long time){
		return toXmlSchemaDate(getLocalDateComponents(time));
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
				c.get(Calendar.DAY_OF_WEEK),
				c.get(Calendar.ZONE_OFFSET)/(1000*60)
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
				c.get(Calendar.DAY_OF_WEEK),
				c.get(Calendar.ZONE_OFFSET)/(1000*60)
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
