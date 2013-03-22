package com.upokecenter.util;

//
//  Public domain random generator to replace java.util.Random
//  Written by Peter O. in 2013
//

/**
 * 
 * A random number generator intended to replace Java's standard
 * generator (java.util.Random).  It's an implementation of the 
 * JKISS algorithm by David Jones at University College London. 
 * According to Jones, it "passes all of the Dieharder tests
 * and the complete BigCrunch [sic] test set in TestU01", both
 * of which are extensive statistical randomness test batteries.
 * See <http://www.cs.ucl.ac.uk/staff/d.jones/GoodPracticeRNG.pdf> 
 * and the references within that paper.
 * 
 * This class should not be used for cryptographic work.  For
 * that, use SecureRandom instead.
 * 
 * @author Peter O.
 *
 */
public final class Random {
	private int x,y,z,c;
	/**
	 * 
	 * Creates a new instance of the Random class,
	 * with a seed that uses the current time.
	 * 
	 */
	public Random(){
		long ct=System.currentTimeMillis();
		x=123456789^((int)ct&0xFFFFFFFF);
		if(x==0)x=123456789;
		y=987654321^((int)(ct>>32)&0xFFFFFFFF);
		if(y==0)y=987654321;
		ct=System.nanoTime();
		z=43219876^((int)ct&0xFFFFFFFF);
		if(z==0)z=43219876;
		c=6543217^((int)(ct>>32)&0xFFFFFFFF);
		if(c==0)c=6543217;
	}
	// JKISS random number generator by David Jones
	/**
	 * Gets a random 32-bit value.
	 * @return a random integer, with an approximately
	 * equal chance for each of its 32 bits to be set
	 * or unset.  The integer may be either positive or 
	 * negative.
	 */
	public int nextValue(){
		long t;
		x=314527869*x+1234567;
		y^=y<<5;
		y^=((y>>7)&0x1FFFFFFF);
		y^=y<<22;
		t=4294584393L*z+c;
		c=((int)(t>>32)&0xFFFFFFFF);
		z=((int)(t)&0xFFFFFFFF);
		return (x+y+z);
	}
	
	/**
	 * Gets a random number within a certain range.
	 * 
	 * @param maxExclusive an integer greater than 0.
	 * @return a random integer that's at least 0 and less than _maxExclusive_.
	 */
	public int nextInt(int maxExclusive) {
	    if(maxExclusive<=0)throw new IllegalArgumentException();
	    int v=0;
	    int mod=(int)(0x7FFFFFFF%maxExclusive);
	    if(mod==maxExclusive-1){
	    	v=(nextValue()&0x7FFFFFFF);
	    } else {
	    	int limit=0x7FFFFFFF-mod;
	    	do{
	    		v=(nextValue()&0x7FFFFFFF);
	    	} while(v>=limit);
	    }
	    return v%maxExclusive;
	}
}
