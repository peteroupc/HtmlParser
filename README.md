HTML5 parser for Java.

Takes an input stream or a file and returns an HTML document tree.  
The API is currently only a subset of the DOM.  Example:

    IDocument doc=HtmlDocument.parseFile(filename);
    for(IElement element : doc.getElementsByTagName("img")){
        System.out.println(element.getAttribute("src"));
    }
    
And here is a more complex example that gets all Open Graph and "image_src"
images specified on a Web page.

    	public static List<String> getWebpageImages(String url) throws IOException {
    		IDocument doc;
    		List<String> images=new ArrayList<String>();
    		doc=HtmlDocument.parseURL(url);
    		for(IElement element : doc.getElementsByTagName("meta")){
    			if("og:image".equals(element.getAttribute("property")) ||
    					"og:image:secure_url".equals(element.getAttribute("property"))){
    				String content=HtmlDocument.getHref(element,element.getAttribute("content"));
    				images.add(content);
    			}
    		}
    		if(images.size()>0)return images;
    		for(IElement element : doc.getElementsByTagName("link")){
    			if("image_src".equals(element.getAttribute("rel"))){
    				String content=HtmlDocument.getHref(element);
    				images.add(content);
    			}
    		}
    		return images;
    	}


Copyright (C) 2013 Peter Occil.  Licensed under the Expat License.

Sample code on this README file is dedicated to the public domain under CC0:
http://creativecommons.org/publicdomain/zero/1.0/

