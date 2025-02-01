package com.upokecenter.html.data;

import java.util.*;
import com.upokecenter.html.*;
import com.upokecenter.util.*;
import com.upokecenter.util.*;

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
      return ie != null && name.equals(ie.GetLocalName()) &&
        HtmlCommon.HTML_NAMESPACE.equals(ie.GetNamespaceURI());
    }

    static boolean IsMathMLElement(IElement ie, String name) {
      return ie != null && name.equals(ie.GetLocalName()) &&
        HtmlCommon.MATHML_NAMESPACE.equals(ie.GetNamespaceURI());
    }

    static boolean IsSvgElement(IElement ie, String name) {
      return ie != null && name.equals(ie.GetLocalName()) &&
        HtmlCommon.SVG_NAMESPACE.equals(ie.GetNamespaceURI());
    }

    public static String ResolveURLUtf8(INode node, String url, String _base) {
      _base = (_base == null) ? (node.GetBaseURI()) : _base;
      // TODO: Use URL specification's version instead of RFC 3986
      return com.upokecenter.util.URIUtility.RelativeResolve(url, _base);
    }

    public static String ResolveURL(INode node, String url, String _base) {
      String encoding = (node instanceof IDocument) ? ((IDocument)node).GetCharset() :
        node.GetOwnerDocument().GetCharset();
      if ("utf-16be".equals(encoding) ||
        "utf-16le".equals(encoding)) {
        encoding = "utf-8";
      }
      _base = (_base == null) ? (node.GetBaseURI()) : _base;
      // TODO: Use URL specification's version instead of RFC 3986
      return com.upokecenter.util.URIUtility.RelativeResolve(url, _base);
    }
  }
