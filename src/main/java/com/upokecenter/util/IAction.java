package com.upokecenter.util;
/*
Written in 2013 by Peter Occil.
Any copyright is dedicated to the Public Domain.
http://creativecommons.org/publicdomain/zero/1.0/

If you like this, you should donate to Peter O.
at: http://peteroupc.github.io/
*/

  /**
   * Not documented yet.
   * @param <T> Type parameter not documented yet.
   */
  public interface IAction<T> {
    /**
     * Does an arbitrary Action. @param parameters An array of parameters that the
     * Action accepts.
     */
    void Action (T... parameters);
  }
