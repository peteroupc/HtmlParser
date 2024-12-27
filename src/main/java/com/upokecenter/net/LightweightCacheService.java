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
import java.net.ResponseCache;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.upokecenter.io.StreamUtility;
import com.upokecenter.util.LowPriorityExecutors;
import com.upokecenter.util.Reflection;

public final class LightweightCacheService {

  ScheduledExecutorService pool;

  volatile long cacheSize=2L*1024L*1024L;

  Object syncRoot=new Object();
  Object syncRootFilesystem=new Object();
  File filePublicPath;
  File filePrivatePath;
  Runnable runnable;
  public LightweightCacheService(File publicPath, File privatePath){
    filePublicPath=publicPath;
    filePrivatePath=privatePath;
    pool=LowPriorityExecutors.newScheduledThreadPool(1);
    runnable=new Runnable(){
      @Override
      public void run() {
        long size=0;
        synchronized(syncRoot){
          size=cacheSize;
        }
        synchronized(syncRootFilesystem){
          if(ResponseCache.getDefault()==null){
            enableCache();
            DownloadHelper.pruneCache(filePublicPath,size);
            DownloadHelper.pruneCache(filePrivatePath,size);
            try {
              StreamUtility.stringToFile("",new File(filePublicPath,".nomedia"));
            } catch (IOException e) {}
            try {
              StreamUtility.stringToFile("",new File(filePrivatePath,".nomedia"));
            } catch (IOException e) {}
          }
        }
      }
    };
    pool.scheduleAtFixedRate(runnable,5,120,TimeUnit.SECONDS);
  }
  public void enableCache(){
    pool.submit(new Runnable(){

      @Override
      public void run() {
        long size=0;
        synchronized(syncRoot){
          size=cacheSize;
        }
        synchronized(syncRootFilesystem){
          enableHttpCache(size);
        }
      }
    });
  }

  private boolean enableHttpCache(long sizeInBytes){
    try {
      return enableHttpCacheInternal(sizeInBytes);
    } catch(IOException e){
      return false;
    }
  }

  private boolean enableHttpCacheInternal(long sizeInBytes) throws IOException{
    File cacheDir=filePublicPath;
    if(cacheDir==null)return false;
    cacheDir=new File(filePublicPath,"httpcache");
    cacheDir.mkdirs();
    // HttpResponseCache added in ICS
    Class<?> clazz=Reflection.getClassForName("android.net.http.HttpResponseCache");
    if(clazz==null && ResponseCache.getDefault()==null){
      ResponseCache legacyCache=(ResponseCache)DownloadHelper.getLegacyResponseCache(cacheDir);
      ResponseCache.setDefault(legacyCache);
      return (ResponseCache.getDefault()!=null);
    }
    Object o=null;
    o=Reflection.invokeStaticByName(clazz,"install",null,cacheDir,sizeInBytes);
    return (o!=null);
  }
  public void setCacheSize(long size){
    if(size<=0)
      throw new IllegalArgumentException();
    synchronized(syncRoot){
      cacheSize=Math.max(size,0);
    }
    pool.submit(runnable);
  }
  public void shutdown(){
    pool.shutdown();
    runnable=null;
  }
}
