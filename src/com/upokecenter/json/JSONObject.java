// Modified by Peter O. to use generics, among
// other things; also moved from org.json.  
// Still in the public domain;
// public domain dedication: http://creativecommons.org/publicdomain/zero/1.0/
package com.upokecenter.json;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;


/**
 * A JSONObject is an unordered collection of name/value pairs. Its
 * external form is a string wrapped in curly braces with colons between the
 * names and values, and commas between the values and names. The internal form
 * is an object having get() and opt() methods for accessing the values by name,
 * and put() methods for adding or replacing values by name. The values can be
 * any of these types: Boolean, JSONArray, JSONObject, Number, String, or the
 * JSONObject.NULL object.
 * <p>
 * The constructor can convert an external form string into an internal form
 * Java object. The toString() method creates an external form string.
 * <p>
 * A get() method returns a value if one can be found, and throws an exception
 * if one cannot be found. An opt() method returns a default value instead of
 * throwing an exception, and so is useful for obtaining optional values.
 * <p>
 * The generic get() and opt() methods return an object, which you can cast or
 * query for type. There are also typed get() and opt() methods that do type
 * checking and type coersion for you.
 * <p>
 * The texts produced by the toString() methods are very strict.
 * The constructors are more forgiving in the texts they will accept:
 * <ul>
 * <li>An extra <code>,</code>&nbsp;<small>(comma)</small> may appear just before the closing brace.</li>
 * <li>Strings may be quoted with <code>'</code>&nbsp;<small>(single quote)</small>.</li>
 * <li>Strings do not need to be quoted at all if they do not contain leading
 *     or trailing spaces, and if they do not contain any of these characters:
 *     <code>{ } [ ] / \ : , ' "</code></li>
 * <li>Numbers may have the 0- (octal) or 0x- (hex) prefix.</li>
 * </ul>
 * <p>
 * Public Domain 2002 JSON.org
 * @author JSON.org
 * @version 0.1
 */
public class JSONObject {

	/**
	 * JSONObject.NULL is equivalent to the value that JavaScript calls null,
	 * whereas Java's null is equivalent to the value that JavaScript calls
	 * undefined.
	 */
	private static final class Null {

		/**
		 * Make a Null object.
		 */
		public Null() {
		}


		/**
		 * There is only intended to be a single instance of the NULL object,
		 * so the clone method returns itself.
		 * @return     NULL.
		 */
		@Override
		protected final Object clone() {
			return this;
		}

		/**
		 * A Null object is equal to the null value and to itself.
		 * @param object    An object to test for nullness.
		 * @return true if the object parameter is the JSONObject.NULL object
		 *  or null.
		 */
		@Override
		public boolean equals(Object object) {
			return object == null || object == this;
		}


		@Override public int hashCode(){
			return 0;
		}


		/**
		 * Get the "null" string value.
		 * @return The string "null".
		 */
		@Override
		public String toString() {
			return "null";
		}
	}


	/**
	 * Produce a string in double quotes with backslash sequences in all the
	 * right places. (Modified so that the slash character is not escaped.)
	 * @param string A String
	 * @return  A String correctly formatted for insertion in a JSON message.
	 */
	public static String quote(String string) {
		if (string == null || string.length() == 0)
			return "\"\"";

		char         c;
		int          i;
		int          len = string.length();
		StringBuilder sb = new StringBuilder(len + 4);
		String       t;

		sb.append('"');
		for (i = 0; i < len; i += 1) {
			c = string.charAt(i);
			switch (c) {
			case '\\':
			case '"':// Peter O: '/' removed as needing escaping
				sb.append('\\');
				sb.append(c);
				break;
			case '\b':
				sb.append("\\b");
				break;
			case '\t':
				sb.append("\\t");
				break;
			case '\n':
				sb.append("\\n");
				break;
			case '\f':
				sb.append("\\f");
				break;
			case '\r':
				sb.append("\\r");
				break;
			default:
				if (c < ' ') {
					t = "000" + Integer.toHexString(c);
					sb.append("\\u" + t.substring(t.length() - 4));
				} else {
					sb.append(c);
				}
				break;
			}
		}
		sb.append('"');
		return sb.toString();
	}

	/**
	 * The hash map where the JSONObject's properties are kept.
	 */
	private final HashMap<String, Object> myHashMap;

	/**
	 * It is sometimes more convenient and less ambiguous to have a NULL
	 * object than to use Java's null value.
	 * JSONObject.NULL.equals(null) returns true.
	 * JSONObject.NULL.toString() returns "null".
	 */
	public static final Object NULL = new Null();

	/**
	 * Produce a string from a number.
	 * @param  n A Number
	 * @return A String.
	 * @exception ArithmeticException JSON can only serialize finite numbers.
	 */
	static public String numberToString(Object n) throws ArithmeticException {
		if (
				(n instanceof Float &&
						(((Float)n).isInfinite() || ((Float)n).isNaN())) ||
						(n instanceof Double &&
								(((Double)n).isInfinite() || ((Double)n).isNaN())))
			throw new ArithmeticException(
					"JSON can only serialize finite numbers.");

		// Shave off trailing zeros and decimal point, if possible.

		String s = toLowerCaseAscii(n.toString());
		if (s.indexOf('e') < 0 && s.indexOf('.') > 0) {
			while (s.endsWith("0")) {
				s = s.substring(0, s.length() - 1);
			}
			if (s.endsWith(".")) {
				s = s.substring(0, s.length() - 1);
			}
		}
		return s;
	}


	public static String toLowerCaseAscii(String s){
		if(s==null)return null;
		int len=s.length();
		char c=0;
		boolean hasUpperCase=false;
		for(int i=0;i<len;i++){
			c=s.charAt(i);
			if(c>='A' && c<='Z'){
				hasUpperCase=true;
				break;
			}
		}
		if(!hasUpperCase)
			return s;
		StringBuilder builder=new StringBuilder();
		for(int i=0;i<len;i++){
			c=s.charAt(i);
			if(c>='A' && c<='Z'){
				builder.append((char)(c+0x20));
			} else {
				builder.append(c);
			}
		}
		return builder.toString();
	}


	/**
	 * Construct an empty JSONObject.
	 */
	public JSONObject() {
		myHashMap = new HashMap<String, Object>();
	}


	private void addCommentIfAny(JSONTokener x) throws ParseException{
		if((x.getOptions()&OPTION_ADD_COMMENTS)!=0){
			// Parse and add the comment if any
			String comment=x.nextComment();
			if(comment.length()>0){
				myHashMap.put("@comment", comment);
			}
		}		
	}

	/**
	 * Construct a JSONObject from a JSONTokener.
	 * @param x A JSONTokener object containing the source string.
	 * @throws ParseException if there is a syntax error in the source string.
	 */
	public JSONObject(JSONTokener x) throws ParseException {
		this();
		int c;
		String key;
		if (x.next() == '%') {
			x.unescape();
		}
		x.back();
		if (x.nextClean() != '{')
			throw x.syntaxError("A JSONObject must begin with '{'");
		while (true) {
			addCommentIfAny(x);
			c = x.nextClean();
			switch (c) {
			case -1:
				throw x.syntaxError("A JSONObject must end with '}'");
			case '}':
				return;
			default:
				x.back();
				addCommentIfAny(x);
				key = x.nextValue().toString();
				if((x.getOptions() & OPTION_NO_DUPLICATES)!=0 &&
						myHashMap.containsKey(key)){
					throw x.syntaxError("Key already exists: "+key);
				}
				break;
			}
			addCommentIfAny(x);
			if (x.nextClean() != ':')
				throw x.syntaxError("Expected a ':' after a key");
			// NOTE: Will overwrite existing value. --Peter O.
			addCommentIfAny(x);
			myHashMap.put(key, x.nextValue());
			addCommentIfAny(x);
			switch (x.nextClean()) {
			case ',':
				addCommentIfAny(x);
				if (x.nextClean() == '}')
					return;
				x.back();
				break;
			case '}':
				return;
			default:
				throw x.syntaxError("Expected a ',' or '}'");
			}
		}
	}


	/**
	 * Construct a JSONObject from a Map.
	 * @param map A map object that can be used to initialize the contents of
	 *  the JSONObject.
	 */
	public JSONObject(Map<String, ?> map) {
		myHashMap = new HashMap<String, Object>(map);
	}


	/**
	 * No duplicates are allowed in the JSON string.
	 */
	public static final int OPTION_NO_DUPLICATES = 1;
	/**
	 * Will parse Shell-style comments (beginning with "#").
	 */
	public static final int OPTION_SHELL_COMMENTS = 2;
	/**
	 * Will add a "@comment" property to all objects with
	 * comments associated with them. Only applies to JSON
	 * objects, not JSON arrays.
	 */
	public static final int OPTION_ADD_COMMENTS = 4;


	/**
	 * Construct a JSONObject from a string.
	 *
	 * @param string    A string beginning
	 *  with <code>{</code>&nbsp;<small>(left brace)</small> and ending
	 *  with <code>}</code>&nbsp;<small>(right brace)</small>.
	 * @param option Options for parsing the string. Currently
	 * OPTION_NO_DUPLICATES, OPTION_SHELL_COMMENTS, and/or
	 * OPTION_ADD_COMMENTS.
	 *  @exception ParseException The string must be properly formatted.
	 */
	public JSONObject(String string, int options) throws ParseException {
		this(new JSONTokener(string,options));
	}

	/**
	 * Construct a JSONObject from a string.
	 *
	 * @param string    A string beginning
	 *  with <code>{</code>&nbsp;<small>(left brace)</small> and ending
	 *  with <code>}</code>&nbsp;<small>(right brace)</small>.
	 *  @exception ParseException The string must be properly formatted.
	 */
	public JSONObject(String string) throws ParseException {
		this(string,0);
	}

	/**
	 * Accumulate values under a key. It is similar to the put method except
	 * that if there is already an object stored under the key then a
	 * JSONArray is stored under the key to hold all of the accumulated values.
	 * If there is already a JSONArray, then the new value is appended to it.
	 * In contrast, the put method replaces the previous value.
	 *
	 * @param key   A key string.
	 * @param value An object to be accumulated under the key.
	 * @return this.
	 * @throws NullPointerException if the key is null
	 */
	public JSONObject accumulate(String key, Object value)
			throws NullPointerException {
		JSONArray a;
		Object o = opt(key);
		if (o == null) {
			put(key, value);
		} else if (o instanceof JSONArray) {
			a = (JSONArray)o;
			a.put(value);
		} else {
			a = new JSONArray();
			a.put(o);
			a.put(value);
			put(key, a);
		}
		return this;
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		JSONObject other = (JSONObject) obj;
		if (myHashMap == null) {
			if (other.myHashMap != null)
				return false;
		} else if (!myHashMap.equals(other.myHashMap))
			return false;
		return true;
	}


	/**
	 * Get the value object associated with a key.
	 *
	 *
	 * @param key   A key string.
	 * @return      The object associated with the key.
	 * @exception NoSuchElementException if the key is not found.
	 */
	public Object get(String key) throws NoSuchElementException {
		Object o = opt(key);
		if (o == null)
			throw new NoSuchElementException("JSONObject[" +
					quote(key) + "] not found.");
		return o;
	}


	/**
	 * Get the boolean value associated with a key.
	 * @param key   A key string.
	 * @return      The truth.
	 *
	 * @exception NoSuchElementException if the key is not found.
	 * @exception ClassCastException
	 *  if the value is not a Boolean or the String "true" or "false".
	 */
	public boolean getBoolean(String key)
			throws ClassCastException, NoSuchElementException {
		Object o = get(key);
		if (o == Boolean.FALSE || o.equals("false"))
			return false;
		else if (o == Boolean.TRUE || o.equals("true"))
			return true;
		throw new ClassCastException("JSONObject[" +
				quote(key) + "] is not a Boolean.");
	}


	/**
	 * Get the double value associated with a key.
	 * @param key   A key string.
	 * @return      The numeric value.
	 * @exception NoSuchElementException if the key is not found or
	 *  if the value is a Number object.
	 * @exception NumberFormatException if the value cannot be converted to a
	 *  number.
	 */
	public double getDouble(String key)
			throws NoSuchElementException, NumberFormatException {
		Object o = get(key);
		if (o instanceof Number)
			return ((Number)o).doubleValue();
		if (o instanceof String)
			return Double.parseDouble((String) o);
		throw new NumberFormatException("JSONObject[" +
				quote(key) + "] is not a number.");
	}


	/**
	 * Get the HashMap the holds that contents of the JSONObject.
	 * @return The getHashMap.
	 */
	 HashMap<String, Object> getHashMap() {
		return myHashMap;
	}


	/**
	 * Get the int value associated with a key.
	 * @param key   A key string.
	 * @return      The integer value.
	 * @exception NoSuchElementException if the key is not found
	 * @exception NumberFormatException if the value cannot be converted to a number.
	 */
	public int getInt(String key)
			throws NoSuchElementException, NumberFormatException {
		Object o = get(key);
		if (o instanceof Number)
			return ((Number)o).intValue();
		return (int)getDouble(key);
	}


	/**
	 * Get the JSONArray value associated with a key.
	 * @param key   A key string.
	 * @return      A JSONArray which is the value.
	 * @exception NoSuchElementException if the key is not found or
	 *  if the value is not a JSONArray.
	 */
	public JSONArray getJSONArray(String key) throws NoSuchElementException {
		Object o = get(key);
		if (o instanceof JSONArray)
			return (JSONArray)o;
		throw new NoSuchElementException("JSONObject[" +
				quote(key) + "] is not a JSONArray.");
	}


	/**
	 * Get the JSONObject value associated with a key.
	 *
	 * @param key   A key string.
	 * @return      A JSONObject which is the value.
	 * @exception NoSuchElementException if the key is not found or
	 *  if the value is not a JSONObject.
	 */
	public JSONObject getJSONObject(String key) throws NoSuchElementException {
		Object o = get(key);
		if (o instanceof JSONObject)
			return (JSONObject)o;
		throw new NoSuchElementException("JSONObject[" +
				quote(key) + "] is not a JSONObject.");
	}


	/**
	 * Get the string associated with a key.
	 * @param key   A key string.
	 * @return      A string which is the value.
	 * @exception NoSuchElementException if the key is not found.
	 */
	public String getString(String key) throws NoSuchElementException {
		return get(key).toString();
	}


	/**
	 * Determine if the JSONObject contains a specific key.
	 * @param key   A key string.
	 * @return      true if the key exists in the JSONObject.
	 */
	public boolean has(String key) {
		return myHashMap.containsKey(key);
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((myHashMap == null) ? 0 : myHashMap.hashCode());
		return result;
	}


	/**
	 * Determine if the value associated with the key is null or if there is
	 *  no value.
	 * @param key   A key string.
	 * @return      true if there is no value associated with the key or if
	 *  the value is the JSONObject.NULL object.
	 */
	public boolean isNull(String key) {
		return JSONObject.NULL.equals(opt(key));
	}


	/**
	 * Get an enumeration of the keys of the JSONObject.
	 *
	 * PeterO: Changed to Iterable
	 *
	 * @return An iterator of the keys.
	 */
	public Iterable<String> keys() {
		return myHashMap.keySet();
	}

	/**
	 * Get the number of keys stored in the JSONObject.
	 *
	 * @return The number of keys in the JSONObject.
	 */
	public int length() {
		return myHashMap.size();
	}


	/**
	 * Produce a JSONArray containing the names of the elements of this
	 * JSONObject.
	 * @return A JSONArray containing the key strings, or null if the JSONObject
	 * is empty.
	 */
	public JSONArray names() {
		JSONArray ja = new JSONArray();
		for(String key : keys()) {
			ja.put(key);
		}
		if (ja.length() == 0)
			return null;
		return ja;
	}


	/**
	 * Get an optional value associated with a key.
	 * @param key   A key string.
	 * @return      An object which is the value, or null if there is no value.
	 * @exception NullPointerException  The key must not be null.
	 */
	public Object opt(String key) throws NullPointerException {
		if (key == null)
			throw new NullPointerException("Null key");
		return myHashMap.get(key);
	}


	/**
	 * Get an optional boolean associated with a key.
	 * It returns false if there is no such key, or if the value is not
	 * Boolean.TRUE or the String "true".
	 *
	 * @param key   A key string.
	 * @return      The truth.
	 */
	public boolean optBoolean(String key) {
		return optBoolean(key, false);
	}


	/**
	 * Get an optional boolean associated with a key.
	 * It returns the defaultValue if there is no such key, or if it is not
	 * a Boolean or the String "true" or "false".
	 *
	 * @param key              A key string.
	 * @param defaultValue     The default.
	 * @return      The truth.
	 */
	public boolean optBoolean(String key, boolean defaultValue) {
		Object o = opt(key);
		if (o != null) {
			if (o == Boolean.FALSE || o.equals("false"))
				return false;
			else if (o == Boolean.TRUE || o.equals("true"))
				return true;
		}
		return defaultValue;
	}


	/**
	 * Get an optional double associated with a key,
	 * or NaN if there is no such key or if its value is not a number.
	 * If the value is a string, an attempt will be made to evaluate it as
	 * a number.
	 *
	 * @param key   A string which is the key.
	 * @return      An object which is the value.
	 */
	public double optDouble(String key)  {
		return optDouble(key, Double.NaN);
	}


	/**
	 * Get an optional double associated with a key, or the
	 * defaultValue if there is no such key or if its value is not a number.
	 * If the value is a string, an attempt will be made to evaluate it as
	 * a number.
	 *
	 * @param key   A key string.
	 * @param defaultValue     The default.
	 * @return      An object which is the value.
	 */
	public double optDouble(String key, double defaultValue)  {
		Object o = opt(key);
		if (o != null) {
			if (o instanceof Number)
				return ((Number)o).doubleValue();
			try {
				return Double.parseDouble((String) o);
			}
			catch(NumberFormatException e) {
			}
		}
		return defaultValue;
	}


	/**
	 * Get an optional int value associated with a key,
	 * or zero if there is no such key or if the value is not a number.
	 * If the value is a string, an attempt will be made to evaluate it as
	 * a number.
	 *
	 * @param key   A key string.
	 * @return      An object which is the value.
	 */
	public int optInt(String key) {
		return optInt(key, 0);
	}


	/**
	 * Get an optional int value associated with a key,
	 * or the default if there is no such key or if the value is not a number.
	 * If the value is a string, an attempt will be made to evaluate it as
	 * a number.
	 *
	 * @param key   A key string.
	 * @param defaultValue     The default.
	 * @return      An object which is the value.
	 */
	public int optInt(String key, int defaultValue) {
		Object o = opt(key);
		if (o != null) {
			if (o instanceof Number)
				return ((Number)o).intValue();
			try {
				return Integer.parseInt((String)o);
			} catch (NumberFormatException e) {
			}
		}
		return defaultValue;
	}


	/**
	 * Get an optional JSONArray associated with a key.
	 * It returns null if there is no such key, or if its value is not a
	 * JSONArray.
	 *
	 * @param key   A key string.
	 * @return      A JSONArray which is the value.
	 */
	public JSONArray optJSONArray(String key) {
		Object o = opt(key);
		if (o instanceof JSONArray)
			return (JSONArray) o;
		return null;
	}


	/**
	 * Get an optional JSONObject associated with a key.
	 * It returns null if there is no such key, or if its value is not a
	 * JSONObject.
	 *
	 * @param key   A key string.
	 * @return      A JSONObject which is the value.
	 */
	public JSONObject optJSONObject(String key) {
		Object o = opt(key);
		if (o instanceof JSONObject)
			return (JSONObject)o;
		return null;
	}


	/**
	 * Get an optional string associated with a key.
	 * It returns an empty string if there is no such key. If the value is not
	 * a string and is not null, then it is coverted to a string.
	 *
	 * @param key   A key string.
	 * @return      A string which is the value.
	 */
	public String optString(String key) {
		return optString(key, "");
	}


	/**
	 * Get an optional string associated with a key.
	 * It returns the defaultValue if there is no such key.
	 *
	 * @param key   A key string.
	 * @param defaultValue     The default.
	 * @return      A string which is the value.
	 */
	public String optString(String key, String defaultValue) {
		Object o = opt(key);
		if (o != null)
			return o.toString();
		return defaultValue;
	}


	/**
	 * Put a key/boolean pair in the JSONObject.
	 *
	 * @param key   A key string.
	 * @param value A boolean which is the value.
	 * @return this.
	 */
	public JSONObject put(String key, boolean value) {
		put(key, Boolean.valueOf(value));
		return this;
	}


	/**
	 * Put a key/double pair in the JSONObject.
	 *
	 * @param key   A key string.
	 * @param value A double which is the value.
	 * @return this.
	 */
	public JSONObject put(String key, double value) {
		put(key, Double.valueOf(value));
		return this;
	}


	/**
	 * Put a key/int pair in the JSONObject.
	 *
	 * @param key   A key string.
	 * @param value An int which is the value.
	 * @return this.
	 */
	public JSONObject put(String key, int value) {
		put(key, Integer.valueOf(value));
		return this;
	}


	/**
	 * Put a key/value pair in the JSONObject. If the value is null,
	 * then the key will be removed from the JSONObject if it is present.
	 * @param key   A key string.
	 * @param value An object which is the value. It should be of one of these
	 *  types: Boolean, Double, Integer, JSONArray, JSONObject, String, or the
	 *  JSONObject.NULL object.
	 * @return this.
	 * @exception NullPointerException The key must be non-null.
	 */
	public JSONObject put(String key, Object value) throws NullPointerException {
		if (key == null)
			throw new NullPointerException("Null key.");
		if (value != null) {
			myHashMap.put(key, value);
		} else {
			remove(key);
		}
		return this;
	}


	/**
	 * Put a key/value pair in the JSONObject, but only if the
	 * value is non-null.
	 * @param key   A key string.
	 * @param value An object which is the value. It should be of one of these
	 *  types: Boolean, Double, Integer, JSONArray, JSONObject, String, or the
	 *  JSONObject.NULL object.
	 * @return this.
	 * @exception NullPointerException The key must be non-null.
	 */
	public JSONObject putOpt(String key, Object value) throws NullPointerException {
		if (value != null) {
			put(key, value);
		}
		return this;
	}

	/**
	 * Remove a name and its value, if present.
	 * @param key The name to be removed.
	 * @return The value that was associated with the name,
	 * or null if there was no value.
	 */
	public Object remove(String key) {
		return myHashMap.remove(key);
	}

	/**
	 * Produce a JSONArray containing the values of the members of this
	 * JSONObject.
	 * @param names A JSONArray containing a list of key strings. This
	 * determines the sequence of the values in the result.
	 * @return A JSONArray of values.
	 */
	public JSONArray toJSONArray(JSONArray names) {
		if (names == null || names.length() == 0)
			return null;
		JSONArray ja = new JSONArray();
		for (int i = 0; i < names.length(); i += 1) {
			ja.put(opt(names.getString(i)));
		}
		return ja;
	}

	/**
	 * Make an JSON external form string of this JSONObject. For compactness, no
	 * unnecessary whitespace is added.
	 * <p>
	 * Warning: This method assumes that the data structure is acyclical.
	 *
	 * @return a printable, displayable, portable, transmittable
	 *  representation of the object, beginning
	 *  with <code>{</code>&nbsp;<small>(left brace)</small> and ending
	 *  with <code>}</code>&nbsp;<small>(right brace)</small>.
	 */
	@Override
	public String toString() {
		Object       o = null;
		StringBuilder sb = new StringBuilder();

		sb.append('{');
		for(String s : keys()){
			if (o != null) {
				sb.append(',');
			}
			o = myHashMap.get(s);
			if (o != null) {
				sb.append(quote(s));
				sb.append(':');
				if (o instanceof String) {
					sb.append(quote((String)o));
				} else if (o instanceof Number) {
					sb.append(numberToString(o));
				} else {
					sb.append(o.toString());
				}
			}
		}
		sb.append('}');
		return sb.toString();
	}


	/**
	 * Make a prettyprinted JSON external form string of this JSONObject.
	 * <p>
	 * Warning: This method assumes that the data structure is acyclical.
	 * @param indentFactor The number of spaces to add to each level of
	 *  indentation.
	 * @return a printable, displayable, portable, transmittable
	 *  representation of the object, beginning
	 *  with <code>{</code>&nbsp;<small>(left brace)</small> and ending
	 *  with <code>}</code>&nbsp;<small>(right brace)</small>.
	 */
	public String toString(int indentFactor) {
		return toString(indentFactor, 0);
	}


	/**
	 * Make a prettyprinted JSON string of this JSONObject.
	 * <p>
	 * Warning: This method assumes that the data structure is acyclical.
	 * @param indentFactor The number of spaces to add to each level of
	 *  indentation.
	 * @param indent The indentation of the top level.
	 * @return a printable, displayable, transmittable
	 *  representation of the object, beginning
	 *  with <code>{</code>&nbsp;<small>(left brace)</small> and ending
	 *  with <code>}</code>&nbsp;<small>(right brace)</small>.
	 */
	 String toString(int indentFactor, int indent) {
		int          i;
		String       pad = "";
		StringBuilder sb = new StringBuilder();
		indent += indentFactor;
		for (i = 0; i < indent; i += 1) {
			pad += ' ';
		}
		sb.append("{\n");
		for(String s : keys()) {
			Object o = myHashMap.get(s);
			if (o != null) {
				if (sb.length() > 2) {
					sb.append(",\n");
				}
				sb.append(pad);
				sb.append(quote(s));
				sb.append(": ");
				if (o instanceof String) {
					sb.append(quote((String)o));
				} else if (o instanceof Number) {
					sb.append(numberToString(o));
				} else if (o instanceof JSONObject) {
					sb.append(((JSONObject)o).toString(indentFactor, indent));
				} else if (o instanceof JSONArray) {
					sb.append(((JSONArray)o).toString(indentFactor, indent));
				} else {
					sb.append(o.toString());
				}
			}
		}
		sb.append('}');
		return sb.toString();
	}

	public static void main(String[] args) throws ParseException{
		String json="["+
			      "{ # foo\n\"foo-key\":\"foo-value\"},\n"+
			      "{ /* This is a\n # multiline comment.*/\n\"bar-key\":\"bar-value\"}]";
		System.out.println(json);
	    JSONArray obj=new JSONArray(json,
	            JSONObject.OPTION_SHELL_COMMENTS | // Support SHELL-style comments
	            JSONObject.OPTION_ADD_COMMENTS // Incorporate comments in the JSON object
	    );
	    System.out.println(obj); // Output the JSON object
		// Objects with comments associated with them will
		// now contain a "@comment" key; get the JSON Pointers
		// (RFC6901) to these objects and remove the "@comment" keys.
		Map<String,Object> pointers=JSONPointer.getPointersWithKeyAndRemove(obj,"@comment");
		// For each JSON Pointer, get its corresponding object.
		// They will always be JSONObjects.
		for(String pointer : pointers.keySet()){
			JSONObject subobj=(JSONObject)JSONPointer.getObject(obj,pointer);
			System.out.println(subobj); // Output the object
			System.out.println(pointers.get(pointer)); // Output the key's value
		}
	}
}