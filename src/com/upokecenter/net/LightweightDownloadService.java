package com.upokecenter.net;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;

import com.upokecenter.util.IAction;
import com.upokecenter.util.LowPriorityExecutors;
import com.upokecenter.util.StreamUtility;

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

	public void setResultPoster(IAction<Runnable> action){
		synchronized(syncRoot){
			postResult=action;
		}
	}

	public void shutdown(){
		service.shutdown();
		pool.shutdown();
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

}
