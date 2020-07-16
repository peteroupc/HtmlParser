package com.upokecenter.util;

import java.util.*;
using Com.Upokecenter.util;

  final class HtmlCommon {
private HtmlCommon() {
}
    public static final String HTML_NAMESPACE = "http://www.w3.org/1999/xhtml";

    public static final String MATHML_NAMESPACE = "http://www.w3.org/1998/Math/MathML";

    public static final String SVG_NAMESPACE = "http://www.w3.org/2000/svg";

    public static final String XLINK_NAMESPACE = "http://www.w3.org/1999/xlink";

    public static final String
    XML_NAMESPACE = "http://www.w3.org/XML/1998/namespace";

    public static final String
    XMLNS_NAMESPACE = "http://www.w3.org/2000/xmlns/";

    static boolean IsHtmlElement(IElement ie, String name) {
      return ie != null && name.equals(ie.getLocalName()) &&
        HtmlCommon.HTML_NAMESPACE.equals(ie.getNamespaceURI());
    }

    static boolean IsMathMLElement(IElement ie, String name) {
      return ie != null && name.equals(ie.getLocalName()) &&
        HtmlCommon.MATHML_NAMESPACE.equals(ie.getNamespaceURI());
    }

    static boolean IsSvgElement(IElement ie, String name) {
      return ie != null && name.equals(ie.getLocalName()) &&
        HtmlCommon.SVG_NAMESPACE.equals(ie.getNamespaceURI());
    }

    public static String ResolveURL(INode node, String url, String _base) {
      String encoding = (node instanceof IDocument) ? ((IDocument)node).getCharset() :
        node.getOwnerDocument().getCharset();
      if ("utf-16be".equals(encoding) ||
        "utf-16le".equals(encoding)) {
        encoding = "utf-8";
      }
      _base = (_base == null) ? (node.getBaseURI()) : _base;
      URL resolved = URL.parse (url, URL.parse (_base), encoding, true);
      return (resolved == null) ? _base : resolved.toString();
    }
  }
