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
package com.upokecenter.encoding;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.MalformedInputException;
import java.nio.charset.UnmappableCharacterException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import com.upokecenter.util.StringUtility;

/**
 * 
 * Main class for an API that converts bytes to and from
 * Unicode characters.
 * 
 * @author Peter
 *
 */
public final class TextEncoding {
	private static final class EncodingErrorReplace implements IEncodingError {
		@Override
		public int emitDecoderError(int[] buffer, int offset, int length) throws IOException {
			buffer[offset]=0xFFFD;
			return 1;
		}

		@Override
		public void emitEncoderError(OutputStream stream, int codePoint) throws IOException {
			stream.write(0x3F);
		}
	}

	private static final class EncodingErrorThrow implements IEncodingError {
		@Override
		public int emitDecoderError(int[] buffer, int offset, int length) throws IOException {
			throw new MalformedInputException(1);
		}

		@Override
		public void emitEncoderError(OutputStream stream, int codePoint) throws IOException {
			throw new UnmappableCharacterException(1);
		}
	}

	/**
	 * 
	 * An encoding error handler that throws exceptions
	 * on decoder and encoder errors.
	 * 
	 */
	public static final IEncodingError ENCODING_ERROR_THROW = new EncodingErrorThrow();

	/**
	 * 
	 * An encoding error handler that replaces ill-formed
	 * bytes with U+FFFD replacement characters, and replaces
	 * values that are not Unicode characters or Unicode characters
	 * that can't be converted to bytes with the byte 0x3F.
	 * 
	 */
	public static final IEncodingError ENCODING_ERROR_REPLACE = new EncodingErrorReplace();
	private TextEncoding(){};

	private static Map<String,String> encodingMap=new HashMap<String,String>();
	private static Map<String,ITextEncoder> indexEncodingMap=new HashMap<String,ITextEncoder>();
	static {
		encodingMap.put("unicode-1-1-utf-8","utf-8");
		encodingMap.put("utf-8","utf-8");
		encodingMap.put("utf8","utf-8");
		encodingMap.put("866","ibm866");
		encodingMap.put("cp866","ibm866");
		encodingMap.put("csibm866","ibm866");
		encodingMap.put("ibm866","ibm866");
		encodingMap.put("csisolatin2","iso-8859-2");
		encodingMap.put("iso-8859-2","iso-8859-2");
		encodingMap.put("iso-ir-101","iso-8859-2");
		encodingMap.put("iso8859-2","iso-8859-2");
		encodingMap.put("iso88592","iso-8859-2");
		encodingMap.put("iso_8859-2","iso-8859-2");
		encodingMap.put("iso_8859-2:1987","iso-8859-2");
		encodingMap.put("l2","iso-8859-2");
		encodingMap.put("latin2","iso-8859-2");
		encodingMap.put("csisolatin3","iso-8859-3");
		encodingMap.put("iso-8859-3","iso-8859-3");
		encodingMap.put("iso-ir-109","iso-8859-3");
		encodingMap.put("iso8859-3","iso-8859-3");
		encodingMap.put("iso88593","iso-8859-3");
		encodingMap.put("iso_8859-3","iso-8859-3");
		encodingMap.put("iso_8859-3:1988","iso-8859-3");
		encodingMap.put("l3","iso-8859-3");
		encodingMap.put("latin3","iso-8859-3");
		encodingMap.put("csisolatin4","iso-8859-4");
		encodingMap.put("iso-8859-4","iso-8859-4");
		encodingMap.put("iso-ir-110","iso-8859-4");
		encodingMap.put("iso8859-4","iso-8859-4");
		encodingMap.put("iso88594","iso-8859-4");
		encodingMap.put("iso_8859-4","iso-8859-4");
		encodingMap.put("iso_8859-4:1988","iso-8859-4");
		encodingMap.put("l4","iso-8859-4");
		encodingMap.put("latin4","iso-8859-4");
		encodingMap.put("csisolatincyrillic","iso-8859-5");
		encodingMap.put("cyrillic","iso-8859-5");
		encodingMap.put("iso-8859-5","iso-8859-5");
		encodingMap.put("iso-ir-144","iso-8859-5");
		encodingMap.put("iso8859-5","iso-8859-5");
		encodingMap.put("iso88595","iso-8859-5");
		encodingMap.put("iso_8859-5","iso-8859-5");
		encodingMap.put("iso_8859-5:1988","iso-8859-5");
		encodingMap.put("arabic","iso-8859-6");
		encodingMap.put("asmo-708","iso-8859-6");
		encodingMap.put("csiso88596e","iso-8859-6");
		encodingMap.put("csiso88596i","iso-8859-6");
		encodingMap.put("csisolatinarabic","iso-8859-6");
		encodingMap.put("ecma-114","iso-8859-6");
		encodingMap.put("iso-8859-6","iso-8859-6");
		encodingMap.put("iso-8859-6-e","iso-8859-6");
		encodingMap.put("iso-8859-6-i","iso-8859-6");
		encodingMap.put("iso-ir-127","iso-8859-6");
		encodingMap.put("iso8859-6","iso-8859-6");
		encodingMap.put("iso88596","iso-8859-6");
		encodingMap.put("iso_8859-6","iso-8859-6");
		encodingMap.put("iso_8859-6:1987","iso-8859-6");
		encodingMap.put("csisolatingreek","iso-8859-7");
		encodingMap.put("ecma-118","iso-8859-7");
		encodingMap.put("elot_928","iso-8859-7");
		encodingMap.put("greek","iso-8859-7");
		encodingMap.put("greek8","iso-8859-7");
		encodingMap.put("iso-8859-7","iso-8859-7");
		encodingMap.put("iso-ir-126","iso-8859-7");
		encodingMap.put("iso8859-7","iso-8859-7");
		encodingMap.put("iso88597","iso-8859-7");
		encodingMap.put("iso_8859-7","iso-8859-7");
		encodingMap.put("iso_8859-7:1987","iso-8859-7");
		encodingMap.put("sun_eu_greek","iso-8859-7");
		encodingMap.put("csiso88598e","iso-8859-8");
		encodingMap.put("csisolatinhebrew","iso-8859-8");
		encodingMap.put("hebrew","iso-8859-8");
		encodingMap.put("iso-8859-8","iso-8859-8");
		encodingMap.put("iso-8859-8-e","iso-8859-8");
		encodingMap.put("iso-ir-138","iso-8859-8");
		encodingMap.put("iso8859-8","iso-8859-8");
		encodingMap.put("iso88598","iso-8859-8");
		encodingMap.put("iso_8859-8","iso-8859-8");
		encodingMap.put("iso_8859-8:1988","iso-8859-8");
		encodingMap.put("visual","iso-8859-8");
		encodingMap.put("csiso88598i","iso-8859-8-i");
		encodingMap.put("iso-8859-8-i","iso-8859-8-i");
		encodingMap.put("logical","iso-8859-8-i");
		encodingMap.put("csisolatin6","iso-8859-10");
		encodingMap.put("iso-8859-10","iso-8859-10");
		encodingMap.put("iso-ir-157","iso-8859-10");
		encodingMap.put("iso8859-10","iso-8859-10");
		encodingMap.put("iso885910","iso-8859-10");
		encodingMap.put("l6","iso-8859-10");
		encodingMap.put("latin6","iso-8859-10");
		encodingMap.put("iso-8859-13","iso-8859-13");
		encodingMap.put("iso8859-13","iso-8859-13");
		encodingMap.put("iso885913","iso-8859-13");
		encodingMap.put("iso-8859-14","iso-8859-14");
		encodingMap.put("iso8859-14","iso-8859-14");
		encodingMap.put("iso885914","iso-8859-14");
		encodingMap.put("csisolatin9","iso-8859-15");
		encodingMap.put("iso-8859-15","iso-8859-15");
		encodingMap.put("iso8859-15","iso-8859-15");
		encodingMap.put("iso885915","iso-8859-15");
		encodingMap.put("iso_8859-15","iso-8859-15");
		encodingMap.put("l9","iso-8859-15");
		encodingMap.put("iso-8859-16","iso-8859-16");
		encodingMap.put("cskoi8r","koi8-r");
		encodingMap.put("koi","koi8-r");
		encodingMap.put("koi8","koi8-r");
		encodingMap.put("koi8-r","koi8-r");
		encodingMap.put("koi8_r","koi8-r");
		encodingMap.put("koi8-u","koi8-u");
		encodingMap.put("csmacintosh","macintosh");
		encodingMap.put("mac","macintosh");
		encodingMap.put("macintosh","macintosh");
		encodingMap.put("x-mac-roman","macintosh");
		encodingMap.put("dos-874","windows-874");
		encodingMap.put("iso-8859-11","windows-874");
		encodingMap.put("iso8859-11","windows-874");
		encodingMap.put("iso885911","windows-874");
		encodingMap.put("tis-620","windows-874");
		encodingMap.put("windows-874","windows-874");
		encodingMap.put("cp1250","windows-1250");
		encodingMap.put("windows-1250","windows-1250");
		encodingMap.put("x-cp1250","windows-1250");
		encodingMap.put("cp1251","windows-1251");
		encodingMap.put("windows-1251","windows-1251");
		encodingMap.put("x-cp1251","windows-1251");
		encodingMap.put("ansi_x3.4-1968","windows-1252");
		encodingMap.put("ascii","windows-1252");
		encodingMap.put("cp1252","windows-1252");
		encodingMap.put("cp819","windows-1252");
		encodingMap.put("csisolatin1","windows-1252");
		encodingMap.put("ibm819","windows-1252");
		encodingMap.put("iso-8859-1","windows-1252");
		encodingMap.put("iso-ir-100","windows-1252");
		encodingMap.put("iso8859-1","windows-1252");
		encodingMap.put("iso88591","windows-1252");
		encodingMap.put("iso_8859-1","windows-1252");
		encodingMap.put("iso_8859-1:1987","windows-1252");
		encodingMap.put("l1","windows-1252");
		encodingMap.put("latin1","windows-1252");
		encodingMap.put("us-ascii","windows-1252");
		encodingMap.put("windows-1252","windows-1252");
		encodingMap.put("x-cp1252","windows-1252");
		encodingMap.put("cp1253","windows-1253");
		encodingMap.put("windows-1253","windows-1253");
		encodingMap.put("x-cp1253","windows-1253");
		encodingMap.put("cp1254","windows-1254");
		encodingMap.put("csisolatin5","windows-1254");
		encodingMap.put("iso-8859-9","windows-1254");
		encodingMap.put("iso-ir-148","windows-1254");
		encodingMap.put("iso8859-9","windows-1254");
		encodingMap.put("iso88599","windows-1254");
		encodingMap.put("iso_8859-9","windows-1254");
		encodingMap.put("iso_8859-9:1989","windows-1254");
		encodingMap.put("l5","windows-1254");
		encodingMap.put("latin5","windows-1254");
		encodingMap.put("windows-1254","windows-1254");
		encodingMap.put("x-cp1254","windows-1254");
		encodingMap.put("cp1255","windows-1255");
		encodingMap.put("windows-1255","windows-1255");
		encodingMap.put("x-cp1255","windows-1255");
		encodingMap.put("cp1256","windows-1256");
		encodingMap.put("windows-1256","windows-1256");
		encodingMap.put("x-cp1256","windows-1256");
		encodingMap.put("cp1257","windows-1257");
		encodingMap.put("windows-1257","windows-1257");
		encodingMap.put("x-cp1257","windows-1257");
		encodingMap.put("cp1258","windows-1258");
		encodingMap.put("windows-1258","windows-1258");
		encodingMap.put("x-cp1258","windows-1258");
		encodingMap.put("x-mac-cyrillic","x-mac-cyrillic");
		encodingMap.put("x-mac-ukrainian","x-mac-cyrillic");
		encodingMap.put("chinese","gbk");
		encodingMap.put("csgb2312","gbk");
		encodingMap.put("csiso58gb231280","gbk");
		encodingMap.put("gb2312","gbk");
		encodingMap.put("gb_2312","gbk");
		encodingMap.put("gb_2312-80","gbk");
		encodingMap.put("gbk","gbk");
		encodingMap.put("iso-ir-58","gbk");
		encodingMap.put("x-gbk","gbk");
		encodingMap.put("gb18030","gb18030");
		encodingMap.put("hz-gb-2312","hz-gb-2312");
		encodingMap.put("big5","big5");
		encodingMap.put("big5-hkscs","big5");
		encodingMap.put("cn-big5","big5");
		encodingMap.put("csbig5","big5");
		encodingMap.put("x-x-big5","big5");
		encodingMap.put("cseucpkdfmtjapanese","euc-jp");
		encodingMap.put("euc-jp","euc-jp");
		encodingMap.put("x-euc-jp","euc-jp");
		encodingMap.put("csiso2022jp","iso-2022-jp");
		encodingMap.put("iso-2022-jp","iso-2022-jp");
		encodingMap.put("csshiftjis","shift_jis");
		encodingMap.put("ms_kanji","shift_jis");
		encodingMap.put("shift-jis","shift_jis");
		encodingMap.put("shift_jis","shift_jis");
		encodingMap.put("sjis","shift_jis");
		encodingMap.put("windows-31j","shift_jis");
		encodingMap.put("x-sjis","shift_jis");
		encodingMap.put("cseuckr","euc-kr");
		encodingMap.put("csksc56011987","euc-kr");
		encodingMap.put("euc-kr","euc-kr");
		encodingMap.put("iso-ir-149","euc-kr");
		encodingMap.put("korean","euc-kr");
		encodingMap.put("ks_c_5601-1987","euc-kr");
		encodingMap.put("ks_c_5601-1989","euc-kr");
		encodingMap.put("ksc5601","euc-kr");
		encodingMap.put("ksc_5601","euc-kr");
		encodingMap.put("windows-949","euc-kr");
		encodingMap.put("csiso2022kr","iso-2022-kr");
		encodingMap.put("iso-2022-kr","iso-2022-kr");
		encodingMap.put("iso-2022-cn","replacement");
		encodingMap.put("iso-2022-cn-ext","replacement");
		encodingMap.put("utf-16be","utf-16be");
		encodingMap.put("utf-16","utf-16le");
		encodingMap.put("utf-16le","utf-16le");
		encodingMap.put("x-user-defined","x-user-defined");
	}

	private static Object syncRoot=new Object();

	/**
	 * Converts a name to a supported character encoding
	 * 
	 * @param encoding the name of an encoding
	 * @return a character encoding, or null if the name
	 * does not resolve to a supported encoding
	 */
	public static String resolveEncoding(String encoding){
		if(encoding==null)return null;
		int index=0;
		int length=encoding.length();
		while(index<length){
			char c=encoding.charAt(index);
			if(c!=0x09 && c!=0x0c && c!=0x0d && c!=0x0a && c!=0x20) {
				break;
			}
			index++;
		}
		int lastIndex=length-1;
		while(lastIndex>=0){
			char c=encoding.charAt(lastIndex);
			if(c!=0x09 && c!=0x0c && c!=0x0d && c!=0x0a && c!=0x20) {
				break;
			}
			lastIndex--;
		}
		encoding=StringUtility.toLowerCaseAscii(encoding.substring(index,lastIndex+1));
		if(encodingMap.get(encoding)!=null)
			return encodingMap.get(encoding);
		return null;
	}

	/**
	 * Utility method to decode an input byte stream into a string.
	 * 
	 * @param input
	 * @param decoder
	 * @param error
	 * 
	 * @throws IOException
	 */
	public static String decodeString(
			InputStream input, ITextDecoder decoder, IEncodingError error)
					throws IOException {
		if(decoder==null || input==null || error==null)
			throw new IllegalArgumentException();
		int[] data=new int[64];
		StringBuilder builder=new StringBuilder();
		while(true){
			int count=decoder.decode(input,data,0,data.length,error);
			if(count<0) {
				break;
			}
			for(int i=0;i<count;i++){
				if(data[i]<=0xFFFF){
					builder.append((char)data[i]);
				} else {
					int ch=data[i]-0x10000;
					int lead=ch/0x400+0xd800;
					int trail=(ch&0x3FF)+0xdc00;
					builder.append((char)lead);
					builder.append((char)trail);
				}
			}
		}
		return builder.toString();
	}

	/**
	 * 
	 * Utility method to write a string to an output byte stream.
	 * 
	 * @param str String. If null, throws IllegalArgumentException.
	 * Any unpaired surrogates in the string are kept intact in the
	 * input to the encoder.
	 * @param output
	 * @param encoder Encoder for converting Unicode characters
	 * to bytes
	 * @param error Error handler called when a Unicode character
	 * cannot be converted to bytes
	 * @throws IOException if the error handler throws an exception
	 * or another I/O error occurs
	 */
	public static void encodeString(
			String str, OutputStream output,
			ITextEncoder encoder, IEncodingError error) throws IOException{
		if(str==null || encoder==null || output==null || error==null)
			throw new IllegalArgumentException();
		int[] data=new int[1];
		int length=str.length();
		for(int i=0;i<length;i++){
			int c=str.charAt(i);
			if(c>=0xD800 && c<=0xDBFF && i+1<length &&
					str.charAt(i+1)>=0xDC00 && str.charAt(i+1)<=0xDFFF){
				// Get the Unicode code point for the surrogate pair
				c=0x10000+(c-0xD800)*0x400+(str.charAt(i+1)-0xDC00);
				i++;
			}
			data[0]=c;
			encoder.encode(output,data,0,1,error);
		}
	}

	private static ITextEncoder getIndexEncoding(String name){
		synchronized(syncRoot){
			ITextEncoder encoder=indexEncodingMap.get(name);
			if(encoder!=null)return encoder;
		}
		if(name.equals("x-user-defined"))return setIndexEncoding(name,new XUserDefinedEncoding());
		else if(name.equals("ibm866"))return setIndexEncoding(name,new SingleByteEncoding(new int[]{1040,1041,1042,1043,1044,1045,1046,1047,1048,1049,1050,1051,1052,1053,1054,1055,1056,1057,1058,1059,1060,1061,1062,1063,1064,1065,1066,1067,1068,1069,1070,1071,1072,1073,1074,1075,1076,1077,1078,1079,1080,1081,1082,1083,1084,1085,1086,1087,9617,9618,9619,9474,9508,9569,9570,9558,9557,9571,9553,9559,9565,9564,9563,9488,9492,9524,9516,9500,9472,9532,9566,9567,9562,9556,9577,9574,9568,9552,9580,9575,9576,9572,9573,9561,9560,9554,9555,9579,9578,9496,9484,9608,9604,9612,9616,9600,1088,1089,1090,1091,1092,1093,1094,1095,1096,1097,1098,1099,1100,1101,1102,1103,1025,1105,1028,1108,1031,1111,1038,1118,176,8729,183,8730,8470,164,9632,160}));
		else if(name.equals("iso-8859-10"))return setIndexEncoding(name,new SingleByteEncoding(new int[]{128,129,130,131,132,133,134,135,136,137,138,139,140,141,142,143,144,145,146,147,148,149,150,151,152,153,154,155,156,157,158,159,160,260,274,290,298,296,310,167,315,272,352,358,381,173,362,330,176,261,275,291,299,297,311,183,316,273,353,359,382,8213,363,331,256,193,194,195,196,197,198,302,268,201,280,203,278,205,206,207,208,325,332,211,212,213,214,360,216,370,218,219,220,221,222,223,257,225,226,227,228,229,230,303,269,233,281,235,279,237,238,239,240,326,333,243,244,245,246,361,248,371,250,251,252,253,254,312}));
		else if(name.equals("iso-8859-13"))return setIndexEncoding(name,new SingleByteEncoding(new int[]{128,129,130,131,132,133,134,135,136,137,138,139,140,141,142,143,144,145,146,147,148,149,150,151,152,153,154,155,156,157,158,159,160,8221,162,163,164,8222,166,167,216,169,342,171,172,173,174,198,176,177,178,179,8220,181,182,183,248,185,343,187,188,189,190,230,260,302,256,262,196,197,280,274,268,201,377,278,290,310,298,315,352,323,325,211,332,213,214,215,370,321,346,362,220,379,381,223,261,303,257,263,228,229,281,275,269,233,378,279,291,311,299,316,353,324,326,243,333,245,246,247,371,322,347,363,252,380,382,8217}));
		else if(name.equals("iso-8859-14"))return setIndexEncoding(name,new SingleByteEncoding(new int[]{128,129,130,131,132,133,134,135,136,137,138,139,140,141,142,143,144,145,146,147,148,149,150,151,152,153,154,155,156,157,158,159,160,7682,7683,163,266,267,7690,167,7808,169,7810,7691,7922,173,174,376,7710,7711,288,289,7744,7745,182,7766,7809,7767,7811,7776,7923,7812,7813,7777,192,193,194,195,196,197,198,199,200,201,202,203,204,205,206,207,372,209,210,211,212,213,214,7786,216,217,218,219,220,221,374,223,224,225,226,227,228,229,230,231,232,233,234,235,236,237,238,239,373,241,242,243,244,245,246,7787,248,249,250,251,252,253,375,255}));
		else if(name.equals("iso-8859-15"))return setIndexEncoding(name,new SingleByteEncoding(new int[]{128,129,130,131,132,133,134,135,136,137,138,139,140,141,142,143,144,145,146,147,148,149,150,151,152,153,154,155,156,157,158,159,160,161,162,163,8364,165,352,167,353,169,170,171,172,173,174,175,176,177,178,179,381,181,182,183,382,185,186,187,338,339,376,191,192,193,194,195,196,197,198,199,200,201,202,203,204,205,206,207,208,209,210,211,212,213,214,215,216,217,218,219,220,221,222,223,224,225,226,227,228,229,230,231,232,233,234,235,236,237,238,239,240,241,242,243,244,245,246,247,248,249,250,251,252,253,254,255}));
		else if(name.equals("iso-8859-16"))return setIndexEncoding(name,new SingleByteEncoding(new int[]{128,129,130,131,132,133,134,135,136,137,138,139,140,141,142,143,144,145,146,147,148,149,150,151,152,153,154,155,156,157,158,159,160,260,261,321,8364,8222,352,167,353,169,536,171,377,173,378,379,176,177,268,322,381,8221,182,183,382,269,537,187,338,339,376,380,192,193,194,258,196,262,198,199,200,201,202,203,204,205,206,207,272,323,210,211,212,336,214,346,368,217,218,219,220,280,538,223,224,225,226,259,228,263,230,231,232,233,234,235,236,237,238,239,273,324,242,243,244,337,246,347,369,249,250,251,252,281,539,255}));
		else if(name.equals("iso-8859-2"))return setIndexEncoding(name,new SingleByteEncoding(new int[]{128,129,130,131,132,133,134,135,136,137,138,139,140,141,142,143,144,145,146,147,148,149,150,151,152,153,154,155,156,157,158,159,160,260,728,321,164,317,346,167,168,352,350,356,377,173,381,379,176,261,731,322,180,318,347,711,184,353,351,357,378,733,382,380,340,193,194,258,196,313,262,199,268,201,280,203,282,205,206,270,272,323,327,211,212,336,214,215,344,366,218,368,220,221,354,223,341,225,226,259,228,314,263,231,269,233,281,235,283,237,238,271,273,324,328,243,244,337,246,247,345,367,250,369,252,253,355,729}));
		else if(name.equals("iso-8859-3"))return setIndexEncoding(name,new SingleByteEncoding(new int[]{128,129,130,131,132,133,134,135,136,137,138,139,140,141,142,143,144,145,146,147,148,149,150,151,152,153,154,155,156,157,158,159,160,294,728,163,164,0,292,167,168,304,350,286,308,173,0,379,176,295,178,179,180,181,293,183,184,305,351,287,309,189,0,380,192,193,194,0,196,266,264,199,200,201,202,203,204,205,206,207,0,209,210,211,212,288,214,215,284,217,218,219,220,364,348,223,224,225,226,0,228,267,265,231,232,233,234,235,236,237,238,239,0,241,242,243,244,289,246,247,285,249,250,251,252,365,349,729}));
		else if(name.equals("iso-8859-4"))return setIndexEncoding(name,new SingleByteEncoding(new int[]{128,129,130,131,132,133,134,135,136,137,138,139,140,141,142,143,144,145,146,147,148,149,150,151,152,153,154,155,156,157,158,159,160,260,312,342,164,296,315,167,168,352,274,290,358,173,381,175,176,261,731,343,180,297,316,711,184,353,275,291,359,330,382,331,256,193,194,195,196,197,198,302,268,201,280,203,278,205,206,298,272,325,332,310,212,213,214,215,216,370,218,219,220,360,362,223,257,225,226,227,228,229,230,303,269,233,281,235,279,237,238,299,273,326,333,311,244,245,246,247,248,371,250,251,252,361,363,729}));
		else if(name.equals("iso-8859-5"))return setIndexEncoding(name,new SingleByteEncoding(new int[]{128,129,130,131,132,133,134,135,136,137,138,139,140,141,142,143,144,145,146,147,148,149,150,151,152,153,154,155,156,157,158,159,160,1025,1026,1027,1028,1029,1030,1031,1032,1033,1034,1035,1036,173,1038,1039,1040,1041,1042,1043,1044,1045,1046,1047,1048,1049,1050,1051,1052,1053,1054,1055,1056,1057,1058,1059,1060,1061,1062,1063,1064,1065,1066,1067,1068,1069,1070,1071,1072,1073,1074,1075,1076,1077,1078,1079,1080,1081,1082,1083,1084,1085,1086,1087,1088,1089,1090,1091,1092,1093,1094,1095,1096,1097,1098,1099,1100,1101,1102,1103,8470,1105,1106,1107,1108,1109,1110,1111,1112,1113,1114,1115,1116,167,1118,1119}));
		else if(name.equals("iso-8859-6"))return setIndexEncoding(name,new SingleByteEncoding(new int[]{128,129,130,131,132,133,134,135,136,137,138,139,140,141,142,143,144,145,146,147,148,149,150,151,152,153,154,155,156,157,158,159,160,0,0,0,164,0,0,0,0,0,0,0,1548,173,0,0,0,0,0,0,0,0,0,0,0,0,0,1563,0,0,0,1567,0,1569,1570,1571,1572,1573,1574,1575,1576,1577,1578,1579,1580,1581,1582,1583,1584,1585,1586,1587,1588,1589,1590,1591,1592,1593,1594,0,0,0,0,0,1600,1601,1602,1603,1604,1605,1606,1607,1608,1609,1610,1611,1612,1613,1614,1615,1616,1617,1618,0,0,0,0,0,0,0,0,0,0,0,0,0}));
		else if(name.equals("iso-8859-7"))return setIndexEncoding(name,new SingleByteEncoding(new int[]{128,129,130,131,132,133,134,135,136,137,138,139,140,141,142,143,144,145,146,147,148,149,150,151,152,153,154,155,156,157,158,159,160,8216,8217,163,8364,8367,166,167,168,169,890,171,172,173,0,8213,176,177,178,179,900,901,902,183,904,905,906,187,908,189,910,911,912,913,914,915,916,917,918,919,920,921,922,923,924,925,926,927,928,929,0,931,932,933,934,935,936,937,938,939,940,941,942,943,944,945,946,947,948,949,950,951,952,953,954,955,956,957,958,959,960,961,962,963,964,965,966,967,968,969,970,971,972,973,974,0}));
		else if(name.equals("iso-8859-8-i"))return setIndexEncoding(name,new SingleByteEncoding(new int[]{128,129,130,131,132,133,134,135,136,137,138,139,140,141,142,143,144,145,146,147,148,149,150,151,152,153,154,155,156,157,158,159,160,0,162,163,164,165,166,167,168,169,215,171,172,173,174,175,176,177,178,179,180,181,182,183,184,185,247,187,188,189,190,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,8215,1488,1489,1490,1491,1492,1493,1494,1495,1496,1497,1498,1499,1500,1501,1502,1503,1504,1505,1506,1507,1508,1509,1510,1511,1512,1513,1514,0,0,8206,8207,0}));
		else if(name.equals("iso-8859-8"))return setIndexEncoding(name,new SingleByteEncoding(new int[]{128,129,130,131,132,133,134,135,136,137,138,139,140,141,142,143,144,145,146,147,148,149,150,151,152,153,154,155,156,157,158,159,160,0,162,163,164,165,166,167,168,169,215,171,172,173,174,175,176,177,178,179,180,181,182,183,184,185,247,187,188,189,190,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,8215,1488,1489,1490,1491,1492,1493,1494,1495,1496,1497,1498,1499,1500,1501,1502,1503,1504,1505,1506,1507,1508,1509,1510,1511,1512,1513,1514,0,0,8206,8207,0}));
		else if(name.equals("koi8-r"))return setIndexEncoding(name,new SingleByteEncoding(new int[]{9472,9474,9484,9488,9492,9496,9500,9508,9516,9524,9532,9600,9604,9608,9612,9616,9617,9618,9619,8992,9632,8729,8730,8776,8804,8805,160,8993,176,178,183,247,9552,9553,9554,1105,9555,9556,9557,9558,9559,9560,9561,9562,9563,9564,9565,9566,9567,9568,9569,1025,9570,9571,9572,9573,9574,9575,9576,9577,9578,9579,9580,169,1102,1072,1073,1094,1076,1077,1092,1075,1093,1080,1081,1082,1083,1084,1085,1086,1087,1103,1088,1089,1090,1091,1078,1074,1100,1099,1079,1096,1101,1097,1095,1098,1070,1040,1041,1062,1044,1045,1060,1043,1061,1048,1049,1050,1051,1052,1053,1054,1055,1071,1056,1057,1058,1059,1046,1042,1068,1067,1047,1064,1069,1065,1063,1066}));
		else if(name.equals("koi8-u"))return setIndexEncoding(name,new SingleByteEncoding(new int[]{9472,9474,9484,9488,9492,9496,9500,9508,9516,9524,9532,9600,9604,9608,9612,9616,9617,9618,9619,8992,9632,8729,8730,8776,8804,8805,160,8993,176,178,183,247,9552,9553,9554,1105,1108,9556,1110,1111,9559,9560,9561,9562,9563,1169,9565,9566,9567,9568,9569,1025,1028,9571,1030,1031,9574,9575,9576,9577,9578,1168,9580,169,1102,1072,1073,1094,1076,1077,1092,1075,1093,1080,1081,1082,1083,1084,1085,1086,1087,1103,1088,1089,1090,1091,1078,1074,1100,1099,1079,1096,1101,1097,1095,1098,1070,1040,1041,1062,1044,1045,1060,1043,1061,1048,1049,1050,1051,1052,1053,1054,1055,1071,1056,1057,1058,1059,1046,1042,1068,1067,1047,1064,1069,1065,1063,1066}));
		else if(name.equals("macintosh"))return setIndexEncoding(name,new SingleByteEncoding(new int[]{196,197,199,201,209,214,220,225,224,226,228,227,229,231,233,232,234,235,237,236,238,239,241,243,242,244,246,245,250,249,251,252,8224,176,162,163,167,8226,182,223,174,169,8482,180,168,8800,198,216,8734,177,8804,8805,165,181,8706,8721,8719,960,8747,170,186,937,230,248,191,161,172,8730,402,8776,8710,171,187,8230,160,192,195,213,338,339,8211,8212,8220,8221,8216,8217,247,9674,255,376,8260,8364,8249,8250,64257,64258,8225,183,8218,8222,8240,194,202,193,203,200,205,206,207,204,211,212,63743,210,218,219,217,305,710,732,175,728,729,730,184,733,731,711}));
		else if(name.equals("windows-1250"))return setIndexEncoding(name,new SingleByteEncoding(new int[]{8364,129,8218,131,8222,8230,8224,8225,136,8240,352,8249,346,356,381,377,144,8216,8217,8220,8221,8226,8211,8212,152,8482,353,8250,347,357,382,378,160,711,728,321,164,260,166,167,168,169,350,171,172,173,174,379,176,177,731,322,180,181,182,183,184,261,351,187,317,733,318,380,340,193,194,258,196,313,262,199,268,201,280,203,282,205,206,270,272,323,327,211,212,336,214,215,344,366,218,368,220,221,354,223,341,225,226,259,228,314,263,231,269,233,281,235,283,237,238,271,273,324,328,243,244,337,246,247,345,367,250,369,252,253,355,729}));
		else if(name.equals("windows-1251"))return setIndexEncoding(name,new SingleByteEncoding(new int[]{1026,1027,8218,1107,8222,8230,8224,8225,8364,8240,1033,8249,1034,1036,1035,1039,1106,8216,8217,8220,8221,8226,8211,8212,152,8482,1113,8250,1114,1116,1115,1119,160,1038,1118,1032,164,1168,166,167,1025,169,1028,171,172,173,174,1031,176,177,1030,1110,1169,181,182,183,1105,8470,1108,187,1112,1029,1109,1111,1040,1041,1042,1043,1044,1045,1046,1047,1048,1049,1050,1051,1052,1053,1054,1055,1056,1057,1058,1059,1060,1061,1062,1063,1064,1065,1066,1067,1068,1069,1070,1071,1072,1073,1074,1075,1076,1077,1078,1079,1080,1081,1082,1083,1084,1085,1086,1087,1088,1089,1090,1091,1092,1093,1094,1095,1096,1097,1098,1099,1100,1101,1102,1103}));
		else if(name.equals("windows-1252"))return setIndexEncoding(name,new SingleByteEncoding(new int[]{8364,129,8218,402,8222,8230,8224,8225,710,8240,352,8249,338,141,381,143,144,8216,8217,8220,8221,8226,8211,8212,732,8482,353,8250,339,157,382,376,160,161,162,163,164,165,166,167,168,169,170,171,172,173,174,175,176,177,178,179,180,181,182,183,184,185,186,187,188,189,190,191,192,193,194,195,196,197,198,199,200,201,202,203,204,205,206,207,208,209,210,211,212,213,214,215,216,217,218,219,220,221,222,223,224,225,226,227,228,229,230,231,232,233,234,235,236,237,238,239,240,241,242,243,244,245,246,247,248,249,250,251,252,253,254,255}));
		else if(name.equals("windows-1253"))return setIndexEncoding(name,new SingleByteEncoding(new int[]{8364,129,8218,402,8222,8230,8224,8225,136,8240,138,8249,140,141,142,143,144,8216,8217,8220,8221,8226,8211,8212,152,8482,154,8250,156,157,158,159,160,901,902,163,164,165,166,167,168,169,0,171,172,173,174,8213,176,177,178,179,900,181,182,183,904,905,906,187,908,189,910,911,912,913,914,915,916,917,918,919,920,921,922,923,924,925,926,927,928,929,0,931,932,933,934,935,936,937,938,939,940,941,942,943,944,945,946,947,948,949,950,951,952,953,954,955,956,957,958,959,960,961,962,963,964,965,966,967,968,969,970,971,972,973,974,0}));
		else if(name.equals("windows-1254"))return setIndexEncoding(name,new SingleByteEncoding(new int[]{8364,129,8218,402,8222,8230,8224,8225,710,8240,352,8249,338,141,142,143,144,8216,8217,8220,8221,8226,8211,8212,732,8482,353,8250,339,157,158,376,160,161,162,163,164,165,166,167,168,169,170,171,172,173,174,175,176,177,178,179,180,181,182,183,184,185,186,187,188,189,190,191,192,193,194,195,196,197,198,199,200,201,202,203,204,205,206,207,286,209,210,211,212,213,214,215,216,217,218,219,220,304,350,223,224,225,226,227,228,229,230,231,232,233,234,235,236,237,238,239,287,241,242,243,244,245,246,247,248,249,250,251,252,305,351,255}));
		else if(name.equals("windows-1255"))return setIndexEncoding(name,new SingleByteEncoding(new int[]{8364,129,8218,402,8222,8230,8224,8225,710,8240,138,8249,140,141,142,143,144,8216,8217,8220,8221,8226,8211,8212,732,8482,154,8250,156,157,158,159,160,161,162,163,8362,165,166,167,168,169,215,171,172,173,174,175,176,177,178,179,180,181,182,183,184,185,247,187,188,189,190,191,1456,1457,1458,1459,1460,1461,1462,1463,1464,1465,0,1467,1468,1469,1470,1471,1472,1473,1474,1475,1520,1521,1522,1523,1524,0,0,0,0,0,0,0,1488,1489,1490,1491,1492,1493,1494,1495,1496,1497,1498,1499,1500,1501,1502,1503,1504,1505,1506,1507,1508,1509,1510,1511,1512,1513,1514,0,0,8206,8207,0}));
		else if(name.equals("windows-1256"))return setIndexEncoding(name,new SingleByteEncoding(new int[]{8364,1662,8218,402,8222,8230,8224,8225,710,8240,1657,8249,338,1670,1688,1672,1711,8216,8217,8220,8221,8226,8211,8212,1705,8482,1681,8250,339,8204,8205,1722,160,1548,162,163,164,165,166,167,168,169,1726,171,172,173,174,175,176,177,178,179,180,181,182,183,184,185,1563,187,188,189,190,1567,1729,1569,1570,1571,1572,1573,1574,1575,1576,1577,1578,1579,1580,1581,1582,1583,1584,1585,1586,1587,1588,1589,1590,215,1591,1592,1593,1594,1600,1601,1602,1603,224,1604,226,1605,1606,1607,1608,231,232,233,234,235,1609,1610,238,239,1611,1612,1613,1614,244,1615,1616,247,1617,249,1618,251,252,8206,8207,1746}));
		else if(name.equals("windows-1257"))return setIndexEncoding(name,new SingleByteEncoding(new int[]{8364,129,8218,131,8222,8230,8224,8225,136,8240,138,8249,140,168,711,184,144,8216,8217,8220,8221,8226,8211,8212,152,8482,154,8250,156,175,731,159,160,0,162,163,164,0,166,167,216,169,342,171,172,173,174,198,176,177,178,179,180,181,182,183,248,185,343,187,188,189,190,230,260,302,256,262,196,197,280,274,268,201,377,278,290,310,298,315,352,323,325,211,332,213,214,215,370,321,346,362,220,379,381,223,261,303,257,263,228,229,281,275,269,233,378,279,291,311,299,316,353,324,326,243,333,245,246,247,371,322,347,363,252,380,382,729}));
		else if(name.equals("windows-1258"))return setIndexEncoding(name,new SingleByteEncoding(new int[]{8364,129,8218,402,8222,8230,8224,8225,710,8240,138,8249,338,141,142,143,144,8216,8217,8220,8221,8226,8211,8212,732,8482,154,8250,339,157,158,376,160,161,162,163,164,165,166,167,168,169,170,171,172,173,174,175,176,177,178,179,180,181,182,183,184,185,186,187,188,189,190,191,192,193,194,258,196,197,198,199,200,201,202,203,768,205,206,207,272,209,777,211,212,416,214,215,216,217,218,219,220,431,771,223,224,225,226,259,228,229,230,231,232,233,234,235,769,237,238,239,273,241,803,243,244,417,246,247,248,249,250,251,252,432,8363,255}));
		else if(name.equals("windows-874"))return setIndexEncoding(name,new SingleByteEncoding(new int[]{8364,129,130,131,132,8230,134,135,136,137,138,139,140,141,142,143,144,8216,8217,8220,8221,8226,8211,8212,152,153,154,155,156,157,158,159,160,3585,3586,3587,3588,3589,3590,3591,3592,3593,3594,3595,3596,3597,3598,3599,3600,3601,3602,3603,3604,3605,3606,3607,3608,3609,3610,3611,3612,3613,3614,3615,3616,3617,3618,3619,3620,3621,3622,3623,3624,3625,3626,3627,3628,3629,3630,3631,3632,3633,3634,3635,3636,3637,3638,3639,3640,3641,3642,0,0,0,0,3647,3648,3649,3650,3651,3652,3653,3654,3655,3656,3657,3658,3659,3660,3661,3662,3663,3664,3665,3666,3667,3668,3669,3670,3671,3672,3673,3674,3675,0,0,0,0}));
		else if(name.equals("x-mac-cyrillic"))return setIndexEncoding(name,new SingleByteEncoding(new int[]{1040,1041,1042,1043,1044,1045,1046,1047,1048,1049,1050,1051,1052,1053,1054,1055,1056,1057,1058,1059,1060,1061,1062,1063,1064,1065,1066,1067,1068,1069,1070,1071,8224,176,1168,163,167,8226,182,1030,174,169,8482,1026,1106,8800,1027,1107,8734,177,8804,8805,1110,181,1169,1032,1028,1108,1031,1111,1033,1113,1034,1114,1112,1029,172,8730,402,8776,8710,171,187,8230,160,1035,1115,1036,1116,1109,8211,8212,8220,8221,8216,8217,247,8222,1038,1118,1039,1119,8470,1025,1105,1103,1072,1073,1074,1075,1076,1077,1078,1079,1080,1081,1082,1083,1084,1085,1086,1087,1088,1089,1090,1091,1092,1093,1094,1095,1096,1097,1098,1099,1100,1101,1102,8364}));
		return null;
	}

	/**
	 * Returns a list of all character encodings supported
	 * by this implementation.
	 */
	public static String[] getSupportedEncodings(){
		ArrayList<String> values=new ArrayList<String>(new HashSet<String>(encodingMap.values()));
		Collections.sort(values);
		return values.toArray(new String[]{});
	}

	private static ITextEncoder setIndexEncoding(String name, ITextEncoder enc){
		synchronized(syncRoot){
			indexEncodingMap.put(name,enc);
		}
		return enc;
	}

	/**
	 * Gets a character encoder for a given character
	 * encoding.
	 * @param name a name of a character encoding
	 * @return a character encoder, or null if the name
	 * does not resolve to a supported encoding, or if
	 * no encoder is supported for that encoding.
	 */
	public static ITextEncoder getEncoder(String name){
		name=resolveEncoding(name);
		if(name==null)
			return null;
		ITextEncoder encoder=getIndexEncoding(name);
		if(encoder!=null)return encoder;
		if(name.equals("utf-8") || name.equals("replacement"))
			return new Utf8Encoding();
		if(name.equals("utf-16le"))
			return new Utf16Encoding(false);
		if(name.equals("utf-16be"))
			return new Utf16Encoding(true);
		if(name.equals("gbk"))
			return new GbkEncoding(false);
		if(name.equals("gb18030"))
			return new GbkEncoding(true);
		if(name.equals("hz-gb-2312"))
			return new HzGb2312Encoding();
		if(name.equals("big5"))
			return new Big5Encoding();
		if(name.equals("shift_jis"))
			return new ShiftJISEncoding();
		if(name.equals("iso-2022-jp"))
			return new Iso2022JPEncoding();
		if(name.equals("euc-jp"))
			return new JapaneseEUCEncoding();
		if(name.equals("euc-kr"))
			return new KoreanEUCEncoding();
		if(name.equals("iso-2022-kr"))
			return new Iso2022KREncoding();
		return null;
	}


	/**
	 * Gets whether an encoding is ASCII compatible
	 * within the meaning of the WHATWG's HTML specification.
	 * 
	 * @param name a name of a character encoding
	 * @return true or false.  Will return false if the name
	 * does not resolve to a supported encoding.
	 */
	public static boolean isAsciiCompatible(String name){
		name=resolveEncoding(name);
		if(name==null)return false;
		// All encodings supported are ASCII-compatible
		// except UTF-16 and the replacement encoding
		return !name.equals("replacement") &&
				!name.equals("utf-16le") &&
				!name.equals("utf-16be");
	}

	/**
	 * Gets a character decoder for a given character
	 * encoding.
	 * @param name a name of a character encoding
	 * @return a character encoder, or null if the name
	 * does not resolve to a supported encoding, or if
	 * no decoder is supported for that encoding.
	 */
	public static ITextDecoder getDecoder(String name){
		name=resolveEncoding(name);
		if(name==null)
			return null;
		ITextEncoder encoder=getIndexEncoding(name);
		if(encoder!=null)return (ITextDecoder)encoder;
		if(name.equals("replacement"))
			return new ReplacementDecoder();
		if(name.equals("utf-8"))
			return new Utf8Encoding();
		if(name.equals("utf-16le"))
			return new Utf16Encoding(false);
		if(name.equals("utf-16be"))
			return new Utf16Encoding(true);
		if(name.equals("gbk"))
			return new GbkEncoding(false);
		if(name.equals("gb18030"))
			return new GbkEncoding(true);
		if(name.equals("hz-gb-2312"))
			return new HzGb2312Encoding();
		if(name.equals("big5"))
			return new Big5Encoding();
		if(name.equals("shift_jis"))
			return new ShiftJISEncoding();
		if(name.equals("iso-2022-jp"))
			return new Iso2022JPEncoding();
		if(name.equals("euc-jp"))
			return new JapaneseEUCEncoding();
		if(name.equals("euc-kr"))
			return new KoreanEUCEncoding();
		if(name.equals("iso-2022-kr"))
			return new Iso2022KREncoding();
		return null;
	}
}
