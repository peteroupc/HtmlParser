/*
Written in 2013 by Peter Occil.  Released to the public domain.
Public domain dedication: http://creativecommons.org/publicdomain/zero/1.0/
 */
package com.upokecenter.util;

import java.io.IOException;

public class BEncodeException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public BEncodeException(String string) {
		super(string);
	}

	public BEncodeException(String s, IOException e) {
		super(s,e);
	}

}
