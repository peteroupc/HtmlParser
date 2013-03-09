package com.upokecenter.encoding;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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
		return decode(stream, TextEncoding.ENCODING_ERROR_THROW);
	}


	@Override
	public int decode(InputStream stream, IEncodingError error) throws IOException {
		if(stream==null)throw new IllegalArgumentException();
		while(true){
			int c=stream.read();
			if(c<0)return -1;
			if(c<0x80)
				return c;
			else {
				int cp=indexes[(c)&0x7F];
				if(cp!=0)return cp;
				if(error.equals(TextEncoding.ENCODING_ERROR_REPLACE))
					return 0xFFFD;
				else {
					int[] data=new int[1];
					int o=error.emitDecoderError(data,0,1);
					if(o>0)return data[0];
				}
			}
		}
	}

	@Override
	public int decode(InputStream stream, int[] buffer, int offset, int length)
			throws IOException {
		return decode(stream, buffer, offset, length, TextEncoding.ENCODING_ERROR_THROW);
	}


	@Override
	public int decode(InputStream stream, int[] buffer, int offset, int length, IEncodingError error)
			throws IOException {
		if(stream==null || buffer==null || offset<0 || length<0 ||
				offset+length>buffer.length)
			throw new IllegalArgumentException();
		byte[] tmp=new byte[1024];
		int i=length;
		int total=0;
		if(TextEncoding.ENCODING_ERROR_REPLACE.equals(error)){
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
		} else {
			int[] data=new int[1];
			while(length>0){
				int c=stream.read();
				if(c<0) {
					break;
				}
				if(c<0x80){
					buffer[offset++]=c;
					total++;
					length--;
				} else {
					int cp=indexes[(c)&0x7F];
					if(cp==0){
						int o=error.emitDecoderError(data,offset,length);
						offset+=o;
						length-=o;
						total+=o;
					} else {
						buffer[offset++]=cp;
						length--;
						total++;
					}
				}
			}
		}
		return (total==0) ? -1 : total;
	}
	@Override
	public void encode(OutputStream stream, int[] array, int offset, int length)
			throws IOException {
		encode(stream, array, offset, length, TextEncoding.ENCODING_ERROR_THROW);
	}


	@Override
	public void encode(OutputStream stream, int[] array, int offset, int length, IEncodingError error)
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
				if(c<0 || c>maxValue){
					error.emitEncoderError(stream, c);
					continue;
				}
				else if(c<0x80){
					if(buffer==null) {
						buffer=new byte[1024];
					}
					buffer[j]=(byte)c;
				} else {
					if(c<minValue){
						error.emitEncoderError(stream, c);
						continue;
					}
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
					} else {
						error.emitEncoderError(stream, c);
						continue;
					}
				}
				offset++;
			}
			i-=count;
			stream.write(buffer,0,count);
		}
	}
}
