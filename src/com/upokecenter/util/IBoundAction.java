/*
Written in 2013 by Peter Occil.  Released to the public domain.
Public domain dedication: http://creativecommons.org/publicdomain/zero/1.0/
 */
package com.upokecenter.util;

public interface IBoundAction<T> {
	public void action(Object thisObject, T... parameters);
}
