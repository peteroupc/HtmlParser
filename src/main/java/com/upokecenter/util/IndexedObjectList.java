package com.upokecenter.util;
/*
Written in 2013 by Peter Occil.
Any copyright is dedicated to the Public Domain.
http://creativecommons.org/publicdomain/zero/1.0/

If you like this, you should donate to Peter O.
at: http://peteroupc.github.io/
*/

import java.util.*;

  /**
   * Not documented yet.
   * @param <T> Type parameter not documented yet.
   */
  public final class IndexedObjectList<T> {
    private List<T> strongrefs = new ArrayList<T>();
    private List<WeakReference> weakrefs = new ArrayList<WeakReference>();
    private Object syncRoot = new Object();

    // Remove the strong reference, but keep the weak
    // reference; the index becomes no good when the
    // _object is garbage collected

    /**
     * Not documented yet.
     * @param index The parameter {@code index} is a 32-bit signed integer.
     * @return A T object.
     */
    public T ReceiveObject(int index) {
      if (index < 0) {
        return null;
      }
      T ret = null;
      synchronized (this.syncRoot) {
        if (index >= this.strongrefs.size()) {
          return null;
        }
        ret = this.strongrefs.get(index);
        if (ret == null) {
          throw new IllegalStateException();
        }
        this.strongrefs.set(index, null);
      }
      return ret;
    }

    // Keep a strong reference and a weak reference

    /**
     * Not documented yet.
     * @param value The parameter {@code value} is a `0 object.
     * @return A 32-bit signed integer.
     */
    public int SendObject(T value) {
      if (value == null) {
        return -1; // Special case for null
      }
      synchronized (this.syncRoot) {
        for (int i = 0; i < this.strongrefs.size(); ++i) {
          if (this.strongrefs.get(i) == null) {
            if (this.weakrefs.get(i) == null ||
              this.weakrefs.get(i).getTarget() == null) {
              // If the _object is garbage collected
              // the index is available for use again
              // System.out.println("Adding _object %d",i);
              this.strongrefs.set(i, value);
              this.weakrefs.set(i, new WeakReference(value));
              return i;
            }
          }
        }
        // Keep a strong and weak reference of
        // the same _object
        int ret = this.strongrefs.size();
        // System.out.println("Adding _object %d",ret);
        this.strongrefs.add(value);
        this.weakrefs.add(new WeakReference(value));
        return ret;
      }
    }
  }
