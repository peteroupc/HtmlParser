/*
Written in 2013 by Peter Occil.
Any copyright is dedicated to the Public Domain.
http://creativecommons.org/publicdomain/zero/1.0/

If you like this, you should donate to Peter O.
at: http://peteroupc.github.io/
*/
package com.upokecenter.util;

public interface IBoundAction<T> {
  public void action(Object thisObject, T... parameters);
}
