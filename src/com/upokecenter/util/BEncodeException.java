package com.upokecenter.util;

import java.io.IOException;

public class BEncodeException extends RuntimeException {

	public BEncodeException(String string) {
		super(string);
	}

	public BEncodeException(IOException e) {
		super(e);
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

}
