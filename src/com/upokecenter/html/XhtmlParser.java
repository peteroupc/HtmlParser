package com.upokecenter.html;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.ext.DefaultHandler2;
import org.xml.sax.helpers.XMLReaderFactory;

import com.upokecenter.encoding.TextEncoding;
import com.upokecenter.net.HeaderParser;
import com.upokecenter.util.DebugUtility;
import com.upokecenter.util.StringUtility;
import com.upokecenter.util.URL;

class XhtmlParser {

   static class ProcessingInstruction extends Node
  implements IProcessingInstruction {

    public String target,data;

    public ProcessingInstruction() {
      super(NodeType.PROCESSING_INSTRUCTION_NODE);
    }

    @Override
    public String getData() {
      return data;
    }

    @Override
    public String getTarget() {
      return target;
    }

  }
   static class XhtmlContentHandler extends DefaultHandler2
  {
    private final List<Element> elements;
    private final List<Element> xmlBaseElements;
    private Document document;
     String baseurl;
     String encoding;
    boolean useEntities=false;
    public XhtmlContentHandler(XhtmlParser parser){
      elements=new ArrayList<Element>();
      xmlBaseElements=new ArrayList<Element>();
    }
    @Override
    public  void characters(char[] arg0, int arg1, int arg2)
        throws SAXException {
      getTextNodeToInsert(getCurrentNode()).text.appendString(new String(arg0,arg1,arg2));
    }



    @Override
    public  void comment(char[] arg0, int arg1, int arg2)
        throws SAXException {
      Comment cmt=new Comment();
      cmt.setData(new String(arg0,arg1,arg2));
      getCurrentNode().appendChild(cmt);
    }

    @Override
    public  void endDocument() throws SAXException {
      stopParsing();
    }

    @Override
    public  void endElement(String arg0, String arg1, String arg2)
        throws SAXException {
      elements.remove(elements.size()-1);
    }

    private Node getCurrentNode(){
      if(elements.size()==0)return document;
      return elements.get(elements.size()-1);
    }


     Document getDocument(){
      return this.document;
    }

    private String getPrefix(String qname){
      String prefix="";
      if(qname!=null){
        int prefixIndex=qname.indexOf(':');
        if(prefixIndex>=0){
          prefix=qname.substring(0,prefixIndex);
        }
      }
      return prefix;
    }

    private Text getTextNodeToInsert(Node node){
      List<Node> childNodes=node.getChildNodesInternal();
      Node lastChild=(childNodes.size()==0) ? null : childNodes.get(childNodes.size()-1);
      if(lastChild==null || lastChild.getNodeType()!=NodeType.TEXT_NODE){
        Text textNode=new Text();
        node.appendChild(textNode);
        return textNode;
      } else
        return ((Text)lastChild);
    }
    @Override
    public  void ignorableWhitespace(char[] arg0, int arg1, int arg2)
        throws SAXException {
      getTextNodeToInsert(getCurrentNode()).text.appendString(new String(arg0,arg1,arg2));
    }

    @Override
    public  void processingInstruction(String arg0, String arg1)
        throws SAXException {
      ProcessingInstruction pi=new ProcessingInstruction();
      pi.target=arg0;
      pi.data=arg1;
      getCurrentNode().appendChild(pi);
    }

    @Override
    public InputSource resolveEntity(String name, String publicId, String baseURI, String systemId)
        throws SAXException, IOException {
      // Always load a blank external entity
      return new InputSource(new ByteArrayInputStream(new byte[]{}));
    }

     void setDocument(Document doc){
      this.document=doc;
    }

    @Override
    public  void skippedEntity(String arg0) throws SAXException {
      DebugUtility.log(arg0);
      if(useEntities){
        int entity=HtmlEntities.getHtmlEntity(arg0);
        if(entity<0){
          int[] twoChars=HtmlEntities.getTwoCharacterEntity(entity);
          getTextNodeToInsert(getCurrentNode()).text.appendInts(twoChars, 0,2);
        } else if(entity<0x110000){
          getTextNodeToInsert(getCurrentNode()).text.appendInt(entity);
        }
      }
      throw new SAXException("Unrecognized entity: "+arg0);
    }

    @Override
    public  void startDTD(String name, String pubid, String sysid){
      DocumentType doctype=new DocumentType();
      doctype.name=name;
      doctype.publicId=pubid;
      doctype.systemId=sysid;
      document.appendChild(doctype);
      if("-//W3C//DTD XHTML 1.0 Transitional//EN".equals(pubid) ||
          "-//W3C//DTD XHTML 1.1//EN".equals(pubid) ||
          "-//W3C//DTD XHTML 1.0 Strict//EN".equals(pubid) ||
          "-//W3C//DTD XHTML 1.0 Frameset//EN".equals(pubid) ||
          "-//W3C//DTD XHTML Basic 1.0//EN".equals(pubid) ||
          "-//W3C//DTD XHTML 1.1 plus MathML 2.0//EN".equals(pubid) ||
          "-//W3C//DTD XHTML 1.1 plus MathML 2.0 plus SVG 1.1//EN".equals(pubid) ||
          "-//W3C//DTD MathML 2.0//EN".equals(pubid) ||
          "-//WAPFORUM//DTD XHTML Mobile 1.0//EN".equals(pubid)){
        useEntities=true;
      }
    }

    @Override
    public  void startElement(String uri, String localName, String arg2,
        Attributes arg3) throws SAXException {
      String prefix=getPrefix(arg2);
      Element element=new Element();
      element.setLocalName(localName);
      if(prefix.length()>0){
        element.setPrefix(prefix);
      }
      if(uri!=null && uri.length()>0){
        element.setNamespace(uri);
      }
      getCurrentNode().appendChild(element);
      for(int i=0;i<arg3.getLength();i++){
        String namespace=arg3.getURI(i);
        Attr attr=new Attr();
        attr.setName(arg3.getQName(i)); // Sets prefix and local name
        attr.setNamespace(namespace);
        attr.setValue(arg3.getValue(i));
        element.addAttribute(attr);
        if("xml:base".equals(arg3.getQName(i))){
          xmlBaseElements.add(element);
        }
      }
      if("http://www.w3.org/1999/xhtml".equals(uri) &&
          "base".equals(localName)){
        String href=element.getAttributeNS("", "href");
        if(href!=null) {
          baseurl=href;
        }
      }
      elements.add(element);
    }

    private void stopParsing() {
      document.encoding=encoding;
      String docbase=document.getBaseURI();
      if(docbase==null || docbase.length()==0){
        docbase=baseurl;
      } else {
        if(baseurl!=null && baseurl.length()>0){
          document.setBaseURI(HtmlDocument.resolveURL(
              document,baseurl,document.getBaseURI()));
        }
      }
      for(Element baseElement : xmlBaseElements){
        String xmlbase=baseElement.getAttribute("xml:base");
        if(!StringUtility.isNullOrEmpty(xmlbase)) {
          baseElement.setBaseURI(xmlbase);
        }
      }
      elements.clear();
    }
  }
  private static String sniffEncoding(InputStream s) throws IOException{
    byte[] data=new byte[4];
    int count=0;
    s.mark(data.length+2);
    try {
      count=s.read(data,0,data.length);
    } finally {
      s.reset();
    }
    if(count>=2 && (data[0]&0xFF)==0xfe && (data[1]&0xFF)==0xff)
      return "utf-16be";
    if(count>=2 && (data[0]&0xFF)==0xff && (data[1]&0xFF)==0xfe)
      return "utf-16le";
    if(count>=3 && (data[0]&0xFF)==0xef && (data[1]&0xFF)==0xbb &&
        (data[2]&0xFF)==0xbf)
      return "utf-8";
    if(count>=4 && (data[0]&0xFF)==0x00 && data[1]==0x3c &&
        data[2]==0x00 && data[3]==0x3f)
      return "utf-16be";
    if(count>=4 && data[0]==0x3c && data[1]==0x00 &&
        data[2]==0x3f && data[3]==0x00)
      return "utf-16le";
    if(count>=4 && data[0]==0x3c && data[1]==0x3f &&
        data[2]==0x78 && data[3]==0x6d){ // <?xm
      data=new byte[128];
      s.mark(data.length+2);
      try {
        count=s.read(data,0,data.length);
      } finally {
        s.reset();
      }
      int i=4;
      if(i+1>count)return "utf-8";
      if(data[i++]!='l')return "utf-8"; // l in <?xml
      boolean space=false;
      while(i<count){
        if(data[i]==0x09||data[i]==0x0a||data[i]==0x0d||data[i]==0x20)
        { space=true; i++; } else {
          break;
        }
      }
      if(!space || i+7>count)return "utf-8";
      if(!(data[i]=='v' && data[i+1]=='e' && data[i+2]=='r' &&
          data[i+3]=='s' && data[i+4]=='i' && data[i+5]=='o' &&
          data[i+6]=='n'))return "utf-8";
      i+=7;
      while(i<count){
        if(data[i]==0x09||data[i]==0x0a||data[i]==0x0d||data[i]==0x20) {
          i++;
        } else {
          break;
        }
      }
      if(i+1>count || data[i++]!='=')return "utf-8";
      while(i<count){
        if(data[i]==0x09||data[i]==0x0a||data[i]==0x0d||data[i]==0x20) {
          i++;
        } else {
          break;
        }
      }
      if(i+1>count)return "utf-8";
      int ch=data[i++];
      if(ch!='"' && ch!='\'')return "utf-8";
      while(i<count){
        if(data[i]==ch){ i++; break; }
        i++;
      }
      space=false;
      while(i<count){
        if(data[i]==0x09||data[i]==0x0a||data[i]==0x0d||data[i]==0x20)
        { space=true; i++; } else {
          break;
        }
      }
      if(i+8>count)return "utf-8";
      if(!(data[i]=='e' && data[i+1]=='n' && data[i+2]=='c' &&
          data[i+3]=='o' && data[i+4]=='d' && data[i+5]=='i' &&
          data[i+6]=='n' && data[i+7]=='g'))return "utf-8";
      i+=8;
      while(i<count){
        if(data[i]==0x09||data[i]==0x0a||data[i]==0x0d||data[i]==0x20) {
          i++;
        } else {
          break;
        }
      }
      if(i+1>count || data[i++]!='=')return "utf-8";
      while(i<count){
        if(data[i]==0x09||data[i]==0x0a||data[i]==0x0d||data[i]==0x20) {
          i++;
        } else {
          break;
        }
      }
      if(i+1>count)return "utf-8";
      ch=data[i++];
      if(ch!='"' && ch!='\'')return "utf-8";
      StringBuilder builder=new StringBuilder();
      while(i<count){
        if(data[i]==ch){
          String encoding=TextEncoding.resolveEncoding(builder.toString());
          if(encoding==null)
            return null;
          if(encoding.equals("utf-16le") || encoding.equals("utf-16be"))
            return null;
          return builder.toString();
        }
        builder.append((char)data[i]);
        i++;
      }
      return "utf-8";
    }
    return "utf-8";
  }


  private final String address;



  private XMLReader reader;


  private final InputSource isource;

  private final XhtmlContentHandler handler;
  private final String encoding;

  private final String[] contentLang;
  public XhtmlParser(InputStream s, String string) throws IOException {
    this(s,string,null,null);
  }
  public XhtmlParser(InputStream s, String string, String charset) throws IOException {
    this(s,string,charset,null);
  }


  public XhtmlParser(InputStream source, String address, String charset, String lang)
      throws IOException {
    if(source==null)throw new IllegalArgumentException();
    if(address!=null && address.length()>0){
      URL url=URL.parse(address);
      if(url==null || url.getScheme().length()==0)
        throw new IllegalArgumentException();
    }
    this.contentLang=HeaderParser.getLanguages(lang);
    this.address=address;
    try {
      this.reader=XMLReaderFactory.createXMLReader();
    } catch (SAXException e) {
      if(e.getCause() instanceof IOException)
        throw (IOException)(e.getCause());
      throw new IOException(e);
    }
    handler=new XhtmlContentHandler(this);
    try {
      reader.setFeature("http://xml.org/sax/features/namespaces",true);
      reader.setFeature("http://xml.org/sax/features/use-entity-resolver2",true);
      reader.setFeature("http://xml.org/sax/features/namespace-prefixes",true);
      reader.setProperty("http://xml.org/sax/properties/lexical-handler",handler);
    } catch (SAXException e) {
      throw new UnsupportedOperationException(e);
    }
    reader.setContentHandler(handler);
    reader.setEntityResolver(handler);
    charset=TextEncoding.resolveEncoding(charset);
    if(charset==null){
      charset=sniffEncoding(source);
      if(charset==null) {
        charset="utf-8";
      }
    }
    this.isource=new InputSource(source);
    this.isource.setEncoding(charset);
    this.encoding=charset;
  }

  public IDocument parse() throws IOException{
    Document doc=new Document();
    doc.address=address;
    handler.baseurl=doc.address;
    handler.setDocument(doc);
    handler.encoding=encoding;
    try {
      reader.parse(isource);
    } catch (SAXException e) {
      if(e.getCause() instanceof IOException)
        throw (IOException)(e.getCause());
      throw new IOException(e);
    }
    if(contentLang.length==1){
      doc.defaultLanguage=contentLang[0];
    }
    return handler.getDocument();
  }
}
