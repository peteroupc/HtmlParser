package com.upokecenter.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;

public final class StreamUtility {
	private StreamUtility(){}

	public static void skipToEnd(InputStream stream){
		if(stream==null)return;
		while(true){
			byte[] x=new byte[1024];
			try {
				int c=stream.read(x,0,x.length);
				if(c<0) {
					break;
				}
			} catch(IOException e){
				break; // maybe this stream is already closed
			}
		}
	}

	public static void copyStream(InputStream stream, OutputStream output)
			throws IOException {
		byte[] buffer=new byte[8192];
		while(true){
			int count=stream.read(buffer,0,buffer.length);
			if(count<0) {
				break;
			}
			output.write(buffer,0,count);
		}
	}

	public static void inputStreamToFile(InputStream stream, File file)
			throws IOException {
		FileOutputStream output=null;
		try {
			output=new FileOutputStream(file);
			copyStream(stream,output);
		} finally {
			if(output!=null) {
				output.close();
			}
		}
	}

	public static String streamToString(InputStream stream)
			throws IOException {
		return streamToString("UTF-8",stream);
	}

	public static String streamToString(String charset, InputStream stream)
			throws IOException {
		Reader reader = new InputStreamReader(stream, charset);
		StringBuilder builder=new StringBuilder();
		char[] buffer = new char[4096];
		while(true){
			int count=reader.read(buffer);
			if(count<0) {
				break;
			}
			builder.append(buffer,0,count);
		}
		return builder.toString();
	}


	public static void stringToStream(String s, OutputStream stream) throws IOException{
		Writer writer=null;
		try {
			writer=new OutputStreamWriter(stream);
			writer.write(s);
		} finally {
			if(writer!=null) {
				writer.close();
			}
		}
	}

	public static void stringToFile(String s, File file) throws IOException{
		Writer writer=null;
		try {
			writer=new FileWriter(file);
			writer.write(s);
		} finally {
			if(writer!=null) {
				writer.close();
			}
		}
	}

	public static String fileToString(File file)
			throws IOException {
		Reader reader = new FileReader(file);
		try {
			StringBuilder builder=new StringBuilder();
			char[] buffer = new char[4096];
			while(true){
				int count=reader.read(buffer);
				if(count<0) {
					break;
				}
				builder.append(buffer,0,count);
			}
			return builder.toString();
		} finally {
			if(reader!=null) {
				reader.close();
			}
		}
	}

}
