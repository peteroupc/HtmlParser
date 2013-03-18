package com.upokecenter.net;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.upokecenter.util.StringUtility;

class HttpHeadersFromMap implements IHttpHeaders {

	Map<String,List<String>> map;
	List<String> list;
	String requestMethod;
	String urlString;

	public HttpHeadersFromMap(String urlString, String requestMethod, Map<String,List<String>> map){
		this.map=map;
		this.urlString=urlString;
		this.requestMethod=requestMethod;
		list=new ArrayList<String>();
		ArrayList<String> keyset=new ArrayList<String>();
		for(String s : this.map.keySet()){
			if(s==null){
				// Add status line (also has the side
				// effect that it will appear first in the list)
				List<String> v=this.map.get(s);
				if(v!=null && v.size()>0){
					list.add(v.get(0));
				} else {
					list.add("HTTP/1.1 200 OK");
				}
			} else {
				keyset.add(s);
			}
		}
		Collections.sort(keyset);
		// Add the remaining headers in sorted order
		for(String s : keyset){
			List<String> v=this.map.get(s);
			if(v!=null && v.size()>0){
				for(String ss : v){
					list.add(s);
					list.add(ss);
				}
			}
		}
	}

	@Override
	public String getRequestMethod() {
		return requestMethod;
	}

	@Override
	public String getHeaderField(String name) {
		if(name==null)return list.get(0);
		name=StringUtility.toLowerCaseAscii(name);
		String last=null;
		for(int i=1;i<list.size();i+=2){
			String key=list.get(i);
			if(name.equals(key)) {
				last=list.get(i+1);
			}
		}
		return last;
	}
	@Override
	public String getHeaderField(int index) {
		if(index==0)return list.get(0);
		if(index<0)return null;
		index=(index-1)*2+1+1;
		if(index<0 || index>=list.size())
			return null;
		return list.get(index+1);
	}
	@Override
	public String getHeaderFieldKey(int index) {
		if(index==0 || index<0)return null;
		index=(index-1)*2+1;
		if(index<0 || index>=list.size())
			return null;
		return list.get(index);
	}
	@Override
	public int getResponseCode() {
		String status=getHeaderField(null);
		if(status==null)return -1;
		return HeaderParser.getResponseCode(status);
	}
	@Override
	public long getHeaderFieldDate(String field, long defaultValue) {
		return HeaderParser.parseHttpDate(getHeaderField(field),defaultValue);
	}

	@Override
	public Map<String, List<String>> getHeaderFields() {
		return Collections.unmodifiableMap(map);
	}

	@Override
	public String getUrl() {
		return urlString;
	}
}