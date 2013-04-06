package com.upokecenter.net;

import java.util.NoSuchElementException;

public final class JSONPointer {

	private static int readPositiveInteger(
			String str, int index, int[] result){
		boolean haveNumber=false;
		boolean haveZeros=false;
		int oldIndex=index;
		result[0]=-1;
		while(index<str.length()){ // skip zeros
			int c=str.charAt(index++);
			if(c!='0'){
				index--;
				break;
			}
			if(haveZeros){
				index--;
				return index;
			}
			haveNumber=true;
			haveZeros=true;
		}
		long value=0;
		while(index<str.length()){
			int number=str.charAt(index++);
			if(number>='0' && number<='9'){
				value=(value*10)+(number-'0');
				haveNumber=true;
				if(haveZeros)
					return oldIndex+1;
			} else {
				index--;
				break;
			}
			if(value>Integer.MAX_VALUE)
				return index-1;
		}
		if(!haveNumber)
			return index;
		result[0]=(int)value;
		return index;
	}

	private final String ref;
	private final Object jsonobj;

	private JSONPointer(Object jsonobj, String ref){
		assert ref!=null;
		this.jsonobj=jsonobj;
		this.ref=ref;
	}

	public String getKey(){
		return ref;
	}

	public Object getParent(){
		return jsonobj;
	}

	public boolean exists(){
		if(jsonobj instanceof JSONArray){
			if(ref.equals("-"))return false;
			int value=Integer.parseInt(ref);
			return (value>=0 && value<((JSONArray)jsonobj).length());
		} else if(jsonobj instanceof JSONObject)
			return ((JSONObject)jsonobj).has(ref);
		else
			return (ref.length()==0);
	}

	/**
	 * 
	 * Gets an index into the specified object, if the object
	 * is an array and is not greater than the array's length.
	 * 
	 * @return The index contained in this instance, or -1 if
	 * the object isn't a JSON array or is greater than the
	 * array's length.
	 */
	public int getIndex(){
		if(jsonobj instanceof JSONArray){
			if(ref.equals("-"))return ((JSONArray)jsonobj).length();
			int value=Integer.parseInt(ref);
			if(value<0)return -1;
			if(value>((JSONArray)jsonobj).length())return -1;
			return value;
		} else
			return -1;
	}

	public Object getValue(){
		if(ref.length()==0)
			return jsonobj;
		if(jsonobj instanceof JSONArray){
			int index=getIndex();
			if(index>=0 && index<((JSONArray)jsonobj).length())
				return ((JSONArray)jsonobj).get(index);
			else
				return null;
		} else if(jsonobj instanceof JSONObject)
			return ((JSONObject)jsonobj).get(ref);
		else
			return (ref.length()==0) ? jsonobj : null;
	}

	public static JSONPointer fromPointer(Object obj, String pointer){
		int index=0;
		if(pointer==null)throw new NullPointerException("pointer");
		if(pointer.length()==0)
			return new JSONPointer(obj,pointer);
		while(true){
			if(obj instanceof JSONArray){
				if(index>=pointer.length() || pointer.charAt(index)!='/')
					throw new IllegalArgumentException(pointer);
				index++;
				int[] value=new int[]{0};
				int newIndex=readPositiveInteger(pointer,index,value);
				if(value[0]<0){
					if(index<pointer.length() && pointer.charAt(index)=='-' &&
							(index+1==pointer.length() || pointer.charAt(index+1)=='/'))
						// Index at the end of the array
						return new JSONPointer(obj,"-");
					throw new IllegalArgumentException(pointer);
				}
				if(newIndex==pointer.length())
					return new JSONPointer(obj,pointer.substring(index));
				else {
					obj=((JSONArray)obj).get(value[0]);
					index=newIndex;
				}
				index=newIndex;
			} else if(obj instanceof JSONObject){
				if(obj.equals(JSONObject.NULL))
					throw new NoSuchElementException(pointer);
				if(index>=pointer.length() || pointer.charAt(index)!='/')
					throw new IllegalArgumentException(pointer);
				index++;
				String key=null;
				int oldIndex=index;
				boolean tilde=false;
				while(index<pointer.length()){
					int c=pointer.charAt(index);
					if(c=='/') {
						break;
					}
					if(c=='~'){
						tilde=true;
						break;
					}
					index++;
				}
				if(!tilde){
					key=pointer.substring(oldIndex,index);
				} else {
					index=oldIndex;
					StringBuilder sb=new StringBuilder();
					while(index<pointer.length()){
						int c=pointer.charAt(index);
						if(c=='/') {
							break;
						}
						if(c=='~'){
							if(index+1<pointer.length()){
								if(pointer.charAt(index+1)=='1'){
									index+=2;
									sb.append('/');
									continue;
								} else if(pointer.charAt(index+1)=='0'){
									index+=2;
									sb.append('~');
									continue;
								}
							}
							throw new IllegalArgumentException(pointer);
						} else {
							sb.append((char)c);
						}
						index++;
					}
					key=sb.toString();
				}
				if(index==pointer.length())
					return new JSONPointer(obj,key);
				else {
					obj=((JSONObject)obj).get(key);
				}
			} else
				throw new NoSuchElementException(pointer);
		}
	}

	/**
	 * 
	 * Gets the JSON object referred to by a JSON Pointer
	 * according to RFC6901.
	 * 
	 * The syntax for pointers is:
	 * <pre>
	 *  '/' KEY '/' KEY [...]
	 * </pre>
	 * where KEY represents a key into the JSON object
	 * or its sub-objects in the hierarchy. For example,
	 * <pre>
	 *  /foo/2/bar
	 * </pre>
	 * means the same as
	 * <pre>
	 *  obj['foo'][2]['bar']
	 * </pre>
	 * in JavaScript.
	 * 
	 * If "~" and "/" occur in a key, they must be escaped
	 * with "~0" and "~1", respectively, in a JSON pointer.
	 * 
	 * @param obj An object, especially a JSONObject or JSONArray
	 * @param pointer A JSON pointer according to RFC 6901.
	 * @return An object within the specified JSON object,
	 * or _obj_ if pointer is the empty string.
	 * @throws NullPointerException if the pointer is null.
	 * @throws IllegalArgumentException if the pointer is invalid
	 * @throws NoSuchElementException if there is no JSON object
	 *  at the given pointer, or if _obj_ is not of type
	 *  JSONObject or JSONArray, unless pointer is the empty string
	 */
	public static Object getObject(Object obj, String pointer){
		if(pointer==null)throw new NullPointerException("pointer");
		if(pointer.length()==0)return obj;
		return JSONPointer.fromPointer(obj,pointer).getValue();
	}
}
