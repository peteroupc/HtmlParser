package com.upokecenter.util;
/*
Written in 2013 by Peter Occil.
Any copyright to this work is released to the Public Domain.
In case this is not possible, this work is also
licensed under the Unlicense: https://unlicense.org/

*/

import java.util.*;

  /**
   * Contains auxiliary methods for working with strings.
   */
  public final class StringUtility {
private StringUtility() {
}
    private static final String[] ValueEmptyStringArray = new String[0];

    /**
     * Not documented yet.
     * @param s The parameter {@code s} is a text string.
     * @return Either {@code true} or {@code false}.
     */
    public static boolean IsNullOrSpaces(String s) {
      if (s == null) {
        return true;
      }
      int len = s.length();
      int index = 0;
      while (index < len) {
        char c = s.charAt(index);
        if (c != 0x09 && c != 0x0a && c != 0x0c && c != 0x0d && c != 0x20) {
          return false;
        }
        ++index;
      }
      return true;
    }

    /**
     * Splits a string by a delimiter. If the string ends with the delimiter, the
     * result will end with an empty string. If the string begins with the
     * delimiter, the result will start with an empty string. If the delimiter is
     * null or empty, exception. @param s a string to split.
     * @param str The parameter {@code str} is a text string.
     * @param delimiter A string to signal where each substring begins and ends.
     * @return An array containing strings that are split by the delimiter. If s is
     * null or empty, returns an array whose sole element is the empty string.
     * @throws NullPointerException The parameter {@code delimiter} is null.
     * @throws IllegalArgumentException Delimiter is empty.
     */
    public static String[] SplitAt(String str, String delimiter) {
      if (delimiter == null) {
        throw new NullPointerException("delimiter");
      }
      if (delimiter.length() == 0) {
        throw new IllegalArgumentException("delimiter is empty.");
      }
      if (((str) == null || (str).length() == 0)) {
        return new String[] { "" };
      }
      int index = 0;
      boolean first = true;
      ArrayList<String> strings = null;
      int delimLength = delimiter.length();
      while (true) {
        int index2 = str.indexOf(delimiter, index);
        if (index2 < 0) {
          if (first) {
            String[] strret = new String[1];
            strret[0] = str;
            return strret;
          }
          strings = (strings == null) ? (new ArrayList<String>()) : strings;
          strings.add(str.substring(index));
          break;
        } else {
          first = false;
          String newstr = str.substring(index, (index)+(index2 - index));
          strings = (strings == null) ? (new ArrayList<String>()) : strings;
          strings.add(newstr);
          index = index2 + delimLength;
        }
      }
      return (String[])strings.toArray(new String[] { });
    }

    /**
     * Splits a string separated by space characters other than form feed. This
     * method acts as though it strips leading and trailing space characters from
     * the string before splitting it. The space characters used here are U+0009,
     * U+000A, U+000D, and U+0020.
     * @param s A string. Can be null.
     * @return An array of all items separated by spaces. If string is null or
     * empty, returns an empty array.
     */
    public static String[] SplitAtSpTabCrLf(String s) {
      if (s == null || s.length() == 0) {
        return ValueEmptyStringArray;
      }
      int index = 0;
      int valueSLength = s.length();
      while (index < valueSLength) {
        char c = s.charAt(index);
        if (c != 0x09 && c != 0x0a && c != 0x0d && c != 0x20) {
          break;
        }
        ++index;
      }
      if (index == s.length()) {
        return ValueEmptyStringArray;
      }
      ArrayList<String> strings = null;
      int lastIndex = index;
      while (index < valueSLength) {
        char c = s.charAt(index);
        if (c == 0x09 || c == 0x0a || c == 0x0d || c == 0x20) {
          if (lastIndex >= 0) {
            strings = (strings == null) ? (new ArrayList<String>()) : strings;
            strings.add(s.substring(lastIndex, (lastIndex)+(index - lastIndex)));
            lastIndex = -1;
          }
        } else {
          if (lastIndex < 0) {
            lastIndex = index;
          }
        }
        ++index;
      }
      if (lastIndex >= 0) {
        if (strings == null) {
          return new String[] { s.substring(lastIndex, (lastIndex)+(index - lastIndex)) };
        }
        strings.add(s.substring(lastIndex, (lastIndex)+(index - lastIndex)));
      }
      return strings.toArray(new String[] { });
    }

    /**
     * Splits a string separated by space characters. This method acts as though it
     * strips leading and trailing space characters from the string before
     * splitting it. The space characters are U+0009, U+000A, U+000C, U+000D, and
     * U+0020.
     * @param s A string. Can be null.
     * @return An array of all items separated by spaces. If string is null or
     * empty, returns an empty array.
     */
    public static String[] SplitAtSpTabCrLfFf(String s) {
      if (s == null || s.length() == 0) {
        return ValueEmptyStringArray;
      }
      int index = 0;
      int valueSLength = s.length();
      while (index < valueSLength) {
        char c = s.charAt(index);
        if (c != 0x09 && c != 0x0a && c != 0x0c && c != 0x0d && c != 0x20) {
          break;
        }
        ++index;
      }
      if (index == s.length()) {
        return ValueEmptyStringArray;
      }
      ArrayList<String> strings = null;
      int lastIndex = index;
      while (index < valueSLength) {
        char c = s.charAt(index);
        if (c == 0x09 || c == 0x0a || c == 0x0c || c == 0x0d || c == 0x20) {
          if (lastIndex >= 0) {
            strings = (strings == null) ? (new ArrayList<String>()) : strings;
            strings.add(s.substring(lastIndex, (lastIndex)+(index - lastIndex)));
            lastIndex = -1;
          }
        } else {
          if (lastIndex < 0) {
            lastIndex = index;
          }
        }
        ++index;
      }
      if (lastIndex >= 0) {
        if (strings == null) {
          return new String[] { s.substring(lastIndex, (lastIndex)+(index - lastIndex)) };
        }
        strings.add(s.substring(lastIndex, (lastIndex)+(index - lastIndex)));
      }
      return strings.toArray(new String[] { });
    }
  }
