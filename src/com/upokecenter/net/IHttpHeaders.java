package com.upokecenter.net;


import java.util.List;
import java.util.Map;

public interface IHttpHeaders {
	public String getUrl();
	public String getRequestMethod();
	public String getHeaderField(String name);
	public String getHeaderField(int name);
	public String getHeaderFieldKey(int name);
	public int getResponseCode();
	public long getHeaderFieldDate(String field, long defaultValue);
	public Map<String,List<String>> getHeaderFields();
}
