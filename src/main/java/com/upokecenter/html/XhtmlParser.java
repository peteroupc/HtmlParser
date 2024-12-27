package com.upokecenter.util;

import java.util.*;
import java.io.*;

using Org.System.Xml.Sax;
using Org.System.Xml.Sax.Helpers;
import com.upokecenter.text.*;
using com.upokecenter.net;
using com.upokecenter.util

  class XhtmlParser {
    class ProcessingInstruction extends Node implements IProcessingInstruction {
      public String target, data;

      public ProcessingInstruction() {
 super(
          NodeType.PROCESSING_INSTRUCTION_NODE);
      }

      public String getData() {
        return data;
      }

      public String getTarget() {
        return target;
      }
    }
    class XhtmlContentHandler extends DefaultHandler {
      private List<Element> elements;
      private List<Element> xmlBaseElements;
      private Document document;
      String baseurl;
      String encoding;
      internal boolean useEntities = false;
      public XhtmlContentHandler(XhtmlParser parser) {
        elements = new ArrayList<Element>();
        xmlBaseElements = new ArrayList<Element>();
      }
      @Override public void Characters(char[] arg0, int arg1, int arg2) {
        getTextNodeToInsert (getCurrentNode()).text.appendString (new
String(arg0, arg1, arg2));
      }

      @Override public void Comment(char[] arg0, int arg1, int arg2) {
        Comment cmt = new Comment();
        cmt.setData (new String(arg0, arg1, arg2));
        getCurrentNode().appendChild (cmt);
      }

      @Override public void EndDocument() {
        stopParsing();
      }

      @Override public void EndElement(String arg0, String arg1, String arg2) {
        elements.remove(elements.size() - 1);
      }

      private Node getCurrentNode() {
        return (elements.size() == 0) ?
          (Node)(document) : (Node)(elements.get(elements.size() - 1));
      }

      Document getDocument() {
        return this.document;
      }

      private String getPrefix(String qname) {
        String prefix = "";
        if (qname != null) {
          int prefixIndex = qname.indexOf(':');
          if (prefixIndex >= 0) {
            prefix = qname.substring(0, prefixIndex);
          }
        }
        return prefix;
      }

      private Text getTextNodeToInsert(Node node) {
        List<Node> childNodes = node.getChildNodesInternal();
        Node lastChild = (childNodes.size() == 0) ? null :
          childNodes.get(childNodes.size() - 1);
        if (lastChild == null || lastChild.getNodeType() !=
NodeType.TEXT_NODE) {
          Text textNode = new Text();
          node.appendChild (textNode);
          return textNode;
        } else {
          return (Text)lastChild;
        }
      }
      @Override public void IgnorableWhitespace(char[] arg0, int arg1, int
arg2) {
        getTextNodeToInsert (getCurrentNode()).text.appendString (new
String(arg0, arg1, arg2));
      }

      @Override public void ProcessingInstruction(String arg0, String arg1) {
        ProcessingInstruction pi = new ProcessingInstruction();
        pi.target = arg0;
        pi.data = arg1;
        getCurrentNode().appendChild (pi);
      }

      public InputSource<InputStream> resolveEntity(String name, String publicId,
        String baseURI, String systemId) {
        // Always load a blank external entity
        return new InputSource<InputStream>(new java.io.ByteArrayInputStream(new byte[] { }));
      }

      void setDocument(Document doc) {
        this.document = doc;
      }

      @Override public void SkippedEntity(String arg0) {
        //System.out.println(arg0);
        if (useEntities) {
          int entity = HtmlEntities.getHtmlEntity (arg0);
          StringBuilder builder = getTextNodeToInsert (getCurrentNode()).text;
          if (entity < 0) {
            int[] twoChars = HtmlEntities.getTwoCharacterEntity (entity);
            if (twoChars[0] <= 0xffff) {
              { builder.append ((char)(twoChars[0]));
              }
            } else if (twoChars[0] <= 0x10ffff) {
              builder.append ((char)((((twoChars[0] - 0x10000) >> 10) &
0x3ff) |
                  0xd800));
              builder.append ((char)(((twoChars[0] - 0x10000) & 0x3ff) |
0xdc00));
            }
            if (twoChars[1] <= 0xffff) {
              { builder.append ((char)(twoChars[1]));
              }
            } else if (twoChars[1] <= 0x10ffff) {
              builder.append ((char)((((twoChars[1] - 0x10000) >> 10) &
0x3ff) |
                  0xd800));
              builder.append ((char)(((twoChars[1] - 0x10000) & 0x3ff) |
0xdc00));
            }
          } else if (entity < 0x110000) {
            if (entity <= 0xffff) {
              { builder.append ((char)(entity));
              }
            } else if (entity <= 0x10ffff) {
              builder.append ((char)((((entity - 0x10000) >> 10) & 0x3ff) |
0xd800));
              builder.append ((char)(((entity - 0x10000) & 0x3ff) | 0xdc00));
            }
          }
        }
        throw new SaxException("Unrecognized entity: " + arg0);
      }

      @Override public void StartDtd(String name, String pubid, String sysid) {
        DocumentType doctype = new DocumentType();
        doctype.name = name;
        doctype.publicId = pubid;
        doctype.systemId = sysid;
        document.appendChild (doctype);
        if ("-//W3C//DTD XHTML 1.0 Transitional//EN".equals (pubid) ||
                 "-//W3C//DTD XHTML 1.1//EN".equals (pubid) ||
                 "-//W3C//DTD XHTML 1.0 Strict//EN".equals (pubid) ||
                 "-//W3C//DTD XHTML 1.0 Frameset//EN".equals (pubid) ||
                 "-//W3C//DTD XHTML Basic 1.0//EN".equals (pubid) ||
                 "-//W3C//DTD XHTML 1.1 plus MathML 2.0//EN".equals (pubid) ||
                 "-//W3C//DTD XHTML 1.1 plus MathML 2.0 plus SVG 1.1//EN"
                 .equals (pubid) ||
                 "-//W3C//DTD MathML 2.0//EN".equals (pubid) ||
                 "-//WAPFORUM//DTD XHTML Mobile 1.0//EN".equals (pubid)) {
          useEntities = true;
        }
      }

      @Override public void StartElement(String uri, String localName,
        String arg2,
        IAttributes arg3) {
        String prefix = getPrefix (arg2);
        Element element = new Element();
        element.setLocalName (localName);
        if (prefix.length() > 0) {
          element.setPrefix (prefix);
        }
        if (uri != null && uri.length() > 0) {
          element.setNamespace (uri);
        }
        getCurrentNode().appendChild (element);
        for (int i = 0; i < arg3.length; ++i) {
          String _namespace = arg3.GetUri (i);
          Attr attr = new Attr();
          attr.setName (arg3.GetQName (i)); // Sets prefix and local name
          attr.setNamespace (_namespace);
          attr.setValue (arg3.GetValue (i));
          element.addAttribute (attr);
          if ("xml:base".equals (arg3.GetQName (i))) {
            xmlBaseElements.add(element);
          }
        }
        if ("http://www.w3.org/1999/xhtml".equals (uri) &&
          "base".equals (localName)) {
          String href = element.getAttributeNS ("", "href");
          if (href != null) {
            baseurl = href;
          }
        }
        elements.add(element);
      }

      private void stopParsing() {
        document.encoding = encoding;
        String docbase = document.getBaseURI();
        if (docbase == null || docbase.length() == 0) {
          docbase = baseurl;
        } else {
          if (baseurl != null && baseurl.length() > 0) {
            document.setBaseURI (HtmlDocument.resolveURL(
                document,
                baseurl,
                document.getBaseURI()));
          }
        }
        for (Object baseElement : xmlBaseElements) {
          String xmlbase = baseElement.getAttribute ("xml:base");
          if (!((xmlbase) == null || (xmlbase).length() == 0)) {
            baseElement.setBaseURI (xmlbase);
          }
        }
        elements.clear();
      }
    }
    private static String sniffEncoding(PeterO.Support.InputStream s) {
      byte[] data = new byte[4];
      int count = 0;
      s.mark (data.length + 2);
      try {
        count = s.read (data, 0, data.length);
      } finally {
        s.reset();
      }
      if (count >= 2 && (data[0] & 0xff) == 0xfe && (data[1] & 0xff) == 0xff) {
        return "utf-16be";
      }
      if (count >= 2 && (data[0] & 0xff) == 0xff && (data[1] & 0xff) == 0xfe) {
        return "utf-16le";
      }
      if (count >= 3 && (data[0] & 0xff) == 0xef && (data[1] & 0xff) == 0xbb &&
        (data[2] & 0xff) == 0xbf) {
        return "utf-8";
      }
      if (count >= 4 && (data[0] & 0xff) == 0x00 && data[1] == 0x3c &&
        data[2] == 0x00 && data[3] == 0x3f) {
        return "utf-16be";
      }
      if (count >= 4 && data[0] == 0x3c && data[1] == 0x00 &&
        data[2] == 0x3f && data[3] == 0x00) {
        return "utf-16le";
      }
      if (count >= 4 && data[0] == 0x3c && data[1] == 0x3f &&
        data[2] == 0x78 && data[3] == 0x6d) { // <?xm
        data = new byte[128];
        s.mark (data.length + 2);
        try {
          count = s.read (data, 0, data.length);
        } finally {
          s.reset();
        }
        int i = 4;
        if (i + 1 > count) {
          return "utf-8";
        }
        if (data[i++] != 'l') {
          return "utf-8"; // l in <?xml
        }
        boolean space = false;
        while (i < count) {
          if (data[i] == 0x09 || data[i] == 0x0a || data[i] == 0x0d||
            data[i] == 0x20) {
            { space = true;
            }
            ++i;
          } else {
            break;
          }
        }
        if (!space || i + 7 > count) {
          return "utf-8";
        }
        if (! (data[i] == 'v' && data[i + 1] == 'e' && data[i + 2] == 'r' &&
            data[i + 3] == 's' && data[i + 4] == 'i' && data[i + 5] == 'o' &&
            data[i + 6] == 'n')) {
          return "utf-8";
        }
        i += 7;
        while (i < count) {
          if (data[i] == 0x09 || data[i] == 0x0a || data[i] == 0x0d||
            data[i] == 0x20) {
            ++i;
          } else {
            break;
          }
        }
        if (i + 1 > count || data[i++] != '=') {
          return "utf-8";
        }
        while (i < count) {
          if (data[i] == 0x09 || data[i] == 0x0a || data[i] == 0x0d||
            data[i] == 0x20) {
            ++i;
          } else {
            break;
          }
        }
        if (i + 1 > count) {
          return "utf-8";
        }
        int ch = data[i++];
        if (ch != '"' && ch != '\'') {
          return "utf-8";
        }
        while (i < count) {
          if (data[i] == ch) {
            { i++;
            }
            break;
          }
          ++i;
        }
        space = false;
        while (i < count) {
          if (data[i] == 0x09 || data[i] == 0x0a || data[i] == 0x0d||
            data[i] == 0x20) {
            { space = true;
            }
            ++i;
          } else {
            break;
          }
        }
        if (i + 8 > count) {
          return "utf-8";
        }
        if (! (data[i] == 'e' && data[i + 1] == 'n' && data[i + 2] == 'c' &&
            data[i + 3] == 'o' && data[i + 4] == 'd' && data[i + 5] == 'i' &&
            data[i + 6] == 'n' && data[i + 7] == 'g')) {
          return "utf-8";
        }
        i += 8;
        while (i < count) {
          if (data[i] == 0x09 || data[i] == 0x0a || data[i] == 0x0d||
            data[i] == 0x20) {
            ++i;
          } else {
            break;
          }
        }
        if (i + 1 > count || data[i++] != '=') {
          return "utf-8";
        }
        while (i < count) {
          if (data[i] == 0x09 || data[i] == 0x0a || data[i] == 0x0d||
            data[i] == 0x20) {
            ++i;
          } else {
            break;
          }
        }
        if (i + 1 > count) {
          return "utf-8";
        }
        ch = data[i++];
        if (ch != '"' && ch != '\'') {
          return "utf-8";
        }
        StringBuilder builder = new StringBuilder();
        while (i < count) {
          if (data[i] == ch) {
            String encoding = Encodings.ResolveAlias (builder.toString());
            if (encoding == null) {
              return null;
            }
            return (encoding.equals ("UTF-16LE") || encoding.equals(
                  "UTF-16BE")) ? (null) : (builder.toString());
          }
          builder.append ((char)data[i]);
          ++i;
        }
        return "UTF-8";
      }
      return "UTF-8";
    }

    private String address;

    private IXmlReader reader;

    private InputSource<InputStream> isource;

    private XhtmlContentHandler handler;
    private String encoding;

    private String[] contentLang;
    public XhtmlParser(PeterO.Support.InputStream s, String _string) {
 this(s, _string, null, null);
    }
    public XhtmlParser(PeterO.Support.InputStream s, String _string, String
      charset) {
 this(s, _string, charset, null);
    }

    public XhtmlParser(PeterO.Support.InputStream source, String address,
      String charset, String lang) {
      if (source == null) {
        throw new IllegalArgumentException();
      }
      if (address != null && address.length() > 0) {
        URL url = URL.parse (address);
        if (url == null || url.getScheme().length == 0) {
          throw new IllegalArgumentException();
        }
      }
      this.contentLang = HeaderParser.getLanguages (lang);
      this.address = address;
      try {
        this.reader = new PeterO.Support.SaxReader();
      } catch (SaxException e) {
        if (e.getCause() instanceof IOException) {
          throw (IOException)(e.getCause());
        }
        throw new IOException("", e);
      }
      handler = new XhtmlContentHandler(this);
      try {
        reader.SetFeature ("http://xml.org/sax/features/namespaces", true);
        reader.SetFeature ("http://xml.org/sax/features/use-entity-resolver2",
          true);
        reader.SetFeature ("http://xml.org/sax/features/namespace-prefixes",
  true);
        reader.setLexicalHandler((handler));
      } catch (SaxException e) {
        throw new UnsupportedOperationException("", e);
      }
      reader.setContentHandler((handler));
      reader.setEntityResolver((handler));
      charset = Encodings.ResolveAlias (charset);
      if (charset == null) {
        charset = sniffEncoding (source);
        charset = (charset == null) ? (("utf-8")) : charset;
      }
      charset = Encodings.ResolveAlias (charset);
      this.isource = new InputSource<InputStream>(source);
      this.isource.setEncoding((charset));
      this.encoding = charset;
    }

    public IDocument parse() {
      Document doc = new Document();
      doc.address = address;
      handler.baseurl = doc.address;
      handler.setDocument (doc);
      handler.encoding = encoding;
      try {
        reader.Parse (isource);
      } catch (SaxException e) {
        if (e.getCause() instanceof IOException) {
          throw (IOException)(e.getCause());
        }
        throw new IOException("", e);
      }
      if (contentLang.length == 1) {
        doc.defaultLanguage = contentLang[0];
      }
      return handler.getDocument();
    }
  }
