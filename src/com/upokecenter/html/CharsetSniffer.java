package com.upokecenter.html;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import com.upokecenter.encoding.TextEncoding;

final class CharsetSniffer {

	private CharsetSniffer(){}
	public static EncodingConfidence sniffEncoding(InputStream stream, String encoding)
			throws IOException{
		stream.mark(3);
		try {
			int b1=stream.read();
			int b2=stream.read();
			if(b1==0xFE && b2==0xFF)
				return EncodingConfidence.UTF16BE;
			if(b1==0xFF && b2==0xFE)
				return EncodingConfidence.UTF16LE;
			int b3=stream.read();
			if(b1==0xEF && b2==0xBB && b3==0xBF)
				return EncodingConfidence.UTF8;
		} finally {
			stream.reset();			
		}
		if(encoding!=null && encoding.length()>0){
			encoding=TextEncoding.resolveEncoding(encoding);
			if(encoding!=null)
				return new EncodingConfidence(encoding,EncodingConfidence.Certain);
		}
		// At this point, the confidence is tentative
		byte[] data=new byte[1024];
		stream.mark(1028);
		int count=0;
		try {
			count=stream.read(data,0,1024);
		} finally {
			stream.reset();
		}
		int position=0;
		while(position<count){
			if(position+4<=count && 
					data[position+0]==0x3c && (data[position+1]&0xFF)==0x21 &&
					(data[position+2]&0xFF)==0x2d && (data[position+3]&0xFF)==0x2d){
				// Skip comment
				int hyphenCount=2;
				position+=4;
				while(position<count){
					int c=(data[position]&0xFF);
					if(c=='-'){
						hyphenCount=Math.min(2,hyphenCount+1);
					} else if(c=='>' && hyphenCount>=2){
						break;
					} else {
						hyphenCount=0;
					}
					position++;
				}
			}
			else if(position+6<=count &&
					data[position]==0x3C &&
					((data[position+1]&0xFF)==0x4D || (data[position+1]&0xFF)==0x6D) &&
					((data[position+2]&0xFF)==0x45 || (data[position+2]&0xFF)==0x65) &&
					((data[position+3]&0xFF)==0x54 || (data[position+3]&0xFF)==0x74) &&
					(data[position+4]==0x41 || data[position+4]==0x61) &&
					(data[position+5]==0x09 || data[position+5]==0x0A || data[position+5]==0x0D ||
					data[position+5]==0x0C || data[position+5]==0x20 || data[position+5]==0x2F)
					){
				// META tag
				boolean haveHttpEquiv=false;
				boolean haveContent=false;
				boolean haveCharset=false;
				boolean gotPragma=false;
				int needPragma=0; // need pragma null
				String charset=null;
				StringBuilder attrName=new StringBuilder();
				StringBuilder attrValue=new StringBuilder();
				position+=5;
				while(true){
					int newpos=CharsetSniffer.readAttribute(data,count,position,attrName,attrValue);
					if(newpos==position) {
						break;
					}
					String attrNameString=attrName.toString();
					if(!haveHttpEquiv && attrNameString.equals("http-equiv")){
						haveHttpEquiv=true;
						if(attrValue.toString().equals("content-type")){
							gotPragma=true;
						}
					} else if(!haveContent && attrNameString.equals("content")){
						haveContent=true;
						if(charset==null){
							String newCharset=CharsetSniffer.extractCharsetFromMeta(attrValue.toString());
							if(newCharset!=null){
								charset=newCharset;
								needPragma=2; // need pragma true
							}
						}
					} else if(!haveCharset && attrNameString.equals("charset")){
						haveCharset=true;
						charset=TextEncoding.resolveEncoding(attrValue.toString());
						needPragma=1; // need pragma false
					}
					position=newpos;
				}
				if(needPragma==0 || (needPragma==2 && !gotPragma) || charset==null){
					position++;
				} else {
					if("utf-16le".equals(charset) || "utf-16be".equals(charset)) {
						charset="utf-8";
					}
					return new EncodingConfidence(charset);
				}
			}
			else if((position+3<=count &&
					data[position]==0x3C &&
					(data[position+1]&0xFF)==0x2F &&
					(((data[position+2]&0xFF)>=0x41 && (data[position+2]&0xFF)<=0x5A) ||
							((data[position+2]&0xFF)>=0x61 && (data[position+2]&0xFF)<=0x7A))) || // </X
							(position+2<=count &&
							data[position]==0x3C &&
							(((data[position+1]&0xFF)>=0x41 && (data[position+1]&0xFF)<=0x5A) ||
									((data[position+1]&0xFF)>=0x61 && (data[position+1]&0xFF)<=0x7A))) // <X
					){
				// </X
				while(position<count){
					if(data[position]==0x09 ||
							data[position]==0x0A ||
							data[position]==0x0C ||
							data[position]==0x0D ||
							data[position]==0x20 ||
							data[position]==0x3E){
						break;
					}
					position++;
				}
				while(true){
					int newpos=CharsetSniffer.readAttribute(data,count,position,null,null);
					if(newpos==position) {
						break;
					}
					position=newpos;
				}
				position++;
			}
			else if(position+2<=count &&
					data[position]==0x3C && 
					((data[position+1]&0xFF)==0x21 || (data[position+1]&0xFF)==0x3F || 
					(data[position+1]&0xFF)==0x2F)){
				// <! or </ or <?
				while(position<count){
					if(data[position]!=0x3E) {
						break;
					}
					position++;
				}
				position++;
			}
			else {
				position++;
			}
		}
		int maybeUtf8=0;
		// Detect UTF-8
		position=0;
		while(position<count){
			if((data[position]&0xFF)<0x80){
				position++;
			} else if(position+2<=count && 
					((data[position]&0xFF)>=0xC2 && (data[position]&0xFF)<=0xDF) &&
					((data[position+1]&0xFF)>=0x80 && (data[position+1]&0xFF)<=0xBF)
					){
				position+=2;
				maybeUtf8=1;
			} else if(position+3<=count &&
					((data[position]&0xFF)>=0xE0 && (data[position]&0xFF)<=0xEF) && 
					((data[position+2]&0xFF)>=0x80 && (data[position+2]&0xFF)<=0xBF)){
				byte startbyte=((data[position]&0xFF)==0xE0) ? (byte)0xA0 : (byte)0x80;
				byte endbyte=((data[position]&0xFF)==0xED) ? (byte)0x9F : (byte)0xBF;
				if((data[position+1]&0xFF)<startbyte || (data[position+1]&0xFF)>endbyte){
					maybeUtf8=-1;
					break;
				}
				position+=3;
				maybeUtf8=1;
			} else if(position+4<=count &&
					((data[position]&0xFF)>=0xF0 && (data[position]&0xFF)<=0xF4) && 
					((data[position+2]&0xFF)>=0x80 && (data[position+2]&0xFF)<=0xBF) &&
					((data[position+3]&0xFF)>=0x80 && (data[position+3]&0xFF)<=0xBF)){
				byte startbyte=((data[position]&0xFF)==0xF0) ? (byte)0x90 : (byte)0x80;
				byte endbyte=((data[position]&0xFF)==0xF4) ? (byte)0x8F : (byte)0xBF;
				if((data[position+1]&0xFF)<startbyte || (data[position+1]&0xFF)>endbyte){
					maybeUtf8=-1;
					break;
				}
				position+=4;
				maybeUtf8=1;
			} else {
				if(position+4>count){
					// we check for position here because the data may
					// end within a UTF-8 byte sequence
					maybeUtf8=-1;
				}
				break;
			}
		}
		if(maybeUtf8==1)return EncodingConfidence.UTF8_TENTATIVE;
		// Fall back
		Locale locale=Locale.getDefault();
		String lang=locale.getLanguage().toLowerCase();
		if(lang.equals("be"))
			return new EncodingConfidence("iso-8859-5");
		if(lang.equals("bg") || lang.equals("ru") || lang.equals("uk"))
			return new EncodingConfidence("windows-1251");
		if(lang.equals("cs") || lang.equals("hu") || lang.equals("pl") || lang.equals("sl"))
			return new EncodingConfidence("iso-8859-2");
		if(lang.equals("ja"))
			return new EncodingConfidence("shift_jis");
		if(lang.equals("zh") && locale.getCountry().toUpperCase().equals("CN"))
			return new EncodingConfidence("gb18030");
		if(lang.equals("zh") && locale.getCountry().toUpperCase().equals("TW"))
			return new EncodingConfidence("big5");
		if(lang.equals("th"))
			return new EncodingConfidence("windows-874");
		if(lang.equals("ko"))
			return new EncodingConfidence("euc-kr");
		if(lang.equals("ku"))
			return new EncodingConfidence("windows-1254");
		if(lang.equals("lt"))
			return new EncodingConfidence("windows-1257");
		if(lang.equals("sk"))
			return new EncodingConfidence("windows-1250");
		if(lang.equals("lv"))
			return new EncodingConfidence("iso-8859-13");
		if(lang.equals("iw") || lang.equals("he")) 
			// NOTE: Java's two-letter code for Hebrew
			return new EncodingConfidence("windows-1255");
		if(maybeUtf8>=0){
			if(lang.equals("ar")||lang.equals("cy")||lang.equals("fa")||
					lang.equals("hr")||lang.equals("kk")||lang.equals("mk")||
					lang.equals("or")||lang.equals("ro")||lang.equals("sr")||
					lang.equals("vi"))
				return EncodingConfidence.UTF8_TENTATIVE;
		}
		return new EncodingConfidence("windows-1252");
	}

	private static int readAttribute(
			byte[] data, 
			int length, 
			int position,
			StringBuilder attrName,
			StringBuilder attrValue
			){
		if(attrName!=null) {
			attrName.setLength(0);
		}
		if(attrValue!=null) {
			attrValue.setLength(0);
		}
		while(position<length &&
				data[position]==0x09 ||
				data[position]==0x0A ||
				data[position]==0x0C ||
				data[position]==0x0D ||
				data[position]==0x20 ||
				data[position]==0x2F){
			position++;
		}
		if(position>=length || data[position]==0x3F)
			return position;
		boolean empty=true;
		boolean tovalue=false;
		// Skip attribute name
		while(true){
			int b=(data[position]&0xFF);
			if(b==0x3D && !empty){
				position++;
				tovalue=true;
				break;
			} else if(b==0x09 || b==0x0a || b==0x0c || b==0x0d || b==0x20){
				break;
			} else if(b==0x2F || b==0x3E)
				return position;
			else {
				if(attrName!=null){
					if(b>=0x41 && b<=0x5a){
						attrName.append((char)(b+0x20));
					} else {
						attrName.append((char)b);
					}
				}
				empty=false;
				position++;
			}
		}
		if(!tovalue){
			while(position<length){
				int b=(data[position]&0xFF);
				if(b!=0x09 && b!=0x0a && b!=0x0c && b!=0x0d && b!=0x20) {
					break;
				}
				position++;
			}
			if(position>=length || (data[position]&0xFF)!=0x3D)
				return position;
			position++;
		}
		while(position<length){
			int b=(data[position]&0xFF);
			if(b!=0x09 && b!=0x0a && b!=0x0c && b!=0x0d && b!=0x20) {
				break;
			}
			position++;
		}
		// Skip value
		if(position>=length)return position;
		int b=(data[position]&0xFF);
		if(b==0x22 || b==0x27){
			position++;
			while(position<length){
				int b2=(data[position]&0xFF);
				if(b==b2) {
					break;
				}
				if(attrValue!=null){
					if(b2>=0x41 && b2<=0x5a){
						attrValue.append((char)(b2+0x20));
					} else {
						attrValue.append((char)b2);
					}
				}
				position++;
			}
			position++;
			return position;
		} else if(b==0x3E)
			return position;
		else {
			if(attrValue!=null){
				if(b>=0x41 && b<=0x5a){
					attrValue.append((char)(b+0x20));
				} else {
					attrValue.append((char)b);
				}
			}
			position++;
		}
		while(true){
			if(position>=length)
				return position;
			b=(data[position]&0xFF);
			if(b==0x09 || b==0x0a || b==0x0c || b==0x0d || b==0x20 || b==0x3e)
				return position;
			if(attrValue!=null){
				if(b>=0x41 && b<=0x5a){
					attrValue.append((char)(b+0x20));
				} else {
					attrValue.append((char)b);
				}
			}
			position++;
		}
	}

	static String extractCharsetFromMeta(String value){
		if(value==null)return value;
		// We assume value is lower-case here
		int index=0;
		int length=value.length();
		while(true){
			index=value.indexOf("charset",0);
			if(index<0)return null;
			index+=7;
			// skip whitespace
			while(index<length){
				char c=value.charAt(index);
				if(c!=0x09 && c!=0x0c && c!=0x0d && c!=0x0a && c!=0x20) {
					break;
				}
				index++;
			}
			if(index>=length)return null;
			if(value.charAt(index)=='='){
				index++;
				break;
			}
		}
		// skip whitespace
		while(index<length){
			char c=value.charAt(index);
			if(c!=0x09 && c!=0x0c && c!=0x0d && c!=0x0a && c!=0x20) {
				break;
			}
			index++;
		}
		if(index>=length)return null;
		char c=value.charAt(index);
		if(c=='"' || c=='\''){
			index++;
			int nextIndex=index;
			while(nextIndex<length){
				char c2=value.charAt(nextIndex);
				if(c==c2)
					return TextEncoding.resolveEncoding(value.substring(index,nextIndex));
				nextIndex++;
			}
			return null;
		} else {
			int nextIndex=index;
			while(nextIndex<length){
				char c2=value.charAt(nextIndex);
				if(c2==0x09 || c2==0x0c || c2==0x0d || c2==0x0a || c2==0x20 || c2==0x3b) {
					break;
				}
				nextIndex++;
			}
			return TextEncoding.resolveEncoding(value.substring(index,nextIndex));
		}
	}

}
