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

using com.upokecenter.util

  public final class DownloadHelper {
    private static final class CacheFilter {
      private String cacheFileName;

      public CacheFilter(String cacheFileName) {
        this.cacheFileName = cacheFileName;
      }

      public boolean accept(PeterO.Support.File dir, String filename) {
        return filename.startsWith(cacheFileName + "-")&&
          !filename.endsWith(".cache");
      }
    }

    class CacheResponseInfo {
      public Object cr = null;
      public PeterO.Support.File trueCachedFile = null;
      public PeterO.Support.File trueCacheInfoFile = null;
    }

    /**
     * * Connects to a URL to download data from that URL. @param urlString a URL
     * _string. All schemes (protocols) supported by Java's URLConnection
     * are supported. Data URLs are also supported. @param callback an
     * _object to call back on, particularly when the data is ready to be
     * downloaded. Can be null. If the _object also implements
     * IDownloadEventListener, it will also have its onConnecting and
     * onConnected methods called. @return the _object returned by the
     * callback's processResponse method. @ if an I/O error occurs,
     * particularly network errors. @ if urlString is null.
     * @param urlString The parameter {@code urlString} is not documented yet.
     * @param callback The parameter {@code callback} is not documented yet.
     * @return A T object.
     */
    public static <T> T downloadUrl(
      String urlString,
      IResponseListener<T> callback) {
      return downloadUrl (urlString, callback, false);
    }

    /**
     * * Connects to a URL to download data from that URL. @param urlString a URL
     * _string. All schemes (protocols) supported by Java's URLConnection
     * are supported. Data URLs are also supported. @param callback an
     * _object to call back on, particularly when the data is ready to be
     * downloaded. Can be null. If the _object also implements
     * IDownloadEventListener, it will also have its onConnecting and
     * onConnected methods called. @param handleErrorResponses if true, the
     * processResponse method of the supplied callback _object will also be
     * called if an error response is returned. In this case, the _stream_
     * argument of that method will contain the error response body, if
     * any, or null otherwise. If false and an error response is received,
     * an IOException may be thrown instead of calling the processResponse
     * method. This parameter does not affect whether an exception is
     * thrown if the connection fails. @return the _object returned by the
     * callback's processResponse method. @ if an I/O error occurs,
     * particularly network errors. @ if urlString is null.
     * @param urlString The parameter {@code urlString} is not documented yet.
     * @param callback The parameter {@code callback} is not documented yet.
     * @param handleErrorResponses The parameter {@code handleErrorResponses} is
     * not documented yet.
     * @return A T object.
     */
    public static <T> T downloadUrl(
      String urlString,
      IResponseListener<T> callback,
      boolean handleErrorResponses) {
      if (urlString == null) {
        throw new NullPointerException();
      }
      boolean isEventHandler = (callback != null && callback is
          IDownloadEventListener<T>);
      URL uri = null;
      if (isEventHandler && callback != null) {
        ((IDownloadEventListener<T>)callback).onConnecting (urlString);
      }
      uri = URL.parse (urlString);
      if (uri == null) {
        throw new IllegalArgumentException();
      }
      return DownloadHelperImpl.downloadUrl (urlString, callback,
          handleErrorResponses);
    }

    static CacheResponseInfo getCachedResponse(
      String urlString,
      PeterO.Support.File pathForCache,
      boolean getStream) {
      boolean[] incompleteName = new boolean[1];
      String cacheFileName = getCacheFileName (urlString,
          incompleteName) + ".htm";
      PeterO.Support.File trueCachedFile = null;
      PeterO.Support.File trueCacheInfoFile = null;
      CacheResponseInfo crinfo = new CacheResponseInfo();
      if (pathForCache != null && pathForCache.isDirectory()) {
        PeterO.Support.File[] cacheFiles = new PeterO.Support.File[] {
          new PeterO.Support.File(pathForCache, cacheFileName)
        };
        if (incompleteName[0]) {
          ArrayList<PeterO.Support.File> list = new ArrayList<PeterO.Support.File>();
          CacheFilter filter = new CacheFilter(cacheFileName);
          for (Object f : pathForCache.listFiles()) {
            if (filter.accept (pathForCache, f.getName())) {
              list.add(f);
            }
          }
          cacheFiles = list.ToArray();
        } else if (!getStream) {
          crinfo.trueCachedFile = cacheFiles[0];
          crinfo.trueCacheInfoFile = new
PeterO.Support.File(crinfo.trueCachedFile.toString() + ".cache");
          return crinfo;
        }
        //System.out.println("%s, getStream=%s",(cacheFiles),getStream);
        for (Object cacheFile : cacheFiles) {
          if (cacheFile.isFile() && getStream) {
            boolean fresh = false;
            IHttpHeaders headers = null;
            PeterO.Support.File cacheInfoFile = new PeterO.Support.File(cacheFile.toString() +
              ".cache");
            if (cacheInfoFile.isFile()) {
              try {
                CacheControl cc = CacheControl.fromFile (cacheInfoFile);
                //System.out.println("havecache: %s",cc!=null);
                if (cc == null) {
                  fresh = false;
                } else {
                  fresh = (cc == null) ? false : cc.isFresh();
                  if (!urlString.equals (cc.getUri())) {
                    // Wrong URI
                    continue;
                  }
                  //System.out.println("reqmethod: %s",cc.getRequestMethod());
                  if (!"get".equals (cc.getRequestMethod())) {
                    fresh = false;
                  }
                }
                headers = (cc == null) ? new FileBasedHeaders(urlString,
  cacheFile.length) :
                  cc.getHeaders (cacheFile.length);
              } catch (IOException ex) {
                //System.out.println(e.getStackTrace());
                fresh = false;
                headers = new FileBasedHeaders(urlString, cacheFile.length);
              }
            } else {
              long maxAgeMillis = 24L * 3600L * 1000L;
              long
              timeDiff = Math.abs (cacheFile.lastModified() -
(DateTimeUtility.getCurrentDate()));
              fresh = (timeDiff <= maxAgeMillis);
              headers = new FileBasedHeaders(urlString, cacheFile.length);
            }
            //System.out.println("fresh=%s",fresh);
            if (!fresh) {
              // Too old, download again
              trueCachedFile = cacheFile;
              trueCacheInfoFile = cacheInfoFile;
              trueCachedFile.delete();
              trueCacheInfoFile.delete();
              break;
            } else {
              PeterO.Support.InputStream stream = null;
              try {
                stream = new PeterO.Support.BufferedInputStream(new
PeterO.Support.WrappedInputStream(new FileStream(cacheFile.toString(),
  FileMode.Open)), 8192);
                crinfo.cr = DownloadHelperImpl.newCacheResponse (stream,
                    headers);
                //System.out.println("headerfields: %s",headers.getHeaderFields());
              } catch (IOException ex) {
                // if we get an exception here, we download again
                crinfo.cr = null;
              } finally {
                if (stream != null) {
                  try {
                    stream.Dispose();
                  } catch (IOException ex) {}
                }
              }
            }
          }
        }
      }
      if (pathForCache != null) {
        if (trueCachedFile == null) {
          if (incompleteName[0]) {
            int i = 0;
            do {
              trueCachedFile = new PeterO.Support.File(pathForCache,
                cacheFileName + "-"+
                (i).toString());
              ++i;
            } while (trueCachedFile.exists());
          } else {
            trueCachedFile = new PeterO.Support.File(pathForCache,
  cacheFileName);
          }
        }
        trueCacheInfoFile = new
PeterO.Support.File(trueCachedFile.toString() + ".cache");
      }
      crinfo.trueCachedFile = trueCachedFile;
      crinfo.trueCacheInfoFile = trueCacheInfoFile;
      return crinfo;
    }
    private static String getCacheFileName(String uri, boolean[] incomplete) {
      StringBuilder builder = new StringBuilder();
      for (int i = 0; i < uri.length(); ++i) {
        char c = uri.charAt(i);
        if (c <= 0x20 || c == 127 || c == '$' || c == '/' || c == '\\' || c
== ':'||
          c == '"' || c == '\'' || c == '|' || c == '<' || c == '>' || c ==
'*'||
          c == '?') {
          builder.append ('$');
          builder.append ("0123456789ABCDEF"[ (c >> 4) & 15]);
          builder.append ("0123456789ABCDEF"[ (c) & 15]);
        } else {
          builder.append (c);
        }
        if (builder.length() >= 190) {
          if (incomplete != null) {
            incomplete[0] = true;
          }
          return builder.toString();
        }
      }
      if (incomplete != null) {
        incomplete[0] = false;
      }
      return builder.toString();
    }

    public static Object getLegacyResponseCache(PeterO.Support.File
      cachePath) {
      return DownloadHelperImpl.newResponseCache (cachePath);
    }

    public static void pruneCache(PeterO.Support.File cache,
      long maximumSize) {
      if (cache == null || !cache.isDirectory()) {
        return;
      }
      while (true) {
        long length = 0;
        boolean exceeded = false;
        long oldest = Long.MAX_VALUE;
        int count = 0;
        List<PeterO.Support.File> files = new ArrayList<PeterO.Support.File>();
        recursiveListFiles (cache, files);
        for (Object file : files) {
          if (file.isFile()) {
            length += file.length;
            if (length > maximumSize) {
              exceeded = true;
            }
            oldest = file.lastModified();
            ++count;
          }
        }
        if (count <= 1 || !exceeded) {
          return;
        }
        long threshold = oldest + Math.abs (oldest -
            DateTimeUtility.getCurrentDate()) / 2;
        count = 0;
        for (Object file : files) {
          if (file.lastModified() < threshold) {
            if (file.isDirectory()) {
              if (file.delete()) {
                ++count;
              }
            } else {
              length -= file.length;
              if (file.delete()) {
                ++count;
              }
              if (length < maximumSize) {
                return;
              }
            }
          }
        }
        if (count == 0) {
          return;
        }
      }
    }

    private static void recursiveListFiles(PeterO.Support.File file,
      List<PeterO.Support.File> files) {
      for (Object f : file.listFiles()) {
        if (f.isDirectory()) {
          recursiveListFiles (f, files);
        }
        files.add(f);
      }
    }

    private DownloadHelper() {}
  }
