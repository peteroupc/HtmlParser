package com.upokecenter.util;
/*
Written in 2013 by Peter Occil.
Any copyright to this work is released to the Public Domain.
In case this is not possible, this work is also
licensed under the Unlicense: https://unlicense.org/

*/

  /**
   * Represents a list of integers or Unicode characters.
   */
  public final class IntList {
    int[] buffer;
    int ptr;
    public IntList() {
      buffer = new int[64];
      ptr = 0;
    }

    public void appendInt(int v) {
      if (ptr < buffer.length) {
        buffer[ptr++] = v;
      } else {
        int[] newbuffer = new int[buffer.length * 2];
        System.arraycopy (buffer, 0, newbuffer, 0, buffer.length);
        buffer = newbuffer;
        buffer[ptr++] = v;
      }
    }

    public void appendInts(int[] array, int offset, int length) {
      if ((array) == null) {
        throw new NullPointerException("array");
      }
      if (offset < 0) {
        throw new IllegalArgumentException("offset less than " + "0 (" + (offset) + ")");
      }
      if (length < 0) {
        throw new IllegalArgumentException("length less than " + "0 (" + (length) + ")");
      }
      if (offset + length > array.length) {
        throw new IllegalArgumentException("offset+length more than " + (array.length) + " (" + (offset + length) + ")");
      }
      if (ptr + length > buffer.length) {
        int[] newbuffer = new int[Math.max(buffer.length * 2, buffer.length +
            length)];
        System.arraycopy (buffer, 0, newbuffer, 0, buffer.length);
        buffer = newbuffer;
      }
      System.arraycopy (array, offset, buffer, ptr, length);
      ptr += length;
    }

    public void appendString(String str) {
      for (int i = 0; i < str.length(); ++i) {
        int c = str.charAt(i);
        if ((c & 0xfc00) == 0xd800 && i + 1 < str.length() &&
          (str.charAt(i + 1) & 0xfc00) == 0xdc00) {
          // Append a UTF-16 surrogate pair
          int cp2 = 0x10000 + (c & 0x3ff) * 0x400 + (str.charAt(i + 1) & 0x3ff);
          appendInt (cp2);
          ++i;
        } else if ((c & 0xf800) == 0xd800) {
          // illegal surrogate
          throw new IllegalArgumentException();
        } else {
          appendInt (c);
        }
      }
    }

    public int[] array() {
      return buffer;
    }
    public void clearAll() {
      ptr = 0;
    }
    public int get(int i) {
        return get (i);
      }
public void set(int i, int value) {
        set (i, value);
      }
    public int get(int index) {
      return buffer[index];
    }

    /**
     * Sets the integer at a specified position to a new value. @param index an
     * index into the list. @param value the integer's new value.
     * @param index The parameter {@code index} is not documented yet.
     * @param value The parameter {@code value} is not documented yet.
     */
    public void set(int index, int value) {
      buffer[index] = value;
    }
    public final int size() {
        return size();
      }
    public int size() {
      return ptr;
    }
    @Override public final String toString() {
      StringBuilder builder = new StringBuilder();
      for (int i = 0; i < ptr; ++i) {
        if (buffer[i] <= 0xffff) {
          builder.append ((char)buffer[i]);
        } else {
          int ch = buffer[i] - 0x10000;
          int lead = ch / 0x400 + 0xd800;
          int trail = (ch & 0x3ff) + 0xdc00;
          builder.append ((char)lead);
          builder.append ((char)trail);
        }
      }
      return builder.toString();
    }
  }
