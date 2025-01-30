package com.upokecenter.net;
/*

Licensed under the Expat License.

Copyright (C) 2013 Peter Occil

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
*/

import java.util.*;

  /**
   * Not documented yet.
   */
  public interface IHttpHeaders {
    /**
     * Not documented yet.
     * @param name The parameter {@code name} is a 32-bit signed integer.
     * @return The return value is not documented yet.
     */
    String GetHeaderField(int name);

    /**
     * Not documented yet.
     * @param name The parameter {@code name} is a text string.
     * @return The return value is not documented yet.
     */
    String GetHeaderField(String name);

    /**
     * Not documented yet.
     * @return The return value is not documented yet.
     */
    long GetHeaderFieldDate(String field, long defaultValue);

    /**
     * Not documented yet.
     * @param name The parameter {@code name} is a 32-bit signed integer.
     * @return The return value is not documented yet.
     */
    String GetHeaderFieldKey(int name);

    /**
     * Not documented yet.
     * @return The return value is not documented yet.
     */
    Map<String, List<String>> GetHeaderFields();

    /**
     * Not documented yet.
     * @return The return value is not documented yet.
     */
    String GetRequestMethod();

    /**
     * Not documented yet.
     * @return The return value is not documented yet.
     */
    int GetResponseCode();

    /**
     * Not documented yet.
     * @return The return value is not documented yet.
     */
    String GetUrl();
  }
