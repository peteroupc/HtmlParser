HTML5 parser for Java.

Takes an input stream or a file and returns an HTML document tree.  
The API is currently only a subset of the DOM.  Example:

    IDocument doc=HtmlDocument.parseFile(filename);
    for(IElement element : doc.getElementsByTagName("img")){
        System.out.println(element.getAttribute("src"));
    }
    
Copyright (C) 2013 Peter Occil.  Licensed under the Expat License.