package com.upokecenter.util;

import java.io.IOException;

public class BEncodeException extends RuntimeException {

	public BEncodeException(String string) {
		super(string);
	}

	public BEncodeException(String s, IOException e) {
		super(s,e);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

}
