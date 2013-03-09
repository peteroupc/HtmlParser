package com.upokecenter.encoding;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

final class GbkEncoding implements ITextEncoder, ITextDecoder {

	boolean gb18030=true;

	public GbkEncoding(boolean gb18030){
		this.gb18030=gb18030;
	}

	private static final int[] gb18030table=new int[]{
		0,0x0080,
		36,0x00A5,
		38,0x00A9,
		45,0x00B2,
		50,0x00B8,
		81,0x00D8,
		89,0x00E2,
		95,0x00EB,
		96,0x00EE,
		100,0x00F4,
		103,0x00F8,
		104,0x00FB,
		105,0x00FD,
		109,0x0102,
		126,0x0114,
		133,0x011C,
		148,0x012C,
		172,0x0145,
		175,0x0149,
		179,0x014E,
		208,0x016C,
		306,0x01CF,
		307,0x01D1,
		308,0x01D3,
		309,0x01D5,
		310,0x01D7,
		311,0x01D9,
		312,0x01DB,
		313,0x01DD,
		341,0x01FA,
		428,0x0252,
		443,0x0262,
		544,0x02C8,
		545,0x02CC,
		558,0x02DA,
		741,0x03A2,
		742,0x03AA,
		749,0x03C2,
		750,0x03CA,
		805,0x0402,
		819,0x0450,
		820,0x0452,
		7922,0x2011,
		7924,0x2017,
		7925,0x201A,
		7927,0x201E,
		7934,0x2027,
		7943,0x2031,
		7944,0x2034,
		7945,0x2036,
		7950,0x203C,
		8062,0x20AD,
		8148,0x2104,
		8149,0x2106,
		8152,0x210A,
		8164,0x2117,
		8174,0x2122,
		8236,0x216C,
		8240,0x217A,
		8262,0x2194,
		8264,0x219A,
		8374,0x2209,
		8380,0x2210,
		8381,0x2212,
		8384,0x2216,
		8388,0x221B,
		8390,0x2221,
		8392,0x2224,
		8393,0x2226,
		8394,0x222C,
		8396,0x222F,
		8401,0x2238,
		8406,0x223E,
		8416,0x2249,
		8419,0x224D,
		8424,0x2253,
		8437,0x2262,
		8439,0x2268,
		8445,0x2270,
		8482,0x2296,
		8485,0x229A,
		8496,0x22A6,
		8521,0x22C0,
		8603,0x2313,
		8936,0x246A,
		8946,0x249C,
		9046,0x254C,
		9050,0x2574,
		9063,0x2590,
		9066,0x2596,
		9076,0x25A2,
		9092,0x25B4,
		9100,0x25BE,
		9108,0x25C8,
		9111,0x25CC,
		9113,0x25D0,
		9131,0x25E6,
		9162,0x2607,
		9164,0x260A,
		9218,0x2641,
		9219,0x2643,
		11329,0x2E82,
		11331,0x2E85,
		11334,0x2E89,
		11336,0x2E8D,
		11346,0x2E98,
		11361,0x2EA8,
		11363,0x2EAB,
		11366,0x2EAF,
		11370,0x2EB4,
		11372,0x2EB8,
		11375,0x2EBC,
		11389,0x2ECB,
		11682,0x2FFC,
		11686,0x3004,
		11687,0x3018,
		11692,0x301F,
		11694,0x302A,
		11714,0x303F,
		11716,0x3094,
		11723,0x309F,
		11725,0x30F7,
		11730,0x30FF,
		11736,0x312A,
		11982,0x322A,
		11989,0x3232,
		12102,0x32A4,
		12336,0x3390,
		12348,0x339F,
		12350,0x33A2,
		12384,0x33C5,
		12393,0x33CF,
		12395,0x33D3,
		12397,0x33D6,
		12510,0x3448,
		12553,0x3474,
		12851,0x359F,
		12962,0x360F,
		12973,0x361B,
		13738,0x3919,
		13823,0x396F,
		13919,0x39D1,
		13933,0x39E0,
		14080,0x3A74,
		14298,0x3B4F,
		14585,0x3C6F,
		14698,0x3CE1,
		15583,0x4057,
		15847,0x4160,
		16318,0x4338,
		16434,0x43AD,
		16438,0x43B2,
		16481,0x43DE,
		16729,0x44D7,
		17102,0x464D,
		17122,0x4662,
		17315,0x4724,
		17320,0x472A,
		17402,0x477D,
		17418,0x478E,
		17859,0x4948,
		17909,0x497B,
		17911,0x497E,
		17915,0x4984,
		17916,0x4987,
		17936,0x499C,
		17939,0x49A0,
		17961,0x49B8,
		18664,0x4C78,
		18703,0x4CA4,
		18814,0x4D1A,
		18962,0x4DAF,
		19043,0x9FA6,
		33469,0xE76C,
		33470,0xE7C8,
		33471,0xE7E7,
		33484,0xE815,
		33485,0xE819,
		33490,0xE81F,
		33497,0xE827,
		33501,0xE82D,
		33505,0xE833,
		33513,0xE83C,
		33520,0xE844,
		33536,0xE856,
		33550,0xE865,
		37845,0xF92D,
		37921,0xF97A,
		37948,0xF996,
		38029,0xF9E8,
		38038,0xF9F2,
		38064,0xFA10,
		38065,0xFA12,
		38066,0xFA15,
		38069,0xFA19,
		38075,0xFA22,
		38076,0xFA25,
		38078,0xFA2A,
		39108,0xFE32,
		39109,0xFE45,
		39113,0xFE53,
		39114,0xFE58,
		39115,0xFE67,
		39116,0xFE6C,
		39265,0xFF5F,
		39394,0xFFE6,
		39419,0xFFFF
	};

	//39394,65510
	//39419,65535

	private static int GB18030CodePoint(int pointer){
		if((pointer>39419 && pointer<189000) || pointer>1237575)
			return -1;
		if(pointer>=189000)
			return 0x10000+pointer-189000;
		int v=-1;
		for(int i=0;i<gb18030table.length;i+=2){
			if(gb18030table[i]<=pointer){
				v=i;
			} else {
				break;
			}
		}
		return gb18030table[v+1]+pointer-gb18030table[v];
	}


	private static int GB18030Pointer(int codepoint){
		if(codepoint<0x80 || codepoint>=0x110000)
			return -1;
		if(codepoint>=0x10000)
			return 189000+codepoint-0x10000;
		if(codepoint==0xFFFF)
			return 39419;
		int v=-1;
		for(int i=0;i<gb18030table.length;i+=2){
			if(gb18030table[i+1]<=codepoint){
				v=i;
			} else {
				break;
			}
		}
		int size=gb18030table[v+2]-gb18030table[v];
		int offset=codepoint-gb18030table[v+1];
		if(offset>=size)
			return -1;
		return gb18030table[v]+offset;
	}

	int gbk1=0;
	int gbk2=0;
	int gbk3=0;

	@Override
	public int decode(InputStream stream, int[] buffer, int offset, int length, IEncodingError error)
			throws IOException {
		if(stream==null || buffer==null || offset<0 || length<0 ||
				offset+length>buffer.length)
			throw new IllegalArgumentException();
		int count=0;
		while(length>0){
			int b=stream.read();
			if(b<0 && (gbk1|gbk2|gbk3)==0)
				return count;
			else if(b<0){
				int o=error.emitDecoderError(buffer, offset, length);
				offset+=o;
				count+=o;
				length-=o;
				break;
			}
			if(gbk3!=0){
				int cp=0;
				if(b>=0x30 && b<=0x39){
					int index=(((gbk1-0x81)*10+gbk2-0x30)*126+gbk3-0x81)*10+b-0x30;
					cp=GB18030CodePoint(index);
				}
				gbk1=gbk2=gbk3=0;
				if(cp==0){
					stream.reset(); // 'decrease the byte pointer by three'
					int o=error.emitDecoderError(buffer, offset, length);
					offset+=o;
					count+=o;
					length-=o;
					continue;
				} else {
					buffer[offset++]=(cp);
					count++;
					length--;
					continue;
				}
			}
			if(gbk2!=0){
				if(b>=0x80 && b<=0xFE){
					gbk3=b;
					continue;
				} else {
					gbk1=gbk2=0;
					stream.reset(); // 'decrease the byte pointer by two'
					int o=error.emitDecoderError(buffer, offset, length);
					offset+=o;
					count+=o;
					length-=o;
					continue;
				}
			}
			if(gbk1!=0){
				if(b>=0x30 && b<=0x39 && gb18030){
					gbk2=b;
					continue;
				} else {
					int pointer=-1;
					int lead=gbk1;
					gbk1=0;
					if((b>=0x40 && b<=0x7E) || (b>=0x80 && b<=0xFE)){
						int offset2=(b<0x7F) ? 0x40 : 0x41;
						pointer=(lead-0x81)*190+(b-offset2);
					}
					int cp=GBK.indexToCodePoint(pointer);
					if(pointer==-1){
						stream.reset(); // 'decrease byte pointer by one'
					}
					if(cp<=0){
						int o=error.emitDecoderError(buffer, offset, length);
						offset+=o;
						count+=o;
						length-=o;
						continue;
					} else {
						buffer[offset++]=(cp);
						count++;
						length--;
						continue;
					}
				}
			}
			if(b<0x80){
				buffer[offset++]=(b);
				count++;
				length--;
				continue;
			} else if(b==0x80){
				buffer[offset++]=(0x20AC);
				count++;
				length--;
				continue;
			} else if(b<=0xFE){
				stream.mark(4);
				gbk1=b;
			} else {
				int o=error.emitDecoderError(buffer, offset, length);
				offset+=o;
				count+=o;
				length-=o;
				continue;
			}
		}
		return (count==0) ? -1 : count;
	}


	@Override
	public int decode(InputStream stream) throws IOException {
		return decode(stream, TextEncoding.ENCODING_ERROR_THROW);
	}


	@Override
	public int decode(InputStream stream, IEncodingError error) throws IOException {
		int[] value=new int[1];
		int c=decode(stream,value,0,1, error);
		if(c<=0)return -1;
		return value[0];
	}
	@Override
	public void encode(OutputStream stream, int[] array, int offset, int length, IEncodingError error)
			throws IOException {
		if(stream==null || array==null)throw new IllegalArgumentException();
		if(offset<0 || length<0 || offset+length>array.length)
			throw new IndexOutOfBoundsException();
		for(int i=0;i<array.length;i++){
			int cp=array[offset+i];
			if(cp<0 || cp>=0x110000){
				error.emitEncoderError(stream, cp);
				continue;
			}
			if(cp<0x7F){
				stream.write((byte)cp);
			} else {
				int pointer=GBK.codePointToIndex(cp);
				if(pointer>=0){
					int lead=pointer/190+0x81;
					int trail=pointer%190;
					trail+=(trail<0x3F) ? 0x40 : 0x41;
					stream.write(lead);
					stream.write(trail);
					continue;
				}
				if(!gb18030){
					error.emitEncoderError(stream, cp);
					continue;
				}
				pointer=GB18030Pointer(cp);
				if(pointer<0){
					error.emitEncoderError(stream, cp);
					continue;
				}
				int b1=pointer/10/126/10;
				pointer-=b1*10*126*10;
				int b2=pointer/10/126;
				pointer-=b2*10*126;
				int b3=pointer/10;
				int b4=pointer-b3*10;
				stream.write(b1+0x81);
				stream.write(b2+0x30);
				stream.write(b3+0x81);
				stream.write(b4+0x30);
			}
		}
	}


	@Override
	public int decode(InputStream stream, int[] buffer, int offset, int length)
			throws IOException {
		return decode(stream,buffer,offset,length,TextEncoding.ENCODING_ERROR_THROW);
	}


	@Override
	public void encode(OutputStream stream, int[] buffer, int offset, int length)
			throws IOException {
		encode(stream,buffer,offset,length,TextEncoding.ENCODING_ERROR_THROW);
	}


}
