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

import java.io.*;
using com.upokecenter.io;
using com.upokecenter.util

  class CacheControl {
    private static class AgedHeaders implements IHttpHeaders {
      CacheControl cc = null;
      long age = 0;
      List<String> list = new ArrayList<String>();

      public AgedHeaders(CacheControl cc, long age, long length) {
        list.add(cc.headers.get(0));
        for (int i = 1; i < cc.headers.size(); i += 2) {
          String key = cc.headers.get(i);
          if (key != null) {
            key = DataUtilities.ToLowerCaseAscii (key);
            if ("content-length".equals (key) || "age".equals (key)) {
              continue;
            }
          }
          list.add(cc.headers.get(i));
          list.add(cc.headers.get(i + 1));
        }
        this.age = age / 1000; // convert age to seconds
        list.add("age");
        list.add((this.age).toString());
        list.add("content-length");
        list.add((length).toString());
        //System.out.println("aged=%s",list);
        this.cc = cc;
      }

      public String getHeaderField(int index) {
        index = (index) * 2 + 1 + 1;
        return (index < 0 || index >= list.size()) ? (null) : (list.get(index + 1));
      }
      public String getHeaderField(String name) {
        if (name == null) {
          return list.get(0);
        }
        name = DataUtilities.ToLowerCaseAscii (name);
        String last = null;
        for (int i = 1; i < list.size(); i += 2) {
          String key = list.get(i);
          if (name.equals (key)) {
            last = list.get(i + 1);
          }
        }
        return last;
      }
      public long getHeaderFieldDate(String field, long defaultValue) {
        return HeaderParser.parseHttpDate (getHeaderField (field),
  defaultValue);
      }
      public String getHeaderFieldKey(int index) {
        index = (index) * 2 + 1;
        return (index < 0 || index >= list.size()) ? (null) : (list.get(index));
      }
      public Map<String, List<String>> getHeaderFields() {
        Map<String, List<String>> map = new
        HashMap<String, List<String>>();
        map.put(null, (new String[] { list.get(0)}));
        for (int i = 1; i < list.size(); i += 2) {
          String key = list.get(i);
          List<String> templist = map.get(key);
          if (templist == null) {
            templist = new ArrayList<String>();
            map.put(key, templist);
          }
          templist.add(list.get(i + 1));
        }
        // Make lists unmodifiable
        for (Object key : new ArrayList<String>(map.keySet())) {
          map.put(key, PeterO.Support.Collections.UnmodifiableList (map.get(key)));
        }
        return PeterO.Support.Collections.UnmodifiableMap (map);
      }
      public String getRequestMethod() {
        return cc.requestMethod;
      }
      public int getResponseCode() {
        return cc.code;
      }

      public String getUrl() {
        return cc.uri;
      }
    }
    private static class CacheControlSerializer {
      public CacheControl readObjectFromStream(InputStream stream) throws java.io.IOException {
        try {
          PeterO.Cbor.CBORObject jsonobj = PeterO.Cbor.CBORObject.ReadJSON
(stream);
          CacheControl cc = new CacheControl();
          cc.cacheability = jsonobj.get("cacheability").AsInt32();
          cc.noStore = jsonobj.get("noStore").AsBoolean();
          cc.noTransform = jsonobj.get("noTransform").AsBoolean();
          cc.mustRevalidate = jsonobj.get("mustRevalidate").AsBoolean();
          cc.requestTime = Long.parseLong(jsonobj.get("requestTime").AsString());
          cc.responseTime = Long.parseLong(jsonobj.get("responseTime").AsString());
          cc.maxAge = Long.parseLong(jsonobj.get("maxAge").AsString());
          cc.date = Long.parseLong(jsonobj.get("date").AsString());
          cc.code = jsonobj.get("code").AsInt32();
          cc.age = Long.parseLong(jsonobj.get("age").AsString());
          cc.uri = jsonobj.get("uri").AsString();
          cc.requestMethod = jsonobj.get("requestMethod").AsString();
          if (cc.requestMethod != null) {
            cc.requestMethod = DataUtilities.ToLowerCaseAscii
(cc.requestMethod);
          }
          cc.headers = new ArrayList<String>();
          PeterO.Cbor.CBORObject jsonarr = jsonobj.get("headers");
          for (int i = 0; i < jsonarr.size(); ++i) {
            String str = jsonarr.get(i).AsString();
            if (str != null && (i % 2) != 0) {
              str = DataUtilities.ToLowerCaseAscii (str);
              if ("age".equals (str) ||
                             "connection".equals (str) ||
                             "keep-alive".equals (str) ||
                             "proxy-authenticate".equals (str) ||
                             "proxy-authorization".equals (str) ||
                             "te".equals (str) ||
                             "trailers".equals (str) ||
                             "transfer-encoding".equals (str) ||
                             "upgrade".equals (str)) {
                // Skip "age" header field and
                // hop-by-hop header fields
                ++i;
                continue;
              }
            }
            cc.headers.add(str);
          }
          return cc;
        } catch (InvalidCastException e) {
          System.out.println (e.getStackTrace());
          return null;
        } catch (NumberFormatException e) {
          System.out.println (e.getStackTrace());
          return null;
        } catch (Json.InvalidJsonException e) {
          System.out.println (e.getStackTrace());
          return null;
        }
      }
      public void writeObjectToStream(CacheControl o, OutputStream stream) throws java.io.IOException {
        PeterO.Cbor.CBORObject jsonobj = PeterO.Cbor.CBORObject.NewMap();
        jsonobj.Set ("cacheability", o.cacheability);
        jsonobj.Set ("noStore", o.noStore);
        jsonobj.Set ("noTransform", o.noTransform);
        jsonobj.Set ("mustRevalidate", o.mustRevalidate);
        jsonobj.Set ("requestTime",
          (o.requestTime).toString());
        jsonobj.Set ("responseTime",
          (o.responseTime).toString());
        jsonobj.Set ("maxAge",
          (o.maxAge).toString());
        jsonobj.Set ("date", (o.date).toString());
        jsonobj.Set ("uri", o.uri);
        jsonobj.Set ("requestMethod", o.requestMethod);
        jsonobj.Set ("code", o.code);
        jsonobj.Set ("age", (o.age).toString());
        PeterO.Cbor.CBORObject jsonarr = PeterO.Cbor.CBORObject.NewArray();
        for (Object header : o.headers) {
          jsonarr.put (header);
        }
        jsonobj.Set ("headers", jsonarr);
        StreamUtility.stringToStream (jsonobj.toString(), stream);
      }
    }

    public static CacheControl fromFile(PeterO.Support.File f) {
      {
        FileStream fs = null;
try {
fs = new FileStream(f.toString(), FileMode.Open);

        return new CacheControlSerializer().readObjectFromStream (fs);
}
finally {
try { if (fs != null) { fs.close(); } } catch (java.io.IOException ex) {}
}
}
    }
    public static CacheControl getCacheControl(IHttpHeaders headers, long
      requestTime) {
      CacheControl cc = new CacheControl();
      boolean proxyRevalidate = false;
      int sMaxAge = 0;
      boolean publicCache = false;
      boolean privateCache = false;
      boolean noCache = false;
      long expires = 0;
      boolean hasExpires = false;
      cc.uri = headers.getUrl();
      String cacheControl = headers.getHeaderField ("cache-control");
      if (cacheControl != null) {
        int index = 0;
        int[] intval = new int[1];
        while (index < cacheControl.length()) {
          int current = index;
          if ((index = HeaderParser.parseToken (cacheControl, current,
  "private",
                  true)) != current) {
            privateCache = true;
          } else if ((index = HeaderParser.parseToken (cacheControl, current,
                  "no-cache",
                  true)) != current) {
            noCache = true;
            //System.out.println("returning early because it saw no-cache");
            return null; // return immediately, this is not cacheable
          } else if ((index = HeaderParser.parseToken(
            cacheControl,
            current,
            "no-store",
            false)) != current) {
            cc.noStore = true;
            //System.out.println("returning early because it saw no-store");
            return null; // return immediately, this is not cacheable or
storable
          } else if ((index = HeaderParser.parseToken(
            cacheControl,
            current,
            "public",
            false)) != current) {
            publicCache = true;
          } else if ((index = HeaderParser.parseToken(
            cacheControl,
            current,
            "no-transform",
            false)) != current) {
            cc.noTransform = true;
          } else if ((index = HeaderParser.parseToken(
            cacheControl,
            current,
            "must-revalidate",
            false)) != current) {
            cc.mustRevalidate = true;
          } else if ((index = HeaderParser.parseToken(
            cacheControl,
            current,
            "proxy-revalidate",
            false)) != current) {
            proxyRevalidate = true;
          } else if ((index = HeaderParser.parseTokenWithDelta(
            cacheControl,
            current,
            "max-age",
            intval)) != current) {
            cc.maxAge = intval[0];
          } else if ((index = HeaderParser.parseTokenWithDelta(
            cacheControl,
            current,
            "s-maxage",
            intval)) != current) {
            sMaxAge = intval[0];
          } else {
            index = HeaderParser.skipDirective (cacheControl, current);
          }
        }
        if (!publicCache && !privateCache && !noCache) {
          noCache = true;
        }
      } else {
        int code = headers.getResponseCode();
        if ((code == 200 || code == 203 || code == 300 || code == 301 || code
            == 410) && headers.getHeaderField ("authorization") == null) {
          publicCache = true;
          privateCache = false;
        } else {
          noCache = true;
        }
      }
      if (headers.getResponseCode() == 206) {
        noCache = true;
      }
      String pragma = headers.getHeaderField ("pragma");
      if (pragma != null && "no-cache"
        .equals (DataUtilities.ToLowerCaseAscii (pragma))) {
        noCache = true;
        //System.out.println("returning early because it saw pragma no-cache");
        return null;
      }
      long now = DateTimeUtility.getCurrentDate();
      cc.code = headers.getResponseCode();
      cc.date = now;
      cc.responseTime = now;
      cc.requestTime = requestTime;
      if (proxyRevalidate) {
        // Enable must-revalidate for simplicity;
        // proxyRevalidate usually only applies to shared caches
        cc.mustRevalidate = true;
      }
      if (headers.getHeaderField ("date") != null) {
        cc.date = headers.getHeaderFieldDate ("date", Long.MIN_VALUE);
        if (cc.date == Long.MIN_VALUE) {
          noCache = true;
        }
      } else {
        noCache = true;
      }
      String expiresHeader = headers.getHeaderField ("expires");
      if (expiresHeader != null) {
        expires = headers.getHeaderFieldDate ("expires", Long.MIN_VALUE);
        hasExpires = (cc.date != Long.MIN_VALUE);
      }
      if (headers.getHeaderField ("age") != null) {
        try {
          cc.age = Integer.parseInt(headers.getHeaderField(
                "age"));
          if (cc.age < 0) {
            cc.age = 0;
          }
        } catch (NumberFormatException ex) {
          cc.age = -1;
        }
      }
      if (cc.maxAge > 0 || sMaxAge > 0) {
        long maxAge = cc.maxAge; // max age in seconds
        if (maxAge == 0) {
          maxAge = sMaxAge;
        }
        if (cc.maxAge > 0 && sMaxAge > 0) {
          maxAge = Math.max (cc.maxAge, sMaxAge);
        }
        cc.maxAge = maxAge * 1000L; // max-age and s-maxage are in seconds
        hasExpires = false;
      } else if (hasExpires && !noCache) {
        long maxAge = expires - cc.date;
        cc.maxAge = (maxAge > Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int)maxAge;
      } else {
        cc.maxAge = (noCache || cc.noStore) ? (0) : (24L * 3600L * 1000L);
      }
      String reqmethod = headers.getRequestMethod();
      if (reqmethod == null || (
          !DataUtilities.ToLowerCaseAscii (reqmethod).equals ("get"))) {
        // caching responses other than GET responses not supported
        return null;
      }
      cc.requestMethod = DataUtilities.ToLowerCaseAscii (reqmethod);
      cc.cacheability = 2;
      if (noCache) {
        cc.cacheability = 0;
      } else if (privateCache) {
        cc.cacheability = 1;
      }
      int i = 0;
      cc.headers.add(headers.getHeaderField (null));
      while (true) {
        String newValue = headers.getHeaderField (i);
        if (newValue == null) {
          break;
        }
        String key = headers.getHeaderFieldKey (i);
        ++i;
        if (key == null) {
          //System.out.println("null key");
          continue;
        }
        key = DataUtilities.ToLowerCaseAscii (key);
        // to simplify matters, don't include Age header fields;
        // so-called hop-by-hop headers are also not included
        if (!"age".equals (key) &&
          !"connection".equals (key) &&
          !"keep-alive".equals (key) &&
          !"proxy-authenticate".equals (key) &&
          !"proxy-authorization".equals (key) &&
          !"te".equals (key) &&
          !"trailer".equals (key) &&
          // NOTE: NOT Trailers
          !"transfer-encoding".equals (key) &&
          !"upgrade".equals (key)) {
          cc.headers.add(key);
          cc.headers.add(newValue);
        }
      }
      //System.out.println(" cc: %s",cc);
      return cc;
    }
    public static void toFile(CacheControl o, PeterO.Support.File file) {
      InputStream fs = new FileStream((file).toString(), FileMode.Create);
      try {
        new CacheControlSerializer().writeObjectToStream (o, fs);
      } finally {
        if (fs != null) {
          fs.Close();
        }
      }
    }
    private int cacheability = 0;
    // Client must not store the response
    // to disk and must remove it from memory
    // as soon as it's finished with it
    private boolean noStore = false;
    // Client must not convert the response
    // to a different format before caching it
    private boolean noTransform = false;
    // Client must re-check the server
    // after the response becomes stale
    private boolean mustRevalidate = false;
    private long requestTime = 0;
    private long responseTime = 0;
    private long maxAge = 0;
    private long date = 0;
    private long age = 0;

    private int code = 0;
    private String uri = "";
    private String requestMethod = "";
    private List<String> headers;

    private CacheControl() {
      headers = new ArrayList<String>();
    }

    private long getAge() {
      long now = DateTimeUtility.getCurrentDate();
      long age = Math.max (0, Math.max (now - date, this.age));
      age += (responseTime - requestTime);
      age += (now - responseTime);
      age = (age > Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int)age;
      return age;
    }

    public int getCacheability() {
      return cacheability;
    }

    public IHttpHeaders getHeaders(long length) {
      return new AgedHeaders(this, getAge(), length);
    }

    public String getRequestMethod() {
      return requestMethod;
    }

    public String getUri() {
      return uri;
    }

    public boolean isFresh() {
      return (cacheability == 0 || noStore) ? (false) : (maxAge > getAge());
    }

    public boolean isMustRevalidate() {
      return mustRevalidate;
    }

    public boolean isNoStore() {
      return noStore;
    }

    public boolean isNoTransform() {
      return noTransform;
    }
    @Override public String toString() {
      return "CacheControl [cacheability=" + cacheability + ", noStore=" +
        noStore + ", noTransform=" + noTransform +
        ", mustRevalidate=" + mustRevalidate + ", requestTime=" +
        requestTime + ", responseTime=" + responseTime + ", maxAge=" +
        maxAge + ", date=" + date + ", age=" + age + ", code=" + code +
        ", headerFields=" + headers + "]";
    }
  }
