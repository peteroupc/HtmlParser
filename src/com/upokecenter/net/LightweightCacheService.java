package com.upokecenter.net;

import java.io.File;
import java.io.IOException;
import java.net.ResponseCache;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.upokecenter.util.LowPriorityExecutors;
import com.upokecenter.util.Reflection;
import com.upokecenter.util.StreamUtility;

public final class LightweightCacheService {

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
			ResponseCache legacyCache=DownloadHelper.getLegacyResponseCache(cacheDir);
			ResponseCache.setDefault(legacyCache);
			return (ResponseCache.getDefault()!=null);
		}
		Object o=null;
		o=Reflection.invokeStaticByName(clazz,"install",null,cacheDir,sizeInBytes);
		return (o!=null);
	}

	ScheduledExecutorService pool;
	volatile long cacheSize=2L*1024L*1024L;
	Object syncRoot=new Object();
	Object syncRootFilesystem=new Object();
	File filePublicPath;
	File filePrivatePath;
	Runnable runnable;

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
	public void close(){
		pool.shutdown();
		runnable=null;
	}
	public void setCacheSize(long size){
		if(size<=0)
			throw new IllegalArgumentException();
		synchronized(syncRoot){
			cacheSize=Math.max(size,0);
		}
		pool.submit(runnable);
	}
}