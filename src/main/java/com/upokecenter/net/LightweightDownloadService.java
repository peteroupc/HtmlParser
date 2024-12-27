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
package com.upokecenter.net;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;

import com.upokecenter.io.StreamUtility;
import com.upokecenter.util.IAction;
import com.upokecenter.util.LowPriorityExecutors;

public final class LightweightDownloadService {
  private final LightweightCacheService service;
  private final ExecutorService pool;
  private IAction<Runnable> postResult=null;
  private final Object syncRoot=new Object();
  public LightweightDownloadService(File publicPath, File privatePath, long cacheSize){
    pool=LowPriorityExecutors.newFixedThreadPool(6);
    service=new LightweightCacheService(publicPath,privatePath);
    service.setCacheSize(cacheSize);
  }

  private void handleDownload(final String url, final IResponseListener<Object> cbobj,
      final IOnFinishedListener<Object> finobj) {
    service.enableCache();
    try {
      final Object value=DownloadHelper.downloadUrl(
          url,
          new IResponseListener<Object>(){
            @Override
            public Object processResponse(String url,
                InputStream stream, IHttpHeaders headers)
                    throws IOException {
              Object value=(cbobj==null) ? null : cbobj.processResponse(url,stream,headers);
              //Skip to the end of the stream; otherwise the response
              //won't be cached by HttpURLConnection
              StreamUtility.skipToEnd(stream);
              return value;
            }
          }, false);
      IAction<Runnable> poster=null;
      synchronized(syncRoot){
        poster=postResult;
      }
      if(poster!=null){
        poster.action(new Runnable(){
          @Override
          public void run() {
            if(finobj!=null) {
              finobj.onFinished(url,value,null);
            }
          }
        });
      }
    } catch (IOException e) {
      if(finobj!=null) {
        finobj.onFinished(url,null,e);
      }
      e.printStackTrace();
    }
  }

  public void sendRequest(final String url, final IResponseListener<Object> cbobj,
      final IOnFinishedListener<Object> finobj) {
    pool.submit(new Runnable(){
      @Override
      public void run() {
        handleDownload(url,cbobj,finobj);
      }
    });
  }

  public void setResultPoster(IAction<Runnable> action){
    synchronized(syncRoot){
      postResult=action;
    }
  }

  public void shutdown(){
    service.shutdown();
    pool.shutdown();
  }

}
