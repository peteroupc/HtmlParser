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

	/**
	 * 
	 * Gets the absolute URL from an HTML element.
	 * 
	 * @param node An IMG, A, AREA, LINK, BASE, FRAME, or SCRIPT element
	 * @return an absolute URL of the element's SRC or HREF, or an 
	 * empty string if none exists.
	 */
	public static String getHref(IElement node){
		String name=node.getTagName();
		String href="";
		if("A".equals(name) || "LINK".equals(name) || "AREA".equals(name) ||
				"BASE".equals(name)){
			href=node.getAttribute("href");
		} else if("IMG".equals(name) || "SCRIPT".equals(name) || "FRAME".equals(name)){
			href=node.getAttribute("src");
		} else
			return "";
		if(href==null || href.length()==0)
			return "";
		return HtmlParser.resolveURL(node,href,null);
	}

	/**
	 * 
	 * Resolves a URL relative to an HTML element.
	 * 
	 * @param node an HTML element.
	 * @param href Absolute or relative URL.
	 * @return an absolute URL corresponding to the HTML element,
	 * or an empty string if _href_ is null or empty.
	 */
	public static String getHref(IElement node, String href){
		if(href==null || href.length()==0)
			return "";
		return HtmlParser.resolveURL(node,href,null);
	}

	/**
	 * 
	 * Parses an HTML document from a URL.
	 * 
	 * @param url URL of the HTML document. In addition to HTTP
	 * and other URLs supported by URLConnection, this method also
	 * supports Data URLs.
	 * @return a document object from the HTML document
	 * @throws IOException if an I/O error occurs, such as a network
	 * error, a download error, and so on.
	 */
	public static IDocument parseURL(String url) throws IOException {
		return DownloadHelper.downloadUrl(url,
				new IResponseListener<IDocument>(){
			@Override
			public IDocument processResponse(String url, InputStream stream,
					IHttpHeaders headers) throws IOException {
				String charset=HeaderParser.getCharset(
						headers.getHeaderField("content-type"),0);
				HtmlParser parser=new HtmlParser(stream,headers.getUrl(),charset);
				return parser.parse();
			}
		});
	}

	/**
	 * 
	 * Parses an HTML document from an input stream, using "about:blank"
	 * as its address.
	 * 
	 * @param stream an input stream
	 * 
	 * @throws IOException if an I/O error occurs.
	 */
	public static IDocument parseStream(InputStream stream)
			throws IOException {
		return parseStream(stream,"about:blank");
	}

	/**
	 * 
	 * Parses an HTML document from an input stream, using the given
	 * URL as its address.
	 * 
	 * @param stream
	 * @param address
	 * 
	 * @throws IOException
	 */
	public static IDocument parseStream(InputStream stream, String address)
			throws IOException {
		if(!stream.markSupported()){
			stream=new BufferedInputStream(stream);
		}
		HtmlParser parser=new HtmlParser(stream,address,null);
		return parser.parse();
	}
	/**
	 * 
	 * Parses an HTML document from a file on the file system.
	 * 
	 * @param file
	 * 
	 * @throws IOException
	 */
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
