package com.upokecenter.html;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.MalformedInputException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.upokecenter.encoding.ITextDecoder;
import com.upokecenter.encoding.TextEncoding;
import com.upokecenter.net.HeaderParser;
import com.upokecenter.net.IHttpHeaders;

final class CharsetSniffer {

	private CharsetSniffer(){}
	
	private static final int NoFeed=0;
	private static final int RSSFeed=1; // application/rss+xml
	private static final int AtomFeed=2; // application/atom+xml

	private static int indexOfBytes(byte[] array, int offset, int count, byte[] pattern){
		int endIndex=Math.min(offset+count, array.length);
		endIndex-=pattern.length-1;
		if(endIndex<0 || endIndex<offset)return -1;
		boolean found=false;
		for(int i=offset;i<endIndex;i++){
			found=true;
			for(int j=0;j<pattern.length;j++){
				if(pattern[j]!=array[i+j]){
					found=false;
					break;
				}
			}
			if(found)return i;
		}
		return -1;
	}

	private static final byte[] rdfNamespace=new byte[]{
		0x68,0x74,0x74,0x70,0x3A,0x2F,0x2F,0x77,0x77,0x77,0x2E,
		0x77,0x33,0x2E,0x6F,0x72,0x67,0x2F,0x31,0x39,0x39,0x39,
		0x2F,0x30,0x32,0x2F,0x32,0x32,0x2D,0x72,0x64,0x66,0x2D,
		0x73,0x79,0x6E,0x74,0x61,0x78,0x2D,0x6E,0x73,0x23
	};
	private static final byte[] rssNamespace=new byte[]{
		0x68,0x74,0x74,0x70,0x3A,0x2F,0x2F,0x70,0x75,0x72,0x6C,
		0x2E,0x6F,0x72,0x67,0x2F,0x72,0x73,0x73,0x2F,0x31,0x2E,
		0x30,0x2F
	};

	private static int sniffFeed(byte[] header, int offset, int count){
		if(header==null || offset<0 || count<0 || offset+count>header.length)
			throw new IllegalArgumentException();
		int endPos=offset+count;
		int index=offset;
		if(index+3<=endPos && (header[index]&0xFF)==0xef &&
				(header[index+1]&0xFF)==0xbb &&
				(header[index+2]&0xFF)==0xbf)
			index+=3;
		while(index<endPos){
			while(index<endPos){
				if(header[index]!=0x09 && header[index]!=0x0a && 
						header[index]!=0x0c && header[index]!=0x0d && 
						header[index]!=0x20){
					if(header[index]!='<'){
						return NoFeed;
					}
					index++;
					break;
				}
				index++;
			}
			while(index<endPos){
				if(index+3<=endPos &&(header[index]&0xFF)==0x21 &&
						(header[index+1]&0xFF)==0x2d && (header[index+2]&0xFF)==0x2d){
					// Skip comment
					int hyphenCount=0;
					index+=3;
					while(index<endPos){
						int c=(header[index]&0xFF);
						if(c=='-'){
							hyphenCount=Math.min(2,hyphenCount+1);
						} else if(c=='>' && hyphenCount>=2){
							index++;
							break;
						} else {
							hyphenCount=0;
						}
						index++;
					}
					break;
				} else if(index+1<=endPos && (header[index]&0xFF)=='!'){
					index++;
					while(index<endPos){
						if(header[index]=='>'){
							index++;
							break;
						}
						index++;
					}				
					break;
				} else if(index+1<=endPos && (header[index]&0xFF)=='?'){
					int charCount=0;
					index++;
					while(index<endPos){
						int c=(header[index]&0xFF);
						if(c=='?'){
							charCount=1;
						} else if(c=='>' && charCount==1){
							index++;
							break;
						} else {
							charCount=0;
						}
						index++;
					}
					break;
				} else if(index+3<=endPos && (header[index]&0xFF)==(int)'r' &&
						(header[index+1]&0xFF)==(int)'s' &&
						(header[index+2]&0xFF)==(int)'s'){
					return RSSFeed;
				} else if(index+4<=endPos && (header[index]&0xFF)=='f' &&
						(header[index+1]&0xFF)=='e' &&
						(header[index+2]&0xFF)=='e' &&
						(header[index+3]&0xFF)=='d'){
					return AtomFeed;
				} else if(index+7<=endPos && (header[index]&0xFF)=='r' &&
						(header[index+1]&0xFF)=='d' &&
						(header[index+2]&0xFF)=='f' &&
						(header[index+3]&0xFF)==':' &&
						(header[index+4]&0xFF)=='R' &&
						(header[index+5]&0xFF)=='D' &&
						(header[index+6]&0xFF)=='F'){
					index+=7;
					if(indexOfBytes(header,index,endPos-index,rdfNamespace)>=0 &&
							indexOfBytes(header,index,endPos-index,rssNamespace)>=0){
						return RSSFeed;
					} else {
						return NoFeed;
					}
				} else {
					return NoFeed;
				}
			}
		}
		return NoFeed;
	}

	static byte[][] patternsHtml=new byte[][]{
		new byte[]{0x3C,0x21,0x44,0x4F,0x43,0x54,0x59,0x50,0x45,0x20,0x48,0x54,0x4D,0x4C},
		new byte[]{(byte)255,(byte)255,(byte)0xdf,(byte)0xdf,(byte)0xdf,(byte)0xdf,(byte)0xdf,(byte)0xdf,(byte)0xdf,(byte)255,(byte)0xdf,(byte)0xdf,(byte)0xdf,(byte)0xdf},
		new byte[]{0x3C,0x48,0x54,0x4D,0x4C},new byte[]{(byte)255,(byte)0xdf,(byte)0xdf,(byte)0xdf,(byte)0xdf},
		new byte[]{0x3C,0x48,0x45,0x41,0x44},new byte[]{(byte)255,(byte)0xdf,(byte)0xdf,(byte)0xdf,(byte)0xdf},
		new byte[]{0x3C,0x53,0x43,0x52,0x49,0x50,0x54},new byte[]{(byte)255,(byte)0xdf,(byte)0xdf,(byte)0xdf,(byte)0xdf,(byte)0xdf,(byte)0xdf},
		new byte[]{0x3C,0x49,0x46,0x52,0x41,0x4D,0x45},new byte[]{(byte)255,(byte)0xdf,(byte)0xdf,(byte)0xdf,(byte)0xdf,(byte)0xdf,(byte)0xdf},
		new byte[]{0x3C,0x48,0x31},new byte[]{(byte)255,(byte)0xdf,(byte)255},
		new byte[]{0x3C,0x44,0x49,0x56},new byte[]{(byte)255,(byte)0xdf,(byte)0xdf,(byte)0xdf},
		new byte[]{0x3C,0x46,0x4F,0x4E,0x54},new byte[]{(byte)255,(byte)0xdf,(byte)0xdf,(byte)0xdf,(byte)0xdf},
		new byte[]{0x3C,0x54,0x41,0x42,0x4C,0x45},new byte[]{(byte)255,(byte)0xdf,(byte)0xdf,(byte)0xdf,(byte)0xdf,(byte)0xdf},
		new byte[]{0x3C,0x41},new byte[]{(byte)255,(byte)0xdf},
		new byte[]{0x3C,0x53,0x54,0x59,0x4C,0x45},new byte[]{(byte)255,(byte)0xdf,(byte)0xdf,(byte)0xdf,(byte)0xdf,(byte)0xdf},
		new byte[]{0x3C,0x54,0x49,0x54,0x4C,0x45},new byte[]{(byte)255,(byte)0xdf,(byte)0xdf,(byte)0xdf,(byte)0xdf,(byte)0xdf},
		new byte[]{0x3C,0x42},new byte[]{(byte)255,(byte)0xdf},
		new byte[]{0x3C,0x42,0x4F,0x44,0x59},new byte[]{(byte)255,(byte)0xdf,(byte)0xdf,(byte)0xdf,(byte)0xdf},
		new byte[]{0x3C,0x42,0x52},new byte[]{(byte)255,(byte)0xdf,(byte)0xdf},
		new byte[]{0x3C,0x50},new byte[]{(byte)255,(byte)0xdf},
		new byte[]{0x3C,0x21,0x2D,0x2D},new byte[]{(byte)255,(byte)255,(byte)255,(byte)255},
	};
	static byte[][] patternsXml=new byte[][]{
		new byte[]{0x3C,0x3F,0x78,0x6D,0x6C},new byte[]{(byte)255,(byte)255,(byte)255,(byte)255,(byte)255},
	};
	static byte[][] patternsPdf=new byte[][]{
		new byte[]{0x25,0x50,0x44,0x46,0x2D},new byte[]{(byte)255,(byte)255,(byte)255,(byte)255,(byte)255}
	};
	static byte[][] patternsPs=new byte[][]{
		new byte[]{0x25,0x21,0x50,0x53,0x2d,0x41,
				0x64,0x6f,0x62,0x65,0x2d},null
	};

	private static boolean matchesPatternAndTagTerminator(
			byte[][] patterns, int index,
			byte[] sequence, int seqIndex, int count){
		byte[] pattern=patterns[index];
		count=Math.min(count,sequence.length-seqIndex);
		byte[] mask=patterns[index+1];
		int len=pattern.length;
		if(len+1<=count){
			for(int i=0;i<len;i++,seqIndex++){
				if((sequence[seqIndex]&mask[i])!=pattern[i])
					return false;
			}
			if(sequence[seqIndex]!=0x20 &&
					sequence[seqIndex]!=0x3e)
				return false;
			return true;
		}
		return false;
	}
	private static boolean matchesPattern(
			byte[][] patterns, int index,
			byte[] sequence, int seqIndex, int count){
		byte[] pattern=patterns[index];
		count=Math.min(count,sequence.length-seqIndex);
		byte[] mask=patterns[index+1];
		int len=pattern.length;
		if(len<=count){
			if(mask==null){
				for(int i=0;i<len;i++,seqIndex++){
					if((sequence[seqIndex])!=pattern[i])
						return false;
				}
			} else {
				for(int i=0;i<len;i++,seqIndex++){
					if((sequence[seqIndex]&mask[i])!=pattern[i])
						return false;
				}

			}
			return true;
		}
		return false;
	}
	private static boolean matchesPattern(
			byte[] pattern,
			byte[] sequence, int seqIndex, int count){
		count=Math.min(count,sequence.length-seqIndex);
		int len=pattern.length;
		if(len<=count){
			for(int i=0;i<len;i++,seqIndex++){
				if((sequence[seqIndex])!=pattern[i])
					return false;
			}
			return true;
		}
		return false;
	}
	private static String sniffUnknownContentType(InputStream input, boolean sniffScriptable) throws IOException{
		byte[] header=new byte[512];
		int count=0;
		input.mark(514);
		try {
			count=input.read(header,0,512);
		} finally {
			input.reset();
		}
		if(sniffScriptable){
			int index=0;
			while(index<count){
				if(header[index]!=0x09 && header[index]!=0x0a &&
						header[index]!=0x0c && header[index]!=0x0d &&
						header[index]!=0x20){
					break;
				}
				index++;
			}
			if(index<count && header[index]==0x3c){
				for(int i=0;i<patternsHtml.length;i+=2){
					if(matchesPatternAndTagTerminator(patternsHtml,
							i,header,index,count)){
						return "text/html";
					}
				}
				for(int i=0;i<patternsXml.length;i+=2){
					if(matchesPattern(patternsXml,
							i,header,index,count)){
						return "text/xml";
					}
				}
			}
			for(int i=0;i<patternsPdf.length;i+=2){
				if(matchesPattern(patternsPdf,
						i,header,0,count)){
					return "text/xml";
				}
			}
		}
		if(matchesPattern(patternsPs,0,header,0,count))
			return "application/postscript";
		if(count>=4 && header[0]==(byte)0xfe && header[1]==(byte)0xff)
			return "text/plain";
		if(count>=4 && header[0]==(byte)0xff && header[1]==(byte)0xfe)
			return "text/plain";
		if(count>=4 && header[0]==(byte)0xef && header[1]==(byte)0xbb &&
				header[2]==(byte)0xbf)
			return "text/plain";
		// Image types
		if(matchesPattern(new byte[]{0,0,1,0},header,0,count))
			return "image/vnd.microsoft.icon";
		if(matchesPattern(new byte[]{0x42,0x4d},header,0,count))
			return "image/bmp";
		if(matchesPattern(new byte[]{0x47,0x49,0x46,0x38,0x37,0x61},header,0,count))
			return "image/gif";
		if(matchesPattern(new byte[]{0x47,0x49,0x46,0x38,0x39,0x61},header,0,count))
			return "image/gif";
		if(matchesPattern(new byte[]{0x52,0x49,0x46,0x46},
				header,0,count) &&
				matchesPattern(new byte[]{0x57,0x45,0x42,0x50,0x56,0x50},
						header,8,count-8))
			return "image/webp";
		if(matchesPattern(new byte[]{(byte)0x89,0x50,0x4e,0x47,0x0d,0x0a,0x1a,0x0a},header,0,count))
			return "image/png";
		if(matchesPattern(new byte[]{(byte)0xff,(byte)0xd8,(byte)0xff},header,0,count))
			return "image/jpeg";
		// Audio and video types
		if(matchesPattern(new byte[]{0x1a,0x45,(byte)0xdf,(byte)0xa3},header,0,count))
			return "video/webm";
		if(matchesPattern(new byte[]{0x2e,0x7e,(byte)0x6e,(byte)0x64},header,0,count))
			return "audio/basic";
		if(matchesPattern(new byte[]{'F','O','R','M'},header,0,count) &&
				matchesPattern(new byte[]{'A','I','F','F'},header,8,count-8))
			return "audio/aiff";
		if(matchesPattern(new byte[]{'I','D','3'},header,0,count))
			return "audio/mpeg";
		if(matchesPattern(new byte[]{'O','g','g','S',0},header,0,count))
			return "application/ogg";
		if(matchesPattern(new byte[]{'M','T','h','d',0,0,0,6},header,0,count))
			return "audio/midi";
		if(matchesPattern(new byte[]{'R','I','F','F'},header,0,count)){
			if(matchesPattern(new byte[]{'A','V','I',' '},header,8,count-8))
				return "video/avi";
			if(matchesPattern(new byte[]{'W','A','V','E'},header,8,count-8))
				return "audio/wave";
		}
		if(count>=12){
			int boxSize=(header[0]&0xFF)<<24;
			boxSize|=(header[1]&0xFF)<<16;
			boxSize|=(header[2]&0xFF)<<8;
			boxSize|=(header[3]&0xFF);
			if((boxSize&3)==0 && boxSize>=0 && count>=boxSize &&
				header[4]==(byte)'f' &&
				header[5]==(byte)'t' &&
				header[6]==(byte)'y' &&
				header[7]==(byte)'p'){
				if(header[8]==(byte)'m' &&
						header[9]==(byte)'p' &&
						header[10]==(byte)'4')
					return "video/mp4";
				int index=16;
				while(index<boxSize){
					if((header[index]&0xFF)=='m' &&
							(header[index+1]&0xFF)=='p' &&
							(header[index+2]&0xFF)=='4')
						return "video/mp4";
					index+=4;
				}
			}
		}
		// Archive types
		if(matchesPattern(new byte[]{0x1f,(byte)0x8b,8},header,0,count))
			return "application/x-gzip";
		if(matchesPattern(new byte[]{'P','K',3,4},header,0,count))
			return "application/zip";
		if(matchesPattern(new byte[]{'R','a','r',' ',0x1a,7,0},header,0,count))
			return "application/x-rar-compressed";
		boolean binary=false;
		for(int i=0;i<count;i++){
			int b=(header[i]&0xFF);
			if(!(b>=0x20 || b==0x09 || b==0x0a || b==0x0c || b==0x0d || b==0x1b)){
				binary=true;
				break;
			}
		}
		if(!binary)return "text/plain";
		return "application/octet-stream";
	}
	
	public static String sniffContentType(InputStream input, IHttpHeaders headers)
	throws IOException {
		String contentType=headers.getHeaderField("content-type");
		if(contentType!=null && (contentType.equals("text/plain") || 
				contentType.equals("text/plain; charset=ISO-8859-1") ||
				contentType.equals("text/plain; charset=iso-8859-1") ||
				contentType.equals("text/plain; charset=UTF-8"))){
			String url=headers.getUrl();
			if(url!=null && url.length()>=5 && 
					(url.charAt(0)=='h' || url.charAt(0)=='H') &&
					(url.charAt(1)=='t' || url.charAt(0)=='T') &&
					(url.charAt(2)=='t' || url.charAt(0)=='T') &&
					(url.charAt(3)=='p' || url.charAt(0)=='P') &&
					(url.charAt(4)==':')){
				return sniffTextOrBinary(input);
			}
		}
		return sniffContentType(input,contentType);
		
	}

	public static String sniffContentType(InputStream input, String mediaType) throws IOException{
		if(mediaType!=null &&
				HeaderParser.skipContentType(mediaType, 0)==mediaType.length()){
			String type=HeaderParser.getMediaType(mediaType,0);
			if(type.equals("text/xml") || type.equals("application/xml") ||
					type.endsWith("+xml")){
				return mediaType;
			}
			if(type.equals("*/*") || type.equals("unknown/unknown") ||
					type.equals("application/unknown")){
				return sniffUnknownContentType(input,true);
			}
			if(type.equals("text/html")){
				byte[] header=new byte[512];
				input.mark(514);
				int count=0;
				try {
					count=input.read(header,0,512);
				} finally {
					input.reset();
				}
				int feed=sniffFeed(header,0,count);
				if(feed==0)return "text/html";
				else if(feed==1)return "application/rss+xml";
				else if(feed==2)return "application/atom+xml";
			}
			return mediaType;
		} else {
			return sniffUnknownContentType(input,true);
		}
	}

	private static String sniffTextOrBinary(InputStream input) throws IOException {
		byte[] header=new byte[512];
		input.mark(514);
		int count=0;
		try {
			count=input.read(header,0,512);
		} finally {
			input.reset();
		}
		if(count>=4 && header[0]==(byte)0xfe && header[1]==(byte)0xff)
			return "text/plain;charset=utf-16be";
		if(count>=4 && header[0]==(byte)0xff && header[1]==(byte)0xfe)
			return "text/plain;charset=utf-16le";
		if(count>=4 && header[0]==(byte)0xef && header[1]==(byte)0xbb &&
				header[2]==(byte)0xbf)
			return "text/plain;charset=utf-8";
		boolean binary=false;
		for(int i=0;i<count;i++){
			int b=(header[i]&0xFF);
			if(!(b>=0x20 || b==0x09 || b==0x0a || b==0x0c || b==0x0d || b==0x1b)){
				binary=true;
				break;
			}
		}
		if(!binary)return "text/plain";
		return sniffUnknownContentType(input,false);
	}
	
	
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
		// Check for UTF-8
		position=0;
		while(position<count){			
			int b=(data[position]&0xFF);
			if(b<0x80){
				position++;
				continue;
			}
			if(position+2<=count &&
					(b>=0xC2 && b<=0xDF) &&
					((data[position+1]&0xFF)>=0x80 && (data[position+1]&0xFF)<=0xBF)
					){
				//DebugUtility.log("%02X %02X",data[position],data[position+1]);
				position+=2;
				maybeUtf8=1;
			} else if(position+3<=count &&
					(b>=0xE0 && b<=0xEF) &&
					((data[position+2]&0xFF)>=0x80 && (data[position+2]&0xFF)<=0xBF)){
				int startbyte=(b==0xE0) ? 0xA0 : 0x80;
				int endbyte=(b==0xED) ? 0x9F : 0xBF;
				//DebugUtility.log("%02X %02X %02X",data[position],data[position+1],data[position+2]);
				if((data[position+1]&0xFF)<startbyte || (data[position+1]&0xFF)>endbyte){
					maybeUtf8=-1;
					break;
				}
				position+=3;
				maybeUtf8=1;
			} else if(position+4<=count &&
					(b>=0xF0 && b<=0xF4) &&
					((data[position+2]&0xFF)>=0x80 && (data[position+2]&0xFF)<=0xBF) &&
					((data[position+3]&0xFF)>=0x80 && (data[position+3]&0xFF)<=0xBF)){
				int startbyte=(b==0xF0) ? 0x90 : 0x80;
				int endbyte=(b==0xF4) ? 0x8F : 0xBF;
				//DebugUtility.log("%02X %02X %02X %02X",data[position],data[position+1],data[position+2],
					//	data[position+3]);
				if((data[position+1]&0xFF)<startbyte || (data[position+1]&0xFF)>endbyte){
					maybeUtf8=-1;
					break;
				}
				position+=4;
				maybeUtf8=1;
			} else {
				if(position+4<count){
					// we check for position here because the data may
					// end within a UTF-8 byte sequence
					maybeUtf8=-1;
				}
				break;
			}
		}
		if(maybeUtf8==1)return EncodingConfidence.UTF8_TENTATIVE;
		// Check for other multi-byte encodings
		boolean hasHighByte=false;
		boolean notKREUC=false;
		boolean notJPEUC=false;
		boolean notShiftJIS=false;
		boolean notBig5=false;
		int maybeHz=0;
		boolean notGbk=false;
		int maybeIso2022=0;
		position=0;
		while(position<count){
			int b=(data[position]&0xFF);
			if(b<0x80){
				if(maybeIso2022==0 && b==0x1b){
					maybeIso2022=1;
				}
				if(maybeHz==0 && b==0x7e){
					maybeHz=1;
				}
				position++;
				continue;
			}
			hasHighByte=true;
			if(b>0xFC){
				notShiftJIS=true;
			}
			if((b>=0x80 && b<=0x8D) || (b==0xFF)){
				notJPEUC=true;
			}
			maybeIso2022=-1;
			maybeHz=-1;
			if(b==0xFF){
				notGbk=true;
			}
			if(b==0x80 || b==0xFF){
				notKREUC=true;
				notBig5=true;
			}
			position++;
		}
		if(maybeHz<=0 && maybeIso2022<=0 && !hasHighByte){
			return EncodingConfidence.UTF8_TENTATIVE;
		}
		List<String> decoders=new ArrayList<String>();
		if(hasHighByte && !notKREUC){
			decoders.add("euc-kr");
		}
		if(hasHighByte && !notBig5){
			decoders.add("big5");
		}
		if(hasHighByte && !notJPEUC){
			decoders.add("euc-jp");
		}
		if(hasHighByte && !notGbk){
			decoders.add("gbk");
		}
		if(hasHighByte && !notShiftJIS){
			decoders.add("shift_jis");
		}
		if(maybeIso2022>0){
			decoders.add("iso-2022-jp");
			decoders.add("iso-2022-kr");
		}
		if(maybeHz>0){
			decoders.add("hz-gb-2312");
		}
		if(decoders.size()>0){
			int[] kana=new int[decoders.size()];
			int[] nonascii=new int[decoders.size()];
			boolean[] nowFailed=new boolean[decoders.size()];
			ByteArrayInputStream[] streams=new ByteArrayInputStream[decoders.size()];
			ITextDecoder[] decoderObjects=new ITextDecoder[decoders.size()];
			for(int i=0;i<decoders.size();i++){
				streams[i]=new ByteArrayInputStream(data,0,count);
				decoderObjects[i]=TextEncoding.getDecoder(decoders.get(i));
			}
			int totalValid=streams.length;
			String validEncoding=null;
			while(true){
				totalValid=0;
				int totalRunning=0;
				for(int i=0;i<streams.length;i++){
					if(streams[i]==null){
						if(decoders.get(i)!=null){
							validEncoding=decoders.get(i);
							totalValid++;
						}
						continue;
					}
					try {
						int c=decoderObjects[i].decode(streams[i]);
						if(c<0){
							// reached end of stream successfully
							streams[i]=null;
						}
						if(c>=0x80){
							nonascii[i]++;
						}
						// if this is a hiragana or katakana
						if(c>=0x3041 && c<=0x30ff){
							kana[i]++;
						}
						//DebugUtility.log("%s %d",decoders.get(i),c);
						validEncoding=decoders.get(i);
						totalValid++;
						totalRunning++;
						nowFailed[i]=false;
					} catch(IOException e){
						// Error
						if(streams[i].available()==0 && e instanceof MalformedInputException){
							// Reached the end of stream; the error
							// was probably due to an incomplete
							// byte sequence
							//DebugUtility.log("at end of stream");
						} else {
							//DebugUtility.log("error %s in %s",
								//	e.getClass().getName(),decoders.get(i));
							streams[i]=null;
							nowFailed[i]=true;
						}
					}
				}
				//if(failedCount>0)
					//DebugUtility.log("failed: %d",failedCount);
				for(int i=0;i<streams.length;i++){
					if(nowFailed[i]){
						nonascii[i]=0;
						kana[i]=0;
						decoderObjects[i]=null;
						decoders.set(i,null);
					}
				}
				if(totalRunning==0 || totalValid<=1) {
					break;
				}
			}
			if(totalValid==1 && validEncoding!=null)
				return new EncodingConfidence(validEncoding);
			//DebugUtility.log(ArrayUtil.toIntList(kana));
			//DebugUtility.log(ArrayUtil.toIntList(nonascii));
			//DebugUtility.log(decoders);
			for(int i=0;i<decoders.size();i++){
				String d=decoders.get(i);
				if(d!=null){
					// return this encoding if the ratio of
					// kana to non-ASCII characters is high
					if(kana[i]>=nonascii[i]/5 && !"gbk".equals(d)){
						return new EncodingConfidence(d);
					}
				}
			}
		}
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
		else if(lang.equals("zh"))
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
				(data[position]==0x09 ||
				data[position]==0x0A ||
				data[position]==0x0C ||
				data[position]==0x0D ||
				data[position]==0x20 ||
				data[position]==0x2F)){
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
