package com.upokecenter.html;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class HtmlDocument {
	private HtmlDocument(){};
	public static IDocument parseStream(InputStream stream)
			throws IOException {
		HtmlParser parser=new HtmlParser(stream);
		return parser.parse();
	}
	public static IDocument parseFile(String file)
			throws IOException {
		InputStream stream=null;
		try {
			stream=new BufferedInputStream(new FileInputStream(file),8192);
			HtmlParser parser=new HtmlParser(stream);
			return parser.parse();
		} finally {
			if(stream!=null) {
				stream.close();
			}
		}
	}
}
