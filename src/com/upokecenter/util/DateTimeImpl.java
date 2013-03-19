package com.upokecenter.util;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public final class DateTimeImpl {
	private DateTimeImpl(){}

	public static long getPersistentCurrentDate(){
		return new Date().getTime();
	}

	public static int convertYear(int twoDigitYear){
		Calendar c=Calendar.getInstance(TimeZone.getTimeZone("GMT"),
				Locale.US);
		c.setTimeInMillis(DateTimeImpl.getPersistentCurrentDate());
		int thisyear=c.get(Calendar.YEAR);
		int this2digityear=thisyear%100;
		int actualyear=twoDigitYear+(thisyear-this2digityear);
		if(twoDigitYear-this2digityear>50){
			actualyear-=100;
		}
		return actualyear;
	}

	public static int[] getCurrentDateComponents(){
		return getDateComponents(getPersistentCurrentDate());
	}


	public static int[] getDateComponents(long date){
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

	public static long toDate(int year, int month, int day,
			int hour, int minute, int second){
		Calendar c=Calendar.getInstance(TimeZone.getTimeZone("GMT"),
				Locale.US);
		c.set(year,month-1,day,hour,minute,second);
		return c.getTime().getTime();
	}
}
