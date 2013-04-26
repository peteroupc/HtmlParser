/*
Written in 2013 by Peter Occil.  Released to the public domain.
Public domain dedication: http://creativecommons.org/publicdomain/zero/1.0/
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
