package com.upokecenter.util;
/*
If you like this, you should donate to Peter O.
at: http://peteroupc.github.io/

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
import com.upokecenter.util.*;
using com.upokecenter.util;

  class HttpHeadersFromMap implements IHttpHeaders {
    private Map<String, List<String>> valueMap;
    private List<String> valueList;
    private String valueRequestMethod;
    private String valueUrlString;

    public HttpHeadersFromMap(
      String valueUrlString,
      String valueRequestMethod,
      Map<String, List<String>> valueMap) {
      this.valueMap = valueMap;
      this.valueUrlString = valueUrlString;
      this.valueRequestMethod = valueRequestMethod;
      this.valueList = new ArrayList<String>();
      ArrayList<String> keyset = new ArrayList<String>();
      for (Object s : this.valueMap.keySet()) {
        if (((s) == null || (s).length() == 0)) {
          // Add status line (also has the side
          // effect that it will appear first in the valueList)
          List<String> v = this.valueMap.get(s);
          if (v != null && v.size() > 0) {
            this.valueList.add(v.get(0));
          } else {
            this.valueList.add("HTTP/1.1 200 OK");
          }
        } else {
          keyset.add(s);
        }
      }
      java.util.Collections.sort(keyset);
      // Add the remaining headers in sorted order
      for (Object s : keyset) {
        List<String> v = this.valueMap.get(s);
        if (v != null && v.size() > 0) {
          for (Object ss : v) {
            this.valueList.add(s);
            this.valueList.add(ss);
          }
        }
      }
    }

    public String getHeaderField(int index) {
      if (index == 0) {
        return this.valueList.get(0);
      }
      if (index < 0) {
        return null;
      }
      index = ((index - 1) * 2) + 1 + 1;
      return (index < 0 || index >= this.valueList.size()) ? null :
        this.valueList.get(index + 1);
    }

    public String getHeaderField(String name) {
      if (name == null) {
        return this.valueList.get(0);
      }
      name = DataUtilities.ToLowerCaseAscii (name);
      String last = null;
      for (int i = 1; i < this.valueList.size(); i += 2) {
        String key = this.valueList.get(i);
        if (name.equals (key)) {
          last = this.valueList.get(i + 1);
        }
      }
      return last;
    }

    public long getHeaderFieldDate(String field, long defaultValue) {
      return 0;
      // TODO
      // return HeaderParser.parseHttpDate(getHeaderField(field), defaultValue);
    }

    public String getHeaderFieldKey(int index) {
      if (index == 0 || index < 0) {
        return null;
      }
      index = ((index - 1) * 2) + 1;
      return (index < 0 || index >= this.valueList.size()) ? null :
        this.valueList.get(index);
    }

    public Map<String, List<String>> getHeaderFields() {
      return PeterO.Support.Collections.UnmodifiableMap (this.valueMap);
    }

    public String getRequestMethod() {
      return this.valueRequestMethod;
    }

    public int getResponseCode() {
      String status = this.getHeaderField (null);
      return -1;
      // TODO
      // return (status == null) ? (-1) :
      // (HeaderParser.getResponseCode(status));
    }

    public String getUrl() {
      return this.valueUrlString;
    }
  }
