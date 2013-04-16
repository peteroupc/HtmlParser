/*
Written in 2013 by Peter Occil.  Released to the public domain.
Public domain dedication: http://creativecommons.org/publicdomain/zero/1.0/
 */
package com.upokecenter.json;

import java.text.ParseException;
import java.util.NoSuchElementException;

public class JSONPatch {

	private static String getString(JSONObject o, String key){
		try {
			return o.getString(key);
		} catch(NoSuchElementException e){
			return null;
		}
	}

	private static Object removeOperation(
			Object o,
			String opStr,
			String path){
		if(path==null)throw new IllegalArgumentException("patch "+opStr);
		if(path.length()==0)
			return o;
		else {
			JSONPointer pointer=JSONPointer.fromPointer(o, path);
			if(!pointer.exists())
				throw new NoSuchElementException("patch "+opStr+" "+path);
			o=pointer.getValue();
			if(pointer.getParent() instanceof JSONArray){
				((JSONArray)pointer.getParent()).removeAt(pointer.getIndex());
			} else if(pointer.getParent() instanceof JSONObject){
				((JSONObject)pointer.getParent()).remove(pointer.getKey());
			}
			return o;
		}
	}

	private static Object addOperation(
			Object o,
			String opStr,
			String path,
			Object value
			){
		if(path==null)throw new IllegalArgumentException("patch "+opStr);
		if(path.length()==0){
			o=value;
		} else {
			JSONPointer pointer=JSONPointer.fromPointer(o, path);
			if(pointer.getParent() instanceof JSONArray){
				int index=pointer.getIndex();
				if(index<0)
					throw new IllegalArgumentException("patch "+opStr+" path");
				((JSONArray)pointer.getParent()).add(index,value);
			} else if(pointer.getParent() instanceof JSONObject){
				String key=pointer.getKey();
				((JSONObject)pointer.getParent()).put(key,value);
			} else
				throw new IllegalArgumentException("patch "+opStr+" path");
		}
		return o;
	}

	private static Object replaceOperation(
			Object o,
			String opStr,
			String path,
			Object value
			){
		if(path==null)throw new IllegalArgumentException("patch "+opStr);
		if(path.length()==0){
			o=value;
		} else {
			JSONPointer pointer=JSONPointer.fromPointer(o, path);
			if(!pointer.exists())
				throw new NoSuchElementException("patch "+opStr+" "+path);
			if(pointer.getParent() instanceof JSONArray){
				int index=pointer.getIndex();
				if(index<0)
					throw new IllegalArgumentException("patch "+opStr+" path");
				((JSONArray)pointer.getParent()).put(index,value);
			} else if(pointer.getParent() instanceof JSONObject){
				String key=pointer.getKey();
				((JSONObject)pointer.getParent()).put(key,value);
			} else
				throw new IllegalArgumentException("patch "+opStr+" path");
		}
		return o;
	}

	private static Object cloneJsonObject(Object o){
		try {
			if(o instanceof JSONArray)
				return new JSONArray(o.toString());
			if(o instanceof JSONObject)
				return new JSONObject(o.toString());
		} catch(ParseException e){
			return o;
		}
		return o;
	}

	public static Object patch(Object o, JSONArray patch){
		// clone the object in case of failure
		o=cloneJsonObject(o);
		for(int i=0;i<patch.length();i++){
			Object op=patch.get(i);
			if(!(op instanceof JSONObject))
				throw new IllegalArgumentException("patch");
			if(o==null)
				throw new IllegalStateException("patch");
			JSONObject patchOp=(JSONObject)op;
			// NOTE: This algorithm requires "op" to exist
			// only once; the JSONObject, however, does not
			// allow duplicates
			String opStr=getString(patchOp,"op");
			if(opStr==null)
				throw new IllegalArgumentException("patch");
			if("add".equals(opStr)){
				// operation
				Object value=null;
				try {
					value=patchOp.get("value");
				} catch(NoSuchElementException e){
					throw new IllegalArgumentException("patch "+opStr+" value");
				}
				o=addOperation(o,opStr,getString(patchOp,"path"),value);
			} else if("replace".equals(opStr)){
				// operation
				Object value=null;
				try {
					value=patchOp.get("value");
				} catch(NoSuchElementException e){
					throw new IllegalArgumentException("patch "+opStr+" value");
				}
				o=replaceOperation(o,opStr,getString(patchOp,"path"),value);
			} else if("remove".equals(opStr)){
				// Remove operation
				String path=patchOp.getString("path");
				if(path==null)throw new IllegalArgumentException("patch "+opStr+" path");
				if(path.length()==0) {
					o=null;
				} else {
					removeOperation(o,opStr,getString(patchOp,"path"));
				}
			} else if("move".equals(opStr)){
				String path=patchOp.getString("path");
				if(path==null)throw new IllegalArgumentException("patch "+opStr+" path");
				String fromPath=patchOp.getString("from");
				if(fromPath==null)throw new IllegalArgumentException("patch "+opStr+" from");
				if(path.startsWith(fromPath))
					throw new IllegalArgumentException("patch "+opStr);
				Object movedObj=removeOperation(o,opStr,fromPath);
				o=addOperation(o,opStr,path,cloneJsonObject(movedObj));
			} else if("copy".equals(opStr)){
				String path=patchOp.getString("path");
				if(path==null)throw new IllegalArgumentException("patch "+opStr+" path");
				String fromPath=patchOp.getString("from");
				if(fromPath==null)throw new IllegalArgumentException("patch "+opStr+" from");
				JSONPointer pointer=JSONPointer.fromPointer(o, path);
				if(!pointer.exists())
					throw new NoSuchElementException("patch "+opStr+" "+fromPath);
				Object copiedObj=pointer.getValue();
				o=addOperation(o,opStr,path,cloneJsonObject(copiedObj));
			} else if("test".equals(opStr)){
				String path=patchOp.getString("path");
				if(path==null)throw new IllegalArgumentException("patch "+opStr+" path");
				Object value=null;
				try {
					value=patchOp.get("value");
				} catch(NoSuchElementException e){
					throw new IllegalArgumentException("patch "+opStr+" value");
				}
				JSONPointer pointer=JSONPointer.fromPointer(o, path);
				if(!pointer.exists())
					throw new NoSuchElementException("patch "+opStr+" "+path);
				Object testedObj=pointer.getValue();
				if((testedObj==null) ? (value!=null) : !testedObj.equals(value))
					throw new IllegalStateException("patch "+opStr);
			}
		}
		return (o==null) ? JSONObject.NULL : o;
	}
}
