/*
Written in 2013 by Peter Occil.
Any copyright is dedicated to the Public Domain.
http://creativecommons.org/publicdomain/zero/1.0/

If you like this, you should donate to Peter O.
at: http://peteroupc.github.io/
 */
package com.upokecenter.util;

import java.io.IOException;

public class BEncodeException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public BEncodeException(String string) {
    super(string);
  }

  public BEncodeException(String s, IOException e) {
    super(s,e);
  }

}
