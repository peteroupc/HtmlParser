package com.upokecenter.util;
/*
Written in 2013 by Peter Occil.
Any copyright to this work is released to the Public Domain.
In case this is not possible, this work is also
licensed under the Unlicense: https://unlicense.org/

*/

  /**
   * Not documented yet.
   * @param <T> Type parameter not documented yet.
   */
  public interface IBoundAction<T> {
    /**
     * Not documented yet.
     */
    void Action (Object thisObject, T... parameters);
  }
