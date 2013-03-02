package com.upokecenter.encoding;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.UnmappableCharacterException;

final class SingleByteEncoding implements ITextEncoder, ITextDecoder {

	int[] indexes;
	int maxValue;
	int minValue;
	public SingleByteEncoding(int[] indexes){
		if(indexes==null || indexes.length<0x80)
			throw new IllegalArgumentException();
		for(int i=0;i<indexes.length;i++){
			maxValue=(i==0) ? indexes[i] : Math.max(maxValue,indexes[i]);
			minValue=(i==0) ? indexes[i] : Math.min(minValue,indexes[i]);
		}
		this.indexes=indexes;
	}


	@Override
	public int decode(InputStream stream) throws IOException {
		if(stream==null)throw new IllegalArgumentException();
		int c=stream.read();
		if(c<0)return -1;
		if(c<0x80)
			return c;
		else {
			int cp=indexes[(c)&0x7F];
			return ((cp==0) ? 0xFFFD : cp);
		}
	}

	@Override
	public int decode(InputStream stream, int[] buffer, int offset, int length)
			throws IOException {
		if(stream==null || buffer==null || offset<0 || length<0 ||
				offset+length>buffer.length)
			throw new IllegalArgumentException();
		byte[] tmp=new byte[1024];
		int i=length;
		int total=0;
		while(i>0){
			int count=stream.read(tmp,0,Math.min(i,buffer.length));
			if(count<0) {
				break;
			}
			total+=count;
			for(int j=0;j<count;j++){
				int c=(tmp[j]&0xFF);
				if(c<0x80){
					buffer[offset++]=(c);
				} else {
					int cp=indexes[(c)&0x7F];
					buffer[offset++]=((cp==0) ? 0xFFFD : cp);
				}
			}
			i-=count;
		}
		return (total==0) ? -1 : total;
	}
	@Override
	public void encode(OutputStream stream, int[] array, int offset, int length)
			throws IOException {
		if(stream==null || array==null)throw new IllegalArgumentException();
		if(offset<0 || length<0 || offset+length>array.length)
			throw new IndexOutOfBoundsException();
		byte[] buffer=null;
		int bufferLength=1024;
		int i=length;
		while(i>0){
			int count=Math.min(i,bufferLength);
			for(int j=0;j<count;j++){
				int c=array[offset];
				if(c<0 || c>maxValue)
					throw new UnmappableCharacterException(1);
				else if(c<0x80){
					if(buffer==null) {
						buffer=new byte[1024];
					}
					buffer[j]=(byte)c;
				} else {
					if(c<minValue)
						throw new UnmappableCharacterException(1);
					int pointer=-1;
					for(int k=0;k<0x80;k++){
						if(indexes[k]==c){
							pointer=k+0x80;
						}
					}
					if(pointer>=0){
						if(buffer==null) {
							buffer=new byte[1024];
						}
						buffer[j]=(byte)pointer;
					} else
						throw new UnmappableCharacterException(1);
				}
				offset++;
			}
			i-=count;
			stream.write(buffer,0,count);
		}
	}
}
