package com.upokecenter.encoding;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.UnmappableCharacterException;

final class HzGb2312Encoding implements ITextEncoder, ITextDecoder {


	boolean flag=false;
	int lead=0;


	@Override
	public int decode(InputStream stream) throws IOException {
		int[] value=new int[1];
		int c=decode(stream,value,0,1);
		if(c<=0)return -1;
		return value[0];
	}
	@Override
	public int decode(InputStream stream, int[] buffer, int offset, int length)
			throws IOException {
		if(stream==null || buffer==null || offset<0 || length<0 ||
				offset+length>buffer.length)
			throw new IllegalArgumentException();
		if(length==0)return 0;
		int count=0;
		while(length>0){
			int b=stream.read();
			if(b<0 && lead==0){
				break;
			} else if(b<0){
				lead=0;
				buffer[offset++]=(0xFFFD);
				length--;
				count++;
				break;
			}
			if(lead==0x7e){
				lead=0;
				if(b==0x7b){
					flag=true;
					continue;
				} else if(b==0x7d){
					flag=false;
					continue;
				} else if(b==0x7e){
					buffer[offset++]=0x7e;
					length--;
					count++;
					continue;
				} else if(b==0x0a){
					continue;
				} else {
					stream.reset();
					buffer[offset++]=(0xFFFD);
					length--;
					count++;
					continue;
				}
			}
			if(lead!=0){
				int thislead=lead;
				int cp=-1;
				lead=0;
				if(b>=0x21 && b<=0x7e){
					cp=(thislead-1)*190+(b+0x3f);
					cp=GBK.indexToCodePoint(cp);
				}
				if(b==0x0a) {
					flag=false;
				}
				if(cp<0){
					buffer[offset++]=(0xFFFD);
					length--;
					count++;
					continue;					
				} else {
					buffer[offset++]=cp;
					length--;
					count++;
					continue;					
				}
			}
			if(b==0x7e){
				lead=0x7e;
				stream.mark(2);
				continue;
			}
			if(flag){
				if(b>=0x20 && b<=0x7f){
					lead=b;
					continue;
				}
				if(b==0x0a){
					flag=false;
				}
				buffer[offset++]=(0xFFFD);
				length--;
				count++;
				continue;
			}
			if(b<=0x7f){
				buffer[offset++]=b;
				length--;
				count++;
				continue;					
			} else {
				buffer[offset++]=(0xFFFD);
				length--;
				count++;
				continue;
			}
		}
		return (count==0) ? -1 : count;
	}

	@Override
	public void encode(OutputStream stream, int[] array, int offset, int length)
			throws IOException {
		if(stream==null || array==null)throw new IllegalArgumentException();
		if(offset<0 || length<0 || offset+length>array.length)
			throw new IndexOutOfBoundsException();
		for(int i=0;i<array.length;i++){
			int cp=array[offset+i];
			if(cp<0 || cp>=0x110000)
				throw new UnmappableCharacterException(1);
			if(cp<=0x7F && flag){
				flag=false;
				stream.write(0x7E);
				stream.write(0x7D);
			}
			if(cp==0x7E){
				stream.write(0x7E);
				stream.write(0x7E);
			}
			if(cp<=0x7F){
				stream.write(cp);
				break;
			}
			int pointer=GBK.codePointToIndex(cp);
			if(pointer<0)
				throw new UnmappableCharacterException(1);
			if(!flag){
				flag=true;
				stream.write(0x7E);
				stream.write(0x7B);
			}
			int lead=pointer/190+1;
			int trail=pointer%190-0x3f;
			if(lead<0x21 || trail<0x21)
				throw new UnmappableCharacterException(1);
			stream.write(lead);
			stream.write(trail);
		}
	}
}

