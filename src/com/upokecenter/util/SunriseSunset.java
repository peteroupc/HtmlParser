/*
Based on code from Sunriset.c, which was
released to the public domain by Paul Schlyter, December 1992
 */
/* Converted to Java and C# by Peter O., 2013. */

package com.upokecenter.util;

/**
 * Contains utility methods for calculating
 * sunrise and sunset times.
 * 
 * @author Peter
 *
 */
public final class SunriseSunset {

	private SunriseSunset(){}

	private static double RADEG=180.0/Math.PI;
	private static double DEGRAD=Math.PI/180.0;
	private static long days_since_2000_Jan_0(long y, long m, long d){
		return  (367L*(y)-((7*((y)+(((m)+9)/12)))/4)+((275*(m))/9)+(d)-730530L);
	}

	/**
	 * 
	 * A rough time of day.
	 * 
	 * @author Peter
	 *
	 */
	public enum DayState {
		/**
		 * Day light.
		 */
		Day,
		/**
		 * Approaching nighttime.
		 */
		DayToNight,
		/**
		 * Nighttime.
		 */
		Night,
		/**
		 * Approaching daytime.
		 */
		NightToDay
	}

	/**
	 * 
	 * Gets whether it's currently day or night at
	 * the given geographic location.
	 * 
	 * @param lat a geographic latitude, in degrees.
	 * South coordinates are negative.
	 * @param lon a geographic longitude, in degrees.
	 * West coordinates are negative.
	 * @return A day state value.
	 */
	public static DayState getCurrentDayState(double lat, double lon){
		int[] components=DateTimeUtility.getCurrentGmtDateComponents();
		double trise[]=new double[1];
		double tset[]=new double[1];
		double hours=components[3]; // hour
		hours+=components[4]/60.0; // minute
		hours+=components[5]/3600.0; // second
		hours+=components[6]/3600000.0; // millisecond
		// Get sunrise times
		int t=__sunriset__(components[0], // year
				components[1], // month
				components[2], // day
				lon,lat,
				-35.0/60.0,true,trise,tset);
		if(t>0)return DayState.Day;
		double twirise[]=new double[1];
		double twiset[]=new double[1];
		// Get civil twilight times
		int twi=__sunriset__(components[0], // year
				components[1], // month
				components[2], // day
				lon,lat,
				-6,false,twirise,twiset);
		if(twi<0)return DayState.Night;
		if(hours<twirise[0])return DayState.Night;
		if(hours<trise[0])return DayState.NightToDay;
		if(hours<tset[0])return DayState.Day;
		if(hours<twiset[0])return DayState.DayToNight;
		return DayState.Night;
	}


	/* The "workhorse" function for sun rise/set times */

	private static int __sunriset__( int year, int month, int day, double lon, double lat,
			double altit, boolean upper_limb, double []trise, double []tset )
	/***************************************************************************/
	/* Note: year,month,date = calendar date, 1801-2099 only.             */
	/*       Eastern longitude positive, Western longitude negative       */
	/*       Northern latitude positive, Southern latitude negative       */
	/*       The longitude value IS critical in this function!            */
	/*       altit = the altitude which the Sun should cross              */
	/*               Set to -35/60 degrees for rise/set, -6 degrees       */
	/*               for civil, -12 degrees for nautical and -18          */
	/*               degrees for astronomical twilight.                   */
	/*         upper_limb: non-zero -> upper limb, zero -> center         */
	/*               Set to non-zero (e.g. 1) when computing rise/set     */
	/*               times, and to zero when computing start/end of       */
	/*               twilight.                                            */
	/*        *rise = where to store the rise time                        */
	/*        *set  = where to store the set  time                        */
	/*                Both times are relative to the specified altitude,  */
	/*                and thus this function can be used to compute       */
	/*                various twilight times, as well as rise/set times   */
	/* Return value:  0 = sun rises/sets this day, times stored at        */
	/*                    *trise and *tset.                               */
	/*               +1 = sun above the specified "horizon" 24 hours.     */
	/*                    *trise set to time when the sun is at south,    */
	/*                    minus 12 hours while *tset is set to the south  */
	/*                    time plus 12 hours. "Day" length = 24 hours     */
	/*               -1 = sun is below the specified "horizon" 24 hours   */
	/*                    "Day" length = 0 hours, *trise and *tset are    */
	/*                    both set to the time when the sun is at south.  */
	/*                                                                    */
	/**********************************************************************/
	{
		double  d,  /* Days since 2000 Jan 0.0 (negative before) */
		sr,         /* Solar distance, astronomical units */
		sRA,        /* Sun's Right Ascension */
		sdec,       /* Sun's declination */
		sradius,    /* Sun's apparent radius */
		t,          /* Diurnal arc */
		tsouth,     /* Time when Sun is at south */
		sidtime;    /* Local sidereal time */

		int rc = 0; /* Return cde from function - usually 0 */

		/* Compute d of 12h local mean solar time */
		d = days_since_2000_Jan_0(year,month,day) + 0.5 - lon/360.0;

		/* Compute local sidereal time of this moment */
		sidtime = revolution( GMST0(d) + 180.0 + lon );

		/* Compute Sun's RA + Decl at this moment */
		double ra_dec_sr[]=sun_RA_dec( d  );
		sRA=ra_dec_sr[0];
		sdec=ra_dec_sr[1];
		sr=ra_dec_sr[2];
		/* Compute time when Sun is at south - in hours UT */
		tsouth = 12.0 - rev180(sidtime - sRA)/15.0;

		/* Compute the Sun's apparent radius, degrees */
		sradius = 0.2666 / sr;

		/* Do correction to upper limb, if necessary */
		if ( upper_limb ) {
			altit -= sradius;
		}

		/* Compute the diurnal arc that the Sun traverses to reach */
		/* the specified altitude altit: */
		{
			double cost;
			cost = ( Math.sin(DEGRAD*altit) - Math.sin(DEGRAD*lat) * Math.sin(DEGRAD*sdec) ) /
					( Math.cos(DEGRAD*lat) * Math.cos(DEGRAD*sdec) );
			if ( cost >= 1.0 )
			{ rc = -1; t = 0.0;}       /* Sun always below altit */
			else if ( cost <= -1.0 )
			{ rc = +1; t = 12.0; }      /* Sun always above altit */ else {
				t = RADEG*Math.acos(cost)/15.0;   /* The diurnal arc, hours */
			}
		}

		/* Store rise and set times - in hours UT */
		trise[0] = tsouth - t;
		tset[0]  = tsouth + t;

		return rc;
	}  /* __sunriset__ */



	/* The "workhorse" function */

	@SuppressWarnings("unused")
	private static double __daylen__( int year, int month, int day, double lon, double lat,
			double altit, boolean upper_limb )
	/**********************************************************************/
	/* Note: year,month,date = calendar date, 1801-2099 only.             */
	/*       Eastern longitude positive, Western longitude negative       */
	/*       Northern latitude positive, Southern latitude negative       */
	/*       The longitude value is not critical. Set it to the correct   */
	/*       longitude if you're picky, otherwise set to to, say, 0.0     */
	/*       The latitude however IS critical - be sure to get it correct */
	/*       altit = the altitude which the Sun should cross              */
	/*               Set to -35/60 degrees for rise/set, -6 degrees       */
	/*               for civil, -12 degrees for nautical and -18          */
	/*               degrees for astronomical twilight.                   */
	/*         upper_limb: non-zero -> upper limb, zero -> center         */
	/*               Set to non-zero (e.g. 1) when computing day length   */
	/*               and to zero when computing day+twilight length.      */
	/**********************************************************************/
	{
		double  d,  /* Days since 2000 Jan 0.0 (negative before) */
		obl_ecl,    /* Obliquity (inclination) of Earth's axis */
		sr,         /* Solar distance, astronomical units */
		slon,       /* True solar longitude */
		sin_sdecl,  /* Sine of Sun's declination */
		cos_sdecl,  /* Cosine of Sun's declination */
		sradius,    /* Sun's apparent radius */
		t;          /* Diurnal arc */

		/* Compute d of 12h local mean solar time */
		d = days_since_2000_Jan_0(year,month,day) + 0.5 - lon/360.0;

		/* Compute obliquity of ecliptic (inclination of Earth's axis) */
		obl_ecl = 23.4393 - 3.563E-7 * d;
		/* Compute Sun's position */
		double slon_sr[]=sunpos( d );
		slon=slon_sr[0]; sr=slon_sr[1];
		/* Compute sine and cosine of Sun's declination */
		sin_sdecl = Math.sin(DEGRAD*obl_ecl) * Math.sin(DEGRAD*slon);
		cos_sdecl = Math.sqrt( 1.0 - sin_sdecl * sin_sdecl );

		/* Compute the Sun's apparent radius, degrees */
		sradius = 0.2666 / sr;

		/* Do correction to upper limb, if necessary */
		if ( upper_limb ) {
			altit -= sradius;
		}

		/* Compute the diurnal arc that the Sun traverses to reach */
		/* the specified altitude altit: */
		{
			double cost;
			cost = ( Math.sin(DEGRAD*altit) - Math.sin(DEGRAD*lat) * sin_sdecl ) /
					( Math.cos(DEGRAD*lat) * cos_sdecl );
			if ( cost >= 1.0 ) {
				t = 0.0;                      /* Sun always below altit */
			} else if ( cost <= -1.0 ) {
				t = 24.0;                     /* Sun always above altit */
			} else {
				t = (2.0/15.0) * RADEG*Math.acos(cost); /* The diurnal arc, hours */
			}
		}
		return t;
	}  /* __daylen__ */


	/* This function computes the Sun's position at any instant */

	static double[] sunpos( double d )
	/******************************************************/
	/* Computes the Sun's ecliptic longitude and distance */
	/* at an instant given in d, number of days since     */
	/* 2000 Jan 0.0.  The Sun's ecliptic latitude is not  */
	/* computed, since it's always very near 0.           */
	/******************************************************/
	{
		double M,         /* Mean anomaly of the Sun */
		w,         /* Mean longitude of perihelion */
		/* Note: Sun's mean longitude = M + w */
		e,         /* Eccentricity of Earth's orbit */
		E,         /* Eccentric anomaly */
		x, y,      /* x, y coordinates in orbit */
		v;         /* True anomaly */

		/* Compute mean elements */
		M = revolution( 356.0470 + 0.9856002585 * d );
		w = 282.9404 + 4.70935E-5 * d;
		e = 0.016709 - 1.151E-9 * d;

		/* Compute true longitude and radius vector */
		E = M + e * RADEG * Math.sin(DEGRAD*M) * ( 1.0 + e * Math.cos(DEGRAD*M) );
		x = Math.cos(DEGRAD*E) - e;
		y = Math.sqrt( 1.0 - e*e ) * Math.sin(DEGRAD*E);
		double r = Math.sqrt( x*x + y*y );              /* Solar distance */
		v = RADEG*Math.atan2( y, x );                  /* True anomaly */
		double lon = v + w;                        /* True solar longitude */
		if ( lon >= 360.0 ) {
			lon -= 360.0;                   /* Make it 0..360 degrees */
		}
		return new double[]{lon, r};
	}

	static double[] sun_RA_dec( double d)
	{
		double lon, obl_ecl, x, y, z;

		/* Compute Sun's ecliptical coordinates */
		double lon_r[]=sunpos( d );
		lon=lon_r[0];
		/* Compute ecliptic rectangular coordinates (z=0) */
		x = lon_r[1] * Math.cos(DEGRAD*lon);
		y = lon_r[1] * Math.sin(DEGRAD*lon);

		/* Compute obliquity of ecliptic (inclination of Earth's axis) */
		obl_ecl = 23.4393 - 3.563E-7 * d;

		/* Convert to equatorial rectangular coordinates - x is unchanged */
		z = y * Math.sin(DEGRAD*obl_ecl);
		y = y * Math.cos(DEGRAD*obl_ecl);

		/* Convert to spherical coordinates */
		double RA = RADEG*Math.atan2( y, x );
		double dec = RADEG*Math.atan2( z, Math.sqrt(x*x + y*y) );
		return new double[]{RA,dec,lon_r[1]};
	}  /* sun_RA_dec */


	/******************************************************************/
	/* This function reduces any angle to within the first revolution */
	/* by subtracting or adding even multiples of 360.0 until the     */
	/* result is >= 0.0 and < 360.0                                   */
	/******************************************************************/

	private static double INV360 =   ( 1.0 / 360.0 );

	static double revolution( double x )
	/*****************************************/
	/* Reduce angle to within 0..360 degrees */
	/*****************************************/
	{
		return( x - 360.0 * Math.floor( x * INV360 ) );
	}  /* revolution */

	static double rev180( double x )
	/*********************************************/
	/* Reduce angle to within +180..+180 degrees */
	/*********************************************/
	{
		return( x - 360.0 * Math.floor( x * INV360 + 0.5 ) );
	}  /* revolution */


	/*******************************************************************/
	/* This function computes GMST0, the Greenwich Mean Sidereal Time  */
	/* at 0h UT (i.e. the sidereal time at the Greenwhich meridian at  */
	/* 0h UT).  GMST is then the sidereal time at Greenwich at any     */
	/* time of the day.  I've generalized GMST0 as well, and define it */
	/* as:  GMST0 = GMST - UT  --  this allows GMST0 to be computed at */
	/* other times than 0h UT as well.  While this sounds somewhat     */
	/* contradictory, it is very practical:  instead of computing      */
	/* GMST like:                                                      */
	/*                                                                 */
	/*  GMST = (GMST0) + UT * (366.2422/365.2422)                      */
	/*                                                                 */
	/* where (GMST0) is the GMST last time UT was 0 hours, one simply  */
	/* computes:                                                       */
	/*                                                                 */
	/*  GMST = GMST0 + UT                                              */
	/*                                                                 */
	/* where GMST0 is the GMST "at 0h UT" but at the current moment!   */
	/* Defined in this way, GMST0 will increase with about 4 min a     */
	/* day.  It also happens that GMST0 (in degrees, 1 hr = 15 degr)   */
	/* is equal to the Sun's mean longitude plus/minus 180 degrees!    */
	/* (if we neglect aberration, which amounts to 20 seconds of arc   */
	/* or 1.33 seconds of time)                                        */
	/*                                                                 */
	/*******************************************************************/

	static double GMST0( double d )
	{
		double sidtim0;
		/* Sidtime at 0h UT = L (Sun's mean longitude) + 180.0 degr  */
		/* L = M + w, as defined in sunpos(). */
		sidtim0 = revolution( (180.0 + 356.0470 + 282.9404) +
				(0.9856002585 + 4.70935E-5) * d );
		return sidtim0;
	}  /* GMST0 */

}