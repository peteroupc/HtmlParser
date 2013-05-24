/*
Written in 2013 by Peter Occil.  
Any copyright is dedicated to the Public Domain.
http://creativecommons.org/publicdomain/zero/1.0/

If you like this, you should donate to Peter O.
at: http://upokecenter.com/d/
 */

package com.upokecenter.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * An object represented in BEncode, a serialization
 * format used in the BitTorrent protocol.
 * For more information, see:
 * https://wiki.theory.org/BitTorrentSpecification#bencoding
 *
 * This class accepts BEncoded strings in UTF-8, and outputs
 * BEncoded strings in UTF-8.
 */
public final class BEncodeObject {

	/**
	 *  Creates a new BEncoded object as an empty dictionary.
	 */
	public static BEncodeObject newDictionary(){
		return new BEncodeObject(new HashMap<String,BEncodeObject>());
	}

	private Object obj=null;
	public static final int TYPE_INTEGER=0;
	public static final int TYPE_STRING=1;
	public static final int TYPE_LIST=2;

	public static final int TYPE_DICTIONARY=3;

	public static BEncodeObject fromByteArray(byte[] buf){
		return fromByteArray(buf,0,buf.length);
	}

	/**
	 * Gets a BEncoded object from parsing a byte array
	 * of data in BEncoding.
	 * 
	 * @param buf
	 * @param off
	 * @param len
	 * 
	 * @throws BEncodeException if an error occurs when
	 * parsing the object.
	 */
	public static BEncodeObject fromByteArray(byte[] buf, int off, int len){
		try {
			return read(new ByteArrayInputStream(buf,off,len));
		} catch (IOException e) {
			throw new BEncodeException("Internal error",e);
		}
	}

	static long getUtf8Length(String s){
		if(s==null)throw new NullPointerException();
		long size=0;
		for(int i=0;i<s.length();i++){
			int c=s.charAt(i);
			if(c<=0x7F) {
				size++;
			} else if(c<=0x7FF) {
				size+=2;
			} else if(c<=0xD7FF || c>=0xE000) {
				size+=3;
			} else if(c<=0xDBFF){ // UTF-16 low surrogate
				i++;
				if(i>=s.length() || s.charAt(i)<0xDC00 || s.charAt(i)>0xDFFF)
					return -1;
				size+=4;
			} else
				return -1;
		}
		return size;
	}

	/**
	 *  Creates a new BEncoded object as an empty list.
	 */
	public static BEncodeObject newList(){
		return new BEncodeObject(new ArrayList<BEncodeObject>());
	}

	/**
	 * Parses a BEncoded object from an input stream.
	 * 
	 * @param stream An input stream.  This stream
	 * must support marking.
	 * @return A BEncoded object.
	 * @throws IllegalArgumentException if marking is not supported on the supplied stream.
	 * @throws IOException if an I/O error occurs.
	 */
	public static BEncodeObject read(InputStream stream) throws IOException{
		if(!stream.markSupported())throw new IllegalArgumentException();
		return new BEncodeObject(readObject(stream));
	}

	private static Map<String,BEncodeObject> readDictionary(InputStream stream) throws IOException{
		Map<String,BEncodeObject> map=new HashMap<String,BEncodeObject>();
		while(true){
			stream.mark(2);
			int c=stream.read();
			if(c=='e') {
				break;
			}
			stream.reset();
			String s=readString(stream);
			Object o=readObject(stream);
			map.put(s,new BEncodeObject(o));
		}
		return map;
	}
	private static long readInteger(InputStream stream) throws IOException{
		boolean haveHex=false;
		char[] buffer=new char[21]; // enough space for the biggest long long
		int bufOffset=0;
		boolean negative=false;
		stream.mark(2);
		if(stream.read()=='-'){
			buffer[bufOffset++]='-';
			negative=true;
		} else {
			stream.reset();
		}
		while(true){ // skip zeros
			stream.mark(2);
			int c=stream.read();
			if(c!='0'){
				if(c>=0) {
					stream.reset();
				}
				break;
			}
			haveHex=true;
		}
		while(true){
			stream.mark(2);
			int number=stream.read();
			if(number>='0' && number<='9'){
				if(bufOffset>=buffer.length)
					throw new BEncodeException(negative ? "Integer too small" : "Integer too big");
				buffer[bufOffset++]=(char)number;
				haveHex=true;
			} else if(number=='e'){
				break;
			} else {
				if(number>=0) {
					stream.reset();
				}
				throw new BEncodeException("'e' expected");
			}
		}
		if(!haveHex)
			throw new BEncodeException("Positive integer expected");
		if(bufOffset==(negative ? 1 : 0))
			return 0;
		try {
			String retstr=new String(buffer,0,bufOffset);
			return Long.parseLong(retstr);
		} catch(NumberFormatException e){
			throw new BEncodeException(negative ? "Integer too small" : "Integer too big");
		}
	}
	private static List<BEncodeObject> readList(InputStream stream) throws IOException{
		List<BEncodeObject> list=new ArrayList<BEncodeObject>();
		while(true){
			stream.mark(2);
			int c=stream.read();
			if(c=='e') {
				break;
			}
			stream.reset();
			Object o=readObject(stream);
			list.add(new BEncodeObject(o));
		}
		return list;
	}
	private static Object readObject(InputStream stream) throws IOException{
		stream.mark(2);
		int c=stream.read();
		if(c=='d')
			return readDictionary(stream);
		else if(c=='l')
			return readList(stream);
		else if(c=='i')
			return readInteger(stream);
		else if(c>='0' && c<='9'){
			stream.reset();
			return readString(stream);
		} else {
			if(c>=0) {
				stream.reset();
			}
			throw new BEncodeException("Object expected");
		}
	}

	private static int readPositiveInteger(InputStream stream) throws IOException{
		boolean haveNumber=false;
		while(true){ // skip zeros
			stream.mark(2);
			int c=stream.read();
			if(c!='0'){
				if(c>=0) {
					stream.reset();
				}
				break;
			}
			haveNumber=true;
		}
		long value=0;
		while(true){
			stream.mark(2);
			int number=stream.read();
			if(number>='0' && number<='9'){
				value=(value*10)+(number-'0');
				haveNumber=true;
			} else {
				if(number>=0) {
					stream.reset();
				}
				break;
			}
			if(value>Integer.MAX_VALUE)
				throw new BEncodeException("Integer too big");
		}
		if(!haveNumber)
			throw new BEncodeException("Positive integer expected");
		return (int)value;
	}

	private static String readString(InputStream stream) throws IOException {
		int length=readPositiveInteger(stream);
		if(stream.read()!=':')
			throw new BEncodeException("Colon expected");
		return readUtf8(stream,length);
	}
	private static String readUtf8(InputStream stream, int byteLength) throws IOException {
		StringBuilder builder=new StringBuilder();
		int cp=0;
		int bytesSeen=0;
		int bytesNeeded=0;
		int lower=0x80;
		int upper=0xBF;
		int pointer=0;
		int markedPointer=-1;
		while(pointer<byteLength || byteLength<0){
			int b=stream.read();
			if(b<0 && bytesNeeded!=0){
				bytesNeeded=0;
				throw new BEncodeException("Invalid UTF-8");
			} else if(b<0){
				if(byteLength>0 && pointer>=byteLength)
					throw new BEncodeException("Premature end of stream");
				break; // end of stream
			}
			if(byteLength>0) {
				pointer++;
			}
			if(bytesNeeded==0){
				if(b<0x80){
					builder.append((char)b);
				}
				else if(b>=0xc2 && b<=0xdf){
					stream.mark(4);
					markedPointer=pointer;
					bytesNeeded=1;
					cp=b-0xc0;
				} else if(b>=0xe0 && b<=0xef){
					stream.mark(4);
					markedPointer=pointer;
					lower=(b==0xe0) ? 0xa0 : 0x80;
					upper=(b==0xed) ? 0x9f : 0xbf;
					bytesNeeded=2;
					cp=b-0xe0;
				} else if(b>=0xf0 && b<=0xf4){
					stream.mark(4);
					markedPointer=pointer;
					lower=(b==0xf0) ? 0x90 : 0x80;
					upper=(b==0xf4) ? 0x8f : 0xbf;
					bytesNeeded=3;
					cp=b-0xf0;
				} else
					throw new BEncodeException("Invalid UTF-8");
				cp<<=(6*bytesNeeded);
				continue;
			}
			if(b<lower || b>upper){
				cp=bytesNeeded=bytesSeen=0;
				lower=0x80;
				upper=0xbf;
				stream.reset();
				pointer=markedPointer;
				throw new BEncodeException("Invalid UTF-8");
			}
			lower=0x80;
			upper=0xbf;
			bytesSeen++;
			cp+=(b-0x80)<<(6*(bytesNeeded-bytesSeen));
			stream.mark(4);
			markedPointer=pointer;
			if(bytesSeen!=bytesNeeded) {
				continue;
			}
			int ret=cp;
			cp=0;
			bytesSeen=0;
			bytesNeeded=0;
			if(ret<=0xFFFF){
				builder.append((char)ret);
			} else {
				int ch=ret-0x10000;
				int lead=ch/0x400+0xd800;
				int trail=(ch&0x3FF)+0xdc00;
				builder.append((char)lead);
				builder.append((char)trail);
			}
		}
		return builder.toString();
	}
	/**
	 * 
	 * Gets a BEncoded object with the value of the given integer.
	 * 
	 * @param value A 32-bit integer.
	 */
	public static BEncodeObject valueOf(int value){
		return new BEncodeObject((long)value);
	}
	/**
	 * 
	 * Gets a BEncoded object with the value of the given long.
	 * 
	 * @param value A 64-bit integer.
	 */
	public static BEncodeObject valueOf(long value){
		return new BEncodeObject(value);
	}
	/**
	 * Gets a BEncoded object with the value of the given string.
	 * 
	 * @param value A string.  Cannot be null.
	 */
	public static BEncodeObject valueOf(String value){
		return new BEncodeObject(value);
	}
	private static void writeInteger(long value, OutputStream stream) throws IOException {
		String value1=Long.toString(value);
		for(int i=0;i<value1.length();i++){
			int c=value1.charAt(i);
			stream.write((c&0x7F));
		}
	}
	private static void writeUtf8(String s, OutputStream stream) throws IOException{
		for(int i=0;i<s.length();i++){
			int c=s.charAt(i);
			if(c<=0x7F){
				stream.write(c);
				continue;
			} else if(c<=0x7FF){
				stream.write((0xC0|((c>>6)&0x1F)));
				stream.write((0x80|(c   &0x3F)));
				continue;
			} else if(c>=0xD800 && c<=0xDBFF){ // UTF-16 lead surrogate
				i++;
				if(i>=s.length() || s.charAt(i)<0xDC00 || s.charAt(i)>0xDFFF)
					throw new BEncodeException("invalid surrogate");
				c=0x10000+(c-0xD800)*0x400+(s.charAt(i)-0xDC00);
			} else if(c>=0xDC00 && c<=0xDFFF)
				throw new BEncodeException("invalid surrogate");
			if(c<=0xFFFF){
				stream.write((0xE0|((c>>12)&0x0F)));
				stream.write((0x80|((c>>6 )&0x3F)));
				stream.write((0x80|(c      &0x3F)));
			} else {
				stream.write((0xF0|((c>>18)&0x07)));
				stream.write((0x80|((c>>12)&0x3F)));
				stream.write((0x80|((c>>6 )&0x3F)));
				stream.write((0x80|(c      &0x3F)));
			}
		}
	}
	private BEncodeObject(){}
	private BEncodeObject(Object o){
		if(o==null)throw new IllegalArgumentException();
		this.obj=o;
	}
	public void add(BEncodeObject value){
		getList().add(value);
	}
	public void add(int value){
		getList().add(BEncodeObject.valueOf(value));
	}
	public void add(int key,BEncodeObject value){
		getList().add(key,value);
	}
	public void add(int key,int value){
		add(key,BEncodeObject.valueOf(value));
	}
	public void add(int key,long value){
		add(key,BEncodeObject.valueOf(value));
	}
	public void add(int key,String value){
		add(key,BEncodeObject.valueOf(value));
	}
	public void add(long value){
		getList().add(BEncodeObject.valueOf(value));
	}
	public void add(String value){
		getList().add(BEncodeObject.valueOf(value));
	}
	/**
	 * Creates a shallow copy of this object.
	 * For lists and dictionaries, the values of
	 * the new copy are the same as those of this
	 * object; they are not copies.
	 * 
	 * @return If this is a dictionary or list,
	 * then a new BEncoded object with the same type
	 * as this object.  If this
	 * is a string or integer, then returns this object.
	 */
	@SuppressWarnings("unchecked")
	public BEncodeObject copy(){
		BEncodeObject beo=this;
		if(beo.obj instanceof Long || beo.obj instanceof String)
			return beo; // integer and string objects are immutable
		if(beo.obj instanceof Map<?,?>){
			BEncodeObject newbeo=BEncodeObject.newDictionary();
			for(String key : ((Map<String,BEncodeObject>)beo.obj).keySet()){
				newbeo.getDictionary().put(key,
						((Map<String,BEncodeObject>)beo.obj).get(key));
			}
			return newbeo;
		}
		if(beo.obj instanceof List<?>){
			BEncodeObject newbeo=BEncodeObject.newList();
			for(BEncodeObject value : ((List<BEncodeObject>)beo.obj)){
				newbeo.getList().add(value);
			}
			return newbeo;
		}
		return null;
	}
	public BEncodeObject get(int key){
		return getList().get(key);
	}
	public BEncodeObject get(String key){
		return getDictionary().get(key);
	}
	@SuppressWarnings("unchecked")
	public Map<String,BEncodeObject> getDictionary(){
		return (Map<String,BEncodeObject>)obj;
	}
	/**
	 * Gets the integer represented by this object, if possible.
	 * 
	 * @return the 32-bit integer for this object.
	 * @throws ClassCastException if this object isn't an Integer
	 * or its value exceeds the range of int.
	 */
	public int getInteger(){
		long ret=(Long)obj;
		if(ret<Integer.MIN_VALUE || ret>Integer.MAX_VALUE)
			throw new ClassCastException();
		return (int)ret;
	}
	@SuppressWarnings("unchecked")
	public List<BEncodeObject> getList(){
		return (List<BEncodeObject>)obj;
	}
	/**
	 * Gets the long represented by this object, if possible.
	 * 
	 * @return the 64-bit integer for this object.
	 * @throws ClassCastException if this object isn't a Long.
	 */
	public long getLong(){
		return (Long)obj;
	}
	public int getObjectType(){
		if(obj instanceof Long)
			return TYPE_INTEGER;
		if(obj instanceof String)
			return TYPE_STRING;
		if(obj instanceof Map<?,?>)
			return TYPE_DICTIONARY;
		if(obj instanceof List<?>)
			return TYPE_LIST;
		throw new AssertionError();
	}
	public String getString(){
		return (String)obj;
	}
	public void put(String key,BEncodeObject value){
		getDictionary().put(key,value);
	}
	public void put(String key,int value){
		put(key,BEncodeObject.valueOf(value));
	}
	public void put(String key,long value){
		put(key,BEncodeObject.valueOf(value));
	}
	public void put(String key,String value){
		put(key,BEncodeObject.valueOf(value));
	}
	public void set(int key,BEncodeObject value){
		getList().set(key,value);
	}
	public void set(int key,int value){
		set(key,BEncodeObject.valueOf(value));
	}


	public void set(int key,long value){
		set(key,BEncodeObject.valueOf(value));
	}

	public void set(int key,String value){
		set(key,BEncodeObject.valueOf(value));
	}

	public int size(){
		if(obj instanceof Map<?,?>)
			return ((Map<?,?>)obj).size();
		else if(obj instanceof List<?>)
			return ((List<?>)obj).size();
		return 0;
	}

	public byte[] toByteArray(){
		ByteArrayOutputStream os=new ByteArrayOutputStream();
		try {
			write(os);
		} catch (IOException e) {
			throw new BEncodeException("Internal error",e);
		}
		return os.toByteArray();
	}

	@SuppressWarnings("unchecked")
	public void write(OutputStream stream) throws IOException{
		if(obj instanceof Long){
			stream.write((byte)'i');
			writeInteger((Long)obj,stream);
			stream.write((byte)'e');
		} else if(obj instanceof String){
			String s=(String)obj;
			long length=getUtf8Length(s);
			if(length<0)
				throw new BEncodeException("invalid string");
			writeInteger(length,stream);
			stream.write((byte)':');
			writeUtf8(s,stream);
		} else if(obj instanceof Map<?,?>){
			stream.write((byte)'d');
			Map<String,BEncodeObject> map=(Map<String,BEncodeObject>)obj;
			for(String key : map.keySet()){
				long length=getUtf8Length(key);
				if(length<0)
					throw new BEncodeException("invalid string");
				writeInteger(length,stream);
				stream.write((byte)':');
				writeUtf8(key,stream);
				map.get(key).write(stream);
			}
			stream.write((byte)'e');
		} else if(obj instanceof List<?>){
			stream.write((byte)'l');
			List<BEncodeObject> list=(List<BEncodeObject>)obj;
			for(BEncodeObject value : list){
				value.write(stream);
			}
			stream.write((byte)'e');
		} else
			throw new BEncodeException("unexpected object type");
	}
}

