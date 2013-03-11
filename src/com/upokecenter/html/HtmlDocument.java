package com.upokecenter.html;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.upokecenter.net.DownloadHelper;
import com.upokecenter.net.HeaderParser;
import com.upokecenter.net.IHttpHeaders;
import com.upokecenter.net.IResponseListener;

public final class HtmlDocument {
	private HtmlDocument(){};

	public static String getHref(IElement node){
		String name=node.getTagName();
		String href="";
		if("A".equals(name) || "LINK".equals(name) || "AREA".equals(name) ||
				"BASE".equals(name)){
			href=node.getAttribute("href");
		} else if("IMG".equals(name) || "SCRIPT".equals(name)){
			href=node.getAttribute("src");
		} else
			return "";
		if(href==null || href.length()==0)
			return "";
		return HtmlParser.resolveURL(node,href,null);
	}

	public static String getHref(IElement node, String href){
		if(href==null || href.length()==0)
			return "";
		return HtmlParser.resolveURL(node,href,null);
	}

	public static IDocument parseURL(String url) throws IOException {
		return DownloadHelper.downloadUrl(url, 
				new IResponseListener<IDocument>(){
			@Override
			public IDocument processResponse(String url, InputStream stream,
					IHttpHeaders headers) throws IOException {
				String charset=HeaderParser.getCharset(
						headers.getHeaderField("content-type"));
				HtmlParser parser=new HtmlParser(stream,headers.getUrl(),charset);
				return parser.parse();
			}
		});
	}

	public static IDocument parseStream(InputStream stream)
			throws IOException {
		return parseStream(stream,"about:blank");
	}
	public static IDocument parseStream(InputStream stream, String address)
			throws IOException {
		if(!stream.markSupported()){
			stream=new BufferedInputStream(stream);
		}
		HtmlParser parser=new HtmlParser(stream,address,null);
		return parser.parse();
	}
	public static IDocument parseFile(String file)
			throws IOException {
		InputStream stream=null;
		try {
			String fileURL="file:///"+new File(file).getAbsolutePath().replace("\\", "/");
			stream=new BufferedInputStream(new FileInputStream(file),8192);
			HtmlParser parser=new HtmlParser(stream,fileURL,null);
			return parser.parse();
		} finally {
			if(stream!=null) {
				stream.close();
			}
		}
	}
}
