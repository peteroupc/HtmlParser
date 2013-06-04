/*
Written in 2013 by Peter Occil.  
Any copyright is dedicated to the Public Domain.
http://creativecommons.org/publicdomain/zero/1.0/

If you like this, you should donate to Peter O.
at: http://upokecenter.com/d/
*/
package com.upokecenter.util;

/**
 * Represents an action that takes any number of parameters
 * of the same type.
 * 
 * @author Peter
 *
 * @param <T>  An arbitrary object type.
 */
public interface IAction<T> {
	/**
	 * Does an arbitrary action.
	 * @param parameters An array of parameters that the action accepts.
	 */
	public void action(T... parameters);
}
