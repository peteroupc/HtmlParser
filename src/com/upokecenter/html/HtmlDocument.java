/*

Licensed under the Expat License.

Copyright (C) 2013 Peter Occil

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.

 */

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
import com.upokecenter.util.URL;

public final class HtmlDocument {
	private static final class ParseURLListener implements
	IResponseListener<IDocument> {
		@Override
		public IDocument processResponse(String url, InputStream stream,
				IHttpHeaders headers) throws IOException {
			String charset=HeaderParser.getCharset(
					headers.getHeaderField("content-type"),0);
			HtmlParser parser=new HtmlParser(stream,headers.getUrl(),charset);
			return parser.parse();
		}
	}
	/**
	 * 
	 * Gets the absolute URL from an HTML element.
	 * 
	 * @param node A HTML element containing a URL
	 * @return an absolute URL of the element's SRC, DATA, or HREF, or an
	 * empty string if none exists.
	 */
	public static String getHref(IElement node){
		String name=node.getTagName();
		String href="";
		if("A".equals(name) || "LINK".equals(name) || "AREA".equals(name) ||
				"BASE".equals(name)){
			href=node.getAttribute("href");
		} else if("OBJECT".equals(name)){
			href=node.getAttribute("data");
		} else if("IMG".equals(name) || "SCRIPT".equals(name) ||
				"FRAME".equals(name) || "SOURCE".equals(name) ||
				"TRACK".equals(name) ||
				"IFRAME".equals(name) ||
				"AUDIO".equals(name) ||
				"VIDEO".equals(name) ||
				"EMBED".equals(name)){
			href=node.getAttribute("src");
		} else
			return "";
		if(href==null || href.length()==0)
			return "";
		return HtmlDocument.resolveURL(node,href,null);
	}

	/**
	 * Utility method for converting a relative URL to an absolute
	 * one, using the base URI and the encoding of the given node.
	 * 
	 * @param node An HTML node, usually an IDocument or IElement
	 * @param href A relative or absolute URL.
	 * @return An absolute URL.
	 */
	public static String getHref(INode node, String href){
		if(href==null || href.length()==0)
			return "";
		return HtmlDocument.resolveURL(node,href,null);
	}

	/**
	 * 
	 * Parses an HTML document from a file on the file system.
	 * Its address will correspond to that file.
	 * 
	 * @param file
	 * 
	 * @throws IOException
	 */
	public static IDocument parseFile(String file)
			throws IOException {
		InputStream stream=null;
		try {
			String fileURL=new File(file).toURI().toString();
			stream=new BufferedInputStream(new FileInputStream(file),8192);
			HtmlParser parser=new HtmlParser(stream,fileURL,null);
			return parser.parse();
		} finally {
			if(stream!=null) {
				stream.close();
			}
		}
	}

	/**
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
	 * @param stream an input stream representing an HTML document.
	 * @param address an absolute URL representing an address.
	 * @return an IDocument representing the HTML document.
	 * @throws IOException if an I/O error occurs
	 * @throws IllegalArgumentException if the given address
	 * is not an absolute URL.
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
				new ParseURLListener(), false);
	}
	private HtmlDocument(){}

	public static String resolveURL(INode node, String url, String base){
		String encoding=((node instanceof IDocument) ?
				((IDocument)node).getCharacterSet() : node.getOwnerDocument().getCharacterSet());
		if("utf-16be".equals(encoding) ||
				"utf-16le".equals(encoding)){
			encoding="utf-8";
		}
		if(base==null){
			base=node.getBaseURI();
		}
		URL resolved=URL.parse(url,URL.parse(base),encoding,true);
		if(resolved==null)
			return base;
		return resolved.toString();
	}
}
